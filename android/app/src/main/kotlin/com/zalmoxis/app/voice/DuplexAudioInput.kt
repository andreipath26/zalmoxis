package com.zalmoxis.app.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/**
 * Exactly one full-duplex microphone stream, open only while the user is in an opted-in
 * conversation. Frames remain in memory and are not recorded to disk or sent to a server.
 * Audio processing availability varies by Android vendor; UI must surface unsupported AEC.
 */
internal class DuplexAudioInput(private val context: Context) : AutoCloseable {
    data class AudioHealth(
        val echoCancellation: Boolean,
        val noiseSuppression: Boolean,
        val sampleRate: Int,
    )

    private var reader: Job? = null
    private var recording: AudioRecord? = null
    private var echo: AcousticEchoCanceler? = null
    private var suppression: NoiseSuppressor? = null
    @Volatile private var stopped = true

    fun start(
        scope: CoroutineScope,
        onHealth: (AudioHealth) -> Unit,
        onPcm16: (ShortArray) -> Unit,
        onError: (String) -> Unit,
    ) {
        check(reader == null) { "Microphone already running" }
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) { "Microphone permission not granted" }
        stopped = false
        reader = scope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            try {
                val min = AudioRecord.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                require(min > 0) { "Unsupported PCM microphone format" }
                val rec = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(min * 2, 4096))
                recording = rec
                check(rec.state == AudioRecord.STATE_INITIALIZED) { "Could not initialize microphone" }
                if (AcousticEchoCanceler.isAvailable()) {
                    echo = AcousticEchoCanceler.create(rec.audioSessionId)
                    echo?.enabled = true
                }
                if (NoiseSuppressor.isAvailable()) {
                    suppression = NoiseSuppressor.create(rec.audioSessionId)
                    suppression?.enabled = true
                }
                onHealth(AudioHealth(echo?.enabled == true, suppression?.enabled == true, sampleRate))
                rec.startRecording()
                val frame = ShortArray(320) // 20 ms at 16 kHz
                while (isActive && !stopped) {
                    val count = rec.read(frame, 0, frame.size, AudioRecord.READ_BLOCKING)
                    if (count < 0) error("AudioRecord.read failed: $count")
                    if (count > 0) onPcm16(frame.copyOf(count))
                }
            } catch (error: Exception) {
                if (!stopped) onError(error.message ?: "Microphone capture failed")
            } finally {
                releaseNative()
            }
        }
    }

    private fun releaseNative() {
        try { recording?.stop() } catch (_: Exception) {}
        echo?.release()
        suppression?.release()
        recording?.release()
        recording = null
        echo = null
        suppression = null
    }

    suspend fun stopAndJoin() {
        stopped = true
        val job = reader
        reader = null
        job?.cancelAndJoin()
    }

    override fun close() {
        stopped = true
        reader?.cancel()
        reader = null
        // AudioRecord.release on capture job completion prevents use-after-free on read().
    }
}
