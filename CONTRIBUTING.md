# Contributing to Athena Backend

Thanks for considering a contribution. This document covers the process; for build/test
mechanics specific to this repo (running a single module, adding an endpoint, adding a
Kafka event), see [`docs/Contributing.md`](docs/Contributing.md).

By participating in this project you're expected to uphold the
[Code of Conduct](CODE_OF_CONDUCT.md).

## Before you start

- For anything beyond a small fix, open an issue first describing what you want to
  change and why — it saves everyone time if the approach needs discussion before code
  is written.
- Check `ROADMAP.md` and open issues/PRs to avoid duplicating work already in flight.

## Fork and branch

1. Fork the repository and clone your fork.
2. Create a branch off `master`, named descriptively:
   ```
   feature/<short-description>     new functionality
   fix/<short-description>         bug fix
   docs/<short-description>        documentation only
   chore/<short-description>       tooling, dependencies, CI
   ```

## Commit messages

Write commits that explain **why**, not just what — the diff already shows what changed.
Keep the summary line under ~72 characters, imperative mood ("Add retry backoff to LM
Studio client", not "Added" or "Adds"). Group related changes into one commit rather than
many tiny ones; split unrelated changes into separate commits rather than one large one.

## Coding style

Match the conventions already used throughout the codebase:
- **Layering**: `controller` (thin HTTP adapter, no business logic) → `service`
  (interface + impl) → `repository`. DTOs (records) at every boundary; JPA entities never
  returned from a controller or crossed between services.
- **Cross-service side effects go through Kafka**, not direct calls — see
  `docs/Architecture.md` for when a synchronous Feign call is appropriate instead
  (existence checks, "generate this now and give me the result").
- **No comments explaining what code does** — name things so the code reads clearly;
  only comment a genuinely non-obvious constraint or workaround.
- Modern Java: records for DTOs/events, `Optional`/streams over manual null-checking
  loops, `Clock`-injected time in anything under test.
- Run `./mvnw clean verify` before opening a PR — this is exactly what CI runs.

## Testing expectations

New behavior needs test coverage in the same style as the surrounding code (see
`docs/Development.md` for the specific patterns: `@WebMvcTest`+MockMvc for controllers,
Mockito for services, injected `Clock` for time-dependent logic, mocked LM Studio
providers for anything AI-backed). A PR that changes behavior without a test covering it
will likely be asked to add one.

## Opening a pull request

- Target `master`.
- Fill in the PR template (`.github/PULL_REQUEST_TEMPLATE.md`) — what changed, why, and
  how you tested it.
- Keep PRs focused; a PR that mixes an unrelated refactor with a feature is harder to
  review and more likely to get stuck.
- CI (`ci.yml`) must pass — full reactor build + test.

## Review process

A maintainer will review for correctness, adherence to the existing architecture
(service boundaries, DB-per-service, event-vs-Feign choice), and test coverage. Expect
feedback rounds on anything non-trivial — that's normal, not a rejection.

## Reporting bugs / requesting features

Use the issue templates under `.github/ISSUE_TEMPLATE/`. For security vulnerabilities,
**do not** open a public issue — see [`SECURITY.md`](SECURITY.md).

## Issue labels (recommended)

The repository doesn't yet have a formal label set configured; suggested starting point:
`bug`, `enhancement`, `documentation`, `good first issue`, `help wanted`, and one
`area:<service-name>` label per module to route issues quickly.

## Recommended repository setup (not yet enabled)

Noted here for whoever administers the GitHub repository — these are suggestions, not
things currently configured:
- **GitHub Discussions** for design questions and proposals, kept separate from Issues.
- **GitHub Projects** board mirroring `ROADMAP.md`'s In Progress / Planned columns.
- **Branch protection on `master`**: require the CI check and at least one approving
  review before merge.
- **GitHub Actions**: beyond the existing `ci.yml` (build+test) and
  `discord-notifications.yml`, consider adding a Docker image build/publish workflow
  once there's somewhere to deploy those images (see `docs/Deployment.md`).
