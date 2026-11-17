package com.friday.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.ui.screens.ActionButton
import com.friday.assistant.ui.telemetry.SavedGenerationMetrics
import com.friday.assistant.ui.theme.FridayDisplayFont
import com.friday.assistant.ui.theme.ObsidianBase
import com.friday.assistant.ui.theme.ObsidianSurfaceElevated
import com.friday.assistant.ui.theme.SpecularHighlightStrong
import com.friday.assistant.ui.theme.SpecularHighlightSubtle
import com.friday.assistant.ui.theme.StarkCyanPrimary
import com.friday.assistant.ui.theme.TelemetryAmber
import com.friday.assistant.ui.theme.TelemetryEmerald
import com.friday.assistant.ui.theme.TextPrimary
import com.friday.assistant.ui.theme.TextSecondary
import com.friday.assistant.ui.theme.TextTertiary
import java.util.Locale

/**
 * Hardware diagnostics inspector for one assistant generation, presented as
 * an Obsidian Glass modal bottom sheet: model + quantization, decode speed,
 * total latency, compute backend and output volume, plus a performance tip
 * with a direct route into Settings when CPU decode is bandwidth-bound.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryHudSheet(
    metrics: SavedGenerationMetrics,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianBase,
        tonalElevation = 0.dp,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp)
        ) {
            // Custom Stark cyan drag handle in place of the stock grey one.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 14.dp, bottom = 16.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(StarkCyanPrimary.copy(alpha = 0.85f))
            )
            // Top specular seam under the sheet's rounded crown.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SpecularHighlightStrong)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "INFERENCE TELEMETRY // DIAGNOSTICS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.4.sp
                ),
                color = TextTertiary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "F.R.I.D.A.Y",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FridayDisplayFont),
                color = TextPrimary
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HudStatCard(
                    modifier = Modifier.weight(1f),
                    label = "DECODE SPEED",
                    value = String.format(Locale.US, "%.1f tok/s", metrics.tokensPerSecond),
                    valueColor = StarkCyanPrimary,
                    valueFontSize = 16.sp
                )
                HudStatCard(
                    modifier = Modifier.weight(1f),
                    label = "TOTAL LATENCY",
                    value = formatDuration(metrics.durationMs),
                    valueColor = TextPrimary,
                    valueFontSize = 16.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val backendHealthy = metrics.backendLabel.contains("GPU") ||
                    metrics.backendLabel.contains("REMOTE")
                HudStatCard(
                    modifier = Modifier.weight(1f),
                    label = "COMPUTE BACKEND",
                    value = metrics.backendLabel.ifBlank { "UNKNOWN" },
                    valueColor = TextSecondary,
                    valueFontSize = 12.sp,
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (backendHealthy) TelemetryEmerald else TelemetryAmber)
                        )
                    }
                )
                HudStatCard(
                    modifier = Modifier.weight(1f),
                    label = "OUTPUT VOLUME",
                    value = "~${metrics.estimatedTokens} tokens",
                    valueColor = TextPrimary,
                    valueFontSize = 14.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            ObsidianSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "ACTIVE MODEL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        ),
                        color = TextTertiary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = metrics.modelName.ifBlank { "UNSPECIFIED MODEL" },
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ObsidianSurfaceElevated)
                                .border(1.dp, SpecularHighlightSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = quantTagFor(metrics.modelName),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.8.sp
                                ),
                                color = StarkCyanPrimary
                            )
                        }
                    }
                }
            }
            if (metrics.tokensPerSecond < 3.0f && metrics.backendLabel == "LOCAL // CPU") {
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianSurfaceElevated)
                        .border(1.dp, TelemetryAmber.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(TelemetryAmber)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "PERFORMANCE TIP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.2.sp
                            ),
                            color = TelemetryAmber
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "CPU int8 decode is memory-bandwidth bound. Enable Use GPU backend in " +
                            "Settings, or switch to a 4-bit quantized model (wi4b32) for faster responses.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 17.sp,
                            fontFamily = FontFamily.Default
                        ),
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    ActionButton(
                        text = "Open Settings",
                        primary = true,
                        enabled = true,
                        onClick = onOpenSettings
                    )
                }
            }
        }
    }
}

/** One specular glass cell of the 2x2 diagnostics grid. */
@Composable
private fun HudStatCard(
    modifier: Modifier,
    label: String,
    value: String,
    valueColor: Color,
    valueFontSize: TextUnit,
    leading: (@Composable () -> Unit)? = null
) {
    ObsidianSurface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                ),
                color = TextTertiary
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = value,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = valueFontSize,
                        fontFeatureSettings = "tnum"
                    ),
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Formats a duration for the latency readout: ms under a second, s above. */
private fun formatDuration(durationMs: Long): String =
    if (durationMs < 1000) "$durationMs ms" else String.format(Locale.US, "%.2fs", durationMs / 1000.0)

/** Quantization hint derived from the model filename. */
private fun quantTagFor(modelName: String): String {
    val n = modelName.lowercase()
    return when {
        "int8" in n -> "INT8"
        "fp16" in n -> "FP16"
        "wi4" in n || "int4" in n || "4bit" in n || "q4" in n -> "4-BIT"
        else -> "STANDARD"
    }
}
