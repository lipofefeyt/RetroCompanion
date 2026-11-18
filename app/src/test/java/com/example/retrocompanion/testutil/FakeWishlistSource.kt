package com.example.retrocompanion.testutil

import com.example.retrocompanion.data.local.WishlistSource
import com.example.retrocompanion.data.local.mergeWishlist
import com.example.retrocompanion.data.model.WishlistEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeWishlistSource(initial: List<WishlistEntry> = emptyList()) : WishlistSource {
    private val state = MutableStateFlow(initial)
    override val wishlistFlow: Flow<List<WishlistEntry>> = state

    override suspend fun addAll(entries: List<WishlistEntry>): Int {
        val (merged, added) = mergeWishlist(state.value, entries)
        state.value = merged
        return added
    }

    override suspend fun toggle(entry: WishlistEntry) {
        val current = state.value
        state.value = if (current.any { it.gameId == entry.gameId }) {
            current.filterNot { it.gameId == entry.gameId }
        } else {
            listOf(entry) + current
        }
    }
}
