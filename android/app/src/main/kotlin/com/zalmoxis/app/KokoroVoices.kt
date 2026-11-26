package com.zalmoxis.app

import java.util.Locale

internal object KokoroVoices {
    private val catalog = mapOf(
        "en-US" to "af_heart af_alloy af_aoede af_bella af_jessica af_kore af_nicole af_nova af_river af_sarah af_sky am_adam am_echo am_eric am_fenrir am_liam am_michael am_onyx am_puck am_santa",
        "en-GB" to "bf_alice bf_emma bf_isabella bf_lily bm_daniel bm_fable bm_george bm_lewis",
        "es" to "ef_dora em_alex em_santa",
        "fr" to "ff_siwis",
        "it" to "if_sara im_nicola",
        "pt" to "pf_dora pm_alex pm_santa",
        "hi" to "hf_alpha hf_beta hm_omega hm_psi",
        "zh" to "zf_xiaobei zf_xiaoni zf_xiaoxiao zf_xiaoyi zm_yunjian zm_yunxi zm_yunxia zm_yunyang",
        "ja" to "jf_alpha jf_gongitsune jf_nezumi jf_tebukuro jm_kumo"
    )
    fun forLocale(locale: Locale): List<String> {
        val key = if (locale.language == "en") {
            if (locale.country == "GB") "en-GB" else "en-US"
        } else locale.language
        return catalog[key]?.split(" ") ?: emptyList()
    }
    fun resolve(locale: Locale, selected: String?): String {
        val voices = forLocale(locale)
        return selected?.takeIf { it in voices } ?: voices.first()
    }
}
