# Athena — Foundation Test Cases

> **Scope:** Environment / infrastructure health verification that must pass **before** any feature-level E2E test runs.
> **Source of truth:** Derived by inspecting `docker-compose.yml`, every `application.yml`, the gateway `JwtAuthenticationFilter`, Liquibase changelogs, Kafka topic/listener definitions, and the Angular frontend config. No features were invented.
> **Do not** treat these as feature tests — they assert the *platform* is up, wired, and consistent.

---

## 0. System Under Test — Verified Facts

### 0.1 Components & host ports

| Component | Type | Host port | Internal addr | Notes |
|---|---|---|---|---|
| discovery-server | Eureka registry | 8761 | discovery-server:8761 | `register-with-eureka=false`, self-preservation disabled |
| api-gateway | Spring Cloud Gateway (WebFlux) | 8080 | api-gateway:8080 | **Only** externally-called port; JWT filter |
| auth-service | Business (Servlet) | 8081 | auth-service:8081 | Postgres + Kafka + S3 (LocalStack) |
| user-service | Business | 8082 | user-service:8082 | Postgres only |
| progress-service | Business | 8083 | progress-service:8083 | Postgres + Kafka + Redis |
| learning-service | Business | 8084 | learning-service:8084 | Postgres + Kafka |
| badge-service | Business | 8085 | badge-service:8085 | Postgres + Kafka + Redis |
| ai-service | Business | 8086 | ai-service:8086 | Postgres + Kafka + Redis + LM Studio |
| interview-service | Business | 8087 | interview-service:8087 | Postgres + Kafka |
| rag-service | Business | 8088 | rag-service:8088 | pgvector Postgres + Kafka + Redis + LM Studio (chat+embeddings) |
| frontend | Angular 20 (ng serve) | 4200 | — | `apiBase = http://localhost:8080` |

### 0.2 Infrastructure

| Infra | Image | Host port | Internal | Consumers |
|---|---|---|---|---|
| Kafka | apache/kafka:3.9.1 (KRaft, single node) | 29092 | kafka:9092 | all business services except user-service |
| Redis | redis:7-alpine | 6379 | redis:6379 | progress, badge, ai, rag (Spring Cache) |
| auth-db | postgres:17-alpine | 5433 | auth-db:5432 | auth-service (`athena_auth`) |
| user-db | postgres:17-alpine | 5434 | user-db:5432 | user-service (`athena_user`) |
| progress-db | postgres:17-alpine | 5435 | progress-db:5432 | progress-service (`athena_progress`) |
| learning-db | postgres:17-alpine | 5436 | learning-db:5432 | learning-service (`athena_learning`) |
| badge-db | postgres:17-alpine | 5437 | badge-db:5432 | badge-service (`athena_badge`) |
| ai-db | postgres:17-alpine | 5438 | ai-db:5432 | ai-service (`athena_ai`) |
| interview-db | postgres:17-alpine | 5439 | interview-db:5432 | interview-service (`athena_interview`) |
| rag-db | pgvector/pgvector:pg17 | 5440 | rag-db:5432 | rag-service (`athena_rag`, `vector` extension) |
| LM Studio | external (host) | 1234 | host.docker.internal:1234 | ai-service, rag-service — **not** part of compose |
| LocalStack | external (host, optional) | 4566 | host.docker.internal:4566 | CloudWatch logs + auth S3 — **not** part of compose |

Each business service owns its **own** Postgres instance (no shared DB).

### 0.3 Compose startup dependency graph (verified from `depends_on`)

- **discovery-server, kafka, redis, all *-db** → started first (dbs/kafka gated by `service_healthy`, redis/discovery by `service_started`).
- **auth-service** ← auth-db (healthy), discovery (started), kafka (healthy).
- **user-service** ← user-db (healthy), discovery (started). *(No Kafka, no Redis.)*
- **progress / badge / ai / rag** ← own db (healthy), discovery (started), kafka (healthy), redis (started).
- **learning / interview** ← own db (healthy), discovery (started), kafka (healthy).
- **api-gateway** ← discovery (started) **only** (does not wait for business services; resolves them lazily via Eureka + `lb://`).

### 0.4 Gateway routing table (verified, order-sensitive)

| # | Route id | Predicate `Path=` | Target `lb://` |
|---|---|---|---|
| 1 | auth-service | `/auth/**` | auth-service |
| 2 | account-service | `/account/**` | auth-service |
| 3 | user-plans | `/users/*/plans` | learning-service |
| 4 | user-badges | `/users/*/badges` | badge-service |
| 5 | user-service | `/users/**` | user-service |
| 6 | progress-service | `/progress/**` | progress-service |
| 7 | learning-plans | `/plans/**` | learning-service |
| 8 | learning-tasks | `/tasks/**` | learning-service |
| 9 | learning-sessions | `/sessions/**` | learning-service |
| 10 | badge-service | `/badges/**` | badge-service |
| 11 | rag-memory | `/ai/memory/**` | rag-service |
| 12 | rag-service | `/rag/**` | rag-service |
| 13 | ai-service | `/ai/**` | ai-service |
| 14 | learning-sessions *(dup id)* | `/learning-sessions/**` | ai-service |
| 15 | daily-journey | `/daily-journey/**` | ai-service |
| 16 | interview-service | `/interviews/**` | interview-service |

