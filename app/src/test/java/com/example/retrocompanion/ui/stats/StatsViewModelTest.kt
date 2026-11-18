package com.example.retrocompanion.ui.stats

import android.app.Application
import com.example.retrocompanion.data.local.CacheKeys
import com.example.retrocompanion.data.local.ResponseCache
import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.UnlockStamp
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

    // --- Unlock activity (streaks + heatmap) ---

    private val activityNow = UnlockActivity.parseUnlockMillis("2026-03-11 12:00:00")!!
    private val activityChunks = UnlockChunks.forWindow(activityNow, UnlockActivity.DEFAULT_WINDOW_DAYS)
    private val utc = java.util.TimeZone.getTimeZone("UTC")

    private fun activityViewModel(api: FakeRetroAchievementsApi, cache: ResponseCache) = StatsViewModel(
        Application(),
        FakeUserCredentialsSource(sampleCredentials),
        RetroAchievementsRepository(api),
        cache,
        nowMillis = { activityNow },
        timeZone = utc
    )

    @Test
    fun `activity fetches every chunk on first load and builds streaks from the unlocks`() =
        runTest(mainDispatcherRule.dispatcher) {
            val ranges = mutableListOf<Pair<Long, Long>>()
            val api = FakeRetroAchievementsApi(earnedBetweenResult = { from, to ->
                ranges += from to to
                if (to >= activityNow / 1000) {
                    listOf(
                        RecentUnlock(date = "2026-03-10 08:00:00", achievementId = 1),
                        RecentUnlock(date = "2026-03-11 08:00:00", achievementId = 2)
                    )
                } else {
                    emptyList()
                }
            })
            val viewModel = activityViewModel(api, emptyCache())

            advanceUntilIdle()

            assertEquals(activityChunks.map { it.fromEpochSeconds to it.toEpochSeconds }, ranges)
            val state = viewModel.uiState.value.activity as SectionState.Success
            assertEquals(2, state.data.currentStreak)
            assertEquals(2, state.data.totalUnlocks)
            assertEquals(false, state.isStale)
        }

    @Test
    fun `activity does not refetch chunks that are cached and settled`() =
        runTest(mainDispatcherRule.dispatcher) {
            val cache = emptyCache()
            activityChunks.forEach { chunk ->
                cache.write(
                    CacheKeys.unlockChunk(sampleCredentials.username, UnlockChunks.CHUNK_DAYS, chunk.index),
                    listOf(UnlockStamp(achievementId = chunk.index.toInt(), date = "2026-03-10 08:00:00"))
                )
            }
            var fetches = 0
            val api = FakeRetroAchievementsApi(earnedBetweenResult = { _, _ -> fetches++; emptyList() })
            activityViewModel(api, cache)

            advanceUntilIdle()

            val unsettled = activityChunks.count { !it.isSettled(activityNow) }
            assertTrue(unsettled in 1 until activityChunks.size)
            assertEquals(unsettled, fetches)
        }

    @Test
    fun `activity falls back to cached chunks as stale data when offline`() =
        runTest(mainDispatcherRule.dispatcher) {
            val cache = emptyCache()
            activityChunks.forEach { chunk ->
                cache.write(
                    CacheKeys.unlockChunk(sampleCredentials.username, UnlockChunks.CHUNK_DAYS, chunk.index),
                    listOf(UnlockStamp(achievementId = chunk.index.toInt(), date = "2026-03-10 08:00:00")),
                    nowMillis = 1_000L
                )
            }
            val api = FakeRetroAchievementsApi(earnedBetweenResult = { _, _ -> throw java.io.IOException("offline") })
            val viewModel = activityViewModel(api, cache)

            advanceUntilIdle()

            val state = viewModel.uiState.value.activity as SectionState.Success
            assertTrue(state.isStale)
            assertEquals(1_000L, state.cachedAtMillis)
            assertTrue(state.data.totalUnlocks > 0)
        }

    @Test
    fun `activity is an error rather than a partial heatmap when a chunk is unavailable`() =
        runTest(mainDispatcherRule.dispatcher) {
            val api = FakeRetroAchievementsApi(earnedBetweenResult = { _, _ -> throw httpException(500) })
            val viewModel = activityViewModel(api, emptyCache())

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.activity is SectionState.Error)
        }

    @Test
    fun `activity retries a rate-limited chunk and then succeeds`() = runTest(mainDispatcherRule.dispatcher) {
        var calls = 0
        val api = FakeRetroAchievementsApi(earnedBetweenResult = { _, _ ->
            calls++
            if (calls == 1) throw httpException(429)
            listOf(RecentUnlock(date = "2026-03-11 08:00:00", achievementId = 1))
        })
        val viewModel = activityViewModel(api, emptyCache())

        advanceUntilIdle()

        val state = viewModel.uiState.value.activity as SectionState.Success
        assertEquals(1, state.data.totalUnlocks)
        // One extra request for the retry on top of one per chunk.
        assertEquals(activityChunks.size + 1, calls)
    }

    @Test
    fun `activity gives up after repeated rate limiting but keeps the chunks it already fetched`() =
        runTest(mainDispatcherRule.dispatcher) {
            val cache = emptyCache()
            val api = FakeRetroAchievementsApi(earnedBetweenResult = { from, _ ->
                if (from >= activityChunks.last().fromEpochSeconds) throw httpException(429)
                emptyList()
            })
            val viewModel = activityViewModel(api, cache)

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.activity is SectionState.Error)
            // The earlier chunks were cached, so the next refresh only needs the missing one.
            val firstKey = CacheKeys.unlockChunk(sampleCredentials.username, UnlockChunks.CHUNK_DAYS, activityChunks.first().index)
            assertTrue(cache.read<List<UnlockStamp>>(firstKey, object : com.google.gson.reflect.TypeToken<List<UnlockStamp>>() {}.type) != null)
        }

    @Test
    fun `activity waits for profile and library to load before requesting chunks`() =
        runTest(mainDispatcherRule.dispatcher) {
            val order = mutableListOf<String>()
            val api = FakeRetroAchievementsApi(
                userSummaryResult = { order += "summary"; UserProfileSummary() },
                completionProgressResult = { _, _ -> order += "library"; UserCompletionProgressPage() },
                earnedBetweenResult = { _, _ -> order += "chunk"; emptyList() }
            )
            activityViewModel(api, emptyCache())

            advanceUntilIdle()

            val firstChunk = order.indexOf("chunk")
            assertTrue(firstChunk > order.indexOf("summary"))
            assertTrue(firstChunk > order.indexOf("library"))
        }
}
