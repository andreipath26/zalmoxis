package com.zalmoxis.app

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.ParcelFileDescriptor
import java.util.concurrent.atomic.AtomicBoolean

/** Real-time 16 kHz PCM pipe with hardware AEC. Never records without an active voice turn. */
internal class DuplexMicrophone private constructor(
    private val recorder: AudioRecord,
    private val echo: AcousticEchoCanceler,
    private val noise: NoiseSuppressor?,
    val readEnd: ParcelFileDescriptor,
    private val writer: ParcelFileDescriptor,
    private val onActivity: (Boolean) -> Unit,
) : AutoCloseable {
    private val running = AtomicBoolean(true)
    private val worker = Thread({
        try {
            ParcelFileDescriptor.AutoCloseOutputStream(writer).use { stream ->
                val bytes = ByteArray(640)
                val gate = AdaptiveVoiceGate()
                var previous = false
                while (running.get()) {
                    val count = recorder.read(bytes, 0, bytes.size)
                    if (count <= 0) break
                    val current = gate.accept(bytes, count)
                    if(current != previous) { previous = current; onActivity(current) }
                    stream.write(bytes, 0, count)
                }
            }
        } catch (_: Exception) { /* Closing the pipe unblocks a pending write. */ }
    }, "zalmoxis-voice-capture")

    override fun close() {
        if (!running.getAndSet(false)) return
        runCatching { recorder.stop() }
        runCatching { readEnd.close() }
        runCatching { writer.close() }
        runCatching { worker.join(250) }
        runCatching { noise?.release() }
        runCatching { echo.release() }
        runCatching { recorder.release() }
    }

    companion object {
        @SuppressLint("MissingPermission")
        fun open(context: Context, onActivity: (Boolean) -> Unit = {}): DuplexMicrophone {
            check(androidx.core.content.ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED)
            check(AcousticEchoCanceler.isAvailable()) { "Hardware echo cancellation unavailable." }
            val minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(minimum > 0)
            val recorder = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum, 3200))
            var echo: AcousticEchoCanceler? = null
            var noise: NoiseSuppressor? = null
            var pipe: Array<ParcelFileDescriptor>? = null
            try {
                check(recorder.state == AudioRecord.STATE_INITIALIZED)
                val aec = checkNotNull(AcousticEchoCanceler.create(recorder.audioSessionId))
                echo = aec
                aec.enabled = true
                check(aec.enabled)
                if (NoiseSuppressor.isAvailable()) {
                    noise = NoiseSuppressor.create(recorder.audioSessionId)
                    noise?.enabled = true
                }
                val descriptors = ParcelFileDescriptor.createPipe()
                pipe = descriptors
                recorder.startRecording()
                check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                return DuplexMicrophone(recorder, aec, noise, descriptors[0], descriptors[1], onActivity).also { it.worker.start() }
            } catch (error: Throwable) {
                pipe?.forEach { runCatching { it.close() } }
                runCatching { noise?.release() }
                runCatching { echo?.release() }
                runCatching { recorder.release() }
                throw error
            }
        }
    }
}
