package com.example.retrocompanion.data.model

import com.google.gson.annotations.SerializedName

data class RecentlyPlayedGame(
    @SerializedName(value = "GameID", alternate = ["gameID", "gameId", "ID"])
    val gameId: Int = 0,

    @SerializedName(value = "ConsoleID", alternate = ["consoleID", "consoleId"])
    val consoleId: Int = 0,

    @SerializedName(value = "ConsoleName", alternate = ["consoleName", "Console"])
    val consoleName: String? = null,

    @SerializedName(value = "Title", alternate = ["title", "GameTitle"])
    val title: String = "",

    @SerializedName(value = "ImageIcon", alternate = ["imageIcon", "Icon", "GameIcon"])
    val imageIcon: String? = null,

    @SerializedName(value = "NumPossibleAchievements", alternate = ["numPossibleAchievements", "PossibleAchievements", "AchievementsTotal", "MaxPossible"])
    val numPossibleAchievements: Int = 0,

    @SerializedName(value = "NumAchieved", alternate = ["numAchieved", "Achieved", "NumAwarded"])
    val numAchieved: Int = 0,

    @SerializedName(value = "Score", alternate = ["score", "MyScore"])
    val score: Int = 0,

    @SerializedName(value = "LastPlayed", alternate = ["lastPlayed"])
    val lastPlayed: String? = null
) {
    val gameIconUrl: String
        get() {
            val icon = imageIcon ?: return ""
            return when {
                icon.startsWith("http://") || icon.startsWith("https://") -> icon
                icon.startsWith("/") -> "https://media.retroachievements.org$icon"
                else -> "https://media.retroachievements.org/Images/$icon"
            }
        }

    val totalAchievements: Int
        get() = maxOf(0, numPossibleAchievements)

    val progressFraction: Float
        get() {
            val total = totalAchievements
            if (total == 0) return 0f
            return (numAchieved.toFloat() / total).coerceIn(0f, 1f)
        }

    val progressPercentage: Int
        get() = (progressFraction * 100).toInt()
}
