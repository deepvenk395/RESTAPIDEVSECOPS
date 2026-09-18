# TaskFlow REST API

An industry-grade **Spring Boot 3** REST API built as a DevSecOps practice target.
It comes with real-world security, observability, CI/CD, and container hardening so you can
learn how a production application is built *and* defended.

## Tech Stack

| Area                | Choice                                        |
| ------------------- | --------------------------------------------- |
| Language / runtime  | Java 17 (Temurin), Spring Boot 3.4            |
| Build               | Maven (with wrapper), Maven Enforcer          |
| Persistence         | Spring Data JPA + Flyway migrations           |
| Database            | H2 (dev/test), PostgreSQL 16 (prod)           |
| AuthN/AuthZ         | Spring Security + stateless JWT (HS256)       |
| Validation          | Jakarta Bean Validation                       |
| API docs            | springdoc-openapi (Swagger UI)                |
| Observability       | Spring Actuator, request-id MDC logging       |
| Packaging           | Multi-stage Dockerfile (non-root user)        |
| CI / Security       | GitHub Actions: CI + SAST/SCA/secret/container scans |

## Quick Start (local, fastest)

Requires only **JDK 17**. The Maven wrapper downloads Maven itself.

```powershell
# Windows
.\mvnw.cmd -B clean verify          # build + all tests + coverage + SBOM

# Linux/macOS (make the wrapper executable once: chmod +x mvnw)
sh mvnw -B clean verify

# Run in dev mode (in-memory H2, seeded, auto-created admin)
.\mvnw.cmd spring-boot:run
# or
java -jar target\taskflow-api-0.1.0-SNAPSHOT.jar
```

App starts on **http://localhost:9000**. Dev seed creates:
- admin / `TaskFlow@Dev987` (role `ROLE_ADMIN`) — **dev only, in-memory**
- a demo user (demo / `DemoUser@1234`) and a few sample tasks

### Interact

- Swagger UI: http://localhost:9000/swagger-ui.html
- API docs (JSON): http://localhost:9000/v3/api-docs
- Health: http://localhost:9000/actuator/health

```bash
# 1. Register a user
curl -s -X POST localhost:9000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"Str0ng-Enough","fullName":"Alice Wonder"}'

# 2. Login (username or email) -> grab "token"
curl -s -X POST localhost:9000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"alice","password":"Str0ng-Enough"}'

# 3. Create a task with the token
curl -s -X POST localhost:9000/api/v1/tasks \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"title":"First task","status":"TODO","priority":"HIGH","dueDate":"2026-12-01"}'

# 4. List your tasks (paginated + filters)
curl -s "localhost:9000/api/v1/tasks?size=20&status=TODO&sortBy=dueDate&sortDirection=desc" \
  -H "Authorization: Bearer <TOKEN>"
```

## Run the full stack (Docker + PostgreSQL, prod profile)

```bash
# 1. Prepare environment
copy .env.example .env          # Windows
# then edit .env with strong secrets
#   openssl rand -base64 48   -> JWT_SECRET

# 2. Start (db healthcheck gates the api)
docker compose up --build -d
```

> The `api` service refuses to start without `POSTGRES_PASSWORD` and `JWT_SECRET`.
> The app also refuses to start in the `prod` profile with the default dev JWT secret.
> Set `SEED_ENABLED=true` once to create the initial admin (password >= 12 chars).

## API Overview

All endpoints are versioned under `/api/v1`. Authenticated with
`Authorization: Bearer <JWT>`. Errors are consistent `application/json` error bodies
(`ApiError`) with a `see fieldErrors` structure for validation failures.

| Method | Path                          | Access      | Description                       |
| ------ | ----------------------------- | ----------- | --------------------------------- |
| POST   | `/api/v1/auth/register`       | public      | Register (returns JWT)            |
| POST   | `/api/v1/auth/login`          | public      | Login (JWT)                       |
| GET    | `/api/v1/auth/me`             | any         | Current user profile              |
| GET    | `/api/v1/tasks`               | auth        | Paginated/filtered list           |
| POST   | `/api/v1/tasks`               | auth        | Create task                       |
| GET    | `/api/v1/tasks/{id}`          | owner/assignee/admin | Get task                |
| PUT    | `/api/v1/tasks/{id}`          | creator/admin | Partial update                  |
| PATCH  | `/api/v1/tasks/{id}/status`   | creator/assignee/admin | Change status           |
| POST   | `/api/v1/tasks/{id}/assign`   | creator/admin | Assign user                    |
| DELETE | `/api/v1/tasks/{id}/assign`   | creator/admin | Unassign                        |
| DELETE | `/api/v1/tasks/{id}`          | creator/admin | Delete                           |
| GET    | `/api/v1/admin/users`         | admin        | List users (paginated)            |
| PATCH  | `/api/v1/admin/users/{id}`    | admin        | Update role/enabled/email/name    |
| DELETE | `/api/v1/admin/users/{id}`    | admin        | Delete user (self-delete guarded) |

