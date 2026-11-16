package com.friday.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun fridayColorScheme() = darkColorScheme(
    primary = StarkCyanPrimary,
    onPrimary = LegacyOnPrimaryDark,
    primaryContainer = UserBubbleGradientTop,
    onPrimaryContainer = Color(0xFFE6F6FF),
    secondary = Color(0xFF93C5FD),
    onSecondary = LegacyOnPrimaryDark,
    background = ObsidianBase,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = HairlineNeutral,
    outlineVariant = HairlineNeutral,
    error = TelemetryCrimson,
    onError = Color(0xFF1A1A1A)
)

@Composable
fun FRIDAYTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = fridayColorScheme(),
        typography = Typography,
        content = content
    )
}
