package com.example.retrocompanion.ui.library

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star as OutlinedStar
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
import com.example.retrocompanion.ui.common.CredentialsStatus
import com.example.retrocompanion.ui.common.SectionState
import com.example.retrocompanion.ui.components.EmptySectionCard
import com.example.retrocompanion.ui.components.LoadingScreen
import com.example.retrocompanion.ui.components.MissingCredentialsScreen
import com.example.retrocompanion.ui.components.NetworkImage
import com.example.retrocompanion.ui.components.RefreshCompletionHaptic
import com.example.retrocompanion.ui.components.WishlistIcons
import com.example.retrocompanion.ui.components.SectionErrorCard
import com.example.retrocompanion.ui.components.StaleDataBanner
import com.example.retrocompanion.ui.components.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLibraryScreen(
    viewModel: GameLibraryViewModel,
    onNavigateToSettings: () -> Unit,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    RefreshCompletionHaptic(isRefreshing = uiState.isRefreshing)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Game Library", fontWeight = FontWeight.Bold) },
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
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            label = { Text("Search your library") },
                            singleLine = true,
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
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
                            when (val state = uiState.games) {
                                is SectionState.Loading -> LoadingScreen(message = "Loading your game library…")

                                is SectionState.Error -> Column(modifier = Modifier.padding(16.dp)) {
                                    SectionErrorCard(
                                        message = state.message,
                                        showGoToSettings = state.isInvalidApiKey,
                                        onRetry = { viewModel.retry() },
                                        onGoToSettings = onNavigateToSettings
                                    )
                                }

                                is SectionState.Success -> {
                                    val filtered = uiState.filteredGames
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (state.isStale && (state.cachedAtMillis != null)) {
                                            item { StaleDataBanner(relativeTime = formatRelativeTime(state.cachedAtMillis)) }
                                        }
                                        val favorites = uiState.favoriteGames
                                        if (favorites.isNotEmpty() && uiState.searchQuery.isBlank()) {
                                            item {
                                                Text(
                                                    text = "Favorites",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                            }
                                            items(items = favorites, key = { "fav-${it.gameId}" }) { game ->
                                                GameProgressRow(
                                                    game = game,
                                                    isFavorite = true,
                                                    isWishlisted = uiState.wishlistGameIds.contains(game.gameId),
                                                    onClick = { onGameClick(game.gameId) },
                                                    onToggleFavorite = { viewModel.toggleFavorite(game.gameId) },
                                                    onToggleWishlist = { viewModel.toggleWishlist(game) }
                                                )
                                            }
                                            item {
                                                Text(
                                                    text = "All Games",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                                )
                                            }
                                        }
                                        if (filtered.isEmpty()) {
                                            item {
                                                EmptySectionCard(
                                                    title = if (uiState.searchQuery.isBlank()) "No Games Found" else "No Matches",
                                                    subtitle = if (uiState.searchQuery.isBlank()) {
                                                        "Play some games on RetroAchievements to see your library here."
                                                    } else {
                                                        "No games match \"${uiState.searchQuery}\"."
                                                    },
                                                    icon = if (uiState.searchQuery.isBlank()) "🎮" else "🔍"
                                                )
                                            }
                                        } else {
                                            items(items = filtered, key = { it.gameId }) { game ->
                                                GameProgressRow(
                                                    game = game,
                                                    isFavorite = uiState.favoriteGameIds.contains(game.gameId),
                                                    isWishlisted = uiState.wishlistGameIds.contains(game.gameId),
                                                    onClick = { onGameClick(game.gameId) },
                                                    onToggleFavorite = { viewModel.toggleFavorite(game.gameId) },
                                                    onToggleWishlist = { viewModel.toggleWishlist(game) }
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
}

@Composable
private fun GameProgressRow(
    game: GameProgressSummary,
    isFavorite: Boolean,
    isWishlisted: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWishlist: () -> Unit,
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
                modifier = Modifier.size(56.dp)
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

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { game.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .padding(vertical = 0.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${game.numAwarded} / ${game.maxPossible} unlocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${game.progressPercentage}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (game.numAwardedHardcore > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "🏆 ${game.numAwardedHardcore} hardcore",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onToggleWishlist) {
                Icon(
                    imageVector = if (isWishlisted) WishlistIcons.Bookmark else WishlistIcons.BookmarkBorder,
                    contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
                    tint = if (isWishlisted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.OutlinedStar,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
