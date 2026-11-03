package com.example.retrocompanion.ui.gamedetails

import android.app.Application
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.model.GameInfo
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import com.example.retrocompanion.testutil.FakeCacheDao
import com.example.retrocompanion.testutil.FakeRetroAchievementsApi
import com.example.retrocompanion.testutil.FakeUserCredentialsSource
import com.example.retrocompanion.testutil.MainDispatcherRule
import com.example.retrocompanion.testutil.httpException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleCredentials = UserCredentials(username = "sample_user", apiKey = "sample-fake-key")

    private fun emptyCache() = ResponseCache(FakeCacheDao())

    @Test
    fun `valid credentials and successful fetch produce Success with the game info`() =
        runTest(mainDispatcherRule.dispatcher) {
            val gameInfo = GameInfo(gameId = 42, title = "Sample Game")
            val api = FakeRetroAchievementsApi(gameInfoResult = { gameInfo })
            val viewModel = GameDetailsViewModel(
                Application(),
                gameId = 42,
                userPreferencesRepository = FakeUserCredentialsSource(sampleCredentials),
                repository = RetroAchievementsRepository(api),
                responseCache = emptyCache()
            )

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is GameDetailsUiState.Success)
            assertEquals(gameInfo, (state as GameDetailsUiState.Success).gameInfo)
        }

    @Test
    fun `missing credentials produce an invalid-key error without calling the API`() =
        runTest(mainDispatcherRule.dispatcher) {
            var apiCalled = false
            val api = FakeRetroAchievementsApi(gameInfoResult = { apiCalled = true; GameInfo() })
            val viewModel = GameDetailsViewModel(
                Application(),
                gameId = 42,
                userPreferencesRepository = FakeUserCredentialsSource(UserCredentials()),
                repository = RetroAchievementsRepository(api),
                responseCache = emptyCache()
            )

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is GameDetailsUiState.Error)
            assertTrue((state as GameDetailsUiState.Error).isInvalidApiKey)
            assertEquals(false, apiCalled)
        }

    @Test
    fun `a rejected key from the API produces an invalid-key error`() = runTest(mainDispatcherRule.dispatcher) {
        val api = FakeRetroAchievementsApi(gameInfoResult = { throw httpException(401) })
        val viewModel = GameDetailsViewModel(
            Application(),
            gameId = 42,
            userPreferencesRepository = FakeUserCredentialsSource(sampleCredentials),
            repository = RetroAchievementsRepository(api),
            responseCache = emptyCache()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is GameDetailsUiState.Error)
        assertTrue((state as GameDetailsUiState.Error).isInvalidApiKey)
    }

    @Test
    fun `retry can recover from an error to a successful load`() = runTest(mainDispatcherRule.dispatcher) {
        var shouldFail = true
        val gameInfo = GameInfo(gameId = 42, title = "Sample Game")
        val api = FakeRetroAchievementsApi(
            gameInfoResult = { if (shouldFail) throw httpException(500) else gameInfo }
        )
        val viewModel = GameDetailsViewModel(
            Application(),
            gameId = 42,
            userPreferencesRepository = FakeUserCredentialsSource(sampleCredentials),
            repository = RetroAchievementsRepository(api),
            responseCache = emptyCache()
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is GameDetailsUiState.Error)

        shouldFail = false
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is GameDetailsUiState.Success)
        assertEquals(gameInfo, (state as GameDetailsUiState.Success).gameInfo)
    }

    @Test
    fun `network failure keeps showing cached game info instead of erroring`() = runTest(mainDispatcherRule.dispatcher) {
        val cachedInfo = GameInfo(gameId = 42, title = "Cached Game")
        val cache = emptyCache()
        cache.write(CacheKeys.gameInfo(sampleCredentials.username, 42), cachedInfo)
        val api = FakeRetroAchievementsApi(gameInfoResult = { throw httpException(500) })
        val viewModel = GameDetailsViewModel(
            Application(),
            gameId = 42,
            userPreferencesRepository = FakeUserCredentialsSource(sampleCredentials),
            repository = RetroAchievementsRepository(api),
            responseCache = cache
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is GameDetailsUiState.Success)
        assertTrue((state as GameDetailsUiState.Success).isStale)
        assertEquals(cachedInfo, state.gameInfo)
    }

    @Test
    fun `successful fetch writes fresh game info to the cache`() = runTest(mainDispatcherRule.dispatcher) {
        val freshInfo = GameInfo(gameId = 42, title = "Fresh Game")
        val cacheDao = FakeCacheDao()
        val api = FakeRetroAchievementsApi(gameInfoResult = { freshInfo })
        val viewModel = GameDetailsViewModel(
            Application(),
            gameId = 42,
            userPreferencesRepository = FakeUserCredentialsSource(sampleCredentials),
            repository = RetroAchievementsRepository(api),
            responseCache = ResponseCache(cacheDao)
        )

        advanceUntilIdle()

        assertTrue(cacheDao.get(CacheKeys.gameInfo(sampleCredentials.username, 42)) != null)
    }
}
