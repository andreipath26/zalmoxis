# Zalmoxis native, offline, full-duplex voice integration

**Work branch:** `feat/full-duplex-qwen-voice`. **Release status: IN PROGRESS; not an APK feature yet.**

## Non-negotiable requirements
- No Android `SpeechRecognizer` or Android `TextToSpeech` in the replacement conversation path.
- Fully local ASR + VAD + model inference + Qwen3-TTS voice generation.
- Microphone capture remains active while output plays; stop and revoke immediately.
- Reject callbacks for cancelled turns, never repeat stale audio.
- No cloud voice fallback and no silent downloads. Voice models have file hashes and licenses.
- Qwen3-TTS is primary; any fallback is an explicit user choice with verified support.

## Native runtime integration boundary

The official Qwen3-TTS LiteRT-LM bundle `qwen3_tts_int8_v2.litertlm` is **2.44 GB** and is *not* a text-only LiteRT-LM `Engine` model. It uses the C++ OmniEngine/OmniSession API and emits 24kHz PCM chunks. Do not pass it to `LiteRtLmRuntime`. Native JNI bridge, pinned upstream build and model provisioning must land before this feature can ship.

Primary source: https://github.com/google-ai-edge/LiteRT-LM/blob/main/omni/docs/Qwen3-TTS-12Hz-0.6B-Base.README.md

Android speech recognition: sherpa-onnx Android AAR with its native `.so` files and an explicitly provisioned compatible offline model. Do not pull a JVM-only artifact or copy binaries without verifying ABI and licenses.

Primary source: https://github.com/k2-fsa/sherpa-onnx/tree/master/android/SherpaOnnxAar

Existing pipelines to evaluate for licensed reuse:
- https://github.com/loyality7/speech-to-speech-mobile
- https://github.com/m15-ai/droidkaigi2026-app-gvp
- https://github.com/pipecat-ai/smart-turn

## Progress gates
1. [x] Dedicated branch, initial deterministic interruption state guard and microphone PCM/AEC substrate
2. [ ] Package licensed Android ASR, VAD and exact model artifacts; implement continuous streaming recognition
3. [ ] Build pinned LiteRT-LM Omni speech native library, Kotlin/JNI adapter and chunked AudioTrack playback
4. [ ] Combine streaming ASR, turn-completion policy, cancellable LLM and streamed Qwen PCM in session orchestrator
5. [ ] Prove acoustic echo suppression, interruption, Bluetooth routing and cancellation on real device
6. [ ] Replace the Voice screen, implement explicit microphone controls, health indicators and privacy settings
7. [ ] Performance tuning with offline 10-minute real conversations, French/English and battery tests
8. [ ] Complete all code, then open/finish single PR, merge, then run CI and APK as owner instructed

**No premature CI claim.** Test source is added but tests are not run until the requested final integration gate. No PR merge or Android APK build before all batches are ready.
