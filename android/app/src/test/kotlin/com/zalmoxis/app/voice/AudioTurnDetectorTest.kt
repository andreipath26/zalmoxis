package com.zalmoxis.app.voice

import org.junit.Assert.*
import org.junit.Test

class AudioTurnDetectorTest {
    @Test fun sustainedVoiceStartsAndSilenceEnds() {
        val detector = AudioTurnDetector()
        repeat(7) { assertFalse(detector.accept(ShortArray(320) { 8000 }, 320).speechStarted) }
        assertTrue(detector.accept(ShortArray(320) { 8000 }, 320).speechStarted)
        var ended = false
        repeat(36) { ended = detector.accept(ShortArray(320), 320).speechEnded || ended }
        assertTrue(ended)
    }

    @Test fun shortNoiseSpikeDoesNotStartConversation() {
        val detector = AudioTurnDetector()
        detector.accept(ShortArray(320) { 12000 }, 320)
        repeat(10) { assertFalse(detector.accept(ShortArray(320), 320).speechStarted) }
    }
}
