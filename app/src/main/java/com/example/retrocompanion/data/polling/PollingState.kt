package com.example.retrocompanion.data.polling

import com.example.retrocompanion.data.model.RecentUnlock

/** An unlock the background poller has already dealt with. */
data class SeenUnlock(val achievementId: Int, val date: String)

/**
 * What the poller remembers between runs so it never notifies about the same unlock twice and
 * never notifies about a backlog. Scoped to [username]: a different account starts fresh.
 */
data class PollingState(
    val username: String = "",
    val lastRunMillis: Long? = null,
    /** Newest unlock date seen so far, in the API's sortable "yyyy-MM-dd HH:mm:ss" format. */
    val highWaterDate: String? = null,
    val seen: List<SeenUnlock> = emptyList()
)

data class DiffResult(
    /** Unlocks to notify about, oldest first. Empty on a baseline run. */
    val newUnlocks: List<RecentUnlock>,
    val state: PollingState,
    val wasBaseline: Boolean
)

/** Pure dedup logic for the poller, kept free of Android and network types. */
object UnlockDeduper {
    /** If the last run is older than this (e.g. notifications were off for a while), start fresh. */
    const val BASELINE_AFTER_MILLIS = 24L * 60 * 60 * 1000

    /** Lookback used when there is nothing to compare against. Anything found is only recorded. */
    const val BASELINE_LOOKBACK_MINUTES = 60

    /** Extra minutes added to the time since the last run so a delayed run can't miss unlocks. */
    const val OVERLAP_MINUTES = 15

    const val MIN_LOOKBACK_MINUTES = 30
    const val MAX_LOOKBACK_MINUTES = 7 * 24 * 60

    /** Cap on remembered unlocks so the stored state can't grow forever. */
    const val MAX_SEEN = 500

    fun isBaseline(previous: PollingState, username: String, nowMillis: Long): Boolean {
        val lastRun = previous.lastRunMillis ?: return true
        return previous.username != username || nowMillis - lastRun > BASELINE_AFTER_MILLIS
    }

    /** Minutes to ask the API to look back: since the last run plus an overlap, within sane bounds. */
    fun lookbackMinutes(previous: PollingState, username: String, nowMillis: Long): Int {
        if (isBaseline(previous, username, nowMillis)) return BASELINE_LOOKBACK_MINUTES
        val sinceLastRun = ((nowMillis - previous.lastRunMillis!!).coerceAtLeast(0) / 60_000).toInt()
        return (sinceLastRun + OVERLAP_MINUTES).coerceIn(MIN_LOOKBACK_MINUTES, MAX_LOOKBACK_MINUTES)
    }

    fun diff(
        fetched: List<RecentUnlock>,
        previous: PollingState,
        username: String,
        nowMillis: Long
    ): DiffResult {
        val baseline = isBaseline(previous, username, nowMillis)
        val usable = fetched.filter { !it.date.isNullOrBlank() && it.achievementId != 0 }

        val seenIds = if (baseline) emptySet() else previous.seen.mapTo(HashSet()) { it.achievementId }
        val highWater = if (baseline) null else previous.highWaterDate

        // The same achievement can be listed once per mode (softcore/hardcore); notify once,
        // for its earliest entry. Anything older than the high-water mark that we don't already
        // know about is a late sync of an old unlock, not news.
        val newUnlocks = if (baseline) {
            emptyList()
        } else {
            usable
                .filter { it.achievementId !in seenIds && (highWater == null || it.date!! >= highWater) }
                .groupBy { it.achievementId }
                .map { (_, entries) -> entries.minBy { it.date!! } }
                .sortedBy { it.date }
        }

        val remembered = (if (baseline) emptyList() else previous.seen) +
            usable.map { SeenUnlock(it.achievementId, it.date!!) }
        val seen = remembered
            .groupBy { it.achievementId }
            .map { (_, entries) -> entries.minBy { it.date } }
            .sortedByDescending { it.date }
            .take(MAX_SEEN)

        val newestFetched = usable.maxOfOrNull { it.date!! }
        val newHighWater = listOfNotNull(highWater, newestFetched).maxOrNull()

        return DiffResult(
            newUnlocks = newUnlocks,
            state = PollingState(
                username = username,
                lastRunMillis = nowMillis,
                highWaterDate = newHighWater,
                seen = seen
            ),
            wasBaseline = baseline
        )
    }
}
