# Project Structure

This document maps the repository layout. Athena Backend is a Maven **multi-module**
project — one aggregator POM (`pom.xml`) with 12 modules, each an independently
deployable Spring Boot application except `athena-common` and `athena-llm`, which are
shared libraries.

```
Athena-Backend-Parent/
├── athena-common/            Shared library — JWT, ApiError, exceptions, S3 image
│                              storage, CloudWatch logging, Kafka event DTOs, KafkaTopics
├── athena-llm/                Shared library — LLM provider abstraction (ChatProvider,
│                              EmbeddingProvider) with an LM Studio implementation
├── discovery-server/          Eureka service registry                         :8761
├── api-gateway/                Spring Cloud Gateway — routing + JWT validation :8080
├── auth-service/               Accounts, JWT issuance, 2FA, devices, sessions  :8081
├── user-service/               User profiles and settings                     :8082
├── progress-service/           Progress metrics and streaks                   :8083
├── learning-service/           Learning plans, tasks, study sessions          :8084
├── badge-service/              Badge catalogue and awards                     :8085
├── ai-service/                 LLM orchestration — onboarding, roadmap, daily
│                              journey, learning sessions, knowledge graph,
│                              interview generation/evaluation, AI badge suggestions :8086
├── interview-service/          Weekly interview lifecycle                     :8087
├── rag-service/                RAG memory, embeddings, retrieval, recommendations :8088
├── docs/                       This documentation set
├── postman/                    Postman collections (V1 / V2 / V3 snapshots)
├── .github/                    CI workflows, issue/PR templates
├── .mvn/, mvnw, mvnw.cmd        Maven Wrapper
├── docker-compose.yml           Full local stack (9 Postgres DBs, Kafka, Redis,
│                              Eureka, all 10 runnable services)
├── .env.example                 CloudWatch logging toggle/endpoint template
└── pom.xml                      Aggregator POM — module list, dependency management
```

## Per-service package layout

Every business service follows the same internal shape (see `docs/Backend.md` for the
per-module detail); most package by technical layer, `ai-service` and `rag-service`
additionally package by **feature** because they own several distinct sub-domains:

```
com.athena.<service>.controller   thin HTTP adapters — no business logic
com.athena.<service>.service      business logic (interface + impl)
com.athena.<service>.repository   Spring Data repositories
com.athena.<service>.dto          request/response records
com.athena.<service>.entity       JPA entities (never returned from controllers)
com.athena.<service>.config       beans and configuration properties
com.athena.<service>.web          @RestControllerAdvice / exception handling
com.athena.<service>.messaging    Kafka producers/consumers (where applicable)
```

`ai-service` example (feature-first, then layered within each feature):
```
com.athena.ai.onboarding.{controller,service,dto,entity}
com.athena.ai.roadmap.{controller,service,dto}
com.athena.ai.dailyjourney.{controller,service,dto,entity,domain}
com.athena.ai.dailyplan.{controller,service,dto}
com.athena.ai.learningsession.{controller,service,dto,entity}
com.athena.ai.knowledgegraph.{controller,service,dto,entity}
com.athena.ai.interview.{controller,service,dto}        (AI question-gen/evaluation)
com.athena.ai.recommendation.{controller,service,dto}    (AI badge suggestions)
com.athena.ai.generation.{controller,service}            (retry lifecycle)
com.athena.ai.messaging                                  (Kafka consumers/producers)
```

`rag-service` example:
```
com.athena.rag.memory.{controller,service,entity,repository,ingestion,chunking}
com.athena.rag.retrieval.{controller,service,domain,dto}
com.athena.rag.rag.{controller,service,dto,entity}        (grounded Q&A)
com.athena.rag.recommendation.{controller,service,model,dto}
com.athena.rag.profile.{controller,service,dto}
com.athena.rag.client                                     (Feign clients into other services)
com.athena.rag.messaging
```

## `athena-common` (shared library, all services depend on it)

```
event/       ~28 Kafka event record types + KafkaTopics constants catalogue
exception/   DuplicateResourceException, InvalidCredentialsException, ResourceNotFoundException
logging/     CloudWatchLogbackAppender + auto-configuration
security/    JwtService, TokenType, AuthHeaders (X-User-Id / X-User-Roles constants)
storage/     ImageStorage abstraction + S3ImageStorage implementation (LocalStack/AWS)
web/         ApiError (uniform error shape) + GlobalExceptionHandler base
```

## `athena-llm` (shared library, ai-service and rag-service depend on it)

```
ChatProvider / EmbeddingProvider     provider-agnostic interfaces
model/                               ChatMessage, ChatRequest, ChatResult, EmbeddingResult, ResponseFormat
spi/lmstudio/                        LM Studio (OpenAI-compatible) implementation + DTOs
config/                               LlmAutoConfiguration, LlmProviderProperties
```

## Related repository

The Angular frontend that consumes this API lives in a separate repository,
[`Tamara556/Athena-Frontend`](https://github.com/Tamara556/Athena-Frontend) — see
`docs/Frontend.md`.
