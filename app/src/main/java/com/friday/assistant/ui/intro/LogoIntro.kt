package com.friday.assistant.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.R
import com.friday.assistant.ui.components.arcRing
import com.friday.assistant.ui.components.hudReticle
import com.friday.assistant.ui.theme.FridayDisplayFont
import com.friday.assistant.ui.theme.FridayMotion
import com.friday.assistant.ui.theme.ObsidianVoid
import com.friday.assistant.ui.theme.StarkCyanBright
import com.friday.assistant.ui.theme.StarkCyanPrimary
import com.friday.assistant.ui.theme.TextPrimary
import com.friday.assistant.ui.theme.TextSecondary
import com.friday.assistant.ui.theme.TelemetryEmerald
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val WORDMARK = "F.R.I.D.A.Y"
private const val TELEMETRY_LINE = "ON-DEVICE NEURAL CORE // READY"

private fun stage(masterT: Float, start: Float, end: Float): Float =
    ((masterT - start) / (end - start)).coerceIn(0f, 1f)

/**
 * Five-stage cold-boot reveal (<=1400ms) driven by a single master timeline:
 * 1. Void + optical reticle lock (0-360ms)
 * 2. Core emblem ignition (60-460ms)
 * 3. Dual-track arc sweep + shockwave (160-1060ms)
 * 4. Staggered wordmark + telemetry lock (320-940ms)
 * 5. Hold (940-1100ms) and spatial top-bar docking (1100-1400ms)
 *
 * The emblem container is pixel-locked at Alignment.Center inside an exact
 * 108dp box (72dp icon) matching the system splash geometry, so the logo
 * never shifts at t=0. Tap anywhere skips by accelerating to completion.
 */
