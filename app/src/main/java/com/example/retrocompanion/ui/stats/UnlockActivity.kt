package com.example.retrocompanion.ui.stats

import com.example.retrocompanion.data.model.UnlockStamp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** One day in the heatmap; [level] is 0 (no unlocks) to 4 (busiest). */
data class HeatmapDay(val epochDay: Long, val count: Int, val level: Int)

/** One heatmap column: Monday..Sunday, with null for days before the window or after today. */
data class HeatmapWeek(val startEpochDay: Long, val days: List<HeatmapDay?>) {
    val totalUnlocks: Int get() = days.sumOf { it?.count ?: 0 }
    val activeDays: Int get() = days.count { (it?.count ?: 0) > 0 }
}

/**
 * Unlock streaks and a calendar heatmap, built from raw unlock timestamps.
 *
 * Days are local calendar days in the device time zone: the API doesn't document a time zone
 * for its day boundaries, so unlock times are read as UTC and then bucketed locally. Streaks
 * and the longest streak only look at the [windowDays] shown, so "longest" is the longest
 * within that window.
 */
data class UnlockActivity(
    val windowDays: Int,
    val totalUnlocks: Int,
    val activeDays: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val weeks: List<HeatmapWeek>
) {
    companion object {
        const val DEFAULT_WINDOW_DAYS = 182
        private const val MILLIS_PER_DAY = 86_400_000L

        /** Days since 1970-01-01 of the local calendar day containing [millis] in [timeZone]. */
        fun localEpochDay(millis: Long, timeZone: TimeZone): Long =
            Math.floorDiv(millis + timeZone.getOffset(millis), MILLIS_PER_DAY)

        /** Monday = 0 .. Sunday = 6 (1970-01-01, epoch day 0, was a Thursday). */
        fun dayOfWeekIndex(epochDay: Long): Int = Math.floorMod(epochDay + 3, 7L).toInt()

        /** Parses the API's "yyyy-MM-dd HH:mm:ss" (UTC) into epoch millis, or null if malformed. */
        fun parseUnlockMillis(date: String): Long? = try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }.parse(date.trim())?.time
        } catch (_: Exception) {
            null
        }

        fun from(
            stamps: List<UnlockStamp>,
            nowMillis: Long,
            timeZone: TimeZone,
            windowDays: Int = DEFAULT_WINDOW_DAYS
        ): UnlockActivity {
            val today = localEpochDay(nowMillis, timeZone)
            val start = today - windowDays + 1

            // The same achievement can appear once per mode (softcore and hardcore) and in
            // overlapping fetches; count each achievement once, at its earliest unlock.
            val earliestById = HashMap<Int, Long>()
            for (stamp in stamps) {
                val millis = parseUnlockMillis(stamp.date) ?: continue
                val existing = earliestById[stamp.achievementId]
                if (existing == null || millis < existing) earliestById[stamp.achievementId] = millis
            }

            val countsByDay = HashMap<Long, Int>()
            for (millis in earliestById.values) {
                val day = localEpochDay(millis, timeZone)
                if (day in start..today) countsByDay.merge(day, 1, Int::plus)
            }

            val maxCount = countsByDay.values.maxOrNull() ?: 0
            fun levelFor(count: Int): Int =
                if (count <= 0 || maxCount <= 0) 0 else ((count * 4 + maxCount - 1) / maxCount).coerceIn(1, 4)

            return UnlockActivity(
                windowDays = windowDays,
                totalUnlocks = countsByDay.values.sum(),
                activeDays = countsByDay.size,
                currentStreak = currentStreak(countsByDay.keys, today),
                longestStreak = longestStreak(countsByDay.keys),
                weeks = buildWeeks(countsByDay, start, today, ::levelFor)
            )
        }

        /** Consecutive active days ending today, or ending yesterday if today has none yet. */
        private fun currentStreak(activeDays: Set<Long>, today: Long): Int {
            var day = if (today in activeDays) today else today - 1
            var streak = 0
            while (day in activeDays) {
                streak++
                day--
            }
            return streak
        }

        private fun longestStreak(activeDays: Set<Long>): Int {
            var longest = 0
            var run = 0
            var previous: Long? = null
            for (day in activeDays.sorted()) {
                run = if (previous != null && day == previous + 1) run + 1 else 1
                if (run > longest) longest = run
                previous = day
            }
            return longest
        }

        private fun buildWeeks(
            countsByDay: Map<Long, Int>,
            start: Long,
            today: Long,
            levelFor: (Int) -> Int
        ): List<HeatmapWeek> {
            val weeks = ArrayList<HeatmapWeek>()
            var weekStart = start - dayOfWeekIndex(start)
            while (weekStart <= today) {
                val days = (0 until 7).map { offset ->
                    val day = weekStart + offset
                    if (day < start || day > today) {
                        null
                    } else {
                        val count = countsByDay[day] ?: 0
                        HeatmapDay(day, count, levelFor(count))
                    }
                }
                weeks += HeatmapWeek(weekStart, days)
                weekStart += 7
            }
            return weeks
        }
    }
}

/**
 * Splits the activity window into fixed 90-day chunks (aligned to the Unix epoch, in UTC) so
 * each can be cached on its own: a chunk that has fully ended can be cached for good and never
 * refetched, and only the current chunk is requested on each refresh.
 */
object UnlockChunks {
    /** Large chunks keep the request count low (3-4 for the window); RetroAchievements rate-limits. */
    const val CHUNK_DAYS = 90L

    /**
     * Unlocks can reach the server after the fact (e.g. synced from an offline session), so a
     * chunk only counts as final this many days after it ends.
     */
    const val SETTLE_DAYS = 3L

    private const val SECONDS_PER_DAY = 86_400L

    data class Chunk(val index: Long, val fromEpochSeconds: Long, val toEpochSeconds: Long) {
        /** True once the chunk ended long enough ago that its contents won't change. */
        fun isSettled(nowMillis: Long): Boolean =
            toEpochSeconds < nowMillis / 1000 - SETTLE_DAYS * SECONDS_PER_DAY
    }

    /**
     * Chunks covering the last [windowDays] days, oldest first. Padded by a day on the old side
     * so local-time day boundaries (up to +/-14h from UTC) are always covered.
     */
    fun forWindow(nowMillis: Long, windowDays: Int): List<Chunk> {
        val nowSeconds = nowMillis / 1000
        val today = Math.floorDiv(nowSeconds, SECONDS_PER_DAY)
        val firstIndex = Math.floorDiv(today - windowDays - 1, CHUNK_DAYS)
        val lastIndex = Math.floorDiv(today, CHUNK_DAYS)
        return (firstIndex..lastIndex).map { index ->
            val from = index * CHUNK_DAYS * SECONDS_PER_DAY
            val chunkEnd = (index + 1) * CHUNK_DAYS * SECONDS_PER_DAY - 1
            // Don't ask for the future; the current chunk ends at "now".
            Chunk(index, from, minOf(chunkEnd, nowSeconds))
        }
    }
}
