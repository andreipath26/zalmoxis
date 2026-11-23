ZALMOXIS: THE COMPLETE ROADMAP (V10 — REFINED BY FULL INDEPENDENT ASSESSMENT)

Sentinel was the former name for Zalmoxis; it is not a separate product. “Sentinel Watch” remains the name of the Phase 3 component.


Everything from V9. Every finding from the assessment integrated. Nothing removed.
PART ONE: THE PRODUCT THESIS
What Zalmoxis Is

Zalmoxis is an Android-native AI agent that remembers, reasons, and acts 
locally, while making its permissions, decisions, data access, 
and failures inspectable by the person who owns it.

This is the thesis. It is not the personalities. It is not the emotional memory. 
It is not the phone control. 
The thesis is delegation without loss of control.
What the Assessment Sharpened

The market has validated the thesis. But the market has not validated Zalmoxis's 
particular product. 
The failures of competitors establish that the problems are meaningful. They do 
not establish that customers will adopt Zalmoxis, trust its implementation, or 
pay the proposed price.

Three separate hypotheses must be tested independently:

    Hypothesis A: The problem exists — People want persistent, reliable AI memory and controlled automation

    Hypothesis B: Zalmoxis solves it better — Local memory, explicit governance and verifiable actions produce a measurably better user experience

    Hypothesis C: Customers will pay enough — The improvement is valuable enough to support the proposed subscription and the cost of developing and maintaining the product

Evidence Tags
Tag	Meaning
[DESIGNED]	Architecture specified. No implementation.
[RESEARCHED]	Grounded in published research or production-tested reference.
[BUILT]	Code exists. Tests pass.
[VALIDATED]	Tested with real users, real data, real outcomes.
[PROVEN]	Demonstrated over time with zero trust-breaking incidents.

As of this writing, almost everything is [DESIGNED] or [RESEARCHED].
PART TWO: WHAT WAS REFINED IN V10

The assessment identified specific findings that required roadmap changes. These are integrated below:
Finding	Integration
MVP scope is overloaded	MVP cut to minimum viable proof
Six-week timeline aggressive	Extended to 8–12 weeks
OTA pipeline has five concrete defects	Corrected in Foundation
Full Trust should be scoped delegation	Replaced global switch in Phase 2.5
Commercial evidence insufficient	Customer discovery added before MVP
Machine-enforced authorization needed	Added to Phase 6
Memory contract needed	Added to Phase 0
Reliability and safety report needed	Added to Phase 7
Prompt-injection threat model needed	Added to Phase 6
Realistic offline contract needed	Added to Phase 1
Cross-session continuity gap	Addressed in Phase 0 and Phase 5
PART THREE: THE NEXT 90 DAYS (PROOF PHASE — REFINED)
Days 1–14: Define the Proof (Gate 1)

Goal: A testable specification with explicit exclusions and no unresolved critical architectural assumption.

Deliverables:
Threat Model [1 week]

One page. Adversaries: malicious apps, malicious documents, malicious users, compromised cloud providers, compromised update infrastructure. Attack surfaces: accessibility service, MCP interop, cloud escalation, OTA updates, memory storage, secrets vault. Blast radius: per-user memory, per-client data, device control, financial data.

Written, published, inviting critique.
Recovery Plan [1 week]
Failure Mode	Detection	Recovery
Memory corruption	Coherence engine	Restore from backup, reconcile
Permission leak	Audit log anomaly	Revoke, re-grant, audit, notify user
Full Trust incident	Panic button, anomaly detection	Stop, downgrade, incident report
OTA bad release	Crash flag	Rollback, halt staged rollout, notify
Cloud compromise	Provider notification	Credential rotation, key revocation
Sub-agent divergence	Coherence engine	Auto-pause, notify, kill option
Memory Contract

The user must be able to answer:

    What does Zalmoxis believe about me?

    Where did each fact come from?

    When was it last confirmed?

    How certain is it?

    Which workflows can access it?

    What has been marked historical?

    What remains after I delete it?

    Can I export everything and use it without Zalmoxis's servers?

Pilot Success and Kill Criteria
Criterion	Threshold	Action
Daily active use	<40% after week 4	Stop. Reassess.
Client data leakage	Any	Stop. Rewrite scoping.
Permission comprehension	<60%	Redesign flow.
Trust-breaking incident	Any	Pause. Audit.
Cloud cost per user	>$5/month without value	Reprice or cut.
User "adds work"	>30%	Reassess.
Model Performance Gate

Before Phase 1 is considered complete, benchmark candidate models on the target device.
Metric	Target
Decode speed	>15 tok/s
Voice round-trip	<2s end-to-end
Memory retrieval	<200ms at 1000+ memories
Battery	<5% daily idle
Memory footprint	<4GB peak
Cold start	<5s

If the target phone cannot meet these, fallback options apply.
Customer Discovery

Before the MVP is built, conduct 5–10 customer discovery interviews with the target profile (busy office workers managing multiple clients).

Exit condition: A testable specification with explicit exclusions and no unresolved critical architectural assumption.
Days 15–56: Build the Core (Gate 2 — REFINED to 8–12 weeks total)

Goal: The complete core workflow works on the target device and passes its defined correctness and isolation tests.

