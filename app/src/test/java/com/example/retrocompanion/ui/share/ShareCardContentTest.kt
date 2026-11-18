package com.example.retrocompanion.ui.share

import com.example.retrocompanion.data.model.GameAchievement
import com.example.retrocompanion.data.model.GameInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ShareCardContentTest {

    private val locale = Locale.US

    private fun achievement(
        id: Int,
        points: Int = 10,
        earned: String? = null,
        hardcore: String? = null,
        numAwarded: Int = 0
    ) = GameAchievement(
        id = id,
        title = "Sample Achievement $id",
        description = "Do a sample thing",
        points = points,
        badgeName = "00$id",
        dateEarned = earned,
        dateEarnedHardcore = hardcore,
        numAwarded = numAwarded
    )

    private fun game(vararg achievements: GameAchievement, players: Int = 1000) = GameInfo(
        gameId = 1,
        title = "Sample Game",
        consoleName = "Sample Console",
        imageIcon = "/Images/000001.png",
        numDistinctPlayersCasual = players,
        achievements = achievements.associateBy { it.id.toString() }
    )

    @Test
    fun `rarity threshold is exclusive and a missing rarity is never rare`() {
        assertTrue(ShareCardContent.isRare(0.5f))
        assertTrue(ShareCardContent.isRare(9.99f))
        assertFalse(ShareCardContent.isRare(ShareCardContent.RARE_THRESHOLD_PERCENT))
        assertFalse(ShareCardContent.isRare(55f))
        assertFalse(ShareCardContent.isRare(null))
    }

    @Test
    fun `achievement card has title, game line, description, points, rarity and date`() {
        val earned = achievement(1, points = 25, earned = "2026-03-03 14:22:10", hardcore = "2026-03-03 14:22:10", numAwarded = 23)
        val card = ShareCardContent.forAchievement(game(earned), earned, locale)!!

        assertEquals(ShareCardKind.RARE_ACHIEVEMENT, card.kind)
        assertEquals("Sample Achievement 1", card.title)
        assertEquals("Sample Game · Sample Console", card.subtitle)
        assertEquals("Do a sample thing", card.description)
        assertEquals("25 pts · 2.3% of players · Hardcore Mar 3, 2026", card.stats)
        assertEquals("https://media.retroachievements.org/Badge/001.png", card.iconUrl)
    }

    @Test
    fun `softcore achievement says unlocked and unknown rarity is left out`() {
        val earned = achievement(1, earned = "2026-03-03 14:22:10")
        val card = ShareCardContent.forAchievement(game(earned, players = 0), earned, locale)!!

        assertEquals("10 pts · Unlocked Mar 3, 2026", card.stats)
    }

    @Test
    fun `a common or unknown-rarity achievement still gets a card, but not the rare label`() {
        val common = achievement(1, earned = "2026-03-03 14:22:10", numAwarded = 500)
        val commonCard = ShareCardContent.forAchievement(game(common), common, locale)!!
        assertEquals(ShareCardKind.ACHIEVEMENT, commonCard.kind)
        assertEquals("10 pts · 50.0% of players · Unlocked Mar 3, 2026", commonCard.stats)

        val unknown = achievement(2, earned = "2026-03-03 14:22:10")
        val unknownCard = ShareCardContent.forAchievement(game(unknown, players = 0), unknown, locale)!!
        assertEquals(ShareCardKind.ACHIEVEMENT, unknownCard.kind)
    }

    @Test
    fun `an achievement that is not earned has no card`() {
        val locked = achievement(1)
        assertNull(ShareCardContent.forAchievement(game(locked), locked, locale))
    }

    @Test
    fun `a malformed date is dropped instead of shown`() {
        val earned = achievement(1, earned = "garbage")
        val card = ShareCardContent.forAchievement(game(earned), earned, locale)!!
        assertFalse(card.stats.contains("garbage"))
    }

    @Test
    fun `fully hardcore game gives a mastery card using the latest hardcore date`() {
        val a = achievement(1, points = 5, earned = "2026-01-01 10:00:00", hardcore = "2026-01-01 10:00:00")
        val b = achievement(2, points = 15, earned = "2026-02-10 10:00:00", hardcore = "2026-02-10 10:00:00")
        val card = ShareCardContent.forCompletion(game(a, b), locale)!!

        assertEquals(ShareCardKind.MASTERY, card.kind)
        assertEquals("Sample Game", card.title)
        assertEquals("Sample Console", card.subtitle)
        assertNull(card.description)
        assertEquals("2 achievements · 20 pts · Mastered Feb 10, 2026", card.stats)
        assertEquals("https://media.retroachievements.org/Images/000001.png", card.iconUrl)
    }

    @Test
    fun `all earned but not all in hardcore is completed rather than mastered`() {
        val a = achievement(1, earned = "2026-01-01 10:00:00", hardcore = "2026-01-01 10:00:00")
        val b = achievement(2, earned = "2026-02-10 10:00:00")
        val card = ShareCardContent.forCompletion(game(a, b), locale)!!

        assertEquals(ShareCardKind.COMPLETION, card.kind)
        assertTrue(card.stats.endsWith("Completed Feb 10, 2026"))
    }

    @Test
    fun `unfinished or empty games have no completion card`() {
        val done = achievement(1, earned = "2026-01-01 10:00:00")
        val notDone = achievement(2)
        assertNull(ShareCardContent.forCompletion(game(done, notDone), locale))
        assertNull(ShareCardContent.forCompletion(game(), locale))
        assertNull(ShareCardContent.forCompletion(GameInfo(gameId = 1, title = "No Achievements"), locale))
    }

    @Test
    fun `cards never contain account details`() {
        val earned = achievement(1, earned = "2026-03-03 14:22:10")
        val card = ShareCardContent.forAchievement(game(earned), earned, locale)!!
        val text = listOfNotNull(card.title, card.subtitle, card.description, card.stats, card.iconUrl).joinToString(" ")
        assertFalse(text.contains("apikey", ignoreCase = true))
        assertFalse(text.contains("y="))
    }
}
