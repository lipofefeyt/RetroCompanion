package com.example.retrocompanion.data.model

import com.google.gson.annotations.SerializedName

data class RecentUnlock(
    @SerializedName(value = "Date", alternate = ["date", "DateUnlocked", "unlockedAt"])
    val date: String? = null,

    @SerializedName(value = "HardcoreMode", alternate = ["hardcoreMode", "hardcore"])
    val hardcoreMode: Int = 0,

    @SerializedName(value = "AchievementID", alternate = ["achievementID", "achievementId", "ID"])
    val achievementId: Int = 0,

    @SerializedName(value = "Title", alternate = ["title", "AchievementTitle"])
    val title: String = "",

    @SerializedName(value = "Description", alternate = ["description"])
    val description: String? = null,

    @SerializedName(value = "BadgeName", alternate = ["badgeName", "Badge"])
    val badgeName: String? = null,

    @SerializedName(value = "Points", alternate = ["points"])
    val points: Int = 0,

    @SerializedName(value = "TrueRatio", alternate = ["trueRatio"])
    val trueRatio: Int? = null,

    @SerializedName(value = "Author", alternate = ["author"])
    val author: String? = null,

    @SerializedName(value = "GameTitle", alternate = ["gameTitle"])
    val gameTitle: String? = null,

    @SerializedName(value = "GameIcon", alternate = ["gameIcon"])
    val gameIcon: String? = null,

    @SerializedName(value = "ConsoleName", alternate = ["consoleName"])
    val consoleName: String? = null,

    @SerializedName(value = "BadgeURL", alternate = ["badgeURL", "badgeUrl"])
    val badgeUrlString: String? = null,

    @SerializedName(value = "GameID", alternate = ["gameID", "gameId"])
    val gameId: Int? = null
) {
    val displayBadgeUrl: String
        get() {
            val url = badgeUrlString
            if (!url.isNullOrBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
                return url
            }
            val name = badgeName
            if (!name.isNullOrBlank()) {
                if (name.startsWith("http://") || name.startsWith("https://")) {
                    return name
                }
                return if (name.endsWith(".png", ignoreCase = true)) {
                    "https://media.retroachievements.org/Badge/$name"
                } else {
                    "https://media.retroachievements.org/Badge/$name.png"
                }
            }
            return ""
        }

    val isHardcore: Boolean
        get() = hardcoreMode == 1
}
