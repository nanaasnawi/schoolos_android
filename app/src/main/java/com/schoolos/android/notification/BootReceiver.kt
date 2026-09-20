package com.schoolos.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Setelah reboot/update aplikasi: subscribe ulang topik FCM + jadwalkan
 * polling fallback agar notif tetap masuk walau HP lama idle.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Timber.d("BootReceiver: %s", action)
        try {
            SchoolOsFirebaseMessagingService.subscribeAllTopics()
        } catch (e: Exception) {
            Timber.w(e, "BootReceiver subscribe failed")
        }
        try {
            NotificationPollReceiver.schedule(context.applicationContext)
        } catch (e: Exception) {
            Timber.w(e, "BootReceiver schedule failed")
        }
        try {
            SchoolOsNotificationService.start(context.applicationContext)
        } catch (e: Exception) {
            Timber.w(e, "BootReceiver start service failed")
        }
    }
}
