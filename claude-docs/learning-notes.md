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
10. [Spring 비동기 (`@EnableAsync` + `@Async`) — 동기 블로킹 회피 + DB 상태머신 + race 방지](#10-spring-비동기-enableasync--async--동기-블로킹-회피--db-상태머신--race-방지)
11. [AWS SSM Run Command — outbound polling 구조 + IAM role / 0 인바운드 / send-command 한계](#11-aws-ssm-run-command--outbound-polling-구조--iam-role--0-인바운드--send-command-한계)
12. [CORS — Same-Origin Policy + 브라우저 차단 메커니즘 + Preflight + allowlist vs 와일드카드](#12-cors--same-origin-policy--브라우저-차단-메커니즘--preflight--allowlist-vs-와일드카드)
13. [OAuth2 — Token-Exchange 패턴 + audience 검증 (confused deputy) + email_verified 위임의 한계](#13-oauth2--token-exchange-패턴--audience-검증-confused-deputy--email_verified-위임의-한계)
14. [Silent catch 의 두 얼굴 — 의도적 침묵 (high-volume) vs 운영 사고 (저-volume) + 메트릭 채널](#14-silent-catch-의-두-얼굴--의도적-침묵-high-volume-vs-운영-사고-저-volume--메트릭-채널)
15. [Mixed Content — HTTPS 페이지의 HTTP API 호출은 브라우저가 *요청 전에* 차단 (Vercel rewrites 가 빠른 해결책)](#15-mixed-content--https-페이지의-http-api-호출은-브라우저가-요청-전에-차단-vercel-rewrites-가-빠른-해결책)
16. [JPA `String` 컬럼의 기본 `VARCHAR(255)` 함정 + `ddl-auto:update` 는 기존 컬럼을 안 넓힌다](#16-jpa-string-컬럼의-기본-varchar255-함정--ddl-autoupdate-는-기존-컬럼을-안-넓힌다)
17. [403 "Invalid CORS request" 의 정체 — 서버가 던지는 CORS 거부 + origin 정확 문자열 매칭 + env 외부화 함정](#17-403-invalid-cors-request-의-정체--서버가-던지는-cors-거부--origin-정확-문자열-매칭--env-외부화-함정)
18. [데이터 기밀성 = 암호화 계층 선택 — 전송 TLS / 저장 TDE / 앱 ALE, 그리고 ALE 만 검색 UX 와 트레이드오프](#18-데이터-기밀성--암호화-계층-선택--전송-tls--저장-tde--앱-ale-그리고-ale-만-검색-ux-와-트레이드오프)
19. [JPA 더티 체킹 — `save()` 를 안 부르는데 UPDATE 가 나가는 이유 (영속성 컨텍스트)](#19-jpa-더티-체킹--save-를-안-부르는데-update-가-나가는-이유-영속성-컨텍스트)
20. [N+1 문제와 `FetchType.LAZY` — 그리고 FK 만 읽어 소유권 검사를 공짜로 만드는 트릭](#20-n1-문제와-fetchtypelazy--그리고-fk-만-읽어-소유권-검사를-공짜로-만드는-트릭)
21. [조회 성능 — 복합 인덱스 컬럼 순서 + 안정 정렬(tiebreaker)](#21-조회-성능--복합-인덱스-컬럼-순서--안정-정렬tiebreaker)
22. [UUID PK vs Auto-increment — ID 추측 방지의 대가는 인덱스 단편화](#22-uuid-pk-vs-auto-increment--id-추측-방지의-대가는-인덱스-단편화)
23. [JPA 연관관계 매핑 — 1:1 단방향 + `@Enumerated(STRING)` 의 함정](#23-jpa-연관관계-매핑--11-단방향--enumeratedstring-의-함정)

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

## 10. Spring 비동기 (`@EnableAsync` + `@Async`) — 동기 블로킹 회피 + DB 상태머신 + race 방지

> 🚀 **공개 일반화판**: [Spring 비동기 패턴 — goospel.github.io](https://goospel.github.io/notes/backend/spring-async-pattern/) — Moodiary 맥락 제거된 일반 패턴 노트. 면접 / 외부 공유 시 이쪽 링크 사용.

### 한 줄 요약
> `@EnableAsync` 는 스위치, `@Async` 는 표시 — 그 메서드는 별도 스레드 풀에서 실행되어 호출자를 블로킹하지 않는다. AI 서버 호출이 5~30초 걸리는데 사용자가 그동안 기다릴 수 없으니, 일기 저장만 즉시 끝내고 AI 호출은 백그라운드 + DB 의 `PENDING / DONE / FAILED` 상태로 결과 전달 + 클라이언트가 폴링으로 받기 — 큐 없는 단순 구조.

### 동기 vs 비동기 — 개념

| 모드 | 흐름 |
|---|---|
| **동기 (Synchronous)** | 호출자 → 함수 실행 끝까지 **블로킹** → 결과 받음 → 다음 줄 진행 |
| **비동기 (Asynchronous)** | 호출자 → 함수 실행 **요청만 던지고 즉시 리턴** → 다음 줄 진행. 결과는 별도 채널로 받음 |

음식점 비유:
- **동기** — 카운터에서 주문하고 음식 나올 때까지 카운터 앞에서 기다림. 그동안 다른 손님 못 받음.
- **비동기** — 주문 번호 받고 자리로 감. 음식 준비되면 진동벨이 울림. 그 사이 카운터는 다음 손님 받음.

→ **핵심 차이**: 비동기는 호출자의 시간을 점유하지 않는다.

### Moodiary 가 비동기를 필요로 한 이유

동기로 구현했을 때의 문제:

```
POST /post (일기 작성)
  ├─ 일기 DB 저장 (50ms)
  ├─ AI 서버 호출 — 응답 생성 + 이모지 분석 (5초 ~ 30초)  ★ 병목
  └─ 클라이언트에 201 응답
```

- 사용자가 5~30초 기다림 — UX 최악
- HTTP timeout 위험 (nginx / ALB / 클라이언트 측 30~60s 초과 가능)
- Tomcat worker thread 가 30초 동안 한 요청에 묶임 → 동시 처리량 ↓
- AI 서버가 죽으면 일기 작성 자체가 실패 — 결합도 ↑

비동기로 바꾼 후:

```
POST /post (일기 작성)
  ├─ 일기 DB 저장 (50ms)
  ├─ AiResponse(status=PENDING) row 같은 트랜잭션에 저장 (10ms)
  ├─ aiResponseService.triggerAsync(postId)   ← 비동기 — 즉시 리턴
  └─ 클라이언트에 201 응답 (총 60ms)

[별도 스레드 (백그라운드)]
  ├─ AI 서버 호출 (5~30초)
  ├─ 결과를 DB 의 PENDING row 에 UPDATE (DONE 또는 FAILED)

[클라이언트]
  GET /post/{id}/ai-response   ← 폴링 (1초마다 호출)
  └─ status 가 PENDING → DONE 으로 바뀐 순간 결과 표시
```

### `@EnableAsync` + `@Async` — Spring 이 실제로 하는 일

`AsyncConfig.java`:

```java
@Configuration
@EnableAsync
public class AsyncConfig {
}
```

`@EnableAsync` 는 **Spring 에게 "이 애플리케이션에서 `@Async` 어노테이션을 인식해라"** 라고 켜는 스위치. 이게 없으면 `@Async` 가 붙어 있어도 무시되고 그냥 동기로 실행된다.

내부적으로 Spring 이 하는 일:
1. 시작 시 `@Async` 가 붙은 메서드를 가진 빈을 스캔
2. 그 빈을 **AOP 프록시 (proxy)** 로 감싼다
3. 프록시는 메서드 호출을 가로채서 **별도 스레드 풀에 실행을 위임**하고 즉시 리턴

`AiResponseService.java`:

```java
@Async
public void triggerAsync(UUID postId) {
    AiResponse aiResponse = aiResponseRepository.findByPost_Id(postId)...;
    try {
        AiInferenceResult result = client.invoke(...);
        aiResponse.markDone(result.content(), result.emoji());
    } catch (AiInferenceException e) {
        aiResponse.markFailed(e.getMessage());
    }
}
```

`PostController` 가 이 메서드를 호출하면:
- **컨트롤러 스레드**: 메서드 호출만 던지고 즉시 다음 줄로 진행 → 201 응답
- **별도 스레드**: 위 메서드 본문을 실행 (5~30초)

### `@Async void` vs `CompletableFuture<T>`

| 리턴 타입 | 의미 |
|---|---|
| `void` | Fire-and-forget. 호출자가 결과 / 예외 받을 방법 없음 |
| `CompletableFuture<T>` | 나중에 `.get()` 으로 결과 / 예외 받기 가능 |

Moodiary 는 `void` — 호출자 (컨트롤러) 는 결과를 신경 쓸 필요 없고, 결과는 DB row 의 `status` 로만 표현된다.

### `ThreadPoolTaskExecutor` — 스레드 풀

`@Async` 가 호출될 때마다 새 스레드를 만들면 비용 + 메모리 폭발 → **풀에서 재사용**. `ThreadPoolTaskExecutor` 가 Spring 의 기본 도구.

**현재 코드에는 명시 안 됨** — PR 4-pre 단계는 Spring 기본 executor 에 맡김. `AsyncConfig.java` 의 주석:

> PR 4-pre 단계에선 executor 빈을 별도 정의하지 않고 Spring 기본값에 맡긴다 — 졸업 데모 트래픽 기준 충분. 실 운영 부하 측정 후 (PR 4-final 머지 이후) `ThreadPoolTaskExecutor` 외부화 검토.

명시할 때의 모양:

```java
@Bean(name = "aiExecutor")
public ThreadPoolTaskExecutor aiExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);        // 항상 살아있는 스레드 5개
    executor.setMaxPoolSize(20);        // 부하 시 최대 20개
    executor.setQueueCapacity(100);     // 대기 큐 100개
    executor.setThreadNamePrefix("ai-");
    executor.initialize();
    return executor;
}
```

그 후 `@Async("aiExecutor")` 로 명시적 풀 지정 → 도메인별 풀 분리로 AI 호출 폭주가 다른 비동기 작업에 영향 X.

### 큐 / 메시지브로커를 안 쓴 이유 — DB 상태 머신으로 단순화

보통 큰 규모는 `Kafka / RabbitMQ / SQS` 를 도입하지만, 졸업 프로젝트 범위에서는 인프라 부담이 큼.

`AiResponseStatus.java`:

```java
public enum AiResponseStatus {
    PENDING,  // 일기 저장 직후 — AI 처리 중
    DONE,     // 성공
    FAILED    // 실패
}
```

→ **DB row 의 `status` 컬럼이 큐 역할**. 단순한 상태 머신.

Trade-off:

| 항목 | 큐 방식 | DB 상태 머신 |
|---|---|---|
| 인프라 | Kafka + Worker | DB 만 |
| 재시도 | 큐 자체 기능 | 수동 구현 필요 |
| 확장성 | Worker 수평 확장 무한 | 동일 인스턴스 내 스레드 풀 |
| 적합 규모 | 대형 서비스 | **졸업 프로젝트 / 소규모** |

### Race Condition 방지 — commit 후 trigger

**잘못된 예** (트랜잭션 안에서 호출):

```java
@Transactional
public UUID create(...) {
    Post post = postRepository.save(...);
    aiResponseRepository.save(new AiResponse(PENDING));
    aiResponseService.triggerAsync(post.getId());  // ★ 트랜잭션 안에서 호출
    return post.getId();
}
```

위 코드는 다음 race 를 만든다:
1. 트랜잭션이 PENDING row 를 **메모리에는 만들었지만 DB commit 전**
2. `triggerAsync` 가 별도 트랜잭션에서 즉시 시작
3. 별도 트랜잭션이 PENDING row 를 SELECT — **아직 commit 안 됨 → 안 보임**
4. `AiResponseNotFoundException` 발생

**Moodiary 의 해결** — 컨트롤러에서 호출 (`PostController.java`):

```java
@PostMapping("/post")
public ResponseEntity<UUID> post(...) {
    UUID postId = service.create(userId, requestDto);
    // create() 의 @Transactional 가 메서드 return 시점에 commit. 이 줄에서 호출하면 새 Async
    // 트랜잭션이 PENDING row 를 안전하게 select 가능 (race 없음).
    aiResponseService.triggerAsync(postId);
    return ResponseEntity.status(HttpStatus.CREATED).body(postId);
}
```

`service.create()` 가 리턴할 때 트랜잭션이 commit 된다. 그 다음 줄에서 `triggerAsync` 호출 → 비동기 스레드가 select 할 때 PENDING row 가 이미 DB 에 있음.

→ PR 4-pre 의 **가장 중요한 설계 결정 중 하나**. 슬라이드 14번 ("비동기 AI 응답 흐름") 의 "race free" 표시가 이것.

### 발표 시 한 줄
> "AI 서버 호출이 5~30초 걸려서 동기로 두면 사용자가 그만큼 대기합니다. `@EnableAsync` + `@Async` 로 별도 스레드 풀에 떠넘기고, DB 의 PENDING/DONE/FAILED 상태로 결과를 전달합니다. 컨트롤러에서 호출하는 이유는 트랜잭션 commit 후 비동기 스레드가 PENDING row 를 안전하게 select 하도록 race 를 막기 위해서입니다."

### 청중 Q&A 대비

**Q. 왜 Kafka 같은 큐를 안 썼나요?**
> 졸업 프로젝트 규모에서는 큐 인프라가 오버 엔지니어링. AI 호출 동시성이 낮고 (사용자가 일기 한 개 쓰면 한 번 호출), 워커 수평 확장도 불필요. DB row 의 status 컬럼이 큐 역할로 충분. 트래픽이 늘면 그때 도입 검토.

**Q. AI 호출이 실패하면 어떻게 되나요?**
> `AiInferenceException` 을 catch 해서 `markFailed(errorMessage)` 호출 → DB row 의 status 가 FAILED 로 전이. 사용자가 폴링으로 받는 응답은 status=FAILED + errorMessage. 일기 자체는 이미 저장돼 있어서 잃지 않음.

**Q. ThreadPool 이 가득 차면?**
> 현재는 Spring 기본 executor 라 사실상 무제한 (실은 4.x 의 새 기본값에 따라 다름). `ThreadPoolTaskExecutor` 명시 후에는 queueCapacity 까지 차면 RejectedExecutionException → 트리거 자체가 실패. 사용자는 일기는 저장됐는데 AI 응답이 영원히 PENDING 으로 남는 상황. 이건 PR 4-final 부하 측정 후 정책 결정 예정.

**Q. `@Async` 가 같은 클래스 내부 호출에서는 안 먹는다고 들었는데?**
> 맞음. Spring AOP 프록시는 외부에서 빈 메서드를 호출할 때만 가로채는 구조. 같은 클래스 안에서 `this.triggerAsync()` 로 호출하면 프록시를 거치지 않아서 동기 실행됨. Moodiary 는 `PostController` 가 `aiResponseService.triggerAsync()` 로 **다른 빈을 통해** 호출하니 안전.

### 코드 위치
- `src/main/java/hoseo/moodiary/config/AsyncConfig.java` — `@EnableAsync` 스위치
- `src/main/java/hoseo/moodiary/service/AiResponseService.java` — `@Async public void triggerAsync(UUID postId)`
- `src/main/java/hoseo/moodiary/controller/PostController.java` — commit 후 trigger 호출
- `src/main/java/hoseo/moodiary/entitiy/AiResponseStatus.java` — `PENDING / DONE / FAILED` enum

### 관련 노트
- [6번. dev/main 분리](#6-dev-와-main-의-의미--왜-dev-머지로는-운영-반영-안-되나) — PR 4-pre 는 dev → main release PR (#62) 후에야 운영 EC2 에 비동기 골격이 반영됨
- [8번. ddl-auto 의 한계](#8-ddl-auto-update-의-한계--운영-머지-후-unique-인덱스-사후-검증이-필요한-이유) — `ai_response` 테이블의 `uk_ai_response_post_id` 가 비동기 1:1 정합성을 보장하는 핵심 제약

---

## 11. AWS SSM Run Command — outbound polling 구조 + IAM role / 0 인바운드 / send-command 한계

> 🚀 **공개 일반화판**: [AWS SSM Run Command — outbound polling 메커니즘 — goospel.github.io](https://goospel.github.io/notes/ops/aws-ssm-outbound-polling/) — Moodiary 맥락 제거된 일반 패턴 노트. 면접 / 외부 공유 시 이쪽 링크 사용.

### 한 줄 요약
> **AWS SSM Run Command** 는 EC2 에 미리 깔린 `amazon-ssm-agent` 가 outbound polling 으로 AWS 큐에서 명령을 받아 root 권한으로 실행하는 구조 — SSH 키 없이 IAM role 만으로 원격 명령 가능, 포트 22 닫고도 됨. `send-command` 는 enqueue 만 보장 (silent fail 가능) — `wait command-executed` + health check 가 있어야 CD 초록불이 진짜 deploy 성공을 의미한다.

### AWS SSM 이 무엇인가

**Systems Manager** — AWS 가 제공하는 **인스턴스 운영 통합 도구 모음**. 한 서비스가 아니라 여러 하위 기능의 묶음:

| 하위 기능 | 역할 |
|---|---|
| **Run Command** (`send-command`) ← Moodiary 가 쓰는 것 | EC2 에 임의 shell 명령 원격 실행 |
| **Session Manager** | SSH 대체 — 브라우저 / CLI 로 EC2 shell 접속 (포트 22 안 열고) |
| **Parameter Store** | 시크릿 / 설정값 중앙 저장 (RDS 비번 등) |
| **Patch Manager** | OS 패치 자동 |
| **State Manager** | EC2 의 desired state 유지 (config drift 방지) |
| **Inventory** | 모든 인스턴스의 설치 SW 목록 자동 수집 |

→ Moodiary 가 직접 쓰는 건 **Run Command** 뿐. 나머지는 졸업 프로젝트 범위 초과.

### SSH 가 아니라 SSM 을 선택한 이유

| 비교 항목 | SSH 방식 | SSM Run Command |
|---|---|---|
| **인증 키** | private key 파일 관리 필요 | IAM role 만 (키 X) |
| **포트** | 22 인바운드 오픈 필요 | **0 인바운드** (agent 가 outbound) |
| **감사 로그** | 직접 구성 (auditd 등) | CloudTrail 에 자동 |
| **GitHub Actions 연동** | SSH key 를 GitHub Secrets 에 박아야 함 | OIDC + IAM role 만 |
| **키 회전 / 유출 대응** | 키 새로 생성 + 모든 인스턴스 재배포 | IAM policy 한 줄 변경 |
| **포트 22 공격면** | 24/7 노출 | **0** |

졸업 프로젝트에서 결정적인 한 가지:
> **GitHub Actions 가 EC2 에 들어가야 하는데, SSH key 를 GitHub Secrets 에 저장하기 싫었다.** 키가 새면 모든 EC2 의 authorized_keys 를 손봐야 함. SSM 은 IAM role 한 줄로 통제 가능.

### SSM Run Command — 내부 작동 원리

핵심: **SSM Agent** 가 사전에 설치되어 있어야 한다.

```
┌─────────────────────┐                  ┌──────────────────────────────┐
│ GitHub Actions      │                  │  EC2 instance                │
│ (CD workflow)       │                  │  ┌────────────────────────┐  │
│                     │                  │  │ amazon-ssm-agent       │  │
│ aws ssm             │                  │  │ (백그라운드 데몬)        │  │
│   send-command ──────► AWS SSM Service │  │                        │  │
│   --instance-ids    │  (Region 내부)   │  │ outbound polling       │  │
│   --document-name   │       │          │  │ (HTTPS, 443) ──────────┼──┘
│   AWS-RunShellScript│       └──────────► (큐에서 명령 받음)        │
│                     │                  │  │                        │  │
│ aws ssm             │                  │  │ shell 실행             │  │
│   wait              │                  │  │ (root 권한)            │  │
│   command-executed ◄─────── 결과 응답  │  │                        │  │
└─────────────────────┘                  │  └────────────────────────┘  │
                                         └──────────────────────────────┘
```

흐름 단계별:
1. **GitHub Actions** 가 `aws ssm send-command` 호출 — 인자: instance ID, document name (`AWS-RunShellScript`), 실행할 shell 명령
2. **AWS SSM 서비스** 가 명령을 큐에 박아둠
3. **EC2 의 ssm-agent** 가 **outbound polling** 으로 (인바운드 X) 큐를 주기적 확인
4. agent 가 명령을 가져와서 **root 권한**으로 shell 실행
5. 결과 (stdout / stderr / exit code) 를 SSM 서비스에 응답
6. GitHub Actions 의 `aws ssm wait command-executed` 가 그 응답을 받음

**핵심 포인트 — Outbound Polling**: EC2 입장에서는 **22번 포트가 닫혀 있어도 됨**. agent 가 HTTPS (443) 로 AWS endpoint 에 polling 만 함. 그래서 보안 그룹 인바운드를 완전히 봉쇄 가능.

### Moodiary 의 실제 CD 명령

`.github/workflows/moodiary-be-cd.yaml` 의 핵심:

```yaml
- name: Deploy via SSM
  run: |
    COMMAND_ID=$(aws ssm send-command \
      --region ap-northeast-2 \
      --instance-ids "${{ secrets.AWS_EC2_INSTANCE_ID }}" \
      --document-name "AWS-RunShellScript" \
      --parameters 'commands=[
        "cd /home/ec2-user/moodiary",
        "docker-compose pull",
        "docker-compose up -d"
      ]' \
      --query "Command.CommandId" \
      --output text)

    aws ssm wait command-executed \
      --command-id "$COMMAND_ID" \
      --instance-id "${{ secrets.AWS_EC2_INSTANCE_ID }}"
```

### IAM Role 두 갈래

#### GitHub Actions → AWS

GitHub Actions 는 OIDC 로 AWS 에 자격증명을 받고, 그 role 이 다음 policy 를 가짐:

```json
{
  "Effect": "Allow",
  "Action": [
    "ssm:SendCommand",
    "ssm:GetCommandInvocation"
  ],
  "Resource": [
    "arn:aws:ec2:ap-northeast-2:*:instance/i-xxxxx",
    "arn:aws:ssm:ap-northeast-2::document/AWS-RunShellScript"
  ]
}
```

→ "이 한 EC2 인스턴스에만, 이 한 문서로만, send-command 가능". 최소 권한.

#### EC2 → AWS SSM

EC2 의 IAM instance profile 에 `AmazonSSMManagedInstanceCore` policy 부착되어야 agent 가 polling 가능. 이게 없으면 ssm-agent 는 떠 있어도 명령을 못 받음 — Moodiary 셋업 초기에 한 번 빼먹어서 헤맸음.

### `wait command-executed` — Silent Fail 의 진실 ([T-019](./troubleshooting.md) / T-023)

`send-command` 는 **명령을 큐에 enqueue 한 순간 성공 반환**한다. 즉, agent 가 실제로 받아서 실행했는지는 **모름**. 이게 silent fail 의 원인이었다:

```
옛 워크플로우 (PR #45 이전):
  aws ssm send-command ...    ← enqueue 성공 → exit 0
                                  ✅ GitHub Actions 초록불

  실제 EC2:
    - agent 가 죽어 있으면 → 명령 영원히 안 받음
    - docker-compose plugin 없음 → 실행 실패
    - 둘 다 GitHub Actions 가 모름

  결과: CD 초록불 = 가짜. 운영 image 갱신은 사람이 수동 SSH 로
        (Claude 가 발견 전까지 수개월간 가짜 초록 체크)
```

PR #45 이후의 안전망:
```bash
COMMAND_ID=$(aws ssm send-command ... --query "Command.CommandId" --output text)

aws ssm wait command-executed --command-id "$COMMAND_ID" --instance-id "..."
   # ↑ 명령이 Success / Failed 상태가 될 때까지 대기 → Failed 면 non-zero exit

curl -fsS http://EC2_IP:8080/v3/api-docs --max-time 30
   # ↑ swagger 가 200 응답할 때까지 polling
```

→ **CD 초록불 = 진짜 deploy 성공** 으로 의미 회복.

### SSM 의 다른 면 — 안 쓰지만 알아두기

#### Parameter Store — 시크릿 중앙 저장

현재 Moodiary 는 시크릿을 **GitHub Secrets + EC2 `.env` 이중 관리**. 단점:
- 둘이 어긋날 위험
- EC2 새로 만들 때마다 `.env` 수동 배포

Parameter Store 면:
```bash
aws ssm get-parameter --name /moodiary/prod/JWT_SECRET --with-decryption
```
- 모든 인스턴스가 같은 시크릿 자동 동기화
- IAM 으로 접근 제어
- 변경 history 자동

→ 졸업 발표 이후 PR 7 (ECS) 와 같이 도입 검토.

#### Session Manager — SSH 대체

현재 Moodiary 는 디버깅 시 SSH 로 EC2 에 들어감 (포트 22 오픈 필요).

Session Manager 면:
```bash
aws ssm start-session --target i-xxxxx
```
- 브라우저 / AWS CLI 에서 직접 shell 접속
- 포트 22 완전 봉쇄 가능
- 모든 세션 CloudTrail 에 자동 로그
- IAM 으로 누가 언제 들어왔는지 통제

→ 보안 향상의 자연스러운 다음 단계.

### 발표 시 한 줄
> "SSH 키를 GitHub Secrets 에 두는 게 부담스러워서 SSM Run Command 를 선택했습니다. EC2 의 ssm-agent 가 outbound polling 으로 AWS 큐에서 명령을 받는 구조라 포트 22 를 닫고도 자동 배포가 됩니다. 다만 send-command 자체는 enqueue 만 보장해서 wait + health check 가 없으면 silent fail 이 일어납니다 — 그게 T-019 의 한 층이었습니다."

### 청중 Q&A 대비

**Q. SSH 보다 느리지 않나요?**
> Polling 주기와 큐 처리 때문에 명령 시작까지 보통 1~3초 지연. 자동 배포 시나리오에서는 문제 없음. 인터랙티브 디버깅에는 Session Manager 가 더 적합.

**Q. ssm-agent 가 죽으면?**
> agent 가 죽으면 명령이 계속 큐에 쌓이다가 timeout. `wait command-executed` 가 fail 응답을 주니 CD 가 빨간불로 알림. 복구는 EC2 SSH (또는 콘솔의 EC2 Instance Connect) 로 들어가서 `sudo systemctl restart amazon-ssm-agent`.

**Q. send-command 와 send-shell-command 같은 다른 게 있나요?**
> `send-command` 가 통합 API 이고, 실행 방식은 **SSM Document** 로 결정됨. `AWS-RunShellScript` (Linux shell), `AWS-RunPowerShellScript` (Windows), `AWS-RunRemoteScript` (S3 의 스크립트) 등. 즉 같은 API 로 OS 무관 명령 가능.

**Q. 명령 결과 stdout 은 어디서 보나요?**
> `aws ssm get-command-invocation --command-id ... --instance-id ...` 로 stdout / stderr 조회. CloudWatch Logs 로 자동 스트리밍하는 옵션도 있음 (Moodiary 는 사용 안 함).

### 코드 위치
- `.github/workflows/moodiary-be-cd.yaml` — `send-command` + `wait command-executed` + health check 전체
- EC2 IAM instance profile — `AmazonSSMManagedInstanceCore` policy
- GitHub Actions OIDC role — `ssm:SendCommand` + `ssm:GetCommandInvocation` 최소 권한
- [`troubleshooting.md` T-019 / T-023](./troubleshooting.md) — silent fail 의 역사와 안전망 추가 경위

### 관련 노트
- [6번. dev/main 분리](#6-dev-와-main-의-의미--왜-dev-머지로는-운영-반영-안-되나) — main 머지가 SSM 트리거
- [7번. SSM 자동화 범위](#7-aws-ssm-자동화--cd-성공-후-ec2-에서-수동-docker-pull-필요한가) — "수동 docker pull 필요한가" 의 답 (현재 항목이 그 메커니즘 자체)
- [10번. Spring 비동기](#10-spring-비동기-enableasync--async--동기-블로킹-회피--db-상태머신--race-방지) — 같은 "큐 + polling" 패턴이 도메인 비동기에도 적용됨 (이쪽은 DB 가 큐, 거기는 AWS SSM 이 큐)

---

## 12. CORS — Same-Origin Policy + 브라우저 차단 메커니즘 + Preflight + allowlist vs 와일드카드

> 🚀 **공개 일반화판**: [CORS — SOP + Preflight + allowlist — goospel.github.io](https://goospel.github.io/notes/backend/cors-fundamentals/) — Moodiary 맥락 제거된 일반 패턴 노트. 면접 / 외부 공유 시 이쪽 링크 사용.

### 한 줄 요약
> **CORS** 는 브라우저의 Same-Origin Policy (다른 origin 응답 읽기 차단) 의 예외를 **서버 응답 헤더로 명시적 허용** 하는 메커니즘. 인증 / JSON body 같은 비단순 요청은 본 요청 전에 **OPTIONS preflight** 로 사전 확인. 와일드카드 `*` 은 `credentials=true` 와 충돌하므로 정확한 origin allowlist 가 안전한 정책.

### Same-Origin Policy (SOP) — CORS 의 전제

브라우저의 가장 오래된 보안 규칙:
> JavaScript 가 **다른 origin 의 응답을 읽을 수 없게 차단**한다.

**Origin** = Protocol + Host + Port 세 요소가 모두 같아야 "same origin":

| URL A | URL B | Same Origin? |
|---|---|---|
| `https://example.com/page` | `https://example.com/api` | ✅ |
| `https://example.com` | `http://example.com` | ❌ (protocol 다름) |
| `https://example.com` | `https://api.example.com` | ❌ (host 다름) |
| `https://example.com:443` | `https://example.com:8080` | ❌ (port 다름) |

### SOP 가 왜 필요한가 — 악성 시나리오

SOP 가 없다면:
```
사용자가 은행 사이트 (bank.com) 로그인 중 — 쿠키 살아있음
   │
   ↓
같은 탭으로 evil.com 방문
   │
   ↓
evil.com 의 JavaScript:
   fetch('https://bank.com/api/transfer?to=hacker&amount=1000000')
   ↑ 브라우저가 자동으로 bank.com 쿠키 첨부
   ↑ 은행 서버는 "로그인된 사용자의 정상 요청" 으로 인식
   ↑ 돈 이체 성공
```

→ SOP 가 이걸 기본적으로 차단. evil.com 의 JS 는 bank.com 의 응답을 읽을 수 없음.

### 그런데 — 정당한 cross-origin 도 많다

Moodiary 의 실제 상황:
```
Frontend:  http://moodiary-frontend.s3-website.ap-northeast-2.amazonaws.com   (S3, 도쿄)
Backend:   http://15.165.95.129:8080                                          (EC2, 서울)
                                ↑ Origin 완전히 다름
```

FE 의 React 가 BE 의 API 를 호출해야 하는데 SOP 가 차단. 그래서 필요한 게 **CORS**.

### CORS — 서버가 "이 origin 은 OK" 라고 선언

CORS 는 HTTP 응답 헤더로 동작. 서버가 응답에 특정 헤더를 박으면, 브라우저가 "OK, 이 origin 에서 온 요청은 허용".

**단순 요청 (Simple Request) 의 흐름**:
```
[브라우저 in S3 (FE)]                        [서버 EC2 (BE)]

GET /post
Origin: http://moodiary-frontend.s3-website...
                                  ─────────►  처리 후 응답

                                  ◄─────────  200 OK
                                              Access-Control-Allow-Origin: http://moodiary-frontend.s3-website...
                                              [body]

브라우저: 응답 헤더의 ACAO 가 내 origin 과
일치 → JS 에 응답 전달 ✅
```

핵심 헤더:
- **요청 헤더 `Origin`** — 브라우저가 자동으로 박음. JS 가 조작 불가능.
- **응답 헤더 `Access-Control-Allow-Origin` (ACAO)** — 서버가 박음. 브라우저가 이걸 확인.

**단순 요청의 조건** — 다 충족해야 simple:
- Method: `GET` / `POST` / `HEAD` 중 하나
- Content-Type: `text/plain` / `application/x-www-form-urlencoded` / `multipart/form-data` 중 하나
- 커스텀 헤더 없음 (`Authorization` 도 없음)

→ **Moodiary 의 모든 API 호출은 `Authorization: Bearer ...` 헤더가 있어서 simple 이 아님** → preflight 필요.

### Preflight Request (`OPTIONS`)

JWT 인증, JSON body 같은 게 들어가면 브라우저가 본 요청 전에 사전 확인:

```
[브라우저]                                    [서버]

─── ① Preflight (OPTIONS) ─────────────────►
OPTIONS /post
Origin: http://moodiary-frontend...
Access-Control-Request-Method: POST
Access-Control-Request-Headers: Authorization,Content-Type

                                  ◄─────────  204 No Content
                                              Access-Control-Allow-Origin: http://moodiary-frontend...
                                              Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
                                              Access-Control-Allow-Headers: Authorization, Content-Type, Accept
                                              Access-Control-Allow-Credentials: true
                                              Access-Control-Max-Age: 3600

브라우저: 모두 OK → 본 요청 진행

─── ② 본 요청 (POST) ──────────────────────►
POST /post
Origin: http://moodiary-frontend...
Authorization: Bearer eyJhbGc...
Content-Type: application/json
{ "title": "...", "content": "..." }

                                  ◄─────────  201 Created
                                              Access-Control-Allow-Origin: http://moodiary-frontend...
                                              [body]
```

→ 모든 요청이 2 번 가는 게 아님. preflight 결과는 `Access-Control-Max-Age` 동안 (Moodiary: 3600초 = 1시간) 브라우저가 캐시. 그 동안 같은 endpoint 의 본 요청은 preflight 없이 바로 감.

### Moodiary 의 실제 CorsConfig

`CorsConfig.java` 의 핵심:

```java
CorsConfiguration config = new CorsConfiguration();

config.setAllowedOrigins(origins);  // ["http://localhost:5173", "http://moodiary-frontend.s3-website..."]
                                     // ↑ ENV var APP_CORS_ALLOWED_ORIGINS 로 받음. 와일드카드 X

config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                                                                   // ↑ OPTIONS 필수 (preflight 자체)

config.setAllowedHeaders(List.of(
    HttpHeaders.AUTHORIZATION,  // JWT Bearer 헤더
    HttpHeaders.CONTENT_TYPE,   // JSON body
    HttpHeaders.ACCEPT
));

config.setExposedHeaders(List.of(HttpHeaders.LOCATION));
   // ↑ JS 가 response.headers.get('Location') 로 직접 읽을 수 있는 헤더

config.setAllowCredentials(true);
   // ↑ 쿠키 / Authorization 헤더를 cross-origin 으로 전송 허용

config.setMaxAge(3600L);
   // ↑ preflight 캐시 1시간 — 같은 endpoint 매번 OPTIONS 안 보냄
```

→ 운영에서는 S3 endpoint URL, 로컬에서는 `localhost:5173` (Vite 기본) 을 환경변수로 주입.

### 자주 만나는 함정 / 함의

#### 함정 1: 와일드카드 `*` + `credentials=true` 충돌

```java
config.setAllowedOrigins(List.of("*"));   // ❌
config.setAllowCredentials(true);
```

브라우저가 거절. 이유: 와일드카드는 "아무 origin 다 OK" 인데 거기에 쿠키 / Authorization 까지 보내는 건 보안 무너짐. 정확한 origin 만 적어야 함.

#### 함정 2: CORS 는 **브라우저** 만의 규칙

- Postman / curl / 백엔드 → 백엔드 호출은 CORS 무관 (Origin 헤더 없음 또는 무시)
- 그래서 Postman 테스트는 다 통과해도, React SPA 에서 호출하면 CORS 에러 가능 — 별도 검증 필수

#### 함정 3: CORS 차단은 **서버가 차단하는 게 아님**

요청은 서버까지 도달함. 서버는 정상 처리하고 응답을 보냄. **브라우저가 응답 헤더를 보고 JS 에 전달 거부**. 즉:
- 서버 로그에는 "200 OK" 가 찍히는데
- 브라우저 콘솔에는 "CORS error" 가 뜸
- DevTools Network 탭에서는 응답이 보이지만 "blocked" 표시

→ "서버는 정상인데 왜 클라이언트가 못 받지" 사고가 여기서 발생.

#### 함의 4: Preflight 는 인증 전에 통과해야

Spring Security 설정에서 **OPTIONS 요청은 인증 검사 X**. Moodiary 의 `SecurityConfig` 가 OPTIONS 를 화이트리스트에 넣었기 때문에 preflight 가 통과 → 본 요청에서야 JWT 검증.

#### 함의 5: 이미 발생한 CORS error 는 "거절된 응답" — 재시도 무의미

CORS 에러는 응답에 ACAO 가 없거나 잘못된 origin 이라 브라우저가 거절한 것. 클라이언트 측 재시도 / 재인증 로직으로 풀리지 않음. **서버 설정 변경만이 답**.

### 발표 시 한 줄
> "FE 가 S3 도쿄, BE 가 EC2 서울에 있어서 origin 이 완전히 다릅니다. 브라우저의 Same-Origin Policy 가 기본적으로 차단하니까, BE 가 Access-Control-Allow-Origin 응답 헤더로 명시적으로 허용해줘야 합니다. JWT 인증 헤더가 있어서 본 요청 전에 OPTIONS preflight 가 먼저 가고요. 와일드카드 대신 정확한 URL allowlist 만 쓰는데, 이게 credentials=true 와 동시 만족이 가능한 유일한 방식이기 때문입니다."

### 청중 Q&A 대비

**Q. 서버에서 CORS 를 막는 게 아니라 브라우저가 막는다고요?**
> 맞습니다. 요청은 서버까지 도달하고 서버는 정상 처리해서 응답을 보냅니다. 단지 브라우저가 응답 헤더를 검사해서 ACAO 가 없거나 다르면 JS 의 fetch().then() 에 응답을 안 넘겨주는 거죠. 그래서 서버 로그에는 200 이 찍히는데 클라이언트 콘솔에는 CORS error 가 뜨는 사고가 흔합니다.

**Q. Postman 으로는 되는데 브라우저에서는 안 돼요. 왜?**
> Postman 은 브라우저가 아니라서 SOP / CORS 검사를 안 합니다. Origin 헤더를 자동으로 박지도 않고요. 그래서 Postman 통과 = 백엔드 정상 동작 증명이지만, 브라우저 통과의 증명은 아닙니다. SPA 통합 테스트가 별도로 필요한 이유.

**Q. 왜 와일드카드를 못 쓰나요?**
> 와일드카드 자체는 가능합니다 — `allowCredentials=false` 이면. 우리는 JWT Authorization 헤더를 cross-origin 으로 보내기 위해 `allowCredentials=true` 가 필요하고, 이 둘은 동시 사용 불가능하다는 게 표준입니다. 모든 origin 에 쿠키 / Auth 를 풀어주면 보안이 무너지니까요.

**Q. Preflight 가 매 요청마다 가나요? 비효율 아닌가요?**
> 첫 요청만요. 응답의 Access-Control-Max-Age 동안 (우리는 1시간) 브라우저가 캐시합니다. 같은 endpoint 의 같은 method / headers 조합이면 그 동안 본 요청만 갑니다.

**Q. S3 endpoint 도 와일드카드 패턴 (`*.s3-website.*.amazonaws.com`) 으로 두면 편하지 않나요?**
> CorsConfig 주석에 적혀 있는 정책 — 패턴 와일드카드는 의도치 않은 다른 S3 버킷도 허용하게 됩니다. 우리 FE 버킷이 아닌 누군가가 자기 S3 사이트에서 우리 API 를 호출할 수 있는 길을 열어주는 셈이라, 정확한 URL 만 명시합니다.

### 코드 위치
- `src/main/java/hoseo/moodiary/config/CorsConfig.java` — `CorsConfigurationSource` 빈, allowlist / methods / headers / credentials / maxAge 정책
- `src/main/java/hoseo/moodiary/config/SecurityConfig.java` — `.cors(Customizer.withDefaults())` 가 위 빈을 자동으로 wire, OPTIONS 인증 면제
- `src/test/java/hoseo/moodiary/config/CorsConfigTest.java` — `@WebMvcTest + @Import(SecurityConfig)` 4 케이스 검증
- `compose.yaml` — `APP_CORS_ALLOWED_ORIGINS=${APP_CORS_ALLOWED_ORIGINS:-http://localhost:5173}` (default fallback)

### 관련 노트
- [1번. Refresh Token](#1-refresh-token--왜-access-token-한-개로-부족한가) — JWT Authorization 헤더가 CORS 의 "비단순 요청" 트리거
- [5번. JWT stateless vs Refresh stateful](#5-jwt-vs-refresh-token-의-본질적-차이--stateless-vs-stateful) — Authorization 헤더 사용 / 쿠키 미사용의 배경
- [11번. AWS SSM](#11-aws-ssm-run-command--outbound-polling-구조--iam-role--0-인바운드--send-command-한계) — 같은 "보안 그룹" 영역 — SSM 은 인바운드 0, CORS 는 origin allowlist

---

## 13. OAuth2 — Token-Exchange 패턴 + audience 검증 (confused deputy) + email_verified 위임의 한계

### 한 줄 요약
> Google 로그인은 "Google 이 서명한 토큰이니까 그대로 믿자" 가 아니다. **누구를 위해 발급된 토큰인지** ({@code aud}) 와 **Google 이 해당 이메일을 진짜로 검증했는지** ({@code email_verified}) 를 BE 가 한 번 더 확인해야 다른 앱 사용자가 우리 서비스로 가장 가입하는 confused deputy 공격을 막을 수 있다.

### 배경 — OAuth2 흐름은 한 가지가 아니다

OAuth2 는 "사용자가 다른 사이트의 인증을 빌려 우리 사이트에 로그인" 이라는 같은 목적을 여러 방식으로 푼다. SPA + 우리 BE 조합에선 보통 둘 중 하나:

| 방식 | 주체 | 흐름 |
|---|---|---|
| **Authorization Code (전통)** | BE 가 OAuth 흐름 주도 | 사용자 → BE → Google redirect → 사용자가 Google 로그인 → Google → BE callback (code) → BE 가 code↔token 교환 → BE 가 user info 호출 |
| **Token-Exchange (우리)** | FE 가 OAuth 흐름 주도, BE 는 검증만 | 사용자 → FE 의 Google Sign-In 버튼 → Google → FE 가 id_token 직접 받음 → FE → BE 에 `POST /auth/oauth2/google { providerAccessToken }` → BE 가 토큰만 검증 + 사용자 info 추출 |

**우리가 token-exchange 를 고른 이유**:
1. **SPA / 모바일 친화** — BE 가 redirect / session 관리할 필요 없음. 우리는 어차피 stateless JWT 라 redirect 흐름 도입하면 부정합.
2. **FE 가 어차피 Google SDK 를 쓴다** — Google Identity Services (GIS) 가 alread popup / button / token 발급 모두 처리. BE 가 같은 일을 또 할 이유 없음.
3. **API 라인 분리** — BE 는 우리 도메인 (User / Post / Calendar) 만, 외부 provider 흐름은 FE 가 담당. 책임 분리가 깔끔.

### 그래서 BE 는 무얼 받는가 — id_token vs access_token

Google Sign-In 의 모던 flow (GIS) 는 기본적으로 **id_token (JWT)** 을 FE 에 돌려준다. 일부 흐름에선 access_token 도 받을 수 있다. 둘은 결정적으로 다르다:

| 토큰 종류 | 형식 | 정보 | 검증 방식 |
|---|---|---|---|
| **id_token** | JWT (header.payload.signature) | payload 에 sub / email / name 등 user info 포함 | **로컬에서 Google JWKS 로 서명 검증** 가능 (네트워크 호출 X) — 또는 tokeninfo endpoint 로 위임 |
| **access_token** | opaque 문자열 | 자체엔 정보 없음 — Google API 호출 권한만 표현 | Google API 호출 (e.g. `userinfo` endpoint) 로 user info 받아와야 |

우리 선택: **Google 의 `tokeninfo` endpoint 에 위임**.
- 입력: `GET https://oauth2.googleapis.com/tokeninfo?id_token=<JWT>`
- Google 이 서명 / 만료 / iss 표준 검증 + payload 를 JSON 으로 풀어서 응답
- 단점: 매번 외부 HTTP 호출 — 로그인 시점에만 일어나는 일이라 latency 부담 미미
- 장점: JWT 라이브러리 / JWKS 캐싱 / 키 회전 처리 없이 외부 호출 1번으로 끝남 — 졸업프로젝트 규모에 적절

### 핵심 함의 1 — Google 이 서명했다 ≠ 우리 토큰이다 (audience 검증 / confused deputy)

여기가 보안의 핵심. tokeninfo 응답이 200 OK 면 토큰은 "진짜 Google 이 발급한 valid 토큰". **하지만 누구를 위해 발급된 토큰인지는 따로 확인해야** 한다.

```json
{
  "aud": "12345.apps.googleusercontent.com",   ← 토큰의 청중 (audience)
  "sub": "108273652891234",
  "email": "alice@gmail.com",
  "email_verified": "true",
  "name": "Alice"
}
```

**시나리오 — confused deputy**:
1. 공격자가 자기 Google 앱 (`hacker-app.apps.googleusercontent.com`) 을 만든다.
2. 자기 앱으로 어떤 사용자 (Alice) 의 Google 로그인을 받는다. Alice 가 동의함.
3. Alice 의 id_token 을 받은 공격자는 그걸 **우리 BE 의 `/auth/oauth2/google` 에 그대로 던진다**.
4. **우리가 aud 검증 안 하면**: tokeninfo 가 200 OK + Alice 정보 반환 → 우리가 "Alice 로그인" 으로 처리 → 공격자가 Alice 계정에 접근.

**confused deputy** 이름의 유래: tokeninfo 가 "권한 있는 대리인 (deputy)" 이라 valid 토큰만 잘 검증해주는데, **그 토큰이 어디로 향한 토큰인지** 는 신경 안 씀 — 그 판단은 우리가 해야. "혼란스러운 대리인" 패턴.

**막는 법** — `aud` 클레임이 우리 `GOOGLE_OAUTH_CLIENT_ID` 와 정확히 일치할 때만 통과:

```java
if (info.aud() == null || !info.aud().equals(expectedAudience)) {
    throw new OAuth2VerificationException("audience 불일치 — 우리 client_id 용 토큰이 아님");
}
```

**왜 이게 우리에 적용되나** — Moodiary 의 client_id (`xxx.apps.googleusercontent.com`) 와 다른 앱의 client_id 는 명시적으로 달라야 한다. tokeninfo 검증은 토큰 발급의 진위만 보장하지, **목적지**는 보장 안 함.

### 핵심 함의 2 — email_verified 위임의 한계

```json
{ "email": "victim@gmail.com", "email_verified": "false" }
```

Google 도 가입 시 모든 이메일에 verification mail 을 보내지는 않는다. SSO / Workspace 계정처럼 다른 IdP 가 위임한 이메일은 Google 측에서 검증 안 된 상태일 수 있다.

**시나리오 — 다른 사람 가장 가입**:
1. 공격자가 자기 Google 계정에 `victim@gmail.com` 을 alias 로 등록 (Google 이 검증 안 한 채로).
2. Google Sign-In 으로 id_token 발급 — email 필드에 `victim@gmail.com` 들어옴.
3. 우리가 email_verified 확인 안 하면: "victim@gmail.com" 로 우리 서비스 가입 → 진짜 victim 이 나중에 가입하려 할 때 이메일 중복 차단.

**막는 법** — `email_verified == "true"` 일 때만 통과 (Google 응답은 boolean 이 아니라 **문자열** "true"/"false" 라는 점 주의):

```java
if (!"true".equals(info.emailVerified())) {
    throw new OAuth2VerificationException("email_verified=false");
}
```

### 핵심 함의 3 — Stub 토글 (`oauth2.client.mode`)

외부 의존성을 분리하기 위해 같은 `OAuth2Provider` 인터페이스의 다른 구현을 부팅 시점에 선택한다:

```java
@Component
@ConditionalOnProperty(name = "oauth2.client.mode", havingValue = "stub", matchIfMissing = true)
public class StubOAuth2Provider implements OAuth2Provider { ... }

@Component
@ConditionalOnProperty(name = "oauth2.client.mode", havingValue = "http")
public class HttpGoogleOAuth2Provider implements OAuth2Provider { ... }
```

- **로컬 dev / 단위 테스트** — `stub` (default). 외부 키 없이 부팅. `stub:{PROVIDER}:{providerId}:{email}:{nickname}` 형식으로 토큰 흉내.
- **운영 / 시연** — `http`. 실제 Google tokeninfo 호출.

`matchIfMissing=true` 가 핵심 — yaml 에 키가 빠져도 stub 이 active 되어 부팅 폭발 방지. 운영은 env var `OAUTH2_CLIENT_MODE=http` 로 override.

같은 패턴이 PR 4 (AI 어댑터) 의 `ai.client.mode` 에서도 재사용된다. **외부 의존성 차단 = Stub 토글** 이 졸업프로젝트에서 발견한 재사용 가능한 패턴.

### 발표 시 한 줄 비유

> "Google 로그인은 'Google 이 valid 라고 한 ID 카드' 받는 거예요. 근데 그 ID 카드가 우리 가게용으로 발급된 건지 (audience), Google 이 이메일 진짜 본인 거 맞는지 확인했는지 (email_verified) — 이 두 개를 우리가 한 번 더 검사해야 합니다. 안 그러면 다른 가게 ID 카드를 우리 가게에 들이밀면 통과하는 confused deputy 공격에 뚫려요."

### 청중 Q&A 대비

**Q. Google 이 이미 검증한 토큰을 왜 또 검증해요?**
> Google 은 토큰의 진위 (서명 / 만료) 만 검증해줍니다. 하지만 **그 토큰이 우리 앱을 위해 발급된 건지** 는 모르죠. 토큰의 `aud` 클레임에 발급 대상 client_id 가 박혀 있는데, 그게 우리 `GOOGLE_OAUTH_CLIENT_ID` 와 같을 때만 우리 토큰입니다. 검증 안 하면 누군가 자기 Google 앱으로 받은 사용자 토큰을 우리 BE 에 던져서 그 사용자로 로그인할 수 있어요 — confused deputy 패턴이에요.

**Q. id_token 과 access_token 의 차이가 뭐예요?**
> id_token 은 JWT 라서 자체에 사용자 정보 (sub / email / name) 가 들어있고 로컬에서 서명 검증 가능. access_token 은 불투명 문자열이라 그 자체로는 정보 없고 Google API 를 호출할 권한만 표현해요. 우리는 id_token 을 받아서 Google tokeninfo endpoint 에 검증 위임합니다 — JWT 라이브러리 / JWKS 캐싱 / 키 회전 처리 없이 외부 호출 1번으로 끝나서 졸업프로젝트 규모에 깔끔합니다.

**Q. 왜 BE 가 redirect 흐름 안 쓰고 FE 가 토큰 받아서 넘기는 방식이에요?**
> 우리는 stateless JWT 인증이라 BE 에 session 이 없습니다. 그런데 OAuth2 redirect 흐름 (Authorization Code) 은 callback 사이에 state 를 유지해야 해서 stateful 이에요 — 우리 인증 방식과 모순이죠. 그리고 FE 는 어차피 Google SDK 를 쓰는데, BE 가 같은 일을 또 할 이유가 없습니다. SPA / 모바일 친화 방식이고, 책임 분리도 깔끔합니다.

**Q. email_verified 가 string "true" 인 이유는?**
> Google tokeninfo endpoint 의 응답 명세가 그래요. JSON boolean 이 아니라 string 으로 옵니다. 라이브러리 없이 직접 파싱할 땐 `"true".equals(...)` 로 비교해야 해서 흔히 놓치는 함정입니다.

**Q. Kakao 도 같이 했어요?**
> 코드 구조는 다 만들어뒀습니다 — `OAuth2Provider` 인터페이스 + `AuthProvider` enum + `POST /auth/oauth2/{provider}` path 변수 — Kakao 추가는 (1) enum 값 1개 + (2) `HttpKakaoOAuth2Provider` 구현 1개로 끝나요. 하지만 졸업프로젝트 범위에서 Kakao Developers 의 "사이트 도메인이 localhost 거부" 가 FE S3 배포 선행을 요구했고, OAuth2 학습 가치는 한 provider 로도 충분히 정리돼서 지금은 Google 하나만 살아있습니다.

**Q. 외부 호출이 실패하면요? Google tokeninfo 가 5xx 던지면?**
> `OAuth2VerificationException` 으로 통합해서 401 응답. FE 가 재로그인을 유도하도록 합니다. 4xx (토큰 거절) 도 같은 401 — FE 입장에서 둘 다 "재로그인 필요" 로 처리할 수 있게.

### 코드 위치
- `src/main/java/hoseo/moodiary/service/oauth2/HttpGoogleOAuth2Provider.java` — `RestClient` 로 tokeninfo 호출 + aud / email_verified 검증 + OAuth2UserInfo 추출
- `src/main/java/hoseo/moodiary/service/oauth2/StubOAuth2Provider.java` — dev / 단위 테스트용 — 외부 HTTP 없이 같은 인터페이스 구현
- `src/main/java/hoseo/moodiary/service/oauth2/OAuth2Service.java` — provider 측 user 가 우리 DB 에 있으면 로그인, 없으면 가입 + 닉네임 충돌 시 suffix
- `src/main/java/hoseo/moodiary/controller/AuthController.java` — `POST /auth/oauth2/{provider}` endpoint
- `src/main/resources/application.yaml` — `oauth2.client.mode` / `oauth2.google.client-id` / `oauth2.google.tokeninfo-url` placeholder
- `src/test/java/hoseo/moodiary/service/oauth2/HttpGoogleOAuth2ProviderTest.java` — WireMock 으로 mock 한 tokeninfo 의 happy path / 4xx / 5xx / aud 불일치 / email_verified=false 분기

### 관련 노트
- [1번. Refresh Token](#1-refresh-token--왜-access-token-한-개로-부족한가) — OAuth2 로그인 성공 후 우리가 발급하는 토큰은 LOCAL 로그인과 동일한 access (1h) + refresh (2w) 쌍
- [5번. JWT stateless](#5-jwt-vs-refresh-token-의-본질적-차이--stateless-vs-stateful) — OAuth2 redirect 흐름 (stateful) 을 우리가 안 쓴 배경
- [10번. Spring 비동기](#10-spring-비동기-enableasync--async--동기-블로킹-회피--db-상태머신--race-방지) — `@ConditionalOnProperty` 같은 토글 패턴이 PR 4 의 `ai.client.mode` 에서도 재사용

---

## 14. Silent catch 의 두 얼굴 — 의도적 침묵 (high-volume) vs 운영 사고 (저-volume) + 메트릭 채널

### 한 줄 요약
> 똑같이 "catch 하고 log 안 떨어뜨림" 패턴이라도 **path 의 volume 에 따라 fix 방향이 정반대** — 저-volume 핸들러의 silent 는 운영 사고 (반드시 log 추가), high-volume 핸들러의 silent 는 의도적 설계 (log 박으면 디스크 폭발 — metrics 채널이 정답).

### 문제 — 같은 모양 다른 의도

두 catch 가 거의 똑같이 생겼다고 가정:

```java
// Case A — GlobalExceptionHandler.handleException
catch (Exception e) {
    return ResponseEntity.status(500).body(...);   // log 0
}

// Case B — JwtAuthenticationFilter
catch (JwtException | IllegalArgumentException e) {
    SecurityContextHolder.clearContext();           // log 0
}
```

코드 모양만 보면 둘 다 "silent swallow" — sweep 으로 잡으면 둘 다 "fix 후보" 로 식별. 하지만:
- **A 는 운영 사고** — 진단 가시성 0 이라 무조건 fix (T-032 본인).
- **B 는 의도적 설계** — 절대 fix 하면 안 됨 (운영 폭발).

이 둘을 판별하는 기준이 **path 의 volume**.

### 핵심 차이 — Volume

| 항목 | A (GlobalExceptionHandler) | B (JwtAuthenticationFilter) |
|---|---|---|
| 호출 빈도 | **드물게** — unhandled 예외 발생 시점만 (= 우리 코드의 버그) | **모든 요청** — Authorization 헤더 들어오는 모든 API 호출 |
| 정상 흐름의 발생 빈도 | 0건 (예외 = 비정상) | 자연 발생 (만료 토큰 / 옛 토큰 / 봇 spam) |
| log 박았을 때 일자 사이즈 | 분당 0~1줄 (사고 시점만) | **분당 수백~수천 줄** (특히 봇 spam 시) |

**volume 의 분기점** → "이 catch 는 분당 몇 번 발생할 수 있는가?" 가 첫 질문. 답이 "사고 시점만" 이면 fix. 답이 "100건/분 이상 가능" 이면 의도적 침묵 + 다른 채널.

### 왜 high-volume 에 log 박으면 폭발하는가

JwtAuthenticationFilter 의 catch 가 trigger 되는 시나리오들:

| 시나리오 | 정상 / 비정상 | 발생 빈도 (졸업 demo 규모) |
|---|---|---|
| 사용자 access token 만료 직전 마지막 호출 | 정상 | 사용자당 매 1시간 |
| FE 가 localStorage 옛 토큰으로 reload | 정상 | 새 탭 / 새 기기 / 캐시 클리어 시 |
| 봇 / 보안 스캐너 임의 토큰 spam | 비정상 (외부) | **분당 수백 ~ 수천** |
| JWT secret 변경 후 옛 토큰 | 운영 자연스러움 | 배포 직후 1~2분 일시 폭증 |

봇 spam 한 번에 1시간 동안 30만 라인 폭발 가능. 디스크 / log aggregation 비용 모두 폭발 + 진짜 사고 (T-032 같은 unhandled) 의 ERROR 가 묻혀버림.

### 그렇다고 "JWT 인증 실패율" 가시성을 포기하는 건 아님 — 메트릭 채널

운영자 입장에선 **"오늘 JWT 인증 실패 추세"** 보고 싶음. 보안 사고 (공격 시도) 감지의 1차 신호. 그래서 채널 분리:

| 채널 | 표현 단위 | 적합한 use case |
|---|---|---|
| **로그 (log.warn)** | 줄 단위 — 시각 + 컨텍스트 + stack trace | **저-volume** + 사고 시점 진단 |
| **메트릭 (Counter)** | rate / 누적 카운트 — 줄 X 숫자 | **high-volume** + 추세 / 알람 |

JWT 인증 실패는 메트릭이 정답:

```java
// (도입 시점 예상 코드 — Micrometer)
@Component
class JwtAuthMetrics {
    private final Counter failures;
    JwtAuthMetrics(MeterRegistry r) {
        this.failures = Counter.builder("auth.jwt.failure")
            .tag("reason", "expired|malformed|invalid_signature")
            .register(r);
    }
}

// JwtAuthenticationFilter 안
catch (ExpiredJwtException e)    { metrics.failures("expired").increment(); ... }
catch (MalformedJwtException e)  { metrics.failures("malformed").increment(); ... }
catch (SignatureException e)     { metrics.failures("invalid_signature").increment(); ... }
```

이러면 Prometheus 가 1초에 한 번 카운터를 긁어서 **시계열 그래프** + **분당 rate 계산** + **알람 룰** ("분당 100건 초과 시 Slack 알림") 까지 가능. 로그 줄 1개도 안 적고도 가시성 완전.

졸업프로젝트 현재 상태:
- 메트릭 도입 안 함 (PR 6 Flyway / PR 4-final 같은 인프라가 더 시급).
- JwtFilter 의 catch 는 docstring 으로 "의도적 침묵 + 운영 시 메트릭 도입 권유" 명시 — 미래의 자신 / 다음 sweep 가 잘못 fix 안 하게 방어.

### 일반화 — Volume × 가시성 trade-off 매트릭스

| Volume | 채널 | 결정 |
|---|---|---|
| **저 (분당 0~10건)** | 로그 | **catch 마다 log 박기** — 가시성 0 이 사고 |
| **중 (분당 10~100건)** | 로그 + 샘플링 (1/N) | 부분 로그 + 메트릭 병행 |
| **고 (분당 100건 이상)** | 메트릭 단독 | **로그 절대 X** — floods 위험 |

분기점은 절대값 아닌 **인프라 처리 능력 대비**. 우리 EC2 + docker logs 는 분당 수십 줄까진 무관, 수백 줄부터 디스크 / grep 성능 영향, 수천 줄이면 log aggregation 비용 폭발.

### 발표 시 한 줄

> "JwtFilter 의 catch 는 log 안 박았는데, 이게 버그가 아니라 의도적입니다. 모든 인증 요청을 거치는 high-volume path 라 log 박으면 봇 spam 만으로 분당 수천 라인 폭발해서 디스크 / grep 둘 다 망가집니다. 대신 운영 모니터링이 필요해지면 Micrometer 카운터로 옮기는 게 정답이라 docstring 에 그 권유까지 박아뒀습니다. 같은 silent catch 패턴이라도 path 의 volume 에 따라 fix 방향이 정반대 — 이게 T-033 sweep 의 핵심 교훈입니다."

### 청중 Q&A 대비

**Q. JwtFilter 가 침묵이면 공격자가 우리를 두드리는지 어떻게 알아요?**
> 단일 요청은 모릅니다. 하지만 메트릭 카운터 도입 후엔 rate 가 보이고, 비정상 spike (평소 분당 10건 → 갑자기 5000건) 를 알람 룰로 잡을 수 있어요. 개별 라인 log 가 아니어도 패턴은 보입니다. 그리고 만약 정말 의심스러운 공격을 깊이 분석하고 싶으면, 그 시점에만 임시로 DEBUG 로그를 활성화하는 방법도 있고요.

**Q. T-032 의 generic Exception 핸들러도 silent 였는데 왜 fix 했나요? 똑같지 않나요?**
> 핵심은 **volume**. T-032 는 generic Exception 핸들러 — 우리 코드의 **버그** 가 발생한 분류 안 된 예외만 거기 떨어집니다. 분당 0~1건. log 박아도 floods 위험 0. 반면 가시성 0 은 운영 진단 불가능. JwtFilter 는 거꾸로 — log 박으면 floods, 침묵이어도 메트릭으로 가시성 확보 가능. **같은 모양 다른 trade-off** 라 fix 방향이 정반대입니다.

**Q. 그럼 모든 catch 에 sweep 들어가서 fix 후보를 어떻게 판별하나요?**
> 두 질문이면 됩니다 — (1) "이 catch 는 어느 volume 인가?" (저 / 중 / 고). (2) "가시성 0 이 운영 사고로 이어지는가?". 답에 따라 4가지 분기:
> - 저 + 가시성 필요 → log 박음 (T-032 본인)
> - 저 + 가시성 불필요 → 그대로 (이미 처리됨)
> - 고 + 가시성 필요 → **메트릭 채널** + docstring 명시 (JwtFilter)
> - 고 + 가시성 불필요 → 그대로 + docstring 만 (드문 케이스)
>
> 카테고리 식별이 sweep 의 1단계, fix 패턴 적용이 2단계.

**Q. 로그와 메트릭 외에 다른 채널은 없나요?**
> 있어요 — **분산 트레이싱 (OpenTelemetry / Zipkin)** 은 요청 단위로 span 을 만들어서 분기 / 지연 / 에러 다 추적. log 처럼 줄마다 풀어쓰지 않고 요청 라이프사이클 단위로 압축. high-volume 경로의 가끔 발생하는 latency spike 같은 거 잡을 때 적합. 졸업프로젝트 범위 초과지만, 운영 서비스 키우면 도입 후보.

### 코드 위치
- `src/main/java/hoseo/moodiary/security/JwtAuthenticationFilter.java` — high-volume 의도적 침묵의 정확한 예시. docstring 에 사유 + 메트릭 권유 명시.
- `src/main/java/hoseo/moodiary/exception/GlobalExceptionHandler.java#handleException` — 저-volume + 가시성 필요 → `log.error` 박힌 fix (T-032).
- `src/main/java/hoseo/moodiary/service/AiResponseService.java#triggerAsync` — async background 의 silent → `log.warn` 박힌 fix (T-033 sweep).

### 관련 노트
- [10번. Spring 비동기](#10-spring-비동기-enableasync--async--동기-블로킹-회피--db-상태머신--race-방지) — `@Async` 안의 catch 도 silent 후보. T-033 sweep 에서 fix.
- [13번. OAuth2 Token-Exchange](#13-oauth2--token-exchange-패턴--audience-검증-confused-deputy--email_verified-위임의-한계) — HttpGoogleOAuth2Provider 의 cause chain 손실 (silent 의 다른 변종) 도 같은 sweep 에서 fix.

---

## 15. Mixed Content — HTTPS 페이지의 HTTP API 호출은 브라우저가 *요청 전에* 차단 (Vercel rewrites 가 빠른 해결책)

### 한 줄 요약
> **HTTPS 페이지에서 HTTP API 를 fetch 하면 브라우저가 요청을 보내기 전에 차단한다 (Active Mixed Content).** 서버는 영원히 그 요청을 받지 않으니 서버 측 로그/CORS/방화벽으로는 절대 진단되지 않는다 — "왜 EC2 로그에 아무것도 안 찍히지?" 가 결정적 단서. 빠른 해결책은 BE HTTPS 가 아니라 **FE 의 same-origin reverse-proxy** (Vercel rewrites / Netlify redirects / Next.js rewrites).

### 문제 — 우리 프로젝트의 정확한 시나리오

졸업 데모를 위해 FE 가 S3 대신 **Vercel** 로 먼저 배포 시도. 회원가입 호출이 브라우저에서 차단되며 "왜 차단되는지" 가 한참 안 잡힘.

| 측 | URL | 프로토콜 |
|---|---|---|
| FE 페이지 | `https://moo-diary-ten.vercel.app` | **HTTPS** (Vercel 자동) |
| BE API | `http://15.165.95.129:8080/auth/login` | **HTTP** (EC2 + Docker, HTTPS 미구성) |

Vercel / Netlify / Cloudflare Pages / Render 등 모던 PaaS 는 **자동으로 HTTPS** 를 붙인다 (Let's Encrypt 백그라운드). 의도 없이 HTTPS 가 자동으로 활성화됨. BE 는 의도적으로 HTTP-only — 도메인/인증서 작업이 졸업 범위 초과라.

이 두 결정이 합쳐지면서 **Mixed Content** 가 발생.

### 메커니즘 — 왜 EC2 로그에 아무것도 안 찍히나

모던 브라우저 (Chrome 80+, Firefox 23+, Safari 9+) 의 **Mixed Content Blocker**:

1. HTTPS 로 로드된 페이지가 그 안에서 HTTP 리소스 요청을 시도한다.
2. 브라우저가 **요청을 네트워크에 보내기 전에 차단**한다.
3. DevTools Console 에 `Mixed Content: The page at 'https://...' was loaded over HTTPS, but requested an insecure XMLHttpRequest endpoint 'http://...'. This request has been blocked` 비슷한 메시지.
4. **EC2 (=서버) 에는 패킷 자체가 도달하지 않는다** — accesslog / docker logs / nginx 어디에도 그 요청의 흔적 없음.

이 "서버 측 로그 0" 신호가 결정적이다 — **CORS preflight 거절** 이나 **방화벽 차단** 이면 OPTIONS / TCP SYN 정도는 서버까지 도달해서 로그가 남는다. Mixed Content 는 **클라이언트 단 한 단계 위**에서 죽는다.

### Active vs Passive — 왜 fetch 만 죽고 이미지는 살아남는가

브라우저는 mixed content 를 두 종류로 분류한다:

| 종류 | 예시 | 처리 |
|---|---|---|
| **Active** | `fetch()`, `XHR`, `<script>`, `<iframe>`, `<link rel=stylesheet>` | **자동 차단** (사용자 개입 불가) |
| **Passive** | `<img>`, `<audio>`, `<video>` (poster 제외) | **경고만** 띄우고 통과 (브라우저별 다름) |

API 호출은 active — XHR/fetch 라 무조건 차단. 이미지를 HTTPS 페이지에서 HTTP 로 띄우는 건 가능하지만 (deprecated 흐름 중) API 는 절대 불가.

> Active 만 막는 이유: passive 리소스가 변조되면 시각적 사기 정도지만, active 가 변조되면 페이지 DOM / 쿠키 / 토큰 / 비밀번호 전부 노출 → 위협 수준이 다르다.

### 왜 CORS preflight 거절과 헷갈리는가

증상 모양이 비슷해 보인다:
- DevTools 의 Network 탭에서 요청이 "failed"
- 서버에서 200/400/401 응답 본 적 없음
- Console 에 빨간 에러

차이:

| 단계 | CORS 실패 | Mixed Content 차단 |
|---|---|---|
| OPTIONS preflight | 브라우저가 보냄 → 서버 도달 | **안 보냄** — 차단이 더 앞 단계 |
| 서버 accesslog | OPTIONS 흔적 있음 | **완전 비어있음** |
| Console 메시지 | `CORS policy: No 'Access-Control-Allow-Origin'...` | `Mixed Content: ... has been blocked` |
| Fix 채널 | 서버 (`@CrossOrigin` / `CorsConfig`) | 클라이언트 측 (URL 자체) |

이 프로젝트의 CORS 는 이미 잡혀있다 ([12번. CORS](#12-cors--same-origin-policy--브라우저-차단-메커니즘--preflight--allowlist-vs-와일드카드)). EC2 보안 그룹도 8080 열려있다. 그런데 차단됐다 — CORS / 방화벽 가설을 버리고 Mixed Content 로 좁히는 게 진단의 핵심 분기점.

### 해결 옵션 비교

| 옵션 | 작업량 | 비용 | 졸업 범위 | 지속성 |
|---|---|---|---|---|
| **A. FE 의 reverse-proxy** (Vercel rewrites / Next.js rewrites / Netlify redirects) ⭐ | 작음 — `vercel.json` 1줄 + API base URL 변경 | 0 | ✅ 내 | 데모용 충분 |
| **B. BE 에 HTTPS** (EC2 + nginx + Let's Encrypt + 무료 도메인 (Duck DNS)) | 중 — BE 인프라 반나절 | 0 | 경계 (인프라 학습 +) | 영구 |
| **C. CloudFront 앞단 HTTPS** | 중 — AWS 리소스 + 도메인 | 도메인 ~만원/년 | 경계 | 영구 |
| **D. FE 를 S3 (HTTP) 로** — 원래 plan 복귀 | 중 — FE 재배포 | 0 | ✅ 내 | 데모용 |

### ⭐ 추천 — A. FE Reverse Proxy

**왜**: 가장 빠르다. BE / DNS / 인증서 작업 전부 회피. **CORS 도 자동 해결** (브라우저 입장에서 same-origin 이 되니까 SOP 통과 + preflight 미발생).

#### 메커니즘

```
[브라우저 https]
    ↓ fetch("/api/auth/login")   ← 같은 도메인 (same-origin, mixed 아님)
[Vercel 엣지 서버]
    ↓ proxy http://15.165.95.129:8080/auth/login   ← 서버 간 통신, 브라우저 무관
[EC2 BE]
```

브라우저 입장에서는 `https://moo-diary-ten.vercel.app/api/auth/login` 으로만 보임 — same-origin HTTPS. Mixed Content / CORS 둘 다 적용 대상 아님. Vercel 의 엣지 노드가 HTTPS 받아서 BE 로 HTTP 프록시 — 서버 간 통신이라 브라우저 Mixed Content 정책 무관.

#### FE 설정 (Vercel — `vercel.json` 레포 루트)

```json
{
  "rewrites": [
    { "source": "/api/:path*", "destination": "http://15.165.95.129:8080/:path*" }
  ]
}
```

#### FE 의 API 호출 base URL

| 변경 전 | 변경 후 |
|---|---|
| `http://15.165.95.129:8080/auth/login` | `/api/auth/login` |
| `http://15.165.95.129:8080/post` | `/api/post` |

`.env` 의 `VITE_API_BASE_URL` (또는 비슷한) 을 `/api` 로.

#### BE 측 작업 — 사실상 없음

CORS allowlist 에 `https://moo-diary-ten.vercel.app` 추가는 권장 (rewrites 안 거치는 경로 대비). 다만 rewrites 만 쓰면 BE 입장에서 origin 이 EC2 자기 자신이라 CORS preflight 자체가 안 일어남.

### 일반화 — 다른 PaaS 도 같은 패턴

| PaaS | 설정 파일 | 키 |
|---|---|---|
| Vercel | `vercel.json` | `rewrites` |
| Netlify | `netlify.toml` 또는 `_redirects` | `[[redirects]]` |
| Cloudflare Pages | `_redirects` 또는 Functions | `proxy` |
| Next.js (어디서든) | `next.config.js` | `rewrites()` (서버 컴포넌트) |
| Vite (개발 모드만) | `vite.config.ts` | `server.proxy` |

**핵심**: 어느 PaaS 든 "FE 호스팅 도메인에서 BE 로 server-side proxy" 를 한 줄이면 설정. 브라우저는 same-origin 만 봐서 mixed content / CORS 무관.

### 발표 시 한 줄 비유

> "안전한 학교 (HTTPS 페이지) 안에 들어와 있는 학생 (브라우저) 이 학교 밖 안전 안 보장 (HTTP) 으로 편지 보내려는 걸 경비원 (Mixed Content Blocker) 이 우편함에 넣기 전에 가로채는 격. 학교 직원 (서버) 은 그 편지가 오려고 했다는 사실조차 모름."

### Q&A 대비

**Q. Mixed Content 와 CORS, 둘 다 브라우저가 차단하는 거 아냐? 뭐가 달라?**
A. 적용 시점이 다름. Mixed Content 는 **요청 직전 (네트워크 호출 전)**, CORS 는 **응답 검사 (서버까지 갔다가 응답 받은 뒤)**. 그래서 서버 로그에 흔적이 남으면 CORS, 안 남으면 Mixed Content 후보. 또 Mixed Content 는 **프로토콜 (https↔http)** 차이가 트리거, CORS 는 **origin (호스트:포트)** 차이가 트리거 — 같은 프로토콜이어도 다른 도메인이면 CORS, 같은 도메인이어도 다른 프로토콜이면 Mixed Content.

**Q. 왜 단순 reverse-proxy 가 CORS 까지 해결하지?**
A. 브라우저는 "사용자가 본 페이지의 origin" 과 "fetch 가 가는 URL 의 origin" 을 비교. reverse-proxy 를 쓰면 fetch URL 이 `/api/...` 라 **같은 도메인** 으로 분류되고 SOP 통과 → preflight 자체가 안 일어남. 브라우저는 Vercel 의 엣지가 백엔드로 어디로 프록시하든 모름 — 그건 서버 간 통신이라 브라우저 정책 범위 밖.

**Q. 그냥 BE 에 HTTPS 붙이는 게 정도 아니야? 왜 우회를 추천?**
A. BE HTTPS 는 옳다 — 운영 영구 운영할 거면 무조건. 하지만 졸업 데모 마감 시점에 BE HTTPS 작업 (도메인 등록 / DNS / Let's Encrypt / nginx 또는 ALB+ACM / Spring 측 HTTPS 또는 reverse-proxy) 은 **인프라 학습 자체로 큰 비중**이라 졸업 범위 분배상 비효율. 빠른 데모 통과 → 졸업 종료 후 시간 남으면 BE HTTPS 로 정도화가 trade-off 우위.

**Q. Vercel rewrites 도 결국 Vercel 엣지가 EC2 로 HTTP 요청을 보내잖아 — 보안 안 좋은 거 아냐?**
A. 맞다. **사용자 ↔ Vercel 까지는 HTTPS (TLS 1.3) 로 안전**, **Vercel ↔ EC2 사이는 HTTP** — 이 구간은 평문. AWS 내부망 또는 같은 리전 같으면 위험이 줄지만, 완전한 보안은 BE 도 HTTPS 가 정도. **데모용** 으로는 충분하지만 운영 정착 시점에 BE HTTPS 또는 BE 와 Vercel 사이 별도 mTLS / 사설 네트워크 검토. 졸업 데모 외 운영 단계 가면 옵션 B / C 로 이전.

**Q. 우리 plan.md 에 "프론트 = S3 only" 라고 명시한 결정이 왜 깨졌어?**
A. 그 결정의 의도가 정확히 이걸 회피하려던 것. S3 정적 호스팅은 HTTP 라 두 쪽 다 HTTP → Mixed Content 미발생. **Vercel 의 자동 HTTPS 가 그 가정을 깬 것이 본질** — FE 가 S3 셋업이 막혀서 Vercel 로 가는 우회 결정을 내릴 때 의사결정 로그의 컨텍스트가 흡수 안 됨. 의사결정 로그가 "왜 이렇게 정했는지" 만 적혀 있고 "FE 가 다른 호스팅 으로 갈 때는 …" 의 후속 조건이 없어서 깨짐. 일반 교훈: **결정 + 그 결정이 의존하는 가정** 둘 다 명시해야 결정이 흔들릴 때 알람이 울림.

### 코드 위치 — 이 프로젝트

이 함정은 **BE 코드가 아닌 FE 설정 + 의사결정 로그**가 위치. BE 측에는 변경 없음.

- `claude-docs/plan.md` 의 의사결정 로그 — "프론트 = S3 only (CloudFront/도메인 X)" 항목의 가정 (S3 = HTTP) 이 Vercel HTTPS 와 충돌함을 후속 sweep 에서 보강 필요.
- FE 레포 (별도) — `vercel.json` 의 rewrites 설정 + API base URL 변경.

### 관련 노트
- [12번. CORS](#12-cors--same-origin-policy--브라우저-차단-메커니즘--preflight--allowlist-vs-와일드카드) — Mixed Content 와 가장 자주 혼동되는 함정. 두 정책의 적용 시점 / 트리거 / fix 채널 비교 표 참조.
- [6번. dev 와 main](#6-dev-와-main-의-의미--왜-dev-머지로는-운영-반영-안-되나) — 인프라 결정이 한 쪽만 바뀌면 다른 쪽 가정이 깨지는 패턴 (BE 의 CD 트리거 가정 vs FE 의 호스팅 가정).

---

## 16. JPA `String` 컬럼의 기본 `VARCHAR(255)` 함정 + `ddl-auto:update` 는 기존 컬럼을 안 넓힌다

### 한 줄 요약
> JPA 에서 `String` 필드에 길이를 안 주면 컬럼이 **`VARCHAR(255)`** 로 매핑된다. 일기 본문처럼 길어질 수 있는 필드는 256자만 넘어도 `Data too long` 으로 INSERT 가 터지고, 매핑 핸들러가 없으면 **500** 으로 떨어진다. 게다가 `ddl-auto:update` 는 **기존 컬럼의 타입/길이를 바꾸지 않아서** 엔티티만 고쳐선 운영 DB 가 안 넓어진다 — 수동 `ALTER` 가 필요하다.

### 어떻게 터졌나 (FE 버그 제보)

```
짧은 일기 → POST /post → 201 정상
여러 줄(=긴) 일기 → POST /post → 500 {"message":"서버 오류가 발생했습니다."}
```

FE 는 "줄바꿈(\n) 처리 문제 아니냐" 고 의심했지만 **틀렸다**. `VARCHAR` 는 개행을 정상 저장한다. 진짜 원인은 **길이** — 여러 줄 일기가 길어서 255자를 넘긴 것. 짧은 글이 통과한 것이 단서.

### 왜 255인가 — JPA/Hibernate 의 기본값

```java
@Column(name = "post_content")   // length 미지정
private String content;          // → VARCHAR(255) 로 매핑됨
```

JPA 스펙상 `String` 의 기본 `length` 가 255. Hibernate 가 DDL 생성 시 `post_content VARCHAR(255)` 로 만든다. 256자가 들어오면:

```
H2:    Value too long for column "POST_CONTENT CHARACTER VARYING(255)": "...(485)"
MySQL: Data too long for column 'post_content'
→ DataIntegrityViolationException → 미핸들 → generic 500
```

### 본문성 필드 vs 짧은 필드

| 필드 종류 | 적정 매핑 |
|---|---|
| 제목 / 닉네임 / 이메일 | `VARCHAR(n)` — 255 이하로 충분 (`@Column(length=...)`) |
| **일기 본문 / 댓글 / 설명** | **`TEXT`** (`@Column(columnDefinition="TEXT")`) — 길이 가변 |

→ 엔티티 만들 때 본문성 필드마다 *"최댓값이 255를 넘을 수 있나?"* 를 자문하는 습관.

### 두 번째 함정 — `ddl-auto:update` 는 기존 컬럼을 안 바꾼다

엔티티를 `TEXT` 로 고쳐도 **이미 `VARCHAR(255)` 로 생성된 운영 컬럼은 그대로**다. `update` 모드는 "누락된 컬럼·테이블 추가" 만 하고, 기존 컬럼의 타입/길이 변경·`NOT NULL` 추가·인덱스 생성은 보장하지 않는다 ([8번 노트](#8-ddl-auto-update-의-한계--운영-머지-후-unique-인덱스-사후-검증이-필요한-이유)와 같은 결). 그래서 운영엔 수동 DDL 이 필수:

```sql
ALTER TABLE post MODIFY COLUMN post_content TEXT;
```

### 해결 — 두 겹 방어

1. **컬럼 확장**: `@Column(columnDefinition="TEXT")` — 긴 본문을 DB 가 받게.
2. **입력 검증**: DTO 에 `@Size(max=N)` — 한도 초과는 binding 단계에서 `MethodArgumentNotValidException` → **400** 으로 거른다. DB 도달 전에 막아 거대 페이로드 방어 + 의미 있는 메시지. (DB 제약 위반 500 을 입력 검증 400 으로 한 겹 앞당기는 패턴 — [14번/T-034](#14-silent-catch-의-두-얼굴--의도적-침묵-high-volume-vs-운영-사고-저-volume--메트릭-채널)의 "binding 단계 예외는 `@ControllerAdvice` 로 400" 철학과 같음.)

### 발표 / Q&A 한 줄
> "일기 본문이 `@Column` 길이 미지정이라 JPA 기본값인 `VARCHAR(255)` 로 매핑돼서, 긴 일기가 `Data too long` 으로 500 이 났습니다. 컬럼을 `TEXT` 로 넓히고 `@Size` 로 입력 단계에서 400 으로 거르는 두 겹으로 고쳤고, `ddl-auto:update` 가 기존 컬럼을 안 바꾸기 때문에 운영엔 `ALTER TABLE … MODIFY … TEXT` 를 따로 적용했습니다."

### 청중 Q&A 대비

**Q. 왜 처음부터 TEXT 로 안 했나요?**
> 엔티티 작성 시 `@Column` 에 length 를 의식적으로 지정하지 않으면 조용히 255가 적용된다. "기본값이 안전할 것" 이라는 가정이 함정. 본문성 필드는 명시적으로 TEXT 를 줘야 한다는 교훈.

**Q. `@Size(max=10000)` 와 컬럼 `TEXT` 중 하나만 있으면 안 되나요?**
> 역할이 다르다. `@Size` 없이 TEXT 만 있으면 64KB 까지 무방비로 받는다(거대 페이로드). TEXT 없이 `@Size` 만 있으면 컬럼이 여전히 255라 256~10000 구간이 DB 에서 터진다. 둘이 같이 있어야 "정상 범위는 저장, 비정상은 400" 이 완성.

**Q. `@Lob` 을 쓰면 안 되나요?**
> `@Lob` + `String` 은 MySQL 에서 `LONGTEXT`(4GB) 로 매핑되어 과하고, 일부 드라이버에서 스트리밍/인코딩 이슈가 있다. 일기 본문 정도는 `columnDefinition="TEXT"`(64KB) 가 명시적이고 충분.

### 코드 위치 — 이 프로젝트
- `src/main/java/hoseo/moodiary/entitiy/Post.java` — `content` 의 `columnDefinition="TEXT"` + 운영 ALTER 주석
- `src/main/java/hoseo/moodiary/dto/request/PostRequestDto.java` — `@Size`(title 255 / content 10000)
- `src/test/java/hoseo/moodiary/entitiy/PostContentLengthTest.java` — `@DataJpaTest` 600자 저장 회귀 테스트
- [troubleshooting T-036](./troubleshooting.md#t-036) — 같은 함정의 trap 기록

### 관련 노트
- [8번. ddl-auto 의 한계](#8-ddl-auto-update-의-한계--운영-머지-후-unique-인덱스-사후-검증이-필요한-이유) — `update` 가 인덱스/제약을 보장 안 하는 것과 동일한 뿌리: "update 는 추가만, 변경은 안 함". 컬럼 타입 변경도 같은 한계.
- [9번. utf8mb4](#9-utf8mb4-가-왜-중요한가--이모지-insert-폭발-시나리오) — 같은 "DB 컬럼 정의가 입력을 못 받아 INSERT 폭발 → 500" 계열의 함정 (charset vs length).

---

## 17. 403 "Invalid CORS request" 의 정체 — 서버가 던지는 CORS 거부 + origin 정확 문자열 매칭 + env 외부화 함정

### 한 줄 요약
> `403 Invalid CORS request` 는 **브라우저가 아니라 서버(Spring)가** 던지는 거부다 — 요청의 `Origin` 헤더가 서버의 허용 목록(allowlist)에 **정확히 일치하지 않을 때** Spring 이 응답을 200 대신 403 으로 끊는다. 매칭은 정규식이 아니라 **scheme + host + port 의 정확한 문자열 일치** — `https://` 와 `http://`, 끝의 `/` 하나가 달라도 불일치. 우리는 allowlist 를 코드가 아니라 **환경변수(`APP_CORS_ALLOWED_ORIGINS`)로 외부화**해서 운영 origin 을 EC2 `.env` 로만 관리한다.

### #12 와 무엇이 다른가 — "정책 개념" vs "이 에러 메시지"
[12번 노트](#12-cors--same-origin-policy--브라우저-차단-메커니즘--preflight--allowlist-vs-와일드카드)가 CORS 의 *원리*(SOP / preflight / 와일드카드)라면, 이 노트는 운영에서 실제로 마주친 **`403 Invalid CORS request` 한 줄의 정체**와 **추가가 안 먹는 함정들**에 초점.

### 어디서 던지나 — 브라우저 차단(#15)과 정반대
| | Mixed Content (#15) | CORS 403 (이 노트) |
|---|---|---|
| 누가 거부 | **브라우저** (요청 전) | **서버** (요청 받고 응답으로) |
| 서버 로그 | 0줄 (요청이 안 떠남) | **남음** (서버까지 도달 → 403 응답) |
| 진단 단서 | "서버에 흔적 없음" | "서버가 403 을 *준다*" |

→ FE 가 "서버까지 도달하지만 403" 이라고 보고한 게 정확히 이 신호. 서버가 응답을 *생성*했으니 Mixed Content(#15)가 아니라 CORS 거부다. 내부적으로 Spring 의 `DefaultCorsProcessor` 가 Origin 불일치 시 `rejectRequest()` 로 403 + 빈 본문을 쓴다(우리 앱은 본문이 `Invalid CORS request`).

### 핵심 함정 — origin 은 "정확한 문자열" 이어야 한다
브라우저가 붙이는 `Origin` 헤더는 **`scheme://host[:port]` 형태뿐** — 경로도, 끝 슬래시도 없다. allowlist 엔 이것과 **글자 단위로 같은** 값이 있어야 한다.

| allowlist 에 적은 값 | 브라우저 Origin `https://moo-diary-ten.vercel.app` 와 매칭? |
|---|---|
| `https://moo-diary-ten.vercel.app` | ✅ 일치 |
| `https://moo-diary-ten.vercel.app/` | ❌ 끝 슬래시 불일치 |
| `http://moo-diary-ten.vercel.app` | ❌ scheme(https↔http) 불일치 |
| `moo-diary-ten.vercel.app` | ❌ scheme 누락 |
| `https://moo-diary-ten.vercel.app/api/auth/login` | ❌ 경로 포함 불일치 |

`setAllowedOrigins(...)` 는 **정확 일치**, 와일드카드(`*.vercel.app`)가 필요하면 `setAllowedOriginPatterns(...)` 를 써야 한다. 단 와일드카드 `*` 는 `allowCredentials=true` 와 충돌(브라우저가 거절)해서 우리는 정확 URL 만 쓰는 정책([12번](#12-cors--same-origin-policy--브라우저-차단-메커니즘--preflight--allowlist-vs-와일드카드) 참조). → Vercel **프리뷰 배포**(`...-git-브랜치-xxx.vercel.app`)는 production 과 다른 호스트라 **별도 origin 으로 추가**해야 한다.

### 두 번째 함정 — allowlist 를 env 로 외부화 → "환경변수 운영" 의 덫
허용 origin 을 코드에 하드코딩하지 않고 `APP_CORS_ALLOWED_ORIGINS` (콤마 구분) 로 빼면, 운영 origin 변경에 **재배포·코드 변경이 필요 없다**(EC2 `.env` 수정 + 컨테이너 재생성으로 끝). CD 가 `.env` 를 덮어쓰지 않아 값이 영속. 대신 env 운영 특유의 함정 2개:

1. **`.env` 에 키가 *없으면* 조용히 기본값으로 동작** — `compose.yaml` 의 `${APP_CORS_ALLOWED_ORIGINS:-http://localhost...}` 처럼 `:-` default 가 있으면, 키 누락 시 에러 없이 **localhost 만 허용** → 운영 origin 전부 차단되는데 로그엔 아무 경고도 없다. "빈 값 ≠ 미설정" 의 또 다른 얼굴.
2. **쉘 프롬프트에 `KEY=val` 친 건 파일 수정이 아니다** — 터미널에서 `APP_CORS_ALLOWED_ORIGINS=...` 를 그냥 엔터 치면 그 **세션에만 사는 쉘 변수**가 만들어질 뿐 `.env` 파일은 그대로다. 파일에 넣으려면 `echo 'KEY=val' >> .env` 또는 에디터로 써야 한다.
3. **env 는 컨테이너 *시작 시점*에만 읽힌다** — `.env` 를 고쳐도 돌고 있는 컨테이너는 옛 값을 들고 있다. `docker-compose up -d` 로 재생성해야 반영.

### 검증 한 방 — preflight 모사
```bash
curl -i -X OPTIONS http://localhost:8080/api/auth/login \
  -H "Origin: https://moo-diary-ten.vercel.app" \
  -H "Access-Control-Request-Method: POST"
# 응답에 Access-Control-Allow-Origin: https://moo-diary-ten.vercel.app 가 있으면 통과
```

### 발표 / Q&A 한 줄
> "`403 Invalid CORS request` 는 브라우저가 아니라 서버가 던지는 거부예요. Origin 헤더가 서버 허용 목록과 **정확한 문자열로 일치**해야 하는데 — scheme·끝 슬래시까지 — 그래서 origin 만 EC2 `.env` 의 `APP_CORS_ALLOWED_ORIGINS` 에 추가하고 컨테이너를 재생성해서 풀었습니다. 허용 목록을 코드가 아니라 환경변수로 외부화해서 재배포 없이 운영에서 origin 을 관리합니다."

### 청중 Q&A 대비
**Q. 이것도 브라우저가 막는 CORS 아닌가요?**
> CORS 의 *판단*은 브라우저가 하지만, `403 Invalid CORS request` 응답 자체는 **서버가 생성**한다. preflight(OPTIONS)나 실제 요청이 서버에 도달했고, 서버가 Origin 을 보고 "허용 안 됨" 으로 403 을 돌려준 것. 그래서 서버 로그에 흔적이 남는다 — 흔적이 0줄이면 그건 Mixed Content(#15) 쪽.

**Q. `https://...app` 와 `https://...app/` 가 정말 다르게 취급되나요?**
> 네. allowlist 매칭은 정규식·prefix 가 아니라 **정확 문자열 비교**. 브라우저 Origin 헤더엔 끝 슬래시·경로가 절대 안 붙으므로, allowlist 에 슬래시를 붙이면 영원히 불일치. 가장 흔한 "추가했는데 왜 안 되지" 원인.

**Q. 왜 코드에 안 박고 환경변수로 뺐나요?**
> origin 은 환경(로컬/운영/FE 호스팅 변경)마다 다른 **운영 설정**이지 비즈니스 로직이 아니다. 외부화하면 FE 가 S3→Vercel 로 바꿔도 BE 코드·재배포 없이 `.env` 한 줄로 대응. 12-factor 의 "config 를 코드에서 분리" 원칙.

### 코드 위치 — 이 프로젝트
- `src/main/java/hoseo/moodiary/config/CorsConfig.java` — `setAllowedOrigins` + `APP_CORS_ALLOWED_ORIGINS` 외부화 + 와일드카드 금지 주석
- `src/main/resources/application.yaml` — `app.cors.allowed-origins: ${APP_CORS_ALLOWED_ORIGINS:...}` 플레이스홀더
- `compose.yaml` — `APP_CORS_ALLOWED_ORIGINS=${APP_CORS_ALLOWED_ORIGINS:-http://localhost...}` (`:-` default 함정의 진원지)
- `src/test/java/hoseo/moodiary/config/CorsConfigTest.java` — preflight 허용/거부 테스트

### 관련 노트
- [12번. CORS](#12-cors--same-origin-policy--브라우저-차단-메커니즘--preflight--allowlist-vs-와일드카드) — SOP / preflight / 와일드카드 vs allowCredentials 의 원리. 이 노트는 그 원리가 만든 `403` 메시지와 운영 함정 편.
- [15번. Mixed Content](#15-mixed-content--https-페이지의-http-api-호출은-브라우저가-요청-전에-차단-vercel-rewrites-가-빠른-해결책) — "서버 로그 0줄 vs 403 이 옴" 으로 둘을 가르는 진단. FE 가 Vercel 로 가며 Mixed Content 를 먼저 풀자 그 다음에 이 CORS 403 이 드러난 순서.
- [6번. dev 와 main](#6-dev-와-main-의-의미--왜-dev-머지로는-운영-반영-안-되나) — "환경변수·인프라 설정은 코드 머지와 별개 채널" 이라는 같은 결.

---

## 18. 데이터 기밀성 = 암호화 계층 선택 — 전송 TLS / 저장 TDE / 앱 ALE, 그리고 ALE 만 검색 UX 와 트레이드오프

### 한 줄 요약
> "암호화를 했나/안 했나" 는 잘못된 질문. 암호화는 **데이터가 거치는 구간마다 다른 계층**으로 적용되고, 각 계층이 막는 위협이 다르다. 핵심 통찰: **전송 TLS·저장 TDE 는 검색 UX 를 안 해쳐서 거의 항상 켜고, 앱 레벨 ALE 만 검색·정렬과 충돌**하므로 "정말 그 위협이 있는가" 를 따져 선택적으로 쓴다.

### 세 계층 — 데이터의 일생으로 보기

```
사용자가 일기 작성
   │
   │  ① 전송 중(in transit)   ← TLS 가 보호 (도청·중간자)
   ▼
백엔드 서버 (메모리에선 평문 처리)
   │
   │  ② 저장하는 순간(at rest) ← TDE 가 보호 (디스크·백업·스냅샷 도난)
   ▼                          ← (ALE 를 쓰면 여기서 앱이 직접 암호화 → 검색이 깨짐)
DB 디스크에 저장
```

| | **TLS** (Transport Layer Security) | **TDE** (Transparent Data Encryption) | **ALE** (Application-Level Encryption) |
|---|---|---|---|
| 지키는 구간 | 이동 중 (in transit) | 저장 중 (at rest) | 저장 중 (at rest) |
| 암호화 주체 | 인프라 (ALB·CloudFront·nginx) | DB/스토리지 자동 (RDS 체크박스) | **우리 앱 코드** |
| 막는 위협 | 네트워크 도청·패킷 가로채기 | 디스크·스냅샷·백업 파일 도난 | DB 자체 유출 (DBA·SQL injection 이 봐도 암호문) |
| 검색·정렬·인덱스 | 영향 없음 | 영향 없음 (DB 가 메모리에선 평문) | ❌ **깨짐** |
| `https` 의 `s` | 이게 TLS (구 SSL) | — | — |

### 왜 ALE 만 검색 UX 를 죽이나
DB 에 `"x8Kf9a2..."` 암호문이 들어가면 DB 입장에선 의미 없는 글자다. `LIKE '%우울%'` 같은 부분일치, `ORDER BY`, 인덱스가 전부 무력화 → **전체 row 를 꺼내 하나씩 복호화 후 메모리에서 필터하는 풀스캔** → 데이터가 늘수록 성능이 선형으로 악화되고 검색 기능 자체가 사라진다. 우회 기법(blind index, deterministic 암호화)도 부분일치 불가·빈도 분석 취약 등 또 다른 비용을 만든다.

### Moodiary 의 판단 (교수 "일기 암호화하라" Q 대비)
일기 서비스의 핵심 UX 가 **키워드 검색·날짜 정렬**(우리 `PostSearchRepository` 가 정확히 이걸 한다)이라, ALE 를 쓰면 그 기능이 깨진다. 교수가 걱정한 "일기 외부 노출" 은 검색 UX 를 해치지 않는 **TLS(전송) + TDE(저장)** 계층으로 충분히 막을 수 있다 → **검색을 죽이는 ALE 대신 UX 중립적인 TLS·TDE 로 같은 기밀성 목표를 달성**하는 것이 합리적. 단, 현재 배포 상태에선 이 두 계층도 아직 미적용 — 브라우저↔백엔드가 평문 HTTP([15번 Mixed Content](#15-mixed-content--https-페이지의-http-api-호출은-브라우저가-요청-전에-차단-vercel-rewrites-가-빠른-해결책) 참조)라 **인프라 고도화 시 HTTPS 종단 + RDS 암호화로 채우는 게 다음 단계**다.

### 인증과 암호화는 다른 축
- **인증/인가**(JWT·소유권 검사) = "앱을 **경유**한" 접근을 막는다 (남의 일기 조회 차단).
- **암호화**(TLS·TDE·ALE) = "앱을 **우회**한" 데이터 접근을 막는다 (네트워크 도청·디스크 도난).
- 그래서 "인증 했으니 암호화 불필요" 는 틀린 말 — 막는 위협이 겹치지 않는다. 다만 위협 모델에 그 우회 경로가 현실적인지를 보고 비용 대비 효과로 계층을 고르는 것.

### 발표 / Q&A 한 줄
> "암호화는 단일 선택이 아니라 전송(TLS)·저장(TDE)·앱(ALE) 계층 선택입니다. 일기는 검색·정렬이 핵심 UX라 인덱스를 깨는 ALE는 비용이 크고, 같은 기밀성을 검색을 안 해치는 TLS·TDE로 달성하는 게 맞다고 보고 ALE를 의도적으로 배제했습니다."

### 코드 위치
- `src/main/java/hoseo/moodiary/repository/PostSearchRepository.java` — ALE 를 쓰면 깨졌을 `containsIgnoreCase`(키워드)·`orderBy`(정렬)
- `claude-docs/security.md` — 보안 정책·약점 정리 (HTTPS 미적용 트레이드오프)

### 관련 노트
- [15번. Mixed Content](#15-mixed-content--https-페이지의-http-api-호출은-브라우저가-요청-전에-차단-vercel-rewrites-가-빠른-해결책) — "브라우저↔백엔드 구간 TLS" 가 현재 왜 비어 있는지(HTTP)와 직결.
- [2번. SHA-256 해시](#2-sha-256-해시--db-에-hash-만-저장하는-이유) / [3번. BCrypt vs SHA-256](#3-bcrypt-vs-sha-256--같은-해시인데-왜-다른-알고리즘) — "해시(복원 불가)" 와 "암호화(복호화 가능)" 는 다른 도구. 비밀번호는 해시, 일기 본문 기밀성은 암호화 계층 문제.

---

## 19. JPA 더티 체킹 — `save()` 를 안 부르는데 UPDATE 가 나가는 이유 (영속성 컨텍스트)

### 한 줄 요약
> `@Transactional` 안에서 조회한 엔티티의 필드를 바꾸면, `repository.save()` 를 부르지 않아도 **트랜잭션 커밋 시점에 Hibernate 가 자동으로 UPDATE** 를 날린다. 비결은 영속성 컨텍스트가 조회 시점의 "스냅샷" 을 들고 있다가 커밋 직전에 현재 값과 비교(dirty checking)하기 때문.

### 우리 코드에서 — UPDATE SQL 이 어디에도 없다
`Post.update(...)` 도 `AiResponse.markDone(...)` 도 그냥 필드 대입만 한다:

```java
// PostService (개념)
@Transactional
public void update(UUID postId, UUID userId, PostRequestDto dto) {
    Post post = postRepository.findById(postId)...;   // 영속 상태로 조회
    post.update(dto.title(), dto.content(), dto.postDate());  // 필드만 바꿈 — save() 없음
}   // ← 메서드 끝 = 트랜잭션 커밋 = 이 순간 Hibernate 가 변경 감지 → UPDATE 발사
```

### 작동 원리 — 영속성 컨텍스트의 스냅샷

```
1. findById → DB row 를 읽어 엔티티 생성
   이때 영속성 컨텍스트가 "스냅샷"(읽은 직후의 값) 을 따로 복사해 둠
2. post.update(...) 로 title/content 변경 → 엔티티(원본)만 바뀜, 스냅샷은 그대로
3. 트랜잭션 커밋 → flush 발생
   Hibernate 가 [현재 엔티티] vs [스냅샷] 을 필드별로 비교
   → 바뀐 컬럼만 골라 UPDATE post SET title=?, content=? WHERE post_id=?
```

→ 핵심 개념 3개: **영속성 컨텍스트(Persistence Context)**, **1차 캐시**, **flush(변경을 SQL 로 내보내는 시점)**.

### 왜 우리는 setter 를 막고 명시 메서드만 두나 (CLAUDE.md 규칙의 진짜 이유)
엔티티에 `@NoArgsConstructor(access = PROTECTED)` + setter 금지 + `update()` / `markDone()` 같은 명시 메서드만 두는 이유가 바로 더티 체킹이다:
- setter 를 열어두면 어디서든 필드를 바꿀 수 있고, 그게 우연히 트랜잭션 안이면 **의도치 않은 UPDATE** 가 조용히 나간다.
- 변경 경로를 `update()` / `markDone()` / `markFailed()` 로 **좁히면**, "이 엔티티가 바뀌는 곳" 이 코드에서 명확해지고 도메인 규칙(상태 전이)을 강제할 수 있다.

### 함정 — detached 엔티티엔 안 먹힌다
더티 체킹은 **영속 상태(persistent)** 엔티티에만 동작한다. 트랜잭션 밖에서 조회했거나(`@Transactional` 없음), 영속성 컨텍스트가 닫힌 뒤(준영속/detached)의 엔티티는 필드를 바꿔도 UPDATE 가 안 나간다 → 이때는 명시적으로 `save()` 가 필요. "save() 안 했는데 왜 저장이 안 되지?" 의 단골 원인.

### 발표 / Q&A 한 줄
> "JPA 는 `@Transactional` 안에서 조회한 엔티티의 변경을 커밋 시점에 자동 감지해서 UPDATE 합니다(더티 체킹). 그래서 저희는 setter 를 막고 `Post.update()` 같은 명시 메서드로만 상태를 바꿔서, 변경 경로를 좁히고 의도치 않은 UPDATE 를 막습니다."

### 코드 위치
- `src/main/java/hoseo/moodiary/entitiy/Post.java` — `update()` (66번 줄), `@NoArgsConstructor(PROTECTED)`
- `src/main/java/hoseo/moodiary/entitiy/AiResponse.java` — `markDone()` / `markFailed()` 상태 전이 메서드

### 관련 노트
- [10번. Spring 비동기](#10-spring-비동기-enableasync--async--동기-블로킹-회피--db-상태머신--race-방지) — `@Async` 스레드가 `markDone()` 으로 PENDING→DONE 시키는 것도 더티 체킹으로 UPDATE 된다. 단 별도 트랜잭션 경계라 commit-후-trigger race 주의.

---

## 20. N+1 문제와 `FetchType.LAZY` — 그리고 FK 만 읽어 소유권 검사를 공짜로 만드는 트릭

### 한 줄 요약
> 연관 엔티티를 `EAGER` 로 두거나 목록을 돈 뒤 각 연관을 건드리면, 목록 쿼리 1번 + 각 행마다 연관 쿼리 N번 = **N+1 쿼리** 가 터진다. `LAZY` 로 미루면 안 건드린 연관은 쿼리가 안 나가고, 특히 **연관 엔티티의 ID(=FK) 만 읽을 땐 추가 쿼리조차 없다**(프록시가 FK 를 이미 들고 있음).

### N+1 이 터지는 모습

```
일기 목록 100개 조회: SELECT * FROM post WHERE user_id = ?   (1번)
for (Post p : posts) p.getUser().getNickname();  // 각 Post 마다
  → SELECT * FROM user WHERE user_id = ?          (100번)
= 총 101번 쿼리. 목록이 커질수록 폭발.
```

### 우리 코드 — `LAZY` 와 "FK 만 읽기" 트릭
`Post.java`:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)   // 즉시 User 조회 안 함
@JoinColumn(name = "user_id", nullable = false)
private User user;

public boolean isOwnedBy(UUID userId) {
    return this.user != null && this.user.getId().equals(userId);  // ★ ID 만 접근
}
```

`LAZY` 면 `post.getUser()` 는 진짜 User 가 아니라 **프록시(가짜 객체)** 를 돌려준다. 그런데 `post.getUser().getId()` 처럼 **PK(=FK 컬럼 user_id) 만** 읽으면, 그 값은 이미 `post` 행의 FK 컬럼에 있으니 **프록시가 추가 SELECT 없이 바로 답한다**. 닉네임·이메일 같은 다른 필드를 건드려야 비로소 User 조회 쿼리가 나간다.

→ 그래서 **소유권 검사(`isOwnedBy`)가 추가 쿼리 0번** 으로 동작한다. Post.java 51~53번 주석이 정확히 이 얘기.

### N+1 진짜로 풀어야 할 때의 해법
연관 엔티티의 다른 필드까지 목록에서 써야 하면 `LAZY` 만으론 N+1 이 재발 → 그땐:

| 기법 | 설명 |
|---|---|
| **fetch join** | `select p from Post p join fetch p.user` — 한 방에 JOIN 으로 가져옴 |
| `@EntityGraph` | 메서드에 어노테이션으로 fetch 그래프 지정 (JPQL 없이) |
| `@BatchSize` / `default_batch_fetch_size` | N번을 `IN (?,?,...)` 한 번으로 묶어 1+1 로 축소 |

### `LAZY` vs `EAGER` 기본값 함정
- `@ManyToOne` / `@OneToOne` 의 JPA 기본값은 **EAGER** → 무심코 두면 항상 연관을 즉시 조회 → N+1 의 온상. 그래서 우리는 둘 다 **명시적으로 LAZY** (Post.user, AiResponse.post).
- `@OneToMany` / `@ManyToMany` 기본값은 LAZY.

### 발표 / Q&A 한 줄
> "연관관계를 EAGER 로 두면 목록 조회에서 N+1 쿼리가 터져서 전부 LAZY 로 명시했습니다. 게다가 소유권 검사는 연관 User 의 ID(=FK) 만 읽기 때문에 LAZY 프록시가 추가 쿼리 없이 답해서, 인가 체크가 쿼리 비용 0으로 동작합니다."

### 코드 위치
- `src/main/java/hoseo/moodiary/entitiy/Post.java` — `@ManyToOne(LAZY)` + `isOwnedBy()` (51~75번)
- `src/main/java/hoseo/moodiary/entitiy/AiResponse.java` — `@OneToOne(LAZY)` post

### 관련 노트
- [21번. 조회 성능](#21-조회-성능--복합-인덱스-컬럼-순서--안정-정렬tiebreaker) — N+1 을 줄여도 인덱스가 없으면 각 쿼리가 풀스캔. "쿼리 수" 와 "쿼리당 비용" 은 다른 축.

---

## 21. 조회 성능 — 복합 인덱스 컬럼 순서 + 안정 정렬(tiebreaker)

### 한 줄 요약
> 우리 목록·캘린더 쿼리는 항상 `WHERE user_id = ? ORDER BY post_date(or created_at)` 꼴. 이걸 **한 인덱스로** 처리하려면 `(user_id, post_date)` 처럼 **등호 필터 컬럼을 앞, 범위/정렬 컬럼을 뒤** 로 두는 복합 인덱스가 필요하다. 그리고 정렬 키가 중복될 때 순서가 흔들리지 않도록 **유니크한 보조 키(tiebreaker)** 를 마지막에 붙인다.

### 복합 인덱스 컬럼 순서가 왜 중요한가
`PostSearchRepository` 주석의 권장: `post(user_id, post_date)`. B-Tree 인덱스는 **왼쪽 컬럼부터 정렬**되어 있다(leftmost prefix rule):
- `user_id` 로 등호 매칭 → 그 사용자 구간으로 점프
- 그 구간 안에서 `post_date` 가 이미 정렬돼 있음 → **정렬(ORDER BY)도 인덱스로 공짜**, 범위(from~to)도 구간 스캔

순서를 `(post_date, user_id)` 로 뒤집으면 → `user_id` 등호 필터에 인덱스를 제대로 못 타고, 정렬도 따로 해야 함. **"등호 먼저, 범위/정렬 나중"** 이 복합 인덱스 설계의 핵심 규칙.

> `ddl-auto: update` 는 인덱스 생성을 보장하지 않으므로([8번](#8-ddl-auto-update-의-한계--운영-머지-후-unique-인덱스-사후-검증이-필요한-이유)), 운영 트래픽이 늘면 이 복합 인덱스는 **수동 DDL** 로 박아야 한다. `EXPLAIN SELECT ...` 로 인덱스를 실제로 타는지 확인.

### 안정 정렬 — 왜 `id` 를 정렬 끝에 끼워넣나
`PostSearchRepository.orderSpecifiers()` 는 1차 키(`postDate`) 뒤에 `createdAt desc → id asc` 를 보조 키로 붙인다:

```java
orders.add(ascending ? primaryPath.asc() : primaryPath.desc());  // 1차: postDate
if (sortField != CREATED_AT) orders.add(post.createdAt.desc());   // 보조1
orders.add(post.id.asc());                                        // 보조2 (유니크 — 최종 tiebreaker)
```

**문제**: 같은 `postDate` 가 여러 건이면 DB 는 그들 사이 순서를 보장하지 않는다 → 새로고침마다 순서가 흔들리고, **페이징 도입 시 같은 글이 두 페이지에 나오거나 누락**(중복/유실)된다.
**해결**: 항상 유니크한 컬럼(`id`)을 정렬 맨 끝에 둬서 순서를 **결정적(deterministic)** 으로 만든다. 이게 stable sort 의 tiebreaker.

### 발표 / Q&A 한 줄
> "목록 쿼리가 `user_id` 등호 + `post_date` 정렬이라 `(user_id, post_date)` 복합 인덱스를 권장합니다 — 등호 컬럼을 앞에 둬야 정렬까지 인덱스로 처리됩니다. 그리고 같은 날짜가 여러 건일 때 순서가 흔들려 페이징이 깨지는 걸 막으려고 유니크한 `id` 를 정렬 마지막 tiebreaker 로 넣었습니다."

### 코드 위치
- `src/main/java/hoseo/moodiary/repository/PostSearchRepository.java` — 복합 인덱스 권장 주석(25~27번) + `orderSpecifiers()` tiebreaker(74~92번)
- `src/main/java/hoseo/moodiary/repository/CalendarRepository.java` — `post(user_id, created_at)` 권장 + 기간 범위 쿼리

### 관련 노트
- [8번. ddl-auto 의 한계](#8-ddl-auto-update-의-한계--운영-머지-후-unique-인덱스-사후-검증이-필요한-이유) — 인덱스가 자동 생성 안 되니 이 복합 인덱스도 수동 DDL 대상.
- [20번. N+1](#20-n1-문제와-fetchtypelazy--그리고-fk-만-읽어-소유권-검사를-공짜로-만드는-트릭) — "쿼리 수 줄이기" 와 "쿼리당 비용(인덱스)" 은 별개의 최적화 축.

---

## 22. UUID PK vs Auto-increment — ID 추측 방지의 대가는 인덱스 단편화

### 한 줄 요약
> 우리는 모든 엔티티 PK 를 `1,2,3` 자동증가가 아니라 **UUID**(`@UuidGenerator`)로 쓴다. 장점은 분산 환경 충돌 없음·ID 로 레코드 수/순서 추측 불가(보안). 대가는 16바이트라 인덱스가 크고, **랜덤 UUID 는 B-Tree 에 무작위로 삽입돼 페이지 분할(page split)·단편화로 INSERT 성능이 나빠진다**.

### 왜 Auto-increment 가 아니라 UUID 인가

| | Auto-increment (`1,2,3`) | UUID |
|---|---|---|
| 생성 주체 | DB (INSERT 시 채번) | 앱/Hibernate (INSERT 전 생성 가능) |
| 분산/병합 | 여러 DB 합칠 때 충돌 | 충돌 사실상 0 |
| 추측 가능성 | `/post/124` → 총 글 수·남의 글 ID 추측 | 추측 불가 (보안) |
| 크기 | 8바이트(BIGINT) | **16바이트** — 인덱스·FK 가 다 커짐 |

→ Moodiary 가 UUID 를 택한 이유는 주로 **ID 추측 방지(보안)** + 컨벤션 일관성. URL 에 `post_id` 가 노출돼도 다른 글을 못 찍는다.

### UUID 의 진짜 단점 — 인덱스 단편화 (면접 가점 포인트)
랜덤 UUID(v4)는 값이 무작위라 PK 인덱스(B-Tree)에 **아무 위치에나** 꽂힌다:
- 새 행이 인덱스 중간중간에 삽입 → 페이지가 꽉 차면 **page split**(쪼개기) 발생 → 디스크 단편화·캐시 효율 저하 → INSERT 성능 악화.
- Auto-increment 는 항상 **맨 뒤에만** append 돼서 이 문제가 없다.
- 그래서 **정렬 가능한(시간순) UUID** 인 **UUIDv7 / ULID** 가 등장 — 앞부분이 타임스탬프라 append-friendly 하면서 UUID 의 장점도 유지.

### 발표 / Q&A 한 줄
> "PK 를 UUID 로 둔 건 URL 로 남의 글 ID 나 전체 글 수를 추측하지 못하게 하려는 보안 목적이 큽니다. 단점은 16바이트라 인덱스가 크고, 랜덤 UUID 라 B-Tree 에 무작위 삽입돼 페이지 분할로 INSERT 성능이 떨어진다는 점이고, 규모가 커지면 시간순 정렬이 되는 UUIDv7/ULID 로 완화할 수 있습니다."

### 코드 위치
- 모든 엔티티 — `@Id @UuidGenerator UUID id` (`Post`, `User`, `AiResponse`, `RefreshToken`)
- 컬럼명 규칙 `<table>_id` (`post_id`, `ai_response_id` …)

### 관련 노트
- [21번. 조회 성능](#21-조회-성능--복합-인덱스-컬럼-순서--안정-정렬tiebreaker) — UUID 가 큰 만큼 복합 인덱스/FK 의 바이트 비용도 함께 커진다.

---

## 23. JPA 연관관계 매핑 — 1:1 단방향 + `@Enumerated(STRING)` 의 함정

### 한 줄 요약
> `AiResponse` 는 `Post` 와 **단방향 1:1** 이고 `post_id` 에 UNIQUE 를 걸어 "한 글당 한 응답" 을 DB 차원에서 보장한다. 상태 컬럼은 `@Enumerated(EnumType.STRING)` — **`ORDINAL`(정수 저장)을 쓰면 enum 순서를 바꾸는 순간 기존 데이터의 의미가 깨지는** 치명적 함정이 있어 거의 항상 STRING 을 쓴다.

### 1:1 을 단방향 + UNIQUE 로 둔 설계
`AiResponse.java`:

```java
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_ai_response_post_id", columnNames = "post_id"))
...
@OneToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "post_id", nullable = false)
private Post post;   // AiResponse → Post 단방향. Post 는 AiResponse 를 모른다.
```

- **단방향**: `AiResponse` 가 `Post` 를 가리키지만 `Post` 에는 `aiResponse` 필드가 없다 → **Post 도메인이 AI 모듈을 모르게** 해서 결합을 끊었다(관심사 분리). 그래서 cascade 도 엔티티에 안 걸고 서비스에서 `deleteByPost_Id()` 로 명시 호출.
- **1:1 정합성**: FK `post_id` 에 UNIQUE → 한 글에 AI 응답이 둘 생기는 걸 **DB 가 거부**. 앱 버그로 중복 trigger 돼도 두 번째 INSERT 가 막힌다.
- 1:1 은 보통 **주 테이블 vs 대상 테이블 중 어디에 FK 를 둘지** 가 설계 포인트인데, 여기선 "응답이 글에 종속" 이라 `ai_response` 쪽에 FK 를 뒀다.

### `@Enumerated(STRING)` vs `ORDINAL` — 면접 단골 함정
`AiResponseStatus { PENDING, DONE, FAILED }` 를 DB 에 어떻게 저장하나:

| | `ORDINAL` (기본값!) | `STRING` (우리 선택) |
|---|---|---|
| 저장 형태 | 정수 0,1,2 (선언 순서) | 문자열 `"PENDING"`,`"DONE"`,`"FAILED"` |
| enum 중간에 값 추가 시 | **재앙** — `{PENDING, RETRY, DONE}` 로 바꾸면 기존 DB 의 `1`(=옛 DONE)이 이제 RETRY 로 해석됨 | 안전 — 문자열이라 순서 무관 |
| 가독성 | DB 에서 숫자만 보임 | DB 에서 의미가 그대로 보임 |

→ `@Enumerated` **기본값이 ORDINAL** 이라 무심코 빠뜨리면 위 함정에 빠진다. 그래서 **항상 `EnumType.STRING` 명시**.

### 발표 / Q&A 한 줄
> "AI 응답은 글과 1:1이라 `post_id` 에 UNIQUE 제약을 걸어 한 글에 응답이 둘 생기는 걸 DB가 막습니다. 단방향으로 둬서 Post 도메인이 AI 모듈에 의존하지 않게 했고요. 상태 enum 은 `EnumType.STRING` 으로 저장합니다 — 기본값인 ORDINAL 은 enum 순서를 바꾸면 기존 데이터 의미가 깨지기 때문입니다."

### 코드 위치
- `src/main/java/hoseo/moodiary/entitiy/AiResponse.java` — `@Table(uniqueConstraints)`, `@OneToOne(LAZY)`, `@Enumerated(STRING)`
- `src/main/java/hoseo/moodiary/entitiy/AiResponseStatus.java` — `PENDING/DONE/FAILED`

### 관련 노트
- [8번. ddl-auto 의 한계](#8-ddl-auto-update-의-한계--운영-머지-후-unique-인덱스-사후-검증이-필요한-이유) — `uk_ai_response_post_id` UNIQUE 가 자동 생성 안 될 수 있어 운영 머지 후 `SHOW INDEX` 검증 필요.
- [10번. Spring 비동기](#10-spring-비동기-enableasync--async--동기-블로킹-회피--db-상태머신--race-방지) — 이 `status` enum 이 큐 없는 비동기의 상태 머신 역할.

---

## 🔄 누적 갱신

이 문서는 **새로운 질문 / 학습이 생길 때마다 추가**된다. 본인이 모르는 걸 묻고 알게 된 모든 기술 개념을 한 곳에 누적.

### 갱신 이력

| 일자 | 추가 항목 |
|---|---|
| 2026-05-27 | 초안 — Refresh Token (1, 4, 5) + SHA-256 (2, 3) + dev/main 흐름 (6) + SSM 자동화 (7) + ddl-auto 한계 (8) + utf8mb4 (9). PR 10 / PR 4-pre 운영 반영 직후 본인이 발표 / Q&A 준비하며 정리. |
| 2026-05-28 | 10번 추가 — Spring 비동기 (`@EnableAsync` + `@Async` + DB 상태머신 + race 방지). 발표 슬라이드 5번 "비동기 \| @EnableAsync + ThreadPoolTaskExecutor" 한 줄의 배경 이해 정리. |
| 2026-05-28 | 11번 추가 — AWS SSM Run Command (outbound polling + IAM role + send-command 한계 + wait/health check 안전망). 기존 7번이 "자동화 범위" 라면 11번은 "SSM 자체의 작동 원리" 로 각도 분리. |
| 2026-05-28 | 12번 추가 — CORS (SOP / Preflight / allowlist vs 와일드카드). 발표 슬라이드 21번 "보안 — CORS" 정책의 배경 이해. 본인이 "CORS가 뭐야?" 발화로 트리거. |
| 2026-05-28 | **10/11/12 → goospel.github.io 공개판 승격** — [spring-async-pattern](https://goospel.github.io/notes/backend/spring-async-pattern/) / [aws-ssm-outbound-polling](https://goospel.github.io/notes/ops/aws-ssm-outbound-polling/) / [cors-fundamentals](https://goospel.github.io/notes/backend/cors-fundamentals/). 4문 자격 통과 (일반화 가능 / 본인 이해 확립 / 같은 스택 누구나 만남 / 검색 키워드 유효) + release PR #66 직후 묶음 임계 (3개) 도달. 글로벌 CLAUDE.md PKM 파이프라인 첫 실 적용. 각 항목 헤더에 공개판 링크 박음. |
| 2026-05-28 | 13번 추가 — OAuth2 Token-Exchange + audience 검증 (confused deputy) + email_verified 위임의 한계. PR 12-final (Google OAuth2 실어댑터) 작업 직후 — 발표 / Q&A 의 핵심 후보 ("이미 Google 검증한 토큰을 왜 또 검증?" / "id_token vs access_token 차이" / "왜 BE redirect 흐름 안 씀?"). 일반화 가능성 매우 높음 — 다음 묶음 임계 도달 시 goospel.github.io 승격 후보. |
| 2026-05-29 | 14번 추가 — Silent catch 의 두 얼굴 (의도적 침묵 vs 운영 사고) + 메트릭 채널. T-033 sweep 직후 사용자의 "JwtFilter 의 의도적 침묵이 뭐냐" 발화로 트리거. **Volume × 가시성 trade-off 매트릭스** 라는 일반화 가능한 분류 박음. 발표 / Q&A 의 "로그 어떻게 관리?" / "관찰성 (observability) 어떻게?" / "보안 이벤트 추적?" 답할 토대. 일반화 가능성 매우 높음 — 다음 묶음 임계 도달 시 goospel.github.io 승격 후보. |
| 2026-05-29 | 15번 추가 — Mixed Content (HTTPS 페이지의 HTTP API 호출을 브라우저가 *요청 전에* 차단) + FE reverse-proxy (Vercel rewrites) 패턴. FE 가 S3 대신 Vercel 배포 시도하면서 자동 HTTPS ↔ EC2 HTTP 충돌로 발견. **EC2 로그에 0줄** 이 결정적 진단 단서. plan.md 의 "프론트 = S3 only" 결정이 의존하던 가정 (S3 = HTTP) 이 Vercel 자동 HTTPS 와 충돌해 깨짐 — "결정 + 의존 가정" 명시 일반 교훈도 박음. 일반화 가능성 매우 높음 (Vercel/Netlify/Cloudflare Pages 어디서나 같은 패턴) — **13 + 14 + 15 = 3개 묶음 임계 도달, 다음 release 직후 goospel.github.io 승격 후보**. |
| 2026-06-09 | 16번 추가 — JPA `String` 컬럼 기본 `VARCHAR(255)` 함정 + `ddl-auto:update` 가 기존 컬럼 타입을 안 바꿈. FE 의 "여러 줄 일기 저장 500" 버그 제보를 `@DataJpaTest` 로 재현해 길이 초과(줄바꿈 무관)로 확정. `TEXT` 확장 + `@Size` 두 겹 방어 + 운영 수동 ALTER. [T-036](./troubleshooting.md#t-036). 일반화 가능성 높음 (JPA/Hibernate 쓰는 모든 프로젝트의 본문성 필드 공통 함정) — **다음 묶음(16+...)으로 goospel.github.io 승격 후보**. |
| 2026-06-11 | 17번 추가 — `403 Invalid CORS request` 의 정체 (브라우저 아닌 **서버**가 던지는 거부 + Origin **정확 문자열** 매칭 + allowlist env 외부화 함정). FE 가 Vercel 배포 후 "서버까지 도달하지만 403" 보고 → EC2 `.env` 의 `APP_CORS_ALLOWED_ORIGINS` 에 Vercel origin 추가로 해소하며 정리. #12(CORS 원리)·#15(Mixed Content)와 "서버 로그 0줄 vs 403 옴" 진단 축으로 연결. env 함정 2건(키 누락 시 `:-` default 조용히 / 쉘 `KEY=val` 은 파일 아님)도 박음. 일반화 가능성 높음 — **다음 묶음으로 goospel.github.io 승격 후보**. |
| 2026-06-15 | **18~23번 묶음 추가 — 보안/DB 심화 6종.** 결과보고서(졸업 발표 자료) 작성 중 교수 Q&A 대비로 본인이 "ALE·TDE·TLS가 뭐냐" / "DB 공부할 거 뭐 있냐" 발화 → ① **18 암호화 계층**(전송 TLS/저장 TDE/앱 ALE, ALE만 검색 UX 트레이드오프 — #15 Mixed Content 와 cross-link), ② **19 JPA 더티 체킹**(영속성 컨텍스트·스냅샷, setter 막는 진짜 이유), ③ **20 N+1 + LAZY**(FK만 읽어 소유권 검사 공짜 트릭), ④ **21 복합 인덱스 컬럼 순서 + 안정 정렬 tiebreaker**, ⑤ **22 UUID PK vs auto-inc**(인덱스 단편화·UUIDv7), ⑥ **23 1:1 단방향 + `@Enumerated(STRING)` ORDINAL 함정**. ①은 일반화 가능성 매우 높음(모든 웹서비스 공통) — goospel.github.io 승격 1순위 후보. ②③④⑤⑥은 JPA/MySQL 스택 공통이라 묶음 승격 후보. 기존 #8·#16(ddl-auto)와 중복 회피해 새 개념만 박음. |
