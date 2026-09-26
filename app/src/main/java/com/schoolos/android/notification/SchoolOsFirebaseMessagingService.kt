package com.schoolos.android.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.schoolos.android.core.notification.SystemNotificationHelper
import timber.log.Timber

class SchoolOsFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val PREFS_FCM = "schoolos_fcm"
        private const val TOPIC_ANNOUNCEMENTS = "school_announcements"
        private const val TOPIC_MATERIALS = "school_materials"
        private const val TOPIC_ASSIGNMENTS = "school_assignments"
        private const val TOPIC_QUIZZES = "school_quizzes"
        private const val TOPIC_GRADES = "school_grades"
        private const val TOPIC_SESSIONS = "school_sessions"

        fun subscribeAllTopics() {
            val topics = listOf(
                TOPIC_ANNOUNCEMENTS, TOPIC_MATERIALS, TOPIC_ASSIGNMENTS,
                TOPIC_QUIZZES, TOPIC_GRADES, TOPIC_SESSIONS,
            )
            topics.forEach { topic ->
                try {
                    FirebaseMessaging.getInstance().subscribeToTopic(topic)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Timber.d("FCM successfully subscribed to topic: %s", topic)
                            } else {
                                Timber.w(task.exception, "FCM failed to subscribe to topic: %s", topic)
                            }
                        }
                } catch (e: Exception) {
                    Timber.w(e, "FCM subscribe error: %s", topic)
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM Registration Token: %s", token)
        val prefs = applicationContext.getSharedPreferences(PREFS_FCM, MODE_PRIVATE)
        prefs.edit()
            .putString("fcm_token", token)
            .putBoolean("fcm_token_synced", false)
            .apply()

        // Re-subscribe SEMUA topik belajar saat token rotate.
        subscribeAllTopics()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("FCM Message Received from: %s", remoteMessage.from)

        // DATA-ONLY adalah jalur utama (server tidak kirim key `notification`).
        val data = remoteMessage.data
        val title = data["title"]
            ?: remoteMessage.notification?.title
            ?: "Pemberitahuan Sekolah"

        val body = data["body"]
            ?: data["content"]
            ?: remoteMessage.notification?.body
            ?: "Ada informasi baru di Akselerasi Edu"

        val category = data["category"] ?: "ANNOUNCEMENT"
        val referenceType = data["reference_type"] ?: category.lowercase()
        val referenceId = data["reference_id"] ?: data["id"] ?: ""
        val navigateTo = data["navigate_to"] ?: navigateTargetFor(category, referenceType)
        val channelId = data["channel_id"]
        val clickAction = data["click_action"]

        // ID unik per pesan agar judul mirip tidak saling menimpa saat idle.
        val notifId = if (referenceId.isNotBlank()) {
            (referenceId + navigateTo).hashCode()
        } else {
            (data["id"]?.hashCode() ?: System.currentTimeMillis().toInt())
        }

        // Record ID into shown_notif_ids so background poll worker never duplicates this notification
        try {
            val syncPrefs = applicationContext.getSharedPreferences("schoolos_notif_sync", Context.MODE_PRIVATE)
            val shownIds = syncPrefs.getStringSet("shown_notif_ids", emptySet()) ?: emptySet()
            val mutableShown = shownIds.toMutableSet()
            if (referenceId.isNotBlank()) mutableShown.add(referenceId)
            val rawId = data["id"]
            if (!rawId.isNullOrBlank()) mutableShown.add(rawId)
            mutableShown.add(notifId.toString())
            syncPrefs.edit().putStringSet("shown_notif_ids", mutableShown).apply()
        } catch (_: Exception) {}

        // Trigger native Android system notification with HIGH priority (wakes up lock screen)
        SystemNotificationHelper.showNotification(
            context = applicationContext,
            notificationId = notifId,
            title = prefixFor(category, title),
            message = body,
            navigateTo = navigateTo,
            channelId = channelId,
            clickAction = clickAction,
            category = category,
        )
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Timber.w("FCM deleted messages — fallback polling /notifications akan mengambilnya")
    }

    private fun navigateTargetFor(category: String, referenceType: String): String {
        val c = category.lowercase()
        val r = referenceType.lowercase()
        return when {
            c.contains("material") || r.contains("material") -> "materials"
            c.contains("assign") || r.contains("assign") -> "assignments"
            c.contains("quiz") || r.contains("quiz") || c.contains("cbt") -> "quizzes"
            c.contains("grade") || r.contains("grade") || c.contains("nilai") -> "grades"
            c.contains("session") || r.contains("session") || c.contains("jadwal") -> "sessions"
            c.contains("reminder") || r.contains("reminder") -> "schedule"
            else -> "notifications"
        }
    }

    private fun prefixFor(category: String, title: String): String {
        if (title.startsWith("📢") || title.startsWith("📚") || title.startsWith("📝") ||
            title.startsWith("💻") || title.startsWith("🏆") || title.startsWith("🔔") ||
            title.startsWith("🎓")
        ) return title
        val c = category.uppercase()
        val emoji = when {
            c.contains("MATERIAL") -> "📚"
            c.contains("ASSIGN") -> "📝"
            c.contains("QUIZ") || c.contains("CBT") -> "💻"
            c.contains("GRADE") || c.contains("NILAI") -> "🏆"
            c.contains("SESSION") || c.contains("JADWAL") -> "🎓"
            c.contains("REMINDER") -> "🔔"
            else -> "📢"
        }
        return "$emoji $title"
    }
}
