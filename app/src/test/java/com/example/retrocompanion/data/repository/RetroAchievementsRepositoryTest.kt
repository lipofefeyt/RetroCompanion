package com.example.retrocompanion.data.repository

import com.example.retrocompanion.data.model.RecentUnlock
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
}
