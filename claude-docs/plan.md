# Moodiary Backend — Roadmap

> 백엔드 작업 진행 상황과 계획을 한 곳에 모은 문서.
> **규칙**: PR이 머지될 때마다 갱신. 완료는 `[x]`로 체크, 진행 중이면 간단히 메모.
> 마지막 갱신: 2026-05-24 (PR #16 머지 직후)

---

## 📊 현재 상태 한눈에

| 항목 | 상태 |
|---|---|
| **운영 URL** | http://15.165.95.129:8080 (Elastic IP, 고정) |
| **Swagger UI** | http://15.165.95.129:8080/swagger-ui/index.html |
| **OpenAPI spec** | http://15.165.95.129:8080/v3/api-docs (FE 협업용) |
| **운영 인프라** | EC2 (Amazon Linux 2023) + Docker + RDS MySQL 9.x |
| **배포 방식** | `push → main` → GitHub Actions → AWS SSM → EC2 `docker run` |
| **현재 운영에 올라간 기능** | Post CRUD (GET 전체/단건, POST, PUT, DELETE) + Swagger UI |
| **dev에 있고 운영 미반영** | PostControllerTest 13개 (자동화 테스트, 운영 동작 무관) |
| **Java/Spring** | Java 25 / Spring Boot 4.0.6 |

> 🛠️ **즉시 조치 필요 없음.** 새 기능 작업하기 좋은 상태.

---

## ✅ 완료된 작업

> 운영 반영 = 🟢, dev only = 🟡

### Post 도메인 (기본 CRUD) — 🟢 운영 반영
| 엔드포인트 | PR | 비고 |
|---|---|---|
| `POST /post` | #14 | Bean Validation (`@NotBlank` title/content) |
| `GET /post` | #14 | 전체 조회 (페이지네이션 X — TODO) |
| `GET /post/{id}` | #14 | 단건 조회, 없으면 404 |
| `PUT /post/{id}` | #14 | dirty checking 기반 수정 |
| `DELETE /post/{id}` | #14 | 204 No Content |
| 전역 예외 처리 | #14 | 404/400/500 → `{"message":"..."}` 통일 |

### 인프라 — 🟢 운영 반영
- [x] Swagger UI (SpringDoc OpenAPI 2.8.3)
- [x] CI/CD GitHub Actions
  - CI: `PR → dev` → `./gradlew build`
  - CD: `push → main` → Docker Hub 푸시 → AWS SSM `send-command` → EC2 배포
- [x] AWS SSM 기반 배포 (옛 SSH 방식 폐기 — `EC2_HOST/USER/KEY` 시크릿 삭제됨)
- [x] **Elastic IP 부착** (`15.165.95.129`) — 인스턴스 stop/start해도 IP 고정
- [x] **EC2 docker compose를 RDS로 통일** — 로컬 mysql 컨테이너 폐기, `.env` 기반 자격증명
- [x] RDS Security Group: EC2 → 3306 인바운드 허용
- [x] RDS에 `post` 테이블 생성 (수동 DDL — Flyway 도입 전까지)

### 문서/관리 — 🟢 운영 반영
- [x] `CLAUDE.md` 실제 코드/CD 상태 동기화 (Claude Code용)
- [x] `.local-logs/` 폴더 + `.gitignore` (운영 로그 비추적)
- [x] `claude-docs/plan.md` 이 문서 (Roadmap)

### 테스트 — 🟡 dev only
- [x] `PostControllerTest` 13개 케이스 (`@WebMvcTest` + `@MockitoBean`) — PR #16
  - POST 4 / GET 전체 2 / GET 단건 2 / PUT 3 / DELETE 2 — 0 failures
- [x] `JpaAuditingConfig` 분리 (`@EnableJpaAuditing`을 `MoodiaryApplication`에서 빼냄)
  - 슬라이스 테스트(`@WebMvcTest`)와 호환 필요

---

## 🔄 진행 중

> 현재 활성 작업 없음. PR #16 머지 직후.

---

## 📋 백로그 (우선순위 + 의존성)

```
PR 0 PostServiceTest ─────────────────────┐
                                          │
PR 1 CD를 compose로 통일 ────────────┐    │
                                     ▼    ▼
PR 2 User + Security ──┐         (독립적)
                       │
PR 3 Post 소유권 ──────┼──► PR 5 Flyway (마이그레이션 도입 적기)
                       │
                       └──► PR 4 AI 비동기 응답
                                          │
PR 6 ECS 이전 ◄───────────── (먼 미래) ──┘
```

### PR 0 — PostServiceTest ⭐⭐⭐ (작음)
**Why**: 컨트롤러 테스트는 끝났으니 서비스 로직 단위 테스트로 마무리.

- [ ] Mockito 기반 (Spring 컨텍스트 없이)
- [ ] `getPost`/`update`/`delete`의 not-found 분기 → `PostNotFoundException` 발사
- [ ] `update`가 `Post.update(...)` 호출하는지 (dirty checking 보장)
- [ ] `delete`가 `existsById==false`면 `deleteById` 미호출 검증

**예상 소요**: 15분 | **의존**: 없음 | **위험**: 없음 (테스트 추가)

---

### PR 1 — CD를 docker compose 호출로 통일 🔧 ⭐⭐⭐
**Why**: 현재 CD는 `docker run`을 직접 호출해 EC2의 `compose.yaml`을 무시한다. 결과적으로:
- `restart: unless-stopped` 빠짐 → **EC2 재부팅 시 컨테이너 자동 기동 X**
- `pull_policy: always` 빠짐
- manual compose와 CD가 서로 모르게 부딪힘

- [ ] EC2의 `compose.yaml`을 repo에 `docker-compose.yml`로 커밋
- [ ] `.env.example` 추가 (RDS_ENDPOINT 등 키만, 값 빈칸)
- [ ] `.env`는 `.gitignore`에 명시
- [ ] CD 워크플로우 수정: SSM 명령을 `cd /home/ec2-user && docker compose pull && docker compose up -d`로 변경
- [ ] 첫 배포 전 EC2에 `.env` 존재 + repo 최신 동기화 확인 (수동 1회)
- [ ] PR 머지 후 인스턴스 재부팅으로 자동 기동 검증

**예상 소요**: 1-2시간 | **의존**: 없음 | **위험**: 첫 배포 시 `.env` 없으면 컨테이너 실패. 사전 점검 필수

---

### PR 2 — User 도메인 + Spring Security 🔐 ⭐⭐
**Why**: 다중 사용자 일기 플랫폼이라 인증 필수. AI 통합 전에 베이스 깔기.

- [ ] `User` 엔티티 (`id`, `email`, `password`(BCrypt), `nickname`, `BaseEntity` 상속)
- [ ] `UserRepository`
- [ ] `SecurityFilterChain` (CSRF off — REST API)
- [ ] `PasswordEncoder` Bean (`BCryptPasswordEncoder`)
- [ ] `POST /auth/signup` — 회원가입
- [ ] `POST /auth/login` — JWT 발급
- [ ] JWT 검증 필터 (Spring Security 6 OAuth2 Resource Server 권장)
- [ ] `application.yaml`에 JWT 시크릿/만료 (운영은 env var)
- [ ] 화이트리스트: `/swagger-ui/**`, `/v3/api-docs/**`, `/auth/**`
- [ ] 테스트: `@WithMockUser` 기반 컨트롤러 테스트
- [ ] **FE 분과 토큰 저장 위치 합의** (localStorage vs httpOnly cookie)

**예상 소요**: 1주 | **의존**: 없음 | **위험**: 화이트리스트 누락 시 Swagger UI 차단. JWT 시크릿 노출

---

### PR 3 — Post를 사용자 소유로 전환 👤 ⭐⭐
**Why**: 인증 베이스 후 Post에 소유자 도입.

- [ ] `Post`에 `@ManyToOne User user` 추가 (`user_id` FK)
- [ ] `PostController`에서 `@AuthenticationPrincipal`로 현재 유저 받기
- [ ] `POST /post`: 현재 유저로 자동 채움
- [ ] `GET /post`: 본인 일기만 (또는 공개/비공개 정책 결정)
- [ ] `PUT/DELETE /post/{id}`: 본인 글 아니면 `403 Forbidden`
- [ ] **DB 마이그레이션**: 운영 `post` 테이블에 `user_id` 컬럼 추가 — 기존 데이터 있으면 처리 필요

**예상 소요**: 2-3일 | **의존**: PR 2 머지 | **위험**: 운영 DB 마이그레이션 동반. **Flyway(PR 5) 같이 도입 강력 추천**

---

### PR 4 — 비동기 AI 응답 모듈 🤖 ⭐⭐
**Why**: 프로젝트 핵심 차별 기능. 일기 → AI 응답.

**설계 방향**: 단순화된 비동기 (큐/메시지브로커 없이 `@Async` + DB 상태 관리)

- [ ] `AiResponse` 엔티티: `id`, `post_id`, `status (PENDING/DONE/FAILED)`, `content`, `error_message`, `BaseEntity`
- [ ] `AiResponseRepository`
- [ ] `@EnableAsync` + `@Async` 메서드 (전용 스레드풀)
- [ ] AI 서버 호출: `RestClient` (Spring 6 새 동기 클라이언트)
- [ ] `POST /post` 흐름 변경:
  1. 일기 저장
  2. `AiResponse(status=PENDING)` 생성
  3. `@Async`로 AI 호출 트리거
  4. 즉시 201 반환 (`postId`)
- [ ] `GET /post/{id}/ai-response` 폴링 엔드포인트
- [ ] 재시도/타임아웃 정책 (Spring Retry)
- [ ] AI 응답 실패 시 처리 (status=FAILED + error_message)
- [ ] 테스트: AI 서버 mock (WireMock 또는 `@MockitoBean` RestClient)
- [ ] **AI 담당자와 API 계약 먼저 합의** (URL/요청바디/응답바디/타임아웃/인증)

**예상 소요**: 1-2주 | **의존**: PR 3 머지 (Post에 소유자 있어야 함) | **위험**: AI 서버 다운 시 일기 작성 자체는 안 막히게 fail-safe 설계 필수

---

### PR 5 — Flyway 도입 (DB 마이그레이션) ⭐
**Why**: 운영 DB 스키마를 코드로 관리. **PR 3에서 `user_id` 추가할 때 같이 도입하면 자연스러움.**

- [ ] `flyway-mysql` 의존성 추가
- [ ] `src/main/resources/db/migration/V1__init_post.sql` — 현재 스키마 베이스라인
- [ ] `V2__add_user.sql`, `V3__post_add_user_id.sql` 등 순차 작성
- [ ] `ddl-auto: validate`로 변경 (자동 DDL 위험 차단)
- [ ] 운영 RDS에 `flyway baseline` 적용 (이미 운영 중인 DB라 베이스라인 필요)

**예상 소요**: 1-2일 | **의존**: PR 3와 함께 (시점이 적기) | **위험**: 베이스라인 적용 실수 시 운영 DB 마이그레이션 꼬임

---

### PR 6 — ECS 이전 ⭐ (먼 미래)
**Why**: 운영 안정성 + 확장성. 졸업 발표 전 시간 남으면.

- [ ] ECR 리포지토리 생성
- [ ] ECS 클러스터 + Fargate 태스크 정의
- [ ] ALB 붙이기 (현재 EIP → 도메인 + HTTPS)
- [ ] CD를 ECR push + ECS service update로 전환
- [ ] CloudWatch 로그 통합

**예상 소요**: 1주 | **의존**: PR 1 권장 (compose 통일된 상태가 옮기기 쉬움) | **위험**: 비용 증가, 학부 졸업프로젝트 범위 초과 가능

---

## 🤝 FE 협업

### 현재 노출된 API (PR #15 이후)
- `POST /post`, `GET /post`, `GET /post/{id}`, `PUT /post/{id}`, `DELETE /post/{id}`

### 공유 자산
- **OpenAPI spec**: http://15.165.95.129:8080/v3/api-docs
  → FE에서 TypeScript 타입 자동 생성 (`openapi-typescript`), 클라이언트 자동 생성 (`openapi-fetch`), 또는 mock 서버(Prism)에 사용 가능
- **Swagger UI**: http://15.165.95.129:8080/swagger-ui/index.html
  → "Try it out"으로 직접 호출 가능

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
```

### DB
```bash
# .env 로드
set -a; source ~/.env; set +a

# RDS 접속 (비번 프롬프트)
mysql -h "$RDS_ENDPOINT" -u "$RDS_USERNAME" -p moodiary

# 또는 환경변수로 비번 자동 (보안 경고 없음)
MYSQL_PWD="$RDS_PASSWORD" mysql -h "$RDS_ENDPOINT" -u "$RDS_USERNAME" moodiary
```

### GitHub Actions
- 워크플로우: https://github.com/Goospel/hoseo-moodiary/actions
- CI 트리거: `PR → dev`
- CD 트리거: `push → main` (보통 dev → main PR 머지가 곧 배포)

### 트러블슈팅 가이드
| 증상 | 1차 확인 |
|---|---|
| 8080 응답 없음 | `docker ps` (컨테이너 살아있나) → `docker logs` |
| 500 응답 | `docker logs --tail=200 hoseo-moodiary` → DB 연결 또는 테이블 미존재 |
| 502/504 게이트웨이 에러 | (현재 ALB 없으니 해당사항 없음) |
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
| DB는 **RDS 단일 소스** | 인스턴스 stop/start 데이터 보존, 백업 자동화 | 이번 세션 |
| **Elastic IP 부착** | 인스턴스 켜고 끌 때마다 IP 갱신하는 게 비효율 (월 5,000원이 인건비보다 쌈) | 이번 세션 |
| AI는 **비동기 + DB 상태** | 큐/브로커 없이 단순 구조로 시작. 졸업프로젝트 범위 | 백로그 PR 4에서 |
| 인증은 **JWT** | Stateless REST API에 적합, 프론트 자유도 | 백로그 PR 2에서 |
| 자격증명 이중 관리 (GitHub Secrets + EC2 `.env`) | 단순함 우선. 추후 AWS Parameter Store로 단일화 검토 | 이번 세션 |

---

## 📝 Changelog

| 일자 | 변경 |
|---|---|
| 2026-05-24 | 이 문서 신설. PR #14/#15/#16 작업 결과 반영. 백로그 의존성 그래프·FE 협업·운영 치트시트·의사결정 로그 섹션 추가 |
