package com.zalmoxis.app.voice

/**
 * Emit conversational sentences as soon as a complete clause is available.
 * Keeps words intact and bounds native synthesis input. Model tokens must be
 * joined before passing through this buffer. The on-device TTS engine is separate.
 */
internal class SpeechSegments(private val maxCharacters: Int = 220) {
    private val buffer = StringBuilder()

    fun append(piece: String): List<String> {
        buffer.append(piece)
        val ready = mutableListOf<String>()
        while (buffer.isNotEmpty()) {
            val end = (0 until buffer.length).firstOrNull { i ->
                val c = buffer[i]
                (c == '.' || c == '!' || c == '?' || c == '\n') &&
                    (i == buffer.lastIndex || buffer[i + 1].isWhitespace())
            }
            val limit = when {
                end != null && end < maxCharacters -> end + 1
                buffer.length > maxCharacters ->
                    buffer.substring(0, maxCharacters).lastIndexOf(' ').takeIf { it > 0 } ?: maxCharacters
                else -> null
            } ?: break
            val text = buffer.substring(0, limit).trim()
            buffer.delete(0, limit)
            if (text.isNotBlank()) ready += text
        }
        return ready
    }

    fun finish(): String = buffer.toString().trim().also { buffer.clear() }

    fun clear() = buffer.clear()
}
