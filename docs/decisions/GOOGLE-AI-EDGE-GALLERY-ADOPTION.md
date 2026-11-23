# Google AI Edge Gallery adoption for Zalmoxis

**Status:** implementation baseline started, 2026-11-23.

## Adopted now

- LiteRT-LM Android runtime, pinned to com.google.ai.edge.litertlm:litertlm-android:0.18.1. Kotlin 2.3.20 is selected because LiteRT-LM 0.18.x publishes Kotlin 2.4 metadata and its current getting-started guide requires Kotlin 2.3 or newer.
- Gallery's local model flow: explicit local file selection, import into app-private storage, background initialization, and streaming text display.
- Gallery's model-manager pattern: a private model shelf with selection, deletion, and restoration of the last-used model.
- Local model provenance: imports receive a private SHA-256 receipt, selected-file source label, import time, and explicit unknown-license state; the digest is checked before load. It identifies the local artifact but does not authenticate the publisher or validate licensing.
- Gallery's inference observability pattern: show first-text latency, total generation time, and stream-event counts. Stream events are not presented as a benchmark token count.
- A repeatable warm benchmark: one discarded warm-up, then three fresh-conversation measurements on a fixed prompt; device, model SHA-256, runtime/backend, latency, elapsed time, and event counts are saved to a private JSON report that the user can export through Android's document picker.
- Gallery's device-first inference shape: the initial runtime uses CPU as a universal fallback. The adapter can grow backend selection and measurement after checking supported hardware/model combinations.
- Voice input/output: on-device Android speech recognition and user-triggered Android TTS that selects an installed voice reporting no network requirement.

## Build and measurement

The app includes a fixed-prompt warm benchmark that saves raw local JSON samples, model SHA-256, and device metadata; the report can be exported by explicit user action. A branch-scoped GitHub Actions workflow is configured to install Android SDK 36, assemble a debug APK, and upload it as an artifact. The first two attempts failed before a runner was assigned, so the build remains unverified.

## Security boundary

The initial conversation registers no tools, downloads no skills, and cannot call MCP, JavaScript, Android intents, or Accessibility APIs. A model response is text only. This preserves the invariant that model output cannot authorize or execute consequential actions.

The app also omits Gallery's Firebase analytics/messaging, Hugging Face OAuth setup, and unrelated media/vision integrations. It currently has no Android INTERNET permission. Model distribution and each model's license must be assessed separately before adding discovery, downloads, or bundled weights.

## Gallery capability adoption map

This map covers the rest of the Gallery surface found in its current Android source and project README. The entries extend the integration plan; they do not remove or rewrite Zalmoxis roadmap requirements.

| Gallery capability | Zalmoxis use | Current state / integration boundary |
|---|---|---|
| LiteRT-LM local model inference | Local chat, later image/audio-capable model sessions | An OnDeviceInferenceRuntime contract and LiteRT-LM CPU adapter are implemented. Add accelerator selection only with device/model measurements. |
| Model discovery and downloads | Discover compatible local models and manage them | Private local shelf is implemented. Each import has a private receipt with SHA-256, local source label, import time, and an explicit unknown-license state; the digest is checked before load and identifies the exact model in benchmark JSON. This detects later local file changes but does not authenticate the publisher or verify licensing. Gallery's Hugging Face catalog/download flow is a reference for an opt-in catalog, transfer progress, resume, gated-model auth, and model metadata. Before network downloads, bind every artifact to source/revision, size, checksum, license, and deletion state. |
| Model benchmark and device metrics | Target-phone model gate and reproducible performance records | Fixed-prompt warm benchmark is implemented with first-text latency, elapsed time, and stream events. Extend with engine-reported token counts, prefill/decode measurements, memory, temperature, and battery where the runtime exposes them; do not infer tok/s from event count. |
| Image and audio input | Local document/photo/audio workflows | Image attachment is implemented through Android's document picker and a private temporary file, with no broad media permission; models must support vision. Speech-to-text dictation uses Android's available on-device recognition service. Direct audio-to-model input remains a separate model capability and needs explicit permission/capability checks. |
| Speech recognition and synthesis | Voice interaction in the roadmap | On-device dictation uses Android SpeechRecognizer on API 31+ when available; RECORD_AUDIO is requested only after a tap and there is no network-recognition fallback. Offline speech output is user-triggered and filters installed Android TTS voices to those reporting that network access is not required. Voice inventory and quality still vary by device; model-based ASR remains a separate capability. |
| Embeddings and semantic media search | Provenance-aware memory and document retrieval | Gallery's on-device embedding and media-search flows are a strong implementation reference for local embedding, index creation, and retrieval evaluation. Integrate behind a Zalmoxis memory-provider contract and compare relevance, deletion, identity isolation, latency, and storage use. |
| Agent skills, MCP, JavaScript, and Android intents | Future bounded workflows and integrations | Reuse typed schemas, status presentation, and consent UX as design references. Keep tools unregistered in the base conversation. Every future invocation must pass Zalmoxis's exact-parameter policy gate and audit path; never accept Gallery's automatic execution or “always allow” as the authority. |
| Mobile Actions and FunctionGemma demos | Local function-call experiments | Useful as a controlled simulator for measuring proposal quality and malformed calls. It does not replace Android platform permission checks or Zalmoxis authorization. |
| Prompt Lab | Prompt and model regression workflow | Build a local prompt-evaluation screen on top of the current benchmark/report format, with versioned prompt cases and explicit memory/privacy assertions. |
| DataStore, WorkManager, and model-download workers | Persist settings and run user-visible transfers | Use pinned public catalog entries, expected size and SHA-256, license metadata, storage preflight, resumable Range transfer, visible progress/cancel/retry, and verified atomic install with provenance. Keep credentials out of durable work input; do not run inference or consequential actions as invisible background work. |
| Firebase analytics and messaging | No current Zalmoxis requirement | These Gallery app integrations conflict with the current no-network baseline and are not in this app foundation. Any later telemetry or push behavior needs an explicit product and privacy decision. |
| XR, video moments, and media cards | Possible multimodal workflows | Gallery proves these are buildable on-device experiences; map them to concrete Zalmoxis memory or ingestion tasks before adding their UI and dependencies. |