MVP Scope (Reduced):
Component	Included
Local document ingestion	PDF, DOCX, email, images with OCR
Document classification	Contract, invoice, form, correspondence
Entity extraction	Parties, dates, amounts, obligations, deadlines
Local conversation	On-device model
Persistent memory	With provenance, inspection, correction, deletion
Identity isolation	Per-user, per-client scoping enforced at storage layer
Basic Android interface	Task dashboard, not just chat
Basic audit trail	Every action logged
Permission presets	Install-time with progressive disclosure
Memory reliability tests	Image/channel bleeding, personalization override
Hard-delete verification	Deletion that actually deletes
Model performance gate	Run on target device

Deferred from MVP:

    Emotional retrieval

    Dream Cycle and self-improvement

    Cross-device federation

    Multiple posture branding and derived agents

    Defense-specific features

    Full OTA rollout infrastructure

    Elaborate power-user tools

Exit condition: The complete core workflow works on the target device and passes its defined correctness and isolation tests.
Days 57–74: Validate with the First Customer (Gate 3)

Goal: Evidence that the product solves a real problem reliably enough to justify continued development.

Deliverables:

    Use representative real documents under an agreed data-handling arrangement

    Measure task success, time saved, answer accuracy and user effort

    Record failures and investigate them rather than hiding them behind aggregate scores

    Test whether the customer would pay for continued use

Exit condition: Evidence that the product solves a real problem reliably enough to justify continued development.
Days 75–90: Make the Investment Decision (Gate 4)

Goal: A documented go, pivot or stop decision based on predetermined criteria.

Deliverables:

    Fix the highest-impact reliability issues

    Run regression tests and security evaluations

    Complete the pilot's initial evaluation and agree on continuation

    Decide whether to expand into voice, additional tools or proactive features

    Publish an honest evidence report covering what works and what remains unproven

Exit condition: A documented go, pivot or stop decision based on predetermined criteria.
PART FOUR: THE FULL ROADMAP
FOUNDATION
The Three Postures

Internally: JARVIS, FRIDAY, EDITH.

Externally, presented as three straightforward autonomy levels:
Level	Behavior	Default
Assist	Answers questions, retrieves memories, researches and proposes actions. No consequential device action without authorization.	Yes
Act	Performs explicitly enabled, bounded tasks using approved tools. Stops at defined risk thresholds and requests confirmation when necessary.	No
Delegate	Runs narrowly scoped workflows under advance authorization, with spending limits, time limits, allowed destinations and automatic expiry.	No

Key design principle: Changing autonomy must never silently expand permissions, alter memory ownership or bypass audit requirements.

Critical distinction: A biometric unlock is not equivalent to informed approval of a particular action. Biometrics can establish that an authorized person is present; the system must still bind approval to the action, its parameters and its consequences.
The Three Market Segments (Hypotheses, Not Commitments)
Segment	Posture	Price	Deployment
Individual	Assist + Act	$10-15/mo (test before $15-20)	Phone
Small Team	Assist + Act + scoped personas	$25-40/seat/mo	Phone + headless server
Regulated Enterprise	Full stack	$100-500/seat/mo	On-premise or air-gapped

For individuals: Test a free or limited local tier against a paid tier around $10–15/month before committing to $15–20. A higher price may work for power users, but only if Zalmoxis demonstrably replaces existing tools or saves meaningful time.

For teams: Charge for more than access to the Android app: administrative controls, policy management, deployment support, device governance and integration with approved systems.

For enterprise: Price the deployment and support obligations separately where appropriate. An air-gapped installation can involve substantial engineering, updates, compliance evidence and support costs.
Team Risk

The roadmap assumes Android depth, Rust runtime work, ML training pipelines, and defense navigation. One person cannot do all of this at once.

MVP mitigation: Cut to Phase 0 + Phase 2 subset. The 8–12 week timeline accounts for solo-founder reality.

Long-term: Hire or partner for ML training, Rust runtime, Android accessibility depth. Do not pretend a solo founder can do it all.
Licensing

AGPL-3.0-or-later for community. Commercial dual-license for enterprise. Contributor CLA. Same pattern as WHAXON. Decided before first public commit.
THE OTA UPDATE PIPELINE (FOUNDATION-LEVEL — CORRECTED)
The Five Defects Fixed

The assessment identified five concrete issues in the V9 OTA example. These are corrected:

Defect 1: Manifest signing key handling inconsistent.

    Fixed: The workflow now creates the signing key file from the environment variable before invoking OpenSSL.

yaml

- name: Write manifest signing key
  run: |
    echo "${{ secrets.MANIFEST_SIGNING_KEY }}" > manifest-signing-key.pem
    chmod 600 manifest-signing-key.pem

Defect 2: Release secrets exposed to build workspace.

    Fixed: Release signing keys are kept outside ordinary CI jobs. Use protected signing infrastructure with short-lived credentials. The keystore is decoded in an isolated job, not the main build workspace.

Defect 3: Version and rollback policy incomplete.

    Fixed: Version ordering is monotonic and enforced. Anti-rollback protection rejects unauthorized downgrade. A defined key-compromise recovery procedure is documented.

Defect 4: Manifest generator needs strict serialization.

    Fixed: Use a proper JSON serializer, not string concatenation. Validate the generated manifest before signing.

bash

jq -n \
  --arg version "$VERSION" \
  --arg apk_url "$APK_URL" \
  --arg apk_hash "$APK_HASH" \
  --arg release_notes "$RELEASE_NOTES" \
  '{version: $version, apk_url: $apk_url, apk_hash: $apk_hash, release_notes: $release_notes}' \
  > update-manifest.json

