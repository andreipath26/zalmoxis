package com.zalmoxis.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/** Explicit on-device recognition only; no network-recognition fallback. */
class OnDeviceSpeechInput(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val languageTag: String? = null,
) : RecognitionListener, AutoCloseable {
    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var finished = false
    private var requestedLanguage = Locale.getDefault().toLanguageTag()

    fun start() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Speech recognition must start on the main thread."
        }
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "On-device dictation requires Android 12 or newer. Text input remains available."
        }
        check(SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            "No on-device speech recognizer is installed. Install an offline recognition language in Android speech settings."
        }
        close()
        finished = false
        val service = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer = service
        service.setRecognitionListener(this)
        val locale = languageTag?.takeIf { it.isNotBlank() } ?: Locale.getDefault().toLanguageTag()
        requestedLanguage = locale
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        service.startListening(intent)
    }

    fun cancel() {
        finished = true
        recognizer?.cancel()
        close()
    }

    override fun close() {
        recognizer?.destroy()
        recognizer = null
    }

    override fun onResults(results: Bundle?) {
        if (finished) return
        finished = true
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()?.trim()
        close()
        if (text.isNullOrEmpty()) onError("No speech detected. Try again in a quieter place.")
        else onResult(text)
    }

    override fun onError(error: Int) {
        if (finished) return
        finished = true
        close()
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Microphone error. Check microphone access and try again."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone access is missing. Enable it in Android app settings."
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "No speech recognized. Speak clearly and try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Stop other dictation and retry."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "The on-device speech service reported a network error; no cloud fallback was started."
            SpeechRecognizer.ERROR_CLIENT -> "The Android speech service stopped unexpectedly. Retry dictation."
            SpeechRecognizer.ERROR_SERVER -> "The installed speech engine failed. Check offline language support."
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ->
                "Offline dictation does not support $requestedLanguage with your installed speech engine (Android code 12). Choose another dictation language in Zalmoxis, or install an engine that supports this language."
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                "Offline speech files for $requestedLanguage are unavailable (Android code 13). Download this language in Android speech recognition settings, then retry."
            else -> "Offline dictation error $error for $requestedLanguage. Check the installed Android speech service and retry."
        }
        onError(message)
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
