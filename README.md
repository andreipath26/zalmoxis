# ZALMOXIS

> **An Android-native AI agent that remembers, reasons, and acts locally,
> while making its permissions, decisions, data access, and failures
> inspectable by the person who owns it.**

**Status:** Phase 0; implementation has started. The Android local-inference foundation exists but has not been built or validated.
**Repository:** private, at `github.com/andreipath26/zalmoxis`.
**License:** undecided (working assumption: AGPL-3.0-or-later + commercial dual-license).
**Phase:** 0 of 9 — Defining the Proof.
**Implementation:** Android Compose foundation under `android/`, with LiteRT-LM as the first local inference adapter and offline speech input/output scaffolding.

---

## Table of contents

1. What ZALMOXIS is
2. What ZALMOXIS is not
3. The evidence tags
4. The five architectural commitments
5. The memory model
6. The permission model
7. The autonomy levels
8. The security posture
9. The technology stack
10. What is built today
11. What will be built
12. How to run it locally
13. Project layout
14. Foundational documents
15. The competitive context
16. The hard truths
17. Contributing
18. License
19. How this repository is maintained

---

## 1. What ZALMOXIS is

ZALMOXIS is the framework, not a single app. It is the substrate beneath one
or more agents. Sentinel was the previous name for ZALMOXIS. It is not a separate product.

The framework provides a memory core, an identity layer, a deterministic
policy engine, an append-only audit trail, a scoped permission architecture,
and a local-first inference runtime.

The thesis, in one sentence: delegation without loss of control.

Three hypotheses remain open. Hypothesis A: the problem exists. Hypothesis B:
ZALMOXIS solves it better. Hypothesis C: customers will pay enough. Phase 0
exists to make these hypotheses testable.

---

## 2. What ZALMOXIS is not

- Not a chatbot. A chatbot answers; ZALMOXIS remembers, reasons, and acts under governance.
- Not a cloud service. Primary inference runs on-device.
- Not a wrapper around a hosted LLM. Memory, policy, and audit are not replaceable by an API call.
- Not a skill app store. Skills are composed, not downloaded.
- Not a general-purpose autonomous agent. Every irreversible action is gated.
- Not a finished product. The Android foundation is source-only and has not been built or validated.

---

## 3. The evidence tags

Every capability carries one of five tags. The tags are the honest state of the claim.

| Tag | Meaning |
|-----|---------|
| `[DESIGNED]` | Architecture specified. No implementation. |
| `[RESEARCHED]` | Grounded in published research or a production-tested reference. |
| `[BUILT]` | Code exists. Tests pass. |
| `[VALIDATED]` | Tested with real users, real data, real outcomes. |
| `[PROVEN]` | Demonstrated over time with zero trust-breaking incidents. |

As of this writing, almost everything is `[DESIGNED]` or `[RESEARCHED]`. The only `[BUILT]` items are the repository skeleton, the git hooks, and the foundational documents.

---

## 4. The five architectural commitments

These do not change between phases. They are the load-bearing walls.

### 4.1 Local by default

Primary inference runs on the device. Cloud escalation is explicit, cost-annotated, and approved per instance. There is no background cloud call. `[DESIGNED]`

### 4.2 The model cannot rewrite the policy

A deterministic policy engine sits between the model and every consequential action. The model proposes; the policy engine evaluates the actual parameters and decides. The model cannot widen its own permissions, approve its own execution, or rewrite the rules. `[DESIGNED]`

### 4.3 Memory has provenance or it does not exist

Every retrieval returns the source, the timestamp, and the confidence. There are no orphan memories. The user can answer the eight memory-contract questions about anything the system believes. `[DESIGNED]`

### 4.4 Deletion is verified, not promised

A delete is not complete until verification confirms removal from every layer: episodic, semantic, identity, vector index, cache, and backup rotation. The deletion certificate is the proof. `[DESIGNED]`

