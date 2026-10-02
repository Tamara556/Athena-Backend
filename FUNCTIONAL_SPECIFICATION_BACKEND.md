# Athena — Backend Functional Specification

> Source of truth for Playwright E2E test planning (API-level). Documents **only implemented** functionality, reverse-engineered from the Spring Boot codebase (`C:\Users\muraz\IdeaProjects\Athena-Backend-Parent`). Companion: `FUNCTIONAL_SPECIFICATION_FRONTEND.md`.

---

## 0. System Overview

Maven multi-module Spring Boot backend. Stack: Java 26, Spring Boot 4, Spring Cloud, PostgreSQL (database-per-service), Liquibase, Kafka (event-driven), Redis (caching), Micrometer/Brave tracing, JJWT (HS256). Local LM Studio model = "the brain" for AI. Docker Compose orchestrates the stack.

### 0.1 Modules & ports

| Module | Port | Role |
|---|---|---|
| discovery-server | 8761 | Eureka service registry |
| **api-gateway** | **8080** | **Only public port.** WebFlux. Validates JWT, injects identity headers, routes. |
| auth-service | 8081 | Register/login/refresh/2FA, account, devices, avatar, data export. Publishes `UserRegisteredEvent`. |
| user-service | 8082 | User profiles + user settings. |
| progress-service | 8083 | Progress, streaks, weekly summary. Consumes `TaskCompletedEvent`, `InterviewEvaluatedEvent`; publishes `StreakUpdatedEvent`. |
| learning-service | 8084 | Plans, tasks, learning sessions. Publishes `TaskCompletedEvent`, `LearningPlanCreatedEvent`. |
| badge-service | 8085 | Badge catalog + awards. Consumes `StreakUpdatedEvent`, `BadgeSuggestionGeneratedEvent`; publishes `BadgeAwardedEvent`. |
| ai-service | 8086 | "The brain": onboarding, roadmaps, daily plans, daily journey, learning sessions, knowledge graph + visualization, AI interviews, badge suggestions, insights, retry. Consumes `UserRegisteredEvent`, `InterviewEvaluatedEvent`, `RoadmapGeneratedEvent`, `LearningSessionCompletedEvent`. |
| interview-service | 8087 | Interview lifecycle (start/submit); Feign → ai-service. |
| rag-service | (reg.) | RAG: document ingest/embeddings, retrieval/search, query, memory profile, recommendations. |
| athena-llm | (lib) | `ChatProvider`/`EmbeddingProvider` abstraction; LM Studio SPI. |
| athena-common | (lib) | JwtService, AuthHeaders, event POJOs, exceptions, ImageStorage. |

### 0.2 Security model (centralized edge auth)

- **Gateway** is the only public entry point. `JwtAuthenticationFilter` (order HIGHEST_PRECEDENCE+10):
  - `OPTIONS` requests pass through (CORS preflight).
  - **Public prefixes:** `/auth/`, `/actuator/` (and exact `/actuator`). For public paths, the filter STRIPS any inbound `X-User-Id` / `X-User-Roles` headers (anti-spoofing).
  - All other paths require `Authorization: Bearer <accessToken>`. Missing/malformed → **401** JSON `{status,error,message,path}`.
  - Valid ACCESS token → injects `X-User-Id` (subject) and `X-User-Roles` (comma-joined) downstream. Invalid/expired → **401**.
- **Downstream services TRUST the gateway headers** and do NOT re-validate JWTs. They read `@RequestHeader(AuthHeaders.USER_ID)` = `X-User-Id`.
- **Tokens:** Access token (short-lived, e.g. 15m), Refresh token (long-lived, e.g. 30d). BCrypt password hashing. Refresh is tied to a device session (see auth-service). Roles: `USER` (default), `ADMIN`.
- JWT secret/issuer from `athena.security.jwt.*` (env `ATHENA_JWT_SECRET`).

### 0.3 Gateway route table (`api-gateway/application.yml`)

