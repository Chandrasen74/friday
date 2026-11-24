package com.friday.assistant.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ResponseParser {
    const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
    const val MAX_RECOGNIZED_CHARS = 32 * 1024
    const val FALLBACK_CHARS = 1200
    const val MAX_ERROR_CHARS = 300

    class ResponseTooLargeException(val limit: Int) :
        IllegalStateException("Remote response exceeded the $limit byte limit.")

    fun readBounded(input: InputStream, maxBytes: Int = MAX_RESPONSE_BYTES): String {
        require(maxBytes >= 0) { "maxBytes must be non-negative." }

        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L

        input.use { stream ->
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) throw ResponseTooLargeException(maxBytes)
                output.write(buffer, 0, read)
            }
        }

        return String(output.toByteArray(), Charsets.UTF_8)
    }

    fun parse(text: String, maxChars: Int = MAX_RECOGNIZED_CHARS): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("{")) {
            val json = JSONObject(trimmed)
            // OpenAI-compatible: choices[0].message.content or choices[0].text
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val first = choices.optJSONObject(0)
                val msg = first?.optJSONObject("message")
                val content = extractText(msg?.opt("content")).ifBlank { extractText(first?.opt("text")) }
                if (content.isNotBlank()) return cap(content, maxChars)
            }
            // Ollama chat: { message: { content } } or generate: { response }
            json.optJSONObject("message")?.let { m ->
                val c = extractText(m.opt("content"))
                if (c.isNotBlank()) return cap(c, maxChars)
            }
            listOf("response", "output_text", "text", "result", "answer", "generated_text").forEach { key ->
                val v = extractText(json.opt(key))
                if (v.isNotBlank()) return cap(v, maxChars)
            }
            // Anthropic-like: content [{type:text,text:...}]
            val contentArr = json.optJSONArray("content")
            if (contentArr != null) {
                val c = extractText(contentArr)
                if (c.isNotBlank()) return cap(c, maxChars)
            }
            val outputArr = json.optJSONArray("output")
            if (outputArr != null) {
                val c = extractText(outputArr)
                if (c.isNotBlank()) return cap(c, maxChars)
            }
            json.optJSONObject("data")?.let { d ->
                val c = extractText(d.opt("text")).ifBlank { extractText(d.opt("content")) }
                if (c.isNotBlank()) return cap(c, maxChars)
            }
            // Some APIs wrap in data array
            json.optJSONArray("data")?.let { d ->
                val c = extractText(d)
                if (c.isNotBlank()) return cap(c, maxChars)
            }
            return cap(trimmed, FALLBACK_CHARS)
        }
        // SSE or plain text fallback
        if (trimmed.contains("data:")) {
            val parts = trimmed.lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("data:") }
                .map { it.removePrefix("data:").trim() }
                .filter { it.isNotBlank() && it != "[DONE]" }
                .mapNotNull { part ->
                    runCatching { parse(part, maxChars) }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                }
                .toList()
            if (parts.isNotEmpty()) {
                val joined = parts.joinToString("")
                // if chunks look cumulative (each starts with previous), prefer the last/full one
                val cumulative = parts.size > 1 && parts.zipWithNext().all { (a, b) -> b.startsWith(a) }
                return cap(if (cumulative) parts.last() else joined, maxChars)
            }
        }
        return cap(trimmed, FALLBACK_CHARS)
    }

    fun extractError(text: String, maxChars: Int = MAX_ERROR_CHARS): String {
        val extracted = try {
            val json = JSONObject(text.trim())
            val msg = json.optJSONObject("error")?.optString("message")
                ?: json.optString("error")
                ?: json.optString("message")
            msg?.takeIf { it.isNotBlank() && it != "null" } ?: text.take(MAX_ERROR_CHARS)
        } catch (_: Exception) {
            text.take(MAX_ERROR_CHARS)
        }
        return cap(extracted, maxChars)
    }

    private fun cap(value: String, limit: Int): String =
        if (value.length <= limit) value else value.take(limit) + "\n[response truncated at $limit characters]"

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
}
