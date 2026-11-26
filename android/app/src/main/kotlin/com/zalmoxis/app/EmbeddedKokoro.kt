package com.zalmoxis.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.google.mediapipe.tasks.audio.texttospeech.TextToSpeech
import com.google.mediapipe.tasks.audio.texttospeech.TextToSpeechOptions
import com.google.mediapipe.tasks.audio.texttospeech.SynthesisOptions
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class EmbeddedKokoro(context: Context, locale: Locale, selectedVoice: String?) : AutoCloseable {
    private val voice = KokoroVoices.resolve(locale, selectedVoice)
    private val cache = KokoroPack.cache(context).apply { mkdirs() }
    private fun create(delegate: Delegate) = TextToSpeech.create(TextToSpeechOptions(
        baseOptions = BaseOptions.builder().setModelAssetPath(KokoroPack.model(context).absolutePath)
            .setDelegate(delegate).build(),
        defaultVoiceId = voice,
        cacheDirectory = cache.absolutePath,
    ))
    private var tts = try { create(Delegate.GPU) } catch (_: Exception) { create(Delegate.CPU) }
    private val epoch = AtomicLong()
    private val lock = Mutex()
    private val cleanup = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var closed = false
    @Volatile private var track: AudioTrack? = null
    suspend fun speak(text: String) = withContext(Dispatchers.IO) {
        lock.withLock {
            check(!closed)
            val turn = epoch.incrementAndGet()
            val rate = 24000
            val audio = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT), rate / 5 * 4))
                .setTransferMode(AudioTrack.MODE_STREAM).build()
            track = audio
            try {
                audio.play()
                var written = 0L
                tts.synthesizeStream(text, SynthesisOptions(voiceId = voice)).collect { chunk ->
                    if (epoch.get() != turn) throw CancellationException("Speech interrupted")
                    check(chunk.sampleRateHz == rate && chunk.samples.all { it.isFinite() }) { "Invalid speech samples." }
                    var offset = 0
                    while (offset < chunk.samples.size && epoch.get() == turn) {
                        ensureActive()
                        val n = audio.write(chunk.samples, offset, chunk.samples.size - offset, AudioTrack.WRITE_NON_BLOCKING)
                        check(n >= 0) { "Audio output failed." }
                        if (n == 0) delay(10) else { offset += n; written += n }
                    }
                }
                if (epoch.get() == turn) {
                    check(written > 0) { "No speech audio generated." }
                    withTimeout(120000) {
                        while (epoch.get() == turn && (audio.playbackHeadPosition.toLong() and 0xffffffffL) < written) delay(20)
                    }
                }
            } finally {
                track = null
                runCatching { audio.stop() }
                audio.release()
            }
        }
    }
    fun stop() {
        epoch.incrementAndGet()
        runCatching { track?.pause(); track?.flush() }
        runCatching { tts.cancel() }
    }
    override fun close() {
        if (closed) return
        closed = true
        stop()
        cleanup.launch { lock.withLock { tts.close() }; cleanup.cancel() }
    }
}
