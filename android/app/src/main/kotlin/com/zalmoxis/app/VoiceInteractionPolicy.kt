package com.zalmoxis.app

import java.util.Locale

internal object VoiceInteractionPolicy {
    private val backchannels = setOf("uh", "um", "mm", "mhm", "uh huh", "yeah", "yes", "okay", "ok", "oui", "d'accord", "sí", "ja", "da")
    private fun normalize(text: String) = text.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N} ]"), " ").trim().replace(Regex("\\s+"), " ")
    fun isInterruption(partial: String, assistant: String): Boolean {
        val p = normalize(partial)
        if (p.length < 3 || p in backchannels) return false
        if (p in setOf("stop", "wait", "arrête", "attends", "para", "halt")) return true
        val echo = normalize(assistant).takeLast(500)
        return p.length >= 5 && !echo.contains(p)
    }
    fun pauseMillis(language: String): Long = if (language.startsWith("ja") || language.startsWith("zh")) 1000L else 850L
    fun speechRate(text: String): Float = if (text.contains(Regex("\\d{3,}"))) 0.92f else 1f
    fun cleanForSpeech(text: String): String = text.replace(Regex("https?://\\S+"), " link ")
        .replace(Regex("[*#_\u0060]"), "").trim()
    fun prompt(text: String, language: String): String =
        "Voice conversation. Answer naturally, briefly, in the language spoken by the user. Avoid markdown and long lists. Respect pauses and interruptions. User message:\n" + text
}
