# Frontend (companion repository)

This repository is **backend-only**. The web client lives in a separate repository:

**[`Tamara556/Athena-Frontend`](https://github.com/Tamara556/Athena-Frontend)**

This document is a summary for backend contributors who need to know how the frontend
consumes this API — it is not a substitute for that repository's own documentation, and
nothing here is authoritative about frontend code.

## Stack

Angular 20 (Signals-based, no NgRx/RxJS-store), structured as:

```
src/app/
├── core/          singletons: services, guards, interceptors, models
├── features/      feature modules — daily-journey, knowledge-graph, interviews,
│                  achievements, progress, athena-insights, learning-session,
│                  profile, settings, streaks
├── pages/         routed page shells — home, login, register, onboarding,
│                  dashboard, roadmap
└── shared/        reusable components/pipes/directives
```

## How it talks to this backend

- **Base URL**: the gateway (`http://localhost:8080` locally) — the frontend never
  calls a business service's own port.
- **Auth**: stores the access/refresh token pair from `/auth/login` /
  `/auth/register`, sends `Authorization: Bearer <token>` on every subsequent request,
  and refreshes via `/auth/refresh` on `401`.
- **Contract**: matches `docs/API.md` — one Angular service per backend domain roughly
  mirrors the endpoint groups there (auth/account, users, progress, learning, badges,
  ai/onboarding, roadmap, daily journey, learning sessions, knowledge graph, interviews,
  rag).
- **Mock-swappable data layer**: several frontend feature pages were originally built
  against typed mock APIs with a contract matching a proposed backend endpoint, then
  swapped to the real endpoint once it existed backend-side (e.g. Knowledge Graph,
  Interviews, Achievements, Progress). Some pages may still be mock-backed if the
  corresponding backend contract doesn't exist yet — check the frontend repo's own
  state rather than assuming parity with this document.

## Scope note

Backend contributors should treat `docs/API.md` as the contract to keep stable;
frontend-side architecture, routing, state management, and UI decisions are out of
scope for this repository's documentation.
