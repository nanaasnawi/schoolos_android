package com.schoolos.android.domain.model

data class GradeEntry(
    val id: String,
    val studentId: String,
    val classId: String,
    val subjectId: String,
    val componentName: String,
    val sourceType: String,
    val rawScore: Double?,
    val maxRawScore: Double?,
    val weightedScore: Double?,
    val weightPercentage: Double?,
    val calculatedAt: String,
)

data class SubjectGradeSummary(
    val subjectId: String,
    val subjectName: String,
    val finalScore: Double,
    val letterGrade: String,
    val completionPercentage: Double,
    val lastCalculated: String,
    val componentCount: Int,
    val gradedComponentCount: Int,
    val passingThreshold: Double = 75.0,
    val isPassed: Boolean = finalScore >= passingThreshold,
    val category: String = "Wajib",
)

data class SubjectGradeDetail(
    val summary: SubjectGradeSummary,
    val components: List<GradeEntry>,
    val weightBreakdown: List<WeightComponent>,
)

data class WeightComponent(
    val name: String,
    val weightPercentage: Double,
    val score: Double?,
    val maxScore: Double?,
)

fun List<GradeEntry>.toSubjectSummary(
    subjectId: String,
    subjectName: String,
    backendFinalScore: Double? = null,
    backendLetterGrade: String? = null,
    passingThreshold: Double = 75.0,
    category: String = "Wajib",
): SubjectGradeSummary {
    val entries = this.filter { it.subjectId == subjectId }
    val graded = entries.filter { it.rawScore != null || it.weightedScore != null }
    
    val finalScore = backendFinalScore 
        ?: entries.firstOrNull { it.weightedScore != null }?.weightedScore
        ?: graded.mapNotNull { it.rawScore }.average().takeIf { !it.isNaN() }
        ?: 0.0

    val letter = backendLetterGrade ?: when {
        finalScore >= passingThreshold + 10.0 -> "A"
        finalScore >= passingThreshold -> "B"
        finalScore >= passingThreshold - 10.0 -> "C"
        finalScore > 0.0 -> "D"
        else -> "-"
    }
    val completion = if (entries.isEmpty()) 0.0 else (graded.size.toDouble() / entries.size) * 100.0
    return SubjectGradeSummary(
        subjectId = subjectId,
        subjectName = subjectName,
        finalScore = finalScore,
        letterGrade = letter,
        completionPercentage = completion,
        lastCalculated = graded.maxOfOrNull { it.calculatedAt }.orEmpty(),
        componentCount = entries.size,
        gradedComponentCount = graded.size,
        passingThreshold = passingThreshold,
        isPassed = finalScore >= passingThreshold,
        category = category,
    )
}

