# Runbook · FE (React + Vite) S3 배포 셋업 가이드

> 이 문서는 **FE 담당자에게 전달**할 셋업 가이드다. BE 레포에 위치한 이유 — BE 측이 운영 endpoint / CORS 허용 origin / AWS 인프라 셋업의 흐름을 알아야 FE 와 통합되기 때문. FE 레포에는 이 가이드의 yaml/정책만 박으면 된다.
>
> **대상 독자**: FE 담당자 (React + Vite 작업자)
> **선행 조건**: AWS 계정 접근권한 + GitHub 레포 admin
> **작성 시점**: 2026-05-26 — PR 8 (프론트 S3 통합) FE 부분

---

## 0. 받은 정보 / 확정값

| 항목 | 값 |
|---|---|
| FE 스택 | React + **Vite** |
| Build command | `npm run build` |
| Build 산출물 dir | `dist/` |
| **S3 버킷 이름** | `moodiary-frontend-459338751419-ap-northeast-1-an` |
| **S3 region** | `ap-northeast-1` (도쿄) |
| BE 운영 endpoint | `http://15.165.95.129:8080` |
| BE region | `ap-northeast-2` (서울) — region 이 다름에 주의 |

> ⚠️ **S3 와 BE 의 region 이 다름** — S3 도쿄 / BE 서울. latency 영향은 적지만 (사용자 → S3 → BE 호출), AWS CLI 명령에서 region 을 박을 때 헷갈리지 말 것. S3 작업은 `--region ap-northeast-1`, BE/RDS 는 `--region ap-northeast-2`.

---

## 1. S3 버킷 설정 (이미 만들어진 상태 가정)

### 1-a. Static website hosting 활성화

콘솔: S3 → 버킷 선택 → 속성 (Properties) → 정적 웹 사이트 호스팅 → 편집 → 활성화
- 호스팅 유형: **정적 웹 사이트 호스팅**
- 인덱스 문서: `index.html`
- 오류 문서: `index.html` (SPA 라우팅 — 새로고침 시 404 방지)

저장 후 화면에 표시되는 **Endpoint URL** 을 기록. 예시:
```
http://moodiary-frontend-459338751419-ap-northeast-1-an.s3-website-ap-northeast-1.amazonaws.com
```

(또는 dot 형식: `s3-website.ap-northeast-1.amazonaws.com` — 콘솔이 보여주는 그대로 사용)

이 endpoint URL 을 BE 담당자에게 전달 → BE 의 운영 CORS 허용 origin 에 추가 (별도 BE PR).

### 1-b. Public Read 버킷 정책

콘솔: S3 → 버킷 → 권한 (Permissions) → 버킷 정책 → 편집 → 다음 JSON 붙여넣기:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowPublicRead",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::moodiary-frontend-459338751419-ap-northeast-1-an/*"
    }
  ]
}
```

> 사전 조건: "퍼블릭 액세스 차단" 의 4가지 옵션 중 "퍼블릭 정책으로 인한 액세스 차단" 이 꺼져 있어야 정책이 효력. 권한 탭에서 확인 후 편집.

### 1-c. (선택) CORS 설정 — 보통 필요 없음

브라우저가 S3 의 정적 파일을 직접 fetch 하는 게 아니라 HTML/JS 만 받아서 실행하는 구조라 S3 자체의 CORS 는 보통 불필요. 폰트/이미지 등을 다른 도메인에서 끌어쓰는 경우만 필요.

---

## 2. AWS IAM — GitHub Actions 가 S3 에 push 할 권한

OIDC 방식 권장 (장기 Access Key 보관 불필요, BE 와 동일 패턴).

### 2-a. IAM Identity Provider 등록 (BE 셋업 시 이미 있으면 skip)

콘솔: IAM → Identity providers → Add provider
- Provider type: **OpenID Connect**
- Provider URL: `https://token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`

### 2-b. IAM Role 생성 — FE 전용

콘솔: IAM → Roles → Create role
- Trusted entity type: **Web identity**
- Identity provider: `token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`
- GitHub organization: `Goospel`
- GitHub repository: `<FE 레포 이름>` (예: `moodiary-frontend`)
- GitHub branch (선택): `main`

Role 이름 예: `moodiary-frontend-cd-role`

### 2-c. 권한 정책 — 최소 권한 (인라인)

