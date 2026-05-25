# Moodiary Backend — 트러블슈팅 로그

> 작업 중 막혔던 지점, 원인, 해결법을 한 파일에 누적.
>
> **갱신 규칙 (CLAUDE.md Workflow rules 와 연동)**:
> - 모든 PR 의 마지막에서 두 번째 task = **"troubleshooting.md 점검"** (PR body 작성 전).
> - 그 PR 진행 중 1분 이상 디버깅한 모든 이슈를 후보로 둔다.
> - 프로젝트 고유 / 다음 사람도 만날 함정이면 → 새 T-### 항목 추가 (인덱스 + 본문).
> - 단순 오타 / 개인 IDE 문제는 skip.
> - 항목 schema: **증상 / 원인 / 해결 / 시점 / 교훈**.
>
> 마지막 갱신: 2026-05-25 (CLAUDE.md 압축 + sweep rule 명문화 + T-018 (T-015 재발))

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
- [T-016](#t-016) `compileOnly` QueryDSL — 첫 사용 PR 에서 `NoClassDefFoundError`
- [T-017](#t-017) `MissingServletRequestParameterException` 미매핑 — 필수 파라미터 누락 시 500
- [T-018](#t-018) **T-015 재발** — 머지된 PR 브랜치에 추가 commit 푸시 → dead branch (Workflow rule 적용 누락)

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

<a id="t-016"></a>
### T-016 · `compileOnly` QueryDSL — 첫 사용 PR 에서 `NoClassDefFoundError`

| | |
|---|---|
| **증상** | PR 5 (Calendar API) 에서 `JPAQueryFactory` 를 처음 실제로 사용하니 테스트에서 `java.lang.NoClassDefFoundError: com/querydsl/core/types/EntityPath` 폭발. CalendarServiceTest 9 case + cascade 로 기존 PostServiceTest / UserServiceTest 까지 `UnfinishedMockingSessionException` 으로 같이 무너짐 |
| **원인** | `build.gradle` 에서 `compileOnly 'com.querydsl:querydsl-jpa:5.0.0:jakarta'` — 컴파일은 통과하지만 **운영 jar 와 test runtime 클래스패스에는 querydsl 클래스가 없다**. Mockito 가 `@Mock CalendarRepository` 를 mock 만들 때 클래스를 ByteBuddy 로 로드하다가 super class init 에서 터짐. 이전엔 QueryDSL 을 실제로 import 한 코드가 한 줄도 없어서 잠재된 결함이 안 드러났을 뿐 |
| **해결** | `compileOnly` → `implementation` 으로 전환. CLAUDE.md 의 \"trap\" 주석도 같이 갱신해 다음 사람이 무심코 되돌리지 않게 함 |
| **시점** | PR 5 |
| **교훈** | 1) `compileOnly` 의존성은 "컴파일은 되지만 실제로 안 쓰는 라이브러리" 한정. 한 줄이라도 실제로 import 해서 런타임 객체를 만든다면 `implementation` 이 맞다.<br>2) 한 테스트 클래스의 `@Mock` 클래스 로딩 실패가 같은 JVM 의 다른 테스트 클래스까지 `UnfinishedMockingSession` 으로 전파시킨다 — 처음 보면 광범위 회귀 같지만 실은 단일 클래스가 깨진 거. 첫 NoClassDefFoundError 부터 추적해야 빨리 찾는다.<br>3) "함부로 만지지 말라" 라고 적힌 trap 도 진짜 막혔을 땐 만져야 한다 — 단 변경 사유와 재검증 절차를 trap 주석에 같이 갱신해야 다음 사람 / 다음 Claude 가 헷갈리지 않는다 |

<a id="t-017"></a>
### T-017 · `MissingServletRequestParameterException` 미매핑 — 필수 파라미터 누락 시 500

