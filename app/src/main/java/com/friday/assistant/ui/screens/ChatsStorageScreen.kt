package com.friday.assistant.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.ui.ChatUiState
import com.friday.assistant.ui.components.ObsidianSurface
import com.friday.assistant.ui.theme.FridayDisplayFont
import com.friday.assistant.ui.theme.ObsidianSurfaceGlass
import com.friday.assistant.ui.theme.ObsidianVoid
import com.friday.assistant.ui.theme.SpecularHighlightStrong
import com.friday.assistant.ui.theme.TextPrimary
import com.friday.assistant.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Chats & storage sub-page: session identity (rename), data controls
 * (clear current / clear all with dialogs) and JSON export/import, all on
 * anodized obsidian glass.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatsStorageScreen(
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
        containerColor = ObsidianVoid,
        topBar = {
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "CHATS & STORAGE",
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SettingsSectionHeader("SESSION IDENTITY") }
            item {
                ObsidianSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Chats are saved locally on device (durable app storage) and restored on launch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        if (activeSession != null) {
                            SettingsTextField("Rename current session", renameText, "Session title", { renameText = it }, singleLine = true)
                            Spacer(Modifier.height(10.dp))
                            ActionButton("Save title", primary = true, enabled = renameText.isNotBlank()) {
                                onRenameSession(activeSession.id, renameText)
                                Toast.makeText(context, "Session renamed", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Text(
                                "No active chat session to rename. Create a new chat from the main screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            item { SettingsSectionHeader("DATA CONTROL") }
            item {
                ObsidianSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Select a .json file previously exported from FRIDAY to import chats.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
