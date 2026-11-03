package com.example.retrocompanion.data.remote

import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Parses saved sample RetroAchievements API responses (no real account data) into the app's
 * data models, the same way the Retrofit/Gson stack in [RetrofitClient] would.
 */
class SampleResponseParsingTest {

    private val gson = Gson()

    private fun readResource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "Missing test resource: $name" }
            .bufferedReader()
            .use { it.readText() }

    @Test
    fun `parses sample recently played games response`() {
        val json = readResource("sample_recently_played_games.json")
        val games: List<RecentlyPlayedGame> = gson.fromJson(json, object : TypeToken<List<RecentlyPlayedGame>>() {}.type)

        assertEquals(2, games.size)

        val finalFantasy = games[0]
        assertEquals(11332, finalFantasy.gameId)
        assertEquals("Final Fantasy Origins", finalFantasy.title)
        assertEquals("PlayStation", finalFantasy.consoleName)
        assertEquals(119, finalFantasy.numPossibleAchievements)
        assertEquals(38, finalFantasy.numAchieved)
        assertEquals(382, finalFantasy.score)
        assertEquals("https://media.retroachievements.org/Images/060249.png", finalFantasy.gameIconUrl)
        assertEquals(31, finalFantasy.progressPercentage)

        val noAchievementsGame = games[1]
        assertEquals(0, noAchievementsGame.numPossibleAchievements)
        assertEquals(0f, noAchievementsGame.progressFraction)
        assertEquals(0, noAchievementsGame.progressPercentage)
    }

    @Test
    fun `parses sample recent achievements response`() {
        val json = readResource("sample_recent_achievements.json")
        val unlocks: List<RecentUnlock> = gson.fromJson(json, object : TypeToken<List<RecentUnlock>>() {}.type)

        assertEquals(1, unlocks.size)

        val unlock = unlocks[0]
        assertEquals(98012, unlock.achievementId)
        assertEquals("Beginner I", unlock.title)
        assertEquals("Pokemon Pinball mini", unlock.gameTitle)
        assertEquals(5, unlock.points)
        assertEquals(true, unlock.isHardcore)
        assertEquals("2023-12-27 16:04:50", unlock.date)
        assertEquals("https://media.retroachievements.org/Badge/108302.png", unlock.displayBadgeUrl)
    }

    @Test
    fun `missing DateEarned fields on an unearned achievement don't break parsing`() {
        val json = """[{"AchievementID": 1, "Title": "Untouched", "HardcoreMode": 0}]"""
        val unlocks: List<RecentUnlock> = gson.fromJson(json, object : TypeToken<List<RecentUnlock>>() {}.type)

        assertEquals(1, unlocks.size)
        assertEquals(false, unlocks[0].isHardcore)
        assertNull(unlocks[0].date)
    }
}
