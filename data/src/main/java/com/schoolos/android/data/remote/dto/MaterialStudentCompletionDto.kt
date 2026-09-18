package com.schoolos.android.data.remote.dto

import com.schoolos.android.domain.model.MaterialStudentCompletion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MaterialStudentCompletionDto(
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String,
    val nisn: String? = null,
    val gender: String? = null,
    @SerialName("class_name") val className: String? = null,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("last_read_at") val lastReadAt: String? = null,
)

fun MaterialStudentCompletionDto.toDomain() = MaterialStudentCompletion(
    studentId = studentId,
    studentName = studentName,
    nisn = nisn,
    gender = gender,
    className = className,
    isCompleted = isCompleted,
    completedAt = completedAt,
    currentPage = currentPage,
    lastReadAt = lastReadAt,
)
