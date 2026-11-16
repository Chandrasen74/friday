package com.friday.assistant.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.friday.assistant.ui.theme.StarkCyanPrimary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Precision aerospace targeting reticle drawn onto the receiving [Modifier]:
 * two concentric hairline rings, cardinal crosshair ticks and 12 radial
 * degree ticks. Locks onto its center by scaling 1.28 -> 1.0, rotating
 * -18 -> 0 degrees and fading in. Animated values are providers read in the
 * draw scope (no recomposition); all constants are hoisted into the cache.
 */
fun Modifier.hudReticle(
    progress: () -> Float
): Modifier = this.drawWithCache {
    val width = size.width
    val height = size.height
    val center = Offset(width / 2f, height / 2f)
    val outerRadius = minOf(width, height) / 2f
    val innerRadius = outerRadius * (136f / 164f)
    val ringColor = StarkCyanPrimary.copy(alpha = 0.16f)
    val tickColor = StarkCyanPrimary.copy(alpha = 0.35f)
    val minorTickColor = StarkCyanPrimary.copy(alpha = 0.20f)
    val hairline = 0.8.dp.toPx()
    val tickWidth = 1.dp.toPx()
    val tickLen = 7.dp.toPx()
    val minorLen = 2.dp.toPx()
    val cardinalAngles = FloatArray(4) { it * 90f }
    val minorAngles = FloatArray(12) { it * 30f }
    onDrawBehind {
        val p = progress().coerceIn(0f, 1f)
        if (p <= 0.001f) return@onDrawBehind
        val scale = 1.28f + (1f - 1.28f) * p
        val rotation = -18f + 18f * p
        withTransform({
            rotate(rotation, center)
            scale(scale, scale, center)
        }) {
            drawCircle(
                color = ringColor,
                alpha = p,
                radius = outerRadius,
                center = center,
                style = Stroke(width = hairline)
            )
            drawCircle(
                color = ringColor,
                alpha = p,
                radius = innerRadius,
                center = center,
                style = Stroke(width = hairline)
            )
            for (angleDeg in cardinalAngles) {
                val angle = Math.toRadians(angleDeg.toDouble())
                val dx = cos(angle).toFloat()
                val dy = sin(angle).toFloat()
                drawLine(
                    color = tickColor,
                    alpha = p,
                    start = Offset(
                        center.x + dx * (outerRadius - tickLen / 2f),
                        center.y + dy * (outerRadius - tickLen / 2f)
                    ),
                    end = Offset(
                        center.x + dx * (outerRadius + tickLen / 2f),
                        center.y + dy * (outerRadius + tickLen / 2f)
                    ),
                    strokeWidth = tickWidth
                )
            }
            for (angleDeg in minorAngles) {
                if (angleDeg % 90f == 0f) continue // cardinals already drawn
                val angle = Math.toRadians(angleDeg.toDouble())
                val dx = cos(angle).toFloat()
                val dy = sin(angle).toFloat()
                drawLine(
                    color = minorTickColor,
                    alpha = p,
                    start = Offset(center.x + dx * innerRadius, center.y + dy * innerRadius),
                    end = Offset(center.x + dx * (innerRadius + minorLen), center.y + dy * (innerRadius + minorLen)),
                    strokeWidth = tickWidth
                )
            }
        }
    }
}
