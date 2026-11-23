# ZALMOXIS

**ZALMOXIS is a local-first AI assistant for Android.** It brings together on-device inference, a private model library, local source storage, and a compact workspace with separate Chat, Knowledge, Downloads, and Settings tabs.

The Android app is under active development. The repository contains the source and a GitHub Actions workflow for producing a debug APK. A successful debug build is not a signed release or a claim that every model works on every device.

## What is in the app

- **Chat:** run supported LiteRT-LM models on the device, stream responses, and attach an image when the selected model supports vision.
- **Downloads:** import a model file or download a reviewed, digest-pinned model from the catalog. Model transfer is user initiated and reports verification status.
- **Knowledge:** keep source files in app-private storage, search supported text and Markdown sources locally, inspect SHA-256 receipts, and delete a source with removal verification. Files without supported text extraction are stored but not indexed.
- **Settings:** run a local inference benchmark, export its JSON report, and review privacy and permission details.
- **On-device speech:** use the Android on-device recognizer for dictation. Response speech uses an installed voice only when it reports no network requirement.

Inference, prompt handling, source search, and benchmark reports are local. Downloading catalog models contacts Hugging Face. The Android manifest requests microphone access only for dictation and internet access for model downloads; microphone access is optional. The app has no account or analytics service, and model output cannot execute tools.

## Project status

The Android implementation is source code in `android/`. The debug workflow lives at [android-build.yml](.github/workflows/android-build.yml). It assembles `app-debug.apk` and uploads it as the `zalmoxis-debug-apk` workflow artifact.

To get the APK, open the repository’s **Actions** tab, select a successful **Android build** run, and download its `zalmoxis-debug-apk` artifact. The artifact is a debug build. No release signing configuration or Play Store package is provided yet.

The app is not yet a complete autonomous agent. Long-term memory reconciliation, policy-gated actions, audit-chain verification, broader document extraction, and the roadmap’s remaining phases are not represented as shipped features merely because they appear in project documents.

## Build from source

Requirements:

- JDK 17
- Android SDK platform 37.1
- Android SDK Build Tools 36.0.0

From the repository root:

```bash
cd android
./gradlew :app:assembleDebug --no-daemon
```

The APK is written to:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

To install a locally built debug APK on a connected device with Android Debug Bridge:

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

## Android stack

| Area | Current implementation |
|---|---|
| UI | Kotlin, Jetpack Compose, Material 3 |
| Local inference | LiteRT-LM Android runtime behind a local inference interface |
| Model storage | App-private storage with local provenance and SHA-256 verification |
| Model catalog | Reviewed entries pinned to repository revisions and file digests |
| Downloads | User-initiated Android transfer workflow with resumable state |
| Knowledge shelf | App-private source vault and local text/Markdown search |
| Speech input | Android on-device speech recognition |
| Speech output | Installed offline-capable Android voice, when available |
| Build | Gradle wrapper, Android Gradle Plugin, GitHub Actions |

Model compatibility, available memory, supported modalities, and performance depend on the device and the individual model. The catalog’s memory guidance is not a benchmark guarantee.

## Repository map

- `android/` — Android app and GitHub Actions build setup
- [ROADMAP.md](ROADMAP.md) — project roadmap and current implementation plan
- [PROJECT-STATE.md](PROJECT-STATE.md) — recorded project state
- [RULE SET.md](RULE%20SET.md) — project rules and governance
- [THREAT-MODEL.md](THREAT-MODEL.md) — security and privacy threat model
- [RECOVERY-PLAN.md](RECOVERY-PLAN.md) — recovery and continuity plan
- [KILL-CRITERIA.md](KILL-CRITERIA.md) — criteria for stopping or changing the project

## Privacy and trust boundaries

- Imported model files and Knowledge sources are kept in the app’s private storage.
- Model downloads use network access and are limited to catalog entries with pinned revisions and expected digests.
- Prompts and inference are processed locally by the selected on-device runtime.
- Speech dictation checks for an on-device recognizer; it does not fall back to a network recognizer.
- Android system permissions and device capabilities still apply. The app cannot bypass the operating system’s security controls.
- Deleting app data removes the app’s local files. Knowledge-source deletion also reports whether app-level removal was verified; this is not a certificate about external backups made by the user or device.

## License

No repository-level `LICENSE` file is currently present. Until the project publishes a license, do not assume the repository is available for reuse or redistribution. Model and dependency licenses remain separate and apply to their respective files and packages.
