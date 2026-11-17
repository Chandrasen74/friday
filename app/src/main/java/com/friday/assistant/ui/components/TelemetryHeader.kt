package com.friday.assistant.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.R
import com.friday.assistant.ui.theme.FridayDisplayFont
import com.friday.assistant.ui.theme.ObsidianSurfaceGlass
import com.friday.assistant.ui.theme.SpecularHighlightSubtle
import com.friday.assistant.ui.theme.TextPrimary
import com.friday.assistant.ui.theme.TextSecondary
import com.friday.assistant.ui.theme.TextTertiary
import kotlin.math.roundToInt

/**
 * Clean elevated glass top bar: translucent obsidian bar with a bottom
 * specular hairline, the 28dp brand logo (whose root-space center is reported
 * through [onLogoPositioned] so the cold-boot emblem can dock into it), the
 * Orbitron wordmark with the current session title, and the action buttons.
 * The model status pill lives exclusively in the navigation drawer, keeping
 * the bar uncluttered so the wordmark and session title never truncate.
 */
@Composable
fun TelemetryHeader(
    sessionTitle: String,
    onMenu: () -> Unit,
    onNew: () -> Unit,
    onRegenerate: (() -> Unit)?,
    regenerateEnabled: Boolean,
    onLogoPositioned: (IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
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
                Icon(
                    Icons.Rounded.Menu,
                    contentDescription = "Open navigation drawer",
                    tint = TextPrimary
                )
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
                    style = TextStyle(
                        fontFamily = FridayDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 2.sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = sessionTitle,
                    style = TextStyle(fontSize = 12.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onRegenerate != null) {
                IconButton(onClick = onRegenerate, enabled = regenerateEnabled) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Regenerate",
                        tint = TextTertiary
                    )
                }
            }
            IconButton(onClick = onNew) {
                Icon(Icons.Rounded.Add, contentDescription = "New chat", tint = TextTertiary)
            }
        }
    }
}
