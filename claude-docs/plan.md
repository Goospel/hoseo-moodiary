# Moodiary Backend — Roadmap

> 백엔드 작업의 **현재 위치 + 다음 경로**. PR 머지 시 갱신.
> 마지막 갱신: 2026-05-28 (PR #66 release — PR 10 Refresh Token 운영 반영 / PR #67~#70 — learning-notes + Pages dev trigger + T-028 + PKM 공개판 승격)
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
| **운영 반영** | Post CRUD + 인증 (회원가입 / JWT 로그인 / **Refresh Token rotation**) + 소유권 + **PR 4-pre 비동기 AI 골격 (Stub)** |
| **dev 에만** | Calendar API (emoji=null 임시) |
| **외부 대기** | PR 4-final (AI 합의), PR 8 후반 (FE S3 endpoint 회신) |
| **다음 핵심 경로** | PR 8 (가시성, 외부 대기) ‖ PR 4-final (외부 대기) → PR 5 후속 (emoji JOIN) → PR 6 (Flyway baseline) ‖ **단독 가능: PR 9 (MkDocs) / PR 12 (소셜 로그인)** |
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
| **GitHub Pages** | #53, #68 | landing + Marp 슬라이드 + **learning-notes (pandoc)** 자동 배포. 트리거 main → **dev** (PR #68) — 학습 노트가 release 전에도 반영. |
| **AI 응답 골격** | #59 | `AiResponse` 엔티티 + `AiResponseClient` Stub + `@Async` + `GET /post/{id}/ai-response` |
| **학습 파이프라인 (PKM)** | #67, #70 | `learning-notes.md` 신설 (12 항목) + 3개 항목 ([Spring 비동기 / AWS SSM 메커니즘 / CORS](https://goospel.github.io/notes/)) goospel.github.io 공개판 첫 승격 |
| **테스트** | 누적 | 70+ pass / 1 skip — Controller 슬라이스 + Service 단위 + JWT 라운드트립 + CORS preflight + Refresh rotation |

---

## 🔄 진행 중 / 외부 대기

| PR | 상태 | 대기 사유 |
|---|---|---|
| **PR 4-final** — AI HTTP 어댑터 + Retry + WireMock | ⏸ 0% | AI 담당자 외부 합의 ([api-contracts.md#합의-항목-체크리스트](./api-contracts.md#합의-항목-체크리스트)) |
| **PR 5** — Calendar API (`GET /calendar?year=YYYY&month=MM`) | 🟡 85% | dev 머지 완료. emoji LEFT JOIN + `(user_id, created_at)` 인덱스는 PR 4-final 후 후속 PR |
| **PR 8 후반** — 프론트 S3 배포 + 통합 검증 | 🟡 50% | BE CORS ✅. FE 가 [`ops-runbooks/frontend-s3-cd-setup.md`](./ops-runbooks/frontend-s3-cd-setup.md) 따라 셋업 + S3 endpoint URL 회신 대기 |

---

## 📋 백로그 (우선순위 + 의존성)

```
PR 8 후반 (FE S3 배포)   ── 졸업 데모 가시성 ⭐⭐⭐, FE 회신 대기
PR 4-final (AI 실어댑터)  ── 차별 기능 ⭐⭐⭐, AI 합의 대기
    └─► PR 5 후속 (emoji JOIN)
PR 6 (Flyway)            ── PR 4-final 머지 시 스키마 베이스라인 같이
PR 9 (MkDocs 문서 사이트) ── 단독 가능. 단 Pages 가 이미 landing+slides+learning-notes 운영 중이라 통합 방식 결정 필요
PR 12 (소셜 로그인 Google + Kakao) ⭐⭐⭐ ── 졸업 데모 가시성, PR 10 ✅ 위에 쌓음 (단독 가능)
PR 11 (OAuth2 Resource Server) ── 선택, 먼 미래 (PR 10 ✅ 정착 후 큰 리팩토링)
PR 7 (ECS 이전)           ── 먼 미래, HTTPS/도메인 도입 시
```

> PR 10 (Refresh Token) 은 #63/#66 으로 **완료 이동**. PR 11 / PR 12 의 "PR 10 권장" 의존은 해소.

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

### PR 9 — MkDocs Material 문서 사이트 📖 ⭐
**Why**: `claude-docs/*` + `README.md` 통합 → "한 페이지에서 모든 것" 졸업 심사 / 외부 공유.
**고려 분기점** (PR 시작 전 합의): 기존 Pages 와의 공존 구조 — 옵션 A. landing/slides/learning-notes 유지 + `/docs/` sub-path 에 MkDocs / 옵션 B. MkDocs Material 로 통째 교체 (learning-notes / slides 까지 MkDocs nav 안으로). [`goospel.github.io`](https://goospel.github.io/) 의 MkDocs 패턴이 직접 reference.
**범위**: 결정에 따라 `deploy-pages.yaml` 확장 또는 분리 workflow.
**의존**: 없음.

### PR 11 — OAuth2 Resource Server 마이그레이션 🔐 ⭐ (선택)
**Why**: 수동 `JwtAuthenticationFilter` → Spring Security 표준 Resource Server. 검증 / 클레임 추출 / JWK 도입 자연스러움.
**고려 분기점**: 자체 발급 유지 vs 외부 IdP, 토큰 호환성 (소프트 전환), `JwtAuthenticationConverter` 로 UUID principal 추출, 발급 코드만 보존.
**의존**: PR 10 ✅ — 큰 리팩토링 안전한 시점. **위험**: 졸업프로젝트 범위 초과 가능 — "시간 남으면" 카테고리.

### PR 12 — 소셜 로그인 (Google + Kakao) 🔐 ⭐⭐⭐
**Why**: 졸업 발표에 "구글로 로그인 한 줄" 임팩트 큼. 한국 사용자 Kakao 자연스러움.
**범위 핵심 결정**: FE 가 provider access token 받아서 BE 로 넘기는 방식 (SPA/모바일 친화). `POST /auth/oauth2/{provider}` 신설. `User` 엔티티에 `provider` enum + `providerId` + password nullable 화. **신규 가입 / 기존 유저 연동 흐름은 PR 10 의 refresh rotation 위에 자연스럽게 쌓임**.
**고려 분기점**: 이메일 중복 정책 (LOCAL + GOOGLE 양쪽 가입 차단 권장), provider userinfo 검증 (Google `tokeninfo` / Kakao `/v2/user/me`), Google/Kakao Console 셋업 (1회), 환경변수 3중 동기화 (compose / EC2 `.env` / GitHub Secrets).
**의존**: PR 10 ✅ (완료).

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

---

## 📝 Changelog

| 일자 | 변경 |
|---|---|
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