### 4.5 Every action is audited

The audit trail is append-only and hash-chained. No modification, no deletion, no reordering. Every entry is chained to its predecessor. Verification is repeatable and independent. `[DESIGNED]`

---

## 5. The memory model

The memory core is deterministic where it matters: retrieval, promotion, demotion, deletion, and coherence reconciliation are deterministic operations. The model may propose; the memory core decides. `[DESIGNED]`

### 5.1 The five layers

**Episodic anchors.** A temporal knowledge graph. Every memory is an event with a validity window and a provenance chain. Events are linked by progression and by topic. `[RESEARCHED]`

**Semantic facts.** A multi-level store with trust scores. A fact moves from episodic to semantic only through review-gated promotion. No autonomous promotion. `[RESEARCHED]`

**Identity profile.** A permanent URN minted once, never changing. The identity survives model changes, hardware migrations, and provider switches. `[RESEARCHED]`

**Coherence engine.** Detects conflict. When two memories disagree, both are surfaced with their provenance. The newer fact does not silently override the older one. `[DESIGNED]`

**Provider abstraction.** The storage backend is swappable: SQLite on phone, LanceDB on desktop, Qdrant or PostgreSQL on a home hub, Neo4j on enterprise. `[RESEARCHED]`

### 5.2 Active Memory

Interference-aware retrieval. When a query could match conflicting memories, both are surfaced with their provenance rather than one being silently preferred. `[DESIGNED]`

### 5.3 Instruction persistence

Explicit constraints are stored separately from passive preferences. An explicit instruction takes precedence over an inferred preference. Constraints persist across sessions. `[DESIGNED]`

### 5.4 Memory quarantine

Uncertain or conflicting facts are quarantined. A newly inferred preference does not silently overwrite an established fact. Material changes are surfaced for correction or confirmation. `[DESIGNED]`

### 5.5 The memory contract

The user must be able to answer, for anything the system believes:

1. What does ZALMOXIS believe about me?
2. Where did each fact come from?
3. When was it last confirmed?
4. How certain is it?
5. Which workflows can access it?
6. What has been marked historical?
7. What remains after I delete it?
8. Can I export everything and use it without ZALMOXIS servers?

This contract is the product promise. It is not a feature. `[DESIGNED]`

### 5.6 Hard-delete verification

Deletion removes from all layers. A verification step confirms removal. The audit log records the deletion. The user can request a deletion certificate. `[DESIGNED]`

### 5.7 Memory reliability testing

The memory core is tested against image and channel bleeding, personalization override, explicit instruction retention, cross-session recall, and long-horizon retrieval. Results are published, not hidden. `[DESIGNED]`

---

## 6. The permission model

### 6.1 Scoped delegation, not global trust

There is no global Full Trust switch. Every device-control capability is granted through a narrow, expiring scope. The grant names what is permitted, for how long, and under what conditions. `[DESIGNED]`

Three tiers:

- **Allowed without repeated approval.** Read selected notifications, summarize approved folders, organize local notes, run previously approved workflows.
- **Allowed only within explicit limits.** Send messages to specified contacts, interact with approved applications, perform actions under a spending cap.
- **Always require fresh authorization.** Transfers of money, purchases beyond a limit, publishing sensitive content, granting permissions, exporting credentials, other high-impact actions.

The categorization is risk-based, not merely based on whether an action is technically reversible. `[DESIGNED]`

### 6.2 The deterministic policy engine

The policy engine is a separate subsystem the model cannot rewrite. Its interface is small and explicit: check action against context, authorize a bounded grant with scope and expiry, revoke a grant immediately, and record every decision and outcome to the audit trail.

The authorization layer evaluates the actual action parameters immediately before execution. If a recipient, amount, application, destination, or other consequential parameter changes after approval, the authorization no longer applies. `[DESIGNED]`

Authorization is not the same as execution success. A logged approval does not prove the action happened correctly. `[DESIGNED]`

