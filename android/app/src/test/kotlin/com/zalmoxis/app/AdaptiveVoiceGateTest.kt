package com.zalmoxis.app
import org.junit.Assert.*
import org.junit.Test
class AdaptiveVoiceGateTest {
    @Test fun speechAndSilence(){
        val gate=AdaptiveVoiceGate()
        val silence=ByteArray(640)
        val loud=ByteArray(640){ if(it%2==0)0 else 16 }
        repeat(20){assertFalse(gate.accept(silence,silence.size))}
        repeat(3){gate.accept(loud,loud.size)}
        assertTrue(gate.speaking)
        repeat(12){gate.accept(silence,silence.size)}
        assertFalse(gate.speaking)
    }
    @Test fun incompleteThoughtGetsLongerPause(){
        assertTrue(TurnRecovery.pauseBudget("I think because")>TurnRecovery.pauseBudget("I'm done."))
    }
}
