package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.ExecutiveDecisionType
import com.schoolos.android.domain.model.SchoolHealthMetrics

interface AnalyticsRepository {
    suspend fun getSchoolStabilityMetrics(): Result<SchoolHealthMetrics>
    suspend fun executePrincipalDecision(
        decisionType: ExecutiveDecisionType,
        targetId: String,
        targetName: String,
        title: String,
        instructions: String
    ): Result<String>
}
