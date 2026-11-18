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
    val byConsole: List<ConsoleStats>,
    val closestToMastery: List<GameProgressSummary> = emptyList()
) {
    companion object {
        /** How many games the "Closest to Mastery" list shows. */
        const val CLOSEST_TO_MASTERY_LIMIT = 5

        /**
         * The started-but-not-mastered games nearest to 100%, best first. Games with no
         * achievements, no unlocks yet, or already mastered are left out. Ties on completion
         * are broken by fewest achievements remaining, then by title, so the order is stable.
         */
        fun closestToMastery(
            games: List<GameProgressSummary>,
            limit: Int = CLOSEST_TO_MASTERY_LIMIT
        ): List<GameProgressSummary> = games
            .filter { it.maxPossible > 0 && it.numAwarded > 0 && it.numAwarded < it.maxPossible }
            .sortedWith(
                compareByDescending<GameProgressSummary> { it.numAwarded.toDouble() / it.maxPossible }
                    .thenBy { it.maxPossible - it.numAwarded }
                    .thenBy { it.title.lowercase() }
            )
            .take(limit)

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
                byConsole = byConsole,
                closestToMastery = closestToMastery(games)
            )
        }
    }
}
