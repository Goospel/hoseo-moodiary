# 학습 노트 — 작업 중 모르고 물어봐서 배운 것들

> 졸업 프로젝트 진행 중 "이게 뭐지?" 하고 물어봐서 알게 된 기술 개념 / 운영 패턴 정리.
> **목적**: 발표 / Q&A / 면접에서 본인이 직접 설명할 수 있는 수준으로 본인 이해 확립. 같은 질문 두 번 안 묻기.
>
> 📚 **관련 문서**:
> - [`troubleshooting.md`](./troubleshooting.md) — 막힌 지점 + 원인 + 해결 (T-### 인덱스)
> - [`security.md`](./security.md) — 보안 정책 + 약점 정리
> - [`api-contracts.md`](./api-contracts.md) — API 상세 명세

---

## 📑 목차

1. [Refresh Token — 왜 access token 한 개로 부족한가](#1-refresh-token--왜-access-token-한-개로-부족한가)
2. [SHA-256 해시 — DB 에 hash 만 저장하는 이유](#2-sha-256-해시--db-에-hash-만-저장하는-이유)
3. [BCrypt vs SHA-256 — 같은 해시인데 왜 다른 알고리즘?](#3-bcrypt-vs-sha-256--같은-해시인데-왜-다른-알고리즘)
4. [Refresh Token Rotation — 왜 한 번 쓰면 무효화하나](#4-refresh-token-rotation--왜-한-번-쓰면-무효화하나)
5. [JWT vs Refresh Token 의 본질적 차이 — stateless vs stateful](#5-jwt-vs-refresh-token-의-본질적-차이--stateless-vs-stateful)
6. [`dev` 와 `main` 의 의미 — 왜 dev 머지로는 운영 반영 안 되나](#6-dev-와-main-의-의미--왜-dev-머지로는-운영-반영-안-되나)
7. [AWS SSM 자동화 — CD 성공 후 EC2 에서 수동 docker pull 필요한가?](#7-aws-ssm-자동화--cd-성공-후-ec2-에서-수동-docker-pull-필요한가)
8. [`ddl-auto: update` 의 한계 — 운영 머지 후 UNIQUE 인덱스 사후 검증이 필요한 이유](#8-ddl-auto-update-의-한계--운영-머지-후-unique-인덱스-사후-검증이-필요한-이유)
9. [`utf8mb4` 가 왜 중요한가 — 이모지 INSERT 폭발 시나리오](#9-utf8mb4-가-왜-중요한가--이모지-insert-폭발-시나리오)

---

## 1. Refresh Token — 왜 access token 한 개로 부족한가

### 한 줄 요약
> 토큰 수명은 **보안과 UX 가 정반대 방향**으로 끌어당기는 trade-off. 한 토큰으로는 양쪽을 다 잡을 수 없어서, 자주 쓰이는 짧은 토큰 (access) + 거의 안 쓰이는 긴 토큰 (refresh) 두 개로 쪼갠다.

### 문제 — 한 토큰의 어정쩡한 절충

| 토큰 수명 | 보안 | UX |
|---|---|---|
| 15분 | 좋음 (탈취돼도 15분만 위험) | 나쁨 (자주 재로그인) |
| **24h** (우리의 옛 상태) | **나쁨** (탈취 시 24h 무방비) | **그저 그럼** (하루마다 재로그인) |
| 7일 | 매우 나쁨 (일주일 무방비) | 좋음 |

→ 어느 수명을 잡아도 **한 측면은 희생**. 24h 는 양쪽 다 어정쩡함.

### 해법 — 두 토큰으로 쪼개기

```
로그인 시:
  Access Token  (1시간, 매 요청에 첨부)
  Refresh Token (2주, access 갱신 전용 — 거의 안 쓰임)

[1시간 후 access 만료]
  클라이언트가 자동으로 refresh 호출 → 새 access 받음
  → 사용자는 갱신 사실도 모르고 2주간 로그인 유지

[2주 후 refresh 만료]
  → 그 때만 재로그인
```

### 양쪽이 동시에 잡히는 이유

- **보안**: Access 가 1시간 — 탈취돼도 최대 1시간만 위험
- **UX**: Refresh 가 2주 — 사용자는 백그라운드 자동 갱신만 일어남, 재로그인 부담 X

**핵심 원리**: *자주 쓰이는 건 짧게, 거의 안 쓰이는 건 길게*. Access 는 모든 API 호출에 첨부되어 노출 위험이 크니까 수명 짧음, Refresh 는 1시간에 한 번만 호출되어 노출 면이 작으니까 길게.

### 발표 시 한 줄 비유
> "24시간짜리 출입증 한 장 대신, 1시간짜리 출입증 + 2주짜리 갱신권 두 장. 출입증을 훔쳐도 1시간 후 자동 만료, 갱신권은 한 번 쓰면 새 걸로 교체."

### 코드 위치
- `JwtTokenProvider.java` — access token 발급 (HS256, 1h)
- `RefreshTokenService.java` — refresh 발급 / rotation / 무효화
- `application.yaml` — `JWT_ACCESS_EXPIRATION_MS` / `JWT_REFRESH_EXPIRATION_MS`

---

## 2. SHA-256 해시 — DB 에 hash 만 저장하는 이유

### 한 줄 요약
> Refresh token 의 raw 값을 DB 에 저장하면 **DB 가 유출됐을 때 모든 사용자 세션이 즉시 탈취된다**. SHA-256 해시만 저장하면 DB 가 털려도 raw 를 복원할 수 없어 안전.

### SHA-256 이 뭔가

**한 방향 해시 함수** — 임의 길이 입력을 고정 64자 hex 로 변환.

```
SHA-256("hello") → "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
SHA-256("hellp") → "8b2c86ea4c01f12b29c6fafa39d3edc1a1ec3df00b6cf6e1a9b1456b3a4e6c6c"
                  └─ 한 글자만 달라도 완전히 다른 출력 (avalanche effect)
```

3가지 핵심 특성:
- **Deterministic** — 같은 입력 → 항상 같은 출력 (검증 가능)
- **Avalanche** — 입력 1비트 변경 → 출력 절반 비트 변경 (예측 불가)
- **One-way** — 출력에서 입력 복원 수학적으로 불가능

### 우리가 저장하는 방식

```
서버: refresh token 발급
  raw = "Xy7-aBcDeF..." (32바이트 secure random, URL-safe base64)
        │
        ├─→ 클라이언트에게 raw 그대로 응답 (단 한 번)
        │
        └─→ DB 에 SHA-256(raw) 저장 = "a3f5c8...(64자 hex)"
```

### 왜 raw 가 아닌 hash?

**시나리오 — DB 가 유출**:

| 저장 방식 | DB 유출 시 결과 |
|---|---|
| raw token | 공격자가 모든 사용자의 raw token 즉시 사용 가능 → 전체 세션 탈취 |
| **SHA-256 hash** | 공격자가 hash 만 봄 → raw 복원 불가 → 세션 사용 불가 |

### 검증 시 어떻게 비교하나?

```
1. 클라이언트가 raw "Xy7-aBcDeF..." 를 헤더로 전송
2. 서버가 SHA-256 한 번 계산 → "a3f5c8..."
3. DB 의 hash "a3f5c8..." 와 비교 → 일치 → 유효!
```

서버는 매번 해시 한 번만 더 계산. raw 는 절대 DB 에 안 남음.

### 비밀번호도 같은 원리

같은 패턴이 비밀번호에도 적용됨 — DB 에 평문 비밀번호 저장 X, **BCrypt 해시만 저장**. 다만 알고리즘은 다름 (다음 섹션).

### 코드 위치
- `RefreshTokenService.java` — `hashToken(rawToken)` 메서드 (SHA-256 hex 변환)
- `RefreshToken.java` — `refresh_token_hash` 컬럼 (DB 저장은 hex 만)

---

## 3. BCrypt vs SHA-256 — 같은 해시인데 왜 다른 알고리즘?

### 한 줄 요약
> 비밀번호는 사용자가 만든 **약한 입력** — 의도적으로 느린 BCrypt 로 brute force 차단. Refresh token 은 서버가 만든 **강한 입력 (256-bit random)** — brute force 불가능하니 빠른 SHA-256 으로 충분.

### 차이 표

| 비교 항목 | 비밀번호 (BCrypt) | Refresh Token (SHA-256) |
|---|---|---|
| **누가 만드나** | 사용자 직접 | 서버 |
| **입력 강도** | 약함 (`abcd1234`, `password!`, 사전 단어) | 매우 강함 (32바이트 secure random, 2^256 가지) |
| **Brute force 위험** | 큼 (사전 공격 / rainbow table) | 사실상 0 (우주 종말까지 못 풀음) |
| **알고리즘 속도** | **느림 (cost 10 ≈ 0.1초)** — 의도적 | **빠름 (μs 단위)** |
| **느림의 효과** | 공격자가 1초에 수십억 번 시도 못 함 | 의미 없음 — 입력 자체가 이미 강해서 |

### 핵심 원리

> *"약한 입력에는 느린 해시로 시간을 늘리고, 강한 입력에는 빠른 해시로 성능을 챙긴다."*

### 추가 — BCrypt 의 cost factor

```java
new BCryptPasswordEncoder(10);  // cost = 10
```

- cost 10 = 2^10 = 1024번 내부 반복 → 검증 1회에 약 0.1초
- 공격자가 1초당 10번밖에 시도 못 함 → 1억 개 비밀번호 사전 공격에 약 116일 필요
- cost 를 12로 올리면 4배 느려짐 → 검증 0.4초 / 공격 비용 4배

### 추가 — 왜 MD5 / SHA-1 이 아닌 SHA-256?

| 알고리즘 | 출력 크기 | 충돌 공격 (collision) |
|---|---|---|
| MD5 | 128-bit | 2004년 발견 — deprecated |
| SHA-1 | 160-bit | 2017년 발견 (SHAttered) — deprecated |
| **SHA-256** | **256-bit** | **현재까지 없음 — 표준** |

→ 현재 보안 표준은 SHA-2 family (SHA-256 / SHA-512). SHA-3 도 있지만 호환성과 성숙도로 SHA-256 이 사실상 표준.

---

## 4. Refresh Token Rotation — 왜 한 번 쓰면 무효화하나

### 한 줄 요약
> Rotation 없이 refresh 가 2주간 계속 유효하면 **한 번 탈취된 refresh 로 2주 동안 무한 access 갱신 가능**. Rotation = "한 번 쓰면 즉시 새 걸로 교체" → 훔친 refresh 는 단 한 번만 유효.

### Rotation 없는 시나리오 (취약)

```
1. 사용자 로그인 → refresh A 발급 (2주 유효)
2. 공격자가 refresh A 탈취 (XSS / 패킷 캡처 / DB 유출 등)
3. 공격자가 refresh A 로 access 계속 갱신 → 2주간 무한 접근
   사용자는 자기 토큰이 도둑맞은 줄도 모름
```

### Rotation 있는 시나리오 (강함)

```
1. 사용자 로그인 → refresh A 발급
2. 공격자가 refresh A 탈취
3. 사용자가 1시간 후 access 갱신 시도 → refresh A 로 호출
   서버: "refresh A revoke + refresh B 발급" ← Rotation!
4. 공격자가 refresh A 로 갱신 시도 → "이미 revoke" → 401 거절
```

### 효과
- 훔친 refresh 는 **단 1회만 유효**
- 정당한 사용자가 먼저 쓰면 훔친 건 즉시 폐기
- **추가 보너스 — 보안 모니터링**: 사용자가 자기가 갱신 안 했는데 refresh 가 무효 응답을 받으면 = "누가 내 토큰을 먼저 썼다" 신호. 전체 계정 강제 로그아웃 / 알림 가능 (선택적 후속).

### Race Condition 우려

> "사용자가 동시에 두 번 refresh 호출하면?" (예: 모바일 + 태블릿 같이)

→ DB 트랜잭션의 비관 락으로 한 쪽이 먼저 처리됨. 나머지는 "이미 revoke 됨" 응답을 받고 클라이언트가 재로그인 흐름으로 fallback. 실 운영에서는 short grace window (예: 5초) 를 두는 패턴도 있음 — 우리는 단순 락 방식.

### 코드 위치
- `RefreshTokenService.rotate(rawToken, userId)` — 트랜잭션 안에서 기존 revoke + 새 발급

---

## 5. JWT vs Refresh Token 의 본질적 차이 — stateless vs stateful

### 한 줄 요약
> JWT (access) 는 서명만 검증하면 되는 **stateless** — DB 조회 0, 빠름. 대신 한 번 발급되면 서버에서 못 막음 (만료 대기). Refresh 는 DB 에 저장된 **stateful** — 검증 시 DB 조회 필요하지만 서버에서 즉시 무효화 가능. 둘을 합치면 **속도와 통제력의 균형**.

### 비교 표

| 항목 | JWT Access | Refresh Token |
|---|---|---|
| 상태 | **Stateless** (서버에 저장 X) | **Stateful** (DB 에 저장) |
| 검증 방법 | 서명 검증 (`HMAC-SHA256`) | DB 조회 후 hash 비교 |
| 검증 속도 | μs 단위 (DB 부담 0) | ms 단위 (DB 한 번 SELECT) |
| 발급 후 서버 통제 | ❌ 불가능 — 만료 대기만 | ✅ 가능 — `revoked_at` 컬럼 update 로 즉시 무효 |
| 수명 권장 | 짧게 (1h) | 길게 (2w) |
| 사용 빈도 | 매 API 요청 | 1시간에 한 번 |

### 왜 둘 다 필요한가

```
Access 만 — 빠르지만 서버 통제력 없음 → 탈취 시 만료 대기 (위험)
Refresh 만 — 매 요청마다 DB 조회 (부담) → 성능 폭망

둘 다 — 매 요청은 stateless 검증 (빠름)
       + 1시간에 한 번만 stateful 갱신 (DB 부담 최소)
       + 서버가 refresh 무효화 가능 (로그아웃 / 침해 대응)
```

### 우리 시스템에 적용된 모습

- **GET /post 같은 일반 요청**: JWT access 만 검증. DB 조회 0 — 빠름
- **POST /auth/refresh**: refresh DB 조회 + rotation — 1시간에 한 번만 발생
- **POST /auth/logout**: refresh `revoked_at` UPDATE — 다음 refresh 시도부터 차단
- **계정 침해 대응**: 향후 "이 사용자의 모든 refresh revoke" 같은 강제 로그아웃도 가능

---

## 6. `dev` 와 `main` 의 의미 — 왜 dev 머지로는 운영 반영 안 되나

### 한 줄 요약
> `dev` 머지는 **CI (테스트 + 빌드 검증)** 만 트리거. **운영 배포 (CD)** 는 `main` push 에서만 트리거. dev → main 의 release PR 머지를 따로 해야 운영 EC2 에 반영됨.

### 우리 CI/CD 흐름

```
PR → dev     →  CI 실행 (./gradlew build, 테스트)
                "코드가 빌드되는가 + 테스트가 깨지지 않는가" 만 확인
                운영에는 아무 영향 X

PR (release) → main  →  CD 실행
                        ├─ Docker Hub 에 새 이미지 push
                        ├─ AWS SSM 으로 EC2 에 명령
                        └─ docker-compose pull && up -d → 컨테이너 교체
                        ✅ 운영 반영 완료
```

### 왜 두 단계로 나눴나

- **dev**: 통합 브랜치. 여러 feature PR 이 머지되어 누적. 실험적/위험한 변경도 일단 dev 까지는 OK.
- **main**: 운영 브랜치. main 의 모든 commit 은 실제로 운영 서버에 돌고 있어야 함.

**release PR (dev → main)** 의 의미: "dev 에 쌓인 여러 PR 을 묶어서 한 번에 운영에 보낼 시점 결정". 운영 머지는 신중해야 하니까 사용자가 명시적으로 타이밍을 결정.

### 실수 사례 (이번 세션에서 본인이 만난 것)

```
"ai_response 테이블이 RDS 에 없는데?"
→ PR #59 (PR 4-pre AiResponse 엔티티) 는 dev 에만 머지된 상태였음
→ 운영 EC2 컨테이너는 PR #54 시점 코드 → AiResponse 엔티티 자체 없음
→ ddl-auto: update 가 만들 코드 자체가 없으니 테이블 자연스럽게 부재
→ release PR (#62) 만들어서 main 에 머지 후, CD 가 새 이미지 배포 후 자동 생성됨
```

**교훈**: feature PR 머지 = 실험 끝. 운영 반영 = 별도의 release PR 머지. **dev 머지를 운영 반영으로 착각하면 "왜 운영에서 안 보이지?" 사고 발생**.

### 코드 위치
- `.github/workflows/moodiary-be-ci.yaml` — PR → dev 트리거
- `.github/workflows/moodiary-be-cd.yaml` — push → main 트리거 (Docker Hub + SSM)
- [README "배포" 섹션](../README.md#-인프라--배포) — 전체 흐름 다이어그램

---

## 7. AWS SSM 자동화 — CD 성공 후 EC2 에서 수동 docker pull 필요한가?

### 한 줄 요약
> **불필요**. CD 워크플로우가 SSM 으로 EC2 안에서 `docker-compose pull` + `docker-compose up -d` + health check 까지 자동 실행. CD 초록불 = EC2 컨테이너가 새 이미지로 재시작 완료 + swagger 200 응답 확인까지 완료된 상태.

### CD 가 자동으로 하는 일 — 5단계

```
1. GitHub Actions OIDC 로 AWS 자격증명 획득
2. Docker Hub 에 새 이미지 push (latest + :SHA 두 태그)
3. AWS SSM send-command 로 EC2 에 명령 발사:
     "cd ~/moodiary && docker-compose pull && docker-compose up -d"
4. SSM wait command-executed — 명령 실행 완료까지 대기
5. health check — http://localhost:8080/v3/api-docs 가 200 응답할 때까지 polling
```

여기까지 **모두 자동**. 사용자가 EC2 SSH 로 들어가서 수동으로 할 일 없음.

### 왜 처음에 헷갈렸나 (이전 silent fail 의 역사)

PR #45 이전의 CD 는 **wait + health check 가 없었음**. 그 때는:
- SSM 이 명령을 enqueue 만 하고 결과 안 보고 "성공" 반환 → silent fail
- EC2 의 docker-compose plugin 미설치 / 이미지 캐시 등으로 실제 컨테이너 교체 실패 가능
- CD 초록불이지만 운영은 옛 이미지 — 사용자가 SSH 로 수동 pull 해야 했던 시기 ([T-019, T-023](./troubleshooting.md))

지금은 wait + health check 가 있어서 **CD 성공 = 진짜 deploy 성공**.

### 의심될 때 확인하는 방법

```bash
# EC2 SSH 에서
docker ps                                              # 컨테이너 살아있는지
docker inspect hoseo-moodiary --format '{{.Created}}'  # 컨테이너 생성 시각
```

`.Created` 시각이 마지막 main 머지 시각 이후면 ✅ 새 이미지로 떠 있는 것. 옛 시각이면 CD 가 뭔가 실패한 거.

### 코드 위치
- `.github/workflows/moodiary-be-cd.yaml` — SSM send-command + wait + health check 모두
- `compose.yaml` — `pull_policy: always` (캐시 무시하고 매번 pull)

---

## 8. `ddl-auto: update` 의 한계 — 운영 머지 후 UNIQUE 인덱스 사후 검증이 필요한 이유

### 한 줄 요약
> Hibernate 의 `ddl-auto: update` 는 새 테이블 / 새 컬럼은 자동 추가하지만 **인덱스 / UNIQUE 제약은 추가를 보장하지 않는다**. 운영 머지 직후 `SHOW INDEX` 로 사후 검증 필수. 누락 시 수동 DDL 적용.

### Hibernate ddl-auto 모드 비교

| 모드 | 동작 |
|---|---|
| `create` | 부팅 시 모든 테이블 DROP → 새로 CREATE. 데이터 다 날아감 |
| `create-drop` | 부팅 시 CREATE, 종료 시 DROP. 테스트용 |
| `update` ← **우리 현재** | 누락 컬럼/테이블 추가. 데이터 보존. **그러나 인덱스/UNIQUE 보장 X** |
| `validate` | 스키마와 엔티티 비교만. 다르면 부팅 실패. 변경 X |
| `none` | 아무것도 안 함 |

### 왜 update 모드를 쓰는가

졸업프로젝트 초기엔 스키마가 자주 바뀌어서 수동 DDL 작성 부담이 큼. update 모드면 새 엔티티 필드 추가 시 Hibernate 가 부팅 시 자동으로 `ALTER TABLE ADD COLUMN` 해줌. 데이터도 보존.

### update 의 한계

```java
@Table(uniqueConstraints = @UniqueConstraint(
    name = "uk_ai_response_post_id",
    columnNames = "post_id"
))
```

이 코드를 새 엔티티에 추가하고 부팅해도 — **운영 DB 에 UNIQUE 인덱스가 안 들어갈 수 있음**. ddl-auto: update 는 컬럼 추가는 보장하지만 제약 변경은 환경에 따라 다름.

### 운영 머지 후 사후 검증 흐름

```bash
# 1. 테이블 자동 생성 확인
mysql -e "SHOW CREATE TABLE ai_response\G"
mysql -e "SHOW CREATE TABLE refresh_token\G"

# 2. 인덱스 / UNIQUE 확인
mysql -e "SHOW INDEX FROM ai_response;"
mysql -e "SHOW INDEX FROM refresh_token;"

# 3. UNIQUE 가 빠져 있으면 (Non_unique=1 이거나 행 자체가 없음) 수동 적용
CREATE UNIQUE INDEX uk_ai_response_post_id ON ai_response (post_id);
CREATE UNIQUE INDEX uk_refresh_token_hash ON refresh_token (refresh_token_hash);
```

### 영구 해소 — PR 6 Flyway

`ddl-auto: validate` 로 전환하고 Flyway 로 DDL 을 명시적 SQL 파일 (`V1__baseline.sql`) 로 관리. 그 때부터는 모든 인덱스 / UNIQUE 가 베이스라인에 박혀 있어 부팅 시 자동 검증.

### 발표 시 한 줄
> "스키마 미확정 단계라 ddl-auto: update 로 자동 마이그레이션을 받고 있는데, 인덱스 보장이 안 돼서 운영 머지 직후 SHOW INDEX 로 사후 검증을 합니다. Flyway 도입 시 영구 해소됩니다."

---

## 9. `utf8mb4` 가 왜 중요한가 — 이모지 INSERT 폭발 시나리오

### 한 줄 요약
> MySQL 의 `utf8` (= `utf8mb3`) 는 최대 3바이트만 저장. 이모지 (😊) 는 4바이트라 INSERT 시 폭발. **이모지 저장 의도가 있으면 DB 와 모든 테이블이 `utf8mb4` 여야 함**.

### MySQL 의 charset 함정

| 이름 | 실제 의미 | 이모지 가능? |
|---|---|---|
| `utf8` (옛) | 최대 3바이트 = `utf8mb3` 의 alias | ❌ |
| `utf8mb3` | 최대 3바이트 | ❌ |
| `utf8mb4` | **최대 4바이트 (진짜 UTF-8)** | ✅ |

→ MySQL 의 `utf8` 은 **표준 UTF-8 의 부분집합** (3바이트 BMP 만). 진짜 UTF-8 = `utf8mb4`.

### 우리 도메인에서 이모지가 중요한 이유

- AI 추론 서버가 일기 분석 후 **그 날의 기분 이모지** (😊 / 😢 / 🥲 등) 반환
- 캘린더 화면에 이모지가 매핑되어 표시
- DB 에 그대로 저장 (변환 로직 X — `claude-docs/plan.md` 의사결정 로그)

만약 운영 RDS 가 `utf8mb3` 면:
```
INSERT INTO ai_response (ai_response_emoji) VALUES ('😊');
→ ERROR 1366: Incorrect string value: '\xF0\x9F\x98\x8A' for column 'ai_response_emoji'
```

→ AI 응답 저장 전체 실패. 사용자에게는 "AI 응답이 안 와요" 처럼 보임.

### 사전 검증 (PR 4-pre 운영 머지 전 실제로 한 일)

```bash
mysql -e "
  SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME
  FROM information_schema.SCHEMATA
  WHERE SCHEMA_NAME='moodiary';

  SELECT TABLE_NAME, TABLE_COLLATION
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA='moodiary';
"
```

→ `utf8mb4` + `utf8mb4_*` collation 이면 OK.

### 운영 적용 (만약 utf8mb3 였다면)

```sql
ALTER DATABASE moodiary CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE post CONVERT TO CHARACTER SET utf8mb4;
ALTER TABLE ai_response CONVERT TO CHARACTER SET utf8mb4;
-- ... 모든 테이블 반복
```

이걸 PR 4-pre 머지 전에 하지 않았다면 PR 4-final 의 실제 AI 응답이 들어올 때 폭발.

### 발표 시 한 줄
> "이모지 저장이 핵심 기능이라 DB 가 utf8mb4 여야 하는데, MySQL 의 utf8 은 사실 3바이트만 저장하는 함정이라 4바이트인 이모지가 폭발합니다. 운영 머지 전 SCHEMATA 와 TABLES 의 collation 둘 다 확인했습니다."

---

## 🔄 누적 갱신

이 문서는 **새로운 질문 / 학습이 생길 때마다 추가**된다. 본인이 모르는 걸 묻고 알게 된 모든 기술 개념을 한 곳에 누적.

### 갱신 이력

| 일자 | 추가 항목 |
|---|---|
| 2026-05-27 | 초안 — Refresh Token (1, 4, 5) + SHA-256 (2, 3) + dev/main 흐름 (6) + SSM 자동화 (7) + ddl-auto 한계 (8) + utf8mb4 (9). PR 10 / PR 4-pre 운영 반영 직후 본인이 발표 / Q&A 준비하며 정리. |
