package com.example.retrocompanion.ui.profile

import android.app.Application
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.ResponseCache
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleCredentials = UserCredentials(username = "sample_user", apiKey = "sample-fake-key")

    private fun emptyCache() = ResponseCache(FakeCacheDao())

    @Test
    fun `missing credentials sets status to Missing`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ProfileViewModel(
            Application(),
            FakeUserCredentialsSource(UserCredentials()),
            RetroAchievementsRepository(FakeRetroAchievementsApi()),
            emptyCache()
        )

        advanceUntilIdle()

        assertEquals(CredentialsStatus.Missing, viewModel.uiState.value.credentialsStatus)
    }

    @Test
    fun `valid credentials load the profile summary`() = runTest(mainDispatcherRule.dispatcher) {
        val summary = UserProfileSummary(user = "sample_user", totalPoints = 1000, totalTruePoints = 2500, rank = 42)
        val api = FakeRetroAchievementsApi(userSummaryResult = { summary })
        val viewModel = ProfileViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            emptyCache()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CredentialsStatus.Present, state.credentialsStatus)
        assertEquals(SectionState.Success(summary), state.profile)
    }

    @Test
    fun `network failure keeps showing cached profile instead of erroring`() = runTest(mainDispatcherRule.dispatcher) {
        val cachedSummary = UserProfileSummary(user = "sample_user", totalPoints = 500)
        val cache = emptyCache()
        cache.write(CacheKeys.userSummary(sampleCredentials.username), cachedSummary)
        val api = FakeRetroAchievementsApi(userSummaryResult = { throw httpException(500) })
        val viewModel = ProfileViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            cache
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value.profile
        assertTrue(state is SectionState.Success)
        assertTrue((state as SectionState.Success).isStale)
        assertEquals(cachedSummary, state.data)
    }

    @Test
    fun `successful fetch writes the profile to the cache`() = runTest(mainDispatcherRule.dispatcher) {
        val summary = UserProfileSummary(user = "sample_user", totalPoints = 10)
        val cacheDao = FakeCacheDao()
        val api = FakeRetroAchievementsApi(userSummaryResult = { summary })
        val viewModel = ProfileViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            ResponseCache(cacheDao)
        )

        advanceUntilIdle()

        assertNotNull(cacheDao.get(CacheKeys.userSummary(sampleCredentials.username)))
    }

    @Test
    fun `a rejected key from the API produces an invalid-key error`() = runTest(mainDispatcherRule.dispatcher) {
        val api = FakeRetroAchievementsApi(userSummaryResult = { throw httpException(401) })
        val viewModel = ProfileViewModel(
            Application(),
            FakeUserCredentialsSource(sampleCredentials),
            RetroAchievementsRepository(api),
            emptyCache()
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value.profile
        assertTrue(state is SectionState.Error)
        assertTrue((state as SectionState.Error).isInvalidApiKey)
    }
}
