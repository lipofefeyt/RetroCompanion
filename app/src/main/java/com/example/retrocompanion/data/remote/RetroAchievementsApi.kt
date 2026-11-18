package com.example.retrocompanion.data.remote

import com.example.retrocompanion.data.model.AchievementOfTheWeekResponse
import com.example.retrocompanion.data.model.GameInfo
import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.UserCompletionProgressPage
import com.example.retrocompanion.data.model.UserProfileSummary
import com.example.retrocompanion.data.model.WantToPlayPage
import retrofit2.http.GET
import retrofit2.http.Query

interface RetroAchievementsApi {

    /**
     * Fetches a user's profile summary (points, rank, avatar, motto).
     * Docs: https://api-docs.retroachievements.org/v1/get-user-summary.html
     *
     * @param authUser The authenticating username (z)
     * @param apiKey The web API key (y)
     * @param targetUser The target username to query (u)
     * @param gamesCount Number of recent games to return in summary (g). Default 0 for profile stats only.
     * @param achievementsCount Number of recent achievements to return in summary (a). Default 0.
     */
    @GET("API_GetUserSummary.php")
    suspend fun getUserSummary(
        @Query("z") authUser: String,
        @Query("y") apiKey: String,
        @Query("u") targetUser: String,
        @Query("g") gamesCount: Int = 0,
        @Query("a") achievementsCount: Int = 0
    ): UserProfileSummary

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

    /**
     * Fetches the current Achievement of the Week. Only the web API key (y) is documented for this
     * endpoint; the authenticating username (z) is sent too, like every other call in this app.
     * The response includes the full list of players who unlocked it, so it can be large.
     * Docs: https://api-docs.retroachievements.org/v1/get-achievement-of-the-week.html
     */
    @GET("API_GetAchievementOfTheWeek.php")
    suspend fun getAchievementOfTheWeek(
        @Query("z") authUser: String,
        @Query("y") apiKey: String
    ): AchievementOfTheWeekResponse

    /**
     * Fetches the user's achievement unlocks between two moments.
     * Docs: https://api-docs.retroachievements.org/v1/get-achievements-earned-between.html
     *
     * @param from Start of the range as an epoch timestamp in seconds (f).
     * @param to End of the range as an epoch timestamp in seconds (t).
     */
    @GET("API_GetAchievementsEarnedBetween.php")
    suspend fun getAchievementsEarnedBetween(
        @Query("z") authUser: String,
        @Query("y") apiKey: String,
        @Query("u") targetUser: String,
        @Query("f") from: Long,
        @Query("t") to: Long
    ): List<RecentUnlock>

    /**
     * Fetches a page of the user's Want to Play list from the site. Read-only: the Web API has
     * no endpoint to add or remove entries. Only returned for the account itself or users that
     * follow each other.
     * Docs: https://api-docs.retroachievements.org/v1/get-user-want-to-play-list.html
     *
     * @param count Number of records to return (c). Default 100, maximum 500.
     * @param offset Number of records to skip (o). Default 0.
     */
    @GET("API_GetUserWantToPlayList.php")
    suspend fun getUserWantToPlayList(
        @Query("z") authUser: String,
        @Query("y") apiKey: String,
        @Query("u") targetUser: String,
        @Query("c") count: Int = 100,
        @Query("o") offset: Int = 0
    ): WantToPlayPage

    /**
     * Fetches a game's details and the user's progress on it.
     * Docs: https://api-docs.retroachievements.org/v1/get-game-info-and-user-progress.html
     *
     * @param gameId The target game ID (g).
     */
    @GET("API_GetGameInfoAndUserProgress.php")
    suspend fun getGameInfoAndUserProgress(
        @Query("z") authUser: String,
        @Query("y") apiKey: String,
        @Query("u") targetUser: String,
        @Query("g") gameId: Int
    ): GameInfo

    /**
     * Fetches a page of the user's full game library with completion progress. Supersedes the
     * legacy API_GetUserCompletedGames.php per the current docs.
     * Docs: https://api-docs.retroachievements.org/v1/get-user-completion-progress.html
     *
     * @param count Number of records to return (c). Default 100, maximum 500.
     * @param offset Number of records to skip (o). Default 0.
     */
    @GET("API_GetUserCompletionProgress.php")
    suspend fun getUserCompletionProgress(
        @Query("z") authUser: String,
        @Query("y") apiKey: String,
        @Query("u") targetUser: String,
        @Query("c") count: Int = 100,
        @Query("o") offset: Int = 0
    ): UserCompletionProgressPage
}
