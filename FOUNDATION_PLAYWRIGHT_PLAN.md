# Athena — Foundation Playwright Automation Plan

> Companion to `FOUNDATION_TEST_CASES.md`. Decides **which tool owns which test**, the **execution order**, and the **reusable scaffolding** so the next step is writing production Playwright/API/Integration tests with minimal re-analysis.
>
> **Guiding principle:** Playwright is a *browser* tool. Most Foundation checks are headless infrastructure assertions and belong to API or Integration harnesses. We use Playwright's **request context** and **web-first assertions** as a thin, uniform runner for HTTP-level foundation checks, and Playwright's **browser context** only where a real page/CORS/rendering matters. Container lifecycle, DB introspection, Kafka/Redis probing stay in an Integration harness.

---

## 1. Tool Ownership per Test

Legend — **PW-UI** (Playwright browser page), **PW-API** (Playwright `request` fixture, no browser), **API** (any HTTP client — can be Playwright request or REST-assured/curl), **INT** (Integration: docker/compose control, JDBC, Kafka/Redis admin clients).

| Test ID | Title (short) | Owner | Rationale |
|---|---|---|---|
| FND-START-001 | Full stack boots | INT | Needs compose orchestration + container status. |
| FND-START-002 | Startup ordering | INT | Reads container timestamps. |
| FND-START-003 | No fatal startup logs | INT | Log scraping. |
| FND-START-004 | Services READY | PW-API / API | HTTP readiness probe. |
| FND-START-005 | Frontend serves shell | **PW-UI** | Real `ng serve` page load. |
| FND-START-006 | Frontend → gateway reachable | **PW-UI** | Browser CORS + network. |
| FND-START-007 | Gateway boots w/o downstreams | INT | Partial-stack control. |
| FND-HEALTH-001 | Aggregate health UP | PW-API / API | HTTP. |
| FND-HEALTH-002 | Liveness UP | PW-API / API | HTTP. |
| FND-HEALTH-003 | Readiness degraded | INT | Requires stopping Redis. |
| FND-HEALTH-004 | DB-down → db DOWN | INT | Requires stopping DB. |
| FND-HEALTH-005 | Info reachable | PW-API / API | HTTP. |
| FND-HEALTH-006 | Sensitive actuators hidden | PW-API / API | HTTP negative. |
| FND-HEALTH-007 | Health public via gateway | PW-API / API | HTTP. |
| FND-GW-001 | All routes forward | PW-API / API | HTTP matrix (needs token). |
| FND-GW-002 | `/users/*/plans` specificity | PW-API / API | HTTP. |
| FND-GW-003 | `/ai/memory` specificity | PW-API / API | HTTP. |
| FND-GW-004 | No token → 401 | PW-API / API | HTTP. |
| FND-GW-005 | Bad/refresh token → 401 | PW-API / API | HTTP. |
| FND-GW-006 | Downstream down → 503 | INT | Requires stopping a service. |
| FND-GW-007 | Unknown route → 404 | PW-API / API | HTTP. |
| FND-GW-008 | Public auth routes | PW-API / API | HTTP. |
| FND-GW-009 | Identity header stripping | INT | Needs downstream echo/header capture. |
| FND-GW-010 | CORS preflight | **PW-UI** / API | Best asserted from a real browser origin. |
| FND-GW-011 | Gateway routes actuator | PW-API / API | HTTP. |
| FND-EUREKA-001 | All register | PW-API / API | HTTP to `/eureka/apps` (JSON). |
| FND-EUREKA-002 | Discovery not self-registered | PW-API / API | HTTP. |
| FND-EUREKA-003 | Register after READY | INT | Timing correlation. |
| FND-EUREKA-004 | Stay registered | INT | Long poll. |
| FND-EUREKA-005 | Re-register after restart | INT | Container restart. |
| FND-EUREKA-006 | Eureka outage tolerated | INT | Stop discovery. |
| FND-LB-001 | Migrations applied | INT | JDBC into `DATABASECHANGELOG`. |
| FND-LB-002 | Hibernate validate passes | INT | Log/boot assertion. |
| FND-LB-003 | No re-run on reboot | INT | Restart + JDBC. |
| FND-LB-004 | pgvector present | INT | JDBC `pg_extension`. |
| FND-LB-005 | Badge seed loaded | INT | JDBC. |
| FND-LB-006 | Migration failure aborts | INT | Isolated tampered DB. |
| FND-REDIS-001 | Redis connected | PW-API (health) + INT (ping) | Mixed. |
| FND-REDIS-002 | Cache write-through | INT | `redis-cli KEYS`. |
| FND-REDIS-003 | Cache read hit | INT | DB-query/log counting. |
| FND-REDIS-004 | Redis unavailable | INT | Stop Redis. |
| FND-REDIS-005 | user-service Redis-independent | INT | Stop Redis + health. |
| FND-KAFKA-001 | Broker connectivity | INT | Admin client. |
| FND-KAFKA-002 | Producer emits event | INT | Test consumer. |
| FND-KAFKA-003 | Consumer groups | INT | `kafka-consumer-groups`. |
| FND-KAFKA-004 | E2E event flow | INT | Produce + observe side effect. |
| FND-KAFKA-005 | Topics auto-created | INT | Admin list. |
| FND-KAFKA-006 | Broker outage | INT | Stop Kafka. |
| FND-KAFKA-007 | Consumer catch-up | INT | Offset replay. |
| FND-KAFKA-008 | Startup w/o Kafka | INT | Partial stack. |
| FND-CONF-001 | Defaults apply | INT | Local run. |
| FND-CONF-002 | Env overrides YAML | INT | Container introspection. |
| FND-CONF-003 | Shared JWT secret | PW-API / API | Mint at auth, use at gateway. |
| FND-CONF-004 | `.env.example` documents | INT | Static lint. |
| FND-CONF-005 | AI config → LM Studio | INT | Config introspection. |
| FND-CONF-006 | Invalid config fails fast | INT | Isolated. |
| FND-CONF-007 | Frontend apiBase matches | INT (static) / PW-UI (runtime) | Both. |
| FND-ENV-001 | Ports free | INT | Pre-flight. |
| FND-ENV-002 | Docker available | INT | Pre-flight. |
| FND-ENV-003 | Env resolvable | INT | Pre-flight. |
| FND-ENV-004 | Core services reachable | PW-API + INT | HTTP + TCP. |
| FND-ENV-005 | Optional externals absent OK | INT | Boot w/o LM Studio/LocalStack. |
| FND-ENV-006 | Missing dep blocks boot | INT | Isolated. |
| FND-ENV-007 | Frontend prereqs | INT | node/npm. |

