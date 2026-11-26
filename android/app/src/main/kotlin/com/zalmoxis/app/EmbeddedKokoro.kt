package com.zalmoxis.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.google.mediapipe.tasks.audio.texttospeech.TextToSpeech
import com.google.mediapipe.tasks.audio.texttospeech.TextToSpeechOptions
import com.google.mediapipe.tasks.audio.texttospeech.SynthesisOptions
import com.google.mediapipe.tasks.audio.texttospeech.TtsStream
import kotlinx.coroutines.channels.ReceiveChannel
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class EmbeddedKokoro(private val context: Context, locale: Locale, selectedVoice: String?, private val pack: VoicePack? = null) : AutoCloseable {
    private val voice = if(pack==null) KokoroVoices.resolve(locale,selectedVoice) else selectedVoice?.takeIf { it in pack.voices } ?: pack.voices.first()
    private val cache = KokoroPack.cache(context).apply { mkdirs() }
    private fun create(delegate: Delegate) = TextToSpeech.create(TextToSpeechOptions(
        baseOptions = BaseOptions.builder().setModelAssetPath((pack?.let { ExtraVoicePacks.model(context,it) } ?: KokoroPack.model(context)).absolutePath)
            .setDelegate(delegate).build(),
        defaultVoiceId = voice,
        cacheDirectory = cache.absolutePath,
    ))
    private var tts = if (pack != null || VoiceDevicePolicy.preferCpu(context)) create(Delegate.CPU) else try { create(Delegate.GPU) } catch (_: Exception) { create(Delegate.CPU) }
    private val epoch = AtomicLong()
    private val lock = Mutex()
    private val cleanup = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var closed = false
    @Volatile private var track: AudioTrack? = null
    @Volatile private var activeStream: TtsStream? = null
    suspend fun speak(text: String) = kotlinx.coroutines.coroutineScope {
        val input = kotlinx.coroutines.channels.Channel<String>(1)
        input.trySend(text)
        input.close()
        speakIncrementally(input)
    }
    suspend fun speakIncrementally(input: ReceiveChannel<String>) = withContext(Dispatchers.IO) {
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
                val stream = tts.openStream(SynthesisOptions(voiceId = voice))
                activeStream = stream
                try {
                    coroutineScope {
                        val writer = launch(Dispatchers.IO) {
                            try { for (delta in input) { ensureActive(); if (delta.isNotEmpty()) stream.pushText(delta) }; stream.finishInput() }
                            catch (e: Throwable) { stream.cancel(); throw e }
                        }
                        try {
                            while (true) {
                                ensureActive()
                                val chunk = stream.readNext() ?: break
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
                            writer.join()
                        } finally { if (!writer.isCompleted) { stream.cancel(); writer.cancel() } }
                    }
                } finally { activeStream = null; stream.close() }
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
        runCatching { activeStream?.cancel() }
        runCatching { tts.cancel() }
    }
    override fun close() {
        if (closed) return
        closed = true
        stop()
        cleanup.launch { lock.withLock { tts.close() }; cleanup.cancel() }
    }
}
