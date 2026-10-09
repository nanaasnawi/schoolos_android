package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.GeneratedCurriculumResult
import com.schoolos.android.domain.model.GeneratedMaterialResult

interface CurriculumGeneratorRepository {
    suspend fun generateCurriculum(
        type: String, // 'ASSIGNMENT_STRUCTURED' | 'ASSIGNMENT_HOMEWORK' | 'QUIZ_MCQ_ONLY' | 'QUIZ_MCQ_ESSAY' | 'EXAM_MONTHLY'
        subjectId: String,
        subjectName: String,
        sourceMode: String = "LATEST_PUBLISHED", // 'LATEST_PUBLISHED' | 'PAST_MONTH'
        topic: String? = null,
        gradeLevel: String? = null,
    ): Result<GeneratedCurriculumResult>

    suspend fun generateMaterial(
        mode: String, // "INFOGRAPHIC" | "ARTICLE"
        topic: String,
        gradeLevel: String = "Kelas 5 SD",
        subjectName: String,
    ): Result<GeneratedMaterialResult>
}
