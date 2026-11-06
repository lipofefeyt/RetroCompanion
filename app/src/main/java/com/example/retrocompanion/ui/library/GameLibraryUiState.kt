package com.example.retrocompanion.ui.library

import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState

data class GameLibraryUiState(
    val credentialsStatus: CredentialsStatus = CredentialsStatus.Resolving,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val favoriteGameIds: Set<Int> = emptySet(),
    val games: SectionState<List<GameProgressSummary>> = SectionState.Loading
) {
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
