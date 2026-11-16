package com.friday.assistant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.data.PersonaConfig
import com.friday.assistant.ui.ChatUiState
import com.friday.assistant.ui.components.ObsidianSurface
import com.friday.assistant.ui.theme.CyanBorderGlow
import com.friday.assistant.ui.theme.FridayDisplayFont
import com.friday.assistant.ui.theme.ObsidianSurfaceElevated
import com.friday.assistant.ui.theme.ObsidianSurfaceGlass
import com.friday.assistant.ui.theme.ObsidianVoid
import com.friday.assistant.ui.theme.SpecularHighlightStrong
import com.friday.assistant.ui.theme.SpecularHighlightSubtle
import com.friday.assistant.ui.theme.StarkCyanPrimary
import com.friday.assistant.ui.theme.TextPrimary
import com.friday.assistant.ui.theme.TextSecondary
import com.friday.assistant.ui.theme.TextTertiary

/**
 * Persona & memory sub-page: persona protocol card (system prompt editing),
 * memory-lattice controls (persistent memories switch, manual add/edit,
 * extraction) and saved-memory cards, all on anodized obsidian glass.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PersonaMemoryScreen(
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
                        "PERSONA & MEMORY",
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
            item { SettingsSectionHeader("PERSONA PROTOCOL") }
            item {
                ObsidianSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Persona / behavior prompt",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "This system prompt is applied to local and remote responses to shape how F.R.I.D.A.Y behaves.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        SettingsTextField(
                            label = "System prompt",
                            value = personaDraft,
                            placeholder = PersonaConfig.DEFAULT_PERSONA_PROMPT,
                            onChange = { personaDraft = it },
                            minLines = 3,
                            maxLines = 10
                        )
                        Spacer(Modifier.height(10.dp))
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
                }
            }

            item { SettingsSectionHeader("MEMORY LATTICE") }
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
                            Text("Use persistent memories", color = TextPrimary)
                            Text(
                                "When enabled, saved memories are included as context in responses.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(checked = state.personaConfig.useMemories, onCheckedChange = onUseMemoriesChange)
                    }
                }
            }
            item {
                ObsidianSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Persistent memory",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Store user preferences, facts, and context. You can add/edit manually or extract from the current chat using the active model.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
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
                            Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
            item {
                ObsidianSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val isEditing = editingMemoryId != null
                        SettingsTextField(
                            label = if (isEditing) "Edit memory" else "Add memory",
                            value = if (isEditing) editingMemoryText else newMemoryText,
                            placeholder = "e.g. Prefers concise answers and dark mode.",
                            onChange = { if (isEditing) editingMemoryText = it else newMemoryText = it }
                        )
                        Spacer(Modifier.height(10.dp))
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
                }
            }

            item {
                SettingsSectionHeader("SAVED MEMORIES (${state.memories.size})")
            }

            if (state.memories.isEmpty()) {
                item {
                    Text(
                        "No memories saved yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } else {
                items(state.memories, key = { it.id }) { mem ->
                    ObsidianSurface(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                mem.content,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (mem.source == "extracted") "Extracted memory" else "Manual memory",
                                color = if (mem.source == "extracted") StarkCyanPrimary else TextTertiary,
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
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
