package com.schoolos.android.data.repository

import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.data.mapper.toDomain
import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.domain.model.GamificationStatus
import com.schoolos.android.domain.repository.GamificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationRepositoryImpl @Inject constructor(
    private val api: SchoolOsApi,
    private val authManager: AuthManager,
) : GamificationRepository {

    override suspend fun getGamificationStatus(studentId: String?): Result<GamificationStatus> = runCatching {
        val targetId = if (!studentId.isNullOrBlank()) {
            studentId.trim()
        } else {
            val childId = authManager.getChildId()
            if (!childId.isNullOrBlank()) childId else null
        }

        val response = api.getGamificationStatus(studentId = targetId)
        val dto = response.data
        if (dto != null) {
            return@runCatching dto.toDomain()
        }
        throw Exception(response.error?.message ?: "Gagal memuat status gamifikasi level & XP.")
    }
}
