package com.example.retrocompanion.data.remote

import com.example.retrocompanion.data.model.AchievementOfTheWeek
import com.example.retrocompanion.data.model.AchievementOfTheWeekResponse
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

    private fun aotw(): AchievementOfTheWeekResponse =
        gson.fromJson(readResource("sample_achievement_of_the_week.json"), AchievementOfTheWeekResponse::class.java)

    @Test
    fun `parses sample achievement of the week response into a compact summary`() {
        val summary = aotw().toSummary("sample_user")!!

        assertEquals(9001, summary.achievementId)
        assertEquals("Sample Weekly Challenge", summary.title)
        assertEquals("Defeat the sample boss without taking damage", summary.description)
        assertEquals(25, summary.points)
        assertEquals(4242, summary.gameId)
        assertEquals("Sample Quest Adventure", summary.gameTitle)
        assertEquals("Sample Console", summary.consoleName)
        assertEquals("2026-03-09T00:00:00.000000Z", summary.startAt)
        assertEquals(400, summary.totalPlayers)
        assertEquals(100, summary.unlocksCount)
        assertEquals(80, summary.unlocksHardcoreCount)
        assertEquals("", summary.badgeUrl)
    }

    @Test
    fun `achievement of the week notices the signed-in user in the unlock list, ignoring case`() {
        assertEquals(true, aotw().toSummary("SAMPLE_USER")!!.unlockedByUser)
        assertEquals(false, aotw().toSummary("someone_else")!!.unlockedByUser)
        assertEquals(false, aotw().toSummary("")!!.unlockedByUser)
    }

    @Test
    fun `achievement of the week without an unlock list or achievement is handled`() {
        val noUnlocks = AchievementOfTheWeekResponse(
            achievement = aotw().achievement,
            game = aotw().game
        )
        assertEquals(false, noUnlocks.toSummary("sample_user")!!.unlockedByUser)
        assertEquals(null, AchievementOfTheWeekResponse().toSummary("sample_user"))
    }

    @Test
    fun `achievement of the week derived values guard against missing data`() {
        val summary = aotw().toSummary("sample_user")!!

        assertEquals(25f, summary.unlockedPercent!!, 0.001f)
        assertEquals(null, summary.copy(totalPlayers = 0).unlockedPercent)
        assertEquals("Mar 9, 2026", summary.featuredSinceLabel(java.util.Locale.US))
        assertEquals(null, summary.copy(startAt = null).featuredSinceLabel(java.util.Locale.US))
        assertEquals(null, summary.copy(startAt = "garbage").featuredSinceLabel(java.util.Locale.US))
        assertEquals(
            "https://media.retroachievements.org/Badge/12345.png",
            summary.copy(badgeName = "12345").badgeUrl
        )
    }

    @Test
    fun `the compact summary no longer carries the unlock list`() {
        val json = gson.toJson(aotw().toSummary("sample_user"))
        assertEquals(false, json.contains("Other_Player"))
        assertEquals(false, json.contains("Unlocks\""))
        val roundTrip = gson.fromJson(json, AchievementOfTheWeek::class.java)
        assertEquals(aotw().toSummary("sample_user"), roundTrip)
    }
}
