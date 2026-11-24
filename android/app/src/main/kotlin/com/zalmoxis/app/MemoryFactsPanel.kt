package com.zalmoxis.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Explicit-user memory, not LLM-inferred memory.
 * Uses a fixed owner/personal scope until identity onboarding is implemented.
 */
@Composable
internal fun MemoryFactsPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { LocalMemoryCore(context) }
    var facts by remember { mutableStateOf(emptyList<MemoryFact>()) }
    var draft by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<MemoryFact?>(null) }
    var editText by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<MemoryFact?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    suspend fun refresh() {
        facts = store.listFacts(MemoryScopePolicy.PERSONAL_USER, MemoryScopePolicy.PERSONAL_CLIENT)
    }
    LaunchedEffect(store) { refresh() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("My confirmed memories", style = MaterialTheme.typography.titleMedium)
        Text(
            "Store facts you explicitly choose to remember. These survive app restarts. " +
                "Imported documents and AI drafts do not become confirmed memories automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(4000) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("A fact to remember") },
            minLines = 2,
            maxLines = 4,
            enabled = !busy,
        )
        Button(enabled = !busy && draft.isNotBlank(), onClick = {
            scope.launch {
                busy = true
                try {
                    store.saveUserFact(MemoryScopePolicy.PERSONAL_USER,
                        MemoryScopePolicy.PERSONAL_CLIENT, draft)
                    draft = ""
                    refresh()
                    notice = "Confirmed memory saved locally."
                } catch (error: Exception) {
                    notice = error.message ?: "Memory could not be saved."
                } finally { busy = false }
            }
        }) { Text("Save confirmed memory") }

        if (notice.isNotBlank()) {
            Text(notice, style = MaterialTheme.typography.bodySmall)
        }
        Text("${facts.size} recent confirmed memories", style = MaterialTheme.typography.labelMedium)
        facts.forEach { fact ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(fact.text, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Explicit user entry · revision ${fact.revision} · ${fact.id.take(8)}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled = !busy, onClick = {
                            editing = fact
                            editText = fact.text
                        }) { Text("Correct") }
                        TextButton(enabled = !busy, onClick = {
                            deleting = fact
                        }) { Text("Delete") }
                    }
                }
            }
        }
    }

    editing?.let { fact ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Correct confirmed memory") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it.take(4000) },
                    label = { Text("Corrected fact") },
                )
            },
            confirmButton = {
                TextButton(enabled = !busy && editText.isNotBlank(), onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val changed = store.correctUserFact(
                                MemoryScopePolicy.PERSONAL_USER, MemoryScopePolicy.PERSONAL_CLIENT,
                                fact.id, fact.revision, editText,
                            )
                            notice = if (changed) "Memory corrected." else
                                "Memory changed elsewhere; reload and try again."
                            refresh()
                            editing = null
                        } catch (error: Exception) {
                            notice = error.message ?: "Correction failed."
                        } finally { busy = false }
                    }
                }) { Text("Save correction") }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text("Cancel") }
            },
        )
    }

    deleting?.let { fact ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete confirmed memory?") },
            text = { Text("Remove this fact from Zalmoxis's local memory database. " +
                "This does not erase independently retained device or system backups.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val removed = store.deleteFact(
                                MemoryScopePolicy.PERSONAL_USER, MemoryScopePolicy.PERSONAL_CLIENT, fact.id,
                            )
                            notice = if (removed) "Memory deleted and database absence checked."
                                else "Memory no longer exists in this scope."
                            refresh()
                            deleting = null
                        } catch (error: Exception) {
                            notice = error.message ?: "Deletion failed."
                        } finally { busy = false }
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel") }
            },
        )
    }
}
