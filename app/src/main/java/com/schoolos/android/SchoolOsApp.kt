package com.schoolos.android

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.schoolos.android.core.designsystem.CompatSvgDecoder
import com.schoolos.android.core.notification.SystemNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class SchoolOsApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = try {
            dagger.hilt.android.EntryPointAccessors.fromApplication(
                this,
                AuthEntryPoint::class.java
            ).apiClient().httpClient
        } catch (_: Throwable) {
            null
        }
        return ImageLoader.Builder(this)
            .apply {
                if (okHttpClient != null) {
                    okHttpClient(okHttpClient)
                }
            }
            .components {
                add(CompatSvgDecoder.Factory())
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface AuthEntryPoint {
        fun authManager(): com.schoolos.android.core.auth.AuthManager
        fun apiClient(): com.schoolos.android.core.network.ApiClient
    }

    override fun onCreate() {
        super.onCreate()
        try {
            com.schoolos.android.core.common.BuildConfig.API_BASE_URL = BuildConfig.API_BASE_URL
            com.schoolos.android.core.common.BuildConfig.APP_VERSION = BuildConfig.VERSION_NAME
        } catch (_: Throwable) {}
        if (BuildConfig.DEBUG) {
            try {
                Timber.plant(Timber.DebugTree())
            } catch (_: Throwable) {}
        }

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

        // Re-sync user-targeted and class topics strictly within the authenticated tenant
        try {
            val entry = dagger.hilt.android.EntryPointAccessors.fromApplication(
                this,
                AuthEntryPoint::class.java
            )
            val authManager = entry.authManager()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val auth = authManager.authState.first()
                if (auth.isLoggedIn && !auth.tenantId.isNullOrBlank()) {
                    com.schoolos.android.notification.SchoolOsFirebaseMessagingService.syncUserTopics(
                        tenantId = auth.tenantId,
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
    }
}
