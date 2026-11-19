package com.example.retrocompanion.data.polling

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import com.example.retrocompanion.MainActivity
import com.example.retrocompanion.R
import com.example.retrocompanion.data.model.RecentUnlock

/** Posts a system notification for each new unlock, on the app's dedicated channel. */
class AndroidUnlockNotifier(context: Context) : UnlockNotifier {

    private val appContext = context.applicationContext
    private val imageLoader by lazy { ImageLoader(appContext) }

    @SuppressLint("MissingPermission") // Guarded by areNotificationsEnabled(), which covers the runtime permission.
    override suspend fun notifyNewUnlocks(unlocks: List<RecentUnlock>) {
        val manager = NotificationManagerCompat.from(appContext)
        // Without permission (or with notifications turned off) there's nowhere to show these.
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(appContext)

        UnlockNotificationPlan.from(unlocks).forEach { planned ->
            val expanded = listOfNotNull(planned.description, planned.text).joinToString("\n")
            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(planned.iconUrl?.let { loadBadge(it) })
                .setContentTitle(planned.title)
                .setContentText(planned.text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(expanded))
                .setContentIntent(openIntent(planned))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            manager.notify(planned.id, notification)
        }
    }

    /** The badge as a bitmap, or null if it can't be loaded: a missing icon never blocks the notification. */
    private suspend fun loadBadge(url: String): Bitmap? = try {
        val request = ImageRequest.Builder(appContext)
            .data(url)
            .allowHardware(false) // notifications need a software bitmap
            .build()
        imageLoader.execute(request).drawable?.toBitmap()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun openIntent(planned: PlannedNotification): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            planned.gameId?.let { putExtra(MainActivity.EXTRA_GAME_ID, it) }
        }
        return PendingIntent.getActivity(
            appContext,
            planned.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        // A channel's importance can't be raised in code once it exists, so the high-importance
        // channel has its own id and the original default-importance one is removed.
        const val CHANNEL_ID = "unlock_notifications_high"
        private const val LEGACY_CHANNEL_ID = "unlock_notifications"

        /** Creates the channel (Android 8+). Safe to call repeatedly. */
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Achievement unlocks",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tells you when you unlock a new RetroAchievements achievement."
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
            manager.createNotificationChannel(channel)
        }
    }
}
