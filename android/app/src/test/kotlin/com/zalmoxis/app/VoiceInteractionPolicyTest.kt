package com.zalmoxis.app
import org.junit.Assert.*
import org.junit.Test
class VoiceInteractionPolicyTest {
    @Test fun echoAndBackchannelsDoNotInterrupt() {
        assertFalse(VoiceInteractionPolicy.isInterruption("okay", "Something else"))
        assertFalse(VoiceInteractionPolicy.isInterruption("the weather", "Let's discuss the weather tomorrow"))
        assertTrue(VoiceInteractionPolicy.isInterruption("stop", "the weather"))
        assertTrue(VoiceInteractionPolicy.isInterruption("change the topic", "the weather"))
    }
    @Test fun cleansSpeech() {
        assertEquals("Hello world", VoiceInteractionPolicy.cleanForSpeech("**Hello** world"))
        assertTrue(VoiceInteractionPolicy.pauseMillis("ja-JP") >= VoiceInteractionPolicy.pauseMillis("en-US"))
    }
}
