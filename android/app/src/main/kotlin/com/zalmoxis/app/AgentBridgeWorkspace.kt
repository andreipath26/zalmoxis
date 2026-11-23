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
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Honest no-key fallback: Android shares messages, but ChatGPT is not connected in-app. */
@Composable
internal fun AgentBridgeWorkspace(onBack: () -> Unit) {
    val context = LocalContext.current
    var draft by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf("Not connected to ChatGPT. Use the Android share sheet to hand off a message.") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Agent Bridge", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onBack) { Text("Workspace") }
        }
        Surface(
            color = ZalmoxisV2.panel,
            border = BorderStroke(1.dp, ZalmoxisV2.stroke),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("User-mediated connection", style = MaterialTheme.typography.titleMedium)
                Text(
                    "No OpenAI API key needed. Android lets you choose ChatGPT or another app as a share target. " +
                        "A share does not deliver a message directly to an assistant, generate a response, or sync automatically.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Remote live MCP connection: not connected. Two-way automatic delivery requires a secure " +
                        "relay, compatible ChatGPT connector access, and device pairing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(AgentBridgeHandoff.MAX_MESSAGE_LENGTH) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Message to send to ChatGPT") },
            minLines = 4,
            maxLines = 10,
        )
        Button(
            onClick = {
                try {
                    AgentBridgeHandoff.share(context, draft)
                    status = "Android share sheet opened. Choose a target and review before sending."
                } catch (error: Exception) {
                    status = error.message ?: "Could not open Android share sheet."
                }
            },
            enabled = draft.isNotBlank(),
        ) { Text("Share message…") }
        Text(status, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "To send a reply back, use Share → Zalmoxis on your phone and review the incoming text. " +
                "Nothing is imported into Knowledge or executed as a tool without a separate approval.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
