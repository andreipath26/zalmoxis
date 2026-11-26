# Live voice acceptance

Google ML Kit GenAI ASR/TTS and existing LiteRT-LM are used; no separate LLM or bundled voice weights. Google-managed voice provisioning can require a download. Automatic interruption is experimental and gated on hardware acoustic echo cancellation. On unsupported devices use the interrupt button. The microphone is released when leaving the voice screen or backgrounding the activity.

Device acceptance (not yet measured):
- Pixel 10, English and French: 30-minute conversation; silence and long pauses; interrupt at start/middle/end of spoken phrase.
- Measure end-of-turn, first audible output and interruption latency with external timestamps; targets 400–800 ms, 1.5 s and 200 ms respectively.
- Test speaker, wired/Bluetooth headset, noisy room, volume changes, no echo self-trigger, permission denial/revocation, screen exit, app background, airplanemode after provisioning.
- Test unsupported AEC and unavailable neural voice; verify manual interruption and installed offline speech fallback; no network fallback for ASR.
- Check model conversation continuity after cancellation, memory usage, thermal behavior, and repeated start/stop.
- CI assembles APK and runs unit tests; a passing build does not certify acoustic behavior.

## Embedded Kokoro (LiteRT)
Uses Google's MediaPipe Tasks Audio 1.2.2 and the upstream checksum-pinned CPU/GPU Kokoro bundle (approximately 295 MB). GPU initialization is preferred, CPU initialization is the compatibility fallback. The Voice screen exposes all 54 voice presets across eight languages, with two English dialects. No Sherpa or ONNX dependency. Unsupported Kokoro languages use Google or Android offline speech. Validate native library coexistence with LiteRT-LM, every language, GPU/CPU, playback cancellation and installation/removal on physical hardware.