위 role 의 "권한" 탭 → 인라인 정책 추가:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "S3SyncToFrontendBucket",
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket",
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": [
        "arn:aws:s3:::moodiary-frontend-459338751419-ap-northeast-1-an",
        "arn:aws:s3:::moodiary-frontend-459338751419-ap-northeast-1-an/*"
      ]
    }
  ]
}
```

정책 이름 예: `S3SyncPolicy`

> 왜 `S3FullAccess` 안 쓰는가 — 다른 모든 버킷에 대한 쓰기/삭제 권한까지 줘버린다. 최소 권한 원칙. (BE 에서 동일 원칙 적용했음)

### 2-d. Role ARN 메모

생성된 role 의 ARN 을 복사. 예:
```
arn:aws:iam::459338751419:role/moodiary-frontend-cd-role
```

---

## 3. GitHub Secrets — FE 레포에 박을 값

FE 레포 → Settings → Secrets and variables → Actions → New repository secret

| Secret 이름 | 값 |
|---|---|
| `AWS_GITHUB_OIDC_ROLE_ARN` | 위 2-d 의 role ARN |
| `AWS_REGION` | `ap-northeast-1` |
| `S3_BUCKET` | `moodiary-frontend-459338751419-ap-northeast-1-an` |
| `VITE_API_BASE_URL` | `http://15.165.95.129:8080` |

> **`VITE_API_BASE_URL` 변수명 권장 이유** — Vite 는 `VITE_*` prefix 만 클라이언트 코드에서 접근 가능. `VITE_API_BASE_URL` 이 의미가 명확하고 향후 게이트웨이/리버스 프록시 추가 시에도 의미 변하지 않음. FE 코드에서는 `import.meta.env.VITE_API_BASE_URL` 로 사용.
>
> 변수명을 다른 걸로 정해도 무방 — 단, 다음 4가지를 같은 이름으로 통일해야 함:
> 1. FE 코드 (`import.meta.env.<NAME>`)
> 2. FE 레포의 GitHub Secret
> 3. 아래 yaml 의 `env:` 섹션
> 4. 로컬 개발의 `.env.local`

---

## 4. FE 레포에 박을 GitHub Actions yaml

FE 레포의 `.github/workflows/frontend-cd.yaml` 로 저장:

```yaml
# CD — main 머지 시 S3 정적 호스팅에 자동 배포.

name: FE - Build & Deploy to S3

on:
  push:
    branches: [main]

jobs:
  build-and-deploy:
    runs-on: ubuntu-24.04
    permissions:
      id-token: write   # OIDC 토큰 발급용
      contents: read

    steps:
      - uses: actions/checkout@v4

      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: '20'   # 또는 프로젝트 .nvmrc 의 버전
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Build
        env:
          VITE_API_BASE_URL: ${{ secrets.VITE_API_BASE_URL }}
        run: npm run build
        # 산출물은 dist/ 에 생성됨 (Vite 기본값)

      - name: Configure AWS credentials (OIDC)
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_GITHUB_OIDC_ROLE_ARN }}
          aws-region: ${{ secrets.AWS_REGION }}

      # 캐시 정책 분리:
      #   - hash 가 박힌 정적 자산 (JS/CSS/이미지) — 캐시 길게
      #   - index.html — 캐시 짧게 (배포 직후 새 hash 의 자산을 가리키는 새 index 가 빠르게 적용되도록)
      - name: Sync to S3 (hashed assets, long cache)
        run: |
          aws s3 sync dist/ s3://${{ secrets.S3_BUCKET }}/ \
            --delete \
            --cache-control "public, max-age=31536000, immutable" \
            --exclude "*.html" \
            --exclude "*.json"

      - name: Sync to S3 (index.html / json, short cache)
        run: |
          aws s3 sync dist/ s3://${{ secrets.S3_BUCKET }}/ \
            --cache-control "public, max-age=60, must-revalidate" \
            --exclude "*" \
            --include "*.html" \
            --include "*.json"

      - name: Output deployment URL
        run: |
          echo "✅ Deployed to:"
          echo "http://${{ secrets.S3_BUCKET }}.s3-website-ap-northeast-1.amazonaws.com"
```

---

## 5. 첫 배포 + 검증

### 5-a. 머지 → CD 진행

