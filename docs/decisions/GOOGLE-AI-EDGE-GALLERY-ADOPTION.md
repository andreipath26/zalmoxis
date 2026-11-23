# Google AI Edge Gallery adoption for Zalmoxis

**Status:** implementation baseline started, 2026-11-23.

## Adopted now

- LiteRT-LM Android runtime, pinned to com.google.ai.edge.litertlm:litertlm-android:0.18.1. Kotlin 2.3.20 is selected because LiteRT-LM 0.18.x publishes Kotlin 2.4 metadata and its current getting-started guide requires Kotlin 2.3 or newer.
- Gallery's local model flow: explicit local file selection, import into app-private storage, background initialization, and streaming text display.
- Gallery's model-manager pattern: a private model shelf with selection, deletion, and restoration of the last-used model.
- Gallery's inference observability pattern: show first-text latency, total generation time, and stream-event counts. Stream events are not presented as a benchmark token count.
- A repeatable warm benchmark: one discarded warm-up, then three fresh-conversation measurements on a fixed prompt; device, model, run latency, elapsed time, and event counts are saved to a private JSON report.
- Gallery's device-first inference shape: the initial runtime uses CPU as a universal fallback. The adapter can grow backend selection and measurement after checking supported hardware/model combinations.

## Build and measurement

The app includes a fixed-prompt warm benchmark that saves raw local JSON samples and device metadata. A branch-scoped GitHub Actions workflow is configured to install Android SDK 36, assemble a debug APK, and upload it as an artifact. The first two attempts failed before a runner was assigned, so the build remains unverified.

## Security boundary

The initial conversation registers no tools, downloads no skills, and cannot call MCP, JavaScript, Android intents, or Accessibility APIs. A model response is text only. This preserves the invariant that model output cannot authorize or execute consequential actions.

The app also omits Gallery's Firebase analytics/messaging, Hugging Face OAuth setup, and unrelated media/vision integrations. It currently has no Android INTERNET permission. Model distribution and each model's license must be assessed separately before adding discovery, downloads, or bundled weights.

## Gallery capability adoption map

This map covers the rest of the Gallery surface found in its current Android source and project README. The entries extend the integration plan; they do not remove or rewrite Zalmoxis roadmap requirements.

| Gallery capability | Zalmoxis use | Current state / integration boundary |
|---|---|---|
| LiteRT-LM local model inference | Local chat, later image/audio-capable model sessions | CPU text path is implemented. Add backend selection only with device/model measurements. |
| Model discovery and downloads | Discover compatible local models and manage them | Private local shelf is implemented. Gallery's Hugging Face catalog/download flow is a reference for an opt-in catalog, transfer progress, resume, gated-model auth, and model metadata. Before network downloads, bind every artifact to source/revision, size, checksum, license, and deletion state. |
| Model benchmark and device metrics | Target-phone model gate and reproducible performance records | Fixed-prompt warm benchmark is implemented with first-text latency, elapsed time, and stream events. Extend with engine-reported token counts, prefill/decode measurements, memory, temperature, and battery where the runtime exposes them; do not infer tok/s from event count. |
| Image and audio input | Local document/photo/audio workflows | Image attachment is implemented through Android's document picker and a private temporary file, with no broad media permission; models must support vision. Add audio with explicit picker or recorder permissions and per-model capability checks. |
| Speech recognition and synthesis | Voice interaction in the roadmap | Gallery's service abstraction and selectable ASR/TTS engines are useful references. Verify true offline behavior, device coverage, language support, permissions, and model/license requirements before connecting a provider. |
| Embeddings and semantic media search | Provenance-aware memory and document retrieval | Gallery's on-device embedding and media-search flows are a strong implementation reference for local embedding, index creation, and retrieval evaluation. Integrate behind a Zalmoxis memory-provider contract and compare relevance, deletion, identity isolation, latency, and storage use. |
| Agent skills, MCP, JavaScript, and Android intents | Future bounded workflows and integrations | Reuse typed schemas, status presentation, and consent UX as design references. Keep tools unregistered in the base conversation. Every future invocation must pass Zalmoxis's exact-parameter policy gate and audit path; never accept Gallery's automatic execution or “always allow” as the authority. |
| Mobile Actions and FunctionGemma demos | Local function-call experiments | Useful as a controlled simulator for measuring proposal quality and malformed calls. It does not replace Android platform permission checks or Zalmoxis authorization. |
| Prompt Lab | Prompt and model regression workflow | Build a local prompt-evaluation screen on top of the current benchmark/report format, with versioned prompt cases and explicit memory/privacy assertions. |
| DataStore, WorkManager, and model-download workers | Persist settings and run user-visible transfers | Reuse patterns for durable settings and cancellable downloads when those features are integrated. Do not run inference or consequential actions as invisible background work. |
| Firebase analytics and messaging | No current Zalmoxis requirement | These Gallery app integrations conflict with the current no-network baseline and are not in this app foundation. Any later telemetry or push behavior needs an explicit product and privacy decision. |
| XR, video moments, and media cards | Possible multimodal workflows | Gallery proves these are buildable on-device experiences; map them to concrete Zalmoxis memory or ingestion tasks before adding their UI and dependencies. |

The Gallery repository is an experimental application, not a drop-in library module. The reusable technical seam is LiteRT-LM plus selected model-management, streaming, device-measurement, and media-processing patterns. Zalmoxis remains a separate app and retains its own runtime, memory, policy, and audit contracts.

## Sources

- AI Edge Gallery source and Apache-2.0 license: https://github.com/google-ai-edge/gallery
- Gallery Android app build: https://github.com/google-ai-edge/gallery/blob/main/Android/src/app/build.gradle.kts
- LiteRT-LM Kotlin getting started: https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md
- LiteRT-LM Android artifacts: https://maven.google.com/web/index.html#com.google.ai.edge.litertlm:litertlm-android

## Follow-on integration boundaries

1. Model discovery/transfer needs source, integrity metadata, license, and deletion handling. The local shelf now covers import, selection, deletion, and reload. Repeat benchmark reports remain in app-private storage.
2. Add a runtime interface before adopting a second engine; compare LiteRT-LM with KILN/llama.cpp on target phones and supported model formats.
3. Keep all action proposals in a deterministic authorization pipeline; never register an automatically executed tool with model inference.
4. Preserve Gallery's device/performance visibility and connect it to measured phone acceptance gates.
