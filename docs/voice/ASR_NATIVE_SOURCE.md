# Sherpa-ONNX Android native runtime

- Official source: https://github.com/k2-fsa/sherpa-onnx
- Pinned release: v2.0.3 (2026-11)
- Official release AAR: `sherpa-onnx-2.0.3.aar`
- SHA-256: `4c766e2e74c9d33d595338c21fe3b305c73ba45d47e96a235675c33142a2ec50`
- Download size: 52,411,426 bytes.
- Android builds fetch it **at CI build time**, verify the exact SHA-256 and package native code into the APK. Users do not separately download the runtime.
- Speech model weights are separate and must follow the privacy, verification and low-storage rules in `LOW_STORAGE_MODEL_PLAN.md`.
- Runtime class: `SherpaStreamingRecognizer`. Requires exact encoder/decoder/joiner/tokens files and rejects missing packs.
- Supported languages are declared by the *installed model*, not automatically inferred from the phone locale. French, English and Romanian must each be verified; do not mislabel a monolingual pack as multilingual.
- No microphone audio is stored, uploaded, or forwarded outside the phone.
