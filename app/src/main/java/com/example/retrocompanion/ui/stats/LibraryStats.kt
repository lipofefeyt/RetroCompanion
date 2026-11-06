package com.example.retrocompanion.ui.stats

import com.example.retrocompanion.data.model.GameProgressSummary

data class ConsoleStats(
    val consoleName: String,
    val gameCount: Int,
    val avgCompletionPercent: Int
)

data class LibraryStats(
    val totalGames: Int,
    val totalAchievementsEarned: Int,
    val totalAchievementsPossible: Int,
    val overallCompletionPercent: Int,
    val hardcoreRatioPercent: Int,
    val masteredGamesCount: Int,
    val nearCompleteGamesCount: Int,
    val byConsole: List<ConsoleStats>
) {
    companion object {
        /** Pure aggregation over the full library, so it's testable without a ViewModel. */
        fun from(games: List<GameProgressSummary>): LibraryStats {
            val totalEarned = games.sumOf { it.numAwarded }
            val totalPossible = games.sumOf { it.maxPossible }
            val totalHardcore = games.sumOf { it.numAwardedHardcore }
            val mastered = games.count { it.maxPossible > 0 && it.numAwarded == it.maxPossible }
            val nearComplete = games.count { it.progressPercentage in 90..99 }

            val byConsole = games
                .groupBy { it.consoleName?.takeIf { name -> name.isNotBlank() } ?: "Unknown" }
                .map { (consoleName, consoleGames) ->
                    ConsoleStats(
                        consoleName = consoleName,
                        gameCount = consoleGames.size,
                        avgCompletionPercent = consoleGames.map { it.progressPercentage }.average().toInt()
                    )
                }
                .sortedByDescending { it.gameCount }

            return LibraryStats(
                totalGames = games.size,
                totalAchievementsEarned = totalEarned,
                totalAchievementsPossible = totalPossible,
                overallCompletionPercent = if (totalPossible > 0) (totalEarned * 100 / totalPossible) else 0,
                hardcoreRatioPercent = if (totalEarned > 0) (totalHardcore * 100 / totalEarned) else 0,
                masteredGamesCount = mastered,
                nearCompleteGamesCount = nearComplete,
                byConsole = byConsole
            )
        }
    }
}
