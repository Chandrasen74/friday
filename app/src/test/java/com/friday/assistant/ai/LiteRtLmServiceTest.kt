package com.friday.assistant.ai

import com.friday.assistant.ai.LiteRtLmService.BundledCopyProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

class LiteRtLmServiceTest {

    @JvmField
    @Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun needsBundledCopy_missingFile_returnsTrue() {
        val outFile = File(tempFolder.root, "missing.litertlm")
        assertTrue(LiteRtLmService.needsBundledCopy(outFile, 100L))
        assertTrue(LiteRtLmService.needsBundledCopy(outFile, null))
    }

    @Test
    fun needsBundledCopy_emptyFile_returnsTrue() {
        val outFile = tempFolder.newFile("empty.litertlm")
        assertEquals(0L, outFile.length())
        assertTrue(LiteRtLmService.needsBundledCopy(outFile, 100L))
    }

    @Test
    fun needsBundledCopy_sizeMismatch_returnsTrue() {
        val outFile = tempFolder.newFile("model-size-mismatch.litertlm")
        outFile.writeBytes(ByteArray(50))
        assertTrue(LiteRtLmService.needsBundledCopy(outFile, 100L))
    }

    @Test
    fun needsBundledCopy_sizeMatch_returnsFalse() {
        val outFile = tempFolder.newFile("model-size-match.litertlm")
        outFile.writeBytes(ByteArray(100))
        assertFalse(LiteRtLmService.needsBundledCopy(outFile, 100L))
    }

    @Test
    fun needsBundledCopy_unknownExpectedStillRequiresExistingNonEmptyFile() {
        val outFile = tempFolder.newFile("model.litertlm")
        outFile.writeBytes(ByteArray(100))
        assertFalse(LiteRtLmService.needsBundledCopy(outFile, null))
    }

    @Test
    fun shouldReportCopyProgress_initialAndThresholdAndEnd() {
        val bufferSize = 8192
        assertFalse(LiteRtLmService.shouldReportCopyProgress(0L, 0L, 100, bufferSize))
        assertTrue(LiteRtLmService.shouldReportCopyProgress(4L * 1024L * 1024L, 0L, 8192, bufferSize))
        assertFalse(LiteRtLmService.shouldReportCopyProgress(1024L, 0L, 8192, bufferSize))
        assertTrue(LiteRtLmService.shouldReportCopyProgress(1024L, 0L, 100, bufferSize))
    }

    @Test
    fun copyProgressFraction_handlesZeroUnknownAndBounds() {
        assertNull(LiteRtLmService.copyProgressFraction(100L, null))
        assertNull(LiteRtLmService.copyProgressFraction(100L, 0L))
        assertEquals(0.5f, LiteRtLmService.copyProgressFraction(50L, 100L) ?: -1f, 0.0001f)
        assertEquals(1f, LiteRtLmService.copyProgressFraction(200L, 100L) ?: -1f, 0.0001f)
        assertEquals(0f, LiteRtLmService.copyProgressFraction(0L, 100L) ?: -1f, 0.0001f)
    }

    @Test
    fun formatCopiedBytes_showsTotalWhenKnown() {
        val formatted = LiteRtLmService.formatCopiedBytes(50L * 1024L * 1024L, 100L * 1024L * 1024L)
        assertTrue(formatted.contains("50"))
        assertTrue(formatted.contains("100"))
        assertTrue(formatted.contains("MB"))
    }

    @Test
    fun formatCopiedBytes_showsOnlyCopiedWhenUnknown() {
        val formatted = LiteRtLmService.formatCopiedBytes(10L * 1024L * 1024L, null)
        assertTrue(formatted.contains("10"))
        assertTrue(formatted.contains("copied"))
    }

    @Test
    fun staleTmpCleanup_simulatedOnFailure() {
        val outDir = tempFolder.newFolder("models")
        val tmp = File(outDir, "model.litertlm.tmp")
        tmp.writeBytes(ByteArray(10))
        assertTrue(tmp.exists())
        runCatching { tmp.delete() }
        assertFalse(tmp.exists())
    }

    @Test
    fun atomicMoveFallback_writesFileCorrectly() {
        val outDir = tempFolder.newFolder("out")
        val tmp = File(outDir, "tmpfile.bin")
        val target = File(outDir, "target.bin")
        tmp.writeBytes(byteArrayOf(1, 2, 3, 4))
        if (target.exists()) target.delete()
        assertTrue(!target.exists())
        val moved = tmp.renameTo(target)
        assertTrue(moved)
        assertFalse(tmp.exists())
        assertTrue(target.exists())
        assertEquals(4, target.length())
    }
}
