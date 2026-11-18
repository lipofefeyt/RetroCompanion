package com.example.retrocompanion.ui.library

import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.WishlistEntry
import com.example.retrocompanion.ui.common.SectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLibraryUiStateTest {

    private val played = GameProgressSummary(gameId = 1, title = "Sample Played Game", maxPossible = 10, numAwarded = 4)
    private val wishPlayed = WishlistEntry(gameId = 1, title = "Sample Played Game")
    private val wishUnplayed = WishlistEntry(gameId = 99, title = "Sample Unplayed Game", consoleName = "Sample Console")

    @Test
    fun `wishlist items include games that are not in the loaded library`() {
        val state = GameLibraryUiState(
            games = SectionState.Success(listOf(played)),
            wishlistEntries = listOf(wishPlayed, wishUnplayed)
        )

        val items = state.wishlistItems
        assertEquals(listOf(1, 99), items.map { it.entry.gameId })
        assertEquals(played, items[0].progress)
        assertNull(items[1].progress)
    }

    @Test
    fun `wishlist items are available while the library is still loading`() {
        val state = GameLibraryUiState(
            games = SectionState.Loading,
            wishlistEntries = listOf(wishUnplayed)
        )

        assertEquals(listOf(99), state.wishlistItems.map { it.entry.gameId })
        assertNull(state.wishlistItems.single().progress)
    }

    @Test
    fun `search query filters wishlist items by title`() {
        val state = GameLibraryUiState(
            wishlistEntries = listOf(wishPlayed, wishUnplayed),
            searchQuery = "unplayed"
        )

        assertEquals(listOf(99), state.wishlistItems.map { it.entry.gameId })
    }

    @Test
    fun `wishlistGameIds reflects the stored entries`() {
        val state = GameLibraryUiState(wishlistEntries = listOf(wishPlayed, wishUnplayed))
        assertEquals(setOf(1, 99), state.wishlistGameIds)
        assertTrue(GameLibraryUiState().wishlistGameIds.isEmpty())
    }
}
