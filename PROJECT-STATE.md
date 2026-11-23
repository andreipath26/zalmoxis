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


---

## 2026-11-23 — Google AI Edge Gallery foundation started

A reviewable Android foundation was added on branch codex/gallery-android-foundation. The initial app uses Jetpack Compose and the LiteRT-LM Android runtime (0.18.1), imports a local .litertlm file into app-private storage, and streams CPU inference. No network permission, telemetry SDK, MCP, model-driven tools, or AccessibilityService is wired into inference. The source has not been built because the implementation environment did not include Android SDK 36; do not mark this as validated or deployment-ready.

See android/README.md and docs/decisions/GOOGLE-AI-EDGE-GALLERY-ADOPTION.md for the adopted Gallery patterns and boundaries. The tracked ROADMAP.md remains the project roadmap; this implementation does not change or truncate it.


## 2026-11-23 — Gallery-based model shelf batch

The Android foundation now includes a private local model catalog: import, list, select, delete, and restore the last-used .litertlm model. Model copying runs on an IO dispatcher and replaces an existing model through a temporary-file/backup sequence. The chat view records first-text latency, total elapsed time, and stream-event count; it does not label stream events as token throughput. A Gradle 8.13 wrapper is included. Android SDK 36 is unavailable in this workspace, so the app remains unbuilt and unvalidated.


## 2026-11-23 — Repeatable local inference benchmark

Added a controlled local benchmark: one warm-up is excluded, then three fresh-conversation runs use the same prompt. Each run records first-text latency, total elapsed time, and runtime stream-event count. Device/API level, model filename, backend, protocol, and raw samples are written to a JSON report in app-private storage. Stream events are explicitly not treated as token counts. Build attempt was blocked because the workspace could not download the Gradle 8.13 distribution; Android SDK 36 is also unavailable. No test suite was added or run.

Implementation branch at the start of this batch: codex/gallery-android-foundation (e91d88ab8eadf425b9ae58f4f38669988b1176f2).


## 2026-11-23 — Remote build path

Added a GitHub Actions workflow scoped to the Android foundation branch and Android file changes. It installs JDK 17 and Android SDK 36, runs the Gradle wrapper to assemble a debug APK, and uploads the APK artifact. The local wrapper attempt was blocked before Gradle could start because the sandbox could not reach services.gradle.org. CI status must be checked before treating the source as build-validated.


The branch-scoped workflow was triggered and retried. Both GitHub Actions attempts ended before a runner was assigned (runner ID 0, no job steps); no APK artifact was produced. The failure is currently an Actions runner allocation issue, not a compiler result. Local build is still blocked by the unavailable Gradle distribution and Android SDK 36.


## 2026-11-23 — Local image input

Added optional image attachment through Android's document picker. The selected image is copied into the app's private cache, passed to LiteRT-LM as an image file with the user's prompt, then removed after inference. Startup clears stale temporary image files. No broad photo permission is requested. Image inference requires a multimodal model; this path has not been built or exercised because the local Gradle/Android SDK toolchain is unavailable and the remote runner failed before job startup.


## 2026-11-23 — On-device speech input

Added push-to-dictate using Android SpeechRecognizer's on-device instance (Android 12/API 31+ and only when the device reports availability). RECORD_AUDIO is requested at runtime only after the user presses the dictation control. Denial leaves text entry available, and there is no network-recognition fallback. Manifest includes the speech-service visibility query and marks microphone hardware optional. This is source-only; the local Android build and GitHub Actions runner are still unavailable.


## 2026-11-23 — Runtime adapter boundary

Extracted the app-facing OnDeviceInferenceRuntime interface and retained LiteRT-LM as its first CPU adapter. Model loading now selects the registered runtime by file format and rejects unregistered formats explicitly. Benchmark reports record runtime and backend identifiers. This creates the integration seam for KILN/llama.cpp while making no claim that its Android backend is implemented.


## 2026-11-23 — Offline speech output

Added user-triggered Android TextToSpeech output. The app selects an installed voice only when Android reports that it does not require a network connection, provides stop control, and does not request a new permission. Manifest package visibility includes the TTS service query. Available voices depend on the device and installed speech data. This source has not been built or run on-device; the Android build remains unverified.


## 2026-11-23 — Google AI Edge Gallery adoption and offline TTS

Recorded the full Gallery capability map as an additive appendix to ROADMAP.md, preserving the preceding V10 text. The appendix pins the inspected Gallery source snapshot and maps model discovery/transfers, inference, benchmarking, multimodal media, speech, embeddings, agent tooling, Prompt Lab, WorkManager, Firebase, XR, video, and media cards to Zalmoxis requirements and current source state. Added offline Android TTS and documented retry behavior and the app inventory. Latest implementation source commit before this checkpoint: f697819c039b1e7ee51f56015122db76c55adec5. Android source remains unbuilt and unvalidated.


## 2026-11-23 — Local model provenance and benchmark identity

Local model imports now receive a private provenance receipt with SHA-256, file size, import time, selected-file source label, an explicit unknown-license value, and publisher-authenticity=false. The app verifies the recorded digest before loading and shows the digest and license status in the model shelf. This detects local changes; it is not a publisher signature or license verification. Benchmark JSON now identifies the exact local model file by SHA-256. Source remains unbuilt and unvalidated.


## 2026-11-23 — Provenance visible in model shelf

The Android model shelf now shows the local SHA-256 prefix, selected-file source label, import timestamp, and license status. A malformed provenance receipt blocks automatic restore and requests a reimport for review. This checkpoint follows model provenance and benchmark identity commit e36205788713a39c6a507d856776bc4708592cb2. Android source remains unbuilt and unvalidated.


## 2026-11-23 — User-controlled benchmark export

Added an Android document-picker export for the latest local benchmark JSON report. The app copies the report only after the user chooses a destination; it does not upload the report or share it automatically. Model changes clear the currently displayed export target while previous private reports remain stored. Source remains unbuilt and unvalidated.


## 2026-11-23 — Benchmark export review

Reviewed the report-export UI and gave the summary text a flexible width so the export control fits narrow screens. Updated the Gallery adoption record to include exact model digest, local provenance receipts, offline speech output, and user-controlled JSON export. Android build validation is still outstanding.


## 2026-11-23 — Invalid model receipt recovery

A malformed local provenance receipt no longer aborts model-catalog display. The affected model remains visible, loading is disabled, and reimporting a replacement of the same name can repair the receipt. This preserves a user recovery path while keeping receipt verification fail-closed. Android source remains unbuilt and unvalidated.



## 2026-11-23 — Pinned model catalog and verified download path

Added public catalog entries for Qwen 2.5 1.5B Instruct and Gemma 4 E2B Instruct, pinned to immutable Hugging Face revisions and exact filenames, sizes, SHA-256 digests, and Apache-2.0 labels. The catalog UI displays download size, RAM guidance, modalities, source revision, license, progress, and cancellation. WorkManager supports range resume, retries, storage preflight, HTTPS Hugging Face/CDN redirects, and size/SHA-256 verification before atomic install with a provenance receipt. A successful download refreshes the local shelf but does not auto-load the model. Prompts, inference, and benchmark reports stay on device; downloads contact Hugging Face. Source has not been Android-built or validated with an on-device transfer. API 34+ User-Initiated Data Transfer is the recommended route for long explicit downloads; WorkManager remains the fallback and needs quota-aware handling on Android 16.