FE 레포 `main` 에 push (또는 PR 머지) → GitHub Actions 의 `FE - Build & Deploy to S3` 워크플로우 자동 실행.

### 5-b. S3 endpoint 접근 확인

브라우저로 endpoint URL 열기. 빈 화면이면 다음 차례로 의심:

| 증상 | 원인 | 해결 |
|---|---|---|
| `403 Forbidden` | 버킷 정책 미적용 또는 "퍼블릭 액세스 차단" 활성 | 1-b 의 정책 + 퍼블릭 액세스 차단 옵션 확인 |
| `404 NoSuchKey` 또는 빈 페이지 | `index.html` 이 버킷 루트에 없음 | `aws s3 ls s3://<버킷>/` 으로 dist 내용이 잘 올라갔는지 확인 |
| 화면은 뜨는데 API 호출이 CORS error | BE 의 CORS 허용 origin 에 S3 endpoint URL 이 안 들어감 | BE 담당자에게 S3 endpoint URL 전달 → BE 가 운영 `.env` 의 `APP_CORS_ALLOWED_ORIGINS` 에 추가 |
| API 호출이 mixed content blocked | FE 는 https, BE 는 http | 일단 FE 도 http 로 접근 (S3 static website 는 기본 http). 나중에 ALB+ACM 으로 https 화 |

### 5-c. API 통합 검증

S3 endpoint 에서 회원가입 → 로그인 → 다이어리 작성 → 캘린더 조회 흐름이 다 동작해야 PR 8 완료.

---

## 6. BE 측 후속 작업 (FE 담당자가 알아야 할 것만)

BE 가 별도 PR 로 다음을 진행함 (S3 endpoint URL 받은 후):

1. **`compose.yaml` 에 `APP_CORS_ALLOWED_ORIGINS` env 항목 추가** — 현재 운영 컨테이너는 yaml 기본값 (`localhost:3000,localhost:5173`) 으로만 도는 중. EC2 `.env` 에서 값을 받게 해야 S3 origin 추가 가능.
2. **EC2 `.env` 에 `APP_CORS_ALLOWED_ORIGINS=http://<S3-endpoint>,http://localhost:5173` 추가**
3. **운영 release** → 새 컨테이너가 S3 origin 도 허용

FE 가 BE 통신을 시작하기 직전에 BE 가 이 셋업을 끝내놔야 첫 요청에서 CORS 통과.

---

## 7. 트러블슈팅 — 자주 막히는 곳

### 7-a. OIDC role 의 trust policy 가 잘못된 경우
GitHub Actions 로그에 `Not authorized to perform sts:AssumeRoleWithWebIdentity`. 원인:
- Role 의 trust policy 의 `repo:Goospel/<레포>:*` 패턴이 실제 레포명/브랜치와 매치 안 됨
- Audience (`sts.amazonaws.com`) 가 다름

해결: IAM Console → Role → Trust relationships 탭에서 다음과 같은지 확인 (레포명 부분):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::459338751419:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:Goospel/<FE-레포명>:*"
        }
      }
    }
  ]
}
```

### 7-b. `aws s3 sync` 가 권한 부족
인라인 정책 (2-c) 의 Resource ARN 이 정확한 버킷 이름인지 — `arn:aws:s3:::<bucket-name>` (버킷 본체) + `arn:aws:s3:::<bucket-name>/*` (오브젝트) **두 개 다** 있어야 함.

### 7-c. `npm run build` 가 fail
- `package.json` 의 `scripts.build` 확인
- Node 버전 mismatch — workflow 의 `node-version` 을 FE 의 `.nvmrc` 또는 `package.json` 의 `engines` 와 일치
- `npm ci` 가 fail → `package-lock.json` 이 commit 됐는지 확인

---

## 8. BE 담당자에게 회신 시 보내줄 정보

배포가 성공하면 BE 담당자에게 다음을 알려주기:

```
S3 endpoint URL: http://moodiary-frontend-459338751419-ap-northeast-1-an.s3-website-ap-northeast-1.amazonaws.com
  (콘솔의 정적 웹 사이트 호스팅 endpoint 그대로)

이 URL 을 BE 의 운영 .env 의 APP_CORS_ALLOWED_ORIGINS 에 추가 부탁드립니다.
```

이걸 받으면 BE 가 별도 PR 로 CORS 허용 → FE 의 API 호출 흐름 통합.
