package com.example.retrocompanion.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response of API_GetGameInfoAndUserProgress.php. "Achievements" in the raw API response is a
 * JSON object keyed by achievement ID (a string), not an array — Gson maps that straight onto
 * a Map<String, GameAchievement> here; [achievementsSortedForDisplay] turns it into a stable list.
 */
data class GameInfo(
    @SerializedName(value = "ID", alternate = ["id"])
    val gameId: Int = 0,

    @SerializedName(value = "Title", alternate = ["title"])
    val title: String = "",

    @SerializedName(value = "ConsoleName", alternate = ["consoleName"])
    val consoleName: String? = null,

    @SerializedName(value = "ImageIcon", alternate = ["imageIcon"])
    val imageIcon: String? = null,

    @SerializedName(value = "ImageBoxArt", alternate = ["imageBoxArt"])
    val imageBoxArt: String? = null,

    @SerializedName(value = "NumAwardedToUser", alternate = ["numAwardedToUser"])
    val numAwardedToUser: Int = 0,

    @SerializedName(value = "Achievements", alternate = ["achievements"])
    val achievements: Map<String, GameAchievement>? = null
) {
    val iconUrl: String
        get() = resolveImageUrl(imageIcon)

    val boxArtUrl: String
        get() = resolveImageUrl(imageBoxArt)

    /** Achievements as a list, sorted by their display order (falls back to achievement ID). */
    val achievementsSortedForDisplay: List<GameAchievement>
        get() = achievements.orEmpty().values.sortedWith(compareBy({ it.displayOrder }, { it.id }))

    private fun resolveImageUrl(path: String?): String {
        if (path.isNullOrBlank()) return ""
        return when {
            path.startsWith("http://") || path.startsWith("https://") -> path
            path.startsWith("/") -> "https://media.retroachievements.org$path"
            else -> "https://media.retroachievements.org/$path"
        }
    }
}
