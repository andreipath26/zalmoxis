package com.zalmoxis.app.voice

/**
 * Storage policy for fully offline speech engines. No silent model downloads and no
 * assumption that a Qwen TTS artifact can be shared with the conversational LLM.
 * A compact voice is preferred on first installation; Qwen is an explicit opt-in.
 */
internal object SpeechStoragePolicy {
    const val MIB = 1024L * 1024L
    const val GIB = 1024L * MIB
    const val RESERVE_BYTES = 768L * MIB
    const val FIRST_RUN_DOWNLOAD_LIMIT = 0L
    const val COMPACT_ENGINE_BUDGET = 350L * MIB
    const val PREMIUM_ENGINE_BUDGET = 1200L * MIB

    enum class Profile { COMPACT, PREMIUM }

    data class Decision(val allowed: Boolean, val reason: String)

    fun mayInstall(
        profile: Profile,
        compressedBytes: Long,
        installedBytes: Long,
        freeBytes: Long,
        explicitlyApproved: Boolean,
    ): Decision {
        if (!explicitlyApproved) return Decision(false, "Speech model installation requires an explicit user action.")
        if (compressedBytes <= 0 || installedBytes <= 0) return Decision(false, "Model size metadata is missing.")
        val budget = when (profile) {
            Profile.COMPACT -> COMPACT_ENGINE_BUDGET
            Profile.PREMIUM -> PREMIUM_ENGINE_BUDGET
        }
        if (installedBytes > budget) return Decision(false, "Model exceeds the selected speech storage budget.")
        // Installation may temporarily hold both a staging copy and a final copy.
        // Reserve operating space and do not proceed if it cannot be guaranteed.
        val temporaryBytes = try { Math.addExact(installedBytes, compressedBytes) }
            catch (_: ArithmeticException) { return Decision(false, "Model size overflow.") }
        val required = try { Math.addExact(temporaryBytes, RESERVE_BYTES) }
            catch (_: ArithmeticException) { return Decision(false, "Model size overflow.") }
        if (freeBytes < required) return Decision(false, "Not enough free storage for safe installation and rollback.")
        return Decision(true, "Storage preflight passed.")
    }
}
