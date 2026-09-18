# Intentional Bugs — SPOILERS

> Do **not** read this file until you have finished hunting, searching, code-reviewing
> and triaging scans. It exists so you can verify your findings and self-assess.

Every bug below is a plausible "regular AI-written" defect. Each one is currently
**silent**: the tests are green, the build is green, and scanners that run in CI may or
may not flag them — exactly like a real codebase.

Legend:
- `[AUTHZ]` access control / privilege issues
- `[LOGGING]` information leakage via logs
- `[CONFIG]` security configuration / hardening
- `[CRYPTO]` secrets / key management
- `[FUNCTIONAL]` behavior bugs
- `[DoS]` resource exhaustion
- `[INFRA]` container / orchestration

| # | Cat         | Location                          | Finding |
| - | ----------- | --------------------------------- | ------- |
| 1 | `AUTHZ`     | `RegisterRequest` + `AuthService.register` | API accepts a client-supplied `role`; registering with `"role":"ROLE_ADMIN"` grants admin. Fix: always assign `ROLE_USER`. |
| 3 | `AUTHZ`     | `TaskService.assign` / `.unassign` | Reassignment gates on `canView` only, so an *assignee* can reassign/unassign tasks. Fix: require `canManage`. |
| 2 | `LOGGING`   | `AuthService.login`                | Logs plaintext password (`log.trace`) each attempt. Fix: never log credentials. |
| 4 | `LOGGING`   | `RequestLoggingFilter`             | Logs full `Authorization: Bearer <JWT>` header at DEBUG. Fix: redact auth headers. |
| 5 | `LOGGING`   | `DataInitializer`                  | Logs seeded admin password in plaintext. Fix: log username only. |
| 6 | `CONFIG`    | `GlobalExceptionHandler.handleGeneric` | 500 body returns `ex.getMessage()`, leaking internals. Fix: generic message. |
| 7 | `CRYPTO`    | `JwtService`                       | Prod fail-fast on default dev JWT secret removed; prod will sign with the known secret. Fix: re-add guard. |
| 8 | `CONFIG`    | `SecurityConfig.cors`              | CORS `*` origin with `allowCredentials(true)`. Fix: restrict to `app.cors.allowed-origins`. |
| 9 | `CONFIG`    | `SecurityConfig` + `application.yml` | All Actuator endpoints exposed + `permitAll` + `show-details: always`. Fix: expose `health,info`, require auth. |
| 10 | `CONFIG`   | `docker-compose.yml`               | `JWT_SECRET` falls back to a known default instead of failing when unset. Fix: `${JWT_SECRET:?}`. |
| 11 | `FUNCTIONAL` | `TaskService.update`             | `PUT` without `description` wipes stored description to `null`. Fix: apply non-null fields only. |
| 12 | `DoS`       | `TaskService.search`               | Page `size` unbounded (`size=9999999` loads everything). Fix: enforce max (e.g. 100). |
| 13 | `AUTHZ/HA`  | `UserService.delete`               | Self-delete + last-admin guards removed; admin can lock everyone out. Fix: re-add both. |
| 14 | `INFRA`     | `Dockerfile`                       | Runtime stage runs as `root`. Fix: add non-root `USER` (e.g. uid 10001). |
| 15 | `FUNCTIONAL` | `TaskService.update`             | `PUT` without `dueDate` wipes the stored date to `null` (same copy-paste flaw as #11). Fix: guard `!= null` before setting. |
| 16 | `FUNCTIONAL` | `SortUtils.from` + `TaskController` | Default sort field is `"id"` but the task whitelist doesn't include it — `GET /api/v1/tasks` with no `sortBy` 400s. Tests always pass an explicit `sortBy` so it went unnoticed until live testing. Fix: include `id` in the whitelist or default to a listed field. |

---

## Fresh-build review: senior-reviewer findings

These aren't code bugs you can inject — they're design/behavior gaps a senior
reviewer would catch on a real first release. No source changes needed; the code
already has the issue.

| Cat         | Location                              | Finding |
| ----------- | ------------------------------------- | ------- |
| AUTHZ/AVAIL | `AuthService.register`                | TOCTOU race: `existsByUsername` then `save` — concurrent identical registrations both succeed. Fix: handle `DataIntegrityViolationException`. |
| AUTHZ       | `AuthService.login` / `UserService.update` | Login identifiers (username/email) are **case-sensitive**; registration normalizes to lowercase but login/update don't. Fix: normalize on login and update. |
| AVAIL       | `TaskService.assign`                  | Assigning tasks to disabled accounts is allowed. Fix: check `assignee.isEnabled()`. |
| AVAIL       | `TaskService.changeStatus`            | No state-transition validation (e.g. DONE→TODO allowed). Fix: enforce valid transitions. |
| VALIDATION  | `RegisterRequest` / `TaskCreateRequest` | No password-complexity requirement (only length ≥8); no `dueDate` future-past validation. Fix: add `@Pattern` + `@FutureOrPresent`. |
| DESIGN      | `TaskController.update`               | Uses `@PutMapping` for partial updates; should be `@PatchMapping`. Fix: change verb. |
| CONFIG      | `SecurityConfig` + `app.cors.allowed-origins` | `allowedOrigins` env var is defined but never read (CORS uses `*` hardcoded). Dead config. Fix: wire it in or remove. |
| CONFIG      | `application.yml` → `server.servlet.session` | Session timeout set but app is stateless (JWT) — dead config. Remove. |
| CONFIG      | `docker-compose.yml` → `SPRING_PROFILES_ACTIVE` | Falls back to `dev` if unset; H2 + admin password would run in prod. Fix: `${SPRING_PROFILES_ACTIVE:?}`. |
| DEPENDENCY  | `Dockerfile` + `pom.xml`              | H2 driver bundled at runtime scope in prod image. Fix: scope as `<scope>test</scope>` only. |

---

## Scoresheet

| # | Found it? | Tool that would catch it |
| - | --------- | ------------------------ |
| 1 | ☐ | Manual API test / custom DAST |
| 2 | ☐ | Semgrep (`java.lang.security.audit.*`) / manual log review |
| 3 | ☐ | Manual API test (attach → reassign as assignee) |
| 4 | ☐ | Semgrep / manual log review |
| 5 | ☐ | Manual log review |
| 6 | ☐ | Manual error-path test |
| 7 | ☐ | Config review / manual run comparison |
| 8 | ☐ | ZAP / Nuclei CORS check / manual OPTIONS request |
| 9 | ☐ | `curl /actuator/env` unauthenticated |
| 10 | ☐ | `docker compose config` review |
| 11 | ☐ | API regression test (PUT without description) |
| 12 | ☐ | Load / fuzz test |
| 13 | ☐ | Manual admin-console test |
| 14 | ☐ | Trivy misconfig / container UID check |
| 15 | ☐ | API regression test (PUT without dueDate) |
| 16 | ☐ | Manual API smoke test (`GET /api/v1/tasks`) |

## Reverting to a hardened baseline

To turn this into a clean production app, apply the fixes in the tables above in order
#1 → #16, re-running `.\mvnw.cmd -B verify` (and the pipelines if CI is configured) after
each one. Each fix is a single, well-scoped change.