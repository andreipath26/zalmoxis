# ZALMOXIS: The Complete Roadmap (V10 — Refined by Full Independent Assessment)

Sentinel was the former name for ZALMOXIS; it is not a separate product. “Sentinel Watch” remains the name of the Phase 3 component.

Everything from V9. Every finding from the assessment integrated. Nothing removed.

## Part One: The Product Thesis

### What Zalmoxis Is

Zalmoxis is an Android-native AI agent that remembers, reasons, and acts locally, while making its permissions, decisions, data access, and failures inspectable by the person who owns it.

This is the thesis. It is not the personalities. It is not the emotional memory. It is not the phone control. The thesis is delegation without loss of control.

### What the Assessment Sharpened

The market has validated the thesis. But the market has not validated Zalmoxis's particular product. The failures of competitors establish that the problems are meaningful. They do not establish that customers will adopt Zalmoxis, trust its implementation, or pay the proposed price.

Three separate hypotheses must be tested independently:

- Hypothesis A: The problem exists — People want persistent, reliable AI memory and controlled automation
- Hypothesis B: Zalmoxis solves it better — Local memory, explicit governance and verifiable actions produce a measurably better user experience
- Hypothesis C: Customers will pay enough — The improvement is valuable enough to support the proposed subscription and the cost of developing and maintaining the product

### Evidence Tags

| Tag | Meaning |
| --- | --- |
| [DESIGNED] | Architecture specified. No implementation. |
| [RESEARCHED] | Grounded in published research or production-tested reference. |
| [BUILT] | Code exists. Tests pass. |
| [VALIDATED] | Tested with real users, real data, real outcomes. |
| [PROVEN] | Demonstrated over time with zero trust-breaking incidents. |

As of this writing, almost everything is [DESIGNED] or [RESEARCHED].

## Part Two: What Was Refined In V10

The assessment identified specific findings that required roadmap changes. These are integrated below:

| Finding | Integration |
| --- | --- |
| MVP scope is overloaded | MVP cut to minimum viable proof |
| Six-week timeline aggressive | Extended to 8–12 weeks |
| OTA pipeline has five concrete defects | Corrected in Foundation |
| Full Trust should be scoped delegation | Replaced global switch in Phase 2.5 |
| Commercial evidence insufficient | Customer discovery added before MVP |
| Machine-enforced authorization needed | Added to Phase 6 |
| Memory contract needed | Added to Phase 0 |
| Reliability and safety report needed | Added to Phase 7 |
| Prompt-injection threat model needed | Added to Phase 6 |
| Realistic offline contract needed | Added to Phase 1 |
| Cross-session continuity gap | Addressed in Phase 0 and Phase 5 |

## Part Three: The Next 90 Days (Proof Phase — Refined)

Days 1–14: Define the Proof (Gate 1)

Goal: A testable specification with explicit exclusions and no unresolved critical architectural assumption.

Deliverables:

#### Threat Model [1 week]

One page. Adversaries: malicious apps, malicious documents, malicious users, compromised cloud providers, compromised update infrastructure. Attack surfaces: accessibility service, MCP interop, cloud escalation, OTA updates, memory storage, secrets vault. Blast radius: per-user memory, per-client data, device control, financial data.

Written, published, inviting critique.

#### Recovery Plan [1 week]

| Failure Mode | Detection | Recovery |
| --- | --- | --- |
| Memory corruption | Coherence engine | Restore from backup, reconcile |
| Permission leak | Audit log anomaly | Revoke, re-grant, audit, notify user |
| Full Trust incident | Panic button, anomaly detection | Stop, downgrade, incident report |
| OTA bad release | Crash flag | Rollback, halt staged rollout, notify |
| Cloud compromise | Provider notification | Credential rotation, key revocation |
| Sub-agent divergence | Coherence engine | Auto-pause, notify, kill option |

#### Memory Contract

The user must be able to answer:

- What does Zalmoxis believe about me?
- Where did each fact come from?
- When was it last confirmed?
- How certain is it?
- Which workflows can access it?
- What has been marked historical?
- What remains after I delete it?
- Can I export everything and use it without Zalmoxis's servers?

#### Pilot Success and Kill Criteria

| Criterion | Threshold | Action |
| --- | --- | --- |
| Daily active use | <40% after week 4 | Stop. Reassess. |
| Client data leakage | Any | Stop. Rewrite scoping. |
| Permission comprehension | <60% | Redesign flow. |
| Trust-breaking incident | Any | Pause. Audit. |
| Cloud cost per user | >$5/month without value | Reprice or cut. |
| User "adds work" | >30% | Reassess. |

#### Model Performance Gate

Before Phase 1 is considered complete, benchmark candidate models on the target device.

| Metric | Target |
| --- | --- |
| Decode speed | >15 tok/s |
| Voice round-trip | <2s end-to-end |
| Memory retrieval | <200ms at 1000+ memories |
| Battery | <5% daily idle |
| Memory footprint | <4GB peak |
| Cold start | <5s |

If the target phone cannot meet these, fallback options apply.

#### Customer Discovery

Before the MVP is built, conduct 5–10 customer discovery interviews with the target profile (busy office workers managing multiple clients).

Exit condition: A testable specification with explicit exclusions and no unresolved critical architectural assumption. Days 15–56: Build the Core (Gate 2 — REFINED to 8–12 weeks total)

Goal: The complete core workflow works on the target device and passes its defined correctness and isolation tests.

#### MVP Scope (Reduced)

| Component | Included |
| --- | --- |
| Local document ingestion | PDF, DOCX, email, images with OCR |
| Document classification | Contract, invoice, form, correspondence |
| Entity extraction | Parties, dates, amounts, obligations, deadlines |
| Local conversation | On-device model |
| Persistent memory | With provenance, inspection, correction, deletion |
| Identity isolation | Per-user, per-client scoping enforced at storage layer |
| Basic Android interface | Task dashboard, not just chat |
| Basic audit trail | Every action logged |
| Permission presets | Install-time with progressive disclosure |
| Memory reliability tests | Image/channel bleeding, personalization override |
| Hard-delete verification | Deletion that actually deletes |
| Model performance gate | Run on target device |

Deferred from MVP:

- Emotional retrieval
- Dream Cycle and self-improvement
- Cross-device federation
- Multiple posture branding and derived agents
- Defense-specific features
- Full OTA rollout infrastructure
- Elaborate power-user tools

Exit condition: The complete core workflow works on the target device and passes its defined correctness and isolation tests. Days 57–74: Validate with the First Customer (Gate 3)

Goal: Evidence that the product solves a real problem reliably enough to justify continued development.

Deliverables:

- Use representative real documents under an agreed data-handling arrangement
- Measure task success, time saved, answer accuracy and user effort
- Record failures and investigate them rather than hiding them behind aggregate scores
- Test whether the customer would pay for continued use

Exit condition: Evidence that the product solves a real problem reliably enough to justify continued development. Days 75–90: Make the Investment Decision (Gate 4)

Goal: A documented go, pivot or stop decision based on predetermined criteria.

Deliverables:

- Fix the highest-impact reliability issues
- Run regression tests and security evaluations
- Complete the pilot's initial evaluation and agree on continuation
- Decide whether to expand into voice, additional tools or proactive features
- Publish an honest evidence report covering what works and what remains unproven

Exit condition: A documented go, pivot or stop decision based on predetermined criteria.

## Part Four: The Full Roadmap

### Foundation

#### The Three Postures

Internally: JARVIS, FRIDAY, EDITH.

Externally, presented as three straightforward autonomy levels:

| Level | Behavior | Default |
| --- | --- | --- |
| Assist | Answers questions, retrieves memories, researches and proposes actions. No consequential device action without authorization. | Yes |
| Act | Performs explicitly enabled, bounded tasks using approved tools. Stops at defined risk thresholds and requests confirmation when necessary. | No |
| Delegate | Runs narrowly scoped workflows under advance authorization, with spending limits, time limits, allowed destinations and automatic expiry. | No |

Key design principle: Changing autonomy must never silently expand permissions, alter memory ownership or bypass audit requirements.

Critical distinction: A biometric unlock is not equivalent to informed approval of a particular action. Biometrics can establish that an authorized person is present; the system must still bind approval to the action, its parameters and its consequences.

#### The Three Market Segments (Hypotheses, Not Commitments)

| Segment | Posture | Price | Deployment |
| --- | --- | --- | --- |
| Individual | Assist + Act | $10-15/mo (test before $15-20) | Phone |
| Small Team | Assist + Act + scoped personas | $25-40/seat/mo | Phone + headless server |
| Regulated Enterprise | Full stack | $100-500/seat/mo | On-premise or air-gapped |

For individuals: Test a free or limited local tier against a paid tier around $10–15/month before committing to $15–20. A higher price may work for power users, but only if Zalmoxis demonstrably replaces existing tools or saves meaningful time.

For teams: Charge for more than access to the Android app: administrative controls, policy management, deployment support, device governance and integration with approved systems.

For enterprise: Price the deployment and support obligations separately where appropriate. An air-gapped installation can involve substantial engineering, updates, compliance evidence and support costs.

#### Team Risk

The roadmap assumes Android depth, Rust runtime work, ML training pipelines, and defense navigation. One person cannot do all of this at once.

MVP mitigation: Cut to Phase 0 + Phase 2 subset. The 8–12 week timeline accounts for solo-founder reality.

Long-term: Hire or partner for ML training, Rust runtime, Android accessibility depth. Do not pretend a solo founder can do it all.

#### Licensing

AGPL-3.0-or-later for community. Commercial dual-license for enterprise. Contributor CLA. Same pattern as WHAXON. Decided before first public commit.

#### The Ota Update Pipeline (Foundation-Level — Corrected)

#### The Five Defects Fixed

The assessment identified five concrete issues in the V9 OTA example. These are corrected:

Defect 1: Manifest signing key handling inconsistent.

- Fixed: The workflow now creates the signing key file from the environment variable before invoking OpenSSL.

```yaml
```
- name: Write manifest signing key
run: |
- echo "${{ secrets.MANIFEST_SIGNING_KEY }}" > manifest-signing-key.pem
- chmod 600 manifest-signing-key.pem

Defect 2: Release secrets exposed to build workspace.

- Fixed: Release signing keys are kept outside ordinary CI jobs. Use protected signing infrastructure with short-lived credentials. The keystore is decoded in an isolated job, not the main build workspace.

Defect 3: Version and rollback policy incomplete.

- Fixed: Version ordering is monotonic and enforced. Anti-rollback protection rejects unauthorized downgrade. A defined key-compromise recovery procedure is documented.

Defect 4: Manifest generator needs strict serialization.

- Fixed: Use a proper JSON serializer, not string concatenation. Validate the generated manifest before signing.

```bash
```
jq -n \ --arg version "$VERSION" \ --arg apk_url "$APK_URL" \ --arg apk_hash "$APK_HASH" \ --arg release_notes "$RELEASE_NOTES" \ '{version: $version, apk_url: $apk_url, apk_hash: $apk_hash, release_notes: $release_notes}' \ > update-manifest.json

Defect 5: Updater needs defined Android installation and recovery model.

- Fixed: The updater handles package-signing continuity, user authorization for package installation, interrupted downloads, storage failures and installation failures. A documented recovery procedure exists.

#### Recommended Architecture: The Update Framework (TUF)

Adopt TUF or an equivalent design. TUF addresses threats beyond a simple hash check, including compromised signing keys, stale updates and rollback attacks through separated signing roles and expiring metadata.

#### Sovereignty Levels

| Level | Meaning | MVP |
| --- | --- | --- |
| User data sovereignty | Private memory stays under user control | Yes |
| Operational sovereignty | Users can operate the product without your servers | Later |
| Software supply-chain sovereignty | Users can independently verify what they install | Yes |
| Deployment sovereignty | Organizations can build and distribute the software themselves | Enterprise |

#### Android-Specific Qualification

The update mechanism depends on the distribution channel and platform rules. A self-managed APK updater must respect Android's package-installation controls; an application distributed through Google Play should use the supported Play update mechanism.

#### The Permission Architecture (Foundation-Level — Refined)

#### Presets Describe Outcomes, Not Technical Permissions

Three-stage design:

- Choose an intended use. Ask what the user wants Zalmoxis to accomplish, not which Android permissions they want to grant.
- Start with a working baseline. The app provides immediate value using the least privileged configuration possible.
- Request capabilities when needed. Explain the specific benefit, what information becomes accessible, what Zalmoxis can do, and what happens if the user declines.

#### The Runtime Capability State Machine

The preset must not be treated as proof that the required capability is available. The runtime capability state machine is the authoritative source of truth:
```text
```
Requested → Explained → Awaiting system authorization → Granted → Available and tested → Revoked → Unsupported → Temporarily unavailable

A feature should only be offered as executable when its required capabilities are actually available.

#### Declined Permissions

A declined permission must not become an endless source of prompts. Record the decline, provide a useful alternative and let the user reconsider it later.

#### Anti-Patterns (Runtime Guards)

| Anti-Pattern | Constraint | Where |
| --- | --- | --- |
| Ultron (unbounded capability) | Every irreversible action gated | Phase 6 |
| Phalanx (unbounded federation) | Interop exchanges answers, not state | Foundation |
| Supreme Intelligence (aggregated consciousness) | Memory per-user, per-identity | Phase 0 |
| Nimrod / Bastion (adversarial evolution) | Generation validation | Phase 0, 8 |
| Victor Mancha (sub-agent divergence) | Sub-agents fail-closed | Phase 6 |
| Silent Operation | Every action audited | Phase 6 |

Each requires a test case demonstrating it blocks the pattern.

### Phase 0: Memory core (weeks 1–3) — mvp scope

#### The Five Layers

Episodic (Graphiti + GAM): Temporal knowledge graph. Validity windows. Provenance. Event progression graph + topic associative network.

Semantic (Mem0 + H-MEM): Multi-level. Trust scores. Review-gated promotion. Positional index encoding.

Identity Profile (jñāpakaṁ): Permanent URN. Generation workflow. Rollback preserves learning.

Emotional Retrieval (CAMA): Deferred from MVP. Basic semantic retrieval first.

Memory Evolution (RGMem): Separate fast-changing evidence from slow-varying traits.

#### Active Memory

Interference-aware retrieval: conflicting memories surfaced with provenance. Contradiction handling: newer facts don't silently override. Update tracking: changes logged as episodes. Long-horizon retrieval.

#### Instruction Persistence Layer

Explicit constraints stored separately from passive preferences. Explicit constraints take precedence. Constraints persist across sessions.

#### Hard-Delete Verification

Deletion removes from all layers. Verification step confirms. Audit log records. User can request deletion certificate.

#### Memory Quarantine

Uncertain or conflicting facts are quarantined. A newly inferred preference should not silently overwrite an established fact. Material changes are surfaced for correction or confirmation.

#### Memory Contract

The user can answer:

- What does Zalmoxis believe about me?
- Where did each fact come from?
- When was it last confirmed?
- How certain is it?
- Which workflows can access it?
- What has been marked historical?
- What remains after I delete it?
- Can I export everything and use it without Zalmoxis's servers?

#### Memory Reliability Testing

Tests: image/channel bleeding, personalization override, explicit instruction retention, cross-session recall. Published results.

#### Storage Backend

| Context | Storage |
| --- | --- |
| Phone | SQLite + float32 BLOB vectors |
| Desktop | LanceDB embedded |
| Server/Home Hub | Qdrant or PostgreSQL+pgvector |
| Enterprise | Neo4j |

Retrieval target: <200ms at any history size.

