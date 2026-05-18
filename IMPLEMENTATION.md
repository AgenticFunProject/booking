# Booking Implementation Guide

Read this before starting any implementation bead.

## Fast Context

Before reading broad project docs, inspect the current repo state:

```bash
git status --short --branch
find . -maxdepth 2 -type f | sort | head -80
bd show <bead-id>
```

Use the bead as the source of truth for scope. If the Spring app already exists,
extend the existing structure. If it does not, create only the structure required
by the bead.

Do not re-plan the backlog unless the bead asks for planning. The implementation
beads already exist and are dependency-linked.

For small beads, read only:

1. `IMPLEMENTATION.md`
2. `AGENTS.md`
3. the bead's `Spec:` file
4. specs listed in that file's `# Depends on:` header
5. `docs/delivery/README.md` before finishing the bead

Do not read every spec by default.

## Workspace

- Use this git checkout, `booking/mayor/rig`, for all git and code changes.
- Use the booking rig root, `booking/`, for `bd` issue commands.
- In a Gas Town checkout, the rig root is usually two directories above this repo:

```bash
cd ../..
bd ready
```

- Do not work from `crew/`, `polecats/`, or `refinery/` directories unless explicitly assigned there by Gas Town.

## Branch Policy

The Mayor, agents, and workers must not make GitHub-pushed repo changes directly
on `master`. This applies to implementation, specs, docs, and planning files.

Before editing files:

```bash
git switch master
git pull --ff-only origin master
git switch -c work/<bead-id>-short-title
```

Use a bead-oriented branch name when a bead exists, or a short docs/planning
branch name for coordination changes. Commit the change there, push the branch,
and open a pull request unless the active workflow explicitly says otherwise.

## Dark Factory Flow

The human does not want to manually review or merge routine implementation PRs.

Agents and workers should still open pull requests from task branches. After the
required local checks and any configured CI checks pass, and the PR is mergeable,
merge the PR and delete the branch without asking the human.

Escalate instead of merging only when there is a blocker, an unfixable failing
check, a destructive change, or an explicit product/architecture decision.

## Parallel PR Coordination

Parallel work is allowed only when beads are independent and likely write sets do
not overlap.

- Use one branch per bead.
- Merge only when GitHub reports the PR is clean/mergeable.
- If another PR merges first, update the branch with `git fetch origin` and
  `git rebase origin/master`.
- Rerun the relevant verification after every rebase.
- Push rebased branches with `git push --force-with-lease`, never plain force.
- Do not intentionally parallelize beads that are likely to edit the same central
  files, such as `pom.xml`, `application.yml`, central service/controller/security
  classes, `AGENTS.md`, or `IMPLEMENTATION.md`.
- Escalate semantic conflicts or unclear merge decisions instead of guessing.

## Start A Bead

1. Run `bd prime` from the booking rig root.
2. Run `bd show <bead-id>` and read the description, dependencies, spec link, and acceptance criteria.
3. Claim the bead with `bd update <bead-id> --claim`.
4. In the git checkout, read `AGENTS.md`, the bead's `Spec:` file, and every file listed in that spec's `# Depends on:` header.
5. Keep the change scoped to the bead. Do not opportunistically implement later beads.

## Phase Plan

Use `bd ready` as the source of truth for exact task IDs and dependency state.
The phases below are the GitHub-readable roadmap: they describe what a person or
agent should deliver without requiring access to this machine's local bead IDs.

### Phase 1: Foundation Setup

Goal: make the repository a usable Spring Boot skeleton.

Deliverables:

- Maven project and dependency baseline.
- Java package tree for controllers, services, repositories, models, DTOs, config, exceptions, clients, mappers, and security.
- Spring Boot application entry point.
- Base application configuration and test profile configuration.
- Project ignore rules and generated-service developer README shell.

Completion signal: the project has a coherent source/resource/test structure,
foundation docs exist, and the smallest meaningful quality gate has been run or
its environment blocker is recorded.

Parallelism: package structure and developer docs can be done together. After
the package structure lands, application entry point, base config, and test
profile config can usually run in separate PRs.

### Phase 2: Domain Model

Goal: implement the booking aggregate, lifecycle vocabulary, validation shape,
and database migration.

Deliverables:

- Booking status enum with the specified lifecycle values.
- Equipment type enum with stable API codes.
- Booking and equipment-line entities.
- Entity equality, `toString`, and JSON serialization safeguards.
- Flyway migration for booking tables, indexes, constraints, and reference counter.

Completion signal: entities and migrations match the domain spec, compile where
the environment permits it, and domain gaps are recorded in delivery evidence.

Parallelism: the status and equipment enums can run together. Entity and
migration work should be mostly sequential because they must agree on field and
table shape.

### Phase 3: Data Access

Goal: add persistence APIs and query helpers used by the service layer.

Deliverables:

- Booking repository with reference, customer, status, schedule, and count queries.
- Equipment-line repository.
- Eager fetch queries for bookings with equipment lines.
- Null-safe booking specifications for optional filters.
- Concurrency-safe yearly reference counter persistence.
- Repository/data-access slice tests when the test environment is ready.

Completion signal: repositories expose the data access contract required by the
service layer, migrations support those queries, and repository verification is
logged.

### Phase 4: Service Layer

