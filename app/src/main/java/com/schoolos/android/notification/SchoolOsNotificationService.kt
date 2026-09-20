package com.schoolos.android.notification

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.schoolos.android.MainActivity
import com.schoolos.android.R
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.feature.notifications.NotificationSyncManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Persistent background sync service for School OS.
 *
 * Keeps real-time SSE stream (/announcements/stream) and instant push sync
 * active 24/7 even when the application is closed or swiped away from Recents.
 */
@AndroidEntryPoint
class SchoolOsNotificationService : Service() {

    @Inject lateinit var notificationSyncManager: NotificationSyncManager
    @Inject lateinit var authManager: AuthManager

    companion object {
        const val CHANNEL_ID = "school_os_service_channel"
        private const val NOTIFICATION_ID = 8801

        fun start(context: Context) {
            try {
                val intent = Intent(context, SchoolOsNotificationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to start SchoolOsNotificationService")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, SchoolOsNotificationService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Timber.w(e, "Failed to stop SchoolOsNotificationService")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createServiceChannel()
        startServiceForeground()
        notificationSyncManager.start()
        Timber.d("SchoolOsNotificationService created & running")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createServiceChannel()
        startServiceForeground()
        notificationSyncManager.start()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Timber.d("SchoolOsNotificationService: App swiped from Recents, maintaining 24/7 notification sync")
        try {
            startServiceForeground()
            notificationSyncManager.start()

            // Schedule quick wake-up intent via AlarmManager as safety net against aggressive OEM task killers
            val restartIntent = Intent(applicationContext, SchoolOsNotificationService::class.java)
            val restartPi = PendingIntent.getService(
                applicationContext,
                8802,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000L, restartPi)
            } else {
                am?.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000L, restartPi)
            }
        } catch (e: Exception) {
            Timber.w(e, "Error during onTaskRemoved in SchoolOsNotificationService")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startServiceForeground() {
        val notification = buildForegroundNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Timber.w(e, "startForeground failed, attempting fallback")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Layanan Latar Belakang School OS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga notifikasi tugas, kuis, dan pengumuman tetap masuk saat aplikasi ditutup"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("School OS")
            .setContentText("Layanan notifikasi & pengingat aktif")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
