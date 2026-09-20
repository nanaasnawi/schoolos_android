package com.schoolos.android

import android.app.Application
import com.schoolos.android.core.notification.SystemNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SchoolOsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.schoolos.android.core.common.BuildConfig.API_BASE_URL = BuildConfig.API_BASE_URL
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize high-priority notification channel early so Google Play Services & FCM can use it even when killed/standby
        SystemNotificationHelper.createNotificationChannel(this)

        // Subscribe to all FCM topics early
        try {
            com.schoolos.android.notification.SchoolOsFirebaseMessagingService.subscribeAllTopics()
        } catch (_: Exception) {}

        // Clean up any old persistent service notification & channel
        try {
            val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            nm?.cancel(8801)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                nm?.deleteNotificationChannel("school_os_service_channel")
            }
        } catch (_: Exception) {}

        // Schedule silent background alarm polling (no sticky notification bar icon!)
        try {
            com.schoolos.android.notification.NotificationPollReceiver.schedule(this)
        } catch (_: Exception) {}
    }
}
