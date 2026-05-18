# Delivery Evidence

This directory contains the coworker-facing evidence produced by the dark factory
implementation workflow. It must be complete enough for another person or agent
to understand delivery from GitHub on their own machine.

These files are the GitHub-readable source of truth for delivery reporting. Do
not require readers to inspect local Beads databases to understand what was
delivered, when it started, when it finished, or how long it took.

Agents should update these files as they complete beads:

- `IMPLEMENTATION_LEDGER.md` records bead-level delivery evidence, including
  PRs, merge commits, changed files, UTC timing, elapsed wall time, and blockers.
- `QUALITY_LOG.md` records verification commands, outcomes, skipped checks, and
  environment blockers.
- Future files owned by reporting beads, such as a spec coverage matrix, demo
  runbook, or final delivery report, should be linked here when they are added.

When a phase is completed, record a cumulative phase audit. The audit should
cover Phase 1 through the phase that just completed, compare implementation
against the relevant specs, and state whether gaps were fixed or filed as new
beads. Record the audit command or manual review in `QUALITY_LOG.md`, and record
the delivery evidence or follow-up bead in `IMPLEMENTATION_LEDGER.md`.

Keep entries concise and factual. Prefer links to PRs and bead IDs over long
narrative.