| Path predicate | Target service |
|---|---|
| `/auth/**` | auth-service |
| `/account/**` | auth-service |
| `/users/*/plans` | learning-service |
| `/users/*/badges` | badge-service |
| `/users/**` | user-service |
| `/progress/**` | progress-service |
| `/plans/**`, `/tasks/**`, `/sessions/**` | learning-service |
| `/badges/**` | badge-service |
| `/ai/memory/**` | rag-service |
| `/rag/**` | rag-service |
| `/ai/**` | ai-service |
| `/learning-sessions/**` | ai-service |
| `/daily-journey/**` | ai-service |
| `/interviews/**` | interview-service |

> Note: `/interviews/**` routes to **interview-service** (real lifecycle), while `/ai/interviews/**` routes to **ai-service** (question generation & evaluation). Ordering matters: `/users/*/plans` and `/users/*/badges` are matched before the generic `/users/**`.

### 0.4 Common error contract

Services use shared exceptions surfaced as JSON `ApiError`:
- `DuplicateResourceException` → **409**
- `InvalidCredentialsException` → **401**
- `ResourceNotFoundException` → **404**
- `AccessForbiddenException` → **403**
- `AiTemporarilyUnavailableException` → AI down; returns a retry id (used to reschedule generation).
- Bean-validation failures (`@Valid`) → **400** with field messages.

---

## 1. auth-service (8081)

Database: users, login events, two-factor challenges, device sessions.

### 1.1 `AuthController` — `/auth`

| Method | Path | Auth | Body | Response |
|---|---|---|---|---|
| POST | `/auth/register` | public | multipart: `firstName,lastName,username,email,password` + optional `image` | **201** `AuthResponse` |
| POST | `/auth/login` | public | `{login,password}` | **200** `AuthResponse` (or 2FA challenge) |
| POST | `/auth/2fa/verify` | public | `{challengeToken,code}` | **200** `AuthResponse` |
| POST | `/auth/refresh` | public | `{refreshToken}` | **200** `AuthResponse` |
| GET | `/auth/users/{userId}/image` | public | — | **200** image bytes (private cache 1h) or **404** |

**`AuthResponse` fields:** `userId, username, firstName, lastName, email, roles, tokenType="Bearer", accessToken, refreshToken, accessTokenTtlSeconds, imageName, twoFactorRequired, challengeToken, sessionId`.

**Register logic:**
- Normalize email + username (trim, lowercase).
- **409** if email exists (`existsByEmailIgnoreCase`) or username exists.
- Hash password (BCrypt), assign role `USER`, persist. Store optional avatar (S3/LocalStack); set `imageName`.
- **Publish `UserRegisteredEvent`** (`athena.user.registered`) → ai-service creates onboarding session.
- Open a device session (with IP/User-Agent) and issue token pair. Returns 201.

**Register validation (`RegisterRequest`):** firstName/lastName `@NotBlank`, `@Size(max 100)`; username `@NotBlank`, `@Pattern ^[A-Za-z0-9._-]{3,50}$`; email `@NotBlank @Email`; password `@NotBlank @Size(8..72) @Pattern` complexity (lower+upper+digit+symbol).

**Login logic:**
- Lookup by email OR username (lowercased). **401** if not found or password mismatch.
- If 2FA enabled: generate code, save `TwoFactorChallenge` (hashed code, **5-min TTL**), send SMS, return `AuthResponse.twoFactorChallenge(challengeId)` (`twoFactorRequired=true`, `challengeToken=challengeId`). **No tokens yet.**
- Else: record login event (IP + truncated UA ≤ 400), open device session, issue tokens.

**2FA verify logic:** parse challengeToken as UUID (else 401); load challenge (else 401); if expired → delete + 401; load account; if 2FA disabled or code mismatch → 401; delete challenge; record login; issue tokens.

**Refresh logic:** validate refresh JWT (else 401); find active device session by refresh token (else 401); load account (else 401); rotate refresh token on the same session; issue new token pair.

**Client IP:** from `X-Forwarded-For` (first) else remote addr.

**Kafka:** publishes `UserRegisteredEvent`.

### 1.2 `AccountController` — `/account` (all require `X-User-Id`)

