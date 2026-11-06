package com.example.retrocompanion.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.retrocompanion.data.model.RecentUnlock
import com.example.retrocompanion.data.model.RecentlyPlayedGame
import com.example.retrocompanion.data.model.UserProfileSummary
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState
import com.example.retrocompanion.ui.components.EmptySectionCard
import com.example.retrocompanion.ui.components.LoadingScreen
import com.example.retrocompanion.ui.components.MissingCredentialsScreen
import com.example.retrocompanion.ui.components.NetworkImage
import com.example.retrocompanion.ui.components.RecentUnlockCard
import com.example.retrocompanion.ui.components.RecentlyPlayedCard
import com.example.retrocompanion.ui.components.SectionErrorCard
import com.example.retrocompanion.ui.components.SectionLoadingRow
import com.example.retrocompanion.ui.components.StaleDataBanner
import com.example.retrocompanion.ui.components.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RetroCompanion",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
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
                CredentialsStatus.Resolving -> {
                    LoadingScreen(message = "Loading…")
                }

                CredentialsStatus.Missing -> {
                    MissingCredentialsScreen(onGoToSettings = onNavigateToSettings)
                }

                CredentialsStatus.Present -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            userProfileSection(
                                state = uiState.userProfile,
                                defaultUsername = uiState.username,
                                onRetry = { viewModel.retryUserProfile() },
                                onGoToSettings = onNavigateToSettings
                            )

                            item {
                                Text(
                                    text = "Recently Played Games",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            recentlyPlayedSection(
                                state = uiState.recentlyPlayed,
                                onRetry = { viewModel.retryRecentlyPlayed() },
                                onGoToSettings = onNavigateToSettings,
                                onGameClick = onGameClick
                            )

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            item {
                                Column {
                                    Text(
                                        text = "Unlocked in Last 7 Days",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Lookback range: 7 days (10,080 minutes)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }

                            recentUnlocksSection(
                                state = uiState.recentUnlocks,
                                filteredUnlocks = uiState.filteredRecentUnlocks,
                                searchQuery = uiState.unlocksSearchQuery,
                                onSearchQueryChanged = { viewModel.onUnlocksSearchQueryChanged(it) },
                                onRetry = { viewModel.retryRecentUnlocks() },
                                onGoToSettings = onNavigateToSettings
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.recentlyPlayedSection(
    state: SectionState<List<RecentlyPlayedGame>>,
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
            if (state.isStale && state.cachedAtMillis != null) {
                item { StaleDataBanner(relativeTime = formatRelativeTime(state.cachedAtMillis)) }
            }
            if (state.data.isEmpty()) {
                item {
                    EmptySectionCard(
                        title = "No Recently Played Games",
                        subtitle = "Play a game on RetroAchievements to see your game progress here."
                    )
                }
            } else {
                items(items = state.data, key = { it.gameId }) { game ->
                    RecentlyPlayedCard(game = game, onClick = { onGameClick(game.gameId) })
                }
            }
        }
    }
}

private fun LazyListScope.recentUnlocksSection(
    state: SectionState<List<RecentUnlock>>,
    filteredUnlocks: List<RecentUnlock>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onRetry: () -> Unit,
    onGoToSettings: () -> Unit
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
            if (state.isStale && state.cachedAtMillis != null) {
                item { StaleDataBanner(relativeTime = formatRelativeTime(state.cachedAtMillis)) }
            }
            if (state.data.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        label = { Text("Search unlocks") },
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                }
            }
            if (filteredUnlocks.isEmpty()) {
                item {
                    EmptySectionCard(
                        title = if (searchQuery.isBlank()) "No Unlocks in the Last 7 Days" else "No Matches",
                        subtitle = if (searchQuery.isBlank()) {
                            "You haven't unlocked any achievements in the past 7 days. Time for a gaming session!"
                        } else {
                            "No unlocks match \"$searchQuery\"."
                        }
                    )
                }
            } else {
                items(items = filteredUnlocks, key = { "${it.achievementId}_${it.date}" }) { unlock ->
                    RecentUnlockCard(unlock = unlock)
                }
            }
        }
    }
}

private fun LazyListScope.userProfileSection(
    state: SectionState<UserProfileSummary>,
    defaultUsername: String,
    onRetry: () -> Unit,
    onGoToSettings: () -> Unit
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
        is SectionState.Success -> item {
            val profile = state.data
            val displayName = profile.user.ifBlank { defaultUsername.ifBlank { "Gamer" } }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (state.isStale && state.cachedAtMillis != null) {
                        StaleDataBanner(relativeTime = formatRelativeTime(state.cachedAtMillis))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NetworkImage(
                            url = profile.avatarUrl,
                            contentDescription = displayName,
                            placeholderGlyph = "👤",
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (!profile.motto.isNullOrBlank()) {
                                Text(
                                    text = "\"${profile.motto}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            profile.rank?.let { rank ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "Rank #$rank",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Points", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "🏆 ${profile.totalPoints}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).padding(start = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "RetroPoints", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "🎯 ${profile.totalTruePoints}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
