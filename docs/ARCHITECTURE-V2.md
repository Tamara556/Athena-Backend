# Athena V2 — Learning Engine & Gamification

Version 2 turns Athena into an event-driven learning platform. It adds two services
(**Learning**, **Badge**), **Kafka** for asynchronous choreography, and **Redis** for
caching — on top of the V1 core (Auth, User, Progress, Gateway, Eureka). No AI.

---

## 1. Services & infrastructure

| Component          | Port | Database (own)   | Role |
|--------------------|------|------------------|------|
| discovery-server   | 8761 | —                | Eureka registry |
| api-gateway        | 8080 | —                | Routing + central JWT validation |
| auth-service       | 8081 | athena_auth      | Register / login / JWT |
| user-service       | 8082 | athena_user      | Profiles |
| progress-service   | 8083 | athena_progress  | Metrics, streaks (Kafka consumer + producer, Redis) |
| **learning-service** | 8084 | athena_learning | Plans, tasks, sessions (Kafka producer) |
| **badge-service**    | 8085 | athena_badge    | Badge rules & awards (Kafka consumer + producer, Redis) |
| kafka              | 29092 (host) / 9092 (internal) | — | Event backbone (KRaft, single node) |
| redis              | 6379 | —                | Caches |

Postgres ports on host: auth 5433, user 5434, progress 5435, learning 5436, badge 5437.

---

## 2. Architecture diagram

```
                              ┌──────────────────────────┐
   client ── Bearer JWT ────► │       api-gateway        │ :8080
                              │  validates JWT, injects  │
                              │  X-User-Id / X-User-Roles│
                              └─────────────┬────────────┘
            ┌───────────────┬──────────────┼───────────────┬───────────────┐
            ▼               ▼              ▼                ▼               ▼
      auth-service    user-service   learning-service   progress-svc    badge-svc
                                          │  ▲              │  ▲            │  ▲
                                  produces│  │ Feign        │  │           │  │
                                          ▼  │(user exists) ▼  │           ▼  │
   ════════════════════════════════════ KAFKA ══════════════════════════════════
        athena.task.completed ─────────────────► progress-service
        athena.plan.created   (analytics)
        athena.streak.updated ─────────────────► badge-service
        athena.badge.awarded  (notifications)

   Redis ◄── progress-service (per-user stats)   badge-service (earned badges)
```

### Event choreography (the core V2 flow)

```
PATCH /tasks/{id}/complete
        │
        ▼  learning-service marks COMPLETED, publishes
   TaskCompletedEvent ──► progress-service consumes
        │                      • completedTasks += 1
        │                      • totalLearningMinutes += durationMinutes
        │                      • recompute streak (daily)
        │                      • evict Redis "progress" cache
        │                      publishes
   StreakUpdatedEvent ──► badge-service consumes
        │                      • AchievementRules.qualifiedBadges(streak, tasks)
        │                      • award newly-qualified badges (idempotent)
        │                      • evict Redis "user-badges" cache
        │                      publishes (one per new badge)
   BadgeAwardedEvent ──► (available for future notification consumers)
```

---

## 3. Kafka topics & event schemas

| Topic                   | Producer          | Consumer(s)       |
|-------------------------|-------------------|-------------------|
| `athena.task.completed` | learning-service  | progress-service  |
| `athena.plan.created`   | learning-service  | (none yet)        |
| `athena.streak.updated` | progress-service  | badge-service     |
| `athena.badge.awarded`  | badge-service     | (none yet)        |

Events are plain JSON (keyed by `userId`). Schemas (`com.athena.common.event`):

```jsonc
// TaskCompletedEvent
{ "userId","taskId","planId","taskType","durationMinutes","completedAt" }
// LearningPlanCreatedEvent
{ "planId","userId","title","createdAt" }
// StreakUpdatedEvent
{ "userId","currentStreak","longestStreak","completedTasks","occurredAt" }
// BadgeAwardedEvent
{ "userId","badgeCode","awardedAt" }
```

> **Serialization note:** events are serialized with the Boot-configured Jackson (3)
> `ObjectMapper` and sent with `String` Kafka serdes. This deliberately avoids
> spring-kafka's `JsonSerializer` type headers, keeping services decoupled and
> sidestepping the Jackson 2/3 split in Spring Boot 4.

---

## 4. Redis caching

| Service          | Cache            | Key       | Evicted when |
|------------------|------------------|-----------|--------------|
| progress-service | `progress`       | userId    | metrics updated (task completed) |
| badge-service    | `user-badges`    | userId    | a badge is awarded |
| badge-service    | `badge-catalogue`| (static)  | TTL (10 min) |

