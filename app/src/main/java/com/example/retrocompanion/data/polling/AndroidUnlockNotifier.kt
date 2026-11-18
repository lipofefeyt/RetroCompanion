package com.example.retrocompanion.data.polling

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.retrocompanion.MainActivity
import com.example.retrocompanion.R
import com.example.retrocompanion.data.model.RecentUnlock

/** Posts a system notification for each new unlock, on the app's dedicated channel. */
class AndroidUnlockNotifier(context: Context) : UnlockNotifier {

    private val appContext = context.applicationContext

    @SuppressLint("MissingPermission") // Guarded by areNotificationsEnabled(), which covers the runtime permission.
    override suspend fun notifyNewUnlocks(unlocks: List<RecentUnlock>) {
        val manager = NotificationManagerCompat.from(appContext)
        // Without permission (or with notifications turned off) there's nowhere to show these.
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(appContext)

        UnlockNotificationPlan.from(unlocks).forEach { planned ->
            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(planned.title)
                .setContentText(planned.text)
                .setContentIntent(openIntent(planned))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            manager.notify(planned.id, notification)
        }
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
        const val CHANNEL_ID = "unlock_notifications"

        /** Creates the channel (Android 8+). Safe to call repeatedly. */
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Achievement unlocks",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Tells you when you unlock a new RetroAchievements achievement."
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
