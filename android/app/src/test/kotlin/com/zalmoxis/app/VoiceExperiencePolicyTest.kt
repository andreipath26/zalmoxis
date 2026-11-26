package com.zalmoxis.app
import org.junit.Test
import org.junit.Assert.*
import java.util.Locale
class VoiceExperiencePolicyTest {
    @Test fun qualityAndSpeedChooseDifferentPacks() {
        assertEquals("pocket",ExtraVoicePacks.ordered(VoiceMode.QUALITY,null,ExtraVoicePacks.all).first().id)
        assertEquals("phonon",ExtraVoicePacks.ordered(VoiceMode.FAST,null,ExtraVoicePacks.all).first().id)
        assertEquals("pocket",ExtraVoicePacks.ordered(VoiceMode.FAST,"pocket",ExtraVoicePacks.all).first().id)
        assertTrue(ExtraVoicePacks.available(Locale.FRENCH).isEmpty())
    }
}
