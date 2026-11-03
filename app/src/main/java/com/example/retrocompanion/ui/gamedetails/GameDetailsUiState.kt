package com.example.retrocompanion.ui.gamedetails

import com.example.retrocompanion.data.model.GameInfo

sealed interface GameDetailsUiState {
    data object Loading : GameDetailsUiState
    data class Error(val message: String, val isInvalidApiKey: Boolean) : GameDetailsUiState
    data class Success(
        val gameInfo: GameInfo,
        /** True when [gameInfo] came from the local cache rather than a successful network fetch. */
        val isStale: Boolean = false,
        val cachedAtMillis: Long? = null
    ) : GameDetailsUiState
}
