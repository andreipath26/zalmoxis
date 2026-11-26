package com.zalmoxis.app.voice

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineNeMoCtcModelConfig
import android.content.Context
import java.io.File

/**
 * Native, fully offline streaming speech recognition. No Android SpeechRecognizer
 * and no cloud fallback. All functions are called on the same audio worker.
 */
internal class SherpaStreamingRecognizer : AutoCloseable {
    data class ModelFiles(
        val model: String = "sherpa-onnx-nemo-streaming-fast-conformer-ctc-en-80ms-int8/model.int8.onnx",
        val tokens: String = "sherpa-onnx-nemo-streaming-fast-conformer-ctc-en-80ms-int8/tokens.txt",
        val language: String = "en",
    )

    data class Update(val partial: String, val final: String? = null)
    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null

    fun load(context: Context, files: ModelFiles = ModelFiles()) {
        require(files.language == "en") { "Only the bundled English ASR pack is validated." }
        close()
        val config = OnlineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
            modelConfig = OnlineModelConfig(
                neMoCtc = OnlineNeMoCtcModelConfig(model = files.model),
                tokens = files.tokens,
                numThreads = 2,
                provider = "cpu",
            ),
            enableEndpoint = true,
        )
        val instance = OnlineRecognizer(assetManager = context.assets, config = config)
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

    fun finishCurrent(): String {
        val instance = checkNotNull(recognizer)
        val current = checkNotNull(stream)
        current.inputFinished()
        while (instance.isReady(current)) instance.decode(current)
        val text = instance.getResult(current).text.trim()
        // The stream is reset after each utterance; no conversation recording is saved.
        instance.reset(current)
        return text
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