Defect 5: Updater needs defined Android installation and recovery model.

    Fixed: The updater handles package-signing continuity, user authorization for package installation, interrupted downloads, storage failures and installation failures. A documented recovery procedure exists.

Recommended Architecture: The Update Framework (TUF)

Adopt TUF or an equivalent design. TUF addresses threats beyond a simple hash check, including compromised signing keys, stale updates and rollback attacks through separated signing roles and expiring metadata.
Sovereignty Levels
Level	Meaning	MVP
User data sovereignty	Private memory stays under user control	Yes
Operational sovereignty	Users can operate the product without your servers	Later
Software supply-chain sovereignty	Users can independently verify what they install	Yes
Deployment sovereignty	Organizations can build and distribute the software themselves	Enterprise
Android-Specific Qualification

The update mechanism depends on the distribution channel and platform rules. A self-managed APK updater must respect Android's package-installation controls; an application distributed through Google Play should use the supported Play update mechanism.
THE PERMISSION ARCHITECTURE (FOUNDATION-LEVEL — REFINED)
Presets Describe Outcomes, Not Technical Permissions

Three-stage design:

    Choose an intended use. Ask what the user wants Zalmoxis to accomplish, not which Android permissions they want to grant.

    Start with a working baseline. The app provides immediate value using the least privileged configuration possible.

    Request capabilities when needed. Explain the specific benefit, what information becomes accessible, what Zalmoxis can do, and what happens if the user declines.

The Runtime Capability State Machine

The preset must not be treated as proof that the required capability is available. The runtime capability state machine is the authoritative source of truth:
text

Requested → Explained → Awaiting system authorization → Granted → Available and tested → Revoked → Unsupported → Temporarily unavailable

A feature should only be offered as executable when its required capabilities are actually available.
Declined Permissions

A declined permission must not become an endless source of prompts. Record the decline, provide a useful alternative and let the user reconsider it later.
ANTI-PATTERNS (RUNTIME GUARDS)
Anti-Pattern	Constraint	Where
Ultron (unbounded capability)	Every irreversible action gated	Phase 6
Phalanx (unbounded federation)	Interop exchanges answers, not state	Foundation
Supreme Intelligence (aggregated consciousness)	Memory per-user, per-identity	Phase 0
Nimrod / Bastion (adversarial evolution)	Generation validation	Phase 0, 8
Victor Mancha (sub-agent divergence)	Sub-agents fail-closed	Phase 6
Silent Operation	Every action audited	Phase 6

Each requires a test case demonstrating it blocks the pattern.
PHASE 0: MEMORY CORE (Weeks 1–3) — MVP SCOPE
The Five Layers

Episodic (Graphiti + GAM): Temporal knowledge graph. Validity windows. Provenance. Event progression graph + topic associative network.

Semantic (Mem0 + H-MEM): Multi-level. Trust scores. Review-gated promotion. Positional index encoding.

Identity Profile (jñāpakaṁ): Permanent URN. Generation workflow. Rollback preserves learning.

Emotional Retrieval (CAMA): Deferred from MVP. Basic semantic retrieval first.

Memory Evolution (RGMem): Separate fast-changing evidence from slow-varying traits.
Active Memory

Interference-aware retrieval: conflicting memories surfaced with provenance. Contradiction handling: newer facts don't silently override. Update tracking: changes logged as episodes. Long-horizon retrieval.
Instruction Persistence Layer

Explicit constraints stored separately from passive preferences. Explicit constraints take precedence. Constraints persist across sessions.
Hard-Delete Verification

Deletion removes from all layers. Verification step confirms. Audit log records. User can request deletion certificate.
Memory Quarantine

Uncertain or conflicting facts are quarantined. A newly inferred preference should not silently overwrite an established fact. Material changes are surfaced for correction or confirmation.
Memory Contract

The user can answer:

    What does Zalmoxis believe about me?

    Where did each fact come from?

    When was it last confirmed?

    How certain is it?

    Which workflows can access it?

    What has been marked historical?

    What remains after I delete it?

    Can I export everything and use it without Zalmoxis's servers?

Memory Reliability Testing

Tests: image/channel bleeding, personalization override, explicit instruction retention, cross-session recall. Published results.
Storage Backend
Context	Storage
Phone	SQLite + float32 BLOB vectors
Desktop	LanceDB embedded
Server/Home Hub	Qdrant or PostgreSQL+pgvector
Enterprise	Neo4j

Retrieval target: <200ms at any history size.
Validation

LongMemEval-M plus reliability tests. Publish results.

Status: [DESIGNED] + [RESEARCHED]. Build in MVP. Publish validation.
PHASE 1: LOCAL BRAIN (Weeks 4–7)
Inference Runtime: KILN

Per-layer dispatcher. Edge0 techniques absorbed.
Model Layer
Model	Purpose
Maple Preview 20B-A1B	Primary (TQ2_0, 5.5 GB, 28–34 tok/s on i5-8350U)
Gemma 4 E2B/E4B	Alternative via LiteRT-LM
FunctionGemma 270M	Function calling
Needle 2 (45M)	Micro tool calling
Three-Tier Execution

    Instant Skills (D.U.M.-E): zero LLM, sub-100ms

    Deterministic Flows: YAML

    Full Agent Loop: novel reasoning

Voice Layer

OpenWakeWord, Whisper, Piper, Silero VAD, HuBERT prosody. Target <2s.
Realistic Offline Contract

