package com.zalmoxis.app

import java.util.Locale

internal object KokoroVoicePolicy {
    fun isCandidate(name: String, voiceLanguage: String, requestedLanguage: String,
        networkRequired: Boolean, features: Set<String>): Boolean =
        !networkRequired && voiceLanguage.equals(requestedLanguage, ignoreCase = true) &&
            name.lowercase(Locale.ROOT).contains("kokoro") &&
            "notInstalled" !in features
}
