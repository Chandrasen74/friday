package com.friday.assistant.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.friday.assistant.ui.theme.CyanBorderGlow
import com.friday.assistant.ui.theme.FridayMotion
import com.friday.assistant.ui.theme.ObsidianSurface
import com.friday.assistant.ui.theme.ObsidianSurfaceElevated
import com.friday.assistant.ui.theme.SpecularHighlightStrong
import com.friday.assistant.ui.theme.SpecularHighlightSubtle

/**
 * Foundational "anodized obsidian" container: layered dark fill, a 1dp
 * directional specular hairline border (top-left catchlight to bottom-right
 * falloff), optional active cyan glow, and tactile 0.975f press compression.
 */
@Composable
fun ObsidianSurface(
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = FridayMotion.Snappy,
        label = "obsidianPressScale"
    )
    val highlight by animateFloatAsState(
        targetValue = if (isHighlighted) 1f else 0f,
        animationSpec = tween(FridayMotion.Quick),
        label = "obsidianHighlight"
    )

    var surfaceModifier: Modifier = modifier
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        .drawWithCache {
            val width = size.width
            val height = size.height
            val shapeSize = Size(width, height)
            val fill = Brush.linearGradient(
                colors = listOf(ObsidianSurfaceElevated, ObsidianSurface),
                start = Offset(0f, height * 0.08f),
                end = Offset(width * 0.4f, height)
            )
            val borderBrush = Brush.linearGradient(
                colors = listOf(
                    SpecularHighlightStrong,
                    SpecularHighlightSubtle,
                    Color(0x05FFFFFF)
                ),
                start = Offset.Zero,
                end = Offset(width, height)
            )
            val activeBrush = Brush.linearGradient(
                colors = listOf(CyanBorderGlow, SpecularHighlightStrong),
                start = Offset.Zero,
                end = Offset(width, height)
            )
            val outlinePath = when (val outline = shape.createOutline(shapeSize, layoutDirection, this)) {
                is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                is Outline.Generic -> outline.path
                else -> null
            }
            val sheen = Brush.radialGradient(
                colors = listOf(
                    CyanBorderGlow.copy(alpha = CyanBorderGlow.alpha * highlight),
                    Color.Transparent
                ),
                center = Offset(width / 2f, 0f),
                radius = maxOf(width, height) * 0.9f
            )
            onDrawBehind {
                outlinePath?.let { path ->
                    drawPath(path, brush = fill)
                    if (highlight > 0.01f) {
                        // Sheen is strictly clipped to the outline so the
                        // radial catchlight can never bleed past the rounded
                        // corners as a rectangular band.
                        clipPath(path) {
                            drawRect(brush = sheen, size = Size(width, height * 0.55f))
                        }
                    }
                    drawPath(path, brush = borderBrush, style = Stroke(width = 1.dp.toPx()))
                    if (highlight > 0.01f) {
                        drawPath(
                            path,
                            brush = activeBrush,
                            style = Stroke(width = 1.dp.toPx()),
                            alpha = highlight
                        )
                    }
                }
            }
        }

    if (onClick != null) {
        surfaceModifier = surfaceModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    }

    Box(
        modifier = surfaceModifier,
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

/** Accessibility role helper for clickable obsidian surfaces used as buttons. */
@Composable
fun ObsidianSurfaceButton(
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    isHighlighted: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = FridayMotion.Snappy,
        label = "obsidianButtonPress"
    )
    val highlight by animateFloatAsState(
        targetValue = if (isHighlighted) 1f else 0f,
        animationSpec = tween(FridayMotion.Quick),
        label = "obsidianButtonHighlight"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .drawWithCache {
                val width = size.width
                val height = size.height
                val shapeSize = Size(width, height)
                val fill = Brush.linearGradient(
                    colors = listOf(ObsidianSurfaceElevated, ObsidianSurface),
                    start = Offset(0f, height * 0.08f),
                    end = Offset(width * 0.4f, height)
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(
                        SpecularHighlightStrong,
                        SpecularHighlightSubtle,
                        Color(0x05FFFFFF)
                    ),
                    start = Offset.Zero,
                    end = Offset(width, height)
                )
                val activeBrush = Brush.linearGradient(
                    colors = listOf(CyanBorderGlow, SpecularHighlightStrong),
                    start = Offset.Zero,
                    end = Offset(width, height)
                )
                val outlinePath = when (val outline = shape.createOutline(shapeSize, layoutDirection, this)) {
                    is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                    is Outline.Generic -> outline.path
                    else -> null
                }
                onDrawBehind {
                    outlinePath?.let { path ->
                        drawPath(path, brush = fill)
                        drawPath(path, brush = borderBrush, style = Stroke(width = 1.dp.toPx()))
                        if (highlight > 0.01f) {
                            drawPath(
                                path,
                                brush = activeBrush,
                                style = Stroke(width = 1.dp.toPx()),
                                alpha = highlight
                            )
                        }
                    }
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.TopStart
    ) {
        content()
    }
}
