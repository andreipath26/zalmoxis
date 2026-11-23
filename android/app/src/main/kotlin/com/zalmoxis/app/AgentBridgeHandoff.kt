package com.zalmoxis.app

import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.util.UUID

/**
 * No-key Android handoff, NOT a live ChatGPT connection.
 * The user chooses the destination via Sharesheet and explicitly accepts
 * incoming shared text before it is displayed or processed.
 */
internal object AgentBridgeHandoff {
    const val MAX_MESSAGE_LENGTH = 12_000
    const val PROTOCOL = "zalmoxis-agent-bridge/1"

    fun outgoing(text: String, direction: String = "zalmoxis-to-chatgpt"): String {
        require(direction == "zalmoxis-to-chatgpt") { "Unsupported handoff direction." }
        require(text.isNotBlank() && text.length <= MAX_MESSAGE_LENGTH) { "Message must be 1–12,000 characters." }
        val envelope = JSONObject()
            .put("protocol", PROTOCOL)
            .put("id", UUID.randomUUID().toString())
            .put("direction", direction)
            .put("origin", "Zalmoxis Android (user-approved handoff)")
            .put("created_at_ms", System.currentTimeMillis())
            .put("message", text.trim())
        return envelope.toString(2)
    }

    fun share(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "Zalmoxis agent message")
            .putExtra(Intent.EXTRA_TEXT, outgoing(text))
        context.startActivity(
            Intent.createChooser(intent, "Send Zalmoxis message using…")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun extractIncoming(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
        val body = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        return body.takeIf { it.isNotBlank() && it.length <= MAX_MESSAGE_LENGTH }
    }
}
