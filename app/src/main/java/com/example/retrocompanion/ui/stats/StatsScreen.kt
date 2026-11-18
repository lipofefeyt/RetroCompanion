package com.example.retrocompanion.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.UserProfileSummary
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState
import com.example.retrocompanion.ui.components.LoadingScreen
import com.example.retrocompanion.ui.components.MissingCredentialsScreen
import com.example.retrocompanion.ui.components.NetworkImage
import com.example.retrocompanion.ui.components.RefreshCompletionHaptic
import com.example.retrocompanion.ui.components.SectionErrorCard
import com.example.retrocompanion.ui.components.StaleDataBanner
import com.example.retrocompanion.ui.components.StatTile
import com.example.retrocompanion.ui.components.formatRelativeTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateToSettings: () -> Unit,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    RefreshCompletionHaptic(isRefreshing = uiState.isRefreshing)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Stats", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (uiState.credentialsStatus) {
                CredentialsStatus.Resolving -> LoadingScreen(message = "Loading…")
                CredentialsStatus.Missing -> MissingCredentialsScreen(onGoToSettings = onNavigateToSettings)
                CredentialsStatus.Present -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pointsSection(state = uiState.profile)
                            activitySection(
                                state = uiState.activity,
                                onRetry = { viewModel.retry() },
                                onGoToSettings = onNavigateToSettings
                            )
                            libraryStatsSection(
                                state = uiState.libraryStats,
                                onRetry = { viewModel.retry() },
                                onGoToSettings = onNavigateToSettings,
                                onGameClick = onGameClick
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.pointsSection(
    state: SectionState<UserProfileSummary>
) {
    val success = state as? SectionState.Success ?: return
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatTile(label = "Points", value = success.data.totalPoints.toString())
                StatTile(label = "True Points", value = success.data.totalTruePoints.toString())
                success.data.rank?.let { rank -> StatTile(label = "Rank", value = "#$rank") }
            }
        }
    }
}

private fun LazyListScope.activitySection(
    state: SectionState<UnlockActivity>,
    onRetry: () -> Unit,
    onGoToSettings: () -> Unit
) {
    when (state) {
        is SectionState.Loading -> item { LoadingScreen(message = "Loading your unlock activity…") }
        is SectionState.Error -> item {
            SectionErrorCard(
                message = state.message,
                showGoToSettings = state.isInvalidApiKey,
                onRetry = onRetry,
                onGoToSettings = onGoToSettings
            )
        }

        is SectionState.Success -> {
            if (state.isStale && state.cachedAtMillis != null) {
                item { StaleDataBanner(relativeTime = formatRelativeTime(state.cachedAtMillis)) }
            }
            item { ActivityCard(activity = state.data) }
        }
    }
}

@Composable
private fun ActivityCard(activity: UnlockActivity, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Unlock Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatTile(label = "Current Streak", value = daysLabel(activity.currentStreak))
                StatTile(label = "Longest Streak", value = daysLabel(activity.longestStreak))
                StatTile(label = "Active Days", value = activity.activeDays.toString())
            }
            Spacer(modifier = Modifier.height(16.dp))
            UnlockHeatmap(weeks = activity.weeks)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${activity.totalUnlocks} unlocks in the last ${activity.windowDays / 7} weeks. " +
                    "Days follow your device time zone; longest streak is within this period.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun daysLabel(days: Int): String = if (days == 1) "1 day" else "$days days"

@Composable
private fun UnlockHeatmap(weeks: List<HeatmapWeek>, modifier: Modifier = Modifier) {
    val dateFormat = remember {
        SimpleDateFormat("MMM d", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }
    val primary = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        weeks.forEach { week ->
            val label = dateFormat.format(Date(week.startEpochDay * 86_400_000L))
            val description = when (week.totalUnlocks) {
                0 -> "Week of $label: no unlocks"
                1 -> "Week of $label: 1 unlock on 1 day"
                else -> "Week of $label: ${week.totalUnlocks} unlocks on ${week.activeDays} days"
            }
            // One TalkBack stop per week instead of seven unlabeled squares.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { contentDescription = description },
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                week.days.forEach { day ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clearAndSetSemantics { }
                            .background(
                                color = when {
                                    day == null -> Color.Transparent
                                    day.level == 0 -> empty
                                    else -> primary.copy(alpha = 0.25f + 0.2f * (day.level - 1))
                                },
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

private fun LazyListScope.libraryStatsSection(
    state: SectionState<LibraryStats>,
    onRetry: () -> Unit,
    onGoToSettings: () -> Unit,
    onGameClick: (Int) -> Unit
) {
    when (state) {
        is SectionState.Loading -> item { LoadingScreen(message = "Loading your library stats…") }
        is SectionState.Error -> item {
            SectionErrorCard(
                message = state.message,
                showGoToSettings = state.isInvalidApiKey,
                onRetry = onRetry,
                onGoToSettings = onGoToSettings
            )
        }

        is SectionState.Success -> {
            if (state.isStale && state.cachedAtMillis != null) {
                item { StaleDataBanner(relativeTime = formatRelativeTime(state.cachedAtMillis)) }
            }
            val stats = state.data
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatTile(label = "Games", value = stats.totalGames.toString())
                            StatTile(label = "Achievements", value = "${stats.totalAchievementsEarned} / ${stats.totalAchievementsPossible}")
                            StatTile(label = "Completion", value = "${stats.overallCompletionPercent}%")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatTile(label = "Hardcore Ratio", value = "${stats.hardcoreRatioPercent}%")
                            StatTile(label = "Mastered", value = stats.masteredGamesCount.toString())
                            StatTile(label = "Near Complete", value = stats.nearCompleteGamesCount.toString())
                        }
                    }
                }
            }
            if (stats.closestToMastery.isNotEmpty()) {
                item {
                    Text(
                        text = "Closest to Mastery",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(items = stats.closestToMastery, key = { "closest-${it.gameId}" }) { game ->
                    ClosestToMasteryRow(game = game, onClick = { onGameClick(game.gameId) })
                }
            }
            item {
                Text(
                    text = "By Console",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(items = stats.byConsole, key = { it.consoleName }) { console ->
                ConsoleStatsRow(console = console)
            }
        }
    }
}

@Composable
private fun ClosestToMasteryRow(
    game: GameProgressSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remaining = game.maxPossible - game.numAwarded
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NetworkImage(
                url = game.iconUrl,
                contentDescription = game.title,
                placeholderGlyph = "🎮",
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${game.progressPercentage}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = if (remaining == 1) "1 achievement left" else "$remaining achievements left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { game.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
private fun ConsoleStatsRow(console: ConsoleStats, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${console.consoleName} (${console.gameCount})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${console.avgCompletionPercent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { console.avgCompletionPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        }
    }
}
