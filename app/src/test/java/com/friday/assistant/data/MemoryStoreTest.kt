package com.friday.assistant.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MemoryStoreTest {
    @JvmField
    @Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun load_legacyEntriesInferApprovalFromSource() {
        val file = File(tempFolder.root, "friday_memory.json")
        file.writeText(
            """
            {
              "persona": {},
              "memories": [
                {"id":"manual-id","content":"Manual fact","source":"manual"},
                {"id":"extracted-id","content":"Extracted fact","source":"extracted"}
              ]
            }
            """.trimIndent()
        )

        val snapshot = MemoryStore(tempFolder.root).load()
        val manual = snapshot.memories.single { it.id == "manual-id" }
        val extracted = snapshot.pending.single { it.id == "extracted-id" }

        assertTrue(manual.approved)
        assertFalse(extracted.approved)
        assertTrue(snapshot.memories.none { it.id == extracted.id })
    }

    @Test
    fun load_missingPendingMemoriesKeyReturnsEmptyPendingList() {
        val file = File(tempFolder.root, "friday_memory.json")
        file.writeText(
            """
            {
              "persona": {},
              "memories": [
                {"id":"manual-id","content":"Manual fact","source":"manual","approved":true}
              ]
            }
            """.trimIndent()
        )

        val snapshot = MemoryStore(tempFolder.root).load()
        assertTrue(snapshot.pending.isEmpty())
    }

    @Test
    fun saveAndLoad_roundTripsApprovedAndPendingProvenance() {
        val store = MemoryStore(tempFolder.root)
        val approved = MemoryItem(
            id = "approved-id",
            content = "Approved fact",
            source = "extracted",
            origin = "local",
            model = "harness-dummy-model",
            approved = true
        )
        val pending = MemoryItem(
            id = "pending-id",
            content = "Pending fact",
            source = "extracted-remote",
            origin = "https://api.example.com:443",
            model = "harness-dummy-model",
            approved = false
        )

        assertTrue(store.save(MemorySnapshot(memories = listOf(approved), pending = listOf(pending))))
        val reloaded = store.load()

        val loadedApproved = reloaded.memories.single { it.id == approved.id }
        assertEquals(approved.content, loadedApproved.content)
        assertEquals(approved.source, loadedApproved.source)
        assertEquals(approved.origin, loadedApproved.origin)
        assertEquals(approved.model, loadedApproved.model)
        assertTrue(loadedApproved.approved)

        val loadedPending = reloaded.pending.single { it.id == pending.id }
        assertEquals(pending.content, loadedPending.content)
        assertEquals(pending.source, loadedPending.source)
        assertEquals(pending.origin, loadedPending.origin)
        assertEquals(pending.model, loadedPending.model)
        assertFalse(loadedPending.approved)
        assertTrue(reloaded.memories.none { it.id == pending.id })
    }

    @Test
    fun save_writesApprovalProvenanceAndPendingMemories() {
        val store = MemoryStore(tempFolder.root)
        val approved = MemoryItem(
            id = "approved-id",
            content = "Approved fact",
            source = "extracted",
            origin = "local",
            model = "harness-dummy-model",
            approved = true
        )
        val pending = MemoryItem(
            id = "pending-id",
            content = "Pending fact",
            source = "extracted-remote",
            origin = "https://api.example.com:443",
            model = "harness-dummy-model",
            approved = false
        )

        assertTrue(store.save(MemorySnapshot(memories = listOf(approved), pending = listOf(pending))))
        val raw = File(tempFolder.root, "friday_memory.json").readText()
        val json = JSONObject(raw)

        assertTrue(raw.contains("\"approved\""))
        assertTrue(raw.contains("\"origin\""))
        assertTrue(raw.contains("\"model\""))
        assertTrue(json.has("pendingMemories"))
        assertEquals(1, json.getJSONArray("pendingMemories").length())
    }

    @Test
    fun load_approvedExtractedRemoteEntryWithoutOriginIsDowngradedToPending() {
        val file = File(tempFolder.root, "friday_memory.json")
        file.writeText(
            """
            {
              "persona": {},
              "memories": [
                {
                  "id":"remote-without-origin",
                  "content":"Remote fact",
                  "source":"extracted-remote",
                  "origin":"   ",
                  "model":"harness-dummy-model",
                  "approved":true
                }
              ]
            }
            """.trimIndent()
        )

        val snapshot = MemoryStore(tempFolder.root).load()
        val pending = snapshot.pending.single { it.id == "remote-without-origin" }

        assertFalse(pending.approved)
        assertTrue(snapshot.memories.none { it.id == "remote-without-origin" })
    }

    @Test
    fun load_approvedExtractedRemoteEntryWithOriginRemainsApproved() {
        val file = File(tempFolder.root, "friday_memory.json")
        file.writeText(
            """
            {
              "persona": {},
              "memories": [
                {
                  "id":"remote-with-origin",
                  "content":"Remote fact",
                  "source":"extracted-remote",
                  "origin":"https://api.example.com:443",
                  "model":"harness-dummy-model",
                  "approved":true
                }
              ]
            }
            """.trimIndent()
        )

        val snapshot = MemoryStore(tempFolder.root).load()
        val approved = snapshot.memories.single { it.id == "remote-with-origin" }

        assertTrue(approved.approved)
        assertTrue(snapshot.pending.none { it.id == "remote-with-origin" })
    }
}
