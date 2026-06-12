# Athena Backend — Version 1

**Athena** is an AI Learning Operating System. This repository is **Version 1: the backend core only** — no AI, no frontend, no messaging. It delivers a production-shaped Spring Boot microservices system: authentication, user profiles, and learning-progress tracking, fronted by an API Gateway and tied together with service discovery.

---

## 1. Architecture overview

```
                         ┌────────────────────────┐
   client  ──────────►   │     api-gateway        │   :8080   (only public port)
   (Bearer JWT)          │  • routing (lb://)     │
                         │  • JWT validation      │
                         │  • injects identity    │
                         └───────────┬────────────┘
                                     │  X-User-Id / X-User-Roles
             ┌───────────────────────┼───────────────────────────┐
             ▼                       ▼                           ▼
   ┌──────────────────┐   ┌──────────────────┐        ┌──────────────────────┐
   │   auth-service   │   │   user-service   │◄───────│   progress-service   │
   │      :8081       │   │      :8082       │ OpenFeign│       :8083          │
   │  issues JWTs     │   │  profiles        │ lb://    │  progress + streaks  │
   └────────┬─────────┘   └────────┬─────────┘        └──────────┬───────────┘
            │                      │                             │
      ┌─────▼─────┐          ┌─────▼─────┐                 ┌──────▼──────┐
      │ auth-db   │          │ user-db   │                 │ progress-db │   (one DB per service)
      │ postgres  │          │ postgres  │                 │ postgres    │
      └───────────┘          └───────────┘                 └─────────────┘

                         ┌────────────────────────┐
   all services  ───►    │   discovery-server     │   :8761   (Eureka registry)
                         └────────────────────────┘
```

**Key decisions**

- **Centralised auth at the edge.** Only the gateway validates JWTs. On success it forwards the verified identity downstream as trusted `X-User-Id` / `X-User-Roles` headers, and *strips any client-supplied copies* so they cannot be spoofed. Downstream services stay simple and never re-validate tokens.
- **Database per service.** No service touches another service's schema. Each has its own Postgres instance.
- **Service-to-service via discovery.** `progress-service` calls `user-service` through **OpenFeign** + Eureka (`lb://user-service`) to confirm a user exists before tracking progress.
- **Shared library (`athena-common`).** Holds only cross-cutting contracts used by more than one module: the `JwtService` (sign + verify), the uniform `ApiError` shape, the reusable `GlobalExceptionHandler` base, and shared exceptions. It is framework-light so both the servlet services and the reactive gateway can depend on it.
- **Clean layering everywhere:** `controller → service → repository`, DTOs at every boundary, entities never leave the service layer.

---

## 2. Tech stack

| Concern            | Choice                                              |
|--------------------|-----------------------------------------------------|
| Language           | Java 26                                             |
| Framework          | Spring Boot 4.0.5                                    |
| Cloud              | Spring Cloud 2025.1.1 (Oakwood) — Eureka, Gateway (WebFlux), OpenFeign |
| Persistence        | Spring Data JPA / Hibernate, PostgreSQL 17          |
| Migrations         | Liquibase                                           |
| Auth               | JWT (JJWT) with access + refresh tokens, BCrypt     |
| Validation         | Jakarta Validation                                  |
| Build              | Maven (multi-module), Maven Wrapper                 |
| Tests              | JUnit 5, Mockito, MockMvc, WebFlux `MockServerWebExchange` |
| Containerisation   | Docker, Docker Compose                              |

---

## 3. Module / folder structure

```
athena-backend-parent/                 (aggregator POM, dependency management)
├── athena-common/                     shared JWT, ApiError, exception handler
├── discovery-server/                  Eureka registry              :8761
├── api-gateway/                       routing + JWT filter         :8080
├── auth-service/                      register / login / refresh   :8081
├── user-service/                      profile CRUD                 :8082
├── progress-service/                  progress + weekly summary    :8083
├── docker-compose.yml                 full local stack
├── postman/Athena.postman_collection.json
└── README.md
```

Every service follows the same package layout:

```
com.athena.<service>.controller   // thin HTTP adapters, no business logic
com.athena.<service>.service      // business logic (interface + impl)
com.athena.<service>.repository   // Spring Data repositories
com.athena.<service>.dto          // request/response records
com.athena.<service>.entity       // JPA entities (never exposed)
com.athena.<service>.config       // beans & configuration properties
com.athena.<service>.web          // @RestControllerAdvice
```

---

## 4. Running it

### Prerequisites
- Docker + Docker Compose **(recommended path — needs nothing else installed)**, or
- JDK 26 + the bundled Maven Wrapper for a local run.

### Option A — Docker Compose (everything)

```bash
docker compose up --build
```

This starts the Eureka registry, three Postgres instances, all three services, and the gateway. Wait until the services register (≈30–60s). Then hit the gateway:

