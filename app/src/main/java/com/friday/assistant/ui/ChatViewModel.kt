package com.friday.assistant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.friday.assistant.ai.ApiInferenceService
import com.friday.assistant.ai.LiteRtLmService
import com.friday.assistant.data.ApiEndpointConfig
import com.friday.assistant.data.ChatMessage
import com.friday.assistant.data.ChatRole
import com.friday.assistant.data.ChatSession
import com.friday.assistant.data.InferenceMode
import com.friday.assistant.data.ModelStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val sessions: List<ChatSession> = listOf(ChatSession()),
    val activeSessionId: String = "",
    val input: String = "",
    val isGenerating: Boolean = false,
    val modelStatus: ModelStatus = ModelStatus(),
    val modelPathInput: String = "",
    val useGpu: Boolean = false,
    val settingsOpen: Boolean = false,
    val inferenceMode: InferenceMode = InferenceMode.LOCAL,
    val apiConfig: ApiEndpointConfig = ApiEndpointConfig()
) {
    val activeSession: ChatSession
        get() = sessions.firstOrNull { it.id == activeSessionId } ?: sessions.first()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val localService = LiteRtLmService(application.applicationContext)
    private val apiService = ApiInferenceService(application.applicationContext)
    private var generationJob: Job? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        val initial = ChatSession(
            title = "New session",
            messages = listOf(
                ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = "Hi, I'm FRIDAY. I can run on-device with LiteRT-LM or use a remote API endpoint you configure in Settings."
                )
            )
        )
        val savedApi = apiService.loadConfig()
        _uiState.value = ChatUiState(
            sessions = listOf(initial),
            activeSessionId = initial.id,
            modelStatus = ModelStatus(statusText = "Preparing bundled model..."),
            apiConfig = savedApi
        )
        viewModelScope.launch {
            val discovered = LiteRtLmService.discoverDefaultModelPath(getApplication<Application>().applicationContext).orEmpty()
            _uiState.update { it.copy(modelPathInput = discovered) }
            if (discovered.isNotBlank()) {
                _uiState.update { it.copy(modelStatus = localService.currentStatus(configuredPath = discovered, loading = true)) }
                val result = localService.loadModel(discovered, _uiState.value.useGpu)
                _uiState.update { state ->
                    state.copy(
                        modelStatus = result.getOrElse { e ->
                            localService.currentStatus(configuredPath = discovered, error = e.message ?: "Failed to load model")
                        }
                    )
                }
            } else {
                _uiState.update { it.copy(modelStatus = ModelStatus(statusText = "No local model loaded")) }
            }
        }
    }

    fun onInputChange(value: String) { _uiState.update { it.copy(input = value) } }
    fun toggleSettings(open: Boolean) { _uiState.update { it.copy(settingsOpen = open) } }
    fun onModelPathChange(value: String) { _uiState.update { it.copy(modelPathInput = value) } }
    fun onUseGpuChange(value: Boolean) { _uiState.update { it.copy(useGpu = value) } }

    fun onInferenceModeChange(mode: InferenceMode) { _uiState.update { it.copy(inferenceMode = mode) } }
    fun onApiBaseUrlChange(value: String) { updateApi { it.copy(baseUrl = value) } }
    fun onApiKeyChange(value: String) { updateApi { it.copy(apiKey = value) } }
    fun onApiModelChange(value: String) { updateApi { it.copy(model = value) } }

    private fun updateApi(transform: (ApiEndpointConfig) -> ApiEndpointConfig) {
        _uiState.update { state ->
            val next = transform(state.apiConfig)
            apiService.saveConfig(next)
            state.copy(apiConfig = next)
        }
    }

    fun saveApiConfig() { apiService.saveConfig(_uiState.value.apiConfig) }

    fun newSession() {
        val session = ChatSession(
            title = "New session",
            messages = listOf(ChatMessage(role = ChatRole.ASSISTANT, content = "New session started. How can I help?"))
        )
        _uiState.update { state ->
            state.copy(sessions = listOf(session) + state.sessions, activeSessionId = session.id, input = "", settingsOpen = false)
        }
    }

    fun selectSession(id: String) { _uiState.update { it.copy(activeSessionId = id, settingsOpen = false) } }

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
            _uiState.update { it.copy(modelStatus = localService.currentStatus(error = "Enter a .litertlm model path.")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(modelStatus = localService.currentStatus(configuredPath = path, loading = true)) }
            val result = localService.loadModel(path, _uiState.value.useGpu)
            _uiState.update { state ->
                state.copy(
                    modelStatus = result.getOrElse { e ->
                        localService.currentStatus(configuredPath = path, error = e.message ?: "Failed to load model")
                    }
                )
            }
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            val status = localService.unload()
            _uiState.update { it.copy(modelStatus = status.copy(configuredPath = it.modelPathInput)) }
        }
    }

    fun sendMessage() {
        val prompt = _uiState.value.input.trim()
        if (prompt.isBlank() || _uiState.value.isGenerating) return

        val sessionId = _uiState.value.activeSessionId
        val history = _uiState.value.activeSession.messages
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
            val mode = _uiState.value.inferenceMode
            val apiConfig = _uiState.value.apiConfig
            val localStatus = _uiState.value.modelStatus
            try {
                when {
                    mode == InferenceMode.REMOTE_API -> {
                        if (!apiConfig.isConfigured) {
                            appendToAssistant(sessionId, assistantId, "Remote API mode is selected but no endpoint is configured. Open Settings and add an API base URL (and optional key/model).", done = true, error = true)
                        } else {
                            val reply = apiService.generateReply(prompt, history, apiConfig)
                            appendToAssistant(sessionId, assistantId, reply.ifBlank { "(empty response)" }, done = true, error = false)
                        }
                    }
                    localStatus.isReady -> {
                        val buffer = StringBuilder()
                        localService.generateReply(prompt).collect { chunk ->
                            buffer.append(chunk)
                            appendToAssistant(sessionId, assistantId, buffer.toString(), done = false, error = false)
                        }
                        appendToAssistant(sessionId, assistantId, buffer.toString().ifBlank { "(empty response)" }, done = true, error = false)
                    }
                    else -> {
                        val fallback = buildFallbackReply(prompt, localStatus, apiConfig)
                        appendToAssistant(sessionId, assistantId, fallback, done = true, error = false)
                    }
                }
            } catch (e: Exception) {
                val msg = "Error: ${e.message ?: "generation failed"}"
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

    private fun buildFallbackReply(prompt: String, status: ModelStatus, api: ApiEndpointConfig): String {
        val configured = status.configuredPath.ifBlank { "(not set)" }
        return buildString {
            append("Local on-device model is not loaded, so I can't generate an on-device response yet.\n\n")
            append("Options:\n")
            append("1. Load a .litertlm model in Settings (bundled or local path).\n")
            append("2. Switch to Remote API mode in Settings and add an endpoint URL (OpenAI-compatible or custom) so FRIDAY can answer via your API.\n\n")
            append("Configured local path: ")
            append(configured)
            if (api.isConfigured) {
                append("\nRemote endpoint: ")
                append(api.baseUrl)
            }
            status.lastError?.let {
                append("\nLast local error: ")
                append(it)
            }
        }
    }

    override fun onCleared() {
        generationJob?.cancel()
        viewModelScope.launch { localService.unload() }
        super.onCleared()
    }
}
