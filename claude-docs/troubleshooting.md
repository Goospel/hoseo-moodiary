# Moodiary Backend — 트러블슈팅 로그

> 작업 중 막혔던 지점, 원인, 해결법을 한 파일에 누적.
> **규칙**: 새로 막힐 때마다 맨 위 인덱스에 한 줄 추가 → 해당 섹션에 상세 기록.
> 마지막 갱신: 2026-05-24 (PR 1 - CD compose 통일)

---

## 📑 인덱스

### Spring Boot 4.x 마이그레이션 함정
- [T-001](#t-001) `@WebMvcTest` 패키지가 이동했다 — `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
- [T-002](#t-002) `@WebMvcTest`에서 `ObjectMapper` 빈 주입 실패 — Jackson auto-config 미포함
- [T-003](#t-003) `@WebMvcTest`가 JPA를 끌고 들어가 "JPA metamodel must not be empty"

### Spring Security + 슬라이스 테스트
- [T-004](#t-004) `@WithMockUser`가 `@Nested` 클래스에 자동 적용되지 않음
- [T-005](#t-005) Spring Security 의존성 추가만으로 모든 엔드포인트 401 — 슬라이스 테스트도 깨짐
- [T-012](#t-012) SecurityConfig가 의존하는 빈이 늘면, 그걸 `@Import`하는 모든 슬라이스 테스트가 깨짐

### 빌드/배포 함정
- [T-013](#t-013) `application.yaml` 이 `.gitignore` 되어 있어 운영에 설정이 전달되지 않음
- [T-014](#t-014) CD 워크플로우에 시크릿 주입 라인 누락 — yaml 기본값이 운영에 노출
- [T-015](#t-015) Claude 가 머지된 PR 을 열린 것으로 착각 → 닫힌 브랜치에 작업 계속

### AWS / 운영 인프라
- [T-006](#t-006) EC2 stop/start 시 퍼블릭 IP가 매번 바뀜 → GitHub Secret 갱신 지옥
- [T-007](#t-007) `docker-compose exec` 서비스명을 컨테이너명으로 착각

### DB
- [T-008](#t-008) `ddl-auto: validate`인데 운영 RDS에 테이블이 없어서 500
- [T-009](#t-009) MySQL `USER`는 예약어 — `@Table(name="users")` 강제

### CLI / 인코딩
- [T-010](#t-010) `mysql -p <비번>` 사이 공백 → 비번이 DB명으로 해석되어 사용법 페이지 출력
- [T-011](#t-011) git-bash에서 curl로 한글 POST 시 400 — 인코딩 문제

---

## 🪤 Spring Boot 4.x 마이그레이션 함정

<a id="t-001"></a>
### T-001 · `@WebMvcTest` 패키지 이동

| | |
|---|---|
| **증상** | `import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;` 컴파일 에러: package does not exist |
| **원인** | Spring Boot 4.0에서 슬라이스 테스트 어노테이션들이 모듈 단위로 재배치됨. WebMvc 슬라이스는 `boot-webmvc-test` 모듈로 이동 |
| **해결** | `import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;` |
| **시점** | PR #16 (PostControllerTest 도입) |
| **교훈** | Spring Boot 4.x로 올릴 때 슬라이스 테스트 어노테이션은 IDE 자동 임포트가 옛 경로를 잡을 수 있으니 첫 컴파일 에러부터 의심 |

<a id="t-002"></a>
### T-002 · `@WebMvcTest`에서 `ObjectMapper` 빈 주입 실패

| | |
|---|---|
| **증상** | `@Autowired private ObjectMapper objectMapper;` → `NoSuchBeanDefinitionException: No qualifying bean of type 'ObjectMapper'` |
| **원인** | Spring Boot 4.0의 `@WebMvcTest`는 Jackson auto-config을 자동 포함하지 않게 변경. 필요한 만큼만 로드 |
| **해결** | 테스트 내부에서 직접 인스턴스화: `private final ObjectMapper objectMapper = new ObjectMapper();` |
| **시점** | PR #16 |
| **교훈** | 슬라이스 테스트 = "더 가벼움" 트레이드오프. 필요한 빈은 직접 생성하거나 `@Import` 추가 |

<a id="t-003"></a>
### T-003 · `@WebMvcTest`가 JPA를 끌고 들어가 "JPA metamodel must not be empty"

| | |
|---|---|
| **증상** | `@WebMvcTest(PostController.class)` 컨텍스트 로딩 시 `IllegalArgumentException: JPA metamodel must not be empty.` |
| **원인** | `MoodiaryApplication.java`에 `@EnableJpaAuditing`이 직접 달려있어, `@SpringBootApplication`이 발견될 때 JPA 인프라(`jpaMappingContext` 등)도 같이 끌려 들어옴. 슬라이스 테스트는 JPA 엔티티를 로드하지 않으므로 metamodel이 비어서 터짐 |
| **해결** | `@EnableJpaAuditing`을 별도 `@Configuration` 클래스(`JpaAuditingConfig`)로 분리. `MoodiaryApplication`에는 `@SpringBootApplication`만 남김. 슬라이스 테스트는 이 설정을 의도적으로 임포트하지 않음 |
| **시점** | PR #16 |
| **교훈** | 메인 애플리케이션 클래스에 `@EnableXxx` 어노테이션을 직접 다는 건 편하지만 슬라이스 테스트와 충돌한다. 기능별 `@Configuration` 분리가 정석 |

---

## 🔐 Spring Security + 슬라이스 테스트

<a id="t-004"></a>
### T-004 · `@WithMockUser`가 `@Nested` 클래스에 자동 적용되지 않음

| | |
|---|---|
| **증상** | 클래스 단위 `@WithMockUser` 또는 `@Nested` 클래스에 `@WithMockUser`를 붙여도 요청이 401. `@WithMockUser`를 테스트 메서드에 직접 달면 동작 |
| **원인** | JUnit 5의 `@Nested`는 별도 테스트 클래스로 취급되어, Spring Security의 `WithSecurityContextTestExecutionListener`가 outer 클래스의 어노테이션을 자동 합성(merge)하지 않음. 메서드 레벨에만 안정적으로 동작 |
| **해결** | 어노테이션 의존을 버리고 **request post-processor**로 변경: `mockMvc.perform(get("/post").with(user("test")))`. `SecurityMockMvcRequestPostProcessors.user(...)` 정적 임포트 |
| **시점** | PR 2-b (#21) |
| **교훈** | `@Nested`와 Spring 테스트 어노테이션은 종종 부딪힌다. 의도가 명시적이고 요청 단위인 post-processor가 더 안전. `@WithMockUser`는 단일 클래스의 메서드 레벨에서만 신뢰 |

<a id="t-005"></a>
### T-005 · Spring Security 의존성 추가만으로 모든 엔드포인트 401

| | |
|---|---|
| **증상** | `spring-boot-starter-security`를 의존성에 추가한 순간, 운영 Post API와 슬라이스 테스트 모두 401 시작 |
| **원인** | Spring Security 6.x의 기본 자동설정은 "모든 엔드포인트 인증 필요" + Basic Auth + 생성된 임시 비번. SecurityFilterChain 빈을 정의하지 않으면 이 기본값이 적용 |
| **해결** | `@Configuration`에 `SecurityFilterChain` 빈을 명시적으로 정의. 인증 도입 전이라면 의도적 `permitAll()`로 시작. CSRF/formLogin/httpBasic은 REST API에선 명시적 `disable()`. 슬라이스 테스트는 `@Import(SecurityConfig.class)` 필수 (그렇지 않으면 기본 자동설정만 적용) |
| **시점** | PR 2-a (#20) |
| **교훈** | Spring Security는 "의존성에 넣자마자 보안 적용". 안전한 기본값이지만, 마이그레이션 중에는 SecurityFilterChain을 먼저 명시하지 않으면 모든 게 무너진다. 자동설정에 의존하지 말고 항상 빈을 정의 |

---

<a id="t-012"></a>
### T-012 · SecurityConfig가 의존하는 빈이 늘면, 그걸 `@Import`하는 모든 슬라이스 테스트가 깨짐

| | |
|---|---|
| **증상** | PR 2-c에서 SecurityConfig에 `JwtAuthenticationFilter` 빈 + `JwtTokenProvider` 파라미터를 추가했더니, `@Import(SecurityConfig.class)`로 SecurityConfig를 끌고 쓰던 모든 슬라이스 테스트가 `UnsatisfiedDependencyException: No qualifying bean of type 'JwtTokenProvider'`로 깨짐 |
| **원인** | `@WebMvcTest`는 컴포넌트 스캔을 최소화하므로 `@Component`인 `JwtTokenProvider`가 자동 등록되지 않는다. `@Import`로 끌고 온 SecurityConfig는 그걸 필요로 하니까 충돌 |
| **해결** | SecurityConfig를 임포트하는 모든 컨트롤러 슬라이스 테스트에 `@MockitoBean private JwtTokenProvider jwtTokenProvider;` 추가. 컨트롤러 자체는 이 빈을 안 쓰지만, 컨텍스트 그래프 완성에 필요 |
| **시점** | PR 2-c |
| **교훈** | 보안 설정은 슬라이스 테스트의 "의존성 그래프 부담"이라는 비용이 따라온다. 새 의존 빈을 SecurityConfig에 추가할 때마다 `@Import(SecurityConfig.class)` 쓰는 테스트 전수 점검 필요. 대안: 테스트 전용 `TestSecurityConfig`를 따로 두고 임포트하는 방식 |

---

## 📦 빌드/배포 함정

<a id="t-013"></a>
### T-013 · `application.yaml` 이 `.gitignore` 되어 운영에 설정이 누락

| | |
|---|---|
| **증상** | PR 2-c에서 `application.yaml` 에 추가한 `jwt.expiration-ms`, `jwt.secret` 설정이 dev 머지 후 사라진 것처럼 보임. 운영 컨테이너에서 `JwtProperties` 가 `expirationMs = 0` 으로 채워져 발급한 JWT 가 즉시 만료 |
| **원인** | `.gitignore` 에 `/src/main/resources/application.yaml` 한 줄이 있었음. 개인 RDS 비밀번호 보호 의도였던 듯한데, 그 결과 **로컬 변경이 git 에 절대 올라가지 않음**. 운영 image 의 JAR 에는 application.yaml 자체가 없거나 옛 버전 |
| **해결** | <br>① `.gitignore` 에서 application.yaml 줄 제거 <br>② 대신 `application-local.yaml` 만 gitignore (개인 override 용 — Spring 이 자동 merge) <br>③ 커밋된 application.yaml 의 모든 비밀값은 `${ENV_VAR:기본값}` 형식으로 — 운영은 env var override, 로컬은 기본값으로 동작 |
| **시점** | PR 3 작업 중 발견 |
| **교훈** | 설정 파일을 통째로 gitignore 하는 건 거의 항상 잘못된 선택. 비밀값만 env var 로 빼고 파일 자체는 추적. "기본값 + 환경별 override" 패턴이 표준. <br><br>**일반화**: 운영에 영향을 주는 파일을 gitignore 하려는 충동이 들면, 그 파일의 어떤 *값*이 비밀인지부터 분리 가능한지 먼저 의심하자 |

---

<a id="t-014"></a>
### T-014 · CD 워크플로우에 시크릿 주입 라인 누락 → yaml 기본값이 운영에 노출

| | |
|---|---|
| **증상** | release PR(#27) 직전 점검 중 발견: CD 워크플로우의 `docker run` 에 `-e JWT_SECRET=...` 가 빠져 있었음. 그대로 main 머지했다면 컨테이너가 yaml 기본값 (`dev-local-secret-...`) 으로 부팅 → 누구나 토큰 위조 가능 |
| **원인** | PR 2-c (#23) 에서 한 번 추가했었는데 어느 시점 머지 과정에서 빠져 있었음. CD 가 `docker run -e ...` 패턴이라 시크릿 추가할 때마다 워크플로우 yaml 을 손으로 고쳐야 하고, 그 라인이 누락돼도 빌드 자체는 통과 |
| **해결** | <br>① 즉시: `-e JWT_SECRET=${{ secrets.JWT_SECRET }} \` 한 줄 추가 PR (#26) → release 전에 합류<br>② 구조적: **PR 1 (CD를 docker compose 호출로 통일)** 이 본질적 해결. compose 가 `.env` 자동 로드하면 시크릿 추가 = `.env` 한 줄 추가로 끝 |
| **시점** | PR #27 release 직전 |
| **교훈** | "필수 환경변수"는 다음 두 가지 방어선이 있어야 함:<br>1. **시작 시 검증** — 코드에서 \"secret이 default 값이면 boot 실패하게\" 만들기 (소소한 fail-fast)<br>2. **CD 가 의도적으로 환경변수 누락하면 알려주기** — env var → \`.env\` 단일 소스 전환 (PR 1) |

<a id="t-015"></a>
### T-015 · Claude 가 머지된 PR 을 열린 것으로 착각 → 닫힌 브랜치에 작업 계속

| | |
|---|---|
| **증상** | 사용자가 GitHub UI 에서 PR 을 머지한 시점과 Claude 가 그걸 인지하는 시점 사이에 갭. Claude 는 자기가 마지막으로 본 PR 상태(open)를 사실로 간주하고, 닫힌/머지된 PR 을 \"작업 중\"으로 참조하거나, 머지된 feature branch 로 새 작업을 시작 |
| **원인** | Claude 의 상태 추적은 마지막 tool call 기준. 사용자가 외부 채널(GitHub Web)로 상태를 바꿔도 Claude 는 모름. 추측에 의존 |
| **해결** | CLAUDE.md 에 **Workflow rules** 섹션 신설 (PR #25). 의무 규칙:<br>1. PR 만들기/언급하기 전에 항상 `gh pr list --state all --limit 10` 으로 상태 확인<br>2. 새 작업 시작 전 `git fetch origin && git checkout dev && git pull origin dev`<br>3. 운영 영향 변경은 PR body 에 "운영 머지 전 필수" 체크리스트 명시 |
| **시점** | 이번 세션 내내 누적되다가 명문화 |
| **교훈** | LLM 은 외부 상태 변화를 자동 감지하지 못한다 — 추측하지 말고 항상 \"확인 명령\"을 한 번 더. 사람한테는 당연하지만 Claude 한테는 명문화 안 하면 반복된다 |

---

## ☁️ AWS / 운영 인프라

<a id="t-006"></a>
### T-006 · EC2 stop/start 시 퍼블릭 IP가 매번 바뀜

| | |
|---|---|
| **증상** | EC2를 비용 절감 차원에서 stop/start 하면 퍼블릭 IPv4가 바뀜. GitHub Secret의 `EC2_HOST`를 매번 갱신해야 CD가 동작 |
| **원인** | EC2 기본 동작 — stop 시 동적으로 할당된 퍼블릭 IP가 풀로 반환 |
| **해결** | **Elastic IP 할당** + 인스턴스 연결. 인스턴스 stop/start 해도 IP 고정. (인스턴스에 부착되어 있는 동안에는 무료, 분리되어 놀고 있으면 시간당 과금) |
| **시점** | 이번 세션 |
| **교훈** | 운영용 인스턴스라면 무조건 Elastic IP. 월 5,000원 미만이고 운영 안정성과 트러블슈팅 시간을 사면 인건비가 훨씬 비쌈 |

<a id="t-007"></a>
### T-007 · `docker-compose exec` 서비스명을 컨테이너명으로 착각

| | |
|---|---|
| **증상** | `docker-compose exec mysql-hoseo bash` → `ERROR: No such service: mysql-hoseo`. 그런데 `docker ps`에는 `mysql-hoseo`가 보임 |
| **원인** | `compose.yaml`의 서비스명(예: `mysql-moodiary`)과 `container_name` 옵션으로 지정한 컨테이너명(`mysql-hoseo`)이 다름. `docker-compose exec`는 서비스명만 받음 |
| **해결** | 둘 중 하나: <br>① `docker-compose exec <서비스명> bash` (compose.yaml의 키 이름) <br>② `docker exec -it <컨테이너명> bash` (compose 안 거치고 docker 직접) |
| **시점** | 이번 세션 |
| **교훈** | `docker-compose` 명령은 서비스명, `docker` 명령은 컨테이너명. 헷갈리면 `docker-compose ps`로 매핑 확인 |

---

## 🗄️ DB

<a id="t-008"></a>
### T-008 · `ddl-auto: validate`인데 운영 RDS에 테이블이 없어서 500

| | |
|---|---|
| **증상** | 첫 배포 후 `POST /post` → 500. 로그: `Table 'moodiary.post' doesn't exist` |
| **원인** | 로컬은 `ddl-auto: create-drop`이라 테이블이 자동 생성됨. 운영 RDS는 운영 안전을 위해 `validate`로 두었는데, 정작 RDS에는 테이블이 없는 상태에서 첫 배포가 나감 |
| **해결** | <br>① 임시: RDS에 수동 DDL로 테이블 생성<br>② 임시: `ddl-auto: update`로 한 번 띄워 자동 생성 후 다시 `validate`<br>③ **본질적**: Flyway 도입 (PR 6 예정) — `db/migration/V1__init.sql` 등 마이그레이션 파일로 관리 |
| **시점** | 첫 운영 배포 시 |
| **교훈** | 로컬과 운영의 `ddl-auto` 정책이 다를 때 첫 배포 함정에 빠진다. Flyway 도입 전까지는 새 엔티티 추가 시 RDS DDL 수동 실행이 필수 — PR 본문에 DDL 명시하는 습관 |

<a id="t-009"></a>
### T-009 · MySQL `USER`는 예약어 — `@Table(name="users")` 강제

| | |
|---|---|
| **증상** | `User` 엔티티의 테이블명이 기본 `user`가 되면 일부 MySQL 버전/모드에서 SQL 문법 충돌 가능. 특히 `SELECT * FROM user WHERE ...` 같은 쿼리가 모호해질 수 있음 |
| **원인** | MySQL에서 `USER`는 예약어 (현재 사용자 반환 함수 `USER()`) |
| **해결** | `@Table(name = "users")` 명시. 다른 RDB 마이그레이션 시에도 안전 |
| **시점** | PR 2-a (#20) — 사전 회피 |
| **교훈** | DB 예약어 충돌은 빈번하지 않지만 한 번 터지면 디버깅이 어렵다. 엔티티명이 일반 단어면 복수형 테이블명을 고려 (`user` → `users`, `order` → `orders` 등) |

---

## 🖥️ CLI / 인코딩

<a id="t-010"></a>
### T-010 · `mysql -p <비번>` 사이 공백 → 비번이 DB명으로 해석

| | |
|---|---|
| **증상** | `mysql -h X -u Y -p mypassword moodiary` → 비번 프롬프트가 뜨지 않고 mysql 사용법 도움말이 길게 출력 |
| **원인** | `-p`와 비번 사이에 공백이 들어가면 `mypassword`가 DB명으로 해석. 그러면 인자 파싱이 깨져서 mysql이 사용법 페이지를 토함 |
| **해결** | 셋 중 하나: <br>① `-p` 단독 → 프롬프트로 입력 (보안 권장)<br>② `MYSQL_PWD="$RDS_PASSWORD" mysql -h X -u Y moodiary` (환경변수)<br>③ `mysql -h X -u Y -pmypassword moodiary` (공백 없이 붙임 — `ps`에 노출됨, 로컬 한정) |
| **시점** | 이번 세션 — RDS 접속 시 |
| **교훈** | mysql CLI는 `-p`만 묘하게 공백 허용을 안 함. 환경변수 방식이 깔끔하고 보안상 안전 |

<a id="t-011"></a>
### T-011 · git-bash에서 curl로 한글 POST 시 400

| | |
|---|---|
| **증상** | git-bash에서 `curl -X POST .../post -d '{"title":"한글","content":"내용"}'` → 400 BadRequest |
| **원인** | git-bash의 기본 인코딩 + curl이 한글 바이트를 UTF-8로 정상 전송하지 못함. 서버는 깨진 JSON으로 받아 파싱 실패 |
| **해결** | <br>① CLI 테스트는 ASCII로 (`title=hello`)<br>② Postman/Swagger UI/InsomniaREST 같은 GUI 도구 사용 (UTF-8 보장)<br>③ 그래도 git-bash로 보내야 한다면 `--data-binary @file.json` + UTF-8 인코딩된 파일 |
| **시점** | 이번 세션 — Post 생성 검증 시 |
| **교훈** | CLI 테스트는 디버깅용 sanity check일 뿐, 실 클라이언트는 FE/Postman/Swagger. CLI에서 한글이 안 나간다고 서버 코드를 고치지 말 것 |

---

## 📝 새 항목 추가 가이드

1. 인덱스에 한 줄: `[T-XXX](#t-xxx) 한 줄 요약`
2. 해당 섹션에 표 추가:
   ```markdown
   <a id="t-xxx"></a>
   ### T-XXX · 제목

   | | |
   |---|---|
   | **증상** | 화면/로그에서 본 것 |
   | **원인** | 왜 그랬는지 |
   | **해결** | 어떻게 풀었는지 (코드/명령) |
   | **시점** | 어떤 PR/세션 |
   | **교훈** | 다음에 안 막히려면 |
   ```
3. 카테고리가 새로우면 섹션 헤더(`##`) 추가
4. 마지막 갱신 일자 갱신

> "한 번 막힌 건 다시 막힌다. 두 번 막히면 비용이 두 배가 된다." — 이 문서의 존재 이유.