| Method | Path | Body | Notes |
|---|---|---|---|
| GET | `/account/me` | — | `AccountResponse` |
| GET | `/account/devices?current={sessionId}` | — | list device sessions (marks current) |
| POST | `/account/devices/{id}/revoke` | — | **204**; revoke a session |
| POST | `/account/devices/revoke-others?current={sessionId}` | — | **204**; revoke all but current |
| GET | `/account/export` | — | JSON blob `athena-data-export.json` (Content-Disposition attachment) |
| GET | `/account/login-activity` | — | list of login events (ip, userAgent, createdAt) |
| GET | `/account/2fa` | — | `{enabled, phoneNumber}` |
| POST | `/account/2fa/setup` | `{phoneNumber}` | texts code; returns masked `{phoneNumber}` |
| POST | `/account/2fa/enable` | `{code}` | verifies + enables; `{enabled,phoneNumber}` |
| POST | `/account/2fa/send-code` | — | texts a fresh code (for disabling) |
| POST | `/account/2fa/disable` | `{code}` | verifies + disables |
| PATCH | `/account/profile` | `{firstName,lastName}` | `AccountResponse` |
| POST | `/account/email` | `{newEmail,currentPassword}` | `AccountResponse` |
| POST | `/account/password` | `{currentPassword,newPassword}` | **204** |
| POST | `/account/image` | multipart `image` | `AccountResponse` |

**Validation:** phone `^\+?[0-9]{8,15}$`; password complexity `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{8,72}$`; email `@Email`; names `@NotBlank`.

**Failure states:** wrong current password on email/password change → 400/401 (invalid credentials); invalid 2FA code → error; revoke unknown device → handled by service.

---

## 2. user-service (8082)

### 2.1 `UserController` — `/users`

| Method | Path | Auth | Body | Response |
|---|---|---|---|---|
| POST | `/users` | authed | `CreateUserProfileRequest` | **201** + `Location` |
| GET | `/users/{id}` | authed | — | `UserProfileResponse` |
| PUT | `/users/{id}` | authed | `UpdateUserProfileRequest` | `UserProfileResponse` |

### 2.2 `UserSettingsController` — `/users/me/settings` (require `X-User-Id`)

| Method | Path | Body | Response |
|---|---|---|---|
| GET | `/users/me/settings` | — | `SettingsResponse` |
| PUT | `/users/me/settings` | `UpdateSettingsRequest` | `SettingsResponse` |

**Settings bundle:** learning (availability/difficulty/style), experience (tone + motivational/reflection/adaptive), notifications (dailyReminder/weeklySummary/interviewReminders/milestones), privacy (personalize/shareAnon).

---

## 3. progress-service (8083)

### 3.1 `ProgressController` — `/progress`

| Method | Path | Auth | Response |
|---|---|---|---|
| GET | `/progress/me` | `X-User-Id` | `ProgressResponse` |
| GET | `/progress/streaks` | `X-User-Id` | `StreakActivityResponse` |
| GET | `/progress/{userId}` | authed | `ProgressResponse` |
| POST | `/progress/update` | authed | `ProgressResponse` |
| GET | `/progress/summary/{userId}` | authed | `WeeklySummaryResponse` |

**`ProgressResponse`:** userId, currentStreak, longestStreak, totalCompletedTasks, etc.
**`StreakActivityResponse`:** activity-by-date, summary, yearly, first-active date (drives the Streaks calendar UI).

### 3.2 Kafka (`ProgressEventConsumer` / `ProgressEventPublisher`)

- **Consumes `TaskCompletedEvent`** (`athena.task.completed`): `progressService.update(+1 task, +durationMinutes)` → then **publishes `StreakUpdatedEvent`** (`athena.streak.updated`) with currentStreak/longestStreak/completedTasks.
- **Consumes `InterviewEvaluatedEvent`** (`athena.interview.evaluated`): `recordInterview(userId)`.

---

## 4. learning-service (8084)

### 4.1 `PlanController`

| Method | Path | Auth | Body |
|---|---|---|---|
| POST | `/plans` | `X-User-Id` | `CreatePlanRequest` → 201 + Location |
| GET | `/plans/{id}` | authed | `PlanResponse` |
| GET | `/users/{userId}/plans` | authed | `List<PlanResponse>` |

### 4.2 `TaskController` — `/tasks`

