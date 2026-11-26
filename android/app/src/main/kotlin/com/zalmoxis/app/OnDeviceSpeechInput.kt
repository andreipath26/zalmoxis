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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest

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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var googleJob: Job? = null

    @OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
    fun start() {
        googleJob?.cancel()
        recognizer?.destroy()
        recognizer = null
        finished = false
        requestedLanguage = languageTag ?: Locale.getDefault().toLanguageTag()
        googleJob = scope.launch {
            for (mode in listOf(SpeechRecognizerOptions.Mode.MODE_ADVANCED, SpeechRecognizerOptions.Mode.MODE_BASIC)) {
                val result = try {
                    val client = SpeechRecognition.getClient(speechRecognizerOptions {
                        locale = Locale.forLanguageTag(requestedLanguage)
                        preferredMode = mode
                    })
                    client.use {
                        if (client.checkStatus() != FeatureStatus.AVAILABLE) null
                        else {
                            val source = AudioSource.fromMic()
                            try {
                                val response = client.startRecognition(speechRecognizerRequest { audioSource = source })
                                    .firstOrNull { it is SpeechRecognizerResponse.FinalTextResponse }
                                (response as? SpeechRecognizerResponse.FinalTextResponse)?.text
                            } finally {
                                source.close()
                                withContext(NonCancellable) { client.stopRecognition() }
                            }
                        }
                    }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { null }
                if (!result.isNullOrBlank()) {
                    finished = true
                    onResult(result.trim())
                    return@launch
                }
            }
            if (!finished) {
                try { startPlatform() } catch (e: Exception) {
                    finished = true
                    onError(e.message ?: "Offline recognition unavailable.")
                }
            }
        }
    }

    private fun startPlatform() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Speech recognition must start on the main thread."
        }
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "On-device dictation requires Android 12 or newer. Text input remains available."
        }
        check(SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            "No on-device speech recognizer is installed. Install an offline recognition language in Android speech settings."
        }
        recognizer?.destroy()
        recognizer = null
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
        finished = true
        googleJob?.cancel()
        googleJob = null
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
