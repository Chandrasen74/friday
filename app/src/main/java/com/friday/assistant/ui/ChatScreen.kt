package com.friday.assistant.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import androidx.compose.ui.ExperimentalComposeUiApi
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.friday.assistant.R
import com.friday.assistant.data.ApiEndpointConfig
import com.friday.assistant.data.ChatMessage
import com.friday.assistant.data.ChatRole
import com.friday.assistant.data.ChatSession
import com.friday.assistant.data.GenerationMetrics
import com.friday.assistant.data.InferenceMode
import com.friday.assistant.data.MemoryItem
import com.friday.assistant.data.ModelStatus
import com.friday.assistant.data.PersonaConfig
import com.friday.assistant.ui.theme.FridayDisplayFont
import com.friday.assistant.ui.screens.PreparingScreen
import com.friday.assistant.ui.theme.FridayMotion
import com.friday.assistant.ui.theme.ObsidianVoid
import com.friday.assistant.ui.components.TelemetryHeader
import com.friday.assistant.ui.components.TypingDots
import com.friday.assistant.ui.components.VoiceWaveform
import com.friday.assistant.ui.intro.LogoIntro
import com.friday.assistant.ui.theme.CyanBorderGlow
import com.friday.assistant.ui.theme.LegacyOnPrimaryDark
import com.friday.assistant.ui.theme.ObsidianBase
import com.friday.assistant.ui.components.ObsidianSurface
import com.friday.assistant.ui.theme.ObsidianSurface as ObsidianSurfaceColor
import com.friday.assistant.ui.theme.ObsidianSurfaceElevated
import com.friday.assistant.ui.theme.SpecularHighlightSubtle
import com.friday.assistant.ui.theme.StarkCyanDeep
import com.friday.assistant.ui.theme.StarkCyanMuted
import com.friday.assistant.ui.theme.StarkCyanPrimary
import com.friday.assistant.ui.theme.TelemetryCrimson
import com.friday.assistant.ui.theme.TextPrimary
import com.friday.assistant.ui.theme.TextSecondary
import com.friday.assistant.ui.theme.TextTertiary
import com.friday.assistant.ui.theme.UserBubbleGradientBottom
import com.friday.assistant.ui.theme.UserBubbleGradientTop
import com.friday.assistant.ui.theme.rememberReducedMotion
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface AppDestination {
    data object Preparing : AppDestination
    data object Chat : AppDestination
    data object Settings : AppDestination
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FridayApp(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val destination = when {
        state.bundledCopyProgress != null -> AppDestination.Preparing
        state.settingsOpen -> AppDestination.Settings
        else -> AppDestination.Chat
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) viewModel.onInputChange(spoken)
        }
    }
    fun launchSpeechInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to F.R.I.D.A.Y")
            }
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // Cold-boot hero: ChatScreen mounts underneath from frame 1 (inference,
    // database and flows run in parallel); the intro overlays and docks.
    // Played once per process launch; skipped while the bundled copy runs.
    var introFinished by rememberSaveable { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    var topBarLogoCenter by remember { mutableStateOf(IntOffset.Zero) }
    LaunchedEffect(state.bundledCopyProgress) {
        if (state.bundledCopyProgress != null) introFinished = true
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            when {
                initialState == AppDestination.Preparing -> {
                    (
                        fadeIn(tween(FridayMotion.Medium, easing = FridayMotion.Emphasized)) +
                            scaleIn(
                                initialScale = 0.96f,
                                animationSpec = tween(FridayMotion.Medium, easing = FridayMotion.Emphasized)
                            )
                        ).togetherWith(fadeOut(tween(220)))
                }
                targetState == AppDestination.Settings -> {
                    (
                        slideInHorizontally(tween(360, easing = FridayMotion.Emphasized)) { it / 4 } +
                            fadeIn(tween(320))
                        ).togetherWith(
                            slideOutHorizontally(tween(360, easing = FridayMotion.Emphasized)) { -it / 12 } +
                                fadeOut(tween(260)) +
                                scaleOut(targetScale = 0.98f, animationSpec = tween(360, easing = FridayMotion.Emphasized))
                        )
                }
                else -> {
                    (
                        slideInHorizontally(tween(360, easing = FridayMotion.Emphasized)) { -it / 12 } +
                            fadeIn(tween(320)) +
                            scaleIn(initialScale = 0.98f, animationSpec = tween(360, easing = FridayMotion.Emphasized))
                        ).togetherWith(
                            slideOutHorizontally(tween(360, easing = FridayMotion.Emphasized)) { it / 4 } +
                                fadeOut(tween(260))
                        )
                }
            }
        },
        label = "fridayAppRouter"
    ) { dest ->
        when (dest) {
            AppDestination.Preparing -> {
                state.bundledCopyProgress?.let { progress ->
                    PreparingScreen(progress = progress)
                }
            }
            AppDestination.Chat -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = true,
                        scrimColor = Color.Transparent,
                        drawerContent = {
                            ModalDrawerSheet(
                                modifier = Modifier
                                    .width(320.dp)
                                    .fillMaxHeight()
                                    .drawWithCache {
                                        onDrawBehind {
                                            // Right-edge specular hairline
                                            drawLine(
                                                color = SpecularHighlightSubtle,
                                                start = Offset(size.width - 0.5f, 0f),
                                                end = Offset(size.width - 0.5f, size.height),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }
                                    },
                                drawerContainerColor = ObsidianBase,
                                drawerTonalElevation = 0.dp
                            ) {
                                DrawerContent(
                                    state = state,
                                    onNewSession = {
                                        viewModel.newSession()
                                        scope.launch { drawerState.close() }
                                    },
                                    onSelectSession = {
                                        viewModel.selectSession(it)
                                        scope.launch { drawerState.close() }
                                    },
                                    onDeleteSession = { viewModel.deleteSession(it) },
                                    onOpenSettings = {
                                        viewModel.toggleSettings(true)
                                        scope.launch { drawerState.close() }
                                    }
                                )
                            }
                        }
                    ) {
                        DrawerDepthBox(drawerState = drawerState) {
                            ChatScreen(
                                state = state,
                                onMenu = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    scope.launch { drawerState.open() }
                                },
                                onNew = { viewModel.newSession() },
                                onInputChange = viewModel::onInputChange,
                                onSend = viewModel::sendMessage,
                                onStop = viewModel::stopGeneration,
                                onRegenerate = viewModel::regenerateLastResponse,
                                onEditMessage = viewModel::startEditMessage,
                                onMic = { launchSpeechInput() },
                                onSpeakMessage = viewModel::speakMessage,
                                onLogoPositioned = { topBarLogoCenter = it }
                            )
                        }
                    }
                    if (!introFinished) {
                        LogoIntro(
                            dockTarget = topBarLogoCenter,
                            reducedMotion = reducedMotion,
                            onFinish = { introFinished = true }
                        )
                    }
                }
            }
            AppDestination.Settings -> {
                SettingsScreen(
                    state = state,
                    onBack = { viewModel.toggleSettings(false) },
                    onOpenChatsStorage = { viewModel.openChatsStorage(true) },
                    onOpenPersonaMemory = { viewModel.openPersonaMemory(true) },
                    onPathChange = viewModel::onModelPathChange,
                    onGpuChange = viewModel::onUseGpuChange,
                    onLoad = viewModel::loadModel,
                    onUnload = viewModel::unloadModel,
                    onInferenceModeChange = viewModel::onInferenceModeChange,
                    onApiBaseUrlChange = viewModel::onApiBaseUrlChange,
                    onApiKeyChange = viewModel::onApiKeyChange,
                    onApiModelChange = viewModel::onApiModelChange,
                    onSpeakRepliesChange = viewModel::onSpeakRepliesChange,
                    onTestApi = viewModel::testApiEndpoint,
                    onRefreshModels = viewModel::refreshAvailableModels,
                    onBackPersona = { viewModel.openPersonaMemory(false) },
                    onBackChats = { viewModel.openChatsStorage(false) },
                    onClearCurrentChat = viewModel::clearCurrentSession,
                    onClearAllChats = viewModel::clearAllChats,
                    onExportChats = viewModel::exportChatsJson,
                    onImportChats = viewModel::importChatsJson,
                    onRenameSession = viewModel::renameSession,
                    onPersonaPromptChange = viewModel::onPersonaPromptChange,
                    onResetPersonaPrompt = viewModel::resetPersonaPrompt,
                    onUseMemoriesChange = viewModel::onUseMemoriesChange,
                    onAddMemory = viewModel::addMemory,
                    onUpdateMemory = viewModel::updateMemory,
                    onDeleteMemory = viewModel::deleteMemory,
                    onClearMemories = viewModel::clearMemories,
                    onExtractMemories = viewModel::extractMemoriesFromCurrentChat
                )
            }
        }
    }
}

