package com.schoolos.android.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.schoolos.android.domain.model.LearningSession
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * BroadcastReceiver triggered by AlarmManager to post teaching schedule reminder notifications.
 */
class TeacherScheduleAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER = "com.schoolos.android.ACTION_TEACHER_SCHEDULE_REMINDER"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_SUBJECT_NAME = "extra_subject_name"
        const val EXTRA_CLASS_NAME = "extra_class_name"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != ACTION_REMINDER) return
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Mata Pelajaran"
        val className = intent.getStringExtra(EXTRA_CLASS_NAME) ?: "Kelas"
        val scheduledTime = intent.getStringExtra(EXTRA_SCHEDULED_TIME) ?: "Hari ini"
        val reminderType = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: TeacherScheduleReminderManager.TYPE_15_MINUTES_BEFORE

        // Quiet hours protection: 21:00 to 06:00 WIB
        if (TeacherScheduleReminderManager.isQuietHours()) {
            Timber.d("TeacherScheduleAlarmReceiver: Quiet hours active (21:00 - 06:00 WIB). Dropping alarm.")
            return
        }

        Timber.d("TeacherScheduleAlarmReceiver fired: %s for %s (%s)", reminderType, subjectName, className)

        TeacherScheduleReminderManager.showReminderNotification(
            context = context.applicationContext,
            sessionId = sessionId,
            subjectName = subjectName,
            className = className,
            scheduledTime = scheduledTime,
            reminderType = reminderType
        )
    }
}

/**
 * Manages alarm scheduling and notifications for teacher daily teaching sessions.
 * Sends alerts strictly 15 minutes before scheduled class time.
 * Reminding teachers to prepare & publish materials, assignments, or quizzes for their assigned classes.
 */
object TeacherScheduleReminderManager {

    const val TYPE_15_MINUTES_BEFORE = "15_MINUTES_BEFORE"
    // Backward compatibility constants
    const val TYPE_1_HOUR_BEFORE = "1_HOUR_BEFORE"
    const val TYPE_CLASS_START = "CLASS_START"

    private const val PREFS_NAME = "teacher_schedule_reminders"

