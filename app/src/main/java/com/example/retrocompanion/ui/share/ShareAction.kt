package com.example.retrocompanion.ui.share

import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Returns a function that renders and shares a card using the current theme's colors (so the
 * image matches light or dark mode). Failures show a short message instead of crashing.
 */
@Composable
fun rememberShareCardAction(): (ShareCardContent) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val palette = ShareCardPalette(
        background = colors.background.toArgb(),
        surface = colors.surfaceVariant.toArgb(),
        onSurface = colors.onSurface.toArgb(),
        onSurfaceVariant = colors.onSurfaceVariant.toArgb(),
        accent = colors.primary.toArgb()
    )
    return { content ->
        scope.launch {
            try {
                ShareCardSharer.share(context, content, palette)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Toast.makeText(context, "Couldn't create the share image.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
