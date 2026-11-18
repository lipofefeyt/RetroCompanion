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

    private fun game(id: Int, title: String, awarded: Int, possible: Int) =
        GameProgressSummary(gameId = id, title = title, maxPossible = possible, numAwarded = awarded)

    @Test
    fun `closest to mastery ranks by completion and excludes mastered, unstarted and empty games`() {
        val games = listOf(
            game(1, "Mastered", 10, 10),
            game(2, "Unstarted", 0, 10),
            game(3, "No Achievements", 0, 0),
            game(4, "Halfway", 5, 10),
            game(5, "Almost", 9, 10),
            game(6, "Barely", 1, 10)
        )

        assertEquals(
            listOf("Almost", "Halfway", "Barely"),
            LibraryStats.closestToMastery(games).map { it.title }
        )
    }

    @Test
    fun `closest to mastery breaks completion ties by fewest remaining then title`() {
        val games = listOf(
            game(1, "Zeta Big", 50, 100),
            game(2, "Beta Small", 5, 10),
            game(3, "Alpha Small", 5, 10)
        )

        assertEquals(
            listOf("Alpha Small", "Beta Small", "Zeta Big"),
            LibraryStats.closestToMastery(games).map { it.title }
        )
    }

    @Test
    fun `closest to mastery uses exact completion rather than rounded percent`() {
        val games = listOf(
            game(1, "Lower", 991, 1000),
            game(2, "Higher", 995, 1000)
        )

        assertEquals(listOf("Higher", "Lower"), LibraryStats.closestToMastery(games).map { it.title })
    }

    @Test
    fun `closest to mastery is limited and empty for an empty library`() {
        val games = (1..8).map { game(it, "Game $it", it, 10) }

        assertEquals(LibraryStats.CLOSEST_TO_MASTERY_LIMIT, LibraryStats.closestToMastery(games).size)
        assertEquals(3, LibraryStats.closestToMastery(games, limit = 3).size)
        assertEquals(8, LibraryStats.closestToMastery(games, limit = 20).size)
        assertEquals(emptyList<GameProgressSummary>(), LibraryStats.closestToMastery(emptyList()))
    }

    @Test
    fun `from includes the closest to mastery list`() {
        val stats = LibraryStats.from(listOf(game(1, "Almost", 9, 10), game(2, "Mastered", 10, 10)))
        assertEquals(listOf("Almost"), stats.closestToMastery.map { it.title })
    }
}
