package com.zalmoxis.app.voice

import org.junit.Assert.*
import org.junit.Test

class SpeechSegmentsTest {
    @Test fun waitsForSentenceBoundary() {
        val stream = SpeechSegments()
        assertTrue(stream.append("Hello, you").isEmpty())
        assertEquals(listOf("Hello, you!"), stream.append("! How are you"))
        assertEquals("How are you", stream.finish())
    }

    @Test fun capsLargeUnpunctuatedResponses() {
        val stream = SpeechSegments(30)
        val chunks = stream.append("one two three four five six seven eight nine ten ")
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.all { it.length <= 30 })
    }
}