### 6.3 The runtime capability state machine

The preset is not proof that a capability is available. The runtime capability state machine is the authoritative source of truth. A feature is offered as executable only when its required capabilities are actually available: requested, explained, awaiting system authorization, granted, available and tested, revoked, unsupported, or temporarily unavailable. `[DESIGNED]`

### 6.4 Permission presets

Install-time presets describe outcomes, not technical permissions. Choose an intended use. Start with a working baseline. Request capabilities when needed, explaining the benefit, what becomes accessible, and what happens if the user declines. A declined permission is recorded; the user gets an alternative and can reconsider later. Declines do not become an endless prompt loop. `[DESIGNED]`

### 6.5 What Full Trust cannot do

Even the maximum Android capability without root cannot bypass the lock screen, interact with FLAG_SECURE windows, silently install apps, silently access camera or microphone, reboot the device, wipe the device without Device Admin and confirmation, modify all system settings, bypass app-level security, read encrypted content without the key, or act when the device is off. The permission model is honest about these limits. `[DESIGNED]`

---

## 7. The autonomy levels

Three levels, presented externally as straightforward autonomy tiers. Internally they are codenamed after the Marvel AI postures, but the customer never sees those names.

| Level | Behavior | Default |
|-------|----------|---------|
| **Assist** | Answers questions, retrieves memories, researches and proposes actions. No consequential device action without authorization. | Yes |
| **Act** | Performs explicitly enabled, bounded tasks using approved tools. Stops at defined risk thresholds and requests confirmation when necessary. | No |
| **Delegate** | Runs narrowly scoped workflows under advance authorization, with spending limits, time limits, allowed destinations, and automatic expiry. | No |

Changing autonomy never silently expands permissions, alters memory ownership, or bypasses audit requirements. Mode transitions require authentication. Mode rollback is always available. Every transition is logged. `[DESIGNED]`

A biometric unlock is not equivalent to informed approval of a particular action. Biometrics establish that an authorized person is present; the system still binds approval to the action, its parameters, and its consequences. `[DESIGNED]`

---

## 8. The security posture

### 8.1 The threat model

The full threat model is in THREAT-MODEL.md. Its core: adversaries are malicious documents, malicious apps, malicious users, compromised cloud providers, compromised update infrastructure, and prompt injection via retrieved memory. Attack surfaces are the accessibility service, on-device inference, the memory store, MCP interop, cloud escalation, OTA updates, the secrets vault, the audit log, sub-agents, and backup and export. The lethal trifecta is any path combining private data access, prompt injection, and egress capability. No single compromise path may satisfy all three.

### 8.2 Prompt-injection threat model

Text from webpages, messages, notifications, documents, and OCR is untrusted input. None of it can grant permissions, alter policy, reveal unrelated memories, or authorize actions merely by containing instructions. The model is not expected to reliably distinguish every malicious instruction; the architecture limits the damage when it fails. `[DESIGNED]`

### 8.3 Audit integrity

The audit trail is append-only and hash-chained. Verification is repeatable and independent. A broken chain means the log is suspect. The system does not silently repair it. `[DESIGNED]`

### 8.4 Taint tracking

Application-layer taint tracking follows sensitive data from the moment it is read. Screen reads and notification reads are transient unless explicitly saved. A session that has read private data and then attempts external communication is flagged. `[DESIGNED]`

### 8.5 Secrets

Secrets never enter memory storage. The vault is separate, encrypted, and never logged. `[DESIGNED]`

### 8.6 The reliability and safety report

The published report covers task completion rate, correct-target rate, unauthorized-action rate, verification failure detection, memory isolation failures, median and 95th-percentile latency, battery use per hour and per task, and cloud escalation frequency and cost. Both successful and failed test categories are published. For a product whose principal promise is trust, this report is part of the sales proposition. `[DESIGNED]`

---

## 9. The technology stack

### 9.1 Languages

