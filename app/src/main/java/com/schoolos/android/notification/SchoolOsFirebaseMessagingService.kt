package com.schoolos.android.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.schoolos.android.core.notification.SystemNotificationHelper
import timber.log.Timber

class SchoolOsFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM Registration Token: %s", token)
        val prefs = applicationContext.getSharedPreferences("schoolos_fcm", MODE_PRIVATE)
        prefs.edit()
            .putString("fcm_token", token)
            .putBoolean("fcm_token_synced", false)
            .apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("FCM Message Received from: %s", remoteMessage.from)

        // Extract title & body from data payload (preferred for background wakeup) or notification payload
        val data = remoteMessage.data
        val title = data["title"]
            ?: remoteMessage.notification?.title
            ?: "Pemberitahuan Sekolah"

        val body = data["body"]
            ?: data["content"]
            ?: remoteMessage.notification?.body
            ?: "Ada informasi baru di School OS"

        val notifId = data["id"]?.hashCode() ?: System.currentTimeMillis().toInt()

        // Trigger native Android system notification with HIGH priority (wakes up lock screen)
        SystemNotificationHelper.showNotification(
            context = applicationContext,
            notificationId = notifId,
            title = if (title.startsWith("📢")) title else "📢 $title",
            message = body
        )
    }
}
