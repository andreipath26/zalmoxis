package com.zalmoxis.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoicePhraseBufferTest {
    @Test fun incrementalSentences() {
        val b = VoicePhraseBuffer()
        assertTrue(b.append("Hello").isEmpty())
        assertEquals(listOf("Hello there."), b.append(" there. How"))
        assertEquals("How are you?", b.append(" are you?").let { b.finish() })
        assertEquals(null, b.finish())
    }
    @Test fun longTextPreservesWords() {
        val b = VoicePhraseBuffer(32)
        val text = (1..20).joinToString(" ") { "word$it" }
        val chunks = b.append(text) + listOfNotNull(b.finish())
        assertEquals(text, chunks.joinToString(" "))
    }
}
