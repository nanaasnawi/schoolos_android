package com.schoolos.android.data.remote.dto

import com.schoolos.android.domain.model.AtRiskStudent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AtRiskStudentDto(
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String,
    val nisn: String? = null,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("class_name") val className: String,
    val category: String,
    val title: String,
    val description: String,
    @SerialName("risk_level") val riskLevel: String = "MEDIUM",
    @SerialName("material_id") val materialId: String? = null,
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("quiz_id") val quizId: String? = null,
    @SerialName("is_reminded") val isReminded: Boolean = false,
    @SerialName("reminded_at") val remindedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun toDomain(): AtRiskStudent {
        val label = when (category) {
            "UNREAD_MATERIAL" -> "Materi Tertinggal"
            "OVERDUE_ASSIGNMENT" -> "Tugas Tertunda"
            "LOW_SCORE" -> "Nilai Rendah"
            "QUIZ_ALERT" -> "Kuis & CBT"
            else -> "Perlu Perhatian"
        }
        return AtRiskStudent(
            studentId = studentId,
            studentName = studentName,
            nisn = nisn ?: "-",
            className = className,
            classId = classId,
            category = category,
            categoryLabel = label,
            riskLevel = riskLevel,
            title = title,
            description = description,
            materialId = materialId,
            assignmentId = assignmentId,
            quizId = quizId,
            isReminded = isReminded,
            remindedAt = remindedAt,
            updatedAt = updatedAt,
        )
    }
}

