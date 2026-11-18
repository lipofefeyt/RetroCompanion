package com.example.retrocompanion.testutil

import com.example.retrocompanion.data.model.AchievementOfTheWeekResponse
import com.example.retrocompanion.data.model.GameInfo
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.UserCompletionProgressPage
import com.example.retrocompanion.data.model.UserProfileSummary
import com.example.retrocompanion.data.model.WantToPlayPage
import com.example.retrocompanion.data.remote.RetroAchievementsApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/** A fake [HttpException] (no real HTTP call), for simulating a specific API status in tests. */
fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaTypeOrNull())))

/**
 * Fake [RetroAchievementsApi] backed by configurable result-producing lambdas, so a test can
 * either return a canned success value or throw (e.g. [httpException] or [java.io.IOException])
 * to drive through [com.example.retrocompanion.data.repository.RetroAchievementsRepository]'s
 * real exception-mapping logic.
 */
class FakeRetroAchievementsApi(
    var userSummaryResult: suspend () -> UserProfileSummary = { UserProfileSummary() },
    var recentlyPlayedGamesResult: suspend () -> List<RecentlyPlayedGame> = { emptyList() },
    var recentAchievementsResult: suspend () -> List<RecentUnlock> = { emptyList() },
    var gameInfoResult: suspend () -> GameInfo = { GameInfo() },
    var completionProgressResult: suspend (count: Int, offset: Int) -> UserCompletionProgressPage =
        { _, _ -> UserCompletionProgressPage() },
    var earnedBetweenResult: suspend (from: Long, to: Long) -> List<RecentUnlock> = { _, _ -> emptyList() },
    var wantToPlayResult: suspend (count: Int, offset: Int) -> WantToPlayPage = { _, _ -> WantToPlayPage() },
    var achievementOfTheWeekResult: suspend () -> AchievementOfTheWeekResponse = { AchievementOfTheWeekResponse() }
) : RetroAchievementsApi {

    /** The lookback (m) of the most recent getRecentAchievements call, for asserting on it. */
    var lastRecentMinutes: Int? = null
        private set
    var recentCallCount: Int = 0
        private set

    override suspend fun getUserSummary(
        authUser: String,
        apiKey: String,
        targetUser: String,
        gamesCount: Int,
        achievementsCount: Int
    ): UserProfileSummary = userSummaryResult()

    override suspend fun getRecentlyPlayedGames(
        authUser: String,
        apiKey: String,
        targetUser: String,
        count: Int
    ): List<RecentlyPlayedGame> = recentlyPlayedGamesResult()

    override suspend fun getRecentAchievements(
        authUser: String,
        apiKey: String,
        targetUser: String,
        minutes: Int
    ): List<RecentUnlock> {
        lastRecentMinutes = minutes
        recentCallCount++
        return recentAchievementsResult()
    }

    override suspend fun getGameInfoAndUserProgress(
        authUser: String,
        apiKey: String,
        targetUser: String,
        gameId: Int
    ): GameInfo = gameInfoResult()

    override suspend fun getAchievementsEarnedBetween(
        authUser: String,
        apiKey: String,
        targetUser: String,
        from: Long,
        to: Long
    ): List<RecentUnlock> = earnedBetweenResult(from, to)

    override suspend fun getAchievementOfTheWeek(
        authUser: String,
        apiKey: String
    ): AchievementOfTheWeekResponse = achievementOfTheWeekResult()

    override suspend fun getUserWantToPlayList(
        authUser: String,
        apiKey: String,
        targetUser: String,
        count: Int,
        offset: Int
    ): WantToPlayPage = wantToPlayResult(count, offset)

    override suspend fun getUserCompletionProgress(
        authUser: String,
        apiKey: String,
        targetUser: String,
        count: Int,
        offset: Int
    ): UserCompletionProgressPage = completionProgressResult(count, offset)
}