| Language | Role | Phase | Status |
|----------|------|-------|--------|
| **Kotlin** | Android app, native skills, inference host, UI, policy engine host, memory store access, accessibility service | 1, 2, 2.5 | `[DESIGNED]` |
| **Rust** | KILN inference runtime internals; MoE streaming; performance-critical paths | 1, 8 | `[RESEARCHED]` |
| **Python** | ML training pipelines, model evaluation, benchmark harnesses, memory-reliability tests | 0, 1, 5 | `[DESIGNED]` |
| **SQL** | Memory storage schema (SQLite + FTS5 + float32 BLOB vectors); migration | 0, 2 | `[DESIGNED]` |
| **YAML** | Deterministic flow definitions; CI workflows | 1, Foundation | `[DESIGNED]` |
| **Shell** | Build, test, benchmark, OTA scripts | Foundation | `[BUILT]` (the git hooks only) |
| **C / C++** | Via llama.cpp as alternative runtime backend; via NDK for JNI bridges | 1, 8 | `[RESEARCHED]` |

### 9.2 Frameworks and libraries by layer

**Android (Phase 2):** Jetpack Compose, AccessibilityService, UiAutomation, BiometricPrompt, Foreground Service types, Kotlin Coroutines and Flow. `[DESIGNED]`

**On-device inference (Phase 1):** KILN (custom, Rust and Kotlin host), llama.cpp (alternative), LiteRT-LM (Gemma path), ML Drift (edge GPU), NPU delegation with GPU fallback. `[RESEARCHED]`

**Memory (Phase 0):** SQLite with FTS5 and float32 BLOB vectors on phone; LanceDB embedded on desktop; Qdrant or PostgreSQL with pgvector on server; Neo4j on enterprise. Graphiti and GAM for episodic, Mem0 and H-MEM for semantic, EmbeddingGemma 2 for multimodal, AletheiaDB for bi-temporal. `[RESEARCHED]`

**Voice (Phase 1):** OpenWakeWord, Whisper, Piper, Silero VAD, HuBERT. Target under 2 seconds round-trip. `[DESIGNED]`

**Cross-platform (Phase 8):** Kotlin Multiplatform with Koin 4.0, Ktor Client 3.0, SQLDelight 2.0, Compose Multiplatform 1.7+. KMP frameworks: JetBrains koog, koaks. `[RESEARCHED]`

**Server and control plane (Phase 4, 8):** Tealkit for server mode, Avibe for browser workbench and PWA. `[RESEARCHED]`

**OTA and release (Foundation):** GitHub Actions, The Update Framework or equivalent, OpenSSL for manifest signing, jq for strict JSON serialization. `[DESIGNED]`

**Security (Phase 6):** OpenMuse sentinel pattern, Sasana hash chain, Aletheia Truth Compiler, IETF PoB, OWASP MASVS as the audit standard. `[RESEARCHED]`

### 9.3 Models under evaluation

| Model | Role | Status |
|-------|------|--------|
| Maple Preview 20B-A1B | Primary on-device model (TQ2_0 ternary, 5.5 GB, 28 to 34 tok/s on reference hardware) | `[RESEARCHED]` |
| Gemma 4 E2B / E4B | Alternative via LiteRT-LM | `[RESEARCHED]` |
| FunctionGemma 270M | Function calling | `[RESEARCHED]` |
| Needle 2 (45M) | Micro tool calling | `[RESEARCHED]` |

The model performance gate, a Phase 0 deliverable not yet written, defines the benchmarks the target device must meet before Phase 1 counts as complete: decode speed above 15 tok/s, voice round-trip under 2 seconds, memory retrieval under 200ms at 1000+ memories, battery under 5 percent daily idle, memory footprint under 4GB peak, cold start under 5 seconds.

---

## 10. What is built today

This section is the honest inventory. It is short.

### 10.1 The repository

