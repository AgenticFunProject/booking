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

Do not read every spec by default.

## Workspace

- Use `/home/gaborvatany/gt/booking/mayor/rig` for all git and code changes.
- Use `/home/gaborvatany/gt/booking` for `bd` issue commands.
- Do not work from `crew/`, `polecats/`, or `refinery/` directories unless explicitly assigned there by Gas Town.

## Start A Bead

1. Run `bd prime` from `/home/gaborvatany/gt/booking`.
2. Run `bd show <bead-id>` and read the description, dependencies, spec link, and acceptance criteria.
3. Claim the bead with `bd update <bead-id> --claim`.
4. In the git checkout, read `AGENTS.md`, the bead's `Spec:` file, and every file listed in that spec's `# Depends on:` header.
5. Keep the change scoped to the bead. Do not opportunistically implement later beads.

## Implementation Order

Start with `bo-u2r.1`, then continue through the ready queue from `bd ready`.

High-level sequence:

1. Foundation: `bo-u2r.*`
2. Domain model: `bo-7or.*`
3. Data access: `bo-eyx.*`
4. Service layer: `bo-0wh.*`
5. REST API: `bo-2tm.*`
6. Error handling, integrations, tests, security, and deployment as they become ready

Use `bd ready` as the source of truth for what can be worked next.

## Project Rules

- Java 21, Spring Boot 3.5.x, Maven.
- Layered architecture: controller -> service -> repository. Security authorizers may query repositories only for ownership checks.
- DTOs are Java records. Never expose JPA entities in API responses.
- Constructor injection only. Do not use field injection.
- Entity IDs are `Long` with `GenerationType.IDENTITY`.
- Timestamps are `Instant` in UTC.
- Messaging/event streaming is out of scope for v1. Do not add Kafka.
- External service real clients are out of scope until contracts exist. Implement interfaces and local stubs only.

## Verification

Run the smallest meaningful gate for the bead:

- Spec/docs-only: `git diff --check`
- Java code after Maven exists: `./mvnw compile`
- Focused tests when test code exists: `./mvnw test -Dtest="<ClassName>"`
- Before closing larger integration beads: run the relevant grouped tests from `AGENTS.md`

If a gate cannot run because the project is not far enough along, note the reason on the bead before closing it.

## Finish A Bead

1. Check `git status`.
2. Run the relevant verification.
3. Commit the code change.
4. Push the branch/commit according to the active workflow.
5. Close the bead with `bd close <bead-id>` only after the acceptance criteria are met.
6. If you discover follow-up work, create a new bead instead of leaving TODO comments.

## Useful Commands

```bash
cd /home/gaborvatany/gt/booking
bd ready
bd show <bead-id>
bd update <bead-id> --claim
bd close <bead-id>

cd /home/gaborvatany/gt/booking/mayor/rig
git status --short --branch
./mvnw compile
```
