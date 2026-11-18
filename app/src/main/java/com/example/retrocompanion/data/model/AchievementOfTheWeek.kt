package com.example.retrocompanion.data.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * API_GetAchievementOfTheWeek.php's response. Only the fields the app uses are mapped. The
 * `Unlocks` list can be long and the docs don't say whether it is complete, so it is only used to
 * tell whether the signed-in user is in it, and is never stored (see [AchievementOfTheWeek]).
 */
data class AchievementOfTheWeekResponse(
    @SerializedName(value = "Achievement", alternate = ["achievement"])
    val achievement: AotwAchievement? = null,

    @SerializedName(value = "Console", alternate = ["console"])
    val console: AotwNamed? = null,

    @SerializedName(value = "Game", alternate = ["game"])
    val game: AotwNamed? = null,

    @SerializedName(value = "StartAt", alternate = ["startAt"])
    val startAt: String? = null,

    @SerializedName(value = "TotalPlayers", alternate = ["totalPlayers"])
    val totalPlayers: Int = 0,

    @SerializedName(value = "Unlocks", alternate = ["unlocks"])
    val unlocks: List<AotwUnlock>? = null,

    @SerializedName(value = "UnlocksCount", alternate = ["unlocksCount"])
    val unlocksCount: Int = 0,

    @SerializedName(value = "UnlocksHardcoreCount", alternate = ["unlocksHardcoreCount"])
    val unlocksHardcoreCount: Int = 0
) {
    /** The compact summary for [username], or null if the response has no achievement in it. */
    fun toSummary(username: String): AchievementOfTheWeek? {
        val achievement = achievement?.takeIf { it.id > 0 } ?: return null
        return AchievementOfTheWeek(
            achievementId = achievement.id,
            title = achievement.title,
            description = achievement.description?.takeIf { it.isNotBlank() },
            points = achievement.points,
            badgeName = achievement.badgeName?.takeIf { it.isNotBlank() },
            gameId = game?.id ?: 0,
            gameTitle = game?.title.orEmpty(),
            consoleName = console?.title?.takeIf { it.isNotBlank() },
            startAt = startAt,
            totalPlayers = totalPlayers,
            unlocksCount = unlocksCount,
            unlocksHardcoreCount = unlocksHardcoreCount,
            unlockedByUser = username.isNotBlank() &&
                unlocks.orEmpty().any { it.user.equals(username, ignoreCase = true) }
        )
    }
}

data class AotwAchievement(
    @SerializedName(value = "ID", alternate = ["id"])
    val id: Int = 0,

    @SerializedName(value = "Title", alternate = ["title"])
    val title: String = "",

    @SerializedName(value = "Description", alternate = ["description"])
    val description: String? = null,

    @SerializedName(value = "Points", alternate = ["points"])
    val points: Int = 0,

    /** Not in the documented response; used if the live response happens to include it. */
    @SerializedName(value = "BadgeName", alternate = ["badgeName"])
    val badgeName: String? = null
)

/** `Console` and `Game` are both `{ "ID": .., "Title": .. }`. */
data class AotwNamed(
    @SerializedName(value = "ID", alternate = ["id"])
    val id: Int = 0,

    @SerializedName(value = "Title", alternate = ["title"])
    val title: String? = null
)

data class AotwUnlock(
    @SerializedName(value = "User", alternate = ["user"])
    val user: String? = null
)

/**
 * The compact, cacheable form of the Achievement of the Week: everything the Home card shows and
 * nothing from the (potentially large) unlock list.
 */
data class AchievementOfTheWeek(
    val achievementId: Int,
    val title: String,
    val description: String?,
    val points: Int,
    val badgeName: String?,
    val gameId: Int,
    val gameTitle: String,
    val consoleName: String?,
    /** ISO 8601 UTC, e.g. "2023-10-23T00:00:00.000000Z". */
    val startAt: String?,
    val totalPlayers: Int,
    val unlocksCount: Int,
    val unlocksHardcoreCount: Int,
    /** True only if the signed-in user was found in the response's unlock list. */
    val unlockedByUser: Boolean
) {
    val badgeUrl: String
        get() = badgeName?.let { "https://media.retroachievements.org/Badge/$it.png" }.orEmpty()

    /** Share of all players who unlocked it, or null if the player total is unknown. */
    val unlockedPercent: Float?
        get() = if (totalPlayers > 0) unlocksCount * 100f / totalPlayers else null

    /** "Mar 9, 2026" for the day it was featured, or null if [startAt] is missing or malformed. */
    fun featuredSinceLabel(locale: Locale = Locale.getDefault()): String? {
        val day = startAt?.take(10) ?: return null
        return try {
            val utc = TimeZone.getTimeZone("UTC")
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = utc
                isLenient = false
            }.parse(day) ?: return null
            SimpleDateFormat("MMM d, yyyy", locale).apply { timeZone = utc }.format(parsed)
        } catch (_: Exception) {
            null
        }
    }
}
