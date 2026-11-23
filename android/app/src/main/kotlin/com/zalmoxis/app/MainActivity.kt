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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) { ZalmoxisHome() }
            }
        }
    }
}

@Composable
private fun ZalmoxisHome() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<OnDeviceInferenceRuntime?>(null) }
    var models by remember { mutableStateOf(emptyList<LocalModel>()) }
    var modelName by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Choose a compatible .litertlm file stored on this device.") }
    var prompt by remember { mutableStateOf("") }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var speechInput by remember { mutableStateOf<OnDeviceSpeechInput?>(null) }
    var speechActive by remember { mutableStateOf(false) }
    var response by remember { mutableStateOf("") }
    var metrics by remember { mutableStateOf("") }
    var benchmarkSummary by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            session?.close()
            speechInput?.close()
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
            status = "Loading model on CPU…"
            try {
                newRuntime.load(context, file)
                val previousRuntime = session
                session = newRuntime
                previousRuntime?.close()
                modelName = file.name
                context.getSharedPreferences("model_state", Context.MODE_PRIVATE)
                    .edit().putString("last_model_name", file.name).apply()
                response = ""
                metrics = ""
                benchmarkSummary = ""
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
                restoredRuntime.load(context, savedFile)
                session = restoredRuntime
                modelName = savedFile.name
                status = "Ready. Inference stays on this device."
            } catch (error: Exception) {
                session?.close()
                session = null
                status = error.message ?: "Could not restore the local model."
            } finally {
                busy = false
            }
        } else if (savedName != null) {
            status = "The saved model file is no longer available. Choose or import a model."
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ZALMOXIS", style = MaterialTheme.typography.headlineMedium)
        Text("Local model workspace", style = MaterialTheme.typography.titleMedium)
        Text(status, style = MaterialTheme.typography.bodyMedium)

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "${model.name} · ${formatBytes(model.sizeBytes)}",
                        modifier = Modifier.weight(1f).padding(top = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = { loadModel(model.file) },
                        enabled = !busy && model.name != modelName,
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
                                    activeRuntime.runtimeName,
                                    activeRuntime.backendName,
                                    samples,
                                )
                            }
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
            Text(benchmarkSummary, style = MaterialTheme.typography.bodyMedium)
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
            Text(response, style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Models are copied into Zalmoxis private storage. No account, analytics, network permission, " +
                "or model-provided tool execution is enabled.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatBytes(size: Long): String = when {
    size >= (1L shl 30) -> String.format("%.1f GiB", size / (1L shl 30).toDouble())
    size >= (1L shl 20) -> String.format("%.0f MiB", size / (1L shl 20).toDouble())
    else -> String.format("%.0f KiB", size / 1024.0)
}

private suspend fun saveBenchmarkReport(
    context: Context,
    modelName: String,
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
