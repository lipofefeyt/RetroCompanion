package com.example.retrocompanion.ui.stats

import com.example.retrocompanion.data.model.UserProfileSummary
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState

data class StatsUiState(
    val credentialsStatus: CredentialsStatus = CredentialsStatus.Resolving,
    val isRefreshing: Boolean = false,
    val profile: SectionState<UserProfileSummary> = SectionState.Loading,
    val libraryStats: SectionState<LibraryStats> = SectionState.Loading,
    val activity: SectionState<UnlockActivity> = SectionState.Loading
)
