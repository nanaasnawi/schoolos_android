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
}
