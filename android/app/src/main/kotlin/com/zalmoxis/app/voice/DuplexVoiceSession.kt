package com.zalmoxis.app.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.zalmoxis.app.OnDeviceInferenceRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

/**
 * Visible, explicitly started, fully local, two-way voice session.
 *
 * ASR: native sherpa-onnx streaming NeMo. VAD: native Silero.
 * TTS: native Qwen3-TTS GGUF or compact on-device Kitten (never Android TTS).
 * AEC: Android's system audio preprocessor, with automatic barge-in only when
 * this hardware capability actually reports available.
 *
 * The 2026 native qwen3-tts.cpp API returns completed PCM for a sentence.
 * Consequently first-audio and barge-in timing are not yet verified as real-time.
 * Barge-in cancels stale playback/results; native inference may continue briefly.
 */
internal class DuplexVoiceSession(
    private val context: Context,
    private val localModel: () -> OnDeviceInferenceRuntime?,
) : AutoCloseable {
    enum class Voice { COMPACT, QWEN }
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val gate = VoiceTurnGate()
    private val history = ArrayDeque<Pair<String, String>>()

    var phase by mutableStateOf(VoiceTurnGate.Phase.OFF)
        private set
    var status by mutableStateOf("Load an AI model to start a private conversation.")
        private set
    var transcript by mutableStateOf("")
        private set
    var reply by mutableStateOf("")
        private set
    var numberOfTurns by mutableStateOf(0)
        private set
    var echoCancellationAvailable by mutableStateOf(false)
        private set
    var voiceMode by mutableStateOf(Voice.COMPACT)
        private set

    @Volatile private var started = false
    @Volatile private var capturePhase = VoiceTurnGate.Phase.OFF
    @Volatile private var echoHealthy = false
    private var preparation: Job? = null
    private var responseJob: Job? = null
    private var soundJob: Job? = null
    private var microphone: DuplexAudioInput? = null
    private var recognizer: SherpaStreamingRecognizer? = null
    private var detector: SileroNativeVad? = null
    private var compactVoice: CompactNativeSpeechBackend? = null
    private var qwenVoice: QwenNativeSpeechBackend? = null

    fun start(mode: Voice) {
        if (started) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            fail("Allow microphone access to start a spoken conversation.")
            return
        }
        if (localModel() == null) {
            fail("Load a local AI model under Models before starting a conversation.")
            return
        }
        started = true
        voiceMode = mode
        phase = VoiceTurnGate.Phase.LISTENING
        capturePhase = VoiceTurnGate.Phase.OFF
        status = "Initializing native offline speech models…"
        preparation = mainScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val asr = SherpaStreamingRecognizer()
                    asr.load(context)
                    recognizer = asr
                    detector = SileroNativeVad(context)
                    if (mode == Voice.COMPACT) {
                        compactVoice = CompactNativeSpeechBackend(context)
                    } else {
                        val voice = OfflineVoiceModelStore.installed(context)
                            .firstOrNull { it.manifest.engine == "qwen3-tts" }
                            ?: error("No verified Qwen3-TTS pack is installed. Select Compact or import an approved complete Qwen GGUF pack.")
                        qwenVoice = QwenNativeSpeechBackend().also {
                            it.load(voice.directory)
                        }
                    }
                }
                if (!started) return@launch
                gate.start()
                phase = gate.phase
                capturePhase = phase
                status = "Listening offline · speak naturally."
                microphone = DuplexAudioInput(context).also { input ->
                    input.start(mainScope,
                        onHealth = { health ->
                            echoHealthy = health.echoCancellation
                            mainScope.launch {
                                echoCancellationAvailable = health.echoCancellation
                                if (!health.echoCancellation) {
                                    status = "Listening offline. Automatic interruption needs echo cancellation; manual Interrupt is available."
                                }
                            }
                        },
                        onPcm16 = { frame -> onMicrophoneFrame(frame) },
                        onError = { message -> mainScope.launch { fail(message) } },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (started) fail(error.message ?: "Native speech initialization failed.")
            }
        }
    }

    private fun onMicrophoneFrame(samples: ShortArray) {
        if (!started) return
        val vad = detector ?: return
        val speech = vad.acceptPcm16(samples)
        val now = capturePhase
        if ((now == VoiceTurnGate.Phase.SPEAKING || now == VoiceTurnGate.Phase.THINKING) &&
            speech.began && echoHealthy) {
            mainScope.launch { interruptForSpeech() }
            return
        }
        if (now != VoiceTurnGate.Phase.LISTENING) return
        val asr = recognizer ?: return
        try {
            val update = asr.acceptPcm16(samples)
            if (update.partial.isNotBlank()) {
                mainScope.launch {
                    if (started && capturePhase == VoiceTurnGate.Phase.LISTENING) transcript = update.partial
                }
            }
            if (update.final != null) {
                mainScope.launch { finalUtterance(update.final) }
            } else if (speech.ended) {
                val final = asr.finishCurrent()
                if (final.isNotBlank()) mainScope.launch { finalUtterance(final) }
            }
        } catch (error: Exception) {
            mainScope.launch { fail(error.message ?: "Offline speech recognition failed.") }
        }
    }

    private fun finalUtterance(text: String) {
        if (!started || capturePhase != VoiceTurnGate.Phase.LISTENING) return
        val token = gate.epoch
        if (!gate.endpoint(token) || !gate.thinking(token)) return
        capturePhase = gate.phase
        phase = gate.phase
        val utterance = text.trim().take(4000)
        transcript = utterance
        reply = ""
        status = "Generating a private local reply…"
        val runtime = localModel() ?: run {
            fail("The local AI model was unloaded.")
            return
        }
        val last = history.toList().takeLast(4)
        val prompt = buildString {
            append("You are Zalmoxis in an offline spoken conversation. Speak conversationally. ")
            append("Answer the newest user question briefly, clearly, and naturally. ")
            append("Never claim you used tools or external services.\n")
            last.forEach { (question, answer) ->
                append("User: ").append(question).append("\nZalmoxis: ").append(answer).append("\n")
            }
            append("User: ").append(utterance).append("\nZalmoxis: ")
        }
        responseJob?.cancel()
        responseJob = mainScope.launch {
            val spokenSegments = Channel<String>(capacity = 3)
            val synthesis = launch(Dispatchers.IO) {
                for (segment in spokenSegments) {
                    if (!started || !gate.isCurrent(token)) break
                    withContext(Dispatchers.Main.immediate) {
                        if (gate.phase == VoiceTurnGate.Phase.THINKING && gate.isCurrent(token)) {
                            gate.speaking(token)
                            phase = gate.phase
                            capturePhase = phase
                            status = "Speaking offline · interrupt at any time."
                        }
                    }
                    if (!gate.isCurrent(token)) break
                    if (voiceMode == Voice.QWEN) {
                        qwenVoice?.speakSegment(segment)
                    } else {
                        compactVoice?.speakSegment(segment)
                    }
                }
            }
            soundJob = synthesis
            val segmenter = SpeechSegments()
            val output = StringBuilder()
            try {
                runtime.stream(prompt).flowOn(Dispatchers.Default).collect { part ->
                    if (!gate.isCurrent(token)) return@collect
                    val trimmed = part.take((9000 - output.length).coerceAtLeast(0))
                    output.append(trimmed)
                    reply = output.toString()
                    for (sentence in segmenter.append(trimmed)) spokenSegments.send(sentence)
                }
                val tail = segmenter.finish()
                if (tail.isNotBlank() && gate.isCurrent(token)) spokenSegments.send(tail)
                spokenSegments.close()
                synthesis.join()
                if (!started || !gate.isCurrent(token)) return@launch
                val answer = output.toString().trim()
                history.addLast(utterance to answer.take(3000))
                while (history.size > 4) history.removeFirst()
                numberOfTurns++
                if (gate.phase == VoiceTurnGate.Phase.SPEAKING) gate.completeAudio(token)
                else if (gate.phase == VoiceTurnGate.Phase.THINKING) {
                    // Empty or silent output: start the next listening epoch.
                    gate.stop()
                    gate.start()
                }
                phase = gate.phase
                capturePhase = phase
                status = "Listening offline · speak naturally."
                transcript = ""
                recognizer?.discardPartial()
                detector?.reset()
            } catch (cancelled: CancellationException) {
                spokenSegments.close()
                synthesis.cancel()
                throw cancelled
            } catch (error: Throwable) {
                spokenSegments.close()
                synthesis.cancel()
                if (started && gate.isCurrent(token)) fail(error.message ?: "Local voice response failed.")
            } finally {
                soundJob = null
            }
        }
    }

    /** Human speaking over assistant playback or manual Interrupt: invalidate every old turn. */
    private fun interruptForSpeech() {
        if (!started) return
        val current = gate.epoch
        if (!gate.hearingSpeech(current)) return
        capturePhase = gate.phase
        phase = gate.phase
        responseJob?.cancel()
        soundJob?.cancel()
        qwenVoice?.interrupt()
        compactVoice?.interrupt()
        val token = gate.resumeAfterInterruption()
        check(gate.isCurrent(token))
        capturePhase = gate.phase
        phase = gate.phase
        status = "Interrupted · listening to your new question."
        recognizer?.discardPartial()
        transcript = ""
    }

    fun interrupt() = interruptForSpeech()

    fun stop() {
        if (!started && phase == VoiceTurnGate.Phase.OFF) return
        started = false
        gate.stop()
        capturePhase = gate.phase
        phase = gate.phase
        status = "Conversation stopped. Microphone off."
        preparation?.cancel()
        responseJob?.cancel()
        soundJob?.cancel()
        qwenVoice?.interrupt()
        compactVoice?.interrupt()
        val activeMic = microphone
        microphone = null
        // Release native libraries only after the active microphone reader exits.
        mainScope.launch {
            activeMic?.stopAndJoin()
            soundJob?.cancelAndJoin()
            withContext(Dispatchers.IO) {
                recognizer?.close(); recognizer = null
                detector?.close(); detector = null
                compactVoice?.close(); compactVoice = null
                qwenVoice?.close(); qwenVoice = null
            }
        }
    }

    private fun fail(message: String) {
        stop()
        gate.fail()
        capturePhase = gate.phase
        phase = gate.phase
        status = message
    }

    override fun close() {
        stop()
        // Do not cancel the cleanup coroutine: native release must run after mic shutdown.
    }
}
