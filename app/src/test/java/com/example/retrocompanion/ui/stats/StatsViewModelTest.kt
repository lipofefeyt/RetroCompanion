package com.example.retrocompanion.ui.stats

import android.app.Application
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.UserCompletionProgressPage
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.model.UserProfileSummary
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
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleCredentials = UserCredentials(username = "sample_user", apiKey = "sample-fake-key")

    private fun emptyCache() = ResponseCache(FakeCacheDao())

    @Test
    fun `missing credentials sets status to Missing`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = StatsViewModel(
            Application(),
            FakeUserCredentialsSource(UserCredentials()),
            RetroAchievementsRepository(FakeRetroAchievementsApi()),
            emptyCache()
        )

        advanceUntilIdle()

        assertEquals(CredentialsStatus.Missing, viewModel.uiState.value.credentialsStatus)
    }

    @Test
    fun `valid credentials load profile and library stats`() = runTest(mainDispatcherRule.dispatcher) {
        val profile = UserProfileSummary(user = "sample_user", totalPoints = 1000)
        val games = listOf(GameProgressSummary(gameId = 1, title = "Sample", maxPossible = 10, numAwarded = 5))
        val api = FakeRetroAchievementsApi(
            userSummaryResult = { profile },
            completionProgressResult = { count, _ -> UserCompletionProgressPage(count = count, total = games.size, results = games) }
        )
        val viewModel = StatsViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            emptyCache()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CredentialsStatus.Present, state.credentialsStatus)
        assertEquals(SectionState.Success(profile), state.profile)
        val libraryState = state.libraryStats
        assertTrue(libraryState is SectionState.Success)
        assertEquals(50, (libraryState as SectionState.Success).data.overallCompletionPercent)
    }

    @Test
    fun `network failure keeps showing cached library stats instead of erroring`() = runTest(mainDispatcherRule.dispatcher) {
        val cachedGames = listOf(GameProgressSummary(gameId = 1, title = "Cached", maxPossible = 10, numAwarded = 10))
        val cache = emptyCache()
        cache.write(CacheKeys.gameLibrary(sampleCredentials.username), cachedGames)
        val api = FakeRetroAchievementsApi(completionProgressResult = { _, _ -> throw httpException(500) })
        val viewModel = StatsViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            cache
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value.libraryStats
        assertTrue(state is SectionState.Success)
        assertTrue((state as SectionState.Success).isStale)
        assertEquals(100, state.data.overallCompletionPercent)
    }
}