| Method | Path | Auth | Body |
|---|---|---|---|
| POST | `/tasks` | authed | `CreateTaskRequest` → **201** |
| GET | `/tasks/{id}` | authed | `TaskResponse` |
| PATCH | `/tasks/{id}/complete` | authed | `TaskResponse` |

Completing a task publishes **`TaskCompletedEvent`** (`athena.task.completed`) → consumed by progress-service.

### 4.3 `SessionController` — `/sessions`

| Method | Path | Auth | Body |
|---|---|---|---|
| POST | `/sessions/start` | `X-User-Id` | `StartSessionRequest` → **201** `SessionResponse` |
| POST | `/sessions/end` | authed | `EndSessionRequest` → `SessionResponse` |

**Kafka:** publishes `LearningPlanCreatedEvent`, `TaskCompletedEvent`.

---

## 5. badge-service (8085)

### 5.1 `BadgeController`

| Method | Path | Auth | Response |
|---|---|---|---|
| GET | `/badges` | authed | `List<BadgeResponse>` (catalog) |
| GET | `/badges/me` | `X-User-Id` | `List<UserBadgeResponse>` (earned; code + awardedAt) |
| GET | `/users/{userId}/badges` | authed | `List<UserBadgeResponse>` |

### 5.2 Kafka (`BadgeEventConsumer` / `BadgeEventPublisher`)

- **Consumes `StreakUpdatedEvent`**: `award(userId, currentStreak, completedTasks)` → for each newly awarded badge, **publishes `BadgeAwardedEvent`** (`athena.badge.awarded`).
- **Consumes `BadgeSuggestionGeneratedEvent`**: `awardSuggested(userId, suggestions)` → publishes `BadgeAwardedEvent` per award.

Award rules are threshold-based (streak/task counts) plus AI-suggested badges.

---

## 6. ai-service (8086) — "the brain"

Databases: onboarding sessions, assessments, generated roadmaps, generated daily plans, knowledge nodes/snapshots, learning sessions + node buffer, daily journeys/blocks, retry records.

