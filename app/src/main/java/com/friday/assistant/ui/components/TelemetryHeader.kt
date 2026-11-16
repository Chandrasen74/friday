package com.friday.assistant.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.friday.assistant.R
import com.friday.assistant.data.ModelStatus
import com.friday.assistant.ui.theme.CyanBorderGlow
import com.friday.assistant.ui.theme.FridayDisplayFont
import com.friday.assistant.ui.theme.FridayMotion
import com.friday.assistant.ui.theme.ObsidianSurfaceElevated
import com.friday.assistant.ui.theme.ObsidianSurfaceGlass
import com.friday.assistant.ui.theme.SpecularHighlightSubtle
import com.friday.assistant.ui.theme.StarkCyanPrimary
import com.friday.assistant.ui.theme.TelemetryAmber
import com.friday.assistant.ui.theme.TelemetryCrimson
import com.friday.assistant.ui.theme.TelemetryEmerald
import com.friday.assistant.ui.theme.TextPrimary
import com.friday.assistant.ui.theme.TextSecondary
import com.friday.assistant.ui.theme.TextTertiary
import com.friday.assistant.ui.theme.rememberReducedMotion
import kotlin.math.roundToInt
import androidx.compose.ui.util.lerp

/**
 * Elevated glass telemetry header: translucent obsidian bar with a bottom
 * specular hairline, the 28dp brand logo (whose root-space center is reported
 * through [onLogoPositioned] so the cold-boot emblem can dock into it), the
 * Orbitron wordmark, live telemetry lines and an animated status pill with a
 * pulsing optical indicator while loading or generating.
 */
@Composable
fun TelemetryHeader(
    status: ModelStatus,
    isGenerating: Boolean,
    subtitle: String,
    metricsLine: String,
    onMenu: () -> Unit,
    onNew: () -> Unit,
    onRegenerate: (() -> Unit)?,
    regenerateEnabled: Boolean,
    onLogoPositioned: (IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotion()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(ObsidianSurfaceGlass)
            .drawWithCache {
                val hairline = 1.dp.toPx()
                val hairlineBrush = Brush.verticalGradient(
                    colors = listOf(SpecularHighlightSubtle, Color(0x08FFFFFF)),
                    startY = size.height - hairline * 6f,
                    endY = size.height
                )
                onDrawBehind {
                    // Bottom specular hairline
                    drawRect(
                        brush = hairlineBrush,
                        topLeft = Offset(0f, size.height - hairline),
                        size = androidx.compose.ui.geometry.Size(size.width, hairline)
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenu) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
            }
            Image(
                painter = painterResource(id = R.drawable.friday_logo_blue),
                contentDescription = "F.R.I.D.A.Y logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(28.dp)
                    .onGloballyPositioned { coords ->
                        val center = coords.boundsInRoot().center
                        onLogoPositioned(IntOffset(center.x.roundToInt(), center.y.roundToInt()))
                    }
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "F.R.I.D.A.Y",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontFamily = FridayDisplayFont),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = metricsLine,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(
                status = status,
                isGenerating = isGenerating,
                reducedMotion = reducedMotion
            )
            if (onRegenerate != null) {
                IconButton(onClick = onRegenerate, enabled = regenerateEnabled) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        tint = TextTertiary
                    )
                }
            }
            IconButton(onClick = onNew) {
                Icon(Icons.Default.Add, contentDescription = "New chat", tint = TextTertiary)
            }
        }
    }
}

@Composable
private fun StatusPill(
    status: ModelStatus,
    isGenerating: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val dotColor = when {
        status.isLoading -> TelemetryAmber
        isGenerating -> StarkCyanPrimary
        status.lastError != null -> TelemetryCrimson
        status.isReady -> TelemetryEmerald
        else -> TextTertiary
    }
    val pulsing = (status.isLoading || isGenerating) && !reducedMotion
    val borderColor by animateColorAsState(
        targetValue = if (status.isLoading || isGenerating) CyanBorderGlow else SpecularHighlightSubtle,
        animationSpec = tween(FridayMotion.Short),
        label = "statusPillBorder"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(ObsidianSurfaceElevated)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = "Model status: ${status.statusText}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (pulsing) {
                val transition = rememberInfiniteTransition(label = "statusPulse")
                val pulse by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1400, easing = LinearEasing)
                    ),
                    label = "statusPulsePhase"
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer {
                            scaleX = lerp(1f, 3.2f, pulse)
                            scaleY = lerp(1f, 3.2f, pulse)
                            alpha = (1f - pulse) * 0.5f
                        }
                        .border(1.dp, dotColor, CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = status.statusText,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