"Runs entirely on the device" is defined precisely. Which features work offline: voice recognition, speech synthesis, embeddings, memory retrieval, scheduled tasks, audit inspection. Which features stop without network. Whether telemetry is sent. Whether crash reporting contains sensitive content.

Publish a clear capability matrix and make the offline state observable.
Model Performance Gate

Run during MVP. If gate fails, fallback options apply.

Status: [DESIGNED] + [RESEARCHED]. Build after MVP validated.
PHASE 2: ANDROID LAYER (Weeks 8–13) — MVP SUBSET
MVP Subset

Document ingestion (PDF, DOCX, email, OCR). Classification. Entity extraction. Indexing (SQLite FTS5 + vectors). Per-client folders. Native Kotlin skills (calendar, alarms, files, email drafts). Audit logging with hash chain. Approval gates. Permission presets. OTA updater (corrected).
The Two-Tier UI

Main UI (Simple Mode): Voice-first conversation. Task dashboard. Recent activity feed. Quick actions. Status indicators. One button: "Advanced." Notion-style restraint. Panic button (Full Trust).

Power User Backend: Memory inspection (editable, deletable, verify deletion). Permission dashboard. Audit log (exportable). Provider configuration. Skill management. Mode posture settings. Advanced query. Export facilities (memory, audit, deletion certificates). Integration settings (MCP, calendar, email).
Full Phase 2 (Post-MVP)

UI automation (accessibility tree). On-device MCP server. Skill routing (reverse-skill). Skill composition (Taku). Scoped skill personas (Karen). Active context sources. Capability state machine. Vendor-specific testing.

Status: [DESIGNED]. MVP subset built first.
PHASE 2.5: FULL DEVICE CAPABILITY (Weeks 14–16 — SCOPED DELEGATION)
Replace Global Full Trust with Scoped Delegation

The assessment's most consequential finding: Keep advanced device control as a differentiating capability, but ship it behind narrow, expiring grants. Earn broader delegation through demonstrated reliability. Do not make unrestricted Full Trust a prerequisite for experiencing the product's value.

Allowed without repeated approval:

    Read selected notifications

    Summarize approved folders

    Organize local notes

    Run previously approved workflows

Allowed only within explicit limits:

    Send messages to specified contacts

    Interact with approved applications

    Perform actions under a spending cap

Always require fresh authorization:

    Transfers of money

    Purchases beyond a limit

    Publishing sensitive content

    Granting permissions

    Exporting credentials

    Other high-impact actions

The exact categories should be risk-based, not simply based on whether an action is technically reversible.
The Deterministic Policy Engine

Do not let the language model be the final authority on whether its own action is permitted. Use a deterministic policy engine between the model and the device-control tools.

Interface:

    check(action, context) — determine whether an action is permitted

    authorize(grant, scope, expiry) — create a bounded authorization

    revoke(grant) — withdraw it immediately

    record(event) — append the decision and outcome to the audit trail

The authorization layer should evaluate the actual action parameters immediately before execution. If a recipient, amount, application, destination or other consequential parameter changes after approval, the authorization must no longer apply.

Distinguish authorization from execution success. A logged approval does not prove the action happened correctly.

A compromised prompt should not be able to rewrite the policy, widen its own permissions or approve its own execution.
What Full Trust Cannot Do

    Bypass lock screen

    Interact with FLAG_SECURE

    Silently install apps

    Silently access camera/microphone

    Reboot device

    Wipe device without Device Admin + confirmation

    Modify all system settings

    Bypass app-level security

    Read encrypted content without key

    Act when device is off

Pre-Ship Checklist

All ten items plus six months zero incidents before general availability. Opt-in, invite-only, with user agreement stating risks and recovery.

Status: [DESIGNED]. Not built. Not validated.
PHASE 3: PROACTIVE ENGINE (Weeks 17–21)
Core [RESEARCHED/DESIGNED]

Default Mode Analog (dual-pulse). Project Sue heartbeat (action/delay/non-action). Sentinel Watch (background event-waiter). KnowU-Bench formalization. PROPER (knowledge gaps). ContextAgent (wearable sensory). Reference implementations (Proactive Sebastian, Sigrid, OpenMuse). Agent Reach. Preference-Based Discovery (Taku). Conditional Escalation (Veronica). Mode Posture Transitions. Relevance Filtering. Explain First, Act Second.
Proactivity Must Earn Its Battery Budget

Keep the heartbeat only if it delivers clear user value. Start with scheduled, user-configured checks and notifications. Avoid continuous background reasoning that drains the battery.

Status: [DESIGNED] + [RESEARCHED]. Build after MVP validated.
PHASE 4: DEVICE-CLOUD BRIDGE (Weeks 22–26)
Core [RESEARCHED/DESIGNED]

EcoAgent (Cloud Planning, On-Device Execution, On-Device Observation). Pre-Understanding Module (48.6x reduction, 3.9s latency vs 15.3s). Provider Fallback (Mercury). User-Visible Escalation. Token Budget (Mercury). Prompt Caching (0.1x/1.25x). Offline Fallback. Routing Log. Tealkit Server Mode. Outbound-Only Remote Access (Avibe). Channel-Agnostic Interface (Avibe).

Status: [DESIGNED] + [RESEARCHED]. Build after MVP validated.
PHASE 5: KNOWLEDGE EVOLUTION (Weeks 27–31)
Core [RESEARCHED/DESIGNED]

