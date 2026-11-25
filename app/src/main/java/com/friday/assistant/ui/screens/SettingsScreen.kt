package com.friday.assistant.ui.screens

import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.data.InferenceMode
import com.friday.assistant.data.ModelStatus
import com.friday.assistant.ui.ChatUiState
import com.friday.assistant.ui.DiagnosticsInfo
import com.friday.assistant.ui.components.ObsidianSurface
import com.friday.assistant.ui.theme.CyanBorderGlow
import com.friday.assistant.ui.theme.FridayDisplayFont
import com.friday.assistant.ui.theme.FridayMotion
import com.friday.assistant.ui.theme.LegacyOnPrimaryDark
import com.friday.assistant.ui.theme.ObsidianSurfaceElevated
import com.friday.assistant.ui.theme.ObsidianSurfaceGlass
import com.friday.assistant.ui.theme.ObsidianVoid
import com.friday.assistant.ui.theme.SpecularHighlightStrong
import com.friday.assistant.ui.theme.SpecularHighlightSubtle
import com.friday.assistant.ui.theme.StarkCyanDeep
import com.friday.assistant.ui.theme.StarkCyanPrimary
import com.friday.assistant.ui.theme.TextPrimary
import com.friday.assistant.ui.theme.TextSecondary
import com.friday.assistant.ui.theme.TextTertiary
import com.friday.assistant.ui.theme.TelemetryAmber
import com.friday.assistant.ui.theme.TelemetryCrimson
import com.friday.assistant.ui.theme.TelemetryEmerald
import kotlinx.coroutines.CancellationException

private enum class SettingsPage { MAIN, PERSONA_MEMORY, CHATS_STORAGE }

