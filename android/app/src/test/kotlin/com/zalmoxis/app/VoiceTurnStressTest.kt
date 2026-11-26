package com.zalmoxis.app
import org.junit.Test
import org.junit.Assert.*
class VoiceTurnStressTest {
    @Test fun repeatedAcousticTurnsDoNotLatch(){
        val gate=AdaptiveVoiceGate()
        val speech=ByteArray(640){if(it%2==0)0 else 12}
        val silence=ByteArray(640)
        repeat(1000){
            repeat(3){gate.accept(speech,640)}
            assertTrue(gate.speaking)
            repeat(12){gate.accept(silence,640)}
            assertFalse(gate.speaking)
        }
    }
    @Test fun truncatedFramesAreSafe(){
        val gate=AdaptiveVoiceGate()
        assertFalse(gate.accept(byteArrayOf(1),1))
        assertFalse(gate.accept(byteArrayOf(),0))
    }
}
