package com.schoolos.android.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Fallback polling: saat Doze menahan FCM data-message atau user menonaktifkan
 * background data, AlarmManager membangunkan aplikasi berkala untuk menarik
 * /notifications yang belum dibaca lalu menampilkannya sebagai notifikasi sistem.
 */
class NotificationPollReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_POLL = "com.schoolos.android.ACTION_POLL_NOTIFICATIONS"
        private const val REQ_CODE = 9001
        private const val INTERVAL_MS = 15 * 60 * 1000L

        fun schedule(context: Context) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, NotificationPollReceiver::class.java).apply { action = ACTION_POLL }
                val pi = PendingIntent.getBroadcast(
                    context, REQ_CODE, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                am.cancel(pi)
                val first = System.currentTimeMillis() + 60_000L
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, first, pi)
                } else {
                    @Suppress("DEPRECATION")
                    am.setRepeating(AlarmManager.RTC_WAKEUP, first, INTERVAL_MS, pi)
                }
                // Inexact repeating sebagai jaring pengaman tambahan (tetap jalan di Doze window).
                try {
                    am.setInexactRepeating(AlarmManager.RTC_WAKEUP, first + INTERVAL_MS, INTERVAL_MS, pi)
                } catch (_: Exception) {}
                Timber.d("Notification poll alarm scheduled")
            } catch (e: Exception) {
                Timber.w(e, "Failed to schedule notification poll")
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                NotificationPollWorker.pollOnce(appContext)
            } catch (e: Exception) {
                Timber.w(e, "Notification poll failed")
            } finally {
                // Jadwalkan alarm exact berikutnya agar tetap bangun saat idle.
                try {
                    val am = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    val next = Intent(appContext, NotificationPollReceiver::class.java).apply { action = ACTION_POLL }
                    val pi = PendingIntent.getBroadcast(
                        appContext, REQ_CODE, next,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am?.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + INTERVAL_MS,
                            pi,
                        )
                    }
                } catch (_: Exception) {}
                try {
                    pendingResult.finish()
                } catch (_: Exception) {}
            }
        }
    }
}
