# API Contracts

> Moodiary 백엔드가 노출하거나 의존하는 API의 **상세 명세**. FE 와 공유하는 단일 진실의 원천.
>
> - **목적이 다른 두 문서**:
>   - [`plan.md`](./plan.md): *왜 / 언제 / 우선순위*
>   - 이 문서: *어떻게 생겼나 (URL / Method / 헤더 / request / response / status / 예시)*
> - **갱신 규칙**: API를 새로 만들거나 응답 포맷을 바꿀 때 함께 갱신. PR description에 "spec 갱신" 한 줄 추가.
> - **실시간 진실의 원천 (구현된 API)**: [Swagger UI](http://15.165.95.129:8080/swagger-ui/index.html). 이 문서는 *설계 의도 + 헤더 요구사항 + 외부 계약* 을 보강.

---

## 📑 목차

- [엔드포인트 요약 (한눈 표)](#엔드포인트-요약-한눈-표)
- [공통 규약](#공통-규약)
  - [공통 헤더](#공통-헤더)
  - [Content-Type / 인코딩](#content-type--인코딩)
  - [날짜·시간](#날짜시간)
  - [ID](#id)
  - [에러 응답 — 공통 포맷](#에러-응답--공통-포맷)
  - [페이지네이션](#페이지네이션)
  - [인증](#인증)
  - [CORS](#cors)
- [구현된 API](#구현된-api)
  - [Auth](#auth)
    - [POST /auth/signup](#post-authsignup)
    - [POST /auth/login](#post-authlogin)
    - [POST /auth/refresh](#post-authrefresh)
    - [POST /auth/logout](#post-authlogout)
  - [Post](#post)
    - [POST /post](#post-post)
    - [GET /post](#get-post)
    - [GET /post/{id}](#get-postid)
    - [PUT /post/{id}](#put-postid)
    - [DELETE /post/{id}](#delete-postid)
  - [Calendar](#calendar)
    - [GET /calendar](#get-calendar)
  - [AI Response (🚧 Stub — PR 4-pre)](#ai-response--stub--pr-4-pre)
    - [GET /post/{id}/ai-response](#get-postidai-response)
- [외부 시스템 계약](#외부-시스템-계약)
  - [AI 추론 서버 (PR 4-final)](#ai-추론-서버-pr-4-final)

---

## 엔드포인트 요약 (한눈 표)

> FE 개발자용 치트시트. **인증 필요** = `Authorization: Bearer <accessToken>` 헤더 첨부 필수. **Body** = 요청 본문 존재 시 `Content-Type: application/json` 필수.

| 기능 | Method | URL | 인증 헤더 | Body | 성공 응답 |
|---|---|---|---|---|---|
| 회원가입 | `POST` | `/auth/signup` | ❌ 불필요 | ✅ JSON | `201` UUID 문자열 |
| 로그인 | `POST` | `/auth/login` | ❌ 불필요 | ✅ JSON | `200` `{ accessToken, refreshToken, userId, nickname }` |
| 토큰 갱신 | `POST` | `/auth/refresh` | ❌ 불필요 | ✅ JSON | `200` `{ accessToken, refreshToken }` (rotation) |
| 로그아웃 | `POST` | `/auth/logout` | ❌ 불필요 | ✅ JSON | `204` (바디 없음) |
| 게시글 작성 | `POST` | `/post` | ✅ 필수 | ✅ JSON | `201` UUID 문자열 |
| 내 게시글 전체 조회 | `GET` | `/post` | ✅ 필수 | ❌ | `200` `PostResponseDto[]` |
| 게시글 단건 조회 | `GET` | `/post/{id}` | ✅ 필수 | ❌ | `200` `PostResponseDto` |
| 게시글 수정 | `PUT` | `/post/{id}` | ✅ 필수 | ✅ JSON | `200` `PostResponseDto` |
| 게시글 삭제 | `DELETE` | `/post/{id}` | ✅ 필수 | ❌ | `204` (바디 없음) |
| 월별 캘린더 조회 | `GET` | `/calendar?year=YYYY&month=MM` | ✅ 필수 | ❌ | `200` `CalendarDayResponseDto[]` |
| AI 응답 폴링 (🚧 Stub) | `GET` | `/post/{id}/ai-response` | ✅ 필수 | ❌ | `200` 상태별 payload |

**화이트리스트 (인증 불필요)** — Spring Security `SecurityConfig.WHITELIST`:
- `/auth/**` — 회원가입/로그인은 인증 자체가 불가능
- `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`, `/swagger-resources/**` — 문서/스펙
- `/error` — 스프링 기본 에러 디스패치

그 외 모든 요청은 `authenticated()` — 토큰 없거나 잘못되면 `401 { "message": "인증이 필요합니다." }`.

---

## 공통 규약

### 공통 헤더

| 헤더 | 언제 | 값 |
|---|---|---|
| `Authorization` | 보호된 엔드포인트 (위 표에서 ✅) | `Bearer <accessToken>` — 로그인 응답의 `accessToken` |
| `Content-Type` | request body 가 있을 때 | `application/json; charset=UTF-8` |
| `Accept` | (권장) | `application/json` |

**응답 헤더** — 서버가 항상 `Content-Type: application/json; charset=UTF-8` 로 응답 (에러 포함). CORS preflight 응답은 `Access-Control-Allow-*` 헤더가 자동 첨부됨.

### Content-Type / 인코딩
- 요청 / 응답 모두 `application/json; charset=UTF-8`
- 이모지 포함 가능 → DB는 **`utf8mb4`** 필수, 응답 인코딩도 UTF-8 강제

### 날짜·시간
- 저장: **UTC** (DB `DATETIME(6)`)
- 응답: **ISO-8601 with offset** (`2026-05-24T13:14:15.123+09:00`) 또는 **KST 명시 ISO** — 코드에서 일관 유지
- 캘린더 등 **"일자" 단위** 그룹핑은 **KST(`Asia/Seoul`) 기준**
- 캘린더 응답의 `date` 는 `yyyy-MM-dd` (시간 없음)

### ID
- 모든 엔티티 PK = **UUID v4** (`@UuidGenerator`)
- URL Path / JSON 모두 표준 36자 표기 (`9c4d401e-63ba-413e-abbe-a6d5cf869f0e`)

### 에러 응답 — 공통 포맷
모든 4xx/5xx 응답은 동일한 envelope:

```json
{ "message": "사람이 읽을 수 있는 한국어 메시지" }
```

상태 코드별 의미:

| HTTP | 의미 | 핸들러 |
|---|---|---|
| `400` | Validation 실패 (`@NotBlank`, `@Email`, `@Pattern`, `@Size` 등) | `MethodArgumentNotValidException` |
| `400` | JSON 파싱 실패 / 형식 오류 | `HttpMessageNotReadableException` |
| `400` | 필수 쿼리 파라미터 누락 | `MissingServletRequestParameterException` |
| `401` | 인증 누락 / 토큰 무효 / 만료 | `SecurityConfig` authenticationEntryPoint |
| `401` | 로그인 시 이메일/비밀번호 불일치 | `InvalidCredentialsException` |
| `403` | 인증은 했지만 권한 없음 (본인 글이 아님 등) | `ForbiddenException` 등 도메인 예외 |
| `404` | 리소스 없음 | `PostNotFoundException` 등 도메인 예외 |
| `409` | 충돌 (이메일/닉네임 중복) | `DuplicateEmailException` / `DuplicateNicknameException` |
| `500` | 서버 오류 (예상 외) | `Exception` fallback |

> 🛑 **breaking change 금지**: 이 envelope 모양(`{"message": "..."}`)을 깨면 프론트가 깨집니다. 필드 추가는 OK, 제거/이름 변경은 X.

### 페이지네이션
- 현재 미적용 (Post 전체 조회는 그냥 List 반환 — 본인 글만)
- 추후 도입 시 Spring Data 표준 `Page<T>` 형태 (`content`, `totalElements`, `totalPages`, `number`)

### 인증
- 헤더: `Authorization: Bearer <accessToken>` — 로그인 응답에서 받은 JWT
- 화이트리스트 (위 "엔드포인트 요약 (한눈 표)" 참조)
- 그 외 모든 엔드포인트는 토큰 필수, 미인증 → `401 { "message": "인증이 필요합니다." }`
- 토큰 검증 흐름: `JwtAuthenticationFilter` 가 헤더 파싱 → `JwtTokenProvider` 가 서명/만료 검증 → `SecurityContext` 에 `UUID userId` 를 principal 로 박음 → 컨트롤러는 `@AuthenticationPrincipal UUID userId` 로 받는다
- 상세 (JWT 구조 / 시크릿 관리 / 약점) → [`security.md`](./security.md)

### CORS
- 허용 origin 만 호출 가능. 기본값: 로컬 dev (`http://localhost:3000`, `http://localhost:5173`)
- 운영은 EC2 `.env` 의 `APP_CORS_ALLOWED_ORIGINS` 환경변수로 S3 endpoint URL 추가
- 와일드카드(*) 허용 X — `allowCredentials=true` 와 충돌하므로 정확한 URL 만
- 허용 메서드: `GET / POST / PUT / DELETE / OPTIONS`
- 허용 헤더: `Authorization` / `Content-Type` / `Accept`
- preflight (OPTIONS) 캐시: 3600초
- 미허용 origin → 403

---

## 구현된 API

> Source of truth: Swagger UI. 아래는 빠른 참조용 요약 + 의도 설명.

### Auth

> 화이트리스트 — **인증 헤더 불필요**. 회원가입/로그인은 인증 자체가 불가능하므로 토큰 없이 호출.

#### `POST /auth/signup`
회원가입. 비밀번호는 BCrypt로 해시되어 저장.

**Headers**
```
Content-Type: application/json
```

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "무디아리"
}
```

- `email` (string, **required**) — `@Email` 형식 검증
- `password` (string, **required**) — **영문+숫자 조합 8자 이상**, 특수문자 불가. 정규식 `^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$`
- `nickname` (string, **required**) — 2~20자

**Response — 201 Created**
```
"9c4d401e-63ba-413e-abbe-a6d5cf869f0e"
```
> ⚠️ 응답 바디는 **UUID 문자열 한 줄** (객체 아님). FE는 `response.text()` 또는 `response.json()` 으로 string 으로 받는다.

**에러**
- `400` — `{"message": "이메일 형식이 올바르지 않습니다."}` / `{"message": "비밀번호는 영문과 숫자를 포함한 8자 이상이어야 합니다."}` / `{"message": "닉네임은 2자 이상 20자 이하여야 합니다."}` 등
- `409` — `{"message": "이미 가입된 이메일입니다."}` / `{"message": "이미 사용 중인 닉네임입니다."}`

---

#### `POST /auth/login`
이메일/비밀번호로 로그인. 성공 시 access token (1시간 수명) + refresh token (2주 수명) 같이 발급. 이후 보호된 엔드포인트는 `Authorization: Bearer <accessToken>` 헤더 첨부.

**Headers**
```
Content-Type: application/json
```

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response — 200 OK**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "Xy7-aBcD...",
  "userId": "9c4d401e-63ba-413e-abbe-a6d5cf869f0e",
  "nickname": "무디"
}
```

- `accessToken` — JWT (HS256). 1시간 수명. 이후 모든 보호된 요청의 `Authorization` 헤더에 `Bearer <accessToken>` 형태로 첨부
- `refreshToken` — 32-byte secure random (base64). **2주 수명**. access 만료 시 `POST /auth/refresh` 로 갱신
- `userId` — 로그인된 사용자 UUID (FE 가 캐싱해두면 편리)
- `nickname` — 로그인된 사용자의 닉네임 (전역 유니크, 최대 20자). FE 헤더/프로필 표시용. LOCAL / OAuth2 로그인 양쪽 동일하게 포함.

> 📌 **토큰 수명** (PR 10):
> - access token = **1시간** (`JWT_ACCESS_EXPIRATION_MS`, 기본 3,600,000ms)
> - refresh token = **2주** (`JWT_REFRESH_EXPIRATION_MS`, 기본 1,209,600,000ms)
> - **Rotation 적용**: refresh 호출마다 기존 토큰 무효화 + 새 토큰 발급 → 탈취된 refresh 가 한 번만 유효

> ⚠️ **FE 보관 정책**: 두 토큰 모두 localStorage 저장 (현재 패턴). XSS 노출 시 둘 다 위험 — 향후 cookie 전환 시점은 PR 11/12 후속에서 재검토.

**에러**
- `400` — 이메일/비밀번호 빈 값 (`@NotBlank`)
- `401` — `{"message": "이메일 또는 비밀번호가 올바르지 않습니다."}` (이메일 미존재와 비번 틀림을 구분하지 않음 — 사용자 enumeration 방지)

---

#### `POST /auth/refresh`
Refresh token 으로 새 access + 새 refresh 발급. **Rotation 적용** — 기존 refresh 는 즉시 무효화.

**Headers**
```
Content-Type: application/json
```

**Request**
```json
{
  "refreshToken": "Xy7-aBcD..."
}
```

**Response — 200 OK**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "AnotherRandomToken..."
}
```

- 응답으로 받은 **새 refresh 를 즉시 저장** + 기존 refresh 는 폐기. 동일 refresh 로 두 번 호출 시 두 번째는 **401**.

**FE 호출 패턴**:
```typescript
// access 만료 401 받으면 자동 갱신 → 원래 요청 재시도
async function refreshToken() {
  const stored = localStorage.getItem('refreshToken');
  const res = await fetch('/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: stored })
  });
  if (!res.ok) {
    // refresh 도 만료 → 재로그인 페이지로
    return null;
  }
  const { accessToken, refreshToken } = await res.json();
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
  return accessToken;
}
```

**에러**
- `400` — `refreshToken` 빈 값 (`@NotBlank`)
- `401` — `{"message": "유효하지 않은 refresh token 입니다."}` (존재 X / 만료 / 이미 revoke — 정보 노출 최소화 위해 셋 다 같은 메시지)

---

#### `POST /auth/logout`
Refresh token 을 무효화. Access token 은 stateless 라 서버에서 즉시 차단 불가 — **FE 는 logout 호출과 함께 localStorage 의 토큰들도 즉시 제거**해야 함.

**Headers**
```
Content-Type: application/json
```

**Request**
```json
{
  "refreshToken": "Xy7-aBcD..."
}
```

**Response — 204 No Content** (응답 body 없음)

> 📌 **Idempotent** — 이미 무효화된 토큰 / 존재하지 않는 토큰을 보내도 204. 사용자가 logout 을 두 번 눌러도 동일.

**FE 호출 패턴**:
```typescript
async function logout() {
  const refresh = localStorage.getItem('refreshToken');
  if (refresh) {
    await fetch('/auth/logout', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: refresh })
    });
  }
  // BE 응답 무관하게 로컬도 즉시 클리어
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  // 로그인 페이지로 redirect
}
```

**에러**
- `400` — `refreshToken` 빈 값

---

### Post

> 모든 Post 엔드포인트는 **인증 헤더 필수** (`Authorization: Bearer <accessToken>`). 소유권 검증 — 본인 글만 조회/수정/삭제 가능. 다른 사람 글 접근 시 `403`.

#### `POST /post`
게시글 생성. 작성자는 토큰에서 추출한 현재 사용자로 자동 설정.

**Headers**
```
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request**
```json
{
  "title": "오늘의 기분",
  "content": "오늘은 기분이 좋았다.",
  "postDate": "2026-05-29"
}
```

- `title` (string, **required**, `@NotBlank`)
- `content` (string, **required**, `@NotBlank`)
- `postDate` (string `yyyy-MM-dd`, **optional**) — 일기 날짜 (entry date). "지나간 날짜의 일기" 작성 시 사용. 누락 시 서버가 오늘 (`LocalDate.now()`) 로 폴백. `createdAt` (작성 시점, 자동) 과 별개.

**Response — 201 Created**
```
"9c4d401e-63ba-413e-abbe-a6d5cf869f0e"
```
> ⚠️ 응답 바디는 **UUID 문자열 한 줄** (객체 아님).

**에러**
- `400` — `{"message": "제목은 필수입니다."}` / `{"message": "내용은 필수입니다."}` / `{"message": "요청 형식이 올바르지 않습니다."}`
- `401` — 인증 누락/실패

---

#### `GET /post`
**내** 게시글 전체 조회. 다른 사용자의 글은 절대 포함 안 됨.

**Headers**
```
Authorization: Bearer <accessToken>
```

**Response — 200 OK**
```json
[
  {
    "id": "9c4d401e-63ba-413e-abbe-a6d5cf869f0e",
    "title": "오늘의 기분",
    "content": "오늘은 기분이 좋았다.",
    "postDate": "2026-05-29"
  }
]
```

빈 결과면 `[]`.

**에러**
- `401` — 인증 누락/실패

> 📌 페이지네이션 없음 (TODO).

---

#### `GET /post/{id}`
단건 조회. 본인 글만 가능.

**Headers**
```
Authorization: Bearer <accessToken>
```

**Path**
- `id` — UUID

**Response — 200 OK**
```json
{
  "id": "9c4d401e-63ba-413e-abbe-a6d5cf869f0e",
  "title": "오늘의 기분",
  "content": "오늘은 기분이 좋았다.",
  "postDate": "2026-05-29"
}
```

**에러**
- `401` — 인증 누락/실패
- `403` — `{"message": "본인 글만 조회할 수 있습니다."}` (다른 사람 글)
- `404` — `{"message": "게시글을 찾을 수 없습니다. id=<uuid>"}`

---

#### `PUT /post/{id}`
수정 (전체 대체). 본인 글만 수정 가능.

**Headers**
```
Authorization: Bearer <accessToken>
Content-Type: application/json
```

**Request**
```json
{
  "title": "수정된 제목",
  "content": "수정된 본문",
  "postDate": "2026-05-28"
}
```
- body는 `POST /post`와 동일한 검증 + `postDate` 도 동일 시맨틱 (선택, 누락 시 오늘 폴백). 날짜 잘못 적었을 때 수정 가능.

**Response — 200 OK**
```json
{
  "id": "9c4d401e-63ba-413e-abbe-a6d5cf869f0e",
  "title": "수정된 제목",
  "content": "수정된 본문",
  "postDate": "2026-05-28"
}
```
> 📌 POST 와 달리 객체 반환 (UUID 문자열 아님).

**에러**
- `400` — validation 실패
- `401` — 인증 누락/실패
- `403` — 본인 글 아님
- `404` — 존재하지 않는 ID

---

#### `DELETE /post/{id}`
삭제. 본인 글만 가능.

**Headers**
```
Authorization: Bearer <accessToken>
```

**Response — 204 No Content**
바디 없음.

**에러**
- `401` — 인증 누락/실패
- `403` — 본인 글 아님
- `404` — 존재하지 않는 ID

---

### Calendar

#### `GET /calendar`
월별 캘린더 데이터 — **그 달 전체 일자**를 배열로 반환. 글 없는 날도 `postId: null, emoji: null` 로 포함하므로 FE 는 인덱스로 격자에 바로 매핑할 수 있다.

> 🚧 **현재 시점**: `emoji` 는 **항상 `null`**. `Post + AiResponse JOIN` 은 PR 4 (AI 비동기 응답) 머지 후 후속 PR 에서 추가. FE 는 `emoji == null` 이면 placeholder(회색 점 등) 로 처리.

**Headers**
```
Authorization: Bearer <accessToken>
```

**Query**
- `year` (int, **required**, 범위 2020 ~ 현재+1)
- `month` (int, **required**, 1-12)

**예시 호출**
```
GET /calendar?year=2026&month=5
```

**Response — 200 OK** (예: 2026-05, **현재 시점**)
```json
[
  { "date": "2026-05-01", "postId": null,                                    "emoji": null },
  { "date": "2026-05-02", "postId": "9c4d401e-63ba-413e-abbe-a6d5cf869f0e", "emoji": null },
  { "date": "2026-05-03", "postId": "ab12cd34-1111-2222-3333-444455556666", "emoji": null },
  ...
  { "date": "2026-05-31", "postId": null,                                    "emoji": null }
]
```

**Response — 200 OK** (PR 4 + 후속 emoji JOIN 머지 후 예상)
```json
[
  { "date": "2026-05-02", "postId": "9c4d401e-...", "emoji": "😊" },
  { "date": "2026-05-03", "postId": "ab12cd34-...", "emoji": "😢" }
]
```

**규칙**:
- 응답 배열 길이 = **그 달의 실제 일수** (28/29/30/31)
- 하루에 여러 글이 있으면 **마지막 글(`created_at` MAX)의 postId** 만 노출
- 일자 그룹핑은 **KST 기준** (Asia/Seoul)
- AI 응답이 `PENDING`/`FAILED`이거나 PR 4 미구현 → `emoji: null` (postId는 채워짐)

**에러**
- `400` — 잘못된 year/month 값 (`{"message": "year는 2020 이상, month는 1-12 사이여야 합니다."}`)
- `400` — 필수 쿼리 파라미터 누락 (`{"message": "필수 파라미터가 누락되었습니다: year"}`)
- `401` — 인증 누락/실패

---

### AI Response (🚧 Stub — PR 4-pre)

> 🚧 **현재 (PR 4-pre, Stub 모드)**: 실제 AI 서버 호출 없이 고정 응답 (`emoji: "😊"`) 을 즉시 반환. `POST /post` 직후 거의 즉시 `DONE` 으로 전이되어 폴링 한 번이면 결과 도달.
>
> **PR 4-final (예정)**: 실제 HTTP 어댑터 + Retry + 타임아웃. 외부 AI 서버 계약은 [아래 외부 시스템 계약 섹션](#ai-추론-서버-pr-4-final) 참조 — 합의 완료 후 어댑터만 교체.

#### `GET /post/{id}/ai-response`
사용자가 일기를 작성하면 즉시 `POST /post` 가 201 을 반환하고, **AI 응답은 비동기로 처리**된다. 프론트는 이 엔드포인트를 폴링해서 완료 여부를 확인.

흐름: `POST /post` (201, 일기 저장 + `AiResponse(PENDING)` 동시 저장 + 비동기 트리거) → FE 폴링 → `status: PENDING` 또는 `DONE` / `FAILED`.

**Headers**
```
Authorization: Bearer <accessToken>
```

**Response — 200 OK** (상태에 따라)

`status: PENDING` — Async 워커가 아직 결과를 못 받은 상태. `errorMessage` 키는 응답에 없음 (`@JsonInclude(NON_NULL)`).
```json
{
  "postId": "9c4d401e-...",
  "status": "PENDING",
  "content": null,
  "emoji": null
}
```

`status: DONE` — 추론 성공. `errorMessage` 키는 응답에 없음.
```json
{
  "postId": "9c4d401e-...",
  "status": "DONE",
  "content": "오늘 기분이 좋으셨군요! 그 순간을 더 자세히 떠올려보세요.",
  "emoji": "😊"
}
```

`status: FAILED` — 추론 실패. `content / emoji` 는 null, `errorMessage` 에 사유.
```json
{
  "postId": "9c4d401e-...",
  "status": "FAILED",
  "content": null,
  "emoji": null,
  "errorMessage": "AI 서버 응답 시간 초과"
}
```

**필드 명시 정책**: `errorMessage` 는 `FAILED` 일 때만 응답에 포함된다 (`@JsonInclude(NON_NULL)`). `PENDING / DONE` 응답에는 `errorMessage` 키 자체가 없다. `content / emoji` 는 비대칭 없이 모든 상태에서 키가 있고 null 일 수 있음.

**에러**
- `401` — 인증 누락/실패
- `403` — 본인 글이 아님 (Post 존재 노출 방지를 위해 404 보다 우선 검증)
- `404` — `postId` 가 존재하지 않음. 메시지: `"AI 응답을 찾을 수 없습니다. postId=<uuid>"`

> 📌 **FE 폴링 정책 합의 필요**: 1초 간격? 지수 백오프? → 합의 후 결정. Stub 단계에선 거의 즉시 DONE 이라 폴링 1~2회로 충분.

---

## 외부 시스템 계약

> 우리가 **호출하는** 서버의 API. Swagger에는 안 나옴. 여기서 합의 → 변경 시 양쪽 동기화.

### AI 추론 서버 (PR 4-final)

#### 우리가 보낼 요청
> ⚠️ **AI 담당자와 합의 필요**. 아래는 우리 측 제안 초안.

```http
POST {AI_SERVER_URL}/inference
Authorization: Bearer <AI_API_KEY>  (또는 합의된 인증 방식)
Content-Type: application/json

{
  "postId": "9c4d401e-...",
  "title": "오늘의 기분",
  "content": "오늘은 기분이 좋았다. 친구를 만나서..."
}
```

#### 우리가 기대하는 응답 (성공)
```json
{
  "content": "AI가 생성한 응답 텍스트",
  "emoji": "😊"
}
```

- `content` (string, required) — 사용자 일기에 대한 AI 응답 본문
- `emoji` (string, required) — **유니코드 이모지 1자**. 사용자의 기분을 나타냄

#### 합의 항목 (체크리스트)
- [ ] **URL**: 어디?
- [ ] **인증**: API 키 헤더? IAM? 미인증? → 결정 후 GitHub Secrets에 추가
- [ ] **응답 포맷**: 위 모양 그대로? 다르면 어댑터 필요
- [ ] **응답 시간**: 평균 / p95 / 타임아웃 기준 (→ 우리 `@Async` 스레드풀 사이즈 / Retry 정책 결정)
- [ ] **이모지 후보 풀**: AI가 어떤 이모지 세트를 사용? (캘린더 UI 일관성)
  - 예: `😊 😢 😡 😴 😍 🤔 😎 🥰 😭 ...` 같은 닫힌 집합인지, 아니면 개방형인지
- [ ] **에러 응답 포맷**: 실패 시 어떤 모양으로 오는지 (HTTP status + body)
- [ ] **레이트 리미트**: 분당 호출 한도?
- [ ] **요청 본문**: title 분리 보낼지, content만 보낼지

#### 우리 측 실패 처리
| 시나리오 | 우리 행동 |
|---|---|
| 응답 시간 초과 (예: 30초) | `AiResponse.status = FAILED`, `errorMessage = "AI 서버 응답 시간 초과"` |
| HTTP 5xx 응답 | Spring Retry로 N회 재시도 후 FAILED |
| HTTP 4xx 응답 | 재시도 없이 FAILED (요청이 잘못된 경우) |
| 응답 파싱 실패 (필드 누락 등) | FAILED, errorMessage에 파싱 에러 |

→ **일기 자체는 무조건 저장 성공**. AI 응답 실패는 일기 작성 흐름을 막지 않음.

---

## 📝 Changelog

| 일자 | 변경 |
|---|---|
| 2026-05-24 | 신설. 공통 규약 / 구현된 Post CRUD 5개 / 예정 API (Auth, AI 응답 폴링, Calendar) / 외부 AI 서버 계약 명세 |
| 2026-05-25 | PR 5 Calendar API 진행 중. `GET /calendar?year=YYYY&month=MM` 구현 — `emoji` 는 PR 4 머지 전까지 항상 null 임시 처리. 공통 규약에 `MissingServletRequestParameterException` 400 매핑 명시. |
| 2026-05-26 | FE 공유용 정비. **엔드포인트 요약 표** 추가 (URL/Method/인증 헤더/Body/응답 한눈에). Auth (`/auth/signup`, `/auth/login`) 와 Calendar (`/calendar`) 를 "예정" → "구현된 API" 로 이동. Post CRUD 에 실제 구현된 인증 헤더 (`Authorization: Bearer`) + 소유권 401/403 매핑 반영. 회원가입 비밀번호 정책 (`^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$`) 명시. 로그인 응답에 `userId` 포함 명시. `409` (이메일/닉네임 중복) 에러 코드 추가. 공통 헤더 섹션 신설. |
| 2026-05-27 | PR 4-pre 머지. `GET /post/{id}/ai-response` 를 "예정 API" → "구현된 API (🚧 Stub 모드)" 로 이동. Stub 박스 + errorMessage 분기 정책 (`@JsonInclude(NON_NULL)`, FAILED 만 포함) 명시. 엔드포인트 요약 표에 "🚧 Stub" 표시. 외부 시스템 계약 섹션 헤더 "PR 4" → "PR 4-final" 로 명확화. `POST /post` 가 일기 + `AiResponse(PENDING)` 같은 트랜잭션 저장 후 비동기 트리거하는 흐름 추가. |
