package com.zalmoxis.app

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/**
 * User-triggered speech output using an installed voice that reports no network requirement.
 *
 * Android devices may have no eligible voice installed. This class never falls back to a
 * network-required voice.
 */
class OfflineSpeechOutput(
    context: Context,
    private val onState: (ready: Boolean, message: String) -> Unit,
) : AutoCloseable {
    private var engine: TextToSpeech? = null
    private var offlineVoice: Voice? = null
    private var closed = false

    init {
        engine = TextToSpeech(context.applicationContext) { result ->
            val tts = engine ?: return@TextToSpeech
            if (closed) return@TextToSpeech
            if (result != TextToSpeech.SUCCESS) {
                onState(false, "Android text-to-speech could not initialize.")
                return@TextToSpeech
            }

            val defaultLocale = Locale.getDefault()
            val candidates = tts.voices.orEmpty().filter { voice ->
                !voice.isNetworkConnectionRequired &&
                    tts.isLanguageAvailable(voice.locale) >= TextToSpeech.LANG_AVAILABLE
            }
            val selected = candidates.sortedWith(
                compareByDescending<Voice> { it.locale.language == defaultLocale.language }
                    .thenByDescending { it.locale == defaultLocale }
            ).firstOrNull()

            if (selected == null) {
                onState(false, "No installed offline speech voice is available on this device.")
                return@TextToSpeech
            }

            val voiceResult = tts.setVoice(selected)
            if (voiceResult != TextToSpeech.SUCCESS) {
                onState(false, "The installed offline speech voice could not be selected.")
                return@TextToSpeech
            }

            offlineVoice = selected
            onState(true, "Offline speech ready (${selected.locale.toLanguageTag()}).")
        }
    }

    fun speak(text: String) {
        val tts = engine ?: return
        if (closed || offlineVoice == null) return
        val spokenText = text.trim()
        if (spokenText.isEmpty()) return
        if (spokenText.length > MAX_INPUT_LENGTH) {
            onState(true, "Response is too long for one speech request; shorten it and try again.")
            return
        }

        val result = tts.speak(
            spokenText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "zalmoxis-response-${System.nanoTime()}",
        )
        if (result != TextToSpeech.SUCCESS) {
            onState(true, "Android could not start offline speech output.")
        }
    }

    fun stop() {
        engine?.stop()
    }

    override fun close() {
        if (closed) return
        closed = true
        engine?.stop()
        engine?.shutdown()
        engine = null
        offlineVoice = null
    }

    private companion object {
        const val MAX_INPUT_LENGTH = 4_000
    }
}
