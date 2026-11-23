# Roadmap Reiteration from the Shared Chat

This file extracts the “What will be built” phase summary from the owner-provided shared chat. It is a roadmap summary, not an implementation claim and not a replacement for the full local-only V10 roadmap or the local-only rule set.

## Phase 0 — Defining the proof (current)

- Threat model — done.
- Recovery plan — done.
- Kill criteria — done.
- Memory Contract — pending.
- Model Performance Gate — pending.
- Customer Discovery Questions — pending.

**Exit condition:** a testable specification with explicit exclusions and no unresolved critical architectural assumption. `[DESIGNED]`

## Phase 1 — The local brain (8 to 12 weeks)

- KILN inference runtime. `[RESEARCHED]`
- Maple Preview or Gemma on-device. `[RESEARCHED]`
- Three-tier execution: instant skills, deterministic flows, and agent loop. `[DESIGNED]`
- Voice pipeline. `[DESIGNED]`
- Realistic offline contract with a published capability matrix. `[DESIGNED]`

**Exit condition:** the complete core workflow works on the target device and passes its defined correctness and isolation tests.

## Phase 2 — The Android layer

**MVP subset:**

- Document ingestion for PDF, DOCX, email, and OCR. `[DESIGNED]`
- Classification and entity extraction. `[DESIGNED]`
- Per-client folder isolation. `[DESIGNED]`
- Native Kotlin skills. `[DESIGNED]`
- Two-tier UI: Simple Mode and Power User Backend. `[DESIGNED]`
- Audit logging with a hash chain. `[DESIGNED]`
- Corrected OTA updater. `[DESIGNED]`

**Post-MVP:** UI automation, on-device MCP server, skill routing, skill composition, scoped skill personas, capability state machine, and vendor-specific testing.

## Phase 2.5 — Scoped delegation

Replaces the global Full Trust switch with narrow, expiring grants and a deterministic policy engine. A panic button is always present. Full Trust remains opt-in and invite-only until six months have passed with zero incidents before general availability. `[DESIGNED]`

## Phase 3 — Proactive engine

Treat the heartbeat as a temporal opportunity. Action, delay, and non-action are all valid. Proactivity must earn its battery budget. `[RESEARCHED]`

## Phase 4 — Device-cloud bridge

Use the EcoAgent three-agent architecture and Pre-Understanding Module (48.6× data reduction), with user-visible cloud escalation, cost disclosure, and approval. `[RESEARCHED]`

## Phase 5 — Knowledge evolution

Conservative, inspectable memory consolidation; skill extraction and composition; derived personas that fail closed; and cross-session continuity. `[RESEARCHED]`

## Phase 6 — Security hardening

Machine-enforced authorization architecture, formal prompt-injection threat model, secrets vault, audit hash chain, injection scanning at ingestion, taint tracking, and OWASP MASVS. `[DESIGNED]`

## Phase 7 — Launch and iteration

Public beta, feedback pipeline, model updates, skills ecosystem, and continuous security review. The published reliability and safety report is a Phase 7 deliverable. `[DESIGNED]`

## Phase 8 — Platform and hardware agnosticism

Kotlin Multiplatform, dual runtime, storage-agnostic memory, cross-device federation, headless server mode, and small-team and enterprise segments. `[RESEARCHED]`

## Phase 9 — Defense partnership (post-PMF)

A long-term strategic option, not a near-term revenue path and not a roadmap dependency. `[DESIGNED]`
