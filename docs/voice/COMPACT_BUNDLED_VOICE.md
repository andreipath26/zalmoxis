# Bundled compact voice source and size

First-run offline voice consists of:
- **Kitten Mini v0.8** TTS, sherpa-onnx native, English, 8 voices. Official archive size **67,547,594 bytes**. Pinned SHA-256: `518f9b130320f690d5b5476df77bde4215fca67773cda16710318e5081234b9d`.
- **Silero VAD** official release ONNX, **643,854 bytes**. Pinned SHA-256: `9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6`.
- **Streaming Zipformer ASR** must be pinned to a trusted exact set of files and verified before bundling. Its original all-precision GitHub archive is huge (310,414,022 bytes), but the `int8` encoder plus decoder/joiner/tokens are much smaller. Bundle only required inference files, never the full archive or development metadata.
- **Native runtime sherpa-onnx AAR** 52,411,426 bytes embedded in APK by Gradle.
- **Premium Qwen3-TTS** is intentionally **not bundled**; an explicit user selection can load a fully verified compatible quantized GGUF talker + tokenizer bundle, with a hard budget.

**Do not call first-run offline voice ready until the complete ASR pack and native speech routes have been built and tested on a phone.** A versioned APK includes compiled code and initial models; it still occupies local storage, but avoids separate large post-install downloads.

Sources:
- https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models
- https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models
