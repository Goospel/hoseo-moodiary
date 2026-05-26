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
    font-size: 26px;
  }
  h1 { color: #2563eb; font-size: 42px; }
  h2 { color: #1e40af; font-size: 34px; }
  h3 { color: #1e3a8a; font-size: 28px; }
  code { background: #f1f5f9; padding: 2px 6px; border-radius: 4px; }
  pre { background: #0f172a; color: #e2e8f0; padding: 12px; border-radius: 6px; font-size: 18px; }
  table { font-size: 22px; }
  th { background: #2563eb; color: white; }
  blockquote { border-left: 4px solid #2563eb; color: #475569; }
  .highlight { background: #fef3c7; padding: 2px 6px; border-radius: 4px; }
---

<!-- _class: lead -->

# Moodiary Backend

### 일기 + AI 무드 캘린더 플랫폼

호서대학교 컴퓨터공학부 졸업프로젝트
**Backend 파트**

---

## 발표 구성

1. **프로젝트 한눈에** — 무엇을 만들었나
2. **여정** — 어떤 과정으로 여기까지 왔나
3. **핵심 구현** — 무엇이 들어가 있나
4. **핵심 트러블슈팅** — 무엇을 학습했나
5. **앞으로** — 무엇을 더 할 것인가

---

<!-- _class: lead -->

# 1. 프로젝트 한눈에

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
| **Backend (이번 발표)** | REST API, DB, 배포, AI 서버 연동 |
| Frontend | React + Vite SPA, S3 정적 호스팅 |
| AI 추론 서버 | 일기 텍스트 → 응답 + 이모지 생성 |

---

## 스택과 운영

| 분류 | 기술 |
|---|---|
| 언어 / 런타임 | **Java 25** (Amazon Corretto) |
| 프레임워크 | **Spring Boot 4.0.6**, Spring Security 6, Spring Data JPA |
| 데이터 | MySQL 9.x (AWS RDS), Hibernate 7.2, **QueryDSL** |
| 인증 | **JWT** (jjwt 0.12.6, HS512, 24h) + BCrypt |
| API 문서 | SpringDoc OpenAPI 3.0.3 (Swagger UI) |
| 운영 | **Docker** + AWS EC2 (Amazon Linux 2023) + **Elastic IP** |
| CI/CD | GitHub Actions → Docker Hub → **AWS SSM** `send-command` → EC2 |

🔗 운영 URL: `http://15.165.95.129:8080` · Swagger 활성

---

<!-- _class: lead -->

# 2. 여정 — 어떤 과정으로 왔는가

---

## 큰 흐름

```
[1단계] 인프라 + 기본 CRUD          PR #1 ~ #16
        Spring Boot 4 셋업, EC2/RDS, Docker, Post CRUD, 슬라이스 테스트

[2단계] 인증 + 소유권                PR #20 ~ #27
        회원가입(BCrypt), JWT 로그인, SecurityFilterChain, Post 소유권 격리

[3단계] 캘린더 + QueryDSL 도입       PR #31 ~ #36
        월별 캘린더 API, QueryDSL 첫 도입, 외부 라이브러리 호환성 첫 학습

[4단계] CORS + 프론트 통합 준비      PR #37 ~ #51   ★ 본 세션 집중
        CorsConfig, FE 가이드, ⚠️ 운영 부팅 폭발 대사건

[5단계] CD 신뢰성 + Workflow 규칙    PR #40 ~ #48
        SSM wait/verify, health check, 옛 silent fail 진실 드러남
```

---

## 단계별 핵심 결정

### 1단계 — 인프라
- **EC2 + RDS 분리** — 로컬 docker mysql 폐기, RDS 로 통일
- **Elastic IP 부착** — stop/start 해도 IP 고정
- **AWS SSM 기반 배포** — SSH 키 관리 불필요

### 2단계 — 인증
- **Stateless JWT** — refresh token 없는 단순화 (24h)
- **`@EnableJpaAuditing` 별도 클래스 분리** — 슬라이스 테스트 호환 (T-003)
- **Post 소유권 격리** — `@ManyToOne User`, 본인 글만 CRUD

---

## 단계별 핵심 결정 (계속)

### 3단계 — 캘린더 + QueryDSL
- **`compileOnly` → `implementation` 전환** (T-016) — runtime classpath 누락 발견
- **타임존 KST 명시** — `LocalDate` 변환으로 날짜 그룹핑
- **`MissingServletRequestParameterException` 매핑** (T-017) — 필수 파라미터 누락 → 400 통일

### 4-5단계 — CORS + 운영 정상화
- **CORS 외부화** — `app.cors.allowed-origins` env var override
- **ApplicationContext 안전망 복원** — `MoodiaryApplicationTests` `@Disabled` 제거 + H2
- **CD 신뢰성 강화** — `aws ssm wait command-executed` + swagger 200 health check
- **docker-compose 명령 형식 통일** — plugin 미등록 환경 대응

---

<!-- _class: lead -->

# 3. 핵심 구현

---

## API 표면

```
인증 (Whitelisted)
  POST /auth/signup         이메일/닉네임 UNIQUE, BCrypt
  POST /auth/login          JWT 발급 (HS512, 24h)

Post — 본인 글만 (소유권 격리)
  POST   /post              인증 필수, 작성자 자동
  GET    /post              본인 글 목록
  GET    /post/{id}         본인 200 / 타인 403 / 없음 404
  PUT    /post/{id}         본인 글만 수정
  DELETE /post/{id}         본인 글만 삭제, 204

Calendar
  GET /calendar?year=&month=    월별 일자 그룹핑
                                emoji 는 PR 4 (AI 응답) 후 후속
```

**전역 예외 처리** — 400/401/403/404/409/500 → `{"message":"..."}` 통일

---

## 보안 한눈에

```
요청 → SecurityFilterChain → JwtAuthenticationFilter → Controller
              |
              ├─ 화이트리스트 (/auth/**, /swagger-ui/**, /v3/api-docs/**)
              |   → permitAll()
              |
              └─ 그 외 → authenticated()
                  미인증 시 401 + {"message":"인증이 필요합니다."}
```

- **CSRF disabled** (REST API)
- **STATELESS** session (JWT 만 신뢰)
- **CORS** — preflight 가 인증 검사 전 통과 (`.cors(Customizer.withDefaults())`)
- **BCrypt** 해시 (cost 10)

---

## 인프라 + CI/CD

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
                               │ ap-NE-2      │                │
                               └──────────────┘                │
```

- **CI** (PR → dev): `./gradlew build` + 테스트
- **CD** (push → main): Docker build & push → SSM → EC2 `docker-compose pull && up -d` → **health check**

---

## 테스트 전략

| 레이어 | 도구 | 케이스 수 |
|---|---|---|
| Service (단위) | JUnit 5 + Mockito | PostService 12, UserService 6, CalendarService 9 |
| Controller (슬라이스) | `@WebMvcTest` + `@MockitoBean` | Post 16, Auth 11, Calendar 4 |
| 보안 컴포넌트 | 라운드트립/위조/만료 | JwtTokenProvider 4 |
| CORS | `@WebMvcTest` + `@Import(SecurityConfig)` | CorsConfig 4 |
| **부팅 안전망** | `@SpringBootTest` + H2 in-memory | **ApplicationContext 1** |

### **총 70 tests · 0 failures**

> 부팅 안전망이 가장 중요 — 슬라이스 테스트만으로는 못 잡는 자동 구성 빈 실패를 빌드 단계에서 잡는다 (대사건 학습 결과)

---

<!-- _class: lead -->

# 4. 핵심 트러블슈팅

---

## 트러블슈팅 문서 — T-### 로그

`claude-docs/troubleshooting.md` 에 작업 중 막힌 모든 지점을 누적 기록.

| 카테고리 | 항목 수 |
|---|---|
| Spring Boot 4.x 마이그레이션 함정 | 3 (T-001 ~ T-003) |
| Spring Security + 슬라이스 테스트 | 3 (T-004, T-005, T-012) |
| **빌드/배포 함정** | **11 (T-013 ~ T-023)** |
| AWS / 운영 인프라 | 2 |
| DB | 2 |
| CLI / 인코딩 | 2 |

> **규칙**: 모든 PR 의 마지막에서 두 번째 task = "troubleshooting.md 점검"
> 1분 이상 디버깅한 모든 이슈를 후보로 두고, 프로젝트 고유 trap 이면 새 항목 추가

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

---

<!-- _class: lead -->

# 5. 앞으로

---

## 단기 — PR 8 마무리

| 단계 | 상태 |
|---|---|
| BE — CORS + compose env 주입 + FE 가이드 | ✅ 완료 |
| FE — 가이드 따라 S3 셋업 + 배포 | ⏳ 외부 대기 |
| BE — EC2 `.env` 에 S3 endpoint 추가 | ⏳ FE 회신 후 |
| 통합 검증 — 회원가입→로그인→다이어리→캘린더 | ⏳ |

졸업 데모의 시각적 인상 확보 — 프론트가 떴고 백엔드와 통신

---

## 중기 — PR 4 AI 비동기 응답 ⭐⭐⭐

### 차별 기능
- 일기 → AI 가 **공감 메시지 + 이모지** 생성
- `AiResponse(PENDING/DONE/FAILED)` 엔티티
- `@Async` + `RestClient` 로 비동기 호출
- `GET /post/{id}/ai-response` 폴링

### 의존 / 위험
- 외부 AI 서버 계약 합의 필요 (`api-contracts.md`)
- 이모지 저장은 utf8mb4 필수
- AI 서버 다운 시 일기 작성 자체는 막히지 않게 fail-safe

PR 4 머지 후 → PR 5 후속 (캘린더에 emoji LEFT JOIN) 자연스럽게 이어짐

---

## 장기 — 안정화 + 확장

### PR 6 — Flyway 도입
- 현재 `ddl-auto: update` 의 한계 영구 해소
- T-019 의 FK orphan 같은 사고 차단
- 외부 의존 X — 단독 진행 가능

### PR 7 — ECS / ALB / ACM / 도메인
- HTTPS 화 + 도메인 + ALB + ACM 인증서
- 졸업 발표 이후 단계 (범위 vs 데모 가시성 트레이드오프)

### 운영 관측성
- CloudWatch 로그 통합 (PR 7 과 함께)
- 알람 (5xx 비율, 컨테이너 죽음, RDS 연결 실패)

---

## 마치며

### 숫자로 보는 성과
- **51 PRs** 머지 — 모든 변경이 PR 단위로 리뷰되고 history 보존
- **23 트러블슈팅 항목** (T-001 ~ T-023) — 막힌 지점이 다음 사람의 시작점이 되도록
- **70 테스트** 0 failures — 슬라이스 + 단위 + 부팅 안전망
- **5층 결함 대사건** 복구 — silent fail 시대를 끝내고 방어선 박음
- **운영 정상화** — CD success 가 진짜 deploy 성공을 의미하게 됨

### 가장 큰 학습
> 사고는 단일 결함이 아니라 **여러 silent fail 의 축적**일 수 있다.
> 안전망과 가시화는 *기능* 만큼 중요하다.

---

<!-- _class: lead -->

# 감사합니다

### Q&A

📚 **문서**:
- README.md — 프로젝트 소개 + 배포 구성
- claude-docs/plan.md — PR 단위 로드맵
- claude-docs/troubleshooting.md — T-### 트러블슈팅 로그
- claude-docs/security.md — JWT / CORS / 인증
- claude-docs/api-contracts.md — API 명세

🔗 **운영**: `http://15.165.95.129:8080/swagger-ui/index.html`