### 6.1 `OnboardingController` — `/ai/onboarding` (require `X-User-Id`)

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/ai/onboarding/start` | — | **201** `StartOnboardingResponse` (deprecated; idempotent) |
| POST | `/ai/onboarding/goal` | `{goal}` | `AssessmentResponse` (AI questions) |
| POST | `/ai/onboarding/assessment` | `{answers:[{question,answer}]}` | `OnboardingResultResponse` (analysis + roadmap + daily plan) |
| GET | `/ai/onboarding/me` | — | `OnboardingStateResponse` |

**Onboarding pipeline (`OnboardingServiceImpl`):**
1. **On `UserRegisteredEvent`**: `createSessionFromRegistration` — creates one `OnboardingSession` (idempotent per user) and publishes `UserOnboardingStartedEvent` (`athena.onboarding.started`).
2. **submitGoal:** save goal, status → `GOAL_SET`, publish `GoalDiscoveredEvent`; call `generation.generateAssessment(userId, goal)` (AI) → persist questions → return them. On `AiException` → schedule retry (`AiTemporarilyUnavailableException` with retryId).
3. **submitAssessment:** requires goal set (else 400); persist answers; publish `AssessmentCompletedEvent`; then `completeOnboardingForSession`:
   - `analyzeGoal` (AI) → domain/level/estimatedMonths/dailyHours/prerequisites; publish `GoalAnalyzedEvent`.
   - `generateRoadmap` (AI) → phases; **status assigned**: index0=CURRENT, index1=AVAILABLE, rest=LOCKED; persist; publish **`RoadmapGeneratedEvent`** (→ learning-session buffer).
   - `seedSkills` into knowledge graph (prerequisites).
   - `generateDailyPlan` (AI) for first phase; persist; publish `DailyPlanGeneratedEvent`. (Daily-plan/KG failure is non-fatal — roadmap still returned.)
   - session status → COMPLETED.
- Validation: goal `@NotBlank @Size(max)`.

### 6.2 `RoadmapController` — `/ai/roadmaps`

| Method | Path | Auth | Response |
|---|---|---|---|
| GET | `/ai/roadmaps/me` | `X-User-Id` | latest `RoadmapResponse` (goal, level, phases[]) |
| GET | `/ai/roadmaps/{id}` | authed | `RoadmapResponse` |

*(Phase completion `POST /ai/roadmaps/me/phases/{index}/complete` is used by the frontend — persists status shift this→COMPLETED, next→CURRENT, next+1→AVAILABLE, returns updated roadmap.)*

### 6.3 `DailyPlanController` — `/ai/daily-plans`
`GET /ai/daily-plans/me` → latest `DailyPlanResponse` (date + items[type,title,description,estimatedMinutes]).

### 6.4 `DailyJourneyController` — `/daily-journey` (require `X-User-Id`)

| Method | Path | Body | Purpose |
|---|---|---|---|
| GET | `/daily-journey/today` | — | today's `DailyJourneyResponse` |
| GET | `/daily-journey/today/why` | — | `WhyReasoning` (AI) |
| POST | `/daily-journey/today/start` | — | begin day |
| POST | `/daily-journey/today/adjust` | `{action}` | adjust plan (`REGENERATE`, etc.) |
| POST | `/daily-journey/today/time` | `{availableMinutes}` | resize the day |
| POST | `/daily-journey/blocks/{blockId}/start` | — | start a block |
| POST | `/daily-journey/blocks/{blockId}/progress` | `{percent}` | progress |
| POST | `/daily-journey/blocks/{blockId}/complete` | — | complete |
| POST | `/daily-journey/blocks/{blockId}/skip` | `{reason?}` (optional body) | skip |
| POST | `/daily-journey/blocks/{blockId}/relink` | — | re-add skipped block |
| POST | `/daily-journey/weaknesses/{knowledgeNodeId}/strengthen` | — | generate weakness drill (AI) |
| POST | `/daily-journey/checkin` | `{confidence,blockId?}` | mentor check-in → AI reply |
| POST | `/daily-journey/reflection` | `{hardestPart,whatClicked,adjustRequest}` | save reflection |
| POST | `/daily-journey/reflection/skip` | — | skip reflection |

Enums: `AdjustAction`, `ConfidenceLevel` (CONFIDENT/UNSURE/NEED_HELP), block type (READING/PRACTICE/VIDEO/QUIZ/SPEAKING/REVIEW/DRILL), difficulty (EASY/MODERATE/CHALLENGING).
**Kafka:** daily.* events (mission generated/adjusted, block completed/skipped, checkin recorded, reflection saved).
**AI:** mission plan, mentor reply, weakness drill, why-reasoning.

### 6.5 `LearningSessionController` — `/learning-sessions` (require `X-User-Id`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/learning-sessions/current` | current session (**404** if none/not ready) |
| POST | `/learning-sessions/generate` | seed buffer from latest roadmap; return current |
| GET | `/learning-sessions/upcoming` | list of upcoming summaries |
| GET | `/learning-sessions/{sessionId}` | session by id (**404** if not ready) |
| POST | `/learning-sessions/{sessionId}/start` | mark IN_PROGRESS |
| POST | `/learning-sessions/{sessionId}/complete` | mark COMPLETED |

**Content shape:** readings[], watchings[] (title, description, videoQuery, videoId), practices[] (practiceType ∈ CODE_EDITOR/LANGUAGE_EXERCISE/SCENARIO/CREATIVE_PROMPT/REFLECTION), quizzes[] (type ∈ SINGLE_CHOICE/MULTIPLE_CHOICE/TRUE_FALSE, options, correctAnswer, explanation). Status ∈ NOT_STARTED/IN_PROGRESS/COMPLETED.
**Buffering:** on `RoadmapGeneratedEvent` → `generateInitialBuffer`; on `LearningSessionCompletedEvent` → `refillBuffer` (rolling lookahead ~5 nodes). Publishes learning.session.* + `NodeBufferRefilledEvent`.
**AI:** lesson content generation (slow on first miss — frontend polls).

### 6.6 `KnowledgeGraphController` — `/ai/knowledge-graph` (require `X-User-Id`)