### Ownership summary

| Owner | Count | Test IDs |
|---|---|---|
| **PW-UI** (true browser) | 4 | FND-START-005, FND-START-006, FND-GW-010, FND-CONF-007 (runtime) |
| **PW-API / API** (HTTP, Playwright request fixture recommended) | 16 | START-004; HEALTH-001,002,005,006,007; GW-001,002,003,004,005,007,008,011; EUREKA-001,002; CONF-003; ENV-004 (HTTP part) |
| **INT** (compose / JDBC / Kafka / Redis) | 33 | all remaining |

> **Playwright's realistic footprint:** ~4 genuine browser tests + up to ~16 HTTP tests if you standardize on Playwright's `request` context as the HTTP runner. Everything requiring container control, DB, Kafka, or Redis internals is Integration — Playwright cannot and should not do it.

---

## 2. Execution Order (staged gate)

Foundation runs as an ordered pipeline; **a failed stage blocks the next** (fail-fast — no point routing if nothing booted).

```
Stage 0  PRE-FLIGHT (INT)
  FND-ENV-001 ports free · FND-ENV-002 docker · FND-ENV-003 env resolvable · FND-ENV-007 frontend prereqs
        │  (must pass before spending time on boot)
        ▼
Stage 1  BOOT (INT)
  FND-START-001 stack up · FND-START-002 ordering · FND-START-003 no fatal logs
  FND-ENV-005 optional externals absent OK
        ▼
Stage 2  PLATFORM READY (INT + API)
  FND-LB-001..005 migrations · FND-START-004 readiness · FND-HEALTH-001,002,005
  FND-EUREKA-001,002,003 registration · FND-KAFKA-001,003,005 broker/topics · FND-REDIS-001
  FND-CONF-001,002,004,005 config · FND-ENV-004 reachability
        ▼
Stage 3  ROUTING & SECURITY (API/PW-API)
  FND-GW-001,002,003,004,005,007,008,011 · FND-CONF-003 shared secret
  FND-HEALTH-007 health-via-gateway · FND-GW-010 CORS
        ▼
Stage 4  DATA-PLANE BEHAVIOR (INT)
  FND-KAFKA-002,004 event flow · FND-REDIS-002,003 caching
        ▼
Stage 5  FRONTEND (PW-UI)
  FND-START-005 shell loads · FND-START-006 gateway reachable · FND-CONF-007 apiBase runtime
        ▼
Stage 6  RESILIENCE / NEGATIVE (INT, run last — they perturb the stack)
  FND-HEALTH-003,004 degrade · FND-GW-006 downstream down · FND-GW-009 header spoof
  FND-EUREKA-004,005,006 · FND-REDIS-004,005 · FND-KAFKA-006,007,008
  FND-LB-003 reboot-idempotent · FND-LB-006 · FND-CONF-006 · FND-ENV-006
```

