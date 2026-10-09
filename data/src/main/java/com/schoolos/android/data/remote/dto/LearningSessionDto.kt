package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LearningSessionDto(
    val id: String,
    @SerialName("lesson_id") val lessonId: String? = null,
    @SerialName("session_type") val sessionType: String? = "scheduled",
    @SerialName("schedule_id") val scheduleId: String? = null,
    @SerialName("subject_id") val subjectId: String? = null,
    @SerialName("class_id") val classId: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("substitute_teacher_id") val substituteTeacherId: String? = null,
    @SerialName("substitute_teacher_name") val substituteTeacherName: String? = null,
    @SerialName("session_date") val sessionDate: String? = null,
    @SerialName("session_number") val sessionNumber: Int = 1,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("scheduled_at") val scheduledAt: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    val status: String,
    val notes: String? = null,
    @SerialName("cancellation_reason") val cancellationReason: String? = null,
    @SerialName("subject_name") val subjectName: String? = null,
    @SerialName("teacher_name") val teacherName: String? = null,
    @SerialName("class_name") val className: String? = null,
    @SerialName("room") val room: String? = null,
)

@Serializable
data class CancelSessionRequestDto(
    val reason: String? = null,
)

@Serializable
data class SubstituteTeacherRequestDto(
    @SerialName("substitute_teacher_id") val substituteTeacherId: String,
    val notes: String? = null,
)

@Serializable
data class SessionAttendanceDto(
    val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("student_id") val studentId: String,
    val status: String,
    @SerialName("checked_in_at") val checkedInAt: String? = null,
    val notes: String? = null,
)

@Serializable
data class RecordAttendanceRequestDto(
    @SerialName("student_id") val studentId: String,
    val status: String,
    @SerialName("checked_in_at") val checkedInAt: String? = null,
    val notes: String? = null,
)