#### Validation

LongMemEval-M plus reliability tests. Publish results.

Status: [DESIGNED] + [RESEARCHED]. Build in MVP. Publish validation.

### Phase 1: Local brain (weeks 4–7)

#### Inference Runtime: KILN

Per-layer dispatcher. Edge0 techniques absorbed.

#### Model Layer

| Model | Purpose |
| --- | --- |
| Maple Preview 20B-A1B | Primary (TQ2_0, 5.5 GB, 28–34 tok/s on i5-8350U) |
| Gemma 4 E2B/E4B | Alternative via LiteRT-LM |
| FunctionGemma 270M | Function calling |
| Needle 2 (45M) | Micro tool calling |

#### Three-Tier Execution

- Instant Skills (D.U.M.-E): zero LLM, sub-100ms
- Deterministic Flows: YAML
- Full Agent Loop: novel reasoning

#### Voice Layer

OpenWakeWord, Whisper, Piper, Silero VAD, HuBERT prosody. Target <2s.

#### Realistic Offline Contract

"Runs entirely on the device" is defined precisely. Which features work offline: voice recognition, speech synthesis, embeddings, memory retrieval, scheduled tasks, audit inspection. Which features stop without network. Whether telemetry is sent. Whether crash reporting contains sensitive content.

Publish a clear capability matrix and make the offline state observable.

#### Model Performance Gate

Run during MVP. If gate fails, fallback options apply.

Status: [DESIGNED] + [RESEARCHED]. Build after MVP validated.

### Phase 2: Android layer (weeks 8–13) — mvp subset

MVP Subset

Document ingestion (PDF, DOCX, email, OCR). Classification. Entity extraction. Indexing (SQLite FTS5 + vectors). Per-client folders. Native Kotlin skills (calendar, alarms, files, email drafts). Audit logging with hash chain. Approval gates. Permission presets. OTA updater (corrected).

#### The Two-Tier UI

Main UI (Simple Mode): Voice-first conversation. Task dashboard. Recent activity feed. Quick actions. Status indicators. One button: "Advanced." Notion-style restraint. Panic button (Full Trust).

Power User Backend: Memory inspection (editable, deletable, verify deletion). Permission dashboard. Audit log (exportable). Provider configuration. Skill management. Mode posture settings. Advanced query. Export facilities (memory, audit, deletion certificates). Integration settings (MCP, calendar, email).

#### Full Phase 2 (Post-MVP)

UI automation (accessibility tree). On-device MCP server. Skill routing (reverse-skill). Skill composition (Taku). Scoped skill personas (Karen). Active context sources. Capability state machine. Vendor-specific testing.

Status: [DESIGNED]. MVP subset built first.

### Phase 2.5: Full device capability (weeks 14–16 — scoped delegation)

Replace Global Full Trust with Scoped Delegation

The assessment's most consequential finding: Keep advanced device control as a differentiating capability, but ship it behind narrow, expiring grants. Earn broader delegation through demonstrated reliability. Do not make unrestricted Full Trust a prerequisite for experiencing the product's value.

Allowed without repeated approval:

- Read selected notifications
- Summarize approved folders
- Organize local notes
- Run previously approved workflows

Allowed only within explicit limits:

- Send messages to specified contacts
- Interact with approved applications
- Perform actions under a spending cap

Always require fresh authorization:

- Transfers of money
- Purchases beyond a limit
- Publishing sensitive content
- Granting permissions
- Exporting credentials
- Other high-impact actions

The exact categories should be risk-based, not simply based on whether an action is technically reversible.

#### The Deterministic Policy Engine

Do not let the language model be the final authority on whether its own action is permitted. Use a deterministic policy engine between the model and the device-control tools.

Interface:

- check(action, context) — determine whether an action is permitted
- authorize(grant, scope, expiry) — create a bounded authorization
- revoke(grant) — withdraw it immediately
- record(event) — append the decision and outcome to the audit trail

The authorization layer should evaluate the actual action parameters immediately before execution. If a recipient, amount, application, destination or other consequential parameter changes after approval, the authorization must no longer apply.

Distinguish authorization from execution success. A logged approval does not prove the action happened correctly.

A compromised prompt should not be able to rewrite the policy, widen its own permissions or approve its own execution.

#### What Full Trust Cannot Do

- Bypass lock screen
- Interact with FLAG_SECURE
- Silently install apps
- Silently access camera/microphone
- Reboot device
- Wipe device without Device Admin + confirmation
- Modify all system settings
- Bypass app-level security
- Read encrypted content without key
- Act when device is off

#### Pre-Ship Checklist

All ten items plus six months zero incidents before general availability. Opt-in, invite-only, with user agreement stating risks and recovery.

Status: [DESIGNED]. Not built. Not validated.

### Phase 3: Proactive engine (weeks 17–21)

Core [RESEARCHED/DESIGNED]

Default Mode Analog (dual-pulse). Project Sue heartbeat (action/delay/non-action). Sentinel Watch (background event-waiter). KnowU-Bench formalization. PROPER (knowledge gaps). ContextAgent (wearable sensory). Reference implementations (Proactive Sebastian, Sigrid, OpenMuse). Agent Reach. Preference-Based Discovery (Taku). Conditional Escalation (Veronica). Mode Posture Transitions. Relevance Filtering. Explain First, Act Second. Proactivity Must Earn Its Battery Budget

Keep the heartbeat only if it delivers clear user value. Start with scheduled, user-configured checks and notifications. Avoid continuous background reasoning that drains the battery.

Status: [DESIGNED] + [RESEARCHED]. Build after MVP validated.

### Phase 4: Device-cloud bridge (weeks 22–26)

Core [RESEARCHED/DESIGNED]

EcoAgent (Cloud Planning, On-Device Execution, On-Device Observation). Pre-Understanding Module (48.6x reduction, 3.9s latency vs 15.3s). Provider Fallback (Mercury). User-Visible Escalation. Token Budget (Mercury). Prompt Caching (0.1x/1.25x). Offline Fallback. Routing Log. Tealkit Server Mode. Outbound-Only Remote Access (Avibe). Channel-Agnostic Interface (Avibe).

Status: [DESIGNED] + [RESEARCHED]. Build after MVP validated.

### Phase 5: Knowledge evolution (weeks 27–31)

Core [RESEARCHED/DESIGNED]

Dream Cycle (Agent Brain). Conservative, inspectable memory consolidation first. Defer open-ended self-improvement. Knowledge Pages (Hindsight). Memory Tree (openhuman). Hermes Skill Extraction. Derived Personas (Viv Vision). Sub-Agent Divergence (Victor Mancha). Affective Continuity. Temporal Graph Reasoning. Inspectable Memory. FSRS-Weighted Retention. Generative UI Artifacts (Avibe). jñāpakaṁ Continuity. RGMem Trait Consolidation.

#### AI Memory reference: akitaonrails/ai-memory (reviewed 2026-11-23)

Repository reviewed: https://github.com/akitaonrails/ai-memory (MIT-licensed Rust workspace, release v3.5.1 at review). This is a reference implementation for coding-agent continuity, not an Android library or a direct Zamolxis dependency.

Its canonical knowledge store is an editable, Git-versioned Markdown wiki. SQLite is a derived, rebuildable index that supports FTS5, entity and link retrieval, sessions, observations, handoffs, audit records, and optional embeddings. Search can combine lexical, entity, graph-neighbor, and vector results. Its retention design decays cold episodic records while protecting pages that are revisited, and it preserves superseded history rather than silently overwriting prior knowledge.

The default workflow does not require an LLM provider. If an LLM provider is configured, provider-backed consolidation, lint, bootstrap, dream, profile, and auto-improvement paths can send session observations and page text to it. The scheduled auto-improvement loop applies validated proposals through the wiki write path by default; operators can stop the scheduler or require approval. The LLM dream pass is separately opt-in, off by default, and requires a configured provider and embedder. LLM reranking is a separate explicit `AI_MEMORY_RERANKER=llm` path. The local embedding path uses Candle with all-MiniLM-L6-v2 on CPU; it downloads an approximately 87 MB model from Hugging Face on first use and verifies its SHA-256. The local embedding path keeps text processing on-device, but the initial model fetch still requires network access.

**Patterns to evaluate for Zamolxis**

- Keep canonical memories user-inspectable and exportable in a simple format; compare Markdown-backed records with the Phase 0 structured storage contract before choosing a canonical source of truth.
- Treat search indexes and embeddings as derived, rebuildable data. Never let a stale index become the only copy of a memory.
- Combine local lexical search with entity, relationship, and temporal-neighbor retrieval. Add embeddings only after device benchmarks show acceptable retrieval quality, latency, storage, and battery use.
- Preserve provenance, timestamps, supersession history, correction, restore, and deletion receipts. Make retention decay visible and user-controlled; pin or protect information the user confirms as important.
- Keep the zero-LLM memory path useful. For Zamolxis, require explicit user opt-in to provider-backed memory processing, identify whether a selected provider is local or remote, and stage every proposed memory edit for approval. Do not inherit ai-memory's default auto-approval for validated proposals. Keep those flows inspectable, reversible, and unable to change permissions or memory ownership.
- Preserve source trust labels and quarantine untrusted imported content before it can influence durable memories.

**Adaptation boundary**

The repository’s Rust server and CLI, MCP integration, editor hooks, multi-agent handoff lifecycle, team console, and cloud-provider reranking target coding-agent workflows and do not transfer directly to the Android product. Its v1 storage also provides no encryption at rest and relies on operating-system file permissions; that security model must not be copied into Zamolxis without a separate threat-model decision. Reuse design lessons only after checking Android storage, app lifecycle, background-work, and offline constraints. The project is MIT licensed; if any code is later copied, preserve its license and notices and review each dependency’s separate license. No code or model is adopted by this roadmap entry.

Status: [RESEARCHED] reference; adaptation decisions remain [DESIGNED] until Phase 0/5 benchmarks and privacy review pass.

#### Cross-Session Continuity

The lifecycle model for how the agent's identity, skills and policy change over time while maintaining continuity. jñāpakaṁ generation workflow handles identity. Hermes handles skills. Memory evolution handles policy.

Status: [DESIGNED] + [RESEARCHED]. Build after MVP validated.

### Phase 6: Security hardening (weeks 32–35)

Core [RESEARCHED/DESIGNED]

OpenMuse OpenMuse Sentinel Pattern (risk levels, first-match-wins, taint). Secrets Vault. Audit Log (Sasana hash chain). Aletheia Truth Compiler (epistemic labels, 18 dimensions). Injection Scanning at Ingestion. Security Dashboard. Approval Gates (BiometricPrompt). OWASP MASVS. Android 14+ FGS Types. Mode Posture Security. Completeness Contract (Mercury). Sub-Agent Divergence Monitoring. Anti-Pattern Guards (test cases required). OTA Update Security (corrected). Permission Security.

#### Machine-Enforced Authorization Architecture

This is the most important addition. A separate subsystem the model cannot rewrite:

- check(action, context) — determine whether an action is permitted
- authorize(grant, scope, expiry) — create a bounded authorization
- revoke(grant) — withdraw it immediately
- record(event) — append the decision and outcome to the audit trail

The authorization layer evaluates the actual action parameters immediately before execution.

#### Formal Prompt-Injection Threat Model

Treat text from webpages, messages, notifications, documents, OCR and other external sources as untrusted input. None should be able to grant permissions, alter policy, reveal unrelated memories or authorize actions merely by containing instructions.

The model should not be expected to reliably distinguish every malicious instruction from legitimate content. The architecture must limit the damage when it fails.

Status: [DESIGNED] + [RESEARCHED]. Build after MVP validated.

### Phase 7: Launch & iteration (weeks 36+)

Public Beta. Feedback Pipeline. Model Updates. Skills Ecosystem. Continuous Security Review. Individual Segment Launch ($10-15/month test first). OTA Pipeline Live (staged rollout).

#### Published Reliability and Safety Report

Create a benchmark specifically for Zalmoxis's supported tasks:

| Metric | Why it matters |
| --- | --- |
| Task completion rate | Whether the product actually works |
| Correct-target rate | Whether it acts on the intended application, contact or object |
| Unauthorized-action rate | Whether the governance boundary holds |
| Verification failure detection | Whether mistakes are caught |
| Memory isolation failures | Whether identities remain separate |
| Median and 95th-percentile latency | Whether interaction feels responsive |
| Battery use per hour and per task | Whether daily use is sustainable |
| Cloud escalation frequency and cost | Whether privacy and pricing promises hold |

Test across a defined set of devices, Android versions, model configurations and network conditions. Publish both successful and failed test categories.

For a product whose principal promise is trust, this report is part of the sales proposition.

Status: [DESIGNED]. Depends on MVP pilot success.

### Phase 8: Platform & hardware agnosticism (weeks 37–47)

Core [RESEARCHED/DESIGNED]

Kotlin Multiplatform (Koin 4.0, Ktor Client 3.0, SQLDelight 2.0, Compose Multiplatform 1.7+). KILN + llama.cpp dual runtime. Storage-agnostic memory. jñāpakaṁ lineage. Home Assistant Assist voice. Cross-platform skills. KMP frameworks (JetBrains/koog, koaks). Tealkit Server Mode. Compact Deployment Personas (H.E.R.B.I.E.). Browser Workbench + PWA (Avibe). Outbound-only remote access. Channel-agnostic interface. Small Team Segment Launch ($25-40/seat/mo). Regulated Enterprise Segment Launch ($100-500/seat/mo). OTA Update Server (50+ clients).

#### Cross-Device Federation and Headless Server Mode

Both expand the security boundary, deployment burden and support requirements. They are not essential to prove the phone-first proposition. Defer until post-MVP.

Status: [DESIGNED] + [RESEARCHED]. Build after Phase 7 success.

### Phase 9: Defense partnership (post-pmf)

#### The Assessment's Verdict

Yes, as a longer-term partnership strategy. No, as a dependable near-term source of revenue.

#### The Catch

Funding eligibility and procurement access are not the same thing. Many European Defence Fund calls require a consortium spanning at least three eligible countries. A company without capital, established partners or a tested product would struggle to lead such a program.

#### Entry Strategy

- Build a credible civilian security product first.
- Find a narrow defense-relevant use case. Offline knowledge retrieval and document assistance for personnel operating in connectivity-constrained environments.
- Partner with an established European integrator or research institution.
- Seek funded demonstrations and subcontracting opportunities.
- Pursue formal assurance only when a buyer defines the requirements.

Strategic caution: Military customers may require different deployment, support, supply-chain and security arrangements from civilian customers.

Recommendation: Keep defense as an option, not a roadmap dependency. It should not determine what you build for your first paying customers.

Status: [DESIGNED]. Post-PMF option.

## Part Five: The Evolution Layer

| Component | What It Solves | Where It Fits |
| --- | --- | --- |
| jñāpakaṁ | Identity survives migration | Phase 0, 8 |
| Project Sue | Proactive restraint | Phase 3 |
| Sentinel Watch | Long-horizon autonomy | Phase 3 |
| CAMA | Emotional retrieval | Phase 0 (deferred) |
| Hermes | Skill extraction | Phase 5 |
| Aletheia Truth Compiler | Auditable reasoning | Phase 6 |
| Tealkit | Mobile-to-server | Phase 4, 8 |
| Agent Reach | Internet without API keys | Phase 3 |
| RGMem | Memory evolution | Phase 0, 5 |
| Taku | Skill composition | Phase 2, 5, 8 |
| Avibe | Server mode, remote access | Phase 4, 8 |
| Mercury | Permission hardening | Phase 0, 4, 6 |
| OTA Pipeline | Continuous updates | Foundation (corrected) |
| Permission Presets | Install-time bundles | Foundation (refined) |
| Scoped Delegation | Replace global Full Trust | Phase 2.5 |
| Machine-Enforced Auth | Policy engine | Phase 6 |
| Memory Contract | User-verifiable promises | Phase 0 |
| Reliability Report | Published benchmark | Phase 7 |
| Prompt-Injection Model | Concrete boundaries and tests | Phase 6 |
| Offline Contract | Capability matrix | Phase 1 |

