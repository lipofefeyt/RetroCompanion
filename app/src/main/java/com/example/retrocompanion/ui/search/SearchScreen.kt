package com.example.retrocompanion.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.retrocompanion.data.model.GameProgressSummary
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.components.EmptySectionCard
import com.example.retrocompanion.ui.components.LoadingScreen
import com.example.retrocompanion.ui.components.MissingCredentialsScreen
import com.example.retrocompanion.ui.components.NetworkImage
import com.example.retrocompanion.ui.common.SectionState
import com.example.retrocompanion.ui.components.RecentUnlockCard
import com.example.retrocompanion.ui.components.RefreshCompletionHaptic
import com.example.retrocompanion.ui.components.SectionErrorCard
import com.example.retrocompanion.ui.components.SectionLoadingRow
import com.example.retrocompanion.ui.components.StaleDataBanner
import com.example.retrocompanion.ui.components.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToSettings: () -> Unit,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    RefreshCompletionHaptic(isRefreshing = uiState.isRefreshing)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Search", fontWeight = FontWeight.Bold) },
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = uiState.query,
                            onValueChange = { viewModel.onQueryChanged(it) },
                            label = { Text("Search games and recent achievements") },
                            singleLine = true,
                            trailingIcon = {
                                if (uiState.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                        PullToRefreshBox(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.refresh() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (uiState.query.isBlank()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    EmptySectionCard(
                                        title = "Search your library",
                                        subtitle = "Find a game or a recent achievement by name.",
                                        icon = "🔎"
                                    )
                                }
                            } else {
                                val gamesLoading = uiState.games is SectionState.Loading
                                val unlocksLoading = uiState.recentUnlocks is SectionState.Loading
                                val noMatches = !gamesLoading && !unlocksLoading &&
                                    uiState.games !is SectionState.Error && uiState.recentUnlocks !is SectionState.Error &&
                                    uiState.filteredGames.isEmpty() && uiState.filteredUnlocks.isEmpty()
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    gamesSection(
                                        state = uiState.games,
                                        filteredGames = uiState.filteredGames,
                                        onRetry = { viewModel.retry() },
                                        onGoToSettings = onNavigateToSettings,
                                        onGameClick = onGameClick
                                    )
                                    recentUnlocksSection(
                                        state = uiState.recentUnlocks,
                                        filteredUnlocks = uiState.filteredUnlocks,
                                        onRetry = { viewModel.retry() },
                                        onGoToSettings = onNavigateToSettings,
                                        onGameClick = onGameClick
                                    )
                                    if (noMatches) {
                                        item {
                                            EmptySectionCard(
                                                title = "No Matches",
                                                subtitle = "No games or recent achievements match \"${uiState.query}\".",
                                                icon = "🔍"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.gamesSection(
    state: SectionState<List<GameProgressSummary>>,
    filteredGames: List<GameProgressSummary>,
    onRetry: () -> Unit,
    onGoToSettings: () -> Unit,
    onGameClick: (Int) -> Unit
) {
    when (state) {
        is SectionState.Loading -> item { SectionLoadingRow() }
        is SectionState.Error -> item {
            SectionErrorCard(
                message = state.message,
                showGoToSettings = state.isInvalidApiKey,
                onRetry = onRetry,
                onGoToSettings = onGoToSettings
            )
        }
        is SectionState.Success -> {
            if (filteredGames.isEmpty()) return
            if (state.isStale && state.cachedAtMillis != null) {
                item { StaleDataBanner(relativeTime = formatRelativeTime(state.cachedAtMillis)) }
            }
            item {
                Text(
                    text = "Games",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(items = filteredGames, key = { "game-${it.gameId}" }) { game ->
                SearchGameRow(game = game, onClick = { onGameClick(game.gameId) })
            }
        }
    }
}

private fun LazyListScope.recentUnlocksSection(
    state: SectionState<List<RecentUnlock>>,
    filteredUnlocks: List<RecentUnlock>,
    onRetry: () -> Unit,
    onGoToSettings: () -> Unit,
    onGameClick: (Int) -> Unit
) {
    when (state) {
        is SectionState.Loading -> item { SectionLoadingRow() }
        is SectionState.Error -> item {
            SectionErrorCard(
                message = state.message,
                showGoToSettings = state.isInvalidApiKey,
                onRetry = onRetry,
                onGoToSettings = onGoToSettings
            )
        }
        is SectionState.Success -> {
            if (filteredUnlocks.isEmpty()) return
            if (state.isStale && state.cachedAtMillis != null) {
                item { StaleDataBanner(relativeTime = formatRelativeTime(state.cachedAtMillis)) }
            }
            item {
                Text(
                    text = "Recently Unlocked Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(items = filteredUnlocks, key = { "unlock-${it.achievementId}_${it.date}" }) { unlock ->
                val gameId = unlock.gameId
                if (gameId != null) {
                    Column(modifier = Modifier.clickable(onClick = { onGameClick(gameId) })) {
                        RecentUnlockCard(unlock = unlock)
                    }
                } else {
                    RecentUnlockCard(unlock = unlock)
                }
            }
        }
    }
}

@Composable
private fun SearchGameRow(
    game: GameProgressSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                game.consoleName?.let { console ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = console,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${game.progressPercentage}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
