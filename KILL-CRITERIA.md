# ZALMOXIS KILL CRITERIA

**Version:** 1.0 — 2026-10-16
**Status:** Active
**Non-goals:** This document does not describe success metrics for a shipping product. It describes the conditions under which the project is stopped.

---

## 1. Purpose

Every project must have a written stopping condition. Without one, the project is infinite by construction, and sunk cost accumulates. This document names the conditions under which Zalmoxis is stopped — not paused, not reduced in scope, but stopped.

The criteria are written **before the pilot begins**. They are not revised after the pilot. If the pilot fails any of these, the project ends.

---

## 2. The Pilot

The pilot is a 90-day deployment with one customer (one busy office, one office worker, real documents). It begins after Phase 0 (Defining the Proof) and Phase 1 (Building the Core) complete. See `ROADMAP.md` for the full timeline.

The pilot is the first real validation of the three hypotheses:
- **Hypothesis A:** The problem exists (people want persistent, reliable AI memory and controlled automation).
- **Hypothesis B:** Zalmoxis solves it better (local memory, explicit governance, verifiable actions produce a measurably better experience).
- **Hypothesis C:** Customers will pay enough (the improvement is valuable enough to support the subscription).

The kill criteria test all three.

---

## 3. Kill Criteria

Every criterion below is measured during the pilot. Any single violation triggers the corresponding action. The action is not negotiable.

| # | Criterion | Threshold | Action |
|---|-----------|-----------|--------|
| 1 | Daily active use | < 40% of workdays after Week 4 | Stop. Reassess. |
| 2 | Client data leakage | Any occurrence | Stop. Rewrite scoping layer. |
| 3 | Permission preset comprehension | < 60% of users understand what they granted | Redesign preset flow. Pause pilot. |
| 4 | Trust-breaking incident | Any occurrence | Pause. Audit. Do not resume until root cause is fixed and verified. |
| 5 | Cloud cost per user | > $5/month without clear user value | Reprice or cut features. |
| 6 | User-reported "adds work" | > 30% of survey responses | Reassess product design. |
| 7 | Model performance gate failure | < 15 tok/s, > 5s cold start, or > 5% daily idle battery | Fall back to smaller model or defer on-device feature. |

---

## 4. What "Stop" Means

When a kill criterion triggers "Stop":

1. **Halt all development.** No new features. No partial commits.
2. **Write the incident report.** What happened, when, why, what was learned.
3. **Preserve the audit trail.** Do not delete anything.
4. **Notify the customer.** Explain what happened.
5. **Archive the repository.** Private archive, no deletion.
6. **Move to post-mortem.** The findings feed the next project.

**Stop does not mean delete.** The work done is knowledge. The knowledge is preserved.

---

## 5. What "Pause" Means

When a kill criterion triggers "Pause":

1. **Halt new development.**
2. **Continue serving existing users.** No new features.
3. **Investigate root cause.**
4. **Fix, verify, resume.** Only after the root cause is fixed and independently verified.
5. **If the fix cannot be delivered within 30 days, escalate to Stop.**

**Pause is a checkpoint. Stop is an ending.**

---

## 6. The Stopping Condition (Non-Pilot)

Separate from the pilot kill criteria, the project has a **final stopping condition**: the point at which Zalmoxis is finished and no further work is done.

Zalmoxis is finished when:

1. The memory core passes LongMemEval-M with > 80% accuracy at 100 sessions.
2. The audit trail hash chain has run without a break for 365 consecutive days in production.
3. At least one regulated enterprise customer is running an air-gapped deployment in production.
4. The community edition has at least 1,000 active installs.
5. The commercial dual-license has generated at least $100,000 in annual revenue.

When all five are true, the project is **finished**. Further work is either maintenance or a new project.

---

## 7. What This Document Is Not

- Not a list of failure modes that trigger pause automatically. Some failures require pause; some require continued investigation. This document names the kill criteria. The roadmap names the pause conditions.
- Not a substitute for the recovery plan. The recovery plan describes how the system recovers from failure. This document describes when the project is stopped.
- Not a description of commercial metrics beyond the pilot. Revenue targets belong in the business plan, not here.

---

## 8. Review Cadence

- Written before the pilot begins (this document).
- Reviewed at the start of the pilot.
- Reviewed at the pilot's midpoint.
- Reviewed at the pilot's end.
- **Not revised after the pilot begins.** If a criterion is wrong, the wrong criterion is what was committed to. The next project learns from the mistake.

---

## 9. The Meta-Principle

**If the pilot fails, the project stops. Not pauses. Stops.**

The sunk cost fallacy is the death of early-stage projects. The kill criteria exist to enforce the discipline that the human cannot enforce on themselves.