**Order-critical pairs** (a broader route must not shadow a narrower one):
- `/users/*/plans` & `/users/*/badges` are declared **before** `/users/**`.
- `/ai/memory/**` is declared **before** `/ai/**`.
- Duplicate route id `learning-sessions` exists (routes #9 and #14) — different paths, so functionally fine but a config smell worth a regression assertion.

### 0.5 Gateway security (verified from `JwtAuthenticationFilter`)

- Public path prefixes: `/auth/`, `/actuator/` (plus exact `/actuator`). All `OPTIONS` requests pass (CORS preflight).
- Any other path **requires** `Authorization: Bearer <access-token>`; missing/malformed → `401` JSON `{"status":401,"error":"Unauthorized",...}`.
- Valid access token → gateway injects `X-User-Id` / `X-User-Roles` headers downstream.
- On public routes the filter **strips** inbound `X-User-Id`/`X-User-Roles` (anti-spoofing).
- Token type must be `ACCESS` (refresh tokens rejected); issuer `athena-auth`; HMAC secret from `ATHENA_JWT_SECRET`.
- Public auth endpoints: `POST /auth/register` (multipart), `POST /auth/login`, `POST /auth/refresh`.

### 0.6 Health endpoints (verified)

- Every service exposes `management.endpoints.web.exposure.include: health,info` (gateway also `gateway`).
- `management.endpoint.health.probes.enabled: true` → `/actuator/health/liveness` and `/actuator/health/readiness` available.
- Health composite auto-includes: `ping`, `diskSpace`, `db` (all business services), `redis` (progress/badge/ai/rag), `kafka` where the binder is present, `discoveryComposite`/`eureka`.
- Gateway adds read-only `/actuator/gateway/routes`.

### 0.7 Liquibase (verified)

- Every business service: `spring.liquibase.change-log: classpath:db/changelog/db.changelog-master.yaml`, `spring.jpa.hibernate.ddl-auto: validate` (Hibernate validates schema against entities — a failed migration or drift aborts startup).
- Changeset counts: auth 7, user 2, progress 2, learning 1, badge 2 (incl. seed), ai 6, interview 1, rag 4 (incl. `CREATE EXTENSION vector`).

### 0.8 Kafka topics (verified in `KafkaTopics`)

`athena.task.completed`, `athena.plan.created`, `athena.streak.updated`, `athena.badge.awarded`, `athena.user.registered`, `athena.roadmap.generated`, `athena.interview.evaluated`, `athena.learning.session.completed`, `athena.badge.suggestion.generated`, `athena.daily.block.completed`, `athena.daily.checkin.recorded`, `athena.memory.document.indexed`, and ~20 more `athena.*` topics. Producers declare `NewTopic` beans (auto-create); consumers use `@KafkaListener(groupId = "<service-name>")`.

---

## 1. Application Startup

### FND-START-001 — Full stack boots via `docker compose up`
- **Priority:** Critical
- **Preconditions:** Docker running; ports in §0.1–0.2 free; `.env` present (or defaults). LM Studio/LocalStack may be absent.
- **Test Steps:**
  1. `docker compose up --build -d`.
  2. Poll `docker compose ps` until all containers report `running`/`healthy` or 5 min timeout.
- **Expected Result:** All infra containers reach `healthy`; all 10 backend service containers reach `running`; no container in `restarting`/`exited`.
- **Failure Conditions:** Any container `exited`, stuck `restarting`, or health never green within timeout.
- **Can be Automated?** Yes
- **Tool:** Integration (compose orchestration / testcontainers or shell), **not** Playwright.

### FND-START-002 — Startup ordering honored (dependencies before dependents)
- **Priority:** High
- **Preconditions:** Clean stack (no prior volumes).
- **Test Steps:**
  1. `docker compose up -d`.
  2. Capture container start timestamps (`docker inspect -f '{{.State.StartedAt}}'`).
  3. Compare each business service start time vs its declared `depends_on` targets.
- **Expected Result:** Each service starts only after its DB is `healthy`, Kafka `healthy` (where required), and discovery started. Gateway starts after discovery.
- **Failure Conditions:** A service starts before a `service_healthy` dependency is green.
- **Can be Automated?** Yes
- **Tool:** Integration.

### FND-START-003 — No fatal exceptions in startup logs
- **Priority:** Critical
- **Preconditions:** Stack started.
- **Test Steps:**
  1. For each service, `docker compose logs <svc>`.
  2. Grep for `APPLICATION FAILED TO START`, `BeanCreationException`, `UnsatisfiedDependency`, `Liquibase` errors, `Connection refused` that do not recover.
- **Expected Result:** Each service logs `Started <App> in N seconds` / `Tomcat started` / `Netty started`; no unrecovered fatal exception.
- **Failure Conditions:** Any `APPLICATION FAILED TO START` or unrecovered stack trace.
- **Can be Automated?** Yes
- **Tool:** Integration.

### FND-START-004 — Each service reaches READY (readiness probe UP)
- **Priority:** Critical
- **Preconditions:** Stack started.
- **Test Steps:** For each internal service `GET /actuator/health/readiness` (via docker exec or mapped port).
- **Expected Result:** `200` with `{"status":"UP"}`.
- **Failure Conditions:** `OUT_OF_SERVICE`/`DOWN` after start window, or `503`.
- **Can be Automated?** Yes
- **Tool:** API.

### FND-START-005 — Frontend dev server starts and serves the shell
- **Priority:** High
- **Preconditions:** Node/Angular installed; backend gateway optional for this check.
- **Test Steps:**
  1. `npm ci` then `npm start` (`ng serve`) in `Athena-Frontend`.
  2. Wait for `Compiled successfully` / `Local: http://localhost:4200`.
  3. `GET http://localhost:4200`.
- **Expected Result:** `200`, HTML contains the app root element; no compilation errors.
- **Failure Conditions:** ng build error, port 4200 unavailable, blank/500 response.
- **Can be Automated?** Yes
- **Tool:** Playwright (page load assertion) + shell for server lifecycle.

### FND-START-006 — Frontend can reach the gateway (`apiBase`)
- **Priority:** High
- **Preconditions:** Frontend on 4200, gateway on 8080.
- **Test Steps:** Load app; assert app issues requests to `http://localhost:8080`; confirm no CORS error in console for a public call.
- **Expected Result:** Public request (e.g. preflight to `/auth/login`) succeeds; CORS headers present for origin `http://localhost:4200`.
- **Failure Conditions:** CORS blocked, connection refused, wrong base URL.
- **Can be Automated?** Yes
- **Tool:** Playwright.

### FND-START-007 — Gateway starts without business services present
- **Priority:** Medium
- **Preconditions:** Only discovery + gateway running.
- **Test Steps:** Start discovery + gateway only; `GET /actuator/health`.
- **Expected Result:** Gateway is UP (it depends only on discovery). Routes to missing services return `503` (see FND-GW-006), but the gateway itself does not fail to start.
- **Failure Conditions:** Gateway fails to start when downstreams absent.
- **Can be Automated?** Yes
- **Tool:** Integration.

---

## 2. Health Endpoints

### FND-HEALTH-001 — Aggregate health UP for every service
- **Priority:** Critical
- **Preconditions:** Full stack healthy.
- **Test Steps:** `GET /actuator/health` on each service (8080–8088, 8761).
- **Expected Result:** `200`, body `{"status":"UP", ...}` with component sub-statuses UP.
- **Failure Conditions:** `503`/`DOWN`, or a component (`db`,`redis`,`kafka`) reporting DOWN.
- **Can be Automated?** Yes
- **Tool:** API.

### FND-HEALTH-002 — Liveness probe UP
- **Priority:** High
- **Preconditions:** Stack up.
- **Test Steps:** `GET /actuator/health/liveness` per service.
- **Expected Result:** `200 {"status":"UP"}`.
- **Failure Conditions:** Non-200 / `DOWN`.
- **Can be Automated?** Yes — **Tool:** API.

### FND-HEALTH-003 — Readiness reflects dependency state (degraded detection)
- **Priority:** High
- **Preconditions:** Stack up; ability to stop a dependency.
- **Test Steps:**
  1. Baseline `GET /actuator/health/readiness` on progress-service → UP.
  2. `docker compose stop redis`.
  3. Re-query health (readiness/aggregate) after cache TTL.
- **Expected Result:** `db`/`redis` component reflects DOWN in aggregate health once Redis is unreachable (health status transitions to DOWN or component shows DOWN); returns to UP after `docker compose start redis`.
- **Failure Conditions:** Health still reports fully UP with Redis down, or never recovers.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-HEALTH-004 — DB-down surfaces `db: DOWN`
- **Priority:** High
- **Preconditions:** Target service up.
- **Test Steps:** `docker compose stop auth-db`; `GET auth-service /actuator/health`.
- **Expected Result:** Aggregate `DOWN`, component `db` DOWN, HTTP `503`.
- **Failure Conditions:** Reports UP with DB stopped.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-HEALTH-005 — Info endpoint reachable
- **Priority:** Low
- **Test Steps:** `GET /actuator/info` per service.
- **Expected Result:** `200` (body may be `{}` if no info contributors).
- **Failure Conditions:** `404`/`500`.
- **Can be Automated?** Yes — **Tool:** API.

### FND-HEALTH-006 — Non-exposed actuator endpoints are hidden
- **Priority:** Medium
- **Preconditions:** Only `health,info`(,`gateway`) exposed.
- **Test Steps:** `GET /actuator/env`, `/actuator/beans`, `/actuator/metrics` on a business service.
- **Expected Result:** `404` (not exposed) — confirms no accidental over-exposure.
- **Failure Conditions:** `200` on a sensitive endpoint.
- **Can be Automated?** Yes — **Tool:** API.

### FND-HEALTH-007 — Health reachable through the gateway (public)
- **Priority:** Medium
- **Test Steps:** `GET http://localhost:8080/actuator/health` (no token).
- **Expected Result:** `200 UP` (path `/actuator/` is public per filter).
- **Failure Conditions:** `401` (would mean public-path list broke).
- **Can be Automated?** Yes — **Tool:** API.

---

## 3. Gateway Routing

### FND-GW-001 — Every configured route forwards to the right service
- **Priority:** Critical
- **Preconditions:** Full stack; a valid access token available (see fixtures).
- **Test Steps:** For each row in §0.4, send a request through `:8080` with a valid token and assert it reaches the intended service (via a known ping/health-style or a 2xx/expected-4xx from the *correct* service, not `503`/`404` from the gateway).
- **Expected Result:** Request is routed to the mapped service; response originates from that service.
- **Failure Conditions:** Gateway `404`/`503`, or routed to wrong service.
- **Can be Automated?** Yes — **Tool:** API.

### FND-GW-002 — Route specificity: `/users/{id}/plans` → learning, not user-service
- **Priority:** High
- **Preconditions:** Valid token; learning-service & user-service up.
- **Test Steps:** `GET /users/{id}/plans`; then `GET /users/{id}/badges`; then `GET /users/{id}`.
- **Expected Result:** `/plans` handled by learning-service, `/badges` by badge-service, bare `/users/{id}` by user-service — proving declaration order defeats `/users/**` shadowing.
- **Failure Conditions:** `/plans` or `/badges` served by user-service (order regression).
- **Can be Automated?** Yes — **Tool:** API.

### FND-GW-003 — Route specificity: `/ai/memory/**` → rag, `/ai/**` → ai
- **Priority:** High
- **Test Steps:** Call `/ai/memory/<...>` and a plain `/ai/<...>` path with a valid token.
- **Expected Result:** `/ai/memory/**` reaches rag-service; other `/ai/**` reaches ai-service.
- **Failure Conditions:** `/ai/memory` swallowed by ai-service route.
- **Can be Automated?** Yes — **Tool:** API.

### FND-GW-004 — Protected route without token → 401
- **Priority:** Critical
- **Test Steps:** `GET /users/me` (or any non-public path) with no `Authorization`.
- **Expected Result:** `401`, JSON body `{"status":401,"error":"Unauthorized","message":"Missing or malformed Authorization header","path":"..."}`.
- **Failure Conditions:** `200`/`403`/`500`, or request reaches downstream.
- **Can be Automated?** Yes — **Tool:** API.

### FND-GW-005 — Protected route with invalid/expired/refresh token → 401
- **Priority:** Critical
- **Test Steps:** Send (a) garbage bearer, (b) expired access token, (c) a valid **refresh** token, to a protected route.
- **Expected Result:** All → `401` `"Invalid or expired access token"`. Refresh token rejected because `TokenType.ACCESS` is required.
- **Failure Conditions:** Any accepted.
- **Can be Automated?** Yes — **Tool:** API.

### FND-GW-006 — Downstream unavailable → 503 (gateway degrades gracefully)
- **Priority:** High
- **Preconditions:** Valid token; stop one target service.
- **Test Steps:** `docker compose stop interview-service`; `GET /interviews/...` with valid token.
- **Expected Result:** Gateway returns `503`/`5xx` (no instance available), remains itself UP; recovers after service restart + Eureka re-registration.
- **Failure Conditions:** Gateway crashes or hangs indefinitely.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-GW-007 — Unknown route → 404
- **Priority:** Medium
- **Test Steps:** `GET /this-does-not-exist` (with and without token).
- **Expected Result:** `404` from gateway (no predicate matches). Note: unmatched path is not in public list, so no-token case still `401` before routing — assert documented behavior.
- **Failure Conditions:** `500` or unexpected forward.
- **Can be Automated?** Yes — **Tool:** API.

### FND-GW-008 — Public auth routes need no token
- **Priority:** Critical
- **Test Steps:** `POST /auth/login`, `POST /auth/register`, `POST /auth/refresh` with no bearer.
- **Expected Result:** Reaches auth-service (business-level 200/400/401, **not** gateway 401 for missing bearer).
- **Failure Conditions:** Gateway blocks with "Missing Authorization header".
- **Can be Automated?** Yes — **Tool:** API.

### FND-GW-009 — Identity header spoofing is stripped on public routes
- **Priority:** High
- **Test Steps:** `POST /auth/login` with forged `X-User-Id: 00000000-...` and `X-User-Roles: ADMIN`.
- **Expected Result:** Gateway removes those headers before forwarding (verified via a downstream echo or by behavior); client cannot inject identity.
- **Failure Conditions:** Forged headers reach downstream.
- **Can be Automated?** Yes — **Tool:** Integration (needs downstream header visibility).

### FND-GW-010 — CORS preflight from frontend origin allowed
- **Priority:** High
- **Test Steps:** `OPTIONS /auth/login` with `Origin: http://localhost:4200`, `Access-Control-Request-Method: POST`.
- **Expected Result:** `2xx` with `Access-Control-Allow-Origin: http://localhost:4200`; OPTIONS bypasses JWT filter.
- **Failure Conditions:** Preflight `401`/missing CORS headers.
- **Can be Automated?** Yes — **Tool:** API / Playwright.

### FND-GW-011 — Gateway routes actuator exposes live route table
- **Priority:** Low
- **Test Steps:** `GET /actuator/gateway/routes`.
- **Expected Result:** `200`; array contains all 16 route ids from §0.4.
- **Failure Conditions:** Missing routes / endpoint absent.
- **Can be Automated?** Yes — **Tool:** API.

---

## 4. Eureka Registration

### FND-EUREKA-001 — All services register
- **Priority:** Critical
- **Preconditions:** Full stack up ~60s.
- **Test Steps:** `GET http://localhost:8761/eureka/apps` (Accept: application/json).
- **Expected Result:** Registry lists `API-GATEWAY, AUTH-SERVICE, USER-SERVICE, PROGRESS-SERVICE, LEARNING-SERVICE, BADGE-SERVICE, AI-SERVICE, INTERVIEW-SERVICE, RAG-SERVICE` with status `UP`.
- **Failure Conditions:** Any expected app missing or `DOWN`.
- **Can be Automated?** Yes — **Tool:** API.

### FND-EUREKA-002 — Discovery server does not self-register
- **Priority:** Medium
- **Test Steps:** Inspect registry for `DISCOVERY-SERVER`.
- **Expected Result:** Absent (`register-with-eureka=false`).
- **Failure Conditions:** Present.
- **Can be Automated?** Yes — **Tool:** API.

### FND-EUREKA-003 — Registration occurs after service is READY
- **Priority:** Medium
- **Test Steps:** Correlate service readiness time vs first appearance in registry.
- **Expected Result:** Service appears UP in Eureka only after it started; instance carries IP (`prefer-ip-address=true`).
- **Failure Conditions:** Registered while still starting / never registers.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-EUREKA-004 — Services stay registered (heartbeat / lease renewal)
- **Priority:** Medium
- **Test Steps:** Poll registry over 2–3 min.
- **Expected Result:** No unexpected eviction; instances remain UP.
- **Failure Conditions:** Instance evicted while healthy.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-EUREKA-005 — Service recovers registration after restart
- **Priority:** High
- **Test Steps:** `docker compose restart badge-service`; poll registry.
- **Expected Result:** BADGE-SERVICE re-registers UP within lease window; gateway routes to it again.
- **Failure Conditions:** Fails to re-register.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-EUREKA-006 — Gateway tolerates Eureka outage without crashing
- **Priority:** Medium
- **Test Steps:** With stack up, `docker compose stop discovery-server` briefly; observe gateway.
- **Expected Result:** Gateway keeps serving from cached registry; does not crash; recovers when Eureka returns. (Self-preservation disabled means stale instances get evicted — assert graceful behavior, not a specific SLA.)
- **Failure Conditions:** Gateway crash / permanent routing loss after Eureka returns.
- **Can be Automated?** Yes — **Tool:** Integration.

---

## 5. Liquibase

### FND-LB-001 — All migrations apply on first boot
- **Priority:** Critical
- **Preconditions:** Fresh volumes (`docker compose down -v`).
- **Test Steps:** Boot stack; query `DATABASECHANGELOG` per DB (count rows).
- **Expected Result:** Row counts match changeset counts (§0.7): auth 7, user 2, progress 2, learning 1, badge 2, ai 6, interview 1, rag 4; every row `EXECTYPE=EXECUTED`.
- **Failure Conditions:** Missing changesets / `MARK_RAN` unexpectedly / service failed to start.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-LB-002 — Hibernate `validate` passes (schema ⇄ entity match)
- **Priority:** Critical
- **Test Steps:** Confirm each service started (ddl-auto=validate would abort on drift).
- **Expected Result:** No `SchemaManagementException`; services UP.
- **Failure Conditions:** Startup abort citing missing column/table.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-LB-003 — Re-boot does not re-run migrations
- **Priority:** High
- **Preconditions:** DBs already migrated (volumes retained).
- **Test Steps:** `docker compose restart <service>`; inspect logs + `DATABASECHANGELOG` timestamps.
- **Expected Result:** Liquibase reports 0 changesets applied; existing rows unchanged; checksums match.
- **Failure Conditions:** Re-execution / checksum mismatch error.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-LB-004 — pgvector extension present in rag-db
- **Priority:** High
- **Test Steps:** `SELECT * FROM pg_extension WHERE extname='vector';` on rag-db.
- **Expected Result:** Row exists (changeset `001-enable-pgvector`).
- **Failure Conditions:** Extension absent → embedding storage broken.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-LB-005 — Seed data loaded (badge catalog)
- **Priority:** Medium
- **Test Steps:** Query badge-db seed table after `002-seed-badges`.
- **Expected Result:** Seed rows present (non-empty badge catalog).
- **Failure Conditions:** Empty catalog.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-LB-006 — Migration failure aborts startup (negative)
- **Priority:** Low
- **Preconditions:** Test-only tampered changelog OR incompatible pre-existing schema.
- **Test Steps:** Introduce a deliberately failing changeset in an isolated test DB; boot service.
- **Expected Result:** Service fails fast with Liquibase error; does not register in Eureka; health never UP.
- **Failure Conditions:** Service starts despite broken schema.
- **Can be Automated?** Yes — **Tool:** Integration (isolated; never against real changelogs).

---

## 6. Redis

### FND-REDIS-001 — Connection established for cache-using services
- **Priority:** High
- **Preconditions:** Redis up; progress/badge/ai/rag up.
- **Test Steps:** `GET /actuator/health` on each; assert `redis` component UP. `redis-cli ping` → `PONG`.
- **Expected Result:** `redis: UP`; PONG.
- **Failure Conditions:** `redis: DOWN` / connection refused.
- **Can be Automated?** Yes — **Tool:** API / Integration.

### FND-REDIS-002 — Cache write-through populates Redis
- **Priority:** Medium
- **Preconditions:** Cacheable endpoint reachable (progress/badge/ai `@Cacheable` methods).
- **Test Steps:** Invoke a cacheable read via gateway (valid token); then `redis-cli KEYS '*'`.
- **Expected Result:** Cache key created for the computed value.
- **Failure Conditions:** No key written despite `type: redis` cache config.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-REDIS-003 — Cache read served on second call
- **Priority:** Medium
- **Test Steps:** Call the same cacheable endpoint twice; verify second hit does not recompute (log/DB-query count) — the cached value is returned.
- **Expected Result:** Value stable; second call served from cache.
- **Failure Conditions:** Always recomputes (cache ineffective).
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-REDIS-004 — Redis unavailable behavior
- **Priority:** High
- **Preconditions:** Service up, then stop Redis.
- **Test Steps:** `docker compose stop redis`; call a cacheable endpoint.
- **Expected Result:** Documented/observed behavior recorded: health `redis: DOWN`; requests either error clearly or fall through to source (whichever the code actually does — **assert the real behavior**, do not assume a fallback exists). Restore Redis → recovers.
- **Failure Conditions:** Silent data corruption / undocumented crash loop.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-REDIS-005 — user-service works without Redis
- **Priority:** Low
- **Test Steps:** Confirm user-service has no Redis dependency and is healthy while Redis is down.
- **Expected Result:** user-service UP regardless of Redis (no `SPRING_DATA_REDIS_HOST` wired).
- **Failure Conditions:** user-service impacted by Redis outage.
- **Can be Automated?** Yes — **Tool:** Integration.

> Note: cache **expiration/TTL** — verify against `CacheConfig` before asserting. Only add a TTL-expiry test if a TTL is actually configured; otherwise mark N/A.

---

## 7. Kafka

### FND-KAFKA-001 — Broker connectivity
- **Priority:** Critical
- **Preconditions:** Kafka up.
- **Test Steps:** `kafka-topics.sh --bootstrap-server localhost:29092 --list` (host) or health.
- **Expected Result:** Command succeeds; broker reachable.
- **Failure Conditions:** Connection refused / timeout.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-KAFKA-002 — Producers healthy (auth publishes `user.registered`)
- **Priority:** High
- **Preconditions:** Full stack; consumer topic present.
- **Test Steps:** `POST /auth/register`; consume `athena.user.registered` from host (29092) with a test consumer.
- **Expected Result:** Exactly one event with key=userId and JSON payload appears.
- **Failure Conditions:** No message produced.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-KAFKA-003 — Consumers subscribed with correct group ids
- **Priority:** High
- **Test Steps:** `kafka-consumer-groups.sh --describe` for groups `ai-service`, `badge-service`, `progress-service`, `learning-service`, etc.
- **Expected Result:** Each service's group exists and is assigned partitions for its subscribed topics (per §0.8 listeners).
- **Failure Conditions:** Missing group / no assignment.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-KAFKA-004 — End-to-end event flow (produce → consume → side effect)
- **Priority:** High
- **Test Steps:** Trigger `athena.task.completed` (via learning flow) and assert progress-service consumes it (streak update / DB change), which itself emits `athena.streak.updated` consumed by badge-service.
- **Expected Result:** Downstream side effects observed; offsets advance.
- **Failure Conditions:** Event stuck / consumer lag grows unbounded.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-KAFKA-005 — Topics auto-created from `NewTopic` beans
- **Priority:** Medium
- **Test Steps:** After boot, `--list` topics; assert all `athena.*` topics from §0.8 exist.
- **Expected Result:** All declared topics present with expected partition/replica (RF=1 single node).
- **Failure Conditions:** Missing topic.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-KAFKA-006 — Broker outage handling
- **Priority:** High
- **Preconditions:** Stack up; stop Kafka.
- **Test Steps:** `docker compose stop kafka`; observe producing service (e.g. auth register).
- **Expected Result:** Producer surfaces error/retries (`acks=all`); consumers reconnect when Kafka returns; no permanent crash of the service. Record actual behavior.
- **Failure Conditions:** Service crash loop / data loss beyond expectation.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-KAFKA-007 — Consumer catch-up after downtime (`auto-offset-reset=earliest`)
- **Priority:** Medium
- **Test Steps:** Stop a consumer service; produce events; restart consumer.
- **Expected Result:** On restart, consumer processes backlog from last committed offset (or earliest for a fresh group).
- **Failure Conditions:** Events skipped.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-KAFKA-008 — Startup without Kafka (compose gates on `service_healthy`)
- **Priority:** Low
- **Test Steps:** Attempt to start a Kafka-dependent service with Kafka absent (outside compose gating) to record behavior.
- **Expected Result:** Under compose it will not start until Kafka healthy (documented). Standalone: connection retries; service may start but Kafka-backed features degraded. Record real behavior.
- **Failure Conditions:** Undocumented hard failure masquerading as healthy.
- **Can be Automated?** Yes — **Tool:** Integration.

---

## 8. Configuration Loading

### FND-CONF-001 — Defaults apply with no env overrides
- **Priority:** High
- **Test Steps:** Run a service locally with only `application.yml` (no env); inspect effective datasource/eureka/kafka values.
- **Expected Result:** Falls back to documented defaults (`localhost:543x`, `localhost:8761`, `localhost:29092`, dev JWT secret).
- **Failure Conditions:** Startup fails despite defaults being present.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-CONF-002 — Env vars override YAML (compose profile)
- **Priority:** Critical
- **Test Steps:** In compose, confirm services connect to container hostnames (`auth-db:5432`, `kafka:9092`, `discovery-server:8761`) not localhost.
- **Expected Result:** `SPRING_DATASOURCE_URL`, `EUREKA_URI`, `KAFKA_BOOTSTRAP_SERVERS`, `SPRING_DATA_REDIS_HOST` take precedence over YAML defaults.
- **Failure Conditions:** Service uses localhost inside container (would fail to connect).
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-CONF-003 — Shared JWT secret consistent across auth ⇄ gateway
- **Priority:** Critical
- **Test Steps:** Confirm both auth-service and api-gateway receive the same `ATHENA_JWT_SECRET` (compose anchor `&jwt-secret`). Mint token at auth, validate at gateway.
- **Expected Result:** Gateway validates auth-issued token (same HMAC secret + issuer `athena-auth`).
- **Failure Conditions:** Token minted by auth rejected by gateway → secret mismatch.
- **Can be Automated?** Yes — **Tool:** API.

### FND-CONF-004 — `.env.example` documents required externals
- **Priority:** Low
- **Test Steps:** Diff `.env.example` keys vs env vars referenced in compose (`ATHENA_LOGGING_CLOUDWATCH_*`, `ATHENA_AI_*`, `ATHENA_FRONTEND_ORIGIN`, etc.).
- **Expected Result:** All overridable externals are discoverable; defaults documented.
- **Failure Conditions:** Undocumented required var with no default.
- **Can be Automated?** Yes — **Tool:** Integration (static lint).

### FND-CONF-005 — AI/Embedding config points at LM Studio
- **Priority:** Medium
- **Test Steps:** Confirm ai-service/rag-service resolve `ATHENA_AI_BASE_URL` → `http://host.docker.internal:1234/v1`, model `qwen3-14b`, embedding `text-embedding-bge-m3`, dim `1024`.
- **Expected Result:** Values resolved; services start even if LM Studio is down (lazy call at request time).
- **Failure Conditions:** Startup coupled to LM Studio availability (would break foundation boot).
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-CONF-006 — Invalid/missing critical config fails fast
- **Priority:** Medium
- **Test Steps:** Start a service with a malformed `SPRING_DATASOURCE_URL`.
- **Expected Result:** Fails fast with a clear datasource error; does not register / never healthy.
- **Failure Conditions:** Silent partial startup.
- **Can be Automated?** Yes — **Tool:** Integration (isolated).

### FND-CONF-007 — Frontend `apiBase` matches gateway port
- **Priority:** Medium
- **Test Steps:** Assert `environment.ts`/`environment.prod.ts` `apiBase = http://localhost:8080` equals gateway `server.port`.
- **Expected Result:** Match.
- **Failure Conditions:** Mismatch → all UI calls fail.
- **Can be Automated?** Yes — **Tool:** Integration (static) / Playwright (runtime).

---

## 9. Environment Validation (pre-flight)

### FND-ENV-001 — Required host ports free before boot
- **Priority:** High
- **Test Steps:** Pre-flight check ports 8080–8088, 8761, 4200, 5433–5440, 6379, 29092.
- **Expected Result:** All free (or owned by our stack).
- **Failure Conditions:** Port in use by foreign process → bind failure.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-ENV-002 — Docker engine + compose available
- **Priority:** Critical
- **Test Steps:** `docker version`, `docker compose version`.
- **Expected Result:** Both present and daemon reachable.
- **Failure Conditions:** Daemon down / compose missing.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-ENV-003 — Required env file / defaults resolvable
- **Priority:** Medium
- **Test Steps:** Ensure `.env` present or all compose `${VAR:-default}` have defaults.
- **Expected Result:** No unresolved required variable.
- **Failure Conditions:** Compose interpolation warning for a var without default.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-ENV-004 — Core services reachable post-boot
- **Priority:** Critical
- **Test Steps:** After boot, TCP/HTTP probe gateway:8080, eureka:8761, each DB port, redis:6379, kafka:29092.
- **Expected Result:** All reachable.
- **Failure Conditions:** Any core endpoint unreachable.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-ENV-005 — Optional externals absent → stack still boots
- **Priority:** High
- **Preconditions:** LM Studio (1234) and LocalStack (4566) **not** running.
- **Test Steps:** Boot full stack; verify all services healthy.
- **Expected Result:** Stack is healthy; only AI-inference and CloudWatch-shipping / S3 upload features are degraded (fail at request time, not at boot).
- **Failure Conditions:** Any service fails to start because LM Studio/LocalStack absent.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-ENV-006 — Missing dependency prevents dependent boot (negative)
- **Priority:** Medium
- **Test Steps:** Start auth-service with auth-db absent (bypassing compose gate).
- **Expected Result:** auth-service retries/fails, does not report healthy, does not register UP.
- **Failure Conditions:** Reports healthy without its DB.
- **Can be Automated?** Yes — **Tool:** Integration.

### FND-ENV-007 — Frontend prerequisites present
- **Priority:** Low
- **Test Steps:** `node -v`, `npm -v`, `npm ci` succeeds in `Athena-Frontend`.
- **Expected Result:** Compatible Node; deps install; `ng` available.
- **Failure Conditions:** Version/install failure.
- **Can be Automated?** Yes — **Tool:** Integration.

---

## Coverage Matrix (area → test IDs)

| Area | Test IDs | Critical count |
|---|---|---|
| 1 Startup | FND-START-001..007 | 001,003,004 |
| 2 Health | FND-HEALTH-001..007 | 001 |
| 3 Gateway | FND-GW-001..011 | 001,004,005,008 |
| 4 Eureka | FND-EUREKA-001..006 | 001 |
| 5 Liquibase | FND-LB-001..006 | 001,002 |
| 6 Redis | FND-REDIS-001..005 | — |
| 7 Kafka | FND-KAFKA-001..008 | 001 |
| 8 Config | FND-CONF-001..007 | 002,003 |
| 9 Environment | FND-ENV-001..007 | 002,004 |

**Total: 53 foundation test cases.** Playwright automation targets a small UI-facing subset; the majority are API/Integration (see `FOUNDATION_PLAYWRIGHT_PLAN.md`).
