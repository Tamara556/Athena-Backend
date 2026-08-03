# Contributing (developer mechanics)

For branch naming, commit style, PR process, and code of conduct, see the root
[`CONTRIBUTING.md`](../CONTRIBUTING.md). This page covers only the repo-specific
mechanics of building and testing changes.

## Building the reactor

```bash
./mvnw clean install
```

Builds every module in dependency order (`athena-common` and `athena-llm` first, since
almost everything depends on them). If you only changed one module, you can build just
its subtree:

```bash
./mvnw -pl <module> -am clean install
```

`-am` ("also make") pulls in whatever that module depends on within the reactor, so you
don't need to `install` the whole thing first.

## Running a single module against the rest of the stack

```bash
docker compose up discovery-server kafka redis <module>-db   # infra + this module's DB
./mvnw -pl <module> spring-boot:run
```

The module registers with the compose-network Eureka instance and can call/be called by
the other services already running in Docker, as long as `EUREKA_URI` and
`SPRING_DATASOURCE_URL` are pointed correctly (defaults in each `application.yml` assume
this exact setup).

## Running tests

```bash
./mvnw test                    # this module and its dependencies
./mvnw -pl <module> test       # just this module
./mvnw clean verify            # the full CI check (ci.yml runs exactly this)
```

See `docs/Development.md` for the testing conventions (Mockito, `@WebMvcTest`, injected
`Clock`, mocked LM Studio providers) to follow when adding new tests.

## Adding a new event

Cross-service side effects go through Kafka, not direct calls (see
`docs/Architecture.md` §4). To add one:
1. Add the event record to `athena-common/src/main/java/com/athena/common/event/`.
2. Add its topic name to `KafkaTopics` in the same package.
3. Publish it from the producing service, consume it in the interested service(s) —
   both sides depend on `athena-common`, so no cross-service compile-time coupling is
   introduced.

## Adding a new endpoint

Follow the existing layering in every module: `controller` (thin — no business logic) →
`service` (interface + impl) → `repository`. DTOs at the boundary; JPA entities never
returned from a controller. Add the route to `api-gateway`'s
`src/main/resources/application.yml` (`spring.cloud.gateway.server.webflux.routes`) if
it's a new path prefix, and update `docs/API.md` + `docs/Backend.md`.

## Recommended repository setup (not yet enabled)

These aren't implemented in this repository today; noted here as recommendations for
whoever administers the GitHub repo, not as existing configuration:
- **GitHub Discussions** — enable for Q&A / design proposals, separate from Issues (bug
  reports and feature requests, see the issue templates in `.github/ISSUE_TEMPLATE/`).
- **Labels** — a minimal starter set: `bug`, `enhancement`, `documentation`,
  `good first issue`, `help wanted`, one label per service (`area:auth-service`,
  `area:ai-service`, etc.) to route issues to the right module quickly.
- **GitHub Projects** — a single board tracking `ROADMAP.md`'s In Progress / Planned
  items would keep the roadmap doc and actual issue tracking from drifting apart.
- **Branch protection on `master`** — require the `ci.yml` check and at least one
  review before merge, once there's more than one regular contributor.
