package com.example.retrocompanion.ui.gamedetails

import com.example.retrocompanion.data.model.GameAchievement
import com.example.retrocompanion.data.model.GameInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementSortTest {

    // Display order is a, b, c, d. "d" is still locked.
    private val a = GameAchievement(
        id = 1, title = "Banana", points = 10, displayOrder = 1, numAwarded = 300,
        dateEarned = "2026-01-02 10:00:00"
    )
    private val b = GameAchievement(
        id = 2, title = "apple", points = 50, displayOrder = 2, numAwarded = 20,
        dateEarned = "2026-03-05 09:30:00"
    )
    private val c = GameAchievement(
        id = 3, title = "Cherry", points = 25, displayOrder = 3, numAwarded = 90,
        dateEarnedHardcore = "2026-02-01 12:00:00"
    )
    private val d = GameAchievement(id = 4, title = "Date", points = 5, displayOrder = 4, numAwarded = 10)

    private fun titles(sort: AchievementSort): List<String> =
        GameDetailsUiState.Success(
            gameInfo = GameInfo(
                gameId = 1,
                achievements = listOf(a, b, c, d).associateBy { it.id.toString() }
            ),
            sort = sort
        ).filteredAchievements.map { it.title }

    @Test
    fun `default keeps the game's display order`() =
        assertEquals(listOf("Banana", "apple", "Cherry", "Date"), titles(AchievementSort.DEFAULT))

    @Test
    fun `date puts the newest unlock first and locked achievements last`() =
        assertEquals(listOf("apple", "Cherry", "Banana", "Date"), titles(AchievementSort.DATE))

    @Test
    fun `points puts the highest value first`() =
        assertEquals(listOf("apple", "Cherry", "Banana", "Date"), titles(AchievementSort.POINTS))

    @Test
    fun `rarity puts the fewest earners first`() =
        assertEquals(listOf("Date", "apple", "Cherry", "Banana"), titles(AchievementSort.RARITY))

    @Test
    fun `title sorts alphabetically ignoring case`() =
        assertEquals(listOf("apple", "Banana", "Cherry", "Date"), titles(AchievementSort.TITLE))
}
