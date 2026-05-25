# API Contracts

> Moodiary 백엔드가 노출하거나 의존하는 API의 **상세 명세**.
>
> - **목적이 다른 두 문서**:
>   - [`plan.md`](./plan.md): *왜 / 언제 / 우선순위*
>   - 이 문서: *어떻게 생겼나 (request / response / status / 예시)*
> - **갱신 규칙**: API를 새로 만들거나 응답 포맷을 바꿀 때 함께 갱신. PR description에 "spec 갱신" 한 줄 추가.
> - **실시간 진실의 원천 (구현된 API)**: [Swagger UI](http://15.165.95.129:8080/swagger-ui/index.html). 이 문서는 *설계 의도 + 예시 + 외부 계약*을 보강.

---

## 📑 목차

- [공통 규약](#공통-규약)
- [구현된 API](#구현된-api)
  - [POST /post](#post-post)
  - [GET /post](#get-post)
  - [GET /post/{id}](#get-postid)
  - [PUT /post/{id}](#put-postid)
  - [DELETE /post/{id}](#delete-postid)
- [예정 API](#예정-api)
  - [Auth (PR 2)](#auth-pr-2)
  - [AI Response 폴링 (PR 4)](#ai-response-폴링-pr-4)
  - [Calendar (PR 5)](#calendar-pr-5)
- [외부 시스템 계약](#외부-시스템-계약)
  - [AI 추론 서버 (PR 4)](#ai-추론-서버-pr-4)

---

## 공통 규약

### Content-Type
- 요청 / 응답 모두 `application/json; charset=UTF-8`
- 이모지 포함 가능 → DB는 **`utf8mb4`** 필수, 응답 인코딩도 UTF-8 강제

### 날짜·시간
- 저장: **UTC** (DB `DATETIME(6)`)
- 응답: **ISO-8601 with offset** (`2026-05-24T13:14:15.123+09:00`) 또는 **KST 명시 ISO** — 코드에서 일관 유지
- 캘린더 등 **"일자" 단위** 그룹핑은 **KST(`Asia/Seoul`) 기준**

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
| `400` | Validation 실패 (`@NotBlank` 등) | `MethodArgumentNotValidException` |
| `400` | JSON 파싱 실패 / 형식 오류 | `HttpMessageNotReadableException` |
| `400` | 필수 쿼리 파라미터 누락 | `MissingServletRequestParameterException` (PR 5 추가) |
| `401` | 인증 누락/실패 | (PR 2에서 추가) |
| `403` | 인증은 했지만 권한 없음 | (PR 3에서 추가, 본인 글 아닌데 수정/삭제 시도) |
| `404` | 리소스 없음 | `PostNotFoundException` 등 도메인 예외 |
| `500` | 서버 오류 (예상 외) | `Exception` fallback |

> 🛑 **breaking change 금지**: 이 envelope 모양(`{"message": "..."}`)을 깨면 프론트가 깨집니다. 필드 추가는 OK, 제거/이름 변경은 X.

### 페이지네이션
- 현재 미적용 (Post 전체 조회는 그냥 List 반환)
- 추후 도입 시 Spring Data 표준 `Page<T>` 형태 (`content`, `totalElements`, `totalPages`, `number`)

### 인증
- 헤더: `Authorization: Bearer <JWT>`
- 화이트리스트: `/swagger-ui/**`, `/v3/api-docs/**`, `/auth/**`
- 그 외 모든 엔드포인트는 토큰 필수
- 상세 (JWT 구조 / 시크릿 관리 / 약점) → [`security.md`](./security.md)

---

## 구현된 API

> Source of truth: Swagger UI. 아래는 빠른 참조용 요약 + 의도 설명.

### `POST /post`
게시글 생성.

**Request**
```http
POST /post
Content-Type: application/json

{
  "title": "오늘의 기분",
  "content": "오늘은 기분이 좋았다."
}
```

- `title` (string, **required**, `@NotBlank`)
- `content` (string, **required**, `@NotBlank`)

**Response — 201 Created**
```
"9c4d401e-63ba-413e-abbe-a6d5cf869f0e"
```
> ⚠️ 응답 바디는 **UUID 문자열 한 줄** (객체 아님). FE는 `response.text()`로 받아야 함.

**에러**
- `400` — `{"message": "제목은 필수입니다."}` / `{"message": "내용은 필수입니다."}` / `{"message": "요청 형식이 올바르지 않습니다."}`

---

### `GET /post`
게시글 전체 조회.

**Response — 200 OK**
```json
[
  {
    "id": "9c4d401e-63ba-413e-abbe-a6d5cf869f0e",
    "title": "hello",
    "content": "first post"
  }
]
```

빈 결과면 `[]`.

> 📌 페이지네이션 없음 (TODO). PR 3(소유권) 시점에 본인 글만 반환하도록 변경.

---

### `GET /post/{id}`
단건 조회.

**Path**
- `id` — UUID

**Response — 200 OK**
```json
{
  "id": "9c4d401e-...",
  "title": "hello",
  "content": "first post"
}
```

**에러**
- `404` — `{"message": "게시글을 찾을 수 없습니다. id=<uuid>"}`

---

### `PUT /post/{id}`
수정 (전체 대체).

**Request**
```http
PUT /post/9c4d401e-...
Content-Type: application/json

{
  "title": "수정된 제목",
  "content": "수정된 본문"
}
```
- body는 `POST /post`와 동일한 `@NotBlank` 검증

**Response — 200 OK**
수정된 게시글 본문 (POST 응답과 다른 점: UUID가 아닌 객체 반환)
```json
{
  "id": "9c4d401e-...",
  "title": "수정된 제목",
  "content": "수정된 본문"
}
```

**에러**
- `400` — validation 실패
- `404` — 존재하지 않는 ID

---

### `DELETE /post/{id}`
삭제.

**Response — 204 No Content**
바디 없음.

**에러**
- `404` — 존재하지 않는 ID

---

## 예정 API

> 아직 구현 안 됨. 작업 들어가기 전 이 섹션에서 합의 → 구현 → Swagger 자동 생성으로 일치 검증.

### Auth (PR 2)

#### `POST /auth/signup`
**Request**
```json
{
  "email": "user@example.com",
  "password": "<원문>",
  "nickname": "닉네임"
}
```

**Response — 201 Created**
```json
{
  "userId": "<uuid>",
  "email": "user@example.com",
  "nickname": "닉네임"
}
```

**에러**
- `400` — 이메일 형식 오류, 비번 정책 위반
- `409` — 이메일 중복 (`{"message": "이미 가입된 이메일입니다."}`)

#### `POST /auth/login`
**Request**
```json
{ "email": "user@example.com", "password": "<원문>" }
```

**Response — 200 OK**
```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**에러**
- `401` — `{"message": "이메일 또는 비밀번호가 올바르지 않습니다."}` (이메일 미존재와 비번 틀림을 구분하지 않음 — 사용자 enumeration 방지)

> 🔐 **합의 사항**:
> - 토큰 저장: localStorage vs httpOnly cookie → FE와 협의 필요
> - 만료시간: 1시간(예시), refresh token 도입 여부 미정
> - 비번 정책: 최소 8자? 특수문자 강제? → 합의 필요

---

### AI Response 폴링 (PR 4)

#### `GET /post/{id}/ai-response`
사용자가 일기를 작성하면 즉시 `POST /post`가 201을 반환하고, **AI 응답은 비동기로 처리**됩니다. 프론트는 이 엔드포인트를 폴링해서 완료 여부를 확인.

**Response — 200 OK** (상태에 따라)

`status: PENDING`
```json
{
  "postId": "<uuid>",
  "status": "PENDING",
  "content": null,
  "emoji": null
}
```

`status: DONE`
```json
{
  "postId": "<uuid>",
  "status": "DONE",
  "content": "오늘 기분이 좋으셨군요! 그 순간을 더 자세히 떠올려보세요.",
  "emoji": "😊"
}
```

`status: FAILED`
```json
{
  "postId": "<uuid>",
  "status": "FAILED",
  "content": null,
  "emoji": null,
  "errorMessage": "AI 서버 응답 시간 초과"
}
```

**에러**
- `404` — postId가 존재하지 않음 또는 해당 post에 AI 응답이 트리거되지 않음
- `403` — 본인 글이 아님 (PR 3 머지 후)

> 📌 **FE 폴링 정책 합의 필요**: 1초 간격? 지수 백오프? → 합의 후 결정

---

### Calendar (PR 5)

#### `GET /calendar?year=YYYY&month=MM`
월별 캘린더 데이터 — **그 달 전체 일자**를 배열로 반환. 글 없는 날도 `emoji: null`로 포함.

> 🚧 **PR 5 구현 시점(현재)**: `emoji` 는 **항상 `null`**. `Post + AiResponse JOIN` 은 PR 4 머지 후 후속 PR 에서 추가. FE 는 `emoji == null` 이면 placeholder(회색 점 등) 로 처리.

**Query**
- `year` (int, required, 범위 2020 ~ 현재+1)
- `month` (int, required, 1-12)

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
- `401` — 인증 필요

---

## 외부 시스템 계약

> 우리가 **호출하는** 서버의 API. Swagger에는 안 나옴. 여기서 합의 → 변경 시 양쪽 동기화.

### AI 추론 서버 (PR 4)

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
