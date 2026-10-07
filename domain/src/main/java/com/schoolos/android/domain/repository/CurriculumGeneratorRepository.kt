package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.GeneratedCurriculumResult

interface CurriculumGeneratorRepository {
    suspend fun generateCurriculum(
        type: String, // 'ASSIGNMENT_STRUCTURED' | 'ASSIGNMENT_HOMEWORK' | 'QUIZ_MCQ_ONLY' | 'QUIZ_MCQ_ESSAY' | 'EXAM_MONTHLY'
        subjectId: String,
        subjectName: String,
        sourceMode: String = "LATEST_PUBLISHED", // 'LATEST_PUBLISHED' | 'PAST_MONTH'
    ): Result<GeneratedCurriculumResult>
}
