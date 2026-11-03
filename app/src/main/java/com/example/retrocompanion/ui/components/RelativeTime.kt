package com.example.retrocompanion.ui.components

import java.util.concurrent.TimeUnit

/** "Just now" / "5 minutes ago" / "3 hours ago" / "2 days ago", for a "showing cached data from…" label. */
fun formatRelativeTime(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val deltaMillis = (nowMillis - timestampMillis).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(deltaMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(deltaMillis)
    val days = TimeUnit.MILLISECONDS.toDays(deltaMillis)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes minute${if (minutes == 1L) "" else "s"} ago"
        hours < 24 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
        else -> "$days day${if (days == 1L) "" else "s"} ago"
    }
}
