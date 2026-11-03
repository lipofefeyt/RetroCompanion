package com.example.retrocompanion.data.remote

import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.RecentUnlock
import retrofit2.http.GET
import retrofit2.http.Query

interface RetroAchievementsApi {

    /**
     * Fetches recently played games for a user.
     * Docs: https://api-docs.retroachievements.org/v1/get-user-recently-played-games.html
     *
     * @param authUser The authenticating username (z)
     * @param apiKey The web API key (y)
     * @param targetUser The target username to query (u)
     * @param count Number of records to return (c). Default 10, maximum 50 per the API docs.
     */
    @GET("API_GetUserRecentlyPlayedGames.php")
    suspend fun getRecentlyPlayedGames(
        @Query("z") authUser: String,
        @Query("y") apiKey: String,
        @Query("u") targetUser: String,
        @Query("c") count: Int = 10
    ): List<RecentlyPlayedGame>

    /**
     * Fetches recent achievement unlocks for a user.
     * Docs: https://api-docs.retroachievements.org/v1/get-user-recent-achievements.html
     *
     * @param authUser The authenticating username (z)
     * @param apiKey The web API key (y)
     * @param targetUser The target username to query (u)
     * @param minutes Lookback window in minutes (m). API default is 60 minutes if omitted;
     *                this client always sends it explicitly (e.g. 10080 for 7 days).
     *                There is no documented count/offset parameter for this endpoint.
     */
    @GET("API_GetUserRecentAchievements.php")
    suspend fun getRecentAchievements(
        @Query("z") authUser: String,
        @Query("y") apiKey: String,
        @Query("u") targetUser: String,
        @Query("m") minutes: Int = 60
    ): List<RecentUnlock>
}
