package com.example.retrocompanion.ui.home

import com.example.retrocompanion.data.model.AchievementOfTheWeek
import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.UserProfileSummary
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState

data class HomeUiState(
    val credentialsStatus: CredentialsStatus = CredentialsStatus.Resolving,
    val username: String = "",
    val isRefreshing: Boolean = false,
    val userProfile: SectionState<UserProfileSummary> = SectionState.Loading,
    val recentlyPlayed: SectionState<List<RecentlyPlayedGame>> = SectionState.Loading,
    val recentUnlocks: SectionState<List<RecentUnlock>> = SectionState.Loading,
    val achievementOfTheWeek: SectionState<AchievementOfTheWeek> = SectionState.Loading,
    val unlocksSearchQuery: String = ""
) {
    /** [recentUnlocks]'s loaded list (if any), filtered by [unlocksSearchQuery] client-side. */
    val filteredRecentUnlocks: List<RecentUnlock>
        get() {
            val success = recentUnlocks as? SectionState.Success ?: return emptyList()
            if (unlocksSearchQuery.isBlank()) return success.data
            return success.data.filter { it.title.contains(unlocksSearchQuery, ignoreCase = true) }
        }
}
