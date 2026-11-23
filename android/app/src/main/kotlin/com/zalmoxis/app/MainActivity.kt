package com.zalmoxis.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import java.io.FileOutputStream

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
    var modelName by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Choose a compatible .litertlm file stored on this device.") }
    var prompt by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    DisposableEffect(session) { onDispose { session.close() } }

    LaunchedEffect(session) {
        val savedName = context.getSharedPreferences("model_state", Context.MODE_PRIVATE)
            .getString("last_model_name", null)
        if (savedName != null) {
            busy = true
            status = "Restoring the last local model…"
            try {
                val localFile = withContext(Dispatchers.IO) {
                    File(context.filesDir, "models/$savedName").takeIf { it.isFile }
                }
                if (localFile == null) {
                    status = "The saved model file is no longer available. Choose it again."
                } else {
                    session.load(context, localFile)
                    modelName = localFile.name
                    status = "Ready. Inference stays on this device."
                }
            } catch (error: Exception) {
                status = error.message ?: "Could not restore the local model."
            } finally {
                busy = false
            }
        }
    }

    val modelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                status = "Copying model into private app storage…"
                try {
                    val localFile = withContext(Dispatchers.IO) { importModel(context, uri) }
                    status = "Loading model on CPU…"
                    session.load(context, localFile)
                    context.getSharedPreferences("model_state", Context.MODE_PRIVATE)
                        .edit().putString("last_model_name", localFile.name).apply()
                    modelName = localFile.name
                    response = ""
                    status = "Ready. Inference stays on this device."
                } catch (error: Exception) {
                    status = error.message ?: "Could not load this model."
                    modelName = null
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
            ) { Text(if (modelName == null) "Choose model" else "Change model") }
            modelName?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }
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
                    busy = true
                    scope.launch {
                        try {
                            session.stream(submitted).collect { piece -> response += piece }
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

private fun importModel(context: Context, uri: Uri): File {
    val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
    val displayName = queryDisplayName(context, uri) ?: "local-model.litertlm"
    require(displayName.endsWith(".litertlm", ignoreCase = true)) {
        "This first runtime accepts .litertlm files."
    }
    val safeName = displayName.substringAfterLast('/').substringAfterLast('\\')
    val destination = File(modelsDir, safeName)
    val temporary = File(modelsDir, "${safeName}.partial")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Could not open the selected model file." }
        FileOutputStream(temporary).use { output -> input.copyTo(output) }
    }
    require(temporary.length() > 0L) { "The selected file is empty." }
    if (destination.exists()) destination.delete()
    check(temporary.renameTo(destination)) { "Could not finish importing the model." }
    return destination
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
    return null
}
