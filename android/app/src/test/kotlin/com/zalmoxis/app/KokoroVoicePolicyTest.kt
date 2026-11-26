package com.zalmoxis.app

import org.junit.Assert.*
import org.junit.Test

class KokoroVoicePolicyTest {
    @Test fun onlyInstalledOfflineMatchingKokoro() {
        assertTrue(KokoroVoicePolicy.isCandidate("soniqo-kokoro-af-heart", "en", "en", false, emptySet()))
        assertFalse(KokoroVoicePolicy.isCandidate("kokoro", "en", "fr", false, emptySet()))
        assertFalse(KokoroVoicePolicy.isCandidate("kokoro", "en", "en", true, emptySet()))
        assertFalse(KokoroVoicePolicy.isCandidate("kokoro", "en", "en", false, setOf("notInstalled")))
        assertFalse(KokoroVoicePolicy.isCandidate("ordinary-voice", "en", "en", false, emptySet()))
    }
}
