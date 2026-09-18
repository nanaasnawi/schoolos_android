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
    }
}
