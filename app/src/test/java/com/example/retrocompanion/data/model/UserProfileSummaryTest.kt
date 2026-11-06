package com.example.retrocompanion.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileSummaryTest {

    private val gson = Gson()

    @Test
    fun `parses user profile summary correctly from json`() {
        val json = """
            {
                "User": "RetroPlayer",
                "TotalPoints": 15420,
                "TotalTruePoints": 28950,
                "Rank": 142,
                "UserPic": "/UserPic/RetroPlayer.png",
                "Motto": "Gotta catch em all!"
            }
        """.trimIndent()

        val summary = gson.fromJson(json, UserProfileSummary::class.java)

        assertEquals("RetroPlayer", summary.user)
        assertEquals(15420, summary.totalPoints)
        assertEquals(28950, summary.totalTruePoints)
        assertEquals(142, summary.rank)
        assertEquals("https://media.retroachievements.org/UserPic/RetroPlayer.png", summary.avatarUrl)
        assertEquals("Gotta catch em all!", summary.motto)
    }

    @Test
    fun `handles blank userPic with fallback avatarUrl`() {
        val summary = UserProfileSummary(user = "TestUser", userPic = "")
        assertEquals("https://media.retroachievements.org/UserPic/TestUser.png", summary.avatarUrl)
    }
}
