package com.example.retrocompanion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Loads [url] via Coil, showing [placeholderGlyph] on a muted, rounded background instead of a
 * broken image whenever [url] is blank (no request is even attempted) or the request fails.
 */
@Composable
fun NetworkImage(
    url: String,
    contentDescription: String?,
    placeholderGlyph: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var loadFailed by remember(url) { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    if (url.isBlank() || loadFailed) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = placeholderGlyph, style = MaterialTheme.typography.titleLarge)
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier.clip(shape),
            onError = { loadFailed = true }
        )
    }
}
