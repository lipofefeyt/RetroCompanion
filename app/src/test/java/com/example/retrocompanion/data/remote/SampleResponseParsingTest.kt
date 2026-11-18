package com.example.retrocompanion.data.remote

import com.example.retrocompanion.data.model.GameInfo
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.UserCompletionProgressPage
import com.example.retrocompanion.data.model.WantToPlayPage
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

    @Test
    fun `parses sample game info response, including the ID-keyed Achievements object`() {
        val json = readResource("sample_game_info.json")
        val gameInfo = gson.fromJson(json, GameInfo::class.java)

        assertEquals("Pokemon Pinball mini", gameInfo.title)
        assertEquals("Pokemon Mini", gameInfo.consoleName)
        assertEquals("https://media.retroachievements.org/Images/028402.png", gameInfo.boxArtUrl)
        assertEquals(2, gameInfo.achievements?.size)

        val sortedAchievements = gameInfo.achievementsSortedForDisplay
        assertEquals(listOf("Beginner I", "Beginner II"), sortedAchievements.map { it.title })

        val earned = sortedAchievements[0]
        assertEquals(true, earned.isEarned)
        assertEquals(true, earned.isEarnedHardcore)
        assertEquals("2023-12-27 16:04:50", earned.dateEarned)
        assertEquals("https://media.retroachievements.org/Badge/108302.png", earned.badgeUrl)

        val unearned = sortedAchievements[1]
        assertEquals(false, unearned.isEarned)
        assertEquals(false, unearned.isEarnedHardcore)
        assertNull(unearned.dateEarned)
        assertNull(unearned.dateEarnedHardcore)
    }

    @Test
    fun `parses sample user completion progress response`() {
        val json = readResource("sample_user_completion_progress.json")
        val page = gson.fromJson(json, UserCompletionProgressPage::class.java)

        assertEquals(2, page.count)
        assertEquals(2, page.total)
        assertEquals(2, page.results.size)

        val ffOrigins = page.results[0]
        assertEquals(11332, ffOrigins.gameId)
        assertEquals("Final Fantasy Origins", ffOrigins.title)
        assertEquals("PlayStation", ffOrigins.consoleName)
        assertEquals(119, ffOrigins.maxPossible)
        assertEquals(38, ffOrigins.numAwarded)
        assertEquals(31, ffOrigins.progressPercentage)
        assertEquals("https://media.retroachievements.org/Images/060249.png", ffOrigins.iconUrl)

        val sonic = page.results[1]
        assertEquals(1448, sonic.gameId)
        assertEquals("Sonic the Hedgehog", sonic.title)
        assertEquals("Mega Drive", sonic.consoleName)
        assertEquals(32, sonic.maxPossible)
        assertEquals(16, sonic.numAwarded)
        assertEquals(50, sonic.progressPercentage)
        assertEquals("https://media.retroachievements.org/Images/001448.png", sonic.iconUrl)
    }

    @Test
    fun `parses sample want to play list response`() {
        val page = gson.fromJson(readResource("sample_want_to_play.json"), WantToPlayPage::class.java)

        assertEquals(2, page.count)
        assertEquals(2, page.total)
        val first = page.results[0]
        assertEquals(1001, first.gameId)
        assertEquals("Sample Quest Adventure", first.title)
        assertEquals("Sample Console", first.consoleName)
        assertEquals(3, first.consoleId)
        assertEquals(400, first.pointsTotal)
        assertEquals(42, first.achievementsPublished)

        val entry = first.toWishlistEntry()
        assertEquals(1001, entry.gameId)
        assertEquals("Sample Quest Adventure", entry.title)
        assertEquals("Sample Console", entry.consoleName)
        assertEquals("https://media.retroachievements.org/Images/000001.png", entry.iconUrl)
    }
}
