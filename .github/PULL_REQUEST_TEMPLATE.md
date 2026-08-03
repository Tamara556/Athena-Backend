## What changed
<!-- Summarize the change. Link the issue this addresses, if any. -->

## Why
<!-- The motivation — what problem this solves or what it enables. -->

## Affected module(s)
<!-- e.g. ai-service, api-gateway, docs/ -->

## How this was tested
<!-- ./mvnw clean verify? Manual testing against docker compose? Specific curl/Postman
     calls? Be specific — "tested locally" isn't enough for a reviewer to trust. -->

## Checklist
- [ ] `./mvnw clean verify` passes locally
- [ ] New/changed behavior has test coverage (see `docs/Development.md` for the
      conventions used in this codebase)
- [ ] Docs updated if this changes an API contract (`docs/API.md`, `docs/Backend.md`) or
      architecture (`docs/Architecture.md`)
- [ ] No secrets or credentials included in the diff
