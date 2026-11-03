package com.example.retrocompanion.data.remote

import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.RecentUnlock
import retrofit2.http.GET
import retrofit2.http.Query

interface RetroAchievementsApi {

    /**
     * Fetches recently played games for a user.
     *
     * @param authUser The authenticating username (z)
     * @param apiKey The web API key (y)
     * @param targetUser The target username to query (u)
     * @param count Maximum number of games to return
     */
    @GET("API_GetUserRecentlyPlayed.php")
    suspend fun getRecentlyPlayed(
        @Query("z") authUser: String,
        @Query("y") apiKey: String,
        @Query("u") targetUser: String,
        @Query("count") count: Int = 10
    ): List<RecentlyPlayedGame>

    /**
     * Fetches recent achievement unlocks for a user.
     *
     * @param authUser The authenticating username (z)
     * @param apiKey The web API key (y)
     * @param targetUser The target username to query (u)
     * @param minutes Lookback window in minutes (m). Default in RetroAchievements API is 60 minutes.
     *                For 7 days lookback, set m = 10080 (7 * 24 * 60).
     * @param count Maximum number of achievements to return
     */
    @GET("API_GetUserRecentAchievements.php")
    suspend fun getRecentAchievements(
        @Query("z") authUser: String,
        @Query("y") apiKey: String,
        @Query("u") targetUser: String,
        @Query("m") minutes: Int = 10080,
        @Query("count") count: Int = 50
    ): List<RecentUnlock>
}
