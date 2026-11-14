package com.friday.assistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class MemorySnapshot(
    val persona: PersonaConfig = PersonaConfig(),
    val memories: List<MemoryItem> = emptyList()
)

class MemoryStore(context: Context) {
    private val file = File(context.filesDir, "friday_memory.json")
    private val lock = Any()

    fun load(): MemorySnapshot = synchronized(lock) {
        if (!file.exists()) return MemorySnapshot()
        try {
            val obj = JSONObject(file.readText())
            val personaObj = obj.optJSONObject("persona")
            val persona = if (personaObj != null) {
                PersonaConfig(
                    systemPrompt = personaObj.optString("systemPrompt", PersonaConfig.DEFAULT_PERSONA_PROMPT)
                        .ifBlank { PersonaConfig.DEFAULT_PERSONA_PROMPT },
                    useMemories = personaObj.optBoolean("useMemories", true)
                )
            } else PersonaConfig()

            val arr = obj.optJSONArray("memories") ?: JSONArray()
            val memories = mutableListOf<MemoryItem>()
            for (i in 0 until arr.length()) {
                val m = arr.optJSONObject(i) ?: continue
                val content = m.optString("content", "").trim()
                if (content.isBlank()) continue
                memories.add(
                    MemoryItem(
                        id = m.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                        content = content.take(1000),
                        createdAt = m.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = m.optLong("updatedAt", System.currentTimeMillis()),
                        source = m.optString("source", "manual").ifBlank { "manual" }
                    )
                )
            }
            MemorySnapshot(persona = persona, memories = memories)
        } catch (_: Exception) {
            MemorySnapshot()
        }
    }

    fun save(snapshot: MemorySnapshot): Boolean = synchronized(lock) {
        try {
            val arr = JSONArray()
            snapshot.memories.forEach { m ->
                arr.put(
                    JSONObject()
                        .put("id", m.id)
                        .put("content", m.content)
                        .put("createdAt", m.createdAt)
                        .put("updatedAt", m.updatedAt)
                        .put("source", m.source)
                )
            }
            val persona = JSONObject()
                .put("systemPrompt", snapshot.persona.systemPrompt)
                .put("useMemories", snapshot.persona.useMemories)
            val root = JSONObject()
                .put("persona", persona)
                .put("memories", arr)

            val tmp = File(file.parentFile ?: return false, "friday_memory.json.tmp")
            tmp.writeText(root.toString())
            if (file.exists()) file.delete()
            if (!tmp.renameTo(file)) {
                file.writeText(root.toString())
                tmp.delete()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun exportJson(): String = synchronized(lock) {
        runCatching { if (file.exists()) file.readText() else "{\"persona\":{},\"memories\":[]}" }.getOrDefault("")
    }
}
