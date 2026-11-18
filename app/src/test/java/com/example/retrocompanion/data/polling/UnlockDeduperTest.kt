package com.example.retrocompanion.data.polling

import com.example.retrocompanion.data.model.RecentUnlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockDeduperTest {

    private val user = "sample_user"
    private val minute = 60_000L
    private val now = 1_800_000_000_000L

    private fun unlock(id: Int, date: String, hardcore: Int = 0) =
        RecentUnlock(date = date, achievementId = id, title = "Sample $id", hardcoreMode = hardcore)

    private fun state(
        lastRun: Long? = now - 15 * minute,
        highWater: String? = "2026-03-10 12:00:00",
        username: String = user,
        vararg seen: SeenUnlock
    ) = PollingState(username, lastRun, highWater, seen.toList())

    @Test
    fun `first run records the backlog but notifies nothing`() {
        val result = UnlockDeduper.diff(
            listOf(unlock(1, "2026-03-10 11:00:00"), unlock(2, "2026-03-10 11:30:00")),
            PollingState(), user, now
        )

        assertTrue(result.wasBaseline)
        assertTrue(result.newUnlocks.isEmpty())
        assertEquals(setOf(1, 2), result.state.seen.map { it.achievementId }.toSet())
        assertEquals("2026-03-10 11:30:00", result.state.highWaterDate)
        assertEquals(now, result.state.lastRunMillis)
        assertEquals(user, result.state.username)
    }

    @Test
    fun `later run notifies only unlocks it has not seen, oldest first`() {
        val previous = state(seen = arrayOf(SeenUnlock(1, "2026-03-10 11:00:00")))
        val result = UnlockDeduper.diff(
            listOf(
                unlock(3, "2026-03-10 12:30:00"),
                unlock(1, "2026-03-10 11:00:00"), // overlap with the previous window
                unlock(2, "2026-03-10 12:10:00")
            ),
            previous, user, now
        )

        assertFalse(result.wasBaseline)
        assertEquals(listOf(2, 3), result.newUnlocks.map { it.achievementId })
        assertEquals("2026-03-10 12:30:00", result.state.highWaterDate)
        assertEquals(setOf(1, 2, 3), result.state.seen.map { it.achievementId }.toSet())
    }

    @Test
    fun `an empty response notifies nothing and keeps the existing state`() {
        val previous = state(seen = arrayOf(SeenUnlock(1, "2026-03-10 11:00:00")))
        val result = UnlockDeduper.diff(emptyList(), previous, user, now)

        assertTrue(result.newUnlocks.isEmpty())
        assertEquals(previous.highWaterDate, result.state.highWaterDate)
        assertEquals(previous.seen, result.state.seen)
        assertEquals(now, result.state.lastRunMillis)
    }

    @Test
    fun `running again over the same response never notifies twice`() {
        val fetched = listOf(unlock(5, "2026-03-10 12:30:00"))
        val first = UnlockDeduper.diff(fetched, state(), user, now)
        val second = UnlockDeduper.diff(fetched, first.state, user, now + 15 * minute)

        assertEquals(1, first.newUnlocks.size)
        assertTrue(second.newUnlocks.isEmpty())
    }

    @Test
    fun `an achievement listed in both softcore and hardcore mode is notified once`() {
        val result = UnlockDeduper.diff(
            listOf(
                unlock(7, "2026-03-10 12:40:00", hardcore = 1),
                unlock(7, "2026-03-10 12:35:00", hardcore = 0)
            ),
            state(), user, now
        )

        assertEquals(1, result.newUnlocks.size)
        assertEquals("2026-03-10 12:35:00", result.newUnlocks.single().date)
    }

    @Test
    fun `an unknown unlock older than the high-water mark is a late sync and is not announced`() {
        val result = UnlockDeduper.diff(
            listOf(unlock(9, "2026-03-09 08:00:00")),
            state(), user, now
        )

        assertTrue(result.newUnlocks.isEmpty())
        // ...but it is remembered so it can't surface later either.
        assertTrue(result.state.seen.any { it.achievementId == 9 })
    }

    @Test
    fun `unlocks with a missing date or id are ignored`() {
        val result = UnlockDeduper.diff(
            listOf(
                RecentUnlock(date = null, achievementId = 1),
                RecentUnlock(date = " ", achievementId = 2),
                RecentUnlock(date = "2026-03-10 12:30:00", achievementId = 0)
            ),
            state(), user, now
        )

        assertTrue(result.newUnlocks.isEmpty())
        assertTrue(result.state.seen.isEmpty())
    }

    @Test
    fun `a different account starts over without notifying`() {
        val previous = state(username = "someone_else", seen = arrayOf(SeenUnlock(1, "2026-03-10 11:00:00")))
        val result = UnlockDeduper.diff(listOf(unlock(2, "2026-03-10 12:30:00")), previous, user, now)

        assertTrue(result.wasBaseline)
        assertTrue(result.newUnlocks.isEmpty())
        assertEquals(user, result.state.username)
        assertEquals(listOf(2), result.state.seen.map { it.achievementId })
    }

    @Test
    fun `a long gap since the last run, such as notifications being off, starts over`() {
        val previous = state(lastRun = now - UnlockDeduper.BASELINE_AFTER_MILLIS - minute)
        val result = UnlockDeduper.diff(listOf(unlock(2, "2026-03-10 12:30:00")), previous, user, now)

        assertTrue(result.wasBaseline)
        assertTrue(result.newUnlocks.isEmpty())
    }

    @Test
    fun `remembered unlocks are capped to the newest ones`() {
        val many = (1..UnlockDeduper.MAX_SEEN + 50).map {
            unlock(it, "2026-03-10 12:%02d:%02d".format(it / 60 % 60, it % 60))
        }
        val result = UnlockDeduper.diff(many, state(highWater = null), user, now)

        assertEquals(UnlockDeduper.MAX_SEEN, result.state.seen.size)
        assertEquals(many.maxOf { it.date!! }, result.state.seen.first().date)
    }

    @Test
    fun `lookback is the time since the last run plus an overlap, within bounds`() {
        fun lookback(minutesAgo: Long) =
            UnlockDeduper.lookbackMinutes(state(lastRun = now - minutesAgo * minute), user, now)

        assertEquals(60, UnlockDeduper.lookbackMinutes(PollingState(), user, now))
        assertEquals(UnlockDeduper.MIN_LOOKBACK_MINUTES, lookback(5))
        assertEquals(15 + UnlockDeduper.OVERLAP_MINUTES, lookback(15))
        assertEquals(600 + UnlockDeduper.OVERLAP_MINUTES, lookback(600))
        assertEquals(60, UnlockDeduper.lookbackMinutes(state(username = "someone_else"), user, now))
    }

    @Test
    fun `state round-trips through its stored form and bad data decodes to a fresh state`() {
        val original = state(seen = arrayOf(SeenUnlock(1, "2026-03-10 11:00:00")))

        assertEquals(original, decodePollingState(encodePollingState(original)))
        assertEquals(PollingState(), decodePollingState(null))
        assertEquals(PollingState(), decodePollingState("{not json"))
    }
}