```bash
# Register (returns access + refresh tokens and your userId)
curl -s -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","password":"password123"}'
```

Tear down (and wipe data):

```bash
docker compose down -v
```

### Option B — Local (Maven Wrapper)

> The wrapper must run on **JDK 26**. If your `JAVA_HOME` points elsewhere, set it first:
> `export JAVA_HOME=/path/to/jdk-26` (Windows PowerShell: `$env:JAVA_HOME='C:\Program Files\Java\jdk-26.0.1'`).

1. Start three Postgres databases (or just use the compose DBs: `docker compose up auth-db user-db progress-db`). Default local ports: `5433/5434/5435`.
2. Build everything: `./mvnw clean install`
3. Run each module (separate terminals), **discovery first**:
   ```bash
   ./mvnw -pl discovery-server   spring-boot:run
   ./mvnw -pl auth-service       spring-boot:run
   ./mvnw -pl user-service       spring-boot:run
   ./mvnw -pl progress-service   spring-boot:run
   ./mvnw -pl api-gateway        spring-boot:run
   ```

Configuration is environment-driven (`SPRING_DATASOURCE_URL`, `EUREKA_URI`, `ATHENA_JWT_SECRET`, `LOGGING_STRUCTURED_FORMAT`); sensible localhost defaults are baked into each `application.yml`.

---

## 5. API reference (all via the gateway, `http://localhost:8080`)

| Method | Path                          | Auth        | Purpose                            |
|--------|-------------------------------|-------------|------------------------------------|
| POST   | `/auth/register`              | public      | Create account, returns tokens     |
| POST   | `/auth/login`                 | public      | Authenticate, returns tokens       |
| POST   | `/auth/refresh`               | public¹     | Rotate tokens from a refresh token |
| POST   | `/users`                      | Bearer      | Create profile (linked to userId)  |
| GET    | `/users/{id}`                 | Bearer      | Fetch profile                      |
| PUT    | `/users/{id}`                 | Bearer      | Replace profile                    |
| POST   | `/progress/update`            | Bearer      | Record a study session (increments)|
| GET    | `/progress/{userId}`          | Bearer      | Current totals + streak            |
| GET    | `/progress/summary/{userId}`  | Bearer      | Rolling 7-day summary              |

¹ `/auth/**` is public at the gateway; the refresh token itself is validated inside auth-service.

### Example end-to-end flow (curl)

```bash
BASE=http://localhost:8080

# 1) Register and capture token + id
RESP=$(curl -s -X POST $BASE/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","password":"password123"}')
TOKEN=$(echo "$RESP" | jq -r .accessToken)
UID=$(echo "$RESP" | jq -r .userId)

# 2) Create profile
curl -s -X POST $BASE/users -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"userId\":\"$UID\",\"name\":\"Ada\",\"age\":30,\"goal\":\"Master DS\",\"dailyStudyHours\":2.5}"

# 3) Log a study session
curl -s -X POST $BASE/progress/update -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"userId\":\"$UID\",\"tasksCompleted\":3,\"minutesSpent\":45}"

# 4) Weekly summary
curl -s $BASE/progress/summary/$UID -H "Authorization: Bearer $TOKEN"
```

A ready-to-run **Postman collection** is in [`postman/`](postman/Athena.postman_collection.json) — Register/Login auto-store the tokens and userId for the protected requests.

### Error contract

Every service returns the same shape on error:

```json
{
  "timestamp": "2026-06-12T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "path": "/users",
  "details": [{ "field": "age", "message": "age must be at least 5" }]
}
```

---

## 6. Testing

```bash
./mvnw test
```

- **Service layer** — pure JUnit 5 + Mockito (repositories and Feign client mocked). Streak logic is tested deterministically via an injected `Clock`.
- **Controllers** — `@WebMvcTest` + MockMvc, service mocked with `@MockitoBean`; covers happy paths, validation 400s, and mapped 401/404/409.
- **Gateway filter** — verified with WebFlux `MockServerWebExchange`, including the anti-spoofing behaviour.

---

## 7. Notes, assumptions & future work

- **Eureka discovery-server** is included because the requirements specify Eureka; it is infrastructure, not one of the four business services.
- **Refresh tokens are stateless** (signed, type-checked, re-validated against the current account on use). Server-side revocation/rotation tracking is intentionally deferred to a later version.
- **`ddl-auto: validate`** — Liquibase owns the schema; Hibernate only checks the mappings match.
- **Secrets** are environment variables with dev defaults. In any real environment, set `ATHENA_JWT_SECRET` (≥ 32 bytes) and DB credentials via your secret manager. The gateway and auth-service must share the same secret + issuer.
- **Structured logging** is opt-in: set `LOGGING_STRUCTURED_FORMAT=ecs` (done in compose) for JSON logs; unset for human-readable local logs.
