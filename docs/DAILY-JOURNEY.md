# Daily Journey — Backend Design

The Daily Journey is Athena's core daily experience: an AI-generated, continuously
adapted learning day built from the user's current roadmap node, weak Knowledge Graph
areas, interview/assessment signals, and the time they have available. This document
defines the persistence model, service placement, REST/event contracts, the
AI-orchestration contract, the block-generation algorithm, and the adjustment triggers.

## 1. Service placement

All Daily Journey logic lives in **ai-service** (`athena_ai`, port 8086). ai-service is
the single LLM integration point and already owns every input this page reads:

- the LLM pipeline (`LlmService`, JSON-schema enforcement, prompt templates),
- the persisted Knowledge Graph (`knowledge_nodes` with mastery/confidence/domain),
- the Roadmap (`generated_roadmaps.content_json` → `RoadmapContent.phases`),
- the per-node `LearningSession` (the current node = first non-`COMPLETED` session,
  already carrying readings/watchings/practices/quizzes),
- consumption of `InterviewEvaluatedEvent`.

Placing Daily Journey here keeps the AI-aware orchestration cohesive and avoids
cross-service chatter. Downstream services (progress, badge) integrate purely through
events. Interview *weakness* data already reaches ai-service via `InterviewEvaluatedEvent`
(recorded into the Knowledge Graph), so the mission reads weak areas from the KG rather
than calling interview-service directly.

The Daily Journey is an orchestration layer **on top of** the existing `LearningSession`.
A `LearningSession` is static, per-node content with a single status. A day needs a
mutable, per-block lifecycle (start/skip/relink/progress), priority drill inserts, and an
adjustment log — so the day is its own aggregate that *references* session content rather
than mutating it.

## 2. Persistence model (`athena_ai`, migration `005-daily-journey.yaml`)

### daily_missions
One mission per user per day (the hero card).

| column | type | notes |
|---|---|---|
| id | uuid PK | |
| user_id | uuid NN | |
| mission_date | date NN | UK `(user_id, mission_date)` |
| roadmap_id | uuid | focus roadmap |
| roadmap_node_id | uuid | focus node (deterministic `nameUUIDFromBytes`) |
| learning_session_id | uuid | source node session |
| title | varchar(300) NN | AI-generated |
| description | text NN | AI-generated |
| goal_context | varchar(300) | e.g. "Next checkpoint interview" |
| difficulty | varchar(20) NN | `Difficulty` |
| available_minutes | int NN | the day's time budget |
| estimated_minutes | int NN | sum of block durations |
| status | varchar(20) NN | `DayStatus` |
| reasoning_json | text | cached "why" reasoning for the day |
| started_at | timestamptz | |
| completed_at | timestamptz | |
| generated_at | timestamptz NN | |
| updated_at | timestamptz NN | |

### daily_blocks
The adaptive timeline. Each block is a persisted, mutable activity.

| column | type | notes |
|---|---|---|
| id | uuid PK | |
| mission_id | uuid NN | FK → daily_missions ON DELETE CASCADE |
| user_id | uuid NN | denormalized for events/queries |
| order_index | int NN | idx `(mission_id, order_index)` |
| type | varchar(20) NN | `BlockType` |
| title | varchar(300) NN | |
| description | text | |
| roadmap_node_id | uuid | |
| knowledge_node_id | uuid | set for weakness drills |
| source_ref | uuid | id of the source session stage, when derived |
| difficulty | varchar(20) NN | `Difficulty` |
| duration_minutes | int NN | |
| status | varchar(20) NN | `BlockStatus` |
| progress_percent | int NN | within-activity progress (0–100) |
| skip_reason | varchar(300) | |
| priority_insert | boolean NN | true for ad-hoc strengthen drills |
| started_at | timestamptz | |
| completed_at | timestamptz | |
| created_at | timestamptz NN | |
| updated_at | timestamptz NN | |

### adjustment_logs
Append-only feed of automatic plan modifications.

| column | type | notes |
|---|---|---|
| id | uuid PK | |
| mission_id | uuid NN | FK → daily_missions ON DELETE CASCADE |
| user_id | uuid NN | |
| type | varchar(20) NN | `AdjustmentType` |
| reason | varchar(300) NN | human-facing reason |
| affected_block_id | uuid | |
| created_at | timestamptz NN | idx `(mission_id, created_at)` |

### daily_checkins
Mentor confidence check-ins.

| column | type | notes |
|---|---|---|
| id | uuid PK | |
| mission_id | uuid NN | FK → daily_missions ON DELETE CASCADE |
| user_id | uuid NN | |
| block_id | uuid | optional block context |
| confidence | varchar(20) NN | `ConfidenceLevel` |
| topic | varchar(300) | |
| reply | text NN | AI-generated mentor reply |
| created_at | timestamptz NN | idx `(mission_id, created_at)` |

