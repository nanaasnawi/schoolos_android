package com.schoolos.android.notification

import android.content.Context
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.common.BuildConfig
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Worker polling langsung via OkHttp (tanpa Hilt graph di Receiver) agar tetap
 * jalan saat aplikasi killed / HP idle. Menarik /notifications lalu menampilkan
 * yang unread sebagai notifikasi sistem PRIORITY_MAX.
 */
object NotificationPollWorker {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PollEntryPoint {
        fun authManager(): AuthManager
    }

    suspend fun pollOnce(appContext: Context) = withContext(Dispatchers.IO) {
        val entry = try {
            EntryPointAccessors.fromApplication(appContext, PollEntryPoint::class.java)
        } catch (e: Exception) {
            Timber.w(e, "Poll hilt entry failed")
            return@withContext
        }
        val auth = try { entry.authManager() } catch (e: Exception) {
            Timber.w(e, "Poll auth unavailable")
            return@withContext
        }
        val token = try { auth.getAccessToken() } catch (_: Exception) { null }
        if (token.isNullOrBlank()) return@withContext

        val serverUrl = try {
            auth.getCustomServerUrl() ?: BuildConfig.API_BASE_URL
        } catch (_: Exception) {
            BuildConfig.API_BASE_URL
        }
        val url = serverUrl.trimEnd('/') + "/notifications?page=1&page_size=20"

        val client = OkHttpClient.Builder()
            .dns(com.schoolos.android.core.network.ResilientDns())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .build()

        val bodyStr = try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Timber.w("Poll HTTP %s", resp.code)
                    return@withContext
                }
                resp.body?.string() ?: return@withContext
            }
        } catch (e: Exception) {
            Timber.w(e, "Poll network failed")
            return@withContext
        }
        parseAndShow(appContext, bodyStr)
    }

    private fun parseAndShow(appContext: Context, bodyStr: String) {
        try {
            val root = JSONObject(bodyStr)
            val data = root.optJSONObject("data") ?: return
            val arr = when {
                data.has("items") -> data.optJSONArray("items")
                data.has("data") -> data.optJSONArray("data")
                else -> null
            } ?: return

            val prefs = appContext.getSharedPreferences("schoolos_notif_sync", Context.MODE_PRIVATE)
            val shown = (prefs.getStringSet("shown_notif_ids", emptySet()) ?: emptySet()).toMutableSet()
            var shownThisCycle = 0

            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id")
                if (id.isBlank() || shown.contains(id)) continue
                val isRead = o.optBoolean("is_read", o.optBoolean("isRead", false))
                if (isRead) {
                    shown.add(id)
                    continue
                }
                val title = o.optString("title", "Pemberitahuan Sekolah")
                val message = o.optString("body", o.optString("content", "Ada informasi baru"))
                val type = o.optString("notification_type", o.optString("notificationType", "ANNOUNCEMENT"))
                val navigateTo = navigateFor(type)
                shown.add(id)
                try {
                    com.schoolos.android.core.notification.SystemNotificationHelper.showNotification(
                        context = appContext,
                        notificationId = (id + navigateTo).hashCode(),
                        title = prefixFor(type, title),
                        message = message,
                        navigateTo = navigateTo,
                    )
                    shownThisCycle++
                } catch (e: Exception) {
                    Timber.w(e, "Poll show failed")
                }
                if (shownThisCycle >= 5) break
            }
            prefs.edit().putStringSet("shown_notif_ids", shown).apply()
        } catch (e: Exception) {
            Timber.w(e, "Poll parse failed")
        }
    }

    private fun navigateFor(type: String): String {
        val t = type.uppercase()
        return when {
            t.contains("MATERIAL") -> "materials"
            t.contains("ASSIGN") || t.contains("TUGAS") -> "assignments"
            t.contains("QUIZ") || t.contains("KUIS") || t.contains("CBT") -> "quizzes"
            t.contains("GRADE") || t.contains("NILAI") -> "grades"
            t.contains("SESSION") || t.contains("SESI") || t.contains("JADWAL") -> "sessions"
            t.contains("REMINDER") -> "schedule"
            else -> "notifications"
        }
    }

    private fun prefixFor(type: String, title: String): String {
        if (title.startsWith("📢") || title.startsWith("📚") || title.startsWith("📝") ||
            title.startsWith("💻") || title.startsWith("🏆") || title.startsWith("🔔") ||
            title.startsWith("🎓")
        ) return title
        val t = type.uppercase()
        val emoji = when {
            t.contains("MATERIAL") -> "📚"
            t.contains("ASSIGN") -> "📝"
            t.contains("QUIZ") || t.contains("CBT") -> "💻"
            t.contains("GRADE") || t.contains("NILAI") -> "🏆"
            t.contains("SESSION") || t.contains("JADWAL") -> "🎓"
            t.contains("REMINDER") -> "🔔"
            else -> "📢"
        }
        return "$emoji $title"
    }
}
