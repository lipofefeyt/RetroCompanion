package com.example.retrocompanion.data.repository

import com.example.retrocompanion.data.model.GameInfo
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.remote.RetroAchievementsApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeRetroAchievementsApi(
    private val recentAchievements: List<RecentUnlock> = emptyList()
) : RetroAchievementsApi {
    override suspend fun getRecentlyPlayedGames(
        authUser: String,
        apiKey: String,
        targetUser: String,
        count: Int
    ): List<RecentlyPlayedGame> = emptyList()

    override suspend fun getRecentAchievements(
        authUser: String,
        apiKey: String,
        targetUser: String,
        minutes: Int
    ): List<RecentUnlock> = recentAchievements

    override suspend fun getGameInfoAndUserProgress(
        authUser: String,
        apiKey: String,
        targetUser: String,
        gameId: Int
    ): GameInfo = GameInfo()
}

class RetroAchievementsRepositoryTest {

    private val sampleCredentials = UserCredentials(username = "sample_user", apiKey = "sample-fake-key")

    @Test
    fun `getRecentUnlocks sorts results newest first regardless of API order`() = runTest {
        val outOfOrder = listOf(
            RecentUnlock(date = "2023-12-25 09:00:00", achievementId = 1, title = "Oldest"),
            RecentUnlock(date = "2023-12-27 16:04:50", achievementId = 2, title = "Newest"),
            RecentUnlock(date = "2023-12-26 12:30:00", achievementId = 3, title = "Middle")
        )
        val repository = RetroAchievementsRepository(FakeRetroAchievementsApi(outOfOrder))

        val result = repository.getRecentUnlocks(sampleCredentials).getOrThrow()

        assertEquals(listOf("Newest", "Middle", "Oldest"), result.map { it.title })
    }
}
