# PROJECT-STATE.md

**Purpose:** Resume-from-cold file for ZALMOXIS. Updated at every pause (rule Z175).  
**Read this first** if context is lost. It is the authoritative "where we are" document.  
**Commit hash at time of this entry:** `050ad767a2e5f5b34cb4f4a184989f8140ca5738`

---

## 2026-10-18 — Session 2: GitHub remote added

### What changed

A private GitHub remote was added. The repo is now backed up to GitHub.

| Item | Value |
|------|-------|
| Remote URL | `https://github.com/andreipath26/zalmoxis` |
| Visibility | **PRIVATE** |
| Remote HEAD before this entry | `f4c2518` |
| Remote files before this entry | 34 |
| Forbidden files on remote | **None** — verified by `git ls-tree -r origin/main` |
| Forbidden files in remote history | **None** — verified by `git log origin/main` |
| Local-only files status | Unchanged. `ROADMAP.md` (53884 bytes) and `RULE SET.md` (25632 bytes) remain on disk, ignored, never committed, never pushed. |

### The verification that was run

`git ls-tree -r origin/main --name-only` — 34 files, no `ROADMAP.md`, no `RULE SET.md`.
`git log origin/main --pretty=format: --name-only | sort -u` — no forbidden file anywhere in remote history.
`gh repo view --json visibility` — `"isPrivate": true`.

### What this means for resume-from-cold

If the local machine is lost, the repo can be cloned from GitHub:

    git clone https://github.com/andreipath26/zalmoxis.git

The clone will contain the 34 tracked files. It will **not** contain `ROADMAP.md` or `RULE SET.md` — those were never pushed and exist only on the original machine. **If the local machine is lost, the roadmap and the rule set are lost with it.** This is the known limitation of the local-only decision. A manual backup of those two files to separate storage is the only protection against that scenario.

### Standing constraint, restated

`ROADMAP.md` and `RULE SET.md` must never be uploaded anywhere. The remote does not change this. The four mechanical layers (`gitignore`, `git/info/exclude`, `pre-commit`, `pre-push`) plus the verified absence from history protect this invariant on every future push.

---

## 2026-10-16 — Session 1: Ground laid

### Where the project lives

    /home/andreipath/Desktop/ZALMOXIS/

Plain local directory. No sync folder. No cloud drive.

### Repository state at pause

| Item | Value |
|------|-------|
| Branch | `main` |
| Commits | 1 |
| HEAD | `050ad767a2e5f5b34cb4f4a184989f8140ca5738` |
| Tracked files | 33 |
| Working tree | Clean |
| Remotes | None |
| Disk usage (working tree) | 280K |
| Disk usage (.git) | 332K |

### What exists on disk

**Tracked (in the commit):**

- `.gitignore` — 46 lines. Excludes Android/Kotlin/IDE/secrets/build/OS junk. Lines 45–46 exclude `ROADMAP.md` and `RULE SET.md`.
- `README.md` — 70 lines. What ZALMOXIS is, what it is not, current status, foundational docs, license TBD.
- `THREAT-MODEL.md` — 128 lines. Adversaries, attack surfaces, blast radius, lethal trifecta, explicit non-goals.
- `RECOVERY-PLAN.md` — 211 lines. Five failure classes, seven recovery procedures, verification, unrecoverable cases.
- `KILL-CRITERIA.md` — 113 lines. Pilot success criteria, kill criteria, stopping condition, meta-principle.
- 28 × `.gitkeep` — directory tree preserved (android/, core/{memory,identity,ingestion,audit,permissions}/{src,tests,contracts,schema}, desktop/, docs/{architecture,research,decisions}, ota/, scripts/, server/, tests/).

**Local-only (present, ignored, never committed, never pushed):**

- `ROADMAP.md` — 53876 bytes. Full V10 roadmap.
- `RULE SET.md` — 25194 bytes. Z1–Z175. Includes Z175 (this file's rule).
- `PROJECT-STATE.md` — this file.

### What is not yet written

Phase 0 remaining deliverables:

1. **Memory Contract** — the eight questions the user must be able to answer about their own memory.
2. **Model Performance Gate** — benchmarks the target device must meet before Phase 1.
3. **Customer Discovery Questions** — 5–10 interview questions for the pilot profile.

### Defense stack for the two local-only files

| Layer | State |
|-------|-------|
| `.gitignore` lines 45–46 | Present |
| `.git/info/exclude` lines 3–4 | Present |
| `.git/hooks/pre-commit` | Present, executable (488 bytes) |
| `.git/hooks/pre-push` | Present, executable (458 bytes) |
| No remote | Confirmed |

`ROADMAP.md` and `RULE SET.md` are not in any commit. Verified at pause.

### How to resume cold

1. `cd /home/andreipath/Desktop/ZALMOXIS/`
2. `git log --oneline` — should show exactly one commit: `050ad76 Initial ZALMOXIS skeleton...`
3. `git status --short` — should be empty.
4. `git remote -v` — should be empty. **If a remote appears, stop and investigate.**
5. Read this file, then `RULE SET.md`, then `ROADMAP.md`.
6. Next session starts with the Memory Contract.

### Decisions made in this session, recorded

- Folder renamed to `ZALMOXIS` (all caps, no spaces).
- `ROADMAP.md` and `RULE SET.md` are local-only, permanently. Five-layer defense installed.
- Phase 0 is scoped to: threat model, recovery plan, kill criteria, memory contract, model performance gate, customer discovery. First three done.
- No remote. No push. No public release. License undecided.
- Z175 added to the rule set: every "we pause" updates this file.

### What to verify first if something looks wrong

    cd ~/Desktop/ZALMOXIS
    git log --all --pretty=format: --name-only | sort -u | grep -E "ROADMAP|RULE"
    git check-ignore -v ROADMAP.md "RULE SET.md"
    ls -la .git/hooks/pre-commit .git/hooks/pre-push

If the first command prints anything, stop. Investigate before any git operation.

---



---

## 2026-11-23 — Owner-directed roadmap tracking change

The repository owner provided the original ZALMOXIS V10 `ROADMAP.md` and directed that the supplied files be reviewed and placed in the repository. The full roadmap is now tracked at the repository root. The `.gitignore` entry excluding `ROADMAP.md` was removed to reflect that decision. The existing `RULE SET.md` exclusion remains in place; no rule-set file was provided in this update. The October 2026 statements above describe the earlier repository state and are preserved as history.

Commits: roadmap `0a3bfe9`; tracking rule `e7a075f`.
