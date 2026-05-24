# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Moodiary is a Spring Boot 4.x REST API backend for a diary/mood tracking application. Stack: Java 25 (Amazon Corretto), Spring Data JPA + MySQL, QueryDSL for complex queries, SpringDoc OpenAPI (Swagger UI), Lombok. Deploys to AWS EC2 as a Docker container via GitHub Actions + AWS SSM.

## Commands

### Build & Run
```bash
# Build (runs tests)
./gradlew build

# Build without tests
./gradlew build -x test

# Run locally (requires a MySQL instance reachable at localhost:3309)
./gradlew bootRun

# Run a single test class
./gradlew test --tests "hoseo.moodiary.MoodiaryApplicationTests"
```

Swagger UI is served at `http://localhost:8080/swagger-ui/index.html` when the app is running.

### Local Development Database
There is no `docker-compose.yml` checked in. Bring up MySQL however you prefer; the app expects:

- host/port: `localhost:3309`
- database: `moodiary`
- credentials: `dev` / `dev123`

Quick one-liner:
```bash
docker run -d --name moodiary-mysql -p 3309:3306 \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=moodiary \
  -e MYSQL_USER=dev -e MYSQL_PASSWORD=dev123 mysql:8
```

Override per-developer settings by editing `src/main/resources/application.yaml` (URL/username/password lines are flagged "개인에 맞게 수정할 것").

### QueryDSL Q-class generation
Q-classes are generated into `src/main/generated/` by `annotationProcessor 'com.querydsl:querydsl-apt'` during `compileJava`. `./gradlew clean` deletes that directory (configured in `build.gradle`).

> **Trap**: QueryDSL is declared as `compileOnly` (not `runtimeOnly`) because of a *"springdoc과 Spring Data 4.x 호환 문제"* — see the comment in `build.gradle`. Don't "fix" this to `runtimeOnly` without re-validating Swagger + Spring Data 4.x boot up.

## Architecture

### Layer Structure
Standard Spring layered architecture: `Controller → Service → Repository → Entity`

```
hoseo.moodiary
├── controller/     # @RestController — HTTP endpoints, returns ResponseEntity
├── service/        # @Service @Transactional — business logic
├── repository/     # Spring Data JPA interfaces (extend JpaRepository)
├── entitiy/        # JPA @Entity classes  ← package name is intentionally "entitiy" (typo); keep using it
│   └── base/       # BaseEntity — createdAt/updatedAt via JPA Auditing
├── dto/
│   ├── request/    # Input DTOs — include toEntity() factory method
│   └── response/   # Output DTOs — @Builder pattern, ErrorResponseDto for error bodies
└── exception/      # Domain exceptions + GlobalExceptionHandler (@RestControllerAdvice)
```

### Key Conventions
- Entity primary keys use UUID (`@UuidGenerator` from Hibernate); column name = `<table>_id` (e.g. `post_id`)
- All entities extend `BaseEntity` and inherit `createdAt` / `updatedAt`
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` on entities; mutation goes through explicit methods (e.g. `Post.update(...)`) so updates work via dirty-checking inside `@Transactional`
- Request DTOs expose a `toEntity()` method; Response DTOs are built inline in the service via `@Builder`
- Read-only service methods carry `@Transactional(readOnly = true)`
- Each domain exception (e.g. `PostNotFoundException`) gets a dedicated `@ExceptionHandler` in `GlobalExceptionHandler` that maps it to an `ErrorResponseDto { message }` with the appropriate HTTP status. Validation errors (`MethodArgumentNotValidException`) and malformed JSON (`HttpMessageNotReadableException`) are already wired there — extend that file rather than catching in controllers.

### Auditing
`@EnableJpaAuditing` lives on `MoodiaryApplication`. Disabling auditing means turning it off there; without it `BaseEntity` timestamps stop populating.

### Database
- Local dev: MySQL on port 3309, db `moodiary`, user `dev`/`dev123`
- `ddl-auto: create-drop` — schema is **dropped and recreated on every restart** locally. Don't rely on local data persisting between runs.
- `show_sql: true` + `format_sql: true` — generated SQL is logged. Useful for verifying QueryDSL output.
- Production: AWS RDS MySQL; credentials injected via `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` environment variables on the container.

## CI/CD

| Trigger | Workflow | What it does |
|---|---|---|
| PR → `dev` | `moodiary-be-ci.yaml` | `./gradlew build` (runs tests) + uploads `build/reports/tests/test/` as artifact |
| `push` → `main` | `moodiary-be-cd.yaml` | Build → push image to Docker Hub → deploy to EC2 via AWS SSM `send-command` |

The CD job runs `docker build` against the repo (using `Dockerfile`, base image `amazoncorretto:25-al2023-headless`) and copies `build/libs/*.jar` into the image. On deploy, SSM runs `docker stop/rm/pull/run` on the target EC2 instance, exposing port 8080 and injecting `SPRING_DATASOURCE_*` env vars.

Required GitHub Secrets:
- Docker Hub: `DOCKER_HUB_USERNAME`, `DOCKER_HUB_TOKEN`
- AWS (OIDC): `AWS_GITHUB_OIDC_ROLE_ARN`, `AWS_REGION`, `EC2_INSTANCE_ID`
- RDS: `RDS_ENDPOINT`, `RDS_USERNAME`, `RDS_PASSWORD`

To reproduce the production image locally:
```bash
./gradlew build -x test
docker build -t moodiary:local .
```
