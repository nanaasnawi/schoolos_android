package com.schoolos.android.data.repository

import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.data.mapper.toDomain
import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.data.remote.dto.AwardXpRequestDto
import com.schoolos.android.data.remote.dto.SubmitDailyQuestRequestDto
import com.schoolos.android.domain.model.AwardXpResult
import com.schoolos.android.domain.model.DailyQuest
import com.schoolos.android.domain.model.DailyQuestSubmitResult
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

    override suspend fun awardXp(
        actionType: String,
        referenceId: String?,
        description: String?,
        studentId: String?,
    ): Result<AwardXpResult> = runCatching {
        val targetId = if (!studentId.isNullOrBlank()) {
            studentId.trim()
        } else {
            val childId = authManager.getChildId()
            if (!childId.isNullOrBlank()) childId else null
        }

        val request = AwardXpRequestDto(
            studentId = targetId,
            actionType = actionType,
            referenceId = referenceId,
            description = description,
        )
        val response = api.awardStudentXp(request)
        val dto = response.data
        if (dto != null) {
            AwardXpResult(
                awarded = dto.awarded,
                alreadyAwarded = dto.alreadyAwarded,
                xpEarned = dto.xpEarned,
                totalXp = dto.totalXp,
                level = dto.level,
                streakDays = dto.streakDays,
                leveledUp = dto.leveledUp,
                message = dto.message,
            )
        } else {
            throw Exception(response.error?.message ?: "Gagal memproses perolehan XP.")
        }
    }

    override suspend fun getDailyQuest(mode: String?): Result<DailyQuest> = runCatching {
        val response = api.getDailyQuest(mode = mode)
        val dto = response.data
        if (dto != null) {
            return@runCatching dto.toDomain()
        }
        throw Exception(response.error?.message ?: "Gagal memuat misi harian dinamis.")
    }

    override suspend fun submitDailyQuest(
        questId: String,
        correctCount: Int,
        totalCount: Int,
        maxCombo: Int,
        durationSeconds: Int,
    ): Result<DailyQuestSubmitResult> = runCatching {
        val response = api.submitDailyQuest(
            SubmitDailyQuestRequestDto(
                questId = questId,
                correctCount = correctCount,
                totalCount = totalCount,
                maxCombo = maxCombo,
                durationSeconds = durationSeconds,
            )
        )
        val dto = response.data
        if (dto != null) {
            return@runCatching dto.toDomain()
        }
        throw Exception(response.error?.message ?: "Gagal menyimpan hasil misi harian.")
    }
}
