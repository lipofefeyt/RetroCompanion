package com.example.retrocompanion.data.model

import com.google.gson.annotations.SerializedName

/** One entry from API_GetUserCompletionProgress.php's "Results" array. */
data class GameProgressSummary(
    @SerializedName(value = "GameID", alternate = ["gameID", "gameId"])
    val gameId: Int = 0,

    @SerializedName(value = "Title", alternate = ["title"])
    val title: String = "",

    @SerializedName(value = "ConsoleName", alternate = ["consoleName"])
    val consoleName: String? = null,

    @SerializedName(value = "ImageIcon", alternate = ["imageIcon"])
    val imageIcon: String? = null,

    @SerializedName(value = "MaxPossible", alternate = ["maxPossible"])
    val maxPossible: Int = 0,

    @SerializedName(value = "NumAwarded", alternate = ["numAwarded"])
    val numAwarded: Int = 0,

    @SerializedName(value = "NumAwardedHardcore", alternate = ["numAwardedHardcore"])
    val numAwardedHardcore: Int = 0
) {
    val iconUrl: String
        get() {
            val icon = imageIcon ?: return ""
            return when {
                icon.startsWith("http://") || icon.startsWith("https://") -> icon
                icon.startsWith("/") -> "https://media.retroachievements.org$icon"
                else -> "https://media.retroachievements.org/Images/$icon"
            }
        }

    val progressFraction: Float
        get() {
            if (maxPossible <= 0) return 0f
            return (numAwarded.toFloat() / maxPossible).coerceIn(0f, 1f)
        }

    val progressPercentage: Int
        get() = (progressFraction * 100).toInt()
}

/** API_GetUserCompletionProgress.php's top-level response shape. */
data class UserCompletionProgressPage(
    @SerializedName(value = "Count", alternate = ["count"])
    val count: Int = 0,

    @SerializedName(value = "Total", alternate = ["total"])
    val total: Int = 0,

    @SerializedName(value = "Results", alternate = ["results"])
    val results: List<GameProgressSummary> = emptyList()
)
