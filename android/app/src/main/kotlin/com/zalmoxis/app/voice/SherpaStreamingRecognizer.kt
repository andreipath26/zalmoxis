package com.zalmoxis.app.voice

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.io.File

/**
 * Native, fully offline streaming speech recognition. No Android SpeechRecognizer
 * and no cloud fallback. All functions are called on the same audio worker.
 */
internal class SherpaStreamingRecognizer : AutoCloseable {
    data class ModelFiles(
        val encoder: File,
        val decoder: File,
        val joiner: File,
        val tokens: File,
        val modelType: String = "zipformer2",
        val language: String,
    )

    data class Update(val partial: String, val final: String? = null)
    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null

    fun load(files: ModelFiles) {
        check(listOf(files.encoder, files.decoder, files.joiner, files.tokens).all { it.isFile }) {
            "A complete native offline ASR model pack is required."
        }
        close()
        val config = OnlineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
            modelConfig = OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = files.encoder.absolutePath,
                    decoder = files.decoder.absolutePath,
                    joiner = files.joiner.absolutePath,
                ),
                tokens = files.tokens.absolutePath,
                modelType = files.modelType,
                numThreads = 2,
                provider = "cpu",
            ),
            enableEndpoint = true,
        )
        val instance = OnlineRecognizer(config = config)
        try {
            recognizer = instance
            stream = instance.createStream()
        } catch (error: Throwable) {
            instance.release()
            recognizer = null
            throw error
        }
    }

    fun acceptPcm16(samples: ShortArray): Update {
        val instance = checkNotNull(recognizer) { "Offline ASR model not loaded." }
        val current = checkNotNull(stream)
        val waveform = FloatArray(samples.size) { i -> samples[i] / 32768.0f }
        current.acceptWaveform(waveform, 16000)
        while (instance.isReady(current)) instance.decode(current)
        val result = instance.getResult(current).text.trim()
        if (instance.isEndpoint(current)) {
            instance.reset(current)
            return Update(partial = "", final = result.takeIf { it.isNotBlank() })
        }
        return Update(partial = result)
    }

    fun discardPartial() {
        val instance = recognizer ?: return
        stream?.let { instance.reset(it) }
    }

    override fun close() {
        stream?.release()
        stream = null
        recognizer?.release()
        recognizer = null
    }
}
