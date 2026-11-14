package com.friday.assistant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.friday.assistant.ai.LiteRtLmService
import com.friday.assistant.data.ChatMessage
import com.friday.assistant.data.ChatRole
import com.friday.assistant.data.ChatSession
import com.friday.assistant.data.ModelStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ChatUiState(
    val sessions: List<ChatSession> = listOf(ChatSession()),
    val activeSessionId: String = "",
    val input: String = "",
    val isGenerating: Boolean = false,
    val modelStatus: ModelStatus = ModelStatus(),
    val modelPathInput: String = "",
    val useGpu: Boolean = false,
    val settingsOpen: Boolean = false
) {
    val activeSession: ChatSession
        get() = sessions.firstOrNull { it.id == activeSessionId } ?: sessions.first()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val service = LiteRtLmService(application.applicationContext)
    private var generationJob: Job? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        val initial = ChatSession(
            title = "New session",
            messages = listOf(
                ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = "Hi, I'm FRIDAY. This build uses an OpenCode-style chat UI and can run on-device inference with LiteRT-LM when a .litertlm model is loaded."
                )
            )
        )
        val discovered = LiteRtLmService.discoverDefaultModelPath(application.applicationContext).orEmpty()
        _uiState.value = ChatUiState(
            sessions = listOf(initial),
            activeSessionId = initial.id,
            modelPathInput = discovered,
            modelStatus = ModelStatus(
                configuredPath = discovered,
                statusText = if (discovered.isNotBlank()) "Model found. Tap Load model." else "No local model loaded"
            )
        )
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun toggleSettings(open: Boolean) {
        _uiState.update { it.copy(settingsOpen = open) }
    }

    fun onModelPathChange(value: String) {
        _uiState.update { it.copy(modelPathInput = value) }
    }

    fun onUseGpuChange(value: Boolean) {
        _uiState.update { it.copy(useGpu = value) }
    }

    fun newSession() {
        val session = ChatSession(
            title = "New session",
            messages = listOf(
                ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = "New session started. How can I help?"
                )
            )
        )
        _uiState.update { state ->
            state.copy(
                sessions = listOf(session) + state.sessions,
                activeSessionId = session.id,
                input = "",
                settingsOpen = false
            )
        }
    }

    fun selectSession(id: String) {
        _uiState.update { it.copy(activeSessionId = id, settingsOpen = false) }
    }

    fun deleteSession(id: String) {
        _uiState.update { state ->
            val remaining = state.sessions.filterNot { it.id == id }
            if (remaining.isEmpty()) {
                val fresh = ChatSession()
                state.copy(sessions = listOf(fresh), activeSessionId = fresh.id)
            } else {
                val active = if (state.activeSessionId == id) remaining.first().id else state.activeSessionId
                state.copy(sessions = remaining, activeSessionId = active)
            }
        }
    }

    fun loadModel() {
        val path = _uiState.value.modelPathInput.trim()
        if (path.isBlank()) {
            _uiState.update { it.copy(modelStatus = service.currentStatus(error = "Enter a .litertlm model path.")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(modelStatus = service.currentStatus(configuredPath = path, loading = true)) }
            val result = service.loadModel(path, _uiState.value.useGpu)
            _uiState.update { state ->
                state.copy(
                    modelStatus = result.getOrElse { e ->
                        service.currentStatus(configuredPath = path, error = e.message ?: "Failed to load model")
                    }
                )
            }
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            val status = service.unload()
            _uiState.update { it.copy(modelStatus = status.copy(configuredPath = it.modelPathInput)) }
        }
    }

    fun sendMessage() {
        val prompt = _uiState.value.input.trim()
        if (prompt.isBlank() || _uiState.value.isGenerating) return

        val sessionId = _uiState.value.activeSessionId
        val userMsg = ChatMessage(role = ChatRole.USER, content = prompt)
        val assistantId = java.util.UUID.randomUUID().toString()
        val placeholder = ChatMessage(id = assistantId, role = ChatRole.ASSISTANT, content = "", isStreaming = true)

        _uiState.update { state ->
            state.copy(
                input = "",
                isGenerating = true,
                sessions = state.sessions.map { session ->
                    if (session.id == sessionId) {
                        val title = if (session.messages.count { it.role == ChatRole.USER } == 0)
                            prompt.take(32).ifBlank { session.title } else session.title
                        session.copy(
                            title = title,
                            messages = session.messages + userMsg + placeholder,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else session
                }
            )
        }

        generationJob = viewModelScope.launch {
            val status = _uiState.value.modelStatus
            if (!status.isReady) {
                val fallback = buildFallbackReply(prompt, status)
                appendToAssistant(sessionId, assistantId, fallback, done = true, error = false)
                _uiState.update { it.copy(isGenerating = false) }
                return@launch
            }

            val buffer = StringBuilder()
            try {
                service.generateReply(prompt).collect { chunk ->
                    buffer.append(chunk)
                    appendToAssistant(sessionId, assistantId, buffer.toString(), done = false, error = false)
                }
                appendToAssistant(sessionId, assistantId, buffer.toString().ifBlank { "(empty response)" }, done = true, error = false)
            } catch (e: Exception) {
                val msg = if (buffer.isNotEmpty()) buffer.toString() + "\n\n[Error: ${e.message ?: "generation failed"}]"
                else "Error: ${e.message ?: "generation failed"}"
                appendToAssistant(sessionId, assistantId, msg, done = true, error = true)
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    private fun appendToAssistant(sessionId: String, messageId: String, content: String, done: Boolean, error: Boolean) {
        _uiState.update { state ->
            state.copy(
                sessions = state.sessions.map { session ->
                    if (session.id == sessionId) {
                        session.copy(
                            messages = session.messages.map { m ->
                                if (m.id == messageId) m.copy(content = content, isStreaming = !done, isError = error) else m
                            },
                            updatedAt = System.currentTimeMillis()
                        )
                    } else session
                }
            )
        }
    }

    private fun buildFallbackReply(prompt: String, status: ModelStatus): String {
        val p = prompt.trim()
        val lower = p.lowercase()
        val base = when {
            lower.contains("time") -> "I can't access system tools in this build yet, but I can answer once a local LiteRT-LM model is loaded."
            lower.contains("hello") || lower.contains("hi") -> "Hello. I'm FRIDAY (baseline). Load a .litertlm model in Settings for on-device AI responses."
            lower.contains("who are you") -> "I'm FRIDAY, a local-first Android assistant scaffold inspired by OpenCode-style chat UI."
            else -> "Local on-device model is not loaded, so I can't generate an AI response yet."
        }
        val hint = buildString {
            append(base)
            append("\n\nTo enable LiteRT-LM inference:\n")
            append("1. Download a compatible .litertlm model (e.g. Gemma from litert-community).\n")
            append("2. Place it in app storage: Android/data/com.friday.assistant/files/models/\n")
            append("3. Set the model path in Settings and tap Load model.")
            val configured = status.configuredPath.ifBlank { "(not set)" }
            append("\n\nConfigured path: ")
            append(configured)
            status.lastError?.let {
                append("\nLast error: ")
                append(it)
            }
        }
        return hint
    }

    override fun onCleared() {
        generationJob?.cancel()
        viewModelScope.launch { service.unload() }
        super.onCleared()
    }
}