### daily_reflections
End-of-day reflection (one per day).

| column | type | notes |
|---|---|---|
| id | uuid PK | |
| mission_id | uuid NN | FK → daily_missions ON DELETE CASCADE |
| user_id | uuid NN | |
| reflection_date | date NN | UK `(user_id, reflection_date)` |
| hardest_part | text | |
| what_clicked | text | |
| adjust_request | text | drives tomorrow's generation |
| skipped | boolean NN | |
| created_at | timestamptz NN | |

### Relationships to existing domain
- `daily_missions.roadmap_id/roadmap_node_id` → `generated_roadmaps` + the deterministic
  node id (`UUID.nameUUIDFromBytes(roadmapId + ":" + nodeIndex)`).
- `daily_missions.learning_session_id` → `learning_sessions`.
- `daily_blocks.knowledge_node_id` → `knowledge_nodes.id`.
- `daily_blocks.source_ref` → the source `reading_materials`/`practice_activities`/… id.

## 3. Enums (`com.athena.ai.domain`)

- `DayStatus`: FORMING, READY, IN_PROGRESS, COMPLETED, REFLECTED
- `BlockStatus`: UPCOMING, CURRENT, COMPLETED, SKIPPED
- `BlockType`: READING, PRACTICE, VIDEO, QUIZ, SPEAKING, REVIEW, DRILL
- `Difficulty`: EASY, MODERATE, CHALLENGING (shared by mission and block)
- `AdjustmentType`: ADD, REVIEW, SIMPLIFY, TRIM, REGENERATE
- `ConfidenceLevel`: CONFIDENT, UNSURE, NEED_HELP

## 4. Block-generation algorithm (`DailyBlockComposer`)

Deterministic composition over AI-generated content (deterministic + AI hybrid).

Inputs: the current `LearningSession` (focus node stages), weak KG nodes
(`mastery < WEAKNESS_MASTERY_THRESHOLD`), `availableMinutes`, the user's recent
block-type skip counts, and the latest reflection's `adjust_request`.

1. **Focus node** = first non-`COMPLETED` `LearningSession` (ordered by `nodeIndex`).
   Mission `title`/`description`/`goalContext`/`difficulty` come from the AI
   `generateDailyMission` call seeded with the node, roadmap goal, domain, level, weak
   areas, and `availableMinutes`.
2. **Type weighting** of the time budget (domain-aware base): PRACTICE 0.35, READING 0.20,
   VIDEO 0.15, QUIZ 0.15, SPEAKING/REVIEW 0.15. A type the user skips frequently
   (`skipCount >= SKIP_ADAPT_THRESHOLD`) has its weight halved; freed budget is
   redistributed proportionally.
3. **Compose core blocks** from the session stages mapped to `BlockType`
   (reading→READING, practice→PRACTICE, watching→VIDEO, quiz→QUIZ, speaking
   practice→SPEAKING), ordered pedagogically (READING → PRACTICE → VIDEO → QUIZ →
   SPEAKING), accumulating durations until the per-type budget is consumed.
4. **Insert weakness blocks**: up to `MAX_WEAKNESS_BLOCKS` REVIEW blocks for the weakest
   KG nodes related to the focus domain, if budget remains.
5. **Difficulty progression**: first block EASY, middle MODERATE, last CHALLENGING,
   clamped by the mission difficulty.
6. First non-completed block → `CURRENT`, the rest `UPCOMING`. `estimated_minutes` = Σ
   block durations.

The composer is pure given its inputs; the AI supplies mission copy and the *content* of
drills. Re-planning (`adjust`/`time`) re-runs the composer over the remaining
(non-`COMPLETED`) blocks only.

## 5. Adjustment triggers (`AdjustmentEngine`) — deterministic rules

Every automatic modification writes an `adjustment_logs` row and publishes
`DailyMissionAdjustedEvent`.

| trigger | rule | effect | type |
|---|---|---|---|
| fast completion | block completed in `< FAST_COMPLETION_RATIO` of its duration **and** last check-in `CONFIDENT` | insert a harder DRILL after it | ADD |
| low quiz confidence | QUIZ block completed with `progress_percent < LOW_CONFIDENCE_PERCENT`, or check-in `UNSURE` | insert a short REVIEW for the topic | REVIEW |
| needs help | check-in `NEED_HELP` | lower difficulty of upcoming blocks one step + shorten | SIMPLIFY |
| reduced time | `adjust-time` lowers `available_minutes` below `estimated_minutes` | drop/shorten lowest-priority upcoming blocks to fit | TRIM |
| explicit replan | user "Adjust plan" → regenerate remaining | recompose upcoming blocks via AI | REGENERATE |

## 6. AI-orchestration contract (internal to ai-service)

`AiGenerationService` gains four schema-enforced methods (mirroring
`generateLearningSession`), each backed by a prompt template under `resources/prompts`.