Dream Cycle (Agent Brain). Conservative, inspectable memory consolidation first. Defer open-ended self-improvement. Knowledge Pages (Hindsight). Memory Tree (openhuman). Hermes Skill Extraction. Derived Personas (Viv Vision). Sub-Agent Divergence (Victor Mancha). Affective Continuity. Temporal Graph Reasoning. Inspectable Memory. FSRS-Weighted Retention. Generative UI Artifacts (Avibe). jñāpakaṁ Continuity. RGMem Trait Consolidation.
Cross-Session Continuity

The lifecycle model for how the agent's identity, skills and policy change over time while maintaining continuity. jñāpakaṁ generation workflow handles identity. Hermes handles skills. Memory evolution handles policy.

Status: [DESIGNED] + [RESEARCHED]. Build after MVP validated.
PHASE 6: SECURITY HARDENING (Weeks 32–35)
Core [RESEARCHED/DESIGNED]

OpenMuse OpenMuse Sentinel Pattern (risk levels, first-match-wins, taint). Secrets Vault. Audit Log (Sasana hash chain). Aletheia Truth Compiler (epistemic labels, 18 dimensions). Injection Scanning at Ingestion. Security Dashboard. Approval Gates (BiometricPrompt). OWASP MASVS. Android 14+ FGS Types. Mode Posture Security. Completeness Contract (Mercury). Sub-Agent Divergence Monitoring. Anti-Pattern Guards (test cases required). OTA Update Security (corrected). Permission Security.
Machine-Enforced Authorization Architecture

This is the most important addition. A separate subsystem the model cannot rewrite:

    check(action, context) — determine whether an action is permitted

    authorize(grant, scope, expiry) — create a bounded authorization

    revoke(grant) — withdraw it immediately

    record(event) — append the decision and outcome to the audit trail

The authorization layer evaluates the actual action parameters immediately before execution.
Formal Prompt-Injection Threat Model

Treat text from webpages, messages, notifications, documents, OCR and other external sources as untrusted input. None should be able to grant permissions, alter policy, reveal unrelated memories or authorize actions merely by containing instructions.

The model should not be expected to reliably distinguish every malicious instruction from legitimate content. The architecture must limit the damage when it fails.

Status: [DESIGNED] + [RESEARCHED]. Build after MVP validated.
PHASE 7: LAUNCH & ITERATION (Weeks 36+)

Public Beta. Feedback Pipeline. Model Updates. Skills Ecosystem. Continuous Security Review. Individual Segment Launch ($10-15/month test first). OTA Pipeline Live (staged rollout).
Published Reliability and Safety Report

Create a benchmark specifically for Zalmoxis's supported tasks:
Metric	Why it matters
Task completion rate	Whether the product actually works
Correct-target rate	Whether it acts on the intended application, contact or object
Unauthorized-action rate	Whether the governance boundary holds
Verification failure detection	Whether mistakes are caught
Memory isolation failures	Whether identities remain separate
Median and 95th-percentile latency	Whether interaction feels responsive
Battery use per hour and per task	Whether daily use is sustainable
Cloud escalation frequency and cost	Whether privacy and pricing promises hold

Test across a defined set of devices, Android versions, model configurations and network conditions. Publish both successful and failed test categories.

For a product whose principal promise is trust, this report is part of the sales proposition.

Status: [DESIGNED]. Depends on MVP pilot success.
PHASE 8: PLATFORM & HARDWARE AGNOSTICISM (Weeks 37–47)
Core [RESEARCHED/DESIGNED]

Kotlin Multiplatform (Koin 4.0, Ktor Client 3.0, SQLDelight 2.0, Compose Multiplatform 1.7+). KILN + llama.cpp dual runtime. Storage-agnostic memory. jñāpakaṁ lineage. Home Assistant Assist voice. Cross-platform skills. KMP frameworks (JetBrains/koog, koaks). Tealkit Server Mode. Compact Deployment Personas (H.E.R.B.I.E.). Browser Workbench + PWA (Avibe). Outbound-only remote access. Channel-agnostic interface. Small Team Segment Launch ($25-40/seat/mo). Regulated Enterprise Segment Launch ($100-500/seat/mo). OTA Update Server (50+ clients).
Cross-Device Federation and Headless Server Mode

Both expand the security boundary, deployment burden and support requirements. They are not essential to prove the phone-first proposition. Defer until post-MVP.

Status: [DESIGNED] + [RESEARCHED]. Build after Phase 7 success.
PHASE 9: DEFENSE PARTNERSHIP (Post-PMF)
The Assessment's Verdict

Yes, as a longer-term partnership strategy. No, as a dependable near-term source of revenue.
The Catch

Funding eligibility and procurement access are not the same thing. Many European Defence Fund calls require a consortium spanning at least three eligible countries. A company without capital, established partners or a tested product would struggle to lead such a program.
Entry Strategy

    Build a credible civilian security product first.

    Find a narrow defense-relevant use case. Offline knowledge retrieval and document assistance for personnel operating in connectivity-constrained environments.

    Partner with an established European integrator or research institution.

    Seek funded demonstrations and subcontracting opportunities.

    Pursue formal assurance only when a buyer defines the requirements.

Strategic caution: Military customers may require different deployment, support, supply-chain and security arrangements from civilian customers.

Recommendation: Keep defense as an option, not a roadmap dependency. It should not determine what you build for your first paying customers.

