package com.friday.assistant.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.friday.assistant.ui.theme.StarkCyanBright
import com.friday.assistant.ui.theme.StarkCyanDeep
import com.friday.assistant.ui.theme.StarkCyanPrimary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dual-track orbital arc-reactor ring drawn onto the receiving [Modifier].
 * Outer primary arc sweeps clockwise with a glowing leading-edge tip dot;
 * inner counter-arc sweeps in segmented dashes. Optional completion shockwave.
 * All animated values are supplied as providers and read inside the draw
 * scope so per-frame updates never recompose the hierarchy. Every brush and
 * dash effect is hoisted into the draw cache; nothing is allocated per frame.
 */
fun Modifier.arcRing(
    sweepDegrees: () -> Float,
    counterSweepDegrees: () -> Float = { sweepDegrees() * 0.75f },
    rotationDegrees: () -> Float = { 0f },
    shockwave: (() -> Float)? = null,
    outerStrokeWidth: Dp = 2.dp,
    innerStrokeWidth: Dp = 1.dp,
    // Inner ring diameter as a ratio of the outer ring diameter.
    innerRadiusRatio: Float = 92f / 108f,
    // Shockwave travel as a ratio of the outer radius (0.574 -> 216dp ring
    // expands to a 340dp diameter before dissolving).
    shockExpansion: Float = 0.81f,
    tipOrbSize: Dp = 3.5.dp
): Modifier = this.drawWithCache {
    val width = size.width
    val height = size.height
    val center = Offset(width / 2f, height / 2f)
    val outerStrokePx = outerStrokeWidth.toPx()
    val outerRadius = minOf(width, height) / 2f - outerStrokePx / 2f - 1f
    val innerRadius = outerRadius * innerRadiusRatio
    val outerBrush = Brush.sweepGradient(
        colors = listOf(StarkCyanDeep, StarkCyanBright, StarkCyanDeep),
        center = center
    )
    val dashPx = (15f / 360f) * (2f * Math.PI.toFloat() * innerRadius)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx), 0f)
    val bloomRadius = outerRadius * 1.9f
    val bloomBrush = Brush.radialGradient(
        colors = listOf(
            StarkCyanPrimary.copy(alpha = 0.32f),
            Color.Transparent
        ),
        center = center,
        radius = bloomRadius
    )
    onDrawBehind {
        val sweep = sweepDegrees().coerceIn(0f, 360f)
        if (sweep > 0.01f) {
            withTransform({
                rotate(rotationDegrees(), center)
            }) {
                drawArc(
                    brush = outerBrush,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2f, outerRadius * 2f),
                    style = Stroke(width = outerStrokePx, cap = StrokeCap.Round)
                )
                if (sweep > 0.1f) {
                    val endAngleRad = Math.toRadians((-90f + sweep).toDouble())
                    val tip = Offset(
                        center.x + outerRadius * cos(endAngleRad).toFloat(),
                        center.y + outerRadius * sin(endAngleRad).toFloat()
                    )
                    drawCircle(
                        color = StarkCyanBright.copy(alpha = 0.35f),
                        radius = tipOrbSize.toPx() * 1.8f,
                        center = tip
                    )
                    drawCircle(color = StarkCyanBright, radius = tipOrbSize.toPx(), center = tip)
                }
                val counter = counterSweepDegrees().coerceIn(0f, 360f)
                if (counter > 0.01f) {
                    drawArc(
                        color = StarkCyanPrimary.copy(alpha = 0.45f),
                        startAngle = -90f,
                        sweepAngle = -counter,
                        useCenter = false,
                        topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                        size = Size(innerRadius * 2f, innerRadius * 2f),
                        style = Stroke(
                            width = innerStrokeWidth.toPx(),
                            cap = StrokeCap.Butt,
                            pathEffect = dashEffect
                        )
                    )
                }
            }
        }
        val shock = shockwave?.invoke()?.coerceIn(0f, 1f) ?: 0f
        if (shock > 0.01f) {
            val ringRadius = outerRadius + (outerRadius * shockExpansion) * shock
            val ringStroke = outerStrokePx * (1f - shock)
            drawCircle(
                color = StarkCyanPrimary,
                alpha = 0.48f * (1f - shock),
                radius = ringRadius,
                center = center,
                style = Stroke(width = ringStroke.coerceAtLeast(0.2f), cap = StrokeCap.Round)
            )
            drawCircle(
                brush = bloomBrush,
                alpha = 1f - shock,
                radius = bloomRadius * (0.95f + 0.55f * shock) / 1.5f,
                center = center
            )
        }
    }
}
