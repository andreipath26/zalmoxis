package com.zalmoxis.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val incomingShare = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingShare.value = AgentBridgeHandoff.extractIncoming(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingShare.value = AgentBridgeHandoff.extractIncoming(intent)
        window.statusBarColor = ZalmoxisDarkColors.background.toArgb()
        window.navigationBarColor = ZalmoxisDarkColors.background.toArgb()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            ZalmoxisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) { ZalmoxisHome(
                    incomingSharedText = incomingShare.value,
                    onSharedTextHandled = { incomingShare.value = null },
                ) }
            }
        }
    }
}

/** Explicit user research commands route to scoped research; ordinary chat stays local. */
private val RESEARCH_SAVE_DIRECTIVE = Regex(
    "\\s+and\\s+(?:save|add|store)\\s+(?:the\\s+)?(?:findings|results|sources|relevant\\s+information)" +
        "(?:\\s+(?:to|in|into)\\s+(?:my\\s+)?(?:knowledge|database))?[.!]?\\s*$",
    RegexOption.IGNORE_CASE,
)

internal fun researchTopicFromChat(message: String): String? {
    val match = Regex(
        "^(?:research|search(?: the web)? for|find (?:github )?repositories (?:for|about)|look up)\\s+(.{3,160})$",
        RegexOption.IGNORE_CASE,
    ).matchEntire(message.trim()) ?: return null
    return match.groupValues[1].replace(RESEARCH_SAVE_DIRECTIVE, "").trim()
        .takeIf { it.length in 3..160 }
}

internal fun hasResearchSaveDirective(message: String): Boolean =
    researchTopicFromChat(message) != null && RESEARCH_SAVE_DIRECTIVE.containsMatchIn(message)


private enum class HomeTab(val title: String) {
    CHAT("Chat"),
    VOICE("Voice"),
    KNOWLEDGE("Knowledge"),
    DOWNLOADS("Models"),
    WORKSPACE("Workspace"),
    RESEARCH("Research"),
    AGENT_BRIDGE("Agent Bridge"),
    SETTINGS("Settings"),
}

