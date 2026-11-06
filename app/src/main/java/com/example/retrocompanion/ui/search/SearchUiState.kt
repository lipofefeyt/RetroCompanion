package com.example.retrocompanion.ui.search

import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState

data class SearchUiState(
    val credentialsStatus: CredentialsStatus = CredentialsStatus.Resolving,
    val isRefreshing: Boolean = false,
    val query: String = "",
    val games: SectionState<List<GameProgressSummary>> = SectionState.Loading,
    val recentUnlocks: SectionState<List<RecentUnlock>> = SectionState.Loading
) {
    /** Empty until a query is typed — this is a search tool, not a browsable list. */
    val filteredGames: List<GameProgressSummary>
        get() {
            if (query.isBlank()) return emptyList()
            val success = games as? SectionState.Success ?: return emptyList()
            return success.data.filter { it.title.contains(query, ignoreCase = true) }
        }

    val filteredUnlocks: List<RecentUnlock>
        get() {
            if (query.isBlank()) return emptyList()
            val success = recentUnlocks as? SectionState.Success ?: return emptyList()
            return success.data.filter { it.title.contains(query, ignoreCase = true) }
        }
}
