package com.friday.assistant.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.DrawerValue
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FridayApp(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !state.settingsOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
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
        when {
            state.settingsOpen && state.chatsStorageOpen -> {
                ChatsStorageScreen(
                    state = state,
                    onBack = { viewModel.openChatsStorage(false) },
                    onClearCurrentChat = viewModel::clearCurrentSession,
                    onClearAllChats = viewModel::clearAllChats,
                    onExportChats = viewModel::exportChatsJson,
                    onImportChats = viewModel::importChatsJson,
                    onRenameSession = viewModel::renameSession
                )
            }
            state.settingsOpen && state.personaMemoryOpen -> {
                PersonaMemoryScreen(
                    state = state,
                    onBack = { viewModel.openPersonaMemory(false) },
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
            state.settingsOpen -> {
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
                    onModelDownloadUrlChange = viewModel::onModelDownloadUrlChange,
                    onDownloadModel = viewModel::downloadModel,
                    onTestApi = viewModel::testApiEndpoint,
                    onRefreshModels = viewModel::refreshAvailableModels
                )
            }
            else -> {
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
                    onEditMessage = viewModel::startEditMessage
                )
            }
        }
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
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
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
                    onDelete = { onDeleteSession(session.id) }
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
    val bg = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onSelect() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
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
    onEditMessage: (String) -> Unit
) {
    val session = state.activeSession
    val listState = rememberLazyListState()
    val messages = session?.messages.orEmpty()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(session?.id, messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding().displayCutoutPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session?.title ?: "F.R.I.D.A.Y",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (session == null) FontWeight.Bold else FontWeight.SemiBold,
                                fontFamily = if (session == null) FridayDisplayFont else FontFamily.Default
                            ),
                            color = if (session == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val mode = when {
                            state.inferenceMode == InferenceMode.REMOTE_API && state.apiConfig.isConfigured -> "Remote API"
                            state.inferenceMode == InferenceMode.REMOTE_API -> "Remote API (not configured)"
                            state.modelStatus.isReady -> "LiteRT-LM local"
                            else -> "Local fallback"
                        }
                        Text(
                            text = "$mode • ${formatMetrics(state.metrics, state.isGenerating)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (session != null) {
                        IconButton(onClick = onRegenerate, enabled = !state.isGenerating) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onNew) {
                        Icon(Icons.Default.Add, contentDescription = "New", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        bottomBar = {
            if (session != null) {
                ComposerBar(
                    value = state.input,
                    isGenerating = state.isGenerating,
                    onValueChange = onInputChange,
                    onSend = onSend,
                    onStop = onStop
                )
            }
        }
    ) { padding ->
        if (session == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.friday_logo_blue),
                        contentDescription = "F.R.I.D.A.Y logo",
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "F.R.I.D.A.Y",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FridayDisplayFont),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No chats yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Create a new chat to start a conversation with F.R.I.D.A.Y.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    ActionButton(text = "Create new chat", primary = true, enabled = true, onClick = onNew)
                }
            }
        } else {
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
                    MessageRow(
                        message = msg,
                        onCopy = {
                            clipboard.setText(AnnotatedString(msg.content))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        onEdit = if (msg.role == ChatRole.USER && !state.isGenerating) { { onEditMessage(msg.id); Toast.makeText(context, "Editing message", Toast.LENGTH_SHORT).show() } } else null
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(message: ChatMessage, onCopy: () -> Unit, onEdit: (() -> Unit)? = null) {
    val isUser = message.role == ChatRole.USER
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 8.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        val bg = when {
            isUser -> MaterialTheme.colorScheme.primaryContainer
            message.isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.surface
        }
        val border = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(14.dp))
                .combinedClickable(onClick = { if (isUser) onEdit?.invoke() }, onLongClick = onCopy)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (!isUser) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "F.R.I.D.A.Y",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FridayDisplayFont),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    if (message.content.isNotBlank() && !message.isStreaming) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp).clickable { onCopy() }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            } else if (onEdit != null) {
                Text(
                    text = "Tap to edit & resend â€¢ long press to copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }
            val display = if (message.content.isBlank() && message.isStreaming) "Thinking..." else message.content
            if (!isUser && !message.isError && display.isNotBlank() && !message.isStreaming) {
                Markdown(
                    content = display,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = display,
                    color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFamily = if (!isUser && (display.contains("```") || display.contains("\n    "))) FontFamily.Monospace else FontFamily.Default)
                )
            }
            if (message.isStreaming && message.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("â— streaming", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val model = m.modelName.takeIf { it.isNotBlank() }
    val parts = listOfNotNull(if (generating) "generating" else null, model, ttft, total, tps, cps, out, chunks)
    return if (parts.isEmpty()) "ready" else parts.joinToString(" â€¢ ")
}

@Composable
private fun ComposerBar(
    value: String,
    isGenerating: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (value.isBlank()) {
                    Text(
                        text = if (isGenerating) "Generating..." else "Message F.R.I.D.A.Y...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = !isGenerating,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, lineHeight = 20.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6
                )
            }
            Spacer(Modifier.width(8.dp))
            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .clickable { onStop() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                val canSend = value.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = canSend) { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

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
    onModelDownloadUrlChange: (String) -> Unit,
    onDownloadModel: () -> Unit,
    onTestApi: () -> Unit,
    onRefreshModels: () -> Unit
) {
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
                item { SettingsTextField("Endpoint URL", state.apiConfig.baseUrl, "https://api.example.com/v1", onApiBaseUrlChange) }
                item { SettingsTextField("API key (optional)", state.apiConfig.apiKey, "sk-...", onApiKeyChange, masked = true) }
                item { SettingsTextField("Model (optional)", state.apiConfig.model, "gpt-4o-mini / model-name", onApiModelChange) }
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
                item { SettingsTextField("Model path", state.modelPathInput, "/storage/.../models/model.litertlm", onPathChange, mono = true) }
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
                    SettingsTextField("Download model URL (.litertlm)", state.modelDownloadUrl, "https://.../model.litertlm", onModelDownloadUrlChange, mono = true)
                    Spacer(Modifier.height(8.dp))
                    ActionButton("Download model", primary = true, enabled = state.modelDownloadUrl.isNotBlank()) { onDownloadModel() }
                    state.busyMessage?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
    var newMemoryText by remember { mutableStateOf("") }
    var editingMemoryId by remember { mutableStateOf<String?>(null) }
    var editingMemoryText by remember { mutableStateOf("") }

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
                    value = state.personaConfig.systemPrompt,
                    placeholder = PersonaConfig.DEFAULT_PERSONA_PROMPT,
                    onChange = onPersonaPromptChange
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("Reset to default", primary = false, enabled = state.personaConfig.systemPrompt != PersonaConfig.DEFAULT_PERSONA_PROMPT) {
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
                        onClearMemories()
                        Toast.makeText(context, "Memories cleared", Toast.LENGTH_SHORT).show()
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
    var renameText by remember(activeSession?.id, activeSession?.title) { mutableStateOf(activeSession?.title ?: "") }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            val ok = !text.isNullOrBlank() && onImportChats(text)
            Toast.makeText(context, if (ok) "Chats imported" else "Import failed (invalid JSON file)", Toast.LENGTH_SHORT).show()
        }
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
                    SettingsTextField("Rename current session", renameText, "Session title", { renameText = it })
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
                        onClearCurrentChat()
                        Toast.makeText(context, "Current chat cleared", Toast.LENGTH_SHORT).show()
                    }
                    ActionButton("Clear all chats", primary = false, enabled = state.sessions.isNotEmpty()) {
                        onClearAllChats()
                        Toast.makeText(context, "All chats cleared", Toast.LENGTH_SHORT).show()
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
private fun SettingsTextField(label: String, value: String, placeholder: String, onChange: (String) -> Unit, mono: Boolean = false, masked: Boolean = false) {
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
            Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
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
