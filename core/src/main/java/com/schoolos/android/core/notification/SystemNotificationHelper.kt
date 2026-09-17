package com.schoolos.android.core.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object SystemNotificationHelper {

    const val CHANNEL_ID = "school_os_announcements_v2"
    private const val CHANNEL_NAME = "Pengumuman & Broadcast Sekolah"
    private const val CHANNEL_DESC = "Pemberitahuan resmi dan pengumuman sekolah"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun isDuplicate(context: Context, title: String, message: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences("schoolos_notif_dedup", Context.MODE_PRIVATE)
            val cleanTitle = title.removePrefix("📢").trim().lowercase()
            val cleanMsg = message.trim().lowercase()
            val key = "dedup_${(cleanTitle + "_" + cleanMsg.take(60)).hashCode()}"
            val now = System.currentTimeMillis()

            val lastShown = prefs.getLong(key, 0L)
            if (now - lastShown < 60_000L) {
                // Duplicate notification within 60 seconds, drop it
                true
            } else {
                prefs.edit().putLong(key, now).apply()
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
    ) {
        // 1. Drop duplicate notifications from multi-channel triggers (FCM + SSE + Polling)
        if (isDuplicate(context, title, message)) {
            return
        }

        // Check Android 13+ permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Ensure channel exists
        createNotificationChannel(context)

        // Wake screen up if currently turned off / in standby
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null && !pm.isInteractive) {
                @Suppress("DEPRECATION")
                val wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE,
                    "schoolos:notification_wake"
                )
                wakeLock.acquire(5000L)
            }
        } catch (_: Exception) {}

        // Deterministic notification slot based on title hash so even if any rapid event arrives,
        // it updates the exact same notification slot instead of creating a second duplicate banner!
        val cleanTitle = title.removePrefix("📢").trim().lowercase()
        val stableId = cleanTitle.hashCode()

        // Sync with shown IDs in NotificationSyncManager
        try {
            val syncPrefs = context.getSharedPreferences("schoolos_notif_sync", Context.MODE_PRIVATE)
            val shownIds = syncPrefs.getStringSet("shown_notif_ids", emptySet()) ?: emptySet()
            val mutableShown = shownIds.toMutableSet()
            mutableShown.add(notificationId.toString())
            mutableShown.add(stableId.toString())
            syncPrefs.edit().putStringSet("shown_notif_ids", mutableShown).apply()
        } catch (_: Exception) {}

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            stableId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = context.applicationInfo.icon.takeIf { it != 0 }
            ?: android.R.drawable.ic_dialog_info

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, false)

        try {
            NotificationManagerCompat.from(context).notify(stableId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
