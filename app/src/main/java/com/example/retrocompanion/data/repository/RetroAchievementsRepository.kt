package com.example.retrocompanion.data.repository

import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.remote.RetroAchievementsApi
import com.example.retrocompanion.data.remote.RetrofitClient

class RetroAchievementsRepository(
    private val api: RetroAchievementsApi = RetrofitClient.api
) {

    /**
     * Fetches recently played games for the user.
     */
    suspend fun getRecentlyPlayed(
        credentials: UserCredentials,
        count: Int = 10
    ): Result<List<RecentlyPlayedGame>> = runCatching {
        require(credentials.isValid) { "User credentials are invalid or missing." }
        api.getRecentlyPlayed(
            authUser = credentials.username,
            apiKey = credentials.apiKey,
            targetUser = credentials.username,
            count = count
        )
    }

    /**
     * Fetches recent achievement unlocks for the user.
     * Lookback minutes (m) is explicitly set to 10080 minutes (7 days) because
     * RetroAchievements API defaults to 60 minutes if omitted or set to 0.
     */
    suspend fun getRecentUnlocks(
        credentials: UserCredentials,
        lookbackMinutes: Int = 10080, // 7 days = 10,080 minutes
        count: Int = 50
    ): Result<List<RecentUnlock>> = runCatching {
        require(credentials.isValid) { "User credentials are invalid or missing." }
        api.getRecentAchievements(
            authUser = credentials.username,
            apiKey = credentials.apiKey,
            targetUser = credentials.username,
            minutes = lookbackMinutes,
            count = count
        )
    }
}