- `generateDailyMission(userId, goal, domain, level, nodeTitle, objectives, weakAreas, availableMinutes)`
  → `DailyMissionPlan { mission{title,description,goalContext,difficulty}, blocks[]{type,title,description,difficulty,durationMinutes} }`
  (`DAILY_MISSION_SCHEMA`, template `daily-mission.txt`).
- `generateWhyReasoning(userId, goal, recentInterview, masteredSkill, weakAreas, nodeTitle)`
  → `WhyReasoning { events[]{icon,label,text}, conclusion }` (`WHY_REASONING_SCHEMA`,
  template `why-reasoning.txt`).
- `generateWeaknessDrill(userId, skillName, domain, masteryPercentage)`
  → `DrillContent { title, description, type, durationMinutes, instructions }`
  (`WEAKNESS_DRILL_SCHEMA`, template `weakness-drill.txt`).
- `generateMentorReply(topic, confidence)` → `MentorReply { reply }`
  (`MENTOR_REPLY_SCHEMA`, template `mentor-reply.txt`).

Generation never persists prompts/raw responses (existing `LlmService` records token
counts/latency only).

## 7. REST API (gateway route `/daily-journey/**` → ai-service)

All endpoints require `X-User-Id`. Reads are cached per day in Redis
(`CACHE_DAILY_JOURNEY`, key = `missionId`), evicted on every mutation.

| method | path | purpose |
|---|---|---|
| GET | `/daily-journey/today` | mission + blocks + adjustments + weaknesses + check-ins + reflection state + day progress (generates/loads today's mission) |
| GET | `/daily-journey/today/why` | cached "why Athena chose this" reasoning |
| POST | `/daily-journey/today/start` | mission → IN_PROGRESS, `started_at` |
| POST | `/daily-journey/today/adjust` | `{action: SIMPLIFY\|INTENSIFY\|REGENERATE}` re-plan remaining blocks |
| POST | `/daily-journey/today/time` | `{availableMinutes}` recompute budget/blocks |
| POST | `/daily-journey/blocks/{blockId}/start` | block → CURRENT, `started_at` |
| POST | `/daily-journey/blocks/{blockId}/progress` | `{percent}` update within-activity progress |
| POST | `/daily-journey/blocks/{blockId}/complete` | block → COMPLETED; runs adjustment triggers |
| POST | `/daily-journey/blocks/{blockId}/skip` | `{reason?}` block → SKIPPED (stays visible) |
| POST | `/daily-journey/blocks/{blockId}/relink` | block → UPCOMING; recompute budget |
| POST | `/daily-journey/weaknesses/{knowledgeNodeId}/strengthen` | priority-insert an AI drill |
| POST | `/daily-journey/checkin` | `{confidence, blockId?}` persist + AI reply + difficulty adjust |
| POST | `/daily-journey/reflection` | `{hardestPart, whatClicked, adjustRequest}` save |
| POST | `/daily-journey/reflection/skip` | record skipped reflection |

The single `DailyJourneyResponse` is the page payload; block/mutation endpoints return
the refreshed `DailyJourneyResponse` so the frontend always re-renders from one source.

## 8. Events

Published (new topics in `KafkaTopics`):

| event | topic | when |
|---|---|---|
| `DailyMissionGeneratedEvent` | `athena.daily.mission.generated` | mission first composed for a day |
| `DailyBlockCompletedEvent` | `athena.daily.block.completed` | block completed |
| `DailyBlockSkippedEvent` | `athena.daily.block.skipped` | block skipped |
| `DailyMissionAdjustedEvent` | `athena.daily.mission.adjusted` | any adjustment applied |
| `DailyCheckinRecordedEvent` | `athena.daily.checkin.recorded` | check-in saved |
| `DailyReflectionSavedEvent` | `athena.daily.reflected` | reflection saved/skipped |

On **mission completion** ai-service additionally publishes the existing
`TaskCompletedEvent` (`athena.task.completed`) so progress-service's streak logic works
unchanged (reuse, not duplication).

Consumed:

- ai-service consumes `DailyBlockCompletedEvent` for weakness drills to nudge the relevant
  `knowledge_nodes.mastery_percentage` upward (closing the loop the Weakness cards read).
- ai-service consumes `DailyReflectionSavedEvent` to gate tomorrow's mission: tomorrow is
  generated only after reflection or a day-end trigger — never pre-generated.

Downstream consumers (wired as their pages are built):

- **progress-service** ← `TaskCompletedEvent` / `DailyBlockCompletedEvent` (time + streak).
- **badge-service** ← mission/reflection completion (streak + achievement awards).
- **Knowledge Graph** (in ai-service) ← drill completions (mastery deltas).

## 9. Code standards

DRY/KISS/YAGNI/SOLID; modern stream/Optional style; no comments or Javadoc; one shared
mapper for `DailyJourneyResponse` assembly; depend on service interfaces across layers.
