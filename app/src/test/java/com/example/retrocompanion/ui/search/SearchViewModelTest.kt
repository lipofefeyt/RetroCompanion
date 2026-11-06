package com.example.retrocompanion.ui.search

import android.app.Application
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.UserCompletionProgressPage
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import com.example.retrocompanion.testutil.FakeCacheDao
import com.example.retrocompanion.testutil.FakeRetroAchievementsApi
import com.example.retrocompanion.testutil.FakeUserCredentialsSource
import com.example.retrocompanion.testutil.MainDispatcherRule
import com.example.retrocompanion.testutil.httpException
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleCredentials = UserCredentials(username = "sample_user", apiKey = "sample-fake-key")

    private fun emptyCache() = ResponseCache(FakeCacheDao())

    @Test
    fun `missing credentials sets status to Missing`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = SearchViewModel(
            Application(),
            FakeUserCredentialsSource(UserCredentials()),
            RetroAchievementsRepository(FakeRetroAchievementsApi()),
            emptyCache()
        )

        advanceUntilIdle()

        assertEquals(CredentialsStatus.Missing, viewModel.uiState.value.credentialsStatus)
    }

    @Test
    fun `valid credentials load games and recent unlocks, and a query filters both`() = runTest(mainDispatcherRule.dispatcher) {
        val games = listOf(GameProgressSummary(gameId = 1, title = "Chrono Trigger"))
        val unlocks = listOf(RecentUnlock(achievementId = 1, title = "Time Traveler", gameId = 1))
        val api = FakeRetroAchievementsApi(
            completionProgressResult = { count, _ -> UserCompletionProgressPage(count = count, total = games.size, results = games) },
            recentAchievementsResult = { unlocks }
        )
        val viewModel = SearchViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            emptyCache()
        )

        advanceUntilIdle()

        assertEquals(CredentialsStatus.Present, viewModel.uiState.value.credentialsStatus)
        assertTrue(viewModel.uiState.value.filteredGames.isEmpty())

        viewModel.onQueryChanged("chrono")
        assertEquals(games, viewModel.uiState.value.filteredGames)
        assertEquals(emptyList<RecentUnlock>(), viewModel.uiState.value.filteredUnlocks)

        viewModel.onQueryChanged("traveler")
        assertEquals(unlocks, viewModel.uiState.value.filteredUnlocks)
        assertEquals(emptyList<GameProgressSummary>(), viewModel.uiState.value.filteredGames)
    }

    @Test
    fun `network failure on one section falls back to its stale cache without blanking the other`() = runTest(mainDispatcherRule.dispatcher) {
        val cachedGames = listOf(GameProgressSummary(gameId = 1, title = "Cached Game"))
        val freshUnlocks = listOf(RecentUnlock(achievementId = 2, title = "Fresh Unlock", gameId = 2))
        val cache = emptyCache()
        cache.write(CacheKeys.gameLibrary(sampleCredentials.username), cachedGames)
        val api = FakeRetroAchievementsApi(
            completionProgressResult = { _, _ -> throw httpException(500) },
            recentAchievementsResult = { freshUnlocks }
        )
        val viewModel = SearchViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            cache
        )

        advanceUntilIdle()

        val gamesState = viewModel.uiState.value.games
        assertTrue(gamesState is SectionState.Success)
        assertTrue((gamesState as SectionState.Success).isStale)
        assertEquals(cachedGames, gamesState.data)

        val unlocksState = viewModel.uiState.value.recentUnlocks
        assertTrue(unlocksState is SectionState.Success)
        assertEquals(freshUnlocks, (unlocksState as SectionState.Success).data)
    }
}
