package com.zalmoxis.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zalmoxis.app.voice.DuplexVoiceSession
import com.zalmoxis.app.voice.VoiceTurnGate
import kotlin.math.cos
import kotlin.math.sin

/** Zalmoxis V2 — common visual language matching the approved Obsidian/Lime concept. */
internal object ZalmoxisV2 {
    val accent = Color(0xFFC2FA59)
    val background = Color(0xFF0B0E0D)
    val panel = Color(0xFF191E1C)
    val stroke = Color(0xFF34403A)
    val muted = Color(0xFFA8B4AE)
}

@Composable
internal fun V2Header(onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ZalmoxisMark(Modifier.size(28.dp))
        Text("ZALMOXIS", style = MaterialTheme.typography.titleMedium, letterSpacing = 2.8.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text("● LOCAL", color = ZalmoxisV2.accent, style = MaterialTheme.typography.labelSmall)
        Text(
            "⚙",
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable(onClickLabel = "Open settings", onClick = onSettings).padding(7.dp),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/** Vector-drawn symbol to keep the brand asset sharp at any screen density. */
@Composable
internal fun ZalmoxisMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension * 0.33f
        for (arm in 0..2) {
            val angle = (arm * 120.0 - 90.0) * Math.PI / 180.0
            val start = Offset(
                cx + radius * 0.30f * cos(angle).toFloat(),
                cy + radius * 0.30f * sin(angle).toFloat(),
            )
            val outer = Offset(
                cx + radius * cos(angle).toFloat(),
                cy + radius * sin(angle).toFloat(),
            )
            val left = Offset(
                cx + radius * 0.73f * cos(angle + 0.95).toFloat(),
                cy + radius * 0.73f * sin(angle + 0.95).toFloat(),
            )
            val right = Offset(
                cx + radius * 0.73f * cos(angle - 0.95).toFloat(),
                cy + radius * 0.73f * sin(angle - 0.95).toFloat(),
            )
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(left.x, left.y, outer.x, outer.y)
                quadraticTo(right.x, right.y, start.x, start.y)
            }
            drawPath(path, color = ZalmoxisV2.accent, style = Stroke(width = size.minDimension * .065f))
        }
    }
}

