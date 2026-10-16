# ZALMOXIS THREAT MODEL

**Version:** 1.0 — 2026-10-16
**Status:** Active
**Non-goals:** This document does not prescribe defenses. It names the threats. Defenses are named in the roadmap and in the policy engine spec.

---

## 1. Assets to Protect

1. **User memory corpus.** Episodic anchors, semantic facts, identity profile, coherence state. Most valuable, least replaceable. Loss is permanent. Leakage is irreversible.

2. **Per-client document store.** For the office deployment, indexed documents, extracted entities, per-client folders. Confidentiality is contractual. Loss is business-ending.

3. **Permission grants.** Scoped delegations the user has approved. Forging a grant is equivalent to forging the user's authority.

4. **Audit trail.** Append-only, hash-chained log of every action. Tampering destroys the trust basis of the system.

5. **Secrets vault.** API keys, tokens, credentials. Encrypted. Never in memory. Never in plaintext.

6. **User identity (URN).** Permanent identifier that survives migration. Compromise means an attacker can impersonate the user to every composed service.

---

## 2. Adversaries

### 2.1 Malicious Documents

Any ingested file: PDFs, DOCX, XLSX, email bodies, attachments, scanned images, plain text. A document can carry a **prompt injection** — instructions addressed to the model that attempt to override its system prompt, extract memory, forge authorizations, or exfiltrate data.

**Realistic capability:** High. Prompt injection is unsolved. Adaptive attackers break state-of-the-art defenses >80% of the time. Small models are markedly easier to steer than large ones.

### 2.2 Malicious Applications

Apps the agent interacts with via accessibility, or apps that can send intents the agent handles. A malicious app can emit fake accessibility events, present screens that look legitimate, trigger actions on the wrong target, or intercept agent input.

**Realistic capability:** Medium-high. Android isolation is strong, but the accessibility layer is shared.

### 2.3 Malicious Users (Multi-User Deployments)

One user's device compromised, or one user attempts to access another's memory. Most dangerous case is **cross-identity memory leakage**.

**Realistic capability:** Low for external attackers (they need device access), high for insiders.

### 2.4 Compromised Cloud Provider

When Zalmoxis escalates a task, the provider sees the distilled task, not raw memory. Provider could log it, refuse deletion, correlate across users, or be compelled by subpoena.

**Realistic capability:** Low for reputable providers. High for jurisdictional compulsion.

### 2.5 Compromised Update Infrastructure

An attacker who compromises OTA can push a malicious update. Signature check defends against tampering, not against key compromise, rollback attacks, stale updates, or GitHub account compromise.

**Realistic capability:** Low for mature signing, high for a solo founder with a single GitHub account.

### 2.6 Prompt Injection via Retrieved Memory

Any memory originally sourced from an untrusted document carries the injection. When the agent retrieves that memory, the injection enters context.

**Realistic capability:** High. Memory persistence amplifies injection.

---

## 3. Attack Surfaces

| Surface | What It Exposes | Primary Threat |
|---------|-----------------|----------------|
| Accessibility service | Every screen, every input, every action | Malicious apps |
| On-device inference | Model weights, prompt, context | Local attacker with device access |
| SQLite memory store | Full memory corpus | Local attacker, backup exposure |
| MCP interop | Memory queries, action logs | Cross-framework leak |
| Cloud escalation | Distilled task, not raw memory | Cloud provider logging |
| OTA updates | Executable code | Update infrastructure compromise |
| Secrets vault | Credentials | Local attacker, vault implementation bug |
| Audit log | Every action, every reasoning trace | Tampering, unauthorized deletion |
| Sub-agents (derived personas) | Inherited memory | Scope expansion, divergence |
| Backup and export | Full state | Portable attack vector |

---

## 4. Blast Radius

| Compromise | Blast Radius | Recovery |
|-----------|--------------|----------|
| Memory corruption | Single identity's history, irreversible without backup | Restore from verified backup |
| Cross-identity leak | Two or more users' full memory corpora | Revoke access, audit, notify |
| Permission forgery | Attacker acts as user in authorized scope | Revoke all grants, re-authenticate |
| Audit tamper | Trust in the log destroyed | Re-verify hash chain; broken chain means log is suspect |
| Secret leak | Every service the secret protects | Rotate immediately |
| URN compromise | Attacker impersonates user to every composed service | Revoke URN, re-mint, notify all services |
| Update compromise | Every client device | Rollback, staged re-release, incident report |
| Sub-agent divergence | Scope creep, unauthorized actions | Kill sub-agent, audit trail |

---

## 5. The Lethal Trifecta

Any single path that combines **private data access** + **prompt injection** + **egress capability** is a critical vulnerability. No single compromise path may satisfy all three.

- Private data access is scoped and audited.
- Prompt injection is scanned at ingestion, not just at inference.
- Egress is gated by the policy engine and by taint tracking.

If any one of the three is broken, the other two must hold.

---

## 6. What This Threat Model Does Not Cover

- Physical attacks (device theft with unlocked screen, hardware keylogging)
- Nation-state adversaries with supply-chain access
- Attacks on the Android OS itself
- Compromise of the cloud provider's inference servers (assumed hardened)
- Social engineering of the user
- Legal compulsion of the user

These are explicit non-goals. They are not defended against. Users are told this during onboarding.

---

## 7. Review Cadence

This threat model is reviewed:
- Before every phase gate
- After any security incident
- After any significant change to the memory architecture
- After any new integration (MCP, cloud provider, sub-agent type)
