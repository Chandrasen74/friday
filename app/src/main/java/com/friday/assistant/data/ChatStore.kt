package com.friday.assistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChatStore(context: Context) {
    private val file = File(context.cacheDir, "friday_chats.json")

    fun load(): Pair<List<ChatSession>, String?> {
        if (!file.exists()) return emptyList<ChatSession>() to null
        return try {
            val text = file.readText()
            val obj = JSONObject(text)
            val activeId = obj.optString("activeSessionId").ifBlank { null }
            val arr = obj.optJSONArray("sessions") ?: JSONArray()
            val sessions = mutableListOf<ChatSession>()
            for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                val msgsArr = s.optJSONArray("messages") ?: JSONArray()
                val messages = mutableListOf<ChatMessage>()
                for (j in 0 until msgsArr.length()) {
                    val m = msgsArr.optJSONObject(j) ?: continue
                    val role = runCatching { ChatRole.valueOf(m.optString("role", "ASSISTANT")) }.getOrDefault(ChatRole.ASSISTANT)
                    messages.add(
                        ChatMessage(
                            id = m.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                            role = role,
                            content = m.optString("content", ""),
                            isStreaming = false,
                            isError = m.optBoolean("isError", false),
                            timestamp = m.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                sessions.add(
                    ChatSession(
                        id = s.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                        title = s.optString("title", "New session"),
                        messages = messages,
                        createdAt = s.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = s.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            sessions to activeId
        } catch (_: Exception) {
            emptyList<ChatSession>() to null
        }
    }

    fun save(sessions: List<ChatSession>, activeSessionId: String) {
        try {
            val arr = JSONArray()
            sessions.forEach { s ->
                val msgs = JSONArray()
                s.messages.forEach { m ->
                    msgs.put(
                        JSONObject()
                            .put("id", m.id)
                            .put("role", m.role.name)
                            .put("content", m.content)
                            .put("isError", m.isError)
                            .put("timestamp", m.timestamp)
                    )
                }
                arr.put(
                    JSONObject()
                        .put("id", s.id)
                        .put("title", s.title)
                        .put("createdAt", s.createdAt)
                        .put("updatedAt", s.updatedAt)
                        .put("messages", msgs)
                )
            }
            val obj = JSONObject()
                .put("activeSessionId", activeSessionId)
                .put("sessions", arr)
            file.writeText(obj.toString())
        } catch (_: Exception) {
        }
    }
}
