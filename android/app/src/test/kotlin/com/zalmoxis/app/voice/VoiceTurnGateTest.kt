package com.zalmoxis.app.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceTurnGateTest {
    @Test fun oldAudioCannotRestartAfterBargeIn() {
        val gate = VoiceTurnGate()
        val first = gate.start()
        assertTrue(gate.endpoint(first))
        assertTrue(gate.thinking(first))
        assertTrue(gate.speaking(first))
        assertTrue(gate.hearingSpeech(first))
        assertEquals(VoiceTurnGate.Phase.INTERRUPTED, gate.phase)
        assertFalse(gate.completeAudio(first))
        val second = gate.resumeAfterInterruption()
        assertNotEquals(first, second)
        assertTrue(gate.endpoint(second))
        assertTrue(gate.thinking(second))
        assertTrue(gate.speaking(second))
        assertTrue(gate.completeAudio(second))
        assertEquals(VoiceTurnGate.Phase.LISTENING, gate.phase)
    }

    @Test fun stoppingInvalidatesEveryInFlightCallback() {
        val gate = VoiceTurnGate()
        val token = gate.start()
        gate.stop()
        assertFalse(gate.endpoint(token))
        assertFalse(gate.isCurrent(token))
        assertEquals(VoiceTurnGate.Phase.OFF, gate.phase)
    }

    @Test fun cannotSpeakBeforeInference() {
        val gate = VoiceTurnGate()
        val token = gate.start()
        assertFalse(gate.speaking(token))
        assertTrue(gate.endpoint(token))
        assertTrue(gate.thinking(token))
        assertTrue(gate.speaking(token))
    }

    @Test fun failClosesEpoch() {
        val gate = VoiceTurnGate()
        val token = gate.start()
        gate.fail()
        assertFalse(gate.isCurrent(token))
        assertEquals(VoiceTurnGate.Phase.ERROR, gate.phase)
    }
}
