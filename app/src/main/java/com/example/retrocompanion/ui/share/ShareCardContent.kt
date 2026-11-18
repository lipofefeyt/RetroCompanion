package com.example.retrocompanion.ui.share

import com.example.retrocompanion.data.model.GameAchievement
import com.example.retrocompanion.data.model.GameInfo
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

enum class ShareCardKind(val label: String, val placeholderGlyph: String) {
    /** Every achievement earned in hardcore mode. */
    MASTERY("MASTERED", "🎮"),

    /** Every achievement earned, but not all in hardcore mode. */
    COMPLETION("COMPLETED", "🎮"),

    /** Under [ShareCardContent.RARE_THRESHOLD_PERCENT] of players have earned it. */
    RARE_ACHIEVEMENT("RARE ACHIEVEMENT", "🏆"),

    /** Any other earned achievement. */
    ACHIEVEMENT("ACHIEVEMENT UNLOCKED", "🏆")
}

/**
 * The text and image URL of a shareable card, with no layout concerns. Kept free of Android
 * types so it can be unit tested; [ShareCardRenderer] turns it into a bitmap. It deliberately
 * carries nothing about the account (no username, no key).
 */
data class ShareCardContent(
    val kind: ShareCardKind,
    val title: String,
    val subtitle: String?,
    val description: String?,
    val stats: String,
    val iconUrl: String
) {
    companion object {
        /** An achievement counts as "rare" when under this share of players have earned it. */
        const val RARE_THRESHOLD_PERCENT = 10f

        fun isRare(rarityPercent: Float?): Boolean =
            rarityPercent != null && rarityPercent < RARE_THRESHOLD_PERCENT

        /** True when the game has achievements and the user has earned all of them. */
        fun isCompleted(game: GameInfo): Boolean {
            val achievements = game.achievements?.values.orEmpty()
            return achievements.isNotEmpty() && achievements.all { it.isEarned }
        }

        fun isMastered(game: GameInfo): Boolean {
            val achievements = game.achievements?.values.orEmpty()
            return achievements.isNotEmpty() && achievements.all { it.isEarnedHardcore }
        }

        /** A card for [achievement], or null if it hasn't been earned (nothing to brag about). */
        fun forAchievement(
            game: GameInfo,
            achievement: GameAchievement,
            locale: Locale = Locale.getDefault()
        ): ShareCardContent? {
            if (!achievement.isEarned) return null
            val hardcore = achievement.isEarnedHardcore
            val date = formatDate(
                if (hardcore) achievement.dateEarnedHardcore else achievement.dateEarned,
                locale
            )
            val rarity = game.rarityPercent(achievement)
            val stats = buildList {
                add("${achievement.points} pts")
                rarity?.let {
                    add(String.format(locale, "%.1f%% of players", it))
                }
                if (date != null) add(if (hardcore) "Hardcore $date" else "Unlocked $date")
            }.joinToString(" · ")
            return ShareCardContent(
                kind = if (isRare(rarity)) ShareCardKind.RARE_ACHIEVEMENT else ShareCardKind.ACHIEVEMENT,
                title = achievement.title,
                subtitle = listOfNotNull(game.title.takeIf { it.isNotBlank() }, game.consoleName?.takeIf { it.isNotBlank() })
                    .joinToString(" · ")
                    .takeIf { it.isNotEmpty() },
                description = achievement.description?.takeIf { it.isNotBlank() },
                stats = stats,
                iconUrl = achievement.badgeUrl
            )
        }

        /** A card for a finished game, or null if the user hasn't earned every achievement. */
        fun forCompletion(
            game: GameInfo,
            locale: Locale = Locale.getDefault()
        ): ShareCardContent? {
            if (!isCompleted(game)) return null
            val mastered = isMastered(game)
            val achievements = game.achievements.orEmpty().values
            val date = formatDate(
                achievements.mapNotNull { if (mastered) it.dateEarnedHardcore else it.dateEarned }.maxOrNull(),
                locale
            )
            val stats = buildList {
                add("${achievements.size} achievements")
                add("${achievements.sumOf { it.points }} pts")
                if (date != null) add(if (mastered) "Mastered $date" else "Completed $date")
            }.joinToString(" · ")
            return ShareCardContent(
                kind = if (mastered) ShareCardKind.MASTERY else ShareCardKind.COMPLETION,
                title = game.title,
                subtitle = game.consoleName?.takeIf { it.isNotBlank() },
                description = null,
                stats = stats,
                iconUrl = game.iconUrl
            )
        }

        /** "2026-03-03 14:22:10" -> "Mar 3, 2026"; null if blank or malformed. */
        internal fun formatDate(raw: String?, locale: Locale): String? {
            if (raw.isNullOrBlank()) return null
            return try {
                val utc = TimeZone.getTimeZone("UTC")
                val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                    timeZone = utc
                    isLenient = false
                }.parse(raw.trim()) ?: return null
                SimpleDateFormat("MMM d, yyyy", locale).apply { timeZone = utc }.format(parsed)
            } catch (_: Exception) {
                null
            }
        }
    }
}