/**
 * Scales and rounds the scaffold content behind the modal drawer while it
 * opens, with an ObsidianVoid depth scrim replacing the default drawer scrim.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerDepthBox(
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val drawerWidthPx = remember(density) { with(density) { 320.dp.toPx() } }
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val fraction = (drawerState.currentOffset / drawerWidthPx).coerceIn(0f, 1f)
                scaleX = lerp(1f, 0.965f, fraction)
                scaleY = lerp(1f, 0.965f, fraction)
                shape = RoundedCornerShape(lerp(0f, 24f, fraction).dp)
                clip = true
            }
            .drawWithContent {
                drawContent()
                val fraction = (drawerState.currentOffset / drawerWidthPx).coerceIn(0f, 1f)
                if (fraction > 0.01f) {
                    drawRect(ObsidianVoid.copy(alpha = 0.55f * fraction))
                }
            }
    ) {
        content()
    }
}

@Composable
private fun DrawerContent(
    state: ChatUiState,
    onNewSession: () -> Unit,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var sessionPendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val pending = sessionPendingDelete?.let { id -> state.sessions.firstOrNull { it.id == id } }
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { sessionPendingDelete = null },
            title = { Text("Delete chat?") },
            text = { Text("Delete \"${pending.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = pending.id
                    sessionPendingDelete = null
                    onDeleteSession(id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { sessionPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 320.dp)
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding()
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.friday_logo_blue),
                    contentDescription = "F.R.I.D.A.Y logo",
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "F.R.I.D.A.Y",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FridayDisplayFont),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onNewSession) {
                Icon(Icons.Default.Add, contentDescription = "New session", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(Modifier.height(8.dp))
        StatusPill(status = state.modelStatus)
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Sessions",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.sessions, key = { it.id }) { session ->
                SessionRow(
                    session = session,
                    selected = session.id == state.activeSessionId,
                    onSelect = { onSelectSession(session.id) },
                    onDelete = { sessionPendingDelete = session.id }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onOpenSettings() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Text("Settings", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SessionRow(
    session: ChatSession,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) StarkCyanMuted else Color.Transparent)
            .clickable { onSelect() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) StarkCyanPrimary else Color.Transparent)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
            )
            Text(
                text = "${session.messages.size} messages",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Delete session",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusPill(status: ModelStatus) {
    val color = when {
        status.isLoading -> MaterialTheme.colorScheme.secondary
        status.isReady -> MaterialTheme.colorScheme.primary
        status.lastError != null -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            text = status.statusText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChatScreen(
    state: ChatUiState,
    onMenu: () -> Unit,
    onNew: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onRegenerate: () -> Unit,
    onEditMessage: (String) -> Unit,
    onMic: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onLogoPositioned: (IntOffset) -> Unit
) {
    val session = state.activeSession
    val reducedMotion = rememberReducedMotion()
    val listState = remember(session?.id) { androidx.compose.foundation.lazy.LazyListState() }
    val messages = session?.messages.orEmpty()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val isNearBottom by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = layout.totalItemsCount
            total == 0 || lastVisible >= total - 2
        }
    }
    var prevMessageCount by remember(session?.id) { mutableStateOf(messages.size) }

    LaunchedEffect(session?.id) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size + 1)
        }
        prevMessageCount = messages.size
    }

    LaunchedEffect(messages.size) {
        if (messages.isEmpty()) {
            prevMessageCount = 0
        } else if (messages.size != prevMessageCount) {
            listState.animateScrollToItem(messages.size + 1)
            prevMessageCount = messages.size
        }
    }

    LaunchedEffect(messages.lastOrNull()?.id, messages.lastOrNull()?.content, messages.lastOrNull()?.isStreaming) {
        if (messages.isNotEmpty() && isNearBottom) {
            listState.scrollToItem(messages.size + 1)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TelemetryHeader(
                status = state.modelStatus,
                isGenerating = state.isGenerating,
                subtitle = buildString {
                    if (session != null && session.title.isNotBlank()) {
                        append(session.title)
                        append("  •  ")
                    }
                    append(
                        when {
                            state.inferenceMode == InferenceMode.REMOTE_API && state.apiConfig.isConfigured -> "Remote API"
                            state.inferenceMode == InferenceMode.REMOTE_API -> "Remote API (not configured)"
                            state.modelStatus.isReady -> "LiteRT-LM local"
                            else -> "Local fallback"
                        }
                    )
                    val modelLabel = state.metrics.modelName.ifBlank {
                        state.modelStatus.resolvedPath?.substringAfterLast('/') ?: ""
                    }.removeSuffix(".litertlm")
                    if (modelLabel.isNotBlank()) {
                        append("  •  ")
                        append(modelLabel)
                    }
                },
                metricsLine = formatMetrics(state.metrics, state.isGenerating),
                onMenu = onMenu,
                onNew = onNew,
                onRegenerate = if (session != null) onRegenerate else null,
                regenerateEnabled = !state.isGenerating && !state.isMemoryBusy,
                onLogoPositioned = onLogoPositioned
            )
        },
        bottomBar = {
            if (session != null) {
                ComposerBar(
                    value = state.input,
                    isGenerating = state.isGenerating,
                    onValueChange = onInputChange,
                    onSend = onSend,
                    onStop = onStop,
                    onMic = onMic
                )
            }
        }
    ) { padding ->
        if (session == null) {
            val prompts = listOf(
                "Draft a professional email reply",
                "Explain a complex topic simply",
                "Brainstorm weekend project ideas",
                "Help me plan my day"
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))
                // Ambient breathing core
                Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    BreathingAura(modifier = Modifier.matchParentSize(), reducedMotion = reducedMotion)
                    Image(
                        painter = painterResource(id = R.drawable.friday_logo_blue),
                        contentDescription = "F.R.I.D.A.Y logo",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "F.R.I.D.A.Y",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FridayDisplayFont),
                    color = TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "READY FOR LOCAL INFERENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                Spacer(Modifier.height(28.dp))
                prompts.forEachIndexed { index, prompt ->
                    PromptCard(
                        text = prompt,
                        index = index,
                        reducedMotion = reducedMotion,
                        onSubmit = {
                            onInputChange(prompt)
                            onSend()
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(8.dp))
                ActionButton(text = "Create new chat", primary = true, enabled = true, onClick = onNew)
            }
        } else {
            val seenMessageIds = rememberSaveable { mutableStateOf(setOf<String>()) }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(messages, key = { it.id }) { msg ->
                    val isNewMessage = remember(msg.id) { msg.id !in seenMessageIds.value }
                    if (isNewMessage) {
                        SideEffect { seenMessageIds.value = seenMessageIds.value + msg.id }
                    }
                    MessageRow(
                        modifier = Modifier.animateItem(
                            fadeInSpec = null,
                            placementSpec = FridayMotion.SnappyPlacement,
                            fadeOutSpec = tween(FridayMotion.Quick)
                        ),
                        message = msg,
                        enterAnimation = isNewMessage,
                        showTyping = state.isGenerating && msg.isStreaming && msg.content.isBlank(),
                        onCopy = {
                            clipboard.setText(AnnotatedString(msg.content))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        onEdit = if (msg.role == ChatRole.USER && !state.isGenerating) { { onEditMessage(msg.id); Toast.makeText(context, "Editing message", Toast.LENGTH_SHORT).show() } } else null,
                        onSpeak = if (msg.role == ChatRole.ASSISTANT && !msg.isError && !msg.isStreaming && msg.content.isNotBlank()) { { onSpeakMessage(msg.id) } } else null,
                        speaking = state.speakingMessageId == msg.id
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    modifier: Modifier = Modifier,
    message: ChatMessage,
    enterAnimation: Boolean = false,
    showTyping: Boolean = false,
    onCopy: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onSpeak: (() -> Unit)? = null,
    speaking: Boolean = false
) {
    val isUser = message.role == ChatRole.USER
    val reducedMotion = rememberReducedMotion()

    // Mount entrance: user bubbles slide up 12dp + fade (240ms); assistant
    // messages fade only (220ms, no vertical jitter). Driven from a single
    // Animatable read inside graphicsLayer; never re-triggered while
    // streaming state flips because the item keeps its identity.
    val entrance = remember { Animatable(if (enterAnimation && !reducedMotion) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (enterAnimation && !reducedMotion && entrance.value < 1f) {
            entrance.animateTo(
                1f,
                tween(durationMillis = if (isUser) 240 else 220, easing = FridayMotion.Emphasized)
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .graphicsLayer {
                alpha = entrance.value
                if (isUser) {
                    translationY = (1f - entrance.value) * 12.dp.toPx()
                }
            },
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 8.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianSurfaceElevated)
                    .border(1.dp, SpecularHighlightSubtle, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = StarkCyanPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        val bubbleShape = if (isUser) {
            RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
        } else {
            RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
        }
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(bubbleShape)
                .then(
                    when {
                        isUser -> Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(UserBubbleGradientTop, UserBubbleGradientBottom)
                            ),
                            shape = bubbleShape
                        )
                        message.isError -> Modifier.background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = bubbleShape
                        )
                        else -> Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(ObsidianSurfaceElevated, ObsidianSurfaceColor)
                            ),
                            shape = bubbleShape
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isUser -> CyanBorderGlow
                        message.isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                        else -> SpecularHighlightSubtle
                    },
                    shape = bubbleShape
                )
                .combinedClickable(onClick = { if (isUser) onEdit?.invoke() }, onLongClick = onCopy)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (!isUser) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "F.R.I.D.A.Y",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FridayDisplayFont),
                        color = StarkCyanPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (message.content.isNotBlank() && !message.isStreaming) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp).clickable { onCopy() }
                        )
                        if (onSpeak != null) {
                            Spacer(Modifier.width(8.dp))
                            SpeakIndicator(
                                speaking = speaking,
                                reducedMotion = reducedMotion,
                                onSpeak = onSpeak
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            } else if (onEdit != null) {
                Text(
                    text = "Tap to edit and resend, long press to copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                Spacer(Modifier.height(4.dp))
            }
            AnimatedContent(
                targetState = showTyping,
                transitionSpec = {
                    fadeIn(tween(FridayMotion.Quick)).togetherWith(fadeOut(tween(FridayMotion.Micro)))
                },
                label = "assistantTyping"
            ) { typing ->
                if (typing) {
                    Box(modifier = Modifier.padding(vertical = 6.dp)) {
                        TypingDots()
                    }
                } else if (!isUser && !message.isError && message.content.isNotBlank() && !message.isStreaming) {
                    Markdown(
                        content = message.content,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = message.content,
                        color = if (message.isError) MaterialTheme.colorScheme.error else TextPrimary,
                        style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFamily = if (!isUser && (message.content.contains("```") || message.content.contains("\n    "))) FontFamily.Monospace else FontFamily.Default)
                    )
                }
            }
            if (message.isStreaming && message.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Streaming...", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun SpeakIndicator(
    speaking: Boolean,
    reducedMotion: Boolean,
    onSpeak: () -> Unit
) {
    val halo = rememberInfiniteTransition(label = "speakHalo")
    val haloPhase by halo.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing)
        ),
        label = "speakHaloPhase"
    )
    Box(
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer {
                val pulse = if (speaking && !reducedMotion) lerp(1f, 1.08f, haloPhase) else 1f
                scaleX = pulse
                scaleY = pulse
            }
            .clickable { onSpeak() }
            .semantics {
                contentDescription = if (speaking) "Stop speaking" else "Speak"
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = speaking,
            transitionSpec = {
                (
                    fadeIn(tween(FridayMotion.Quick)) +
                        scaleIn(initialScale = 0.85f, animationSpec = tween(FridayMotion.Quick))
                    ).togetherWith(
                    fadeOut(tween(FridayMotion.Micro)) +
                        scaleOut(targetScale = 0.85f, animationSpec = tween(FridayMotion.Micro))
                )
            },
            label = "speakIndicator"
        ) { active ->
            if (active) {
                VoiceWaveform(barCount = 4, barWidth = 2.dp, maxHeight = 12.dp)
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun formatMetrics(m: GenerationMetrics, generating: Boolean): String {
    if (m.startedAtMs == null && !generating) return "ready"
    val ttft = m.ttftMs?.let { "${it}ms TTFT" }
    val total = m.totalMs?.let { "${it}ms total" }
    val tps = m.tokensPerSec?.let { String.format(Locale.US, "%.1f tok/s", it) }
    val cps = m.charsPerSec?.let { String.format(Locale.US, "%.1f ch/s", it) }
    val out = if (m.outputChars > 0) "${m.outputChars} chars (~${m.estimatedTokens} tok)" else null
    val chunks = if (m.chunkCount > 0) "${m.chunkCount} chunks" else null
    val parts = listOfNotNull(if (generating) "generating" else null, ttft, total, tps, cps, out, chunks)
    return if (parts.isEmpty()) "ready" else parts.joinToString(" | ")
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

@Composable
private fun ComposerBar(
    value: String,
    isGenerating: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onMic: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            ObsidianSurface(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(FridayMotion.SnappySize),
                shape = RoundedCornerShape(26.dp),
                isHighlighted = focused
            ) {
                if (value.isBlank()) {
                    Text(
                        text = if (isGenerating) "Generating..." else "Message F.R.I.D.A.Y...",
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = !isGenerating,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp),
                    cursorBrush = SolidColor(StarkCyanPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    maxLines = 6
                )
            }
            Spacer(Modifier.width(8.dp))
            ComposerActionButton(
                isGenerating = isGenerating,
                canSend = value.isNotBlank(),
                onSend = onSend,
                onStop = onStop,
                onMic = onMic
            )
        }
    }
}

private enum class ComposerAction { MIC, SEND, STOP }

/**
 * Kinetic composer action: one 44dp tactile target whose pill morphs between
 * Mic (empty input), Send (gradient cyan pill, dark icon) and Stop (crimson
 * glass pill, stop square) via AnimatedContent, with 0.90f press compression.
 */
