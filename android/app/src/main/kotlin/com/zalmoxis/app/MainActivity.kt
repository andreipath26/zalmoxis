package com.zalmoxis.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
    val session = remember { LocalInferenceSession() }
    var models by remember { mutableStateOf(emptyList<LocalModel>()) }
    var modelName by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Choose a compatible .litertlm file stored on this device.") }
    var prompt by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var metrics by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    DisposableEffect(session) { onDispose { session.close() } }

    fun loadModel(file: File) {
        scope.launch {
            busy = true
            status = "Loading model on CPU…"
            try {
                session.load(context, file)
                modelName = file.name
                context.getSharedPreferences("model_state", Context.MODE_PRIVATE)
                    .edit().putString("last_model_name", file.name).apply()
                response = ""
                metrics = ""
                status = "Ready. Inference stays on this device."
            } catch (error: Exception) {
                status = error.message ?: "Could not load this model."
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(session) {
        models = withContext(Dispatchers.IO) { LocalModelCatalog.list(context) }
        val savedName = context.getSharedPreferences("model_state", Context.MODE_PRIVATE)
            .getString("last_model_name", null)
        val savedFile = savedName?.let { name -> models.firstOrNull { it.name == name }?.file }
        if (savedFile != null) {
            busy = true
            status = "Restoring the last local model…"
            try {
                session.load(context, savedFile)
                modelName = savedFile.name
                status = "Ready. Inference stays on this device."
            } catch (error: Exception) {
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
                    val file = withContext(Dispatchers.IO) { LocalModelCatalog.import(context, uri) }
                    models = withContext(Dispatchers.IO) { LocalModelCatalog.list(context) }
                    loadModel(file)
                } catch (error: Exception) {
                    status = error.message ?: "Could not import this model."
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
                                        session.close()
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
                val submitted = prompt.trim()
                if (submitted.isNotEmpty()) {
                    prompt = ""
                    response = ""
                    metrics = ""
                    busy = true
                    scope.launch {
                        val startedAt = System.nanoTime()
                        var firstOutputMs: Long? = null
                        var outputChunks = 0
                        try {
                            session.stream(submitted).collect { piece ->
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
                            busy = false
                        }
                    }
                }
            },
            enabled = modelName != null && prompt.isNotBlank() && !busy,
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
