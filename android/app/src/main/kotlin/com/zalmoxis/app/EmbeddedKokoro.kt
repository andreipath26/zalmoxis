package com.zalmoxis.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.*
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class EmbeddedKokoro(context: Context, locale: Locale) : AutoCloseable {
    private val root=KokoroPack.root(context)
    private val tts=OfflineTts(config=OfflineTtsConfig(model=OfflineTtsModelConfig(
        kokoro=OfflineTtsKokoroModelConfig(model=File(root,"model.int8.onnx").path,
            voices=File(root,"voices.bin").path, tokens=File(root,"tokens.txt").path,
            dataDir=File(root,"espeak-ng-data").path, lang=if(locale.country=="GB") "en-gb" else "en-us"),
        numThreads=2),maxNumSentences=1))
    private val epoch=AtomicLong()
    private val lock=Mutex()
    private val cleanup=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    @Volatile private var closed=false
    @Volatile private var track: AudioTrack?=null
    suspend fun speak(text:String)=withContext(Dispatchers.IO) {
        lock.withLock {
            check(!closed)
            val turn=epoch.incrementAndGet()
            val rate=tts.sampleRate()
            val audio=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_FLOAT),rate/5*4))
                .setTransferMode(AudioTrack.MODE_STREAM).build()
            track=audio
            try {
                audio.play()
                var written=0L
                var invalid=false
                tts.generateWithCallback(text, sid=0, speed=1f) { samples ->
                    if(samples.any { !it.isFinite() } || samples.isNotEmpty() && samples.count { kotlin.math.abs(it)>=0.999f } > samples.size/2) { invalid=true; 0 }
                    else {
                        var offset=0
                        while(offset<samples.size && epoch.get()==turn && isActive) {
                            val n=audio.write(samples,offset,samples.size-offset,AudioTrack.WRITE_NON_BLOCKING)
                            if(n<0) break
                            if(n==0) Thread.sleep(10) else { offset+=n; written+=n }
                        }
                        if(epoch.get()==turn && isActive && offset==samples.size) 1 else 0
                    }
                }
                check(!invalid) { "Kokoro produced invalid audio." }
                withTimeoutOrNull(120000) {
                    while(epoch.get()==turn && (audio.playbackHeadPosition.toLong() and 0xffffffffL)<written) delay(20)
                }
            } finally { track=null; runCatching { audio.stop() }; audio.release() }
        }
    }
    fun stop() { epoch.incrementAndGet(); runCatching { track?.pause(); track?.flush() } }
    override fun close() {
        if(closed)return
        closed=true; stop()
        cleanup.launch { lock.withLock { tts.release() }; cleanup.cancel() }
    }
}
