package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.GradeEntry
import com.schoolos.android.domain.model.GradebookSummaryItem
import com.schoolos.android.domain.model.StudentGradeInputItem

interface GradeRepository {
    suspend fun getGradebook(classId: String? = null, subjectId: String? = null): Result<List<GradeEntry>>
    suspend fun getGradebookSummary(classId: String? = null, subjectId: String? = null, studentId: String? = null): Result<List<GradebookSummaryItem>>
    suspend fun saveGradebook(
        classId: String? = null,
        subjectId: String? = null,
        subjectName: String? = null,
        weightSumatif: Double = 0.6,
        weightAts: Double = 0.2,
        weightSas: Double = 0.2,
        kktp: Double = 75.0,
        grades: List<StudentGradeInputItem>
    ): Result<Boolean>
}
