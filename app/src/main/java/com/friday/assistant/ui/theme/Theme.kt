package com.friday.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OpenCodeDarkColorScheme = darkColorScheme(
    primary = Color(0xFF7DD3FC),
    onPrimary = Color(0xFF0B1720),
    primaryContainer = Color(0xFF1E3A52),
    onPrimaryContainer = Color(0xFFE6F6FF),
    secondary = Color(0xFF93C5FD),
    onSecondary = Color(0xFF0B1720),
    background = Color(0xFF1A1A1D),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF202024),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF26262B),
    onSurfaceVariant = Color(0xFFA8B3C2),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF2A2F36),
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