@Composable
private fun ComposerActionButton(
    isGenerating: Boolean,
    canSend: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onMic: () -> Unit
) {
    val action = when {
        isGenerating -> ComposerAction.STOP
        canSend -> ComposerAction.SEND
        else -> ComposerAction.MIC
    }
    val enabled = when (action) {
        ComposerAction.STOP -> true
        ComposerAction.SEND -> canSend
        ComposerAction.MIC -> true
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = FridayMotion.Snappy,
        label = "composerActionPress"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                when (action) {
                    ComposerAction.STOP -> onStop()
                    ComposerAction.SEND -> onSend()
                    ComposerAction.MIC -> onMic()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = action,
            transitionSpec = {
                (
                    scaleIn(initialScale = 0.78f, animationSpec = tween(FridayMotion.Quick)) +
                        fadeIn(tween(FridayMotion.Quick))
                    ).togetherWith(
                    scaleOut(targetScale = 0.78f, animationSpec = tween(FridayMotion.Micro)) +
                        fadeOut(tween(FridayMotion.Micro))
                )
            },
            label = "composerAction"
        ) { target ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        when (target) {
                            ComposerAction.SEND -> Modifier.background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(StarkCyanPrimary, StarkCyanDeep)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            ComposerAction.STOP -> Modifier
                                .background(TelemetryCrimson.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                                .border(1.dp, TelemetryCrimson.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
                            ComposerAction.MIC -> Modifier
                                .background(ObsidianSurfaceElevated, RoundedCornerShape(14.dp))
                                .border(1.dp, SpecularHighlightSubtle, RoundedCornerShape(14.dp))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (target) {
                    ComposerAction.SEND -> Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = LegacyOnPrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                    ComposerAction.STOP -> Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(TelemetryCrimson)
                    )
                    ComposerAction.MIC -> Icon(
                        Icons.Default.Mic,
                        contentDescription = "Speak input",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Ultra-slow breathing radial aura behind the empty-state emblem
 * (scale 1.0 <-> 1.03, alpha 0.12 <-> 0.24, 3400ms OpticalEase). Static under
 * reduced motion; animated values read only inside graphicsLayer/draw.
 */
@Composable
private fun BreathingAura(modifier: Modifier = Modifier, reducedMotion: Boolean) {
    val transition = rememberInfiniteTransition(label = "breathingAura")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = FridayMotion.OpticalEase),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "auraScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = FridayMotion.OpticalEase),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                if (reducedMotion) {
                    scaleX = 1f
                    scaleY = 1f
                    this.alpha = 0.16f
                } else {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
            }
            .drawWithCache {
                val aura = Brush.radialGradient(
                    colors = listOf(
                        StarkCyanPrimary.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension / 2f
                )
                onDrawBehind { drawCircle(brush = aura) }
            }
    )
}

/**
 * Starter prompt card: 55ms staggered rise-in, cyan border flash on press
 * before submitting the prompt.
 */
@Composable
private fun PromptCard(
    text: String,
    index: Int,
    reducedMotion: Boolean,
    onSubmit: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (reducedMotion) {
            visible = true
        } else {
            delay(index * 55L)
            visible = true
        }
    }
    var flashed by remember { mutableStateOf(false) }
    LaunchedEffect(flashed) {
        if (flashed) {
            delay(120)
            onSubmit()
        }
    }
    val entrance by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FridayMotion.Emphasized),
        label = "promptCardEntrance"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entrance
                translationY = (1f - entrance) * 14.dp.toPx()
            }
    ) {
        ObsidianSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            isHighlighted = flashed,
            onClick = { if (!flashed) flashed = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = StarkCyanPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
            }
        }
    }
}