    /**
     * Strict check for quiet hours: 21:00 WIB (night) until 06:00 WIB (morning).
     * No alarms or loud notifications must ever trigger during quiet hours.
     */
    fun isQuietHours(): Boolean {
        return try {
            val zoneJakarta = ZoneId.of("Asia/Jakarta")
            val hour = ZonedDateTime.now(zoneJakarta).hour
            hour >= 21 || hour < 6
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Computes exact epoch millis of when a teaching session starts in Asia/Jakarta (WIB).
     * Rejects 00:00:00 midnight fallbacks so alarms NEVER fire at midnight.
     */
    fun getSessionStartEpochMs(session: LearningSession): Long? {
        val zoneJakarta = ZoneId.of("Asia/Jakarta")
        val today = LocalDate.now(zoneJakarta)

        // 1. Prioritize session.startTime (e.g. "07:30", "08:15", "08:00:00")
        val rawTime = session.startTime?.trim()
        if (!rawTime.isNullOrBlank()) {
            try {
                val cleanTime = rawTime.substringBefore(" ").take(5)
                val parts = cleanTime.split(":")
                if (parts.size >= 2) {
                    val hour = parts[0].toIntOrNull()
                    val minute = parts[1].toIntOrNull()
                    if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                        val sessionDate = session.sessionDate?.let {
                            try { LocalDate.parse(it.substringBefore("T")) } catch (_: Exception) { today }
                        } ?: today
                        return sessionDate.atTime(hour, minute).atZone(zoneJakarta).toInstant().toEpochMilli()
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Fallback to scheduledAt or startedAt
        val iso = session.scheduledAt ?: session.startedAt
        if (!iso.isNullOrBlank()) {
            val ms = parseEpochMs(iso)
            if (ms != null) {
                val zdt = Instant.ofEpochMilli(ms).atZone(zoneJakarta)
                // Guard: If it parsed to midnight (00:00:00), it's date-only and lacks time!
                // Midnight classes do not exist in schools; reject to prevent 00:21 WIB alarms.
                if (zdt.hour == 0 && zdt.minute == 0) {
                    return null
                }
                return ms
            }
        }

        return null
    }

    fun parseEpochMs(isoString: String?): Long? {
        if (isoString.isNullOrBlank()) return null
        val trimmed = isoString.trim()
        return try {
            val zoneJakarta = ZoneId.of("Asia/Jakarta")
            when {
                trimmed.toLongOrNull() != null -> trimmed.toLong()
                trimmed.endsWith("Z") || trimmed.contains("+") || (trimmed.contains("-") && trimmed.count { it == '-' } > 2) -> {
                    try {
                        OffsetDateTime.parse(trimmed).toInstant().toEpochMilli()
                    } catch (_: Exception) {
                        Instant.parse(trimmed).toEpochMilli()
                    }
                }
                trimmed.contains("T") -> {
                    val clean = trimmed.substringBefore(".")
                    LocalDateTime.parse(clean).atZone(zoneJakarta).toInstant().toEpochMilli()
                }
                trimmed.contains(" ") -> {
                    val clean = trimmed.replace(' ', 'T').substringBefore(".")
                    LocalDateTime.parse(clean).atZone(zoneJakarta).toInstant().toEpochMilli()
                }
                trimmed.length == 10 && trimmed.count { it == '-' } == 2 -> {
                    // Date-only without time: DO NOT default to midnight for alarms
                    null
                }
                else -> Instant.parse(trimmed).toEpochMilli()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun formatHourMinute(epochMs: Long): String {
        return try {
            val instant = Instant.ofEpochMilli(epochMs)
            val zdt = instant.atZone(ZoneId.of("Asia/Jakarta"))
            val formatter = DateTimeFormatter.ofPattern("HH:mm 'WIB'", Locale("id", "ID"))
            zdt.format(formatter)
        } catch (_: Exception) {
            "Hari ini"
        }
    }

    private fun isAlreadyNotified(context: Context, sessionId: String, reminderType: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = LocalDate.now(ZoneId.of("Asia/Jakarta")).toString()
        val key = "${sessionId}_${reminderType}_$todayStr"
        return prefs.getBoolean(key, false)
    }

    private fun markAsNotified(context: Context, sessionId: String, reminderType: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = LocalDate.now(ZoneId.of("Asia/Jakarta")).toString()
        val key = "${sessionId}_${reminderType}_$todayStr"
        prefs.edit().putBoolean(key, true).apply()
    }

    /**
     * Show notification immediately via SystemNotificationHelper with sound & vibration.
     * Guaranteed to never play during quiet hours (21:00 - 06:00 WIB).
     */
    fun showReminderNotification(
        context: Context,
        sessionId: String,
        subjectName: String,
        className: String,
        scheduledTime: String,
        reminderType: String
    ) {
        try {
            if (isQuietHours()) {
                Timber.d("Quiet hours active (21:00 - 06:00 WIB). Suppressing reminder.")
                return
            }

            if (isAlreadyNotified(context, sessionId, reminderType)) {
                Timber.d("Teacher reminder already notified today: %s (%s)", sessionId, reminderType)
                return
            }

            val title = "15 Menit Lagi: Jadwal Sesi Modul $subjectName"
            val message = "Jadwal pembelajaran rombel $className dimulai pukul $scheduledTime. Silakan persiapkan modul materi pembelajaran atau penugasan untuk warga belajar."

            val notifId = (sessionId + "_" + reminderType).hashCode() and 0x7FFFFFFF

            SystemNotificationHelper.showNotification(
                context = context,
                notificationId = notifId,
                title = title,
                message = message,
                navigateTo = "learning",
                channelId = SystemNotificationHelper.CHANNEL_LEARNING_ID,
                category = "TEACHER_SCHEDULE_REMINDER"
            )

            markAsNotified(context, sessionId, reminderType)
            Timber.i("Teacher schedule notification posted: %s - %s", title, message)
        } catch (t: Throwable) {
            Timber.w(t, "Failed to show reminder notification safely: %s", t.message)
        }
    }

    /**
     * Schedule alarms strictly 15 minutes before teaching sessions start.
     * Only schedules for today's active teaching sessions belonging to the teacher.
     */
    fun scheduleReminders(context: Context, sessions: List<LearningSession>) {
        if (sessions.isEmpty()) return
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val now = System.currentTimeMillis()
            val zoneJakarta = ZoneId.of("Asia/Jakarta")
            val today = LocalDate.now(zoneJakarta)

            for (session in sessions) {
                // Ignore completed or cancelled sessions
                if (session.status.equals("completed", ignoreCase = true) ||
                    session.status.equals("cancelled", ignoreCase = true)
                ) {
                    continue
                }

                val scheduledMs = getSessionStartEpochMs(session) ?: continue
                val sessionDate = Instant.ofEpochMilli(scheduledMs).atZone(zoneJakarta).toLocalDate()
                // Only process sessions scheduled for TODAY
                if (sessionDate != today) continue

                val subject = session.subjectName?.takeIf { it.isNotBlank() && !it.contains("-") } ?: "Mata Pelajaran"
                val className = session.className?.takeIf { it.isNotBlank() && !it.contains("-") } ?: (session.room ?: "Kelas")
                val timeStr = formatHourMinute(scheduledMs)

                // 15 Minutes Before Start Time
                val fifteenMinsBeforeMs = scheduledMs - 15 * 60 * 1000L

                if (fifteenMinsBeforeMs > now) {
                    // Future schedule: set exact alarm
                    val intent = Intent(context, TeacherScheduleAlarmReceiver::class.java).apply {
                        action = TeacherScheduleAlarmReceiver.ACTION_REMINDER
                        putExtra(TeacherScheduleAlarmReceiver.EXTRA_SESSION_ID, session.id)
                        putExtra(TeacherScheduleAlarmReceiver.EXTRA_SUBJECT_NAME, subject)
                        putExtra(TeacherScheduleAlarmReceiver.EXTRA_CLASS_NAME, className)
                        putExtra(TeacherScheduleAlarmReceiver.EXTRA_SCHEDULED_TIME, timeStr)
                        putExtra(TeacherScheduleAlarmReceiver.EXTRA_REMINDER_TYPE, TYPE_15_MINUTES_BEFORE)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context,
                        (session.id + "_15m").hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    safeScheduleAlarm(am, fifteenMinsBeforeMs, pi)
                } else if (now in fifteenMinsBeforeMs until scheduledMs) {
                    // Currently in the 15-minute preparation window right before class starts
                    if (!isAlreadyNotified(context, session.id, TYPE_15_MINUTES_BEFORE) && !isQuietHours()) {
                        showReminderNotification(context, session.id, subject, className, timeStr, TYPE_15_MINUTES_BEFORE)
                    }
                }
                // Note: If now >= scheduledMs, class has already started or ended. Do not sound preparation alarm!
            }
        } catch (t: Throwable) {
            Timber.w(t, "Teacher schedule reminders check failed safely: %s", t.message)
        }
    }

    private fun safeScheduleAlarm(am: AlarmManager, timeMs: Long, pi: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pi)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pi)
            } else {
                @Suppress("DEPRECATION")
                am.set(AlarmManager.RTC_WAKEUP, timeMs, pi)
            }
        } catch (e: SecurityException) {
            Timber.w(e, "Exact alarm permission missing, falling back to allow-while-idle")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pi)
                }
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Timber.w(e, "Failed to schedule alarm")
        }
    }
}
