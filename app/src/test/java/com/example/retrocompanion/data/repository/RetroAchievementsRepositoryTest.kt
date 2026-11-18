package com.example.retrocompanion.data.repository

import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.UnlockStamp
import com.example.retrocompanion.data.model.UserCompletionProgressPage
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.model.UserProfileSummary
import com.example.retrocompanion.data.model.WantToPlayGame
import com.example.retrocompanion.data.model.WantToPlayPage
import com.example.retrocompanion.testutil.FakeRetroAchievementsApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetroAchievementsRepositoryTest {

    private val sampleCredentials = UserCredentials(username = "sample_user", apiKey = "sample-fake-key")

    @Test
    fun `getRecentUnlocks sorts results newest first regardless of API order`() = runTest {
        val outOfOrder = listOf(
            RecentUnlock(date = "2023-12-25 09:00:00", achievementId = 1, title = "Oldest"),
            RecentUnlock(date = "2023-12-27 16:04:50", achievementId = 2, title = "Newest"),
            RecentUnlock(date = "2023-12-26 12:30:00", achievementId = 3, title = "Middle")
        )
        val api = FakeRetroAchievementsApi(recentAchievementsResult = { outOfOrder })
        val repository = RetroAchievementsRepository(api)

        val result = repository.getRecentUnlocks(sampleCredentials).getOrThrow()

        assertEquals(listOf("Newest", "Middle", "Oldest"), result.map { it.title })
    }

    @Test
    fun `getUnlocksBetween passes the range through and keeps only id and date`() = runTest {
        var requested: Pair<Long, Long>? = null
        val api = FakeRetroAchievementsApi(earnedBetweenResult = { from, to ->
            requested = from to to
            listOf(
                RecentUnlock(date = "2026-03-10 08:00:00", achievementId = 1, title = "Sample One"),
                RecentUnlock(date = null, achievementId = 2, title = "No Date"),
                RecentUnlock(date = " ", achievementId = 3, title = "Blank Date")
            )
        })

        val result = RetroAchievementsRepository(api)
            .getUnlocksBetween(sampleCredentials, fromEpochSeconds = 100L, toEpochSeconds = 200L)
            .getOrThrow()

        assertEquals(100L to 200L, requested)
        assertEquals(listOf(UnlockStamp(1, "2026-03-10 08:00:00")), result)
    }

    @Test
    fun `getAllGamesProgress pages through until Total is reached`() = runTest {
        val allGames = (1..600).map { GameProgressSummary(gameId = it, title = "Game $it") }
        val offsetsRequested = mutableListOf<Int>()
        val api = FakeRetroAchievementsApi(
            completionProgressResult = { count, offset ->
                offsetsRequested += offset
                UserCompletionProgressPage(
                    count = count,
                    total = allGames.size,
                    results = allGames.drop(offset).take(count)
                )
            }
        )

        val result = RetroAchievementsRepository(api).getAllGamesProgress(sampleCredentials).getOrThrow()

        assertEquals(600, result.size)
        assertEquals(listOf(0, 500), offsetsRequested)
    }

    @Test
    fun `getAllGamesProgress stops at the safety cap instead of paging forever`() = runTest {
        var callCount = 0
        val api = FakeRetroAchievementsApi(
            completionProgressResult = { count, offset ->
                callCount++
                // Always claims a huge total, so without a safety cap this would page forever.
                UserCompletionProgressPage(
                    count = count,
                    total = 1_000_000,
                    results = List(count) { GameProgressSummary(gameId = offset + it, title = "Game ${offset + it}") }
                )
            }
        )

        val result = RetroAchievementsRepository(api).getAllGamesProgress(sampleCredentials).getOrThrow()

        assertEquals(3, callCount)
        assertEquals(1500, result.size)
    }

    private fun wantToPlay(vararg ids: Int) = ids.map { WantToPlayGame(gameId = it, title = "Sample $it") }

    @Test
    fun `getWantToPlayList pages through until Total is reached`() = runTest {
        val offsets = mutableListOf<Int>()
        val api = FakeRetroAchievementsApi(wantToPlayResult = { _, offset ->
            offsets += offset
            when (offset) {
                0 -> WantToPlayPage(count = 2, total = 3, results = wantToPlay(1, 2))
                else -> WantToPlayPage(count = 1, total = 3, results = wantToPlay(3))
            }
        })

        val result = RetroAchievementsRepository(api).getWantToPlayList(sampleCredentials).getOrThrow()

        assertEquals(listOf(1, 2, 3), result.map { it.gameId })
        assertEquals(listOf(0, 2), offsets)
    }

    @Test
    fun `getWantToPlayList stops on an empty page and at the safety cap`() = runTest {
        val emptyApi = FakeRetroAchievementsApi(wantToPlayResult = { _, _ -> WantToPlayPage(total = 10) })
        assertEquals(emptyList<WantToPlayGame>(), RetroAchievementsRepository(emptyApi).getWantToPlayList(sampleCredentials).getOrThrow())

        var calls = 0
        val endlessApi = FakeRetroAchievementsApi(wantToPlayResult = { _, _ ->
            calls++
            WantToPlayPage(count = 1, total = 1_000_000, results = wantToPlay(calls))
        })
        RetroAchievementsRepository(endlessApi).getWantToPlayList(sampleCredentials).getOrThrow()
        assertEquals(3, calls)
    }

    @Test
    fun `identical requests made at the same time share one API call`() = runTest {
        var calls = 0
        val gate = CompletableDeferred<Unit>()
        val api = FakeRetroAchievementsApi(userSummaryResult = {
            calls++
            gate.await()
            UserProfileSummary()
        })
        // Different repository instances, as each screen's view model has its own.
        val first = async { RetroAchievementsRepository(api).getUserSummary(sampleCredentials) }
        val second = async { RetroAchievementsRepository(api).getUserSummary(sampleCredentials) }
        runCurrent()

        gate.complete(Unit)

        assertTrue(first.await().isSuccess)
        assertTrue(second.await().isSuccess)
        assertEquals(1, calls)
    }

    @Test
    fun `a request made after the previous one finished goes to the network again`() = runTest {
        var calls = 0
        val api = FakeRetroAchievementsApi(userSummaryResult = { calls++; UserProfileSummary() })
        val repository = RetroAchievementsRepository(api)

        repository.getUserSummary(sampleCredentials)
        repository.getUserSummary(sampleCredentials)

        assertEquals(2, calls)
    }

    @Test
    fun `different credentials or arguments are never shared`() = runTest {
        var summaryCalls = 0
        var recentCalls = 0
        val gate = CompletableDeferred<Unit>()
        val api = FakeRetroAchievementsApi(
            userSummaryResult = { summaryCalls++; gate.await(); UserProfileSummary() },
            recentAchievementsResult = { recentCalls++; gate.await(); emptyList() }
        )
        val other = UserCredentials(username = "other_user", apiKey = "other-fake-key")
        val repository = RetroAchievementsRepository(api)

        val a = async { repository.getUserSummary(sampleCredentials) }
        val b = async { repository.getUserSummary(other) }
        val c = async { repository.getRecentUnlocks(sampleCredentials, lookbackMinutes = 60) }
        val d = async { repository.getRecentUnlocks(sampleCredentials, lookbackMinutes = 120) }
        runCurrent()
        gate.complete(Unit)
        listOf(a, b).forEach { it.await() }
        listOf(c, d).forEach { it.await() }

        assertEquals(2, summaryCalls)
        assertEquals(2, recentCalls)
    }

    @Test
    fun `a failed shared request reports the failure to every waiting caller`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val api = FakeRetroAchievementsApi(userSummaryResult = {
            gate.await()
            throw com.example.retrocompanion.testutil.httpException(429)
        })
        val first = async { RetroAchievementsRepository(api).getUserSummary(sampleCredentials) }
        val second = async { RetroAchievementsRepository(api).getUserSummary(sampleCredentials) }
        runCurrent()

        gate.complete(Unit)

        assertTrue(first.await().exceptionOrNull() is com.example.retrocompanion.data.remote.RetroApiException.RateLimited)
        assertTrue(second.await().exceptionOrNull() is com.example.retrocompanion.data.remote.RetroApiException.RateLimited)
    }
}
