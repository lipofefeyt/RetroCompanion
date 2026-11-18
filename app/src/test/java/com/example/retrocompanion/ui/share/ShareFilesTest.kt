package com.example.retrocompanion.ui.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ShareFilesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val now = 10_000_000_000L

    private fun card(dir: File, name: String, modified: Long) =
        File(dir, name).apply {
            writeText("png")
            setLastModified(modified)
        }

    @Test
    fun `prepareDir creates only the dedicated share folder inside the cache`() {
        val dir = ShareFiles.prepareDir(tempFolder.root, now)

        assertTrue(dir.isDirectory)
        assertEquals(File(tempFolder.root, ShareFiles.DIR_NAME), dir)
        assertEquals("share_cards", ShareFiles.DIR_NAME)
    }

    @Test
    fun `deleteStale removes old cards but keeps recent ones`() {
        val dir = ShareFiles.prepareDir(tempFolder.root, now)
        val old = card(dir, "old.png", now - ShareFiles.MAX_AGE_MILLIS - 1_000)
        val recent = card(dir, "recent.png", now - 1_000)

        val deleted = ShareFiles.deleteStale(dir, now)

        assertEquals(1, deleted)
        assertFalse(old.exists())
        assertTrue(recent.exists())
    }

    @Test
    fun `deleteStale only touches png files`() {
        val dir = ShareFiles.prepareDir(tempFolder.root, now)
        val other = card(dir, "notes.txt", 0L)

        ShareFiles.deleteStale(dir, now)

        assertTrue(other.exists())
    }

    @Test
    fun `prepareDir cleans stale cards from earlier shares`() {
        val dir = File(tempFolder.root, ShareFiles.DIR_NAME).apply { mkdirs() }
        val old = card(dir, "old.png", 0L)

        ShareFiles.prepareDir(tempFolder.root, now)

        assertFalse(old.exists())
    }

    @Test
    fun `deleteStale on a missing directory is a no-op`() {
        assertEquals(0, ShareFiles.deleteStale(File(tempFolder.root, "missing"), now))
    }

    @Test
    fun `new card files are png and unique per timestamp`() {
        val dir = ShareFiles.prepareDir(tempFolder.root, now)
        val first = ShareFiles.newCardFile(dir, 1L)
        val second = ShareFiles.newCardFile(dir, 2L)

        assertEquals("png", first.extension)
        assertFalse(first == second)
        assertEquals(dir, first.parentFile)
    }
}
