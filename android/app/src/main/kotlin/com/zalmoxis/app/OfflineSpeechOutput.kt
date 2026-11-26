package com.zalmoxis.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import kotlinx.coroutines.*
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.texttospeech.*
import android.media.AudioAttributes

/** User-initiated speech with installed, non-network voices only. */
class OfflineSpeechOutput(
    private val context: Context,
    private val onState: (ready: Boolean, message: String) -> Unit,
) : AutoCloseable {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var offlineVoice: Voice? = null
    private var closed = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var neural: SpeechSynthesizer? = null
    private var neuralVoice: VoiceInfo? = null
    private var playback: Job? = null

    init {
        scope.launch {
            val ready = try {
                val client = SpeechSynthesis.getClient(speechSynthesizerOptions(Locale.getDefault()))
                neural = client
                val voices = client.getSupportedVoices()
                val voice = voices.firstOrNull { client.checkStatus(it) == FeatureStatus.AVAILABLE }
                    ?: voices.firstOrNull { client.checkStatus(it) == FeatureStatus.DOWNLOADABLE }
                if (voice != null && client.checkStatus(voice) == FeatureStatus.DOWNLOADABLE) {
                    onState(false, "Preparing Google on-device neural voice…")
                    client.download(voice).collect { }
                }
                if (voice != null && client.checkStatus(voice) == FeatureStatus.AVAILABLE) {
                    neuralVoice = voice
                    true
                } else false
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { false }
            if (!closed && ready) onState(true, "Google on-device neural voice ready.")
            else if (!closed) {
                neural?.close()
                neural = null
                initPlatform()
            }
        }
    }

    private fun initPlatform() {
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
                mainHandler.post { if (!closed) onState(true, "Speech finished.") }
            }
            @Deprecated("Deprecated by Android")
            override fun onError(utteranceId: String?) {
                mainHandler.post { if (!closed) onState(true, "Android speech playback failed. Check device audio output.") }
            }
        })
        onState(true, "Offline voice ready (${selected.locale.toLanguageTag()}).")
    }

    fun speak(text: String) {
        if (closed || text.isBlank()) return
        val client = neural
        val voice = neuralVoice
        if (client != null && voice != null && !closed) {
            stop()
            playback = scope.launch {
                try {
                    client.synthesize(SpeechSynthesizerRequest(
                        input = SpeechSynthesizerRequest.Input.Text(text, voice),
                        audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
                    )).collect { event ->
                        if (event is SpeechSynthesizerResponse.Error) error("Neural synthesis failed.")
                    }
                    if (!closed) onState(true, "Speech finished.")
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { if (!closed) onState(true, e.message ?: "Speech failed.") }
            }
            return
        }
        speakPlatform(text)
    }

    private fun speakPlatform(text: String) {
        val tts = engine
        if (closed || tts == null || offlineVoice == null) {
            onState(false, "Offline speech is not ready. Configure an installed offline voice.")
            return
        }
        val spokenText = text.trim()
        if (spokenText.isEmpty()) return
        val maxLength = TextToSpeech.getMaxSpeechInputLength()
        val chunks = spokenText.chunked(maxLength.coerceAtLeast(1))
        chunks.forEachIndexed { index, chunk ->
            val result = tts.speak(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "zalmoxis-${System.nanoTime()}-$index",
            )
            if (result != TextToSpeech.SUCCESS) {
                onState(true, "Android could not queue offline speech output.")
                return
            }
        }
    }

    fun stop() {
        playback?.cancel()
        playback = null
        neural?.stop()
        engine?.stop()
    }

    override fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        neural?.stop()
        neural?.close()
        neural = null
        neuralVoice = null
        engine?.stop()
        engine?.shutdown()
        engine = null
        offlineVoice = null
    }
}
