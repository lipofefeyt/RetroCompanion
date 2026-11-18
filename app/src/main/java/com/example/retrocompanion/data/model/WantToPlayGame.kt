package com.example.retrocompanion.data.model

import com.google.gson.annotations.SerializedName

/** One entry from API_GetUserWantToPlayList.php's "Results" array. */
data class WantToPlayGame(
    @SerializedName(value = "ID", alternate = ["id", "GameID", "gameId"])
    val gameId: Int = 0,

    @SerializedName(value = "Title", alternate = ["title"])
    val title: String = "",

    @SerializedName(value = "ImageIcon", alternate = ["imageIcon"])
    val imageIcon: String? = null,

    @SerializedName(value = "ConsoleID", alternate = ["consoleID", "consoleId"])
    val consoleId: Int? = null,

    @SerializedName(value = "ConsoleName", alternate = ["consoleName"])
    val consoleName: String? = null,

    @SerializedName(value = "PointsTotal", alternate = ["pointsTotal"])
    val pointsTotal: Int = 0,

    @SerializedName(value = "AchievementsPublished", alternate = ["achievementsPublished"])
    val achievementsPublished: Int = 0
) {
    fun toWishlistEntry() = WishlistEntry(
        gameId = gameId,
        title = title,
        consoleName = consoleName,
        imageIcon = imageIcon
    )
}

/** API_GetUserWantToPlayList.php's top-level response shape. */
data class WantToPlayPage(
    @SerializedName(value = "Count", alternate = ["count"])
    val count: Int = 0,

    @SerializedName(value = "Total", alternate = ["total"])
    val total: Int = 0,

    @SerializedName(value = "Results", alternate = ["results"])
    val results: List<WantToPlayGame> = emptyList()
)
