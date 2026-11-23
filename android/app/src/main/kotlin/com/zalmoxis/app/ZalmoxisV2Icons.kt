package com.zalmoxis.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Device-independent stroke icons. Drawn in a 24x24 coordinate system. */
@Composable
internal fun V2Icon(name: String, modifier: Modifier = Modifier, tint: Color = ZalmoxisV2.accent) {
    Canvas(modifier) {
        val u = size.minDimension / 24f
        val x0 = (size.width - 24f * u) / 2f
        val y0 = (size.height - 24f * u) / 2f
        fun point(x: Float, y: Float) = Offset(x0 + x * u, y0 + y * u)
        fun line(a: Float, b: Float, c: Float, d: Float) =
            drawLine(tint, point(a, b), point(c, d), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
        fun circle(x: Float, y: Float, r: Float) =
            drawCircle(tint, r * u, point(x, y), style = Stroke(1.7f * u))
        fun rect(x: Float, y: Float, w: Float, h: Float) =
            drawRect(tint, topLeft = point(x, y), size = Size(w * u, h * u), style = Stroke(1.7f * u))
        when (name) {
            "Chat" -> {
                circle(12f, 10.5f, 7.5f)
                val tail = Path().apply {
                    moveTo(point(6.5f, 15.7f).x, point(6.5f, 15.7f).y)
                    lineTo(point(5f, 21f).x, point(5f, 21f).y)
                    lineTo(point(10.3f, 17.8f).x, point(10.3f, 17.8f).y)
                }
                drawPath(tail, tint, style = Stroke(1.7f * u))
            }
            "Knowledge" -> {
                val left = Path().apply {
                    moveTo(point(12f, 6f).x, point(12f, 6f).y)
                    quadraticTo(point(8f, 3f).x, point(8f, 3f).y, point(3f, 5f).x, point(3f, 5f).y)
                    lineTo(point(3f, 18f).x, point(3f, 18f).y)
                    quadraticTo(point(8f, 16f).x, point(8f, 16f).y, point(12f, 19f).x, point(12f, 19f).y)
                }
                val right = Path().apply {
                    moveTo(point(12f, 6f).x, point(12f, 6f).y)
                    quadraticTo(point(16f, 3f).x, point(16f, 3f).y, point(21f, 5f).x, point(21f, 5f).y)
                    lineTo(point(21f, 18f).x, point(21f, 18f).y)
                    quadraticTo(point(16f, 16f).x, point(16f, 16f).y, point(12f, 19f).x, point(12f, 19f).y)
                }
                drawPath(left, tint, style = Stroke(1.7f * u))
                drawPath(right, tint, style = Stroke(1.7f * u))
                line(12f, 6f, 12f, 19f)
            }
            "Models" -> {
                val hex = Path()
                for (i in 0..6) {
                    val angle = (i % 6) * PI / 3 - PI / 2
                    val p = point(12f + (9f * cos(angle).toFloat()), 12f + (9f * sin(angle).toFloat()))
                    if (i == 0) hex.moveTo(p.x, p.y) else hex.lineTo(p.x, p.y)
                }
                drawPath(hex, tint, style = Stroke(1.7f * u))
                circle(12f, 12f, 3.1f)
            }
            "Workspace" -> {
                listOf(4f to 4f, 14f to 4f, 4f to 14f, 14f to 14f).forEach { (a,b) ->
                    rect(a, b, 6f, 6f)
                }
            }
            "Microphone" -> {
                drawRoundRect(
                    tint,
                    topLeft = point(9f, 3f),
                    size = Size(6f * u, 12f * u),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * u),
                    style = Stroke(1.8f * u),
                )
                val cup = Path().apply {
                    moveTo(point(6f, 11f).x, point(6f, 11f).y)
                    cubicTo(point(6f, 22f).x, point(6f, 22f).y, point(18f, 22f).x, point(18f, 22f).y, point(18f, 11f).x, point(18f, 11f).y)
                }
                drawPath(cup, tint, style = Stroke(1.7f * u))
                line(12f, 19f, 12f, 22f)
                line(8.5f, 22f, 15.5f, 22f)
            }
            "Attach" -> {
                line(12f, 4f, 12f, 20f)
                line(4f, 12f, 20f, 12f)
            }
            "Send" -> {
                val arrow = Path().apply {
                    moveTo(point(4f, 12f).x, point(4f, 12f).y)
                    lineTo(point(20f, 12f).x, point(20f, 12f).y)
                    moveTo(point(14f, 6f).x, point(14f, 6f).y)
                    lineTo(point(20f, 12f).x, point(20f, 12f).y)
                    lineTo(point(14f, 18f).x, point(14f, 18f).y)
                }
                drawPath(arrow, tint, style = Stroke(2f * u, cap = StrokeCap.Round))
            }
            "Stop" -> {
                drawRoundRect(tint, topLeft = point(6f, 6f), size = Size(12f*u, 12f*u),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f*u))
            }
        }
    }
}
