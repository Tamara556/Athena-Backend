# Backend Services

Detailed reference for all 12 Maven modules. All ports/DB names are taken directly from
`docker-compose.yml`; all endpoints are taken directly from the `@RestController`
classes in each module (not from older design docs, which have since drifted).

Every business endpoint below is reached **through the gateway** (`http://localhost:8080`)
unless noted; the gateway injects `X-User-Id` / `X-User-Roles` after validating the
caller's JWT (see `docs/Security.md`). Endpoints under a service's own port are for
internal/direct use only (tests, service-to-service Feign calls).

---

## discovery-server — Eureka registry
**Port:** 8761 · **DB:** none

Plain Spring Cloud Netflix Eureka server. Every other module registers with it on boot
and discovers peers through it (`lb://<service-name>` URIs in the gateway and in Feign
clients). No business logic.

---

## api-gateway — Edge routing + JWT validation
**Port:** 8080 (the only publicly exposed port) · **DB:** none

Spring Cloud Gateway (WebFlux). Two responsibilities:
1. **Routing** — `Path=/**` predicates map each route to a `lb://<service>` Eureka URI
   (see `docs/Architecture.md` for the full route table).
2. **Authentication** — `JwtAuthenticationFilter` (`com.athena.gateway.filter`) is a
   `WebFilter` at `Ordered.HIGHEST_PRECEDENCE + 10`. It:
   - lets `/auth/**` and `/actuator/**` through unauthenticated, **stripping** any
     client-supplied `X-User-Id`/`X-User-Roles` first (anti-spoofing);
   - for every other path, requires `Authorization: Bearer <token>`, parses/validates it
     with the shared `JwtService`, and injects verified `X-User-Id` / `X-User-Roles`
     headers before forwarding.

No `WebSecurityConfig`/Spring Security filter chain — auth is this one custom filter.
Depends on `athena-common` only (for `JwtService`/`AuthHeaders`).

---

## auth-service — Identity, sessions, 2FA
**Port:** 8081 · **DB:** `athena_auth` (5433 host)

Owns the account lifecycle end-to-end: registration (with optional avatar upload straight
to S3-compatible storage), login, JWT issuance/refresh, phone-based 2FA, device-session
tracking, login-activity history, profile/email/password changes, and a GDPR-style full
data export that fans out to every other service via Feign.

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/register` | multipart: account fields + optional `image`; returns tokens |
| POST | `/auth/login` | returns tokens, or a 2FA challenge if enabled |
| POST | `/auth/2fa/verify` | complete login with a 2FA code |
| POST | `/auth/refresh` | rotate access token from a refresh token |
| GET | `/auth/users/{userId}/image` | public avatar fetch (byte stream, 1h cache) |
| GET | `/account/me` | current account |
| PATCH | `/account/profile` | update name etc. |
| POST | `/account/email` | change email |
| POST | `/account/password` | change password |
| POST | `/account/image` | multipart avatar upload |
| GET | `/account/login-activity` | recent login events (IP, user agent, time) |
| GET | `/account/devices` | active device sessions |
| POST | `/account/devices/{id}/revoke` | revoke one device |
| POST | `/account/devices/revoke-others` | revoke all but the current device |
| GET | `/account/2fa` | 2FA status |
| POST | `/account/2fa/setup` | begin 2FA setup (phone number → sends code) |
| POST | `/account/2fa/enable` | confirm code → enable 2FA |
| POST | `/account/2fa/send-code` | send a code to disable 2FA |
| POST | `/account/2fa/disable` | confirm code → disable 2FA |
| GET | `/account/export` | full data export (JSON download) aggregated from every service |

**Key packages:** `entity` (`UserAccount`, `DeviceSession`, `LoginEvent`,
`TwoFactorChallenge`), `sms` (`SmsSender` abstraction; `TwilioSmsSender` +
`LoggingSmsSender` fallback for local dev), `client` (Feign clients into user, progress,
ai, badge, and interview services — used only by `DataExportService`), `messaging`
(`AuthEventPublisher` — publishes `UserRegisteredEvent`).

**Depends on:** `athena-common` (JWT issuance/validation, S3 image storage, exceptions).
**Produces:** `UserRegisteredEvent` (`athena.user.registered`) — consumed by `ai-service`
to auto-start onboarding (see `docs/Architecture.md`).

---

## user-service — Profiles & settings
**Port:** 8082 · **DB:** `athena_user` (5434 host)

| Method | Path | Purpose |
|---|---|---|
| POST | `/users` | create a profile (linked to the auth `userId`) |
| GET | `/users/{id}` | fetch a profile |
| PUT | `/users/{id}` | replace a profile |
| GET | `/users/me/settings` | current user's settings |
| PUT | `/users/me/settings` | update settings |

**Key packages:** `entity` (`UserProfile`, `UserSettings`), `mapper`
(`UserProfileMapper`). No Kafka, no Feign — the simplest service in the system.

---

## progress-service — Metrics & streaks
**Port:** 8083 · **DB:** `athena_progress` (5435 host)

| Method | Path | Purpose |
|---|---|---|
| GET | `/progress/me` | authenticated user's metrics (Redis-cached) |
| GET | `/progress/{userId}` | metrics by id |
| GET | `/progress/streaks` | streak/activity detail for the authenticated user |
| GET | `/progress/summary/{userId}` | rolling 7-day summary |
| POST | `/progress/update` | record a study session (legacy direct-write path) |

Consumes `TaskCompletedEvent` (`athena.task.completed`) to increment counters and
recompute streaks, evicting the Redis `progress` cache; publishes `StreakUpdatedEvent`
(`athena.streak.updated`) for `badge-service`. Calls `user-service` via OpenFeign
(`lb://user-service`) to confirm a user exists before writing progress.