/**
 * Executive settings host: owns the internal MAIN / PERSONA_MEMORY /
 * CHATS_STORAGE page router (directional slides) and the Android 14+
 * predictive back gesture, where the whole settings surface tracks the
 * user's finger (translation + scale + alpha) before committing the pop.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onOpenChatsStorage: () -> Unit,
    onOpenPersonaMemory: () -> Unit,
    onPathChange: (String) -> Unit,
    onGpuChange: (Boolean) -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onInferenceModeChange: (InferenceMode) -> Unit,
    onApiBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onApiModelChange: (String) -> Unit,
    onSpeakRepliesChange: (Boolean) -> Unit,
    onTestApi: () -> Unit,
    onRefreshModels: () -> Unit,
    onBackPersona: () -> Unit,
    onBackChats: () -> Unit,
    onClearCurrentChat: () -> Unit,
    onClearAllChats: () -> Unit,
    onExportChats: () -> String,
    onImportChats: (String) -> Boolean,
    onRenameSession: (String, String) -> Unit,
    onPersonaPromptChange: (String) -> Unit,
    onResetPersonaPrompt: () -> Unit,
    onUseMemoriesChange: (Boolean) -> Unit,
    onAddMemory: (String) -> Unit,
    onUpdateMemory: (String, String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onClearMemories: () -> Unit,
    onApproveMemory: (String) -> Unit,
    onDiscardMemory: (String) -> Unit,
    onApproveAllMemories: () -> Unit,
    onExtractMemories: () -> Unit
) {
    val page = when {
        state.personaMemoryOpen -> SettingsPage.PERSONA_MEMORY
        state.chatsStorageOpen -> SettingsPage.CHATS_STORAGE
        else -> SettingsPage.MAIN
    }

    val backProgress = remember { Animatable(0f) }
    PredictiveBackHandler {
        try {
            it.collect { event -> backProgress.snapTo(event.progress) }
            backProgress.snapTo(0f)
            when (page) {
                SettingsPage.PERSONA_MEMORY -> onBackPersona()
                SettingsPage.CHATS_STORAGE -> onBackChats()
                SettingsPage.MAIN -> onBack()
            }
        } catch (e: CancellationException) {
            backProgress.animateTo(0f, FridayMotion.Snappy)
            throw e
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = backProgress.value
                translationX = p * size.width * 0.22f
                scaleX = 1f - (p * 0.03f)
                scaleY = 1f - (p * 0.03f)
                alpha = 1f - (p * 0.25f)
            }
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val deeper = targetState.ordinal > initialState.ordinal
                if (deeper) {
                    (
                        slideInHorizontally(tween(FridayMotion.Medium, easing = FridayMotion.Emphasized)) { it / 5 } +
                            fadeIn(tween(320))
                        ).togetherWith(
                            slideOutHorizontally(tween(FridayMotion.Medium, easing = FridayMotion.Emphasized)) { -it / 10 } +
                                fadeOut(tween(220))
                        )
                } else {
                    (
                        slideInHorizontally(tween(FridayMotion.Medium, easing = FridayMotion.Emphasized)) { -it / 10 } +
                            fadeIn(tween(320))
                        ).togetherWith(
                            slideOutHorizontally(tween(FridayMotion.Medium, easing = FridayMotion.Emphasized)) { it / 5 } +
                                fadeOut(tween(220))
                        )
                }
            },
            label = "settingsPages"
        ) { current ->
            when (current) {
                SettingsPage.MAIN -> SettingsMainPage(
                    state = state,
                    onBack = onBack,
                    onOpenChatsStorage = onOpenChatsStorage,
                    onOpenPersonaMemory = onOpenPersonaMemory,
                    onPathChange = onPathChange,
                    onGpuChange = onGpuChange,
                    onLoad = onLoad,
                    onUnload = onUnload,
                    onInferenceModeChange = onInferenceModeChange,
                    onApiBaseUrlChange = onApiBaseUrlChange,
                    onApiKeyChange = onApiKeyChange,
                    onApiModelChange = onApiModelChange,
                    onSpeakRepliesChange = onSpeakRepliesChange,
                    onTestApi = onTestApi,
                    onRefreshModels = onRefreshModels
                )
                SettingsPage.PERSONA_MEMORY -> PersonaMemoryScreen(
                    state = state,
                    onBack = onBackPersona,
                    onPersonaPromptChange = onPersonaPromptChange,
                    onResetPersonaPrompt = onResetPersonaPrompt,
                    onUseMemoriesChange = onUseMemoriesChange,
                    onAddMemory = onAddMemory,
                    onUpdateMemory = onUpdateMemory,
                    onDeleteMemory = onDeleteMemory,
                    onClearMemories = onClearMemories,
                    onApproveMemory = onApproveMemory,
                    onDiscardMemory = onDiscardMemory,
                    onApproveAllMemories = onApproveAllMemories,
                    onExtractMemories = onExtractMemories
                )
                SettingsPage.CHATS_STORAGE -> ChatsStorageScreen(
                    state = state,
                    onBack = onBackChats,
                    onClearCurrentChat = onClearCurrentChat,
                    onClearAllChats = onClearAllChats,
                    onExportChats = onExportChats,
                    onImportChats = onImportChats,
                    onRenameSession = onRenameSession
                )
            }
        }
    }
}

@Composable
private fun SettingsMainPage(
    state: ChatUiState,
    onBack: () -> Unit,
    onOpenChatsStorage: () -> Unit,
    onOpenPersonaMemory: () -> Unit,
    onPathChange: (String) -> Unit,
    onGpuChange: (Boolean) -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onInferenceModeChange: (InferenceMode) -> Unit,
    onApiBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onApiModelChange: (String) -> Unit,
    onSpeakRepliesChange: (Boolean) -> Unit,
    onTestApi: () -> Unit,
    onRefreshModels: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Scaffold(
        containerColor = ObsidianVoid,
        topBar = {
            SettingsTopBar(title = "SETTINGS", onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- NEURAL ENGINE ----
            item { SettingsSectionHeader("NEURAL ENGINE") }
            item {
                ObsidianSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Inference mode",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ModeChip("Local (LiteRT-LM)", state.inferenceMode == InferenceMode.LOCAL) { onInferenceModeChange(InferenceMode.LOCAL) }
                            ModeChip("Remote API", state.inferenceMode == InferenceMode.REMOTE_API) { onInferenceModeChange(InferenceMode.REMOTE_API) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (state.inferenceMode == InferenceMode.REMOTE_API)
                                "Active: Remote API. FRIDAY will send messages to your configured endpoint."
                            else
                                "Active: Local (LiteRT-LM). FRIDAY will use the on-device model.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StarkCyanPrimary
                        )

                        if (state.inferenceMode == InferenceMode.REMOTE_API) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Remote API endpoint",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Flexible endpoints: OpenAI-compatible (/v1/chat/completions), Ollama (/api/chat, /api/generate), or a full custom URL. Base hosts auto-resolve to /v1/chat/completions. API key/model optional depending on provider. The endpoint must use https://, and cleartext http:// is rejected because the API key and chat context are sent.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(10.dp))
                            SettingsTextField("Endpoint URL", state.apiConfig.baseUrl, "https://api.example.com/v1", onApiBaseUrlChange, singleLine = true)
                            Spacer(Modifier.height(10.dp))
                            SettingsTextField("API key (optional)", state.apiConfig.apiKey, "sk-...", onApiKeyChange, masked = true, singleLine = true)
                            Spacer(Modifier.height(10.dp))
                            SettingsTextField("Model (optional)", state.apiConfig.model, "gpt-4o-mini / model-name", onApiModelChange, singleLine = true)
                            Spacer(Modifier.height(10.dp))
                            val status = if (state.apiConfig.isConfigured) "Endpoint configured and will be used for chat." else "No endpoint configured. Add a URL to use Remote API."
                            Text(
                                status,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.apiConfig.isConfigured) StarkCyanPrimary else TelemetryCrimson
                            )
                            Spacer(Modifier.height(8.dp))
                            ActionButton("Test endpoint", primary = true, enabled = state.apiConfig.isConfigured) { onTestApi() }
                            state.apiTestMessage?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        } else {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "On-device model (LiteRT-LM)",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Use a .litertlm model. Bundled model is auto-detected; you can also provide a local path.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(10.dp))
                            ModelStatusPill(status = state.modelStatus)
                            Spacer(Modifier.height(10.dp))
                            SettingsTextField("Model path", state.modelPathInput, "/storage/.../models/model.litertlm", onPathChange, mono = true, singleLine = true)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ActionButton(text = if (state.modelStatus.isLoading) "Loading..." else "Load model", primary = true, enabled = !state.modelStatus.isLoading, onClick = onLoad)
                                ActionButton(text = "Unload", primary = false, enabled = state.modelStatus.isReady, onClick = onUnload)
                                ActionButton(text = "Refresh", primary = false, enabled = true, onClick = onRefreshModels)
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "Model manager",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Preloaded with the app (copied to device on first launch): Qwen3-0.6B (default, 343 MB), Granite 4.0 350M int8 (416 MB), Granite 4.0 350M fp16 (690 MB). Tap a model below to select it, then Load model.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Available models",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            if (state.availableModels.isEmpty()) {
                                Text(
                                    "No .litertlm models found in app storage.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.availableModels.forEach { path ->
                                        val name = path.substringAfterLast('/')
                                        val selected = state.modelPathInput == path
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(ObsidianSurfaceElevated)
                                                .border(
                                                    1.dp,
                                                    if (selected) CyanBorderGlow else SpecularHighlightSubtle,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable { onPathChange(path) }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(name, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                                Text(
                                                    path,
                                                    color = TextTertiary,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                if (selected) "Selected" else "Use",
                                                color = if (selected) StarkCyanPrimary else TextSecondary,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Use GPU backend", color = TextPrimary)
                                    Text(
                                        "Try GPU for supported models/devices; behavior depends on runtime.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(checked = state.useGpu, onCheckedChange = onGpuChange)
                            }
                            state.modelStatus.resolvedPath?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Loaded: $it",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = TextSecondary
                                )
                            }
                            state.modelStatus.lastError?.let {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Error: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TelemetryCrimson
                                )
                            }
                        }
                    }
                }
            }

            // ---- VOICE & SPEECH ----
            item { SettingsSectionHeader("VOICE & SPEECH") }
            item {
                ObsidianSurface(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Speak replies aloud", color = TextPrimary)
                            Text(
                                "Reads assistant replies and session greetings with on-device text-to-speech. Tap the speaker icon on a reply to hear it on demand.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(checked = state.speakReplies, onCheckedChange = onSpeakRepliesChange)
                    }
                }
            }

            // ---- MEMORY & PERSONA ----
            item { SettingsSectionHeader("MEMORY & PERSONA") }
            item {
                SettingsChevronRow(
                    title = "Persona & memory",
                    subtitle = "Behavior prompt, persistent memories, and on-chat extraction.",
                    onClick = onOpenPersonaMemory
                )
            }

            // ---- STORAGE & DIAGNOSTICS ----
            item { SettingsSectionHeader("STORAGE & DIAGNOSTICS") }
            item {
                SettingsChevronRow(
                    title = "Chats & storage",
                    subtitle = "Sessions, rename, import/export, and clearing stored history.",
                    onClick = onOpenChatsStorage
                )
            }
            item {
                ObsidianSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Diagnostics",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Snapshot of device, model, and last generation state. Copy it into a bug report.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianSurfaceElevated)
                                .border(1.dp, SpecularHighlightSubtle, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                formatDiagnostics(state.diagnostics),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        ActionButton("Copy diagnostics", primary = false, enabled = true) {
                            clipboard.setText(AnnotatedString(formatDiagnostics(state.diagnostics)))
                            Toast.makeText(context, "Diagnostics copied", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/**
 * Shared settings top bar: translucent glass strip with a bottom specular
 * hairline, a back chevron and a monospaced display title.
 */