Goal: implement business orchestration and lifecycle behavior.

Deliverables:

- Business exception classes.
- External client interfaces and local stub implementations.
- Booking state machine.
- Booking reference generator.
- Create, read, list, confirm, start, complete, and cancel service flows.
- Focused service tests.

Completion signal: the service layer owns business rules, keeps authorization at
the API/security boundary, and tests or verification notes cover lifecycle
success and failure paths.

### Phase 5: API And Error Handling

Goal: expose the service through REST endpoints with structured API errors.

Deliverables:

- Request and response DTO records.
- Booking mapper.
- Booking controller endpoints for create, get, list, cancel, confirm, start, and complete.
- OpenAPI annotations and paths.
- Structured error response DTOs.
- Global exception handler mappings for business, validation, and framework errors.
- Controller and error-handling tests.

Completion signal: API behavior matches the endpoint specs, entities are not
exposed directly, and error responses use the documented shape.

### Phase 6: Integration And Security

Goal: add production-facing client infrastructure and authorization behavior.

Deliverables:

- Typed integration properties.
- RestClient configuration and logging interceptor.
- Resilience4j defaults and health configuration.
- JWT properties, token provider, authentication filter, security config, and error handlers.
- Security context helper and booking access authorizer.
- Ownership checks wired into controllers.
- Integration and security tests.

Completion signal: local stubs remain usable, production client/security
infrastructure is present, and ownership rules are verified or documented.

### Phase 7: Test Suite

Goal: broaden confidence across unit, integration, security, and end-to-end flows.

Deliverables:

- Test data builders and JWT test helpers.
- Domain, repository, service, controller, security, and full lifecycle tests.
- Focused commands documented for running each test category.

Completion signal: the intended test groups can be run, failures are fixed or
recorded with clear blockers, and the quality log reflects the real state.

### Phase 8: Deployment And Final Report

Goal: make the service runnable/demoable and produce coworker-facing evidence.

Deliverables:

- Dockerfile, Docker Compose stack, profile-specific config, logging, request tracing, environment example, CI workflow, Makefile, and final README pass.
- Spec coverage matrix.
- Demo and API runbook.
- Final delivery report.
- Final quality gate.

Completion signal: another person can understand what was built from GitHub,
run or inspect the service using documented steps, and review implementation,
quality, demo, and known-limitation evidence.

## Project Rules

- Java 21, Spring Boot 3.5.x, Maven.
- Layered architecture: controller -> service -> repository. Security authorizers may query repositories only for ownership checks.
- DTOs are Java records. Never expose JPA entities in API responses.
- Constructor injection only. Do not use field injection.
- Entity IDs are `Long` with `GenerationType.IDENTITY`.
- Timestamps are `Instant` in UTC.
- Messaging/event streaming is out of scope for v1. Do not add Kafka.
- External service real clients are out of scope until contracts exist. Implement interfaces and local stubs only.
- Keep documentation, specs, comments, and examples portable. Do not write user-specific absolute home-directory paths; use repo-relative paths or Gas Town role paths like `booking/mayor/rig`.

## Verification

Run the smallest meaningful gate for the bead:

- Spec/docs-only: `git diff --check`
- Java code after Maven exists: `./mvnw compile`
- Focused tests when test code exists: `./mvnw test -Dtest="<ClassName>"`
- Before closing larger integration beads: run the relevant grouped tests from `AGENTS.md`

If a gate cannot run because the project is not far enough along, note the reason on the bead before closing it.

## Delivery Evidence

Every implementation bead should leave enough evidence for a coworker-facing
delivery report. Coworkers may read only GitHub, so do not rely on local Beads
or local machine state for report data.

When finishing a bead, record the important details in the bead notes and in the
delivery evidence files:

- bead ID and title
- branch name
- PR URL
- merge commit
- started UTC timestamp
- completed UTC timestamp
- elapsed wall time
- timing source, such as copied bead fields, an agent timer, or CI timestamps
- verification commands and results
- notable files changed
- blockers, skipped checks, or follow-up beads
- demo notes or API examples, when relevant

Copy timing into `docs/delivery/IMPLEMENTATION_LEDGER.md` before closing the
bead or merging the PR. Do not write instructions that require readers to infer
timing from local bead databases.

The reporting epic is `bo-8z3`. It owns the implementation ledger, quality log,
spec coverage matrix, demo runbook, and final delivery report.

Current evidence files:

- `docs/delivery/README.md`
- `docs/delivery/IMPLEMENTATION_LEDGER.md`
- `docs/delivery/QUALITY_LOG.md`

## Finish A Bead

1. Check `git status`.
2. Run the relevant verification.
3. Record delivery evidence for the bead in `docs/delivery/IMPLEMENTATION_LEDGER.md` and, when verification changed or ran, `docs/delivery/QUALITY_LOG.md`.
4. Commit the code change.
5. Push the branch/commit according to the active workflow.
6. Close the bead with `bd close <bead-id>` only after the acceptance criteria are met.
7. If you discover follow-up work, create a new bead instead of leaving TODO comments.

## Useful Commands

```bash
cd ../..
bd ready
bd show <bead-id>
bd update <bead-id> --claim
bd close <bead-id>

cd mayor/rig
git status --short --branch
./mvnw compile
```