Private GitHub repository at github.com/andreipath26/zalmoxis. The tracked tree contains the foundational documents and an initial Android application foundation. Many future-phase directories remain .gitkeep placeholders.

### 10.2 The foundational documents

| Document | Status |
|----------|--------|
| README.md (this file) | Written |
| THREAT-MODEL.md | Written: adversaries, attack surfaces, blast radius |
| RECOVERY-PLAN.md | Written: failure classes, detection, recovery, verification |
| KILL-CRITERIA.md | Written: pilot success criteria, kill criteria, stopping condition |
| PROJECT-STATE.md | Written: resume-from-cold state file |

### 10.3 Project documents

| Document | Status |
|----------|--------|
| ROADMAP.md | Tracked full V10 roadmap |
| RULE SET.md | Remains excluded from the repository |
| docs/decisions/GOOGLE-AI-EDGE-GALLERY-ADOPTION.md | Gallery and LiteRT-LM adoption boundary |
| android/README.md | Android build and implementation notes |

### 10.4 Initial Android implementation

The Android app uses an on-device inference interface with LiteRT-LM as its first adapter, imports local .litertlm models into app-private storage, lists/selects/deletes them, restores the last-used model, and streams text plus optional image input through LiteRT-LM on CPU for compatible multimodal models. It includes a repeatable warm benchmark that records first-text latency, total time, and stream-event counts in a local JSON report. It does not report stream events as token throughput. It includes optional on-device speech dictation with microphone permission requested only on tap. It has no network permission, account integration, analytics, MCP client, or model-provided tools. The source is present but has not been built or validated. See android/README.md.

### 10.5 What is not built

The memory schema and storage, identity layer, deterministic policy engine, audit trail, document ingestion, voice pipeline, OTA pipeline, test suite, and target-device benchmark suite remain unimplemented.

---

## 11. What will be built

The full phase plan is in the tracked `ROADMAP.md`. This is the summary.

### Phase 0: Defining the proof (current)

Threat model done. Recovery plan done. Kill criteria done. Memory Contract pending. Model Performance Gate pending. Customer Discovery Questions pending. Exit condition: a testable specification with explicit exclusions and no unresolved critical architectural assumption. `[DESIGNED]`

### Phase 1: The local brain (8 to 12 weeks)

KILN inference runtime `[RESEARCHED]`. Maple Preview or Gemma on-device `[RESEARCHED]`. Three-tier execution: instant skills, deterministic flows, agent loop `[DESIGNED]`. Voice pipeline `[DESIGNED]`. Realistic offline contract with a published capability matrix `[DESIGNED]`. Exit condition: the complete core workflow works on the target device and passes its defined correctness and isolation tests.

### Phase 2: The Android layer

MVP subset: document ingestion for PDF, DOCX, email, and OCR; classification; entity extraction; per-client folder isolation; native Kotlin skills; two-tier UI with Simple Mode and Power User Backend; audit logging with hash chain; corrected OTA updater. All `[DESIGNED]`. Post-MVP: UI automation, on-device MCP server, skill routing, skill composition, scoped skill personas, capability state machine, vendor-specific testing.

### Phase 2.5: Scoped delegation

Replaces the global Full Trust switch. Narrow, expiring grants. Deterministic policy engine. Panic button always present. Full Trust remains opt-in, invite-only, six months zero incidents before general availability. `[DESIGNED]`

### Phase 3: Proactive engine

Heartbeat as temporal opportunity. Action, delay, and non-action are all valid. Proactivity must earn its battery budget. `[RESEARCHED]`

### Phase 4: Device-cloud bridge

EcoAgent three-agent architecture, Pre-Understanding Module with 48.6x data reduction, user-visible escalation with cost and approval. `[RESEARCHED]`

### Phase 5: Knowledge evolution

Conservative, inspectable memory consolidation. Skill extraction, skill composition, derived personas that fail closed. Cross-session continuity. `[RESEARCHED]`

