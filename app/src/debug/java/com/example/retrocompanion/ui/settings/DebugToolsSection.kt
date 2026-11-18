package com.example.retrocompanion.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.retrocompanion.data.local.UserPreferencesRepository
import com.example.retrocompanion.data.polling.AndroidUnlockNotifier
import com.example.retrocompanion.data.repository.RetroAchievementsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Debug builds only (this file lives in src/debug; src/release has a no-op twin). Fetches the
 * signed-in user's most recent real unlock and posts it through the real [AndroidUnlockNotifier],
 * so the channel, permission, badge, description and tap-to-open can be checked on a device
 * without earning a new achievement. Nothing is logged or stored.
 */
@Composable
fun DebugToolsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Spacer(modifier = Modifier.height(32.dp))
    Column {
        Text(
            text = "Debug tools",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    val credentials = UserPreferencesRepository(context).userCredentialsFlow.first()
                    val latest = RetroAchievementsRepository()
                        .getRecentUnlocks(credentials, lookbackMinutes = LOOKBACK_MINUTES)
                        .getOrNull()
                        ?.firstOrNull() // sorted newest first by the repository
                    if (latest == null) {
                        Toast.makeText(
                            context,
                            "No recent unlock found (is the key saved?)",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        AndroidUnlockNotifier(context).notifyNewUnlocks(listOf(latest))
                    }
                }
            }
        ) {
            Text("Notify my latest unlock")
        }
    }
}

private const val LOOKBACK_MINUTES = 60 * 24 * 30 // 30 days