Status: [DESIGNED]. Post-PMF option.
PART FIVE: THE EVOLUTION LAYER
Component	What It Solves	Where It Fits
jñāpakaṁ	Identity survives migration	Phase 0, 8
Project Sue	Proactive restraint	Phase 3
Sentinel Watch	Long-horizon autonomy	Phase 3
CAMA	Emotional retrieval	Phase 0 (deferred)
Hermes	Skill extraction	Phase 5
Aletheia Truth Compiler	Auditable reasoning	Phase 6
Tealkit	Mobile-to-server	Phase 4, 8
Agent Reach	Internet without API keys	Phase 3
RGMem	Memory evolution	Phase 0, 5
Taku	Skill composition	Phase 2, 5, 8
Avibe	Server mode, remote access	Phase 4, 8
Mercury	Permission hardening	Phase 0, 4, 6
OTA Pipeline	Continuous updates	Foundation (corrected)
Permission Presets	Install-time bundles	Foundation (refined)
Scoped Delegation	Replace global Full Trust	Phase 2.5
Machine-Enforced Auth	Policy engine	Phase 6
Memory Contract	User-verifiable promises	Phase 0
Reliability Report	Published benchmark	Phase 7
Prompt-Injection Model	Concrete boundaries and tests	Phase 6
Offline Contract	Capability matrix	Phase 1
PART SIX: THE MARVEL ARCHITECTURE MAP
The Three Postures (Internal Only)
Marvel AI	Zalmoxis Equivalent	Phase
J.A.R.V.I.S.	JARVIS posture	0, 3, 5
F.R.I.D.A.Y.	FRIDAY posture	1, 2, 4
E.D.I.T.H.	EDITH posture	4, 6, 8

