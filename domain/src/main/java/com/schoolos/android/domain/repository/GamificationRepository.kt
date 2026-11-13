package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.AwardXpResult
import com.schoolos.android.domain.model.DailyQuest
import com.schoolos.android.domain.model.DailyQuestSubmitResult
import com.schoolos.android.domain.model.GamificationStatus

interface GamificationRepository {
    suspend fun getGamificationStatus(studentId: String? = null): Result<GamificationStatus>
    suspend fun awardXp(
        actionType: String,
        referenceId: String? = null,
        description: String? = null,
        studentId: String? = null,
    ): Result<AwardXpResult>
    suspend fun getDailyQuest(mode: String? = null): Result<DailyQuest>
    suspend fun submitDailyQuest(
        questId: String,
        correctCount: Int,
        totalCount: Int,
        maxCombo: Int,
        durationSeconds: Int,
    ): Result<DailyQuestSubmitResult>
}