| | |
|---|---|
| **증상** | `GET /calendar` 에 `year` 파라미터 없이 호출 → `{"message": "서버 오류가 발생했습니다."}` + HTTP 500. 사용자 쪽 입력 오류인데 서버 버그처럼 보고됨 |
| **원인** | `GlobalExceptionHandler` 에 `MissingServletRequestParameterException` 핸들러가 없었음. fallback `@ExceptionHandler(Exception.class)` 가 잡아서 500 으로 떨어뜨렸다. 이전에는 모든 컨트롤러가 `@RequestBody` 만 쓰고 `@RequestParam` 으로 필수 쿼리 파라미터를 받는 곳이 없어서 잠재된 결함이 안 드러났을 뿐 — PR 5 의 `GET /calendar?year=&month=` 가 첫 케이스 |
| **해결** | `@ExceptionHandler(MissingServletRequestParameterException.class)` 추가, `ResponseEntity.badRequest()` + `"필수 파라미터가 누락되었습니다: " + e.getParameterName()` 메시지 반환 (400). `api-contracts.md` 공통 규약에도 매핑 명시 |
| **시점** | PR 5 |
| **교훈** | 1) 새 컨트롤러 패턴(여기선 `@RequestParam` 필수)을 처음 도입할 때마다 `GlobalExceptionHandler` 의 catch-all `Exception` 핸들러가 가리고 있는 4xx 케이스가 있는지 점검해야 한다. 500 으로 떨어지는 클라이언트 오류는 디버깅 시간을 잡아먹는다.<br>2) 비슷한 후보들 — `MethodArgumentTypeMismatchException` (year=abc 같은 타입 오류), `ConstraintViolationException` (`@Validated` 적용 시), `HttpRequestMethodNotSupportedException` (잘못된 메서드). 다음에 필요해지면 같이 추가.<br>3) **테스트로 잡힌다** — Controller 테스트에 "파라미터 누락 → 400" 케이스 한 줄이 있으면 운영 전에 잡힘. 새 컨트롤러 추가할 때 입력 검증 테스트는 반드시 포함 |

<a id="t-018"></a>
### T-018 · **T-015 재발** — 머지된 PR 브랜치에 추가 commit 푸시 → dead branch

| | |
|---|---|
| **증상** | PR #31(Calendar API)이 사용자에 의해 머지된 직후, Claude 가 같은 브랜치(`feat/calendar-api`)에 2 commit(`ce1f299` sweep rule 명문화, `d5f04cd` CLAUDE.md 압축)을 추가 push. 머지된 PR 에는 자동 반영되지 않으므로 두 변경은 origin 의 dead branch 에 고립 — `dev` 에는 들어가지 않음. 사용자가 발견하기 전까지 "반영됐다"는 잘못된 보고 |
| **원인** | T-015 로 이미 기록되고 CLAUDE.md Workflow rules 에 "Before creating a PR — always check PR state first" 로 명문화되어 있었지만, 그 규칙이 **"PR 생성 직전"** 으로만 좁게 적용되고 있었다. PR 이 열려있다고 "확인한 시점" 이후의 추가 push 직전에는 다시 확인하지 않음 → PR 이 그 사이에 머지되었을 가능성을 무시 |
| **해결** | <br>① 누락 commit 을 새 PR(`docs/claude-md-compress-and-sweep-rule`)로 부활 — dev 동기화 → 새 브랜치 → 최종 결과물 한 번에 작성 → push → PR. <br>② Workflow rule 범위 확장: "Before creating a PR" 만이 아니라 **"OR before pushing more commits to an existing branch"** 도 동일 의무. CLAUDE.md 의 규칙 헤더 자체를 다시 작성해서 "추가 push" 도 명시적 트리거로 포함 |
| **시점** | PR #31 머지 직후 / 이 PR 에서 복구 |
| **교훈** | 1) 규칙을 명문화했다고 끝이 아니다. **규칙의 적용 시점 범위**가 좁게 박혀버리면 같은 실수가 다른 모양으로 재발한다. T-015 는 "PR 생성 전" 만 잡았지 "추가 push 전" 은 못 잡았다.<br>2) Claude 의 상태 모델은 마지막 tool call 기준이고 외부 머지를 자동 감지하지 못한다 — push 명령은 **항상** 다음으로 시작: `gh pr list --state all --limit 10`. push 자체를 멱등하지 않은 작업으로 취급해야 한다.<br>3) 같은 실수의 재발은 단순 부주의가 아니라 **규칙의 구멍**을 가리킨다. 재발 케이스를 별도 T-### 로 기록해서 다음 Claude 가 "T-015 만 봤어요" 로 끝나지 않게 한다 |

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