@Composable
fun LogoIntro(
    dockTarget: IntOffset,
    reducedMotion: Boolean,
    onFinish: () -> Unit
) {
    val master = remember { Animatable(0f) }
    val finished = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val scope = rememberCoroutineScope()
    var rootSize by remember { mutableStateOf(IntSize.Zero) }

    fun finish() {
        if (finished.compareAndSet(false, true)) onFinish()
    }

    fun skip() {
        scope.launch {
            try {
                master.animateTo(1f, tween(durationMillis = 150, easing = FridayMotion.Emphasized))
            } catch (_: CancellationException) {
                // Fast-forward was preempted; treat as done.
            }
            finish()
        }
    }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            master.snapTo(1f)
            finish()
        } else {
            val result = runCatching {
                master.animateTo(1f, tween(durationMillis = FridayMotion.BootTotal, easing = LinearEasing))
            }
            if (result.isSuccess) finish()
        }
    }

    val dock = if (dockTarget != IntOffset.Zero) {
        dockTarget
    } else {
        IntOffset((rootSize.width / 2f).roundToInt(), (rootSize.height * 0.12f).roundToInt())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .pointerInput(Unit) { detectTapGestures(onTap = { skip() }) }
            .onSizeChanged { rootSize = it }
            .drawWithCache {
                val vignette = Brush.radialGradient(
                    colors = listOf(Color(0xFF141923), ObsidianVoid),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = maxOf(size.width, size.height) * 0.75f
                )
                onDrawBehind { drawRect(vignette) }
            }
            .clearAndSetSemantics {
                contentDescription = "F.R.I.D.A.Y startup sequence. Tap anywhere to skip."
            }
    ) {
        val centerDp = 108.dp

        // ---- Backdrop group: reticle + wordmark + telemetry (fades at dock) ----
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val t = master.value * FridayMotion.BootTotal
                    alpha = 1f - FridayMotion.HudLock(stage(t, 1140f, FridayMotion.BootDockEnd.toFloat()))
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(164.dp)
                    .hudReticle {
                        FridayMotion.HudLock(stage(master.value * FridayMotion.BootTotal, 0f, 360f))
                    }
            )

            // Wordmark + telemetry anchored below the exact-center emblem box
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 112.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val letterSpacing by remember {
                        derivedStateOf {
                            val tt = master.value * FridayMotion.BootTotal
                            when {
                                tt < 560f -> 7f
                                tt < 760f -> 4f
                                else -> 2.5f
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WORDMARK.forEachIndexed { index, char ->
                            val start = 320f + index * 32f
                            val end = start + 240f
                            val glyphColor by remember(index) {
                                derivedStateOf {
                                    val p = FridayMotion.Emphasized(stage(master.value * FridayMotion.BootTotal, start, end))
                                    when {
                                        p < 0.35f -> StarkCyanBright
                                        p < 0.75f -> StarkCyanPrimary
                                        else -> TextPrimary
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    val p = FridayMotion.Emphasized(stage(master.value * FridayMotion.BootTotal, start, end))
                                    alpha = p
                                    translationY = (1f - p) * 12.dp.toPx()
                                }
                            ) {
                                Text(
                                    text = char.toString(),
                                    style = TextStyle(
                                        fontFamily = FridayDisplayFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 26.sp,
                                        letterSpacing = letterSpacing.sp
                                    ),
                                    color = glyphColor
                                )
                            }
                            if (index != WORDMARK.lastIndex) {
                                Spacer(Modifier.width(1.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    TelemetryStrip(master)
                }
            }
        }

        // ---- Emblem + arc ring (docks into the top bar) ----
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(centerDp)
                .graphicsLayer {
                    val t = master.value * FridayMotion.BootTotal
                    val d = FridayMotion.Emphasized(stage(t, 1100f, FridayMotion.BootDockEnd.toFloat()))
                    val rootW = rootSize.width.toFloat()
                    val rootH = rootSize.height.toFloat()
                    translationX = (dock.x - rootW / 2f) * d
                    translationY = (dock.y - rootH / 2f) * d
                    val targetScale = 28f / 72f
                    val s = 1f + (targetScale - 1f) * d
                    scaleX = s
                    scaleY = s
                }
        ) {
            // Bioluminescent core glow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val t = master.value * FridayMotion.BootTotal
                        val p = FridayMotion.Emphasized(stage(t, 60f, 460f))
                        scaleX = 0.6f + 0.4f * p
                        scaleY = 0.6f + 0.4f * p
                        alpha = p
                    }
                    .drawWithCache {
                        val glow = Brush.radialGradient(
                            colors = listOf(
                                StarkCyanPrimary.copy(alpha = 0.24f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.minDimension / 2f
                        )
                        onDrawBehind { drawCircle(brush = glow) }
                    }
            )
            Image(
                painter = painterResource(id = R.drawable.friday_logo_blue),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .graphicsLayer {
                        val t = master.value * FridayMotion.BootTotal
                        val p = FridayMotion.LogoPopEase(stage(t, 60f, 460f))
                        val s = 0.84f + 0.16f * p
                        scaleX = s
                        scaleY = s
                        alpha = p.coerceIn(0f, 1f)
                    }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .arcRing(
                        sweepDegrees = {
                            val t = master.value * FridayMotion.BootTotal
                            FridayMotion.Emphasized(stage(t, 160f, 760f)) * 360f
                        },
                        counterSweepDegrees = {
                            val t = master.value * FridayMotion.BootTotal
                            FridayMotion.Emphasized(stage(t, 160f, 760f)) * 270f
                        },
                        shockwave = {
                            val t = master.value * FridayMotion.BootTotal
                            stage(t, 660f, 1060f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun TelemetryStrip(master: Animatable<Float, *>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .graphicsLayer {
                    val t = master.value * FridayMotion.BootTotal
                    // Two 80ms blinks then solid lock
                    alpha = when {
                        t < 620f -> 0f
                        t < 700f -> if (((t - 620f) / 40f).toInt() % 2 == 0) 1f else 0.25f
                        t < 780f -> if (((t - 700f) / 40f).toInt() % 2 == 0) 1f else 0.25f
                        else -> 1f
                    }
                }
                .background(TelemetryEmerald, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = TELEMETRY_LINE,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp
            ),
            color = TextSecondary,
            modifier = Modifier.drawWithContent {
                val t = master.value * FridayMotion.BootTotal
                val wipe = FridayMotion.HudLock(stage(t, 620f, 940f))
                clipRect(right = size.width * wipe) {
                    this@drawWithContent.drawContent()
                }
            }
        )
    }
}
