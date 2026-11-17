package com.friday.assistant.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.friday.assistant.ui.theme.StarkCyanPrimary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Precision aerospace targeting reticle drawn onto the receiving [Modifier]:
 * two concentric hairline rings, cardinal crosshair ticks and [minorTickCount]
 * radial degree ticks. Locks onto its center by scaling [startScale] -> 1.0,
 * rotating [startRotation] -> 0 degrees and fading in. Animated values are
 * providers read in the draw scope (no recomposition); all constants are
 * hoisted into the cache.
 */
fun Modifier.hudReticle(
    progress: () -> Float,
    innerRadiusRatio: Float = 136f / 164f,
    hairlineWidth: Dp = 0.8.dp,
    cardinalTickLength: Dp = 7.dp,
    minorTickCount: Int = 12,
    minorTickLength: Dp = 2.dp,
    startScale: Float = 1.28f,
    startRotation: Float = -18f
): Modifier = this.drawWithCache {
    val width = size.width
    val height = size.height
    val center = Offset(width / 2f, height / 2f)
    val outerRadius = minOf(width, height) / 2f
    val innerRadius = outerRadius * innerRadiusRatio
    val ringColor = StarkCyanPrimary.copy(alpha = 0.16f)
    val tickColor = StarkCyanPrimary.copy(alpha = 0.35f)
    val minorTickColor = StarkCyanPrimary.copy(alpha = 0.20f)
    val hairline = hairlineWidth.toPx()
    val tickWidth = 1.dp.toPx()
    val tickLen = cardinalTickLength.toPx()
    val minorLen = minorTickLength.toPx()
    val cardinalAngles = FloatArray(4) { it * 90f }
    val minorAngles = FloatArray(minorTickCount) { it * (360f / minorTickCount) }
    onDrawBehind {
        val p = progress().coerceIn(0f, 1f)
        if (p <= 0.001f) return@onDrawBehind
        val scale = startScale + (1f - startScale) * p
        val rotation = startRotation + (0f - startRotation) * p
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
