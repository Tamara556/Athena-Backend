# Athena V3 — Intelligent Onboarding & Adaptive Learning Core

V3 makes Athena an AI mentor. It adds **ai-service** (the brain, integrating a local
**LM Studio** model) and **interview-service** (weekly AI-evaluated assessments), on top
of the V1/V2 platform. No external paid AI — LM Studio only.

---

## 1. New services & ports

| Service           | Port | DB                | Role |
|-------------------|------|-------------------|------|
| **ai-service**    | 8086 | athena_ai (5438)  | LM Studio integration, onboarding, goal analysis, roadmap & daily-plan generation, interview Q-gen + evaluation |
| **interview-service** | 8087 | athena_interview (5439) | Weekly interview lifecycle; calls ai-service for generation/evaluation |

LM Studio is expected at `http://localhost:1234/v1` (host). In Docker the ai-service
reaches it via `host.docker.internal`.

---

## 2. Architecture

```
client ─JWT─▶ api-gateway ─┬─▶ auth-service ──(internal)──┐
                           │                              │ firstName
                           ├─▶ ai-service ◀───────────────┘  (Feign /internal/accounts)
                           │      │  WebClient (retries, timeouts)
                           │      ▼
                           │   LM Studio  (OpenAI-compatible, local)  http://localhost:1234/v1
                           │
                           └─▶ interview-service ──Feign──▶ ai-service (/ai/interviews/*)

  Kafka events: onboarding.started, goal.discovered, assessment.completed, goal.analyzed,
                roadmap.generated, dailyplan.generated, interview.{started,completed,evaluated}
  Redis (30-min TTL): onboarding / prompt / AI-response / recommendation caches
```

### Why this shape
- **ai-service owns all LLM interaction.** interview-service never calls LM Studio
  directly — it calls ai-service over Feign. One integration point, one place to harden.
- **LM Studio via WebClient** with connect/response timeouts and bounded retries on
  transient failures only (`AiProperties`). Failures surface as `503`, never `500`.
- **Robust JSON.** Local models wrap JSON in prose/fences; `JsonExtractor` pulls the
  first balanced object/array before Jackson parsing.
- **Privacy.** `ai_requests`/`ai_responses` store only metadata (promptType, model,
  latency, token count, success) — never prompt content.

---

## 3. Onboarding sequence (the core flow)

```
Register (auth-service)  ──▶  POST /ai/onboarding/start
                                  • ai-service fetches firstName from auth (internal Feign)
                                  • greets: "Hello, Roza 👋 ... What would you like to learn?"
                                  • emits onboarding.started
POST /ai/onboarding/goal {goal}
                                  • LM generates a domain-specific adaptive assessment
                                  • stores assessment, emits goal.discovered
                                  • returns questions
POST /ai/onboarding/assessment {answers[]}
                                  • LM analyzes goal+answers -> {domain, level, estimatedMonths, dailyHours, prerequisites}
                                  • LM generates roadmap (phases) -> stored
                                  • LM generates first daily plan -> stored
                                  • emits assessment.completed, goal.analyzed, roadmap.generated, dailyplan.generated
                                  • returns {analysis, roadmap, dailyPlan}  ← user enters dashboard
GET /ai/roadmaps/me , GET /ai/daily-plans/me , GET /ai/onboarding/me
```

## 4. Adaptive learning loop

```
task completed (learning-service, V2)
   └▶ progress updated (progress-service, V2)
        └▶ weekly interview (interview-service)
             POST /interviews/start {domain, level}      → ai-service generates questions
             POST /interviews/{id}/submit {answers[]}     → ai-service evaluates
                  → {score, passed, weaknesses[], recommendations[]}
                  → emits interview.completed + interview.evaluated
   (poor results → weaknesses feed future remediation plans — extension point)
```

---

