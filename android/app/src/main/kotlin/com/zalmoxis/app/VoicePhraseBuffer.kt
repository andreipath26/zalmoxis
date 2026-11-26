package com.zalmoxis.app

/** Groups incremental model text into pronounceable phrases without waiting for a full turn. */
internal class VoicePhraseBuffer(private val maxChars: Int = 180) {
    private val pending = StringBuilder()
    init { require(maxChars >= 32) }
    fun append(delta: String): List<String> {
        pending.append(delta)
        val phrases = mutableListOf<String>()
        while (pending.isNotEmpty()) {
            val sentence = pending.indices.firstOrNull { i ->
                pending[i] in ".!?" && i + 1 < pending.length && pending[i + 1].isWhitespace() &&
                    !(pending[i] == '.' && i > 0 && pending[i - 1].isDigit())
            }
            val limit = if (pending.length >= maxChars) {
                (maxChars downTo maxChars / 2).firstOrNull { pending[it - 1].isWhitespace() } ?: maxChars
            } else null
            val cut = listOfNotNull(sentence?.plus(1), limit).minOrNull() ?: break
            val phrase = pending.substring(0, cut).trim()
            pending.delete(0, cut)
            if (phrase.isNotEmpty()) phrases += phrase
        }
        return phrases
    }
    fun finish(): String? = pending.toString().trim().takeIf { it.isNotEmpty() }.also { pending.clear() }
}
