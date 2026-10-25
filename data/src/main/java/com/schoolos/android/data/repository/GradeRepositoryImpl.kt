package com.schoolos.android.data.repository

import com.schoolos.android.core.database.dao.GradeDao
import com.schoolos.android.core.database.mapper.toDomain as entityToDomain
import com.schoolos.android.core.database.mapper.toEntity
import com.schoolos.android.data.mapper.toDomain
import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.domain.model.GradeEntry
import com.schoolos.android.domain.repository.GradeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeRepositoryImpl @Inject constructor(
    private val api: SchoolOsApi,
    private val gradeDao: GradeDao,
) : GradeRepository {

    override suspend fun getGradebook(classId: String?, subjectId: String?): Result<List<GradeEntry>> = runCatching {
        try {
            val response = api.getGradebook(classId?.ifBlank { null }, subjectId?.ifBlank { null })
            val grades = response.data?.map { it.toDomain() }
            if (grades != null) {
                gradeDao.insertAll(grades.map { it.toEntity() })
                return@runCatching grades
            }
        } catch (e: Exception) {
            android.util.Log.w("GradeRepo", "Remote getGradebook failed, falling back to cache: ${e.message}")
        }
        val cached = try {
            if (!classId.isNullOrBlank() && !subjectId.isNullOrBlank()) {
                gradeDao.getGradesBySubject(classId, subjectId).first()
            } else {
                gradeDao.getGrades().first()
            }
        } catch (_: Exception) {
            emptyList()
        }
        cached.map { it.entityToDomain() }
    }

    override suspend fun getGradebookSummary(
        classId: String?,
        subjectId: String?,
        studentId: String?
    ): Result<List<com.schoolos.android.domain.model.GradebookSummaryItem>> = runCatching {
        val res = api.getGradebookSummary(
            classId = classId?.ifBlank { null },
            subjectId = subjectId?.ifBlank { null },
            studentId = studentId?.ifBlank { null }
        )
        res.data?.map { dto ->
            com.schoolos.android.domain.model.GradebookSummaryItem(
                studentId = dto.studentId,
                classId = dto.classId,
                subjectId = dto.subjectId,
                subjectName = dto.subjectName,
                sumatifM1 = dto.sumatifM1,
                sumatifM2 = dto.sumatifM2,
                sumatifM3 = dto.sumatifM3,
                sumatifM4 = dto.sumatifM4,
                avgSumatif = dto.avgSumatif,
                ats = dto.ats,
                sas = dto.sas,
                finalScore = dto.finalScore,
                letterGrade = dto.letterGrade,
                passed = dto.passed ?: false,
                status = dto.status,
                highestCompetencyDesc = dto.highestCompetencyDesc,
                lowestCompetencyDesc = dto.lowestCompetencyDesc,
                formatifNotes = dto.formatifNotes
            )
        } ?: emptyList()
    }

    override suspend fun saveGradebook(
        classId: String?,
        subjectId: String?,
        subjectName: String?,
        weightSumatif: Double,
        weightAts: Double,
        weightSas: Double,
        kktp: Double,
        grades: List<com.schoolos.android.domain.model.StudentGradeInputItem>
    ): Result<Boolean> = runCatching {
        val req = com.schoolos.android.data.remote.dto.SaveGradebookRequestDto(
            classId = classId?.ifBlank { null },
            subjectId = subjectId?.ifBlank { null },
            subjectName = subjectName?.ifBlank { null },
            weightSumatif = weightSumatif,
            weightAts = weightAts,
            weightSas = weightSas,
            kktp = kktp,
            grades = grades.map { g ->
                com.schoolos.android.data.remote.dto.StudentGradeInputDto(
                    studentId = g.studentId,
                    sumatifM1 = g.sumatifM1,
                    sumatifM2 = g.sumatifM2,
                    sumatifM3 = g.sumatifM3,
                    sumatifM4 = g.sumatifM4,
                    ats = g.ats,
                    sas = g.sas,
                    formatifNotes = g.formatifNotes,
                    finalScore = g.finalScore,
                    letterGrade = g.letterGrade,
                    passed = g.passed,
                    status = g.status ?: "published"
                )
            }
        )
        val res = api.saveGradebook(req)
        res.data ?: true
    }
}
