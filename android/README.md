# Zalmoxis Android foundation

This is the first Android application foundation. It uses the Google AI Edge
Gallery's local model workflow as a reference and LiteRT-LM as its on-device
inference engine.

## What is present

- A Jetpack Compose Android shell.
- Local model import through Android's document picker into private app storage.
- A LiteRT-LM CPU backend with streaming text responses.
- A private local model shelf with size display, selection, deletion, and last-model restoration.
- Basic per-turn first-text latency, elapsed time, and stream-event measurements.
- No network permission, account sign-in, analytics SDK, MCP client, or model-provided tool execution.
- The model is instructed that it can propose actions but has no tools.

## Build

Use JDK 17 and Android SDK 36. From this directory run:

    ./gradlew :app:assembleDebug

The Android SDK was not available in the implementation workspace, so this
initial source has not been built here. Select a compatible LiteRT-LM model
file on-device to use the chat screen. Model architecture, hardware support,
licensing, and redistribution rights are model-specific; inspect those before
packaging model weights.

## Gallery reuse

Reference: https://github.com/google-ai-edge/gallery (Apache-2.0).
Its local model loading, streaming inference, and device-side workflow informed
this foundation. The app calls the standalone LiteRT-LM Kotlin API, which is
also the engine used by Gallery for local inference:
https://github.com/google-ai-edge/LiteRT-LM

Gallery-specific agent tools and network features are not wired into Zalmoxis.
The Zalmoxis permission engine must remain deterministic and outside the model.
Any future tool call must be represented as a proposal, evaluated against exact
parameters and an expiring grant, and recorded before execution.
