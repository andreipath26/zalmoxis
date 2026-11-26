package com.zalmoxis.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

/** User-initiated speech with installed, non-network voices only. */
class OfflineSpeechOutput(
    context: Context,
    private val onState: (ready: Boolean, message: String) -> Unit,
    private val onUtteranceFinished: (() -> Unit)? = null,
) : AutoCloseable {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var offlineVoice: Voice? = null
    private var closed = false
    private var lastUtterance: String? = null

    init {
        val created = TextToSpeech(context.applicationContext) { result ->
            // Some TTS implementations deliver initialization synchronously.
            // Post until the constructor-assigned engine is available.
            mainHandler.post {
                if (!closed) finishInitialization(result)
            }
        }
        engine = created
    }

    private fun finishInitialization(result: Int) {
        val tts = engine ?: return
        if (result != TextToSpeech.SUCCESS) {
            onState(false, "Android speech synthesis failed to initialize. Check the installed text-to-speech engine in Settings.")
            return
        }
        val locale = Locale.getDefault()
        val installed = try {
            tts.voices.orEmpty().filter { voice ->
                !voice.isNetworkConnectionRequired &&
                    tts.isLanguageAvailable(voice.locale) >= TextToSpeech.LANG_AVAILABLE
            }
        } catch (error: Exception) {
            onState(false, "Cannot inspect installed offline voices: ${error.message}")
            return
        }
        val selected = installed.sortedWith(
            compareByDescending<Voice> { it.locale == locale }
                .thenByDescending { it.locale.language == locale.language }
        ).firstOrNull()
        if (selected == null) {
            onState(false, "No offline voice is installed. Download an offline voice in Android Text-to-speech settings.")
            return
        }
        if (tts.setVoice(selected) != TextToSpeech.SUCCESS) {
            onState(false, "Android could not activate its installed offline voice.")
            return
        }
        offlineVoice = selected
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                mainHandler.post { if (!closed && utteranceId == lastUtterance) { lastUtterance = null; onState(true, "Speech finished."); onUtteranceFinished?.invoke() } }
            }
            @Deprecated("Deprecated by Android")
            override fun onError(utteranceId: String?) {
                mainHandler.post { if (!closed && utteranceId == lastUtterance) { lastUtterance = null; onState(false, "Android speech playback failed. Check device audio output."); onUtteranceFinished?.invoke() } }
            }
        })
        onState(true, "Offline voice ready (${selected.locale.toLanguageTag()}).")
    }

    fun speak(text: String) {
        val tts = engine
        if (closed || tts == null || offlineVoice == null) {
            onState(false, "Offline speech is not ready. Configure an installed offline voice.")
            return
        }
        val spokenText = text.trim()
        if (spokenText.isEmpty()) return
        val maxLength = TextToSpeech.getMaxSpeechInputLength()
        val chunks = spokenText.chunked(maxLength.coerceAtLeast(1))
        val batch = System.nanoTime()
        lastUtterance = "zalmoxis-$batch-${chunks.lastIndex}"
        chunks.forEachIndexed { index, chunk ->
            val result = tts.speak(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "zalmoxis-$batch-$index",
            )
            if (result != TextToSpeech.SUCCESS) {
                onState(true, "Android could not queue offline speech output.")
                return
            }
        }
    }

    fun stop() {
        lastUtterance = null
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
}
