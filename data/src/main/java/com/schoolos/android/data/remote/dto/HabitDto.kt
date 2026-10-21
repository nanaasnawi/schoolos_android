package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class HabitEntryDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String? = null,
    @SerialName("class_name") val className: String? = null,
    @SerialName("entry_date") val entryDate: String,
    @SerialName("academic_year") val academicYear: String = "2026/2027",
    val semester: String = "GANJIL",
    val habits: JsonObject? = null,
    @SerialName("habits_notes") val habitsNotes: JsonObject? = null,
    @SerialName("completed_count") val completedCount: Int = 0,
    @SerialName("compliance_rate") val complianceRate: Double = 0.0,
    @SerialName("verification_status") val verificationStatus: String = "PENDING",
    @SerialName("verified_by_user_id") val verifiedByUserId: String? = null,
    @SerialName("verified_by_role") val verifiedByRole: String? = null,
    @SerialName("verified_at") val verifiedAt: String? = null,
    @SerialName("parent_feedback") val parentFeedback: String? = null,
    @SerialName("teacher_notes") val teacherNotes: String? = null,
    @SerialName("override_reason") val overrideReason: String? = null,
)

@Serializable
data class RecordHabitEntryRequestDto(
    @SerialName("student_id") val studentId: String,
    @SerialName("entry_date") val entryDate: String,
    @SerialName("academic_year") val academicYear: String? = null,
    val semester: String? = null,
    val habits: JsonObject,
    @SerialName("habits_notes") val habitsNotes: JsonObject? = null,
)

@Serializable
data class VerifyHabitParentRequestDto(
    @SerialName("entry_id") val entryId: String,
    @SerialName("parent_feedback") val parentFeedback: String? = null,
)

@Serializable
data class RecordHabitResponseDto(
    val id: String = "",
    @SerialName("student_id") val studentId: String = "",
    @SerialName("entry_date") val entryDate: String = "",
    @SerialName("completed_count") val completedCount: Int = 0,
    @SerialName("compliance_rate") val complianceRate: Double = 0.0,
    @SerialName("verification_status") val verificationStatus: String = "",
    val message: String = "",
)

@Serializable
data class VerifyHabitResponseDto(
    val id: String = "",
    @SerialName("student_id") val studentId: String = "",
    @SerialName("verification_status") val verificationStatus: String = "",
    val message: String = "",
)
