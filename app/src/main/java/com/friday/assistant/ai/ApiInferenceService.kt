package com.friday.assistant.ai

import android.content.Context
import com.friday.assistant.data.ApiEndpointConfig
import com.friday.assistant.data.ChatMessage
import com.friday.assistant.data.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

class ApiInferenceService(private val appContext: Context) {
    private val prefs = appContext.getSharedPreferences("friday_api", Context.MODE_PRIVATE)

    fun loadConfig(): ApiEndpointConfig = ApiEndpointConfig(
        baseUrl = prefs.getString("base_url", "") ?: "",
        apiKey = prefs.getString("api_key", "") ?: "",
        model = prefs.getString("model", "") ?: ""
    )

    fun saveConfig(config: ApiEndpointConfig) {
        prefs.edit()
            .putString("base_url", config.baseUrl.trim())
            .putString("api_key", config.apiKey.trim())
            .putString("model", config.model.trim())
            .apply()
    }

    suspend fun testEndpoint(config: ApiEndpointConfig): String = withContext(Dispatchers.IO) {
        if (config.baseUrl.isBlank()) error("Endpoint URL is required")
        val reply = generateReply("Reply with exactly: FRIDAY_OK", emptyList(), config, "You are FRIDAY, a helpful assistant.", emptyList())
        "Connected. Response: ${reply.take(120)}"
    }

