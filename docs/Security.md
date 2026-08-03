# Security Architecture

This document describes the **current implementation** of Athena's security model. For
how to report a vulnerability, see [`SECURITY.md`](../SECURITY.md) at the repo root.

## 1. JWT trust model

- `auth-service` issues signed JWTs (JJWT, `athena-common/security/JwtService`) —
  **access tokens** (15 min default TTL) and **refresh tokens** (30 days default TTL,
  `athena.security.jwt.access-token-ttl` / `refresh-token-ttl`, both overridable).
- `api-gateway` is the **only** component that validates tokens. Its
  `JwtAuthenticationFilter` (`Ordered.HIGHEST_PRECEDENCE + 10`) runs before routing:
  - `/auth/**` and `/actuator/**` pass through unauthenticated;
  - every other path requires `Authorization: Bearer <token>`; a missing/invalid/expired
    token gets a `401` with a uniform JSON body before it ever reaches a business
    service.
- On success, the gateway injects **verified** `X-User-Id` / `X-User-Roles` headers
  (`athena-common/security/AuthHeaders`) and forwards the request.
- **Anti-spoofing**: on *every* request — including the public `/auth/**` paths — the
  gateway strips any client-supplied `X-User-Id`/`X-User-Roles` before forwarding. A
  client cannot impersonate another user by sending these headers directly; only the
  gateway can set them, and only after validating a real token.
- Downstream services trust `X-User-Id`/`X-User-Roles` unconditionally and never
  re-validate the token themselves. This model depends on services never being reachable
  except through the gateway (true in the compose network; would need a network policy /
  service mesh boundary in a real multi-tenant deployment — see `docs/Deployment.md`).
- The gateway and `auth-service` **share the same JWT secret** (`ATHENA_JWT_SECRET`,
  wired through a compose YAML anchor locally) — they must agree, since one issues and
  the other verifies.

## 2. Passwords

`BCryptPasswordEncoder` (Spring Security Crypto) hashes passwords in `auth-service`;
plaintext passwords are never stored or logged.

## 3. Two-factor authentication (auth-service)

Phone-number-based 2FA via SMS (`TwilioSmsSender` in production config;
`LoggingSmsSender` — logs the code instead of sending it — as the local-dev default).

- `POST /account/2fa/setup` — register a phone number, sends a verification code.
- `POST /account/2fa/enable` — confirm the code → 2FA turned on for the account.
- On login with 2FA enabled, `/auth/login` returns a challenge instead of tokens;
  `POST /auth/2fa/verify` completes the login with the SMS code.
- `POST /account/2fa/send-code` / `POST /account/2fa/disable` — code-gated disable, so
  turning 2FA off also requires proving control of the phone.

Codes are short-lived challenges (`TwoFactorChallenge` entity), not long-lived secrets.

## 4. Device sessions & login activity

Every login records a `LoginEvent` (IP address, user agent, timestamp) and a
`DeviceSession`. `GET /account/login-activity` and `GET /account/devices` expose this to
the account owner; `POST /account/devices/{id}/revoke` and
`POST /account/devices/revoke-others` let a user shut down sessions they don't
recognize — the closest thing this system has to session-hijacking mitigation today (no
automatic anomaly detection).

IP extraction respects `X-Forwarded-For` (first entry) when present, falling back to the
raw remote address — relevant if you put a reverse proxy in front of the gateway in a
real deployment.

## 5. Data export & account data

`GET /account/export` aggregates the caller's data from every service that holds any
(`user`, `progress`, `ai`, `badge`, `interview` — via Feign clients in
`auth-service/client`) into a single downloadable JSON file. This is a manual,
on-demand, self-service export — there's no automated data-retention/deletion policy
implemented yet (tracked in `ROADMAP.md`).

## 6. Object storage — profile images

Avatars uploaded at registration or via `/account/image` go through
`athena-common/storage` (`S3ImageStorage`) to an S3-compatible bucket (LocalStack
locally, real AWS/S3 in a real deployment). Images are served back through
`auth-service` (`GET /auth/users/{userId}/image`, not directly from the bucket), which
lets access rules live in application code rather than bucket policy, at the cost of
routing image bytes through the service.

## 7. AI data privacy (ai-service, rag-service)

- `ai_requests` / `ai_responses` (ai-service) persist **only metadata** — prompt type,
  model name, latency, token counts, success/failure — never the prompt or completion
  text itself.
- `rag-service`'s memory documents are explicitly opt-in content the user ingests
  (`POST /rag/documents`) with a `Visibility` (default `PRIVATE`); retrieval and
  grounded Q&A only ever operate over the calling user's own documents
  (`userId`-scoped at every query/search/ingest call).
- Knowledge Graph endpoints enforce **self-or-ADMIN** access explicitly in code
  (`KnowledgeGraphController.assertCanAccess`) — a user cannot read another user's
  mastery data or history by guessing a `userId`, and the check throws a `403`, not a
  silent empty response.

## 8. Observability & secrets

- Structured logging (`LOGGING_STRUCTURED_FORMAT=ecs`) and Micrometer/Brave tracing
  propagate trace/span IDs across HTTP, Feign, and Kafka — useful for tracing a request
  across services without exposing request bodies in logs.
- All secrets (JWT secret, DB credentials, S3/CloudWatch keys) are **environment
  variables** with insecure defaults for local dev only (`local-dev-secret-...`,
  `athena`/`athena`, `test`/`test`). **None of these defaults are safe to run with
  outside a local machine** — a real deployment must override every one of them via a
  proper secret manager (this repo does not integrate one; see `docs/Deployment.md`).

## 9. Known gaps (documented, not implemented)

- No OAuth2/social login — email+password (+ optional 2FA) only.
- No rate limiting at the gateway (no bucket4j/Redis-based throttling on `/auth/login`
  or elsewhere) — a brute-force-protection gap worth closing before a public deployment.
- No centralized secret management (Vault, AWS Secrets Manager) — plain env vars only.
- No automated dependency/CVE scanning wired into CI (`ci.yml` runs build+test only).
