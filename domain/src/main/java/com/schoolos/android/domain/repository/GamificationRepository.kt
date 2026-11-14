package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.AwardXpResult
import com.schoolos.android.domain.model.DailyChestResult
import com.schoolos.android.domain.model.DailyQuest
import com.schoolos.android.domain.model.DailyQuestSubmitResult
import com.schoolos.android.domain.model.FeedPetResult
import com.schoolos.android.domain.model.GamificationStatus

interface GamificationRepository {
    suspend fun getGamificationStatus(studentId: String? = null): Result<GamificationStatus>
    suspend fun awardXp(
        actionType: String,
        referenceId: String? = null,
        description: String? = null,
        studentId: String? = null,
    ): Result<AwardXpResult>
    suspend fun getDailyQuest(mode: String? = null, round: Int = 0): Result<DailyQuest>
    suspend fun submitDailyQuest(
        questId: String,
        correctCount: Int,
        totalCount: Int,
        maxCombo: Int,
        durationSeconds: Int,
        roundNumber: Int = 0,
    ): Result<DailyQuestSubmitResult>
    suspend fun feedShibaPet(): Result<FeedPetResult>
    suspend fun openDailyChest(): Result<DailyChestResult>
}
