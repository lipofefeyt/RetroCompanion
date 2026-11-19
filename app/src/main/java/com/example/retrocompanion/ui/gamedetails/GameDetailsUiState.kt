package com.example.retrocompanion.ui.gamedetails

import com.example.retrocompanion.data.model.GameAchievement
import com.example.retrocompanion.data.model.GameInfo

enum class AchievementFilter(val label: String) {
    ALL("All"),
    UNLOCKED("Unlocked"),
    LOCKED("Locked"),
    HARDCORE("Hardcore")
}

/** How the achievement list is ordered. [DEFAULT] is the game's own display order. */
enum class AchievementSort(val label: String) {
    DEFAULT("Default"),
    DATE("Date unlocked"),
    POINTS("Points"),
    RARITY("Rarity"),
    TITLE("Title")
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
        val isWishlisted: Boolean = false,
        val sort: AchievementSort = AchievementSort.DEFAULT
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
                return sorted(filteredBySearch)
            }

        /** [list] ordered by [sort]; ties keep the game's display order (the sorts are stable). */
        private fun sorted(list: List<GameAchievement>): List<GameAchievement> = when (sort) {
            AchievementSort.DEFAULT -> list
            // Newest unlock first, then everything still locked in display order. The API's
            // "yyyy-MM-dd HH:mm:ss" dates sort correctly as plain strings.
            AchievementSort.DATE -> list.sortedWith(
                compareByDescending<GameAchievement> { it.unlockDate != null }
                    .thenByDescending { it.unlockDate.orEmpty() }
            )
            AchievementSort.POINTS -> list.sortedByDescending { it.points }
            // Rarest (fewest players have it) first.
            AchievementSort.RARITY -> list.sortedBy { it.numAwarded }
            AchievementSort.TITLE -> list.sortedBy { it.title.lowercase() }
        }
    }
}
