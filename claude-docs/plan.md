# Moodiary Backend — Roadmap

> 백엔드 작업 진행 상황과 계획을 한 곳에 모은 문서.
> **규칙**: PR이 머지될 때마다 갱신. 완료는 `[x]`로 체크, 진행 중이면 간단히 메모.
> 마지막 갱신: 2026-05-24 (PR #27 release 머지 — 인증 라인 + Post 소유권 운영 반영)
>
> 📚 **관련 문서**:
> - [`README.md`](../README.md) — 프로젝트 소개 (외부 공개용)
> - [`api-contracts.md`](./api-contracts.md) — API 상세 명세 (request/response/예시, 외부 AI 서버 계약)
> - [`security.md`](./security.md) — JWT / 비밀번호 / 시크릿 관리 / 인가 규칙 / 약점 정리
> - [`troubleshooting.md`](./troubleshooting.md) — 막혔던 지점 + 원인 + 해결 로그 (지속 누적)
> - [Swagger UI](http://15.165.95.129:8080/swagger-ui/index.html) — 구현된 API의 실시간 진실의 원천

---

## 📊 현재 상태 한눈에

| 항목 | 상태 |
|---|---|
| **운영 URL** | http://15.165.95.129:8080 (Elastic IP, 고정) |
| **Swagger UI** | http://15.165.95.129:8080/swagger-ui/index.html |
| **OpenAPI spec** | http://15.165.95.129:8080/v3/api-docs (FE 협업용) |
| **운영 인프라** | EC2 (Amazon Linux 2023) + Docker + RDS MySQL 9.x |
| **배포 방식** | `push → main` → GitHub Actions → AWS SSM → EC2 `docker run` |
| **현재 운영에 올라간 기능** | **Post CRUD + 인증(회원가입/JWT 로그인) + Post 소유권** |
| **dev에 있고 운영 미반영** | (PR 5 머지 시) Calendar API — emoji=null 임시 처리 |
| **핵심 미구현 (예정)** | **AI 비동기 응답** / **프론트엔드 S3 배포 + 통합** / 캘린더 emoji LEFT JOIN / Flyway |
| **Java/Spring** | Java 25 / Spring Boot 4.0.6 |

> 🛠️ **즉시 조치 필요 없음.** 인증 라인 완성 + 운영 반영까지 끝. 다음 핵심 기능(AI) 들어갈 좋은 출발선.

---

## ✅ 완료된 작업

> 운영 반영 = 🟢, dev only = 🟡

### Post 도메인 — 🟢 운영 반영
| 엔드포인트 | PR | 비고 |
|---|---|---|
| `POST /post` | #14, #24 | 인증 필수, 작성자 자동 채움 |
| `GET /post` | #14, #24 | **본인 글만** (소유권 격리) |
| `GET /post/{id}` | #14, #24 | 본인 글 200, 타인 글 403, 없으면 404 |
| `PUT /post/{id}` | #14, #24 | 본인 글만 수정 가능 |
| `DELETE /post/{id}` | #14, #24 | 본인 글만 삭제 가능, 204 |
| 전역 예외 처리 | #14, #20, #23, #24 | 400/401/403/404/409/500 → `{"message":"..."}` 통일 |

### 인증 — 🟢 운영 반영
| 엔드포인트 | PR | 비고 |
|---|---|---|
| `POST /auth/signup` | #20 | 이메일/닉네임 UNIQUE, BCrypt, 영문+숫자 8자 이상 |
| `POST /auth/login` | #23 | HS512 JWT (24h), refresh 없음, 열거 공격 방지 401 |
| SecurityFilterChain | #21 | 화이트리스트 외 `authenticated()`, 미인증 401 JSON |
| JwtAuthenticationFilter | #23 | Bearer 토큰 → `SecurityContext` UUID principal 주입 |

### 인프라 — 🟢 운영 반영
- [x] Swagger UI (SpringDoc OpenAPI 2.8.3)
- [x] CI/CD GitHub Actions
  - CI: `PR → dev` → `./gradlew build`
  - CD: `push → main` → Docker Hub → AWS SSM → EC2 `docker run`
- [x] AWS SSM 기반 배포 (옛 SSH 방식 폐기)
- [x] **Elastic IP 부착** (`15.165.95.129`) — 인스턴스 stop/start해도 IP 고정
- [x] **EC2 docker compose를 RDS로 통일** — 로컬 mysql 컨테이너 폐기, `.env` 기반 자격증명
- [x] RDS Security Group: EC2 → 3306 인바운드 허용
- [x] **`application.yaml` 추적 + env var 플레이스홀더 패턴** — 비밀값은 `${ENV_VAR:기본값}` (PR #24)
- [x] **`ddl-auto: update`** — 스키마 변경 시 컨테이너 부팅으로 자동 보강 (PR #25)
- [x] **CD 워크플로우에 JWT_SECRET 주입** — `docker run -e JWT_SECRET=...` (PR #26)
- [x] GitHub Secret `JWT_SECRET` 등록 + EC2 `.env`에 동일 키 반영

### 테스트 — 🟢 운영 반영
- [x] `PostControllerTest` — `@WebMvcTest` + `@MockitoBean` 슬라이스 테스트 (PR #16, #24)
  - 인증 흐름 16 cases (CRUD 13 + 미인증 401 3)
- [x] `PostServiceTest` — 순수 Mockito 단위 (PR #19, #24)
  - 12 cases (CRUD 9 + 소유권 분기 3)
- [x] `UserServiceTest` — 회원가입 + 로그인 분기 (PR #20, #23)
  - 6 cases (signup 3 + login 3)
- [x] `AuthControllerTest` — `@WebMvcTest` (PR #20, #23)
  - 11 cases (signup 7 + login 4)
- [x] `JwtTokenProviderTest` — 라운드트립 / 만료 / 위조 / 형식 검증 (PR #23)
  - 4 cases
- [x] `JpaAuditingConfig` 분리 (`@EnableJpaAuditing`을 `MoodiaryApplication`에서 빼냄)
  - 슬라이스 테스트 호환 필수

**총 49 passed, 1 skipped, 0 failures** (최근 빌드 기준)

### 문서/관리 — 🟢 운영 반영
- [x] `CLAUDE.md` — Claude Code 가이드 + **Workflow rules** 섹션 (PR #25)
- [x] `claude-docs/plan.md` 이 문서 (Roadmap)
- [x] `claude-docs/api-contracts.md` API 명세 (PR #18)
- [x] `claude-docs/troubleshooting.md` 누적 트러블슈팅 (PR #22, #24)
- [x] `.local-logs/` 폴더 + `.gitignore` (운영 로그 비추적)

---

## 🔄 진행 중

> **PR 5 — Calendar API** (AI 합의 대기 중 우회 작업)
> - `GET /calendar?year=YYYY&month=MM` 엔드포인트 신설
> - QueryDSL 첫 도입 — `CalendarRepository` 가 `JPAQueryFactory` 사용
> - 그 달 전체 일수 배열 반환 (빈 날 포함), KST 기준 일자 그룹핑, 하루 다중 글이면 마지막 글
> - **emoji 필드는 항상 null** — PR 4 머지 후 후속 PR 에서 AiResponse LEFT JOIN 추가
> - `build.gradle` 의 querydsl-jpa 가 `compileOnly` → `implementation` 변경 (CLAUDE.md trap 갱신)
> - `MissingServletRequestParameterException` 핸들러 추가 (필수 파라미터 누락 → 400)
> - 테스트 13 추가 (CalendarServiceTest 9 + CalendarControllerTest 4)

---

## 📋 백로그 (우선순위 + 의존성)

```
PR 8 프론트 S3 배포 + CORS   (독립적, 데모 가시성 ⭐⭐⭐)
                              ↑ BE 담당자가 프론트 CD 까지 직접 세팅

PR 4 AI 비동기 응답           (AI 담당자 합의 필요 ⭐⭐⭐)
    └─► 캘린더 emoji LEFT JOIN 후속 PR (PR 5-후속)

PR 6 Flyway                  (PR 4 의 AiResponse 스키마와 같이 도입 권장)

PR 7 ECS 이전                (먼 미래, HTTPS / 도메인 도입 시 자연스러움)
```

> **다음 핵심 경로**: **PR 8 → PR 4 → (PR 5 후속 emoji JOIN)**.
> - **PR 8** 이 가시성 우선 — 졸업 발표에 "프론트가 떴고 백엔드와 통신" 까지 필수.
> - **PR 4** 는 차별 기능 (AI 응답 + 이모지). AI 담당자 답변 대기 중이면 PR 8 부터 진행.
> - **PR 6** 은 스키마 안정화 후 (PR 4 머지 시점) 같이.

### PR 4 — 비동기 AI 응답 모듈 (글 + 기분 이모지) 🤖 ⭐⭐⭐
**Why**: 프로젝트 핵심 차별 기능. 일기 → AI가 **응답 텍스트 + 기분 이모지** 둘 다 생성. 이모지는 PR 5 캘린더에서 사용됨.

**설계 방향**: 단순화된 비동기 (큐/메시지브로커 없이 `@Async` + DB 상태 관리)

📋 **API 상세 명세** → [`api-contracts.md#ai-response-폴링-pr-4`](./api-contracts.md#ai-response-폴링-pr-4)
🔌 **외부 AI 서버 계약** → [`api-contracts.md#ai-추론-서버-pr-4`](./api-contracts.md#ai-추론-서버-pr-4)

**구현 체크리스트**:
- [ ] `AiResponse` 엔티티: `id`, `post_id`, `status (PENDING/DONE/FAILED)`, `content`, `emoji` (VARCHAR(8)), `error_message`, `BaseEntity`
- [ ] `AiResponseRepository`
- [ ] `@EnableAsync` + `@Async` 메서드 (전용 스레드풀)
- [ ] AI 서버 호출: `RestClient` (Spring 6 새 동기 클라이언트)
- [ ] `POST /post` 흐름 변경: 일기 저장 → `AiResponse(PENDING)` 생성 → `@Async`로 호출 트리거 → 즉시 201 반환
- [ ] `GET /post/{id}/ai-response` 폴링 엔드포인트
- [ ] 재시도/타임아웃 정책 (Spring Retry)
- [ ] DB 컬럼은 **`utf8mb4`** 사용 (이모지 필수)
- [ ] 테스트: AI 서버 mock (WireMock 또는 `@MockitoBean` RestClient)
- [ ] **선행 합의 필요** — AI 담당자와의 계약 체크리스트는 [api-contracts.md의 합의 항목](./api-contracts.md#합의-항목-체크리스트) 참조

**예상 소요**: 1-2주 | **의존**: 없음 (PR 3 인증/소유권 이미 운영 반영) | **위험**: AI 서버 다운 시 일기 작성 자체는 안 막히게 fail-safe 설계 필수, 이모지 인코딩(utf8mb4)

---

### PR 5 — Calendar API 📅 ⭐⭐ (🔄 진행 중)
**Why**: 프로젝트 핵심 화면. 월별 보기로 "그 달 내가 어떤 기분이었는지" 한눈에 확인. AI가 만든 이모지를 날짜에 매핑.

📋 **API 상세 명세** → [`api-contracts.md#calendar-pr-5`](./api-contracts.md#calendar-pr-5)

**구현 체크리스트**:
- [x] `CalendarController` + `CalendarService`
- [x] `CalendarDayResponseDto` (date, emoji, postId)
- [x] Repository 쿼리: 본인 + 지정 월 → QueryDSL 로 `BETWEEN` 조회, in-memory 일자별 그룹핑
  - 한 달 단위 row 가 많지 않아 윈도우 함수 대신 application 그룹핑 채택
- [ ] **Post + AiResponse JOIN** — emoji 가져오기 — **PR 4 머지 후 후속 PR**
- [x] 시간대 처리 — KST 기준 일자 그룹핑 (`LocalDate` 변환)
- [ ] **인덱스**: `post(user_id, created_at)` 복합 인덱스 — DDL 수동 적용 필요 (`ddl-auto: update` 가 보장 안 함)
- [x] 빈 달도 그 달 실제 일수만큼 채워서 반환
- [x] 컨트롤러/서비스 테스트: 빈 달, 일부 채워진 달, 같은 날 여러 글 → 마지막 글, 윤년
- [x] **유효성 검증**: year ≥ 2020, year ≤ 현재+1, month 1-12 → `CalendarInvalidRangeException` → 400

**부수 효과**:
- `build.gradle` querydsl-jpa: `compileOnly` → `implementation` (NoClassDefFoundError 해결)
- `GlobalExceptionHandler` 에 `MissingServletRequestParameterException` 매핑 (400 일관화)

**예상 소요**: 3-5일 | **의존**: PR 4 (AiResponse 엔티티 — emoji JOIN 한정) | **위험**: 타임존 버그(KST/UTC 혼동), 인덱스 누락 시 월 조회 풀스캔

---

### PR 6 — Flyway 도입 (DB 마이그레이션 + 스키마 잠금) ⭐
**Why**: 현재 `ddl-auto: update`는 빠른 개발에 좋지만, 컬럼 삭제/타입 변경/제약 추가가 안 됨. 스키마 안정화 시점에 Flyway 로 잠궈서 명시적 마이그레이션 관리.

- [ ] `flyway-mysql` 의존성 추가
- [ ] `src/main/resources/db/migration/V1__baseline.sql` — 현재 운영 스키마 베이스라인 (users + post + ai_response)
- [ ] `ddl-auto: validate`로 변경 (자동 DDL 위험 차단)
- [ ] 운영 RDS에 `flyway baseline` 적용 (이미 운영 중인 DB라 베이스라인 필요)
- [ ] CLAUDE.md / plan.md에 "ddl-auto: validate 모드로 전환됨" 명시

**예상 소요**: 1-2일 | **의존**: PR 4 완성 시점 (AiResponse 스키마도 포함된 베이스라인) | **위험**: 베이스라인 적용 실수 시 운영 DB 마이그레이션 꼬임

---

### PR 7 — ECS 이전 ⭐ (먼 미래)
**Why**: 운영 안정성 + 확장성. 졸업 발표 전 시간 남으면.

- [ ] ECR 리포지토리 생성
- [ ] ECS 클러스터 + Fargate 태스크 정의
- [ ] ALB 붙이기 (현재 EIP → 도메인 + HTTPS)
- [ ] CD를 ECR push + ECS service update로 전환
- [ ] CloudWatch 로그 통합

**예상 소요**: 1주 | **의존**: PR 1 권장 (compose 통일된 상태가 옮기기 쉬움) | **위험**: 비용 증가, 학부 졸업프로젝트 범위 초과 가능

---

### PR 8 — 프론트엔드 S3 정적 배포 + CORS 통합 🌐 ⭐⭐⭐
**Why**: 졸업 발표 = "프론트가 떴고 백엔드와 통신해서 일기 작성 / 캘린더 확인 가능" 까지 가야 의미 있는 데모. 현재는 Swagger UI 만으로 발표하는데 시각적 인상 약함.

**설계 결정** (이번 PR 의 의사결정 — [의사결정 로그](#-의사결정-로그) 참조):
- 호스팅: **S3 정적 웹사이트만**. CloudFront / 도메인 / HTTPS 생략. 둘 다 HTTP 라 mixed content 문제 없음.
- 프론트 CD: **BE 담당자가 프론트 레포에 직접 GitHub Actions 세팅**. FE 담당자는 코드만. 졸업 데모 분업 패턴.
- 도메인: X — S3 endpoint URL 그대로 사용.

**왜 이 수준에서 멈추는가**:
- CloudFront / HTTPS / 도메인까지 가면 백엔드도 HTTPS 가 되어야 (mixed content 방지) → ALB + ACM + 도메인 작업 연쇄 → 범위가 PR 7 (ECS) 급으로 커짐.
- 둘 다 HTTP 면 일관됨. 졸업 발표 후 운영 안정화 단계에 업그레이드 옵션으로 둔다.

---

**BE 레포(이 레포)의 일** — 사실상 CORS 한 클래스가 전부:
- [ ] `WebMvcConfigurer` 기반 CORS 설정 클래스 — allowed origins:
  - S3 endpoint URL (예: `http://moodiary-frontend.s3-website.ap-northeast-2.amazonaws.com`)
  - 로컬 dev (`http://localhost:3000`, `http://localhost:5173` 등 — FE 빌드 도구에 따라)
- [ ] (선택) `application.yaml` 에 `app.cors.allowed-origins` 외부화 — env var override 가능하게
- [ ] `SecurityFilterChain` 에 `.cors(Customizer.withDefaults())` 활성화
- [ ] (선택) `application.yaml` 의 `springdoc.servers` 에 운영 URL 명시 — Swagger UI 에서 직접 호출 시 운영 서버 선택 가능
- [ ] CORS preflight (OPTIONS) 단위 테스트 — Origin 헤더 시뮬레이션해서 `Access-Control-Allow-Origin` 응답 확인

---

**인프라 셋업** (BE 담당자가 AWS 콘솔 + CLI 로 1회):
- [ ] S3 버킷 생성 — 예: `moodiary-frontend`, region `ap-northeast-2`
- [ ] Static Website Hosting 활성화 → `*.s3-website.ap-northeast-2.amazonaws.com` endpoint URL 받기
- [ ] 버킷 정책 — Public Read (정적 사이트라 의도된 공개. `s3:GetObject` 만 `*` 에 허용)
- [ ] 프론트 레포에 GitHub OIDC role 부착 — S3 sync 권한만 (기존 백엔드 OIDC 패턴 재사용)

---

**프론트 레포 작업** (BE 담당자가 직접 — FE 담당자는 코드만):
- [ ] 프론트 코드의 API base URL env 합의 — 예: `VITE_API_URL=http://15.165.95.129:8080` (Vite 기준). FE 담당자에게 확인 필요.
- [ ] `.github/workflows/frontend-cd.yaml` 작성:
  ```yaml
  # 대략적 흐름
  - npm ci
  - npm run build           # → dist/ 또는 build/ 산출
  - aws configure ... OIDC
  - aws s3 sync ./dist s3://moodiary-frontend/ --delete
  ```
- [ ] 빌드 산출 디렉토리 확인 (Vite=`dist/`, CRA=`build/`, Next.js static export=`out/`)

---

**통합 검증** (인프라 + BE + 프론트 다 완성 후):
- [ ] S3 endpoint URL 접속 → 프론트 UI 로딩
- [ ] 회원가입 → 로그인 → JWT 발급 → localStorage 저장 정상
- [ ] 이후 fetch 에 `Authorization: Bearer <token>` 자동 첨부
- [ ] 일기 CRUD → 본인 글만 조회/수정/삭제 정상 (소유권 격리 확인)
- [ ] 캘린더 조회 → 일자별 postId 매핑 정상 (emoji 는 PR 4 후 후속)
- [ ] CORS preflight (OPTIONS) 가 200 으로 떨어지고 실제 요청 정상

---

**예상 소요**: BE CORS 작업 **0.5일** + 인프라 + 프론트 CD **1-2일** + 통합 검증 **0.5일** = **총 2-3일**

**의존**: 없음 (인증 + Post + 캘린더 다 완성, 프론트 통합만 남음)

**위험**:
- 프론트 레포의 빌드 명령/산출 디렉토리/env 패턴 미합의 — FE 담당자와 사전 확인 필수
- CORS origin 오타로 차단 — 테스트와 통합 검증으로 잡음
- BE 담당자가 프론트 코드 구조 미숙지 시 CD 디버깅 어려움 — FE 담당자와 페어 작업 권장
- S3 버킷 Public Read 설정 시 AWS Block Public Access 기본값 끄는 작업 필요 — 콘솔에서 헷갈리기 쉬움

**운영 머지 전 필수**:
- [ ] CORS allowed origins 환경변수 → EC2 `.env` 갱신 (외부화 옵션 적용 시)
- [ ] S3 버킷 endpoint URL 변경 시 GitHub Secret + EC2 `.env` 동시 갱신

---

## 🤝 FE 협업

### 현재 노출된 API (PR #27 release 이후)

**인증 (Whitelisted — 토큰 없이 호출 가능)**
- `POST /auth/signup` — `{email, password, nickname}` → 201 + UUID
- `POST /auth/login` — `{email, password}` → 200 + `{accessToken, userId}`

**Post (Authorization 헤더 필수)**
- `POST /post` — 작성자 자동 채움
- `GET /post` — **본인 글만** 반환
- `GET /post/{id}` — 본인 글만 (타인 403)
- `PUT /post/{id}` — 본인 글만 수정
- `DELETE /post/{id}` — 본인 글만 삭제

### FE 호출 패턴
```typescript
// 로그인 후 토큰 저장
const { accessToken } = await fetch('/auth/login', { ... }).then(r => r.json());
localStorage.setItem('token', accessToken);

// 이후 모든 호출에 헤더 첨부
fetch('/post', {
  headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
});
```

### 공유 자산
- **OpenAPI spec**: http://15.165.95.129:8080/v3/api-docs
  → FE에서 TypeScript 타입 자동 생성 (`openapi-typescript`), 클라이언트 자동 생성 (`openapi-fetch`), mock 서버(Prism) 등에 사용
- **Swagger UI**: http://15.165.95.129:8080/swagger-ui/index.html
  → "Try it out"으로 직접 호출 가능
- **API 계약 문서**: [`api-contracts.md`](./api-contracts.md)
  → 응답 포맷·예시·예정 API 명세·외부 AI 서버 계약까지

### 배포 (PR 8 머지 후 예정)
- **프론트 운영 URL**: S3 endpoint (예: `http://moodiary-frontend.s3-website.ap-northeast-2.amazonaws.com`) — 머지 후 갱신
- **CD**: 프론트 레포 push → GitHub Actions → `aws s3 sync ./dist s3://moodiary-frontend/ --delete` (FE 담당자는 빌드만 신경, CD 는 BE 담당자가 세팅)
- **API URL 환경변수**: 프론트 빌드 시점에 `VITE_API_URL=http://15.165.95.129:8080` 같은 env 로 박힘. BE URL 바뀌면 프론트 재빌드 필요
- **CORS**: 백엔드가 S3 endpoint + 로컬 dev origins 만 허용. 프론트가 새 도메인 사용 시 BE `application.yaml` 의 `app.cors.allowed-origins` 갱신 필요

### 변경 정책
- **breaking change** 발생 시 FE 분에게 사전 공유 (Discord/Slack)
- 새 엔드포인트는 `@Schema` 어노테이션으로 description 명시
- 응답 포맷 변경 금지 (특히 `{"message":"..."}` 에러 포맷 깨면 FE가 다 깨짐)

---

## 🛠️ 운영 치트시트

자주 쓰는 명령어 모음. EC2 SSH 접속 후 사용.

### 배포/컨테이너
```bash
# 컨테이너 상태
docker ps

# 로그 (실시간)
docker logs -f hoseo-moodiary

# 로그 (최근 100줄)
docker logs --tail=100 hoseo-moodiary

# 수동 재배포 (CD 못 기다릴 때)
docker compose pull && docker compose up -d

# 환경변수 주입 확인 (값 노출 X, 길이만)
docker exec hoseo-moodiary printenv JWT_SECRET | wc -c
```

### DB
```bash
# .env 로드
set -a; source ~/.env; set +a

# RDS 접속 (비번 프롬프트)
mysql -h "$RDS_ENDPOINT" -u "$RDS_USERNAME" -p moodiary

# 또는 환경변수로 비번 자동 (보안 경고 없음)
MYSQL_PWD="$RDS_PASSWORD" mysql -h "$RDS_ENDPOINT" -u "$RDS_USERNAME" moodiary

# 테스트 데이터 정리 (졸업 데모 직전)
MYSQL_PWD="$RDS_PASSWORD" mysql -h "$RDS_ENDPOINT" -u "$RDS_USERNAME" moodiary -e "
  DELETE FROM post WHERE user_id IN (SELECT user_id FROM users WHERE user_email LIKE '%@test.com');
  DELETE FROM users WHERE user_email LIKE '%@test.com';
"
```

### GitHub Actions
- 워크플로우: https://github.com/Goospel/hoseo-moodiary/actions
- CI 트리거: `PR → dev`
- CD 트리거: `push → main` (보통 dev → main PR 머지가 곧 배포)

### 트러블슈팅 가이드
| 증상 | 1차 확인 |
|---|---|
| 8080 응답 없음 | `docker ps` (컨테이너 살아있나) → `docker logs` |
| 500 응답 | `docker logs --tail=200 hoseo-moodiary` → DB 연결 / 스키마 |
| 401인데 이상함 | 토큰 만료 (24h) → 재로그인. JWT_SECRET 변경됐는지도 확인 |
| 403인데 이상함 | 본인 토큰 맞나 + `userId` 일치 확인 |
| RDS 접속 안됨 | RDS Security Group 인바운드 확인 |
| CD 실패 | GitHub Actions 로그 → SSM 단계 / OIDC 자격증명 |

---

## 💡 의사결정 로그

> 왜 이렇게 했는지 기록. 나중에 "왜 이렇게 짰지?" 싶을 때 참고.

| 결정 | 이유 | 시점 |
|---|---|---|
| Spring Boot **4.0** 채택 | 최신 학습, Java 25와 궁합 | 프로젝트 시작 |
| **UUID 기본키** | 분산 환경 가정, 보안상 ID 추측 방지 | 프로젝트 시작 |
| 배포는 **AWS SSM** | SSH 키 관리 부담 제거 | PR #10 (이전) |
| DB는 **RDS 단일 소스** | 인스턴스 stop/start 데이터 보존, 백업 자동화 | 이전 세션 |
| **Elastic IP 부착** | 인스턴스 켜고 끌 때마다 IP 갱신하는 게 비효율 | 이전 세션 |
| AI는 **비동기 + DB 상태** | 큐/브로커 없이 단순 구조로 시작. 졸업프로젝트 범위 | 백로그 PR 4에서 |
| 인증은 **JWT (HS512, 24h, no refresh)** | Stateless REST API에 적합, 졸업 데모 단순성 | PR #23 |
| **JWT 토큰 → Authorization 헤더** | localStorage 저장, FE 단순성 우선 | PR #23 합의 |
| **`@AuthenticationPrincipal UUID`** 패턴 | principal에 UUID 박아서 컨트롤러 시그니처 깔끔 | PR #24 |
| **GET /post 본인 글만** | 일기 도메인에 자연스러움, 캘린더 API와 일관 | PR #24 합의 |
| **자격증명 이중 관리** (GitHub Secrets + EC2 `.env`) | 단순함 우선. 추후 AWS Parameter Store로 단일화 검토 | 이전 세션 |
| **`application.yaml` 추적 + env var 플레이스홀더** | gitignore 했더니 설정이 운영에 누락되는 사고 (T-013) | PR #24 |
| **`ddl-auto: update`** | 스키마 미확정 단계에서 손DDL 부담 제거. 안정화 시 validate 로 | PR #25 |
| **CLAUDE.md에 Workflow rules** | "사용자가 머지한 줄 모르고 작업" 실수 반복 차단 | PR #25 |
| **열거 공격 방지 401** | 이메일 존재 여부 노출 X, 메시지 통일 | PR #23 |
| **이모지는 유니코드 그대로 저장** (😊 같은 실제 문자) | DB-FE 변환 로직 불필요, 가장 단순. `utf8mb4` 필수 | 이전 세션 |
| **하루 여러 글이면 캘린더는 마지막 글의 이모지** | 구현 가장 단순 | 이전 세션 |
| **캘린더 API는 한 달 전체(31일) 반환** | 빈 날도 `emoji: null`. FE 부담 감소 | 이전 세션 |
| 캘린더 응답 emoji = **Post + AiResponse JOIN** | denormalize 필요 시점에 재검토 | 이전 세션 |
| **프론트 호스팅 = S3 정적 웹사이트만** | CloudFront/HTTPS/도메인 가면 BE도 HTTPS 작업 연쇄 → 졸업 데모 범위 초과. 둘 다 HTTP 일관성 | PR 8 |
| **프론트 도메인 X** | S3 endpoint URL 그대로. 월 0원, 데모에 명함 필요 X | PR 8 |
| **프론트 CD = BE 담당자가 직접** | FE 담당자 CI/CD 학습 부담 흡수. 한 번 세팅하면 자동. 졸업 데모 분업 패턴 | PR 8 |

---

## 📝 Changelog

| 일자 | 변경 |
|---|---|
| 2026-05-24 | 이 문서 신설. PR #14/#15/#16 작업 결과 반영. 백로그 의존성 그래프·FE 협업·운영 치트시트·의사결정 로그 섹션 추가 |
| 2026-05-24 | **캘린더 기능 도메인 정보 반영**. PR 4(AI 응답)에 `emoji` 필드 추가. 신규 PR 5 — Calendar API. 이모지 저장/하루 다중 글 처리/응답 포맷 의사결정 로그 기록. Flyway·ECS 번호 한 단계씩 밀림(PR 6, 7). |
| 2026-05-24 | **API 계약 문서 분리** ([`api-contracts.md`](./api-contracts.md) 신설). plan.md의 PR 4/5 상세 spec을 그쪽으로 이동, 링크로 대체. 외부 AI 서버 계약도 동일 문서에서 관리. |
| 2026-05-24 | **인증 라인 완성 + 운영 반영** (release PR #27). PR 0/2-a/2-b/2-c/3 + 인프라 PR 5개 한 번에 main 머지. 백로그에서 PR 0/2/3 제거, PR 4 가 새 핵심 경로 시작점. README.md 신설 연동. |
| 2026-05-25 | **PR 5 Calendar API 진행 중**. QueryDSL 첫 도입 → `build.gradle` querydsl-jpa compileOnly → implementation 으로 전환(CLAUDE.md trap 갱신). emoji 는 PR 4 의존이라 일단 null. `MissingServletRequestParameterException` → 400 매핑 추가. 테스트 13 cases 추가(전체 66 pass / 1 skip). |
| 2026-05-25 | **PR 8 (프론트 S3 배포 + CORS) 신설**. 졸업 데모 가시성 확보를 새 핵심 경로로. S3 only / 도메인 X / 프론트 CD 는 BE 담당자가 직접 세팅. 백로그 그래프 갱신 — PR 8 → PR 4 → (PR 5 후속 emoji JOIN). |

