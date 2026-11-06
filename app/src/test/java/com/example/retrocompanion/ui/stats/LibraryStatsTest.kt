package com.example.retrocompanion.ui.stats

import com.example.retrocompanion.data.model.GameProgressSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryStatsTest {

    @Test
    fun `aggregates totals, completion, hardcore ratio, and mastered-near-complete counts`() {
        val games = listOf(
            GameProgressSummary(gameId = 1, title = "Mastered", consoleName = "NES", maxPossible = 10, numAwarded = 10, numAwardedHardcore = 10),
            GameProgressSummary(gameId = 2, title = "Near Complete", consoleName = "NES", maxPossible = 10, numAwarded = 9, numAwardedHardcore = 0),
            GameProgressSummary(gameId = 3, title = "Started", consoleName = "SNES", maxPossible = 20, numAwarded = 5, numAwardedHardcore = 5)
        )

        val stats = LibraryStats.from(games)

        assertEquals(3, stats.totalGames)
        assertEquals(24, stats.totalAchievementsEarned)
        assertEquals(40, stats.totalAchievementsPossible)
        assertEquals(60, stats.overallCompletionPercent)
        assertEquals(62, stats.hardcoreRatioPercent)
        assertEquals(1, stats.masteredGamesCount)
        assertEquals(1, stats.nearCompleteGamesCount)
        assertEquals(
            listOf("NES" to 2, "SNES" to 1),
            stats.byConsole.map { it.consoleName to it.gameCount }
        )
    }

    @Test
    fun `empty library produces zeroed stats without dividing by zero`() {
        val stats = LibraryStats.from(emptyList())

        assertEquals(0, stats.totalGames)
        assertEquals(0, stats.overallCompletionPercent)
        assertEquals(0, stats.hardcoreRatioPercent)
        assertEquals(emptyList<ConsoleStats>(), stats.byConsole)
    }

    @Test
    fun `games with a blank console name are grouped as Unknown`() {
        val games = listOf(
            GameProgressSummary(gameId = 1, title = "No Console", consoleName = null, maxPossible = 5, numAwarded = 1)
        )

        val stats = LibraryStats.from(games)

        assertEquals(listOf("Unknown"), stats.byConsole.map { it.consoleName })
    }
}
