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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
        Text("KNOWLEDGE", style = MaterialTheme.typography.titleMedium)
        Text(
            "Private source shelf. Originals stay unchanged on this device. Search reads text and Markdown up to 2 MB; other files are stored but not parsed.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = { picker.launch(arrayOf("*/*")) }, enabled = !busy) { Text("Import sources") }
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Search text and Markdown sources") }, enabled = !busy, singleLine = true,
        )
        if (query.isNotBlank()) {
            Text("${matches.size} matching source(s)", style = MaterialTheme.typography.labelMedium)
            matches.forEach { match ->
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
        Text("SAVED SOURCES · ${items.size}", style = MaterialTheme.typography.labelMedium)
        items.forEach { source ->
            Surface(
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(source.name, style = MaterialTheme.typography.titleSmall)
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
                    TextButton(onClick = { pendingDelete = source }, enabled = !busy) { Text("Delete source") }
                }
            }
        }
        if (items.isEmpty()) Text("No sources yet. Import text or Markdown files to create a searchable local knowledge shelf.")
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