---

## learning-service — Plans, tasks, sessions
**Port:** 8084 · **DB:** `athena_learning` (5436 host)

| Method | Path | Purpose |
|---|---|---|
| POST | `/plans` | create a learning plan |
| GET | `/plans/{id}` | fetch a plan |
| GET | `/users/{userId}/plans` | a user's plans |
| POST | `/tasks` | create a task under a plan |
| GET | `/tasks/{id}` | fetch a task |
| PATCH | `/tasks/{id}/complete` | mark COMPLETED (idempotent) |
| POST | `/sessions/start` | start a study session for a task |
| POST | `/sessions/end` | end a session, computing duration |

Publishes `LearningPlanCreatedEvent` (`athena.plan.created`) and `TaskCompletedEvent`
(`athena.task.completed`, consumed by `progress-service`).

---

## badge-service — Badge catalogue & awards
**Port:** 8085 · **DB:** `athena_badge` (5437 host)

| Method | Path | Purpose |
|---|---|---|
| GET | `/badges` | full catalogue |
| GET | `/badges/me` | authenticated user's earned badges (Redis-cached) |
| GET | `/users/{userId}/badges` | a user's earned badges |

No public "award" endpoint — badges are only ever awarded by consuming events:
`StreakUpdatedEvent` (rule-based badges — streak/task-count thresholds, see
`docs/Architecture.md`) and `BadgeSuggestionGeneratedEvent` (AI-suggested badges from
`ai-service`, validated by `BadgeSuggestionValidator` before persisting). Publishes
`BadgeAwardedEvent` (`athena.badge.awarded`). Awards are idempotent on
`(user_id, badge_id)`.

---

## ai-service — LLM orchestration (the largest module)
**Port:** 8086 · **DB:** `athena_ai` (5438 host)

The single integration point for the local LLM (LM Studio, via `athena-llm`). Owns six
sub-domains:

### Onboarding (`/ai/onboarding`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/ai/onboarding/start` | `@Deprecated` — idempotently returns the session Kafka already created |
| POST | `/ai/onboarding/goal` | submit a learning goal → AI-generated adaptive assessment |
| POST | `/ai/onboarding/assessment` | submit answers → analysis + roadmap + first daily plan |
| GET | `/ai/onboarding/me` | current onboarding session state |

Onboarding is now **event-driven**: `auth-service` publishes `UserRegisteredEvent` on
registration; `ai-service` consumes it and creates the session automatically.

