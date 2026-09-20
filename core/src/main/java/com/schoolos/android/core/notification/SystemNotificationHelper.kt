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
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import android.content.ContentResolver
import android.media.AudioManager
import android.net.Uri

object SystemNotificationHelper {

    const val CHANNEL_ID = "school_os_announcements_v4"
    const val CHANNEL_LEARNING_ID = "school_os_learning_v2"
    private const val CHANNEL_NAME = "Pengumuman & Broadcast Sekolah"
    private const val CHANNEL_DESC = "Pemberitahuan resmi dan pengumuman sekolah"
    private const val CHANNEL_LEARNING_NAME = "Materi, Tugas, Kuis & Nilai"
    private const val CHANNEL_LEARNING_DESC = "Notifikasi pembelajaran: materi, tugas, kuis/CBT, nilai, dan jadwal"

    fun getSoundUri(context: Context): Uri {
        return try {
            Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/notification")
        } catch (_: Exception) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            // Clean up obsolete/locked channels from earlier versions to reset OEM sound restrictions
            val obsoleteChannels = listOf(
                "school_os_announcements_v1",
                "school_os_announcements_v2",
                "school_os_announcements_v3",
                "school_os_learning_v1",
                "fcm_fallback_notification_channel"
            )
            for (oldChannel in obsoleteChannels) {
                try {
                    manager?.deleteNotificationChannel(oldChannel)
                } catch (_: Exception) {}
            }

            val importance = NotificationManager.IMPORTANCE_HIGH
            val soundUri = getSoundUri(context)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            // Channel kedua untuk event belajar agar tidak tenggelam oleh pengumuman.
            val learning = NotificationChannel(CHANNEL_LEARNING_ID, CHANNEL_LEARNING_NAME, importance).apply {
                description = CHANNEL_LEARNING_DESC
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 120, 300)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            manager?.createNotificationChannel(channel)
            manager?.createNotificationChannel(learning)
        }
    }

    private fun isDuplicate(context: Context, title: String, message: String, navigateTo: String = "notifications"): Boolean {
        return try {
            val prefs = context.getSharedPreferences("schoolos_notif_dedup", Context.MODE_PRIVATE)
            val cleanTitle = title
                .removePrefix("📢").removePrefix("📚").removePrefix("📝")
                .removePrefix("💻").removePrefix("🏆").removePrefix("🔔").removePrefix("🎓")
                .trim().lowercase()
            val cleanMsg = message.trim().lowercase()
            val key = "dedup_${(navigateTo + "_" + cleanTitle + "_" + cleanMsg.take(60)).hashCode()}"
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
        navigateTo: String = "notifications",
        channelId: String? = null,
        clickAction: String? = null,
        category: String? = null,
    ) {
        // 1. Drop duplicate notifications from multi-channel triggers (FCM + SSE + Polling)
        //    Dedup key mencakup navigateTo agar materi/tugas/kuis berbeda tidak saling menelan.
        if (isDuplicate(context, title, message, navigateTo)) {
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

        // Deterministic notification slot per pesan — pakai ID dari FCM (bukan hash
        // judul!) agar tiap materi/tugas/kuis tampil sendiri dan tidak menimpa tray.
        val notifyId = notificationId

        // Sync with shown IDs in NotificationSyncManager
        try {
            val syncPrefs = context.getSharedPreferences("schoolos_notif_sync", Context.MODE_PRIVATE)
            val shownIds = syncPrefs.getStringSet("shown_notif_ids", emptySet()) ?: emptySet()
            val mutableShown = shownIds.toMutableSet()
            mutableShown.add(notificationId.toString())
            syncPrefs.edit().putStringSet("shown_notif_ids", mutableShown).apply()
        } catch (_: Exception) {}

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", navigateTo)
            if (!clickAction.isNullOrBlank()) action = clickAction
        } ?: Intent(Intent.ACTION_MAIN).apply {
            `package` = context.packageName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", navigateTo)
            if (!clickAction.isNullOrBlank()) action = clickAction
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifyId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pilih channel sesuai kategori: pengumuman vs event belajar.
        val effectiveChannel = when {
            !channelId.isNullOrBlank() -> channelId
            navigateTo == "notifications" -> CHANNEL_ID
            else -> CHANNEL_LEARNING_ID
        }

        val iconRes = context.applicationInfo.icon.takeIf { it != 0 }
            ?: android.R.drawable.ic_dialog_info

        val soundUri = getSoundUri(context)

        val builder = NotificationCompat.Builder(context, effectiveChannel)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(soundUri, AudioManager.STREAM_NOTIFICATION)
            .setOnlyAlertOnce(false)
            .setVibrate(longArrayOf(0, 350, 150, 350))
            .setLights(Color.BLUE, 1000, 1000)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notifyId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }

        // Active sound player: guarantees loud & clear playback of custom notification.mp3
        // whether the application is in foreground (where Android OS silences notification sounds)
        // or during active background alerts.
        try {
            NotificationSoundPlayer.playSound(context)
        } catch (_: Exception) {}
    }

    @Suppress("unused")
    fun activeCount(context: Context): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.activeNotifications?.size ?: 0
            } else 0
        } catch (_: Exception) { 0 }
    }
}
