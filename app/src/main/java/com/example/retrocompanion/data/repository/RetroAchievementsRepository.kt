package com.example.retrocompanion.data.repository

import com.example.retrocompanion.data.model.AchievementOfTheWeek
import com.example.retrocompanion.data.model.GameInfo
import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.UnlockStamp
import com.example.retrocompanion.data.model.UserCredentials
import com.example.retrocompanion.data.model.UserProfileSummary
import com.example.retrocompanion.data.model.WantToPlayGame
import com.example.retrocompanion.data.remote.RetroAchievementsApi
import com.example.retrocompanion.data.remote.RetroApiException
import com.example.retrocompanion.data.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import retrofit2.HttpException
import java.io.IOException

class RetroAchievementsRepository(
    private val api: RetroAchievementsApi = RetrofitClient.api
) {

    /** Fetches the user profile summary (points, rank, avatar, motto). */
    suspend fun getUserSummary(
        credentials: UserCredentials
    ): Result<UserProfileSummary> = coalesced("summary:${credentials.hashCode()}") {
        safeApiCall {
            require(credentials.isValid) { "User credentials are invalid or missing." }
            api.getUserSummary(
                authUser = credentials.username,
                apiKey = credentials.apiKey,
                targetUser = credentials.username
            )
        }
    }

    /**
     * Fetches recently played games for the user.
     *
     * @param count Number of records to request (c). Capped at the API's documented maximum of 50.
     */
    suspend fun getRecentlyPlayed(
        credentials: UserCredentials,
        count: Int = 10
    ): Result<List<RecentlyPlayedGame>> = coalesced("recentlyPlayed:${credentials.hashCode()}:$count") {
        safeApiCall {
            require(credentials.isValid) { "User credentials are invalid or missing." }
            api.getRecentlyPlayedGames(
                authUser = credentials.username,
                apiKey = credentials.apiKey,
                targetUser = credentials.username,
                count = count.coerceIn(1, MAX_RECENTLY_PLAYED_COUNT)
            )
        }
    }

    /**
     * Fetches recent achievement unlocks for the user.
     * Lookback minutes (m) is explicitly set to 10080 minutes (7 days) because
     * RetroAchievements API defaults to 60 minutes if omitted or set to 0.
     */
    suspend fun getRecentUnlocks(
        credentials: UserCredentials,
        lookbackMinutes: Int = SEVEN_DAYS_IN_MINUTES
    ): Result<List<RecentUnlock>> = coalesced("recentUnlocks:${credentials.hashCode()}:$lookbackMinutes") {
        safeApiCall {
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
    }

    /**
     * Fetches the id and date of every achievement the user unlocked between two epoch-second
     * timestamps. Entries without a date are dropped since they can't be placed on a day.
     */
    suspend fun getUnlocksBetween(
        credentials: UserCredentials,
        fromEpochSeconds: Long,
        toEpochSeconds: Long
    ): Result<List<UnlockStamp>> = safeApiCall {
        require(credentials.isValid) { "User credentials are invalid or missing." }
        api.getAchievementsEarnedBetween(
            authUser = credentials.username,
            apiKey = credentials.apiKey,
            targetUser = credentials.username,
            from = fromEpochSeconds,
            to = toEpochSeconds
        ).mapNotNull { unlock ->
            unlock.date?.takeIf { it.isNotBlank() }?.let { UnlockStamp(unlock.achievementId, it) }
        }
    }

    /**
     * Fetches the current Achievement of the Week as a compact summary (the response's long unlock
     * list is reduced to "did this user unlock it"). A response with no achievement in it is
     * reported as a failure rather than an empty card.
     */
    suspend fun getAchievementOfTheWeek(
        credentials: UserCredentials
    ): Result<AchievementOfTheWeek> = coalesced("aotw:${credentials.hashCode()}") {
        safeApiCall {
            require(credentials.isValid) { "User credentials are invalid or missing." }
            val response = api.getAchievementOfTheWeek(
                authUser = credentials.username,
                apiKey = credentials.apiKey
            )
            response.toSummary(credentials.username)
                ?: throw RetroApiException.UnexpectedError("The Achievement of the Week response had no achievement.")
        }
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
     * Fetches the user's full game library with completion progress, paging through
     * API_GetUserCompletionProgress.php until the account's full total is reached (or a safety
     * cap of [MAX_LIBRARY_PAGES] pages, comfortably covering virtually any real account).
     */
    suspend fun getAllGamesProgress(
        credentials: UserCredentials
    ): Result<List<GameProgressSummary>> = coalesced("library:${credentials.hashCode()}") {
        safeApiCall {
            require(credentials.isValid) { "User credentials are invalid or missing." }
            val accumulated = mutableListOf<GameProgressSummary>()
            var offset = 0
            repeat(MAX_LIBRARY_PAGES) {
                val page = api.getUserCompletionProgress(
                    authUser = credentials.username,
                    apiKey = credentials.apiKey,
                    targetUser = credentials.username,
                    count = LIBRARY_PAGE_SIZE,
                    offset = offset
                )
                accumulated += page.results
                if (page.results.isEmpty() || accumulated.size >= page.total) return@safeApiCall accumulated
                offset += page.results.size
            }
            accumulated
        }
    }

    /**
     * Fetches the user's whole Want to Play list from the site, paging through
     * API_GetUserWantToPlayList.php until the reported total is reached (or a safety cap of
     * [MAX_WANT_TO_PLAY_PAGES] pages).
     */
    suspend fun getWantToPlayList(
        credentials: UserCredentials
    ): Result<List<WantToPlayGame>> = safeApiCall {
        require(credentials.isValid) { "User credentials are invalid or missing." }
        val accumulated = mutableListOf<WantToPlayGame>()
        var offset = 0
        repeat(MAX_WANT_TO_PLAY_PAGES) {
            val page = api.getUserWantToPlayList(
                authUser = credentials.username,
                apiKey = credentials.apiKey,
                targetUser = credentials.username,
                count = WANT_TO_PLAY_PAGE_SIZE,
                offset = offset
            )
            accumulated += page.results
            if (page.results.isEmpty() || accumulated.size >= page.total) return@safeApiCall accumulated
            offset += page.results.size
        }
        accumulated
    }

    /**
     * Several screens load the same data when the app starts, each through its own repository.
     * While one identical request (same call, same credentials, same arguments) is still in
     * flight, later callers wait for its result instead of sending another: RetroAchievements
     * rate-limits, and duplicates were enough to trip it. Once a request finishes, the next
     * call makes a fresh one, so refreshes always hit the network.
     */
    private suspend fun <T> coalesced(key: String, call: suspend () -> Result<T>): Result<T> {
        val fullKey = "${System.identityHashCode(api)}:$key"
        @Suppress("UNCHECKED_CAST")
        val shared = synchronized(inFlight) {
            inFlight.getOrPut(fullKey) {
                // Lazy, so the completion handler is registered before the call can finish.
                val deferred = sharedScope.async(start = CoroutineStart.LAZY) { call() }
                deferred.invokeOnCompletion {
                    synchronized(inFlight) { if (inFlight[fullKey] === deferred) inFlight.remove(fullKey) }
                }
                deferred
            }
        } as Deferred<Result<T>>
        shared.start()
        return shared.await()
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
        /** Runs shared requests so one caller going away doesn't cancel it for the others. */
        private val sharedScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        private val inFlight = HashMap<String, Deferred<*>>()

        private const val SEVEN_DAYS_IN_MINUTES = 10080
        private const val MAX_RECENTLY_PLAYED_COUNT = 50
        private const val LIBRARY_PAGE_SIZE = 500
        private const val MAX_LIBRARY_PAGES = 3
        private const val WANT_TO_PLAY_PAGE_SIZE = 500
        private const val MAX_WANT_TO_PLAY_PAGES = 3
    }
}
