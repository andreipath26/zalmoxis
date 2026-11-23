package com.zalmoxis.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** One-shot Android on-device speech input. It never falls back to a network recognizer. */
class OnDeviceSpeechInput(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
) : RecognitionListener, AutoCloseable {
    private var recognizer: SpeechRecognizer? = null

    fun start() {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "On-device speech recognition requires Android 12 or newer."
        }
        check(SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            "This device does not provide an on-device speech recognizer."
        }

        close()
        val newRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer = newRecognizer
        newRecognizer.setRecognitionListener(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        newRecognizer.startListening(intent)
    }

    fun cancel() {
        recognizer?.cancel()
        close()
    }

    override fun close() {
        recognizer?.destroy()
        recognizer = null
    }

    override fun onResults(results: Bundle?) {
        val text = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
        close()
        if (text.isNullOrEmpty()) {
            onError("No speech was recognized.")
        } else {
            onResult(text)
        }
    }

    override fun onError(error: Int) {
        close()
        onError("On-device speech recognition failed (code $error).")
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
