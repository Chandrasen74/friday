package com.friday.assistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class MemorySnapshot(
    val persona: PersonaConfig = PersonaConfig(),
    val memories: List<MemoryItem> = emptyList(),
    val pending: List<MemoryItem> = emptyList()
)

class MemoryStore(filesDir: File) {
    private val file = File(filesDir, "friday_memory.json")
    private val lock = Any()

    constructor(context: Context) : this(context.filesDir)

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

            val loadedMemories = decodeMemories(obj.optJSONArray("memories"))
            val loadedPending = decodeMemories(obj.optJSONArray("pendingMemories"))
            val memories = loadedMemories.filter { it.approved }
            val pending = loadedMemories.filterNot { it.approved } + loadedPending

            MemorySnapshot(persona = persona, memories = memories, pending = pending)
        } catch (_: Exception) {
            MemorySnapshot()
        }
    }

    fun save(snapshot: MemorySnapshot): Boolean = synchronized(lock) {
        try {
            val arr = JSONArray()
            snapshot.memories.forEach { memory ->
                arr.put(memoryJson(memory))
            }
            val pendingArr = JSONArray()
            snapshot.pending.forEach { memory ->
                pendingArr.put(memoryJson(memory))
            }
            val persona = JSONObject()
                .put("systemPrompt", snapshot.persona.systemPrompt)
                .put("useMemories", snapshot.persona.useMemories)
            val root = JSONObject()
                .put("persona", persona)
                .put("memories", arr)
                .put("pendingMemories", pendingArr)

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

    private fun decodeMemories(array: JSONArray?): List<MemoryItem> {
        if (array == null) return emptyList()
        val memories = mutableListOf<MemoryItem>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val content = obj.optString("content", "").trim()
            if (content.isBlank()) continue

            val source = obj.optString("source", "manual").ifBlank { "manual" }
            val storedApproved = if (obj.has("approved")) {
                obj.optBoolean("approved", source == "manual")
            } else {
                source == "manual"
            }
            val origin = obj.optString("origin", "")
            // Defense in depth: file authority remains an open platform question tracked by
            // audit leads L-2, L-3 and L-6. This is not a cryptographic trust boundary.
            val approved = storedApproved && (source == "manual" || origin.isNotBlank())
            memories.add(
                MemoryItem(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    content = content.take(1000),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    source = source,
                    origin = origin,
                    model = obj.optString("model", ""),
                    approved = approved
                )
            )
        }
        return memories
    }

    private fun memoryJson(memory: MemoryItem): JSONObject = JSONObject()
        .put("id", memory.id)
        .put("content", memory.content)
        .put("createdAt", memory.createdAt)
        .put("updatedAt", memory.updatedAt)
        .put("source", memory.source)
        .put("origin", memory.origin)
        .put("model", memory.model)
        .put("approved", memory.approved)
}
