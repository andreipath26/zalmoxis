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
    private val preferKokoro: Boolean = false,
    private val kokoroVoiceId: String? = null,
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
    private var kokoro: EmbeddedKokoro? = null
    private var completion: CompletableDeferred<Unit>? = null
    private var lastUtterance: String? = null
    private var platformReady = CompletableDeferred<Boolean>()

    init {
        scope.launch {
            val ready = if (preferKokoro && prepareKokoro()) true else try {
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
            if (!closed && ready) onState(true, if (kokoro != null) "Kokoro local voice ready." else "Google on-device neural voice ready.")
            else if (!closed) {
                neural?.close()
                neural = null
                if (prepareKokoro()) onState(true, "Kokoro local voice ready.") else initPlatform()
            }
        }
    }

    private suspend fun prepareKokoro(): Boolean {
        if (!KokoroPack.installed(context) || KokoroVoices.forLocale(locale).isEmpty() || VoiceDevicePolicy.lowMemory(context)) return false
        return try {
            val local = withContext(Dispatchers.IO) { EmbeddedKokoro(context, locale, kokoroVoiceId) }
            if (closed) { local.close(); false } else { kokoro = local; true }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { false }
        catch (_: LinkageError) { false }
    }

    private fun initPlatform() {
        platformReady = CompletableDeferred()
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
            platformReady.complete(false)
            onState(false, "Android speech synthesis failed to initialize. Check the installed text-to-speech engine in Settings.")
            return
        }
        val installed = try {
            tts.voices.orEmpty().filter { voice ->
                !voice.isNetworkConnectionRequired && voice.locale.language == locale.language &&
                    tts.isLanguageAvailable(voice.locale) >= TextToSpeech.LANG_AVAILABLE
            }
        } catch (error: Exception) {
            platformReady.complete(false)
            onState(false, "Cannot inspect installed offline voices: ${error.message}")
            return
        }
        val selected = installed.sortedWith(
            compareByDescending<Voice> { it.locale == locale }
                .thenByDescending { it.quality }
        ).firstOrNull()
        if (selected == null) {
            platformReady.complete(false)
            onState(false, "No offline voice is installed. Download an offline voice in Android Text-to-speech settings.")
            return
        }
        if (tts.setVoice(selected) != TextToSpeech.SUCCESS) {
            platformReady.complete(false)
            onState(false, "Android could not activate its installed offline voice.")
            return
        }
        offlineVoice = selected
        attachListener(tts)
        platformReady.complete(true)
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
        playback = scope.launch {
            try {
                val client = neural
                val voice = neuralVoice
                if (client != null && voice != null) {
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
                        return@launch
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) {
                        client.stop()
                        client.close()
                        neural = null
                        neuralVoice = null
                        onState(true, "Google voice failed; switching to local fallback.")
                    }
                }
                ensureActive()
                if (kokoro == null) prepareKokoro()
                val local = kokoro
                if (local != null) {
                    try {
                        local.speak(text)
                        pending?.complete(Unit)
                        return@launch
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) {
                        local.close()
                        kokoro = null
                        onState(true, "Kokoro failed; switching to Android offline voice.")
                    }
                }
                ensureActive()
                if (offlineVoice == null) {
                    engine?.shutdown()
                    engine = null
                    initPlatform()
                    check(withTimeout(15000) { platformReady.await() }) { "No compatible offline voice is installed." }
                }
                ensureActive()
                speakPlatform(text)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                pending?.completeExceptionally(e)
                if (!closed) onState(false, e.message ?: "Speech failed.")
            }
        }
    }

    private fun speakPlatform(text: String) {
        val tts = engine
        if (closed || tts == null || offlineVoice == null) {
            completion?.completeExceptionally(IllegalStateException("Offline speech is not ready."))
            onState(false, "Offline speech is not ready. Configure an installed offline voice.")
            return
        }
        tts.setSpeechRate(VoiceInteractionPolicy.speechRate(text))
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
        kokoro?.stop()
        engine?.stop()
    }

    override fun close() {
        if (closed) return
        closed = true
        stop()
        scope.cancel()
        kokoro?.close()
        kokoro = null
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
