package com.zalmoxis.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import kotlin.math.sqrt

/** A user-controlled, on-device voice session. Never opens the network or device tools. */
internal class VoiceConversationController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val model: () -> OnDeviceInferenceRuntime?,
    private val language: () -> String?,
) : AutoCloseable {
    enum class Phase { IDLE, PREPARING, LISTENING, THINKING, SPEAKING, INTERRUPTED, ERROR }

    var phase by mutableStateOf(Phase.IDLE)
        private set
    var detail by mutableStateOf("Select a model and start a voice conversation.")
        private set
    var lastUser by mutableStateOf("")
        private set
    var lastReply by mutableStateOf("")
        private set
    var turnCount by mutableStateOf(0)
        private set
    var interruptionAvailable by mutableStateOf(false)
        private set

    private var enabled = false
    private var recognizer: OnDeviceSpeechInput? = null
    private var speech: OfflineSpeechOutput? = null
    private var inferenceJob: Job? = null
    private var monitorJob: Job? = null
    private val history = ArrayDeque<Pair<String, String>>()
    private var generation = 0

    fun start() {
        if (enabled) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            fail("Microphone permission is required for an active conversation.")
            return
        }
        if (model() == null) {
            fail("Load a compatible local model in Models before starting voice conversation.")
            return
        }
        enabled = true
        generation++
        phase = Phase.PREPARING
        detail = "Preparing the installed offline voice…"
        speech = OfflineSpeechOutput(
            context,
            onState = { ready, message ->
                if (!enabled) return@OfflineSpeechOutput
                detail = message
                if (!ready) {
                    fail(message)
                } else if (phase == Phase.PREPARING) {
                    listen()
                }
            },
            onUtteranceFinished = {
                if (enabled && phase == Phase.SPEAKING) {
                    stopMonitor()
                    listen()
                }
            },
        )
    }

    private fun listen() {
        if (!enabled) return
        stopMonitor()
        recognizer?.cancel()
        recognizer = null
        phase = Phase.LISTENING
        detail = "Listening on this device. Speak naturally."
        try {
            val token = generation
            val input = OnDeviceSpeechInput(
                context = context,
                languageTag = language(),
                onResult = { transcript ->
                    if (enabled && token == generation) {
                        recognizer = null
                        onTranscript(transcript)
                    }
                },
                onError = { error ->
                    if (enabled && token == generation) {
                        recognizer = null
                        // Some Android recognition services time out during silence. Restart
                        // only for these nonfatal conditions, never on unsupported language.
                        if (error.startsWith("No speech")) {
                            phase = Phase.PREPARING
                            detail = "No speech heard; listening again…"
                            scope.launch {
                                delay(400)
                                if (enabled && generation == token) listen()
                            }
                        } else {
                            fail(error)
                        }
                    }
                },
            )
            recognizer = input
            input.start()
        } catch (error: Exception) {
            fail(error.message ?: "On-device recognition could not start.")
        }
    }

    private fun onTranscript(text: String) {
        val runtime = model()
        if (runtime == null) {
            fail("The selected model was unloaded. Reload it before continuing.")
            return
        }
        lastUser = text.take(4000)
        lastReply = ""
        phase = Phase.THINKING
        detail = "Generating a local reply…"
        val token = generation
        val recent = history.toList().takeLast(4).joinToString("\n") {
            "User: ${it.first}\nZalmoxis: ${it.second}"
        }
        val prompt = buildString {
            append("You are Zalmoxis in an offline spoken conversation. Reply naturally, ")
            append("briefly and conversationally. Never claim tools were used. ")
            append("Answer the user's newest statement with its context.\n")
            if (recent.isNotBlank()) append(recent).append("\n")
            append("User: ").append(text.take(4000)).append("\nZalmoxis: ")
        }
        inferenceJob?.cancel()
        inferenceJob = scope.launch {
            try {
                val answer = StringBuilder()
                runtime.stream(prompt).flowOn(Dispatchers.Default).collect { tokenText ->
                    if (!enabled || generation != token) return@collect
                    if (answer.length < 9000) {
                        answer.append(tokenText.take(9000 - answer.length))
                        lastReply = answer.toString()
                    }
                }
                if (!enabled || generation != token) return@launch
                val spoken = answer.toString().trim()
                if (spoken.isEmpty()) {
                    detail = "The local model returned no speech text."
                    listen()
                } else {
                    history.addLast(lastUser to spoken.take(3000))
                    while (history.size > 4) history.removeFirst()
                    turnCount++
                    phase = Phase.SPEAKING
                    detail = "Speaking offline · interrupt me at any time."
                    speech?.speak(spoken)
                    startMonitor(token)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (enabled && generation == token) fail(error.message ?: "Local inference failed.")
            }
        }
    }

    /**
     * During playback, capture a microphone stream separately from speech recognition.
     * Android acoustic echo cancellation (AEC) is enabled when available; energy detection
     * is deliberately conservative because speakers can be picked up as microphone input.
     * Only capture on a user-started foreground session. No audio is persisted.
     */
    private fun startMonitor(token: Int) {
        stopMonitor()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return
        monitorJob = scope.launch(Dispatchers.IO) {
            var record: AudioRecord? = null
            var aec: AcousticEchoCanceler? = null
            try {
                val min = AudioRecord.getMinBufferSize(
                    16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
                )
                if (min <= 0) return@launch
                val capture = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    16000, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(min * 2, 3200),
                )
                record = capture
                if (capture.state != AudioRecord.STATE_INITIALIZED) return@launch
                if (AcousticEchoCanceler.isAvailable()) {
                    aec = AcousticEchoCanceler.create(capture.audioSessionId)
                    aec?.enabled = true
                }
                // Without AEC, speech playback would be mistaken for the user's voice.
                val echoControlled = aec?.enabled == true
                withContext(Dispatchers.Main) {
                    if (enabled && token == generation) interruptionAvailable = echoControlled
                }
                if (!echoControlled) return@launch
                capture.startRecording()
                val samples = ShortArray(320)
                var speechFrames = 0
                // Allow AEC to settle before interpreting input.
                delay(350)
                while (isActive && enabled && token == generation && phase == Phase.SPEAKING) {
                    val count = capture.read(samples, 0, samples.size)
                    if (count <= 0) break
                    var energy = 0.0
                    for (i in 0 until count) energy += samples[i].toDouble() * samples[i]
                    val rms = sqrt(energy / count)
                    speechFrames = if (rms > 3200.0) speechFrames + 1 else 0
                    if (speechFrames >= 9) {
                        // Do not run audio-service calls from the capture thread.
                        withContext(Dispatchers.Main) {
                            if (enabled && token == generation && phase == Phase.SPEAKING) {
                                phase = Phase.INTERRUPTED
                                detail = "Interrupted · listening to you now."
                                speech?.stop()
                                stopMonitor()
                                listen()
                            }
                        }
                        break
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { interruptionAvailable = false }
            } finally {
                try { record?.stop() } catch (_: Exception) {}
                try { aec?.release() } catch (_: Exception) {}
                try { record?.release() } catch (_: Exception) {}
            }
        }
    }

    private fun stopMonitor() {
        monitorJob?.cancel()
        monitorJob = null
        interruptionAvailable = false
    }

    fun stop() {
        enabled = false
        generation++
        inferenceJob?.cancel()
        inferenceJob = null
        recognizer?.cancel()
        recognizer = null
        stopMonitor()
        speech?.close()
        speech = null
        phase = Phase.IDLE
        detail = "Conversation stopped. Your microphone is off."
    }

    private fun fail(message: String) {
        stop()
        phase = Phase.ERROR
        detail = message
    }

    override fun close() = stop()
}
