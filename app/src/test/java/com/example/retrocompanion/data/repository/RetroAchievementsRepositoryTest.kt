package com.example.retrocompanion.data.repository

import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.UnlockStamp
import com.example.retrocompanion.data.model.UserCompletionProgressPage
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.testutil.FakeRetroAchievementsApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}
