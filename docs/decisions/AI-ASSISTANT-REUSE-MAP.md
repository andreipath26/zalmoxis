# AI Assistant to Zalmoxis: Reuse Map

**Reviewed:** 2026-11-23  
**Purpose:** Identify what the AI-ASSISTANT repository can contribute to Zalmoxis without confusing their products, roadmaps, or acceptance status.

## Decision

Keep the repositories separate. Zalmoxis is an Android-first product centered on inspectable memory and governed device actions. AI-ASSISTANT is a local-first framework for multiple ecosystem projects, with a deterministic Python core, optional local inference, and bounded helper tools.

AI-ASSISTANT offers reusable **design patterns and candidate components**. Its phase passes do not satisfy Zalmoxis gates, and its current implementation is not a drop-in Zalmoxis runtime.

## Component mapping

| Zalmoxis requirement | AI-ASSISTANT material | Fit and boundary |
|---|---|---|
| Deterministic command and authorization boundary (Zalmoxis Phases 2.5 and 6) | [P2 engine, typed intents, execution control, and audit](https://github.com/andreipath26/AI-ASSISTANT/tree/main/ASSISTANT/core); [G2 review](https://github.com/andreipath26/AI-ASSISTANT/blob/main/docs/p2/G2_TECHNICAL_REVIEW.md) | **Reuse as a design reference; port selectively.** The Python kernel has typed commands, scoped grants, one-use approvals, dry runs, cancellation, and fail-closed dispatch. Zalmoxis still needs Android-native authorization against the final action parameters, bounded expiring grants, immediate revocation, and its own acceptance tests. P2 passing does not pass a Zalmoxis gate. |
| Local inference (Zalmoxis Phase 1) | [P3 llama.cpp/Qwen3 integration](https://github.com/andreipath26/AI-ASSISTANT/blob/main/docs/p3/LLAMACPP_QWEN3_INTEGRATION.md) | **Reference only.** AI-ASSISTANT uses a llama.cpp/GGUF path; Zalmoxis specifies KILN and evaluates Maple/Gemma. The current P3 release gate is unpassed, and model/runtime redistribution and device qualification remain open. |
| Local voice (Zalmoxis Phase 1) | AI-ASSISTANT P4 speech implementation and packaging scripts; status documented in the [P5 handoff](https://github.com/andreipath26/AI-ASSISTANT/blob/main/docs/p5/IMPLEMENTATION_HANDOFF.md) | **Reference only.** The implementation is not evidence of a qualified Android speech stack. G4 remains unpassed; Zalmoxis still needs its own latency, device, privacy, and offline-capability evidence. |
| Persistent memory (Zalmoxis Phase 0) | [P5 handoff](https://github.com/andreipath26/AI-ASSISTANT/blob/main/docs/p5/IMPLEMENTATION_HANDOFF.md) | **Not equivalent.** AI-ASSISTANT provides optional per-user/application/session SQLite conversation history and workflow checkpoints. Zalmoxis requires temporal episodic and semantic memory, provenance, identity continuity, contradiction handling, quarantine, verified deletion, and the user-facing memory contract. P5 is implemented but G5 is unpassed; its history storage is not Zalmoxis memory. |
| Android app and device skills (Zalmoxis Phase 2) | AI-ASSISTANT Android chat preview, model bridge, and P5 Android companion | **Limited overlap.** This is a chat/inference preview, not Zalmoxis’s document ingestion, OCR/classification, per-client isolation, memory inspection, permission UI, or governed device automation. Physical-device qualification is still outstanding. |
| Local tools, skills, and scheduling (Zalmoxis Phases 2.5 and 3) | [P6 implementation status](https://github.com/andreipath26/AI-ASSISTANT/blob/main/docs/p6/IMPLEMENTATION_STATUS.md) and [G6 review](https://github.com/andreipath26/AI-ASSISTANT/blob/main/docs/p6/G6_TECHNICAL_REVIEW.md) | **Reuse safety patterns, not the implementation as-is.** P6 qualifies bounded file operations, declarative skills, a foreground scheduler, and a bubblewrap worker for a specific Linux profile. It does not provide Android skills, unattended background autonomy, or Zalmoxis’s proactive engine. G6 passed only for that scoped Linux profile. |
| Audit integrity and secrets (Zalmoxis Foundation and Phase 6) | AI-ASSISTANT core audit module and P2 audit contract | **Partial pattern match.** AI-ASSISTANT records metadata-only events; its in-memory fallback is ephemeral and not tamper-evident. Zalmoxis requires an append-only hash chain and a separate secrets vault. Treat these as unimplemented in Zalmoxis until built and verified there. |
| Ecosystem integrations (later Zalmoxis phases) | AI-ASSISTANT P7 universal integration SDK | **No current component to reuse.** P7 is not started. Do not count planned SDK work as delivered integration capability. |

## Zalmoxis phase implications

- **Phase 0:** AI-ASSISTANT’s governance work is useful process precedent, but it does not supply Zalmoxis’s pending memory contract, target-device performance gate, or customer discovery.
- **Phase 1:** P3/P4 are research and implementation references only. They do not replace Zalmoxis’s KILN/model decisions or Android qualification.
- **Phase 2 and 2.5:** Reuse the authorization concepts as design input. Build and qualify Zalmoxis’s Android document workflow, native skills, and scoped delegation separately.
- **Phases 3–9:** No delivered AI-ASSISTANT component currently satisfies Zalmoxis’s proactive engine, knowledge evolution, identity persistence, device-cloud bridge, enterprise platform, or defense-partnership deliverables.

Zalmoxis remains a documentation-and-skeleton repository at Phase 0; its roadmap marks the first implementation work as not yet built. AI-ASSISTANT’s P0–P2 and scoped P6 results must not be represented as Zalmoxis implementation or evidence.

## Roadmap issue to resolve before using it as a delivery baseline

The 90-day proof section schedules first-customer validation on Days 57–74. The kill-criteria document says the 90-day pilot starts only after Phase 0 and Phase 1, while the full roadmap schedules Phase 1 in Weeks 4–7 and the Phase 2 MVP subset in Weeks 8–13. Reconcile the pilot start, MVP completion, and gate dates into one sequence before treating the calendar as committed.

**Recommended use:** retain the repositories as separate projects; use this map to select specific design patterns for Zalmoxis; require Zalmoxis-specific contracts, Kotlin/Android implementation, and evidence for every adopted capability.