private enum class SettingsPage { MAIN, PERSONA_MEMORY, CHATS_STORAGE }

/**
 * Executive settings host: owns the internal MAIN / PERSONA_MEMORY /
 * CHATS_STORAGE page router (directional slides) and the Android 14+
 * predictive back gesture, where the whole settings surface tracks the
 * user's finger (translation + scale + alpha) before committing the pop.
 */
@Composable
private fun SettingsScreen(
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding().displayCutoutPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Inference mode", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeChip("Local (LiteRT-LM)", state.inferenceMode == InferenceMode.LOCAL) { onInferenceModeChange(InferenceMode.LOCAL) }
                    ModeChip("Remote API", state.inferenceMode == InferenceMode.REMOTE_API) { onInferenceModeChange(InferenceMode.REMOTE_API) }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (state.inferenceMode == InferenceMode.REMOTE_API)
                        "Active: Remote API. FRIDAY will send messages to your configured endpoint."
                    else
                        "Active: Local (LiteRT-LM). FRIDAY will use the on-device model.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (state.inferenceMode == InferenceMode.REMOTE_API) {
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Remote API endpoint", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Flexible endpoints: OpenAI-compatible (/v1/chat/completions), Ollama (/api/chat, /api/generate), or a full custom URL. Base hosts auto-resolve to /v1/chat/completions. API key/model optional depending on provider.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item { SettingsTextField("Endpoint URL", state.apiConfig.baseUrl, "https://api.example.com/v1", onApiBaseUrlChange, singleLine = true) }
                item { SettingsTextField("API key (optional)", state.apiConfig.apiKey, "sk-...", onApiKeyChange, masked = true, singleLine = true) }
                item { SettingsTextField("Model (optional)", state.apiConfig.model, "gpt-4o-mini / model-name", onApiModelChange, singleLine = true) }
                item {
                    val status = if (state.apiConfig.isConfigured) "Endpoint configured and will be used for chat." else "No endpoint configured. Add a URL to use Remote API."
                    Text(status, style = MaterialTheme.typography.bodySmall, color = if (state.apiConfig.isConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    ActionButton("Test endpoint", primary = true, enabled = state.apiConfig.isConfigured) { onTestApi() }
                    state.apiTestMessage?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Local model settings are hidden while Remote API mode is active.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("On-device model (LiteRT-LM)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Use a .litertlm model. Bundled model is auto-detected; you can also provide a local path.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item { StatusPill(status = state.modelStatus) }
                item { SettingsTextField("Model path", state.modelPathInput, "/storage/.../models/model.litertlm", onPathChange, mono = true, singleLine = true) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton(text = if (state.modelStatus.isLoading) "Loading..." else "Load model", primary = true, enabled = !state.modelStatus.isLoading, onClick = onLoad)
                        ActionButton(text = "Unload", primary = false, enabled = state.modelStatus.isReady, onClick = onUnload)
                        ActionButton(text = "Refresh", primary = false, enabled = true, onClick = onRefreshModels)
                    }
                }
                item {
                    Text("Model manager", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Preloaded with the app (copied to device on first launch): Qwen3-0.6B (default, 343 MB), Granite 4.0 350M int8 (416 MB), Granite 4.0 350M fp16 (690 MB). Tap a model below to select it, then Load model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Text("Available models", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    if (state.availableModels.isEmpty()) {
                        Text("No .litertlm models found in app storage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.availableModels.forEach { path ->
                            val name = path.substringAfterLast('/')
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface).clickable { onPathChange(path) }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                                    Text(path, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(if (state.modelPathInput == path) "Selected" else "Use", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use GPU backend", color = MaterialTheme.colorScheme.onSurface)
                            Text("Try GPU for supported models/devices; behavior depends on runtime.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.useGpu, onCheckedChange = onGpuChange)
                    }
                }
                item {
                    state.modelStatus.resolvedPath?.let {
                        Text("Loaded: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.modelStatus.lastError?.let {
                        Spacer(Modifier.height(6.dp))
                        Text("Error: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Remote API settings are hidden while Local mode is active.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))
                Text("Voice & speech", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Speak replies aloud", color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Reads assistant replies and session greetings with on-device text-to-speech. Tap the speaker icon on a reply to hear it on demand.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = state.speakReplies, onCheckedChange = onSpeakRepliesChange)
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))
                Text("Persona & memory", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text("Configure F.R.I.D.A.Y's behavior prompt and manage persistent memories used across chats.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                ActionButton("Open Persona & memory", primary = true, enabled = true) { onOpenPersonaMemory() }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))
                Text("Chats & storage", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text("Manage sessions, rename chats, import/export, and clear stored history in a separate section.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                ActionButton("Open Chats & storage", primary = true, enabled = true) { onOpenChatsStorage() }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))
                Text("Diagnostics", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Snapshot of device, model, and last generation state. Copy it into a bug report.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        formatDiagnostics(state.diagnostics),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))
                ActionButton("Copy diagnostics", primary = false, enabled = true) {
                    clipboard.setText(AnnotatedString(formatDiagnostics(state.diagnostics)))
                    Toast.makeText(context, "Diagnostics copied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun PersonaMemoryScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onPersonaPromptChange: (String) -> Unit,
    onResetPersonaPrompt: () -> Unit,
    onUseMemoriesChange: (Boolean) -> Unit,
    onAddMemory: (String) -> Unit,
    onUpdateMemory: (String, String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onClearMemories: () -> Unit,
    onExtractMemories: () -> Unit
) {
    val context = LocalContext.current
    var newMemoryText by rememberSaveable { mutableStateOf("") }
    var editingMemoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingMemoryText by rememberSaveable { mutableStateOf("") }
    var personaDraft by rememberSaveable(state.personaConfig.systemPrompt) { mutableStateOf(state.personaConfig.systemPrompt) }
    var showClearMemoriesDialog by rememberSaveable { mutableStateOf(false) }

    if (showClearMemoriesDialog) {
        AlertDialog(
            onDismissRequest = { showClearMemoriesDialog = false },
            title = { Text("Clear all memories?") },
            text = { Text("This will permanently delete all saved memories.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearMemoriesDialog = false
                    onClearMemories()
                    Toast.makeText(context, "Memories cleared", Toast.LENGTH_SHORT).show()
                }) { Text("Clear all") }
            },
            dismissButton = { TextButton(onClick = { showClearMemoriesDialog = false }) { Text("Cancel") } }
        )
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding().displayCutoutPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Persona & memory",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Persona / behavior prompt", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(
                    "This system prompt is applied to local and remote responses to shape how F.R.I.D.A.Y behaves.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                SettingsTextField(
                    label = "System prompt",
                    value = personaDraft,
                    placeholder = PersonaConfig.DEFAULT_PERSONA_PROMPT,
                    onChange = { personaDraft = it },
                    minLines = 3,
                    maxLines = 10
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("Save prompt", primary = true, enabled = personaDraft != state.personaConfig.systemPrompt) {
                        onPersonaPromptChange(personaDraft)
                        Toast.makeText(context, "Persona prompt saved", Toast.LENGTH_SHORT).show()
                    }
                    ActionButton("Reset to default", primary = false, enabled = personaDraft != PersonaConfig.DEFAULT_PERSONA_PROMPT) {
                        onResetPersonaPrompt()
                        Toast.makeText(context, "Persona reset to default", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Use persistent memories", color = MaterialTheme.colorScheme.onSurface)
                        Text("When enabled, saved memories are included as context in responses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = state.personaConfig.useMemories, onCheckedChange = onUseMemoriesChange)
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Persistent memory", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Store user preferences, facts, and context. You can add/edit manually or extract from the current chat using the active model.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton(
                        text = if (state.isMemoryBusy) "Working..." else "Extract from current chat",
                        primary = true,
                        enabled = !state.isMemoryBusy && !state.isGenerating
                    ) { onExtractMemories() }
                    ActionButton("Clear all memories", primary = false, enabled = state.memories.isNotEmpty() && !state.isMemoryBusy) {
                        showClearMemoriesDialog = true
                    }
                }
                state.memoryBusyMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                val isEditing = editingMemoryId != null
                SettingsTextField(
                    label = if (isEditing) "Edit memory" else "Add memory",
                    value = if (isEditing) editingMemoryText else newMemoryText,
                    placeholder = "e.g. Prefers concise answers and dark mode.",
                    onChange = { if (isEditing) editingMemoryText = it else newMemoryText = it }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isEditing) {
                        ActionButton("Save", primary = true, enabled = editingMemoryText.isNotBlank()) {
                            val id = editingMemoryId ?: return@ActionButton
                            onUpdateMemory(id, editingMemoryText)
                            editingMemoryId = null
                            editingMemoryText = ""
                            Toast.makeText(context, "Memory updated", Toast.LENGTH_SHORT).show()
                        }
                        ActionButton("Cancel", primary = false, enabled = true) {
                            editingMemoryId = null
                            editingMemoryText = ""
                        }
                    } else {
                        ActionButton("Add memory", primary = true, enabled = newMemoryText.isNotBlank()) {
                            onAddMemory(newMemoryText)
                            newMemoryText = ""
                            Toast.makeText(context, "Memory added", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            item {
                Text("Saved memories (${state.memories.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }

            if (state.memories.isEmpty()) {
                item {
                    Text("No memories saved yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(state.memories, key = { it.id }) { mem ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(mem.content, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (mem.source == "extracted") "Extracted memory" else "Manual memory",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionButton("Edit", primary = false, enabled = !state.isMemoryBusy) {
                                editingMemoryId = mem.id
                                editingMemoryText = mem.content
                            }
                            ActionButton("Delete", primary = false, enabled = !state.isMemoryBusy) {
                                onDeleteMemory(mem.id)
                                if (editingMemoryId == mem.id) {
                                    editingMemoryId = null
                                    editingMemoryText = ""
                                }
                                Toast.makeText(context, "Memory deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ChatsStorageScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onClearCurrentChat: () -> Unit,
    onClearAllChats: () -> Unit,
    onExportChats: () -> String,
    onImportChats: (String) -> Boolean,
    onRenameSession: (String, String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeSession = state.activeSession
    var renameText by rememberSaveable(activeSession?.id) { mutableStateOf(activeSession?.title ?: "") }
    var showClearCurrentDialog by rememberSaveable { mutableStateOf(false) }
    var showClearAllDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(activeSession?.id, activeSession?.title) {
        if (renameText.isBlank() || renameText != (activeSession?.title ?: "")) {
            renameText = activeSession?.title ?: ""
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val maxBytes = 10L * 1024L * 1024L
                        val bytes = input.readBytes()
                        if (bytes.size.toLong() > maxBytes) null else bytes.toString(Charsets.UTF_8)
                    }
                }.getOrNull()
            }
            val ok = !text.isNullOrBlank() && onImportChats(text)
            Toast.makeText(context, if (ok) "Chats imported" else "Import failed (invalid, empty, or too large file)", Toast.LENGTH_SHORT).show()
        }
    }

    if (showClearCurrentDialog) {
        AlertDialog(
            onDismissRequest = { showClearCurrentDialog = false },
            title = { Text("Clear current chat?") },
            text = { Text("This will remove messages in the current chat session. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearCurrentDialog = false
                    onClearCurrentChat()
                    Toast.makeText(context, "Current chat cleared", Toast.LENGTH_SHORT).show()
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { showClearCurrentDialog = false }) { Text("Cancel") } }
        )
    }
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear all chats?") },
            text = { Text("This will delete all saved chat sessions. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearAllDialog = false
                    onClearAllChats()
                    Toast.makeText(context, "All chats cleared", Toast.LENGTH_SHORT).show()
                }) { Text("Clear all") }
            },
            dismissButton = { TextButton(onClick = { showClearAllDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding().displayCutoutPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Chats & storage",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Chats & storage", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text("Chats are saved locally on device (durable app storage) and restored on launch.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                if (activeSession != null) {
                    SettingsTextField("Rename current session", renameText, "Session title", { renameText = it }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    ActionButton("Save title", primary = true, enabled = renameText.isNotBlank()) {
                        onRenameSession(activeSession.id, renameText)
                        Toast.makeText(context, "Session renamed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Text("No active chat session to rename. Create a new chat from the main screen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("Clear current chat", primary = false, enabled = activeSession != null) {
                        showClearCurrentDialog = true
                    }
                    ActionButton("Clear all chats", primary = false, enabled = state.sessions.isNotEmpty()) {
                        showClearAllDialog = true
                    }
                }
                Spacer(Modifier.height(10.dp))
                ActionButton("Copy chats export (JSON)", primary = true, enabled = true) {
                    val json = onExportChats().ifBlank { "{\"sessions\":[]}" }
                    clipboard.setText(AnnotatedString(json))
                    Toast.makeText(context, "Chats JSON copied", Toast.LENGTH_SHORT).show()
                }
                Spacer(Modifier.height(10.dp))
                ActionButton("Upload chats JSON file", primary = false, enabled = true) {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Select a .json file previously exported from FRIDAY to import chats.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SettingsTextField(
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
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        if (value.isBlank()) {
            Text(
                placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontFamily = if (mono || masked) FontFamily.Monospace else FontFamily.Default
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActionButton(text: String, primary: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) bg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, color = if (enabled) fg else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
    }
}
