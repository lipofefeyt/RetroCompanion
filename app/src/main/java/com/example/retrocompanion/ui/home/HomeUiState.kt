package com.example.retrocompanion.ui.home

import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.RecentUnlock

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object MissingCredentials : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Success(
        val username: String,
        val recentlyPlayed: List<RecentlyPlayedGame>,
        val recentUnlocks: List<RecentUnlock>
    ) : HomeUiState
}
