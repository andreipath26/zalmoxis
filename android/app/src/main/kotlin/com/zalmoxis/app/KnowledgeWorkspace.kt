package com.zalmoxis.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

internal data class KnowledgeSource(
    val id: String, val name: String, val mimeType: String, val sizeBytes: Long,
    val sha256: String, val importedAtMillis: Long, val searchable: Boolean,
    val originUrl: String? = null, val originAdapter: String? = null,
    val fetchedAtMillis: Long? = null, val tainted: Boolean = false,
    val draft: Boolean = false, val citedUrls: List<String> = emptyList(),
)
internal data class KnowledgeMatch(val source: KnowledgeSource, val excerpt: String)

internal object LocalKnowledgeVault {
    private const val MAX_INDEX_BYTES = 2L * 1024L * 1024L
    private fun root(context: Context) = File(context.filesDir, "knowledge-vault")
    private fun sources(context: Context) = File(root(context), "sources").apply { mkdirs() }
    private fun metadata(context: Context) = File(root(context), "metadata").apply { mkdirs() }

    fun list(context: Context): List<KnowledgeSource> =
        metadata(context).listFiles { file -> file.extension == "json" }.orEmpty()
            .mapNotNull { file ->
                runCatching {
                    val json = JSONObject(file.readText())
                    val sourceFile = File(sources(context), json.getString("stored_name"))
                    if (!sourceFile.isFile) return@runCatching null
                    KnowledgeSource(
                        json.getString("id"), json.getString("name"),
                        json.optString("mime_type", "application/octet-stream"),
                        json.optLong("size_bytes", sourceFile.length()),
                        json.getString("sha256"), json.optLong("imported_at", 0L),
                        json.optBoolean("searchable", false),
                        json.optString("origin_url").takeIf { it.startsWith("https://") },
                        json.optString("source_adapter").takeIf { it.isNotBlank() },
                        json.optLong("fetched_at").takeIf { it > 0 },
                        json.optBoolean("tainted", false),
                        json.optString("origin_type") == "ai_research_draft",
                        json.optJSONArray("source_urls")?.let { urls ->
                            (0 until urls.length()).map { urls.optString(it) }
                                .filter { it.startsWith("https://github.com/") }
                        }.orEmpty(),
                    )
                }.getOrNull()
            }.sortedByDescending { it.importedAtMillis }