Cached DTOs implement `Serializable` (JDK serialization) so caching needs no
JSON-on-Redis configuration. Default TTL is 10 minutes.

---

## 5. Badge rules

Evaluated by `AchievementRules` from a single `StreakUpdatedEvent` (which carries
both streak and cumulative task count):

| Trigger              | Badge |
|----------------------|-------|
| completedTasks ≥ 1   | `FIRST_TASK` |
| completedTasks ≥ 10  | `TASKS_10` |
| completedTasks ≥ 50  | `TASKS_50` |
| completedTasks ≥ 100 | `TASKS_100` |
| currentStreak ≥ 7    | `STREAK_7` |
| currentStreak ≥ 14   | `STREAK_14` |
| currentStreak ≥ 30   | `STREAK_30` |
| currentStreak ≥ 100  | `STREAK_100` |

`FIRST_PLAN_COMPLETED` is seeded in the catalogue but **not auto-awarded** — the
required event set has no "plan completed" event. Awarding it is a documented
extension: learning-service would publish a `PlanCompletedEvent` when the last task
of a plan is completed, and badge-service would consume it.

Awards are **idempotent**: a unique `(user_id, badge_id)` constraint plus an
"already earned" check mean re-processing an event never double-awards.

---

## 6. API reference (new / changed in V2 — all via `http://localhost:8080`)

All endpoints require `Authorization: Bearer <token>`. The gateway validates the JWT
and forwards a trusted `X-User-Id` header; services derive the caller from it.

### Learning (learning-service)
| Method | Path | Body | Notes |
|--------|------|------|-------|
| POST | `/plans` | `{title, description}` | userId from token; emits `LearningPlanCreatedEvent` |
| GET  | `/plans/{id}` | — | |
| GET  | `/users/{userId}/plans` | — | routed to learning-service ahead of `/users/**` |
| POST | `/tasks` | `{planId, title, description, taskType, estimatedMinutes}` | `taskType ∈ READING,WATCHING,QUIZ,PRACTICE,EXERCISE` |
| GET  | `/tasks/{id}` | — | |
| PATCH| `/tasks/{id}/complete` | — | emits `TaskCompletedEvent` (idempotent) |
| POST | `/sessions/start` | `{taskId}` | moves task → IN_PROGRESS |
| POST | `/sessions/end` | `{sessionId}` | computes durationMinutes |

### Progress (progress-service)
| Method | Path | Notes |
|--------|------|-------|
| GET | `/progress/me` | authenticated user's metrics (Redis-cached) |
| GET | `/progress/{userId}` | metrics by id |
| GET | `/progress/summary/{userId}` | rolling 7-day summary (V1) |

### Badges (badge-service)
| Method | Path | Notes |
|--------|------|-------|
| GET | `/badges` | full catalogue (9 badges) |
| GET | `/badges/me` | authenticated user's earned badges (Redis-cached) |
| GET | `/users/{userId}/badges` | a user's earned badges |

---

## 7. Security

Unchanged model from V1: the **gateway is the only JWT validator**. It strips any
client-supplied `X-User-Id`/`X-User-Roles` and re-injects verified values from the
token. Downstream services trust those headers (services are not exposed publicly).
Every `/plans`, `/tasks`, `/sessions`, `/progress`, `/badges` route is protected;
only `/auth/**` and `/actuator/**` are public at the gateway.

---

## 8. Observability

- **Structured logging** — ECS JSON in containers (`LOGGING_STRUCTURED_FORMAT=ecs`).
- **Request tracing** — `micrometer-tracing-bridge-brave` on the V2 services; trace
  and span IDs propagate across HTTP and Kafka and appear in the structured logs
  (`management.tracing.sampling.probability=1.0`). Point at Zipkin/Tempo to export.
- **Global exception handling** — shared `GlobalExceptionHandler` → uniform `ApiError`.

---

## 9. Running V2

```bash
docker compose up --build
```

Brings up Eureka, Kafka (KRaft), Redis, five Postgres instances, and all services.
Wait ~60–90s for registration. Then use `postman/Athena-V2.postman_collection.json`
(Register → Create Profile → Create Plan → Create Task → Complete Task → My Progress
→ My Badges). Completing a task propagates through Kafka in ~1–2s.

Kafka is reachable from the host at `localhost:29092` (services use `kafka:9092`).

### Verified end-to-end
`complete task` → `GET /progress/me` shows `completedTasks=1, streak=1, minutes=30`
→ `GET /badges/me` shows `FIRST_TASK`. The full Kafka chain, Jackson-3 event
serialization, and Redis caching were confirmed against the running stack.
