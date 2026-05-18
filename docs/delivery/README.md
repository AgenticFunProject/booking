# Delivery Evidence

This directory contains the coworker-facing evidence produced by the dark factory
implementation workflow.

These files are the GitHub-readable source of truth for delivery reporting. Do
not require readers to inspect local Beads databases to understand what was
delivered, when it started, when it finished, or how long it took.

Agents should update these files as they complete beads:

- `IMPLEMENTATION_LEDGER.md` records bead-level delivery evidence.
- `QUALITY_LOG.md` records verification commands and outcomes.

Keep entries concise and factual. Prefer links to PRs and bead IDs over long
narrative.
