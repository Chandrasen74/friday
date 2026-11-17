package com.friday.assistant.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.R
import com.friday.assistant.ai.LiteRtLmService
import com.friday.assistant.ui.components.arcRing
import com.friday.assistant.ui.components.hudReticle
import com.friday.assistant.ui.theme.FridayDisplayFont
import com.friday.assistant.ui.theme.ObsidianSurface
import com.friday.assistant.ui.theme.ObsidianSurfaceElevated
import com.friday.assistant.ui.theme.ObsidianVoid
import com.friday.assistant.ui.theme.SpecularHighlightSubtle
import com.friday.assistant.ui.theme.StarkCyanBright
import com.friday.assistant.ui.theme.StarkCyanDeep
import com.friday.assistant.ui.theme.StarkCyanPrimary
import com.friday.assistant.ui.theme.TextPrimary
import com.friday.assistant.ui.theme.TextSecondary
import com.friday.assistant.ui.theme.TextTertiary
import com.friday.assistant.ui.theme.rememberReducedMotion
import kotlin.math.roundToInt

/**
 * First-launch bundled-model preparation screen: the same centered 72dp
 * emblem / reticle / orbital arc core as the cold-boot intro, above a custom
 * glowing cyan telemetry track with a tabular percentage readout.
 * Preserves the original keep-screen-on and moveTaskToBack back behavior.
 */
@Composable
fun PreparingScreen(progress: LiteRtLmService.BundledCopyProgress) {
    val view = LocalView.current
    val activity = LocalContext.current as? Activity
    DisposableEffect(view) {
        view.keepScreenOn = true
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            view.keepScreenOn = false
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    BackHandler {
        activity?.moveTaskToBack(true)
    }

    val reducedMotion = rememberReducedMotion()
    val fraction = LiteRtLmService.copyProgressFraction(progress.bytesCopied, progress.bytesTotal)
    val percent = ((fraction ?: 0f).coerceIn(0f, 1f) * 100f).roundToInt()

    // Gentle orbital rotation (360 deg / 3600ms); frozen when reduced motion.
    val orbitalRotation = if (reducedMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "orbitalRotation")
        val r by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orbitalRotationAngle"
        )
        r
    }
    // Indeterminate sweep phase for unknown-size copies; frozen when reduced motion.
    val indeterminatePhase: () -> Float = if (reducedMotion) {
        ({ 0f })
    } else {
        val transition = rememberInfiniteTransition(label = "indeterminateSweep")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "indeterminateSweepPhase"
        )
        ({ phase })
    }

    val detail = buildString {
        append("Model ${progress.fileIndex} of ${progress.fileCount}: ${progress.fileName}")
        append('\n')
        append(LiteRtLmService.formatCopiedBytes(progress.bytesCopied, progress.bytesTotal))
    }
    val progressDescription = if (fraction != null) {
        "Copy progress $percent percent for ${progress.fileName}"
    } else {
        "Copying ${progress.fileName}, ${LiteRtLmService.formatCopiedBytes(progress.bytesCopied, null)}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Orbital core: reticle at 164dp, emblem container locked at 108dp (72dp icon)
        Box(modifier = Modifier.size(164.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hudReticle(progress = { 1f })
            )
            Box(modifier = Modifier.size(108.dp)) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .arcRing(
                            sweepDegrees = { 300f },
                            counterSweepDegrees = { 120f },
                            rotationDegrees = { orbitalRotation }
                        )
                )
                Image(
                    painter = painterResource(id = R.drawable.friday_logo_blue),
                    contentDescription = "F.R.I.D.A.Y logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = "Preparing F.R.I.D.A.Y",
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FridayDisplayFont),
            color = TextPrimary
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Copying bundled models for first launch. This can take several minutes and only happens once.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        TelemetryProgressTrack(
            fractionProvider = { fraction },
            indeterminatePhase = indeterminatePhase,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = progressDescription }
        )

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = percent.toString().padStart(3, '0') + "%",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 1.5.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = TextPrimary
            )
            Text(
                text = LiteRtLmService.formatCopiedBytes(progress.bytesCopied, progress.bytesTotal),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Keep the app open while setup finishes. If setup is interrupted, it will resume on the next launch.",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Glowing cyan telemetry progress track. The draw cache holds every brush;
 * per-frame values (fraction / indeterminate phase) are provider-read.
 * fraction == null renders an indeterminate sweeping segment.
 */
@Composable
private fun TelemetryProgressTrack(
    fractionProvider: () -> Float?,
    indeterminatePhase: () -> Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(6.dp)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val r = h / 2f
                val trackBrush = Brush.verticalGradient(
                    colors = listOf(ObsidianSurfaceElevated, ObsidianSurface),
                    startY = 0f,
                    endY = h
                )
                val fillBrush = Brush.horizontalGradient(
                    colors = listOf(StarkCyanDeep, StarkCyanPrimary, StarkCyanBright),
                    startX = 0f,
                    endX = w
                )
                val borderBrush = Brush.verticalGradient(
                    colors = listOf(SpecularHighlightSubtle, Color.Transparent),
                    startY = 0f,
                    endY = h
                )
                onDrawBehind {
                    val f = fractionProvider()
                    drawRoundRect(
                        brush = trackBrush,
                        cornerRadius = CornerRadius(r, r),
                        size = Size(w, h)
                    )
                    drawRoundRect(
                        brush = borderBrush,
                        cornerRadius = CornerRadius(r, r),
                        size = Size(w, h),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                    if (f != null) {
                        val fillW = w * f.coerceIn(0f, 1f)
                        if (fillW > 0.5f) {
                            drawRoundRect(
                                brush = fillBrush,
                                topLeft = Offset.Zero,
                                size = Size(fillW, h),
                                cornerRadius = CornerRadius(r, r)
                            )
                            drawCircle(
                                color = StarkCyanBright.copy(alpha = 0.35f),
                                radius = h * 1.6f,
                                center = Offset(fillW - r, h / 2f)
                            )
                            drawCircle(
                                color = StarkCyanBright,
                                radius = r,
                                center = Offset(fillW - r, h / 2f)
                            )
                        }
                    } else {
                        val phase = indeterminatePhase()
                        val segW = w * 0.25f
                        val segStart = (w - segW) * phase
                        drawRoundRect(
                            brush = fillBrush,
                            topLeft = Offset(segStart, 0f),
                            size = Size(segW, h),
                            cornerRadius = CornerRadius(r, r)
                        )
                    }
                }
            }
    )
}
