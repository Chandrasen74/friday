package com.friday.assistant.ui.telemetry

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * Immutable snapshot of one assistant generation, captured the moment the
 * message flips from streaming to done and kept around so the telemetry
 * footer and the diagnostics sheet can render it long after the fact.
 */
data class SavedGenerationMetrics(
    val tokensPerSecond: Float,
    val durationMs: Long,
    val modelName: String = "",
    val backendLabel: String = "",
    val estimatedTokens: Int = 0,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Cold-boot durable store for post-generation telemetry, keyed by message id.
 *
 * Design goals:
 * - Zero schema migration: one JSON document under a single SharedPreferences
 *   key, serialized with the platform [org.json.JSONObject] (no extra deps).
 * - Tiny and fast: capped at the 500 most recent message ids (LRU by
 *   [SavedGenerationMetrics.timestampMs]), so the document stays under ~40 KB
 *   and hydrates in well under a millisecond.
 * - O(1) reads once hydrated: [get] serves from an in-memory map so LazyColumn
 *   scroll frames never touch disk.
 * - Fire-and-forget writes: [put] updates memory immediately and persists the
 *   whole document asynchronously via `apply()`.
 *
 * Cleanup is wired to the existing chat-management actions: clearing the
 * current chat evicts its message ids, clearing all chats wipes the store.
 */
class GenerationTelemetryCache private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Guarded by `this`; hydrated lazily on first access.
    private val entries = HashMap<String, SavedGenerationMetrics>()
    private var hydrated = false

    fun get(messageId: String): SavedGenerationMetrics? {
        synchronized(this) {
            if (!hydrated) hydrate()
            return entries[messageId]
        }
    }

    fun put(messageId: String, metrics: SavedGenerationMetrics) {
        val snapshot: Map<String, SavedGenerationMetrics>
        synchronized(this) {
            if (!hydrated) hydrate()
            entries[messageId] = metrics
            prune()
            snapshot = HashMap(entries)
        }
        persist(snapshot)
    }

    fun remove(messageId: String) {
        val snapshot: Map<String, SavedGenerationMetrics>?
        synchronized(this) {
            if (!hydrated) hydrate()
            val removed = entries.remove(messageId) != null
            snapshot = if (removed) HashMap(entries) else null
        }
        snapshot?.let(::persist)
    }

    fun clearAll() {
        synchronized(this) {
            entries.clear()
            hydrated = true
        }
        prefs.edit().remove(KEY).apply()
    }

    /** Evicts the oldest entries so the map never exceeds [MAX_ENTRIES]. */
    private fun prune() {
        if (entries.size <= MAX_ENTRIES) return
        val overflow = entries.size - MAX_ENTRIES
        entries.entries
            .sortedBy { it.value.timestampMs }
            .take(overflow)
            .forEach { entries.remove(it.key) }
    }

    private fun persist(snapshot: Map<String, SavedGenerationMetrics>) {
        try {
            val root = JSONObject()
            snapshot.forEach { (id, m) ->
                val entry = JSONObject()
                    .put("t", m.tokensPerSecond.toDouble())
                    .put("d", m.durationMs)
                    .put("m", m.modelName)
                    .put("b", m.backendLabel)
                    .put("k", m.estimatedTokens)
                    .put("s", m.timestampMs)
                root.put(id, entry)
            }
            prefs.edit().putString(KEY, root.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Telemetry persist failed", e)
        }
    }

    private fun hydrate() {
        hydrated = true
        val raw = prefs.getString(KEY, null) ?: return
        try {
            val root = JSONObject(raw)
            val ids = root.keys()
            while (ids.hasNext()) {
                val id = ids.next()
                val o = root.optJSONObject(id) ?: continue
                entries[id] = SavedGenerationMetrics(
                    tokensPerSecond = o.optDouble("t", 0.0).toFloat(),
                    durationMs = o.optLong("d", 0L),
                    modelName = o.optString("m", ""),
                    backendLabel = o.optString("b", ""),
                    estimatedTokens = o.optInt("k", 0),
                    timestampMs = o.optLong("s", 0L)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Telemetry hydrate failed; resetting store", e)
            entries.clear()
        }
    }

    companion object {
        private const val TAG = "GenTelemetryCache"
        private const val PREFS_NAME = "friday_generation_metrics"
        private const val KEY = "metrics_v1"
        private const val MAX_ENTRIES = 500

        @Volatile
        private var instance: GenerationTelemetryCache? = null

        fun getInstance(context: Context): GenerationTelemetryCache =
            instance ?: synchronized(this) {
                instance ?: GenerationTelemetryCache(context.applicationContext).also { instance = it }
            }
    }
}
