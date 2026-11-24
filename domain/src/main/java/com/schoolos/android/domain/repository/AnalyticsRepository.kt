package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.ExecutiveDecisionType
import com.schoolos.android.domain.model.GtkSupervisionOverview
import com.schoolos.android.domain.model.SchoolHealthMetrics
import com.schoolos.android.domain.model.TeacherSupervisionProfile

interface AnalyticsRepository {
    suspend fun getSchoolStabilityMetrics(): Result<SchoolHealthMetrics>
    suspend fun getGtkSupervisionOverview(): Result<GtkSupervisionOverview>
    suspend fun getTeacherSupervisionDetail(teacherId: String): Result<TeacherSupervisionProfile>
    suspend fun executePrincipalDecision(
        decisionType: ExecutiveDecisionType,
        targetId: String,
        targetName: String,
        title: String,
        instructions: String
    ): Result<String>
}
