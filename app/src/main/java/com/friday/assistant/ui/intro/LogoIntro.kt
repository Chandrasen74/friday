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
import androidx.compose.ui.platform.LocalView
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
import com.friday.assistant.ui.theme.FridayHaptics
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
 * Five-stage cinematic cold-boot reveal (2600ms) driven by a single master
 * timeline:
 * 1. Seamless splash handoff + optical reticle lock (0-650ms)
 * 2. Dual-track arc-reactor sweep + shockwave (250-1700ms)
 * 3. Staggered wordmark + telemetry lock (650-1750ms)
 * 4. Executive hold (1750-2150ms)
 * 5. Spatial top-bar docking (2150-2600ms)
 *
 * The emblem is pixel-locked at Alignment.Center as an exact 160dp icon
 * inside the 216dp orbital assembly. The OS 12+ splash renders as a solid
 * ObsidianVoid field with no system icon (transparent 1x1 splash drawable),
 * so the emblem ignites from nothing over the first 350ms (alpha 0 -> 1,
 * scale 0.78 -> 1.0 with a one-bounce pop) while the HUD reticle locks
 * inward. All per-frame values are
 * provider lambdas read inside graphicsLayer/draw scopes so the hierarchy
 * never recomposes during the boot. Tap anywhere skips to completion.
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
    val view = LocalView.current
    var rootSize by remember { mutableStateOf(IntSize.Zero) }

    fun finish() {
        if (finished.compareAndSet(false, true)) {
            // Stage 5 dock completed (or the user skipped to it): one subtle
            // scalar tick marks the emblem locking into the top bar.
            FridayHaptics.tickLight(view, reducedMotion)
            onFinish()
        }
    }

    fun skip() {
        scope.launch {
            try {
                master.animateTo(1f, tween(durationMillis = 220, easing = FridayMotion.Emphasized))
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
        // ---- Backdrop group: reticle + wordmark + telemetry (fades at dock) ----
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val t = master.value * FridayMotion.BootTotal
                    alpha = 1f - FridayMotion.HudLock(
                        stage(t, FridayMotion.BootHoldEnd.toFloat(), FridayMotion.BootDockEnd.toFloat())
                    )
                }
        ) {
            // Outer optical reticle: 288dp concentric hairline rings locking
            // around the 160dp emblem (scale 1.25 -> 1.0, rotate -22 -> 0).
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(288.dp)
                    .hudReticle(
                        progress = {
                            val t = master.value * FridayMotion.BootTotal
                            FridayMotion.HudLock(stage(t, 0f, FridayMotion.BootReticleEnd.toFloat()))
                        },
                        innerRadiusRatio = 256f / 288f,
                        hairlineWidth = 0.9.dp,
                        cardinalTickLength = 10.dp,
                        minorTickCount = 16,
                        startScale = 1.25f,
                        startRotation = -22f
                    )
            )

            // Wordmark + telemetry anchored below the 288dp reticle, the
            // emblem itself stays locked at the exact viewport center.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 184.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val letterSpacing by remember {
                        derivedStateOf {
                            val t = master.value * FridayMotion.BootTotal
                            7f - 4.5f * FridayMotion.Emphasized(
                                stage(t, FridayMotion.BootWordmarkStart.toFloat(), FridayMotion.BootWordmarkEnd.toFloat())
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WORDMARK.forEachIndexed { index, char ->
                            val start = FridayMotion.BootWordmarkStart + index * 55f
                            val end = start + 450f
                            val glyphColor by remember(index) {
                                derivedStateOf {
                                    val t = master.value * FridayMotion.BootTotal
                                    val p = FridayMotion.Emphasized(stage(t, start, end))
                                    when {
                                        p < 0.35f -> StarkCyanBright
                                        p < 0.75f -> StarkCyanPrimary
                                        else -> TextPrimary
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    val t = master.value * FridayMotion.BootTotal
                                    val p = FridayMotion.Emphasized(stage(t, start, end))
                                    alpha = p
                                    translationY = (1f - p) * 14.dp.toPx()
                                }
                            ) {
                                Text(
                                    text = char.toString(),
                                    style = TextStyle(
                                        fontFamily = FridayDisplayFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
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

        // ---- Dock group: arc ring + glow + emblem (docks into the top bar) ----
        // 216dp orbital assembly holding the exact 160dp emblem dead-center.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(216.dp)
                .graphicsLayer {
                    val t = master.value * FridayMotion.BootTotal
                    val d = FridayMotion.Emphasized(
                        stage(t, FridayMotion.BootHoldEnd.toFloat(), FridayMotion.BootDockEnd.toFloat())
                    )
                    val rootW = rootSize.width.toFloat()
                    val rootH = rootSize.height.toFloat()
                    translationX = (dock.x - rootW / 2f) * d
                    translationY = (dock.y - rootH / 2f) * d
                    // 160dp emblem -> 28dp top-bar logo (28/160 = 0.175).
                    val targetScale = 28f / 160f
                    val s = 1f + (targetScale - 1f) * d
                    scaleX = s
                    scaleY = s
                }
        ) {
            // Bioluminescent core glow (240dp radial bloom, alpha -> 0.30).
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val t = master.value * FridayMotion.BootTotal
                        val bloom = FridayMotion.Emphasized(stage(t, 0f, FridayMotion.BootEmblemEnd.toFloat()))
                        val d = FridayMotion.Emphasized(
                            stage(t, FridayMotion.BootHoldEnd.toFloat(), FridayMotion.BootDockEnd.toFloat())
                        )
                        scaleX = 0.6f + 0.4f * bloom
                        scaleY = 0.6f + 0.4f * bloom
                        alpha = 0.30f * bloom * (1f - d)
                    }
                    .drawWithCache {
                        val glow = Brush.radialGradient(
                            colors = listOf(
                                StarkCyanPrimary,
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = 120.dp.toPx()
                        )
                        onDrawBehind { drawCircle(brush = glow, radius = 120.dp.toPx()) }
                    }
            )
            // Dual-track arc-reactor ring: 216dp outer primary arc (2.5dp
            // stroke) sweeping 0 -> 360 over the lock window, 192dp dashed
            // counter-arc (1.2dp, 45% alpha), 4dp leading-edge orb and the
            // 216 -> 340dp completion shockwave. Fades out while docking.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val t = master.value * FridayMotion.BootTotal
                        val d = FridayMotion.Emphasized(
                            stage(t, FridayMotion.BootHoldEnd.toFloat(), FridayMotion.BootDockEnd.toFloat())
                        )
                        alpha = 1f - d
                    }
                    .arcRing(
                        sweepDegrees = {
                            val t = master.value * FridayMotion.BootTotal
                            FridayMotion.Emphasized(stage(t, FridayMotion.BootArcStart.toFloat(), FridayMotion.BootArcEnd.toFloat())) * 360f
                        },
                        counterSweepDegrees = {
                            val t = master.value * FridayMotion.BootTotal
                            FridayMotion.Emphasized(stage(t, FridayMotion.BootArcStart.toFloat(), FridayMotion.BootArcEnd.toFloat())) * 270f
                        },
                        shockwave = {
                            val t = master.value * FridayMotion.BootTotal
                            stage(t, FridayMotion.BootShockwaveStart.toFloat(), FridayMotion.BootShockwaveEnd.toFloat())
                        },
                        outerStrokeWidth = 2.5.dp,
                        innerStrokeWidth = 1.2.dp,
                        innerRadiusRatio = 192f / 216f,
                        shockExpansion = 0.574f,
                        tipOrbSize = 4.dp
                    )
            )
            // The emblem: the OS splash now renders with NO icon, so there is
            // no static logo to hand off from. The emblem ignites from
            // nothing over the first 350ms (alpha 0 -> 1, scale 0.78 -> 1.0
            // with the one-bounce LogoPopEase overshoot) exactly as the HUD
            // reticle locks inward around it.
            Image(
                painter = painterResource(id = R.drawable.friday_logo_blue),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(160.dp)
                    .graphicsLayer {
                        val t = master.value * FridayMotion.BootTotal
                        val p = FridayMotion.LogoPopEase(
                            stage(t, 0f, FridayMotion.BootEmblemPopEnd.toFloat())
                        )
                        alpha = p.coerceIn(0f, 1f)
                        val s = 0.78f + 0.22f * p
                        scaleX = s
                        scaleY = s
                    }
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
                    // Two 90ms blinks between 1300-1570ms, then solid lock.
                    alpha = when {
                        t < FridayMotion.BootTelemetryStart -> 0f
                        t < 1390f -> 1f
                        t < 1480f -> 0.25f
                        t < 1570f -> 1f
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
                fontSize = 10.5.sp,
                letterSpacing = 1.5.sp
            ),
            color = TextSecondary,
            modifier = Modifier.drawWithContent {
                val t = master.value * FridayMotion.BootTotal
                val wipe = FridayMotion.HudLock(
                    stage(t, FridayMotion.BootTelemetryStart.toFloat(), FridayMotion.BootTelemetryEnd.toFloat())
                )
                clipRect(right = size.width * wipe) {
                    this@drawWithContent.drawContent()
                }
            }
        )
    }
}
