## 11. What will be built

The full phase plan is in the roadmap (local-only). This is the summary.

### Phase 0 — Defining the proof (current)

- Threat model — **done**
- Recovery plan — **done**
- Kill criteria — **done**
- Memory Contract — **pending**
- Model Performance Gate — **pending**
- Customer Discovery Questions — **pending**

**Exit condition:** a testable specification with explicit exclusions and no unresolved critical architectural assumption. `[DESIGNED]`

### Phase 1 — The local brain (8–12 weeks)

- KILN inference runtime `[RESEARCHED]`
- Maple Preview or Gemma on-device `[RESEARCHED]`
- Three-tier execution (instant skills, deterministic flows, agent loop) `[DESIGNED]`
- Voice pipeline `[DESIGNED]`
- Realistic offline contract — published capability matrix `[DESIGNED]`

**Exit condition:** the complete core workflow works on the target device and passes its defined correctness and isolation tests.

### Phase 2 — The Android layer

**MVP subset:**

- Document ingestion (PDF, DOCX, email, OCR) `[DESIGNED]`
- Classification, entity extraction `[DESIGNED]`
- Per-client folder isolation `[DESIGNED]`
- Native Kotlin skills `[DESIGNED]`
- Two-tier UI (Simple Mode + Power User Backend) `[DESIGNED]`
- Audit logging with hash chain `[DESIGNED]`
- Corrected OTA updater `[DESIGNED]`

**Post-MVP:** UI automation, on-device MCP server, skill routing, skill composition, scoped skill personas, capability state machine, vendor-specific testing.

### Phase 2.5 — Scoped delegation

Replaces the global Full Trust switch. Narrow, expiring grants. Deterministic policy engine. Panic button always present. Full Trust remains opt-in, invite-only, six months zero incidents before general availability. `[DESIGNED]`

### Phase 3 — Proactive engine

Heartbeat as temporal opportunity; action, delay, and non-action are all valid. Proactivity must earn its battery budget. `[RESEARCHED]`

### Phase 4 — Device-cloud bridge

EcoAgent three-agent architecture, Pre-Understanding Module (48.6× data reduction), user-visible escalation with cost and approval. `[RESEARCHED]`

### Phase 5 — Knowledge evolution

Conservative, inspectable memory consolidation. Skill extraction, skill composition, derived personas (fail-closed). Cross-session continuity. `[RESEARCHED]`

### Phase 6 — Security hardening

Machine-enforced authorization architecture. Formal prompt-injection threat model. Secrets vault. Audit hash chain. Injection scanning at ingestion. Taint tracking. OWASP MASVS. `[DESIGNED]`

### Phase 7 — Launch and iteration

Public beta, feedback pipeline, model updates, skills ecosystem, continuous security review. The published reliability and safety report is a Phase 7 deliverable. `[DESIGNED]`

### Phase 8 — Platform and hardware agnosticism

Kotlin Multiplatform, dual runtime, storage-agnostic memory, cross-device federation, headless server mode, small-team and enterprise segments. `[RESEARCHED]`

### Phase 9 — Defense partnership (post-PMF)

A long-term strategic option, not a near-term revenue path. Not a roadmap dependency. `[DESIGNED]`