@Composable
private fun ZalmoxisHome(
    incomingSharedText: String?,
    onSharedTextHandled: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<OnDeviceInferenceRuntime?>(null) }
    var models by remember { mutableStateOf(emptyList<LocalModel>()) }
    var modelName by remember { mutableStateOf<String?>(null) }
    var modelSha256 by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Choose a compatible .litertlm file stored on this device.") }
    var prompt by rememberSaveable { mutableStateOf("") }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var speechInput by remember { mutableStateOf<OnDeviceSpeechInput?>(null) }
    var speechActive by remember { mutableStateOf(false) }
    var liveVoice by remember { mutableStateOf(false) }
    var livePhase by remember { mutableStateOf("Idle") }
    var liveTranscript by remember { mutableStateOf("") }
    var liveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var liveEpoch by remember { mutableStateOf(0L) }
    var liveInterrupted by remember { mutableStateOf(false) }
    var liveMuted by remember { mutableStateOf(false) }
    var liveCaptions by rememberSaveable { mutableStateOf(true) }
    var liveMetrics by remember { mutableStateOf("") }
    var liveToolsAllowed by rememberSaveable { mutableStateOf(false) }
    var liveTurnJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var liveBargeInput by remember { mutableStateOf<OnDeviceSpeechInput?>(null) }
    var liveBargeEnabled by rememberSaveable { mutableStateOf(true) }
    val speechPreferences = remember(context) {
        context.getSharedPreferences("zalmoxis_speech", Context.MODE_PRIVATE)
    }
    var preferKokoro by remember { mutableStateOf(speechPreferences.getBoolean("prefer_kokoro", false)) }
    var kokoroVoiceId by remember { mutableStateOf(speechPreferences.getString("kokoro_voice_id", null)) }
    var dictationLanguage by rememberSaveable {
        mutableStateOf(speechPreferences.getString("dictation_language", "system") ?: "system")
    }
    var dictationMenuExpanded by remember { mutableStateOf(false) }
    val dictationOptions = listOf(
        "system" to "Device language",
        "en-US" to "English (US)",
        "en-GB" to "English (UK)",
        "fr-FR" to "Français",
        "ro-RO" to "Română",
        "es-ES" to "Español",
        "de-DE" to "Deutsch",
        "it-IT" to "Italiano",
        "pt-BR" to "Português (Brasil)",
        "hi-IN" to "हिन्दी",
        "zh-CN" to "中文",
        "ja-JP" to "日本語",
    )
    val dictationLanguageLabel = dictationOptions.firstOrNull { it.first == dictationLanguage }?.second
        ?: dictationLanguage
    var speechOutput by remember { mutableStateOf<OfflineSpeechOutput?>(null) }
    var speechOutputReady by remember { mutableStateOf(false) }
    var speechOutputInitializing by remember { mutableStateOf(false) }
    var response by remember { mutableStateOf("") }
    var activePrompt by remember { mutableStateOf("") }
    var priorTurns by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    var pendingDeleteModel by remember { mutableStateOf<LocalModel?>(null) }
    var expandedModel by remember { mutableStateOf<String?>(null) }
    var metrics by remember { mutableStateOf("") }
    var benchmarkSummary by remember { mutableStateOf("") }
    var latestBenchmarkFile by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.CHAT) }
    var confirmExit by remember { mutableStateOf(false) }

    // Android's back gesture is triggered by swiping inward from either screen edge.
    // The app uses Compose tabs rather than navigation destinations, so without
    // this handler Android finishes the Activity even when another tab is visible.
    BackHandler(enabled = true) {
        selectedTab = when (selectedTab) {
            HomeTab.CHAT -> {
                confirmExit = true
                HomeTab.CHAT
            }
            HomeTab.RESEARCH, HomeTab.AGENT_BRIDGE -> HomeTab.WORKSPACE
            else -> HomeTab.CHAT
        }
    }
    var researchTopic by rememberSaveable { mutableStateOf("") }
    var researchSaveRequested by rememberSaveable { mutableStateOf(false) }
    var availableAppUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    val chatScrollState = rememberScrollState()
    val knowledgeScrollState = rememberScrollState()
    val downloadsScrollState = rememberScrollState()
    val settingsScrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        runCatching { GitHubAppUpdates.check(context) }
            .onSuccess { availableAppUpdate = it }
    }

    val currentSession by rememberUpdatedState(session)
    val currentSpeechInput by rememberUpdatedState(speechInput)
    val currentSpeechOutput by rememberUpdatedState(speechOutput)
    val currentImageFile by rememberUpdatedState(imageFile)
    DisposableEffect(Unit) {
        onDispose {
            currentSession?.close()
            currentSpeechInput?.close()
            currentSpeechOutput?.close()
            currentImageFile?.delete()
        }
    }

    fun startOnDeviceSpeech() {
        try {
            val input = OnDeviceSpeechInput(
                context = context,
                languageTag = dictationLanguage.takeUnless { it == "system" },
                onResult = { recognizedText ->
                    prompt = listOfNotNull(prompt.trim().takeIf { it.isNotEmpty() }, recognizedText)
                        .joinToString(" ")
                    status = "Dictation finished on device."
                    speechActive = false
                    speechInput = null
                },
                onError = { message ->
                    status = message
                    speechActive = false
                    speechInput = null
                },
            )
            speechInput = input
            speechActive = true
            status = "Listening offline · $dictationLanguageLabel."
            input.start()
        } catch (error: Exception) {
            speechInput?.close()
            speechInput = null
            speechActive = false
            status = error.message ?: "Could not start on-device speech recognition."
        }
    }

    fun stopLiveVoice() {
        if (!liveVoice) return
        liveEpoch++
        liveVoice = false
        liveJob?.cancel()
        liveBargeInput?.close()
        liveBargeInput = null
        speechInput?.close()
        speechInput = null
        speechActive = false
        speechOutput?.close()
        speechOutput = null
        speechOutputReady = false
        livePhase = "Idle"
    }

    fun beginLiveVoice() {
        if (busy || session == null || liveVoice || liveJob?.isCompleted == false) return
        speechInput?.close()
        speechInput = null
        speechActive = false
        speechOutput?.close()
        speechOutput = null
        speechOutputReady = false
        speechOutputInitializing = false
        liveMuted = false
        liveMetrics = ""
        liveVoice = true
        val epoch = ++liveEpoch
        liveJob = scope.launch {
            try {
                livePhase = "Preparing voice"
                val output = OfflineSpeechOutput(context, dictationLanguage.takeUnless { it == "system" }?.let(Locale::forLanguageTag) ?: Locale.getDefault(), preferKokoro, kokoroVoiceId) { ready, message ->
                    if (liveEpoch == epoch) {
                        speechOutputReady = ready
                        status = message
                    }
                }
                speechOutput = output
                kotlinx.coroutines.withTimeout(120_000) {
                    while (!speechOutputReady) kotlinx.coroutines.delay(100)
                }
                var nextTurn: String? = null
                while (liveVoice && liveEpoch == epoch) {
                    while (liveMuted) kotlinx.coroutines.delay(100)
                    livePhase = "Listening"
                    speechActive = true
                    val heard = nextTurn?.also { nextTurn = null } ?: kotlinx.coroutines.suspendCancellableCoroutine<String> { continuation ->
                        val input = OnDeviceSpeechInput(context,
                            onResult = { if (continuation.isActive) continuation.resumeWith(Result.success(it)) },
                            onError = { if (continuation.isActive) {
                                if (it.startsWith("No speech")) continuation.resumeWith(Result.success(""))
                                else continuation.resumeWith(Result.failure(IllegalStateException(it)))
                            } },
                            onPartial = { if (!liveMuted) liveTranscript = it },
                            languageTag = dictationLanguage.takeUnless { it == "system" })
                        speechInput = input
                        try { input.start() } catch (error: Exception) {
                            if (continuation.isActive) continuation.resumeWith(Result.failure(error))
                        }
                    }
                    speechInput?.close()
                    speechInput = null
                    speechActive = false
                    if (heard.isBlank() || liveMuted) { kotlinx.coroutines.delay(350); continue }
                    val turnStarted = android.os.SystemClock.elapsedRealtime()
                    var firstText: Long? = null
                    var firstSpeech: Long? = null
                    liveTranscript = heard
                    if (activePrompt.isNotBlank()) priorTurns = (priorTurns + (activePrompt to response)).takeLast(20)
                    activePrompt = heard
                    response = ""
                    busy = true
                    livePhase = "Thinking"
                    liveInterrupted = false
                    try {
                        kotlinx.coroutines.coroutineScope {
                            val phrases = kotlinx.coroutines.channels.Channel<String>(8)
                            val segmenter = VoicePhraseBuffer(VoiceDevicePolicy.phraseSize(context))
                            val interruption = kotlinx.coroutines.CompletableDeferred<String>()
                            var bargeStarted = false
                            val speaker = launch {
                                for (phrase in phrases) {
                                    if (liveInterrupted) continue
                                    livePhase = "Speaking"
                                    if (firstSpeech == null) firstSpeech = android.os.SystemClock.elapsedRealtime() - turnStarted
                                    if (!liveMuted && liveBargeEnabled && !bargeStarted) {
                                        bargeStarted = true
                                        val listener = OnDeviceSpeechInput(context,
                                            languageTag = dictationLanguage.takeUnless { it == "system" },
                                            duplex = true,
                                            onPartial = { partial ->
                                                if (VoiceInteractionPolicy.isInterruption(partial, response) && !liveInterrupted && !liveMuted) {
                                                    liveInterrupted = true
                                                    output.stop()
                                                    livePhase = "Listening to interruption"
                                                }
                                            },
                                            onResult = { text ->
                                                if (VoiceInteractionPolicy.isInterruption(text, response) && !liveMuted) {
                                                    liveInterrupted = true
                                                    output.stop()
                                                    interruption.complete(text)
                                                    liveTurnJob?.cancel()
                                                }
                                            },
                                            onError = { status = "Automatic interruption unavailable; use the interrupt button." })
                                        liveBargeInput = listener
                                        listener.start()
                                    }
                                    val spoken = VoiceInteractionPolicy.cleanForSpeech(phrase)
                                    if (spoken.isNotBlank()) output.speakAndAwait(spoken)
                                }
                            }
                            val generation = launch {
                                val spokenPrompt = VoiceInteractionPolicy.prompt(heard, dictationLanguage)
                                val toolAnswer = if (liveToolsAllowed) VoiceTools.execute(context, heard) else null
                                val stream = if (toolAnswer != null) kotlinx.coroutines.flow.flowOf(toolAnswer) else checkNotNull(session).stream(spokenPrompt).flowOn(Dispatchers.Default)
                                stream.collect { piece ->
                                    if (firstText == null && piece.isNotBlank()) firstText = android.os.SystemClock.elapsedRealtime() - turnStarted
                                    response += piece
                                    if (!liveInterrupted) {
                                        for (phrase in segmenter.append(piece)) phrases.send(phrase)
                                    }
                                }
                                if (!liveInterrupted) segmenter.finish()?.let { phrases.send(it) }
                            }
                            liveTurnJob = generation
                            try {
                                generation.join()
                                phrases.close()
                                speaker.join()
                                if (liveInterrupted && bargeStarted && !interruption.isCompleted) {
                                    kotlinx.coroutines.withTimeoutOrNull(2500) { interruption.await() }?.let { nextTurn = it }
                                } else if (interruption.isCompleted) nextTurn = interruption.await()
                            } finally {
                                liveTurnJob = null
                                liveBargeInput?.close()
                                liveBargeInput = null
                                phrases.cancel()
                                speaker.cancel()
                            }
                        }
                    } finally { busy = false }
                    liveMetrics = "First text: ${firstText ?: 0} ms · Speech queued: ${firstSpeech ?: 0} ms · Turn: ${android.os.SystemClock.elapsedRealtime() - turnStarted} ms"
                    kotlinx.coroutines.delay(150)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { status = error.message ?: "Voice conversation failed." }
            finally {
                if (liveEpoch == epoch) {
                    speechInput?.close()
                    speechInput = null
                    speechActive = false
                    liveBargeInput?.close()
                    liveBargeInput = null
                    speechOutput?.close()
                    speechOutput = null
                    speechOutputReady = false
                    liveVoice = false
                    livePhase = "Idle"
                }
            }
        }
    }

    LaunchedEffect(selectedTab) { if (selectedTab != HomeTab.VOICE && liveVoice) stopLiveVoice() }
    DisposableEffect(context) {
        val owner = context as? LifecycleOwner
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) stopLiveVoice()
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }

    val livePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && selectedTab == HomeTab.VOICE) beginLiveVoice()
        else status = "Microphone permission is required for live voice."
    }

    val microphonePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startOnDeviceSpeech()
        else status = "Microphone permission was declined. You can still type."
    }

    fun loadModel(file: File) {
        scope.launch {
            busy = true
            val newRuntime = try {
                LocalInferenceRuntimeFactory.createFor(file)
            } catch (error: Exception) {
                status = error.message ?: "Unsupported model format."
                busy = false
                return@launch
            }
            status = "Verifying local model digest and loading on CPU…"
            try {
                val verifiedSha256 = withContext(Dispatchers.IO) {
                    LocalModelCatalog.verify(context, file)
                }
                newRuntime.load(context, file)
                val previousRuntime = session
                session = newRuntime
                withContext(Dispatchers.IO) { previousRuntime?.close() }
                modelName = file.name
                modelSha256 = verifiedSha256
                context.getSharedPreferences("model_state", Context.MODE_PRIVATE)
                    .edit().putString("last_model_name", file.name).apply()
                response = ""
                activePrompt = ""
                priorTurns = emptyList()
                metrics = ""
                benchmarkSummary = ""
                latestBenchmarkFile = null
                status = "Ready. Inference stays on this device."
            } catch (error: Exception) {
                withContext(Dispatchers.IO) { newRuntime.close() }
                status = error.message ?: "Could not load this model."
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            File(context.cacheDir, "zalmoxis-attachments").deleteRecursively()
        }
        models = withContext(Dispatchers.IO) { LocalModelCatalog.list(context) }
        val savedName = context.getSharedPreferences("model_state", Context.MODE_PRIVATE)
            .getString("last_model_name", null)
        val savedFile = savedName?.let { name -> models.firstOrNull { it.name == name }?.file }
        if (savedFile != null) {
            busy = true
            status = "Restoring the last local model…"
            try {
                val restoredRuntime = LocalInferenceRuntimeFactory.createFor(savedFile)
                modelSha256 = withContext(Dispatchers.IO) {
                    LocalModelCatalog.verify(context, savedFile)
                }
                restoredRuntime.load(context, savedFile)
                session = restoredRuntime
                modelName = savedFile.name
                status = "Ready. Inference stays on this device."
            } catch (error: Exception) {
                session?.close()
                session = null
                modelName = null
                modelSha256 = null
                status = error.message ?: "Could not restore the local model."
            } finally {
                busy = false
            }
        } else if (savedName != null) {
            status = "The saved model file is no longer available. Choose or import a model."
        }
    }

    val benchmarkExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val report = latestBenchmarkFile
        if (uri != null && report != null) {
            scope.launch {
                busy = true
                status = "Exporting the local benchmark report…"
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "wt").use { output ->
                            requireNotNull(output) { "Could not open the selected export destination." }
                            report.inputStream().use { input -> input.copyTo(output) }
                        }
                    }
                    status = "Benchmark report exported through the selected document provider."
                } catch (error: Exception) {
                    status = error.message ?: "Could not export benchmark report."
                } finally {
                    busy = false
                }
            }
        }
    }

    val modelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                status = "Importing model into private app storage…"
                try {
                    val file = withContext(Dispatchers.IO) { LocalModelCatalog.importFile(context, uri) }
                    models = withContext(Dispatchers.IO) { LocalModelCatalog.list(context) }
                    busy = false
                    loadModel(file)
                } catch (error: Exception) {
                    status = error.message ?: "Could not import this model."
                    busy = false
                }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                status = "Copying selected image into temporary private storage…"
                try {
                    val newFile = withContext(Dispatchers.IO) { copyImage(context, uri) }
                    imageFile?.let { old ->
                        withContext(Dispatchers.IO) { old.delete() }
                    }
                    imageFile = newFile
                    status = "Image attached. It will be removed after this request."
                } catch (error: Exception) {
                    status = error.message ?: "Could not attach this image."
                } finally {
                    busy = false
                }
            }
        }
    }


    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        V2Header(onSettings = { selectedTab = HomeTab.SETTINGS })
        if (availableAppUpdate != null && selectedTab != HomeTab.SETTINGS) {
            androidx.compose.material3.TextButton(
                onClick = { selectedTab = HomeTab.SETTINGS },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "New signed Zalmoxis build available — review update",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (busy || status != "Choose a compatible .litertlm file stored on this device.") {
            Text(
                status,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
            )
        }
        val activeScroll = when (selectedTab) {
            HomeTab.CHAT, HomeTab.VOICE -> chatScrollState
            HomeTab.KNOWLEDGE -> knowledgeScrollState
            HomeTab.DOWNLOADS -> downloadsScrollState
            HomeTab.WORKSPACE, HomeTab.RESEARCH, HomeTab.AGENT_BRIDGE, HomeTab.SETTINGS -> settingsScrollState
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(activeScroll)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (selectedTab) {
                HomeTab.CHAT -> {
                    if (activePrompt.isEmpty() && priorTurns.isEmpty()) {
                        V2Welcome(
                            onKnowledge = { selectedTab = HomeTab.KNOWLEDGE },
                            onModels = { selectedTab = HomeTab.DOWNLOADS },
                            onVoice = { selectedTab = HomeTab.VOICE },
                        )
                    }
                    Text(
                        modelName?.let { "Active model · $it" }
                            ?: "No model loaded. Choose one in Downloads to start a conversation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
        if (imageFile != null) Text("Image attached to next request", style = MaterialTheme.typography.bodySmall)
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        priorTurns.forEach { (question, answer) ->
            Text(question, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            if (answer.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(answer, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (activePrompt.isNotBlank()) {
            Text(activePrompt, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        if (metrics.isNotBlank()) Text(metrics, style = MaterialTheme.typography.bodySmall)
        if (response.isNotBlank()) {
            Text("Response", style = MaterialTheme.typography.titleMedium)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    response,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        if (response.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val output = speechOutput
                        if (output == null || !speechOutputReady) {
                            output?.close()
                            speechOutputReady = false
                            speechOutputInitializing = true
                            speechOutput = OfflineSpeechOutput(context, locale = dictationLanguage.takeUnless { it == "system" }?.let(Locale::forLanguageTag) ?: Locale.getDefault(), preferKokoro = preferKokoro, kokoroVoiceId = kokoroVoiceId) { ready, message ->
                                val shouldStartPlayback = ready && speechOutputInitializing
                                speechOutputReady = ready
                                speechOutputInitializing = false
                                status = message
                                if (shouldStartPlayback) speechOutput?.speak(response)
                            }
                            status = "Preparing installed offline speech voices…"
                        } else {
                            output.speak(response)
                            status = "Speaking with an installed voice that reports no network requirement."
                        }
                    },
                    enabled = !busy && !speechOutputInitializing,
                ) {
                    Text(
                        when {
                            speechOutputInitializing -> "Preparing offline speech…"
                            speechOutputReady -> "Speak response offline"
                            speechOutput == null -> "Prepare offline speech"
                            else -> "Retry offline speech setup"
                        }
                    )
                }
                if (speechOutputReady) {
                    Button(
                        onClick = {
                            speechOutput?.stop()
                            status = "Speech stopped."
                        },
                        enabled = !busy,
                    ) { Text("Stop speech") }
                }
            }
            Text(
                "Speech synthesis is user-triggered and selects an installed voice that reports no network requirement.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
                }
                HomeTab.VOICE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text("Dictation language:", style = MaterialTheme.typography.bodySmall)
                        androidx.compose.foundation.layout.Box {
                            TextButton(
                                enabled = !speechActive,
                                onClick = { dictationMenuExpanded = true },
                            ) { Text(dictationLanguageLabel) }
                            DropdownMenu(
                                expanded = dictationMenuExpanded,
                                onDismissRequest = { dictationMenuExpanded = false },
                            ) {
                                dictationOptions.forEach { (tag, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            dictationLanguage = tag
                                            speechPreferences.edit().putString("dictation_language", tag).apply()
                                            dictationMenuExpanded = false
                                            status = "Dictation language set to $label. Offline support depends on the installed Android recognizer."
                                        },
                                    )
                                }
                            }
                        }
                    }
                    V2VoicePanel(
                        isListening = liveVoice,
                        feedback = if (liveVoice) livePhase else status,
                        onToggle = {
                            if (liveVoice) stopLiveVoice()
                            else if (session == null || busy) status = "Load a local model before starting a conversation."
                            else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) beginLiveVoice()
                            else livePermission.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onBack = { stopLiveVoice(); selectedTab = HomeTab.CHAT },
                    )
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        androidx.compose.material3.Switch(checked = liveBargeEnabled, onCheckedChange = { liveBargeEnabled = it }, enabled = !liveVoice)
                        Text("Automatic voice interruption (experimental)")
                    }
                    KokoroSettings(
                        enabled = !liveVoice && !busy,
                        preferred = preferKokoro,
                        locale = dictationLanguage.takeUnless { it == "system" }?.let(Locale::forLanguageTag) ?: Locale.getDefault(),
                        voiceId = kokoroVoiceId,
                        onVoice = { kokoroVoiceId = it; speechPreferences.edit().putString("kokoro_voice_id", it).apply() },
                        onPreferred = { preferKokoro = it; speechPreferences.edit().putBoolean("prefer_kokoro", it).apply() },
                    )
                    if (!liveVoice) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            androidx.compose.material3.Switch(checked = liveToolsAllowed, onCheckedChange = { liveToolsAllowed = it })
                            Text("Allow explicit spoken research and memory commands")
                        }
                    }
                    if (liveVoice) {
                        Text("Voice session · $livePhase", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { stopLiveVoice() }) { Text("End conversation") }
                            TextButton(onClick = {
                                liveMuted = !liveMuted
                                if (liveMuted) { speechInput?.close(); liveBargeInput?.close(); livePhase = "Microphone muted"; stopLiveVoice() }
                            }) { Text("Mute / end mic session") }
                            TextButton(onClick = { liveCaptions = !liveCaptions }) { Text(if (liveCaptions) "Hide text" else "Show text") }
                        }
                        if (livePhase == "Speaking") {
                            Button(onClick = { liveInterrupted = true; speechOutput?.stop(); liveTurnJob?.cancel(); livePhase = "Listening to interruption" }) { Text("Interrupt speech") }
                        }
                    }
                    if (liveMetrics.isNotBlank()) Text(liveMetrics, style = MaterialTheme.typography.bodySmall)
                    if (liveCaptions && liveTranscript.isNotBlank()) {
                        Text("You: $liveTranscript")
                        Text(response)
                    }
                    if (prompt.isNotBlank()) {
                        Text("Transcript", style = MaterialTheme.typography.titleMedium)
                        Text(prompt, style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { selectedTab = HomeTab.CHAT }) { Text("Use in chat") }
                    }
                }
                HomeTab.KNOWLEDGE -> {
                    KnowledgeWorkspace(
                        busy = busy,
                        onBusyChange = { busy = it },
                        onStatus = { status = it },
                    )
                }
                HomeTab.DOWNLOADS -> {
                    Text("Models", style = MaterialTheme.typography.headlineMedium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { modelPicker.launch(arrayOf("*/*")) },
                enabled = !busy,
            ) { Text("Import model") }
            modelName?.let {
                Text("Active: $it", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (models.isNotEmpty()) {
            Text("Models on this device", style = MaterialTheme.typography.titleMedium)
            models.forEach { model ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("⬡", style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${formatBytes(model.sizeBytes)} · " +
                                        if (model.provenanceValid) "Verified local digest" else "Needs reimport",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (model.provenanceValid)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.error,
                                )
                            }
                            if (model.name == modelName) {
                                Text("ACTIVE", color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        TextButton(onClick = {
                            expandedModel = if (expandedModel == model.name) null else model.name
                        }) { Text(if (expandedModel == model.name) "Hide details" else "Show details") }
                        if (expandedModel == model.name) {
                            Text(
                                "SHA-256 ${model.sha256.take(16)}…\n" +
                                    "${model.sourceDescription} · Imported ${formatTimestamp(model.importedAtEpochMs)}\n" +
                                    "License: ${model.licenseStatus}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { loadModel(model.file) },
                            enabled = !busy && model.name != modelName && model.provenanceValid,
                        ) { Text(if (model.name == modelName) "Loaded" else "Load") }
                        Button(
                            onClick = { pendingDeleteModel = model },
                            enabled = !busy,
                        ) { Text("Delete") }
                        }
                    }
                }
            }
        }


        Text("Available models", style = MaterialTheme.typography.titleMedium)
        Text(
            "Catalog downloads contact Hugging Face. Prompts and inference stay on this device. " +
                "Review the license and device guidance; downloading does not verify runtime compatibility.",
            style = MaterialTheme.typography.bodySmall,
        )
        ModelCatalog.entries.forEach { entry ->
            CatalogDownloadCard(
                entry = entry,
                installed = models.any { it.name == entry.fileName },
                onInstalled = {
                    scope.launch {
                        models = withContext(Dispatchers.IO) { LocalModelCatalog.list(context) }
                        status = "${entry.displayName} downloaded and verified. Choose Load when ready."
                    }
                },
            )
        }
                }
                HomeTab.WORKSPACE -> {
                    V2Workspace(
                        onKnowledge = { selectedTab = HomeTab.KNOWLEDGE },
                        onModels = { selectedTab = HomeTab.DOWNLOADS },
                        onResearch = { selectedTab = HomeTab.RESEARCH },
                        onAgentBridge = { selectedTab = HomeTab.AGENT_BRIDGE },
                    )
                }
                HomeTab.RESEARCH -> {
                    ResearchWorkspace(
                        onBack = { selectedTab = HomeTab.WORKSPACE },
                        onKnowledge = { selectedTab = HomeTab.KNOWLEDGE },
                        initialTopic = researchTopic,
                        onInitialTopicConsumed = { researchTopic = ""; researchSaveRequested = false },
                        initialSaveProposal = researchSaveRequested,
                        localRuntime = session,
                    )
                }
                HomeTab.AGENT_BRIDGE -> {
                    AgentBridgeWorkspace(
                        onBack = { selectedTab = HomeTab.WORKSPACE },
                        localRuntime = session,
                        inferenceBusy = busy,
                    )
                }
                HomeTab.SETTINGS -> {
                    Text("Settings", style = MaterialTheme.typography.headlineMedium)
                    AppUpdateSettings()
                    Text("Diagnostics", style = MaterialTheme.typography.titleMedium)
        if (modelName != null && session != null) {
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        benchmarkSummary = ""
                        status = "Running one warm-up and three measured CPU runs…"
                        try {
                            val activeRuntime = checkNotNull(session)
                            val samples = activeRuntime.runWarmBenchmark(
                                prompt = "Reply with exactly OK.",
                                measuredRuns = 3,
                            )
                            val file = withContext(Dispatchers.IO) {
                                saveBenchmarkReport(
                                    context,
                                    modelName!!,
                                    checkNotNull(modelSha256),
                                    activeRuntime.runtimeName,
                                    activeRuntime.backendName,
                                    samples,
                                )
                            }
                            latestBenchmarkFile = file
                            val averageTotal = samples.map { it.totalMs }.average().toLong()
                            val firstTextTimes = samples.mapNotNull { it.firstTextMs }
                            val averageFirstText =
                                if (firstTextTimes.isEmpty()) null else firstTextTimes.average().toLong()
                            benchmarkSummary =
                                "3 measured runs · average first text: " +
                                    (averageFirstText?.let { "$it ms" } ?: "unavailable") +
                                    " · average total: $averageTotal ms"
                            status = "Benchmark saved in private app storage: ${file.name}"
                        } catch (error: Exception) {
                            status = error.message ?: "Benchmark failed."
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
            ) { Text("Run 3-run benchmark") }
            Text(
                "Runs a fixed prompt after one unreported warm-up. Reports latency and stream events; " +
                    "it does not claim token throughput.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (benchmarkSummary.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    benchmarkSummary,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = {
                        latestBenchmarkFile?.let { report ->
                            benchmarkExporter.launch(report.name)
                        }
                    },
                    enabled = !busy && latestBenchmarkFile != null,
                ) { Text("Export JSON") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Models are stored in Zalmoxis private storage. Catalog downloads contact Hugging Face; " +
                "prompts, inference, and benchmark reports stay local. No account, analytics, or " +
                "model-provided tool execution is enabled.",
            style = MaterialTheme.typography.bodySmall,
        )
                }
            }
        }
        if (selectedTab == HomeTab.CHAT) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text("Offline dictation:", style = MaterialTheme.typography.labelSmall)
                androidx.compose.foundation.layout.Box {
                    TextButton(
                        enabled = !speechActive,
                        onClick = { dictationMenuExpanded = true },
                    ) { Text(dictationLanguageLabel) }
                    DropdownMenu(
                        expanded = dictationMenuExpanded,
                        onDismissRequest = { dictationMenuExpanded = false },
                    ) {
                        dictationOptions.forEach { (tag, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    dictationLanguage = tag
                                    speechPreferences.edit().putString("dictation_language", tag).apply()
                                    dictationMenuExpanded = false
                                    status = "Dictation language set to $label. Retry the microphone."
                                },
                            )
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ZalmoxisV2.background,
                tonalElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { imagePicker.launch(arrayOf("image/*")) },
                            enabled = modelName != null && !busy,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                        ) { V2Icon("Attach", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimary) }
                        Button(
                            onClick = {
                                if (speechActive) {
                                    speechInput?.cancel()
                                    speechInput = null
                                    speechActive = false
                                    status = "Dictation stopped."
                                } else if (ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO,
                                    ) == PackageManager.PERMISSION_GRANTED) {
                                    startOnDeviceSpeech()
                                } else {
                                    microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            enabled = !busy,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                        ) {
                            V2Icon(
                                if (speechActive) "Stop" else "Microphone",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask Zalmoxis…") },
                            enabled = !busy && !speechActive,
                            maxLines = 3,
                            singleLine = false,
                        )
                                Button(
                                    onClick = {
                                        val selectedImage = imageFile
                                        val submitted = prompt.trim().ifEmpty {
                                            if (selectedImage != null) "Describe this image." else ""
                                        }
                                        if (submitted.isNotEmpty()) {
                                            val researchRequest = if (selectedImage == null)
                                                researchTopicFromChat(submitted) else null
                                            if (researchRequest != null) {
                                                researchTopic = researchRequest
                                                researchSaveRequested = hasResearchSaveDirective(submitted)
                                                prompt = ""
                                                selectedTab = HomeTab.RESEARCH
                                            } else {
                                            if (activePrompt.isNotBlank()) {
                                                priorTurns = (priorTurns + (activePrompt to response)).takeLast(20)
                                            }
                                            activePrompt = submitted
                                            prompt = ""
                                            imageFile = null
                                            response = ""
                                            metrics = ""
                                            busy = true
                                            scope.launch {
                                                val startedAt = System.nanoTime()
                                                var firstOutputMs: Long? = null
                                                var outputChunks = 0
                                                try {
                                                    checkNotNull(session) { "Load a local model first." }
                                                        .stream(submitted, selectedImage)
                                                        .flowOn(Dispatchers.Default)
                                                        .collect { piece ->
                                                        if (firstOutputMs == null) {
                                                            firstOutputMs = (System.nanoTime() - startedAt) / 1_000_000L
                                                        }
                                                        outputChunks += 1
                                                        response += piece
                                                    }
                                                    val totalMs = (System.nanoTime() - startedAt) / 1_000_000L
                                                    metrics = "First text: ${firstOutputMs ?: 0} ms · Total: ${totalMs} ms · Stream events: $outputChunks"
                                                    status = "Response complete. No tools were available to the model."
                                                } catch (error: Exception) {
                                                    status = error.message ?: "Inference failed."
                                                } finally {
                                                    withContext(Dispatchers.IO) { selectedImage?.delete() }
                                                    busy = false
                                                }
                                            }
                                            }
                                        }
                                    },
                                    enabled = (modelName != null || researchTopicFromChat(prompt) != null) &&
                                        (prompt.isNotBlank() || imageFile != null) && !busy,
                                ) {
                                    V2Icon("Send", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                }
                        
                        
                    }
                    if (imageFile != null) {
                        TextButton(
                            onClick = {
                                val pending = imageFile
                                imageFile = null
                                scope.launch { withContext(Dispatchers.IO) { pending?.delete() } }
                            },
                        ) { Text("Remove attached image") }
                    }
                }
            }
        }
        V2Navigation(
            selected = when (selectedTab) {
                HomeTab.VOICE, HomeTab.SETTINGS -> "CHAT"
                HomeTab.RESEARCH, HomeTab.AGENT_BRIDGE -> "WORKSPACE"
                else -> selectedTab.name
            },
            onNavigate = { key ->
                selectedTab = HomeTab.valueOf(key)
            },
        )
    }


    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Leave Zalmoxis?") },
            text = { Text("Your current session will close. Cancel to stay in the app.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    (context as? android.app.Activity)?.finish()
                }) { Text("Exit") }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("Stay") }
            },
        )
    }

    if (incomingSharedText != null) {
        AlertDialog(
            onDismissRequest = onSharedTextHandled,
            title = { Text("Receive shared agent message?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Source: Android share sheet (sender identity not verified).")
                    Text(
                        incomingSharedText.take(1200),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 12,
                    )
                    Text("Accepting fills your chat draft. It does not execute any tools or modify Knowledge.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prompt = incomingSharedText.take(AgentBridgeHandoff.MAX_MESSAGE_LENGTH)
                    selectedTab = HomeTab.CHAT
                    onSharedTextHandled()
                }) { Text("Use as chat draft") }
            },
            dismissButton = {
                TextButton(onClick = onSharedTextHandled) { Text("Discard") }
            },
        )
    }

    pendingDeleteModel?.let { model ->
        AlertDialog(
            onDismissRequest = { pendingDeleteModel = null },
            title = { Text("Delete local model?") },
            text = { Text("Remove ${model.name} from this device? You may need to import or download it again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteModel = null
                        scope.launch {
                            busy = true
                            try {
                                val removed = withContext(Dispatchers.IO) { LocalModelCatalog.delete(model) }
                                if (removed) {
                                    if (model.name == modelName) {
                                        session?.close()
                                        session = null
                                        modelName = null
                                        modelSha256 = null
                                        response = ""
                                        activePrompt = ""
                                        priorTurns = emptyList()
                                        context.getSharedPreferences("model_state", Context.MODE_PRIVATE)
                                            .edit().remove("last_model_name").apply()
                                    }
                                    models = withContext(Dispatchers.IO) { LocalModelCatalog.list(context) }
                                }
                                status = if (removed) "Model deleted." else "Could not verify model deletion."
                            } catch (error: Exception) {
                                status = error.message ?: "Could not delete the model."
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteModel = null }) { Text("Cancel") }
            },
        )
    }

}

