package com.example.retrocompanion.data.model

import com.google.gson.annotations.SerializedName

data class UserProfileSummary(
    @SerializedName(value = "User", alternate = ["user", "username"])
    val user: String = "",

    @SerializedName(value = "TotalPoints", alternate = ["totalPoints", "Points", "points"])
    val totalPoints: Int = 0,

    @SerializedName(value = "TotalTruePoints", alternate = ["totalTruePoints", "TruePoints", "truePoints"])
    val totalTruePoints: Int = 0,

    @SerializedName(value = "Rank", alternate = ["rank"])
    val rank: Int? = null,

    @SerializedName(value = "UserPic", alternate = ["userPic", "UserIcon", "avatar"])
    val userPic: String? = null,

    @SerializedName(value = "Motto", alternate = ["motto"])
    val motto: String? = null
) {
    val avatarUrl: String
        get() {
            val pic = userPic
            return when {
                pic.isNullOrBlank() -> "https://media.retroachievements.org/UserPic/$user.png"
                pic.startsWith("http://") || pic.startsWith("https://") -> pic
                pic.startsWith("/") -> "https://media.retroachievements.org$pic"
                else -> "https://media.retroachievements.org/UserPic/$pic"
            }
        }
}
