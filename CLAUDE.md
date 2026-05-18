# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Moodiary is a Spring Boot 4.x REST API backend for a diary/mood tracking application. It uses Java 25, JPA with MySQL, QueryDSL for complex queries, and deploys to AWS EC2 via Docker through GitHub Actions.

## Commands

### Build & Run
```bash
# Build (runs tests)
./gradlew build

# Build without tests
./gradlew build -x test

# Run locally (requires MySQL at localhost:3309)
./gradlew bootRun

# Run a single test class
./gradlew test --tests "hoseo.moodiary.MoodiaryApplicationTests"
```

### Local Development Database
```bash
# Start MySQL via Docker Compose (exposes port 3309)
docker compose up mysql-moodiary -d

# Start full stack
docker compose up -d
```

### QueryDSL Q-class generation
QueryDSL Q-classes are generated into `src/main/generated/` during compilation. Run `./gradlew compileJava` to regenerate. The `clean` task deletes this directory.

## Architecture

### Layer Structure
Standard Spring layered architecture: `Controller → Service → Repository → Entity`

```
hoseo.moodiary
├── controller/     # @RestController — HTTP endpoints, returns ResponseEntity
├── service/        # @Service @Transactional — business logic
├── repository/     # Spring Data JPA interfaces (extend JpaRepository)
├── entitiy/        # JPA @Entity classes
│   └── base/       # BaseEntity — provides createdAt/updatedAt via JPA Auditing
└── dto/
    ├── request/    # Input DTOs — include toEntity() factory method
    └── response/   # Output DTOs — @Builder pattern
```

### Key Conventions
- Entity primary keys use UUID (`@UuidGenerator` from Hibernate)
- All entities extend `BaseEntity` for audit timestamps
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` on entities
- Request DTOs include a `toEntity()` conversion method
- Response DTOs use `@Builder` and are constructed inline in the service layer

### Database
- Local dev: MySQL on port 3309 (credentials: `dev`/`dev123`, database: `moodiary`)
- `ddl-auto: create-drop` is set — schema is recreated on each restart locally
- Production: AWS RDS MySQL, credentials injected via environment variables

## CI/CD

| Trigger | Workflow | What it does |
|---|---|---|
| PR → `dev` branch | CI | Builds and runs tests, uploads test report artifact |
| PR → `main` branch | CD | Builds, pushes Docker image to Docker Hub, deploys to EC2 via SSH |

Docker image is `amazoncorretto:25-al2023-headless` based. The built JAR (`build/libs/*.jar`) is copied into the image. Build the JAR locally with `./gradlew build -x test` before building the Docker image manually.

Required GitHub Secrets: `DOCKER_HUB_USERNAME`, `DOCKER_HUB_TOKEN`, `EC2_HOST`, `EC2_USER`, `EC2_KEY`, `RDS_ENDPOINT`, `RDS_USERNAME`, `RDS_PASSWORD`.