@Composable
private fun CatalogDownloadCard(
    entry: ModelCatalogEntry,
    installed: Boolean,
    onInstalled: () -> Unit,
) {
    val context = LocalContext.current
    var downloadState by remember(entry.id) { mutableStateOf<ModelDownloadState?>(null) }
    var actionMessage by remember(entry.id) { mutableStateOf<String?>(null) }

    DisposableEffect(context, entry.id) {
        val owner = context as LifecycleOwner
        val liveData = ModelDownloadCoordinator.observe(context, entry.id)
        val observer = Observer<ModelDownloadState> { state -> downloadState = state }
        liveData.observe(owner, observer)
        onDispose { liveData.removeObserver(observer) }
    }

    LaunchedEffect(downloadState?.status, installed) {
        if (downloadState?.status == ModelDownloadStatus.SUCCEEDED && !installed) onInstalled()
    }

    val transferStatus = downloadState?.status
    val downloaded = downloadState?.bytesDownloaded ?: 0L
    val total = downloadState?.totalBytes ?: entry.sizeBytes
    val progressPercent = if (total > 0L) ((downloaded * 100L) / total).toInt() else -1
    val active = transferStatus == ModelDownloadStatus.RUNNING ||
        transferStatus == ModelDownloadStatus.ENQUEUED
    val finished = installed && transferStatus != ModelDownloadStatus.RUNNING &&
        transferStatus != ModelDownloadStatus.ENQUEUED

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(entry.displayName, style = MaterialTheme.typography.titleSmall)
            Text(
                "${entry.summary} ${formatBytes(entry.sizeBytes)} · ${entry.minimumMemoryGb} GB RAM guidance · " +
                    "${entry.modalities.joinToString().replaceFirstChar { it.uppercase() }} · ${entry.licenseName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Source: ${entry.repoId} at pinned revision ${entry.revision.take(10)}… · License: ${entry.licenseUrl}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (active) {
                LinearProgressIndicator(
                    progress = { (progressPercent.coerceIn(0, 100)) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (progressPercent >= 0) {
                        "Downloading: $progressPercent% (${formatBytes(downloaded)} / ${formatBytes(total)})"
                    } else {
                        "Download queued…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (transferStatus == ModelDownloadStatus.FAILED) {
                Text(
                    downloadState?.error ?: "Download failed. Retry to resume the verified partial transfer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (transferStatus == ModelDownloadStatus.CANCELLED) {
                Text(
                    "Download cancelled. A valid partial transfer may resume on retry.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            actionMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Button(
                onClick = {
                    if (active) {
                        ModelDownloadCoordinator.cancel(context, entry.id)
                    } else if (!installed) {
                        actionMessage = if (ModelDownloadCoordinator.start(context, entry.id)) null
                        else "Download could not be started."
                    }
                },
                enabled = active || !installed,
            ) {
                Text(
                    when {
                        active -> "Cancel download"
                        installed -> "Installed"
                        transferStatus == ModelDownloadStatus.FAILED ||
                            transferStatus == ModelDownloadStatus.CANCELLED -> "Retry download"
                        else -> "Download"
                    }
                )
            }
        }
    }
}

private fun formatTimestamp(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))

private fun formatBytes(size: Long): String = when {
    size >= (1L shl 30) -> String.format("%.1f GiB", size / (1L shl 30).toDouble())
    size >= (1L shl 20) -> String.format("%.0f MiB", size / (1L shl 20).toDouble())
    else -> String.format("%.0f KiB", size / 1024.0)
}

private suspend fun saveBenchmarkReport(
    context: Context,
    modelName: String,
    modelSha256: String,
    runtimeName: String,
    backendName: String,
    samples: List<InferenceSample>,
): File {
    val directory = File(context.filesDir, "benchmarks").apply { mkdirs() }
    val createdAt = System.currentTimeMillis()
    val runs = JSONArray()
    samples.forEach { sample ->
        runs.put(
            JSONObject()
                .put("run", sample.run)
                .put("first_text_ms", sample.firstTextMs ?: JSONObject.NULL)
                .put("total_ms", sample.totalMs)
                .put("stream_events", sample.streamEvents)
        )
    }
    val report = JSONObject()
        .put("schema_version", 1)
        .put("created_at_epoch_ms", createdAt)
        .put("model_file", modelName)
        .put("model_sha256", modelSha256)
        .put("runtime", runtimeName)
        .put("backend", backendName)
        .put("device_manufacturer", Build.MANUFACTURER)
        .put("device_model", Build.MODEL)
        .put("android_api", Build.VERSION.SDK_INT)
        .put("protocol", "one warm-up excluded; three fresh-conversation runs; fixed prompt")
        .put("prompt", "Reply with exactly OK.")
        .put("unit_note", "stream_events are runtime events, not token counts")
        .put("runs", runs)
    return File(directory, "benchmark-$createdAt.json").apply {
        writeText(report.toString(2))
    }
}

private fun copyImage(context: Context, uri: Uri): File {
    val mimeType = context.contentResolver.getType(uri)
    require(mimeType?.startsWith("image/", ignoreCase = true) == true) {
        "Choose an image file."
    }
    val directory = File(context.cacheDir, "zalmoxis-attachments").apply { mkdirs() }
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "img"
    val destination = File(directory, "image-${java.util.UUID.randomUUID()}.$extension")
    try {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open the selected image." }
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        require(destination.length() > 0L) { "The selected image is empty." }
        return destination
    } catch (error: Throwable) {
        destination.delete()
        throw error
    }
}
