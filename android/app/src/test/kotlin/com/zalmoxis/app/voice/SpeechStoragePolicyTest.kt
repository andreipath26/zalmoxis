package com.zalmoxis.app.voice

import org.junit.Assert.*
import org.junit.Test

class SpeechStoragePolicyTest {
    private val m = SpeechStoragePolicy.MIB
    @Test fun neverDownloadsWithoutConsent() {
        assertEquals(0L, SpeechStoragePolicy.FIRST_RUN_DOWNLOAD_LIMIT)
        val decision = SpeechStoragePolicy.mayInstall(
            SpeechStoragePolicy.Profile.COMPACT, 100 * m, 100 * m, 4_000 * m, false)
        assertFalse(decision.allowed)
    }
    @Test fun rejectsLargeDefaultSpeechModel() {
        val decision = SpeechStoragePolicy.mayInstall(
            SpeechStoragePolicy.Profile.COMPACT, 800 * m, 800 * m, 5_000 * m, true)
        assertFalse(decision.allowed)
    }
    @Test fun refusesLowDiskHeadroom() {
        val decision = SpeechStoragePolicy.mayInstall(
            SpeechStoragePolicy.Profile.PREMIUM, 800 * m, 800 * m, 1_800 * m, true)
        assertFalse(decision.allowed)
    }
    @Test fun permitsCompactVerifiedSizeWithEnoughSpace() {
        val decision = SpeechStoragePolicy.mayInstall(
            SpeechStoragePolicy.Profile.COMPACT, 150 * m, 150 * m, 4_000 * m, true)
        assertTrue(decision.allowed)
    }
    @Test fun blocksCurrentMultiGigabyteQwenBundle() {
        val decision = SpeechStoragePolicy.mayInstall(
            SpeechStoragePolicy.Profile.PREMIUM, 2440 * m, 2440 * m, 10_000 * m, true)
        assertFalse(decision.allowed)
    }
}
