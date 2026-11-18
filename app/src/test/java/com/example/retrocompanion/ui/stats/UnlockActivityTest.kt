package com.example.retrocompanion.ui.stats

import com.example.retrocompanion.data.model.UnlockStamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class UnlockActivityTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun millis(date: String) = UnlockActivity.parseUnlockMillis(date)!!

    /** A Wednesday at midday UTC. */
    private val now = millis("2026-03-11 12:00:00")

    private fun stamp(id: Int, date: String) = UnlockStamp(achievementId = id, date = date)

    private fun activity(vararg stamps: UnlockStamp, zone: TimeZone = utc, window: Int = 182) =
        UnlockActivity.from(stamps.toList(), now, zone, window)

    @Test
    fun `empty input gives zeroed activity`() {
        val result = activity()
        assertEquals(0, result.totalUnlocks)
        assertEquals(0, result.activeDays)
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
        assertTrue(result.weeks.all { week -> week.days.all { it == null || it.count == 0 } })
    }

    @Test
    fun `current streak counts consecutive days ending today`() {
        val result = activity(
            stamp(1, "2026-03-09 08:00:00"),
            stamp(2, "2026-03-10 08:00:00"),
            stamp(3, "2026-03-11 08:00:00")
        )
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }

    @Test
    fun `current streak survives a day with no unlocks yet today`() {
        val result = activity(
            stamp(1, "2026-03-09 08:00:00"),
            stamp(2, "2026-03-10 08:00:00")
        )
        assertEquals(2, result.currentStreak)
    }

    @Test
    fun `current streak is zero once a full day has been missed`() {
        val result = activity(
            stamp(1, "2026-03-07 08:00:00"),
            stamp(2, "2026-03-08 08:00:00")
        )
        assertEquals(0, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun `longest streak can be an earlier run`() {
        val result = activity(
            stamp(1, "2026-01-01 08:00:00"),
            stamp(2, "2026-01-02 08:00:00"),
            stamp(3, "2026-01-03 08:00:00"),
            stamp(4, "2026-01-04 08:00:00"),
            stamp(5, "2026-03-11 08:00:00")
        )
        assertEquals(1, result.currentStreak)
        assertEquals(4, result.longestStreak)
        assertEquals(5, result.activeDays)
    }

    @Test
    fun `several unlocks on one day are one active day`() {
        val result = activity(
            stamp(1, "2026-03-11 01:00:00"),
            stamp(2, "2026-03-11 02:00:00"),
            stamp(3, "2026-03-11 03:00:00")
        )
        assertEquals(3, result.totalUnlocks)
        assertEquals(1, result.activeDays)
        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `the same achievement unlocked in both modes is counted once at its earliest date`() {
        val result = activity(
            stamp(1, "2026-03-10 08:00:00"),
            stamp(1, "2026-03-11 08:00:00")
        )
        assertEquals(1, result.totalUnlocks)
        assertEquals(1, result.activeDays)
        // Counted on the 10th, so today (the 11th) has none but yesterday's streak is intact.
        assertEquals(1, result.currentStreak)
    }

    @Test
    fun `day boundaries follow the device time zone`() {
        val plusTwo = TimeZone.getTimeZone("GMT+02:00")
        // 23:30 UTC on the 10th is already 01:30 on the 11th at +02:00.
        val result = activity(stamp(1, "2026-03-10 23:30:00"), zone = plusTwo)
        val today = UnlockActivity.localEpochDay(now, plusTwo)
        val countedDay = result.weeks.flatMap { it.days }.filterNotNull().single { it.count > 0 }
        assertEquals(today, countedDay.epochDay)
    }

    @Test
    fun `unlocks older than the window are ignored`() {
        val result = activity(
            stamp(1, "2025-01-01 08:00:00"),
            stamp(2, "2026-03-11 08:00:00"),
            window = 30
        )
        assertEquals(1, result.totalUnlocks)
    }

    @Test
    fun `unparseable and future-dated entries are skipped`() {
        val result = activity(
            stamp(1, "not a date"),
            stamp(2, ""),
            stamp(3, "2026-03-20 08:00:00"),
            stamp(4, "2026-03-11 08:00:00")
        )
        assertEquals(1, result.totalUnlocks)
    }

    @Test
    fun `heatmap weeks start on Monday and mark days outside the window as null`() {
        val result = activity(window = 10)
        val today = UnlockActivity.localEpochDay(now, utc)
        val start = today - 9

        assertTrue(result.weeks.all { it.days.size == 7 })
        assertEquals(0, UnlockActivity.dayOfWeekIndex(result.weeks.first().startEpochDay))
        // Wednesday today: Thu..Sun of the last week haven't happened yet.
        assertEquals(listOf(true, true, true, false, false, false, false), result.weeks.last().days.map { it != null })
        val inWindow = result.weeks.flatMap { it.days }.filterNotNull()
        assertEquals(10, inWindow.size)
        assertEquals(start, inWindow.first().epochDay)
        assertEquals(today, inWindow.last().epochDay)
    }

    @Test
    fun `heat levels scale from 1 to 4 relative to the busiest day`() {
        val busy = (1..8).map { stamp(it, "2026-03-10 0$it:00:00") }
        val quiet = stamp(100, "2026-03-09 08:00:00")
        val result = UnlockActivity.from(busy + quiet, now, utc)
        val byDay = result.weeks.flatMap { it.days }.filterNotNull().associateBy { it.epochDay }
        val today = UnlockActivity.localEpochDay(now, utc)

        assertEquals(4, byDay.getValue(today - 1).level)
        assertEquals(1, byDay.getValue(today - 2).level)
        assertEquals(0, byDay.getValue(today).level)
    }

    @Test
    fun `week totals sum their days`() {
        val result = activity(
            stamp(1, "2026-03-09 08:00:00"),
            stamp(2, "2026-03-09 09:00:00"),
            stamp(3, "2026-03-10 09:00:00")
        )
        val week = result.weeks.last()
        assertEquals(3, week.totalUnlocks)
        assertEquals(2, week.activeDays)
    }

    @Test
    fun `parseUnlockMillis reads the API format as UTC and rejects garbage`() {
        assertEquals(0L, UnlockActivity.parseUnlockMillis("1970-01-01 00:00:00"))
        assertNotNull(UnlockActivity.parseUnlockMillis("2023-12-27 16:04:50"))
        assertNull(UnlockActivity.parseUnlockMillis("2023-13-45 99:99:99"))
        assertNull(UnlockActivity.parseUnlockMillis("yesterday"))
    }
}
