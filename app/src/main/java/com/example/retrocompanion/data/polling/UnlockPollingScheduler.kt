package com.example.retrocompanion.data.polling

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Turns background unlock polling on and off. Safe to call repeatedly. */
object UnlockPollingScheduler {
    const val UNIQUE_WORK_NAME = "unlock_polling"

    /** WorkManager's minimum period for periodic work. */
    const val INTERVAL_MINUTES = 15L

    private const val BACKOFF_MINUTES = 5L

    fun enable(context: Context) {
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            // KEEP so re-enabling (or the app restarting) doesn't reset the timer.
            ExistingPeriodicWorkPolicy.KEEP,
            buildRequest()
        )
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    internal fun buildRequest(): PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<UnlockPollWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()
}
