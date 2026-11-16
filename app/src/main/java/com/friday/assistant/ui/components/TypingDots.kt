package com.friday.assistant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.friday.assistant.ui.theme.StarkCyanPrimary
import com.friday.assistant.ui.theme.rememberReducedMotion
import kotlin.math.sin

private const val CYCLE_MS = 750
private const val PHASE_OFFSET_MS = 140

/**
 * Fluid harmonic phase-wave typing indicator for pre-token LLM latency:
 * three 6dp cyan dots with a 750ms cycle and 140ms phase offset per dot,
 * each scaling 0.75 <-> 1.15, translating -2 <-> +2dp and pulsing
 * alpha 0.30 <-> 1.0. Frozen in a clean resting state under reduced motion.
 */
@Composable
fun TypingDots(modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val phase = if (reducedMotion) {
        0.25f
    } else {
        val transition = rememberInfiniteTransition(label = "typingDots")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = CYCLE_MS, easing = LinearEasing)
            ),
            label = "typingDotsPhase"
        )
        p
    }
    val travelPx = with(LocalDensity.current) { 2.dp.toPx() }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        if (reducedMotion) {
                            scaleX = 1f
                            scaleY = 1f
                            alpha = 0.85f
                        } else {
                            val localPhase = (phase - index * (PHASE_OFFSET_MS.toFloat() / CYCLE_MS) + 1f) % 1f
                            val wave = 0.5f + 0.5f * sin(2f * Math.PI.toFloat() * localPhase)
                            val s = lerp(0.75f, 1.15f, wave)
                            scaleX = s
                            scaleY = s
                            translationY = lerp(-2f, 2f, wave) * travelPx / 2f
                            alpha = lerp(0.30f, 1.0f, wave)
                        }
                    }
                    .size(6.dp)
                    .background(StarkCyanPrimary, CircleShape)
            )
        }
    }
}