**Rules**
- Stages 0–2 are hard gates for the whole suite. If Stage 1 fails, abort — feature E2E must not run.
- Stage 6 (resilience) mutates the running stack (stops Redis/Kafka/services). It runs **after** all positive checks and must **restore** state in teardown; ideally against a disposable stack instance.
- Within Stage 3, obtain the auth token **once** (see fixtures) and reuse across the routing matrix.

---

## 3. Reusable Fixtures

### 3.1 Playwright project layout (proposed)

```
foundation-tests/
  playwright.config.ts        # projects: "pre-flight", "api", "ui"; global setup/teardown
  fixtures/
    stack.fixture.ts          # compose up/down (globalSetup) — INT
    endpoints.ts              # service base URLs & ports (single source, mirrors §0.1)
    auth.fixture.ts           # register+login → { accessToken, refreshToken, userId }
    apiClient.fixture.ts      # Playwright request context w/ baseURL=gateway + auth header
    infra.fixture.ts          # pg (JDBC/pg client), redis client, kafka admin — INT helpers
  tests/
    preflight/  boot/  platform/  routing/  dataplane/  frontend/  resilience/
```

### 3.2 Core fixtures

| Fixture | Provides | Used by | Scope |
|---|---|---|---|
| **`stack`** | `docker compose up --build --wait` in `globalSetup`; teardown `down -v`. Exposes readiness gate (waits on healthchecks). | all | global (session) |
| **`endpoints`** | Constant map: `{ gateway:8080, eureka:8761, auth:8081, ... , frontend:4200, redis:6379, kafka:29092, dbPorts:{...} }`. Mirrors `FOUNDATION_TEST_CASES.md §0`. | all | static |
| **`authToken`** | Registers a throwaway user via `/auth/register`, logs in via `/auth/login`, returns access + refresh tokens + userId. Also mints an **expired** token and exposes the **refresh** token for negative cases. | Stage 3 routing | worker |
| **`apiClient`** | Playwright `request.newContext({ baseURL: gateway, extraHTTPHeaders: { Authorization: Bearer <token> } })`; plus an **anonymous** variant (no auth) for public/negative tests. | HTTP tests | worker |
| **`db`** | Per-service Postgres client keyed by service name → host port (5433–5440). Helpers: `changelogCount(svc)`, `hasExtension(svc,'vector')`, `rowCount(svc,table)`. | Liquibase, Redis-cache verification | test |
| **`kafkaAdmin`** | Kafka AdminClient + throwaway consumer/producer on `localhost:29092`. Helpers: `listTopics()`, `describeGroup(id)`, `consumeOne(topic, timeout)`. | Kafka tests | test |
| **`redis`** | Redis client on `localhost:6379`. Helpers: `ping()`, `keys(pattern)`, `flushForTest()`. | Redis tests | test |
| **`eureka`** | Typed client for `/eureka/apps` (JSON). Helper: `registeredApps(): Set<string>`. | Eureka tests | test |
| **`serviceControl`** | Wraps `docker compose stop/start/restart <svc>` + re-wait-healthy. **Resilience only.** Auto-restore in teardown. | Stage 6 | test (isolated) |

### 3.3 Frontend (PW-UI) fixture
- `frontendServer`: starts `ng serve` (or expects it on 4200 via `webServer` in `playwright.config`), waits for `http://localhost:4200` to return 200.
- Reuse Playwright's built-in `webServer` config to own the `ng serve` lifecycle for the `ui` project only.

---

## 4. Setup / Teardown Strategy