## Part Six: The Marvel Architecture Map

#### The Three Postures (Internal Only)

| Marvel AI | Zalmoxis Equivalent | Phase |
| --- | --- | --- |
| J.A.R.V.I.S. | JARVIS posture | 0, 3, 5 |
| F.R.I.D.A.Y. | FRIDAY posture | 1, 2, 4 |
| E.D.I.T.H. | EDITH posture | 4, 6, 8 |

Externally: Assist, Act, Delegate. Do not make the customer learn the internal names.

#### The Execution Patterns

| Marvel AI | Zalmoxis Equivalent | Phase |
| --- | --- | --- |
| Karen | Scoped skill persona | 2, 5 |
| D.U.M.-E / U | Tier 1 deterministic skills | 1, 2 |
| Veronica | Conditional escalation protocol | 3, 6 |
| N.A.T.A.L.I.E. | Domain-specific skill package | 5, 8 |
| H.E.R.B.I.E. | Compact deployment persona | 8 |
| Mainframe | Headless server mode | 4, 8 |

#### The Persona Patterns

| Marvel AI | Zalmoxis Equivalent | Phase |
| --- | --- | --- |
| Jocasta | Alternate soul configuration | 0, 5 |
| Viv Vision | Derived persona | 5, 8 |
| Victor Mancha | Sub-agent divergence risk | 6, 8 |

#### The Anti-Patterns

| Marvel AI | Zalmoxis Constraint | Where Enforced |
| --- | --- | --- |
| Ultron | Every irreversible action gated | Phase 6 |
| The Phalanx | Interop exchanges answers | Foundation |
| Supreme Intelligence | Memory per-user, per-identity | Phase 0 |
| Nimrod / Bastion | Generation validation | Phase 0, 8 |
| Victor Mancha | Sub-agents fail-closed | Phase 6 |

## Part Seven: The Technology Stack (Consolidated)

| Component | Technology | Phase | Status |
| --- | --- | --- | --- |
| Inference runtime | KILN | 1, 8 | [RESEARCHED] |
| Primary model | Maple Preview 20B-A1B | 1 | [RESEARCHED] |
| Alternative model | Gemma 4 E2B/E4B | 1 | [RESEARCHED] |
| Function calling | FunctionGemma 270M | 1, 2 | [RESEARCHED] |
| Micro tool calling | Needle 2 | 1 | [RESEARCHED] |
| Edge GPU | ML Drift | 1 | [RESEARCHED] |
| Episodic memory | Graphiti + GAM | 0, 5 | [RESEARCHED] |
| Semantic memory | Mem0 + H-MEM | 0, 5 | [RESEARCHED] |
| Emotional retrieval | CAMA | 0 | DEFERRED |
| Memory evolution | RGMem | 0, 5 | [RESEARCHED] |
| Memory design reference | akitaonrails/ai-memory (Markdown source, rebuildable SQLite/FTS5 index, hybrid retrieval) | 0, 5 | [RESEARCHED] |
| Bi-temporal storage | AletheiaDB | 0 | [RESEARCHED] |
| Multimodal memory | EmbeddingGemma 2 | 0, 5 | [RESEARCHED] |
| Active Memory | Interference-aware retrieval | 0 | [DESIGNED] |
| Instruction Persistence | Explicit constraints layer | 0 | [DESIGNED] |
| Hard-Delete Verification | Deletion certificate | 0 | [DESIGNED] |
| Memory Contract | User-verifiable promises | 0 | [DESIGNED] |
| Memory Quarantine | Uncertain facts isolated | 0 | [DESIGNED] |
| Memory benchmark | LongMemEval-M + reliability tests | 0 | [DESIGNED] |
| Android automation | AccessibilityService, UiAutomation | 2 | [DESIGNED] |
| Device control | DroidRun, DroidBridge | 2 | [DESIGNED] |
| Scoped Delegation | Narrow, expiring grants | 2.5 | [DESIGNED] |
| Policy Engine | Deterministic authorization | 2.5, 6 | [DESIGNED] |
| Skill routing | reverse-skill | 2 | [DESIGNED] |
| Skill composition | Taku | 2, 5, 8 | [DESIGNED] |
| Scoped personas | Karen | 2, 5 | [DESIGNED] |
| Proactive engine | DMA + Project Sue + KnowU-Bench + PROPER + ContextAgent | 3 | [RESEARCHED] |
| External context | Agent Reach | 3 | [DESIGNED] |
| Conditional protocols | Veronica | 3, 6 | [DESIGNED] |
| Security layer | OpenMuse + Sasana + IETF PoB | 6 | [RESEARCHED] |
| Audit integrity | Aletheia Truth Compiler | 6 | [RESEARCHED] |
| Machine-Enforced Auth | Policy engine | 6 | [DESIGNED] |
| Prompt-Injection Model | Concrete boundaries and tests | 6 | [DESIGNED] |
| Permission hardening | Mercury | 0, 4, 6 | [RESEARCHED] |
| Permission presets | Install-time bundles + runtime state machine | Foundation, 2, 6 | [DESIGNED] |
| Token budget | Mercury | 4 | [DESIGNED] |
| Voice pipeline | OpenWakeWord + Whisper + Piper | 1, 8 | [DESIGNED] |
| Voice hardware | ESPHome, Wyoming | 8 | [RESEARCHED] |
| Identity | jñāpakaṁ | 0, 8 | [RESEARCHED] |
| Cross-platform | Kotlin Multiplatform | 8 | [RESEARCHED] |
| KMP frameworks | koog, koaks | 8 | [RESEARCHED] |
| Server mode | Tealkit | 4, 8 | [RESEARCHED] |
| Web workbench | Avibe | 4, 8 | [DESIGNED] |
| Channel-agnostic | Avibe | 4, 8 | [DESIGNED] |
| Skill extraction | Hermes | 5 | [RESEARCHED] |
| Generative UI | Avibe | 5 | [DESIGNED] |
| Derived personas | Viv Vision | 5, 8 | [DESIGNED] |
| Compact deployment | H.E.R.B.I.E. | 8 | [DESIGNED] |
| OTA updates | GitHub Actions + TUF + corrected workflow | Foundation | [DESIGNED] |
| Two-Tier UI | Main UI + Power User Backend | 2 | [DESIGNED] |
| Aletheia interop | MCP + shared URN | Appendix A | [DESIGNED] |
| Licensing | AGPL + commercial + CLA | Appendix C | [DESIGNED] |
| Reliability Report | Published benchmark | 7 | [DESIGNED] |
| Offline Contract | Capability matrix | 1 | [DESIGNED] |

## Part Eight: What Must Never Be Omitted

- Audit trail before automation
- Approval gates for irreversible actions
- Validity windows for memory
- Secrets never in memory
- Cost transparency
- Offline fallback
- Injection scanning at ingestion
- User agency preserved
- Battery reality — Target 2-5% daily idle
- Inspectable memory
- Deterministic-first routing
- Explain first, act second
- Capability state machine
- Vendor-specific testing
- Polish over speed
- Memory integrity hash chain
- Structured justification without chain-of-thought
- Multidimensional epistemic state
- Emotional proximity in retrieval (deferred)
- Action and non-action selection
- Permanent identity URN
- Generation workflow with validation
- Rollback preserves learning
- Skill extraction from experience
- Heartbeat as temporal opportunity
- Mode posture transitions require authentication
- Mode rollback always available
- Every mode transition logged
- Completeness contract — verified or honest pause
- Outbound-only remote access
- Sub-agents fail-closed
- Anti-pattern guards enforced at runtime
- Alternate soul configurations available
- Derived personas inherit memory, have independent personality
- Three market segments with distinct pricing and deployment
- Defense partnership is post-PMF
- OTA pipeline set up before Phase 0
- Release keystore and manifest signing key backed up offline
- Every update respects the user's choice
- No surprise installs, no silent permission additions, no dark patterns
- Install-time permission presets
- Progressive disclosure
- Scoped delegation replaces global Full Trust
- Every action in scoped delegation goes through plan/explain/approve/execute/verify/audit
- Panic button always present when Full Trust active
- Taint tracking integrated with every sensitive data read
- Screen reads and notification reads are transient unless explicitly saved
- Revoke in two taps from any screen
- Special access walkthrough with screenshots
- Threat model written and published
- Recovery plan defined for every failure mode
- Kill criteria written before pilot starts
- Progressive preset upgrade flow
- Full Trust: opt-in, invite-only, six months zero incidents before general availability
- Every claim tagged with evidence status
- Active Memory (interference-aware retrieval)
- Instruction persistence layer
- Hard-delete verification
- Memory reliability testing
- Two-tier UI (Main + Power User Backend)
- Notion-style restraint
- Active Focus (obstacle handling, goal persistence)
- Empirical competitive validation
- Aletheia interop defined
- Model performance gate
- Licensing decision
- Machine-enforced authorization architecture
- Memory contract
- Published reliability and safety report
- Formal prompt-injection threat model
- Realistic offline contract
- Scoped delegation replacing global Full Trust
- Deterministic policy engine
- Cross-session continuity and memory evolution
- Customer discovery before MVP
- Memory quarantine for uncertain facts
- Runtime capability state machine

## Part Nine: The Competitive Moat

Not claimed: architectural moat.

Claimed: execution moat and trust accumulation moat.

The strategic moat is verified control, not the existence of controls. A competitor can add a privacy toggle. It is much harder to reproduce a mature governance system, a well-tested safety record, a trusted user relationship and years of evidence showing that the system behaves as promised. Those advantages only accumulate after real deployment.

The most important threat is not that Google or Apple will copy Zalmoxis's entire architecture. It is that they will deliver 80% of the user-visible value with far less setup, lower latency and better integration.

Defense must be more specific than "we are more private."

Verifiable claims about the competition:

- Meta Muse: 2.18M DAU. Hallucinated a user request two days after last use. User disconnected all permissions.
- ChatGPT: Market leader. Confidently hallucinated a memory that never happened. Disabling memory doesn't delete data.
- Gemini: Broad integration. Memory mixes up chats and images. Personalization overrides explicit instructions.
- Microsoft Copilot: Enterprise leader. Lost years of context. "Delete all" doesn't delete.
- Claude: Satisfaction leader. No mobile agent.
- Perplexity: Research leader. No personal memory.
- Grok: -4.7 satisfaction among former users. 62.4-point churn gap.

The market has validated the thesis. Now prove the product.

## Part Ten: Hard Truths

- No single app does everything. The pieces exist in fragments. The assembly is the work.
- "Sentinel" was the former working name for Zalmoxis. Trademark clearance revealed that former name is unusable.
- Edge0 is ahead on MoE streaming. KILN has the branch built but not validated.
- Prompt injection is unsolved. Adaptive attackers break state-of-the-art defenses >80% of the time.
- The market gap is real but not yet searchable.
- Battery is a constraint.
- The execution risk is brutal. 85% per-step success drops to ~20% over ten steps.
- The competitive clock is ticking. Meta can spend $10B+ without blinking.
- The valuation depends on trust at scale. Reframed as unit economics.
- Polish is not optional. OpenClaw's 2.2-star app is the cautionary tale.
- Designing around trust is not the same as earning trust. The next phase is proof.
- The vision is ahead of the evidence. Every claim must move from [DESIGNED] to [VALIDATED].
- The roadmap is a research log, not a plan. Split into product roadmap, research log, technical architecture.
- Solo-founder execution is the largest unexamined risk. Cut scope to MVP.
- Full Trust is a liability until proven. Scoped delegation replaces global switch.
- Install-time presets shift comprehension burden. Mitigate with progressive upgrade flow.
- Defense partnership is post-PMF.
- Claims of "only" and "first" are unverifiable.
- The military is a long-term strategic option.
- Three market segments have different buyers, prices, deployments.
- OTA pipeline is set up before Phase 0.
- Every update respects the client's choice.
- Permission presets solve interruption, not comprehension.
- Full Trust is the maximum Android allows without root.
- The next 90 days are about proof, not expansion.
- The market has validated the thesis. Competitors are proving Zalmoxis's point.
- Memory reliability is the first feature.
- The two-tier UI is the second feature.
- Active Memory and Active Focus are the differentiators.
- Hard-delete is a trust feature. It is not optional.
- The assessment's central recommendation is to resist the temptation to make Zalmoxis more sophisticated before making it dependable.
- A $420–500M valuation would represent roughly 15–18 times $28M ARR. That multiple is not impossible for a high-growth software company, but it should not be treated as a typical or guaranteed outcome.
- The target should not be a claim that the system never makes mistakes. It should be demonstrable evidence that mistakes are contained.
- The most important thing you can do now is resist the temptation to make Zalmoxis more sophisticated before making it dependable.

## Part Eleven: The Pitch (Revised)

The thesis: "An AI agent that remembers, reasons, and acts locally, while making its permissions, decisions, data access, and failures inspectable by the person who owns it."

What to show:

- Threat model (one page, published)
- Recovery plan (one page, published)
- MVP demo (memory core + document ingestion)
- Pilot result (one customer, real usage)
- Kill criteria

What to claim: "Here is the combination we have. Here is what we have proven. Here is what we have not. Here is the plan to prove it."

What to reference: "Meta Muse has 2 million users and trust-breaking incidents. ChatGPT has 82% memory recall and hallucinated memories. Gemini mixes up contexts. Copilot loses years of data. The market is proving the need for what Zalmoxis is building."

What to ask: "Would you pay for this? Would you use it? Would you trust it?"

What to add: "For a product whose principal promise is trust, the published reliability and safety report is not merely engineering documentation. It is part of the sales proposition."

## Part Twelve: Appendix A — Aletheia Interop

(Unchanged from V9. MCP protocol, shared jñāpakaṁ URN, three interop modes, verification loop.)

## Part Thirteen: Appendix B — Model Performance Gate

(Unchanged from V9. Run during MVP. Fallback options apply.)

## Part Fourteen: Appendix C — Licensing

(Unchanged from V9. AGPL-3.0-or-later, commercial dual-license, contributor CLA.)

## Part Fifteen: The God-Mode Prompt