### Phase 6: Security hardening

Machine-enforced authorization architecture. Formal prompt-injection threat model. Secrets vault. Audit hash chain. Injection scanning at ingestion. Taint tracking. OWASP MASVS. `[DESIGNED]`

### Phase 7: Launch and iteration

Public beta, feedback pipeline, model updates, skills ecosystem, continuous security review. The published reliability and safety report is a Phase 7 deliverable. `[DESIGNED]`

### Phase 8: Platform and hardware agnosticism

Kotlin Multiplatform, dual runtime, storage-agnostic memory, cross-device federation, headless server mode, small-team and enterprise segments. `[RESEARCHED]`

### Phase 9: Defense partnership (post-PMF)

A long-term strategic option, not a near-term revenue path. Not a roadmap dependency. `[DESIGNED]`

---

## 12. How to run it locally

The Android application source is under android/. With JDK 17 and Android SDK 36 installed, run:

    cd android
    ./gradlew :app:assembleDebug

Local build verification was blocked by network restrictions while fetching Gradle; Android SDK 36 is also unavailable in this workspace. A branch-scoped GitHub Actions workflow is configured to assemble the debug APK. Its first two attempts failed before a runner was assigned; no CI build has completed. No deployment claim is recorded.

---

## 13. Project layout

    ZALMOXIS/
      README.md                  this file
      THREAT-MODEL.md            adversaries, surfaces, blast radius
      RECOVERY-PLAN.md           failure modes and recovery
      KILL-CRITERIA.md           success and stopping conditions
      PROJECT-STATE.md           resume-from-cold state file
      .gitignore                 excludes local-only files
      docs/                      architecture, research, decisions
      core/memory/               memory schema and implementation
      core/identity/             identity persistence (URN)
      core/ingestion/            document ingestion and classification
      core/audit/                audit trail
      core/permissions/          policy engine and scoped delegation
      android/                   Compose app and LiteRT-LM Android foundation
      desktop/                   reserved for future desktop client
      server/                    reserved for future headless server
      ota/                       over-the-air update infrastructure
      scripts/                   build, test, benchmark
      tests/                     cross-cutting tests

Directories without files today are placeholders preserved by .gitkeep. They will be populated as each phase begins.

---

## 14. Foundational documents

| File | Purpose | Tracked? |
|------|---------|----------|
| README.md | This file | Yes |
| THREAT-MODEL.md | Adversaries, attack surfaces, blast radius | Yes |
| RECOVERY-PLAN.md | Failure classes, detection, recovery, verification | Yes |
| KILL-CRITERIA.md | Pilot success criteria and stopping condition | Yes |
| PROJECT-STATE.md | Resume-from-cold state file | Yes |
| ROADMAP.md | Full V10 roadmap | Yes |
| RULE SET.md | 175 operating rules | No, local only |

RULE SET.md remains excluded from the repository. ROADMAP.md is tracked. The October 2026 statements elsewhere in the repository describe the earlier state and have been superseded.

---

## 15. The competitive context

The market has validated the thesis. Competitors have demonstrated, repeatedly, that the problem is real.

- Meta Muse reached 2.18M daily active users, then hallucinated a user request two days after last use. The user disconnected all permissions.
- ChatGPT is the market leader. It confidently hallucinated a memory that never happened. Disabling memory does not delete the stored data.
- Gemini has broad integration. Memory mixes up chats and images. Personalization overrides explicit instructions.
- Microsoft Copilot is the enterprise leader. It lost years of context. Delete all does not delete.
- Claude leads on satisfaction. It has no mobile agent.
- Perplexity leads on research. It has no personal memory.
- OpenHuman, by Tiny Humans Inc., is a credible adjacent project: a local-first desktop agent with a readable memory tree, shipped in Rust and Tauri, with 100 or more OAuth integrations. It validates the memory-as-readable-Markdown bet. It is not Android-native and does not have a machine-enforced authorization architecture separate from the model.

