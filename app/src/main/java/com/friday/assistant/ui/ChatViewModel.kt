package com.friday.assistant.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.friday.assistant.ai.ApiInferenceService
import com.friday.assistant.ai.LiteRtLmService
import com.friday.assistant.data.ApiEndpointConfig
import com.friday.assistant.data.ChatMessage
import com.friday.assistant.data.ChatRole
import com.friday.assistant.data.ChatSession
import com.friday.assistant.data.ChatStore
import com.friday.assistant.data.GenerationMetrics
import com.friday.assistant.data.InferenceMode
import com.friday.assistant.data.MemoryItem
import com.friday.assistant.data.MemoryStore
import com.friday.assistant.data.ModelStatus
import com.friday.assistant.data.PersonaConfig
import com.friday.assistant.speech.TtsService
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

data class DiagnosticsInfo(
    val device: String = "",
    val androidVersion: String = "",
    val abi: String = "",
    val appVersion: String = "",
    val inferenceMode: String = "",
    val modelFile: String = "",
    val modelLoadMs: Long? = null,
    val lastGenerationMs: Long? = null,
    val lastGenerationError: String? = null,
    val sessionCount: Int = 0,
    val messageCount: Int = 0,
    val ttsReady: Boolean = false
)

data class ChatUiState(
    val sessions: List<ChatSession> = emptyList(),
    val activeSessionId: String = "",
    val input: String = "",
    val isGenerating: Boolean = false,
    val modelStatus: ModelStatus = ModelStatus(),
    val modelPathInput: String = "",
    val useGpu: Boolean = false,
    val settingsOpen: Boolean = false,
    val chatsStorageOpen: Boolean = false,
    val personaMemoryOpen: Boolean = false,
    val inferenceMode: InferenceMode = InferenceMode.LOCAL,
    val apiConfig: ApiEndpointConfig = ApiEndpointConfig(),
    val metrics: GenerationMetrics = GenerationMetrics(),
    val editingMessageId: String? = null,
    val availableModels: List<String> = emptyList(),
    val apiTestMessage: String? = null,
    val personaConfig: PersonaConfig = PersonaConfig(),
    val memories: List<MemoryItem> = emptyList(),
    val memoryBusyMessage: String? = null,
    val isMemoryBusy: Boolean = false,
    val speakReplies: Boolean = false,
    val speakingMessageId: String? = null,
    val diagnostics: DiagnosticsInfo = DiagnosticsInfo(),
    val bundledCopyProgress: LiteRtLmService.BundledCopyProgress? = null
) {
    val activeSession: ChatSession?
        get() = sessions.firstOrNull { it.id == activeSessionId } ?: sessions.firstOrNull()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val appPrefs = application.applicationContext.getSharedPreferences("friday_prefs", Application.MODE_PRIVATE)
    private val localService = LiteRtLmService(application.applicationContext)
    private val apiService = ApiInferenceService(application.applicationContext)
    private val chatStore = ChatStore(application.applicationContext)
    private val memoryStore = MemoryStore(application.applicationContext)
    private val tts = TtsService(application.applicationContext)
    private var generationJob: Job? = null
    private val generationId = AtomicLong(0L)
    @Volatile private var activeGenerationId: Long = 0L
    @Volatile private var lastGenerationError: String? = null
    @Volatile private var lastGenerationMs: Long? = null
    @Volatile private var lastModelLoadMs: Long? = null
    @Volatile private var speakingMessageId: String? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        val savedApi = apiService.loadConfig()
        val savedMode = runCatching { InferenceMode.valueOf(appPrefs.getString("inference_mode", InferenceMode.LOCAL.name) ?: InferenceMode.LOCAL.name) }
            .getOrDefault(InferenceMode.LOCAL)
        val savedPathPref = appPrefs.getString("model_path", "") ?: ""
        val savedUseGpu = appPrefs.getBoolean("use_gpu", false)
        val savedSpeakReplies = appPrefs.getBoolean("speak_replies", false)
        val (savedSessions, savedActiveId) = chatStore.load()
        val memorySnapshot = memoryStore.load()
        val sessions = if (savedSessions.isNotEmpty()) {
            savedSessions.map { s -> s.copy(messages = s.messages.map { it.copy(isStreaming = false) }) }
        } else {
            emptyList()
        }
        val activeId = sessions.firstOrNull { it.id == savedActiveId }?.id ?: sessions.firstOrNull()?.id.orEmpty()
        Log.i("FRIDAY", "VM: init mode=${savedMode.name} speakReplies=$savedSpeakReplies sessions=${sessions.size}")
        _uiState.value = ChatUiState(
            sessions = sessions,
            activeSessionId = activeId,
            modelStatus = ModelStatus(statusText = "Preparing bundled model..."),
            apiConfig = savedApi,
            inferenceMode = savedMode,
            modelPathInput = savedPathPref,
            useGpu = savedUseGpu,
            personaConfig = memorySnapshot.persona,
            memories = memorySnapshot.memories,
            speakReplies = savedSpeakReplies
        )
        pushDiagnostics()
        tts.onSpeakingChanged = { speaking -> if (!speaking) setSpeakingMessage(null) }
        viewModelScope.launch {
            val discovered = try {
                LiteRtLmService.discoverDefaultModelPath(getApplication<Application>().applicationContext) { progress ->
                    _uiState.update { it.copy(bundledCopyProgress = progress, modelStatus = ModelStatus(statusText = "Preparing bundled models...")) }
                }.orEmpty()
            } catch (e: Exception) {
                Log.w("FRIDAY", "MODEL: bundled copy failed: ${e.message}")
                _uiState.update { it.copy(bundledCopyProgress = null, modelStatus = ModelStatus(statusText = "Failed to prepare bundled models", lastError = e.message)) }
                ""
            }
            _uiState.update { it.copy(bundledCopyProgress = null) }
            val chosen = savedPathPref.ifBlank { discovered }
            _uiState.update { it.copy(modelPathInput = chosen) }
            refreshAvailableModels()
            if (chosen.isNotBlank()) {
                _uiState.update { it.copy(modelStatus = localService.currentStatus(configuredPath = chosen, loading = true)) }
                val t0 = System.currentTimeMillis()
                val result = localService.loadModel(chosen, savedUseGpu)
                lastModelLoadMs = System.currentTimeMillis() - t0
                Log.i("FRIDAY", "MODEL: startup load ${result.fold(onSuccess = { "ok" }, onFailure = { "failed: ${it.message}" })} in $lastModelLoadMs ms (${chosen.substringAfterLast('/')})")
                _uiState.update { state ->
                    state.copy(
                        modelStatus = result.getOrElse { e ->
                            localService.currentStatus(configuredPath = chosen, error = e.message ?: "Failed to load model")
                        }
                    )
                }
            } else {
                _uiState.update { it.copy(modelStatus = ModelStatus(statusText = "No local model loaded")) }
            }
            pushDiagnostics()
        }
    }

    fun onInputChange(value: String) { _uiState.update { it.copy(input = value) } }
    fun toggleSettings(open: Boolean) {
        _uiState.update { it.copy(settingsOpen = open, chatsStorageOpen = if (open) it.chatsStorageOpen else false, personaMemoryOpen = if (open) it.personaMemoryOpen else false) }
        if (open) pushDiagnostics()
    }
    fun openChatsStorage(open: Boolean) { _uiState.update { it.copy(chatsStorageOpen = open, personaMemoryOpen = false, settingsOpen = if (open) true else it.settingsOpen) } }
    fun openPersonaMemory(open: Boolean) { _uiState.update { it.copy(personaMemoryOpen = open, chatsStorageOpen = false, settingsOpen = if (open) true else it.settingsOpen) } }

    fun onInferenceModeChange(mode: InferenceMode) {
        _uiState.update { it.copy(inferenceMode = mode) }
        appPrefs.edit().putString("inference_mode", mode.name).apply()
    }
    fun onApiBaseUrlChange(value: String) { updateApi { it.copy(baseUrl = value) } }
    fun onApiKeyChange(value: String) { updateApi { it.copy(apiKey = value) } }
    fun onApiModelChange(value: String) { updateApi { it.copy(model = value) } }

    fun refreshAvailableModels() {
        viewModelScope.launch(Dispatchers.IO) {
            val models = LiteRtLmService.listAvailableModels(getApplication())
            _uiState.update { it.copy(availableModels = models) }
        }
    }

    fun testApiEndpoint() {
        viewModelScope.launch {
            _uiState.update { it.copy(apiTestMessage = "Testing endpoint...") }
            val res = runCatching { apiService.testEndpoint(_uiState.value.apiConfig) }
            _uiState.update { it.copy(apiTestMessage = res.getOrElse { "Test failed: ${it.message}" }) }
        }
    }

    fun onModelPathChange(value: String) {
        _uiState.update { it.copy(modelPathInput = value) }
        appPrefs.edit().putString("model_path", value).apply()
    }
    fun onUseGpuChange(value: Boolean) {
        _uiState.update { it.copy(useGpu = value) }
        appPrefs.edit().putBoolean("use_gpu", value).apply()
    }

    fun onSpeakRepliesChange(value: Boolean) {
        _uiState.update { it.copy(speakReplies = value) }
        appPrefs.edit().putBoolean("speak_replies", value).apply()
        if (!value) {
            tts.stop()
            setSpeakingMessage(null)
        }
        Log.i("FRIDAY", "TTS: speakReplies=$value")
    }

    fun speakMessage(messageId: String) {
        val msg = _uiState.value.activeSession?.messages?.firstOrNull { it.id == messageId } ?: return
        val text = msg.content.trim()
        if (text.isBlank()) return
        if (tts.isSpeaking() && speakingMessageId == messageId) {
            Log.i("FRIDAY", "TTS: stopped from speaker icon")
            tts.stop()
            setSpeakingMessage(null)
            return
        }
        Log.i("FRIDAY", "TTS: speak message ${text.length} chars")
        setSpeakingMessage(messageId)
        tts.speak(text)
    }

    private fun setSpeakingMessage(messageId: String?) {
        speakingMessageId = messageId
        _uiState.update { it.copy(speakingMessageId = messageId) }
    }

    private fun buildDiagnostics(): DiagnosticsInfo {
        val s = _uiState.value
        val app = getApplication<Application>()
        val pkg = runCatching { app.packageManager.getPackageInfo(app.packageName, 0) }.getOrNull()
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) pkg?.longVersionCode ?: 0L else (pkg?.versionCode ?: 0).toLong()
        return DiagnosticsInfo(
            device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            androidVersion = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})",
            abi = android.os.Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
            appVersion = "${pkg?.versionName ?: "?"} (versionCode $versionCode)",
            inferenceMode = if (s.inferenceMode == InferenceMode.REMOTE_API) "Remote API" else "Local (LiteRT-LM)",
            modelFile = s.modelStatus.resolvedPath?.substringAfterLast('/') ?: s.modelPathInput.substringAfterLast('/'),
            modelLoadMs = lastModelLoadMs,
            lastGenerationMs = lastGenerationMs,
            lastGenerationError = lastGenerationError,
            sessionCount = s.sessions.size,
            messageCount = s.sessions.sumOf { it.messages.size },
            ttsReady = tts.isReady()
        )
    }

    private fun pushDiagnostics() {
        _uiState.update { it.copy(diagnostics = buildDiagnostics()) }
    }

    private fun updateApi(transform: (ApiEndpointConfig) -> ApiEndpointConfig) {
        _uiState.update { state ->
            val next = transform(state.apiConfig)
            apiService.saveConfig(next)
            state.copy(apiConfig = next)
        }
    }

    fun saveApiConfig() { apiService.saveConfig(_uiState.value.apiConfig) }

    private val chatPersistMutex = Mutex()
    private var chatPersistJob: Job? = null
    private val memoryPersistMutex = Mutex()
    private var memoryPersistJob: Job? = null

    private fun persistChats() {
        chatPersistJob?.cancel()
        chatPersistJob = viewModelScope.launch(Dispatchers.IO) {
            chatPersistMutex.withLock {
                val snapshot = _uiState.value
                chatStore.save(snapshot.sessions, snapshot.activeSessionId)
            }
        }
    }

    private fun persistMemory() {
        memoryPersistJob?.cancel()
        memoryPersistJob = viewModelScope.launch(Dispatchers.IO) {
            memoryPersistMutex.withLock {
                val snapshot = _uiState.value
                memoryStore.save(com.friday.assistant.data.MemorySnapshot(snapshot.personaConfig, snapshot.memories))
            }
        }
    }

    fun onPersonaPromptChange(value: String) {
        _uiState.update { it.copy(personaConfig = it.personaConfig.copy(systemPrompt = value)) }
        persistMemory()
    }
    fun resetPersonaPrompt() {
        _uiState.update { it.copy(personaConfig = it.personaConfig.copy(systemPrompt = PersonaConfig.DEFAULT_PERSONA_PROMPT)) }
        persistMemory()
    }
    fun onUseMemoriesChange(value: Boolean) {
        _uiState.update { it.copy(personaConfig = it.personaConfig.copy(useMemories = value)) }
        persistMemory()
    }
    fun addMemory(content: String) {
        val text = content.trim()
        if (text.isBlank()) return
        _uiState.update { state ->
            state.copy(memories = listOf(MemoryItem(content = text.take(1000))) + state.memories)
        }
        persistMemory()
    }
    fun updateMemory(id: String, content: String) {
        val text = content.trim()
        if (text.isBlank()) return
        _uiState.update { state ->
            state.copy(memories = state.memories.map { if (it.id == id) it.copy(content = text.take(1000), updatedAt = System.currentTimeMillis()) else it })
        }
        persistMemory()
    }
    fun deleteMemory(id: String) {
        _uiState.update { state -> state.copy(memories = state.memories.filterNot { it.id == id }) }
        persistMemory()
    }
    fun clearMemories() {
        _uiState.update { it.copy(memories = emptyList()) }
        persistMemory()
    }

    fun extractMemoriesFromCurrentChat() {
        if (_uiState.value.isGenerating || _uiState.value.isMemoryBusy) return
        val session = _uiState.value.activeSession ?: run {
            _uiState.update { it.copy(memoryBusyMessage = "No active chat to extract memories from.") }
            return
        }
        val transcript = session.messages
            .filter { (it.role == ChatRole.USER || it.role == ChatRole.ASSISTANT) && it.content.isNotBlank() && it.content != "Thinking..." }
            .takeLast(20)
            .joinToString("\n") { m ->
                val role = if (m.role == ChatRole.USER) "User" else "Assistant"
                "$role: ${m.content.trim().take(700)}"
            }
        if (transcript.isBlank()) {
            _uiState.update { it.copy(memoryBusyMessage = "Current chat has no messages to extract from.") }
            return
        }

        val mode = _uiState.value.inferenceMode
        val apiConfig = _uiState.value.apiConfig
        val localStatus = _uiState.value.modelStatus
        if (mode == InferenceMode.REMOTE_API && !apiConfig.isConfigured) {
            _uiState.update { it.copy(memoryBusyMessage = "Configure Remote API in Settings to extract memories.") }
            return
        }
        if (mode == InferenceMode.LOCAL && !localStatus.isReady) {
            _uiState.update { it.copy(memoryBusyMessage = "Load a local model in Settings to extract memories.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMemoryBusy = true, memoryBusyMessage = "Extracting memories...") }
            try {
                val prompt = buildString {
                    append("Extract durable, useful memories from this chat (user preferences, facts about the user, ongoing goals/projects, important context). ")
                    append("Return ONLY a bullet list, one memory per line starting with '- '. ")
                    append("No intro, no outro, no duplicates, max 8 items. If none, return '- (none)'.\n\n")
                    append("Chat transcript:\n")
                    append(transcript)
                }
                val extractionSystem = "You extract concise persistent memories from conversations. Output only bullet points."
                val reply = when (mode) {
                    InferenceMode.REMOTE_API -> apiService.generateReply(prompt, emptyList(), apiConfig, extractionSystem, emptyList())
                    InferenceMode.LOCAL -> {
                        var latest = ""
                        localService.generateReply(prompt, emptyList(), extractionSystem, emptyList()).collect { latest = it }
                        latest
                    }
                }
                val parsed = parseExtractedMemories(reply)
                if (parsed.isEmpty()) {
                    _uiState.update { it.copy(isMemoryBusy = false, memoryBusyMessage = "No new memories extracted.") }
                } else {
                    val existing = _uiState.value.memories.map { it.content.trim().lowercase() }.toSet()
                    val newItems = parsed.filterNot { existing.contains(it.trim().lowercase()) }
                        .map { MemoryItem(content = it, source = "extracted") }
                    _uiState.update { state ->
                        state.copy(
                            isMemoryBusy = false,
                            memories = newItems + state.memories,
                            memoryBusyMessage = if (newItems.isEmpty()) "No new memories added (duplicates)." else "Added ${newItems.size} memory item(s)."
                        )
                    }
                    persistMemory()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isMemoryBusy = false, memoryBusyMessage = "Memory extraction failed: ${e.message ?: "unknown error"}") }
            }
        }
    }

    private fun parseExtractedMemories(text: String): List<String> {
        val lines = text.lineSequence().map { it.trim() }.toList()
        val bullets = lines.mapNotNull { line ->
            when {
                line.startsWith("- ") -> line.removePrefix("- ").trim()
                line.startsWith("* ") -> line.removePrefix("* ").trim()
                Regex("^\\d+[.)]\\s+").containsMatchIn(line) -> line.replace(Regex("^\\d+[.)]\\s+"), "").trim()
                else -> null
            }
        }.filter { it.isNotBlank() && !it.equals("(none)", ignoreCase = true) && !it.equals("none", ignoreCase = true) }
            .map { it.take(300) }
        if (bullets.isNotEmpty()) return bullets.distinct().take(8)
        // fallback: take short non-empty lines if model didn't follow format
        return lines.filter { it.isNotBlank() && it.length <= 200 && !it.equals("(none)", ignoreCase = true) }
            .take(5).distinct()
    }

    fun newSession() {
        if (_uiState.value.isGenerating) stopGeneration()
        val session = ChatSession(
            title = "New session",
            messages = listOf(ChatMessage(role = ChatRole.ASSISTANT, content = "New session started. How can I help?"))
        )
        _uiState.update { state ->
            state.copy(sessions = listOf(session) + state.sessions, activeSessionId = session.id, input = "", settingsOpen = false, chatsStorageOpen = false, editingMessageId = null)
        }
        persistChats()
        pushDiagnostics()
        if (_uiState.value.speakReplies) {
            setSpeakingMessage(session.messages.first().id)
            tts.speak("New session started. How can I help?")
        }
    }

    fun selectSession(id: String) {
        if (_uiState.value.isGenerating) stopGeneration()
        _uiState.update { it.copy(activeSessionId = id, settingsOpen = false, chatsStorageOpen = false, editingMessageId = null) }
        persistChats()
    }

    fun deleteSession(id: String) {
        if (_uiState.value.activeSessionId == id && _uiState.value.isGenerating) stopGeneration()
        _uiState.update { state ->
            val remaining = state.sessions.filterNot { it.id == id }
            if (remaining.isEmpty()) {
                state.copy(sessions = emptyList(), activeSessionId = "", editingMessageId = null, input = "")
            } else {
                val active = if (state.activeSessionId == id) remaining.first().id else state.activeSessionId
                state.copy(sessions = remaining, activeSessionId = active, editingMessageId = if (state.activeSessionId == id) null else state.editingMessageId)
            }
        }
        persistChats()
        pushDiagnostics()
    }

    fun loadModel() {
        val path = _uiState.value.modelPathInput.trim()
        if (path.isBlank()) {
            _uiState.update { it.copy(modelStatus = localService.currentStatus(error = "Enter a .litertlm model path.")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(modelStatus = localService.currentStatus(configuredPath = path, loading = true)) }
            val t0 = System.currentTimeMillis()
            val result = localService.loadModel(path, _uiState.value.useGpu)
            lastModelLoadMs = System.currentTimeMillis() - t0
            Log.i("FRIDAY", "MODEL: ${result.fold(onSuccess = { "loaded" }, onFailure = { "failed: ${it.message}" })} in $lastModelLoadMs ms (${path.substringAfterLast('/')})")
            _uiState.update { state ->
                state.copy(
                    modelStatus = result.getOrElse { e ->
                        localService.currentStatus(configuredPath = path, error = e.message ?: "Failed to load model")
                    }
                )
            }
            pushDiagnostics()
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            val status = localService.unload()
            _uiState.update { it.copy(modelStatus = status.copy(configuredPath = it.modelPathInput)) }
        }
    }

    private fun cancelActiveGeneration(markStopped: Boolean) {
        val prevId = activeGenerationId
        if (prevId != 0L) {
            activeGenerationId = 0L
            Log.i("FRIDAY", "GEN: cancelled by user")
            tts.stop()
            setSpeakingMessage(null)
        }
        generationJob?.cancel()
        generationJob = null
        if (markStopped) {
            _uiState.update { state ->
                val hadStreaming = state.sessions.any { s -> s.messages.any { it.isStreaming } }
                state.copy(
                    isGenerating = false,
                    metrics = state.metrics.copy(finishedAtMs = System.currentTimeMillis()),
                    sessions = state.sessions.map { session ->
                        session.copy(
                            messages = session.messages.map { m ->
                                if (m.isStreaming) m.copy(isStreaming = false, content = m.content.ifBlank { "(stopped)" }) else m
                            },
                            updatedAt = if (hadStreaming) System.currentTimeMillis() else session.updatedAt
                        )
                    }
                )
            }
            persistChats()
        }
    }

    fun stopGeneration() {
        if (!_uiState.value.isGenerating) return
        cancelActiveGeneration(markStopped = true)
    }

    fun regenerateLastResponse() {
        if (_uiState.value.isGenerating || _uiState.value.isMemoryBusy) return
        cancelActiveGeneration(markStopped = false)
        val session = _uiState.value.activeSession ?: return
        val lastAssistantIdx = session.messages.indexOfLast { it.role == ChatRole.ASSISTANT }
        val lastUserIdx = session.messages.indexOfLast { it.role == ChatRole.USER }
        if (lastAssistantIdx == -1 || lastUserIdx == -1 || lastUserIdx > lastAssistantIdx) return
        val prompt = session.messages[lastUserIdx].content
        val trimmed = session.messages.take(lastUserIdx + 1)
        val assistantId = java.util.UUID.randomUUID().toString()
        val placeholder = ChatMessage(id = assistantId, role = ChatRole.ASSISTANT, content = "", isStreaming = true)
        _uiState.update { state ->
            state.copy(
                isGenerating = true,
                editingMessageId = null,
                sessions = state.sessions.map { s ->
                    if (s.id == session.id) s.copy(messages = trimmed + placeholder, updatedAt = System.currentTimeMillis()) else s
                }
            )
        }
        startGeneration(session.id, prompt, trimmed, assistantId)
        persistChats()
    }

    fun renameSession(sessionId: String, title: String) {
        val t = title.trim().ifBlank { "New session" }
        _uiState.update { state ->
            state.copy(sessions = state.sessions.map { s -> if (s.id == sessionId) s.copy(title = t, updatedAt = System.currentTimeMillis()) else s })
        }
        persistChats()
    }

    fun clearCurrentSession() {
        if (_uiState.value.isGenerating) stopGeneration()
        val sessionId = _uiState.value.activeSessionId
        _uiState.update { state ->
            state.copy(editingMessageId = null, sessions = state.sessions.map { s ->
                if (s.id == sessionId) s.copy(
                    title = "New session",
                    messages = listOf(ChatMessage(role = ChatRole.ASSISTANT, content = "Chat cleared. How can I help?")),
                    updatedAt = System.currentTimeMillis()
                ) else s
            })
        }
        persistChats()
        pushDiagnostics()
        if (_uiState.value.speakReplies) {
            _uiState.value.activeSession?.messages?.firstOrNull()?.id?.let { setSpeakingMessage(it) }
            tts.speak("Chat cleared. How can I help?")
        }
    }

    fun clearAllChats() {
        if (_uiState.value.isGenerating) stopGeneration()
        _uiState.update { it.copy(sessions = emptyList(), activeSessionId = "", input = "", editingMessageId = null) }
        chatPersistJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            chatPersistMutex.withLock {
                chatStore.clear()
            }
        }
        pushDiagnostics()
    }

    fun exportChatsJson(): String {
        val s = _uiState.value
        return chatStore.exportJson(s.sessions, s.activeSessionId)
    }

    fun importChatsJson(json: String): Boolean {
        val (sessions, activeId) = chatStore.parseImport(json)
        if (sessions.isEmpty()) return false
        if (_uiState.value.isGenerating) stopGeneration()
        val fixed = sessions.map { s -> s.copy(messages = s.messages.map { it.copy(isStreaming = false) }) }
        val resolvedActive = fixed.firstOrNull { it.id == activeId }?.id ?: fixed.first().id
        _uiState.update {
            it.copy(
                sessions = fixed,
                activeSessionId = resolvedActive,
                settingsOpen = false,
                chatsStorageOpen = false,
                personaMemoryOpen = false,
                editingMessageId = null,
                input = "",
                isGenerating = false
            )
        }
        persistChats()
        pushDiagnostics()
        return true
    }

    fun startEditMessage(messageId: String) {
        val session = _uiState.value.activeSession ?: return
        val msg = session.messages.firstOrNull { it.id == messageId && it.role == ChatRole.USER } ?: return
        _uiState.update { it.copy(input = msg.content, editingMessageId = messageId) }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingMessageId = null) }
    }

    fun sendMessage() {
        val prompt = _uiState.value.input.trim()
        if (prompt.isBlank() || _uiState.value.isGenerating || _uiState.value.isMemoryBusy) return
        cancelActiveGeneration(markStopped = false)

        var stateNow = _uiState.value
        var session = stateNow.activeSession
        if (session == null) {
            val created = ChatSession(
                title = "New session",
                messages = listOf(ChatMessage(role = ChatRole.ASSISTANT, content = "New session started. How can I help?"))
            )
            _uiState.update { state -> state.copy(sessions = listOf(created) + state.sessions, activeSessionId = created.id) }
            stateNow = _uiState.value
            session = stateNow.activeSession ?: return
        }
        val sessionId = session.id
        val editingId = stateNow.editingMessageId
        val assistantId = java.util.UUID.randomUUID().toString()
        val placeholder = ChatMessage(id = assistantId, role = ChatRole.ASSISTANT, content = "", isStreaming = true)

        if (editingId != null) {
            val idx = session.messages.indexOfFirst { it.id == editingId }
            if (idx >= 0) {
                val base = session.messages.take(idx)
                val userMsg = session.messages[idx].copy(content = prompt, timestamp = System.currentTimeMillis())
                val newMessages = base + userMsg + placeholder
                _uiState.update { state ->
                    state.copy(
                        input = "",
                        isGenerating = true,
                        editingMessageId = null,
                        sessions = state.sessions.map { s ->
                            if (s.id == sessionId) s.copy(messages = newMessages, updatedAt = System.currentTimeMillis()) else s
                        }
                    )
                }
                startGeneration(sessionId, prompt, base, assistantId)
                persistChats()
                return
            }
        }

        val history = session.messages
        val userMsg = ChatMessage(role = ChatRole.USER, content = prompt)
        _uiState.update { state ->
            state.copy(
                input = "",
                isGenerating = true,
                editingMessageId = null,
                sessions = state.sessions.map { s ->
                    if (s.id == sessionId) {
                        val title = if (s.messages.count { it.role == ChatRole.USER } == 0)
                            prompt.take(32).ifBlank { s.title } else s.title
                        s.copy(
                            title = title,
                            messages = s.messages + userMsg + placeholder,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else s
                }
            )
        }

        startGeneration(sessionId, prompt, history, assistantId)
        persistChats() // save user message/placeholder immediately
    }

    private fun isValidEndpointUrl(url: String): Boolean {
        val u = url.trim()
        return (u.startsWith("http://") || u.startsWith("https://")) && u.length > 10
    }

    private fun activeMemories(): List<String> = _uiState.value.let { s ->
        if (s.personaConfig.useMemories) s.memories.map { it.content } else emptyList()
    }

    private fun isActiveGeneration(id: Long): Boolean = activeGenerationId == id

    private fun startGeneration(sessionId: String, prompt: String, history: List<ChatMessage>, assistantId: String) {
        val myId = generationId.incrementAndGet()
        activeGenerationId = myId
        val start = System.currentTimeMillis()
        val modeLabel = if (_uiState.value.inferenceMode == InferenceMode.REMOTE_API) "remote" else "local"
        val modelName = if (_uiState.value.inferenceMode == InferenceMode.REMOTE_API) {
            _uiState.value.apiConfig.model.ifBlank { _uiState.value.apiConfig.baseUrl.substringAfterLast('/').ifBlank { "remote-api" } }
        } else {
            _uiState.value.modelStatus.resolvedPath?.substringAfterLast('/') ?: "local-model"
        }
        _uiState.update { it.copy(metrics = GenerationMetrics(startedAtMs = start, modelName = modelName, mode = modeLabel)) }
        Log.i("FRIDAY", "GEN: start mode=$modeLabel model=$modelName history=${history.size}")
        generationJob = viewModelScope.launch {
            val mode = _uiState.value.inferenceMode
            val apiConfig = _uiState.value.apiConfig
            val localStatus = _uiState.value.modelStatus
            val persona = _uiState.value.personaConfig.systemPrompt
            val memories = activeMemories()
            var first = true
            var chunks = 0
            var terminalHandled = false
            try {
                when {
                    mode == InferenceMode.REMOTE_API -> {
                        if (!apiConfig.isConfigured) {
                            appendToAssistant(sessionId, assistantId, "Remote API mode is selected but no endpoint is configured. Open Settings and add an API base URL (and optional key/model).", done = true, error = true)
                        } else if (!isValidEndpointUrl(apiConfig.baseUrl)) {
                            appendToAssistant(sessionId, assistantId, "Remote API endpoint URL looks invalid. Use a full http(s) URL (e.g. https://api.example.com/v1).", done = true, error = true)
                        } else {
                            val reply = apiService.generateReply(prompt, history, apiConfig, persona, memories)
                            val now = System.currentTimeMillis()
                            chunks = 1
                            if (isActiveGeneration(myId)) {
                                _uiState.update { it.copy(metrics = it.metrics.copy(firstTokenAtMs = it.metrics.firstTokenAtMs ?: now, finishedAtMs = now, outputChars = reply.length, chunkCount = chunks)) }
                            }
                            appendToAssistant(sessionId, assistantId, reply.ifBlank { "(empty response)" }, done = true, error = false)
                            maybeSpeakReply(reply, assistantId)
                        }
                    }
                    localStatus.isReady -> {
                        val localHistory = history
                            .filter { it.role == ChatRole.USER || it.role == ChatRole.ASSISTANT }
                            .mapNotNull { m ->
                                val role = if (m.role == ChatRole.USER) "User" else "Assistant"
                                val text = m.content.trim()
                                if (text.isBlank() || text == "Thinking...") null else role to text
                            }
                        var latest = ""
                        localService.generateReply(prompt, localHistory, persona, memories).collect { chunk ->
                            if (!isActiveGeneration(myId)) return@collect
                            latest = chunk
                            chunks++
                            if (first && latest.isNotBlank()) {
                                first = false
                                _uiState.update { state ->
                                    if (isActiveGeneration(myId)) state.copy(metrics = state.metrics.copy(firstTokenAtMs = System.currentTimeMillis())) else state
                                }
                            }
                            _uiState.update { state ->
                                if (isActiveGeneration(myId)) state.copy(metrics = state.metrics.copy(outputChars = latest.length, chunkCount = chunks)) else state
                            }
                            appendToAssistant(sessionId, assistantId, latest, done = false, error = false)
                        }
                        if (isActiveGeneration(myId)) {
                            _uiState.update { it.copy(metrics = it.metrics.copy(finishedAtMs = System.currentTimeMillis(), outputChars = latest.length, chunkCount = chunks)) }
                        }
                        appendToAssistant(sessionId, assistantId, latest.ifBlank { "(empty response)" }, done = true, error = false)
                        maybeSpeakReply(latest, assistantId)
                    }
                    else -> {
                        val fallback = buildFallbackReply(prompt, localStatus, apiConfig)
                        if (isActiveGeneration(myId)) {
                            _uiState.update { it.copy(metrics = it.metrics.copy(finishedAtMs = System.currentTimeMillis(), outputChars = fallback.length, chunkCount = 1)) }
                        }
                        appendToAssistant(sessionId, assistantId, fallback, done = true, error = false)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (isActiveGeneration(myId)) {
                    val current = _uiState.value.sessions.flatMap { it.messages }.firstOrNull { it.id == assistantId }
                    val stopped = current?.content?.ifBlank { "(stopped)" } ?: "(stopped)"
                    _uiState.update { state ->
                        if (isActiveGeneration(myId)) state.copy(metrics = state.metrics.copy(finishedAtMs = System.currentTimeMillis(), outputChars = stopped.length, chunkCount = chunks), isGenerating = false) else state
                    }
                    if (current?.isStreaming != false) {
                        appendToAssistant(sessionId, assistantId, stopped, done = true, error = false)
                    } else {
                        persistChats()
                    }
                    activeGenerationId = 0L
                    generationJob = null
                    terminalHandled = true
                }
                throw e
            } catch (e: Exception) {
                if (isActiveGeneration(myId)) {
                    val msg = "Error: ${e.message ?: "generation failed"}"
                    lastGenerationError = e.message ?: "generation failed"
                    Log.w("FRIDAY", "GEN: error: ${e.message}")
                    _uiState.update { state ->
                        if (isActiveGeneration(myId)) state.copy(metrics = state.metrics.copy(finishedAtMs = System.currentTimeMillis(), outputChars = msg.length, chunkCount = chunks)) else state
                    }
                    appendToAssistant(sessionId, assistantId, msg, done = true, error = true)
                }
            } finally {
                if (!terminalHandled && isActiveGeneration(myId)) {
                    activeGenerationId = 0L
                    generationJob = null
                    _uiState.update { it.copy(isGenerating = false) }
                    persistChats()
                }
                if (isActiveGeneration(myId) || terminalHandled) {
                    lastGenerationMs = System.currentTimeMillis() - start
                    Log.i("FRIDAY", "GEN: finished in $lastGenerationMs ms chunks=$chunks")
                    pushDiagnostics()
                }
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
        // persistence on completion is handled by generation flow for the active generation; avoid duplicate/stale writes from old jobs
    }

    private fun maybeSpeakReply(text: String, messageId: String? = null) {
        if (!_uiState.value.speakReplies) return
        val clean = text.trim()
        if (clean.isBlank() || clean.startsWith("(stopped") || clean.startsWith("(empty")) return
        setSpeakingMessage(messageId)
        tts.speak(clean)
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
        cancelActiveGeneration(markStopped = false)
        tts.shutdown()
        try {
            kotlinx.coroutines.runBlocking { localService.unload() }
        } catch (_: Exception) {
        }
        super.onCleared()
    }
}