**Security notes**
- Users only ever see/act on tasks they created or are assigned to; foreign tasks return `404`
  (not `403`) to avoid resource enumeration.
- `BCrypt` (strength 12) for password hashing; JWT secret must be ≥ 256 bit.
- Request IDs (`X-Request-Id`) are correlated in logs via MDC; auth headers are never logged.
- Admin endpoint rule is enforced both by URL matcher and per-resource ownership checks.

## Configuration (12-factor)

All secrets/toggles are environment variables (`SPRING_PROFILES_ACTIVE=dev|prod`).

| Variable                | Default / Purpose                                    |
| ----------------------- | ---------------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`| `dev` (H2) or `prod` (PostgreSQL)                    |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Datasource (prod)                     |
| `JWT_SECRET`            | Dev default; ≥32 bytes; required & fail-fast in prod |
| `JWT_EXPIRATION_SECONDS`| `3600` (dev), `900` (prod default)                   |
| `CORS_ALLOWED_ORIGINS`  | comma-separated origins (default `http://localhost:3000`) |
| `SEED_ENABLED`          | create initial admin when `true`                     |
| `SEED_ADMIN_USERNAME` / `SEED_ADMIN_PASSWORD` | initial admin credentials           |

## Project Layout

```
src/main/java/com/devsecops/taskflow
├── config/       # properties, security beans wiring, OpenAPI, seed data
├── controller/   # REST endpoints (auth, tasks, admin)
├── dto/          # request/response records (+ bean validation)
├── domain/       # JPA entities + enums
├── exception/    # domain exceptions + global error handler
├── repository/   # Spring Data repositories + JPA Specifications
├── security/     # JWT, filters, authentication entry points
├── service/      # business logic (auth, user, task)
└── web/          # request-id logging filter
src/test/         # unit + MockMvc integration tests (H2, test profile)
src/main/resources/db/migration/   # Flyway migrations (own the schema)
```

## What's already "DevSecOps-grade" here

| Practice | Where |
| -------- | ----- |
| CI with tests, coverage, SBOM | `.github/workflows/ci.yml` (JaCoCo + CycloneDX) |
| SAST                    | CodeQL (`.github/workflows/security.yml`) |
| SCA / Dependencies      | OWASP Dependency-Check + dependency-review-action |
| Secret scanning         | Gitleaks over full history |
| Container scanning      | Trivy (filesystem + built image) with fail-gate on CRITICAL/HIGH |
| Dependency updates      | `dependabot.yml` |
| Image hardening         | `Dockerfile`: multi-stage, non-root `appuser`, healthcheck, pinned runners |
| Secrets in pipeline     | only via GitHub Secrets / `.env` (git-ignored) |
| Fail-fast config        | prod refuses default JWT secret; weak-seed guard |
| Least-privilege         | role-based endpoints + per-resource ownership checks |

## Practice ideas (do these next)

1. **Hunt the injected bugs.** This repo ships with **16 intentionally planted defects**
   (authz escalation, credential logging, weak CORS/actuator config, destructive updates,
   unbounded pagination, root container, and more) plus ~10 design/behavior gaps a
   first-release build would have (case-sensitive logins, TOCTOU registration deadlock,
   dead config, etc.). The tests still pass — like a real codebase. Find them with
   Semgrep/CodeQL, manual API testing, log review, and config review, then report them
   here. When you're done, or to self-check, read
   `docs/intentional-bugs.md` (open it only when you want the solutions).
2. **Break it, then fix it.** Push a "vulnerable" branch (e.g. log a password, hardcode a
   secret, disable JWT validation) and watch Gitleaks / Semgrep / CodeQL / test failures gate it.
3. **Add rate limiting** on `/auth/login` (bucket4j) to demonstrate brute-force defense.
4. **Add refresh-token rotation** with revocation list.
5. **Customize CodeQL** queries and add a Semgrep OWASP-Top-10 job.
6. **Add a DAST job** (OWASP ZAP full-scan against a spun-up container) in CI.
7. **Add E2E contract tests** (Spring Cloud Contract / Pact) and publish coverage to Codecov.
8. **Deploy** with GitHub Actions + a Kubernetes/Helm manifest, expose metrics (Prometheus),
   and wire health probes to your orchestrator.
9. **Threat-model it**: write an ADR for the auth flow, document trust boundaries, and add
   an incident response runbook.

## Requirements

- JDK 17+ (local builds); Docker 24+/Compose v2 for the full stack.
- Git: to keep the Maven wrapper executable on Linux/macOS, run
  `git add --chmod=+x mvnw` once, and `git rm --cached mvnw` first if already staged.

## License

MIT — see `LICENSE` if present; otherwise use it freely for learning.