### Roadmap (`/ai/roadmaps`)
| Method | Path | Purpose |
|---|---|---|
| GET | `/ai/roadmaps/me` | the caller's roadmap |
| GET | `/ai/roadmaps/{id}` | a roadmap by id |
| POST | `/ai/roadmaps/me/phases/{index}/complete` | mark a roadmap phase complete |

### Daily plan (`/ai/daily-plans`) & Daily Journey (`/daily-journey`)
Daily plan is the original single-shot AI daily plan; Daily Journey (see
`docs/Architecture.md` and `docs/DAILY-JOURNEY.md`) is its successor — a full adaptive,
block-by-block day with check-ins and reflections.

| Method | Path | Purpose |
|---|---|---|
| GET | `/ai/daily-plans/me` | today's (legacy) plan |
| GET | `/daily-journey/today` | today's mission + blocks (generates on first call) |
| GET | `/daily-journey/today/why` | cached "why Athena chose this" reasoning |
| POST | `/daily-journey/today/start` | mission → IN_PROGRESS |
| POST | `/daily-journey/today/adjust` | re-plan remaining blocks (SIMPLIFY/INTENSIFY/REGENERATE) |
| POST | `/daily-journey/today/time` | recompute the day for a new time budget |
| POST | `/daily-journey/blocks/{blockId}/start` \| `/progress` \| `/complete` \| `/skip` \| `/relink` | per-block lifecycle |
| POST | `/daily-journey/weaknesses/{knowledgeNodeId}/strengthen` | insert a priority AI drill |
| POST | `/daily-journey/checkin` | confidence check-in → AI mentor reply |
| POST | `/daily-journey/reflection` \| `/reflection/skip` | end-of-day reflection |

### Learning Sessions (`/learning-sessions`)
Per-roadmap-node generated lesson content (readings, watchings, practice, quiz), kept a
rolling 5-node lookahead ahead of the user via Kafka consumers.

| Method | Path | Purpose |
|---|---|---|
| GET | `/learning-sessions/current` | first non-completed session, full detail |
| GET | `/learning-sessions/upcoming` | not-completed sessions, summaries |
| GET | `/learning-sessions/{sessionId}` | full detail (owner only) |
| POST | `/learning-sessions/generate` | on-demand generation |
| POST | `/learning-sessions/{sessionId}/start` \| `/complete` | lifecycle |

### Knowledge Graph (`/ai/knowledge-graph`)
Per-user skill mastery graph seeded from onboarding, updated from interview weaknesses
and quiz results, with a visualization + history layer (nodes/edges/summary/insights).

| Method | Path | Purpose |
|---|---|---|
| GET | `/ai/knowledge-graph/me` \| `/{userId}` | raw node list (self or ADMIN) |
| POST | `/ai/knowledge-graph/update` | apply mastery updates |
| GET | `/ai/knowledge-graph/me/visualization` \| `/{userId}/visualization` | node/edge graph + insights |
| GET | `/ai/knowledge-graph/me/history` \| `/{userId}/history` | snapshot timeline |

### AI interview support (`/ai/interviews`, internal — called by interview-service)
| Method | Path | Purpose |
|---|---|---|
| POST | `/ai/interviews/questions` | generate interview questions for a domain/level |
| POST | `/ai/interviews/evaluate` | score submitted answers |

### AI badge suggestions (`/ai/badges`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/ai/badges/suggest` | AI proposes badges for a domain; publishes an event for `badge-service` to validate/persist |

### Retry lifecycle (`/ai/retry`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/ai/retry/{requestId}` | manually re-run a failed AI step (also runs on a schedule) |

**Key design points:** structured-output JSON schemas enforced on every generation call
(`ResponseFormat.ofSchema`) so LM Studio's free-text quirks never corrupt persistence;
`ai_requests`/`ai_responses` store only metadata (promptType, model, latency, token
count) — prompt/response content is never persisted; LM outages degrade to `503` with a
persisted retry record, never a `500`. Full detail in `docs/ARCHITECTURE-V3.md` and
`docs/DAILY-JOURNEY.md`.

