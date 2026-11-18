package com.example.retrocompanion.ui.library

import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.WishlistEntry
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState

/** Progress of importing the site's Want to Play list into the local wishlist. */
sealed interface WishlistImportState {
    data object Idle : WishlistImportState
    data object Running : WishlistImportState

    /** [added] games were new; [alreadyPresent] were already on the wishlist. */
    data class Done(val added: Int, val alreadyPresent: Int) : WishlistImportState

    data class Failed(val message: String) : WishlistImportState
}

data class GameLibraryUiState(
    val credentialsStatus: CredentialsStatus = CredentialsStatus.Resolving,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val favoriteGameIds: Set<Int> = emptySet(),
    val wishlistEntries: List<WishlistEntry> = emptyList(),
    val showWishlistOnly: Boolean = false,
    val wishlistImport: WishlistImportState = WishlistImportState.Idle,
    val games: SectionState<List<GameProgressSummary>> = SectionState.Loading
) {
    val wishlistGameIds: Set<Int>
        get() = wishlistEntries.mapTo(HashSet()) { it.gameId }

    /**
     * Wishlisted games filtered by [searchQuery], each joined with the user's progress when the
     * game is in the loaded library. Built from the locally stored entries, so it also includes
     * games the user has never played and works before (or without) the library loading.
     */
    val wishlistItems: List<WishlistItem>
        get() {
            val progressById = (games as? SectionState.Success)?.data?.associateBy { it.gameId }.orEmpty()
            return wishlistEntries
                .filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) }
                .map { WishlistItem(it, progressById[it.gameId]) }
        }

    /** Pinned favorite games loaded in the user's library. */
    val favoriteGames: List<GameProgressSummary>
        get() {
            val success = games as? SectionState.Success ?: return emptyList()
            if (favoriteGameIds.isEmpty()) return emptyList()
            return success.data.filter { favoriteGameIds.contains(it.gameId) }
        }

    /** The loaded library (if any), filtered by [searchQuery] client-side — no server-side search exists. */
    val filteredGames: List<GameProgressSummary>
        get() {
            val success = games as? SectionState.Success ?: return emptyList()
            if (searchQuery.isBlank()) return success.data
            return success.data.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
}

/** A wishlist entry plus the user's progress in it, if the game is in their library. */
data class WishlistItem(
    val entry: WishlistEntry,
    val progress: GameProgressSummary?
)
