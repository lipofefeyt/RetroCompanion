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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.retrocompanion.ui.components.EmptySectionCard
import com.example.retrocompanion.ui.components.ErrorScreen
import com.example.retrocompanion.ui.components.LoadingScreen
import com.example.retrocompanion.ui.components.MissingCredentialsScreen
import com.example.retrocompanion.ui.components.RecentUnlockCard
import com.example.retrocompanion.ui.components.RecentlyPlayedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
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
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
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
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    LoadingScreen(message = "Fetching recently played & last 7 days unlocks...")
                }

                is HomeUiState.MissingCredentials -> {
                    MissingCredentialsScreen(onGoToSettings = onNavigateToSettings)
                }

                is HomeUiState.Error -> {
                    ErrorScreen(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        onOpenSettings = onNavigateToSettings
                    )
                }

                is HomeUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // User Header
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Hello, ${state.username}!",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Here is your RetroAchievements activity",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Section 1: Recently Played Games
                        item {
                            Text(
                                text = "Recently Played Games",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        if (state.recentlyPlayed.isEmpty()) {
                            item {
                                EmptySectionCard(
                                    title = "No Recently Played Games",
                                    subtitle = "Play a game on RetroAchievements to see your game progress here."
                                )
                            }
                        } else {
                            items(
                                items = state.recentlyPlayed,
                                key = { it.gameId }
                            ) { game ->
                                RecentlyPlayedCard(game = game)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Section 2: Recent Unlocks (Last 7 Days)
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

                        if (state.recentUnlocks.isEmpty()) {
                            item {
                                EmptySectionCard(
                                    title = "No Unlocks in the Last 7 Days",
                                    subtitle = "You haven't unlocked any achievements in the past 7 days. Time for a gaming session!"
                                )
                            }
                        } else {
                            items(
                                items = state.recentUnlocks,
                                key = { "${it.achievementId}_${it.date}" }
                            ) { unlock ->
                                RecentUnlockCard(unlock = unlock)
                            }
                        }
                    }
                }
            }
        }
    }
}