- **Global setup (once):** `stack` fixture runs `docker compose up --build --wait` on a clean state (`down -v` first for deterministic Liquibase counts). Gate the whole suite on all healthchecks green. Capture container start timestamps here for FND-START-002.
- **Global teardown:** `docker compose down -v` (drop volumes so the next run re-migrates from zero). Collect `docker compose logs` as CI artifacts on failure.
- **Worker setup:** `authToken` + `apiClient` created once per worker and reused across the routing matrix (avoid re-registering per test).
- **Test-level teardown for resilience (Stage 6):** each `serviceControl`/`kafkaAdmin`/`redis` perturbation MUST restore the stopped component and wait for health before the test ends. Prefer running Stage 6 as a **separate Playwright project** (or a dedicated disposable compose instance) so a mid-test crash cannot corrupt Stages 0–5 results.
- **Isolation for negative-migration/config tests (FND-LB-006, FND-CONF-006, FND-ENV-006):** never mutate the real changelogs or the shared stack. Spin an **ephemeral single-service + single-DB** compose (or Testcontainers) with the deliberately-broken input, assert the failure, dispose. Keep entirely out of the main stack.
- **Idempotency test (FND-LB-003):** must run **without** a preceding `down -v` — it asserts a *restart* on an already-migrated volume applies 0 changesets. Sequence it before global teardown, after Stage 5.

---

## 5. Dependencies Between Tests

| This test | Depends on | Why |
|---|---|---|
| Everything | FND-START-001 (stack up) | Nothing runs on a dead stack. |
| All routing (GW-*) | FND-EUREKA-001 | `lb://` needs services registered. |
| GW-001..003, CONF-003 | `authToken` fixture | Protected routes need a valid token. |
| FND-GW-005 (bad/refresh token) | `authToken` (refresh + expired variants) | Needs real tokens to negate. |
| FND-KAFKA-002/004 | FND-KAFKA-005 (topics exist), FND-START-001 | Can't consume from absent topics. |
| FND-REDIS-002/003 | FND-REDIS-001 (connected) | Cache needs live Redis. |
| FND-LB-002 | FND-LB-001 | validate assumes migrations ran. |
| FND-LB-003 | a completed migrated run | Idempotency needs prior state. |
| Stage 6 (all) | Stages 0–5 green | Perturbation only meaningful on a proven-good baseline. |
| FND-START-006, CONF-007(runtime) | FND-START-005 (frontend up) + gateway up | Browser needs both ends. |

**Independent / parallelizable** (share only the read-only stack): HEALTH-001/002/005, EUREKA-001/002, GW read-only route checks, KAFKA-003/005 listing. Run these concurrently within their stage. Serialize anything that stops a container.

---

## 6. What Playwright will NOT do (explicit non-goals)

- Container lifecycle, log scraping, port pre-flight → Integration/shell.
- JDBC / `DATABASECHANGELOG` / `pg_extension` inspection → Integration (DB client).
- Kafka admin, consumer-group describe, offset replay → Integration (Kafka clients).
- Redis key inspection / cache-hit counting → Integration (Redis client + DB query counting).
- Stopping/starting infra for degraded-state tests → Integration (compose control).

Playwright earns its place on: the **4 UI/CORS tests** and, optionally, as the **uniform HTTP runner** for the ~16 API-level checks (using its `request` context) so the whole foundation suite lives in one report. If you prefer, those 16 can instead run in a JVM API harness (REST-assured) co-located with the Integration tests — either is valid; pick one runner and keep it consistent.

---

## 7. Ready-to-implement checklist

1. Create `foundation-tests/` with the fixture layout in §3.1.
2. Encode `endpoints.ts` from `FOUNDATION_TEST_CASES.md §0.1–0.2` (single source of truth).
3. Implement `stack` global setup with `--wait` + clean `down -v`.
4. Implement `authToken` (register→login) + expired/refresh token variants.
5. Wire Stages 0→6 as ordered Playwright projects with `dependencies`.
6. Add JDBC/Kafka/Redis integration helpers (Java Testcontainers **or** Node clients — align with the team's existing test stack; the backend is JVM/Maven, so a Failsafe + Testcontainers integration module is the natural home for all INT tests, with Playwright owning only Stage 3 HTTP + Stage 5 UI).
7. Gate feature E2E on a green Foundation run.

> **Recommended split for this repo:** Given the backend is Maven/JVM with an existing test setup, put **all INT tests** in a Maven `foundation-it` module (Failsafe + Testcontainers reusing `docker-compose.yml`), and use **Playwright (TS)** only for Stage 5 UI + Stage 3 HTTP/CORS. This keeps each check in the tool that runs it most naturally and avoids forcing infra assertions through a browser tool.
