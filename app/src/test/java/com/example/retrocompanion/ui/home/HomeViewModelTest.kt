package com.example.retrocompanion.ui.home

import android.app.Application
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import com.example.retrocompanion.testutil.FakeRetroAchievementsApi
import com.example.retrocompanion.testutil.FakeUserCredentialsSource
import com.example.retrocompanion.testutil.MainDispatcherRule
import com.example.retrocompanion.testutil.httpException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleCredentials = UserCredentials(username = "sample_user", apiKey = "sample-fake-key")

    @Test
    fun `missing credentials sets status to Missing`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = HomeViewModel(
            Application(),
            FakeUserCredentialsSource(UserCredentials()),
            RetroAchievementsRepository(FakeRetroAchievementsApi())
        )

        advanceUntilIdle()

        assertEquals(CredentialsStatus.Missing, viewModel.uiState.value.credentialsStatus)
    }

    @Test
    fun `valid credentials load both sections successfully`() = runTest(mainDispatcherRule.dispatcher) {
        val games = listOf(RecentlyPlayedGame(gameId = 1, title = "Sample Game"))
        val unlocks = listOf(RecentUnlock(achievementId = 1, title = "Sample Achievement"))
        val api = FakeRetroAchievementsApi(
            recentlyPlayedGamesResult = { games },
            recentAchievementsResult = { unlocks }
        )
        val viewModel = HomeViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api)
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CredentialsStatus.Present, state.credentialsStatus)
        assertEquals("sample_user", state.username)
        assertEquals(SectionState.Success(games), state.recentlyPlayed)
        assertEquals(SectionState.Success(unlocks), state.recentUnlocks)
    }

    @Test
    fun `one section failing does not affect the other`() = runTest(mainDispatcherRule.dispatcher) {
        val unlocks = listOf(RecentUnlock(achievementId = 1, title = "Sample Achievement"))
        val api = FakeRetroAchievementsApi(
            recentlyPlayedGamesResult = { throw httpException(401) },
            recentAchievementsResult = { unlocks }
        )
        val viewModel = HomeViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api)
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        val recentlyPlayedState = state.recentlyPlayed
        assertTrue(recentlyPlayedState is SectionState.Error)
        assertTrue((recentlyPlayedState as SectionState.Error).isInvalidApiKey)
        assertEquals(SectionState.Success(unlocks), state.recentUnlocks)
    }

    @Test
    fun `retryRecentlyPlayed only reloads that section`() = runTest(mainDispatcherRule.dispatcher) {
        var recentlyPlayedCalls = 0
        var recentUnlocksCalls = 0
        val api = FakeRetroAchievementsApi(
            recentlyPlayedGamesResult = { recentlyPlayedCalls++; emptyList() },
            recentAchievementsResult = { recentUnlocksCalls++; emptyList() }
        )
        val viewModel = HomeViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api)
        )
        advanceUntilIdle()
        assertEquals(1, recentlyPlayedCalls)
        assertEquals(1, recentUnlocksCalls)

        viewModel.retryRecentlyPlayed()
        advanceUntilIdle()

        assertEquals(2, recentlyPlayedCalls)
        assertEquals(1, recentUnlocksCalls)
    }

    @Test
    fun `refresh reloads both sections and ends with isRefreshing false`() = runTest(mainDispatcherRule.dispatcher) {
        var recentlyPlayedCalls = 0
        var recentUnlocksCalls = 0
        val api = FakeRetroAchievementsApi(
            recentlyPlayedGamesResult = { recentlyPlayedCalls++; emptyList() },
            recentAchievementsResult = { recentUnlocksCalls++; emptyList() }
        )
        val viewModel = HomeViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api)
        )
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, recentlyPlayedCalls)
        assertEquals(2, recentUnlocksCalls)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }
}
