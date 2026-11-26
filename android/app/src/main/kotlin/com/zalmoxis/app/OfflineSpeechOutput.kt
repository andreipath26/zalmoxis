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
    private val locale: Locale = Locale.getDefault(),
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
    private var completion: CompletableDeferred<Unit>? = null
    private var lastUtterance: String? = null

    init {
        scope.launch {
            val ready = try {
                val client = SpeechSynthesis.getClient(speechSynthesizerOptions(locale))
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
                initKokoroOrPlatform()
            }
        }
    }

    /** Prefer an explicitly advertised installed Kokoro voice over ordinary system TTS.
     * The model stays in its optional Android engine; no duplicate weights are bundled.
     */
    private suspend fun initKokoroOrPlatform() {
        val probe = TextToSpeech(context.applicationContext) { }
        val packages = try {
            probe.engines.map { it.name }.distinct()
        } catch (_: Exception) { emptyList() }
        finally { probe.shutdown() }
        for (packageName in packages) {
            if (closed) return
            val candidate = initializeEngine(packageName) ?: continue
            var retained = false
            try {
                val voice = candidate.voices.orEmpty()
                    .filter { KokoroVoicePolicy.isCandidate(it.name, it.locale.language, locale.language,
                        it.isNetworkConnectionRequired, it.features.orEmpty()) }
                    .sortedWith(compareByDescending<Voice> { it.locale == locale }.thenByDescending { it.quality })
                    .firstOrNull()
                if (voice != null && candidate.setVoice(voice) == TextToSpeech.SUCCESS) {
                    engine = candidate
                    offlineVoice = voice
                    retained = true
                    attachListener(candidate)
                    onState(true, "Kokoro offline neural voice ready (${voice.locale.toLanguageTag()}).")
                    return
                }
            } catch (_: Exception) {
                // An optional external engine must never prevent ordinary offline fallback.
            } finally { if (!retained) candidate.shutdown() }
        }
        if (!closed) initPlatform()
    }

    private suspend fun initializeEngine(packageName: String): TextToSpeech? {
        val result = CompletableDeferred<Int>()
        val candidate = TextToSpeech(context.applicationContext, { result.complete(it) }, packageName)
        var keep = false
        try {
            keep = withTimeoutOrNull(2000) { result.await() } == TextToSpeech.SUCCESS && !closed
            return if (keep) candidate else null
        } finally { if (!keep) candidate.shutdown() }
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
        val installed = try {
            tts.voices.orEmpty().filter { voice ->
                !voice.isNetworkConnectionRequired && voice.locale.language == locale.language &&
                    tts.isLanguageAvailable(voice.locale) >= TextToSpeech.LANG_AVAILABLE
            }
        } catch (error: Exception) {
            onState(false, "Cannot inspect installed offline voices: ${error.message}")
            return
        }
        val selected = installed.sortedWith(
            compareByDescending<Voice> { it.locale == locale }
                .thenByDescending { it.quality }
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
        attachListener(tts)
        onState(true, "Offline voice ready (${selected.locale.toLanguageTag()}).")
    }

    private fun attachListener(tts: TextToSpeech) {
        tts.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                mainHandler.post { if (!closed && utteranceId == lastUtterance) { completion?.complete(Unit); onState(true, "Speech finished.") } }
            }
            @Deprecated("Deprecated by Android")
            override fun onError(utteranceId: String?) {
                mainHandler.post { if (!closed && utteranceId == lastUtterance) { completion?.completeExceptionally(IllegalStateException("Speech playback failed.")); onState(true, "Android speech playback failed. Check device audio output.") } }
            }
        })
    }

    suspend fun speakAndAwait(text: String) {
        if (text.isBlank()) return
        speak(text)
        val pending = completion ?: return
        withTimeout(180_000) { pending.await() }
    }

    fun speak(text: String) {
        if (closed || text.isBlank()) return
        stop()
        completion = CompletableDeferred()
        val pending = completion
        val client = neural
        val voice = neuralVoice
        if (client != null && voice != null && !closed) {
            playback = scope.launch {
                try {
                    client.synthesize(SpeechSynthesizerRequest(
                        input = SpeechSynthesizerRequest.Input.Text(text, voice),
                        audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
                    )).collect { event ->
                        if (event is SpeechSynthesizerResponse.Error) error("Neural synthesis failed.")
                    }
                    pending?.complete(Unit)
                    if (!closed) onState(true, "Speech finished.")
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { pending?.completeExceptionally(e); if (!closed) onState(true, e.message ?: "Speech failed.") }
            }
            return
        }
        speakPlatform(text)
    }

    private fun speakPlatform(text: String) {
        val tts = engine
        if (closed || tts == null || offlineVoice == null) {
            completion?.completeExceptionally(IllegalStateException("Offline speech is not ready."))
            onState(false, "Offline speech is not ready. Configure an installed offline voice.")
            return
        }
        val spokenText = text.trim()
        if (spokenText.isEmpty()) return
        val maxLength = TextToSpeech.getMaxSpeechInputLength()
        val chunks = spokenText.chunked(maxLength.coerceAtLeast(1))
        val prefix = "zalmoxis-${System.nanoTime()}"
        lastUtterance = "$prefix-${chunks.lastIndex}"
        chunks.forEachIndexed { index, chunk ->
            val result = tts.speak(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "$prefix-$index",
            )
            if (result != TextToSpeech.SUCCESS) {
                completion?.completeExceptionally(IllegalStateException("Could not queue speech."))
                onState(true, "Android could not queue offline speech output.")
                return
            }
        }
    }

    fun stop() {
        completion?.complete(Unit)
        lastUtterance = null
        playback?.cancel()
        playback = null
        neural?.stop()
        engine?.stop()
    }

    override fun close() {
        if (closed) return
        closed = true
        stop()
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
