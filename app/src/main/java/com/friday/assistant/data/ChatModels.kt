package com.friday.assistant.data

import java.util.UUID

enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class MemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val source: String = "manual",        // manual | extracted | extracted-remote
    val origin: String = "",               // canonical endpoint origin, or "local"
    val model: String = "",                // model that produced the item
    val approved: Boolean = true           // default true; remotely extracted items are created with false
)

data class PersonaConfig(
    val systemPrompt: String = DEFAULT_PERSONA_PROMPT,
    val useMemories: Boolean = true
) {
    companion object {
        const val DEFAULT_PERSONA_PROMPT =
            "You are F.R.I.D.A.Y., a calm, capable, and efficient personal assistant inspired by Tony Stark's AI. " +
            "Be clear, concise, and practical. Anticipate helpful next steps when appropriate, but do not be intrusive. " +
            "Use a professional, friendly tone with light dry wit only when it fits naturally. " +
            "Answer the user's request directly, stay on topic, and be honest when you do not know something."
    }
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
    val model: String = "",
    val apiKeyOrigin: String = ""
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
