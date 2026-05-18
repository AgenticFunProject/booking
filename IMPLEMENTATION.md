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
delivery report.

When finishing a bead, record the important details in the bead notes or in the
delivery evidence files once they exist:

- bead ID and title
- branch name
- PR URL
- merge commit
- verification commands and results
- notable files changed
- blockers, skipped checks, or follow-up beads
- demo notes or API examples, when relevant

The reporting epic is `bo-8z3`. It owns the implementation ledger, quality log,
spec coverage matrix, demo runbook, and final delivery report.

## Finish A Bead

1. Check `git status`.
2. Run the relevant verification.
3. Record delivery evidence for the bead.
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
