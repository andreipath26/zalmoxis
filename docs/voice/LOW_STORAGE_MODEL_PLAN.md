# Low-storage offline voice requirements

**Decision (2026-11-26):** Natural, interruptible offline speech must not entail a mandatory 2.44 GB Qwen download. Keep all eight voice-engine batches in the same feature branch and PR. Do not run CI/build before the integrated patch is ready.

## Evidence
- Google's Qwen3-TTS LiteRT-LM 0.6B `qwen3_tts_int8_v2.litertlm` is **2.44 GB**. The Omni runtime is distinct from the current text-only engine.
- GGUF Qwen3-TTS models have multiple quantized footprints, but **the entire speech generation pipeline includes the codec/tokenizer**. A ~500 MB talker alone is not a complete working speech model.
- An independent Q5 GGUF package exists at approximately **874 MB**, with different runtime and possible quality tradeoffs. Quality, performance, packaging and licensing require device verification.
- The compact Kokoro-82M ONNX variants can be around 122–160 MB in certain releases; language-specific phonemizers, voices and supporting libraries add overhead. Model bundles must be measured as a whole.
- Tiny or quantized ASR also consumes storage. A first-run offline voice requires both ASR and TTS; optimize the **combined** budget, not just TTS.

## Shipping policy
1. Prefer a **single compact voice model** bundled with the app when licenses and Play/App Bundle size constraints permit. No large first-run download; no automatic use of Android cloud voices.
2. **Qwen3-TTS remains the premium quality path**, but installing it is explicit and conditional. Never represent a small talker without decoder as complete TTS. Native Qwen inference must be implemented before promising it in UI.
3. No second copy of an identical model, no repeated download when the content hash is already available, no model duplication between ASR/TTS and LLM unless format/runtime compatibility is proven.
4. Never bypass storage preflight: checksum, exact size, complete license/provenance, sufficient rollback headroom, safe staging and cleanup.
5. Expose exact incremental storage cost before user approves installation, and permit deletion of unused speech model packs.
6. Keep voice models in app-private storage. No uploading microphones, transcripts or generated audio. No silent network inference.
7. Compact budget <= 350 MiB for **TTS**; premium budget <= 1,200 MiB for **complete TTS bundle**; preserve >= 768 MiB free for system/rollback. The overall speech stack still needs separately budgeted ASR, native libs and LLM.
8. The old system TTS must not become the voice conversation default. A lightweight replacement must produce audio through an actual local inference engine.

## Native reuse assessment
- `Danmoreng/qwen3-tts-android` MIT-licensed Android JNI Kotlin interface using `qwen3-tts.cpp`; the native source is an explicit git submodule pinned to commit `4562731dc612cb87b4cd1eedac275a17d1078773`.
- Vendor only the code required for playback and synthesis, with preserved copyright/license. Validate project ABI (arm64-v8a) and minSdk (upstream 31 versus Zalmoxis 26), then package native libraries via the existing GitHub Android CI.
- **Warning:** upstream `QwenEngine.synthesize` returns a complete FloatArray, not proof of live PCM streaming. Sentence-level segmentation is not equivalent to native sample streaming. Must verify low-latency sample playback and cancellation APIs.
- Do not copy another project’s APK or embed speech model binaries in source control.

## Acceptance gates
- Cold install: user can converse offline without downloading gigabytes, and UI displays accurate total installed voice+ASR size.
- Qwen: compatible complete model with voice + tokenizer + codec, explicit installation, license, size/hash verified, and no duplicate storage.
- Disk cleanup: no partial model debris after a cancelled or failed installation.
- Correctness: offline 10-minute duplex conversation with barge-in under realistic speaker and Bluetooth conditions, using actual local speech models.
- Benchmark the Qwen premium and compact engines separately for latency, naturalness, languages, RAM and battery.
