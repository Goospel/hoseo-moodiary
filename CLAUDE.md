# CLAUDE.md

이 파일은 Claude Code (claude.ai/code) 가 이 저장소에서 작업할 때 참조하는 가이드다.

> 📚 정적 정보 (빌드 명령, 패키지 구조, CI/CD 트리거 표, GitHub Secrets) 는 대부분 **[README.md](./README.md)** 에 있다 — Claude 는 셋업 관련 디테일은 README 를 우선 참조한다. 이 파일에는 *코드를 짤 때 / 워크플로우를 돌릴 때* Claude 가 반드시 알아야 하는 것만 담는다.

## 프로젝트 개요

Moodiary 는 다이어리/무드 트래킹 애플리케이션을 위한 Spring Boot 4.x REST API 백엔드. 스택: Java 25 (Amazon Corretto), Spring Data JPA + MySQL, 복잡한 쿼리는 QueryDSL, SpringDoc OpenAPI (Swagger UI), Lombok. AWS EC2 에 Docker 컨테이너로 배포 — GitHub Actions + AWS SSM 경로.

## 빠르게 보는 명령

- 빌드 (테스트 포함): `./gradlew build`
- 단일 테스트 클래스: `./gradlew test --tests "<FQN>"`
- 로컬 실행은 `localhost:3309` 의 MySQL 이 필요 (db `moodiary`, 사용자 `dev`/`dev123`). 셋업 → [README "빠른 시작"](./README.md).
- **`src/main/resources/application.yaml` 은 절대 직접 편집하지 마라.** 새 키를 추가하려면 `${ENV_VAR:기본값}` 플레이스홀더로 적는다. 개인 오버라이드는 `src/main/resources/application-local.yaml` (gitignored, Spring 이 자동 merge) 에 넣는다.
- **QueryDSL**: `querydsl-jpa` 는 `implementation` 이다 (`compileOnly` 아님). Q-class 는 `src/main/generated/` 로 생성된다. 이 설정을 만지기 전에 [troubleshooting T-016](./claude-docs/troubleshooting.md) 을 반드시 읽어라 — 진짜 이유가 있다.

## 아키텍처

레이어 구조: `Controller → Service → Repository → Entity`. 패키지 레이아웃은 [README "아키텍처"](./README.md) 참조.

