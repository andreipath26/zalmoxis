package com.zalmoxis.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Deterministic no-network regression tests for the first native Research channel. */
class NativeResearchPolicyTest {
    @Test fun chatResearchCommandsDoNotRequirePastedLinks() {
        assertEquals("offline AI agents for Android",
            researchTopicFromChat("Research offline AI agents for Android"))
        assertEquals("Android on-device models",
            researchTopicFromChat("Search the web for Android on-device models"))
        assertEquals("open-source OCR",
            researchTopicFromChat("Research open-source OCR and save findings to my Knowledge"))
        assertTrue(hasResearchSaveDirective(
            "Research open-source OCR and save findings to my Knowledge"))
        assertTrue(!hasResearchSaveDirective("Research open-source OCR"))
        assertEquals(null, researchTopicFromChat("Explain what research is"))
    }

    @Test fun onlyApprovedSourcesAreAdvertised() {
        val sources = NativeResearch.supportedSources()
        assertEquals(2, sources.size)
        assertTrue(sources.all { it.contains("GitHub") })
    }

    @Test fun canonicalRepositoryHostIsShownForConsent() {
        assertEquals("github.com",
            NativeResearch.hostForConsent("https://github.com/owner/repository"))
        assertEquals("raw.githubusercontent.com",
            NativeResearch.hostForConsent("https://raw.githubusercontent.com/owner/repo/main/README.md"))
    }

    @Test fun rejectNonHttpsAndCredentialUrls() {
        rejects("http://github.com/owner/repo")
        rejects("https://username:password@github.com/owner/repo")
        rejects("file:///etc/passwd")
        rejects("javascript:alert(1)")
    }

    @Test fun rejectRedirectHelpersAndEncodedTargetsAtValidationBoundary() {
        rejects("https://github.com:8443/owner/repo")
        rejects("https://github.com/owner/repo?secret=value")
        rejects("https://github.com/owner/repo#fragment")
        rejects("https://github.com/owner/../repo")
        rejects("https://github.com/owner//repo")
    }

    private fun rejects(url: String) {
        try {
            NativeResearch.hostForConsent(url)
            throw AssertionError("Expected URL rejection: $url")
        } catch (_: IllegalArgumentException) {
            // Invalid URL correctly rejected before any network call.
        }
    }
}
