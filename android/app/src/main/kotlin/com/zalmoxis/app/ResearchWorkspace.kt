package com.zalmoxis.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** The local LLM cannot call this component. Each network access requires the user's tap. */
@Composable
internal fun ResearchWorkspace(
    onBack: () -> Unit,
    onKnowledge: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enteredUrl by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<ResearchResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("No online research has been requested.") }
    var saved by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Research", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onBack) { Text("Workspace") }
        }
        Text(
            "Public GitHub sources only. Research uses the network, while model inference " +
                "and saved Knowledge stay on your device. No login, cookies or model-initiated tools.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = enteredUrl,
            onValueChange = {
                enteredUrl = it
                result = null
                saved = false
                status = "Review the URL before allowing a network request."
            },
            label = { Text("Public GitHub URL") },
            placeholder = { Text("https://github.com/owner/repository") },
            enabled = !loading && !saving,
            minLines = 2,
            maxLines = 3,
        )
        val target = runCatching { NativeResearch.hostForConsent(enteredUrl) }.getOrNull()
        Text(
            "Network destination: ${target ?: "No valid HTTPS GitHub URL"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = {
                val requested = enteredUrl.trim()
                scope.launch {
                    loading = true
                    result = null
                    saved = false
                    status = "Fetching the approved public GitHub URL…"
                    try {
                        result = NativeResearch.fetch(requested)
                        status = "Fetched public data. Review its content before saving locally."
                    } catch (error: Exception) {
                        status = error.message ?: "Could not retrieve this public source."
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading && !saving && target in setOf("github.com", "raw.githubusercontent.com"),
        ) { Text(if (loading) "Fetching…" else "Fetch public source") }

        Text(status, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        result?.let { source ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = ZalmoxisV2.panel,
                border = BorderStroke(1.dp, ZalmoxisV2.stroke),
            ) {
                Column(modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(source.title, style = MaterialTheme.typography.titleMedium)
                    Text("Source: ${source.canonicalUrl}",
                        style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Fetched: ${DateFormat.getDateTimeInstance().format(Date(source.fetchedAtMillis))} · " +
                            "${source.adapter} · SHA-256 ${source.sha256.take(16)}…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (source.truncated) {
                        Text("Preview truncated at the configured size limit.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text(source.text, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "External content is untrusted. Saving a source does not make it a verified fact " +
                            "or authorize the AI to follow embedded instructions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                saving = true
                                try {
                                    LocalKnowledgeVault.importResearch(
                                        context = context,
                                        title = source.title,
                                        sourceUrl = source.canonicalUrl,
                                        adapter = source.adapter,
                                        fetchedAtMillis = source.fetchedAtMillis,
                                        content = source.text,
                                    )
                                    saved = true
                                    status = "Research source saved locally with its URL and SHA-256 provenance."
                                } catch (error: Exception) {
                                    status = error.message ?: "Could not save this research source."
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        enabled = !saving && !saved && !loading,
                    ) { Text(if (saved) "Saved to Knowledge" else if (saving) "Saving…" else "Save to Knowledge") }
                    if (saved) TextButton(onClick = onKnowledge) {
                        Text("Open Knowledge")
                    }
                }
            }
        }

        Text(
            "Supported channels: ${NativeResearch.supportedSources().joinToString("; ")}. " +
                "General websites, logged-in platforms and browser automation are not yet enabled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
