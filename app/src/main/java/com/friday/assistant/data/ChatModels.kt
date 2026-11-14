package com.friday.assistant.data

import java.util.UUID

enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New session",
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ModelStatus(
    val configuredPath: String = "",
    val resolvedPath: String? = null,
    val isReady: Boolean = false,
    val isLoading: Boolean = false,
    val statusText: String = "No local model loaded",
    val lastError: String? = null
)

enum class InferenceMode {
    LOCAL,
    REMOTE_API
}

data class ApiEndpointConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = ""
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank()
}

data class GenerationMetrics(
    val startedAtMs: Long? = null,
    val firstTokenAtMs: Long? = null,
    val finishedAtMs: Long? = null,
    val outputChars: Int = 0,
    val modelName: String = "",
    val chunkCount: Int = 0,
    val mode: String = ""
) {
    val ttftMs: Long? get() = if (startedAtMs != null && firstTokenAtMs != null) firstTokenAtMs - startedAtMs else null
    val totalMs: Long? get() = if (startedAtMs != null && finishedAtMs != null) finishedAtMs - startedAtMs else if (startedAtMs != null) System.currentTimeMillis() - startedAtMs else null
    val estimatedTokens: Int get() = if (outputChars <= 0) 0 else kotlin.math.max(1, (outputChars / 4.0).toInt())
    val charsPerSec: Double? get() {
        val t = totalMs ?: return null
        if (t <= 0) return null
        return outputChars * 1000.0 / t
    }
    val tokensPerSec: Double? get() {
        val t = totalMs ?: return null
        if (t <= 0) return null
        return estimatedTokens * 1000.0 / t
    }
}
