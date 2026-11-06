package com.example.retrocompanion.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameInfoTest {

    @Test
    fun `rarityPercent is null when the player count is unknown`() {
        val gameInfo = GameInfo(numDistinctPlayersCasual = 0)
        val achievement = GameAchievement(id = 1, numAwarded = 10)

        assertNull(gameInfo.rarityPercent(achievement))
    }

    @Test
    fun `rarityPercent computes the earned percentage from awarded count and total players`() {
        val gameInfo = GameInfo(numDistinctPlayersCasual = 200)
        val achievement = GameAchievement(id = 1, numAwarded = 50)

        assertEquals(25f, gameInfo.rarityPercent(achievement))
    }
}