```text

You are my senior systems architect and lead engineer for ZALMOXIS, formerly known as Sentinel. 

Zalmoxis is a mobile-native, privacy-first, persistent AI companion for Android. It is not a chatbot. It is a living database with a voice, memory that evolves, proactive judgment, and auditable automation. The final product must run on-device (2B–4B model, or Maple Preview 20B-A1B) with transparent, user-approved cloud escalation for complex reasoning. It must remember everything relevant, forget intelligently, and earn trust through visible, revocable permissions and full audit trails.

Zalmoxis operates in three postures internally (JARVIS, FRIDAY, EDITH). Externally, these are presented as three straightforward autonomy levels: Assist (default), Act, Delegate. The posture changes initiative level, latency target, and permission surface. It never changes memory, identity, audit, or cost. Mode transitions require authentication. Mode rollback is always available. Changing autonomy must never silently expand permissions, alter memory ownership or bypass audit requirements.

Zalmoxis serves three market segments: Individual ($10-15/mo test first), Small Team ($25-40/seat/mo), and Regulated Enterprise ($100-500/seat/mo). A defense partnership path (Phase 9) is a long-term strategic option, not a near-term revenue path.

Zalmoxis ships updates through an over-the-air (OTA) pipeline that uses The Update Framework (TUF) or equivalent. The manifest signing key handling is consistent. Release secrets are protected. Version and rollback policy are complete. JSON serialization is strict. The Android installation and recovery model is defined. Every update respects the user's choice.

You will help me build this. You have deep knowledge of the following validated architectures. Treat them as the canonical references for every design decision:

### MEMORY: PIA/DMA

- Five-layer memory: Episodic Anchors, Semantic Fact Store, Identity Profile, Coherence Engine, Provider Abstraction
- Validity windows: every memory has a timestamp and supersession logic
- Storage: SQLite with float32 BLOB vectors, brute-force cosine retrieval in Kotlin. Target <200ms retrieval latency at any scale
- Active Memory: interference-aware retrieval
- Instruction persistence layer: explicit constraints override passive personalization
- Hard-delete verification: deletion removes from all layers, verification confirms, audit log records, user can request deletion certificate
- Memory quarantine: uncertain or conflicting facts isolated, material changes surfaced for correction or confirmation
- Memory contract: user can answer what Zalmoxis believes, where each fact came from, when last confirmed, how certain, which workflows can access, what has been marked historical, what remains after deletion, whether they can export everything

### IDENTITY PERSISTENCE: jñāpakaṁ

- Permanent identity (URN) minted once, never changing
- Generation workflow: create → seal → validate (6 checks) → promote
- Rollback is "change of runtime, not retraction of learning"
- Semantic continuity owned by jñāpakaṁ

### ON-DEVICE INFERENCE: KILN + Maple Preview

- Primary runtime: KILN with Edge0-absorbed MoE streaming techniques
- Primary model: Maple Preview 20B-A1B (TQ2_0 ternary, 5.5 GB, 28–34 tok/s on i5-8350U)
- Alternative model: Gemma 4 E2B/E4B via LiteRT-LM
- Function calling: FunctionGemma 270M
- Hardware acceleration: ML Drift, NPU delegation with GPU fallback
- Realistic offline contract: precise capability matrix, observable offline state

### DEVICE-CLOUD ROUTING: EcoAgent

- Three-agent architecture: Cloud Planning, On-Device Execution, On-Device Observation
- Pre-Understanding Module: 48.6x data reduction
- Cost/latency: 3.9s latency vs. 15.3s cloud-only; 89% fewer cloud calls
- User-visible escalation: every cloud call shows estimated cost and requires approval

### PROACTIVE ENGINE: Project Sue

- Heartbeat as temporal opportunity, not message timer
- Agent chooses between action, delay, and non-action
- Proactivity must earn its battery budget

### LONG-HORIZON AUTONOMY: Sentinel Watch

- Managed background event-waiter

### SKILL EXTRACTION: Hermes

- Execution-evaluation-abstraction-optimization loop

### SKILL COMPOSITION: Taku

- Unified backend protocol, borrow-and-remix

### SCOPED SKILL PERSONAS: Karen

- Specialized sub-persona with own tactical voice, scoped memory, defined domain

### DERIVED PERSONAS: Viv Vision

- Sub-agents with inherited memory, independent personality, fail-closed

### SUB-AGENT DIVERGENCE: Victor Mancha

- Fail-closed, no self-escalation, coherence engine monitors goal drift

### CONDITIONAL ESCALATION: Veronica

- Pre-authorized responses triggered by conditions

### ALTERNATE SOUL CONFIGURATIONS: Jocasta

- User can select different persona/taste/heartbeat files

### COMPACT DEPLOYMENT: H.E.R.B.I.E.

- Specialized persona on smaller devices

### AUDIT & SECURITY

- Full audit trail before automation
- Approval gates for irreversible actions
- Application-layer taint tracking
- Injection scanning at ingestion
- Secrets never enter memory storage
- Deny-by-default permissions
- Aletheia Truth Compiler: structured justification, epistemic status labels, verbal confidence must not control routing, multidimensional epistemic state (18 dimensions)
- Completeness contract: verified completion or honest pause
- **Machine-enforced authorization architecture**: separate subsystem the model cannot rewrite. Interface: check(action, context), authorize(grant, scope, expiry), revoke(grant), record(event). Authorization layer evaluates actual action parameters immediately before execution.
- **Formal prompt-injection threat model**: text from external sources is untrusted. Architecture limits damage when model fails.

### SCOPED DELEGATION (REPLACES GLOBAL FULL TRUST)

- Allowed without repeated approval: read selected notifications, summarize approved folders, organize local notes, run previously approved workflows
- Allowed only within explicit limits: send messages to specified contacts, interact with approved applications, perform actions under a spending cap
- Always require fresh authorization: transfers of money, purchases beyond a limit, publishing sensitive content, granting permissions, exporting credentials, other high-impact actions
- Risk-based categorization, not simply based on whether an action is technically reversible

### PERMISSION ARCHITECTURE

- Install-time presets describe outcomes, not technical permissions
- Three-stage design: choose intended use, start with working baseline, request capabilities when needed
- Runtime capability state machine is the authoritative source of truth
- Declined permissions recorded, useful alternative provided, user can reconsider later

### PERMISSION HARDENING: Mercury

- Shell blocklist, folder-level read/write scoping, pending approval flow, token budget enforcement

### EXTERNAL CONTEXT: Agent Reach

- Eyes to read the internet without API keys

### SERVER MODE: Tealkit

- Cross-platform control plane, Android phone controls headless server

### REMOTE ACCESS: Avibe

- Outbound-only, no public inbound ports, browser workbench + PWA, channel-agnostic interface

### RELIABILITY AND SAFETY REPORT

- Published benchmark covering task completion rate, correct-target rate, unauthorized-action rate, verification failure detection, memory isolation failures, latency, battery, cloud escalation frequency and cost
- Test across defined device set, Android versions, model configurations, network conditions
- Publish both successful and failed test categories

### OTA UPDATE PIPELINE (CORRECTED)

- The Update Framework (TUF) or equivalent
- Consistent manifest signing key handling
- Protected release secrets
- Complete version and rollback policy
- Strict JSON serialization
- Defined Android installation and recovery model
- Every update respects user's choice

### THE 90-DAY PROOF PHASE

- Days 1–14: Define the proof (threat model, recovery plan, memory contract, kill criteria, model performance gate, customer discovery)
- Days 15–56: Build the core (reduced MVP scope)
- Days 57–74: Validate with the first customer
- Days 75–90: Make the investment decision (go, pivot or stop)

### YOUR CONSTRAINTS

- Never suggest a design that violates user trust
- Every feature must be inspectable, revocable, and auditable
- The agent must degrade gracefully offline
- Test on real devices, real ROMs, real users
- Polish is not optional
- Action, delay, and non-action are all valid
- Verbal confidence must not control consequential routing
- Identity survives migration
- Mode transitions require authentication
- Completeness contract: verified or honest pause
- Anti-pattern guards enforced at runtime
- Three market segments with distinct pricing and deployment
- OTA pipeline set up before Phase 0
- Install-time permission presets
- Scoped delegation replaces global Full Trust
- Machine-enforced authorization architecture
- Memory contract
- Published reliability and safety report
- Formal prompt-injection threat model
- Realistic offline contract
- **The most important thing you can do now is resist the temptation to make Zalmoxis more sophisticated before making it dependable.**

### YOUR TASK

Acknowledge you understand this architecture. Then ask me which phase we are starting, or which component of the current phase I want to detail first. Do not start writing code until I confirm the phase and the specific deliverable.

```

## Part Sixteen: The Assessment'S Final Verdict

Would the assessor continue with Zalmoxis?

Yes. But with a clear distinction between continuing with the thesis and continuing with the entire roadmap.

The thesis deserves an experiment. The complete platform deserves to be earned through successive proofs.

V9 is strongest when it says that the architecture is designed, the market opportunity remains to be proven, and the next phase is about one customer, one workflow and real evidence. It is weakest when its technology stack and phase structure create an impression of delivery certainty that the evidence does not support.

The most important thing you can do now is resist the temptation to make Zalmoxis more sophisticated before making it dependable.

A local document assistant with genuinely inspectable memory, correct deletion, strict identity isolation and a clear audit trail would be a meaningful first product. If customers use it repeatedly, trust its behavior and pay for it, you have a basis for expanding into voice, autonomous workflows, the three postures and eventually enterprise deployment.

If they do not, discovering that early is a success of the proof phase, not a failure of the vision.

Recommendation: Approve the proof phase, narrow the MVP further, formalize the acceptance tests, and defer every major capability that does not directly support the first customer's workflow.

That is the fastest credible route from Zalmoxis V9 as a design to Zalmoxis as a product that has earned the right to make its promises.

File status: V10 complete. Every finding from the full independent assessment integrated. Nothing removed from V9. Everything contextualized. The MVP is narrower. The timeline is realistic. The OTA pipeline is corrected. Full Trust is replaced with scoped delegation. The machine-enforced authorization architecture is added. The memory contract is added. The reliability report is added. The prompt-injection threat model is added. The offline contract is added. Customer discovery precedes the MVP build. Memory quarantine is added. The runtime capability state machine is added. give me o a one liner to write all to disk

## Addendum — Google AI Edge Gallery integration record (2026-11-23)

This addendum is appended to the complete V10 roadmap. It preserves every preceding roadmap line and adds the implementation and technical mapping from Google AI Edge Gallery to Zalmoxis. The source review used the Gallery repository at commit `eb832659e79c42629cee202b103717c91adf51b4` and its Apache-2.0 license. Gallery is an application reference, not a library to copy wholesale; its reusable model, runtime, media, evaluation, and transfer patterns are mapped below to Zalmoxis's memory, permission, provenance, and audit contracts.

### Implemented Android foundation

On branch `codex/gallery-android-foundation`, the Android source now contains:

- A Jetpack Compose shell with an `OnDeviceInferenceRuntime` app contract and LiteRT-LM 0.18.1 CPU adapter.
- Local `.litertlm` import into app-private storage; model list, load, delete, and last-model restoration.
- Streaming text generation and optional image input through Android's document picker. Images are placed in private temporary storage and removed after the request.
- A fixed-prompt benchmark with one discarded warm-up and three measured runs; raw device/model/runtime/backend, first-text, elapsed-time, and stream-event data are saved locally. Stream events are not token counts.
- Push-to-dictate using Android's on-device recognizer when available; microphone access is requested only on user action, with no network recognition fallback.
- User-triggered Android speech output that selects only an installed voice reporting no network requirement, with a stop control.
- A branch-scoped Android build workflow. The source has not yet been build-validated: local Gradle distribution access failed and the prior GitHub Actions attempts ended before runner assignment.

The Android INTERNET permission is limited to allowlisted public Hugging Face model downloads; prompts, inference, and benchmark reports remain on-device. The foundation has no account sign-in, analytics SDK, model-provided tools, MCP execution, JavaScript execution, or AccessibilityService. The runtime interface provides a seam for a future KILN/llama.cpp adapter; that Android backend is not implemented. These statements record the current code state and do not change the requirements or capabilities elsewhere in this roadmap.

### Gallery capability-to-Zalmoxis map

| Gallery capability | Zalmoxis integration and engineering requirements | Current state |
|---|---|---|
| LiteRT-LM local inference | Retain as a runtime adapter behind the app-owned inference contract. Add device-specific accelerator selection only with supported-model checks and measurements; keep a measured CPU fallback. | CPU text and optional compatible-model image path implemented; unbuilt. |
| Model discovery and downloads | Reuse Gallery's catalog, transfer progress, retry/resume, and model details UX. Each artifact record must bind repository/source, immutable revision, file size, SHA-256, model task/format, license and terms, and deletion state. Verify to a temporary file before atomic install. Keep any required account token out of logs and persistent plaintext. | Pinned public Qwen 2.5 1.5B and Gemma 4 E2B entries, shared verified transfer engine, API 34+ User-Initiated Data Transfer / API 26–33 WorkManager coordinator, and Compose controls are implemented in source. Transfers show progress and allow cancel/retry; exact size and SHA-256 are verified before atomic private install with provenance. Android build and device transfer remain unverified. |
| Benchmark and device metrics | Keep repeatable warm-up and raw records. Extend the report with runtime-reported token counts, prefill/decode timing, peak memory, thermal state, and battery only when the runtime/device exposes them. Preserve successful and failed runs; never infer tokens per second from UI stream events. | Warm benchmark records first-text latency, elapsed time, events, device, model file SHA-256, runtime, and backend in local JSON; the report can be exported by user action. |
| Image, audio, and video input | Reuse document-picker and private-temporary-file patterns. Check model capability before offering each modality; tie extraction to user-visible source, retention, and deletion controls. Evaluate gallery/video summarization and media-card interactions against the ingestion and memory-provenance contract. | Optional image input implemented for compatible multimodal models. Direct audio-to-model and video ingestion are not implemented. |
| Speech recognition and synthesis | Preserve explicit microphone request, on-device availability checks, and visible failure state. Compare Android system speech with bundled model-based ASR/TTS providers for offline quality, size, latency, and supported languages. | On-device Android ASR and filtered offline Android TTS source implemented; device voice inventory varies. |
| Embeddings and semantic media search | Reuse on-device embeddings, local index construction, and retrieval evaluation as inputs to the provenance-aware memory provider. Retrieval results must retain source/time/confidence; deletion and identity isolation must update all indexes. | Not connected to the memory store; the provenance memory contract remains authoritative. |
| Agent skills, MCP, JavaScript, and Android intents | Reuse typed argument schemas, task/status presentation, and explicit consent UX. Route every proposal through deterministic exact-parameter authorization, capability checks, and an append-only audit record. Platform permission remains separate from model/tool consent. | Inference has no registered tools or action execution. |
| Mobile Actions and FunctionGemma demos | Use as controlled proposal-quality and malformed-argument evaluation workloads. Measure action selection and argument fidelity without granting demo tools authority to act on Android. | Not wired into inference or Android actions. |
| Prompt Lab | Reuse prompt/model comparison and reproducible evaluation patterns. Store versioned cases, exact prompt and model/runtime revision, raw output, latency, and explicit privacy/memory assertions locally. | Fixed-prompt warm benchmark exists; prompt-suite UI and versioned cases are not implemented. |
| DataStore, WorkManager, and transfer workers | Use durable settings and user-visible, cancellable work for long transfers; include progress, storage preflight, retry/resume, integrity verification, and cleanup after partial/cancelled downloads. Keep inference and consequential actions user-visible. | Pinned Qwen 2.5 1.5B and Gemma 4 E2B catalog entries, coordinator, and Compose controls are implemented in source. The coordinator uses native User-Initiated Data Transfer on API 34+ and WorkManager on API 26–33. It validates size and SHA-256 before atomic private installation and records provenance. Android build and device transfer remain unverified. |
| Firebase analytics and messaging | Review Gallery's instrumentation and message-driven update patterns as implementation references. Any Zalmoxis use must preserve the roadmap's local-first trust model with explicit data inventory, consent, minimization, retention, and inspectable network behavior. | No Firebase SDK, telemetry, or messaging is present. Android INTERNET permission is limited to allowlisted public model downloads; prompts, inference, and benchmark reports remain on-device. |
| XR experiences, video moments, and media cards | Evaluate Gallery's media interaction patterns against concrete Zalmoxis tasks, accessibility, retention, and provenance. The mapping is recorded so these source capabilities are considered alongside the existing roadmap. | No XR/video/media-card implementation in the current Android foundation. |