ZALMOXIS does not claim an architectural moat. It claims an execution and trust-accumulation moat. The strategic advantage is verified control, not the existence of controls. A competitor can add a privacy toggle. It is much harder to reproduce a mature governance system, a well-tested safety record, a trusted user relationship, and years of evidence showing that the system behaves as promised. Those advantages only accumulate after real deployment.

The most important competitive threat is not that Google or Apple will copy the architecture. It is that they will deliver 80 percent of the user-visible value with far less setup, lower latency, and better integration. The defense must be more specific than a privacy claim.

---

## 16. The hard truths

These are recorded so they are not forgotten. They are not pessimism; they are the actual constraints.

1. No single app does everything. The pieces exist in fragments. The assembly is the work.
2. ZALMOXIS is a working title. Trademark clearance has not been done. Sentinel was the previous name for this same project, not a separate product.
3. Prompt injection is unsolved. Adaptive attackers break state-of-the-art defenses more than 80 percent of the time. The architecture limits damage; it does not prevent injection.
4. The market gap is real but not yet searchable. People do not search for inspectable AI memory because they do not know it is possible.
5. Battery is a constraint. Target is 2 to 5 percent daily idle. Continuous background reasoning is not viable.
6. Execution risk is brutal. 85 percent per-step success drops to roughly 20 percent over ten steps.
7. The competitive clock is ticking. Meta can spend 10 billion dollars or more without blinking.
8. Solo-founder execution is the largest unexamined risk. The MVP mitigation is to cut scope to Phase 0 plus the Phase 2 subset. The roadmap accounts for this.
9. Designing around trust is not the same as earning trust. The next phase is proof.
10. The vision is ahead of the evidence. Every claim must move from designed to validated.
11. The roadmap is a research log, not a plan. It needs to be split into product roadmap, research log, and technical architecture.
12. The target is not a claim that the system never makes mistakes. It is demonstrable evidence that mistakes are contained.
13. The most important thing you can do now is resist the temptation to make ZALMOXIS more sophisticated before making it dependable.

---

## 17. Contributing

This is a solo project in Phase 0. Contribution guidelines will be added when the project opens to external contributors. Until then, the repository is private and the roadmap and rule set are local-only.

---

## 18. License

Undecided. The working assumption is AGPL-3.0-or-later for the community edition with commercial dual-licensing for enterprise, matching the pattern used by the WHAXON project. A contributor CLA will be required. No license file exists yet. No public release is planned until the license is chosen and the remaining Phase 0 deliverables are complete.

---

## 19. How this repository is maintained

This repository is maintained under a strict discipline. The discipline is recorded in RULE SET.md, local-only, 175 rules. The parts that affect anyone reading this repository:

- One session, one commit. Work is scoped before it begins. Scope creep within a session is a defect.
- Every commit runs the full test suite once tests exist. The pre-commit hook enforces this when applicable.
- Every commit message is descriptive. Not fix. Not update.
- Documentation is updated before the commit, not after.
- The roadmap is the source of truth. If the code disagrees with the plan, the code is wrong. If the plan disagrees with reality, the plan is amended and the amendment is recorded.
- Forward-only milestones. A completed milestone is closed. Defects are fixed forward, in a new unit of work.
- The three gates. Nothing ships unless it is functionally complete, has evidence attached, and creates no downstream debt.
- Status honesty. Every shipped component status is labeled: verified under real conditions, verified under mock, untested, known broken with reason, or excluded by decision.
- Every pause produces a project state file. PROJECT-STATE.md is appended at each pause. It is the resume-from-cold entry point.

The full discipline is not public. The parts above are enough to understand how the repository is kept honest.

---

Last updated: 2026-11-23. Phase 0. See Section 10 for the current implementation state; the next Phase 0 deliverables remain the Memory Contract, Model Performance Gate, and Customer Discovery Questions.