**Depends on:** `athena-common`, `athena-llm`. Publishes/consumes most of the ~28 event
types in `KafkaTopics`. Uses Redis for response caching (learning sessions, KG
visualization, daily journey), all with mutation-triggered eviction.

---

## interview-service — Weekly assessment lifecycle
**Port:** 8087 · **DB:** `athena_interview` (5439 host)

| Method | Path | Purpose |
|---|---|---|
| POST | `/interviews/start` | start an interview for a domain/level |
| GET | `/interviews/me` | the caller's interviews |
| GET | `/interviews/{id}` | fetch one |
| POST | `/interviews/{id}/submit` | submit answers → AI evaluation |

Never calls LM Studio directly — every AI call goes through `ai-service` via OpenFeign
(`/ai/interviews/questions`, `/ai/interviews/evaluate`). Publishes
`InterviewStartedEvent`, `InterviewCompletedEvent`, `InterviewEvaluatedEvent` (the last
carries `domain` + `weaknesses`, consumed by `ai-service` to update the Knowledge Graph).

---

## rag-service — Memory, retrieval & recommendations
**Port:** 8088 · **DB:** `athena_rag` (5440 host, **pgvector/pgvector:pg17** image —
the only Postgres instance in the stack with the `pgvector` extension)

The newest module and the one with no prior design doc; a Retrieval-Augmented
Generation layer on top of `ai-service`'s domain data. Four sub-domains:

### Memory ingestion (`/rag/documents`, internal `/ai/memory`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/rag/documents` | ingest a document (chunked, embedded, stored as vectors) |
| DELETE | `/rag/documents/{documentId}` | remove a document and its chunks |
| POST | `/rag/reindex/me` | re-chunk/re-embed all of the caller's documents |
| GET | `/ai/memory/me` | a profile summary of what's stored for the caller |

Pipeline: `TextChunker`/`ContentPreprocessor`/`ContentHashing` split and dedupe content
→ `EmbeddingService` calls the LM Studio embeddings endpoint (`athena-llm`
`EmbeddingProvider`) → vectors are written via `JdbcChunkVectorRepository` (raw JDBC —
pgvector's type isn't a first-class Hibernate mapping) into `MemoryDocument`/`MemoryChunk`.

### Retrieval (`/rag/search`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/rag/search` | vector similarity search over the caller's memory, with snippets |

### Grounded Q&A (`/rag/query`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/rag/query` | ask a question; answer is grounded in retrieved chunks with citations |

`ContextAssembler` + `GroundingPolicy` + `PromptBuilder` assemble retrieved chunks into a
prompt, log the query (`RagQueryLog`), and return `RagAnswerResponse` with `Citation`s
back to the source chunks — this is what keeps answers traceable instead of freely
hallucinated.

### Recommendations (`/rag/recommendations/next`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/rag/recommendations/next` | "what should I do next" — pulls roadmap/knowledge-graph/progress context via Feign into an AI-generated suggestion |

Calls `RoadmapClient`, `KnowledgeGraphClient`, `LearningSessionClient`, `ProgressClient`
(all OpenFeign, `lb://ai-service` / `lb://progress-service`) to assemble context rather
than duplicating that state locally.

**Depends on:** `athena-common`, `athena-llm`. Uses Redis and Kafka
(`RagIngestionConsumer`/`RagEventPublisher`, e.g. `MemoryDocumentIndexedEvent`).

---

## Module dependency summary

```
athena-common  ← every module (JWT, ApiError, events, storage, logging)
athena-llm     ← ai-service, rag-service (LM Studio access)
api-gateway    → (routes to) every business service via Eureka
ai-service     → auth-service*, learning-service, progress-service (Feign, selective)
rag-service    → ai-service, progress-service (Feign, for recommendation context)
auth-service   → user-service, progress-service, ai-service, badge-service,
                  interview-service (Feign, DataExportService only)
interview-service → ai-service (Feign, question generation/evaluation)
progress-service  → user-service (Feign, existence check)
```
\* the earlier direct `ai-service → auth-service` Feign dependency for onboarding
greetings was removed in favor of `UserRegisteredEvent` carrying the name (see
`docs/ARCHITECTURE-V3.md` §R2); auth-service still calls *into* ai-service for exports.
