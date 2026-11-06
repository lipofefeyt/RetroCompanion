package com.example.retrocompanion.ui.home

import android.app.Application
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import com.example.retrocompanion.testutil.FakeCacheDao
import com.example.retrocompanion.testutil.FakeRetroAchievementsApi
import com.example.retrocompanion.testutil.FakeUserCredentialsSource
import com.example.retrocompanion.testutil.MainDispatcherRule
import com.example.retrocompanion.testutil.httpException
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleCredentials = UserCredentials(username = "sample_user", apiKey = "sample-fake-key")

    private fun emptyCache() = ResponseCache(FakeCacheDao())

    @Test
    fun `missing credentials sets status to Missing`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = HomeViewModel(
            Application(),
            FakeUserCredentialsSource(UserCredentials()),
            RetroAchievementsRepository(FakeRetroAchievementsApi()),
            emptyCache()
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
            RetroAchievementsRepository(api),
            emptyCache()
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
            RetroAchievementsRepository(api),
            emptyCache()
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
            RetroAchievementsRepository(api),
            emptyCache()
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
            RetroAchievementsRepository(api),
            emptyCache()
        )
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, recentlyPlayedCalls)
        assertEquals(2, recentUnlocksCalls)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `cached data is shown immediately as stale while the network call is still in flight`() =
        runTest(mainDispatcherRule.dispatcher) {
            val cachedGames = listOf(RecentlyPlayedGame(gameId = 1, title = "Cached Game"))
            val freshGames = listOf(RecentlyPlayedGame(gameId = 2, title = "Fresh Game"))
            val cache = emptyCache()
            cache.write(CacheKeys.recentlyPlayed(sampleCredentials.username), cachedGames)

            val networkGate = CompletableDeferred<Unit>()
            val api = FakeRetroAchievementsApi(
                recentlyPlayedGamesResult = { networkGate.await(); freshGames }
            )
            val viewModel = HomeViewModel(
                Application(),
                FakeUserCredentialsSource(sampleCredentials),
                RetroAchievementsRepository(api),
                cache
            )

            runCurrent()

            val midFlightState = viewModel.uiState.value.recentlyPlayed
            assertTrue(midFlightState is SectionState.Success)
            assertTrue((midFlightState as SectionState.Success).isStale)
            assertEquals(cachedGames, midFlightState.data)

            networkGate.complete(Unit)
            advanceUntilIdle()

            val finalState = viewModel.uiState.value.recentlyPlayed
            assertTrue(finalState is SectionState.Success)
            assertFalse((finalState as SectionState.Success).isStale)
            assertEquals(freshGames, finalState.data)
        }

    @Test
    fun `network failure keeps showing cached data instead of erroring`() = runTest(mainDispatcherRule.dispatcher) {
        val cachedGames = listOf(RecentlyPlayedGame(gameId = 1, title = "Cached Game"))
        val cache = emptyCache()
        cache.write(CacheKeys.recentlyPlayed(sampleCredentials.username), cachedGames)
        val api = FakeRetroAchievementsApi(recentlyPlayedGamesResult = { throw httpException(500) })
        val viewModel = HomeViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            cache
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value.recentlyPlayed
        assertTrue(state is SectionState.Success)
        assertTrue((state as SectionState.Success).isStale)
        assertEquals(cachedGames, state.data)
    }

    @Test
    fun `successful fetch writes fresh data to the cache`() = runTest(mainDispatcherRule.dispatcher) {
        val freshGames = listOf(RecentlyPlayedGame(gameId = 9, title = "Fresh Game"))
        val cacheDao = FakeCacheDao()
        val cache = ResponseCache(cacheDao)
        val api = FakeRetroAchievementsApi(recentlyPlayedGamesResult = { freshGames })
        val viewModel = HomeViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            cache
        )

        advanceUntilIdle()

        assertNotNull(cacheDao.get(CacheKeys.recentlyPlayed(sampleCredentials.username)))
    }
}
