package com.friday.assistant.ai

import android.content.Context
import android.util.Log
import com.friday.assistant.data.ApiEndpointConfig
import com.friday.assistant.data.ChatMessage
import com.friday.assistant.data.ChatRole
import com.friday.assistant.data.MemoryContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiInferenceService(private val appContext: Context) {
    private val prefs = appContext.getSharedPreferences("friday_api", Context.MODE_PRIVATE)

    fun loadConfig(): ApiEndpointConfig {
        val baseUrl = prefs.getString("base_url", "") ?: ""
        val apiKey = prefs.getString("api_key", "") ?: ""
        val model = prefs.getString("model", "") ?: ""
        val storedOrigin = prefs.getString("api_key_origin", "") ?: ""
        return ApiEndpointConfig(
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model,
            apiKeyOrigin = CredentialBinding.bindOrigin(baseUrl, apiKey, storedOrigin)
        )
    }

    fun saveConfig(config: ApiEndpointConfig) {
        prefs.edit()
            .putString("base_url", config.baseUrl.trim())
            .putString("api_key", config.apiKey.trim())
            .putString("model", config.model.trim())
            .putString(
                "api_key_origin",
                CredentialBinding.bindOrigin(config.baseUrl, config.apiKey, config.apiKeyOrigin)
            )
            .apply()
    }

    suspend fun testEndpoint(config: ApiEndpointConfig): String = withContext(Dispatchers.IO) {
        if (config.baseUrl.isBlank()) error("Endpoint URL is required")
        EndpointSecurity.requireSecureEndpoint(config.baseUrl)
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
        RemoteRequestPolicy.validate(config, endpoint)
        Log.i("FRIDAY", "API: POST $endpoint model=${config.model.ifBlank { "(default)" }} history=${history.size}")
        val apiStart = System.currentTimeMillis()
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setInstanceFollowRedirects(false)
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
            RemoteRequestPolicy.rejectRedirects(code, conn.getHeaderField("Location"))
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val text = ResponseParser.readBounded(stream)
            if (code !in 200..299) {
                Log.w("FRIDAY", "API: HTTP $code error=${extractError(text).take(200)}")
                error("API $code: ${extractError(text)}")
            }
            val reply = parseResponse(text)
            Log.i("FRIDAY", "API: HTTP $code reply ${reply.length} chars in ${System.currentTimeMillis() - apiStart} ms")
            reply
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
        append(MemoryContext.render(memories))
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

    private fun parseResponse(text: String): String = ResponseParser.parse(text)

    private fun extractError(text: String): String = ResponseParser.extractError(text)
}
