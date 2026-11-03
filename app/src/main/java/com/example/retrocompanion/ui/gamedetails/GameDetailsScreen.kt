package com.example.retrocompanion.ui.gamedetails

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.retrocompanion.data.model.GameAchievement
import com.example.retrocompanion.ui.components.EmptySectionCard
import com.example.retrocompanion.ui.components.LoadingScreen
import com.example.retrocompanion.ui.components.NetworkImage
import com.example.retrocompanion.ui.components.SectionErrorCard
import com.example.retrocompanion.ui.components.StaleDataBanner
import com.example.retrocompanion.ui.components.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailsScreen(
    viewModel: GameDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (uiState as? GameDetailsUiState.Success)?.gameInfo?.title ?: "Game Details"
                    Text(text = title, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
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
            when (val state = uiState) {
                is GameDetailsUiState.Loading -> LoadingScreen(message = "Loading game details…")

                is GameDetailsUiState.Error -> Column(modifier = Modifier.padding(16.dp)) {
                    SectionErrorCard(
                        message = state.message,
                        showGoToSettings = state.isInvalidApiKey,
                        onRetry = { viewModel.retry() },
                        onGoToSettings = onNavigateToSettings
                    )
                }

                is GameDetailsUiState.Success -> {
                    val game = state.gameInfo
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.isStale && state.cachedAtMillis != null) {
                            item { StaleDataBanner(relativeTime = formatRelativeTime(state.cachedAtMillis)) }
                        }

                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NetworkImage(
                                    url = game.boxArtUrl,
                                    contentDescription = game.title,
                                    placeholderGlyph = "🎮",
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = game.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    game.consoleName?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${game.numAwardedToUser} / ${game.achievements?.size ?: 0} unlocked",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Achievements",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val achievements = game.achievementsSortedForDisplay
                        if (achievements.isEmpty()) {
                            item {
                                EmptySectionCard(
                                    title = "No Achievements",
                                    subtitle = "This game has no tracked achievements."
                                )
                            }
                        } else {
                            items(items = achievements, key = { it.id }) { achievement ->
                                AchievementRow(achievement)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(achievement: GameAchievement, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isEarned) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NetworkImage(
                url = achievement.badgeUrl,
                contentDescription = achievement.title,
                placeholderGlyph = "🏆",
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${achievement.title} (${achievement.points} pts)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (achievement.isEarned) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                achievement.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = when {
                        achievement.isEarnedHardcore -> "Unlocked (hardcore): ${achievement.dateEarnedHardcore}"
                        achievement.isEarned -> "Unlocked: ${achievement.dateEarned}"
                        else -> "Locked"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (achievement.isEarned) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
    }
}
