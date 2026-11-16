package com.friday.assistant.ui.theme

import android.database.ContentObserver
import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * [CubicBezierEasing] wrapper with an `operator fun invoke` so curves can be
 * sampled inline as `curve(t)` inside draw/graphicsLayer providers, while
 * still being passed directly as an [Easing] to tween specs.
 */
class FridayEase(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float
) : Easing {
    private val base = CubicBezierEasing(x1, y1, x2, y2)

    override fun transform(fraction: Float): Float = base.transform(fraction)

    operator fun invoke(fraction: Float): Float = base.transform(fraction)
}

/**
 * Central motion source of truth: temporal scale, custom curves and
 * critically calibrated springs for the F.R.I.D.A.Y motion system.
 */
object FridayMotion {
    // Temporal scale (ms)
    const val Micro = 90
    const val Quick = 150
    const val Short = 250
    const val Medium = 380
    const val Long = 650
    const val BootTotal = 1400

    // Boot choreography stage boundaries (ms)
    const val BootReticleEnd = 360
    const val BootEmblemEnd = 460
    const val BootArcEnd = 760
    const val BootShockwaveStart = 660
    const val BootShockwaveEnd = 1060
    const val BootWordmarkEnd = 920
    const val BootTelemetryStart = 620
    const val BootTelemetryEnd = 940
    const val BootHoldEnd = 1100
    const val BootDockEnd = 1400

    // Custom easing curves. FridayEase adds operator invoke for inline sampling.
    val Emphasized = FridayEase(0.2f, 0.0f, 0.0f, 1.0f)
    val HudLock = FridayEase(0.05f, 0.75f, 0.1f, 1.0f)
    val OpticalEase = FridayEase(0.4f, 0.0f, 0.2f, 1.0f)

    // One-bounce overshoot curve used for the emblem ignition (spring-like
    // feel without a separate Animatable, so the whole boot stays frame-locked
    // to a single master timeline and can skip instantly).
    val LogoPopEase = FridayEase(0.34f, 1.56f, 0.64f, 1.0f)

    // Springs
    val LogoPop = spring<Float>(dampingRatio = 0.60f, stiffness = 260f)
    val Snappy = spring<Float>(dampingRatio = 0.82f, stiffness = 450f)
    val SnappyPlacement = spring<IntOffset>(dampingRatio = 0.82f, stiffness = 450f)
    val SnappySize = spring<IntSize>(dampingRatio = 0.82f, stiffness = 450f)
    val SpatialSlide = spring<IntOffset>(dampingRatio = 0.88f, stiffness = 360f)

    // Shared specular gradient anchors
    val SpecularBorderStops = listOf(
        SpecularHighlightStrong,
        SpecularHighlightSubtle,
        SpecularShadowEdge
    )

    fun specularBorderBrush(width: Float, height: Float) =
        androidx.compose.ui.graphics.Brush.linearGradient(
            colors = SpecularBorderStops,
            start = Offset.Zero,
            end = Offset(width, height)
        )
}

private fun readAnimatorScale(context: Context): Float = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
}.getOrDefault(1f)

/**
 * Live reduced-motion observer. Returns true when the system
 * "Animator duration scale" developer option is set to "Animation off".
 * Reacts immediately via a ContentObserver, no app restart required.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    var reduced by remember { mutableStateOf(readAnimatorScale(context) == 0f) }
    DisposableEffect(context) {
        val resolver = context.contentResolver
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                reduced = readAnimatorScale(context) == 0f
            }
        }
        runCatching { resolver.registerContentObserver(uri, false, observer) }
        onDispose { runCatching { resolver.unregisterContentObserver(observer) } }
    }
    return reduced
}