## 5. AI endpoints (via gateway, JWT required)

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/ai/onboarding/start` | greet + ask the goal |
| POST | `/ai/onboarding/goal` | submit goal → adaptive assessment |
| POST | `/ai/onboarding/assessment` | submit answers → analysis + roadmap + daily plan |
| GET  | `/ai/onboarding/me` | session state |
| GET  | `/ai/roadmaps/me`, `/ai/roadmaps/{id}` | roadmap |
| GET  | `/ai/daily-plans/me` | today's plan |
| POST | `/ai/interviews/questions`, `/ai/interviews/evaluate` | used by interview-service (Feign) |

### Interview endpoints
| Method | Path | Purpose |
|--------|------|---------|
| POST | `/interviews/start` | generate a new interview |
| GET  | `/interviews/me`, `/interviews/{id}` | list / fetch |
| POST | `/interviews/{id}/submit` | submit answers → AI evaluation |

---

## 6. Databases (Liquibase, one per service)

- **ai_db**: `ai_requests`, `ai_responses`, `onboarding_sessions`, `assessments`,
  `generated_roadmaps`, `generated_daily_plans`, `recommendations`.
- **interview_db**: `interviews`, `interview_questions`, `interview_results`.

## 7. Configuration (ai-service)

```properties
athena.ai.base-url=${ATHENA_AI_BASE_URL:http://localhost:1234/v1}
athena.ai.model=${ATHENA_AI_MODEL:qwen3-14b}
athena.ai.connect-timeout=5s
athena.ai.read-timeout=120s
athena.ai.max-retries=2
athena.ai.retry-backoff=2s
athena.ai.temperature=0.4
athena.ai.max-tokens=2048
```

## 8. Observability
- Structured ECS logs; Micrometer/Brave tracing (trace+span IDs propagate over HTTP,
  Feign and Kafka). LM calls log model, latency and token usage.
- Global exception handling → uniform `ApiError`; AI failures → `503`.

---

## 9. Running V3

1. Start **LM Studio**, load a model (e.g. `qwen3-14b`), enable the local server
   (OpenAI-compatible) on port 1234.
2. `docker compose up --build` (brings up the new ai-db, interview-db, ai-service,
   interview-service alongside everything else). Set `ATHENA_AI_MODEL` to match the
   model id you loaded if it differs.
3. Use `postman/Athena-V3.postman_collection.json`: Register → Onboarding Start → Goal
   → Assessment → Roadmap/Daily Plan → Interview.

> **Verification note:** the full reactor builds and all unit/slice tests pass (LM
> client mocked). The live AI calls require a running LM Studio, which is environment-
> specific; `/ai/onboarding/start` works without the model (deterministic greeting),
> while goal/assessment/roadmap/interview steps require it.

## 10. Weekly scheduler
The weekly interview scheduler fires on cron; wiring the due-user roster remains an
extension (interviews can also be started on demand via the REST API).

---

# V3 Refinements (mandatory features — implemented)

## R1. Automatic onboarding via events
The explicit start call is gone from the client journey. Registration drives onboarding.

```
auth-service.register()
   └─ save account ─▶ publish UserRegisteredEvent ─▶ Kafka (athena.user.registered)
                                                        │
ai-service AiEventConsumer.onUserRegistered ◀──────────┘
   └─ OnboardingService.createSessionFromRegistration (idempotent)
        • persists userId, firstName, lastName, status=WAITING_FOR_GOAL
        • publishes onboarding.started
Client: GET /ai/onboarding/me → greeting + "What would you like to learn?"
        POST /ai/onboarding/goal → ...
```
`POST /ai/onboarding/start` is retained but `@Deprecated`; it idempotently returns the
already-created session (no duplicate).

## R2. Auth Feign dependency removed
ai-service no longer calls auth-service for the greeting. `firstName`/`lastName` arrive
in `UserRegisteredEvent` and are persisted on the onboarding session. The `AccountClient`
Feign client and auth-service's internal account endpoint were deleted; ai-service no
longer depends on OpenFeign.

## R3. Knowledge Graph (MVP)
`knowledge_nodes` (userId, skillName, domain, masteryPercentage, confidenceScore) with a
unique (userId, skillName) upsert key.

```
onboarding complete ─▶ KnowledgeGraphService.seedSkills(prerequisites @20%)
interview evaluated ─▶ (Kafka athena.interview.evaluated, now carries domain+weaknesses)
                        ai-service AiEventConsumer ─▶ recordWeaknesses(@35%)
client/quizzes      ─▶ POST /ai/knowledge-graph/update
every mutation      ─▶ publish KnowledgeGraphUpdatedEvent
```
Endpoints: `GET /ai/knowledge-graph/me`, `GET /ai/knowledge-graph/{userId}`,
`POST /ai/knowledge-graph/update`.

## R4. AI-generated badges (validated by Badge Service)
```
POST /ai/badges/suggest {domain}
   └─ AiGenerationService.generateBadgeSuggestions (LM)
   └─ publish BadgeSuggestionGeneratedEvent ─▶ Kafka
badge-service BadgeEventConsumer.onBadgeSuggestion
   └─ BadgeSuggestionValidator (UPPER_SNAKE code, name/desc/icon limits)
   └─ persist Badge (if new) + award UserBadge (idempotent)
   └─ publish BadgeAwardedEvent
```
ai-service NEVER persists badges; badge-service remains the source of truth.

## R5. LM Studio fallback & recovery
On an LM outage during onboarding, state is preserved (goal/answers saved before the
call) and a retry is scheduled instead of returning a bare 500.

```
submitGoal / submitAssessment
   └─ AiException ─▶ AiRetryService.record(PENDING, payloadReference=sessionId)
                  ─▶ throw AiTemporarilyUnavailableException
   handler ─▶ HTTP 503 + {status:"TEMPORARILY_UNAVAILABLE", message, retryAvailable:true, retryId}

Recovery:
   • Scheduled RetryDispatcher.retryPending() re-runs the persisted step server-side
   • POST /ai/retry/{requestId} re-runs on demand
   • status flow: PENDING → PROCESSING → COMPLETED | (retryCount++ → PENDING) → FAILED
```
Entity `ai_request_retries` (id, requestType, userId, payloadReference, status,
retryCount, timestamps). No onboarding sessions, assessments or interviews are lost.

## New/changed events
`UserRegisteredEvent`, `BadgeSuggestionGeneratedEvent` (+ `BadgeSuggestion`); topics
`athena.user.registered`, `athena.badge.suggestion.generated`. `InterviewEvaluatedEvent`
now includes `domain` + `weaknesses`.

> **Verification:** full reactor builds; all unit/slice tests pass (LM client mocked).
> Live AI calls require a running LM Studio. Auto-onboarding, KG seeding/updates, badge
> validation, and the retry lifecycle are covered by unit tests.

---

# V3 Knowledge Graph Visualization (final refinement)

The internal knowledge graph now has a **visualization layer** so the frontend renders
the user's evolving knowledge state without building graph structures itself.

## Model
- `KnowledgeNode` (existing) gains a `category` column (CORE / ADVANCED).
- `KnowledgeEdge` (new): `userId, sourceSkill, targetSkill, relationshipType` —
  `PREREQUISITE | SUPPORTS | RELATED | ADVANCES_TO`, unique per
  `(userId, source, target, type)`.
- `KnowledgeGraphSnapshot` (new): `userId, generatedAt, averageMastery, serializedGraph`
  — point-in-time evolution snapshots for timelines.

## Visualization-ready response
`GET /ai/knowledge-graph/me/visualization` returns `{ domain, generatedAt, nodes[],
edges[], summary, insights[] }`. Node `status` is derived: mastery ≥85 `MASTERED`, ≥50
`LEARNING`, else `WEAKNESS`. `confidence` is the 0–100 form of the stored score.
`summary` has strongest/weakest skills, `averageMastery`, `totalSkills`. **Insights are
rule-based (no LM Studio)** — strongest, weakest, and a "improving X accelerates Y"
hint derived from an edge into a weak skill.

## Graph generation & evolution
```
onboarding complete ─▶ seedSkills: nodes + PREREQUISITE chain
interview evaluated ─▶ recordWeaknesses: ADVANCED nodes + SUPPORTS edge from strongest skill
POST /ai/knowledge-graph/update ─▶ mastery changes
   each mutation ─▶ KnowledgeGraphService.afterChange(userId)
                      • KnowledgeGraphUpdatedEvent
                      • VisualizationService.onGraphChanged: evict caches; snapshot if
                        |Δ avgMastery| ≥ 5; publish KnowledgeGraphVisualizationGeneratedEvent
                        (+ KnowledgeGraphSnapshotCreatedEvent when a snapshot is taken)
```

### Sequence — visualization read (cached)
```
GET /ai/knowledge-graph/me/visualization
  controller (counter: requests) ─▶ VisualizationService.getVisualization
      Redis HIT  ─▶ cached KnowledgeGraphVisualizationResponse
      Redis MISS ─▶ counter: cache.miss; Timer: generation
                    build(nodes, edges, summary, insights) ─▶ cache (30-min TTL) ─▶ return
```

## Endpoints
| Method | Path | Notes |
|--------|------|-------|
| GET | `/ai/knowledge-graph/me/visualization` | self |
| GET | `/ai/knowledge-graph/{userId}/visualization` | self or ADMIN (403 otherwise) |
| GET | `/ai/knowledge-graph/me/history` | snapshot timeline |
| GET | `/ai/knowledge-graph/{userId}/history` | self or ADMIN |

## Redis & security & observability
- Caches `knowledge-graph:visualization` and `knowledge-graph:history`, keyed by userId,
  30-min TTL; evicted on every graph mutation.
- JWT required; `/me/*` is self; `/{userId}/*` requires self or `ADMIN` (X-User-Roles),
  else `403` — consistent with the gateway-enforced auth model.
- Metrics: `athena.kg.visualization.requests` (counter), `...cache.miss` (counter),
  `athena.kg.visualization.generation` (timer), `athena.kg.snapshots.created` (counter).
  Cache hit ratio = (requests − cache.miss) / requests.

## New events
`KnowledgeGraphVisualizationGeneratedEvent`, `KnowledgeGraphSnapshotCreatedEvent`
(topics `athena.knowledge.visualization.generated`, `athena.knowledge.snapshot.created`).

---

# Learning Sessions — AI-generated lessons with rolling lookahead

Roadmap phases ("nodes") are generated during onboarding, but the actual study
material is generated on demand by **ai-service**, one `LearningSession` per node.

## Node identity
Roadmap nodes are the phases stored in `generated_roadmaps.content_json` (no node
table). A node's stable id is derived deterministically:
`roadmapNodeId = UUID.nameUUIDFromBytes(roadmapId + ":" + nodeIndex)`. A session is
unique per `(user_id, roadmap_node_id)`, which makes generation idempotent.

## Rolling lookahead (constant 5-node buffer)
- On `RoadmapGeneratedEvent` (onboarding complete), ai-service consumes the event and
  generates lessons for the **first 5 nodes only** (`LEARNING_SESSION_BUFFER = 5`).
- On `LearningSessionCompletedEvent`, ai-service generates the **next node that has no
  session yet**, keeping ~5 nodes ahead. Both triggers run in Kafka consumers — no
  synchronous blocking of the request thread.

## Aggregate & stages
`LearningSession` (id, userId, roadmapId, roadmapNodeId, nodeIndex, title, status,
estimatedMinutes, generatedAt, updatedAt; status `NOT_STARTED | IN_PROGRESS | COMPLETED`)
owns four persisted stages, each in its own table with a FK + cascade delete and a
`session_id` index:
- `reading_materials` (title, content, estimatedMinutes, orderIndex)
- `watching_materials` (title, description, **videoQuery** — search queries, never URLs)
- `practice_activities` (title, description, **practiceType**, instructions, starterContent)
- `quiz_questions` (question, type, options, correctAnswer, explanation)

## Domain-aware practice
LM Studio picks the `practiceType` from the domain: `CODE_EDITOR` (programming, with
starter code), `LANGUAGE_EXERCISE` (languages), `SCENARIO` (business),
`CREATIVE_PROMPT` (creative), `REFLECTION` (theory-heavy).

## AI generation
All four stages are produced in **one** structured LM Studio response, enforced with a
JSON schema (`ResponseFormat.ofSchema`). Only structured outputs are persisted — prompts
and raw LM responses are never stored (the AiRequest/AiResponse audit keeps metadata only).

## Redis
The assembled session detail is cached as `learning-session::{sessionId}` (30-min TTL)
and evicted whenever the session changes (start/complete).

## Kafka events
`LearningSessionGeneratedEvent`, `LearningSessionStartedEvent`,
`LearningSessionCompletedEvent`, `NodeBufferRefilledEvent`
(topics `athena.learning.session.generated|started|completed`,
`athena.learning.node.buffer.refilled`).

## Endpoints (gateway-routed `/learning-sessions/**` → ai-service)
| Method | Path | Notes |
|--------|------|-------|
| GET | `/learning-sessions/current` | first non-completed session (full detail) |
| GET | `/learning-sessions/upcoming` | not-completed sessions (summaries) |
| GET | `/learning-sessions/{sessionId}` | full detail, owner only |
| POST | `/learning-sessions/{sessionId}/start` | → IN_PROGRESS |
| POST | `/learning-sessions/{sessionId}/complete` | → COMPLETED (triggers buffer refill) |
