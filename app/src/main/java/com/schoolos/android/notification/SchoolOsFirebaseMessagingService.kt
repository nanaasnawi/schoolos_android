package com.schoolos.android.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.schoolos.android.core.notification.SystemNotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import timber.log.Timber

class SchoolOsFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val PREFS_FCM = "schoolos_fcm"
        private const val TOPIC_ANNOUNCEMENTS = "school_announcements"

        private fun cleanHexId(raw: String): String =
            raw.trim().replace("-", "").lowercase()

        fun syncUserTopics(
            tenantId: String?,
            userId: String?,
            classId: String?,
            role: String?,
            className: String? = null,
            context: Context? = null,
        ) {
            if (tenantId.isNullOrBlank()) {
                Timber.d("FCM syncUserTopics skipped: tenantId is blank")
                return
            }

            val fcm = FirebaseMessaging.getInstance()
            val cleanTenant = cleanHexId(tenantId)
            val tenantPrefix = "t_$cleanTenant"
            val prefs = context?.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE)

            // Unsubscribe from legacy unscoped topic so cross-school pushes are never received
            fcm.unsubscribeFromTopic(TOPIC_ANNOUNCEMENTS)

            // 1. Tenant-scoped general announcement topic
            val tenantAnnTopic = "${tenantPrefix}_$TOPIC_ANNOUNCEMENTS"
            val oldTenantAnn = prefs?.getString("subscribed_tenant_ann_topic", null)
            if (oldTenantAnn != null && oldTenantAnn != tenantAnnTopic) {
                fcm.unsubscribeFromTopic(oldTenantAnn)
            }
            fcm.subscribeToTopic(tenantAnnTopic)

            prefs?.edit()
                ?.putString("active_tenant_id", tenantId.trim())
                ?.putString("subscribed_tenant_ann_topic", tenantAnnTopic)
                ?.putString("active_user_id", userId?.trim())
                ?.putString("active_class_id", classId?.trim())
                ?.putString("active_role", role?.trim())
                ?.apply()

            // 2. Tenant-scoped role topic
            val cleanRole = when {
                role.isNullOrBlank() -> null
                role.contains("principal", true) || role.contains("kepala", true) || role.contains("headmaster", true) -> "principal"
                role.contains("admin", true) || role.contains("operator", true) || role.contains("staff", true) || role.contains("staf", true) -> "staff"
                role.contains("teacher", true) || role.contains("guru", true) || role.contains("tutor", true) -> "teacher"
                role.contains("parent", true) || role.contains("wali", true) -> "parent"
                else -> "student"
            }
            if (cleanRole != null) {
                val roleTopic = "${tenantPrefix}_role_$cleanRole"
                val oldRole = prefs?.getString("subscribed_role_topic", null)
                if (oldRole != null && oldRole != roleTopic) {
                    fcm.unsubscribeFromTopic(oldRole)
                }
                fcm.subscribeToTopic(roleTopic)
                prefs?.edit()?.putString("subscribed_role_topic", roleTopic)?.apply()
            }

            // 3. Tenant-scoped class topic
            if (!classId.isNullOrBlank()) {
                val cleanClass = cleanHexId(classId.removePrefix("class_"))
                val classTopic = "${tenantPrefix}_class_$cleanClass"
                val oldClass = prefs?.getString("subscribed_class_topic", null)
                if (oldClass != null && oldClass != classTopic) {
                    fcm.unsubscribeFromTopic(oldClass)
                }
                fcm.subscribeToTopic(classTopic)
                prefs?.edit()?.putString("subscribed_class_topic", classTopic)?.apply()
                Timber.d("FCM Subscribed to tenant class topic: %s", classTopic)
            }

            // 4. Tenant-scoped private user topic
            if (!userId.isNullOrBlank()) {
                val cleanUser = cleanHexId(userId.removePrefix("user_"))
                val userTopic = "${tenantPrefix}_user_$cleanUser"
                val oldUser = prefs?.getString("subscribed_user_topic", null)
                if (oldUser != null && oldUser != userTopic) {
                    fcm.unsubscribeFromTopic(oldUser)
                }
                fcm.subscribeToTopic(userTopic)
                prefs?.edit()?.putString("subscribed_user_topic", userTopic)?.apply()
                Timber.d("FCM Subscribed to tenant private user topic: %s", userTopic)
            }
        }

        fun unsubscribeUserTopics(context: Context) {
            val fcm = FirebaseMessaging.getInstance()
            val prefs = context.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE)
            fcm.unsubscribeFromTopic(TOPIC_ANNOUNCEMENTS)
            prefs.getString("subscribed_tenant_ann_topic", null)?.let { fcm.unsubscribeFromTopic(it) }
            prefs.getString("subscribed_class_topic", null)?.let { fcm.unsubscribeFromTopic(it) }
            prefs.getString("subscribed_user_topic", null)?.let { fcm.unsubscribeFromTopic(it) }
            prefs.getString("subscribed_role_topic", null)?.let { fcm.unsubscribeFromTopic(it) }
            prefs.edit()
                .remove("active_tenant_id")
                .remove("subscribed_tenant_ann_topic")
                .remove("subscribed_class_topic")
                .remove("subscribed_user_topic")
                .remove("subscribed_role_topic")
                .remove("active_user_id")
                .remove("active_class_id")
                .remove("active_role")
                .apply()
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM Registration Token refreshed")
        val prefs = applicationContext.getSharedPreferences(PREFS_FCM, MODE_PRIVATE)
        prefs.edit()
            .putString("fcm_token", token)
            .putBoolean("fcm_token_synced", false)
            .apply()

        val activeTenant = prefs.getString("active_tenant_id", null)
        if (!activeTenant.isNullOrBlank()) {
            syncUserTopics(
                tenantId = activeTenant,
                userId = prefs.getString("active_user_id", null),
                classId = prefs.getString("active_class_id", null),
                role = prefs.getString("active_role", null),
                context = applicationContext,
            )
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("FCM Message Received from: %s", remoteMessage.from)

        // DATA-ONLY adalah jalur utama (server tidak kirim key `notification`).
        val data = remoteMessage.data

        val prefs = applicationContext.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE)
        val activeTenantId = prefs.getString("active_tenant_id", null)
        val payloadTenantId = data["tenant_id"]
        if (activeTenantId.isNullOrBlank()) {
            Timber.w("Dropped FCM push: user not logged into any active school tenant")
            return
        }
        if (!payloadTenantId.isNullOrBlank() && !payloadTenantId.equals(activeTenantId, ignoreCase = true)) {
            Timber.w("Dropped cross-tenant FCM push: payload tenant %s != active %s", payloadTenantId, activeTenantId)
            return
        }
        val title = data["title"]
            ?: remoteMessage.notification?.title
            ?: "Pemberitahuan Sekolah"

        val body = data["body"]
            ?: data["content"]
            ?: remoteMessage.notification?.body
            ?: "Ada informasi baru di Akselerasi-Edu"

        val category = data["category"] ?: "ANNOUNCEMENT"
        val referenceType = data["reference_type"] ?: category.lowercase()
        val referenceId = data["reference_id"] ?: data["id"] ?: ""
        val navigateTo = data["navigate_to"] ?: navigateTargetFor(category, referenceType)
        val rawChannelId = data["channel_id"]
        val channelId = when (rawChannelId) {
            "school_os_announcements_v1", "school_os_announcements_v2", "school_os_announcements_v3", "school_os_announcements_v4" -> SystemNotificationHelper.CHANNEL_ID
            "school_os_learning_v1", "school_os_learning_v2" -> SystemNotificationHelper.CHANNEL_LEARNING_ID
            null, "" -> null
            else -> rawChannelId
        }
        val clickAction = data["click_action"]
        val deepLink = data["deep_link"]

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
            if (referenceId.isNotBlank()) {
                mutableShown.add(referenceId)
                mutableShown.add("schoolos_$referenceId")
            }
            val rawId = data["id"]
            if (!rawId.isNullOrBlank()) mutableShown.add(rawId)
            mutableShown.add(notifId.toString())
            syncPrefs.edit().putStringSet("shown_notif_ids", mutableShown).apply()
        } catch (_: Exception) {}

        // Quiet hours protection: drop reminder alarms/notifications between 21:00 and 06:00 WIB
        val isReminder = category.contains("REMINDER", ignoreCase = true) || 
                         referenceType.contains("reminder", ignoreCase = true)
        if (isReminder && com.schoolos.android.core.notification.TeacherScheduleReminderManager.isQuietHours()) {
            Timber.d("FCM Reminder dropped during quiet hours (21:00 - 06:00 WIB)")
            return
        }

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
            referenceId = referenceId,
            referenceType = referenceType,
            deepLink = deepLink,
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
        return title.trim()
    }
}
