package com.zalmoxis.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                ) { ZalmoxisHome() }
            }
        }
    }
}

private enum class HomeTab(val title: String) {
    CHAT("Chat"),
    KNOWLEDGE("Knowledge"),
    DOWNLOADS("Downloads"),
    SETTINGS("Settings"),
}

@Composable
private fun ZalmoxisHome() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<OnDeviceInferenceRuntime?>(null) }
    var models by remember { mutableStateOf(emptyList<LocalModel>()) }
    var modelName by remember { mutableStateOf<String?>(null) }
    var modelSha256 by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Choose a compatible .litertlm file stored on this device.") }
    var prompt by remember { mutableStateOf("") }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var speechInput by remember { mutableStateOf<OnDeviceSpeechInput?>(null) }
    var speechActive by remember { mutableStateOf(false) }
    var speechOutput by remember { mutableStateOf<OfflineSpeechOutput?>(null) }
    var speechOutputReady by remember { mutableStateOf(false) }
    var speechOutputInitializing by remember { mutableStateOf(false) }
    var response by remember { mutableStateOf("") }
    var metrics by remember { mutableStateOf("") }
    var benchmarkSummary by remember { mutableStateOf("") }
    var latestBenchmarkFile by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.CHAT) }
    val chatScrollState = rememberScrollState()
    val knowledgeScrollState = rememberScrollState()
    val downloadsScrollState = rememberScrollState()
    val settingsScrollState = rememberScrollState()

    DisposableEffect(Unit) {
        onDispose {
            session?.close()
            speechInput?.close()
            speechOutput?.close()
        }
    }

    fun startOnDeviceSpeech() {
        try {
            val input = OnDeviceSpeechInput(
                context = context,
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
            status = "Listening. Speech recognition is on device."
            input.start()
        } catch (error: Exception) {
            speechInput?.close()
            speechInput = null
            speechActive = false
            status = error.message ?: "Could not start on-device speech recognition."
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startOnDeviceSpeech()
        else status = "Microphone permission was declined. You can still type."
    }

    fun loadModel(file: File) {
        val newRuntime = LocalInferenceRuntimeFactory.createFor(file)
        scope.launch {
            busy = true
            status = "Verifying local model digest and loading on CPU…"
            try {
                val verifiedSha256 = withContext(Dispatchers.IO) {
                    LocalModelCatalog.verify(context, file)
                }
                newRuntime.load(context, file)
                val previousRuntime = session
                session = newRuntime
                previousRuntime?.close()
                modelName = file.name
                modelSha256 = verifiedSha256
                context.getSharedPreferences("model_state", Context.MODE_PRIVATE)
                    .edit().putString("last_model_name", file.name).apply()
                response = ""
                metrics = ""
                benchmarkSummary = ""
                latestBenchmarkFile = null
                status = "Ready. Inference stays on this device."
            } catch (error: Exception) {
                newRuntime.close()
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
                 
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("ZALMOXIS", style = MaterialTheme.typography.headlineSmall)
            Text(
                "LOCAL MODEL WORKSPACE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                HomeTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) },
                    )
                }
            }
        }

        val activeScroll = when (selectedTab) {
            HomeTab.CHAT -> chatScrollState
            HomeTab.KNOWLEDGE -> knowledgeScrollState
            HomeTab.DOWNLOADS -> downloadsScrollState
            HomeTab.SETTINGS -> settingsScrollState
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(activeScroll)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (selectedTab) {
                HomeTab.CHAT -> {
                    Text("CHAT", style = MaterialTheme.typography.titleMedium)
                    Text(
                        modelName?.let { "Active model · $it" }
                            ?: "No model loaded. Choose one in Downloads to start a conversation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { imagePicker.launch(arrayOf("image/*")) },
                enabled = modelName != null && !busy,
            ) { Text("Attach image") }
            imageFile?.let { attached ->
                Text("Image attached", modifier = Modifier.padding(top = 12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { attached.delete() }
                            imageFile = null
                            status = "Image removed."
                        }
                    },
                    enabled = !busy,
                ) { Text("Clear") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (speechActive) {
                        speechInput?.cancel()
                        speechInput = null
                        speechActive = false
                        status = "Dictation stopped."
                    } else if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startOnDeviceSpeech()
                    } else {
                        microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                enabled = !busy,
            ) { Text(if (speechActive) "Stop dictation" else "Dictate on device") }
            Text(
                "Microphone access is requested only when dictation is started; there is no network fallback.",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Message") },
            enabled = modelName != null && !busy,
            minLines = 2,
        )

        Button(
            onClick = {
                val selectedImage = imageFile
                val submitted = prompt.trim().ifEmpty {
                    if (selectedImage != null) "Describe this image." else ""
                }
                if (submitted.isNotEmpty()) {
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
                                .stream(submitted, selectedImage).collect { piece ->
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
            },
            enabled = modelName != null && (prompt.isNotBlank() || imageFile != null) && !busy,
        ) { Text(if (busy) "Working…" else "Send") }

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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val output = speechOutput
                        if (output == null || !speechOutputReady) {
                            output?.close()
                            speechOutputReady = false
                            speechOutputInitializing = true
                            speechOutput = OfflineSpeechOutput(context) { ready, message ->
                                speechOutputReady = ready
                                speechOutputInitializing = false
                                status = message
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
                HomeTab.KNOWLEDGE -> {
                    KnowledgeWorkspace(
                        busy = busy,
                        onBusyChange = { busy = it },
                        onStatus = { status = it },
                    )
                }
                HomeTab.DOWNLOADS -> {
                    Text("DOWNLOADS", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { modelPicker.launch(arrayOf("*/*")) },
                enabled = !busy,
            ) { Text("Import model") }
            modelName?.let { Text("Active: $it", modifier = Modifier.padding(top = 12.dp)) }
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            if (model.provenanceValid) {
                                "${model.name} · ${formatBytes(model.sizeBytes)}\n" +
                                    "SHA-256 ${model.sha256.take(16)}…\n" +
                                    "${model.sourceDescription} · Imported ${formatTimestamp(model.importedAtEpochMs)}\n" +
                                    "License: ${model.licenseStatus}"
                            } else {
                                "${model.name} · ${formatBytes(model.sizeBytes)}\n" +
                                    "Provenance record invalid · Reimport this model to use it."
                            },
                            modifier = Modifier.weight(1f).padding(top = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Button(
                            onClick = { loadModel(model.file) },
                            enabled = !busy && model.name != modelName && model.provenanceValid,
                        ) { Text(if (model.name == modelName) "Loaded" else "Load") }
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    try {
                                        if (model.name == modelName) {
                                            session?.close()
                                            session = null
                                            modelName = null
                                            modelSha256 = null
                                            context.getSharedPreferences("model_state", Context.MODE_PRIVATE)
                                                .edit().remove("last_model_name").apply()
                                        }
                                        val deleted = withContext(Dispatchers.IO) {
                                            LocalModelCatalog.delete(model)
                                        }
                                        models = withContext(Dispatchers.IO) {
                                            LocalModelCatalog.list(context)
                                        }
                                        status = if (deleted) "Model deleted from app storage." else "Could not delete model."
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            enabled = !busy,
                        ) { Text("Delete") }
                    }
                }
            }
        }

        Text("Download a model", style = MaterialTheme.typography.titleMedium)
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
                HomeTab.SETTINGS -> {
                    Text("SETTINGS & DIAGNOSTICS", style = MaterialTheme.typography.titleMedium)
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
                    Text("PRIVACY & PERMISSIONS", style = MaterialTheme.typography.titleMedium)
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
    }
}
t(8.dp))
        Text(
            "Models are stored in Zalmoxis private storage. Catalog downloads contact Hugging Face; " +
                "prompts, inference, and benchmark reports stay local. No account, analytics, or " +
                "model-provided tool execution is enabled.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

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

    LaunchedEffect(downloadState?.status) {
        if (downloadState?.status == ModelDownloadStatus.SUCCEEDED) onInstalled()
    }

    val transferStatus = downloadState?.status
    val downloaded = downloadState?.bytesDownloaded ?: 0L
    val total = downloadState?.totalBytes ?: entry.sizeBytes
    val progressPercent = if (total > 0L) ((downloaded * 100L) / total).toInt() else -1
    val active = transferStatus == ModelDownloadStatus.RUNNING ||
        transferStatus == ModelDownloadStatus.ENQUEUED
    val finished = transferStatus == ModelDownloadStatus.SUCCEEDED && installed

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
                    } else if (!finished) {
                        actionMessage = if (ModelDownloadCoordinator.start(context, entry.id)) null
                        else "Download could not be started."
                    }
                },
                enabled = active || !finished,
            ) {
                Text(
                    when {
                        active -> "Cancel download"
                        finished -> "Installed"
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
