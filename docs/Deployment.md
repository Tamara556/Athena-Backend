# Deployment

## What exists today

**Docker Compose is the only deployment artifact in this repository.**
`docker-compose.yml` stands up the full 22-service local stack described in
`docs/Infrastructure.md`. There is no separate staging/production compose file, no
Kubernetes manifests or Helm chart, and no infrastructure-as-code (Terraform/CDK/etc.)
— all of that is future work, not a currently-supported path. Treat anything below
beyond "run compose" as a **recommendation**, not documentation of an existing system.

## Continuous Integration

`.github/workflows/ci.yml` runs on every push/PR to `master`:

```yaml
JDK 26 (temurin) → mvn -B -ntp clean verify → upload surefire reports
```

It builds and tests every module in the reactor. It does **not**:
- build or publish Docker images,
- run integration tests against a live compose stack,
- deploy anywhere.

A second workflow, `.github/workflows/discord-notifications.yml`, posts PR/issue/release
events to a Discord webhook — a contributor-notification convenience, unrelated to
deployment.

## Configuration & secrets model

Every module reads configuration from `application.yml` defaults plus environment
variable overrides (see `docs/Infrastructure.md` and `docs/Security.md` §8 for the full
variable list). All secrets — the JWT signing key, database credentials, S3/CloudWatch
access keys — are plain environment variables with insecure local-dev defaults baked
into `docker-compose.yml`. **None of the checked-in defaults are safe outside a local
machine.** There is no secret-manager integration (Vault, AWS Secrets Manager, etc.) in
this codebase today.

## Recommended next steps (not implemented — tracked in `ROADMAP.md`)

These are documentation-only recommendations, offered because they're the natural next
steps for taking this from "runs locally" to "deployable," not because any of this
exists in the repo:

1. **Image publishing** — extend CI to build and push versioned images per module
   (the Dockerfiles already exist and build cleanly; only the publish step is missing).
2. **Externalized secrets** — replace the compose-file plaintext env vars with a real
   secret manager before running anywhere but a laptop.
3. **A production-shaped compose overlay or orchestrator manifests** — Kubernetes
   (with per-service `Deployment`/`Service`/`HorizontalPodAutoscaler`) is the natural fit
   given the system is already stateless-service-plus-own-database; Eureka could be kept
   for local dev while relying on the orchestrator's own service discovery in a cluster.
4. **A managed Postgres/Kafka/Redis** in place of the compose containers for anything
   beyond local development.
5. **Health-check-driven readiness** — every service already exposes Spring Actuator
   (`management.endpoints.web.exposure`); wiring liveness/readiness probes into an
   orchestrator is a small step from what's already there.
6. **CI integration/contract tests against a live compose stack**, since today CI only
   proves each module builds and its own unit/slice tests pass — not that the full
   choreography (Kafka events, Feign calls across services) still works end-to-end.
