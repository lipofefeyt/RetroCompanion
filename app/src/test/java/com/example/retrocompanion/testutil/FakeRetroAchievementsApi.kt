package com.example.retrocompanion.testutil

import com.example.retrocompanion.data.model.GameInfo
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.RecentlyPlayedGame
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
    var recentlyPlayedGamesResult: suspend () -> List<RecentlyPlayedGame> = { emptyList() },
    var recentAchievementsResult: suspend () -> List<RecentUnlock> = { emptyList() },
    var gameInfoResult: suspend () -> GameInfo = { GameInfo() }
) : RetroAchievementsApi {

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
    ): List<RecentUnlock> = recentAchievementsResult()

    override suspend fun getGameInfoAndUserProgress(
        authUser: String,
        apiKey: String,
        targetUser: String,
        gameId: Int
    ): GameInfo = gameInfoResult()
}