The Gallery repository is an experimental application, not a drop-in library module. The reusable technical seam is LiteRT-LM plus selected model-management, streaming, device-measurement, and media-processing patterns. Zalmoxis remains a separate app and retains its own runtime, memory, policy, and audit contracts.

## Sources

- AI Edge Gallery source and Apache-2.0 license: https://github.com/google-ai-edge/gallery
- Gallery Android app build: https://github.com/google-ai-edge/gallery/blob/main/Android/src/app/build.gradle.kts
- LiteRT-LM Kotlin getting started: https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md
- LiteRT-LM Android artifacts: https://maven.google.com/web/index.html#com.google.ai.edge.litertlm:litertlm-android

## Follow-on integration boundaries

1. The pinned public model catalog and verified WorkManager transfer path are implemented in source, including source/revision, integrity metadata, license, progress, cancellation, retry, and private shelf installation. Build and device transfer validation remain outstanding; API 34+ User-Initiated Data Transfer remains the recommended route for long explicit downloads.
2. The runtime interface is in place. Implement and compare a KILN/llama.cpp adapter on target phones and supported model formats before claiming Android GGUF/KILN support.
3. Keep all action proposals in a deterministic authorization pipeline; never register an automatically executed tool with model inference.
4. Preserve Gallery's device/performance visibility and connect it to measured phone acceptance gates.

## Pinned model-download integration (2026-11-23)

The first catalog batch is limited to public LiteRT-LM artifacts with a pinned repository commit, exact filename, expected byte count, SHA-256, and Apache-2.0 label. Candidate entries:

| Catalog entry | Source | Artifact | Size | RAM / modality | SHA-256 |
|---|---|---|---:|---|---|
| Qwen2.5 1.5B Instruct | [litert-community/Qwen2.5-1.5B-Instruct](https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/tree/5e6e7a58d03777898b31330a15d2e0a5a9107de3) at revision `5e6e7a58d03777898b31330a15d2e0a5a9107de3` | `Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm` | 1,597,922,856 bytes | 6 GB guidance / text | `c4a9b7289365ab80fe599b844b77ad253eb3c53fb0e5d963d699146a6f01255e` |
| Gemma 4 E2B Instruct | [litert-community/gemma-4-E2B-it-litert-lm](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/6e5c4f1e395deb959c494953478fa5cec4b8008f) at revision `6e5c4f1e395deb959c494953478fa5cec4b8008f` | `gemma-4-E2B-it.litertlm` | 2,588,147,712 bytes | 8 GB guidance / text, image, audio | `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c` |

The listed sizes and digests pin the expected artifacts; they do not replace checking each upstream model card and license. Catalog downloads contact Hugging Face. Model prompts, inference, imported files, and benchmark reports remain on-device; no prompts or reports are uploaded by the downloader. The Compose catalog controls let the user start a transfer, cancel it while queued or running, review byte progress, and retry a failed or cancelled transfer. The button shows “Installed” after verified installation; the user then chooses “Load” from the model shelf. A transfer never starts inference automatically.

The transfer worker must accept only a catalog identifier, construct URLs from allowlisted fields, follow HTTPS redirects only to Hugging Face hosts, preflight app-private storage, resume only after validating HTTP range metadata, and check exact size and SHA-256 before atomically installing the file with a provenance receipt. Partial files must not appear in the loadable model shelf. No bearer token belongs in persisted WorkManager input; this first catalog contains public entries only.

The implemented coordinator routes API 34+ user-initiated downloads through Android User-Initiated Data Transfer jobs and uses WorkManager on API 26–33. It exposes one observable state contract for progress, cancellation, retry, and errors. Android 16's JobScheduler quota caveat is addressed by the API 34+ route for long user-started transfers; the fallback remains for older supported devices. The Compose catalog controls are now wired to this coordinator and refresh the private model shelf after verified install. This source is not release-verified until the Android build and an on-device transfer both succeed.

## Additional model-source references

- Qwen model card: https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct
- Gemma 4 model card: https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- Android long-running WorkManager guidance: https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running
- Android User-Initiated Data Transfer guidance: https://developer.android.com/develop/background-work/background-tasks/uidt
