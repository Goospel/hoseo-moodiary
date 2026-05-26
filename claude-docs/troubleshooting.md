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
> 마지막 갱신: 2026-05-26 (T-023 추가 — `docker compose` 스페이스 vs 하이픈 + 옛 CD 들의 silent fail 진실)

---

## 🚨 이번 대사건 (PR #38 → #42, 2026-05-26) — 한 운영 검증에서 5층 결함이 동시 노출

CORS 운영 검증 (PR #38) 직후 컨테이너 restart loop → 표면은 "CORS preflight 401" 이었지만 실제로는 **5개의 독립적 결함이 동시에 들켜난 다층 사건**. 다음 사람/Claude 가 비슷한 증상에 부딪히면 아래 4개 항목을 순서대로 읽는다:

| 층 | 결함 | 항목 |
|---|---|---|
| 1 (메타) | 머지된 PR 의 상태를 추적 못 함 → 머지된 PR 본문을 사후 수정 / dead branch 작업 | [T-020](#t-020), [T-018](#t-018), [T-015](#t-015) |
| 2 (인프라) | SSM agent 죽음 + CD `send-command` async 응답만 보고 "성공" 처리 → 여러 PR 분 변경이 운영에 도달 못 함 | [T-019](#t-019) |
| 3 (코드) | 운영 첫 부팅에서 잠재 결함 동시 폭발 — springdoc 2.8.3 ↔ Spring Boot 4 비호환 + Post→User FK orphan 데이터 | [T-019](#t-019) |
| 4 (테스트) | `@SpringBootTest` 가 `@Disabled` 라 운영 부팅 폭발이 빌드 단계에서 안 잡힘 | [T-019](#t-019) |
| 5 (배포 인지) | dev 머지만으로 운영 도달한다고 가정 / `docker compose pull` 만 돌리면 새 image 가 받아져 온다고 가정 | [T-021](#t-021), [T-022](#t-022) |
| 6 (CD 명령) | 워크플로우의 `docker compose` (스페이스) 가 EC2 에서 unknown — 옛 CD 들은 silent fail 로 가려져 있었음 → PR #45 의 wait/verify 가 비로소 노출 | [T-023](#t-023) |

**핵심 진단 휴리스틱** — 운영이 이상하면 표면 증상부터 보지 말고:
1. `docker ps` — 컨테이너 살아있나?
2. `docker logs --tail 100` — restart loop 이면 부팅 stack trace
3. `docker inspect ... --format '{{.Created}}'` 시각 vs 마지막 main 머지 시각 비교 — **새 image 가 진짜 들어왔는지의 가장 빠른 판별**

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
- [T-019](#t-019) SSM agent 죽음 + CD `send-command` async 응답만 보고 "성공" 처리 → 여러 PR 분의 변경이 운영에 도달 못 한 채 누적 → 첫 수동 deploy 에서 두 가지 잠재 결함 (springdoc/Spring Boot 4 호환성 + Post→User FK 마이그레이션) 동시 폭발 → 컨테이너 restart loop
- [T-020](#t-020) **T-018 재발 (3번째)** — 머지된 PR 의 본문을 사후 수정 / Workflow 규칙의 트리거 범위가 `gh pr edit` 같은 메타데이터 변경을 안 다뤘던 회색 지대
- [T-021](#t-021) **dev 머지 ≠ 운영 도달** — CD 트리거가 `main` push 인데 dev 까지만 머지하고 운영 검증 시도 → Docker Hub `latest` 가 여전히 옛 image
- [T-022](#t-022) **`docker compose pull` ≠ 새 image 보장** — Docker Hub `latest` 가 갱신되지 않으면 EC2 에서 pull 해도 동일 sha. 운영 deploy 가 진짜 일어났는지의 판별은 `docker inspect .Created` 시각
- [T-023](#t-023) **`docker compose` (스페이스) vs `docker-compose` (하이픈)** — EC2 에는 standalone binary 만 등록되어 `docker compose` 가 unknown sub-command 로 즉시 fail. 옛 CD 들은 silent fail 로 가려져 있었고, PR #45 의 wait/verify 가 처음으로 이를 노출

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

<a id="t-019"></a>
### T-019 · SSM agent 죽음 + CD silent fail + 누적 배포 폭발 + ApplicationContext 안전망 부재

| | |
|---|---|
| **증상** | PR 8 (CORS) 운영 검증에서 preflight 가 401 → 그 후 manual `docker-compose pull && up -d` 로 새 image 띄웠더니 컨테이너 `Up 9 seconds` 후 죽고 다시 시작하는 **restart loop**. `curl /swagger-ui/index.html` → `Connection reset by peer`, `curl -X OPTIONS /post` → `Empty reply from server`. CORS 가 망가진 줄 알았는데 실은 컨테이너 자체가 안 떠 있던 상태. |
| **원인** | **4-5층이 동시에 무너졌다.** (배포 인지 갭 [T-021](#t-021) / [T-022](#t-022) 까지 합치면 5층 — 별도 항목으로 분리.) 표면 한 가지 (CORS preflight 401) 가 모든 층을 가렸다.<br><br>**① SSM agent 가 EC2 에서 죽어 있었음** — `systemctl status amazon-ssm-agent` 가 dead 였고 journal 도 비어 있었음. 언제 죽었는지 모름.<br><br>**② CD 워크플로우가 silent fail** — `.github/workflows/cd-*.yaml` 의 `aws ssm send-command` 는 비동기. command 가 SSM 큐에 enqueue 되면 즉시 "Pending" 응답이 오고 워크플로우는 그걸 "성공" 으로 종료. 실제로 EC2 에서 `docker pull` 이 일어났는지는 확인하지 않는다. SSM agent 가 죽었으면 큐에 쌓이기만 하고 EC2 는 영원히 새 image 를 안 받는다. GitHub Actions 의 초록 체크는 **거짓 신호**였다.<br><br>**③ 그 사이 머지된 PR 들이 한꺼번에 폭발** — SSM 이 죽고 나서 머지된 PR #24 (Post→User), #31 (Calendar/QueryDSL), #38 (CORS) 은 운영에 한 번도 도달하지 못함. user 가 수동으로 `docker-compose pull` 하는 순간 **누적된 모든 변경이 첫 실배포로 동시에 들어감** → 잠재 결함 두 개가 같이 터짐:<br>&nbsp;&nbsp;a) **springdoc-openapi 2.8.3 ↔ Spring Boot 4 비호환** — `springdoc-openapi-starter-webmvc-ui:2.8.3` 의 `QuerydslPredicateOperationCustomizer` 가 `org.springframework.data.util.TypeInformation` 클래스를 참조하는데, Spring Boot 4 / Spring Data Commons 4.x 에서 위치가 변경되어 `NoClassDefFoundError`. QueryDSL 이 classpath 에 있어야 트리거되므로 PR #31 까지는 `compileOnly` 라 안 터졌고, PR #31 의 `implementation` 전환부터 잠재돼 있었지만 SSM 죽음 때문에 운영 미반영 → 노출 지연.<br>&nbsp;&nbsp;b) **Post → User FK 마이그레이션 미적용** — PR #24 머지 후 운영 RDS 가 한 번도 새 Hibernate DDL 을 실행해본 적 없음. `post` 테이블에 user 없던 시절의 row 가 남아 `ALTER TABLE post ADD FOREIGN KEY user_id REFERENCES users(user_id)` 가 거부됨.<br><br>**④ ApplicationContext 안전망 부재** — `MoodiaryApplicationTests` 가 `@Disabled` 였다. 빌드/단위 테스트에서 full ApplicationContext 를 한 번도 안 띄움 → 운영 첫 부팅에서만 (a) 가 폭발. 슬라이스 테스트 (`@WebMvcTest`, `@DataJpaTest`) 는 springdoc/QueryDSL 자동 구성 빈을 건드리지 않으므로 절대 못 잡는다. |
| **해결** | **시도 → 평가:**<br><br>**(O) 옳았던 것**:<br>① `docker logs hoseo-moodiary --tail 100` 으로 실제 stack trace 확인 — "CORS 401" 표면 증상에 머물지 않고 컨테이너 부팅 로그까지 내려간 게 결정적.<br>② 운영 버그를 단위 테스트로 재현 — `MoodiaryApplicationTests` 의 `@Disabled` 제거 + H2 testRuntime 추가 → 운영과 동일한 `BeanCreationException` 을 빌드 단계에서 재현 (TDD RED).<br>③ springdoc-openapi `2.8.3 → 3.0.3` 으로 업 — 3.0.x 가 Spring Boot 4.x 호환 라인. RED 였던 ApplicationContext 테스트가 GREEN.<br>④ RDS orphan row 클린업은 runbook 으로 분리 (`claude-docs/ops-runbooks/post-orphan-cleanup-2026-05-26.md`) — 코드 변경과 데이터 변경을 섞지 않음.<br><br>**(X) 옳지 않았거나 잘못 짚었던 것**:<br>① **초기 진단을 CORS 설정 오류로 좁힘** — `preflight=401` 결과만 보고 `Access-Control-Request-Method` 헤더 누락 / origin 매칭 실패 등을 의심. 실제로는 컨테이너가 죽어 있어서 어떤 요청이든 비정상이었음. **컨테이너 health (`docker ps`, `docker logs`) 를 먼저 확인했어야 함.**<br>② **이전 운영 검증 결과 (`swagger=200`, `preflight=401`) 를 "현재 컨테이너 상태"의 증거로 사용** — 그 검증은 restart loop 중 컨테이너가 잠깐 살아있던 짧은 창에서 운 좋게 잡힌 응답이었다. **부팅이 unstable 한 컨테이너의 단일 시점 curl 응답은 신뢰할 수 없다.** 검증 스크립트는 health probe 로 5–10초 간격으로 여러 번 쳤어야.<br>③ **이미지 digest 비교를 "deploy 성공" 의 증거로 사용** — `docker inspect ... .Image` 가 새 sha 였으니 "deploy 됐다" 고 결론. 그러나 image 가 바뀌었다고 **그 안의 코드가 부팅에 성공한다는 보장은 없다**. digest 는 빌드 단계 검증일 뿐, 부팅 검증은 별도.<br>④ **롤백 옵션을 진지하게 고려하지 않음** — fix forward 가 합리적이긴 했지만 검토 시점에 Docker Hub 에 이전 안정 image 태그가 없다는 사실 (only `latest`) 을 확인하고 나서야 fix forward 가 사실상 강제됐음을 인지. 옛 working image 의 태그 보관 정책이 없는 게 사고 대응 옵션을 줄였다. |
| **시점** | PR #38 (CORS) 머지 후 운영 검증 — 2026-05-26 |
| **교훈** | **1. SSM agent 의 silent death 는 GitHub Actions 의 초록 체크로 가려진다 — CD 신뢰성 자체가 별도 검증 대상**<br>`aws ssm send-command` 는 enqueue 응답만 받는다. CD 가 "성공" 으로 끝나도 EC2 에서 실제로 명령이 실행됐는지 모른다. 다음 PR (별도 인프라 PR) 에서 다음을 추가해야 한다:<br>&nbsp;&nbsp;- `aws ssm send-command --output text --query "Command.CommandId"` 로 command-id 받기<br>&nbsp;&nbsp;- `aws ssm wait command-executed --command-id <id> --instance-id <ec2>` 로 실행 완료까지 대기<br>&nbsp;&nbsp;- 그 다음 `aws ssm get-command-invocation` 로 exit code 확인<br>&nbsp;&nbsp;- 또는 health probe (`curl http://EC2:8080/actuator/health`) 를 명시적 step 으로<br><br>**2. `@SpringBootTest` 가 `@Disabled` 면 슬라이스 테스트만으로는 운영 부팅 폭발을 절대 못 잡는다**<br>springdoc / 자동 구성 빈 / `JpaAuditing` 활성화 / 모든 `@Component` 스캔 — 이런 자동 구성은 full ApplicationContext 부팅에서만 검증된다. `@WebMvcTest`, `@DataJpaTest` 는 의도적으로 더 작은 그래프만 띄우므로 안전망이 아니다. **`@Disabled` 된 `@SpringBootTest` 는 안전망이 아니라 안전망의 모양만 흉내내는 위장**. 다시 disable 하지 말 것 (해당 테스트의 javadoc 에도 명시).<br><br>**3. 라이브러리 메이저 버전 호환성은 BOM 만으로 보장되지 않는다**<br>Spring Boot 4 로 메이저 업했을 때 build.gradle 에서 명시 버전이 박혀있는 의존성은 BOM 의 트랜지티브 관리를 받지 않는다. springdoc 처럼 명시 버전 박힌 라이브러리는 메이저 업 시 별도로 호환 버전을 확인해야 한다. (`build.gradle` 의 모든 명시 버전 의존성에 "Spring Boot 4 compat?" 코멘트 박는 것도 한 방법.)<br><br>**4. "표면 증상" 으로 좁히지 말고 "부팅 상태" 부터 검증한다**<br>운영 API 가 예상 외 응답을 내면 (CORS 401, 500, timeout 등) **CORS 설정/엔드포인트 구현을 의심하기 전에 컨테이너가 부팅에 성공했는지부터 확인.** 디버깅 첫 명령은 `docker ps` + `docker logs --tail 100`.<br><br>**5. ddl-auto: update 는 "엔티티 도입 ↔ 운영 데이터" 갭을 운영 첫 부팅 시점에 폭발시킨다**<br>새 NOT NULL FK 가 들어가는 PR 은 PR body 에 RDS 데이터 검사/마이그레이션 SQL 을 명시해야 한다. Flyway 도입 (PR 6) 전까지는 PR body 의 "운영 머지 전 필수" 체크리스트에 새 FK / NOT NULL 컬럼 추가 항목 별도 표시.<br><br>**6. Docker image 태그 보관 정책 없음 = 롤백 옵션 없음**<br>`goospel/hoseo-moodiary-linux:latest` 만 push 하면 옛 안정 버전이 사라진다. CD 에서 `latest` 외에 `${{ github.sha }}` 또는 의미 있는 버전 태그를 함께 push 하도록 하면 응급 롤백 옵션 확보. (이 PR 의 후속 인프라 PR 에서 다룰 것.)<br><br>**7. 운영 deploy 가 진짜 일어났는지의 가장 빠른 판별: `docker inspect ... .Created` 시각 vs 마지막 main 머지 시각**<br>이번 사고에서 "새 image 가 들어왔는가?" 를 확인하는 가장 결정적인 한 줄이었다:<br>&nbsp;&nbsp;```bash<br>&nbsp;&nbsp;docker inspect hoseo-moodiary --format '{{.Created}}'<br>&nbsp;&nbsp;```<br>&nbsp;&nbsp;출력 시각이 **마지막 main 머지 시각 이전**이면 새 image 가 EC2 에 아예 안 들어온 상태 — CD 또는 SSM 단계의 silent fail. CD 의 GitHub Actions "success" 체크보다 신뢰도 높음. 표면 증상 (CORS, 500, timeout) 부터 보지 말고 이 한 줄부터.<br><br>**8. 사고는 여러 결함의 동시 노출일 수 있다 — 한 층 fix 만으로 끝나지 않는다**<br>이 사고는 "CORS 버그" 같은 단일 원인이 아니라 5층의 독립 결함이 한 운영 검증에서 동시에 들켜난 것. 한 층 (springdoc) 만 고치고 멈췄으면 다음 deploy 에서 또 SSM 죽음으로 사고 재발. 진단 시 "이거 말고 다른 게 더 있나?" 의 의심을 항상 유지. [상단의 "이번 대사건" 표](#-이번-대사건-pr-38--42-2026-05-26--한-운영-검증에서-5층-결함이-동시-노출) 참조. |

<a id="t-020"></a>
### T-020 · **T-018 재발 (3번째)** — 머지된 PR 의 본문을 사후 수정 / Workflow 규칙의 회색 지대

| | |
|---|---|
| **증상** | PR #40 이 사용자에 의해 GitHub UI 에서 머지된 직후 (08:11:27), Claude 가 그 사실을 인지하지 못한 채 PR body 의 "운영 머지 전 필수" 순서/의존관계를 더 명확히 적기 위해 `gh pr edit 40 --body ...` 를 실행. 머지된 PR 의 본문이 GitHub 에서는 갱신되었지만 dev branch 의 코드/history 에는 영향 없음. 다음 사람 (사용자) 에게 "왜 머지된 PR 의 body 가 새로 바뀌어있지?" 라는 혼란만 줌 |
| **원인** | T-015 (PR 생성 전 확인), T-018 (추가 push 전 확인) 에 이어 **세 번째 재발**. 매번 회색 지대가 새로운 모양으로 나타났다:<br><br>- **T-015 (1차)**: "PR 만들기" 만 트리거였음 → "추가 push" 를 못 잡음<br>- **T-018 (2차)**: "추가 push" 도 트리거로 확장 → "PR 메타데이터 변경 (`gh pr edit`)" 을 못 잡음<br>- **T-020 (3차)**: `gh pr edit` 는 dead branch 에 commit 을 고립시키진 않으니 history 손상은 없지만, 머지된 PR 본문을 사후 수정하는 작업 자체가 무의미하고 혼란만 줌. CLAUDE.md 의 트리거 목록이 "PR 생성 / 추가 push" 만 좁게 열거하고 있던 게 직접적 구멍 |
| **해결** | **시도 → 평가:**<br><br>**(O) 옳았던 것**:<br>① 사용자가 "이미 머지했다" 라고 지적하자마자 즉시 인정 + `gh pr list` + `git fetch` + `git pull` 로 실제 상태 확인. T-018 때 학습된 복구 절차를 그대로 적용.<br>② Workflow 규칙을 **재발 패턴 자체** 의 관점에서 일반화 결정 — "또 새로운 회색 지대가 발견될 때마다 T-021, T-022 가 늘어나면 끝이 없다" 는 사용자의 판단을 받아들여 트리거 정의를 회색 지대 한정 열거가 아닌 "`gh pr` 으로 시작하는 거의 모든 명령" 으로 일반화.<br>③ 손상이 없었음에도 (메타데이터 변경뿐) T-020 으로 별도 기록 — T-018 의 교훈 "재발 케이스는 별도 T-### 로 기록해서 다음 Claude 가 'T-015/T-018 만 봤어요' 로 끝나지 않게 한다" 를 그대로 따름.<br><br>**(X) 옳지 않았던 것**:<br>① PR #40 머지 직후 `gh pr edit` 실행 직전에 `gh pr list` 를 *안 했다*. T-015/T-018 에 명문화된 규칙이 있었음에도 "`gh pr edit` 는 push 가 아니니까 다르다" 는 무의식적 판단으로 건너뜀. 규칙의 명문화된 트리거 목록이 좁다는 게 직접 원인이었지만, **명문화되지 않은 작업이라도 PR 관련이면 한 번 확인했어야 한다** — "규칙 목록에 없으면 안 해도 된다" 는 게 아니라 "PR 관련 작업은 의심부터" 가 본질.<br>② 본문 업데이트 직전 사용자에게 "지금 머지됐어?" 를 묻지도 않음. T-018 의 교훈은 단순 명령 자동화가 아니라 **상태가 외부에서 바뀔 수 있는 모든 자원은 의심부터** 라는 일반 원칙. 사용자 확인 한 마디면 사고를 피했을 것. |
| **시점** | PR #40 머지 직후 — 2026-05-26 |
| **교훈** | **1. 재발 패턴의 일반화 — 회색 지대를 한정 열거하지 말 것**<br>T-015 → T-018 → T-020 의 흐름은 "트리거 목록을 좁게 열거 → 새 회색 지대가 발견 → 목록에 한 줄 추가" 의 반복이었다. CLAUDE.md 의 규칙은 이번에 **"`gh pr` 으로 시작하는 거의 모든 명령" 으로 일반화** 되었다 — 새로운 서브커맨드 (`gh pr review`, `gh pr ready` 등) 가 미래에 추가되어도 같은 규칙이 자동 적용되도록.<br><br>**2. 손상이 없는 실수도 기록한다**<br>T-020 은 dead branch 고립 같은 history 손상은 없었다 — `gh pr edit` 는 GitHub 의 PR 메타데이터만 바꾸고 git history 에는 영향 X. 그러나 **"무해한 실수도 같은 패턴의 재발이면 기록한다"** — 다음번엔 어떤 모양으로 손상이 동반될지 모른다.<br><br>**3. "규칙 목록에 없으면 안 해도 된다" 가 아니라 "관련 작업이면 의심부터"**<br>명문화된 규칙은 알려진 함정을 막을 뿐, 새로운 함정에 대해서는 "의심하는 습관" 만이 방어선이다. PR 번호를 *읽거나 바꾸는* 모든 명령은 "지금 그 PR 이 어떤 상태지?" 를 한 번 더 묻는 습관으로 가야 한다.<br><br>**4. 외부에서 상태가 바뀔 수 있는 자원은 의심부터**<br>일반화: **GitHub PR, RDS 데이터, EC2 컨테이너 상태, 운영 image 태그** — 사람이 외부 채널로 바꿀 수 있는 자원에 대한 작업 직전엔 *항상* 현재 상태를 한 번 더 확인. tool call 사이의 시간은 LLM 에게는 0초지만 외부 세계에는 충분히 긴 시간이다. |

<a id="t-021"></a>
### T-021 · dev 머지 ≠ 운영 도달 — CD 트리거가 `main` 인 것을 잠시 잊음

| | |
|---|---|
| **증상** | PR #40 (springdoc 3.0.3 fix) 을 dev 에 머지하고 EC2 SSM agent 살린 다음, EC2 에서 `docker compose pull && up -d` 돌렸는데 컨테이너 부팅 로그가 **PR #40 이전과 100% 동일한** `BeanCreationException: queryDslQuerydslPredicateOperationCustomizer` → `NoClassDefFoundError: TypeInformation`. fix 가 운영에 안 들어간 것처럼 동작 |
| **원인** | CD 워크플로우 (`moodiary-be-cd.yaml`) 의 트리거는 `push: branches: [main]`. PR #40 은 dev 에 머지됐을 뿐 main 으로 가는 release PR (dev → main) 이 아직 만들어지지 않은 상태. 그래서:<br>1. main 에 새 commit 이 없음 → CD 가 트리거되지 않음 → Docker Hub 의 `goospel/hoseo-moodiary-linux:latest` 가 갱신되지 않음 (PR #39 머지 시점 image 그대로)<br>2. EC2 에서 `docker compose pull` 해도 Docker Hub 의 동일 sha 를 또 받아옴 → 같은 옛 코드<br>3. 같은 옛 코드 = 같은 springdoc 2.8.3 = 같은 NoClassDefFoundError<br><br>fresh 한 fix 를 빨리 운영에 반영하려는 충동에 "fix → dev 머지 → 운영 검증" 의 직관이 작동했지만, 이 프로젝트의 CD 트리거 구조는 "dev → main release PR → CD" 의 한 단계가 더 필요. README "배포" 섹션에 적혀있지만 디버깅 흐름 한가운데서는 잊혀짐 |
| **해결** | **시도 → 평가:**<br><br>**(O) 옳았던 것**:<br>① 사용자가 부팅 로그를 다시 보여줬을 때 `Created` 시각을 확인하라고 요청 → 컨테이너 Created (07:39) < PR #40 머지 (08:11) 임을 즉시 확인 → "옛 image 가 돌고 있다" 진단이 한 번에 떨어짐.<br>② 그 다음 release PR (dev → main) 즉시 생성 (PR #42) → 사용자가 머지 → CD 자동 진행 → 새 image 빌드.<br><br>**(X) 옳지 않았던 것**:<br>① PR #40 머지 직후 사용자에게 "이제 운영 검증 가능" 처럼 안내한 점. **release PR 단계를 명시했어야 함** — 안내 흐름에 "dev 머지 → release PR → main 머지 → CD → 검증" 의 전체 경로를 박았어야 했다. 사용자가 "이건 영향 없는거야?" 라고 직접 물어보고 나서야 release PR 의 필요성이 명시됨.<br>② 컨테이너 Created 시각 확인을 **첫 번째** 진단 단계로 안 했다. 표면 stack trace (springdoc NoClassDefFoundError) 가 PR #40 이전과 *동일* 한 것을 보고도 "또 다른 QueryDSL 호환성 문제일까" 같은 새로운 가설부터 의심. **표면 stack trace 가 동일하다면 코드도 동일할 가능성이 가장 높다 — Created 시각으로 image 확인부터.** |
| **시점** | PR #40 머지 직후, 운영 검증 시도 — 2026-05-26 |
| **교훈** | **1. CD 트리거가 `main` push 라면 "dev 머지 = 운영 도달" 이 아니다**<br>이 프로젝트의 CD/CI 분리는:<br>&nbsp;&nbsp;- **CI** (`moodiary-be-ci.yaml`) — PR to dev → 빌드/테스트 (운영 영향 X)<br>&nbsp;&nbsp;- **CD** (`moodiary-be-cd.yaml`) — push to main → Docker image build + push + SSM deploy<br>fix PR 을 dev 에 머지한 뒤 운영 검증을 하기 전에 **반드시 release PR (dev → main)** 을 하나 더 만들어 머지. 이 단계는 README "배포" 와 plan.md 에 적혀 있지만 디버깅 흐름 한가운데서 자주 잊혀진다.<br><br>**2. 운영 검증 직전 한 줄 체크: "내 fix 의 코드가 main 에 있는가?"**<br>구체적으로:<br>&nbsp;&nbsp;```bash<br>&nbsp;&nbsp;git log --oneline origin/main | head -3<br>&nbsp;&nbsp;```<br>&nbsp;&nbsp;여기에 내 fix commit 이 안 보이면 운영 검증을 시작할 수 없다 — Docker Hub 의 `latest` 도 그 fix 를 포함하지 않은 상태이기 때문.<br><br>**3. PR 머지 안내 시 다음 단계를 항상 명시**<br>"dev 에 머지하세요" 가 끝이 아니라 "dev 머지 → 코드 검증 → release PR → main 머지 → CD 자동 진행 → 검증" 의 전체 경로를 매번 박아라. 안내가 한 단계라도 빠지면 사용자가 그 단계를 건너뛰고 다음 단계에서 사고를 겪는다. |

<a id="t-022"></a>
### T-022 · `docker compose pull` ≠ 새 image 보장 — Docker Hub 의 `latest` 가 옛 image 면 받아와도 동일 sha

| | |
|---|---|
| **증상** | EC2 SSH 에서 `docker compose pull && docker compose up -d` 돌렸는데 `docker inspect hoseo-moodiary --format '{{.Created}}'` 가 이전과 동일한 `2026-05-26T07:39:23` 그대로. "pull 했는데 왜 새 image 가 안 들어왔지?" 라는 혼란 |
| **원인** | `docker compose pull` 은 **registry (Docker Hub) 에 있는 tag 의 image 를 가져오는** 명령이다. 그 tag 가 가리키는 image 자체가 갱신되지 않았다면 "받아와봐야 동일 sha". compose.yaml 의 `pull_policy: always` 옵션도 마찬가지 — registry 의 image 가 새 거면 받아오고, 같으면 그냥 cache 사용.<br><br>이번 사건의 시나리오 ([T-021](#t-021)):<br>1. PR #40 fix 가 dev 에 머지됨<br>2. main 에는 아직 머지 안 됨 → CD 안 돔 → Docker Hub `latest` 는 PR #39 시점 image 그대로<br>3. EC2 에서 `docker compose pull` 해도 동일 sha 받아옴<br>4. `docker compose up -d` 가 "이미 그 image 의 컨테이너가 도는 중" 으로 판단 → 컨테이너 재생성 안 함, 또는 재생성해도 같은 image 라 같은 부팅 실패 |
| **해결** | **시도 → 평가:**<br><br>**(O) 옳았던 것**:<br>① `docker inspect ... .Created` 시각으로 image 갱신 여부를 명시적으로 확인 — 사용자에게 "Created 시각이 머지 시각 이후인지 보라" 라고 안내한 게 시나리오 A/B/C 를 5초 만에 가른 결정타.<br>② Docker Hub 의 image 가 안 바뀐 진짜 원인 ([T-021](#t-021): release PR 미머지) 으로 거슬러 올라감 — EC2 의 docker 동작을 더 깊이 파지 않고 한 단계 위 (CD 트리거) 의 원인으로 정확히 점프.<br><br>**(X) 옳지 않았던 것**:<br>① 처음에 사용자에게 "수동 fallback: `docker compose pull && up -d`" 를 안내할 때 **Docker Hub 가 새 image 를 push 받기 전이라는 사전 조건을 명시 안 함**. 사용자가 그 조건이 충족됐다고 가정하고 명령만 돌리니 시간만 낭비. 안내에는 항상 사전 조건도 함께. |
| **시점** | PR #40 머지 후 운영 검증 시도 — 2026-05-26 |
| **교훈** | **1. `docker compose pull` 은 "Docker Hub 가 새 image 를 갖고 있을 때만" 새 image 를 가져온다**<br>로컬에서 `pull` 명령이 0 byte 다운로드로 즉시 끝나면 registry 의 image 가 변경되지 않은 것. 다른 진단 명령:<br>&nbsp;&nbsp;```bash<br>&nbsp;&nbsp;# Docker Hub 의 latest 의 sha digest 확인<br>&nbsp;&nbsp;docker manifest inspect <user>/<image>:latest | grep -i digest<br>&nbsp;&nbsp;# 로컬 image sha 와 비교<br>&nbsp;&nbsp;docker inspect <image>:latest --format '{{.Id}}'<br>&nbsp;&nbsp;```<br><br>**2. 운영 deploy 검증의 표준 한 줄: `docker inspect ... .Created`**<br>이번 사고의 진단 핵심이었다. Created 시각이 마지막 main 머지 시각 *이후* 면 새 image, *이전* 이면 새 image 가 EC2 에 도달 못 한 상태 — 어느 단계에서 끊겼는지 (CD silent fail / Docker Hub push 실패 / SSM 실행 실패) 를 별도로 진단. [T-019](#t-019) 의 교훈 #7 도 동일.<br><br>**3. 명령 안내에는 사전 조건도 함께**<br>"수동 fallback: `docker compose pull && up -d`" 같은 안내는 사용자에게 명확하지만, 그 명령이 의미 있으려면 Docker Hub 의 image 가 새로 push 된 상태여야 한다는 사전 조건이 있다. 안내문에 사전 조건을 함께 박아라 — "Docker Hub 의 `latest` 가 갱신된 후 (= CD 가 성공한 후) `docker compose pull && up -d`". |

<a id="t-023"></a>
### T-023 · `docker compose` (스페이스) vs `docker-compose` (하이픈) — 옛 CD 들의 silent fail 진실

| | |
|---|---|
| **증상** | PR #45 (CD 신뢰성 강화) 가 main 에 들어간 후 첫 release 인 PR #46 의 CD 가 **1분 9초 만에 fail**. GitHub Actions 로그:<br>&nbsp;&nbsp;```<br>&nbsp;&nbsp;CommandId: 00d06e9a-3737-4fb2-a5b7-721b2d5c6245<br>&nbsp;&nbsp;Wait for SSM execution<br>&nbsp;&nbsp;aws: [ERROR]: Waiter CommandExecuted failed:<br>&nbsp;&nbsp;  For expression "Status" we matched expected path: "Failed"<br>&nbsp;&nbsp;```<br>SSM 명령이 EC2 에 도달은 했으나 (CommandId 받음 = SSM agent 살아있음) 실행 자체가 1.5초 만에 Failed. |
| **원인** | 워크플로우의 `docker compose pull` / `docker compose up -d` (스페이스, Docker Compose v2 plugin 형식) 가 EC2 에서 **unknown sub-command**. EC2 의 `docker compose --version` 출력이 `Docker version 25.0.14` (= `compose` 가 인식 안 돼서 `docker --version` 으로 fallback) 였다.<br><br>EC2 에는 **standalone `docker-compose` v2 binary 만 있고** docker plugin 으로는 등록 안 된 상태. 즉 `docker-compose` (하이픈) 은 동작, `docker compose` (스페이스) 는 fail.<br><br>**더 큰 진실** — 옛 워크플로우도 `docker compose` (스페이스) 였다. 그러면 옛 CD 들도 모두 SSM 단계에선 같은 unknown sub-command 로 fail 했을 것. 그런데 GitHub Actions 는 success 표시 — `aws ssm send-command` 가 enqueue 응답만 받고 끝났기 때문 ([T-019](#t-019) 의 silent fail). **즉 옛 CD 의 자동 deploy 는 한 번도 진짜로 작동한 적이 없었다.** 운영 image 갱신은 모두 사용자가 SSH 로 들어가 수동 `docker-compose pull && up -d` 돌렸을 때만. PR #45 의 wait/verify 가 이 진실을 처음으로 노출. |
| **해결** | **시도 → 평가:**<br><br>**(O) 옳았던 것**:<br>① 사용자에게 "1번~4번 명령을 직접 EC2 에서 한 줄씩 돌려봐" 하고 segment 별 진단을 부탁한 점 — 어디서 fail 하는지 격리해 `docker compose` vs `docker-compose` 차이를 빠르게 찾아냄.<br>② `docker compose --version` 출력 (`Docker version 25.0.14` 만 나옴) 한 줄로 확정 — `compose` sub-command 미등록의 결정적 증거.<br>③ PR #45 의 wait/verify 가 이 silent fail 을 진짜로 드러낸 것 자체가 가치 있는 일임을 짚어둠 — CD fail 떨어진 게 사고가 아니라 **방어선이 의도대로 작동한 결과**.<br>④ fix 는 워크플로우만 한 줄 (`docker compose` → `docker-compose`) 로 끝낼 수 있는 가장 작은 변경 — EC2 환경 변경 (plugin 설치) 같은 더 큰 일은 별도 PR 로.<br>⑤ README / CLAUDE.md / compose.yaml 의 명령 예시도 같이 통일 — 다음 사람이 두 형식 보고 헷갈리지 않게.<br><br>**(X) 옳지 않았던 것**:<br>① PR #45 작성 시 EC2 의 docker compose plugin 등록 여부를 확인하지 않음. 옛 워크플로우의 `docker compose` 를 무비판적으로 복사 — 그게 실제로는 옛 환경에서도 fail 하고 있었음을 의심 안 함. CD 신뢰성 PR 의 본질이 "옛 동작 그대로 두지 말고 의심부터" 인데, 정작 명령 자체는 옛 것을 그대로 둔 모순.<br>② 옛 CD 들이 "성공" 으로 끝났던 게 silent fail 의 결과일 수 있다는 가능성을 PR #45 단계에서는 충분히 고려 안 함. T-019 의 교훈에 "옛 deploy 가 실제로 EC2 에 도달한 적 있는지 모른다" 라고 적혀 있었음에도, 그게 명령 자체의 invalid 까지 의심하게 만들진 못함. |
| **시점** | PR #46 release CD 첫 실행 (PR #45 의 wait/verify 가 처음 작동한 시점) — 2026-05-26 |
| **교훈** | **1. 옛 명령이 "이미 동작 중이니까 OK" 라는 가정은 silent fail 환경에선 무효**<br>"이전에 잘 됐다" 는 증거가 안 된다 — 그 "성공" 자체가 가짜였을 수 있다. CD 신뢰성을 강화하는 PR 은 **명령 자체의 validity 도 같이 검증**해야 한다. 옛 명령을 그대로 두지 말고 한 번씩 다 의심.<br><br>**2. `docker compose` (스페이스, Docker CLI plugin) ≠ `docker-compose` (standalone binary)**<br>- `docker compose <cmd>` = `docker` 명령의 sub-command 형태. Docker Engine 의 plugin 폴더 (`~/.docker/cli-plugins/docker-compose` 또는 system-wide) 에 binary 가 등록되어 있어야 작동.<br>- `docker-compose <cmd>` = standalone binary. PATH 에만 있으면 작동.<br>- 둘 다 같은 compose v2 일 수도 있지만 등록 위치가 다르다. **환경에서 어느 쪽이 살아있는지 확인 명령**:<br>&nbsp;&nbsp;```bash<br>&nbsp;&nbsp;docker compose version    # "Docker Compose version v2.x.x" 가 나오면 plugin OK<br>&nbsp;&nbsp;docker-compose --version  # standalone OK 여부<br>&nbsp;&nbsp;```<br>&nbsp;&nbsp;`docker compose --version` 이 `Docker version ...` 로 응답하면 plugin 등록 안 됨.<br><br>**3. SSM 의 silent fail 은 명령의 invalid 까지 가린다**<br>`aws ssm send-command` 의 enqueue 응답이 success 라는 건 *명령이 큐에 들어갔다* 는 의미일 뿐, 그 명령이 valid 인지조차 검증하지 않는다. T-019 의 wait/verify (PR #45) 가 들어와야 비로소 invalid 명령 / 실행 실패가 GitHub Actions 로 흘러나옴. **CD 신뢰성 강화는 invalid 명령 발견의 첫 단계** — 이번 PR 직후 더 많은 latent 결함이 노출될 수 있다고 가정.<br><br>**4. CD fail 떨어진 것 자체에 당황하지 말 것**<br>새 방어선이 들어간 직후 CD fail = 방어선이 잡아준 것 = **성공의 신호**. 진짜 사고는 그 fail 의 진단을 게을리하거나, "옛날엔 됐는데" 라며 방어선을 무력화시킬 때 시작된다.<br><br>**5. 같은 의미의 두 명령이 환경에 따라 호환 안 되는 경우, 문서/명령 전부를 한 형식으로 통일**<br>이번 fix 의 부수 작업: README, CLAUDE.md, compose.yaml, workflow 의 `docker compose` 전부를 `docker-compose` 로 통일. 두 형식을 섞으면 다음 사람 (또는 미래의 자신) 이 또 같은 trap. |

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
