package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleComplianceResponseDto(
    val date: String,
    @SerialName("total_scheduled") val totalScheduled: Long,
    @SerialName("completed_count") val completedCount: Long,
    @SerialName("in_progress_count") val inProgressCount: Long,
    @SerialName("scheduled_count") val scheduledCount: Long,
    @SerialName("substituted_count") val substitutedCount: Long,
    @SerialName("cancelled_count") val cancelledCount: Long,
    @SerialName("overdue_unrecorded_count") val overdueUnrecordedCount: Long,
    @SerialName("compliance_rate") val complianceRate: Double,
    val items: List<ComplianceItemDto> = emptyList(),
)

@Serializable
data class ComplianceItemDto(
    @SerialName("schedule_id") val scheduleId: String,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("class_id") val classId: String,
    @SerialName("class_name") val className: String,
    @SerialName("subject_id") val subjectId: String,
    @SerialName("subject_name") val subjectName: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("teacher_name") val teacherName: String,
    @SerialName("substitute_teacher_id") val substituteTeacherId: String? = null,
    @SerialName("substitute_teacher_name") val substituteTeacherName: String? = null,
    @SerialName("day_of_week") val dayOfWeek: Int,
    val date: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val room: String? = null,
    val state: String, // SCHEDULED, IN_PROGRESS, COMPLETED, SUBSTITUTED, CANCELLED, OVERDUE_UNRECORDED
    @SerialName("attendance_count") val attendanceCount: Long = 0,
    val notes: String? = null,
)
