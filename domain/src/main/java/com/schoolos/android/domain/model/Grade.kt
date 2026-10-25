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
    
    // SSOT: Perhitungan nilai rapor mutlak dieksekusi oleh backend Rust Axum (ADR-0011).
    // Client Android hanya memvisualisasikan data dari server tanpa formula bayangan.
    val finalScore = backendFinalScore 
        ?: entries.firstOrNull { it.weightedScore != null }?.weightedScore
        ?: 0.0

    val letter = backendLetterGrade ?: if (finalScore > 0.0) "" else "-"
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

data class GradebookSummaryItem(
    val studentId: String,
    val classId: String? = null,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val sumatifM1: Double? = null,
    val sumatifM2: Double? = null,
    val sumatifM3: Double? = null,
    val sumatifM4: Double? = null,
    val avgSumatif: Double? = null,
    val ats: Double? = null,
    val sas: Double? = null,
    val finalScore: Double? = null,
    val letterGrade: String? = null,
    val passed: Boolean = false,
    val status: String? = null,
    val highestCompetencyDesc: String? = null,
    val lowestCompetencyDesc: String? = null,
    val formatifNotes: String? = null,
)

data class StudentGradeInputItem(
    val studentId: String,
    val sumatifM1: Double? = null,
    val sumatifM2: Double? = null,
    val sumatifM3: Double? = null,
    val sumatifM4: Double? = null,
    val ats: Double? = null,
    val sas: Double? = null,
    val formatifNotes: String? = null,
    val finalScore: Double? = null,
    val letterGrade: String? = null,
    val passed: Boolean? = null,
    val status: String? = "published",
)

data class GradeEvalResult(
    val avgSumatif: Double,
    val finalScore: Double,
    val letterGrade: String,
    val statusKktp: String, // "Tuntas KKTP" | "Perlu Remedial" | "Belum Diinput"
)

fun computeKurikulumMerdekaGrade(
    m1: Double,
    m2: Double,
    m3: Double,
    atsVal: Double,
    sasVal: Double,
    scheme: String = "A", // "A" (60/20/20) or "B" (100% Modul Kesetaraan)
    kktpThreshold: Double = 75.0,
): GradeEvalResult {
    val activeModules = listOf(m1, m2, m3).filter { it > 0.0 }
    val avgM = if (activeModules.isNotEmpty()) activeModules.average() else 0.0

    if (activeModules.isEmpty() && atsVal == 0.0 && sasVal == 0.0) {
        return GradeEvalResult(avgSumatif = 0.0, finalScore = 0.0, letterGrade = "-", statusKktp = "Belum Diinput")
    }

    val calculated = if (scheme == "B") {
        if (avgM > 0.0) avgM else maxOf(atsVal, sasVal)
    } else {
        if (atsVal > 0.0 && sasVal > 0.0) {
            avgM * 0.6 + atsVal * 0.2 + sasVal * 0.2
        } else if (atsVal > 0.0) {
            avgM * 0.6 + atsVal * 0.4
        } else if (sasVal > 0.0) {
            avgM * 0.6 + sasVal * 0.4
        } else {
            avgM
        }
    }

    val roundedNa = kotlin.math.round(calculated)
    val pred = when {
        roundedNa >= 88 -> "A"
        roundedNa >= 75 -> "B"
        roundedNa >= 65 -> "C"
        else -> "D"
    }
    val status = if (roundedNa >= kktpThreshold) "Tuntas KKTP" else "Perlu Remedial"

    return GradeEvalResult(
        avgSumatif = kotlin.math.round(avgM * 10.0) / 10.0,
        finalScore = roundedNa,
        letterGrade = pred,
        statusKktp = status
    )
}

