# ZALMOXIS

**ZALMOXIS is a local-first AI assistant for Android.** It brings together on-device inference, a private model library, local source storage, and a redesigned Obsidian/Lime mobile interface with Chat, Voice, Knowledge, Models, and Workspace destinations; Settings remains accessible from the header.

The Android app is under active development. The repository contains the source and a GitHub Actions workflow for producing a debug APK. A successful debug build is not a signed release or a claim that every model works on every device.

## Zalmoxis V2 — approved interface direction

![Zalmoxis V2: Chat, Voice, Knowledge, Models, Workspace](docs/assets/zalmoxis-v2-mockup.svg)

> **Design reference, not a running-app screenshot.** This five-screen SVG storyboard follows the approved Obsidian/Lime concept. The original high-resolution generated concept remains the visual target; the checked-in SVG is a recreated, version-controlled reference rather than the original PNG. Real Android screenshots and device-level visual comparison are required before declaring an exact match.

The V2 UI implementation is being developed around these five experiences:

- **Chat:** assistant welcome screen, focused conversations, offline local inference, contextual quick actions, clear model readiness, and voice entry.
- **Voice:** dedicated voice view with distinct listening and idle states; Android microphone permission and installed offline language remain mandatory.
- **Knowledge:** local source shelf, source retrieval, and inspectable provenance. The design shows a richer library than the current simple text/Markdown search, so unsupported document formats are not represented as implemented.
- **Models:** local model management, verified downloads, device guidance, and transparent installed/available status. Models pictured in the design are illustrative, not a compatibility guarantee.
- **Workspace:** structured capabilities and future workflows. Unimplemented agent actions are marked **Planned** rather than exposed as functional controls.

**Visual acceptance:** compare Android screenshots to the approved concept on a typical phone and small phone at default and large font size; verify content alignment, navigation, button sizes, contrast, keyboard/inset handling, loading/error states, and reduced-motion behavior. An approximation is not an exact match until the screenshots and real-device interaction tests establish it. The underlying app continues to use Kotlin, Compose, and local-first runtime boundaries.

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

## Update Zalmoxis from GitHub on your phone

Open **Settings → App updates → Check for updates**. Zalmoxis also checks for new signed releases when opened and displays a notice when one exists. Choose **Download verified update**, then **Install update**. Android asks you to authorize the installation; the app never silently installs anything. The update checker requests the public GitHub Releases API only (no sign-in or GitHub token). It downloads only from the Zalmoxis GitHub release channel and checks the SHA-256 digest, package name, increasing version code, and signing certificate before offering the installer.

**Why GitHub releases instead of a raw Actions artifact?** The normal `zalmoxis-debug-apk` workflow artifact is a ZIP archive and may expire or require GitHub authentication. Every successful, **main-branch** workflow can publish a **signed Android pre-release asset** named `zalmoxis-update.apk` when the four persistent signing secrets below are configured. This is the same workflow that uploads the regular debug artifact. Pull-request builds cannot publish installable phone updates. If the signing secrets are absent, CI still attempts the debug build but intentionally skips publishing an update.

### One-time signing setup for maintainers

Generate an Android application signing keystore **once**, keep it backed up securely, and configure these four GitHub Actions repository secrets (Settings → Secrets and variables → Actions):

- `ZALMOXIS_SIGNING_KEYSTORE_B64` — base64-encoded contents of the JKS/PKCS12 signing keystore (single line, no newlines)
- `ZALMOXIS_SIGNING_STORE_PASSWORD` — keystore password
- `ZALMOXIS_SIGNING_KEY_ALIAS` — signing key alias
- `ZALMOXIS_SIGNING_KEY_PASSWORD` — signing key password

Never commit the keystore or passwords into Git. GitHub Actions assembles a signed release APK with an automatically increasing `versionCode` based on the workflow run number and publishes `android-build-<run_number>` as a pre-release. The GitHub release asset's SHA-256 digest is checked before installation. A workflow artifact alone is not an update publication.

**Important existing-install migration:** Android updates require the **same signing certificate** as the installed copy. An existing APK signed with another debug key cannot be updated in place by the new release key. A one-time reinstall may be necessary and may erase app-private data; export or back up anything important **before** removing the old app. Thereafter, keep using the **same** release keystore for every build. Losing or replacing it blocks updates over previously installed versions.

The updater is currently an implementation in source, **not a verified running feature**. It must pass a real APK build and two consecutive signed-update installation tests (including denial/cancellation, no-network and tampered-APK cases) before being marked ready for deployment. Google Play distribution would require separate policy and installer-compliance review.

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