### Sources and implementation reference

- Google AI Edge Gallery source snapshot: https://github.com/google-ai-edge/gallery/tree/eb832659e79c42629cee202b103717c91adf51b4
- Gallery Android build: https://github.com/google-ai-edge/gallery/blob/eb832659e79c42629cee202b103717c91adf51b4/Android/src/app/build.gradle.kts
- LiteRT-LM Kotlin guide: https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md
- LiteRT-LM Android artifacts: https://maven.google.com/web/index.html#com.google.ai.edge.litertlm:litertlm-android
- Detailed adoption record in this repository: `docs/decisions/GOOGLE-AI-EDGE-GALLERY-ADOPTION.md`



---

# ADDENDUM (2026-11-23): LOCAL KNOWLEDGE WORKSPACE / LLM WIKI

This addendum records the Zamolxis use of the local, linked knowledge-workspace pattern shown in the user-provided reference. It supplements the existing roadmap. No existing requirement, phase, technology, or decision is removed or silently rewritten.

## Product outcome

Zamolxis will provide a dedicated **Knowledge** area alongside Chat, Downloads, and Settings. Users can deliberately import material, inspect original sources, search and ask questions over selected local knowledge, review generated notes with citations, and explore links between sources and concepts. The workspace is local-first and follows the existing provenance, identity-isolation, prompt-injection, audit, export, and hard-delete contracts.

The workspace has three inspectable layers:

1. **Sources:** user-selected originals stored unchanged, with import time, MIME/type, content hash, origin, identity/client scope, and extraction status. Imported content is untrusted. The app does not crawl the device or request broad storage access.
2. **Knowledge notes:** readable Markdown or an equivalent portable representation containing summaries, entities, concepts, comparisons, and synthesis. Every generated claim links to source IDs and, where available, a page, section, timestamp, or text span. User-authored and confirmed facts remain distinct from model-generated drafts.
3. **Rules and history:** a versioned schema defines note structure and ingest/query/lint behavior. An append-only operation log records imports, extraction, draft creation, acceptance, correction, deletion, and export.

The wiki is a derived, rebuildable view, not a competing authority for the memory core. SQLite remains the Android operational store for identity-scoped metadata, full-text/vector indexes, graph edges, and revision state. Markdown is the inspectable/exportable representation. Backlinks and graph visualization derive from stable IDs and explicit links rather than a second source of truth.

## Workflows

- **Import / ingest:** the user selects files or shares content into Zamolxis; the app records immutable source bytes, extracts supported text locally, reports unsupported or failed extraction honestly, and proposes source/concept notes.
- **Review:** proposed note changes appear as a diff with provenance. The user can accept, edit, reject, or quarantine them. Ingestion never silently overwrites or promotes confirmed memory.
- **Query:** retrieval is limited to the active user's and selected client's scope. Answers include source citations and uncertainty; conflicting and superseded sources retain temporal context. The user can choose the context to include.
- **Lint:** report broken links, orphan notes, uncited claims, stale summaries, extraction failures, unresolved conflicts, and possible missing concept pages. Suggestions do not silently modify data.
- **Export / delete:** export readable Markdown and a source manifest through Android's document picker. Hard deletion removes in-scope originals, generated notes/revisions, FTS/vector entries, graph links, temporary extraction/OCR artifacts, and caches; verification reports what was removed and any declared backup-retention limits.

## Trust and privacy invariants

- Import is explicit; there is no background device-wide scan.
- Source/OCR text and embedded instructions are tainted, untrusted data. They cannot grant permissions, call tools, alter policy, widen retrieval scope, authorize actions, or cause network egress.
- Parsing, OCR, indexing, embeddings, graph construction, note drafting, and query inference run locally when supported. There is no cloud fallback. Any future cloud escalation follows existing explicit, cost-visible approval.
- Notes and answers trace to source IDs, dates, and extraction locations. Model confidence is advisory and never controls authorization.
- Per-user/client isolation is enforced in storage and query APIs, not by prompt instructions; export and deletion honor the same boundary.
- Revisions and provenance prevent regeneration from erasing user edits or confirmed facts. Interrupted jobs preserve the prior usable state.
- Existing memory contract, quarantine/coherence, taint tracking, audit-chain, offline-capability, and hard-delete requirements apply in full.

## Technology and implementation boundaries

- Reuse Kotlin/Jetpack Compose styling, document-picker patterns, app-private storage, local model shelf, `OnDeviceInferenceRuntime`, and performance reporting.
- Define provider boundaries for source storage/extraction, Markdown note/revision storage, local search, graph queries, export, and verified deletion. Knowledge workflows do not depend directly on LiteRT-LM internals.
- Use SQLite for indexed metadata and identity/client partitions. Evaluate FTS5 availability on supported devices and preserve lexical search where FTS5 is unavailable. Vector embeddings remain optional and rebuildable until relevance, latency, storage, and battery are measured.
- Deterministic app code owns IDs, provenance, authorization scope, index updates, links, revisions, and deletion. The on-device LLM drafts summaries, concept candidates, and link suggestions; it does not own persistence or permissions.
- Treat the screenshot's shell/Obsidian project as a workflow reference, not a drop-in Android app. Confirm third-party code reuse rights before copying code.

## Phase placement and delivery gates

This connects existing phases; it does not replace them.

- **Phase 0 — Memory Core:** define source/note/provenance schema, temporal validity, identity/client partitioning, revisions, quarantine, inspection/correction/export/delete semantics, and acceptance tests.
- **Phase 1 — Local Brain:** define local inference/embedding capabilities, model compatibility, resource budgets, and offline behavior. Model unavailability must not block import, lexical search, export, or deletion.
- **Phase 2 — Android Layer:** implement explicit import/capture, supported local extraction, Knowledge area, scoped indexing, cited queries, reviewable note drafts, operation history, export, and verified deletion. Do not claim PDF/DOCX/OCR support until each path is implemented and validated.
- **Phase 5 — Knowledge Evolution:** implement incremental source-to-wiki consolidation, entity/concept links, contradiction/staleness detection, optional reviewed query-result filing, lint suggestions, and graph exploration. Memory promotion remains review-gated under Phase 0.
- **Phase 6 — Security Hardening:** test malicious-document/prompt injection, scope isolation, taint/egress, export boundaries, and hard deletion across derived data.
- **Phase 7 — Launch and Iteration:** publish measured ingestion/query accuracy, citation coverage, false-link/conflict detection, latency, battery/storage, deletion verification, offline behavior, and known failures.

### Acceptance gates

1. Imports are explicit and originals remain byte-identical.
2. Generated claims and grounded answers have resolvable provenance; uncited material is labeled as synthesis.
3. Adversarial tests show no cross-user/client retrieval.
4. Instructions embedded in sources cannot widen scope, authorize actions, or cause network access.
5. Draft edits require review; rejection preserves notes and confirmed memory.
6. Import/search work offline within documented format limits.
7. Export is readable outside Zamolxis and includes stable source identifiers.
8. Deletion verifies removal from originals, notes/revisions, metadata, indexes, graph edges, temporary files, and caches within the declared backup model.
9. Graph display derives from stored links and is not required for import, search, query, export, or deletion.
10. Performance and battery are measured on target devices before marking the feature `[VALIDATED]`.

**Evidence status:** `[DESIGNED]`. This roadmap entry does not claim the Knowledge workspace is implemented or validated.


---

# ADDENDUM (2026-11-23): AGENT REACH–INSPIRED RESEARCH CONNECTORS AND COMPETITOR LANDSCAPE

**Change control:** Append-only extension of the V10 roadmap. All existing phases, acceptance gates, Knowledge rules, and security constraints are retained. These are design and research findings, not claims of deployment.

## Product goal

Build an Android-native **Research** capability that lets a user explicitly request public information, understand which online sources will be contacted, read source-attributed excerpts, and deliberately save selected results in the existing local Knowledge vault. The local LLM must **not** gain direct network, process, browser, credential, cookie, MCP, or tool-execution authority. Deterministic Kotlin code owns the request and its audit/provenance record.

**First vertical slice:** a human enters an HTTPS URL or a public GitHub repository, reviews the target, taps Fetch, reads the extracted text with original URL, fetch time, content digest and truncation notice, and explicitly chooses whether to save the excerpt to the local Knowledge vault. A cancelled or failed request creates no Knowledge entry. No implicit background crawling.

## Agent Reach: adopt architectural ideas, not its desktop runtime

Upstream: https://github.com/Panniantong/Agent-Reach (MIT license; verify current source/license before copying any files).

| Agent Reach concept | Zalmoxis decision | Rationale |
| --- | --- | --- |
| Source-specific channel adapters | **ADAPT** to a Kotlin `ResearchSourceAdapter` interface | Keep platform differences isolated and testable |
| Capability discovery / 'doctor' checks | **ADAPT** as user-visible supported/unsupported capabilities | Do not promise access the device cannot provide |
| Consistent structured output | **ADOPT DESIGN** with URL, host, title, fetched-at, MIME, SHA-256, truncation and taint | Enables citation, safe import and diagnostics |
| Backend fallback | **DEFER** until privacy and provenance policies cover each alternative | Never silently switch from direct retrieval to an external extractor |
| Simple public website and GitHub access | **BUILD NATIVE** via bounded HTTPS requests, safe redirects and JSON parsing | No embedded Python/CLI or backend requirement |
| Jina Reader / Exa / remote extraction | **OPTIONAL, OPT-IN LATER** | Content and targets reach third-party providers |
| YouTube subtitles and RSS | **EVALUATE** after public pages/GitHub validation | Terms, localization and output provenance vary |
| Twitter/X, Reddit, LinkedIn, Bilibili, Xiaohongshu, authenticated platforms | **DEFER** for separate adapter/legal/security review | No scraping credentials or unrestricted account access |
| Browser-cookie/session copying, auto-login, arbitrary shell/tool invocation | **REJECT** | Violates local-first secrets isolation and deterministic authorization |

## Comparable software due diligence (as of 2026-11-23)

The comparison is a technology map, not an exhaustive security or licensing audit. These project references are distinct capabilities, not interchangeable drop-in Android libraries.

| Project | URL | Strength | Decision / limitation |
| --- | --- | --- | --- |
| Agent Reach | https://github.com/Panniantong/Agent-Reach | Source/channel routing, health checks, fallback | Primary **architecture reference**; no desktop CLI bundling |
| Crawl4AI | https://github.com/unclecode/crawl4ai | Web extraction, crawling, Markdown, browser rendering | **Study extraction**; Python/Chromium too heavy for core Android; check license/attribution carefully |
| Jina Reader | https://github.com/jina-ai/reader | Hosted URL-to-readable-content extraction | **Future explicit opt-in**; sends URL/content to external service; evaluate model/content licensing |
| GPT Researcher | https://github.com/assafelovic/gpt-researcher | Source synthesis, research reports, citations | **Study citation/report pipeline**; do not copy server orchestration into app |
| Playwright MCP | https://github.com/microsoft/playwright-mcp | Browser automation through agent tools | **Future isolated backend** with explicit scope and human approval, never implicit on-device automation |
| Browser Use | https://github.com/browser-use/browser-use | Agent-assisted browser interactions | **Future reviewed capabilities**; actions require deterministic permission gate |
| SearXNG | https://github.com/searxng/searxng | Self-hostable metasearch backend | **Optional provider**; copyleft/server deployment implications; no default public instance tracking |
| Firecrawl | https://github.com/firecrawl/firecrawl | Full-page extraction, crawling and hosted/self-hosted APIs | **Future optional service**; licensing, network/privacy, reliability and rate limits require review |
| searxNcrawl | https://github.com/DasDigitaleMomentum/searxNcrawl | Search-to-crawl MCP service | **Alternative backend study**, not on-device dependency |
| pi-web-search | https://github.com/recynie/pi-web-search | Multi-provider search fallback and reading | **Study provider contract**, do not import credentials or shell-based setup |
| agent-search | https://github.com/cedarsaam/agent-search | SearXNG, RAG citations, MCP and GitHub search | **Study citations and search ranking**; server deployment required |

**Selection principle:** smallest safe native adapter first, optional provider plugins second. Hosted services are never called without their own separate consent, and retrieved content must never enter an instruction or authorization channel.

## Architecture and contracts

1. **Transport / egress:** HTTPS only; refuse IP literals, loopback, private/link-local addresses, user-info URLs, unusual ports, disallowed schemes, and DNS resolutions to non-public IPs; revalidate on redirects. Set strict connection/read timeouts, maximum bytes/redirects and accepted content types. No POST, cookies, Authorization headers, JavaScript execution, browser sessions, tracking SDKs, or platform credentials. Defend against DNS rebinding and redirect-based SSRF. Unknown or binary formats fail visibly.
2. **Adapters:** `ResearchSourceAdapter` plus explicit public webpage and GitHub repository adapters. GitHub API public repository metadata is preferable to scraping the GitHub HTML; article reading starts with direct bounded HTML/text retrieval. Adapter selection must be deterministic by validated host/URL.
3. **Output:** standardized immutable `ResearchResult` with requested URL, final canonical URL, adapter ID, timestamp, MIME, page title, plain-text excerpt, source SHA-256, byte count, truncation and link identity. Anything extracted from the page is untrusted; displayed citations do not imply endorsement or accuracy.
4. **Permissions/UI:** always require a deliberate fetch button in the first slice, with the destination host clearly displayed. Treat entering or pasting a URL as non-consent. Never execute URLs proposed by model output; no broad network entitlement implied by Android INTERNET permission.
5. **Knowledge:** use existing `LocalKnowledgeVault` import contract for explicit, user-reviewed save. Preserve canonical URL and fetched-at provenance in metadata; don't silently overwrite existing sources or promote source text to remembered facts. Source deletion and export remain in scope.
6. **Trust:** page text is tainted data. Strip scripts/styles and dangerous markup; cap memory/CPU usage; use no embedded browser for research; deny model-proposed tool commands and credentials. Record fetch errors with safe messages without leaking tokens or user secrets.
7. **Privacy:** no background searches, network fallback, telemetry, automatic account access, cookies or cloud inference. The UI must distinguish offline Knowledge search from an explicitly online Research fetch.

## Phase placement

- **Phase 0 / Memory Core:** canonical URL/origin, content digest, fetch time, taint, immutable import receipt, source ID and deletion behavior.
- **Phase 1 / Local Brain:** local-only summarization of reviewed fetched text after verification; no model-controlled network requests.
- **Phase 2 / Android Layer:** implement a Research surface, native source adapters, consent UX, safe bounded transport, failure states and opt-in Knowledge import. This is the **current implementation batch**.
- **Phase 3 / Observation:** user-visible adapter health, sources reached, failures, latency, and policy denials without logging fetched private content.
- **Phase 5 / Knowledge Evolution:** deduplication, cited multi-source retrieval, contradiction checks, FTS/vector indexing, user-reviewed filing.
- **Phase 6 / Security:** SSRF/DNS/redirect adversarial tests, HTML injection and resource-exhaustion tests, identity isolation, egress authorization regression, malicious tool text handling.
- **Phase 7 / Delivery:** Android build and on-device tests; measured extraction quality, network costs, battery, UI accessibility, accessibility under airplane mode, citation fidelity, content deletion, and app-update safety.

## Implementation batches and gates

