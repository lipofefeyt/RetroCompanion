package com.example.retrocompanion.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * The opt-in switch for unlock notifications. Turning it on first makes sure the app is allowed
 * to post notifications: on Android 13+ it explains why and then asks for POST_NOTIFICATIONS;
 * if that is denied (or notifications are off in system settings) the switch stays off and a
 * button opens the app's notification settings.
 */
@Composable
fun UnlockNotificationsSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var canPost by remember { mutableStateOf(NotificationPermission.canPost(context)) }
    var showExplanation by rememberSaveable { mutableStateOf(false) }
    var showBlocked by rememberSaveable { mutableStateOf(false) }

    // Coming back from system settings: pick up any change the user made there.
    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canPost = NotificationPermission.canPost(context)
                if (canPost) showBlocked = false
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canPost = NotificationPermission.canPost(context)
        if (canPost) {
            showBlocked = false
            onEnabledChange(true)
        } else {
            showBlocked = true
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unlock notifications",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Get a notification when you unlock a new achievement. The app checks " +
                            "RetroAchievements about every 15 minutes while you have a connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { wantOn ->
                        when {
                            !wantOn -> {
                                showBlocked = false
                                onEnabledChange(false)
                            }
                            NotificationPermission.canPost(context) -> onEnabledChange(true)
                            NotificationPermission.needsRuntimeRequest(context) -> showExplanation = true
                            else -> showBlocked = true
                        }
                    },
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            if (showBlocked || (enabled && !canPost)) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (enabled) {
                        "Notifications are turned off for RetroCompanion in system settings, so you won't see any."
                    } else {
                        "Notifications are blocked for RetroCompanion, so this can't be turned on yet."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { context.startActivity(NotificationPermission.settingsIntent(context)) }) {
                    Text("Open notification settings")
                }
            }
        }
    }

    if (showExplanation) {
        AlertDialog(
            onDismissRequest = { showExplanation = false },
            title = { Text("Allow notifications?") },
            text = {
                Text(
                    "RetroCompanion needs your permission to show a notification when you unlock an " +
                        "achievement. Android will ask you next. Nothing is sent anywhere except " +
                        "your own RetroAchievements requests."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExplanation = false
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showExplanation = false }) { Text("Not now") }
            }
        )
    }
}
