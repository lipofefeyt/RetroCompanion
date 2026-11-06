package com.example.retrocompanion.ui.profile

import com.example.retrocompanion.data.model.UserProfileSummary
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState

data class ProfileUiState(
    val credentialsStatus: CredentialsStatus = CredentialsStatus.Resolving,
    val isRefreshing: Boolean = false,
    val profile: SectionState<UserProfileSummary> = SectionState.Loading
)
