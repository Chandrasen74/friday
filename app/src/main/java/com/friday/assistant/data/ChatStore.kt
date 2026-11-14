package com.friday.assistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChatStore(context: Context) {
    private val file = File(context.filesDir, "friday_chats.json")
    private val legacyCacheFile = File(context.cacheDir, "friday_chats.json")
    private val lock = Any()

    fun load(): Pair<List<ChatSession>, String?> {
        val source = synchronized(lock) {
            when {
                file.exists() -> file
                legacyCacheFile.exists() -> legacyCacheFile
                else -> null
            }
        } ?: return emptyList<ChatSession>() to null
        return try {
            val text = synchronized(lock) { source.readText() }
            val parsed = parseSessionsJson(text)
            if (source == legacyCacheFile && parsed.first.isNotEmpty()) {
                runCatching { save(parsed.first, parsed.second ?: parsed.first.first().id) }
            }
            parsed
        } catch (_: Exception) {
            emptyList<ChatSession>() to null
        }
    }

    fun save(sessions: List<ChatSession>, activeSessionId: String): Boolean = synchronized(lock) {
        try {
            val obj = toJson(sessions, activeSessionId)
            val tmp = File(file.parentFile ?: return false, "friday_chats.json.tmp")
            tmp.writeText(obj.toString())
            if (file.exists()) file.delete()
            if (!tmp.renameTo(file)) {
                file.writeText(obj.toString())
                tmp.delete()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun exportJson(): String = synchronized(lock) {
        runCatching { if (file.exists()) file.readText() else toJson(emptyList(), "").toString() }.getOrDefault("")
    }

    fun parseImport(json: String): Pair<List<ChatSession>, String?> {
        return try {
            if (json.length > 5_000_000) return emptyList<ChatSession>() to null
            parseSessionsJson(json)
        } catch (_: Exception) {
            emptyList<ChatSession>() to null
        }
    }

    fun clear() {
        synchronized(lock) {
            runCatching { if (file.exists()) file.delete() }
            runCatching { if (legacyCacheFile.exists()) legacyCacheFile.delete() }
        }
    }

    private fun toJson(sessions: List<ChatSession>, activeSessionId: String): JSONObject {
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
        return JSONObject()
            .put("activeSessionId", activeSessionId)
            .put("sessions", arr)
    }

    private fun parseSessionsJson(text: String): Pair<List<ChatSession>, String?> {
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
                    title = s.optString("title", "New session").take(120),
                    messages = messages,
                    createdAt = s.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = s.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }
        val resolvedActive = sessions.firstOrNull { it.id == activeId }?.id ?: sessions.firstOrNull()?.id
        return sessions to resolvedActive
    }
}