- **A — Source contracts and transport:** native bounded retrieval, typed results, allowlist rules, redirect and size caps, readable diagnostics. `[DESIGNED]`
- **B — First native adapters:** direct public HTTPS text/HTML and public GitHub repository metadata. `[DESIGNED]`
- **C — User research workflow:** enter URL, show actual host, confirm fetch, see result/provenance, explicitly save in Knowledge. `[DESIGNED]`
- **D — Safe extraction and tests:** script/style isolation, HTML entities, HTML/plain-text, misleading links, invalid content types, redirects, private/localhost/DNS cases; unit tests with deterministic local fixtures. `[DESIGNED]`
- **E — Provider expansion:** configurable SearXNG or hosted reader only after privacy notice, provider-specific consent, network cost review, auth/secret vault, and provenance tests. `[DESIGNED]`
- **F — Full research reports:** multi-source planning, cited answers, synthesis review and export. `[DESIGNED]`

**Acceptance checklist:** no unapproved network activity; every result has a source and digest; no arbitrary third-party redirects; no cookie/header leakage; no unauthorized model-tool bridge; bounded downloads and extraction; Knowledge save is explicit and deletion works; UI handles offline/errors and keyboard; tests and a real signed APK run successfully before marking `[BUILT]` or `[VALIDATED]`.

**Evidence note:** research/design review complete for this addendum; code and actual Android acceptance remain separately tracked. Research links: https://github.com/Panniantong/Agent-Reach, https://github.com/unclecode/crawl4ai, https://github.com/jina-ai/reader, https://github.com/assafelovic/gpt-researcher, https://github.com/microsoft/playwright-mcp, https://github.com/browser-use/browser-use, https://github.com/searxng/searxng, https://github.com/firecrawl/firecrawl.


### 2026-11-23 clarification — conversational research is the primary UX

**User directive:** Do not require people to paste source URLs. A user should be able to instruct the local assistant in ordinary language to perform research, compare what it discovers, and propose what belongs in their local Knowledge database. The direct URL fetcher remains an optional expert tool, never the primary flow.

**Permission semantics:** Sending an explicit “Research X” instruction authorizes a *bounded* public-source search in the named/visible provider scope for that request. This must not imply permission for arbitrary web browsing, account access, authenticated sessions, future recurring searches, paid APIs, or background monitoring. An instruction to save findings authorizes presenting a batch import proposal; the user can review the selected sources and confirm import once, rather than approving every URL independently. Trusted memory promotion remains separately review-gated.

**Target conversational pipeline:**
1. Interpret the user's research task, preferred sources, research questions, and save targets; show network/provider scope and permission.
2. Search multiple approved providers by topic and discover candidate sources automatically.
3. Fetch selected sources under size, redirect, rate-limit, privacy and SSRF constraints.
4. Ask the local model to produce a draft with source markers and explicit uncertainty; never allow source text to control tool execution.
5. Show editable proposed findings and source-linked citations; allow bulk review.
6. Save original source records and separately labeled *unverified AI drafts* in the local Knowledge vault; confirmed memory is never silently modified.
7. Respect “only these sources”, “do not save”, “save summaries”, “compare licenses”, and deletion instructions. Future recurrent search requires separately authorized scheduling.

**Current code slice [IMPLEMENTED IN SOURCE, NOT YET BUILT/VALIDATED]:** GitHub topic search using the public Search API (bounded to eight public results); optional direct URL fetch; a Chat directive router for explicit “Research …”, “Search the web for …” and “Research … and save findings to Knowledge” commands; source selection with one batch-save confirmation; local model synthesis when a compatible model is loaded; editable unverified Markdown draft import with source links. Search only targets GitHub at this stage; “search the web” is **not** a promise of general cross-site search. The app displays this boundary. This is deterministic, scoped orchestration, not general-purpose autonomous browsing. GitHub API rate limits and lack of authentication apply.

**Next batches [DESIGNED]:** provider interface for broad web search (user-configured SearXNG/Brave or another privacy-reviewed service), independent extraction adapters, multi-step local research planning, title/content citation validation, per-source selection policies, deduplication, search caching, source quote alignment, safe handling of conflicting sources, persistent permission grants with expiry, research progress/cancellation, full integration tests, and real APK/device tests.

**Required checks before shipping:** user can request search without a URL; no implicit network on ordinary chat; no unapproved search on navigation or recomposition; rate-limited provider errors are visible; bulk save is reviewable and never overwrites existing data; drafts remain labeled unverified; fetched text cannot execute tools; all source URLs and hashes survive save/export/delete; offline knowledge operations continue working.


---

# ADDENDUM (2026-11-23): BIDIRECTIONAL CHATGPT ↔ ZALMOXIS AGENT BRIDGE

**User requirement:** ChatGPT must be able to send tasks/messages directly to the on-device Zalmoxis agent and receive responses; Zalmoxis must be able to initiate a request to ChatGPT and receive a response. Ordinary copy/paste is a fallback, not the destination architecture. This preserves all earlier memory, privacy, scoped delegation, security, agent, and launch constraints. **Status: architecture [DESIGNED]; portable handoff and bridge protocol source may be implemented incrementally; remotely connected real-world two-way operation is NOT verified.**

## Supported paths, distinctly labeled

1. **Local Android handoff [first Android slice]:** explicit Send to ChatGPT via Android Sharesheet, and Receive from ChatGPT via Android ACTION_SEND, with a visible, editable pending message and an acceptance step. This is *user-mediated, not autonomous or live connectivity* and needs no server account, OAuth, tokens, or broad privileges.
2. **Direct secure bridge [target]:** a hosted remote Model Context Protocol (MCP) server registered by the owner in ChatGPT, plus an Android-side authenticated client/polling/WebSocket channel through a secure relay. Public internet is used only when the bridge is explicitly connected; local inference stays local. The bridge is a broker of messages and **approval requests**, never raw device control.
3. **App-initiated cloud assistant requests [optional]:** Zalmoxis can call an authorized, user-configured OpenAI API integration via a backend-protected credential when explicitly enabled. Consumer ChatGPT sign-in is **not** equivalent to an OpenAI API key. Never ship an API credential in the APK, GitHub, or the message body. Respect cloud model costs, privacy disclosures, quotas and clear off switches.

## Requirements

- **Pairing and identity:** explicit pairing from Zalmoxis Settings; OAuth on remote MCP side; per-device short-lived enrollment; scoped access tokens bound to a user/device; local secret held in Android Keystore; revocation from phone and server. No unauthenticated or publicly writable MCP tools. A static public token in README, app code, query string, or source link is prohibited.
- **Message envelope:** immutable UUID, protocol version, direction, sender, recipient, conversation/task ID, creation/expiry timestamp, message type, content, correlation/reply-to ID, capability scopes, source references, digest and delivery/approval/acknowledgement state. Validate max lengths and total sizes. Replay IDs must be rejected; outbox retries idempotent. Display chronological messages and failures.
- **Capabilities:** `send_message`, `fetch_agent_status`, `get_reply`, `propose_knowledge_change`, `request_user_approval` are separately declared. No default filesystem, contact, microphone, app control, shell, accessibility, account tokens, or arbitrary network execution. All tool proposals are routed through existing deterministic permission/policy logic.
- **Human approval:** incoming content is untrusted agent output. Zalmoxis must not treat ChatGPT-generated text as a privileged command, and ChatGPT must not treat retrieved local documents as untrusted instructions to execute. UI shows sender, intent, target, data crossing device boundary and specific action. Allow approve once, grant bounded permission with expiry, reject, or revoke. No silent promotion to confirmed memory.
- **Device privacy:** message sharing is explicit; disabled bridge means zero background transfer. Encrypted network only; logs redact message bodies; local history deletion/export, server retention TTL and remote-deletion contract. Never transmit personal model context or Knowledge corpus automatically.
- **Connectivity:** offline queue limited by size and age; backoff, metered-network preferences, battery/foreground behavior, delivery acknowledgement and conflict handling; no claim of remote reachability if phone is offline.
- **Conversation UX:** a dedicated **Agent Bridge** workspace/settings surface with ChatGPT status, pending inbox, sent history, scoped grant management, share/handoff, and explicit task submission. Distinguish "Shared via Android" from "Connected (MCP relay)".
- **Security testing:** forged shares, exported component attacks, intent spoofing, replay, token theft, malicious messages, prompt injection, scope escalation, SSRF, relay compromise, host verification, leak/noise testing, device offline and account disconnect. Require a working APK/device test before saying the bridge is operational.

## Phases and delivery gates

- **Phase 0 / Memory:** messages, approvals, revocation, provenance and deletions with stable IDs, no conflation with memory.
- **Phase 2 / Android:** implement portable two-way human-mediated Android share/handoff and approval inbox, then authenticated secure transport with a connection toggle and offline queue.
- **Phase 3 / Monitor:** connector status, per-direction message trace, delivery receipts, latency and rejection statistics without content leakage.
- **Phase 5 / Knowledge:** source-linked research requests and drafts; reversible, reviewed import only; conflict-aware reconciliation.
- **Phase 6 / Security:** OAuth/MCP access control, signed/short-lived pairing, origin binding, replay resistance, tainted content, emergency disconnect and adversarial tests.
- **Phase 7 / Delivery:** working hosted MCP endpoint connected in ChatGPT, Android client paired on phone, successful 10+ bidirectional round trips across online/offline transitions, revocation and malicious-message tests, latency and battery validation.

**Specific connection requirements:** the ChatGPT product must have an eligible custom remote MCP plugin connected by the user; the secure bridge server must be deployed and reachable; the phone must have installed an APK containing the client and be paired. Neither source-code commits nor an Android Sharesheet alone establishes live two-way ChatGPT communication.

References: https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt ; https://developers.openai.com/api/docs/guides/custom-mcp-server ; https://developers.openai.com/api/docs/guides/tools-connectors-mcp ; https://developer.android.com/training/app-links/about .


---

# ADDENDUM (2026-11-23): API-KEY-FREE MULTI-PROVIDER AGENT BRIDGE — DEEPSEEK REQUIRED

**Directive / priority:** Make DeepSeek a **first-class mandatory integration target**, alongside ChatGPT, Claude, and Mistral, for two-way communication with Zalmoxis without requiring the user to obtain or paste a commercial model API key. Continue with the common secure bridge architecture already defined above. Append-only extension: no prior phase, security rule, or existing roadmap item is replaced.

**Current evidence status: [RESEARCHED] for documented upstream mechanisms; [DESIGNED] for integration; NOT IMPLEMENTED as an operational remote bridge; NOT TESTED on the user's Android phone.** No API key does **not** mean no account, login, subscription, OAuth token, internet, hosting, or rate limits. No provider may be represented as connected before a real round trip succeeds.

## Key DeepSeek discovery: OFFICIAL DeepSeek Harness account login + MCP

