# Infrastructure

The only deployment shape that exists today is **Docker Compose for local development**
(`docker-compose.yml`, 22 services). There is no Kubernetes/Helm, no managed-cloud
Terraform, and no staging/production compose overlay — see the gaps called out in
`docs/Deployment.md` and `ROADMAP.md`.

## 1. Docker Compose stack

```
name: athena
```

### Infrastructure containers
| Service | Image | Host port | Purpose |
|---|---|---|---|
| `discovery-server` | built from source | 8761 | Eureka registry |
| `kafka` | `apache/kafka:3.9.1` | 29092 (host) / 9092 (internal) | Event backbone, KRaft mode, single broker |
| `redis` | `redis:7-alpine` | 6379 | Caching |

Kafka runs in **KRaft mode** (no ZooKeeper) with one internal listener (`INTERNAL`,
used by services via `kafka:9092`) and one external listener (`EXTERNAL`, used by the
host via `localhost:29092`).

### Per-service Postgres (one database per business service, no exceptions)
| DB container | Host port | Database name |
|---|---|---|
| `auth-db` | 5433 | `athena_auth` |
| `user-db` | 5434 | `athena_user` |
| `progress-db` | 5435 | `athena_progress` |
| `learning-db` | 5436 | `athena_learning` |
| `badge-db` | 5437 | `athena_badge` |
| `ai-db` | 5438 | `athena_ai` |
| `interview-db` | 5439 | `athena_interview` |
| `rag-db` | 5440 | `athena_rag` — **`pgvector/pgvector:pg17`** image (the only DB running the pgvector extension) |

All other DB containers use plain `postgres:17`. Every DB has a `pg_isready` healthcheck
that the corresponding service `depends_on: condition: service_healthy`.

### Business services
| Service | Host port |
|---|---|
| `auth-service` | 8081 |
| `user-service` | 8082 |
| `progress-service` | 8083 |
| `learning-service` | 8084 |
| `badge-service` | 8085 |
| `ai-service` | 8086 |
| `interview-service` | 8087 |
| `rag-service` | 8088 |
| `api-gateway` | 8080 (the only port meant to be called from outside the stack) |

## 2. Build strategy

Every module's `Dockerfile` is a two-stage build run **from the repo root** (`context: .`
in compose, so the Maven Wrapper and full reactor are available for `-am` — "also make"
— to build shared modules first):

```dockerfile
FROM eclipse-temurin:26-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -q -pl <module> -am clean package -DskipTests

FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /workspace/<module>/target/*.jar app.jar
EXPOSE <port>
ENTRYPOINT ["java", "-jar", "app.jar"]
```

JDK 26 to build, JRE 26 to run; tests are skipped in the image build (CI runs them
separately — see `docs/Deployment.md`).

## 3. Shared configuration (YAML anchors in `docker-compose.yml`)

- `x-jwt-secret` — one shared secret used by `api-gateway` and `auth-service` (they must
  agree, since the gateway validates tokens `auth-service` issues).
- `x-cloudwatch-env` — the `ATHENA_LOGGING_CLOUDWATCH_*` block merged into every
  service's environment (see §4).

## 4. Logging — CloudWatch via LocalStack (or real AWS)

`athena-common/logging` (`CloudWatchLogbackAppender` + auto-configuration) ships
structured logs to CloudWatch Logs from every service. Controlled by `.env` (copy from
`.env.example`):

```properties
ATHENA_LOGGING_CLOUDWATCH_ENABLED=true
# ATHENA_LOGGING_CLOUDWATCH_ENDPOINT=http://host.docker.internal:4566   # LocalStack
```

LocalStack is **not** part of the compose stack — run it separately if you want log
shipping to actually land somewhere locally:

```bash
docker run --rm -it -p 4566:4566 -e LOCALSTACK_AUTH_TOKEN=ls-... localstack/localstack-pro:latest
```

Set `ATHENA_LOGGING_CLOUDWATCH_ENABLED=false` to turn it off entirely (services still log
to stdout/console either way). Separately, `LOGGING_STRUCTURED_FORMAT=ecs` (baked into
every service's compose environment) switches the console format itself to structured
ECS JSON — independent of whether CloudWatch shipping is on.

## 5. Object storage — S3 via LocalStack (or real AWS)

Profile-picture uploads (`auth-service`, register/`/account/image`) go through
`athena-common/storage` (`ImageStorage` → `S3ImageStorage`), configured per-service:

```yaml
ATHENA_STORAGE_S3_ENDPOINT: http://host.docker.internal:4566   # LocalStack by default
ATHENA_STORAGE_S3_REGION: us-east-1
ATHENA_STORAGE_S3_BUCKET: athena-user-images
```

Same LocalStack instance as CloudWatch logging serves both; point both at real AWS
endpoints/credentials for a non-local environment.

## 6. LM Studio (the AI runtime)

Not containerized — expected to run on the **host machine** with its OpenAI-compatible
local server enabled on port 1234. `ai-service` and `rag-service` reach it via
`host.docker.internal` (compose adds `extra_hosts: host.docker.internal:host-gateway`
for this):

```yaml
ATHENA_AI_BASE_URL: http://host.docker.internal:1234/v1
ATHENA_AI_MODEL: qwen3-14b
ATHENA_EMBEDDING_BASE_URL: http://host.docker.internal:1234/v1
ATHENA_EMBEDDING_MODEL: text-embedding-bge-m3
ATHENA_RAG_EMBEDDING_DIMENSION: 1024
```

Load a chat-capable model (e.g. `qwen3-14b`) and an embedding model
(`text-embedding-bge-m3`) in LM Studio before exercising onboarding, roadmap, learning
sessions, interviews, Daily Journey, or any `rag-service` endpoint. `/ai/onboarding/me`
and other read-only state endpoints work without a live model; anything that generates
content does not.

## 7. Bringing the stack up

```bash
docker compose up --build
```

Startup order is enforced by `depends_on: condition: service_healthy` chains (DBs → Kafka
→ discovery-server → business services → gateway). Expect ~60–90s for every service to
register with Eureka on a cold start. Tear down and wipe all data:

```bash
docker compose down -v
```
