package com.example.retrocompanion.ui.home

import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.RecentUnlock

/** Whether we've determined yet whether valid credentials are stored. */
enum class CredentialsStatus {
    /** Still reading from DataStore; nothing to show yet. */
    Resolving,
    Missing,
    Present
}

/** Independent loading/success/error state for one Home section. "Empty" is Success(emptyList()). */
sealed interface SectionState<out T> {
    data object Loading : SectionState<Nothing>
    data class Success<T>(val data: T) : SectionState<T>
    data class Error(val message: String, val isInvalidApiKey: Boolean) : SectionState<Nothing>
}

data class HomeUiState(
    val credentialsStatus: CredentialsStatus = CredentialsStatus.Resolving,
    val username: String = "",
    val isRefreshing: Boolean = false,
    val recentlyPlayed: SectionState<List<RecentlyPlayedGame>> = SectionState.Loading,
    val recentUnlocks: SectionState<List<RecentUnlock>> = SectionState.Loading
)