### 코드 컨벤션 (Claude 가 반드시 내재화)
- 레이어별 패키지: `controller / service / repository / entitiy / dto.{request,response} / exception / security / config`. `entitiy/` 는 의도된 역사적 오타다 — 고치지 말고 그대로 사용한다.
- 엔티티 PK 는 UUID (`@UuidGenerator` — Hibernate 기본 제공); 컬럼명은 `<table>_id` (예: `post_id`).
- 모든 엔티티는 `BaseEntity` 를 상속해서 `createdAt` / `updatedAt` 을 자동으로 받는다.
- 엔티티에는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`; 변경은 명시적 메서드 (예: `Post.update(...)`) 를 거치게 한다. 그래야 `@Transactional` 안에서 dirty-checking 으로 업데이트가 동작한다.
- Request DTO 는 `toEntity()` 메서드를 노출; Response DTO 는 서비스 안에서 `@Builder` 로 인라인 생성.
- Read-only 서비스 메서드에는 `@Transactional(readOnly = true)` 를 단다.
- 각 도메인 예외 (예: `PostNotFoundException`) 는 `GlobalExceptionHandler` 안에 전용 `@ExceptionHandler` 를 두고, `ErrorResponseDto { message }` + 적절한 HTTP 상태로 매핑한다. Validation (`MethodArgumentNotValidException`), 잘못된 JSON (`HttpMessageNotReadableException`), 필수 쿼리 파라미터 누락 (`MissingServletRequestParameterException`) 은 이미 연결되어 있다 — 컨트롤러에서 잡지 말고 이 파일을 확장하라.

### Auditing
`@EnableJpaAuditing` 은 `JpaAuditingConfig` 에 있다 (메인 클래스에 직접 달면 슬라이스 테스트가 깨진다). 이게 빠지면 `BaseEntity` 의 timestamp 가 안 채워진다.

### DB 정책
- `ddl-auto: update` (application.yaml 기본값) — 스키마는 아직 확정되지 않은 단계. Hibernate 가 부팅 시 누락 컬럼/테이블을 추가하고 기존 데이터는 보존한다.
- `update` 는 **인덱스/제약 조건의 생성을 보장하지 않는다** — 인덱스/제약이 중요한 경우 수동으로 DDL 을 작성하고 PR 에 명시한다.
- 계획: Flyway 도입 (PR 6) 시점에 `validate` 로 전환. 그 전까지는 `ddl-auto` 를 함부로 바꾸지 마라.

## CI/CD 메모
트리거 표 + 시크릿 목록은 [README "배포"](./README.md). Claude 가 기억해야 할 한 가지: `main` 의 CD 경로 = GitHub Actions → Docker Hub → AWS SSM → EC2 `docker-compose` (compose.yaml 은 저장소 루트, SSM 이 배포 시 pull 한다). 명령은 **`docker-compose` (하이픈)** — EC2 환경 의존, T-023 참조. compose 에 새 환경변수가 추가되면 **GitHub Secrets 와 EC2 `.env` 양쪽** 을 모두 업데이트해야 한다.

## Workflow 규칙 (Claude 가 반드시 읽는다)

이건 제안이 아니라 의무 사항이다.

### 모든 PR 관련 작업 전 — 항상 PR 상태 확인

사용자는 GitHub UI 에서 PR 을 머지한다. Claude 의 tool call 사이에 머지가 일어나는 경우가 많다. Claude 가 PR 이 아직 열려 있다고 가정하고 그 브랜치/PR 에 작업을 계속하거나, 메시지에서 "open" 인 것처럼 언급하면 — 헛수고 + 혼란스러운 메시지 + **머지된 PR 에 자동 반영되지 않아 `dev` 에 도달하지 못하고 dead branch 에 고립된 commit** (T-018 에서 일어난 일) 또는 **머지된 PR 의 본문을 무의미하게 수정** (T-020 에서 일어난 일) 이 발생한다.

**다음 모든 시점에 반드시 실행** — `gh pr` 명령으로 PR 상태를 *읽거나 바꾸는* 거의 모든 작업이 트리거다:

- 새 PR 을 만들기 직전 (`gh pr create`),
- 이미 PR 이 열려 있는 브랜치에 추가 commit 을 push 하기 직전 (`git push`),
- **머지/오픈된 PR 의 본문/제목/라벨/리뷰어를 수정하기 직전** (`gh pr edit`),
- **PR 에 코멘트를 달기 직전** (`gh pr comment`),
- **PR 을 close / reopen / merge 하기 직전** (`gh pr close|reopen|merge`),
- 메시지에서 PR 번호를 언급하기 직전.

```bash
gh pr list --state all --limit 10
```

그 다음 점검:
- PR 이 이미 머지됐는가? → **그 브랜치에 push / 메타데이터 변경을 멈춰라.** 머지된 PR 의 body/comment 수정은 dev branch 의 코드/history 에 영향이 없어서 *의미 없는 작업*이고, 다음 사람에게 혼란만 준다. `dev` 를 동기화하고, 남은 변경이 필요하면 새 브랜치 + 별도 PR.
- PR 만들려는 브랜치가 이미 머지된 상태인가? → 최신 `dev` 에서 새로 브랜치 따라, 머지된 브랜치에 다시 push 하지 말고.
- 새 PR 만들기보다 이미 열린 PR 에 합치는 게 맞는가? → 분리 전에 사용자에게 물어봐라.
- 언급하려는 PR 번호가 아직 open 인가, 이미 머지됐는가? 표현을 그에 맞춰서 한다.

> **트리거 범위 확장의 역사**: T-015 는 "PR 생성 전" 만 명시, T-018 은 "추가 push 전" 까지 확장, T-020 은 "PR 메타데이터 변경 전" 까지 확장. 같은 패턴이 회색 지대를 새로 발견할 때마다 재발했다 — 그래서 위 목록은 회색 지대 한정 열거가 아니라 **"`gh pr` 으로 시작하는 거의 모든 명령" 으로 일반화** 된 것이다. 새로운 `gh pr` 서브커맨드가 미래에 추가되어도 이 규칙은 그대로 적용된다.

### PR 생성과 머지의 분담 — Claude 는 생성, 사용자는 머지

**역할 분담** (사용자 명시 합의, 2026-05-26):

- **PR 생성은 Claude 의 일** — 브랜치 따기 → commit → push → `gh pr create` 까지. PR 본문도 Claude 가 작성.
- **머지는 사용자의 일** — Claude 는 `gh pr merge` 를 자동으로 돌리지 마라. 사용자가 GitHub UI 에서 리뷰 후 직접 머지한다.
- **그래서 PR 생성 후 머지 진행 여부를 사용자에게 묻지 마라.** PR 링크 + 핵심 요약만 보고하고 그 task 는 종료. AskUserQuestion 으로 "merge 진행할까?" 를 띄우는 건 사용자가 명시한 분담을 거스르는 노이즈다. 사용자는 다음 task 로 자연스럽게 넘어가거나 머지를 직접 한 뒤 다음 지시를 준다.

**예외 — 사용자가 명시적으로 머지까지 요청한 경우에만 Claude 가 머지**:
- "머지해" / "squash merge 해" / "이거 dev 까지 넣어줘" 같이 머지 행위를 명시한 메시지.
- 단순히 "PR 만들어줘" / "dev 로 머지하자" 같은 표현은 **PR 생성까지가 범위** — 한국어의 "머지하자" 는 흐름상 "PR 만들어서 dev 로 보내자" 까지의 의미로 굳어졌다 (Anthropic auto-classifier 도 같은 해석).

> 이 규칙의 트리거: PR #55 (이 PR 직전 PR) 에서 Claude 가 `gh pr merge` 를 자동으로 시도했다가 classifier 가 거부 → AskUserQuestion 으로 승인 받음. 사용자가 "PR은 너가 생성해. 머지는 내가 할게. 이것도 CLAUDE.md 로 규칙으로 정해놔. 계속 물어보지말고" 라고 분담을 명시.

### 새 feature/fix 작업 시작 전 — dev 동기화
```bash
git fetch origin && git checkout dev && git pull origin dev && git checkout -b <new-branch>
```
로컬 `dev` 는 거의 항상 stale 하다 — 사용자가 GitHub 에서 원격으로 머지하기 때문이다. **stale 한 로컬 `dev` 에서 절대 브랜치를 따지 마라.**

### 운영 영향 변경
`application.yaml`, `.gitignore`, `compose.yaml`, `.github/workflows/`, DB 스키마, 환경변수 — 이 중 하나라도 건드리는 변경은 PR body 에 **"운영 머지 전 필수"** 체크리스트를 박아라. 깊이 묻지 말고 눈에 띄게.

### CI / CD / Pages 워크플로우 fail 진단 — 0번째 진단은 [githubstatus.com](https://www.githubstatus.com/)

GitHub Actions / Pages / CD 워크플로우가 "Set up job" / 액션 download / artifact upload / Pages deploy 같은 **GitHub 인프라 호출 단계에서 즉시 fail** 할 때 — 그러니까 우리 코드/스크립트가 돌기도 전에 죽을 때 — Claude 의 첫 진단은 **항상 [githubstatus.com](https://www.githubstatus.com/) 확인** 이다. 코드도, 워크플로우 yaml 도, 액션 버전도, OIDC 권한도 만지기 전에.

이유: GitHub 인프라 incident 가 지속 중이면 codeload / Actions API / Pages API 가 일시적으로 404 / 500 / timeout 을 던지는데, `gh run view --log-failed` 의 에러는 늘 "특정 SHA tarball 을 못 받음" / "특정 endpoint 가 응답 안 함" 같이 **마치 영구 결함처럼 보이는 모양**으로 떨어진다. 재실행해도 incident 안 풀린 동안엔 같은 에러가 반복되니 "같은 SHA 로 두 번 fail = 그 SHA 가 사라진 영구 상태" 같은 잘못된 추론으로 빨려들기 쉽다 — 그러나 재실행만으로는 "일시 장애" 와 "영구 deprecated" 를 판별할 수 없다. **외부 신호 (githubstatus) 를 봐야 비로소 갈린다.** 이걸 빠뜨리면 멀쩡한 액션 버전을 멋대로 메이저 업그레이드하는 잘못된 fix PR 까지 만들 수 있다 (T-024 에서 실제로 일어난 일).

**진단 순서 (의무)**:
1. `gh run view <run-id> --log-failed` 로 정확한 에러 메시지 잡기.
2. 에러가 **GitHub 인프라 호출** (`codeload.github.com`, `api.github.com`, Pages deployment, OIDC token 발급, Actions runner provisioning 등) 에 관한 거면 → **[githubstatus.com](https://www.githubstatus.com/) 확인**.
3. `Git Operations` / `API Requests` / `Actions` / `Pages` / `Webhooks` 중 해당 컴포넌트가 **오렌지 (degraded) 또는 빨강 (major outage)** 이면 → **아무 것도 고치지 말고 GitHub 가 복구할 때까지 대기**. 그 사이에 코드 / 액션 버전 / yaml 을 만지면 잘못된 진단이 PR 로 굳어버린다.
4. githubstatus 가 깨끗한데도 같은 에러가 반복되면 그제서야 코드 / 액션 버전 / 토큰 / 권한 의심.

**예외 — 우리 스크립트 단계가 죽은 경우**: 워크플로우의 `run:` 블록 안 (예: `./gradlew build`, `marp ...`, `docker-compose pull`) 에서 fail 한 거면 우리 코드/명령의 문제일 가능성이 훨씬 높다 — 그 때는 githubstatus 우선순위가 낮아지고 직접 디버깅이 먼저. **이 규칙은 "GitHub 인프라가 우리 코드 돌기 전에 죽인 경우" 에 한정**.

> 트리거 사례: T-024 (PR #55 직후 Deploy GitHub Pages 가 `actions/upload-pages-artifact@v3` 의 SHA tarball 404 로 두 번 연속 fail). Claude 가 `gh api` 로 액션 ref/태그를 파보며 "옛 메이저가 deprecated 된 모양이다, v5 로 올리자" 라는 그럴듯한 가설로 점프했지만 — 사용자가 githubstatus 의 Actions/Pages 오렌지를 보고 진단을 바로잡아줬다. **매끄러운 가설일수록 의심을 한 번 더, 그리고 외부 신호를 한 번 더.**

### PR 생성 전 — troubleshooting 로그 sweep
Claude 가 여는 모든 PR 의 task 리스트에서 **마지막에서 두 번째 task = "troubleshooting.md 점검"** 이다. 다음 체크리스트를 돌린다:

1. **세션을 다시 훑어라** — 이 PR 진행 중 부딪힌 모든 에러 (빌드 실패, 테스트 실패, 스택 트레이스, "어 이거 왜 이러지?" 순간, 설정 trap). *1분 이상 디버깅한 모든 것* 을 후보로 둔다.
2. 각 후보에 대해 물어라:
   - `claude-docs/troubleshooting.md` 에 이미 있는가? → skip.
   - 프로젝트 고유 (다음 사람 / 다음 Claude 도 똑같이 만날) 함정인가? → **반드시 새 T-### 항목 추가.**
   - 일반적이거나 일회성 (예: 명령어 오타, IDE 특이사항) 인가? → skip.
3. 새 항목은 기존 schema 를 따른다: **증상 / 원인 / 해결 / 시점 / 교훈**. 상단 인덱스에도 한 줄 추가하고 `<a id="t-NNN"></a>` 로 링크.
4. **PR body 작성 시점에 떠올리려고 하지 마라** — 그때는 이미 다른 데로 옮겨갔다. sweep 자체를 task 리스트에 명시적인 task 로 박아두고, `gh pr create` 전에 끝낸다.

> 이 규칙이 절대적인 이유: PR #31 에서 `MissingServletRequestParameterException → 500` 발견이 plan / api-contracts / PR body 에는 적혔지만 troubleshooting.md 에는 빠졌다 — 그런데 그곳이 정확히 미래의 Claude 가 같은 trap 이 다시 터질 때 grep 으로 찾는 위치다. 수정은 미래의 Claude 가 찾을 곳에 정확히 박혀 있어야 한다.
