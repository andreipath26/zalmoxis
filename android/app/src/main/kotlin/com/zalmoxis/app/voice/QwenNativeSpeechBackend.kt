package com.zalmoxis.app.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.qwen.tts.studio.engine.QwenEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fully local Qwen3-TTS speech backend using the existing MIT-licensed Android
 * qwen3-tts.cpp JNI runtime. This does not invoke Android TextToSpeech.
 *
 * The currently reused native API synthesizes a complete PCM buffer per utterance,
 * not callback-streamed PCM. The controller should submit short sentence chunks.
 * Cancellation stops AudioTrack immediately, but an in-flight native call may
 * continue until the native inference function returns. Do not claim sub-200 ms
 * interruption or genuine sample-streaming until native cancellation is implemented.
 */
internal class QwenNativeSpeechBackend : AutoCloseable {
    private var engine: QwenEngine? = null
    @Volatile private var playback: AudioTrack? = null
    @Volatile private var closed = false
    var loadedModel: String? = null
        private set

    suspend fun load(modelDirectory: File, modelName: String? = null) = withContext(Dispatchers.IO) {
        check(!closed) { "Voice engine is closed" }
        check(modelDirectory.isDirectory) { "Qwen speech model is not installed." }
        check(modelDirectory.listFiles().orEmpty().any { it.extension == "gguf" }) {
            "Missing Qwen GGUF model files."
        }
        val newEngine = QwenEngine()
        try {
            newEngine.setBackendPreference(QwenEngine.BACKEND_CPU)
            newEngine.setCpuThreads(4)
            check(newEngine.loadModels(modelDirectory.absolutePath, modelName)) {
                newEngine.getLastError() ?: "Native Qwen3-TTS model failed to initialize."
            }
            engine?.close()
            engine = newEngine
            loadedModel = modelName ?: modelDirectory.name
        } catch (error: Throwable) {
            newEngine.close()
            throw error
        }
    }

    /**
     * Accepts a segment of locally generated text and emits fully generated audio.
     * The native model uses 24kHz PCM, but the true rate comes from the JNI result.
     */
    suspend fun speakSegment(text: String, speaker: String? = null) {
        if (text.isBlank()) return
        val native = checkNotNull(engine) { "Load a Qwen voice model before speaking." }
        val result = withContext(Dispatchers.IO) {
            currentCoroutineContext().ensureActive()
            native.synthesize(
                text = text.take(500),
                params = QwenEngine.NativeParams(speaker = speaker, maxAudioTokens = 512),
            )
        }
        currentCoroutineContext().ensureActive()
        require(result.success) { result.errorMsg ?: "Qwen speech generation failed." }
        val samples = result.audio ?: error("Qwen returned no audio samples.")
        require(samples.isNotEmpty() && result.sampleRate in 8000..96000) {
            "Qwen returned invalid audio format."
        }
        play(samples, result.sampleRate)
    }

    private suspend fun play(samples: FloatArray, sampleRate: Int) = withContext(Dispatchers.IO) {
        val minimum = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT,
        )
        require(minimum > 0) { "Android does not support this PCM playback format." }
        val player = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minimum * 2, 16384))
            .build()
        try {
            playback = player
            player.play()
            var offset = 0
            while (offset < samples.size) {
                currentCoroutineContext().ensureActive()
                val written = player.write(samples, offset, minOf(samples.size - offset, 2048),
                    AudioTrack.WRITE_BLOCKING)
                check(written > 0) { "AudioTrack failed to write speech." }
                offset += written
            }
            // Allow the final samples to drain, but remain promptly cancellable.
            val ms = ((samples.size.toLong() * 1000L) / sampleRate).coerceAtMost(120_000)
            val end = System.nanoTime() + ms * 1_000_000L
            while (player.playbackHeadPosition.toLong() < samples.size && System.nanoTime() < end) {
                currentCoroutineContext().ensureActive()
                kotlinx.coroutines.delay(15)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } finally {
            playback = null
            try { player.pause(); player.flush(); player.release() } catch (_: Exception) { }
        }
    }

    fun interrupt() {
        val player = playback ?: return
        try { player.pause(); player.flush() } catch (_: Exception) { }
    }

    override fun close() {
        closed = true
        interrupt()
        // Do not free the JNI model from the UI thread while synthesize() may still run.
        // The session owner must first cancel/join its active synth job.
        engine?.close()
        engine = null
        loadedModel = null
    }
}
