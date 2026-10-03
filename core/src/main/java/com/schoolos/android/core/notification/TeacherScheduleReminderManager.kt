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
        val reminderType = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: "1_HOUR_BEFORE"

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
 * Sends alerts:
 * 1. 1 hour before scheduled class time
 * 2. At class start time
 * Reminding teachers to prepare & publish materials, assignments, or quizzes.
 */
object TeacherScheduleReminderManager {

    const val TYPE_1_HOUR_BEFORE = "1_HOUR_BEFORE"
    const val TYPE_CLASS_START = "CLASS_START"

    private const val PREFS_NAME = "teacher_schedule_reminders"

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
                    LocalDate.parse(trimmed).atStartOfDay(zoneJakarta).toInstant().toEpochMilli()
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
        val todayStr = LocalDate.now().toString()
        val key = "${sessionId}_${reminderType}_$todayStr"
        return prefs.getBoolean(key, false)
    }

    private fun markAsNotified(context: Context, sessionId: String, reminderType: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = LocalDate.now().toString()
        val key = "${sessionId}_${reminderType}_$todayStr"
        prefs.edit().putBoolean(key, true).apply()
    }

    /**
     * Show notification immediately via SystemNotificationHelper with loud sound & vibration.
     */
    fun showReminderNotification(
        context: Context,
        sessionId: String,
        subjectName: String,
        className: String,
        scheduledTime: String,
        reminderType: String
    ) {
        if (isAlreadyNotified(context, sessionId, reminderType)) {
            Timber.d("Teacher reminder already notified today: %s (%s)", sessionId, reminderType)
            return
        }

        val title = if (reminderType == TYPE_1_HOUR_BEFORE) {
            "⏰ 1 Jam Lagi: Jadwal Mengajar $subjectName"
        } else {
            "🚨 Waktunya Mengajar: $subjectName • $className"
        }

        val message = if (reminderType == TYPE_1_HOUR_BEFORE) {
            "Jadwal mengajar kelas $className dimulai pukul $scheduledTime. Jangan lupa siapkan dan isi materi pembelajaran, tugas, atau kuis untuk murid Anda!"
        } else {
            "Jam mata pelajaran telah dimulai! Pastikan materi ajar, penugasan, atau kuis sudah diterbitkan ke murid."
        }

        val notifId = (sessionId + "_" + reminderType).hashCode() and 0x7FFFFFFF

        SystemNotificationHelper.showNotification(
            context = context,
            notificationId = notifId,
            title = title,
            message = message,
            navigateTo = if (reminderType == TYPE_1_HOUR_BEFORE) "learning" else "sessions",
            channelId = SystemNotificationHelper.CHANNEL_LEARNING_ID,
            category = "TEACHER_SCHEDULE_REMINDER"
        )

        markAsNotified(context, sessionId, reminderType)
        Timber.i("Teacher schedule notification posted: %s - %s", title, message)
    }

    /**
     * Schedule alarms and check immediate notifications for all of today's teacher sessions.
     */
    fun scheduleReminders(context: Context, sessions: List<LearningSession>) {
        if (sessions.isEmpty()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        for (session in sessions) {
            val scheduledMs = parseEpochMs(session.scheduledAt ?: session.startedAt) ?: continue
            val subject = session.subjectName?.takeIf { it.isNotBlank() && !it.contains("-") } ?: "Mata Pelajaran"
            val className = session.className?.takeIf { it.isNotBlank() && !it.contains("-") } ?: (session.room ?: "Kelas")
            val timeStr = formatHourMinute(scheduledMs)

            // 1. One Hour Before (scheduledMs - 60 mins)
            val oneHourBeforeMs = scheduledMs - 60 * 60 * 1000L
            if (oneHourBeforeMs > now) {
                // Future: Schedule AlarmManager alarm
                val intent = Intent(context, TeacherScheduleAlarmReceiver::class.java).apply {
                    action = TeacherScheduleAlarmReceiver.ACTION_REMINDER
                    putExtra(TeacherScheduleAlarmReceiver.EXTRA_SESSION_ID, session.id)
                    putExtra(TeacherScheduleAlarmReceiver.EXTRA_SUBJECT_NAME, subject)
                    putExtra(TeacherScheduleAlarmReceiver.EXTRA_CLASS_NAME, className)
                    putExtra(TeacherScheduleAlarmReceiver.EXTRA_SCHEDULED_TIME, timeStr)
                    putExtra(TeacherScheduleAlarmReceiver.EXTRA_REMINDER_TYPE, TYPE_1_HOUR_BEFORE)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    (session.id + "_1h").hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, oneHourBeforeMs, pi)
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, oneHourBeforeMs, pi)
                    }
                    Timber.d("Scheduled 1-hour teacher reminder for %s at %s", subject, oneHourBeforeMs)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to schedule 1-hour alarm for session %s", session.id)
                }
            } else if (now in oneHourBeforeMs until scheduledMs) {
                // Current time is within 1 hour before class start: notify immediately if not yet notified
                if (!isAlreadyNotified(context, session.id, TYPE_1_HOUR_BEFORE)) {
                    showReminderNotification(context, session.id, subject, className, timeStr, TYPE_1_HOUR_BEFORE)
                }
            }

            // 2. Class Start Time (scheduledMs)
            val startMs = scheduledMs
            if (startMs > now) {
                // Future: Schedule AlarmManager alarm
                val intent = Intent(context, TeacherScheduleAlarmReceiver::class.java).apply {
                    action = TeacherScheduleAlarmReceiver.ACTION_REMINDER
                    putExtra(TeacherScheduleAlarmReceiver.EXTRA_SESSION_ID, session.id)
                    putExtra(TeacherScheduleAlarmReceiver.EXTRA_SUBJECT_NAME, subject)
                    putExtra(TeacherScheduleAlarmReceiver.EXTRA_CLASS_NAME, className)
                    putExtra(TeacherScheduleAlarmReceiver.EXTRA_SCHEDULED_TIME, timeStr)
                    putExtra(TeacherScheduleAlarmReceiver.EXTRA_REMINDER_TYPE, TYPE_CLASS_START)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    (session.id + "_start").hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startMs, pi)
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, startMs, pi)
                    }
                    Timber.d("Scheduled class-start teacher reminder for %s at %s", subject, startMs)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to schedule class-start alarm for session %s", session.id)
                }
            } else if (session.status.equals("active", ignoreCase = true) || (now in startMs until (startMs + 2 * 3600_000L))) {
                // Class is starting now or active: notify immediately if not yet notified
                if (!isAlreadyNotified(context, session.id, TYPE_CLASS_START)) {
                    showReminderNotification(context, session.id, subject, className, timeStr, TYPE_CLASS_START)
                }
            }
        }
    }
}
