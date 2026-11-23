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