@Composable
private fun SettingsTopBar(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .displayCutoutPadding()
            .background(ObsidianSurfaceGlass)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FridayDisplayFont,
                    letterSpacing = 1.5.sp
                ),
                color = TextPrimary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SpecularHighlightStrong)
        )
    }
}

/** Monospaced micro section header for the executive settings hierarchy. */
@Composable
internal fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 1.6.sp
        ),
        color = TextTertiary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

/** Chevron glass row that pushes deeper into a settings sub-page. */
@Composable
private fun SettingsChevronRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = FridayMotion.Snappy,
        label = "chevronRowPress"
    )
    ObsidianSurface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = StarkCyanPrimary,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

/** Compact glass telemetry pill for the loaded model status. */
@Composable
private fun ModelStatusPill(status: ModelStatus, modifier: Modifier = Modifier) {
    val dotColor = when {
        status.isLoading -> TelemetryAmber
        status.lastError != null -> TelemetryCrimson
        status.isReady -> TelemetryEmerald
        else -> TextTertiary
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(ObsidianSurfaceElevated)
            .border(1.dp, SpecularHighlightSubtle, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = status.statusText,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Mode selector chip: cyan gradient pill when selected, glass otherwise. */
@Composable
internal fun ModeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .then(
                if (selected) {
                    Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(StarkCyanPrimary, StarkCyanDeep)
                            ),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .border(1.dp, CyanBorderGlow, RoundedCornerShape(999.dp))
                } else {
                    Modifier
                        .background(ObsidianSurfaceElevated, RoundedCornerShape(999.dp))
                        .border(1.dp, SpecularHighlightSubtle, RoundedCornerShape(999.dp))
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            color = if (selected) LegacyOnPrimaryDark else TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** Glass text field with a cyan focus border and monospace option. */
@Composable
internal fun SettingsTextField(
    label: String,
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    mono: Boolean = false,
    masked: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (focused) CyanBorderGlow else SpecularHighlightSubtle,
        animationSpec = tween(FridayMotion.Quick),
        label = "settingsFieldBorder"
    )
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary
    )
    Spacer(Modifier.height(6.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurfaceElevated)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        if (value.isBlank()) {
            Text(
                placeholder,
                color = TextTertiary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (singleLine) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 13.sp,
                fontFamily = if (mono || masked) FontFamily.Monospace else FontFamily.Default
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(StarkCyanPrimary),
            visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
        )
    }
}

/** Primary = cyan gradient pill, secondary = glass pill, 0.97f press. */
@Composable
internal fun ActionButton(text: String, primary: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = FridayMotion.Snappy,
        label = "settingsActionPress"
    )
    val labelColor = when {
        !enabled -> TextTertiary
        primary -> LegacyOnPrimaryDark
        else -> TextPrimary
    }
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (primary && enabled) {
                    Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(StarkCyanPrimary, StarkCyanDeep)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, CyanBorderGlow, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                        .background(ObsidianSurfaceElevated, RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (enabled) SpecularHighlightSubtle else SpecularHighlightSubtle.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, color = labelColor, style = MaterialTheme.typography.labelLarge)
    }
}

private fun formatDiagnostics(d: DiagnosticsInfo): String = buildString {
    appendLine("Device: ${d.device}")
    appendLine("OS: ${d.androidVersion}")
    appendLine("ABI: ${d.abi}")
    appendLine("App: v${d.appVersion}")
    appendLine("Mode: ${d.inferenceMode}")
    appendLine("Model: ${d.modelFile.ifBlank { "(none)" }}")
    d.modelLoadMs?.let { appendLine("Last model load: $it ms") }
    d.lastGenerationMs?.let { appendLine("Last generation: $it ms") }
    d.lastGenerationError?.let { appendLine("Last error: $it") }
    appendLine("Sessions: ${d.sessionCount} (${d.messageCount} messages)")
    appendLine("TTS: ${if (d.ttsReady) "ready" else "unavailable"}")
}
