package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GradeEntryDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("class_id") val classId: String,
    @SerialName("subject_id") val subjectId: String,
    @SerialName("component_name") val componentName: String,
    @SerialName("source_type") val sourceType: String,
    @SerialName("raw_score") val rawScore: Double? = null,
    @SerialName("max_raw_score") val maxRawScore: Double? = null,
    @SerialName("weighted_score") val weightedScore: Double? = null,
    @SerialName("weight_percentage") val weightPercentage: Double? = null,
    @SerialName("calculated_at") val calculatedAt: String,
)

@Serializable
data class GradebookSummaryDto(
    @SerialName("student_id") val studentId: String,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("subject_id") val subjectId: String? = null,
    @SerialName("subject_name") val subjectName: String? = null,
    @SerialName("sumatif_m1") val sumatifM1: Double? = null,
    @SerialName("sumatif_m2") val sumatifM2: Double? = null,
    @SerialName("sumatif_m3") val sumatifM3: Double? = null,
    @SerialName("sumatif_m4") val sumatifM4: Double? = null,
    @SerialName("avg_sumatif") val avgSumatif: Double? = null,
    @SerialName("ats") val ats: Double? = null,
    @SerialName("sas") val sas: Double? = null,
    @SerialName("final_score") val finalScore: Double? = null,
    @SerialName("letter_grade") val letterGrade: String? = null,
    @SerialName("passed") val passed: Boolean? = false,
    @SerialName("status") val status: String? = null,
    @SerialName("highest_competency_desc") val highestCompetencyDesc: String? = null,
    @SerialName("lowest_competency_desc") val lowestCompetencyDesc: String? = null,
    @SerialName("formatif_notes") val formatifNotes: String? = null,
)

@Serializable
data class SaveGradebookRequestDto(
    @SerialName("class_id") val classId: String? = null,
    @SerialName("subject_id") val subjectId: String? = null,
    @SerialName("subject_name") val subjectName: String? = null,
    @SerialName("weight_sumatif") val weightSumatif: Double? = 0.6,
    @SerialName("weight_ats") val weightAts: Double? = 0.2,
    @SerialName("weight_sas") val weightSas: Double? = 0.2,
    @SerialName("kktp") val kktp: Double? = 75.0,
    @SerialName("grades") val grades: List<StudentGradeInputDto>,
)

@Serializable
data class StudentGradeInputDto(
    @SerialName("student_id") val studentId: String,
    @SerialName("sumatif_m1") val sumatifM1: Double? = null,
    @SerialName("sumatif_m2") val sumatifM2: Double? = null,
    @SerialName("sumatif_m3") val sumatifM3: Double? = null,
    @SerialName("sumatif_m4") val sumatifM4: Double? = null,
    @SerialName("ats") val ats: Double? = null,
    @SerialName("sas") val sas: Double? = null,
    @SerialName("formatif_notes") val formatifNotes: String? = null,
    @SerialName("final_score") val finalScore: Double? = null,
    @SerialName("letter_grade") val letterGrade: String? = null,
    @SerialName("passed") val passed: Boolean? = null,
    @SerialName("status") val status: String? = "published",
)
