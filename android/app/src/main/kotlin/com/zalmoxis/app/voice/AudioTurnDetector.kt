package com.zalmoxis.app.voice

import kotlin.math.sqrt

/**
 * Fast local voice-activity front-end. Not a substitute for Silero VAD or Smart Turn.
 * Model-backed endpointing takes priority whenever installed. The fallback is conservative
 * and never claims to know whether a linguistic sentence is complete.
 */
internal class AudioTurnDetector(
    private val sampleRate: Int = 16000,
    private val thresholdFactor: Float = 3.5f,
    private val minSpeechMs: Int = 160,
    private val minSilenceMs: Int = 700,
) {
    data class Frame(val speechStarted: Boolean, val speechEnded: Boolean, val active: Boolean, val rms: Float)
    private var noiseFloor = 170.0f
    private var speechMs = 0
    private var silenceMs = 0
    private var speaking = false

    fun accept(samples: ShortArray, count: Int): Frame {
        require(count in 0..samples.size)
        if (count == 0) return Frame(false, false, speaking, 0f)
        var sum = 0.0
        for (i in 0 until count) sum += samples[i].toDouble() * samples[i]
        val rms = sqrt(sum / count).toFloat()
        val durationMs = ((count.toLong() * 1000L) / sampleRate).toInt().coerceAtLeast(1)
        if (!speaking) noiseFloor = noiseFloor * 0.995f + rms.coerceAtMost(noiseFloor * 2.0f) * 0.005f
        val energetic = rms > maxOf(450f, noiseFloor * thresholdFactor)
        var started = false
        var ended = false
        if (energetic) {
            silenceMs = 0
            speechMs += durationMs
            if (!speaking && speechMs >= minSpeechMs) {
                speaking = true
                started = true
            }
        } else {
            if (speaking) {
                silenceMs += durationMs
                if (silenceMs >= minSilenceMs) {
                    speaking = false
                    ended = true
                    speechMs = 0
                    silenceMs = 0
                }
            } else {
                speechMs = 0
            }
        }
        return Frame(started, ended, speaking, rms)
    }

    fun reset() {
        speechMs = 0
        silenceMs = 0
        speaking = false
    }
}
