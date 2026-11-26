package com.zalmoxis.app.voice

/**
 * Pure deterministic guard for a full-duplex voice session. The guard assigns an epoch to
 * each turn; any ASR/LLM/TTS callback from an old epoch is rejected after barge-in/stop.
 * Native microphone and model callbacks must never call Android UI APIs themselves.
 */
internal class VoiceTurnGate {
    enum class Phase { OFF, LISTENING, ENDPOINTING, THINKING, SPEAKING, INTERRUPTED, ERROR }

    var phase: Phase = Phase.OFF
        private set
    var epoch: Long = 0L
        private set

    fun start(): Long {
        epoch++
        phase = Phase.LISTENING
        return epoch
    }

    fun hearingSpeech(token: Long): Boolean {
        if (!isCurrent(token) || phase == Phase.OFF) return false
        if (phase == Phase.SPEAKING || phase == Phase.THINKING) {
            epoch++ // Invalidate all ongoing inference and audio streams atomically.
            phase = Phase.INTERRUPTED
            return true
        }
        if (phase == Phase.LISTENING || phase == Phase.ENDPOINTING) {
            phase = Phase.LISTENING
        }
        return false
    }

    fun endpoint(token: Long): Boolean {
        if (!isCurrent(token) || phase != Phase.LISTENING) return false
        phase = Phase.ENDPOINTING
        return true
    }

    fun thinking(token: Long): Boolean {
        if (!isCurrent(token) || phase != Phase.ENDPOINTING) return false
        phase = Phase.THINKING
        return true
    }

    fun speaking(token: Long): Boolean {
        if (!isCurrent(token) || phase != Phase.THINKING) return false
        phase = Phase.SPEAKING
        return true
    }

    fun completeAudio(token: Long): Boolean {
        if (!isCurrent(token) || phase != Phase.SPEAKING) return false
        phase = Phase.LISTENING
        return true
    }

    fun resumeAfterInterruption(): Long {
        require(phase == Phase.INTERRUPTED) { "No active interruption" }
        phase = Phase.LISTENING
        return epoch
    }

    fun isCurrent(token: Long): Boolean = token == epoch && phase != Phase.OFF && phase != Phase.ERROR

    fun fail() {
        epoch++
        phase = Phase.ERROR
    }

    fun stop() {
        epoch++
        phase = Phase.OFF
    }
}
