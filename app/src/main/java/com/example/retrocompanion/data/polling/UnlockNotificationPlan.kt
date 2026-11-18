package com.example.retrocompanion.data.polling

import com.example.retrocompanion.data.model.RecentUnlock

/** One notification to post. [gameId] is where tapping it should go, if known. */
data class PlannedNotification(
    val id: Int,
    val title: String,
    val text: String,
    val gameId: Int?
)

/**
 * Decides what to notify for a batch of new unlocks. Pure and Android-free so it can be unit
 * tested. Content is limited to the achievement, its game and its points: nothing about the
 * account, and never the API key.
 */
object UnlockNotificationPlan {
    /** More new unlocks than this in one poll are folded into a single summary. */
    const val MAX_INDIVIDUAL = 5

    const val SUMMARY_ID = Int.MIN_VALUE

    fun from(unlocks: List<RecentUnlock>): List<PlannedNotification> {
        if (unlocks.isEmpty()) return emptyList()
        if (unlocks.size > MAX_INDIVIDUAL) {
            val games = unlocks.mapNotNull { it.gameTitle?.takeIf { title -> title.isNotBlank() } }.distinct()
            val text = when (games.size) {
                0 -> "Open RetroCompanion to see them."
                1 -> games.single()
                else -> "${games.first()} and ${games.size - 1} more"
            }
            val onlyGame = unlocks.mapNotNull { it.gameId }.distinct().singleOrNull()
            return listOf(
                PlannedNotification(
                    id = SUMMARY_ID,
                    title = "${unlocks.size} new achievements unlocked",
                    text = text,
                    gameId = onlyGame
                )
            )
        }
        return unlocks.map { unlock ->
            val details = listOfNotNull(
                unlock.gameTitle?.takeIf { it.isNotBlank() },
                unlock.points.takeIf { it > 0 }?.let { "$it pts" }
            ).joinToString(" · ")
            PlannedNotification(
                id = unlock.achievementId,
                title = unlock.title.ifBlank { "Achievement unlocked" },
                text = details.ifEmpty { "Achievement unlocked" },
                gameId = unlock.gameId
            )
        }
    }
}
