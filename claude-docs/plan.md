# Moodiary Backend — Roadmap

> 백엔드 작업 진행 상황과 계획을 한 곳에 모은 문서.
> **규칙**: PR이 머지될 때마다 갱신. 완료는 `[x]`로 체크, 진행 중이면 간단히 메모.
> 마지막 갱신: 2026-05-26 (PR 8 BE 부분 완료 + 운영 부팅 폭발 대사건 복구 + CD 신뢰성 강화)
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

PR 9 문서 사이트 (MkDocs)    (GitHub Pages 인프라 위에 풀 docs 통합, 미래)

PR 7 ECS 이전                (먼 미래, HTTPS / 도메인 도입 시 자연스러움)

# ── 인증 강화 라인 (PR 2 인증 완료 위에 쌓음) ──
PR 2 (인증 — 완료) ─► PR 10 Refresh Token ─┬─► PR 11 OAuth2 Resource Server (선택, 먼 미래)
                                           └─► PR 12 소셜 로그인 (Google + Kakao, 데모 가시성 ⭐⭐⭐)
```

> **다음 핵심 경로**: **PR 8 → PR 4 → (PR 5 후속 emoji JOIN)**.
> - **PR 8** 이 가시성 우선 — 졸업 발표에 "프론트가 떴고 백엔드와 통신" 까지 필수.
> - **PR 4** 는 차별 기능 (AI 응답 + 이모지). AI 담당자 답변 대기 중이면 PR 8 부터 진행.
> - **PR 6** 은 스키마 안정화 후 (PR 4 머지 시점) 같이.
> - **인증 강화 라인 (PR 10/11/12)** 은 데모 핵심 경로 (PR 8/4) 안정화 후 착수. **PR 10 (Refresh Token)** 은 보안/UX trade-off 해소가 동기, **PR 12 (소셜 로그인)** 은 졸업 데모 가시성, **PR 11 (OAuth2 Resource Server)** 는 표준화/리팩토링 (선택).

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

### PR 9 — 프로젝트 문서 사이트 (MkDocs Material) 📖 ⭐ (미래)
**Why**: 이미 GitHub Pages 인프라 (landing + 슬라이드 자동 배포) 가 들어가 있다. 그 위에 `claude-docs/*` + `README.md` 의 markdown 들을 **풀 문서 사이트** 로 통합하면 졸업 심사 / 교수 / 외부 공유 시 "한 페이지에서 모든 것" 보여줄 수 있음.

**구현 후보 — MkDocs Material**:
- markdown 그대로 + 검색 기능 + 깔끔한 테마 + Mermaid 다이어그램 native 지원
- 같은 GitHub Pages 인프라 위에 통합 (`deploy-pages.yaml` 확장)

**사이트 구조 안**:
- `/` — 현재 landing page 유지 또는 MkDocs 의 index 로 통합
- `/slides/` — Marp 슬라이드 (현재 그대로)
- `/docs/` — `claude-docs/*` 와 `README.md` 통합 (네비게이션 + 사이드바)
  - 로드맵 (plan.md)
  - API 명세 (api-contracts.md)
  - 보안 (security.md)
  - 트러블슈팅 (troubleshooting.md)
  - 운영 runbook (ops-runbooks/)

**의존**: 없음 — Pages 활성화 + Marp 배포가 이미 들어가 있는 상태 (이번 인프라 PR) 면 그 위에 단순 확장
**예상 소요**: 1-2일 (테마/네비 조정 + 자동 build pipeline)
**위험**: 작업 부담은 콘텐츠 양에 따라. markdown 그대로 가는 거라 코드 변경 X.

---

### PR 8 — 프론트엔드 S3 정적 배포 + CORS 통합 🌐 ⭐⭐⭐ (✅ BE 부분 완료, FE 답변 대기 중)
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
- [x] `CorsConfig` — `CorsConfigurationSource` Bean. allowed origins / methods / headers / credentials 명시
- [x] `application.yaml` 에 `app.cors.allowed-origins` 외부화 — `APP_CORS_ALLOWED_ORIGINS` env var override
- [x] `SecurityFilterChain` 에 `.cors(Customizer.withDefaults())` 활성화 — preflight 가 인증 검사 전 통과
- [x] CORS preflight 단위 테스트 4 cases (허용/미허용/localhost/Authorization 헤더 포함)
- [x] **`compose.yaml` 에 `APP_CORS_ALLOWED_ORIGINS` env 주입 + docker compose `:-` default 패턴** — EC2 `.env` 가 비어 있어도 dev origins 으로 안전 동작 (별도 PR)
- [ ] (선택) `springdoc.servers` 에 운영 URL 명시 — 후속 PR 로

---

**인프라 셋업** (BE 담당자가 AWS 콘솔 + CLI 로 1회):
- [x] S3 버킷 생성 — 실제: `moodiary-frontend-459338751419-ap-northeast-1-an`, region `ap-northeast-1` (도쿄)
  - 예상 region (`ap-northeast-2` 서울) 과 다름 — BE/RDS 는 서울, FE S3 는 도쿄. latency 영향은 사용자 → S3 만 도쿄, S3 → BE 호출은 발생 X (브라우저가 직접 BE 콜) 라 미미
- [ ] Static Website Hosting 활성화 → endpoint URL 받기 (FE 담당자가 1번 단계로 진행, 가이드에 명시)
- [ ] 버킷 정책 — Public Read (정적 사이트라 의도된 공개. `s3:GetObject` 만 `*` 에 허용) — 가이드의 1-b
- [ ] 프론트 레포에 GitHub OIDC role 부착 — S3 sync 권한만 (기존 백엔드 OIDC 패턴 재사용) — 가이드의 2-a/2-b/2-c

---

**프론트 레포 작업** — FE 답변 받은 정보 (스택: React + Vite, build: `npm run build`, output: `dist/`)
- [x] **셋업 가이드 작성 완료** — [`claude-docs/ops-runbooks/frontend-s3-cd-setup.md`](./ops-runbooks/frontend-s3-cd-setup.md) (PR #49). FE 담당자에게 전달해 그쪽이 따라 셋업
  - S3 버킷 정책 / IAM OIDC role / GitHub Secrets / `frontend-cd.yaml` template / 검증 / 트러블슈팅 8개 섹션
  - `VITE_API_BASE_URL` 권장 변수명 + 캐시 정책 분리 (hash 박힌 자산 long-cache, index.html short-cache)
- [ ] **FE 가 가이드 따라 셋업 진행** (외부 대기)
- [ ] **FE 가 S3 endpoint URL 회신** → BE 가 EC2 `.env` 의 `APP_CORS_ALLOWED_ORIGINS` 갱신 (compose 의 default 는 localhost 만 허용 중)

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
- [ ] **FE 가 S3 endpoint URL 회신하면** EC2 `.env` 에 `APP_CORS_ALLOWED_ORIGINS=http://<S3-endpoint>,http://localhost:5173` 추가
- [ ] S3 버킷 endpoint URL 변경 시 GitHub Secret + EC2 `.env` 동시 갱신

---

### PR 10 — Refresh Token 도입 🔐 ⭐⭐ (가까운 미래)
**Why**: 현재 access token 만료 (`JwtProperties.expirationMs` — 운영 기본 24h) 가 보안/UX trade-off 의 가운데 어중간한 값. 보안 강화하려고 짧게 (15분) 잡으면 사용자가 자주 튕기고, UX 살리려고 길게 (30일) 잡으면 토큰 탈취 시 노출 창이 커진다. Refresh token 패턴으로 **access 짧게 (15분 등) + refresh 길게 (2주 등)** 로 분리해서 둘 다 만족.

📋 **API 상세는 PR 10 시작 시 [`api-contracts.md`](./api-contracts.md) 에 작성** — 이번 PR 은 백로그 추가까지가 범위.

**고려해야 할 분기점들** (PR 시작 전 합의 필수):
- [ ] **저장 위치 결정** — DB (revoke 가능, 서버 부담 약간) vs httpOnly cookie (XSS 안전, CSRF 대비 필요) vs 둘 다. 현재 FE 가 localStorage 사용 중인 점 고려
- [ ] **회전 (rotation) 적용 여부** — 적용 시 refresh 사용할 때마다 새 refresh 발급 + 기존 무효화. race condition (동시 요청) 주의
- [ ] **새 엔드포인트 `POST /auth/refresh`** — `{refreshToken}` → `{accessToken, refreshToken?}` 응답. rotation 적용 시 새 refresh 같이 반환
- [ ] **`LoginResponseDto` 변경** — `refreshToken` 필드 추가. FE 측 토큰 저장 패턴 변경 합의 필요 (cookie 채택 시 응답 body 에서는 빠짐)
- [ ] **DB 스키마** — `refresh_token` 테이블 (userId, tokenHash, expiresAt, revokedAt, BaseEntity). DB 정책상 `ddl-auto: update` 가 인덱스 보장 X → `(user_id, token_hash)` UNIQUE 인덱스 수동 DDL 적용 필요
- [ ] **로그아웃 엔드포인트 `POST /auth/logout`** — 의미가 refresh 무효화로 바뀜 (access 는 stateless 라 서버에서 막을 수 없음, 만료 대기). FE 와 합의 (logout 시 localStorage 클리어 + BE 호출)
- [ ] **테스트** — refresh 정상 회전, expired refresh 거절, revoked refresh 거절, rotation race 시나리오

**예상 소요**: 3-5일 | **의존**: 없음 (PR 2 인증 위에 쌓음) | **위험**: cookie 저장 채택 시 CORS `allowCredentials=true` + origin 화이트리스트 재검토 필요 (현재 PR 8 의 `CorsConfig` 갱신), rotation race condition (동시 refresh 요청 시 둘 다 새 토큰 받고 한 쪽이 즉시 무효화되어 사용자 튕김 — DB 트랜잭션 + 짧은 grace window 설계 필요)

---

### PR 11 — OAuth2 Resource Server 마이그레이션 🔐 ⭐ (선택, 먼 미래)
**Why**: 현재 `JwtAuthenticationFilter` 가 수동 토큰 파싱 + `JwtTokenProvider` 가 직접 jjwt 호출. Spring Security 표준 `OAuth2 Resource Server` 로 교체하면 JWT 서명 검증/만료/클레임 추출이 표준 라이브러리로 흡수되고, JWK 도입 가능 (외부 IdP 연동 시 자연스러움), 코드량 감소, 보안 표준 준수.

📋 **API 상세는 PR 11 시작 시 [`api-contracts.md`](./api-contracts.md) + [`security.md`](./security.md) 에 작성**.

**고려 분기점**:
- [ ] **자체 발급 JWT 그대로 Resource Server 로 검증** (`NimbusJwtDecoder.withSecretKey`) vs **외부 IdP 위임** (Auth0 / Keycloak / Cognito) — 졸업 데모 범위에서는 자체 발급 유지가 현실적
- [ ] **마이그레이션 중 토큰 호환성** — 기존 발급된 access/refresh 가 그대로 검증되는지 (소프트 전환). 키/알고리즘 변경 없으면 호환됨
- [ ] **`@AuthenticationPrincipal UUID userId` 추출 방식 변경** — 현재 `Authentication.getPrincipal()` 직접 캐스팅, Resource Server 채택 시 `Jwt` 객체에서 subject 클레임 매핑하는 `JwtAuthenticationConverter` 필요
- [ ] **기존 `JwtAuthenticationFilter` / `JwtTokenProvider` 폐기 vs 발급 전용으로 축소** — 검증은 Resource Server, 발급은 여전히 우리 코드 (BCrypt 검증 후 토큰 발급)
- [ ] **테스트 갱신** — `@WithMockUser` → `@WithJwt` 패턴 변경 또는 직접 `Jwt` 빌드

**예상 소요**: 1주 | **의존**: PR 10 권장 (refresh 흐름 정착 후 큰 리팩토링이 안전) | **위험**: 운영 중 토큰 호환성 깨짐 위험 (마이그레이션 중 사용자 강제 재로그인 가능), FE 측 영향은 없으나 BE 컨트롤러 시그니처 영향 가능, 졸업프로젝트 범위 초과 가능 — **PR 7 (ECS) 같이 "시간 남으면" 카테고리**

---

### PR 12 — 소셜 로그인 (Google + Kakao) 🔐 ⭐⭐⭐ (졸업 데모 가시성)
**Why**: 졸업 발표에 "구글로 로그인 한 줄" = 데모 임팩트 큼. 신규 가입 마찰 감소 (이메일/비번 입력 부담 X). 한국 사용자 기준 Kakao 도 자연스러움. 이미 인증 라인 (PR 2) + Refresh Token (PR 10) 위에 얹는 거라 구조 변경은 적음.

📋 **API 상세는 PR 12 시작 시 [`api-contracts.md`](./api-contracts.md) 에 작성**.

**고려 분기점**:
- [ ] **인증 방식 선택** — Spring Security `oauth2Login` (서버 redirect 방식, 전통) vs **FE 가 provider access token 받아서 BE 로 넘기는 방식** (SPA/모바일 친화)
  - **권장: 후자.** FE 는 Google/Kakao SDK 로 access token 받음 → `POST /auth/oauth2/{provider}` 로 BE 에 넘김 → BE 가 provider `userinfo` 엔드포인트로 검증 + 우리 JWT 발급. 모바일 확장 시 자유도 ↑
- [ ] **새 엔드포인트** — `POST /auth/oauth2/google`, `POST /auth/oauth2/kakao` (request: `{providerAccessToken}`, response: 우리 JWT — PR 10 적용 시 access + refresh)
- [ ] **`User` 엔티티 확장** — `provider` enum (LOCAL/GOOGLE/KAKOO), `providerId` 문자열, password nullable 화 (소셜 가입자는 비번 없음). 스키마 변경 → `ddl-auto: update` 로 컬럼 추가는 자동, 단 기존 row 의 `provider` 는 수동 backfill 필요 (`UPDATE users SET provider='LOCAL' WHERE provider IS NULL`)
- [ ] **이메일 중복 정책 결정** — 같은 이메일이 LOCAL + GOOGLE 양쪽으로 가입 가능? 통합? 분리?
  - 권장: 신규 소셜 가입 시 같은 이메일의 LOCAL 계정이 있으면 차단 + "이미 가입된 이메일입니다" 안내 (안전한 기본)
- [ ] **Google** — OAuth client id/secret 발급 (Google Cloud Console). FE 가 토큰 받는 방식이면 BE 는 `https://oauth2.googleapis.com/tokeninfo?access_token=...` 호출로 검증만 (secret 불필요할 수도)
- [ ] **Kakao** — REST API key 발급 (Kakao Developers). `https://kapi.kakao.com/v2/user/me` 로 userinfo 조회
- [ ] **환경변수 / Secret 정책** — `GOOGLE_CLIENT_ID`, `KAKAO_REST_API_KEY` 등. tokeninfo 검증만 하면 secret 없이 client id 만 노출돼도 안전 → EC2 `.env` 만으로 충분. server-side OAuth flow 채택 시 secret 도 → GitHub Secrets + EC2 `.env` 양쪽 (compose.yaml 의 새 env 추가는 운영 머지 전 필수)
- [ ] **테스트** — provider userinfo 응답 mock (WireMock), 신규 가입 시 User 생성, 기존 LOCAL 이메일 충돌 거절, JWT 발급 흐름

**예상 소요**: 1-2주 | **의존**: PR 10 권장 (소셜 가입자도 토큰 만료 → refresh 가 자연스러움) | **위험**:
- Google/Kakao Console 셋업 (1회성 IDE 외 작업, 권한 승인 등 외부 절차)
- 이메일 중복 처리 정책이 사용자 경험에 직접 영향 → 결정 신중
- FE 측 redirect 흐름 vs token-pass 방식 합의 (위 "권장: 후자" 채택 시 FE 가 provider SDK 직접 사용 필요 — FE 담당자와 사전 합의)
- compose.yaml / EC2 `.env` / GitHub Secrets 3중 동기화 — **운영 머지 전 필수** 체크리스트 필요

**운영 머지 전 필수**:
- [ ] compose.yaml 에 `GOOGLE_CLIENT_ID`, `KAKAO_REST_API_KEY` env 주입 추가 (PR 8 `:-` default 패턴 따름)
- [ ] EC2 `.env` 에 동일 키 추가
- [ ] (server-side flow 채택 시) GitHub Secrets 에 `GOOGLE_CLIENT_SECRET` 등록 + CD 워크플로우에 주입

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
| 2026-05-26 | **PR 8 BE 부분 완료** — `CorsConfig` + `application.yaml` 외부화 + `compose.yaml` env 주입 (`:-default` 패턴으로 함정 회피). FE 셋업 가이드 [`ops-runbooks/frontend-s3-cd-setup.md`](./ops-runbooks/frontend-s3-cd-setup.md) 작성 (PR #49). FE 답변 받음 — React + Vite, `npm run build`, `dist/`. S3 endpoint URL 회신 대기 중. |
| 2026-05-26 | **운영 부팅 폭발 대사건** ([T-019](./troubleshooting.md#t-019)) — PR #38 CORS 검증 중 발견. 5층 결함 동시 노출: SSM agent 죽음 + CD silent fail + springdoc 2.8.3 ↔ Spring Boot 4 비호환 + ApplicationContext 안전망 부재 + `docker compose` (스페이스) ≠ `docker-compose` (하이픈). 복구: springdoc → 3.0.3, `MoodiaryApplicationTests` `@Disabled` 제거, RDS orphan post 클린업, CD 워크플로우에 SSM `wait command-executed` + health check 추가, image 태그 `:${{ github.sha }}` 함께 push. 옛 CD 들이 사실은 한 번도 자동 deploy 에 성공한 적 없었던 진실까지 드러남. |
| 2026-05-26 | **Workflow 규칙 일반화** ([T-020](./troubleshooting.md#t-020)) — 머지된 PR 본문을 사후 수정한 사고. CLAUDE.md 의 트리거를 "PR 생성 / 추가 push 전" 만이 아니라 "`gh pr` 으로 시작하는 거의 모든 명령 전" 으로 일반화. T-015 → T-018 → T-020 세 번째 재발. |
| 2026-05-26 | **GitHub Pages 인프라 신설** — landing page (`site/index.html`) + Marp 슬라이드 자동 배포 (`/slides/`). `main` push 시 GitHub Actions 가 자동 build & deploy. 졸업 심사 / 교수 공유용 단일 URL 확보. 백로그 PR 9 (MkDocs Material 풀 문서 사이트) 신설 — 같은 인프라 위에 `claude-docs/*` 통합 예정. |
| 2026-05-26 | **인증 강화 백로그 추가** (PR 10 Refresh Token, PR 11 OAuth2 Resource Server, PR 12 소셜 로그인). 현재 access-only 24h JWT 의 보안/UX trade-off 해소 (PR 10), 표준 Resource Server 로 리팩토링 (PR 11, 선택), 졸업 데모 가시성용 Google/Kakao 로그인 (PR 12). 의존성 라인: PR 2 (완료) → PR 10 → {PR 11 (선택), PR 12}. API 상세는 각 PR 시작 시 `api-contracts.md` 에 작성 — 이번 PR 은 plan.md 만 갱신. |

