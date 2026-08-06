# Roadmap

Built only from evidence in this repository — uncommitted work, explicitly-flagged
extension points in the design docs, and gaps identified while writing
`docs/Architecture.md`, `docs/API.md`, `docs/Security.md`, and `docs/Deployment.md`.
Nothing here is invented; where something is a recommendation rather than an
in-flight change, it's labeled as such.

## Completed

- **V1 — Backend core**: gateway-centralized JWT auth, Eureka discovery, auth/user/
  progress services, database-per-service.
- **V2 — Learning engine & gamification**: learning-service, badge-service, Kafka event
  choreography, Redis caching.
- **V3 — AI mentor platform**: ai-service (onboarding, roadmap, daily plan), interview-
  service, event-driven auto-onboarding, Knowledge Graph MVP + visualization/history,
  AI-generated badge suggestions, AI-outage retry lifecycle.
- **Daily Journey**: adaptive, block-by-block daily learning plan with check-ins,
  reflections, and rule-based adjustment triggers.
- **Learning Sessions**: per-roadmap-node AI-generated lesson content with a rolling
  5-node lookahead buffer.
- **CloudWatch structured logging** and **S3-backed profile image storage**.
- **RAG memory platform** (rag-service): document ingestion/chunking/embeddings,
  pgvector similarity search, grounded Q&A with citations, next-step recommendations.
- **Account security features**: 2FA, device-session management, login-activity
  history, self-service data export.
- **Open-source release preparation**: README rewrite, `docs/` knowledge base, GitHub
  community health files (this change).

## In Progress

- **Roadmap controller/service changes** — `ai-service`'s `RoadmapController`,
  `RoadmapService`, and `RoadmapServiceImpl` have uncommitted local changes as of this
  writing (`git status` on `master`). Not yet documented beyond what's captured in
  `docs/Backend.md`'s existing roadmap endpoint table, since the change isn't merged.
- **Weekly interview scheduler roster wiring** — the scheduler that fires interview
  generation on a cron already exists, but wiring it to the actual due-user roster is an
  explicitly flagged extension point (`docs/ARCHITECTURE-V3.md` §10); interviews can
  currently only be started on demand via the REST API.
- **Test-coverage ramp** — the five-layer test suite (unit, Testcontainers integration,
  `RestClient` API, Feign consumer-contract, Kafka workflow) and JaCoCo reporting are in
  place (`docs/Development.md` §Running tests); aggregate line coverage is ~42%. Raising
  it toward the 90% goal is a matter of continuing the established patterns across the
  remaining service/controller layers, plus adding consumer **idempotency** and a
  **dead-letter/retry** topology (neither implemented today) and tests for them.

## Planned (recommended, not implemented)

These are recommendations surfaced while documenting the codebase, not commitments or
scheduled work:

- **OpenAPI / springdoc-openapi** — no module currently generates a machine-readable API
  spec or exposes Swagger UI; `docs/API.md` is hand-maintained (see `docs/API.md` and
  `docs/Architecture.md` §5).
- **`FIRST_PLAN_COMPLETED` badge auto-award** — seeded in the badge catalogue but never
  awarded, because no service currently publishes a "plan completed" event
  (`docs/ARCHITECTURE-V2.md` §5). `learning-service` would need to publish a
  `PlanCompletedEvent` when a plan's last task completes.
- **CI Docker image publishing** — `ci.yml` builds and tests every module but doesn't
  build or push any of the 10 existing Dockerfiles (`docs/Deployment.md`).
- **Refreshed Postman collections** — the three collections in `postman/` predate
  `rag-service`, the Daily Journey endpoints, and several `auth-service` routes (2FA,
  devices, export) (`docs/API.md`).
- **Rate limiting at the gateway** — no throttling exists today on `/auth/login` or
  elsewhere (`docs/Security.md` §9).
- **Centralized secret management** — all secrets are plain environment variables today;
  a Vault/Secrets-Manager-backed setup is recommended before any non-local deployment
  (`docs/Security.md` §8, `docs/Deployment.md`).
- **A production deployment path** — no Kubernetes/Helm manifests or IaC exist; Docker
  Compose is local-development-only today (`docs/Deployment.md`).
- **`ai-service` service-boundary refactor** — a DDD-oriented split of `ai-service`'s
  many sub-domains (onboarding, roadmap, Daily Journey, learning sessions, Knowledge
  Graph, AI interview support, AI badge suggestions) has prior design analysis but is
  not implemented in this repository (`docs/Architecture.md` §5).
- **GitHub Discussions, labels, and a Projects board** — process recommendations, see
  `CONTRIBUTING.md`'s closing section.
