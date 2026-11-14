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