    suspend fun import(context: Context, uri: Uri): KnowledgeSource = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null } ?: "imported-source"
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val id = UUID.randomUUID().toString()
        val extension = name.substringAfterLast('.', "").takeIf { it.length in 1..12 }
            ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
        val stored = "$id.$extension"
        val directory = sources(context)
        val temp = File(directory, "$stored.partial")
        val destination = File(directory, stored)
        try {
            resolver.openInputStream(uri).use { input ->
                val stream = requireNotNull(input) { "Could not read the selected source." }
                temp.outputStream().buffered().use { output -> stream.copyTo(output) }
            }
            require(temp.isFile && temp.length() > 0L) { "The selected source is empty." }
            val digest = MessageDigest.getInstance("SHA-256")
            temp.inputStream().buffered().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val sha = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
            val searchable = isText(name, mime) && temp.length() <= MAX_INDEX_BYTES
            check(temp.renameTo(destination)) { "Could not safely save the selected source." }
            val importedAt = System.currentTimeMillis()
            val receipt = JSONObject()
                .put("id", id).put("name", name).put("mime_type", mime)
                .put("stored_name", stored).put("size_bytes", destination.length())
                .put("sha256", sha).put("imported_at", importedAt).put("searchable", searchable)
            val receiptFile = File(metadata(context), "$id.json")
            val receiptTemp = File(metadata(context), "$id.json.partial")
            receiptTemp.writeText(receipt.toString())
            check(receiptTemp.renameTo(receiptFile)) { "Could not save the source receipt." }
            KnowledgeSource(id, name, mime, destination.length(), sha, importedAt, searchable)
        } catch (error: Throwable) {
            temp.delete()
            destination.delete()
            File(metadata(context), "$id.json.partial").delete()
            throw error
        }
    }

    /** Explicit user-reviewed online research import with immutable source and provenance receipt. */
    suspend fun importResearch(
        context: Context,
        title: String,
        sourceUrl: String,
        adapter: String,
        fetchedAtMillis: Long,
        content: String,
    ): KnowledgeSource = withContext(Dispatchers.IO) {
        require(sourceUrl.startsWith("https://")) { "Research origins must be HTTPS URLs." }
        val bytes = content.toByteArray(Charsets.UTF_8)
        require(bytes.isNotEmpty() && bytes.size <= MAX_INDEX_BYTES) {
            "This research excerpt is empty or exceeds the 2 MB local text index limit."
        }
        val id = UUID.randomUUID().toString()
        val safeTitle = title.take(120).ifBlank { "Research source" }
        val stored = "$id.txt"
        val temp = File(sources(context), "$stored.partial")
        val destination = File(sources(context), stored)
        val receiptFile = File(metadata(context), "$id.json")
        val receiptTemp = File(metadata(context), "$id.json.partial")
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }
        try {
            temp.writeBytes(bytes)
            check(temp.renameTo(destination)) { "Could not save research source." }
            val receipt = JSONObject()
                .put("id", id).put("name", safeTitle).put("mime_type", "text/plain")
                .put("stored_name", stored).put("size_bytes", bytes.size)
                .put("sha256", sha).put("imported_at", System.currentTimeMillis())
                .put("searchable", true).put("origin_type", "explicit_research")
                .put("origin_url", sourceUrl).put("source_adapter", adapter)
                .put("fetched_at", fetchedAtMillis).put("tainted", true)
            receiptTemp.writeText(receipt.toString())
            check(receiptTemp.renameTo(receiptFile)) { "Could not save research provenance." }
            KnowledgeSource(id, safeTitle, "text/plain", bytes.size.toLong(), sha,
                receipt.optLong("imported_at"), true, sourceUrl, adapter,
                fetchedAtMillis, true)
        } catch (error: Throwable) {
            temp.delete()
            destination.delete()
            receiptTemp.delete()
            receiptFile.delete()
            throw error
        }
    }

    /** An editable, explicitly saved LLM synthesis draft, never a verified source or memory fact. */
    suspend fun importResearchDraft(
        context: Context,
        title: String,
        draftText: String,
        sourceUrls: List<String>,
    ): KnowledgeSource = withContext(Dispatchers.IO) {
        require(sourceUrls.isNotEmpty() && sourceUrls.size <= 8) {
            "Drafts require 1–8 public source citations."
        }
        require(sourceUrls.all { it.startsWith("https://github.com/") }) {
            "The current research draft requires canonical GitHub source URLs."
        }
        val content = buildString {
            append("# AI-generated research draft — verify before relying on it\\n")
            append("This is a model-generated interpretation, not a confirmed memory.\\n\\n")
            append(draftText.trim())
            append("\\n\\n## Public source links\\n")
            sourceUrls.distinct().forEachIndexed { index, url ->
                append("[")
                append(index + 1)
                append("] ")
                append(url)
                append("\\n")
            }
        }
        val bytes = content.toByteArray(Charsets.UTF_8)
        require(draftText.isNotBlank() && bytes.size <= MAX_INDEX_BYTES) {
            "Draft is empty or exceeds the local Knowledge text limit."
        }
        val id = UUID.randomUUID().toString()
        val stored = "$id.md"
        val temp = File(sources(context), "$stored.partial")
        val destination = File(sources(context), stored)
        val receiptFile = File(metadata(context), "$id.json")
        val receiptTemp = File(metadata(context), "$id.json.partial")
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }
        try {
            temp.writeBytes(bytes)
            check(temp.renameTo(destination)) { "Could not write the research draft." }
            val now = System.currentTimeMillis()
            val receipt = JSONObject()
                .put("id", id)
                .put("name", "AI draft · " + title.take(100))
                .put("mime_type", "text/markdown")
                .put("stored_name", stored)
                .put("size_bytes", bytes.size)
                .put("sha256", sha)
                .put("imported_at", now)
                .put("searchable", true)
                .put("origin_type", "ai_research_draft")
                .put("source_adapter", "local_inference")
                .put("source_urls", org.json.JSONArray(sourceUrls.distinct()))
                .put("tainted", true)
                .put("review_status", "unverified")
            receiptTemp.writeText(receipt.toString())
            check(receiptTemp.renameTo(receiptFile)) { "Could not write draft provenance." }
            KnowledgeSource(id, receipt.getString("name"), "text/markdown",
                bytes.size.toLong(), sha, now, true,
                originAdapter = "local_inference", tainted = true, draft = true,
                citedUrls = sourceUrls.distinct())
        } catch (error: Throwable) {
            temp.delete()
            destination.delete()
            receiptTemp.delete()
            receiptFile.delete()
            throw error
        }
    }

    suspend fun search(context: Context, query: String): List<KnowledgeMatch> = withContext(Dispatchers.IO) {
        val term = query.trim()
        if (term.length < 2) return@withContext emptyList()
        list(context).asSequence().filter { it.searchable }.mapNotNull { source ->
            val receipt = JSONObject(File(metadata(context), "${source.id}.json").readText())
            val sourceFile = File(sources(context), receipt.getString("stored_name"))
            val text = runCatching { sourceFile.readText(Charsets.UTF_8) }.getOrNull()
                ?: return@mapNotNull null
            val index = text.indexOf(term, ignoreCase = true)
            if (index < 0) null else {
                val start = (index - 90).coerceAtLeast(0)
                val end = (index + term.length + 130).coerceAtMost(text.length)
                KnowledgeMatch(source, text.substring(start, end).replace(Regex("\\s+"), " ").trim())
            }
        }.take(30).toList()
    }

    suspend fun delete(context: Context, source: KnowledgeSource): Boolean = withContext(Dispatchers.IO) {
        val receiptFile = File(metadata(context), "${source.id}.json")
        val receipt = runCatching { JSONObject(receiptFile.readText()) }.getOrNull()
            ?: return@withContext false
        val original = File(sources(context), receipt.optString("stored_name"))
        val sourceRemoved = !original.exists() || original.delete()
        val receiptRemoved = !receiptFile.exists() || receiptFile.delete()
        sourceRemoved && receiptRemoved && !original.exists() && !receiptFile.exists()
    }

    private fun isText(name: String, mime: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return mime.startsWith("text/") || ext in setOf("txt", "md", "markdown", "csv", "json", "xml")
    }
}

