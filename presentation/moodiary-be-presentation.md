---
marp: true
theme: default
paginate: true
size: 16:9
header: 'Moodiary — Backend'
footer: '2026 호서대 졸업프로젝트 · BE 발표'
style: |
  section {
    font-family: 'Pretendard', 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
    font-size: 24px;
  }
  h1 { color: #2563eb; font-size: 40px; }
  h2 { color: #1e40af; font-size: 32px; }
  h3 { color: #1e3a8a; font-size: 26px; }
  code { background: #f1f5f9; padding: 2px 6px; border-radius: 4px; }
  pre { background: #0f172a; color: #e2e8f0; padding: 12px; border-radius: 6px; font-size: 17px; }
  table { font-size: 20px; }
  th { background: #2563eb; color: white; }
  blockquote { border-left: 4px solid #2563eb; color: #475569; }
  .highlight { background: #fef3c7; padding: 2px 6px; border-radius: 4px; }
---

<!-- _class: lead -->

# Moodiary Backend

### 일기 + AI 무드 캘린더 플랫폼

호서대학교 컴퓨터공학부 졸업프로젝트
**Backend 파트**

<!--
안녕하십니까. 호서대학교 컴퓨터공학부 졸업프로젝트 "Moodiary" 의 백엔드 파트 발표를 시작하겠습니다.
Moodiary 는 일기 작성과 AI 감성 분석을 결합한 월별 무드 캘린더 플랫폼입니다.
저는 3인 분업 중 백엔드 API 서버 / 데이터베이스 / 운영 인프라 전체를 담당했습니다.
오늘 발표는 약 20분으로, 무엇을 만들었는지, 어떤 과정을 거쳤는지, 그리고 그 과정에서 어떤 운영 사고와 학습이 있었는지를 보여드리겠습니다.
-->

---

## 발표 구성

1. **프로젝트 한눈에** — 무엇을 만들었나
2. **여정** — 어떤 과정으로 여기까지 왔나
3. **핵심 구현** — 아키텍처 / DB / 인프라 / 보안
4. **핵심 트러블슈팅** — 무엇을 학습했나
5. **앞으로** — 무엇을 더 할 것인가

<!--
발표는 다섯 섹션으로 진행합니다.
첫 섹션은 Moodiary 가 무엇이고, 어떤 기술 스택으로 만들어졌는지를 짧게 설명합니다.
두 번째는 프로젝트가 진행된 큰 흐름과, 각 단계에서 어떤 결정을 했는지입니다.
세 번째 핵심 구현 섹션이 가장 중요합니다 — 아키텍처, 데이터베이스, 인프라, 보안 네 영역을 차례로 보여드립니다.
네 번째는 작업 중 부딪힌 운영 사고들과, 거기서 학습한 일반화된 교훈입니다.
마지막은 남은 작업과 졸업 발표 이후의 계획입니다.
-->

---

<!-- _class: lead -->

# 1. 프로젝트 한눈에

<!--
첫 섹션입니다. Moodiary 가 무엇이고, 어떤 기술 스택 위에 올라가 있는지 짧게 보여드리겠습니다.
-->

---

## Moodiary 가 무엇인가

> 사용자가 일기를 작성하면 AI 가 **공감 메시지 + 그 날의 기분 이모지**를 생성.
> 그 이모지는 **월별 캘린더 화면**에 매핑되어 한 달치 감정을 한눈에 보여준다.

```
   일기 작성  →  AI 분석 (글 + 😊)  →  캘린더에 매핑
```

### 3인 분업

| 파트 | 담당 |
|---|---|
| **Backend (이번 발표)** | REST API, DB, 배포, 인증, AI 비동기 응답 |
| Frontend | React + Vite SPA, S3 정적 호스팅 |
| AI 추론 서버 | 일기 텍스트 → 응답 + 이모지 생성 |

<!--
Moodiary 의 핵심 흐름은 단순합니다.
사용자가 일기를 쓰면, 그 일기를 AI 서버가 분석해서 공감 메시지와 함께 그 날의 기분을 이모지로 만들어줍니다.
그 이모지들이 월별 캘린더에 매핑되어, 사용자가 "이 달엔 내가 어땠지" 를 시각적으로 한 번에 볼 수 있습니다.
3인 분업이라 프론트와 AI 추론 서버는 다른 두 분이 담당하시고, 백엔드 API 서버는 제가 단독으로 담당했습니다.
백엔드의 역할은 인증, 일기 CRUD, AI 서버 비동기 호출, 캘린더 데이터 집계 — 그리고 이 모든 것의 배포와 운영입니다.
-->

---

## 스택과 운영

| 분류 | 기술 |
|---|---|
| 언어 / 런타임 | **Java 25** (Amazon Corretto) |
| 프레임워크 | **Spring Boot 4.0.6**, Spring Security 6, Spring Data JPA, **QueryDSL 5** |
| 데이터 | MySQL 9.x (AWS RDS, **utf8mb4**), Hibernate 7.2 |
| 인증 | **JWT** (HS256, access 1h) + **Refresh Token** (2w, rotation, SHA-256) + BCrypt |
| 비동기 | `@EnableAsync` + `ThreadPoolTaskExecutor` |
| API 문서 | SpringDoc OpenAPI 3.0.3 (Swagger UI) |
| 운영 | **Docker** + AWS EC2 (Amazon Linux 2023) + **Elastic IP** |
| CI/CD | GitHub Actions → Docker Hub → **AWS SSM** `send-command` → EC2 |

🔗 운영 URL: `http://15.165.95.129:8080` · Swagger 활성

<!--
스택을 빠르게 짚어드리면 — 언어는 Java 25, 프레임워크는 가장 최신인 Spring Boot 4.0.6 입니다.
인증 부분이 최근 강화된 영역입니다. 처음엔 access token 한 개만 24시간으로 운영했는데, 보안과 UX 사이의 trade-off 가 어중간했습니다.
지금은 access 1시간, refresh 2주 + rotation 패턴으로 분리했고, refresh 는 SHA-256 해시로 DB 에 저장합니다.
비동기 영역도 최근 들어왔습니다 — AI 서버 호출이 동기로 일어나면 사용자가 응답을 기다려야 해서, @Async 로 분리하고 사용자는 폴링으로 결과를 받습니다.
운영은 AWS EC2 위에 Docker 컨테이너로 띄우고, Elastic IP 로 주소를 고정시켰습니다.
배포는 SSH 키 관리 부담을 없애려고 AWS SSM 의 send-command 방식을 사용합니다. GitHub Actions 가 main 에 push 되면 자동으로 Docker Hub 에 이미지를 올리고, SSM 으로 EC2 에 명령을 보내서 새 컨테이너로 교체합니다.
-->

---

<!-- _class: lead -->

# 2. 여정 — 어떤 과정으로 왔는가

<!--
두 번째 섹션입니다. 프로젝트가 시작부터 지금까지 어떤 큰 흐름을 거쳤고, 각 단계에서 어떤 결정을 했는지 짧게 보여드리겠습니다.
-->

---

## 큰 흐름

```
[1단계] 인프라 + 기본 CRUD          PR #1 ~ #16
        Spring Boot 4 셋업, EC2/RDS, Docker, Post CRUD, 슬라이스 테스트

[2단계] 인증 + 소유권                PR #20 ~ #27
        회원가입(BCrypt), JWT 로그인, SecurityFilterChain, Post 소유권 격리

[3단계] 캘린더 + QueryDSL 도입       PR #31 ~ #36
        월별 캘린더 API, QueryDSL 첫 도입, 외부 라이브러리 호환성 첫 학습

[4단계] CORS + 프론트 통합 준비      PR #37 ~ #51
        CorsConfig, FE 가이드, ⚠️ 운영 부팅 폭발 대사건

[5단계] CD 신뢰성 + Workflow 규칙    PR #45 ~ #54
        SSM wait/verify, health check, 옛 silent fail 진실 드러남

[6단계] 비동기 AI 응답 + 인증 강화   PR #59, #63   ★ 본 세션 핵심
        AiResponse 엔티티, @Async + DB 상태머신, Refresh Token (rotation)
```

<!--
6단계로 정리했습니다.
1단계는 인프라와 기본 CRUD — Spring Boot 4 셋업, EC2 와 RDS 분리, 일기 CRUD 까지.
2단계는 인증 — 회원가입과 JWT 로그인, 그리고 본인 글만 보이게 하는 소유권 격리.
3단계는 캘린더 — 월별로 일기를 그룹핑하는 API 인데, 이 때 QueryDSL 을 처음 도입했고 외부 라이브러리 호환성 함정을 처음 만났습니다.
4단계와 5단계는 운영 관련입니다. 프론트와 통합하기 위해 CORS 를 셋업하다가, 운영 컨테이너가 restart loop 에 빠진 큰 사고를 만났습니다. 그 복구 과정이 5단계입니다.
6단계가 이번 발표 직전까지의 핵심 — 비동기 AI 응답 골격을 올리고, 인증 라인을 Refresh Token 으로 강화한 단계입니다.
-->

---

## 단계별 핵심 결정

### 1-2단계 — 인프라 + 인증
- **EC2 + RDS 분리** — 로컬 docker mysql 폐기, RDS 단일 소스 (Elastic IP 고정)
- **AWS SSM 기반 배포** — SSH 키 관리 불필요
- **`@EnableJpaAuditing` 별도 클래스 분리** — 슬라이스 테스트 호환 (T-003)
- **`@AuthenticationPrincipal UUID`** — principal 에 UUID 박아 컨트롤러 깔끔

### 3단계 — 캘린더 + QueryDSL
- **`compileOnly` → `implementation`** (T-016) — runtime classpath 누락
- **`MissingServletRequestParameterException` 매핑** (T-017) — 필수 파라미터 누락 → 400 통일

### 4-5단계 — 운영 정상화
- **ApplicationContext 부팅 안전망 복원** — `MoodiaryApplicationTests` 활성화
- **CD wait/verify + health check** — silent fail 차단

<!--
각 단계의 핵심 결정만 짚어드리면 —
1-2단계에서 EC2 와 RDS 를 분리하면서, 인스턴스를 켰다 껐다 해도 데이터는 RDS 에 안전하게 남도록 했습니다. Elastic IP 도 부착해서 IP 가 바뀌지 않게 고정했습니다.
배포는 SSH 키를 관리하는 부담을 피하려고 AWS SSM 으로 갔는데, 이게 나중에 silent fail 의 원인이 되기도 합니다.
인증 영역에서는 컨트롤러 시그니처를 깨끗하게 유지하려고 @AuthenticationPrincipal UUID 패턴을 채택했습니다.
3단계의 QueryDSL 도입 때 compileOnly 와 implementation 차이로 NoClassDefFoundError 가 나서 한참 헤맸는데, 이 함정은 그 후 모든 외부 라이브러리 도입 시 첫 번째로 점검하는 항목이 됐습니다.
4-5단계의 부팅 안전망 복원과 CD 신뢰성 강화는 다음 트러블슈팅 섹션에서 더 자세히 보여드리겠습니다.
-->

---

## 6단계 — 비동기 AI 응답 + Refresh Token

### PR 4-pre — 비동기 AI 응답 골격
- **`AiResponse` 1:1 엔티티** — `PENDING → DONE / FAILED` 상태머신
- **`@Async` + DB 상태 관리** — 큐 / 메시지브로커 없는 단순 구조
- **Stub 어댑터** — AI 합의 전 단독 진행 (PR 4-final 시 HTTP 어댑터로 교체)
- **`@JsonInclude(NON_NULL)`** — DONE 시 `errorMessage` 직렬화 X

### PR 10 — Refresh Token (Rotation)
- **Access 1h + Refresh 2w** — 보안/UX trade-off 해소
- **SHA-256 hex 저장** — raw token 노출 시에도 DB 만으론 복원 불가
- **Rotation** — refresh 사용 시 즉시 revoke + 새 발급 (탈취 노출 창 ↓)
- **`POST /auth/logout`** — refresh 무효화 (access 는 만료 대기)

<!--
6단계가 이번 세션의 핵심입니다. 두 개 PR 을 마저 짚어드리겠습니다.
PR 4-pre 는 AI 비동기 응답의 골격입니다. AI 추론 서버를 담당하는 분과 외부 합의가 아직 안 됐는데, 그 합의를 기다리면 백엔드 작업이 막혀버립니다.
그래서 골격만 먼저 만들고, 실제 HTTP 호출 어댑터는 합의 후 한 클래스만 교체하면 되도록 분리했습니다.
큐나 메시지 브로커는 도입하지 않았습니다 — 졸업 프로젝트 범위에서는 @Async 와 DB 상태 머신으로 충분합니다. 사용자가 일기를 쓰면 PENDING 상태로 row 가 같이 저장되고, 비동기 스레드가 AI 호출 후 DONE 또는 FAILED 로 전이합니다. 클라이언트는 폴링으로 결과를 받아갑니다.
PR 10 의 Refresh Token 은 보안과 UX 의 trade-off 해소가 동기였습니다. 처음엔 access 만 24시간이었는데, 짧게 잡으면 사용자가 자주 튕기고 길게 잡으면 토큰 탈취 시 노출 창이 커집니다.
지금은 access 1시간, refresh 2주로 분리하고, refresh 는 rotation 패턴으로 한 번 쓰면 즉시 무효화됩니다. DB 에는 SHA-256 해시만 저장해서 raw token 이 새도 DB 만으론 복원이 안 됩니다.
-->

---

<!-- _class: lead -->

# 3. 핵심 구현

### 아키텍처 / DB / 인프라 / 보안

<!--
세 번째 섹션입니다. 백엔드의 핵심 구현을 네 영역 — 아키텍처, 데이터베이스, 인프라, 보안 — 으로 나눠서 보여드리겠습니다.
가장 길고 가장 중요한 섹션입니다.
-->

---

## 3-1. 아키텍처 — 레이어 + 패키지

```
Controller (HTTP, @AuthenticationPrincipal UUID)
    ↓
Service (@Transactional 비즈니스 로직 + 소유권 검증)
    ↓
Repository (Spring Data JPA + QueryDSL)
    ↓
Entity (BaseEntity → createdAt/updatedAt 자동, UUID PK)
```

### 패키지 구성

```
hoseo.moodiary
├── controller/     # @RestController
├── service/        # 비즈니스 + 소유권 검증
│   └── ai/         # AI 어댑터 (Stub / Real 교체 지점)
├── repository/     # Spring Data + QueryDSL
├── entitiy/        # JPA 엔티티 (* 의도적 오타 유지)
│   └── base/       # BaseEntity (@EnableJpaAuditing)
├── dto/{request,response}/   # @JsonInclude(NON_NULL) 정책
├── exception/      # 도메인 예외 + GlobalExceptionHandler
├── security/       # JwtTokenProvider, JwtAuthenticationFilter
└── config/         # SecurityConfig, CorsConfig, AsyncConfig 등
```

<!--
아키텍처는 전형적인 4-레이어 구조입니다. Controller → Service → Repository → Entity.
특이한 점 두 가지만 짚어드리면 — service/ai/ 폴더는 AI 어댑터의 교체 지점입니다. 지금은 Stub 클래스 하나뿐이지만, PR 4-final 에서 HTTP 어댑터 추가 시 interface 로 추출됩니다.
entitiy 폴더 이름은 오타입니다 — 프로젝트 초기에 박힌 오타인데, 의도적으로 유지하고 있습니다. 한 번 굳어진 패키지 이름은 리팩토링 비용이 크고, 코드 진실과 명명 정합성보다 일관성이 더 중요합니다.
@AuthenticationPrincipal UUID 패턴은 컨트롤러 메서드 시그니처에 UUID userId 를 자동으로 받게 해서, 매번 SecurityContext 를 직접 만지지 않아도 됩니다.
-->

---

## 3-1. 인증 흐름 (런타임)

```
1. POST /auth/login → UserService 가 BCrypt 검증
2. JwtTokenProvider.createAccessToken(userId)            ← 1시간
   RefreshTokenService.issue(userId, SHA-256 hex 저장)   ← 2주
3. 응답 { accessToken, refreshToken, userId }

4. 클라이언트 → Authorization: Bearer <accessToken>
5. JwtAuthenticationFilter — 토큰 검증
6. @AuthenticationPrincipal UUID 자동 주입

[access 만료 시]
7. POST /auth/refresh { refreshToken }
8. RefreshTokenService.rotate
   ├─ 기존 refresh 즉시 revoke
   └─ 새 access + 새 refresh 발급

[로그아웃]
9. POST /auth/logout { refreshToken } → revoke (idempotent)
```

<!--
런타임에서 인증이 어떻게 흐르는지 단계별로 보여드리겠습니다.
사용자가 로그인하면, UserService 가 BCrypt 로 비밀번호를 검증합니다. 성공하면 JwtTokenProvider 가 1시간짜리 access token 을 만들고, RefreshTokenService 가 2주짜리 refresh token 을 발급합니다. 이 때 refresh 는 SHA-256 해시만 DB 에 저장합니다.
이후 클라이언트는 모든 요청에 Authorization Bearer 헤더를 박고, JwtAuthenticationFilter 가 토큰을 검증해서 UUID 를 SecurityContext 에 주입합니다.
access token 이 1시간 후 만료되면, 클라이언트가 refresh token 으로 /auth/refresh 를 호출합니다. 이 때 핵심이 rotation 입니다 — 기존 refresh 는 즉시 무효화되고, 새 access 와 새 refresh 가 같이 발급됩니다. 그래서 탈취된 refresh 가 단 한 번만 유효합니다.
로그아웃은 /auth/logout 으로 refresh 를 무효화합니다. access 는 stateless 라 서버에서 막을 수 없고, 1시간 후 만료를 기다립니다. FE 는 localStorage 를 즉시 클리어합니다.
-->

---

## 3-1. 비동기 AI 응답 흐름 (PR 4-pre)

```
1. POST /post → PostService.create()
   ├─ Post 저장
   └─ AiResponse(status=PENDING) 같은 트랜잭션에 저장
                   ↑ 정합성 보장

2. PostController 가 commit 후 triggerAsync(postId) 호출
                                  ↑ race free

3. @Async 스레드:
   ├─ AiResponseClient.invoke(...)   ← 현재 Stub, PR 4-final 시 HTTP
   ├─ 성공 → PENDING → DONE + content + emoji
   └─ AiInferenceException → PENDING → FAILED + errorMessage

4. 클라이언트는 GET /post/{id}/ai-response 로 폴링
   응답: @JsonInclude(NON_NULL) — DONE 시 errorMessage 직렬화 X
```

<!--
비동기 AI 응답 흐름에서 가장 신경 쓴 두 가지를 짚어드리겠습니다.
첫째, 일기와 PENDING row 의 정합성입니다. 일기를 저장할 때 AiResponse 의 PENDING row 도 같은 트랜잭션 안에서 함께 저장합니다. 트랜잭션이 실패하면 둘 다 롤백되어, "일기는 있는데 AI 응답 row 가 없는" 상태가 절대 안 생깁니다.
둘째, race condition 방지입니다. PostController 는 service.create() 가 트랜잭션 commit 까지 끝낸 후에야 triggerAsync 를 호출합니다. 그래야 비동기 스레드가 PENDING row 를 조회할 때 이미 commit 된 상태가 보장됩니다.
응답 직렬화에는 @JsonInclude(NON_NULL) 을 적용해서, status 별로 다른 필드만 노출되게 했습니다. DONE 응답에는 errorMessage 가 아예 직렬화되지 않고, FAILED 응답에는 content / emoji 가 빠집니다.
-->

---

## 3-2. 데이터베이스 — ER 다이어그램

```
+--------+ 1:N +--------+ 1:1 +-----------+
|  User  |─────|  Post  |─────| AiResponse|
+--------+     +--------+     +-----------+
    │ 1:N         post_id      post_id (UK)
    ↓             FK
+-------------+
|RefreshToken |
+-------------+
  token_hash (UK)
```

### 4 테이블 관계

- **User** ←→ **Post**: 1:N (작성자)
- **Post** ←→ **AiResponse**: 1:1 (`uk_ai_response_post_id` UNIQUE)
- **User** ←→ **RefreshToken**: 1:N (한 사용자 여러 디바이스)

<!--
데이터베이스는 4개 테이블입니다.
User 와 Post 는 1:N — 한 사용자가 여러 일기를 씁니다.
Post 와 AiResponse 는 1:1 입니다. 일기 하나당 AI 응답 하나가 짝을 이룹니다. 이 1:1 관계는 ai_response 테이블의 post_id 컬럼에 UNIQUE 제약이 박혀 있어서 보장됩니다.
User 와 RefreshToken 도 1:N 입니다. 한 사용자가 여러 디바이스 / 브라우저에서 로그인할 수 있어서, 디바이스마다 별도 refresh token 을 가집니다.
이 ER 은 README 의 Mermaid erDiagram 에 더 자세한 컬럼 정보까지 정리되어 있습니다.
-->

---

## 3-2. DB 핵심 정책

| 정책 | 내용 |
|---|---|
| **PK** | UUID v4 (`@UuidGenerator`). 컬럼명 `<table>_id`. ID 추측 방지 |
| **Auditing** | `BaseEntity` 상속 → `created_at`/`updated_at` 자동 |
| **Charset** | `utf8mb4` — 이모지 (😊) 그대로 저장, 변환 로직 X |
| **불변 + 명시 메서드** | setter 없음. `Post.update(...)` / `AiResponse.markDone(...)` → 트랜잭션 dirty-checking 으로 UPDATE |
| **`ddl-auto: update`** | 부팅 시 누락 컬럼/테이블 자동 추가. **인덱스/UNIQUE 보장 X** → 운영 머지 후 수동 확인 |
| **Flyway 도입 예정** | PR 6 시점에 `validate` 로 전환 |

> 운영 머지 후 `SHOW INDEX FROM ai_response;` 로 `uk_ai_response_post_id` 사후 검증 필수

<!--
DB 정책에서 가장 졸업프로젝트 다운 결정 두 가지를 짚어드리면 —
첫째, UUID 기본키입니다. 분산 환경 가정과 ID 추측 방지가 동기입니다. /post/1, /post/2 처럼 순차 정수면 다른 사용자의 글 ID 를 쉽게 추측할 수 있습니다. UUID 면 그 공격이 막힙니다.
둘째, ddl-auto update 정책입니다. 졸업 프로젝트는 스키마가 자주 바뀌는데, 매번 수동 DDL 을 쓰면 부담이 큽니다. update 모드면 새 엔티티 컬럼이 추가될 때 Hibernate 가 부팅 시 자동으로 ALTER TABLE 해줍니다.
다만 update 모드의 한계가 있습니다 — 인덱스와 UNIQUE 제약은 추가를 보장하지 않습니다. 그래서 PR 4-pre 가 운영 머지된 후 직접 SHOW INDEX 로 uk_ai_response_post_id 가 들어갔는지 확인했습니다. 다행히 이번엔 잘 들어갔지만, 그게 보장이 아니라 운입니다.
이 한계를 영구 해소하려고 PR 6 에서 Flyway 를 도입할 예정입니다. 그 시점엔 ddl-auto 를 validate 로 전환합니다.
이모지 저장은 utf8mb4 가 필수입니다. 만약 utf8mb3 면 4바이트 이모지 INSERT 시 폭발합니다. PR 4-pre 머지 전에 운영 RDS 가 utf8mb4 인지 확인하고 진행했습니다.
-->

---

## 3-3. 인프라 — Runtime

```
[FE: React+Vite]              [BE: Spring Boot 4]      [AI: 별도 EC2]
       ↓ build                      ↓ build                    ↑
       ↓ aws s3 sync                ↓ Docker Hub               │
  ┌─────────────┐              ┌──────────────┐                │
  │ AWS S3      │              │ AWS EC2      │ ← SSM ← GitHub │
  │ ap-NE-1     │ ─ HTTP API ─►│ Amazon Linux │   Actions      │
  │ (도쿄)      │              │ ap-NE-2 서울 │                │
  └─────────────┘              └──────────────┘                │
                                      ↓ JDBC                   │
                               ┌──────────────┐                │
                               │ AWS RDS MySQL│                │
                               │ utf8mb4      │                │
                               └──────────────┘                │
```

- **EC2 IP 고정** — Elastic IP `15.165.95.129`
- **RDS 단일 소스** — 자격증명은 GitHub Secrets + EC2 `.env` 이중

<!--
운영 인프라는 이렇게 분포되어 있습니다.
프론트는 S3 정적 호스팅, 백엔드는 EC2 위 Docker, AI 서버는 별도 EC2 — 모두 AWS 입니다.
백엔드와 RDS 는 서울 리전입니다. 프론트 S3 만 도쿄 리전인데, 다른 분이 셋업할 때의 선택이었습니다. latency 영향은 사용자가 S3 에서 정적 자산만 받는 거라 미미합니다.
EC2 에는 Elastic IP 가 부착되어 있어서, 인스턴스를 stop/start 해도 IP 가 바뀌지 않습니다. RDS 자격증명 같은 시크릿은 GitHub Secrets 와 EC2 의 .env 파일에 이중으로 관리하고 있습니다. 이건 단순함을 우선한 선택이고, 졸업 후 운영 안정화 단계에 AWS Parameter Store 로 단일화하는 걸 검토할 예정입니다.
-->

---

## 3-3. 인프라 — CI/CD 파이프라인

**dev 머지 = CI 만**, **main 머지 = CD 가 자동 배포**

```
PR → dev                       PR (release) → main
   ↓                                   ↓
GitHub Actions (CI)        GitHub Actions (CD, OIDC)
   ↓                                   ↓
./gradlew build              Docker Hub: latest + :SHA
   ↓                                   ↓
[테스트 리포트 artifact]       AWS SSM send-command
                                       ↓
                              (1) docker-compose pull
                              (2) docker-compose up -d
                              (3) wait command-executed
                              (4) health check /v3/api-docs
                                       ↓
                              ✅ deploy 성공 보장
```

- **이미지 SHA 태그** — 응급 롤백 옵션 확보
- **wait + health check** — T-019 의 silent fail 차단

<!--
CI/CD 는 두 단계로 분리되어 있습니다.
dev 머지는 CI 만 — gradle build 와 테스트 실행입니다. dev 가 깨질 위험 없이 안전합니다.
main 머지가 CD 인데, OIDC 로 AWS 에 인증하고, Docker Hub 에 이미지를 두 태그로 푸시합니다. latest 와 commit SHA. SHA 태그는 응급 롤백 옵션입니다.
그 다음 SSM send-command 로 EC2 에 들어가서 docker-compose pull, up 을 실행합니다.
여기서 중요한 게 마지막 두 단계 — wait command-executed 와 health check 입니다. 이 두 안전망이 없던 시절에는 SSM 이 명령을 enqueue 만 하고 결과를 안 보고 그냥 성공이라고 보고했습니다. 실제 EC2 컨테이너는 실패해도 GitHub Actions 가 초록불을 띄웠습니다. 이 silent fail 이 T-019 대사건의 한 층이었습니다.
지금은 SSM 의 명령 실행 완료까지 wait 하고, swagger 응답이 200 으로 떨어질 때까지 polling 합니다. 그래야 CD 의 초록불이 진짜 deploy 성공을 의미하게 됩니다.
-->

---

## 3-4. 보안 — 인증

| 항목 | 정책 |
|---|---|
| 비밀번호 | **BCrypt** (strength 10). 평문은 어디에도 남기지 않음 |
| Access Token | **JWT HS256** (jjwt 0.12.6), **1h** 수명. Claims: `sub=UUID` |
| Refresh Token | 32-byte secure random. **2주** 수명. DB 에 **SHA-256 hex 만 저장** |
| **Rotation** | refresh 사용 시 즉시 revoke + 새 발급. **탈취 노출 창 최소화** |
| 로그아웃 | refresh 무효화 (idempotent). Access 는 만료 대기 |

### 정보 노출 최소화
- **열거 공격 방지** — `/auth/login` 401 메시지가 "이메일 미존재" / "비번 틀림" 구분 X
- **Refresh 검증 실패 통일** — 존재 X / 만료 / revoke 모두 같은 401
- **`@JsonInclude(NON_NULL)`** — DONE 응답에 `errorMessage` 직렬화 X

<!--
보안의 인증 영역입니다. 이미 흐름은 보여드렸으니, 정책의 디테일을 짚어드리겠습니다.
가장 신경 쓴 게 refresh token 의 DB 저장 방식입니다. raw token 을 그대로 DB 에 저장하면, DB 가 유출됐을 때 그 자체로 모든 사용자 세션이 탈취됩니다. SHA-256 hex 만 저장하면 raw token 이 새도 DB 만으론 복원이 불가능합니다.
Rotation 도 같은 원리입니다 — refresh 가 단 한 번만 유효하니까, 만약 누가 refresh 를 훔쳐도 정당한 사용자가 한 번이라도 refresh 를 쓰는 순간 훔친 토큰은 무효가 됩니다.
정보 노출 최소화 영역에서는 열거 공격 방지가 핵심입니다. 로그인 실패 시 "이메일이 존재하지 않습니다" 와 "비밀번호가 틀렸습니다" 를 구분해서 보여주면, 공격자가 이메일 존재 여부를 알아낼 수 있습니다. 우리는 둘 다 같은 401 메시지로 응답합니다.
응답 직렬화의 @JsonInclude(NON_NULL) 도 보안 정책의 일부입니다. AI 응답이 성공했을 때 errorMessage 필드 자체가 응답 JSON 에 나타나지 않습니다.
-->

---

## 3-4. 보안 — 인가 + CORS + 시크릿

### 인가
- **`@AuthenticationPrincipal UUID userId`** — 컨트롤러가 현재 사용자 UUID 자동 수신
- 도메인 소유권 — `Post.isOwnedBy(userId)` / `PostAccessDeniedException` (403)
- 화이트리스트 외 `authenticated()` — 미인증 시 401 통일

### CORS
- 명시적 origin allowlist (`app.cors.allowed-origins`). 와일드카드 `*` 금지
- `compose.yaml` 의 `${VAR:-default}` — EC2 `.env` 누락 시에도 dev origins 안전 동작
- preflight 는 인증 검사 전 통과

### 시크릿 관리
- `application.yaml` 추적 ✅ — 값은 모두 `${ENV_VAR:기본값}` 플레이스홀더 ([T-013](#) 사고 이후 정책)
- `JWT_SECRET` / RDS 자격증명 — GitHub Secrets + EC2 `.env` 이중

<!--
인가 영역에서는 소유권 검증이 핵심입니다. 일기를 조회할 때 본인 글이면 200, 타인 글이면 403, 존재하지 않으면 404 로 분기합니다. Post.isOwnedBy(userId) 메서드 한 줄로 처리됩니다.
CORS 는 명시적 origin 화이트리스트만 허용합니다. 와일드카드는 절대 안 쓰는데, allowCredentials 와 충돌하고 보안상 위험합니다.
compose.yaml 의 default 패턴은 운영 사고 대비입니다. EC2 의 .env 파일에 APP_CORS_ALLOWED_ORIGINS 가 누락되면 옛날에는 부팅이 실패했는데, 지금은 dev origins 으로 fallback 해서 적어도 컨테이너는 뜨도록 했습니다.
시크릿 관리에서는 application.yaml 을 git 에 추적합니다. 이건 처음엔 직관에 반하는 결정이었는데 — application.yaml 을 gitignore 했더니 운영에 설정이 누락되는 사고가 났습니다. T-013 사건입니다. 이후 application.yaml 은 git 에 두고, 모든 비밀값은 ${ENV_VAR:기본값} 플레이스홀더로 쓰는 정책으로 굳혔습니다.
-->

---

## 3-5. API 표면 (Authorization 필수: ✅)

```
인증 (Whitelisted)
  POST /auth/signup         이메일/닉네임 UNIQUE, BCrypt
  POST /auth/login          access (1h) + refresh (2w) 발급
  POST /auth/refresh        rotation: 기존 즉시 revoke + 새 발급
  POST /auth/logout         refresh 무효화 (idempotent)

Post — 본인 글만 (소유권 격리)  ✅
  POST   /post              인증 필수, 작성자 자동, PENDING row 동시 생성
  GET    /post              본인 글 목록
  GET    /post/{id}         본인 200 / 타인 403 / 없음 404
  PUT    /post/{id}         본인 글만 수정
  DELETE /post/{id}         본인 글만 삭제 (AiResponse cascade)

AI Response (Stub, PR 4-pre)  ✅
  GET /post/{id}/ai-response   폴링, 본인 글만, @JsonInclude(NON_NULL)

Calendar  ✅
  GET /calendar?year=&month=    월별 일자 그룹핑 (emoji 는 PR 5 후속)
```

<!--
지금까지 운영에 반영된 API 전체를 한 페이지로 정리하면 이렇습니다.
인증 라인이 PR 10 이후 네 개로 늘었습니다. signup, login, refresh, logout 입니다.
Post CRUD 는 모두 본인 글만 접근 가능합니다. POST /post 가 호출되면 같은 트랜잭션 안에서 AiResponse 의 PENDING row 도 같이 만들어집니다. 그래서 일기 저장과 PENDING 상태의 정합성이 보장됩니다.
DELETE /post 는 AiResponse 도 cascade 로 같이 지웁니다. FK 제약 때문에 자식인 AiResponse 를 먼저, 부모인 Post 를 나중에 지웁니다.
새로 들어온 GET /post/{id}/ai-response 는 폴링 엔드포인트입니다. 본인 글만 접근 가능하고, status 별로 다른 필드를 반환합니다.
캘린더는 이미 운영 반영됐고, emoji 필드만 PR 4-final 머지 후 후속 PR 에서 JOIN 으로 채워질 예정입니다.
-->

---

## 3-6. 테스트 전략

| 레이어 | 도구 | 케이스 |
|---|---|---|
| Service (단위) | JUnit 5 + Mockito | PostService, UserService, CalendarService, **AiResponseService**, **RefreshTokenService** |
| Controller (슬라이스) | `@WebMvcTest` + `@MockitoBean` | Post, Auth, Calendar |
| 보안 컴포넌트 | 라운드트립 / 만료 / 위조 | JwtTokenProvider |
| CORS | `@WebMvcTest` + `@Import(SecurityConfig)` | CorsConfig 4 |
| **부팅 안전망** | `@SpringBootTest` + H2 | **ApplicationContext 1** |

### **총 80+ tests · 0 failures**

> 부팅 안전망이 가장 중요 — 슬라이스 테스트만으로는 못 잡는 자동 구성 빈 실패를 빌드 단계에서 잡는다 (T-019 학습 결과)

<!--
테스트 전략은 4-레이어로 분리되어 있습니다.
가장 많은 게 Service 단위 테스트입니다 — PostService, UserService, CalendarService, 그리고 최근 추가된 AiResponseService 와 RefreshTokenService 까지.
Controller 는 @WebMvcTest 슬라이스로 인증 / 권한 / 입력 검증을 빠르게 검증합니다.
보안 컴포넌트는 JwtTokenProvider 의 라운드트립과, 만료된 토큰 / 위조된 토큰 / 잘못된 형식 토큰을 각각 테스트합니다.
CORS 는 다른 슬라이스 테스트와 달리 SecurityConfig 를 Import 해서, 실제 필터 체인 위에서 preflight 가 어떻게 동작하는지를 검증합니다.
가장 중요한 게 마지막 부팅 안전망입니다 — @SpringBootTest 로 H2 in-memory 위에 ApplicationContext 를 띄워서 전체 빈 그래프가 잘 wiring 되는지를 확인합니다. 슬라이스 테스트는 mock 으로 의존성을 빼버리니까 못 잡는 자동 구성 결함을 이 한 테스트가 빌드 단계에서 잡습니다.
이게 T-019 의 한 층이었습니다. 그 사고 후 이 안전망이 들어갔습니다.
-->

---

<!-- _class: lead -->

# 4. 핵심 트러블슈팅

<!--
네 번째 섹션입니다. 작업 중 부딪힌 가장 큰 운영 사고 세 가지와, 거기서 얻은 일반화된 학습을 보여드리겠습니다.
-->

---

## 트러블슈팅 문서 — T-### 로그

`claude-docs/troubleshooting.md` 에 작업 중 막힌 모든 지점을 누적 기록.

| 카테고리 | 항목 수 |
|---|---|
| Spring Boot 4.x 마이그레이션 함정 | 3 (T-001 ~ T-003) |
| Spring Security + 슬라이스 테스트 | 3 (T-004, T-005, T-012) |
| **빌드/배포 함정** | **11 (T-013 ~ T-023)** |
| AWS / 운영 인프라 | 2 |
| **워크트리 / CLI / 인코딩** | **4 (T-025 ~ T-027)** |
| DB | 2 |

> **규칙**: 모든 PR 의 마지막에서 두 번째 task = "troubleshooting.md 점검"
> 1분 이상 디버깅한 모든 이슈를 후보로 두고, 프로젝트 고유 trap 이면 새 항목 추가

<!--
트러블슈팅은 별도 문서로 누적 기록합니다. T-001 부터 T-027 까지 27 개 항목이 쌓여 있습니다.
가장 많은 카테고리는 빌드와 배포 함정 — 운영 환경의 미묘한 차이들입니다.
최근에는 워크트리와 CLI 인코딩 함정도 4개가 추가됐습니다. 한국어 commit 메시지가 PowerShell 에서 깨지는 문제 등인데, 다음 사람도 같은 함정을 만날 수 있어서 기록해둡니다.
규칙은 단순합니다 — 모든 PR 의 마지막에서 두 번째 task 가 troubleshooting.md 점검입니다. 1분 이상 디버깅한 모든 이슈를 후보로 두고, 프로젝트 고유의 함정이면 새 항목으로 추가합니다.
이 누적이 가치 있는 이유는, 미래의 작업자가 같은 함정에서 헤매지 않게 해주기 때문입니다.
-->

---

## 가장 큰 사건 — T-019 "운영 부팅 폭발 대사건"

### 표면 증상
> CORS preflight 운영 검증에서 401 → 실제는 컨테이너가 **restart loop**

### 진실 — 5층 결함이 한 검증에서 동시 노출

| 층 | 결함 |
|---|---|
| 1 (메타) | 머지된 PR 의 상태 추적 못 함 (T-015/T-018/T-020) |
| 2 (인프라) | **SSM agent 죽음 + CD silent fail** — 여러 PR 운영 미도달 |
| 3 (코드) | springdoc 2.8.3 ↔ Spring Boot 4 비호환 + Post→User FK orphan |
| 4 (테스트) | `@SpringBootTest` 가 `@Disabled` 라 부팅 폭발 빌드 단계에서 안 잡힘 |
| 5 (배포 인지) | dev 머지 ≠ 운영 도달 / `docker compose pull` ≠ 새 image |

→ 표면 한 가지가 모든 층을 가렸다

<!--
이게 가장 큰 사건입니다. T-019, "운영 부팅 폭발 대사건" 이라고 불립니다.
시작은 단순했습니다 — CORS preflight 가 운영에서 401 로 떨어졌습니다. CORS 설정의 문제라고 생각해서 한참 디버깅했습니다.
실제로는 컨테이너 자체가 restart loop 에 빠져 있었습니다. 그 안에 5개 층의 결함이 동시에 있었습니다.
1층은 메타 — 사용자가 GitHub UI 에서 PR 을 머지하는 시점과 Claude 가 그 사실을 인지하는 시점에 갭이 있었습니다. 작업이 머지된 PR 에 자꾸 추가됐는데, 그게 dev 에 도달하지 못하고 dead branch 에 고립됐습니다.
2층은 인프라 — SSM agent 가 죽어서 CD 가 명령을 보내도 EC2 가 받지 못했습니다. 그런데 SSM 은 send-command 가 enqueue 만 되면 성공이라고 보고했습니다. silent fail 입니다.
3층은 코드 — Spring Boot 를 4.x 로 올렸는데, springdoc OpenAPI 라이브러리의 2.8.3 버전이 호환되지 않았습니다. 부팅 첫 단계에서 컨텍스트 로드 실패였습니다.
4층은 테스트 — 우리는 @SpringBootTest 가 있었는데, 어느 시점에 누가 @Disabled 를 박아놨습니다. 그래서 ApplicationContext 부팅이 빌드 단계에서 검증되지 않았습니다. springdoc 호환성 결함이 운영 첫 부팅까지 잠재했습니다.
5층은 배포 인지 — dev 머지가 곧 운영 도달이라고 잘못 인식했습니다. 그리고 docker compose pull 이 새 이미지를 가져온다고 가정했는데, 실제로는 캐시된 옛 이미지로 컨테이너를 재시작하는 경우가 있었습니다.
이 다섯 층이 동시에 있는데 표면은 하나 — CORS 401 — 였습니다. 그 한 가지가 모든 층을 가렸습니다.
-->

---

## T-019 의 결정적 교훈

### "표면 증상" 에서 멈추지 말고 "부팅 상태" 부터 검증

운영 API 가 예상 외 응답 (CORS 401, 500, timeout) 을 내면:
**CORS 설정 / 엔드포인트 구현을 의심하기 전에**

```bash
# 진단 첫 명령 — 3줄
docker ps                                          # 컨테이너 살아있나?
docker logs hoseo-moodiary --tail 100              # restart loop 이면 부팅 stack trace
docker inspect hoseo-moodiary --format '{{.Created}}'  # 새 image 진짜 들어왔나?
```

> `.Created` 시각 vs 마지막 main 머지 시각 비교가 **운영 deploy 가 진짜 일어났는지의 가장 빠른 판별**

<!--
T-019 의 결정적 교훈은 단순합니다 — 표면 증상에서 멈추지 마라.
운영 API 가 이상한 응답을 내면, 코드를 의심하기 전에 컨테이너부터 봐야 합니다.
docker ps 로 컨테이너가 살아있는지, docker logs 로 부팅 stack trace 가 있는지, docker inspect 로 컨테이너가 언제 만들어졌는지를 봅니다.
가장 빠른 진단은 docker inspect 의 .Created 시각과 마지막 main 머지 시각을 비교하는 겁니다. main 머지가 1시간 전인데 컨테이너가 1일 전 거면, deploy 가 진짜로 안 일어난 거고 다른 모든 의심은 무의미해집니다.
이 3줄 명령이 사고 이전엔 진단 체크리스트에 없었고, 사고 이후에 가장 먼저 도는 첫 명령이 됐습니다.
-->

---

## T-023 — 옛 CD 들의 silent fail 진실

### 발견 시점
PR #45 (CD 신뢰성 강화 — wait/verify/health check) 가 main 에 처음 들어간 직후 release PR 의 CD 가 **1.5초 만에 fail**.

```
aws: [ERROR]: Waiter CommandExecuted failed:
  matched expected path: "Failed"
```

### 진실
- 워크플로우의 `docker compose` (스페이스) 가 EC2 에서 **unknown sub-command**
- EC2 의 Docker 는 plugin 없는 standalone `docker-compose` 만 가짐

### **더 큰 진실**
> 옛 워크플로우도 같은 명령이었다. **옛 CD 들의 자동 deploy 는 한 번도 진짜로 작동한 적이 없었다.** SSM 의 send-command silent fail 이 그것을 가렸을 뿐. 운영 image 갱신은 모두 수동 SSH 로 일어났다.

PR #45 의 wait/verify 가 비로소 이 진실을 노출.

<!--
T-023 은 그 이전의 silent fail 이 얼마나 오래 가려져 있었는지를 보여주는 사건입니다.
PR 45 에서 CD 신뢰성을 강화했습니다 — SSM 명령 실행 완료까지 wait 하고, health check 까지 추가했습니다. 이게 main 에 처음 들어간 직후, 다음 release PR 의 CD 가 1.5초 만에 fail 했습니다.
원인은 docker compose 와 docker-compose 의 차이였습니다. 워크플로우는 docker compose 라고 스페이스로 썼는데, EC2 의 Docker 는 plugin 이 없어서 standalone docker-compose (하이픈) 만 지원했습니다.
근데 더 큰 진실이 있습니다 — 옛날 워크플로우도 같은 명령이었습니다. 즉, 옛 CD 들의 자동 deploy 는 한 번도 진짜로 작동한 적이 없었습니다.
운영 이미지 갱신은 사람이 EC2 에 SSH 로 들어가서 수동으로 docker pull 을 했던 거였습니다. CD 의 초록불은 가짜였습니다. SSM 의 send-command silent fail 이 그 진실을 가리고 있었던 겁니다.
PR 45 의 wait/verify 가 비로소 이 가짜 초록불을 빨간색으로 노출시킨 겁니다. 진실은 안 좋았지만, 가시화는 가치 있었습니다.
-->

---

## 일반화된 학습 — 가시화의 가치

### 안전망이 없으면 사고는 보이지 않고 *축적된다*

- 옛 CD silent fail (T-019) — 수 개월간 가짜 초록 체크
- `@SpringBootTest` `@Disabled` (T-019) — springdoc 호환성 결함이 운영 첫 부팅까지 잠재
- `docker compose` 명령 invalid (T-023) — silent fail 에 가려 발견 늦음

### 우리가 박은 방어선
1. **ApplicationContext 부팅 테스트** — full bean wiring 을 빌드 단계에서 검증
2. **CD wait/verify** — `send-command` enqueue 가 아니라 실행 결과까지 확인
3. **health check** — swagger 200 이 응답할 때까지 polling
4. **Image 태그 `:${{ github.sha }}` 보관** — 응급 롤백 옵션
5. **`docker inspect .Created` 휴리스틱** — 운영 진단 첫 명령

<!--
이 사고들에서 얻은 일반화된 학습은 가시화의 가치입니다.
안전망이 없으면 사고는 보이지 않는 게 아니라, 보이지 않은 채로 축적됩니다. 그러다 한 검증에서 동시에 노출되어 큰 사고가 됩니다.
저희가 박은 방어선은 다섯 개입니다.
첫째, ApplicationContext 부팅 테스트 — @Disabled 를 영구 제거해서 빈 wiring 결함을 빌드 단계에서 잡습니다.
둘째, CD wait/verify — SSM send-command 가 enqueue 됐다고 성공이 아니라, 명령 실행이 완료될 때까지 wait 합니다.
셋째, health check — 컨테이너가 떴다고 끝이 아니라, swagger 가 200 으로 응답할 때까지 polling 합니다.
넷째, image SHA 태그 — 모든 이미지를 latest 와 commit SHA 두 태그로 푸시해서, 응급 롤백할 수 있게 했습니다.
다섯째, docker inspect .Created — 운영에서 이상이 보이면 가장 먼저 도는 휴리스틱입니다.
이 다섯 방어선이 박힌 후로는 silent fail 시대가 끝났습니다.
-->

---

## 워크플로우 자체에도 규칙 — Claude/사람 협업

### T-015 → T-018 → T-020 — 세 번 재발한 패턴

> 사용자가 GitHub UI 에서 PR 머지하는 시점과 Claude 가 그 사실을 인지하는 시점 사이에 갭

매번 새로운 회색 지대로 재발:
- T-015 — "PR 생성 직전 확인" 만 규칙
- T-018 — "추가 commit push 직전" 확장
- T-020 — `gh pr edit` 같은 메타데이터 변경

### 규칙의 일반화 (CLAUDE.md)
> "회색 지대 한정 열거" 가 아닌 **"`gh pr` 으로 시작하는 거의 모든 명령 직전 `gh pr list` 확인"** 으로 일반화

미래에 새 서브커맨드 추가돼도 자동 적용

<!--
이 발표에서 마지막으로 꺼낼 학습은 — 코드만이 아니라 사람과 도구의 협업 워크플로우 자체에도 규칙이 필요하다는 점입니다.
저는 Claude Code 라는 AI 도구를 활용해서 작업했습니다. 그런데 같은 패턴의 사고가 세 번 재발했습니다. T-015, T-018, T-020 입니다.
패턴은 단순합니다 — 사용자가 GitHub UI 에서 PR 을 머지하면, 그 사실을 AI 가 인지하지 못해서 머지된 PR 에 계속 작업을 추가하는 사고입니다.
첫 번째는 PR 생성 직전 확인 규칙으로 막았고, 두 번째는 추가 commit push 직전 규칙으로 확장했고, 세 번째는 PR 메타데이터 변경 직전 규칙으로 또 확장했습니다.
세 번이 재발한 후에, 규칙을 회색 지대 한정 열거가 아니라 "gh pr 으로 시작하는 거의 모든 명령 직전" 으로 일반화했습니다. 미래에 새 gh 서브커맨드가 추가돼도 자동 적용되는 형태로요.
이 학습이 의미 있는 이유는 — 일반화된 규칙이 유의미한 발전이라는 점입니다. 특정 케이스를 막는 규칙은 곧 다음 회색 지대에 재발합니다. 일반화된 규칙이라야 미래의 변종도 같이 막힙니다.
-->

---

<!-- _class: lead -->

# 5. 앞으로

<!--
마지막 섹션입니다. 남은 작업과, 졸업 발표 이후의 계획을 짧게 보여드리겠습니다.
-->

---

## 외부 합의 대기 중 (병렬 진행 불가)

### PR 4-final — AI 비동기 응답 실어댑터 ⭐⭐⭐
- 현재 Stub 어댑터를 실 HTTP 호출로 교체
- AI 추론 서버 담당자와 응답 포맷 / endpoint / API key 합의 후
- WireMock 통합 테스트 + Retry 정책 (5xx N회)
- 어댑터 한 클래스 교체만 — 골격은 이미 PR 4-pre 에서 운영 반영됨

### PR 8 후반 — 프론트 S3 통합 검증 ⭐⭐⭐
- BE CORS / `compose env` / FE 가이드 ✅ 완료
- FE 가 가이드 따라 S3 셋업 → endpoint URL 회신 대기
- 회신 후 EC2 `.env` 갱신 → 통합 검증

### PR 5 후속 — 캘린더 emoji JOIN
- `Post` × `AiResponse` LEFT JOIN 으로 일자별 이모지 매핑
- PR 4-final 의존

<!--
앞으로 할 일을 세 가지 그룹으로 나눠서 보여드리겠습니다.
첫 그룹은 외부 합의 대기 — 다른 분의 답변이 와야 진행되는 작업들입니다.
PR 4-final 은 AI 추론 서버의 실 HTTP 호출 어댑터입니다. 골격은 PR 4-pre 에서 이미 운영 반영됐고, 실 어댑터는 AI 서버 담당자와 응답 포맷을 합의한 후 한 클래스만 교체하면 됩니다. 이렇게 분리한 이유는 외부 합의 대기로 백엔드 진행이 막히는 걸 피하기 위해서였습니다.
PR 8 후반은 프론트 S3 배포 통합입니다. 백엔드의 CORS, compose env, FE 가이드는 다 완료됐고, 프론트 담당자가 가이드를 따라 S3 를 셋업한 후 endpoint URL 을 회신해주면 백엔드의 .env 를 갱신해서 통합 검증을 합니다.
PR 5 후속은 캘린더에 이모지를 채우는 작업인데, PR 4-final 의 AiResponse 가 실 응답을 가지고 있어야 의미가 있어서, PR 4-final 머지 후 진행됩니다.
-->

---

## 단독 진행 가능 (외부 의존 0)

### PR 6 — Flyway 도입 ⭐
- `ddl-auto: update` 의 한계 영구 해소
- 베이스라인 SQL 작성 + `ddl-auto: validate` 전환
- PR 4-final 머지 후 스키마 안정화 시점에

### PR 9 — MkDocs 문서 사이트 📖 ⭐
- 이미 들어간 GitHub Pages 인프라 위에 `claude-docs/*` 통합
- 졸업 심사 / 교수 / 외부 공유용 "한 페이지에서 모든 것"

### PR 11 — OAuth2 Resource Server 마이그레이션 🔐 (선택)
- 수동 JWT 파싱 → Spring Security 표준 Resource Server
- 졸업 프로젝트 범위 초과 가능 — "시간 남으면"

### PR 12 — 소셜 로그인 (Google + Kakao) 🔐 ⭐⭐⭐
- 데모 가시성 — "구글로 로그인 한 줄" 임팩트 큼

<!--
두 번째 그룹은 단독 진행 가능한 작업들 — 외부 합의 없이 백엔드 단독으로 진행할 수 있는 백로그입니다.
PR 6 Flyway 가 가장 우선순위입니다. 앞에서 보여드린 ddl-auto update 의 한계를 영구 해소하는 작업입니다. 베이스라인 SQL 을 작성해서 운영 스키마를 잠그고, ddl-auto 를 validate 로 전환합니다. PR 4-final 의 스키마까지 안정된 후에 진행합니다.
PR 9 MkDocs 는 문서 사이트입니다. 이미 GitHub Pages 인프라가 들어가 있어서, 그 위에 claude-docs 폴더의 모든 markdown 을 통합하는 작업입니다. 졸업 심사나 교수님께 보여드릴 단일 URL 을 확보할 수 있습니다.
PR 11 의 OAuth2 Resource Server 는 현재 수동으로 JWT 를 파싱하는 코드를 Spring Security 표준으로 교체하는 리팩토링입니다. 졸업 프로젝트 범위를 초과할 수 있어서 시간 남으면 진행하는 카테고리입니다.
PR 12 의 소셜 로그인은 졸업 발표의 가시성 측면에서 가장 임팩트가 큽니다 — 구글로 로그인 한 줄로 데모 인상이 확 달라집니다.
-->

---

## 먼 미래 (졸업 발표 이후)

### PR 7 — ECS / ALB / ACM / 도메인 ⭐
- HTTPS 화 + 도메인 + ALB + ACM 인증서
- 운영 안정성 + 확장성 + CloudWatch 로그 통합
- 졸업프로젝트 범위 초과 — "시간 남으면"

### 운영 관측성
- CloudWatch 알람 (5xx 비율, 컨테이너 죽음, RDS 연결 실패)
- 메트릭 대시보드 (응답 시간, 동시 접속자)
- 로그 검색 (CloudWatch Logs Insights)

<!--
마지막은 졸업 발표 이후의 먼 미래 계획입니다.
PR 7 은 ECS 이전과 HTTPS 화입니다. 현재는 EC2 위에 Docker 컨테이너로 단순 배포하고 있는데, ECS 로 옮기면서 ALB 와 ACM 인증서를 붙여서 HTTPS 와 도메인을 도입하는 작업입니다. 졸업 프로젝트 범위는 분명히 초과하니까, 시간이 남으면 진행합니다.
운영 관측성도 같은 범위입니다. 지금은 docker logs 로 한 컨테이너만 보고 있는데, CloudWatch 로 알람과 메트릭 대시보드를 붙이면 운영 안정성이 한 단계 올라갑니다.
이 영역은 모두 졸업 발표가 끝난 후에, 만약 이 프로젝트를 계속 운영한다면 진행할 후보들입니다.
-->

---

## 마치며 — 숫자로 보는 성과

- **63 PRs** 머지 — 모든 변경이 PR 단위로 리뷰되고 history 보존
- **27 트러블슈팅 항목** (T-001 ~ T-027) — 막힌 지점이 다음 사람의 시작점이 되도록
- **80+ 테스트** 0 failures — 슬라이스 + 단위 + 부팅 안전망
- **4 테이블 / 4 핵심 정책** — UUID PK / Auditing / utf8mb4 / 명시 메서드
- **5층 결함 대사건** 복구 — silent fail 시대를 끝내고 방어선 박음
- **운영 정상화** — CD success 가 진짜 deploy 성공을 의미하게 됨

### 가장 큰 학습
> 사고는 단일 결함이 아니라 **여러 silent fail 의 축적**일 수 있다.
> 안전망과 가시화는 *기능* 만큼 중요하다.

<!--
숫자로 정리하면 — 63개 PR 머지, 27개 트러블슈팅 항목, 80개가 넘는 테스트, 4개 테이블과 4개 핵심 DB 정책, 그리고 5층 결함 대사건의 복구입니다.
모든 변경이 PR 단위로 분리되어 있어서, 어떤 결정이 언제 왜 들어갔는지가 git history 에 보존되어 있습니다. 미래의 작업자 — 또는 미래의 저 자신 — 가 이 코드를 다시 봤을 때 빠르게 맥락을 잡을 수 있습니다.
가장 큰 학습은 처음과 마지막에 같은 문장으로 묶입니다 — 사고는 단일 결함이 아니라 여러 silent fail 의 축적일 수 있다. 그래서 안전망과 가시화는 기능을 만드는 것만큼 중요합니다.
이번 졸업 프로젝트의 백엔드 작업에서 가장 가치 있게 얻은 것은, 기능 자체보다 이 방어선들이라고 생각합니다.
-->

---

<!-- _class: lead -->

# 감사합니다

### Q&A

📚 **문서**:
- README.md — 프로젝트 소개 + 아키텍처 + DB + 인프라 + 보안
- claude-docs/plan.md — PR 단위 로드맵
- claude-docs/troubleshooting.md — T-### 트러블슈팅 로그
- claude-docs/security.md — JWT / Refresh / CORS / 인증 / 약점
- claude-docs/api-contracts.md — API 명세 + 외부 AI 계약

🔗 **운영**: `http://15.165.95.129:8080/swagger-ui/index.html`

<!--
발표는 여기까지입니다. 들어주셔서 감사합니다.
모든 문서와 코드는 GitHub 의 공개 레포에 있고, README, plan, troubleshooting, security, api-contracts 다섯 문서가 각각 다른 관점의 진실의 원천입니다.
운영 서버는 발표 시점에도 살아있어서, Swagger UI 로 직접 호출 테스트가 가능합니다.
질문 받겠습니다.
-->
