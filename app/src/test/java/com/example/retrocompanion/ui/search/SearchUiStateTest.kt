package com.example.retrocompanion.ui.search

import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.ui.common.SectionState
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchUiStateTest {

    private val games = listOf(
        GameProgressSummary(gameId = 1, title = "Super Mario Bros."),
        GameProgressSummary(gameId = 2, title = "Sonic the Hedgehog")
    )

    private val unlocks = listOf(
        RecentUnlock(achievementId = 1, title = "Mushroom Kingdom Hero", gameId = 1),
        RecentUnlock(achievementId = 2, title = "Speedster", gameId = 2)
    )

    private fun state(query: String) = SearchUiState(
        query = query,
        games = SectionState.Success(games),
        recentUnlocks = SectionState.Success(unlocks)
    )

    @Test
    fun `blank query returns no results from either section`() {
        val uiState = state("")
        assertEquals(emptyList<GameProgressSummary>(), uiState.filteredGames)
        assertEquals(emptyList<RecentUnlock>(), uiState.filteredUnlocks)
    }

    @Test
    fun `query matches game title case-insensitively`() {
        val uiState = state("mario")
        assertEquals(listOf(games[0]), uiState.filteredGames)
    }

    @Test
    fun `query matches achievement title case-insensitively`() {
        val uiState = state("SPEEDSTER")
        assertEquals(listOf(unlocks[1]), uiState.filteredUnlocks)
    }

    @Test
    fun `query with no matches returns empty lists`() {
        val uiState = state("zzz-no-such-thing")
        assertEquals(emptyList<GameProgressSummary>(), uiState.filteredGames)
        assertEquals(emptyList<RecentUnlock>(), uiState.filteredUnlocks)
    }

    @Test
    fun `non-success section states never contribute results`() {
        val uiState = SearchUiState(
            query = "mario",
            games = SectionState.Loading,
            recentUnlocks = SectionState.Error(message = "failed", isInvalidApiKey = false)
        )
        assertEquals(emptyList<GameProgressSummary>(), uiState.filteredGames)
        assertEquals(emptyList<RecentUnlock>(), uiState.filteredUnlocks)
    }
}
