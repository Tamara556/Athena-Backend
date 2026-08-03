# Local Development

## Prerequisites

- **JDK 26** (the Maven Wrapper is pinned to it — `java.version`/`maven.compiler.release`
  in the root `pom.xml`)
- **Docker + Docker Compose** — the fastest path; brings up everything including all 9
  Postgres databases, Kafka, and Redis
- **[LM Studio](https://lmstudio.ai)** — required for any AI-backed feature (onboarding,
  roadmap, learning sessions, Daily Journey, interviews, RAG). Load a chat model (e.g.
  `qwen3-14b`) and an embedding model (`text-embedding-bge-m3`), enable the local
  OpenAI-compatible server on port 1234.
- Maven itself is **not** required — use the bundled wrapper (`./mvnw` / `mvnw.cmd`).

## Two ways to run it

### Option A — everything in Docker (recommended first run)
```bash
docker compose up --build
```
See `docs/Infrastructure.md` for the full service/port table and startup ordering.

### Option B — one module at a time, locally
Useful when iterating on a single service without rebuilding images.

1. Start just the infrastructure + this service's DB via compose, e.g.:
   ```bash
   docker compose up discovery-server kafka redis ai-db
   ```
2. Build the reactor once: `./mvnw clean install`
3. Run the module you're changing:
   ```bash
   ./mvnw -pl ai-service spring-boot:run
   ```
   Spring Boot DevTools-style restarts aren't wired in; use your IDE's run
   configuration for faster iteration (IntelliJ: run the `*Application` class directly
   with the module's dependencies already on the classpath after step 2).

Configuration is environment-variable-driven with localhost-friendly defaults baked into
each `application.yml` (`SPRING_DATASOURCE_URL`, `EUREKA_URI`, `ATHENA_JWT_SECRET`,
`ATHENA_AI_BASE_URL`, etc.) — see `docs/Infrastructure.md` §3–6 for the full variable
list and `.env.example` for the CloudWatch toggle.

## Running tests

```bash
./mvnw test          # unit + slice tests, all modules
./mvnw clean verify   # what CI runs (ci.yml)
```

Testing conventions observed across the codebase (follow these when adding tests):

- **Service layer** — pure JUnit 5 + Mockito; repositories and Feign clients are mocked.
  Time-dependent logic (streaks, scheduling) is tested deterministically via an injected
  `java.time.Clock`, never `Instant.now()`/`LocalDate.now()` directly in test-covered code.
- **Controllers** — `@WebMvcTest` + `MockMvc`, service layer mocked with
  `@MockitoBean`; cover the happy path, validation `400`s, and the mapped
  `401`/`403`/`404`/`409` cases.
- **`api-gateway`'s filter** — WebFlux `MockServerWebExchange`, including a positive
  assertion that client-supplied `X-User-Id`/`X-User-Roles` are stripped (the
  anti-spoofing behavior in `docs/Security.md` §1).
- LM Studio calls are always mocked at the `athena-llm` provider interface
  (`ChatProvider`/`EmbeddingProvider`) — no test depends on a running model.

## Logging

- Local default: human-readable console logs.
- `LOGGING_STRUCTURED_FORMAT=ecs` switches every service to structured ECS JSON console
  output (set automatically in `docker-compose.yml`).
- `ATHENA_LOGGING_CLOUDWATCH_ENABLED=false` turns off CloudWatch shipping entirely if you
  don't want to run LocalStack locally (see `docs/Infrastructure.md` §4).

## Postman

Three collections in [`postman/`](../postman) (`Athena`, `Athena-V2`, `Athena-V3`) are
useful starting points but are historical snapshots — see the gap noted in
`docs/API.md`. Register/Login requests auto-store the returned tokens for the protected
requests that follow, within each collection's own Postman environment scripts.
