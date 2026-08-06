# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); this project has not yet made a
tagged `1.0.0` release (see [`SUPPORTED_VERSIONS.md`](SUPPORTED_VERSIONS.md)), so entries
below are reconstructed from git history and the versioned design docs in `docs/`
(`ARCHITECTURE-V2.md`, `ARCHITECTURE-V3.md`, `DAILY-JOURNEY.md`) rather than from formal
release notes. Dates are the actual merge dates from git history.

## [Unreleased]
- Testing milestone — production-grade, multi-layer test suite and infrastructure:
  - **JaCoCo** wired across the reactor with a new `coverage-report` aggregator module
    (HTML + XML at `coverage-report/target/site/jacoco-aggregate/`); **Maven Failsafe**
    added so `*IT` integration tests run under `verify`.
  - **Unit** coverage expanded (RAG core, daily-journey algorithms, roadmap/generation/LLM
    orchestration, `athena-common` exception handling): aggregate line coverage
    22.5% → 42.2%, with no coverage inflation.
  - **Integration** (Testcontainers): PostgreSQL + Liquibase repository tests, and a
    real **pgvector** cosine-similarity search test for `rag-service`.
  - **API**: full real-HTTP tests via Spring `RestClient` (auth register/login/refresh and
    an authenticated, header-scoped progress endpoint).
  - **Consumer contract**: all four cross-service Feign clients verified against a stubbed
    provider.
  - **Kafka**: a real-broker event-driven workflow test (produce → consume → downstream
    publish).
  - Tooling adapted for Spring Boot 4 / JDK 26 (RestClient over Rest Assured/
    `TestRestTemplate`; JDK `HttpServer` stub over WireMock; Testcontainers pinned to
    1.20.5). See `docs/Development.md` §Running tests.
- Documentation overhaul: rewritten `README.md`, new `docs/` knowledge base
  (`Architecture.md`, `Backend.md`, `API.md`, `Security.md`, `Infrastructure.md`,
  `Development.md`, `Deployment.md`, `Frontend.md`, `Project-Structure.md`,
  `Contributing.md`), GitHub community health files, issue/PR templates.
- In-progress work on `ai-service`'s roadmap controller/service (uncommitted on
  `master` as of this writing).

## 2026-07-27 — RAG memory platform
- Added `rag-service`: document ingestion and chunking, embeddings via a local LLM,
  pgvector-backed similarity search, grounded question-answering with citations, and
  "what's next" recommendations assembled from roadmap/knowledge-graph/progress context.
- Added `athena-llm`: shared `ChatProvider`/`EmbeddingProvider` abstraction over LM
  Studio, extracted so both `ai-service` and the new `rag-service` share one LLM client.
- Largest single change to date (350 files, ~8,600 lines added) — see
  `docs/Backend.md`'s `rag-service` section for the full endpoint/architecture detail
  reconstructed from the current source, since this addition predates any design doc.

## 2026-06-30 — AI platform refinements
- Continued build-out of `ai-service`: automatic event-driven onboarding (registration
  triggers onboarding without an explicit client call), the Knowledge Graph MVP and its
  visualization/history layer, AI-generated badge suggestions (validated by
  `badge-service` before being persisted), the AI-outage retry lifecycle, and the Daily
  Journey adaptive daily-plan system and Learning Sessions (rolling 5-node lookahead
  lesson generation). See `docs/ARCHITECTURE-V3.md` (refinements R1–R5 and the Knowledge
  Graph Visualization section) and `docs/DAILY-JOURNEY.md`.

## 2026-06-17 — S3 image storage
- Added `athena-common/storage` (`ImageStorage`/`S3ImageStorage`) and wired
  profile-picture upload into `auth-service` registration and `/account/image`, backed
  by an S3-compatible bucket (LocalStack locally).

## 2026-06-16 — CloudWatch structured logging
- Added `athena-common/logging` (`CloudWatchLogbackAppender` + auto-configuration) for
  optional structured log shipping to CloudWatch Logs (or LocalStack locally) across
  every service.

## 2026-06-16 — AI mentor platform (V3)
- Added `ai-service`: local-LLM-powered onboarding (goal capture → adaptive assessment
  → domain/level analysis), AI-generated learning roadmaps, and AI-generated daily plans.
- Added `interview-service`: weekly AI-evaluated interviews, calling `ai-service` over
  OpenFeign for question generation and evaluation rather than talking to the LLM
  directly.
- See `docs/ARCHITECTURE-V3.md` for the full design of this milestone.

## 2026-06-13 — Learning engine & gamification (V2)
- Added `learning-service` (plans, tasks, study sessions) and `badge-service` (badge
  catalogue, rule-based awards).
- Introduced Kafka event choreography (`athena.task.completed`, `athena.plan.created`,
  `athena.streak.updated`, `athena.badge.awarded`) and Redis caching, wiring
  `progress-service` to consume task-completion events and `badge-service` to consume
  streak updates.
- See `docs/ARCHITECTURE-V2.md` for the full design of this milestone.

## 2026-06-12 — Initial backend core (V1)
- Initial commit: `discovery-server` (Eureka), `api-gateway` (routing + centralized JWT
  validation), `auth-service` (register/login/refresh), `user-service` (profiles),
  `progress-service` (progress + streaks), and the `athena-common` shared library.
- Established the architectural baseline every later addition follows: database per
  service, gateway-only JWT validation with anti-spoofing header stripping, and clean
  controller → service → repository layering.