| Method | Path | Auth notes |
|---|---|---|
| GET | `/ai/knowledge-graph/me` | own nodes |
| GET | `/ai/knowledge-graph/{userId}` | any (path-based) |
| POST | `/ai/knowledge-graph/update` | apply `UpdateKnowledgeRequest` |
| GET | `/ai/knowledge-graph/me/visualization` | own viz (nodes, edges, summary, insights); increments metric |
| GET | `/ai/knowledge-graph/{userId}/visualization` | **403** unless caller==target or role contains `ADMIN` |
| GET | `/ai/knowledge-graph/me/history` | snapshot summaries |
| GET | `/ai/knowledge-graph/{userId}/history` | **403** unless self/ADMIN |

**Authorization:** `assertCanAccess` — non-self access requires `X-User-Roles` to contain `ADMIN`, else `AccessForbiddenException` (**403**). This is the one place RBAC is enforced downstream.
**Kafka:** `KnowledgeGraphUpdatedEvent`, visualization/snapshot events. Weaknesses recorded from `InterviewEvaluatedEvent`.

### 6.7 `AiInterviewController` — `/ai/interviews`

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/ai/interviews/questions` | `GenerateInterviewQuestionsRequest{domain,level}` | `InterviewQuestions` (AI) |
| POST | `/ai/interviews/evaluate` | `EvaluateInterviewRequest` | `InterviewEvaluation` (AI) |

Called by interview-service (Feign). Not directly by the frontend.

### 6.8 `AiBadgeController` — `/ai/badges`
`POST /ai/badges/suggest` `{domain}` (require `X-User-Id`) → `List<BadgeSuggestion>` (AI). May publish `BadgeSuggestionGeneratedEvent` → badge-service awards.

### 6.9 `RetryController` — `/ai/retry/{requestId}`
`POST` → `RetryOutcomeResponse`. Re-runs a previously scheduled AI generation (used when `AiTemporarilyUnavailableException` was thrown, e.g. onboarding assessment/complete during model downtime).

### 6.10 Insights — `/ai/insights/me`
Returns the AI-authored learner-model `InsightsProfile` (letter, learning style, patterns, strengths, explorations, evolution, potential, future letter). Consumed by the Insights page.

### 6.11 AI generation internals (`AiGenerationServiceImpl`)

All generation goes through `LlmService.generateJson(...)` with a rendered prompt template + a **JSON schema** (`ResponseFormat.ofSchema`) so the local model returns structured JSON. Generators: assessment, goal analysis, roadmap, daily plan, learning session, daily mission, why-reasoning, weakness drill, mentor reply, interview questions, interview evaluation, badge suggestions. **Latency is high** (local LM Studio) — expect multi-minute responses; failures raise `AiException` → retry scheduling.

---

## 7. interview-service (8087)

### 7.1 `InterviewController` — `/interviews` (require `X-User-Id`)

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/interviews/start` | `StartInterviewRequest` | **201** `InterviewResponse` |
| GET | `/interviews/me` | — | `List<InterviewResponse>` |
| GET | `/interviews/{id}` | authed | `InterviewResponse` |
| POST | `/interviews/{id}/submit` | `SubmitInterviewRequest` | `InterviewResultResponse` |

**Logic:** `start` calls ai-service (Feign) to generate questions; `submit` calls ai-service to evaluate answers → publishes **`InterviewEvaluatedEvent`** (`athena.interview.evaluated`) → consumed by ai-service (record weaknesses in KG) and progress-service (record interview). Also `InterviewStartedEvent`/`InterviewCompletedEvent`. A weekly cron for scheduled interviews exists but roster-selection wiring is incomplete.

> **Frontend note:** the Interviews UI is currently mock and does NOT call these endpoints — but they are live and testable directly at the API level.

---

## 8. rag-service

Databases: memory documents, embeddings/chunks (pgvector-style retrieval), profiles.

