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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.friday.assistant.ui.theme.StarkCyanPrimary
import com.friday.assistant.ui.theme.rememberReducedMotion
import kotlin.math.sin

/**
 * Harmonic equalizer bars for TTS speaking / voice input states.
 * Bars oscillate at staggered frequencies; the bottom edge is the anchor so
 * bars grow upward. Frozen at a calm mid amplitude under reduced motion or
 * when inactive.
 */
@Composable
fun VoiceWaveform(
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    barWidth: Dp = 2.dp,
    maxHeight: Dp = 14.dp,
    color: Color = StarkCyanPrimary
) {
    val reducedMotion = rememberReducedMotion()
    val phase = if (reducedMotion) {
        0.25f
    } else {
        val transition = rememberInfiniteTransition(label = "voiceWaveform")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing)
            ),
            label = "voiceWaveformPhase"
        )
        p
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(barCount) { index ->
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val freq = 0.8f + index * 0.13f
                        val wave = 0.5f + 0.5f * sin(
                            2f * Math.PI.toFloat() * (phase * freq + index * 0.9f)
                        )
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        scaleY = if (reducedMotion) 0.55f else lerp(0.28f, 1f, wave)
                    }
                    .size(width = barWidth, height = maxHeight)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}
