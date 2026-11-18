package com.example.retrocompanion.data.local

import com.example.retrocompanion.data.model.WishlistEntry
import kotlinx.coroutines.flow.Flow

/**
 * Where the local "want to play" wishlist lives. The RetroAchievements Web API's Want to Play
 * list is read-only, so this list is stored on-device only. An interface so ViewModels can be
 * tested with a fake instead of a real DataStore.
 */
interface WishlistSource {
    /** Wishlisted games, most recently added first. */
    val wishlistFlow: Flow<List<WishlistEntry>>

    /** Adds [entry] if its game isn't wishlisted, otherwise removes that game. */
    suspend fun toggle(entry: WishlistEntry)
}
