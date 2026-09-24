package com.schoolos.android.data.repository

import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.data.mapper.toDomain
import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.domain.model.Achievement
import com.schoolos.android.domain.repository.AchievementRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepositoryImpl @Inject constructor(
    private val api: SchoolOsApi,
    private val authManager: AuthManager,
) : AchievementRepository {

    override suspend fun getAchievements(): Result<List<Achievement>> = runCatching {
        val state = authManager.authState.first()
        val targetStudentId = if (state.isParent) {
            state.childId ?: authManager.getChildId()
        } else {
            state.userId ?: authManager.getStudentId()
        }

        if (targetStudentId.isNullOrBlank()) {
            return@runCatching emptyList()
        }

        val response = api.getStudentAchievements(targetStudentId)
        response.data?.map { it.toDomain() }
            ?: throw Exception(response.error?.message ?: "Gagal memuat lencana pencapaian dari server API.")
    }

    override suspend fun getStudentAchievements(studentId: String): Result<List<Achievement>> = runCatching {
        if (studentId.isBlank()) return@runCatching emptyList()
        val response = api.getStudentAchievements(studentId.trim())
        response.data?.map { it.toDomain() }
            ?: throw Exception(response.error?.message ?: "Gagal memuat lencana pencapaian murid.")
    }
}
