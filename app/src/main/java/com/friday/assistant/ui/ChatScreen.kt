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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
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
import com.friday.assistant.data.InferenceMode
import com.friday.assistant.data.MemoryItem
import com.friday.assistant.data.ModelStatus
import com.friday.assistant.ui.theme.FridayDisplayFont
import com.friday.assistant.ui.screens.PreparingScreen
import com.friday.assistant.ui.screens.SettingsScreen
import com.friday.assistant.ui.screens.ActionButton
import com.friday.assistant.ui.telemetry.GenerationTelemetryCache
import com.friday.assistant.ui.telemetry.SavedGenerationMetrics
import com.friday.assistant.ui.theme.FridayHaptics
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
import com.friday.assistant.ui.components.TelemetryHudSheet
import com.friday.assistant.ui.components.plasmaStreamGlow
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
    // Same durable store the chat surface writes to; cleared alongside the
    // chat-management actions so wiped history leaves no orphaned metrics.
    val telemetryCache = remember { GenerationTelemetryCache.getInstance(context) }

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
                                        val hairlineX = size.width - 0.5f
                                        val hairlineStart = Offset(hairlineX, 0f)
                                        val hairlineEnd = Offset(hairlineX, size.height)
                                        val hairlineStroke = 1.dp.toPx()
                                        onDrawBehind {
                                            // Right-edge specular hairline
                                            drawLine(
                                                color = SpecularHighlightSubtle,
                                                start = hairlineStart,
                                                end = hairlineEnd,
                                                strokeWidth = hairlineStroke
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
                                onLogoPositioned = { topBarLogoCenter = it },
                                onOpenSettings = { viewModel.toggleSettings(true) }
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
                    onClearCurrentChat = {
                        state.activeSession?.messages?.forEach { telemetryCache.remove(it.id) }
                        viewModel.clearCurrentSession()
                    },
                    onClearAllChats = {
                        telemetryCache.clearAll()
                        viewModel.clearAllChats()
                    },
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
                val raw = drawerState.currentOffset / drawerWidthPx
                val fraction = if (raw.isNaN()) 0f else raw.coerceIn(0f, 1f)
                scaleX = lerp(1f, 0.965f, fraction)
                scaleY = lerp(1f, 0.965f, fraction)
                shape = RoundedCornerShape(lerp(0f, 24f, fraction).dp)
                clip = true
            }
            .drawWithContent {
                drawContent()
                val raw = drawerState.currentOffset / drawerWidthPx
                val fraction = if (raw.isNaN()) 0f else raw.coerceIn(0f, 1f)
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
    onLogoPositioned: (IntOffset) -> Unit,
    onOpenSettings: () -> Unit
) {
    val session = state.activeSession
    val reducedMotion = rememberReducedMotion()
    val listState = remember(session?.id) { androidx.compose.foundation.lazy.LazyListState() }
    val messages = session?.messages.orEmpty()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    // Durable telemetry store: survives process death and cold boots, so
    // historical assistant bubbles keep their metrics footer after restart.
    val telemetryCache = remember(context) { GenerationTelemetryCache.getInstance(context) }
    val view = LocalView.current
    // Currently inspected generation for the Telemetry HUD sheet; null when
    // the modal is closed.
    var hudMetrics by remember { mutableStateOf<SavedGenerationMetrics?>(null) }
    val isNearBottom by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = layout.totalItemsCount
            total == 0 || lastVisible >= total - 2
        }
    }
    var prevMessageCount by remember(session?.id) { mutableStateOf(messages.size) }

    // Per-message generation telemetry: captured the moment an assistant
    // message finishes streaming, and mirrored into GenerationTelemetryCache
    // so restored history keeps its footer across cold boots. Greetings and
    // restored history never stream while mounted, so they never re-capture.
    val generationFootprints = remember(session?.id) {
        mutableStateMapOf<String, SavedGenerationMetrics>()
    }

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
                sessionTitle = session?.title?.takeIf { it.isNotBlank() } ?: "New session",
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
                    // Capture the VM's last-generation metrics on the
                    // streaming -> done flip of a mounted assistant message.
                    val prevStreaming = remember(msg.id) { mutableStateOf<Boolean?>(null) }
                    LaunchedEffect(msg.id, msg.isStreaming) {
                        val was = prevStreaming.value
                        prevStreaming.value = msg.isStreaming
                        if (was == true && !msg.isStreaming) {
                            val m = state.metrics
                            if (
                                msg.role == ChatRole.ASSISTANT &&
                                !msg.isError &&
                                msg.content.isNotBlank() &&
                                (m.tokensPerSec != null || m.totalMs != null)
                            ) {
                                val backendLabel = when {
                                    state.inferenceMode == InferenceMode.REMOTE_API -> "REMOTE API"
                                    state.useGpu -> "LOCAL // GPU"
                                    else -> "LOCAL // CPU"
                                }
                                val saved = SavedGenerationMetrics(
                                    tokensPerSecond = (m.tokensPerSec ?: 0.0).toFloat(),
                                    durationMs = m.totalMs ?: 0L,
                                    modelName = m.modelName.ifBlank {
                                        state.modelStatus.resolvedPath?.substringAfterLast('/') ?: ""
                                    },
                                    backendLabel = backendLabel,
                                    estimatedTokens = m.estimatedTokens.coerceAtLeast(1)
                                )
                                generationFootprints[msg.id] = saved
                                telemetryCache.put(msg.id, saved)
                                // Tactile lock-in the exact frame the footer
                                // becomes available.
                                FridayHaptics.tickConfirm(view, reducedMotion)
                            }
                        }
                    }
                    // Restored history resolves its footer from the durable
                    // cache; freshly finished generations come from the map.
                    val cachedFootprint = remember(msg.id) { telemetryCache.get(msg.id) }
                    MessageRow(
                        modifier = Modifier.animateItem(
                            fadeInSpec = null,
                            placementSpec = FridayMotion.SnappyPlacement,
                            fadeOutSpec = tween(FridayMotion.Quick)
                        ),
                        message = msg,
                        enterAnimation = isNewMessage,
                        showTyping = state.isGenerating && msg.isStreaming && msg.content.isBlank(),
                        footprint = generationFootprints[msg.id] ?: cachedFootprint,
                        onCopy = {
                            clipboard.setText(AnnotatedString(msg.content))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        onEdit = if (msg.role == ChatRole.USER && !state.isGenerating) { { onEditMessage(msg.id); Toast.makeText(context, "Editing message", Toast.LENGTH_SHORT).show() } } else null,
                        onSpeak = if (msg.role == ChatRole.ASSISTANT && !msg.isError && !msg.isStreaming && msg.content.isNotBlank()) { { onSpeakMessage(msg.id) } } else null,
                        speaking = state.speakingMessageId == msg.id,
                        onOpenHud = {
                            hudMetrics = it
                            FridayHaptics.tickLight(view, reducedMotion)
                        }
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
    hudMetrics?.let { metrics ->
        TelemetryHudSheet(
            metrics = metrics,
            onDismiss = { hudMetrics = null },
            onOpenSettings = {
                hudMetrics = null
                onOpenSettings()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    modifier: Modifier = Modifier,
    message: ChatMessage,
    enterAnimation: Boolean = false,
    showTyping: Boolean = false,
    footprint: SavedGenerationMetrics? = null,
    onCopy: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onSpeak: (() -> Unit)? = null,
    speaking: Boolean = false,
    onOpenHud: (SavedGenerationMetrics) -> Unit = {}
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .graphicsLayer {
                alpha = entrance.value
                if (isUser) {
                    translationY = (1f - entrance.value) * 12.dp.toPx()
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
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
            val bubbleContent: @Composable () -> Unit = {
                Column(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        // Loading/blank states (typing-dots frame, restored
                        // empty placeholder, empty model output) must never
                        // collapse into a flat capsule: hold a stable
                        // intrinsic minimum height from frame 1 so the
                        // header and indicator keep their breathing room.
                        .then(if (!isUser) Modifier.defaultMinSize(minHeight = 56.dp) else Modifier)
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
            if (isUser) {
                bubbleContent()
            } else {
                Box(
                    modifier = Modifier.plasmaStreamGlow(message.isStreaming, bubbleShape, reducedMotion)
                ) {
                    bubbleContent()
                }
            }
        }
        // Post-generation micro-telemetry: only after the assistant stream
        // completes, never mid-stream, with a gentle fade + expand entrance.
        val showMetrics = !isUser && !message.isStreaming && !message.isError &&
            message.content.isNotBlank() && footprint != null
        AnimatedVisibility(
            visible = showMetrics,
            enter = fadeIn(tween(240)) + expandVertically(tween(220)),
            modifier = Modifier.padding(start = 40.dp, top = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { footprint?.let(onOpenHud) }
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(StarkCyanPrimary.copy(alpha = 0.7f))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatFootprint(footprint),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.8.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = TextTertiary
                )
                // Trailing chevron hints the pill opens the diagnostics sheet.
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "›",
                    style = TextStyle(fontSize = 12.sp),
                    color = TextTertiary
                )
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

/** Formats the captured post-generation telemetry: tok/s and total time. */
private fun formatFootprint(footprint: SavedGenerationMetrics?): String {
    if (footprint == null) return ""
    val speed = if (footprint.tokensPerSecond > 0f) {
        String.format(Locale.US, "%.1f tok/s", footprint.tokensPerSecond)
    } else {
        null
    }
    val duration = footprint.durationMs.takeIf { it > 0 }?.let { ms ->
        if (ms < 1000) "${ms}ms" else String.format(Locale.US, "%.2fs", ms / 1000.0)
    }
    return listOfNotNull(speed, duration).joinToString("  •  ")
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
    val composerShape = RoundedCornerShape(26.dp)
    // Obsidian glass capsule chrome: crisp 1.2dp hairline that ignites cyan
    // on focus plus a soft 2dp outer halo, both tweened over 180ms.
    val borderColor by animateColorAsState(
        targetValue = if (focused) StarkCyanPrimary.copy(alpha = 0.65f) else SpecularHighlightSubtle,
        animationSpec = tween(durationMillis = 180),
        label = "composerBorder"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (focused) 0.28f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "composerHalo"
    )
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(FridayMotion.SnappySize)
                    .drawBehind {
                        if (haloAlpha > 0.01f) {
                            val outset = 2.dp.toPx()
                            drawRoundRect(
                                color = StarkCyanPrimary.copy(alpha = haloAlpha),
                                topLeft = Offset(-outset, -outset),
                                size = Size(size.width + outset * 2f, size.height + outset * 2f),
                                cornerRadius = CornerRadius(26.dp.toPx() + outset)
                            )
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .clip(composerShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF1A1A23), Color(0xFF131319))
                            ),
                            shape = composerShape
                        )
                        .drawWithCache {
                            // Top inner sheen, strictly inside the clipped capsule.
                            val sheen = Brush.radialGradient(
                                colors = listOf(Color(0x14FFFFFF), Color.Transparent),
                                center = Offset(size.width / 2f, 0f),
                                radius = maxOf(size.width, size.height) * 0.9f
                            )
                            onDrawBehind {
                                drawRect(brush = sheen, size = Size(size.width, size.height * 0.5f))
                            }
                        }
                        .border(width = 1.2.dp, color = borderColor, shape = composerShape)
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
            .semantics {
                contentDescription = when (action) {
                    ComposerAction.STOP -> "Stop generating"
                    ComposerAction.SEND -> "Send message"
                    ComposerAction.MIC -> "Voice input"
                }
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
                    .size(46.dp)
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
    val view = LocalView.current
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
            onClick = {
                if (!flashed) {
                    FridayHaptics.tickLight(view, reducedMotion)
                    flashed = true
                }
            }
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