    suspend fun generateReply(
        prompt: String,
        history: List<ChatMessage>,
        config: ApiEndpointConfig,
        systemPrompt: String = com.friday.assistant.data.PersonaConfig.DEFAULT_PERSONA_PROMPT,
        memories: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val endpoint = resolveEndpoint(config.baseUrl)
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 120000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (config.apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer ${config.apiKey.trim()}")
                setRequestProperty("x-api-key", config.apiKey.trim())
            }
        }
        try {
            val body = buildRequestBody(endpoint, prompt, history, config, systemPrompt, memories)
            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val text = stream.bufferedReader().use(BufferedReader::readText)
            if (code !in 200..299) {
                error("API $code: ${extractError(text)}")
            }
            parseResponse(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun resolveEndpoint(baseUrl: String): String {
        val raw = baseUrl.trim()
        val u = raw.trimEnd('/')
        val lower = u.lowercase()
        return when {
            lower.endsWith("/chat/completions") -> u
            lower.endsWith("/api/chat") -> u
            lower.endsWith("/api/generate") -> u
            lower.endsWith("/v1/messages") -> u
            lower.endsWith("/completions") -> u
            lower.endsWith("/responses") -> u
            lower.endsWith("/v1") -> "$u/chat/completions"
            // if user provided a path (likely full endpoint), respect it
            u.removePrefix("https://").removePrefix("http://").contains("/") -> u
            else -> "$u/v1/chat/completions"
        }
    }

    private fun buildRequestBody(
        endpoint: String,
        prompt: String,
        history: List<ChatMessage>,
        config: ApiEndpointConfig,
        systemPrompt: String,
        memories: List<String>
    ): JSONObject {
        val lower = endpoint.lowercase()
        val filteredHistory = history.filter { it.role == ChatRole.USER || it.role == ChatRole.ASSISTANT }.takeLast(12)
        val system = buildSystemText(systemPrompt, memories)
        return when {
            lower.endsWith("/api/generate") -> JSONObject().apply {
                if (config.model.isNotBlank()) put("model", config.model.trim())
                put("prompt", buildPromptTranscript(prompt, filteredHistory, system))
                put("stream", false)
            }
            lower.endsWith("/api/chat") -> JSONObject().apply {
                if (config.model.isNotBlank()) put("model", config.model.trim())
                put("messages", buildMessagesJson(prompt, filteredHistory, system))
                put("stream", false)
            }
            lower.endsWith("/v1/messages") -> JSONObject().apply {
                if (config.model.isNotBlank()) put("model", config.model.trim()) else put("model", "claude")
                put("max_tokens", 1024)
                put("system", system)
                val msgs = JSONArray()
                filteredHistory.forEach { m ->
                    val role = if (m.role == ChatRole.USER) "user" else "assistant"
                    msgs.put(JSONObject().put("role", role).put("content", m.content))
                }
                msgs.put(JSONObject().put("role", "user").put("content", prompt))
                put("messages", msgs)
            }
            else -> JSONObject().apply {
                if (config.model.isNotBlank()) put("model", config.model.trim())
                put("messages", buildMessagesJson(prompt, filteredHistory, system))
                put("stream", false)
                put("temperature", 0.3)
            }
        }
    }

    private fun buildSystemText(systemPrompt: String, memories: List<String>): String = buildString {
        append(systemPrompt.trim().ifBlank { com.friday.assistant.data.PersonaConfig.DEFAULT_PERSONA_PROMPT })
        val mem = memories.map { it.trim() }.filter { it.isNotBlank() }.take(20)
        if (mem.isNotEmpty()) {
            append("\n\nPersistent memories about the user/context (use when relevant, do not invent):\n")
            mem.forEach { append("- ").append(it.take(300)).append('\n') }
        }
    }

    private fun buildMessagesJson(prompt: String, history: List<ChatMessage>, system: String): JSONArray {
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", system))
        history.forEach { m ->
            val role = if (m.role == ChatRole.USER) "user" else "assistant"
            messages.put(JSONObject().put("role", role).put("content", m.content))
        }
        messages.put(JSONObject().put("role", "user").put("content", prompt))
        return messages
    }

    private fun buildPromptTranscript(prompt: String, history: List<ChatMessage>, system: String): String = buildString {
        append(system.trim()).append("\n\n")
        history.forEach { m ->
            val role = if (m.role == ChatRole.USER) "User" else "Assistant"
            append(role).append(": ").append(m.content.trim()).append("\n")
        }
        append("User: ").append(prompt.trim()).append("\nAssistant:")
    }

    private fun parseResponse(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("{")) {
            val json = JSONObject(trimmed)
            // OpenAI-compatible: choices[0].message.content or choices[0].text
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val first = choices.optJSONObject(0)
                val msg = first?.optJSONObject("message")
                val content = extractText(msg?.opt("content")).ifBlank { extractText(first?.opt("text")) }
                if (content.isNotBlank()) return content
            }
            // Ollama chat: { message: { content } } or generate: { response }
            json.optJSONObject("message")?.let { m ->
                val c = extractText(m.opt("content"))
                if (c.isNotBlank()) return c
            }
            listOf("response", "output_text", "text", "result", "answer", "generated_text").forEach { key ->
                val v = extractText(json.opt(key))
                if (v.isNotBlank()) return v
            }
            // Anthropic-like: content [{type:text,text:...}]
            val contentArr = json.optJSONArray("content")
            if (contentArr != null) {
                val c = extractText(contentArr)
                if (c.isNotBlank()) return c
            }
            val outputArr = json.optJSONArray("output")
            if (outputArr != null) {
                val c = extractText(outputArr)
                if (c.isNotBlank()) return c
            }
            json.optJSONObject("data")?.let { d ->
                val c = extractText(d.opt("text")).ifBlank { extractText(d.opt("content")) }
                if (c.isNotBlank()) return c
            }
            // Some APIs wrap in data array
            json.optJSONArray("data")?.let { d ->
                val c = extractText(d)
                if (c.isNotBlank()) return c
            }
            return trimmed.take(1200)
        }
        // SSE or plain text fallback
        if (trimmed.contains("data:")) {
            val parts = trimmed.lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("data:") }
                .map { it.removePrefix("data:").trim() }
                .filter { it.isNotBlank() && it != "[DONE]" }
                .mapNotNull { part -> runCatching { parseResponse(part) }.getOrNull() }
                .toList()
            if (parts.isNotEmpty()) return parts.last().ifBlank { parts.joinToString("") }
        }
        return trimmed.take(1200)
    }

    private fun extractText(value: Any?): String {
        return when (value) {
            null, JSONObject.NULL -> ""
            is String -> value.trim()
            is JSONArray -> {
                (0 until value.length()).mapNotNull { i ->
                    val v = value.opt(i)
                    when (v) {
                        is JSONObject -> extractText(v.opt("text")).ifBlank { extractText(v.opt("content")) }.ifBlank { extractText(v.opt("value")) }
                        is String -> v.trim()
                        else -> extractText(v)
                    }.ifBlank { null }
                }.joinToString("\n").trim()
            }
            is JSONObject -> extractText(value.opt("text")).ifBlank { extractText(value.opt("content")) }.ifBlank { extractText(value.opt("value")) }
            else -> value.toString().trim().takeIf { it != "null" }.orEmpty()
        }
    }

    private fun extractError(text: String): String {
        return try {
            val json = JSONObject(text.trim())
            val msg = json.optJSONObject("error")?.optString("message")
                ?: json.optString("error")
                ?: json.optString("message")
            msg?.takeIf { it.isNotBlank() && it != "null" } ?: text.take(300)
        } catch (_: Exception) {
            text.take(300)
        }
    }
}
