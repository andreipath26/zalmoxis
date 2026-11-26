package com.zalmoxis.app.voice

import android.content.Context
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig

/** Bundled 644 KB Silero VAD, real neural speech activity detection, no network. */
internal class SileroNativeVad(context: Context) : AutoCloseable {
    private val detector = Vad(
        assetManager = context.assets,
        config = VadModelConfig(
            sileroVadModelConfig = SileroVadModelConfig(
                model = "silero_vad.onnx",
                threshold = 0.55f,
                minSpeechDuration = 0.16f,
                minSilenceDuration = 0.40f,
                windowSize = 512,
                maxSpeechDuration = 25f,
            ),
            sampleRate = 16000,
            numThreads = 1,
            provider = "cpu",
        ),
    )
    private val pending = FloatArray(512)
    private var pendingCount = 0
    private var active = false

    data class Event(val began: Boolean, val ended: Boolean, val speaking: Boolean)

    fun acceptPcm16(samples: ShortArray): Event {
        var began = false
        var ended = false
        for (sample in samples) {
            pending[pendingCount++] = sample / 32768f
            if (pendingCount == pending.size) {
                detector.acceptWaveform(pending)
                pendingCount = 0
                val next = detector.isSpeechDetected()
                if (next && !active) began = true
                if (!next && active) ended = true
                active = next
                while (!detector.empty()) detector.pop()
            }
        }
        return Event(began, ended, active)
    }

    fun reset() {
        detector.reset()
        pendingCount = 0
        active = false
    }

    override fun close() = detector.release()
}
