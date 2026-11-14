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

    suspend fun generateReply(prompt: String, history: List<ChatMessage>, config: ApiEndpointConfig): String = withContext(Dispatchers.IO) {
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
            }
        }
        try {
            val messages = JSONArray()
            messages.put(JSONObject().put("role", "system").put("content",
                "You are FRIDAY, a helpful assistant. Answer the user's question directly, clearly, and completely."))
            history.filter { it.role == ChatRole.USER || it.role == ChatRole.ASSISTANT }
                .takeLast(10)
                .forEach { m ->
                    val role = if (m.role == ChatRole.USER) "user" else "assistant"
                    messages.put(JSONObject().put("role", role).put("content", m.content))
                }
            messages.put(JSONObject().put("role", "user").put("content", prompt))

            val body = JSONObject().apply {
                if (config.model.isNotBlank()) put("model", config.model.trim())
                put("messages", messages)
                put("stream", false)
                put("temperature", 0.3)
            }
            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val text = stream.bufferedReader().use(BufferedReader::readText)
            if (code !in 200..299) {
                error("API $code: ${text.take(300)}")
            }
            parseResponse(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun resolveEndpoint(baseUrl: String): String {
        val u = baseUrl.trim().trimEnd('/')
        return when {
            u.endsWith("/chat/completions") -> u
            u.endsWith("/v1") -> "$u/chat/completions"
            u.contains("/v1/") -> u
            else -> "$u/v1/chat/completions"
        }
    }

    private fun parseResponse(text: String): String {
        val json = JSONObject(text)
        // OpenAI-compatible: choices[0].message.content
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val msg = choices.optJSONObject(0)?.optJSONObject("message")
            val content = msg?.optString("content")?.trim().orEmpty()
            if (content.isNotEmpty()) return content
            val textContent = choices.optJSONObject(0)?.optString("text")?.trim().orEmpty()
            if (textContent.isNotEmpty()) return textContent
        }
        // Some APIs: output/content or response
        json.optString("response").takeIf { it.isNotBlank() }?.let { return it.trim() }
        json.optString("output").takeIf { it.isNotBlank() }?.let { return it.trim() }
        // Try nested content arrays
        val outputArr = json.optJSONArray("output")
        if (outputArr != null && outputArr.length() > 0) {
            val c = outputArr.optJSONObject(0)?.optJSONArray("content")
            if (c != null && c.length() > 0) {
                val t = c.optJSONObject(0)?.optString("text")?.trim().orEmpty()
                if (t.isNotEmpty()) return t
            }
        }
        return text.take(1200)
    }
}
