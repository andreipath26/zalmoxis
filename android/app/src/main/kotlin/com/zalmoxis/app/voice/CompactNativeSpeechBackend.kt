package com.zalmoxis.app.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKittenModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Small-footprint offline voice, provided by the sherpa-onnx native engine.
 * It replaces Android TextToSpeech; Qwen3-TTS remains the optional expressive engine.
 * The pinned compact English pack ships as APK assets, avoiding a first-run download.
 */
internal class CompactNativeSpeechBackend(context: Context) : AutoCloseable {
    private val tts = OfflineTts(
        assetManager = context.assets,
        config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                kitten = OfflineTtsKittenModelConfig(
                    model = "kitten-mini-en-v0_8/model.onnx",
                    voices = "kitten-mini-en-v0_8/voices.bin",
                    tokens = "kitten-mini-en-v0_8/tokens.txt",
                    dataDir = "kitten-mini-en-v0_8/espeak-ng-data",
                ),
                numThreads = 3,
                provider = "cpu",
            ),
            maxNumSentences = 1,
        ),
    )
    @Volatile private var track: AudioTrack? = null

    suspend fun speakSegment(text: String, voiceId: Int = 0) {
        val generated = withContext(Dispatchers.IO) {
            currentCoroutineContext().ensureActive()
            tts.generate(text.take(320), sid = voiceId)
        }
        currentCoroutineContext().ensureActive()
        val minimum = AudioTrack.getMinBufferSize(generated.sampleRate,
            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        require(minimum > 0) { "PCM float output is not available" }
        val audio = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(generated.sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT).build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minimum * 2, 16384))
            .build()
        try {
            track = audio
            audio.play()
            var offset = 0
            while (offset < generated.samples.size) {
                currentCoroutineContext().ensureActive()
                val amount = audio.write(generated.samples, offset,
                    minOf(2048, generated.samples.size - offset), AudioTrack.WRITE_BLOCKING)
                check(amount > 0) { "Voice audio playback failed" }
                offset += amount
            }
            while (audio.playbackHeadPosition < generated.samples.size.toLong()) {
                currentCoroutineContext().ensureActive()
                delay(12)
            }
        } finally {
            track = null
            try { audio.pause(); audio.flush(); audio.release() } catch (_: Exception) { }
        }
    }

    fun interrupt() {
        try { track?.pause(); track?.flush() } catch (_: Exception) { }
    }

    override fun close() {
        interrupt()
        tts.release()
    }
}
