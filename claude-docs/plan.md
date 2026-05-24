# Moodiary Backend — Roadmap

> 이 파일은 백엔드 작업 진행 상황을 추적합니다.
> 완료된 항목은 `[x]`로 체크하고, 진행 중이면 본문에 메모를 남깁니다.
> PR이 머지될 때마다 갱신.

---

## ✅ 완료

### Post 도메인 (기본 CRUD)
- [x] `POST /post` — 게시글 생성
- [x] `GET /post` — 전체 조회
- [x] `GET /post/{id}` — 단건 조회
- [x] `PUT /post/{id}` — 수정 (dirty checking)
- [x] `DELETE /post/{id}` — 삭제
- [x] `PostNotFoundException` + `GlobalExceptionHandler` (404/400/500 통합 응답 포맷 `{"message":"..."}`)
- [x] PR #14 → dev 머지
- [x] PR #15 → main 머지 (운영 반영)

### 인프라
- [x] Swagger UI 도입 (SpringDoc OpenAPI 2.8.3) — `/swagger-ui/index.html`
- [x] CI/CD GitHub Actions 구축 (CI: PR→dev / CD: push→main)
- [x] AWS SSM 기반 배포로 전환 (옛 SSH 방식 폐기)
- [x] **Elastic IP 부착** (`15.165.95.129`) — 인스턴스 stop/start해도 IP 고정
- [x] **EC2 docker compose를 RDS로 통일** — 로컬 mysql 컨테이너 제거, `.env`로 RDS 자격증명 관리
- [x] RDS Security Group: EC2 → 3306 인바운드 허용
- [x] RDS에 `post` 테이블 생성 (수동 DDL)
- [x] 옛 SSH 시절 GitHub Secrets 정리 (`EC2_HOST`/`USER`/`KEY` 삭제)

### 문서/관리
- [x] `CLAUDE.md` 실제 코드/CD 상태와 동기화
- [x] `.local-logs/` 폴더 + `.gitignore` (운영 로그 비추적)

### 테스트
- [x] `PostControllerTest` — `@WebMvcTest` + `@MockitoBean` 기반 13개 케이스
  - POST 4개 (성공/title 빈/content 빈/JSON 깨짐)
  - GET 전체 2개, GET 단건 2개
  - PUT 3개, DELETE 2개
- [x] `./gradlew test` 통과 검증 (13/13 ✅)
- [x] `JpaAuditingConfig` 분리 — `@WebMvcTest` 슬라이스 테스트와 호환되도록
- [x] PR #16 생성 (`test/post-controller` → `dev`)

---

## 🔄 진행 중

