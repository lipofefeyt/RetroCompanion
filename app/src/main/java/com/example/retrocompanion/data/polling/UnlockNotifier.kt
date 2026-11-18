package com.example.retrocompanion.data.polling

import com.example.retrocompanion.data.model.RecentUnlock

/** Receives the unlocks the background poller found. Implemented by the notification layer. */
fun interface UnlockNotifier {
    /** [unlocks] are new since the last run, oldest first. May throw to ask for a retry. */
    suspend fun notifyNewUnlocks(unlocks: List<RecentUnlock>)
}

/** Used until notifications are wired up: the poller still runs and tracks state, silently. */
object NoOpUnlockNotifier : UnlockNotifier {
    override suspend fun notifyNewUnlocks(unlocks: List<RecentUnlock>) = Unit
}
