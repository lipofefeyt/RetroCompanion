package com.example.retrocompanion.ui.home

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
    val recentUnlocks: SectionState<List<RecentUnlock>> = SectionState.Loading
)
