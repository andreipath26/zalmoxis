# Zalmoxis Android foundation

This is the first Android application foundation. It adapts Google AI Edge
Gallery's local model workflow behind a Zalmoxis-owned on-device runtime
interface; LiteRT-LM is the first runtime adapter.

## What is present

- A Jetpack Compose Android shell.
- Local model import through Android's document picker into private app storage.
- An OnDeviceInferenceRuntime contract and LiteRT-LM CPU adapter with streaming text and optional image input for compatible multimodal models.
- A private local model shelf with size and SHA-256 display, selection, deletion, last-model restoration, local digest verification before loading, and explicit unknown-license labeling for local imports. Invalid receipts keep the model visible for replacement but disable loading. The digest identifies the local file but does not authenticate its publisher.
- A pinned catalog for public Qwen 2.5 1.5B and Gemma 4 E2B LiteRT-LM artifacts, showing source revision, size, checksum, license, RAM guidance, modalities, and download progress. WorkManager provides visible cancellable transfers with retry/resume and verifies size plus SHA-256 before private installation. The user still explicitly selects Load; downloaded models do not execute automatically.
- Image selection through Android's document picker, copied to private cache and deleted after the request; no broad photo permission.
- On-device speech dictation through Android's on-device recognizer where available; microphone permission is requested only when dictation starts, with no network recognizer fallback.
- User-triggered speech output using an installed Android TTS voice that reports no network requirement; network-required voices are never selected.
- Basic per-turn first-text latency and elapsed-time measurements.
- A fixed-prompt benchmark with one warm-up and three measured runs; raw results are saved as JSON with device and model metadata and can be exported through Android's document picker on explicit request.
- Network access downloads only the pinned public model artifacts from Hugging Face; prompts, inference, and benchmark reports stay on device. No account sign-in, analytics SDK, MCP client, or model-provided tool execution is enabled.
- The model is instructed that it can propose actions but has no tools.

## Build

Use JDK 17 and Android SDK 36. From this directory run:

    ./gradlew :app:assembleDebug

Android SDK 36 was unavailable and the Gradle distribution could not be fetched in the implementation workspace. The branch includes a GitHub Actions workflow configured to assemble the debug APK. Its first two attempts failed before a runner was assigned, so no APK has been produced. Select a compatible LiteRT-LM model
file on-device to use the chat screen. Model architecture, hardware support,
licensing, and redistribution rights are model-specific; inspect those before
packaging model weights.

## Gallery reuse

Reference: https://github.com/google-ai-edge/gallery (Apache-2.0).
Its local model loading, streaming inference, and device-side workflow informed
this foundation. The app calls the standalone LiteRT-LM Kotlin API, which is
also the engine used by Gallery for local inference:
https://github.com/google-ai-edge/LiteRT-LM

Gallery-specific agent tools and network features are not wired into Zalmoxis. Model download source is present, but Android build and on-device transfer validation remain outstanding. For long explicit downloads, add Android 14+ User-Initiated Data Transfer support; retain WorkManager fallback for API 26–33 and account for Android 16 JobScheduler quota limits.
The Zalmoxis permission engine must remain deterministic and outside the model.
Any future tool call must be represented as a proposal, evaluated against exact
parameters and an expiring grant, and recorded before execution.
