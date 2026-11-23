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
import androidx.compose.runtime.LaunchedEffect
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

/**
 * Explicit scope: public GitHub discovery and reading. Search results are untrusted.
 * The LLM does not own the network or persistence interfaces.
 */
@Composable
internal fun ResearchWorkspace(
    onBack: () -> Unit,
    onKnowledge: () -> Unit,
    initialTopic: String = "",
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var topic by rememberSaveable { mutableStateOf(initialTopic) }
    var url by rememberSaveable { mutableStateOf("") }
    var hits by remember { mutableStateOf(emptyList<ResearchSearchHit>()) }
    var current by remember { mutableStateOf<ResearchResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Choose a topic or direct public GitHub source.") }

    suspend fun fetch(sourceUrl: String) {
        loading = true
        current = null
        saved = false
        status = "Fetching public metadata or readable source text from GitHub…"
        try {
            current = NativeResearch.fetch(sourceUrl)
            status = "Source retrieved. Review the evidence and choose what to save."
        } catch (error: Exception) {
            status = error.message ?: "Unable to retrieve this public source."
        } finally {
            loading = false
        }
    }

    suspend fun search(query: String) {
        loading = true
        current = null
        saved = false
        hits = emptyList()
        status = "Searching public GitHub repositories for: $query"
        try {
            hits = NativeResearch.searchRepositories(query)
            status = if (hits.isEmpty()) "No public repositories found for this query."
                     else "${hits.size} public repositories found. Open a result to inspect its source."
        } catch (error: Exception) {
            status = error.message ?: "Search failed. Check your connection or rate limits."
        } finally {
            loading = false
        }
    }

    // A direct conversational instruction to "research X" is permission to search
    // the *specific public GitHub scope* shown here, not to search other websites.
    LaunchedEffect(initialTopic) {
        if (initialTopic.isNotBlank()) {
            topic = initialTopic
            search(initialTopic)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Research", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onBack) { Text("Workspace") }
        }
        Text(
            "Tell Zalmoxis what to investigate. This first version searches public GitHub repositories; " +
                "no links need to be pasted. The request is sent to GitHub, not to a cloud AI model.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = topic,
            onValueChange = {
                topic = it
                hits = emptyList()
                current = null
                status = "Search topic is only sent when you tap Search."
            },
            label = { Text("What should I research?") },
            placeholder = { Text("Offline AI agents for Android") },
            enabled = !loading && !saving,
            minLines = 2,
            maxLines = 3,
        )
        Text(
            "Network scope: api.github.com · public repository search · up to 8 results · no account access",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            enabled = !loading && !saving && topic.trim().length in 3..160,
            onClick = { scope.launch { search(topic.trim()) } },
        ) { Text(if (loading) "Researching…" else "Search for sources") }

        if (hits.isNotEmpty()) {
            Text("Sources found", style = MaterialTheme.typography.titleMedium)
            hits.forEach { hit ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ZalmoxisV2.panel,
                    border = BorderStroke(1.dp, ZalmoxisV2.stroke),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(hit.title, style = MaterialTheme.typography.titleSmall)
                        Text(hit.description, style = MaterialTheme.typography.bodySmall)
                        Text("${hit.stars} stars · Updated ${hit.updatedAt} · ${hit.url}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            enabled = !loading && !saving,
                            onClick = { scope.launch { fetch(hit.url) } },
                        ) { Text("Read source") }
                    }
                }
            }
        }

        Text("Or open a specific source", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = url,
            onValueChange = {
                url = it
                current = null
                saved = false
            },
            label = { Text("Optional GitHub URL") },
            placeholder = { Text("https://github.com/owner/repo") },
            enabled = !loading && !saving,
            maxLines = 3,
        )
        val directHost = runCatching { NativeResearch.hostForConsent(url) }.getOrNull()
        Text("Destination: ${directHost ?: "No source selected"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        if (directHost in setOf("github.com", "raw.githubusercontent.com")) {
            Button(
                enabled = !loading && !saving,
                onClick = { scope.launch { fetch(url.trim()) } },
            ) { Text("Read URL") }
        }

        Text(status, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        current?.let { source ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ZalmoxisV2.panel,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, ZalmoxisV2.stroke),
            ) {
                Column(modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(source.title, style = MaterialTheme.typography.titleMedium)
                    Text(source.canonicalUrl, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Fetched ${DateFormat.getDateTimeInstance().format(Date(source.fetchedAtMillis))} · " +
                            "${source.adapter} · SHA-256 ${source.sha256.take(16)}…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (source.truncated) {
                        Text("Only a bounded excerpt was downloaded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                    Text(source.text, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "External text is untrusted and cannot grant tools or permissions. " +
                            "Saving this source does not convert claims into verified memories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        enabled = !saving && !saved && !loading,
                        onClick = {
                            scope.launch {
                                saving = true
                                try {
                                    LocalKnowledgeVault.importResearch(
                                        context, source.title, source.canonicalUrl, source.adapter,
                                        source.fetchedAtMillis, source.text,
                                    )
                                    saved = true
                                    status = "Source saved in your local Knowledge library with provenance."
                                } catch (error: Exception) {
                                    status = error.message ?: "Knowledge import failed."
                                } finally {
                                    saving = false
                                }
                            }
                        },
                    ) {
                        Text(when {
                            saved -> "Saved to Knowledge"
                            saving -> "Saving…"
                            else -> "Save source to Knowledge"
                        })
                    }
                    if (saved) TextButton(onClick = onKnowledge) { Text("Open Knowledge") }
                }
            }
        }

        Text(
            "Current limitations: repository discovery and metadata, plus specific GitHub raw text files. " +
                "Broad web search, multi-source LLM analysis, and automatic memory changes need additional " +
                "providers, bounded execution, and review controls.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
