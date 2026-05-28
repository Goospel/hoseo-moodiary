# Moodiary Backend — Roadmap

> 백엔드 작업의 **현재 위치 + 다음 경로**. PR 머지 시 갱신.
> 마지막 갱신: 2026-05-29 (release PR #79 + hot-fix release PR #81 — PR 12 Google OAuth2 풀체인 운영 반영 완료. T-031/T-032 함정 박힘.)
>
> 📚 **상세는 다른 문서로 위임**:
> - [`api-contracts.md`](./api-contracts.md) — API 명세 (request/response/예시/외부 AI 계약)
> - [`security.md`](./security.md) — JWT / 비밀번호 / 시크릿 / 인가 / 약점
> - [`troubleshooting.md`](./troubleshooting.md) — 막힘 + 원인 + 해결 로그 (지속 누적)
> - [`learning-notes.md`](./learning-notes.md) — 모르고 물어봐서 배운 기술 개념 정리 (발표 / Q&A 대비)
> - [`ops-runbooks/ec2-cheatsheet.md`](./ops-runbooks/ec2-cheatsheet.md) — EC2 운영 치트시트
> - [Swagger UI](http://15.165.95.129:8080/swagger-ui/index.html) — 구현된 API 의 실시간 진실의 원천

---

## 📊 현재 상태

| 항목 | 값 |
|---|---|
| **운영 URL** | http://15.165.95.129:8080 (Elastic IP, 고정) |
| **운영 반영** | Post CRUD + 인증 (회원가입 / JWT 로그인 / Refresh Token rotation / **Google OAuth2 소셜 로그인**) + 소유권 + PR 4-pre 비동기 AI 골격 (Stub) + **MkDocs 문서 사이트 (`/docs/`)** |
| **dev 에만** | Calendar API (emoji=null 임시) |
| **외부 대기** | PR 4-final (AI 합의), PR 8 후반 (FE S3 endpoint 회신) |
| **다음 핵심 경로** | PR 8 (가시성, FE 회신 대기) ‖ PR 4-final (AI 합의 대기) → PR 5 후속 (emoji JOIN) → PR 6 (Flyway baseline) |
| **스택** | Java 25 / Spring Boot 4.0.6 / EC2 + RDS MySQL 9 |

---

## ✅ 완료 (운영 반영)

| 도메인 | PR | 핵심 결과 |
|---|---|---|
| **Post CRUD** | #14, #24 | 작성자 자동 채움, 본인 글만 조회/수정/삭제, 403/404 분기 |
| **인증** | #20, #21, #23, #24 | 회원가입 (BCrypt, UNIQUE) + HS512 JWT + 열거 공격 방지 401 |
| **JWT 필터** | #23 | Bearer → `SecurityContext` UUID principal, `@AuthenticationPrincipal UUID` 패턴 |
| **Refresh Token (PR 10)** | #63, #66 | access 1h / refresh 2w + rotation + SHA-256 hash 저장 + `POST /auth/refresh` + `POST /auth/logout` 의미 변경. release PR #66 으로 운영 반영. |
| **전역 예외** | #14, #20, #23, #24 | 400/401/403/404/409/500 → `{"message":"..."}` 통일 |
| **인프라** | 다수 | EC2 + RDS + Elastic IP + AWS SSM CD + `application.yaml` env 플레이스홀더 + `JWT_SECRET` 주입 |
| **CORS (PR 8 BE)** | #38 | `CorsConfig` + `APP_CORS_ALLOWED_ORIGINS` 외부화 + compose `:-` default |
| **GitHub Pages** | #53, #68, **#72~#74** | landing + Marp 슬라이드 + **MkDocs Material 문서 사이트 (`/docs/`)** 자동 배포. 트리거 main → **dev** (PR #68) — release 전 반영. PR 9 (#72) 하이브리드 옵션 C 로 learning-notes 가 MkDocs 안으로 흡수됨. T-029/T-030 두 fix iteration (README ↔ index 자동 충돌, outbound 링크 평면화 충돌). |
| **PR 9 MkDocs (단독)** | #72, #73, #74 | `/docs/` 에 Material 테마 문서 사이트. claude-docs/\* + README 한 곳 검색. 사이트 홈 + 로드맵 + API 명세 + 보안 + 트러블슈팅 + 학습 노트 + 운영 Runbook + README 모두 nav. **strict 빌드 컨벤션 함정 2건 박힘** ([T-029](./troubleshooting.md#t-029) / [T-030](./troubleshooting.md#t-030)). |
| **AI 응답 골격** | #59 | `AiResponse` 엔티티 + `AiResponseClient` Stub + `@Async` + `GET /post/{id}/ai-response` |
| **학습 파이프라인 (PKM)** | #67, #70 | `learning-notes.md` 신설 (12 항목) + 3개 항목 ([Spring 비동기 / AWS SSM 메커니즘 / CORS](https://goospel.github.io/notes/)) goospel.github.io 공개판 첫 승격 |
| **PR 12 Google OAuth2 (풀체인)** | #76, #77, #78, #79, #80, #81 | **운영 반영 완료**. token-exchange 패턴 — FE 가 Google Sign-In 으로 id_token 받아 BE 에 POST → BE 가 Google `tokeninfo` 호출 + `aud` (confused deputy 방어) + `email_verified` 검증 → DB 의 (provider, providerId) 조회 → 신규/기존 분기 + JWT 발급. `oauth2.client.mode=stub\|http` 토글로 dev/운영 분리. **Kakao 제외** (졸업프로젝트 범위). `users` 테이블 ALTER 4건 (provider/providerId/password nullable/UNIQUE) 사전 적용. 운영 deploy 직후 [T-031](./troubleshooting.md#t-031) (path enum case-sensitivity) + [T-032](./troubleshooting.md#t-032) (silent 500 진단 가림막) 함정 발견 → hot-fix release PR #81 로 박음. |
| **테스트** | 누적 | 120+ pass — Controller 슬라이스 + Service 단위 + JWT 라운드트립 + CORS preflight + Refresh rotation + OAuth2 Stub + WireMock Google tokeninfo + OAuth2 path case 5종 |

---

## 🔄 진행 중 / 외부 대기

| PR | 상태 | 대기 사유 |
|---|---|---|
| **PR 4-final** — AI HTTP 어댑터 + Retry + WireMock | ⏸ 0% | AI 담당자 외부 합의 ([api-contracts.md#합의-항목-체크리스트](./api-contracts.md#합의-항목-체크리스트)) |
| **PR 5** — Calendar API (`GET /calendar?year=YYYY&month=MM`) | 🟡 85% | dev 머지 완료. emoji LEFT JOIN + `(user_id, created_at)` 인덱스는 PR 4-final 후 후속 PR |
| **PR 8 후반** — 프론트 S3 배포 + 통합 검증 | 🟡 50% | BE CORS ✅. FE 가 [`ops-runbooks/frontend-s3-cd-setup.md`](./ops-runbooks/frontend-s3-cd-setup.md) 따라 셋업 + S3 endpoint URL 회신 대기. **OAuth2 통합** 도 같이 검증 가능해짐 (PR 12 풀체인 운영 안착). |

---

## 📋 백로그 (우선순위 + 의존성)

```
PR 8 후반 (FE S3 배포)   ── 졸업 데모 가시성 ⭐⭐⭐, FE 회신 대기
PR 4-final (AI 실어댑터)  ── 차별 기능 ⭐⭐⭐, AI 합의 대기
    └─► PR 5 후속 (emoji JOIN)
PR 6 (Flyway)            ── PR 4-final 머지 시 스키마 베이스라인 같이
PR 11 (OAuth2 Resource Server) ── 선택, 먼 미래 (PR 10 ✅ 정착 후 큰 리팩토링)
PR 7 (ECS 이전)           ── 먼 미래, HTTPS/도메인 도입 시
```

> **PR 12 Google OAuth2 풀체인 #76 ~ #81** 운영 반영 완료 — backlog → 완료. 외부 Console 셋업 (Google Cloud Console) + RDS 사전 ALTER 4건 (users) + env 3-way sync 모두 끝. release PR #79 + hot-fix #81 두 사이클. PR 10/9 도 이미 완료 이동. PR 11 의 "PR 10 권장" 의존도 해소.

> 각 PR 의 **구현 체크리스트 / 위험 / 운영 머지 전 필수 항목**은 해당 PR 시작 시점에 PR body 에 작성한다.
> plan.md 는 "무엇 / 왜 / 의존" 까지만.

### PR 4-final — AI 비동기 응답 실어댑터 🤖 ⭐⭐⭐
**Why**: PR 4-pre 의 Stub 을 실제 HTTP 호출로 교체. 일기 → AI 응답 + 기분 이모지 생성.
**Why now blocked**: AI 담당자와 응답 포맷 / endpoint / API key 합의 필요.
**범위**: `AiResponseClient` interface 추출 + `HttpAiResponseClient` (`RestClient`) + Spring Retry (5xx 만 N회) + WireMock 통합 테스트 + `ai.client.mode` 토글 + EC2 `.env` 에 `AI_*` env 추가.
**의존**: PR 4-pre ✅, AI 합의.

### PR 5 후속 — Calendar emoji JOIN 📅 ⭐⭐
**Why**: PR 5 가 emoji=null 로 우회됐던 거 채움. `AiResponse` LEFT JOIN 으로 일자별 이모지 매핑.
**의존**: PR 4-final 머지. **부가**: `post(user_id, created_at)` 복합 인덱스 수동 DDL.

### PR 6 — Flyway 도입 ⭐
**Why**: `ddl-auto: update` 가 컬럼 삭제 / 타입 변경 / 제약 추가 불가. 스키마 안정화 시점에 잠금.
**범위**: `flyway-mysql` + `V1__baseline.sql` (users + post + ai_response) + `ddl-auto: validate` 전환 + 운영 RDS 에 `flyway baseline` 적용.
**의존**: PR 4-final 머지 (AiResponse 스키마 안착 후 베이스라인 같이).

### PR 8 후반 — 프론트 S3 통합 검증 🌐 ⭐⭐⭐
**Why**: 졸업 발표에 "프론트가 떠서 BE 호출" 까지가 의미 있는 데모.
**남은 일**: FE 가 [셋업 가이드](./ops-runbooks/frontend-s3-cd-setup.md) 따라 S3 + GitHub Actions 셋업 → S3 endpoint URL 회신 → BE 가 EC2 `.env` 의 `APP_CORS_ALLOWED_ORIGINS` 갱신 → 회원가입 / 로그인 / 일기 CRUD / 캘린더 통합 검증.
**의존**: FE 회신 (외부).

### PR 11 — OAuth2 Resource Server 마이그레이션 🔐 ⭐ (선택)
**Why**: 수동 `JwtAuthenticationFilter` → Spring Security 표준 Resource Server. 검증 / 클레임 추출 / JWK 도입 자연스러움.
**고려 분기점**: 자체 발급 유지 vs 외부 IdP, 토큰 호환성 (소프트 전환), `JwtAuthenticationConverter` 로 UUID principal 추출, 발급 코드만 보존.
**의존**: PR 10 ✅ — 큰 리팩토링 안전한 시점. **위험**: 졸업프로젝트 범위 초과 가능 — "시간 남으면" 카테고리.

### PR 7 — ECS 이전 ⭐ (먼 미래)
**Why**: 운영 안정성 + 확장성 + HTTPS / 도메인.
**범위**: ECR + Fargate + ALB + ACM + CloudWatch.
**위험**: 졸업프로젝트 범위 초과 가능.

---

## 🤝 FE 협업

API 명세 + 호출 패턴 + 변경 정책 → **[`api-contracts.md`](./api-contracts.md)** 단일 소스.
프론트 배포 가이드 → **[`ops-runbooks/frontend-s3-cd-setup.md`](./ops-runbooks/frontend-s3-cd-setup.md)**.

공유 자산:
- **OpenAPI spec**: http://15.165.95.129:8080/v3/api-docs (TS 타입 / 클라이언트 / mock 자동 생성)
- **Swagger UI**: http://15.165.95.129:8080/swagger-ui/index.html
- **변경 정책**: breaking change 시 사전 공유. 응답 포맷 `{"message":"..."}` 깨면 FE 다 깨짐 — 절대 변경 X.

---

## 💡 의사결정 로그

> 왜 이렇게 했는지. 나중에 "왜?" 싶을 때 참고.

| 결정 | 이유 |
|---|---|
| Spring Boot 4.0 + Java 25 | 최신 학습, 궁합 |
| UUID 기본키 | 분산 환경 가정, ID 추측 방지 |
| AWS SSM 배포 (SSH 폐기) | SSH 키 관리 부담 제거 |
| RDS 단일 소스 + Elastic IP | 데이터 보존 + IP 고정 |
| AI = 비동기 + DB 상태 (큐 X) | 졸업프로젝트 범위, 단순성 |
| JWT HS512 + Authorization 헤더 | Stateless, FE 단순성 (localStorage) |
| Refresh Token: DB 저장 + rotation + SHA-256 hash | DB 가 털려도 raw 복원 불가 + 훔친 refresh 단 1회 유효. httpOnly cookie 대신 DB 인 이유 — CORS 의 `credentials=true` 와 와일드카드 충돌 회피 + cross-domain SPA 호환성. |
| access 1h / refresh 2w | 자주 쓰이는 건 짧게 (탈취 노출 최소), 거의 안 쓰이는 건 길게 (UX 유지) |
| `@AuthenticationPrincipal UUID` 패턴 | 컨트롤러 시그니처 깔끔 |
| GET /post 본인 글만 | 일기 도메인, 캘린더 일관 |
| `application.yaml` 추적 + env var 플레이스홀더 | gitignore 사고 ([T-013](./troubleshooting.md#t-013)) 재발 차단 |
| `ddl-auto: update` (안정화 시 validate) | 스키마 미확정 단계 손DDL 부담 제거 |
| 열거 공격 방지 401 | 이메일 존재 노출 X |
| 이모지 = 유니코드 그대로 (`utf8mb4`) | 변환 로직 X, 가장 단순 |
| 캘린더 하루 다중 글 = 마지막 글 | 구현 단순 |
| 캘린더 응답 = 한 달 전체 (빈 날 `emoji: null`) | FE 부담 감소 |
| 프론트 = S3 only (CloudFront/도메인 X) | CloudFront 가면 BE 도 HTTPS 작업 연쇄 → 졸업 범위 초과 |
| 프론트 CD = BE 담당자 직접 | FE 담당자 CI/CD 학습 부담 흡수 |
| PR 4 → PR 4-pre / PR 4-final 분할 | AI 외부 합의 대기로 막힌 차단 해소 |
| PR 생성 = Claude / 머지 = 사용자 | 분담 명시 (PR #56) |
| Pages trigger main → dev (PR #68) | learning-notes 가 release 전에도 반영되도록. dev 가 main 의 superset 이라 안전. environment allowlist 도 함께 갱신 필요 ([T-028](./troubleshooting.md#t-028)). |
| 학습 (PKM) 3-layer 시스템 — project learning-notes → goospel.github.io 공개판 → learning-vault 사적 | 현장 메모 마찰 0 + 의식적 승격 + 사적 / 공개 분리. 묶음 기준 3-5개 / release 직후 / 4문 자격. PR #67/#70 으로 첫 사이클 가동. |
| PR 9 MkDocs 하이브리드 옵션 C (#72) | landing 보존 + learning-notes 를 MkDocs 안으로 흡수. 옵션 A (sub-path 만 추가, 디자인 4종 공존) 와 B (통째 교체, 기존 landing 재작업) 의 균형점. **둘 다 잃을 필요 없음** — landing 디자인 보존 + 검색 기능 + 문서 결속력. |
| 빌드 시점 sed 치환 — 원본 contract / 사이트 contract 분리 점 (#73 T-029 / #74 T-030) | claude-docs/\* 원본은 GitHub UI 친화 (`../README.md`, `./claude-docs/X.md`), MkDocs 빌드 사본은 평면화된 디렉토리. 둘 다 만족시키려면 빌드 시점에 사본에만 sed. README outbound 는 GitHub blob URL 로 외부화. mkdocs / docusaurus / hugo 어디서나 같은 패턴. |
| PR 12 분할 (12-pre + 12-final) | OAuth2 외부 Console 셋업 (사용자 책임, 1회) 이 BE 진행을 막는 차단점. Stub 으로 외부 의존성 차단 해소 → BE 만으로 OAuth2 흐름 정착 (12-pre) → 외부 셋업 후 실 어댑터 합류 (12-final). PR 4-pre / 4-final 과 같은 패턴 — **외부 의존성 분리는 차단 해소의 1번 도구**. |
| Kakao 제외 (PR 12-final) | Kakao Developers 의 "사이트 도메인 localhost 거부" 가 FE S3 배포 선행을 요구 → 졸업프로젝트 범위에서 외부 마찰 큼. 학습 가치 (OAuth2 token-exchange 패턴) 가 Google 과 거의 동일해서 한 provider 로 충분. **부활 비용 작음**: enum 값 + adapter 1개. 졸업 후 부활 시 같은 코드 패턴 복제. |
| OAuth2 토글 패턴 (`oauth2.client.mode=stub\|http`) | provider 추상화의 활성 구현을 부팅 시점에 결정. dev / 로컬 / 단위테스트 = `stub`, 운영 / 시연 = `http`. `@ConditionalOnProperty` + `matchIfMissing=true` 로 default 가 stub — 외부 키 없어도 부팅됨. 같은 패턴은 PR 4-final 의 `ai.client.mode` 로 재사용 예정. |
| Path enum case-insensitive 정규화 (#80 T-031 fix) | Spring 기본 `String→Enum` 변환은 case-sensitive. `@PathVariable AuthProvider provider` 가 소문자 `google` path 변환 실패 → `MethodArgumentTypeMismatchException` → generic 500. Swagger 의 "대소문자 무관" promise 와 어긋남. **fix**: controller 가 `String` 으로 받아 `.toUpperCase()` 명시 정규화 + 미지원 값은 `InvalidOAuth2ProviderException` (400). 같은 패턴은 다른 enum path 도입 시 재사용. |
| GlobalExceptionHandler 의 generic `Exception` 에 ERROR 로깅 (#80 T-032 fix) | `@ExceptionHandler` 가 catch 하면 Spring default exception logging 발동 안 함. 무로깅 silent 500 은 운영 진단 0. 응답 body 는 그대로 (사용자 노출 정보 변경 없음), `log.error("...", e)` 로 stdout 만 풍부. 향후 silent 500 후보 발견의 1차 단서. |
| Squash release 후 dev → main merge 충돌의 표준 해결 (#79, #81) | main 의 release squash commit 이 dev 의 개별 commit 과 같은 줄 건드려 자동 머지 불가. dev 가 strict semantic superset 임을 명시 검증 후 `git merge origin/main -X ours` 로 자동 해결. 이 sweep 후 push 하면 release PR 이 자동 mergeable. **사용자 OK 필수** — auto classifier 가 처음엔 차단했던 패턴. |

---

## 📝 Changelog

| 일자 | 변경 |
|---|---|
| 2026-05-29 | **release PR #81 — T-031/T-032 hot-fix 운영 반영** — release PR #79 의 deploy 직후 발견된 함정 2건 (path enum case-sensitivity / silent 500 진단 가림막) 의 fix #80 을 main 으로. dev → main merge 충돌은 `-X ours` 패턴으로 자동 해결 (의사결정 로그 참조). 운영 검증: `/auth/oauth2/google` (소문자) → 401 정상 / `/auth/oauth2/twitter` → 400 정상. |
| 2026-05-29 | **fix PR #80 — OAuth2 path case-insensitive + GlobalExceptionHandler 진단 로깅** — release #79 직후 발견한 두 함정 같이. controller 가 `String` 으로 받아 `.toUpperCase()` 정규화 + 신규 `InvalidOAuth2ProviderException` (400). `@Slf4j` + `log.error` 추가. 테스트 5 케이스 추가 (소/대/혼합 case + 미지원 + LOCAL service-level reject). troubleshooting [T-031](./troubleshooting.md#t-031) / [T-032](./troubleshooting.md#t-032). |
| 2026-05-29 | **release PR #79 — PR 9 MkDocs + PR 12 Google OAuth2 풀체인 + PKM 첫 사이클 (#67~#78) 운영 반영** — dev → main merge 가 main 의 release squash commit 들과 충돌 → `-X ours` 패턴으로 dev (semantic superset) 우선 자동 해결 후 push. 운영 머지 전 체크리스트 4건 모두 ✅ (compose env passthrough / GitHub Secrets / EC2 .env / RDS ALTER 4건). CD 성공 후 운영 검증에서 [T-031](./troubleshooting.md#t-031) 발견 → 즉시 hot-fix #80 + release #81 사이클로 fix. |
| 2026-05-28 | **PR 12-final — Google OAuth2 실어댑터 + Kakao 제외 (#77)** — `HttpGoogleOAuth2Provider` (`RestClient` + tokeninfo + aud / email_verified 검증) + WireMock 단위 테스트 8개 + `oauth2.client.mode` 토글 (`@ConditionalOnProperty` matchIfMissing=stub). `AuthProvider` enum 에서 KAKAO 제거. application.yaml 에 `${GOOGLE_OAUTH_CLIENT_ID:dummy}` + `${GOOGLE_TOKENINFO_URL:...}` + `${OAUTH2_CLIENT_MODE:stub}` placeholder 추가. learning-notes 13 추가 (OAuth2 Token-Exchange + audience + email_verified). |
| 2026-05-28 | **PR 12-compose (#78)** — `compose.yaml` 에 OAuth2 env 2개 (`OAUTH2_CLIENT_MODE` + `GOOGLE_OAUTH_CLIENT_ID`) 컨테이너 주입 + `.env.example` 보충. PR 12-final 의 운영 머지 전 사전 작업. |
| 2026-05-28 | **PR 12-pre 머지 (#76)** — OAuth2 Stub 흐름 + User 엔티티 provider/providerId 추가. 외부 Console 셋업 / HTTP 의존 없이 BE 만으로 OAuth2 흐름 정착. PR 4-pre 와 같은 외부 의존성 차단 해소 패턴. |
| 2026-05-28 | **PR 9 MkDocs Material 문서 사이트 완료 (#72 + #73 + #74)** — `/docs/` 에 claude-docs/\* + README + 학습 노트 통합 사이트. 하이브리드 옵션 C (landing 보존 + learning-notes 흡수). strict 빌드 두 fix iteration ([T-029](./troubleshooting.md#t-029) README↔index 자동 충돌 / [T-030](./troubleshooting.md#t-030) README outbound 평면화 충돌 + **patch incompleteness** 경고). 결과 10개 URL 모두 200 OK. |
| 2026-05-28 | **PKM 공개판 첫 승격 (PR #70)** — learning-notes 항목 10/11/12 (Spring 비동기 / AWS SSM 메커니즘 / CORS) 를 [goospel.github.io](https://goospel.github.io/notes/) 로 일반화 승격. 글로벌 CLAUDE.md PKM 파이프라인 (3-5개 묶음 + 4문 자격 + release 직후 타이밍) 첫 실 적용. 각 항목 헤더에 공개판 링크 마커. |
| 2026-05-28 | **Pages 워크플로우 확장 + dev trigger 전환 (PR #68)** — `claude-docs/learning-notes.md` 를 pandoc 으로 HTML 빌드 → `/learning-notes/` 노출. 트리거 main → dev (학습 노트가 release 전에도 반영). environment allowlist 누락 함정 [T-028](./troubleshooting.md#t-028) 발견. |
| 2026-05-27 | **release PR #66 — PR 10 Refresh Token 운영 반영** — access 1h / refresh 2w + rotation + SHA-256 hash + `POST /auth/refresh` / `POST /auth/logout`. README (#64) + 발표 자료 (#65) 도 같이 최신화. |
| 2026-05-27 | **learning-notes.md 신설 (PR #67)** — 모르고 물어봐서 배운 기술 개념 9개 정리 (이후 12개로 확장). 발표 / Q&A / 면접 대비 본인 이해 확립. |
| 2026-05-27 | **plan.md 압축 (PR #61)** — FE 협업 / 운영 치트시트 / 완료 PR 체크리스트 등 중복 / 만료 콘텐츠 정리. 551줄 → ~180줄. 운영 치트시트는 [`ops-runbooks/ec2-cheatsheet.md`](./ops-runbooks/ec2-cheatsheet.md) 로 분리. |
| 2026-05-27 | **PR 4-pre 머지 (#59) + release (#62)** — 비동기 AI 응답 골격 + Stub 어댑터. `AiResponse` 엔티티 + `@Async` + `GET /post/{id}/ai-response` 운영 반영. 단순화 결정: interface 제거, 이벤트 패턴 제거, executor 외부화 제거. |
| 2026-05-27 | **PR 4 를 두 단계 분할** — PR 4-pre (Stub 골격, 단독 진행) / PR 4-final (HTTP 어댑터, AI 합의 후). AI 담당자 외부 합의 대기 차단 해소. |
| 2026-05-26 | **인증 강화 백로그 추가** (PR 10 Refresh / PR 11 OAuth2 RS / PR 12 소셜 로그인). |
| 2026-05-26 | **GitHub Pages 인프라 신설** — landing + Marp 슬라이드 자동 배포. PR 9 (MkDocs) 백로그 추가. |
| 2026-05-26 | **운영 부팅 폭발 대사건** ([T-019](./troubleshooting.md#t-019)) — springdoc 2.8.3 ↔ Spring Boot 4 비호환 등 5층 결함. 복구 + CD 신뢰성 강화. |
| 2026-05-26 | **PR 8 BE 부분 완료** — `CorsConfig` + compose env `:-default` 패턴. FE 셋업 가이드 작성. |
| 2026-05-24 | **인증 라인 완성 + 운영 반영** (release PR #27) — PR 0/2/3 + 인프라 PR 5개 한 번에 main 머지. |
| 2026-05-24 | **이 문서 신설** — 백로그 의존성 그래프 + FE 협업 + 의사결정 로그 섹션. |