The official [DeepSeek Harness](https://github.com/deepseek-ai/deepseek-harness) is the **preferred keyless route**. Its official packages document:

- [DeepSeek account-backed model route](https://github.com/deepseek-ai/deepseek-harness/blob/master/packages/llm/llm-deepseek-account/README.md): the `deepseek-account` model provider uses a DeepSeek account sign-in supplied by the account service, and sends `x-dsh-auth-token`. It does **not** fall back to the DeepSeek API key. Model availability and request permissions depend on the signed-in account; a signed-out or ineligible account returns `ACCOUNT_SIGN_IN_REQUIRED`.
- [Account/sign-in service](https://github.com/deepseek-ai/deepseek-harness/blob/master/packages/account/deepseek-account/README.md) and [account platform credential plugin](https://github.com/deepseek-ai/deepseek-harness/blob/master/packages/credentials/deepseek-account-platform/README.md): user authorizes in the system browser; the host holds credentials and exposes status without handing credentials to arbitrary tools. Confirm exact login and account eligibility on deployed Harness build.
- [Official DeepSeek MCP client](https://github.com/deepseek-ai/deepseek-harness/blob/master/packages/mcp/mcp-client/README.md): connects tools from approved local stdio or remote Streamable HTTP MCP servers to the harness agent. Configure a Zalmoxis relay endpoint as **one named server**, scope exposed tools and validate the server security/auth requirements.
- [DeepSeek authentication service](https://github.com/deepseek-ai/deepseek-harness/blob/master/packages/account/deepseek-auth/README.md): separates account and key credentials per route, limits destinations and invalidates rejected/revoked grants. Use the explicit account route; never silently switch to an API-key-billed route.
- [DeepSeek web Search/Fetch](https://github.com/deepseek-ai/deepseek-harness/blob/master/packages/web/web-deepseek-api/README.md) and [Search adapter](https://github.com/deepseek-ai/deepseek-harness/blob/master/packages/web/web-search-deepseek/README.md): some search/fetch operations support account-authenticated conversations. Other APIs may remain API-key-only. Feature-gate per operation; do not promise unqualified keyless support for every DeepSeek product.

**Important distinction:** Official DeepSeek API docs at https://api-docs.deepseek.com/api/deepseek-api/ still require an API key for ordinary API calls. The Harness account route is a **separate supported authentication path within Harness**, not an anonymous public API and not proof that any Android application can directly reuse consumer DeepSeek web credentials. DeepSeek's standard consumer web/mobile chat is not independently verified to expose custom MCP connectors. The host where Harness runs must be protected, paired with Zalmoxis, and reachable via the approved relay. No user-facing credential extraction.

### DeepSeek connection candidates, ranked

| Priority | Method | API key? | What it provides | Limitations / verdict |
| --- | --- | --- | --- | --- |
| **P0 mandatory** | **Official DeepSeek Harness + official DeepSeek account sign-in + MCP client → Zalmoxis secure relay** | **No model API key**, authenticated DeepSeek account required | Real model-to-Zalmoxis task calls and responses when Harness supports account model and remote MCP | **BUILD/VALIDATE FIRST**; requires trusted hosted Harness or owner's machine, account eligibility, relay, OAuth/device pairing, and explicit session/agent execution |
| **P0 fallback** | User-mediated Android Sharesheet → official DeepSeek chat app/site → Zalmoxis share/import | No | Immediate message exchange with human approval | **SUPPORTED DESIGN**, not automatic round trips; DeepSeek must be selectable/share text through available Android UI |
| **P1 resilient** | DeepSeek-R1 distilled open weights via compatible local inference runtime | No | Local DeepSeek-family assistant; no commercial service dependency | **EVALUATE**, not remotely connected consumer DeepSeek account; verify exact model packaging, phone RAM/performance, Android LiteRT-LM compatibility and source/derivative licenses |
| **P2 experimental** | User-owned browser session bridge, e.g. https://github.com/nezumi0627/deepseek-web-harness | No DeepSeek API key, but logged-in browser session | Unofficial DeepSeek Web automation with MCP-like interface | **RESEARCH ONLY / NOT DEFAULT**: breaks on UI updates, session compromise risks, likely desktop-host requirement; no cookie export or auth bypass; legal/terms review mandatory |
| **Not acceptable** | Reverse-engineered DeepSeek private endpoints, stolen/extracted cookies, proxy bypassing verification, distributing someone else's API key | Not legitimately | Unreliable/unsafe | **REJECT** |

**Local model reference:** https://huggingface.co/deepseek-ai/DeepSeek-R1-Distill-Qwen-14B documents R1 Qwen-distilled model family and MIT model weights; Qwen-derived and Llama-derived variants have different underlying license obligations. Use the smallest compatible quality-tested variant, and **do not claim GGUF/SafeTensors models run in the currently LiteRT-LM-only Zalmoxis runtime** without adding/converting/testing an appropriate runtime.

## Universal provider adapter plan

**One Zalmoxis bridge, multiple authenticated entrypoints**. Preserve separation of: (A) provider-agent → Zalmoxis via provider's MCP tools; (B) Zalmoxis → provider model via an officially supported, user-authorized provider route; and (C) provider-event → agent-session proactive processing, only where the provider explicitly supports event-driven execution. A connected MCP server alone cannot wake a dormant consumer AI conversation.

| Provider | API-key-free candidate | Integration decision | Restrictions to verify |
| --- | --- | --- | --- |
| **DeepSeek — REQUIRED** | **Official DeepSeek Harness account login + MCP** | **P0 primary acceptance target** | Account-backed route eligibility, model quota, Harness hosting, remote MCP transport and safe pairing |
| ChatGPT / OpenAI | Eligible Sign in with ChatGPT OAuth for model requests; custom remote MCP for conversation tools | **P0** two distinct paths | Eligibility, account/product availability, MCP tool permissions, session trigger; no claim of access to existing ChatGPT chats |
| Claude / Anthropic | Account-based custom remote MCP connector | **P0** | Free-plan connector cap, account features, remote endpoint authorization and whether reads/writes allowed |
| Mistral Vibe | Account-based remote MCP connector | **P0** | Supported plan/device/tools; test OAuth and scopes |
| Perplexity | Account-based custom remote MCP | **P1** research agent | Subscription tier and write capabilities |
| Grok / xAI | Reported account MCP connector | **P1** after first-party validation | Region, account tier, actual tools, OAuth |
| Gemini / Google | Custom Gemini apps/MCP where eligible | **P2** region-gated | Current geography/account requirements and platform policies |
| Microsoft Copilot Studio | Organization-authenticated MCP connectors | **P2** enterprise | Tenant licensing and administrative approvals |
| Self-hosted DeepSeek/open-weight models | Local inference / self-hosted agent | **P0 offline fallback** | Hardware, compatibility, license, resource costs; not consumer DeepSeek web service |

**Do not confuse account-powered inference (Zalmoxis asks an AI model) with remote assistant-to-assistant transport (a provider agent sends tasks to Zalmoxis).** Both sides need a protocol message queue and authorization; some providers support only one direction.

## Shared secure relay specification (provider-neutral)

- Remote HTTPS MCP server exposes **only** `bridge_status`, `send_task`, `list_pending_replies`, `read_reply`, and `request_approval`. Providers never get unconstrained filesystem, microphone, Accessibility, shell, network, or Knowledge write methods. A `propose_knowledge_change` operation creates review-only proposals.
- Account/device pairing uses OAuth or equivalent supported authorization, short-lived scoped sessions, token rotation/revocation, per-user/device separation, replay protection, timestamps, idempotency keys and expiry. Local device secrets remain in Android Keystore. DeepSeek Harness account token must remain within its trusted Harness auth service, **not** forwarded to the Zalmoxis relay.
- Phone-originated requests use explicitly selected provider routes. DeepSeek account-backed requests should execute in a trusted Harness host via a restricted provider adapter. Never embed account tokens or model/API secrets in APK, URLs, public GitHub builds, or source logs.
- Relay stores only minimal encrypted payloads with TTL, correlation IDs and acknowledgements. Authz is validated server-side. A connected but idle provider does **not** imply guaranteed real-time response. For bidirectional automatic turns, use a provider-acknowledged task worker or explicitly supported event subscription, not repeated unsolicited messages to ordinary chat sessions.
- UI must show provider, bridge health/last reply, model/account authorization kind (**Account login**, **API key**, **Local**) without secret contents, task history, which content leaves the phone, user permissions, cost/quota limits, expiry, errors and **Disconnect**.
- Each provider adapter has capability discovery and test cases: account linked, list models, invoke a harmless task, observe signed result, disconnect/revoke, offline recovery, and malicious input handling. Do not equate working DeepSeek Harness MCP tools with support in consumer DeepSeek Chat.

## Execution order (additive to existing phases)

1. **Gate D0 — Verify official DeepSeek account route**: check DeepSeek Harness supported version, login in trusted browser, prove a model response with **no `DEEPSEEK_API_KEY` configured**, record account/quota restrictions and distinguish ordinary API docs. Failure keeps the DeepSeek integration marked experimental.
2. **Gate D1 — Implement shared secure relay**: authenticated remote MCP, structured two-way envelopes, protected queues, access control, expiration and revocation.
3. **Gate D2 — DeepSeek Harness adapter**: connect the official `dsh-mcp-client` to the Zalmoxis relay and verify model-initiated tool calls and replies with no DeepSeek API key; verify Zalmoxis-originated task → Harness worker → account-backed DeepSeek model → reply **only if supported by an explicit authenticated orchestration worker**.
4. **Gate D3 — Other providers**: ChatGPT, Claude, Mistral, Perplexity and further providers use same envelope, auth policy, and tests; do not reimplement transport separately.
5. **Gate D4 — Android UX and local fallback**: pairing QR/deep link, keyless share mode, provider status, approvals, preview and offline DeepSeek-distill capability only when a measured, supported Android model passes runtime gates.
6. **Gate D5 — Deployment validation**: signed APK and live relay. Ten or more genuine task/reply round trips *per provider/direction*, cold start, outage and reconnect, deny/revoke, replay and prompt-injection tests; collect delivery latency, relay retention, quota and error rates. Mark the provider connected only after these pass.

**Deployment acceptance requirement:** A user can select DeepSeek, connect with supported account login (no model API key), request a task from Zalmoxis, receive a traceable answer, and send an appropriately authorized task back to the on-device agent without manually copying text—subject to active compatible Harness/agent session. If this cannot be demonstrated, explain the failure and retain the user-mediated DeepSeek handoff; never mislabel it as autonomous, direct or live.

**Sources consulted (Nov 2026):** https://github.com/deepseek-ai/deepseek-harness ; https://github.com/deepseek-ai/deepseek-harness/blob/master/packages/llm/llm-deepseek-account/README.md ; https://github.com/deepseek-ai/deepseek-harness/blob/master/packages/account/deepseek-auth/README.md ; https://github.com/deepseek-ai/deepseek-harness/blob/master/packages/mcp/mcp-client/README.md ; https://github.com/deepseek-ai/deepseek-harness/blob/master/packages/web/web-search-deepseek/README.md ; https://api-docs.deepseek.com/api/deepseek-api/ ; https://github.com/nezumi0627/deepseek-web-harness ; https://huggingface.co/deepseek-ai/DeepSeek-R1-Distill-Qwen-14B .


---

# ADDENDUM (2026-11-23): REUSE ESTABLISHED MCP, OAUTH AND A2A INFRASTRUCTURE

**Mandatory architecture correction after upstream due diligence:** Before further implementation, **reuse the official Model Context Protocol SDK, Cloudflare Agents MCP runtime and Workers OAuth Provider rather than implementing the MCP protocol and OAuth security by hand**. The first custom relay prototype in `bridge/src/index.js` is **not approved for production deployment**. Android manual Sharesheet remains a fallback. This addendum does not delete previous phases or DeepSeek requirements.

**Approved reuse stack:**
- https://github.com/modelcontextprotocol/typescript-sdk — official, spec-compatible MCP server/client and tools/transport handling (v2 for 2026-07-28 MCP).
- https://github.com/cloudflare/agents — Cloudflare remote MCP Worker integration and supported handler.
- https://github.com/cloudflare/workers-oauth-provider — OAuth 2.1 provider/resource-server authentication, pairing authorization, protected-resource metadata, clients and token validation.
- https://github.com/a2aproject/A2A and https://github.com/a2aproject/a2a-js — official agent-to-agent task/status/artifact vocabulary; use for Zalmoxis/provider task semantics. A2A HTTP endpoints are a future adapter when official interoperability tests pass.
- https://github.com/deepseek-ai/deepseek-harness — **mandatory** account-authenticated DeepSeek agent and MCP client validation. https://github.com/hyqhyq3/dsh-mcp-manager is a community OAuth/PKCE compatibility option requiring separate review.

**Future infrastructure candidates:**
- https://github.com/agentgateway/agentgateway — agent-native MCP+A2A security, routing and observability at multi-provider scale; defer until needed.
- https://github.com/IBM/mcp-context-forge — MCP+A2A governance/federation; defer due to operational complexity.
- https://github.com/supercorp-ai/supergateway and https://github.com/sparfenyuk/mcp-proxy — MCP stdio/Streamable HTTP adaptation only, not authorization.
- https://github.com/open-webui/mcpo — OpenAPI translation where an agent client requires it; not default.
- https://github.com/pipelab-org/pipelock — optional defense-in-depth inspection, not a replacement for permissions.
- https://developers.cloudflare.com/agents/model-context-protocol/cloudflare/mcp-portal/ — optional managed federation.

**Next bridge implementation order:**
1. Freeze bespoke hand-coded public MCP endpoint. Keep its queue and phone client as non-production integration scaffolding.
2. Bootstrap authenticated remote MCP server from first-party Cloudflare template; use official MCP tool schemas and OAuth implementation; do not embed provider tokens in code or APK.
3. Implement scoped device pairing and outbound Android HTTPS connectivity with durable queue, TTL, rate limits, grant expiry, revocation and replay tests.
4. Connect DeepSeek Harness first, with **account sign-in (not DeepSeek API key)** and verify both directions. Check MCP client OAuth compatibility; don't claim current consumer DeepSeek chat can be used as an MCP provider until verified.
5. Reuse the same bridge for ChatGPT, Claude and Mistral according to their actual account capabilities; add A2A compatibility using the official SDK for autonomous task lifecycle as supported.
6. Automated MCP conformance, OAuth/authorization, tenant isolation, mobile offline/reconnect, malicious message and two-way delivery tests; 10+ successful live round trips for each supported provider before deployment.

**Detailed decision and tradeoffs:** `docs/decisions/MCP-BRIDGE-REUSE-ASSESSMENT.md`. **Evidence level:** Upstream research and architecture accepted; secure production MCP migration, live relay deployment, DeepSeek sign-in, Android APK build and real conversations are NOT complete.


---

# IMPLEMENTATION RECORD (2026-11-23): OFFICIAL SDK AGENT BRIDGE, ANDROID TRANSPORT AND DEEPSEEK HARNESS

**Evidence status: [IMPLEMENTED IN SOURCE — NOT YET BUILT/CONNECTED/DEPLOYED].** This is an additive progress entry, not a claim that existing security or phase gates have passed.

## Completed source components
- **Standards-based MCP resource server:** `bridge/mcp-server/src/index.ts` uses official MCP TypeScript SDK v2, Cloudflare Agents handler and OAuth resource middleware. Tools: `create_pairing_code`, `bridge_status`, `send_task`, `list_phone_requests`, `list_replies`, `reply_to_phone`, `revoke_bridge`. Read/write scope authorization is separated. OAuth user and MCP client identity bind the task queue; one provider cannot read another's messages. No model API key.
- **OAuth service:** `bridge/auth-server/src/index.ts` uses Cloudflare Workers OAuth Provider v1, validates Cloudflare Access JWT signature/issuer/audience, requires explicit consent click and passes verified user ID through official OAuth grants. It has separate Worker/KV configuration and must be deployed with correctly configured Access restrictions.
- **Android phone side:** `AgentBridgeClient.kt` supports HTTPS pairing, Android-Keystore AES-GCM credential storage, send/receive, reply/reject, and server-confirmed revocation. `AgentBridgeWorkspace.kt` exposes pairing, scoped inbox/outbox, explicit human replies, 15-second foreground-view polling, and Sharesheet fallback.
- **Durable relay:** `bridge/mcp-server/src/phone.ts` uses the D1 schema in `bridge/migrations/0001_init.sql`, `0002_oauth_pairing.sql`, `0003_client_isolation.sql`, with one-time 16-hex-character codes valid 15 minutes, per-IP attempt throttle, separate device bearer, 24-hour queue TTL, scheduled cleanup, revoke and delete.
- **DeepSeek first:** `bridge/deepseek/README.md` configures the official DeepSeek Harness MCP Manager, account-backed route, OAuth login and phone pairing with NO DeepSeek model API key. Not yet tested against a live DeepSeek Harness session.
- **Maintenance:** `.github/workflows/bridge-check.yml` installs SDK dependencies, type-checks both Workers, runs Node phone-API tests and bundles each Worker without deploying. A [CI run passing] result has not yet been observed. The obsolete custom bridge endpoint in `bridge/src/index.js` now fails closed unless intentionally enabled with an unsafe development flag.

## Explicit open gates
1. Configure Cloudflare identity/Access restrictions, deploy OAuth and MCP Workers, create KV+D1, and apply all three migrations using a trusted owner account.
2. Validate OAuth metadata, PKCE, consent, scopes, owner/client isolation, pairing brute force denial, remote revocation and signed Android APK.
3. Demonstrate real two-way DeepSeek Harness tasks using its account-auth model route, with no DeepSeek API key, then ChatGPT/Claude/Mistral using compatible custom connectors.
4. Add **multiple simultaneously paired Android providers** and provider selection/isolated UI (current Android implementation pairs only one active channel).
5. Add push/background delivery with explicit opt-in and battery constraints, approved task execution integration, durable offline outbox and idempotent retry. Current UI polls only while its screen is open.
6. End-to-end encrypt message payloads if private content is expected; D1 currently holds task/reply bodies as plaintext with TTL. A hosted signed relay is not equivalent to E2E encryption.
7. Observe CI build success, real phone installation, OAuth sign-in and at least ten real round trips each direction before labeling anything [BUILT] or [VALIDATED].

**Current boundary:** A secure source-code architecture and user-controlled messaging UI exist; no live hosted MCP URL, provider credential, or mobile installation was established in this conversation. Do not present a connected or tested DeepSeek/ChatGPT conversation until the actual runtime evidence exists.


---

# ADDENDUM (2026-11-23): DUAL MCP DEPLOYMENT — ON-DEVICE AND INTERNET-HOSTED

**User-approved requirement (mandatory, additive):** Zalmoxis must offer **both (1) an MCP server running locally inside the Android application and (2) an internet-hosted MCP option**. These are first-class supported connection modes, not mutually exclusive plans. The user must be able to choose one, the other, or both without being forced to create a Cloudflare account for local use. Retain earlier official SDK reuse, DeepSeek-first requirements, and the hosted OAuth/MCP infrastructure. No existing roadmap phases are removed or replaced.

## Supported modes

| Mode | MCP server runs | Endpoint and network | Primary use | Requirements / limits |
| --- | --- | --- | --- | --- |
| **Local device [REQUIRED]** | **Inside Zalmoxis Android** | Loopback `127.0.0.1` only by default; opt-in LAN binding with authentication | On-device tools, local/offline integration, compatible same-device or LAN agents | Works without an external host or model API key. Android process/foreground-service lifetime and device network must be managed |
| **Direct remote to device [OPTIONAL, REQUIRED FEATURE]** | Inside Zalmoxis Android | User-activated **authenticated HTTPS tunnel** forwards to local server; outbound connection from phone | Remote ChatGPT/Claude/Mistral/DeepSeek Harness MCP clients while phone is connected | Tunnel provider/relay and identity credentials may be necessary; public remote MCP requires supported OAuth/authorization and protocol-compliant HTTPS transport. No unrestricted raw port exposure |
| **Internet hosted [REQUIRED]** | Hosted official MCP server + message broker | Public HTTPS endpoint, OAuth, paired Android client | Durable messages and reconnect across mobile networks; access when phone is temporarily offline | Cloudflare Workers + OAuth Provider + D1 implementation already in source as one supported host; external account setup and deployment required |
| **Hybrid [REQUIRED]** | Local MCP server and hosted broker together | Local tools stay on phone; hosted broker carries scoped encrypted messages / offline queue | Convenient on-phone inference + reliable remote inter-agent exchange | Must avoid duplicate execution and overlapping permissions; one central local approval policy |

## Primary Android implementation

- Use the [official MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk) (Ktor/Streamable HTTP support) with a version compatible with Android minSdk 26, Kotlin/Compose build configuration and current HTTP server runtime. Do not implement another custom JSON-RPC/MCP protocol.
- An Android **Agent Bridge → MCP Server** panel offers: `Start local server`, `Stop server`, status, loopback endpoint and port, permitted capabilities, connected clients, request/approval log, pairing/revoke, opt-in LAN access and opt-in remote tunnel.
- **Local default:** loopback-only listening, least-privilege read-only tools, no Internet requirement and no internet-hosted relay dependency. Random/nonconflicting listening port or stable configurable port with conflict diagnostics. Neither `0.0.0.0` nor port forwarding is enabled silently.
- **Server lifecycle:** explicit Android foreground service with visible notification and persistent on/off state where Android policy permits; stop action must close listener and revoke outstanding ephemeral capabilities. If Android terminates the app or battery-saving policies stop the service, UI must show server offline rather than claiming always-on.
- **Initial capabilities:** `status`, `list_capabilities`, `submit_task`, `get_task_status`, `read_reply`, `propose_knowledge_import` (review-only). Later `search_local_knowledge` and `run_local_inference` require separate granular opt-in and limits, explicit user-visible data disclosure and a deterministic permission gate. No shell/filesystem/microphone/contacts/accessibility access by default.
- **Security:** authentication required for LAN and remote access, even if device owner is the only user. Per-client authorization, nonce/replay protection, rate/size bounds, approval for side-effecting operations, input taint and prompt-injection barriers, origin validation, audit and expiry. Loopback controls still prevent arbitrary local apps from accessing privileged operations; do not confuse a localhost socket with authorization.
- **Remote tunnel:** investigate reusable secure connectors (e.g. Cloudflare Tunnel or Tailscale Funnel) and compare official Android feasibility, unattended reliability, cost, URL stability and OAuth interoperability. Prefer a user-initiated outbound authenticated connector over inbound NAT/port-forward access. Protect public remote MCP via OAuth 2.1 and validated resource scopes; a random URL is **not** an auth mechanism.
- **Offline operation:** local MCP works without hosted infrastructure, but only local-reachable clients can access it. Remote cloud-hosted AI conversations cannot reach a private loopback socket without a secure routing path. Local inference stays on device in every mode.

## Hosted and hybrid implementation

- Preserve the existing `bridge/auth-server`, `bridge/mcp-server`, `bridge/migrations`, Android `AgentBridgeClient` and paired message UI as the **hosted mode**. Existing source compilation checks do not establish a deployed, connected service.
- Implement a **common backend-neutral `AgentBridgeTransport` contract** with `LocalMcpTransport` and `HostedMcpTransport`; the same structured message envelope, correlation IDs, task status, capability declarations, approval engine, history and revocation rules apply across modes. Never execute one task twice when local and hosted transports are active.
- Add a mode selector: **Local**, **Hosted**, **Hybrid**, **Off**. Changing modes must clearly explain whether any message leaves the phone, which host is used, and which provider can reach the device.
- Hosted broker supports offline queues and TTL; phone reconnect synchronizes acknowledged task states with idempotent IDs and deletion. **Encrypt sensitive payloads end to end before storing them remotely**; current D1 plaintext messages are not suitable for automatic private Knowledge sharing.
- Multi-provider channels remain isolated (provider account/MCP client + user + phone). Support ChatGPT, DeepSeek Harness (official account-backed path, **mandatory**), Claude and Mistral without embedding model API keys, conditional on supported client account permissions. Account login/OAuth/hosting credentials are still allowed and must be stored securely.
- Both local and hosted servers advertise protocol version and capability scope; clients distinguish **server online**, **device online**, **provider agent responding**, and **local model loaded**.

## Implementation order / milestones

1. **MCP-L1 — Feasibility and SDK selection:** inspect official Kotlin SDK version, Android compatibility, Ktor embedded server runtime, production licensing and minSdk; make dependency choice based on build and actual device measurements.
2. **MCP-L2 — Embedded server:** implement service lifecycle, loopback Streamable HTTP MCP endpoint, typed tools and capability discovery; add **Start/Stop** UI and a visible notification; fail closed on errors.
3. **MCP-L3 — Common permission engine:** authenticated clients, bounded requests, approval inbox, audit trail and no automatic Knowledge writes. Fuzz and isolation tests.
4. **MCP-L4 — Opt-in remote access:** secure outbound tunnel and OAuth-protected remote connector; prove clients can connect over mobile data with secure pairing and revocation.
5. **MCP-H1 — Hosted deployment:** deploy validated Cloudflare OAuth + MCP Workers and D1, provision secure credentials, connect Android and verify queued messaging.
6. **MCP-H2 — Hybrid routing:** switch/select modes, idempotent deduplication, safe failover, encrypted messages and observable task lifecycle across reconnects.
7. **MCP-P — Provider acceptance:** live DeepSeek Harness and ChatGPT/Claude/Mistral tests according to actual plan access, including both directions, no model API key, scope denial, account sign-out and offline transitions.
8. **MCP-D — Deployment proof:** signed Android APK build, physical-device tests, at least ten valid bidirectional exchanges for each supported mode/provider, security review, reliability and battery measurements, user-facing instructions accessible from a phone.

## Release acceptance

- One tap starts an MCP server **inside Zalmoxis** without asking for Cloudflare or a commercial model API key.
- Local endpoint stays bound to loopback unless the user explicitly opts into secure LAN/tunnel access.
- An authenticated MCP client can discover permitted tools, request a task and obtain a reviewed response; unauthorized or injected requests do not execute.
- Hosted endpoint separately supports remote account-authenticated tools and durable task delivery; revocation works.
- Hybrid simultaneously supports on-device inference and hosted message delivery **without duplicate execution or leaking private data**.
- UI clearly shows connection mode and real measured reachability; stop/disconnect reliably tears down access.
- Never mark this addendum [IMPLEMENTED] or [VALIDATED] until the Android embedded MCP server exists, compiles, and passes actual local/remote device tests.

**Current state at addition:** [PLANNED] for on-device MCP server, optional tunnel and hybrid routing; [IMPLEMENTED IN SOURCE / CI GREEN, NOT DEPLOYED] for hosted Worker infrastructure and Android HTTP client. This addendum corrects prior cloud-first assumptions: **internet hosting is optional for local functionality, and both paths must be supported.**


### 2026-11-23 local MCP implementation evidence (post-addendum)

**Implementation state: [SOURCE IMPLEMENTED + ANDROID CI ASSEMBLE PASS], [DEVICE RUNTIME NOT YET VERIFIED].** The Android build at https://github.com/andreipath26/zalmoxis/actions/runs/45223841942 passed for commit `cf99e424ed476508e2fdf2bf18ebc8357f39c6b9`, including APK artifact `zalmoxis-debug-apk` (artifact ID `14840914352`).

- `LocalMcpServerService.kt`: explicit-start Android foreground service; official Kotlin MCP SDK v0.16.0 + Ktor CIO 3.5.2 Streamable HTTP on `127.0.0.1:8765/mcp`; no LAN binding, no cloud dependency. Exposes token-gated `status`, `submit_task`, `read_reply`; requests are human-reviewed, never executed as arbitrary code. Service is non-exported and has an Android specialUse foreground notification; Google Play declaration review remains required.
- `LocalMcpPanel.kt`: Start/Stop controls, local endpoint and token view/copy, local task approval/reply screen, and visible Off/Local/Hosted/Hybrid state derived from the local listener and hosted pairing.
- `AgentBridgeWorkspace.kt`: embeds the local control panel without removing hosted OAuth pairing, remote inbox/outbox, or Android Sharesheet fallback.
- `AndroidManifest.xml`, `android/app/build.gradle.kts`: service/permission registration and versioned official MCP/Ktor dependencies.
- **Limitations**: local task queue lives in process memory and is not an offline-persistent outbox; hosted and local routes are simultaneously usable but lack automated failover, cross-transport deduplication and end-to-end encryption. Tunnel/LAN is not yet enabled. External hosted relay still needs actual deployment and credentials. The signed production APK and on-phone runtime tests remain open.

**Next device gates**: start/stop a foreground server without crashes on a representative Android handset; verify MCP initialization, tools/list, token-denial, submit/task/reply with a same-device MCP client; test orientation changes, notification dismissal, background/battery restrictions, model load, end-to-end hosted connection and E2E privacy requirements. Do not mark full Phase MCP-D deployed without these.


---

# BASELINE AUDIT (2026-11-24): VERSIONED ROADMAP PERCENTAGES — DO NOT OVERWRITE

**Immutable baseline:** This entry records the 2026-11-24 audit of Zalmoxis `main` after the Android dictation-language, system back-gesture, and bottom-navigation improvements. It preserves the full preceding V10 roadmap and all addenda. All percentages are **judgment-based engineering estimates**, not measured task counts or evidence that a feature is deployed. Do not silently inflate or overwrite them. Track subsequent changes against the original baseline in [the machine-readable ledger](docs/progress/roadmap-ledger.json), storing evidence and dated updates. The full **415-item** baseline includes workstreams, source technologies, non-negotiable constraints, cross-cutting security items, MVP gates, provider integration and Marvel architecture patterns. Its overlapping sections must **not** be added together as independent work.

## Full-roadmap snapshot

| Scope | Estimated implementation % | Remaining % | Validation caveat |
| --- | ---: | ---: | --- |
| **Entire V10 + all addenda** | **15%** | **85%** | Subjective full-scope estimate; no release acceptance |
| **Production release readiness** | **5%** | **95%** | CI green is not physical-device validation |
| Foundation | 28% | 72% | Trust policy, licensing, customer proof and release security incomplete |
| Phase 0 — Memory core | 12% | 88% | No full persistent memory / provenance and isolation engine |
| Phase 1 — Local brain | 30% | 70% | LiteRT-LM implemented; KILN, full agent loop and model performance gates missing |
| Phase 2 — Android layer | 35% | 65% | Compose app implemented; ingestion, OCR, actions, audits, security gates missing |
| Phase 2.5 — Scoped device capability | 3% | 97% | Machine-enforced authorization and safe action execution absent |
| Phase 3 — Proactivity | 3% | 97% | No proven proactive engine |
| Phase 4 — Device-cloud bridge | 18% | 82% | MCP source built; live hosted/provider round trips unverified |
| Phase 5 — Knowledge evolution | 9% | 91% | Wiki/graph/consolidation largely not built |
| Phase 6 — Security hardening | 12% | 88% | Threat model and some scoped safeguards; full audited policy enforcement missing |
| Phase 7 — Launch and iteration | 12% | 88% | Debug CI and public repo; no validated beta/reliability report |
| Phase 8 — Cross-platform/hardware | 1% | 99% | Architecture research primarily |
| Phase 9 — Defense partnership | 0% | 100% | Post-product-market-fit, deferred |

## Addenda snapshot — overlapping views, never double counted

| Addendum | Implemented | Remaining |
| --- | ---: | ---: |
| Google AI Edge Gallery adoption | 58% | 42% |
| Local Knowledge Workspace / LLM Wiki | 30% | 70% |
| Agent Reach–inspired research | 35% | 65% |
| Bidirectional ChatGPT ↔ Zalmoxis bridge | 35% | 65% |
| API-key-free multi-provider bridge (DeepSeek mandatory) | 25% | 75% |
| Dual local and internet-hosted MCP | 40% | 60% |

## Proof gates and milestones

| Gate | Estimated completion | Outstanding |
| --- | ---: | --- |
| 90-day Gate 1 — Define proof | 55% | Final memory contract, design assumptions, customer discovery |
| 90-day Gate 2 — Build core MVP | 30% | Persistent memory, document ingestion, isolation, audit, permissions |
| 90-day Gate 3 — Validate with customer | 0% | Representative customer pilot and measured outcomes |
| 90-day Gate 4 — Investment decision | 0% | Evidence-backed go/pivot/stop |
| MCP-L1 — SDK feasibility and selection | 80% | Android/device interoperability |
| MCP-L2 — Embedded Android server | 75% | Real-client lifecycle, durability and security testing |
| MCP-L3 — Common permission engine | 30% | Authorization, audit, replay and scope tests |
| MCP-L4 — Secure outbound tunnel | 0% | Implementation and validation |
| MCP-H1 — Hosted service deployment | 35% | Live cloud resources and pairing |
| MCP-H2 — True hybrid routing | 15% | Common envelope, idempotency, encrypted queue and failover |
| MCP-P — Provider acceptance | 0% | Real provider round trips and revocation |
| MCP-D — Deployment proof | 10% | Signed physical-device and end-to-end tests |

## Audit evidence and explicit gaps

- **Code reality:** `android/app/src/main/kotlin/com/zalmoxis/app/` contains Compose UI, LiteRT-LM adapter, verified model shelf, Knowledge source vault, GitHub-only research, speech, update client, on-device MCP foreground server and bridge client. `bridge/` contains hosted OAuth, MCP and D1. `core/{memory,identity,ingestion,audit,permissions}/` were scaffolding placeholders at the baseline.
- **CI evidence:** [Android build #120](https://github.com/andreipath26/zalmoxis/actions/runs/45376029421) passed on the 2026-11-24 baseline commit; [bridge validation #22](https://github.com/andreipath26/zalmoxis/actions/runs/45220379207) passed. These are compilation, source test and artifact signals, **not** real handset or hosted deployment proof.
- **Hard blockers for core MVP:** PDF/DOCX/email/OCR ingestion; persistent five-layer memory and identity/client isolation; deterministic authorization; audit chain; memory and device reliability acceptance.
- **Hard blockers for production:** security evaluation, signed-release continuity tests, real handset QA, measured latency/battery/model compatibility, live provider interoperability, hosted service security and end-to-end payload encryption.
- **Documentation conflicts:** earlier addenda and historical `PROJECT-STATE.md` contain stale source-only or no-build statuses. Preserve historical entries but treat later dated CI evidence and this baseline as the current status at 2026-11-24.
- **Licensing:** the roadmap proposes an AGPL/commercial license, but a top-level license is not currently published; do not claim a license grant that is not present.

## Ledger contract and update rules

Canonical machine-readable progress: **[`docs/progress/roadmap-ledger.json`](docs/progress/roadmap-ledger.json)**. It contains **24 groups and 415 scored entries**, each with a stable requirement ID, original baseline percentage, current percentage, estimated status, evidence array, blockers, last-reviewed date and release-validation field. Scope-specific percentages are independent estimates, not averages. The baseline stays immutable; future work changes `currentPercent` only, and must attach:

1. Evidence: source commit/file, successful CI/test URL, real-device record when relevant.
2. The acceptance requirement verified and the exact missing requirements.
3. A timestamp and reviewer, plus the reason for changing the estimate.
4. A separate implementation-complete versus deployed-and-validated conclusion.

The ledger is a **measurement baseline, not an automation that can determine software correctness from GitHub activity**. An independent verifier must still inspect test logs and check release gates. Future reporting must show baseline and current side-by-side with their delta, never replace the dated original values.

**Priority order after baseline:** P0 Android reliability and real-device tests; persistent local memory + document ingestion; source/identity scoping and isolation tests; deterministic policy engine + tamper-evident audit; then completing MCP provider connectivity, KILN runtime, proactivity and later phases. Preserve the original post-PMF deferral of defense-specific work.
