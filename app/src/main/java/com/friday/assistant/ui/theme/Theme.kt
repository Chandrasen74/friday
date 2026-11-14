package com.friday.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OpenCodeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE6E6E6),
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFF2F2F2),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF121212),
    background = Color(0xFF0F0F10),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF141416),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF1C1C1F),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF2E2E33),
    outlineVariant = Color(0xFF252529),
    error = Color(0xFFF87171),
    onError = Color(0xFF1A1A1A)
)

@Composable
fun FRIDAYTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OpenCodeDarkColorScheme,
        typography = Typography,
        content = content
    )
}
