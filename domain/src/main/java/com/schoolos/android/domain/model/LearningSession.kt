package com.schoolos.android.domain.model

data class LearningSession(
    val id: String,
    val lessonId: String? = null,
    val sessionType: String = "scheduled",
    val scheduleId: String? = null,
    val subjectId: String? = null,
    val classId: String,
    val teacherId: String,
    val substituteTeacherId: String? = null,
    val substituteTeacherName: String? = null,
    val sessionDate: String? = null,
    val sessionNumber: Int = 1,
    val startTime: String? = null,
    val endTime: String? = null,
    val scheduledAt: String?,
    val startedAt: String?,
    val endedAt: String?,
    val status: String,
    val notes: String?,
    val cancellationReason: String? = null,
    val subjectName: String? = null,
    val teacherName: String? = null,
    val className: String? = null,
    val room: String? = null,
)

data class SessionAttendance(
    val id: String,
    val sessionId: String,
    val studentId: String,
    val status: String,
    val checkedInAt: String?,
    val notes: String?,
)
