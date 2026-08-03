# Security Policy

For an explanation of the current security *architecture* (JWT model, 2FA, data
handling, known gaps), see [`docs/Security.md`](docs/Security.md). This document is
about **reporting a vulnerability**, not describing the design.

## Supported Versions

This project has not yet had a `1.0` release; `master` is the only supported line of
development. See [`SUPPORTED_VERSIONS.md`](SUPPORTED_VERSIONS.md) for the full policy.

| Version | Supported |
|---|---|
| `master` (latest commit) | ✅ |
| anything else | ❌ |

## Reporting a Vulnerability

**Please do not open a public GitHub issue for a suspected security vulnerability.**

Use **[GitHub Security Advisories](https://github.com/Tamara556/Athena-Backend/security/advisories/new)**
to report privately. This lets maintainers assess and fix the issue before it's publicly
disclosed.

Please include:
- A description of the vulnerability and its potential impact.
- Steps to reproduce (a minimal repro is very helpful).
- Which module(s)/endpoint(s) are affected.

## What to expect

This is a small, actively-developed open source project without a dedicated security
team or a formal SLA. Maintainers will acknowledge new advisories and aim to respond
with an initial assessment as soon as reasonably possible; timelines are best-effort,
not guaranteed. Once a fix is available, a coordinated disclosure timeline will be
agreed upon with the reporter before any public advisory is published.

## Scope

In scope: any of the 12 modules in this repository (`athena-common`, `athena-llm`,
`discovery-server`, `api-gateway`, `auth-service`, `user-service`, `progress-service`,
`learning-service`, `badge-service`, `ai-service`, `interview-service`, `rag-service`)
and the `docker-compose.yml` local stack configuration.

Out of scope: the companion frontend repository
([`Tamara556/Athena-Frontend`](https://github.com/Tamara556/Athena-Frontend)) — report
issues there in that repository instead. Also out of scope: vulnerabilities that only
manifest when running the checked-in local-development defaults (e.g. the placeholder
JWT secret / DB credentials baked into `docker-compose.yml`) — those are documented as
unsafe-outside-local-dev in `docs/Security.md` and `docs/Deployment.md`, not
vulnerabilities in the application code itself.