### 8.1 `DocumentController` (require `X-User-Id`)

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/rag/documents` | `IngestDocumentRequest{title,content,learningDomain,category?,visibility?}` | **201** `DocumentResponse` (chunks + embeds) |
| DELETE | `/rag/documents/{documentId}` | — | **204** |
| POST | `/rag/reindex/me` | — | `ReindexResponse` (re-embed user docs) |

Defaults: sourceType `MATERIAL`, category `MATERIAL`, visibility `PRIVATE`.

### 8.2 `SearchController`
`POST /rag/search` (require `X-User-Id`) `SearchRequest{query,sourceTypes?,domain?,page?,size?}` → `SearchResponse{query,page,size,total,results[]}`. Size defaults to `retrievalTopK`, capped at `maxSearchResults`; min similarity filter applied; snippet length 280.

### 8.3 `RagController`
`POST /rag/query` (require `X-User-Id`) `RagQueryRequest{query,...}` → `RagAnswerResponse` — retrieval-augmented answer (retrieves user chunks + calls LLM).

### 8.4 `RecommendationController`
`POST /rag/recommendations/next` (require `X-User-Id`, optional body) → `NextRecommendationResponse` — next best action from memory profile. Publishes `RecommendationGeneratedEvent`.

### 8.5 `MemoryController` — `/ai/memory/me`
`GET` (require `X-User-Id`) → `MemoryProfileResponse` — the learner's aggregated memory profile.

### 8.6 Kafka (`RagIngestionConsumer` / `RagEventPublisher`)
Consumes learning/domain events to ingest material; publishes `MemoryDocumentIndexedEvent` (`athena.memory.document.indexed`).

---

## 9. Kafka topics (full registry — `KafkaTopics`)

| Constant | Topic |
|---|---|
| TASK_COMPLETED | athena.task.completed |
| PLAN_CREATED | athena.plan.created |
| STREAK_UPDATED | athena.streak.updated |
| BADGE_AWARDED | athena.badge.awarded |
| ONBOARDING_STARTED | athena.onboarding.started |
| GOAL_DISCOVERED | athena.goal.discovered |
| ASSESSMENT_COMPLETED | athena.assessment.completed |
| GOAL_ANALYZED | athena.goal.analyzed |
| ROADMAP_GENERATED | athena.roadmap.generated |
| DAILY_PLAN_GENERATED | athena.dailyplan.generated |
| RECOMMENDATION_GENERATED | athena.recommendation.generated |
| KNOWLEDGE_GRAPH_UPDATED | athena.knowledge.updated |
| INTERVIEW_STARTED/COMPLETED/EVALUATED | athena.interview.* |
| USER_REGISTERED | athena.user.registered |
| BADGE_SUGGESTION_GENERATED | athena.badge.suggestion.generated |
| KNOWLEDGE_GRAPH_VISUALIZATION_GENERATED | athena.knowledge.visualization.generated |
| KNOWLEDGE_GRAPH_SNAPSHOT_CREATED | athena.knowledge.snapshot.created |
| LEARNING_SESSION_GENERATED/STARTED/COMPLETED | athena.learning.session.* |
| NODE_BUFFER_REFILLED | athena.learning.node.buffer.refilled |
| DAILY_MISSION_GENERATED/ADJUSTED | athena.daily.mission.* |
| DAILY_BLOCK_COMPLETED/SKIPPED | athena.daily.block.* |
| DAILY_CHECKIN_RECORDED | athena.daily.checkin.recorded |
| DAILY_REFLECTION_SAVED | athena.daily.reflected |
| MEMORY_DOCUMENT_INDEXED | athena.memory.document.indexed |

Events are published as JSON strings keyed by `userId` (`AiEventPublisher.publish(topic, userId, event)`).

---

## 10. End-to-end business workflows (event choreography)

### W1 — Registration → onboarding session (async)
`POST /auth/register` → auth-service saves user, issues tokens (**201**), publishes `UserRegisteredEvent` → **ai-service** `AiEventConsumer.onUserRegistered` → creates onboarding session + `UserOnboardingStartedEvent`.
**Test implication:** onboarding session appears shortly after register (eventual). `POST /ai/onboarding/start` is idempotent and also ensures the session.

### W2 — Goal → assessment → roadmap → daily plan (mostly synchronous AI)
`POST /ai/onboarding/goal` → `GoalDiscoveredEvent` + AI assessment. `POST /ai/onboarding/assessment` → `AssessmentCompletedEvent` → AI goal analysis (`GoalAnalyzedEvent`) → AI roadmap (`RoadmapGeneratedEvent`) → KG seed → AI daily plan (`DailyPlanGeneratedEvent`). Returns analysis+roadmap+plan.

### W3 — Roadmap generated → learning-session buffer
`RoadmapGeneratedEvent` → ai-service `LearningSessionEventConsumer.onRoadmapGenerated` → `generateInitialBuffer` (AI generates first lessons). `GET /learning-sessions/current` returns **404** until the first lesson is ready (frontend polls/generates).

### W4 — Lesson completion → buffer refill
`POST /learning-sessions/{id}/complete` → `LearningSessionCompletedEvent` → `refillBuffer` (keeps rolling lookahead).

### W5 — Task completion → streak → badge
learning-service `PATCH /tasks/{id}/complete` → `TaskCompletedEvent` → progress-service updates + `StreakUpdatedEvent` → badge-service `award(...)` → `BadgeAwardedEvent`. Streaks/badges then visible via `/progress/streaks` and `/badges/me`.

### W6 — Interview → evaluation → KG weaknesses + progress
interview-service `POST /interviews/{id}/submit` → (Feign) ai-service evaluate → `InterviewEvaluatedEvent` → ai-service records KG weaknesses; progress-service records interview. Weaknesses then surface in the Daily Journey and Knowledge Graph.

### W7 — Badge suggestions (AI)
`POST /ai/badges/suggest` → `BadgeSuggestionGeneratedEvent` → badge-service `awardSuggested` → `BadgeAwardedEvent`.

### W8 — RAG ingest → indexed
Material ingested (`POST /rag/documents` or event-driven) → embeddings stored → `MemoryDocumentIndexedEvent`. Later retrieval (`/rag/search`, `/rag/query`, `/rag/recommendations/next`) uses these + LLM.

---

## 11. Edge cases (API-level test checklist)

| Category | Case / expected |
|---|---|
| Missing/invalid JWT | Any non-public path without valid Bearer → gateway **401** JSON. |
| Header spoofing | Injecting `X-User-Id` on a public path → stripped by gateway. |
| Expired access token | **401** at gateway; client must refresh via `/auth/refresh`. |
| Refresh invalid / session revoked | `/auth/refresh` **401** (`REFRESH_TOKEN_INVALID`). |
| Duplicate register | email/username exists → **409**. |
| Register validation | bad username pattern / weak password / bad email → **400**. |
| Login wrong creds | **401** (same message for unknown user & bad password). |
| 2FA | challenge TTL 5 min; expired → 401 + challenge deleted; wrong/format code → 401; 2FA disabled mid-flow → 401. |
| Onboarding order | assessment before goal → **400** ("Goal must be submitted…"). |
| AI unavailable | onboarding goal/assessment → `AiTemporarilyUnavailableException` with retryId; retry via `POST /ai/retry/{id}`. |
| AI slow | roadmap/lesson/mission generation multi-minute; `/learning-sessions/current` returns 404 until ready. |
| KG cross-user access | `/ai/knowledge-graph/{otherUserId}/visualization` without ADMIN → **403**. |
| Empty resources | no roadmap/plan → 404/empty; `/badges/me`, `/progress/streaks` empty for new users. |
| Data export | returns JSON attachment; downstream-only via `X-User-Id`. |
| Revoke current device | invalidates that session's refresh token (client is logged out on next 401). |
| Idempotency | onboarding session creation idempotent per user. |
| Downstream trust | downstream services accept any request the gateway forwards (no per-service JWT check) — direct-to-service calls bypass auth in a compromised network (defense is the gateway). |

---

## Appendix A — Auth constants (validation reference)

| Field | Rule |
|---|---|
| First/Last name | required, ≤ 100 chars |
| Username | `^[A-Za-z0-9._-]{3,50}$` (3–50) |
| Password | 8–72 chars, must contain lower + upper + digit + symbol |
| Phone (2FA) | `^\+?[0-9]{8,15}$` |
| Email | RFC-ish `@Email` |
| 2FA code | 6 digits (client), hashed + 5-min TTL (server) |

## Appendix B — Identity headers (athena-common `AuthHeaders`)

- `X-User-Id` — user UUID (injected by gateway from JWT subject).
- `X-User-Roles` — comma-separated roles (e.g. `USER` or `USER,ADMIN`).
