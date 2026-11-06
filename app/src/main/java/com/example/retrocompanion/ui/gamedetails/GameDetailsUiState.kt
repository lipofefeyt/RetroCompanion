package com.example.retrocompanion.ui.gamedetails

import com.example.retrocompanion.data.model.GameAchievement
import com.example.retrocompanion.data.model.GameInfo

enum class AchievementFilter(val label: String) {
    ALL("All"),
    UNLOCKED("Unlocked"),
    LOCKED("Locked"),
    HARDCORE("Hardcore")
}

sealed interface GameDetailsUiState {
    data object Loading : GameDetailsUiState
    data class Error(val message: String, val isInvalidApiKey: Boolean) : GameDetailsUiState
    data class Success(
        val gameInfo: GameInfo,
        /** True when [gameInfo] came from the local cache rather than a successful network fetch. */
        val isStale: Boolean = false,
        val cachedAtMillis: Long? = null,
        val searchQuery: String = "",
        val selectedFilter: AchievementFilter = AchievementFilter.ALL,
        val isFavorite: Boolean = false,
        val sortByRarity: Boolean = false
    ) : GameDetailsUiState {
        val filteredAchievements: List<GameAchievement>
            get() {
                val list = gameInfo.achievementsSortedForDisplay
                val filteredByStatus = when (selectedFilter) {
                    AchievementFilter.ALL -> list
                    AchievementFilter.UNLOCKED -> list.filter { it.isEarned }
                    AchievementFilter.LOCKED -> list.filter { !it.isEarned }
                    AchievementFilter.HARDCORE -> list.filter { it.isEarnedHardcore }
                }
                val filteredBySearch = if (searchQuery.isBlank()) {
                    filteredByStatus
                } else {
                    val query = searchQuery.trim()
                    filteredByStatus.filter {
                        it.title.contains(query, ignoreCase = true) ||
                            (it.description?.contains(query, ignoreCase = true) == true)
                    }
                }
                return if (sortByRarity) {
                    filteredBySearch.sortedBy { it.numAwarded }
                } else {
                    filteredBySearch
                }
            }
    }
}
