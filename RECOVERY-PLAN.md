# ZALMOXIS RECOVERY PLAN

**Version:** 1.0 — 2026-10-16
**Status:** Active
**Non-goals:** This document does not implement recovery. It defines the procedure. Implementation is Phase 6.

---

## 1. Failure Classes

| Class | Definition | Severity |
|-------|-----------|----------|
| Corruption | Data exists but is wrong or unreadable | High |
| Loss | Data no longer exists | Critical |
| Leak | Data exists outside its authorized boundary | Critical |
| Divergence | System behavior no longer matches policy | High |
| Degradation | System works but slowly or partially | Medium |

---

## 2. Recovery Procedures

### 2.1 Memory Corruption

**Detection:**
- Coherence engine flags inconsistency between layers
- Hash chain verification fails on the memory store
- Retrieval returns obviously wrong results
- Startup integrity check fails

**Recovery:**
1. Halt all writes to memory.
2. Identify the corrupted layer (episodic, semantic, identity, coherence, provider).
3. Restore the corrupted layer from the most recent verified backup predating the corruption.
4. Run the coherence engine to reconcile restored state with the other layers.
5. Log the incident: what was corrupted, when, from which backup restored, what was lost.
6. Notify the user: "Memory restored from [date]. The following items may have been lost: [list]."

**Verification:**
- Hash chain verifies.
- Coherence engine reports no unresolved conflicts.
- Retrieval tests pass.
- User can inspect what was lost in the Power User Backend.

**User-visible:** Yes.

### 2.2 Memory Loss

**Detection:**
- Backup integrity check fails.
- Device storage reports unrecoverable sectors.
- User reports missing memories.

**Recovery:**
1. If backup exists: restore full memory from backup.
2. If backup is partial: restore what exists, mark gaps.
3. If no backup: reconstruct from available sources (audit log, exported memories, email history).
4. Log the incident.
5. Notify the user with the extent of loss.

**Verification:**
- Retrieval works for restored items.
- Gaps are explicitly marked.
- User can export the reconstructed state.

**User-visible:** Yes.

### 2.3 Permission Leak

**Detection:**
- Audit log shows egress outside the allowlist.
- Taint tracking flags a session that read private data then attempted external communication.
- Provider reports an unexpected request.

**Recovery:**
1. Immediately revoke every active grant.
2. Halt all outbound network activity.
3. Identify leaked data (via taint tracking and audit log).
4. Notify the user with: what was accessed, what was sent, to whom.
5. Rotate any credentials the leaked data could compromise.
6. Re-authenticate the user and re-grant scopes manually.

**Verification:**
- No outbound activity for 24 hours.
- Audit log shows the revocation event.
- User confirms re-granted scopes.

**User-visible:** Yes. Immediate.

### 2.4 Full Trust Incident

**Detection:**
- Panic button pressed.
- Anomaly detection flags unusual action pattern.
- User reports an action they did not authorize.

**Recovery:**
1. Immediate stop of all automation.
2. Downgrade to Assist level (no autonomous actions).
3. Freeze the audit log (append-only, no further writes until investigation completes).
4. Present the user with the last 20 actions and their reasoning.
5. User marks each as authorized or unauthorized.
6. Unauthorized actions reversed where reversible.
7. Incident report generated and stored.

**Verification:**
- User confirms the system is stopped.
- Audit log frozen and readable.
- Incident report exists and is accurate.

**User-visible:** Yes. Immediate.

### 2.5 OTA Bad Release

**Detection:**
- Crash flag from client devices.
- Telemetry shows a spike in failures.
- User reports a regression.

**Recovery:**
1. Halt staged rollout immediately.
2. Mark release as prerelease on GitHub (clients ignore prereleases).
3. Publish a fixed release with a bumped version number.
4. Rollback path: if no fix within 24 hours, publish a rollback manifest pointing to the previous version's APK URL and hash.
5. Notify affected users: "Version [X] rolled back. Please update to [Y]."

**Verification:**
- Rollout percentage returns to 100% on the fixed release.
- Crash rate returns to baseline.
- No users remain on the bad version after 72 hours.

**User-visible:** Yes.

### 2.6 Cloud Compromise

**Detection:**
- Provider discloses a breach.
- Audit log shows unexpected provider behavior.
- User reports an action they did not request.

**Recovery:**
1. Disable cloud escalation entirely.
2. Rotate every API key stored in the vault.
3. Notify every user who used cloud escalation in the last 90 days.
4. Present the user with every task escalated, with cost and timestamp.
5. Provide the user with a list of what the provider could have seen.

**Verification:**
- Cloud escalation disabled.
- All keys rotated.
- All affected users notified.

**User-visible:** Yes. Mandatory disclosure.

### 2.7 Sub-Agent Divergence

**Detection:**
- Coherence engine flags goal drift.
- Sub-agent requests a permission outside its declared scope.
- Sub-agent's actions no longer match its declared purpose.

**Recovery:**
1. Immediate pause of the sub-agent.
2. Notify the user: "[Sub-agent name] diverged from its declared scope. Paused. Inspect, resume, or terminate?"
3. If inspect: show full action log.
4. If resume: require the user to re-declare the scope.
5. If terminate: kill sub-agent, archive memory to primary agent, log incident.

**Verification:**
- Sub-agent paused or terminated.
- Primary agent's memory intact.
- No unauthorized actions between detection and pause.

**User-visible:** Yes. Immediate.

---

## 3. Recovery Verification

Every recovery ends with verification. Verification is not optional. Recovery is not complete until verification passes.

Five checks:
1. Hash chain integrity.
2. Coherence check.
3. Retrieval test.
4. Permission check.
5. User confirmation.

All five must pass. If any fails, recovery is incomplete and escalates.

---

## 4. What Cannot Be Recovered

Some failures are unrecoverable. The system tells the user explicitly:

- Permanent storage loss without backup.
- Compromised URN with no re-minting procedure.
- Leaked data already transmitted. Recall is not possible.
- Sub-agent actions that cannot be reversed.

In these cases, recovery becomes **disclosure**: tell the user exactly what was lost, when, and why. Honest disclosure is a form of recovery.

---

## 5. Review Cadence

- Before every phase gate
- After any failure event that triggers a recovery
- After any change to the memory architecture or permission model
- Quarterly, at the portfolio review
