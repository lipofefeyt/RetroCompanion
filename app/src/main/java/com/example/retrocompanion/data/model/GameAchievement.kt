package com.example.retrocompanion.data.model

import com.google.gson.annotations.SerializedName

/**
 * One entry from API_GetGameInfoAndUserProgress.php's "Achievements" object (keyed by achievement
 * ID — see [GameInfo]). DateEarned/DateEarnedHardcore are only present in the response once the
 * user has earned the achievement, so they're nullable here and their presence (not a boolean
 * flag) is what "earned" means.
 */
data class GameAchievement(
    @SerializedName(value = "ID", alternate = ["id"])
    val id: Int = 0,

    @SerializedName(value = "Title", alternate = ["title"])
    val title: String = "",

    @SerializedName(value = "Description", alternate = ["description"])
    val description: String? = null,

    @SerializedName(value = "Points", alternate = ["points"])
    val points: Int = 0,

    @SerializedName(value = "BadgeName", alternate = ["badgeName"])
    val badgeName: String? = null,

    @SerializedName(value = "DisplayOrder", alternate = ["displayOrder"])
    val displayOrder: Int = 0,

    @SerializedName(value = "DateEarned", alternate = ["dateEarned"])
    val dateEarned: String? = null,

    @SerializedName(value = "DateEarnedHardcore", alternate = ["dateEarnedHardcore"])
    val dateEarnedHardcore: String? = null
) {
    val isEarned: Boolean get() = !dateEarned.isNullOrBlank()
    val isEarnedHardcore: Boolean get() = !dateEarnedHardcore.isNullOrBlank()

    val badgeUrl: String
        get() {
            val name = badgeName
            if (name.isNullOrBlank()) return ""
            return "https://media.retroachievements.org/Badge/$name.png"
        }
}
