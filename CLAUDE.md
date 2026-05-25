# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> 📚 Most static info (build commands, package structure, CI/CD trigger table, GitHub Secrets) lives in **[README.md](./README.md)** — Claude defaults there for setup details. This file captures only what Claude needs while *writing code and operating workflow*.

## Project Overview

Moodiary is a Spring Boot 4.x REST API backend for a diary/mood tracking application. Stack: Java 25 (Amazon Corretto), Spring Data JPA + MySQL, QueryDSL for complex queries, SpringDoc OpenAPI (Swagger UI), Lombok. Deploys to AWS EC2 as a Docker container via GitHub Actions + AWS SSM.

## Commands at a glance

- Build (with tests): `./gradlew build`
- Single test class: `./gradlew test --tests "<FQN>"`
- Local run needs MySQL at `localhost:3309` (db `moodiary`, user `dev`/`dev123`). Setup → [README "빠른 시작"](./README.md).
- **Never edit `src/main/resources/application.yaml` directly.** New keys must use `${ENV_VAR:default}` placeholders. Per-developer overrides go in `src/main/resources/application-local.yaml` (gitignored, auto-merged by Spring).
- **QueryDSL**: `querydsl-jpa` is `implementation` (not `compileOnly`). Q-classes generated to `src/main/generated/`. See [troubleshooting T-016](./claude-docs/troubleshooting.md) before changing this — there's a real reason.

## Architecture

Layered: `Controller → Service → Repository → Entity`. Package layout in [README "아키텍처"](./README.md).

### Code conventions (Claude must internalize)
- Layer packages: `controller / service / repository / entitiy / dto.{request,response} / exception / security / config`. Note `entitiy/` is an intentional historical typo — keep using it.
- Entity primary keys use UUID (`@UuidGenerator` from Hibernate); column name = `<table>_id` (e.g. `post_id`).
- All entities extend `BaseEntity` and inherit `createdAt` / `updatedAt`.
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` on entities; mutation goes through explicit methods (e.g. `Post.update(...)`) so updates work via dirty-checking inside `@Transactional`.
- Request DTOs expose a `toEntity()` method; Response DTOs are built inline in the service via `@Builder`.
- Read-only service methods carry `@Transactional(readOnly = true)`.
- Each domain exception (e.g. `PostNotFoundException`) gets a dedicated `@ExceptionHandler` in `GlobalExceptionHandler` mapping to `ErrorResponseDto { message }` + appropriate HTTP status. Validation (`MethodArgumentNotValidException`), malformed JSON (`HttpMessageNotReadableException`), and missing query params (`MissingServletRequestParameterException`) are already wired — extend that file rather than catching in controllers.

### Auditing
`@EnableJpaAuditing` lives on `JpaAuditingConfig` (not the main class — slice tests would break otherwise). Without it, `BaseEntity` timestamps stop populating.

### Database policy
- `ddl-auto: update` (default in `application.yaml`) — schema is mid-flight. Hibernate adds missing columns/tables on boot; existing data is kept.
- `update` does **not** reliably create indexes/constraints — when those matter, write the DDL manually and document it in the PR.
- Plan: switch to `validate` when Flyway lands (PR 6). Don't change `ddl-auto` casually before then.

## CI/CD note
Trigger table + secrets in [README "배포"](./README.md). One thing worth remembering: CD path on `main` = GitHub Actions → Docker Hub → AWS SSM → EC2 `docker compose` (compose.yaml at repo root, pulled by SSM at deploy). If a new env var is added to compose, **both** GitHub Secrets and EC2 `.env` must be updated.

## Workflow rules (Claude reads this)

These are mandatory, not suggestions.

### Before creating a PR — always check PR state first
The user merges PRs from the GitHub UI, often between Claude's tool calls. If Claude assumes a PR is still open and keeps pushing to its branch / referring to it as open, the result is wasted work and confusing messages.

**Always run this before creating a new PR, or before referencing a PR number in a message:**
```bash
gh pr list --state all --limit 10
```

Then:
- Is the branch you're about to PR from already merged? → branch off latest `dev` instead, don't re-push to a merged branch.
- Is there an open PR you should be adding to rather than creating a new one? → ask the user before splitting.
- The PR number you're about to mention — is it still open or already merged? Phrase accordingly.

### Before starting any new feature/fix work — sync dev
```bash
git fetch origin && git checkout dev && git pull origin dev && git checkout -b <new-branch>
```
Local `dev` is almost always stale because the user merges remotely. Never branch off stale local `dev`.

### Production-impacting changes
Any change to: `application.yaml`, `.gitignore`, `compose.yaml`, `.github/workflows/`, DB schema, or env vars — must be flagged in the PR body with a **"운영 머지 전 필수"** checklist. Don't bury it.

### Before creating any PR — troubleshooting log sweep
For every PR Claude opens, the second-to-last task in the task list must be **"troubleshooting.md 점검"**. Walk through this checklist:

1. **Re-scan the session** — every error you hit during this PR (build failures, test failures, stack traces, "wait that's weird" moments, config traps that bit you). Treat *anything you had to debug for more than a minute* as a candidate.
2. For each candidate, ask:
   - Is it already in `claude-docs/troubleshooting.md`? → skip.
   - Is it project-specific (would bite the next person / next Claude on this codebase)? → **must add as a new T-### entry**.
   - Is it generic / one-off (e.g. you mistyped a command, IDE quirk)? → skip.
3. New entries follow the existing schema: **증상 / 원인 / 해결 / 시점 / 교훈**. Add to the index at the top and link with `<a id="t-NNN"></a>`.
4. **Don't rely on remembering at PR-body time** — by then you've already moved on. The sweep is its own task in the task list, performed before `gh pr create`.

> Why this is a hard rule: in PR #31 the `MissingServletRequestParameterException → 500` finding was captured in plan/api-contracts/PR-body but slipped through troubleshooting.md — and that's exactly the file the next Claude will grep when the same trap fires elsewhere. The fix has to land where future-Claude looks for it.
