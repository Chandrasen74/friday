package com.friday.assistant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.friday.assistant.ui.theme.FridayMotion
import com.friday.assistant.ui.theme.StarkCyanPrimary

/**
 * Kinetic stream plasma glow for assistant bubbles that are actively
 * generating tokens.
 *
 * Drawn on an UNCLIPPED wrapper around the bubble (the bubble's own
 * `.clip(bubbleShape)` would otherwise cut the outer halo away): content is
 * drawn first, then a crisp illuminated hairline border, a soft outer halo,
 * and a vertical energy sweep travelling along the left spine.
 *
 * - [isStreaming] drives an `activeWeight` (0f idle -> 1f streaming) with a
 *   300ms emphasized tween; below 0.01f nothing is drawn at all, so idle and
 *   historical bubbles carry zero custom-draw overhead.
 * - While streaming with motion enabled, a 1400ms harmonic pulse oscillates
 *   the hairline alpha (0.22 <-> 0.78), modulates the halo, and slides the
 *   spine sweep top-to-bottom.
 * - With reduced motion, a static steady border stands in for the pulse.
 * - On completion the weight dissolves back to 0 over 300ms, landing the
 *   bubble on its resting specular hairline just as the telemetry footer
 *   reveals.
 */
@Composable
fun Modifier.plasmaStreamGlow(
    isStreaming: Boolean,
    cornerShape: Shape,
    reducedMotion: Boolean
): Modifier {
    val activeState: State<Float> = animateFloatAsState(
        targetValue = if (isStreaming) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FridayMotion.Emphasized),
        label = "plasmaActiveWeight"
    )
    // Only compose the infinite pulse while it is actually needed; idle and
    // reduced-motion bubbles never pay for a running transition.
    val pulseState: State<Float>? = if (isStreaming && !reducedMotion) {
        val pulse = rememberInfiniteTransition(label = "plasmaPulse")
        pulse.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = LinearEasing)
            ),
            label = "plasmaPulsePhase"
        )
    } else {
        null
    }

    return this.drawWithCache {
        val outline = cornerShape.createOutline(size, layoutDirection, this)
        val haloSize = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx())
        val haloOutline = cornerShape.createOutline(haloSize, layoutDirection, this)
        onDrawWithContent {
            drawContent()
            val active = activeState.value
            if (active <= 0.01f) return@onDrawWithContent
            if (reducedMotion) {
                // Accessibility fallback: steady border, no oscillation.
                drawOutline(
                    outline,
                    StarkCyanPrimary.copy(alpha = 0.55f * active),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                return@onDrawWithContent
            }
            val pulse = pulseState?.value ?: 0f
            // Crisp illuminated hairline riding the bubble edge.
            drawOutline(
                outline,
                StarkCyanPrimary.copy(alpha = lerp(0.22f, 0.78f, pulse) * active),
                style = Stroke(width = 1.4.dp.toPx())
            )
            // Soft outer halo: a 4dp stroke on an outline outset by 2dp and
            // drawn back 2dp so it blooms just outside the bubble edge.
            translate(-2.dp.toPx(), -2.dp.toPx()) {
                drawOutline(
                    haloOutline,
                    StarkCyanPrimary.copy(alpha = 0.14f * pulse * active),
                    style = Stroke(width = 4.dp.toPx())
                )
            }
            // Vertical energy sweep along the left spine.
            val sweepHeight = size.height * 0.42f
            val sweepTop = (size.height - sweepHeight) * pulse
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        StarkCyanPrimary.copy(alpha = 0.40f * active),
                        Color.Transparent
                    ),
                    startY = sweepTop,
                    endY = sweepTop + sweepHeight
                ),
                topLeft = Offset(0f, sweepTop),
                size = Size(3.dp.toPx(), sweepHeight)
            )
        }
    }
}
