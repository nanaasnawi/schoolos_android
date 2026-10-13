package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.GamificationStatus

interface GamificationRepository {
    suspend fun getGamificationStatus(studentId: String? = null): Result<GamificationStatus>
}
