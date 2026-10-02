# Athena Foundation Tests — Playwright (Phase 1.1)

Browser + HTTP **smoke** checks that verify the app environment is healthy before
feature-level E2E runs. Source of truth: [`../FOUNDATION_TEST_CASES.md`](../FOUNDATION_TEST_CASES.md)
and [`../FOUNDATION_PLAYWRIGHT_PLAN.md`](../FOUNDATION_PLAYWRIGHT_PLAN.md).

> Integration checks (Kafka, Redis, PostgreSQL, Liquibase, Docker orchestration,
> Eureka lifecycle/resilience) are **out of scope here** — they belong to the
> Maven/Testcontainers suite. This project only automates the browser/HTTP subset.

## What runs

| Layer | Spec | Test IDs | Needs backend? |
|---|---|---|---|
| Frontend (browser) | `tests/frontend/landing.spec.ts` | FND-START-005 + landing render | No |
| Frontend (browser) | `tests/frontend/runtime-config.spec.ts` | FND-CONF-007 (runtime) | No |
| HTTP | `tests/backend/gateway-health.spec.ts` | FND-HEALTH-007 / FND-ENV-004 | Yes* |
| HTTP | `tests/backend/public-routes.spec.ts` | FND-GW-008, FND-GW-004 | Yes* |
| HTTP | `tests/backend/eureka-registration.spec.ts` | FND-EUREKA-001/002 | Yes* |
| Browser (CORS) | `tests/backend/browser-backend.spec.ts` | FND-START-006, FND-GW-010 | Yes* |

\* Backend-dependent specs **skip with a clear reason** when the gateway is not
reachable — the suite still goes green without the Docker stack up.

## Run

```bash
npm ci
npx playwright install          # first time: browser binaries
npm test                        # all projects (chromium, firefox, webkit)
npm run test:chromium           # single browser
npm run report                  # open the HTML report
```

Playwright starts the Angular dev server automatically (`webServer` → `npm start`
in `../Athena-Frontend`). To exercise the backend specs, start the stack first:

```bash
cd .. && docker compose up --build -d
```

## Design

- **Page Object Model** — `src/pages/*.page.ts` (no selectors in specs).
- **Fixtures** — `src/fixtures/test-fixtures.ts` provides page objects, per-worker
  gateway/eureka HTTP clients, and a `backendAvailable` gate (probed once/worker).
- **Config** — `src/config/env.ts` is the single source of endpoints (env-overridable).
- **Deterministic waits** — web-first assertions and `waitForRequest`; no `sleep`.
- **Artifacts** — HTML + JSON reports; trace / screenshot / video retained on failure.
