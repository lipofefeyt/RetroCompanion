package com.example.retrocompanion.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.retrocompanion.data.model.AchievementOfTheWeek
import com.example.retrocompanion.ui.components.NetworkImage

/** Home card for the current Achievement of the Week; tapping it opens the game's details. */
@Composable
fun AchievementOfTheWeekCard(
    achievement: AchievementOfTheWeek,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val featuredSince = achievement.featuredSinceLabel()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open ${achievement.gameTitle}", onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Achievement of the Week",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NetworkImage(
                    url = achievement.badgeUrl,
                    contentDescription = achievement.title,
                    placeholderGlyph = "🏆",
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${achievement.title} (${achievement.points} pts)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    achievement.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = listOfNotNull(
                    achievement.gameTitle.takeIf { it.isNotBlank() },
                    achievement.consoleName
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            achievement.unlockedPercent?.let { percent ->
                Text(
                    text = "${achievement.unlocksCount} of ${achievement.totalPlayers} players unlocked it " +
                        "(${"%.1f".format(percent)}%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            featuredSince?.let {
                Text(
                    text = "Featured since $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Only claim it when the user was actually found; the unlock list may be incomplete.
            if (achievement.unlockedByUser) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓ You've unlocked this",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
