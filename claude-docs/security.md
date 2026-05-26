# Moodiary Backend — 보안 정리

> 사용자(BE 담당자)가 "현재 우리 시스템의 보안이 어떻게 동작하는지" 한눈에 파악하기 위한 문서.
> **목적**: 졸업 발표/면접 때 질문 받았을 때 자신있게 답할 수 있도록.
> **갱신**: 보안 관련 코드 / 정책 / 시크릿 관리 방식이 바뀔 때마다.
>
> 📚 관련 문서:
> - [`plan.md`](./plan.md) — 우선순위 / 로드맵
> - [`api-contracts.md`](./api-contracts.md) — 에러 응답 포맷
> - [`troubleshooting.md`](./troubleshooting.md) — T-013, T-014 (secret 관리 사고들)

---

## 📑 목차

1. [한 줄 요약 + 한 그림](#-한-줄-요약--한-그림)
2. [JWT 인증](#-jwt-인증)
3. [비밀번호 관리](#-비밀번호-관리)
4. [JWT_SECRET 관리 (가장 중요)](#-jwt_secret-관리-가장-중요)
5. [인가 (Authorization) 규칙](#-인가-authorization-규칙)
6. [Spring Security 필터 체인](#-spring-security-필터-체인)
7. [에러 응답 표준](#-에러-응답-표준)
8. [알려진 약점 / 의도된 트레이드오프](#-알려진-약점--의도된-트레이드오프)
9. [절대 하지 말 것](#-절대-하지-말-것)

---

## 🎯 한 줄 요약 + 한 그림

**Stateless JWT** 인증 — 서버는 세션을 저장하지 않는다. 클라이언트가 매 요청마다 `Authorization: Bearer <토큰>` 헤더로 자신을 증명. 토큰은 서버 secret 으로 서명되어 위조 불가.

```
회원가입                로그인                   이후 요청
─────────              ──────                  ────────
POST /auth/signup      POST /auth/login        GET /post
{email,pw,nick}        {email,pw}              Authorization: Bearer <JWT>
       │                      │                       │
       ▼                      ▼                       ▼
  UserService           UserService              JwtAuthenticationFilter
  BCrypt.encode(pw)     BCrypt.matches(pw)       토큰 검증 (서명+만료)
  DB 저장 (해시만)       JwtTokenProvider         SecurityContext 에
                        .createAccessToken()     UUID 주입
                              │                       │
                              ▼                       ▼
                        {accessToken,userId}      Controller 에서
                                                  @AuthenticationPrincipal
                                                  UUID userId 로 받음
```

핵심: 비밀번호는 **DB 에 평문 절대 X (BCrypt 해시만)**. 토큰은 **서버 secret 으로 서명 (위조 불가)**. 세션 저장소 없음.

---

## 🔐 JWT 인증

### 토큰 안에 뭐가 들어있나
표준 JWT Claims 3개만:

| Claim | 의미 | 우리가 박는 값 |
|---|---|---|
| `sub` (subject) | 토큰 소유자 식별자 | 사용자 UUID (예: `9c4d401e-63ba-...`) |
| `iat` (issued at) | 발급 시각 | 발급 순간 (UNIX timestamp) |
| `exp` (expiration) | 만료 시각 | `iat + 24h` (기본) |

> ⚠️ **이메일이나 비밀번호는 토큰에 안 박는다**. JWT 의 payload 는 Base64 인코딩일 뿐 암호화 X — 누구든 디코딩 가능. 그래서 sub 에 UUID만 박고 민감 정보는 절대 X.

### 알고리즘 — JWT_SECRET 길이에 따라 자동 선택
jjwt 0.12 의 `Keys.hmacShaKeyFor(secret.getBytes())` 가 키 사이즈에 맞는 HMAC-SHA 알고리즘을 자동 선택한다:

| JWT_SECRET 바이트 길이 | 선택되는 알고리즘 |
|---|---|
| 32 ~ 63 바이트 | **HS256** |
| 64 ~ 95 바이트 | **HS384** |
| 96 바이트 이상 | **HS512** |

코드 위치: `JwtTokenProvider.java` 생성자.

> 🚨 **운영에서 어떤 알고리즘이 쓰이는지 확실하지 않다면 EC2 의 `.env` 의 `JWT_SECRET` 길이를 세 보고 위 표로 판단**. (코드 주석에는 "HS256" 으로 적혀있지만 이는 코드가 막 짜여졌을 시점 가정값일 뿐 — 실제는 위 자동 선택이 결정.)

### 만료 시간
- 기본: **24시간** (86,400,000 ms)
- 설정: `application.yaml` 의 `jwt.expiration-ms`, env var `JWT_EXPIRATION_MS` 로 override
- **Refresh token 없음** — 만료되면 그냥 재로그인. 졸업 데모 단순성.

### 발급 흐름 (login)
```
1. POST /auth/login { email, password }
2. UserService.login()
   ├─ repository.findByEmail() → User
   ├─ passwordEncoder.matches(평문, 해시) → true 여야 통과
   └─ jwtTokenProvider.createAccessToken(user.id)
3. 응답: { accessToken, userId }
```

코드 위치: `UserService.login()`.

### 검증 흐름 (모든 보호된 요청)
```
1. 요청에 Authorization: Bearer <token> 헤더 있음
2. JwtAuthenticationFilter.doFilterInternal()
   ├─ Bearer 접두사 떼고 token 추출
   ├─ jwtTokenProvider.getUserId(token)
   │   ├─ key 로 서명 검증 (위조면 JwtException)
   │   ├─ exp 검증 (만료면 ExpiredJwtException)
   │   └─ sub 에서 UUID 파싱
   └─ SecurityContext 에 UsernamePasswordAuthenticationToken 주입
       (principal = UUID, role = ROLE_USER)
3. Controller 에서 @AuthenticationPrincipal UUID userId 로 받음
```

코드 위치: `JwtAuthenticationFilter.java`.

**중요 설계 결정**: 토큰 검증 실패 시 **필터는 401 을 던지지 않고 그냥 통과**시킨다. 이유 — 화이트리스트(`/auth/**`, `/swagger-ui/**`) 도 같은 필터를 거치는데 토큰이 없거나 잘못됐다고 401 을 쏘면 화이트리스트가 동작 안 함. 보호된 자원이면 뒤의 `AuthorizationFilter` 가 알아서 401 로 거절.

---

## 🔑 비밀번호 관리

### 저장 방식
- **BCrypt** 해시. `BCryptPasswordEncoder` (Spring Security 기본). cost factor 10 (default).
- DB 컬럼: `users.password` = BCrypt 해시 60자 (예: `$2a$10$N9qo8uLOickgx2ZMRZoMye...`)
- **평문 비밀번호는 어디에도 저장 X** — 로그/응답에도 절대 안 나옴

### 정책
회원가입 시 Bean Validation:
- 이메일: `@Email` 형식 검증 + UNIQUE
- 비밀번호: 정규식 `^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$`
  - 영문 1자 이상 + 숫자 1자 이상 + **8자 이상**
  - **특수문자 불가** (영문/숫자만 허용)
  - 위반 시 400 + `{"message":"비밀번호는 영문과 숫자를 포함한 8자 이상이어야 합니다."}`
- 닉네임: 2~20자, UNIQUE

코드 위치: `UserSignupRequestDto.java`.

### 로그인 실패 시 — 열거 공격(Enumeration) 방지
이메일이 없든, 비밀번호가 틀리든 **동일한 401** + **동일한 메시지** 반환:
```json
{"message": "이메일 또는 비밀번호가 올바르지 않습니다."}
```

**왜 이게 보안인가**: 만약 "이메일 없음" 과 "비밀번호 틀림" 을 구분해서 응답하면, 공격자가 `/auth/login` 으로 임의 이메일을 던져서 우리 DB 에 가입된 이메일 명단을 추출 가능. 401 메시지 통일로 차단.

코드 위치: `UserService.login()` — 두 케이스 모두 `throw new InvalidCredentialsException()`.

---

## 🔒 JWT_SECRET 관리 (가장 중요)

> **이 섹션이 깨지면 시스템 전체 보안 의미 없음**. JWT_SECRET 이 유출되면 공격자가 임의 사용자 흉내내는 토큰을 자유롭게 발급 가능.

### 어디에 저장되는가
| 환경 | 저장 위치 | 비고 |
|---|---|---|
| 로컬 dev | `application.yaml` 의 기본값 `dev-local-secret-CHANGE-ME-...` | 64자 이상 (HS512 자동 선택) — 충돌 방지용 더미. 운영 절대 사용 X |
| GitHub Actions (CI/CD) | GitHub Secret `JWT_SECRET` | Actions 가 운영 컨테이너 띄울 때 env 로 주입 |
| 운영 (EC2) | `/home/ec2-user/.env` 의 `JWT_SECRET=...` | docker compose 가 자동 로드 → 컨테이너 환경변수로 들어감 |

### Spring 이 어떻게 읽나
`application.yaml`:
```yaml
jwt:
  secret: ${JWT_SECRET:dev-local-secret-CHANGE-ME-in-production-min-32-bytes-required!}
```

`${ENV_VAR:기본값}` 패턴 — 환경변수가 있으면 그 값 사용, 없으면 기본값. 운영은 env var override 로 진짜 시크릿이 들어가고, 로컬은 기본값(더미)으로 동작.

### 절대 하지 말 것
- ❌ `JWT_SECRET` 을 git 에 커밋 (코드, yaml, `.env`, README 등 어디든)
- ❌ Slack / 이메일 / 카톡으로 공유 (사용자 한정. GitHub Secret + AWS 만)
- ❌ 운영 yaml 기본값을 진짜 비밀로 박기 (그러면 git 추적 = 누출)
- ❌ HS256 미만 길이로 줄이기 (32 바이트 미만은 jjwt 가 부팅 거부)

### 과거 사고 (절대 반복하지 말 것)
- **T-013**: `application.yaml` 자체가 `.gitignore` 되어 있어 운영에 설정 누락 → JWT 만료시간이 0 으로 들어가 발급 즉시 만료. 해결 = yaml 추적 + env var override 패턴.
- **T-014**: CD 워크플로우의 `docker run` 에서 `-e JWT_SECRET=...` 라인이 누락 → yaml 기본값(`dev-local-secret-...`) 으로 운영 부팅. 누구나 토큰 위조 가능 직전. 다행히 release 직전 점검 중 발견.

→ 자세히는 [`troubleshooting.md`](./troubleshooting.md) T-013, T-014.

### Secret 교체 절차 (유출 의심 시)
1. 새 secret 생성 (예: `openssl rand -base64 64`)
2. GitHub Secrets `JWT_SECRET` 값 갱신
3. EC2 `.env` 의 `JWT_SECRET` 값 갱신 (`ssh` 접속)
4. `docker compose pull && docker compose up -d` 로 재배포
5. **결과**: 기존 발급된 모든 토큰 무효화 (서명 키 바뀜) → 모든 사용자 재로그인 강제

---

## 🚦 인가 (Authorization) 규칙

### 화이트리스트 — 토큰 없이 호출 가능
`SecurityConfig.WHITELIST`:
- `/auth/**` — 회원가입/로그인 (인증 자체가 불가능하니까)
- `/swagger-ui/**`, `/swagger-ui.html` — Swagger UI
- `/v3/api-docs/**` — OpenAPI spec (FE 의 TypeScript 자동 생성용)
- `/swagger-resources/**` — Swagger 리소스
- `/error` — Spring 기본 에러 디스패치 (막으면 예외 페이지도 401 되어 디버깅 불가)

### 그 외 모든 엔드포인트 — `authenticated()`
토큰 없으면 401:
```json
{"message": "인증이 필요합니다."}
```

코드 위치: `SecurityConfig.securityFilterChain()`.

### 소유권 격리 — "본인 글만"
Post 도메인 핵심 규칙:
- `POST /post` — 작성자 = 현재 인증된 사용자 자동 채움 (클라이언트가 user_id 박을 수 없음)
- `GET /post` — 본인 글만 반환 (`findAllByUser_Id(currentUserId)`)
- `GET /post/{id}` — 본인 글이면 200, 타인 글이면 **403** (404 가 아님 — 존재는 확인되지만 권한 없음)
- `PUT /post/{id}`, `DELETE /post/{id}` — 본인 글만 수정/삭제

코드 위치: `PostService.loadOwned()`. 본인 확인은 `Post.isOwnedBy(UUID)` 메서드로.

> **403 vs 404 미묘한 결정**: 타인 글 조회 시 "없습니다 (404)" 로 숨길지 "접근 권한 없음 (403)" 으로 솔직히 알릴지. 우리는 **403** 채택 — 어차피 UUID 라 무작위 추측 어렵고, FE 가 정확한 에러 메시지 받는 게 디버깅에 좋음. UUID 가 예측 가능했다면 404 가 안전.

---

## 🛡️ Spring Security 필터 체인

### 설정 (SecurityConfig)
```java
http
  .csrf(disable)              // REST API — CSRF 토큰 의미 없음
  .httpBasic(disable)         // Basic Auth 안 씀
  .formLogin(disable)         // 폼 로그인 안 씀
  .logout(disable)            // stateless 라 server 로그아웃 의미 X
  .sessionManagement(STATELESS)  // 세션 저장 X
  .authorizeHttpRequests(...)    // 화이트리스트 + 나머지 authenticated
  .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

### 미인증 응답 — JSON 으로 통일
```java
.exceptionHandling(eh -> eh.authenticationEntryPoint((req, res, ex) -> {
    res.setStatus(401);
    res.setContentType("application/json");
    res.getWriter().write("{\"message\":\"인증이 필요합니다.\"}");
}))
```

→ FE 가 어떤 미인증 케이스든 동일한 JSON 포맷으로 받음. 응답 envelope 일관성.

### 필터 순서
1. `JwtAuthenticationFilter` ← 우리가 만든 것 (Bearer 토큰 검증)
2. `UsernamePasswordAuthenticationFilter` (기본, 사용 안 함)
3. `AuthorizationFilter` (기본, "이 요청 인증됐나?" 검사)

`addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)` 로 JWT 필터를 가장 먼저.

---

## 📨 에러 응답 표준

| HTTP | 의미 | 언제 |
|---|---|---|
| `400` | 잘못된 요청 | Bean Validation 실패 (이메일 형식/비번 정책), 잘못된 JSON, 필수 파라미터 누락 |
| `401` | 인증 실패 | 토큰 누락/만료/위조, 로그인 자격증명 불일치 |
| `403` | 권한 없음 | 인증은 됐지만 본인 글 아닌 글 접근 |
| `404` | 리소스 없음 | 글 자체가 존재 안 함 |
| `409` | 충돌 | 이메일/닉네임 중복 (signup) |
| `500` | 서버 오류 | 예상 외 (fallback) |

**응답 envelope 동일**: 모든 4xx/5xx 는 `{"message": "사람이 읽을 수 있는 메시지"}`.

코드 위치: `GlobalExceptionHandler.java`.

---

## ⚠️ 알려진 약점 / 의도된 트레이드오프

> **졸업 데모 범위라 의도적으로 수용한 것들**. 운영 서비스로 가려면 보강 필요.

| 약점 | 영향 | 보강 방법 (참고) |
|---|---|---|
| **HTTPS 안 됨** (HTTP only) | 네트워크 도청 시 토큰/비밀번호 평문 노출 | ALB + ACM 인증서 / nginx + Let's Encrypt. PR 7 (ECS) 시점 또는 별도 PR |
| **Refresh token 없음** | 24시간 후 사용자가 다시 로그인 | Refresh + Access 분리, refresh 만 httpOnly cookie. 졸업 데모 후 |
| **JWT 무효화 안 됨** | 로그아웃해도 만료 전까지 토큰 유효. 분실 시 24h 까지 노출 | Redis 블랙리스트 또는 짧은 만료 + refresh |
| **Rate limiting 없음** | `/auth/login` 으로 비밀번호 무차별 대입 가능 | Spring Cloud Gateway, nginx limit_req, 또는 Bucket4j |
| **비밀번호 정책 약함** | 특수문자 강제 X, 8자 (NIST 권장 따른 절충) | 정규식 강화. 단 너무 빡세면 UX 손상 |
| ~~CORS 없음~~ → **CORS 도입됨 (PR 8)** | 허용 origin 만 호출 가능. 운영은 EC2 `.env` 의 `APP_CORS_ALLOWED_ORIGINS` 로 관리. 와일드카드 X (allowCredentials=true 와 충돌) | OK |
| **JWT secret 회전 자동화 X** | 의심 사고 시 수동으로 .env 갱신 + 재배포 | AWS Secrets Manager + 주기적 회전 |
| **로그에 토큰 흘릴 위험** | 디버그 로그 추가 시 실수로 토큰/비번 찍을 가능 | 현재는 그런 로그 없음. Logback config 에 마스킹 패턴 |
| **CSRF disabled** | 의도된 — JWT 헤더 인증은 CSRF 영향 없음 | OK (쿠키 인증 도입 시 재검토) |

---

## 🚨 절대 하지 말 것 (체크리스트)

면접/발표에서 "보안 어떻게 했어요?" 물으면 이걸 안 했다는 게 진짜 안 좋게 보임:

- [ ] `JWT_SECRET` 을 git 에 커밋 (어디든 — yaml 기본값에 진짜 비밀 박지 마라)
- [ ] 비밀번호를 평문으로 어디든 저장 (DB / 로그 / 응답)
- [ ] 로그인 실패 메시지를 "이메일 없음" vs "비번 틀림" 으로 분리 (열거 공격 허용됨)
- [ ] JWT 의 payload 에 이메일/비밀번호 등 민감 정보 박기 (payload 는 암호화 X, Base64 인코딩일 뿐)
- [ ] 컨트롤러 시그니처에 `@RequestParam UUID userId` 같이 사용자가 직접 박는 user_id 받기 (위조 가능 — `@AuthenticationPrincipal UUID` 만)
- [ ] CORS 를 `allowedOrigins("*")` + `allowCredentials(true)` 조합 (브라우저가 거부하기도 하고 보안 X)
- [ ] 디버그 위해 `show_sql: true` 가 운영에서 SQL 로그에 비밀번호 평문 노출하는지 확인 안 함 → 현재는 BCrypt 해시만 저장되므로 OK 지만 향후 새 컬럼 추가 시 주의
- [ ] EC2 보안그룹의 3306 (RDS) 을 `0.0.0.0/0` 으로 열어두기 → 현재는 EC2 SG 에서만 허용

---

## 📝 면접/발표 답변 가이드 (자주 받을 질문)

### Q. 인증을 어떻게 구현했나요?
"Stateless JWT 입니다. 클라이언트가 `/auth/login` 으로 자격증명 보내면 서버가 BCrypt 로 검증 후 사용자 UUID 를 sub claim 에 담아 HMAC-SHA 서명한 토큰을 발급합니다. 이후 모든 요청은 `Authorization: Bearer <토큰>` 헤더로 인증하며, `JwtAuthenticationFilter` 가 서명/만료를 검증해 `SecurityContext` 에 UUID 를 주입합니다."

### Q. 비밀번호는 어떻게 저장하나요?
"BCrypt 해시로만 저장합니다. cost factor 는 기본값 10. 평문은 어디에도 저장하지 않습니다. Spring Security 의 `BCryptPasswordEncoder` 빈을 사용합니다."

### Q. JWT secret 은 어떻게 관리하나요?
"`application.yaml` 에 `${JWT_SECRET:기본값}` 플레이스홀더로 두고, 운영은 GitHub Secret + EC2 의 `.env` 환경변수로 override 합니다. yaml 의 기본값은 로컬 dev 용 더미라 운영에서는 절대 쓰이지 않습니다. 과거 secret 누락 사고 두 건이 있어서 troubleshooting.md 에 기록해두고 동일한 사고가 반복되지 않도록 절차를 정리했습니다."

### Q. 로그인 실패 시 응답을 통일한 이유?
"열거 공격(Enumeration) 방지입니다. 이메일이 없는 경우와 비밀번호가 틀린 경우를 다르게 응답하면, 공격자가 임의 이메일을 던져서 우리 DB 에 가입된 이메일 명단을 추출할 수 있습니다. 그래서 두 경우 모두 동일한 401 + 동일한 메시지로 응답합니다."

### Q. 다른 사용자의 글은 어떻게 보호되나요?
"`@AuthenticationPrincipal UUID userId` 로 현재 사용자 ID 를 받고, `Post.isOwnedBy(userId)` 로 소유권을 검증합니다. 본인 글이 아니면 `PostAccessDeniedException` → 403. 컨트롤러가 user_id 를 파라미터로 받지 않기 때문에 클라이언트가 위조할 여지가 없습니다."

### Q. HTTPS 안 쓰는 이유?
"졸업 데모 범위에서 의도적으로 수용한 트레이드오프입니다. HTTPS 도입하려면 ALB + ACM 인증서 / 도메인 / 백엔드 인프라 재구성이 필요해서 PR 7 (ECS 이전) 시점에 같이 묶어둘 예정입니다. 현재 약점은 알고 있고 보안 문서에도 명시되어 있습니다."

→ "약점을 명시한 문서를 가지고 있다" 자체가 좋은 인상.
