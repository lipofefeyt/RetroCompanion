package com.example.retrocompanion.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockChunksTest {

    private val now = UnlockActivity.parseUnlockMillis("2026-03-11 12:00:00")!!
    private val nowSeconds = now / 1000

    @Test
    fun `chunks are contiguous, oldest first, and never extend past now`() {
        val chunks = UnlockChunks.forWindow(now, UnlockActivity.DEFAULT_WINDOW_DAYS)

        // 90-day chunks: a 26-week window is covered by only a few requests.
        assertTrue(chunks.size in 3..4)
        chunks.zipWithNext().forEach { (a, b) ->
            assertEquals(a.index + 1, b.index)
            assertEquals(a.toEpochSeconds + 1, b.fromEpochSeconds)
        }
        assertEquals(nowSeconds, chunks.last().toEpochSeconds)
    }

    @Test
    fun `chunks cover the whole window including the padding day`() {
        val windowDays = 182
        val chunks = UnlockChunks.forWindow(now, windowDays)
        val oldestNeededSeconds = nowSeconds - (windowDays + 1) * 86_400L

        assertTrue(chunks.first().fromEpochSeconds <= oldestNeededSeconds)
    }

    @Test
    fun `chunk boundaries are fixed so cache keys stay stable between runs`() {
        val earlier = UnlockChunks.forWindow(now, 182)
        val later = UnlockChunks.forWindow(now + 86_400_000L, 182)

        val earlierByIndex = earlier.associateBy { it.index }
        later.filter { it.index in earlierByIndex && it.index != later.last().index }.forEach { chunk ->
            assertEquals(earlierByIndex.getValue(chunk.index).fromEpochSeconds, chunk.fromEpochSeconds)
            assertEquals(earlierByIndex.getValue(chunk.index).toEpochSeconds, chunk.toEpochSeconds)
        }
    }

    @Test
    fun `only chunks that ended more than the settle period ago count as settled`() {
        val chunks = UnlockChunks.forWindow(now, UnlockActivity.DEFAULT_WINDOW_DAYS)

        assertFalse(chunks.last().isSettled(now))
        assertTrue(chunks.first().isSettled(now))

        val justEnded = UnlockChunks.Chunk(index = 0, fromEpochSeconds = 0, toEpochSeconds = nowSeconds - 3600)
        assertFalse(justEnded.isSettled(now))
        val longAgo = UnlockChunks.Chunk(
            index = 0,
            fromEpochSeconds = 0,
            toEpochSeconds = nowSeconds - (UnlockChunks.SETTLE_DAYS + 1) * 86_400L
        )
        assertTrue(longAgo.isSettled(now))
    }
}