Externally: Assist, Act, Delegate. Do not make the customer learn the internal names.
The Execution Patterns
Marvel AI	Zalmoxis Equivalent	Phase
Karen	Scoped skill persona	2, 5
D.U.M.-E / U	Tier 1 deterministic skills	1, 2
Veronica	Conditional escalation protocol	3, 6
N.A.T.A.L.I.E.	Domain-specific skill package	5, 8
H.E.R.B.I.E.	Compact deployment persona	8
Mainframe	Headless server mode	4, 8
The Persona Patterns
Marvel AI	Zalmoxis Equivalent	Phase
Jocasta	Alternate soul configuration	0, 5
Viv Vision	Derived persona	5, 8
Victor Mancha	Sub-agent divergence risk	6, 8
The Anti-Patterns
Marvel AI	Zalmoxis Constraint	Where Enforced
Ultron	Every irreversible action gated	Phase 6
The Phalanx	Interop exchanges answers	Foundation
Supreme Intelligence	Memory per-user, per-identity	Phase 0
Nimrod / Bastion	Generation validation	Phase 0, 8
Victor Mancha	Sub-agents fail-closed	Phase 6
PART SEVEN: THE TECHNOLOGY STACK (CONSOLIDATED)
Component	Technology	Phase	Status
Inference runtime	KILN	1, 8	[RESEARCHED]
Primary model	Maple Preview 20B-A1B	1	[RESEARCHED]
Alternative model	Gemma 4 E2B/E4B	1	[RESEARCHED]
Function calling	FunctionGemma 270M	1, 2	[RESEARCHED]
Micro tool calling	Needle 2	1	[RESEARCHED]
Edge GPU	ML Drift	1	[RESEARCHED]
Episodic memory	Graphiti + GAM	0, 5	[RESEARCHED]
Semantic memory	Mem0 + H-MEM	0, 5	[RESEARCHED]
Emotional retrieval	CAMA	0	DEFERRED
Memory evolution	RGMem	0, 5	[RESEARCHED]
Bi-temporal storage	AletheiaDB	0	[RESEARCHED]
Multimodal memory	EmbeddingGemma 2	0, 5	[RESEARCHED]
Active Memory	Interference-aware retrieval	0	[DESIGNED]
Instruction Persistence	Explicit constraints layer	0	[DESIGNED]
Hard-Delete Verification	Deletion certificate	0	[DESIGNED]
Memory Contract	User-verifiable promises	0	[DESIGNED]
Memory Quarantine	Uncertain facts isolated	0	[DESIGNED]
Memory benchmark	LongMemEval-M + reliability tests	0	[DESIGNED]
Android automation	AccessibilityService, UiAutomation	2	[DESIGNED]
Device control	DroidRun, DroidBridge	2	[DESIGNED]
Scoped Delegation	Narrow, expiring grants	2.5	[DESIGNED]
Policy Engine	Deterministic authorization	2.5, 6	[DESIGNED]
Skill routing	reverse-skill	2	[DESIGNED]
Skill composition	Taku	2, 5, 8	[DESIGNED]
Scoped personas	Karen	2, 5	[DESIGNED]
Proactive engine	DMA + Project Sue + KnowU-Bench + PROPER + ContextAgent	3	[RESEARCHED]
External context	Agent Reach	3	[DESIGNED]
Conditional protocols	Veronica	3, 6	[DESIGNED]
Security layer	OpenMuse + Sasana + IETF PoB	6	[RESEARCHED]
Audit integrity	Aletheia Truth Compiler	6	[RESEARCHED]
Machine-Enforced Auth	Policy engine	6	[DESIGNED]
Prompt-Injection Model	Concrete boundaries and tests	6	[DESIGNED]
Permission hardening	Mercury	0, 4, 6	[RESEARCHED]
Permission presets	Install-time bundles + runtime state machine	Foundation, 2, 6	[DESIGNED]
Token budget	Mercury	4	[DESIGNED]
Voice pipeline	OpenWakeWord + Whisper + Piper	1, 8	[DESIGNED]
Voice hardware	ESPHome, Wyoming	8	[RESEARCHED]
Identity	jñāpakaṁ	0, 8	[RESEARCHED]
Cross-platform	Kotlin Multiplatform	8	[RESEARCHED]
KMP frameworks	koog, koaks	8	[RESEARCHED]
Server mode	Tealkit	4, 8	[RESEARCHED]
Web workbench	Avibe	4, 8	[DESIGNED]
Channel-agnostic	Avibe	4, 8	[DESIGNED]
Skill extraction	Hermes	5	[RESEARCHED]
Generative UI	Avibe	5	[DESIGNED]
Derived personas	Viv Vision	5, 8	[DESIGNED]
Compact deployment	H.E.R.B.I.E.	8	[DESIGNED]
OTA updates	GitHub Actions + TUF + corrected workflow	Foundation	[DESIGNED]
Two-Tier UI	Main UI + Power User Backend	2	[DESIGNED]
Aletheia interop	MCP + shared URN	Appendix A	[DESIGNED]
Licensing	AGPL + commercial + CLA	Appendix C	[DESIGNED]
Reliability Report	Published benchmark	7	[DESIGNED]
Offline Contract	Capability matrix	1	[DESIGNED]
PART EIGHT: WHAT MUST NEVER BE OMITTED

    Audit trail before automation

    Approval gates for irreversible actions

    Validity windows for memory

    Secrets never in memory

    Cost transparency

    Offline fallback

    Injection scanning at ingestion

    User agency preserved

    Battery reality — Target 2-5% daily idle

    Inspectable memory

    Deterministic-first routing

    Explain first, act second

    Capability state machine

    Vendor-specific testing

    Polish over speed

    Memory integrity hash chain

    Structured justification without chain-of-thought

    Multidimensional epistemic state

    Emotional proximity in retrieval (deferred)

    Action and non-action selection

    Permanent identity URN

    Generation workflow with validation

    Rollback preserves learning

    Skill extraction from experience

    Heartbeat as temporal opportunity

    Mode posture transitions require authentication

    Mode rollback always available

    Every mode transition logged

    Completeness contract — verified or honest pause

    Outbound-only remote access

    Sub-agents fail-closed

    Anti-pattern guards enforced at runtime

    Alternate soul configurations available

    Derived personas inherit memory, have independent personality

    Three market segments with distinct pricing and deployment

    Defense partnership is post-PMF

    OTA pipeline set up before Phase 0

    Release keystore and manifest signing key backed up offline

    Every update respects the user's choice

    No surprise installs, no silent permission additions, no dark patterns

    Install-time permission presets

    Progressive disclosure

    Scoped delegation replaces global Full Trust

    Every action in scoped delegation goes through plan/explain/approve/execute/verify/audit

    Panic button always present when Full Trust active

    Taint tracking integrated with every sensitive data read

    Screen reads and notification reads are transient unless explicitly saved

    Revoke in two taps from any screen

    Special access walkthrough with screenshots

    Threat model written and published

    Recovery plan defined for every failure mode

    Kill criteria written before pilot starts

    Progressive preset upgrade flow

    Full Trust: opt-in, invite-only, six months zero incidents before general availability

    Every claim tagged with evidence status

    Active Memory (interference-aware retrieval)

    Instruction persistence layer

    Hard-delete verification

    Memory reliability testing

    Two-tier UI (Main + Power User Backend)

    Notion-style restraint

    Active Focus (obstacle handling, goal persistence)

    Empirical competitive validation

    Aletheia interop defined

    Model performance gate

    Licensing decision

    Machine-enforced authorization architecture

    Memory contract

    Published reliability and safety report

    Formal prompt-injection threat model

    Realistic offline contract

    Scoped delegation replacing global Full Trust

    Deterministic policy engine

    Cross-session continuity and memory evolution

    Customer discovery before MVP

    Memory quarantine for uncertain facts

    Runtime capability state machine

PART NINE: THE COMPETITIVE MOAT

Not claimed: architectural moat.

Claimed: execution moat and trust accumulation moat.

The strategic moat is verified control, not the existence of controls. A competitor can add a privacy toggle. It is much harder to reproduce a mature governance system, a well-tested safety record, a trusted user relationship and years of evidence showing that the system behaves as promised. Those advantages only accumulate after real deployment.

The most important threat is not that Google or Apple will copy Zalmoxis's entire architecture. It is that they will deliver 80% of the user-visible value with far less setup, lower latency and better integration.

Defense must be more specific than "we are more private."

Verifiable claims about the competition:

    Meta Muse: 2.18M DAU. Hallucinated a user request two days after last use. User disconnected all permissions.

    ChatGPT: Market leader. Confidently hallucinated a memory that never happened. Disabling memory doesn't delete data.

    Gemini: Broad integration. Memory mixes up chats and images. Personalization overrides explicit instructions.

    Microsoft Copilot: Enterprise leader. Lost years of context. "Delete all" doesn't delete.

    Claude: Satisfaction leader. No mobile agent.

    Perplexity: Research leader. No personal memory.

    Grok: -4.7 satisfaction among former users. 62.4-point churn gap.

