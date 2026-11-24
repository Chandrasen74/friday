package com.friday.assistant.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class ResponseParserTest {
    private class CountingInputStream(
        private val totalBytes: Int,
        private val chunkSize: Int
    ) : InputStream() {
        var bytesHandedOut = 0
            private set
        private var remaining = totalBytes

        override fun read(): Int {
            if (remaining <= 0) return -1
            remaining -= 1
            bytesHandedOut += 1
            return 0
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining <= 0) return -1
            val count = minOf(length, chunkSize, remaining)
            remaining -= count
            bytesHandedOut += count
            return count
        }
    }

    private fun truncationMarker(limit: Int): String =
        "\n[response truncated at $limit characters]"

    private fun assertRecognizedBranch(body: String, limit: Int, expectedPrefix: String = "A") {
        val parsed = ResponseParser.parse(body, limit)
        val marker = truncationMarker(limit)
        assertTrue("Expected prefix '$expectedPrefix' in '$parsed'", parsed.startsWith(expectedPrefix))
        assertTrue("Unexpected length ${parsed.length}", parsed.length <= limit + marker.length)
        assertTrue("Expected truncation marker in '$parsed'", parsed.endsWith(marker))
    }

    @Test
    fun readBounded_readsSmallPayloadAndRejectsOversizedPayload() {
        val sentinel = "FRIDAY_CLIP_SENTINEL_7F3A"
        assertEquals(
            sentinel,
            ResponseParser.readBounded(ByteArrayInputStream(sentinel.toByteArray(Charsets.UTF_8)))
        )

        val error = assertThrows(ResponseParser.ResponseTooLargeException::class.java) {
            ResponseParser.readBounded(
                ByteArrayInputStream("A".repeat(100).toByteArray(Charsets.UTF_8)),
                maxBytes = 16
            )
        }
        assertEquals(16, error.limit)
        assertTrue(error.message.orEmpty().contains("16"))
        assertEquals(2 * 1024 * 1024, ResponseParser.MAX_RESPONSE_BYTES)
    }

    @Test
    fun parse_capsEveryRecognizedJsonBranch() {
        val payload = "A".repeat(500)
        val bodies = listOf(
            "{\"choices\":[{\"message\":{\"content\":\"$payload\"}}]}",
            "{\"choices\":[{\"text\":\"$payload\"}]}",
            "{\"message\":{\"content\":\"$payload\"}}",
            "{\"response\":\"$payload\"}",
            "{\"output_text\":\"$payload\"}",
            "{\"text\":\"$payload\"}",
            "{\"result\":\"$payload\"}",
            "{\"answer\":\"$payload\"}",
            "{\"generated_text\":\"$payload\"}",
            "{\"content\":[{\"type\":\"text\",\"text\":\"$payload\"}]}",
            "{\"output\":[{\"text\":\"$payload\"}]}",
            "{\"data\":{\"text\":\"$payload\"}}",
            "{\"data\":{\"content\":\"$payload\"}}",
            "{\"data\":[{\"text\":\"$payload\"}]}"
        )

        bodies.forEach { body ->
            assertRecognizedBranch(body, limit = 100)
        }
    }

    @Test
    fun parse_capsRecognizedJsonAtDefaultLimit() {
        val payload = "A".repeat(ResponseParser.MAX_RECOGNIZED_CHARS + 500)
        val body = "{\"choices\":[{\"message\":{\"content\":\"$payload\"}}]}"
        assertRecognizedBranch(body, limit = ResponseParser.MAX_RECOGNIZED_CHARS)
    }

    @Test
    fun parse_capsJoinedAndCumulativeSseResponses() {
        val joined = listOf(
            "data: {\"choices\":[{\"message\":{\"content\":\"${"A".repeat(80)}\"}}]}",
            "data: {\"choices\":[{\"message\":{\"content\":\"${"B".repeat(80)}\"}}]}",
            "data: {\"choices\":[{\"message\":{\"content\":\"${"C".repeat(80)}\"}}]}"
        ).joinToString("\n")
        assertRecognizedBranch(joined, limit = 100)

        val cumulative = listOf(
            "data: {\"choices\":[{\"message\":{\"content\":\"${"A".repeat(60)}\"}}]}",
            "data: {\"choices\":[{\"message\":{\"content\":\"${"A".repeat(120)}\"}}]}"
        ).joinToString("\n")
        assertRecognizedBranch(cumulative, limit = 100)
    }

    @Test
    fun parse_capsUnknownJsonAndPlainTextFallbacks() {
        val unknownJson = "{\"unknown\":\"${"A".repeat(2000)}\"}"
        val plainText = "A".repeat(2000)
        val marker = truncationMarker(ResponseParser.FALLBACK_CHARS)

        val unknownParsed = ResponseParser.parse(unknownJson)
        assertTrue(unknownParsed.startsWith("{"))
        assertTrue(unknownParsed.length <= ResponseParser.FALLBACK_CHARS + marker.length)
        assertTrue(unknownParsed.endsWith(marker))

        val plainParsed = ResponseParser.parse(plainText)
        assertTrue(plainParsed.startsWith("A"))
        assertTrue(plainParsed.length <= ResponseParser.FALLBACK_CHARS + marker.length)
        assertTrue(plainParsed.endsWith(marker))
        assertEquals(1200, ResponseParser.FALLBACK_CHARS)
    }

    @Test
    fun extractError_capsOversizedErrorPayload() {
        val body = "{\"error\":{\"message\":\"${"E".repeat(1000)}\"}}"
        val parsed = ResponseParser.extractError(body)
        val marker = truncationMarker(ResponseParser.MAX_ERROR_CHARS)

        assertTrue(parsed.startsWith("E"))
        assertTrue(parsed.length <= ResponseParser.MAX_ERROR_CHARS + marker.length)
        assertTrue(parsed.endsWith(marker))
        assertEquals(300, ResponseParser.MAX_ERROR_CHARS)
    }

    @Test
    fun readBounded_throwsBeforeConsumingTheWholeStreamWhenLimitIsExceeded() {
        val totalBytes = 64
        val stream = CountingInputStream(totalBytes = totalBytes, chunkSize = 24)

        val error = assertThrows(ResponseParser.ResponseTooLargeException::class.java) {
            ResponseParser.readBounded(stream, maxBytes = 16)
        }

        assertEquals(16, error.limit)
        assertEquals(24, stream.bytesHandedOut)
        assertTrue(stream.bytesHandedOut < totalBytes)
    }
}
