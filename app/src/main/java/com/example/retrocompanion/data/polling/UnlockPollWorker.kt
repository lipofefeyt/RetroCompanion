package com.example.retrocompanion.data.polling

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.retrocompanion.data.local.UserPreferencesRepository
import com.example.retrocompanion.data.repository.RetroAchievementsRepository

/**
 * WorkManager entry point for [UnlockPoller]. Deliberately thin and silent: it logs nothing,
 * since requests carry the API key and the poller's results are fixed strings anyway.
 */
class UnlockPollWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val poller = UnlockPoller(
            credentialsSource = UserPreferencesRepository(applicationContext),
            repository = RetroAchievementsRepository(),
            stateStore = DataStorePollingStateStore(applicationContext),
            notifier = AndroidUnlockNotifier(applicationContext)
        )
        return when (poller.poll()) {
            is PollOutcome.Completed, PollOutcome.NoCredentials -> Result.success()
            is PollOutcome.Retry ->
                // Give up on this period after a few tries; the next periodic run starts fresh.
                if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
            // A periodic work request keeps its schedule after a failure; only this run ends.
            is PollOutcome.Failed -> Result.failure()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
