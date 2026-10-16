# ZALMOXIS

**An Android-native AI agent that remembers, reasons, and acts locally, while making its permissions, decisions, data access, and failures inspectable by the person who owns it.**

**Status:** [DESIGNED]. No implementation yet. This repository is in Phase 0 — defining the proof before writing code.

**Non-goals (this phase):** No model inference, no Android automation, no memory implementation. Phase 0 is documentation only.

---

## What Zalmoxis Is

Zalmoxis is the framework. It is not a single app. It is the substrate beneath one or more agents:

- **Memory core** — five layers: episodic anchors, semantic facts, identity profile, coherence engine, provider abstraction.
- **Identity persistence** — a permanent URN that survives model changes, hardware migrations, and provider switches.
- **Deterministic policy engine** — separate from the model. The model cannot rewrite the policy.
- **Audit trail** — append-only, hash-chained, exportable.
- **Scoped delegation** — every device-control capability is granted through narrow, expiring scopes. No global Full Trust switch.

The Android product that runs on Zalmoxis is currently codenamed *Sentinel* internally. That name is not yet cleared and will be decided separately.

## What Zalmoxis Is Not

- Not a chatbot.
- Not a cloud service.
- Not a wrapper around a hosted LLM.
- Not an app store for skills.

Zalmoxis runs locally. Cloud escalation exists but is explicit, cost-annotated, and requires user approval per instance.

---

## Current Status

| Layer | Status |
|-------|--------|
| Directory tree | Built (this session) |
| Foundational documents | Being written (this session) |
| Memory schema | Not started |
| Memory implementation | Not started |
| Android layer | Not started |
| Policy engine | Not started |
| Audit trail | Not started |
| Model integration | Not started |

See `ROADMAP.md` (local-only, not committed) for the full V10 roadmap.

## How to Run It Locally

Nothing to run yet. No code exists. This section will be updated when Phase 1 begins.

## Foundational Documents

| File | Purpose |
|------|---------|
| `README.md` | This file |
| `THREAT-MODEL.md` | Adversaries, attack surfaces, blast radius |
| `RECOVERY-PLAN.md` | Failure classes, detection, recovery, verification |
| `KILL-CRITERIA.md` | Pilot success criteria and stopping condition |
| `ROADMAP.md` | **Local-only.** Not committed. Not pushed. |
| `RULE SET.md` | **Local-only.** Not committed. Not pushed. |

## Contributing

This is a solo project in Phase 0. Contribution guidelines will be added when the project opens to external contributors.

## License

Undecided. The working assumption is AGPL-3.0-or-later for the community edition with commercial dual-licensing for enterprise. Final decision deferred until just before public release.