The market has validated the thesis. Now prove the product.
PART TEN: HARD TRUTHS

    No single app does everything. The pieces exist in fragments. The assembly is the work.

    "Sentinel" was the former working name for Zalmoxis. Trademark clearance revealed that former name is unusable.

    Edge0 is ahead on MoE streaming. KILN has the branch built but not validated.

    Prompt injection is unsolved. Adaptive attackers break state-of-the-art defenses >80% of the time.

    The market gap is real but not yet searchable.

    Battery is a constraint.

    The execution risk is brutal. 85% per-step success drops to ~20% over ten steps.

    The competitive clock is ticking. Meta can spend $10B+ without blinking.

    The valuation depends on trust at scale. Reframed as unit economics.

    Polish is not optional. OpenClaw's 2.2-star app is the cautionary tale.

    Designing around trust is not the same as earning trust. The next phase is proof.

    The vision is ahead of the evidence. Every claim must move from [DESIGNED] to [VALIDATED].

    The roadmap is a research log, not a plan. Split into product roadmap, research log, technical architecture.

    Solo-founder execution is the largest unexamined risk. Cut scope to MVP.

    Full Trust is a liability until proven. Scoped delegation replaces global switch.

    Install-time presets shift comprehension burden. Mitigate with progressive upgrade flow.

    Defense partnership is post-PMF.

    Claims of "only" and "first" are unverifiable.

    The military is a long-term strategic option.

    Three market segments have different buyers, prices, deployments.

    OTA pipeline is set up before Phase 0.

    Every update respects the client's choice.

    Permission presets solve interruption, not comprehension.

    Full Trust is the maximum Android allows without root.

    The next 90 days are about proof, not expansion.

    The market has validated the thesis. Competitors are proving Zalmoxis's point.

    Memory reliability is the first feature.

    The two-tier UI is the second feature.

    Active Memory and Active Focus are the differentiators.

    Hard-delete is a trust feature. It is not optional.

    The assessment's central recommendation is to resist the temptation to make Zalmoxis more sophisticated before making it dependable.

    A $420–500M valuation would represent roughly 15–18 times $28M ARR. That multiple is not impossible for a high-growth software company, but it should not be treated as a typical or guaranteed outcome.

    The target should not be a claim that the system never makes mistakes. It should be demonstrable evidence that mistakes are contained.

    The most important thing you can do now is resist the temptation to make Zalmoxis more sophisticated before making it dependable.

PART ELEVEN: THE PITCH (REVISED)

The thesis: "An AI agent that remembers, reasons, and acts locally, while making its permissions, decisions, data access, and failures inspectable by the person who owns it."

What to show:

    Threat model (one page, published)

    Recovery plan (one page, published)

    MVP demo (memory core + document ingestion)

    Pilot result (one customer, real usage)

    Kill criteria

What to claim: "Here is the combination we have. Here is what we have proven. Here is what we have not. Here is the plan to prove it."

What to reference: "Meta Muse has 2 million users and trust-breaking incidents. ChatGPT has 82% memory recall and hallucinated memories. Gemini mixes up contexts. Copilot loses years of data. The market is proving the need for what Zalmoxis is building."

What to ask: "Would you pay for this? Would you use it? Would you trust it?"

What to add: "For a product whose principal promise is trust, the published reliability and safety report is not merely engineering documentation. It is part of the sales proposition."
PART TWELVE: APPENDIX A — ALETHEIA INTEROP

(Unchanged from V9. MCP protocol, shared jñāpakaṁ URN, three interop modes, verification loop.)
PART THIRTEEN: APPENDIX B — MODEL PERFORMANCE GATE

(Unchanged from V9. Run during MVP. Fallback options apply.)
PART FOURTEEN: APPENDIX C — LICENSING

(Unchanged from V9. AGPL-3.0-or-later, commercial dual-license, contributor CLA.)
PART FIFTEEN: THE GOD-MODE PROMPT
text

You are my senior systems architect and lead engineer for a project codenamed **Zalmoxis**. 

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

PART SIXTEEN: THE ASSESSMENT'S FINAL VERDICT

Would the assessor continue with Zalmoxis?

Yes. But with a clear distinction between continuing with the thesis and continuing with the entire roadmap.

The thesis deserves an experiment. The complete platform deserves to be earned through successive proofs.

V9 is strongest when it says that the architecture is designed, the market opportunity remains to be proven, and the next phase is about one customer, one workflow and real evidence. It is weakest when its technology stack and phase structure create an impression of delivery certainty that the evidence does not support.

The most important thing you can do now is resist the temptation to make Zalmoxis more sophisticated before making it dependable.

A local document assistant with genuinely inspectable memory, correct deletion, strict identity isolation and a clear audit trail would be a meaningful first product. If customers use it repeatedly, trust its behavior and pay for it, you have a basis for expanding into voice, autonomous workflows, the three postures and eventually enterprise deployment.

If they do not, discovering that early is a success of the proof phase, not a failure of the vision.

Recommendation: Approve the proof phase, narrow the MVP further, formalize the acceptance tests, and defer every major capability that does not directly support the first customer's workflow.

That is the fastest credible route from Zalmoxis V9 as a design to Zalmoxis as a product that has earned the right to make its promises.

File status: V10 complete. Every finding from the full independent assessment integrated. Nothing removed from V9. Everything contextualized. The MVP is narrower. The timeline is realistic. The OTA pipeline is corrected. Full Trust is replaced with scoped delegation. The machine-enforced authorization architecture is added. The memory contract is added. The reliability report is added. The prompt-injection threat model is added. The offline contract is added. Customer discovery precedes the MVP build. Memory quarantine is added. The runtime capability state machine is added.
give me o a one liner to write all to disk
