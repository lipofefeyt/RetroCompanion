package com.example.retrocompanion.data.repository

import com.example.retrocompanion.data.model.GameInfo
import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.remote.RetroAchievementsApi
import com.example.retrocompanion.data.remote.RetroApiException
import com.example.retrocompanion.data.remote.RetrofitClient
import retrofit2.HttpException
import java.io.IOException

class RetroAchievementsRepository(
    private val api: RetroAchievementsApi = RetrofitClient.api
) {

    /**
     * Fetches recently played games for the user.
     *
     * @param count Number of records to request (c). Capped at the API's documented maximum of 50.
     */
    suspend fun getRecentlyPlayed(
        credentials: UserCredentials,
        count: Int = 10
    ): Result<List<RecentlyPlayedGame>> = safeApiCall {
        require(credentials.isValid) { "User credentials are invalid or missing." }
        api.getRecentlyPlayedGames(
            authUser = credentials.username,
            apiKey = credentials.apiKey,
            targetUser = credentials.username,
            count = count.coerceIn(1, MAX_RECENTLY_PLAYED_COUNT)
        )
    }

    /**
     * Fetches recent achievement unlocks for the user.
     * Lookback minutes (m) is explicitly set to 10080 minutes (7 days) because
     * RetroAchievements API defaults to 60 minutes if omitted or set to 0.
     */
    suspend fun getRecentUnlocks(
        credentials: UserCredentials,
        lookbackMinutes: Int = SEVEN_DAYS_IN_MINUTES
    ): Result<List<RecentUnlock>> = safeApiCall {
        require(credentials.isValid) { "User credentials are invalid or missing." }
        val unlocks = api.getRecentAchievements(
            authUser = credentials.username,
            apiKey = credentials.apiKey,
            targetUser = credentials.username,
            minutes = lookbackMinutes
        )
        // The API's ordering isn't documented/guaranteed; sort newest first ourselves.
        // "Date" is "yyyy-MM-dd HH:mm:ss", so a plain string sort is also a correct
        // chronological sort. Unlocks with no date (shouldn't happen) sort last.
        unlocks.sortedByDescending { it.date.orEmpty() }
    }

    /** Fetches a game's details and the current user's progress on it. */
    suspend fun getGameInfo(
        credentials: UserCredentials,
        gameId: Int
    ): Result<GameInfo> = safeApiCall {
        require(credentials.isValid) { "User credentials are invalid or missing." }
        api.getGameInfoAndUserProgress(
            authUser = credentials.username,
            apiKey = credentials.apiKey,
            targetUser = credentials.username,
            gameId = gameId
        )
    }

    /**
     * Runs [block], translating network/HTTP failures into the distinct [RetroApiException]
     * subtypes so callers can tell a rejected key, rate limiting, no network, and an unexpected
     * response apart.
     */
    private suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: HttpException) {
        Result.failure(RetroApiException.fromHttpStatus(e.code(), e))
    } catch (e: IOException) {
        Result.failure(RetroApiException.NoNetwork(e))
    } catch (e: IllegalArgumentException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(RetroApiException.UnexpectedError(e.localizedMessage ?: "Unexpected error.", e))
    }

    companion object {
        private const val SEVEN_DAYS_IN_MINUTES = 10080
        private const val MAX_RECENTLY_PLAYED_COUNT = 50
    }
}