@Composable
internal fun KnowledgeWorkspace(busy: Boolean, onBusyChange: (Boolean) -> Unit, onStatus: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf(emptyList<KnowledgeSource>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    var matches by remember { mutableStateOf(emptyList<KnowledgeMatch>()) }
    var pendingDelete by remember { mutableStateOf<KnowledgeSource?>(null) }

    suspend fun refresh() {
        items = withContext(Dispatchers.IO) { LocalKnowledgeVault.list(context) }
        matches = LocalKnowledgeVault.search(context, query)
    }
    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(query, items) {
        delay(250)
        matches = LocalKnowledgeVault.search(context, query)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            onBusyChange(true)
            onStatus("Importing selected sources into private storage…")
            scope.launch {
                try {
                    var count = 0
                    var failures = 0
                    uris.forEach { uri ->
                        runCatching { LocalKnowledgeVault.import(context, uri) }
                            .onSuccess { count += 1 }
                            .onFailure { failures += 1 }
                    }
                    refresh()
                    onStatus(
                        "$count source(s) saved locally; $failures failed. " +
                            "Only supported text files are searchable."
                    )
                } catch (error: Exception) {
                    onStatus(error.message ?: "Could not import selected sources.")
                } finally { onBusyChange(false) }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column {
                Text("Knowledge", style = MaterialTheme.typography.headlineMedium)
                Text("${items.size} private sources", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { picker.launch(arrayOf("*/*")) }, enabled = !busy) { Text("＋ Add") }
        }
        Text(
            "Your private library, searchable on this device. Text and Markdown sources up to 2 MB are indexed.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Search text and Markdown sources") }, enabled = !busy, singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("All", "Documents", "Books", "Notes").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                )
            }
        }
        fun matchesCategory(name: String, mime: String): Boolean {
            val ext = name.substringAfterLast('.', "").lowercase()
            return when (selectedFilter) {
                "Documents" -> ext in setOf("pdf", "doc", "docx", "txt", "csv") ||
                    mime == "application/pdf"
                "Books" -> ext in setOf("epub", "mobi", "azw", "azw3")
                "Notes" -> ext in setOf("md", "markdown", "txt")
                else -> true
            }
        }
        val filteredItems = items.filter { matchesCategory(it.name, it.mimeType) }
        val filteredMatches = matches.filter { matchesCategory(it.source.name, it.source.mimeType) }
        if (query.isNotBlank()) {
            Text("${filteredMatches.size} matching source(s)", style = MaterialTheme.typography.labelMedium)
            filteredMatches.forEach { match ->
                Surface(
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(match.source.name, style = MaterialTheme.typography.titleSmall)
                        Text(match.excerpt, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Source ID ${match.source.id.take(8)} · SHA-256 ${match.source.sha256.take(16)}…",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Text("SAVED SOURCES · ${filteredItems.size}", style = MaterialTheme.typography.labelMedium)
        filteredItems.forEach { source ->
            Surface(
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            when (source.name.substringAfterLast('.', "").lowercase()) {
                                "pdf" -> "▣"
                                "epub" -> "▤"
                                "md", "markdown" -> "✎"
                                else -> "▧"
                            },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(source.name, style = MaterialTheme.typography.titleSmall)
                    }
                    val imported = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.ROOT)
                        .format(java.util.Date(source.importedAtMillis))
                    val extractionState = if (source.searchable) "Text indexed locally" else "Stored locally · text extraction unavailable"
                    Text(
                        "$extractionState · ${source.sizeBytes} bytes · ${source.mimeType}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Imported $imported · SHA-256 ${source.sha256.take(16)}…",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (source.draft) {
                        Text(
                            "UNVERIFIED AI DRAFT · inspect before using as a fact",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        source.citedUrls.forEach { link ->
                            Text(link, color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (source.originUrl != null) {
                        Text(
                            "Online source: ${source.originUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Untrusted external content · ${source.originAdapter ?: "unknown adapter"} · " +
                                "Fetched ${source.fetchedAtMillis?.let { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.ROOT).format(java.util.Date(it)) } ?: "unknown"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { pendingDelete = source }, enabled = !busy) { Text("Delete source") }
                }
            }
        }
        if (filteredItems.isEmpty()) Text(
            if (items.isEmpty()) "No sources yet. Use Add to create your private knowledge library."
            else "No sources in this category.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
    }

    pendingDelete?.let { source ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete source?") },
            text = { Text("This removes the original file and its local receipt from this device.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    pendingDelete = null
                    onBusyChange(true)
                    onStatus("Deleting source and verifying removal…")
                    scope.launch {
                        try {
                            val removed = LocalKnowledgeVault.delete(context, source)
                            refresh()
                            onStatus(if (removed) "Source deleted and removal verified." else "Could not verify source deletion.")
                        } catch (error: Exception) {
                            onStatus(error.message ?: "Could not delete source.")
                        } finally { onBusyChange(false) }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
