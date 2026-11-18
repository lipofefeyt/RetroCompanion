package com.example.retrocompanion.data.polling

import com.example.retrocompanion.data.local.UserCredentialsSource
import com.example.retrocompanion.data.remote.RetroApiException
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

sealed interface PollOutcome {
    /** The poll ran. [newUnlockCount] unlocks were handed to the notifier. */
    data class Completed(val newUnlockCount: Int, val wasBaseline: Boolean) : PollOutcome

    /** No usable credentials are stored, so there was nothing to do. */
    data object NoCredentials : PollOutcome

    /** A transient problem (offline, rate limit, server error): worth trying again soon. */
    data class Retry(val reason: String) : PollOutcome

    /** A problem retrying won't fix (e.g. the API key was rejected). */
    data class Failed(val reason: String) : PollOutcome
}

/**
 * One background poll: ask the API for recent unlocks (a single request), work out which are
 * new, hand them to [notifier], and remember them. Reasons in [PollOutcome] are fixed strings,
 * never exception messages or request data, so nothing sensitive can leak through them.
 */
class UnlockPoller(
    private val credentialsSource: UserCredentialsSource,
    private val repository: RetroAchievementsRepository,
    private val stateStore: PollingStateStore,
    private val notifier: UnlockNotifier,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun poll(): PollOutcome {
        val credentials = credentialsSource.userCredentialsFlow.first()
        if (!credentials.isValid) return PollOutcome.NoCredentials

        val previous = stateStore.load()
        val now = nowMillis()
        val minutes = UnlockDeduper.lookbackMinutes(previous, credentials.username, now)

        val result = repository.getRecentUnlocks(credentials, lookbackMinutes = minutes)
        val fetched = result.getOrElse { error -> return outcomeFor(error) }

        val diff = UnlockDeduper.diff(fetched, previous, credentials.username, now)
        if (diff.newUnlocks.isNotEmpty()) {
            // Notify before saving: if this fails we retry and notify again (at-least-once)
            // rather than saving first and silently losing the notification.
            try {
                notifier.notifyNewUnlocks(diff.newUnlocks)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return PollOutcome.Retry("Could not show the notification.")
            }
        }
        stateStore.save(diff.state)
        return PollOutcome.Completed(diff.newUnlocks.size, diff.wasBaseline)
    }

    private fun outcomeFor(error: Throwable): PollOutcome = when (error) {
        is RetroApiException.InvalidApiKey -> PollOutcome.Failed("The API key was rejected.")
        is RetroApiException.RateLimited -> PollOutcome.Retry("Rate limited.")
        is RetroApiException.NoNetwork -> PollOutcome.Retry("No network connection.")
        is RetroApiException.UnexpectedError -> PollOutcome.Retry("Unexpected API response.")
        else -> PollOutcome.Failed("Polling failed.")
    }
}
