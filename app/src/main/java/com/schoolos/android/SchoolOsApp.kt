package com.schoolos.android

import android.app.Application
import com.schoolos.android.core.notification.SystemNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class SchoolOsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            com.schoolos.android.core.common.BuildConfig.API_BASE_URL = BuildConfig.API_BASE_URL
        } catch (_: Throwable) {}
        try {
            Timber.plant(Timber.DebugTree())
        } catch (_: Throwable) {}

        // Global uncaught exception logger
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Timber.e(throwable, "FATAL UNCAUGHT EXCEPTION in %s: %s", thread.name, throwable.message)
            } catch (_: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Initialize high-priority notification channel early so Google Play Services & FCM can use it even when killed/standby
        try {
            SystemNotificationHelper.createNotificationChannel(this)
        } catch (_: Throwable) {}

        // Subscribe to all FCM topics early
        try {
            com.schoolos.android.notification.SchoolOsFirebaseMessagingService.subscribeAllTopics()
        } catch (_: Throwable) {}

        // Re-sync user-targeted and class topics immediately if user is already authenticated
        try {
            val entry = dagger.hilt.android.EntryPointAccessors.fromApplication(
                this,
                com.schoolos.android.notification.NotificationPollWorker.PollEntryPoint::class.java
            )
            val authManager = entry.authManager()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val auth = authManager.authState.first()
                if (auth.isLoggedIn) {
                    com.schoolos.android.notification.SchoolOsFirebaseMessagingService.syncUserTopics(
                        userId = auth.userId,
                        classId = auth.classId,
                        role = auth.role,
                        className = auth.className,
                        context = this@SchoolOsApp,
                    )
                }
            }
        } catch (_: Throwable) {}

        // Clean up any old persistent service notification & channel
        try {
            val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            nm?.cancel(8801)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                nm?.deleteNotificationChannel("school_os_service_channel")
            }
        } catch (_: Throwable) {}

        // Schedule silent background alarm polling (no sticky notification bar icon!)
        try {
            com.schoolos.android.notification.NotificationPollReceiver.schedule(this)
        } catch (_: Throwable) {}
    }
}