@Composable
internal fun V2Navigation(
    selected: String,
    onNavigate: (String) -> Unit,
) {
    val items = listOf(
        Triple("Chat", "Chat", "CHAT"),
        Triple("Workspace", "Workspace", "WORKSPACE"),
        Triple("Knowledge", "Knowledge", "KNOWLEDGE"),
        Triple("Models", "Models", "DOWNLOADS"),
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ZalmoxisV2.background,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { (title, glyph, key) ->
                val active = selected == key
                Column(
                    modifier = Modifier.weight(1f).clickable(onClickLabel = "Open $title") { onNavigate(key) }.padding(vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .background(if (active) Color(0xFF25341B) else Color.Transparent, RoundedCornerShape(30.dp))
                            .padding(horizontal = 15.dp, vertical = 4.dp),
                    ) {
                        V2Icon(glyph, modifier = Modifier.size(23.dp), tint = if (active) ZalmoxisV2.accent else ZalmoxisV2.muted)
                    }
                    Text(
                        title,
                        color = if (active) ZalmoxisV2.accent else ZalmoxisV2.muted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
internal fun V2QuickAction(label: String, glyph: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = ZalmoxisV2.panel,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ZalmoxisV2.stroke),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(glyph, color = ZalmoxisV2.accent, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun V2Welcome(onKnowledge: () -> Unit, onModels: () -> Unit, onVoice: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().height(78.dp)) {
                val center = size.height / 2
                for (i in 0..2) {
                    val path = androidx.compose.ui.graphics.Path()
                    val amplitude = size.height * (0.12f + i * 0.07f)
                    for (x in 0..size.width.toInt() step 4) {
                        val f = x / size.width
                        val envelope = sin(Math.PI * f).toFloat()
                        val y = center + amplitude * envelope * sin(2.0 * Math.PI * (f.toDouble() * 1.65 + i * .13)).toFloat()
                        if (x == 0) path.moveTo(x.toFloat(), y) else path.lineTo(x.toFloat(), y)
                    }
                    drawPath(path, ZalmoxisV2.accent.copy(alpha = .7f - i * .16f), style = Stroke(1.5.dp.toPx()))
                }
            }
        }
        Text("Your AI, your device.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text("Private intelligence that works for you. Ask, search, or speak naturally.", color = ZalmoxisV2.muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        V2QuickAction("Search my knowledge", "⌕", onKnowledge)
        V2QuickAction("Manage on-device models", "⬡", onModels)
        V2QuickAction("Talk with Zalmoxis", "◉", onVoice)
    }
}

@Composable
internal fun V2DuplexVoicePanel(
    phase: VoiceTurnGate.Phase,
    feedback: String,
    transcript: String,
    answer: String,
    turns: Int,
    echoCancellation: Boolean,
    mode: DuplexVoiceSession.Voice,
    onMode: (DuplexVoiceSession.Voice) -> Unit,
    onToggle: () -> Unit,
    onInterrupt: () -> Unit,
    onBack: () -> Unit,
) {
    val active = phase != VoiceTurnGate.Phase.OFF && phase != VoiceTurnGate.Phase.ERROR
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Live Voice", style = MaterialTheme.typography.titleLarge)
        Text("Private · On-device · Hands-free", color = ZalmoxisV2.muted)
        Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(210.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(ZalmoxisV2.accent.copy(alpha = .20f), Color.Transparent)),
                    radius = size.minDimension * .50f,
                )
                drawCircle(
                    color = ZalmoxisV2.accent.copy(alpha = if (active) .95f else .42f),
                    radius = size.minDimension * .35f,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
            ZalmoxisMark(Modifier.size(85.dp))
        }
        Text(
            when (phase) {
                VoiceTurnGate.Phase.OFF -> "Ready to talk"
                VoiceTurnGate.Phase.LISTENING -> "Listening…"
                VoiceTurnGate.Phase.ENDPOINTING -> "Understanding…"
                VoiceTurnGate.Phase.THINKING -> "Thinking…"
                VoiceTurnGate.Phase.SPEAKING -> "Speaking…"
                VoiceTurnGate.Phase.INTERRUPTED -> "Interrupted"
                VoiceTurnGate.Phase.ERROR -> "Voice needs attention"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(feedback, style = MaterialTheme.typography.bodyMedium,
            color = ZalmoxisV2.muted, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                enabled = !active,
                onClick = { onMode(DuplexVoiceSession.Voice.COMPACT) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mode == DuplexVoiceSession.Voice.COMPACT) ZalmoxisV2.accent else ZalmoxisV2.panel,
                    contentColor = if (mode == DuplexVoiceSession.Voice.COMPACT) ZalmoxisV2.background else Color.White,
                ),
            ) { Text("Compact") }
            Button(
                enabled = !active,
                onClick = { onMode(DuplexVoiceSession.Voice.QWEN) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mode == DuplexVoiceSession.Voice.QWEN) ZalmoxisV2.accent else ZalmoxisV2.panel,
                    contentColor = if (mode == DuplexVoiceSession.Voice.QWEN) ZalmoxisV2.background else Color.White,
                ),
            ) { Text("Qwen3") }
        }
        Text(
            if (mode == DuplexVoiceSession.Voice.COMPACT)
                "Small English voice included in the APK. No separate model download."
            else "Qwen3-TTS is optional and requires a complete verified local model pack.",
            style = MaterialTheme.typography.bodySmall, color = ZalmoxisV2.muted,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = ZalmoxisV2.accent,
                contentColor = ZalmoxisV2.background,
            ),
        ) { Text(if (active) "■ End conversation" else "● Start live conversation") }
        if (active) {
            TextButton(onClick = onInterrupt) { Text("Interrupt now") }
            Text(
                if (echoCancellation) "Echo control active · hands-free interruptions available"
                else "Hardware echo control unavailable · use Interrupt to change subject",
                style = MaterialTheme.typography.bodySmall,
                color = ZalmoxisV2.muted,
                textAlign = TextAlign.Center,
            )
        }
        if (transcript.isNotBlank()) {
            Text("You", style = MaterialTheme.typography.titleSmall)
            Text(transcript, style = MaterialTheme.typography.bodyLarge)
        }
        if (answer.isNotBlank()) {
            Text("Zalmoxis", style = MaterialTheme.typography.titleSmall)
            Text(answer, style = MaterialTheme.typography.bodyLarge)
        }
        Text("Completed turns: $turns", color = ZalmoxisV2.muted,
            style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onBack) { Text("Return to chat") }
    }
}

@Composable
internal fun V2Workspace(onKnowledge: () -> Unit, onModels: () -> Unit, onResearch: () -> Unit, onAgentBridge: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Workspace", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Your personal AI toolkit. Capabilities unlock as they are implemented and verified.",
            color = ZalmoxisV2.muted,
            style = MaterialTheme.typography.bodyMedium,
        )
        val tiles = listOf(
            Triple("▤", "Summarize documents", false),
            Triple("◫", "Analyze information", false),
            Triple("⌕", "Public-source research", true),
            Triple("✎", "Writing assistant", false),
            Triple("▦", "Plan & organize", false),
            Triple("◇", "Custom workflows", false),
        )
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (glyph, label, available) ->
                    Surface(
                        onClick = if (available) onResearch else ({ }),
                        enabled = available,
                        modifier = Modifier.weight(1f).height(122.dp),
                        color = ZalmoxisV2.panel,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ZalmoxisV2.stroke),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(glyph, color = ZalmoxisV2.accent, style = MaterialTheme.typography.headlineSmall)
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (available) "Available" else "Coming later",
                                color = ZalmoxisV2.muted,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
        Text("Available now", style = MaterialTheme.typography.titleMedium)
        V2QuickAction("Knowledge library", "▤", onKnowledge)
        V2QuickAction("Manage local AI models", "⬡", onModels)
        V2QuickAction("Research public GitHub sources", "⌕", onResearch)
        V2QuickAction("Agent Bridge · keyless handoff", "◇", onAgentBridge)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = ZalmoxisV2.panel,
            border = BorderStroke(1.dp, ZalmoxisV2.stroke),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Capabilities", style = MaterialTheme.typography.titleMedium)
                listOf(
                    "✓  Local AI model inference",
                    "✓  Local text knowledge search",
                    "✓  On-device speech, if installed",
                    "○  Policy-gated agent actions — planned",
                ).forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall, color = ZalmoxisV2.muted)
                }
            }
        }
    }
}
