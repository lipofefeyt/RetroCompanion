package com.example.retrocompanion.data.polling

import com.example.retrocompanion.data.model.RecentUnlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockNotificationPlanTest {

    private fun unlock(
        id: Int,
        title: String = "Sample $id",
        game: String? = "Sample Game",
        points: Int = 10,
        gameId: Int? = 100
    ) = RecentUnlock(
        achievementId = id,
        title = title,
        gameTitle = game,
        points = points,
        gameId = gameId,
        date = "2026-03-10 12:00:00"
    )

    @Test
    fun `nothing new means nothing to show`() {
        assertTrue(UnlockNotificationPlan.from(emptyList()).isEmpty())
    }

    @Test
    fun `a single unlock shows its title, game and points and opens its game`() {
        val planned = UnlockNotificationPlan.from(listOf(unlock(1, title = "Sample Title", points = 25))).single()

        assertEquals("Sample Title", planned.title)
        assertEquals("Sample Game · 25 pts", planned.text)
        assertEquals(100, planned.gameId)
        assertEquals(1, planned.id)
    }

    @Test
    fun `missing details are left out rather than shown as blanks`() {
        val noGame = UnlockNotificationPlan.from(listOf(unlock(1, game = null, points = 5))).single()
        val bare = UnlockNotificationPlan.from(listOf(unlock(2, title = "", game = " ", points = 0, gameId = null))).single()

        assertEquals("5 pts", noGame.text)
        assertEquals("Achievement unlocked", bare.title)
        assertEquals("Achievement unlocked", bare.text)
        assertNull(bare.gameId)
    }

    @Test
    fun `a few unlocks each get their own notification with distinct ids`() {
        val planned = UnlockNotificationPlan.from((1..UnlockNotificationPlan.MAX_INDIVIDUAL).map { unlock(it) })

        assertEquals(UnlockNotificationPlan.MAX_INDIVIDUAL, planned.size)
        assertEquals(planned.size, planned.map { it.id }.toSet().size)
    }

    @Test
    fun `a burst of unlocks is folded into one summary`() {
        val burst = (1..UnlockNotificationPlan.MAX_INDIVIDUAL + 1).map { unlock(it) }

        val planned = UnlockNotificationPlan.from(burst).single()

        assertEquals("${burst.size} new achievements unlocked", planned.title)
        assertEquals("Sample Game", planned.text)
        assertEquals(UnlockNotificationPlan.SUMMARY_ID, planned.id)
        assertEquals(100, planned.gameId)
    }

    @Test
    fun `a summary across several games names one and counts the rest and does not pick a game`() {
        val burst = (1..6).map { unlock(it, game = "Game ${it % 3}", gameId = it % 3) }

        val planned = UnlockNotificationPlan.from(burst).single()

        assertTrue(planned.text.endsWith("and 2 more"))
        assertNull(planned.gameId)
    }

    @Test
    fun `an individual notification carries the description and badge, a summary does not`() {
        val single = UnlockNotificationPlan.from(
            listOf(unlock(1).copy(description = "Do the thing", badgeName = "00123"))
        ).single()
        assertEquals("Do the thing", single.description)
        assertEquals("https://media.retroachievements.org/Badge/00123.png", single.iconUrl)

        val blank = UnlockNotificationPlan.from(listOf(unlock(2).copy(description = "  "))).single()
        assertNull(blank.description)
        assertNull(blank.iconUrl)

        val summary = UnlockNotificationPlan.from(
            (1..UnlockNotificationPlan.MAX_INDIVIDUAL + 1).map { unlock(it).copy(description = "x", badgeName = "1") }
        ).single()
        assertNull(summary.description)
        assertNull(summary.iconUrl)
    }

    @Test
    fun `notifications never include account details`() {
        val planned = UnlockNotificationPlan.from(listOf(unlock(1)))
        val text = planned.joinToString(" ") { it.title + " " + it.text }
        assertFalse(text.contains("key", ignoreCase = true))
        assertFalse(text.contains("sample_user"))
    }
}
