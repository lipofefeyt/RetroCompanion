package com.example.retrocompanion.ui.home

import android.app.Application
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.model.AchievementOfTheWeek
import com.example.retrocompanion.data.model.AchievementOfTheWeekResponse
import com.example.retrocompanion.data.model.AotwAchievement
import com.example.retrocompanion.data.model.AotwNamed
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

    @Test
    fun `unlocks search query filters recent unlocks by title, case-insensitively`() = runTest(mainDispatcherRule.dispatcher) {
        val unlocks = listOf(
            RecentUnlock(achievementId = 1, title = "Sonic Speedster"),
            RecentUnlock(achievementId = 2, title = "Final Boss Down"),
            RecentUnlock(achievementId = 3, title = "Sonic Collector")
        )
        val api = FakeRetroAchievementsApi(recentAchievementsResult = { unlocks })
        val viewModel = HomeViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            emptyCache()
        )
        advanceUntilIdle()

        viewModel.onUnlocksSearchQueryChanged("sonic")

        assertEquals(
            listOf("Sonic Speedster", "Sonic Collector"),
            viewModel.uiState.value.filteredRecentUnlocks.map { it.title }
        )
    }

    private fun aotwResponse() = AchievementOfTheWeekResponse(
        achievement = AotwAchievement(id = 9001, title = "Sample Weekly Challenge", points = 25),
        game = AotwNamed(id = 4242, title = "Sample Quest Adventure"),
        totalPlayers = 400,
        unlocksCount = 100
    )

    @Test
    fun `achievement of the week loads into its own section`() = runTest(mainDispatcherRule.dispatcher) {
        val api = FakeRetroAchievementsApi(achievementOfTheWeekResult = { aotwResponse() })
        val viewModel = HomeViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            emptyCache()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value.achievementOfTheWeek as SectionState.Success
        assertEquals(9001, state.data.achievementId)
        assertEquals(4242, state.data.gameId)
    }

    @Test
    fun `achievement of the week failing does not affect the other Home sections`() =
        runTest(mainDispatcherRule.dispatcher) {
            val api = FakeRetroAchievementsApi(
                recentlyPlayedGamesResult = { listOf(RecentlyPlayedGame(gameId = 1, title = "Sample Game")) },
                achievementOfTheWeekResult = { throw httpException(500) }
            )
            val viewModel = HomeViewModel(
                Application(),
                FakeUserCredentialsSource(sampleCredentials),
                RetroAchievementsRepository(api),
                emptyCache()
            )

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.achievementOfTheWeek is SectionState.Error)
            assertTrue(viewModel.uiState.value.recentlyPlayed is SectionState.Success)
        }

    @Test
    fun `cached achievement of the week stays visible when the refresh fails`() =
        runTest(mainDispatcherRule.dispatcher) {
            val cache = emptyCache()
            val cached = AchievementOfTheWeek(
                achievementId = 9001, title = "Cached Challenge", description = null, points = 10,
                badgeName = null, gameId = 4242, gameTitle = "Sample Quest Adventure", consoleName = null,
                startAt = null, totalPlayers = 0, unlocksCount = 0, unlocksHardcoreCount = 0, unlockedByUser = false
            )
            cache.write(CacheKeys.achievementOfTheWeek(sampleCredentials.username), cached)
            val api = FakeRetroAchievementsApi(achievementOfTheWeekResult = { throw httpException(500) })
            val viewModel = HomeViewModel(
                Application(),
                FakeUserCredentialsSource(sampleCredentials),
                RetroAchievementsRepository(api),
                cache
            )

            advanceUntilIdle()

            val state = viewModel.uiState.value.achievementOfTheWeek as SectionState.Success
            assertEquals("Cached Challenge", state.data.title)
            assertTrue(state.isStale)
        }
}