> (현재 비어있음 — PR #16 머지 대기)

---

## 📋 백로그 (예정)

> 우선순위 순. PR 단위로 잘게 쪼개서 진행.

### PR 0 — PostServiceTest (우선순위 ⭐⭐⭐, 작음)
**Why**: 컨트롤러 테스트는 끝났으니 서비스 로직(404 분기·dirty checking) 단위 테스트로 마무리.

- [ ] Mockito 기반 단위 테스트 (Spring 컨텍스트 안 띄움)
- [ ] `getPost`/`update`/`delete`의 not-found 분기 → `PostNotFoundException`
- [ ] `update`가 `Post.update(...)` 호출하는지 (dirty checking 보장)
- [ ] `delete`가 `existsById` false면 `deleteById` 미호출 검증

**난이도**: 매우 작음 (15분 내) | **PR #16 머지 직후 바로 가능**

---

### PR 1 — CD를 docker compose 호출로 통일 🔧 (우선순위 ⭐⭐⭐)
**Why**: 현재 CD는 `docker run`을 직접 호출해서 EC2의 `compose.yaml`을 무시함. 결과적으로 `restart: unless-stopped`/`pull_policy: always` 정책이 빠지고, **EC2 재부팅 시 컨테이너 자동 기동 안 됨**.

- [ ] `compose.yaml`을 repo에 `docker-compose.yml`로 커밋 (`.env`는 gitignore)
- [ ] `.env.example` 추가 (RDS_ENDPOINT 등 키만)
- [ ] CD 워크플로우 수정: SSM 명령을 `docker compose pull && docker compose up -d`로 변경
- [ ] 첫 배포 전에 EC2에서 `.env` 존재 확인 (수동)

**난이도**: 작음 | **다음 EC2 재부팅 전에 처리하는 게 좋음**

---

### PR 2 — User 도메인 + Spring Security 베이스 🔐 (우선순위 ⭐⭐)
**Why**: 다중 사용자 일기 플랫폼이라 인증 필수. AI 통합 전에 베이스 깔아두면 깔끔.

- [ ] `User` 엔티티 (`id`, `email`, `password`(BCrypt), `nickname`, `BaseEntity` 상속)
- [ ] `UserRepository`
- [ ] `SecurityFilterChain` 설정 (CSRF off — REST API라)
- [ ] `PasswordEncoder` Bean (`BCryptPasswordEncoder`)
- [ ] `POST /auth/signup` — 회원가입
- [ ] `POST /auth/login` — JWT 토큰 발급
- [ ] JWT 검증 필터 (Spring Security 6 OAuth2 Resource Server 권장)
- [ ] `application.yaml`에 JWT 시크릿/만료시간 (운영은 env var)
- [ ] 화이트리스트: `/swagger-ui/**`, `/v3/api-docs/**`, `/auth/**`
- [ ] 테스트: `@WithMockUser` 기반 컨트롤러 테스트

**난이도**: 중간 | **Post는 손대지 않음** (다음 PR에서)

---

### PR 3 — Post를 사용자 소유로 전환 👤 (우선순위 ⭐⭐)
**Why**: 인증 베이스가 깔린 후 Post에 소유자 개념 도입.

- [ ] `Post`에 `@ManyToOne User user` 추가 (`user_id` FK)
- [ ] `PostController`에서 `@AuthenticationPrincipal`로 현재 유저 받기
- [ ] `POST /post`: 현재 유저로 자동 채움
- [ ] `GET /post`: 본인 일기만 조회 (또는 공개/비공개 정책 결정)
- [ ] `PUT/DELETE /post/{id}`: 본인 글이 아니면 `403 Forbidden`
- [ ] **DB 마이그레이션**: `post` 테이블에 `user_id` 컬럼 추가 (운영에 데이터 있으니 주의)
- [ ] Flyway 도입 고려 (이 시점이 적기)

**난이도**: 중간 | **운영 DB 마이그레이션 동반** — Flyway 같이 도입 추천

---

### PR 4 — 비동기 AI 응답 모듈 🤖 (우선순위 ⭐⭐)
**Why**: 프로젝트 핵심 차별 기능. 일기 → AI 응답.

**설계 방향**: 단순화된 비동기 (큐/메시지브로커 없이 `@Async` + DB 상태 관리)

- [ ] `AiResponse` 엔티티: `id`, `post_id`, `status (PENDING/DONE/FAILED)`, `content`, `error_message`, `BaseEntity`
- [ ] `AiResponseRepository`
- [ ] `@EnableAsync` + `@Async` 메서드 (별 스레드풀)
- [ ] AI 서버 호출: `RestClient` (Spring 6 새 동기 클라이언트)
- [ ] **AI 서버 URL/타임아웃 설정** ← AI 담당자와 API 계약 협의 필요
- [ ] `POST /post` 흐름 변경:
  1. 일기 저장
  2. `AiResponse(status=PENDING)` 생성
  3. `@Async`로 AI 호출 트리거
  4. 즉시 201 반환 (`postId`)
- [ ] `GET /post/{id}/ai-response` 폴링 엔드포인트
- [ ] 재시도/타임아웃 정책 (Spring Retry 검토)
- [ ] AI 응답 실패 시 처리 (status=FAILED + error_message)
- [ ] 테스트: AI 서버 mock (WireMock 또는 `@MockitoBean` RestClient)

**난이도**: 큼 | **AI 담당자와 API 계약 먼저 합의** (URL/요청바디/응답바디/타임아웃)

---

### PR 5 — Flyway 도입 (선택, 우선순위 ⭐)
**Why**: 운영 DB 스키마를 코드로 관리. PR 3에서 user_id 추가할 때 같이 도입하면 자연스러움.

- [ ] `flyway-mysql` 의존성 추가
- [ ] `src/main/resources/db/migration/V1__init_post.sql` (현재 스키마 베이스라인)
- [ ] `V2__add_user.sql`, `V3__post_add_user_id.sql` 등 순차 작성
- [ ] `ddl-auto: validate`로 변경 (자동 DDL 위험 차단)
- [ ] 운영 RDS에 베이스라인 적용 (`flyway baseline` 또는 콘솔 명령)

---

### PR 6 — ECS 이전 (먼 미래, 우선순위 ⭐)
**Why**: 운영 안정성 + 확장성. 졸업 발표 전 시간 남으면.

- [ ] ECR 리포지토리 생성
- [ ] ECS 클러스터 + Fargate 태스크 정의
- [ ] ALB 붙이기 (현재 EIP 대신 도메인 + HTTPS)
- [ ] CD를 ECR push + ECS service update로 전환
- [ ] CloudWatch 로그 통합

---

## 💡 메모 / 결정사항

- **AI 통신 방식**: 비동기로 시작 → 차후 발전. 단순한 `@Async` + DB 상태 관리 모델.
- **인증**: Spring Security + JWT 도입 예정.
- **DB**: RDS 단일 소스. 로컬 mysql 컨테이너는 폐기.
- **CD 트리거**: `push → main`만 트리거. `dev`는 통합 브랜치.
- **EC2 자격증명 관리**: GitHub Secrets (CD용) + EC2 `~/.env` (manual compose용) 이중 관리 중. 추후 AWS Parameter Store/Secrets Manager로 단일화 검토.

## 🔗 참고 링크
- 운영 Swagger UI: http://15.165.95.129:8080/swagger-ui/index.html (PR #15 머지 후)
- OpenAPI spec: http://15.165.95.129:8080/v3/api-docs (프론트엔드와 공유용)
- GitHub Actions: https://github.com/Goospel/goospel-moodiary/actions