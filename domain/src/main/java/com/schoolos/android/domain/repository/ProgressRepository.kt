package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.Progress
import com.schoolos.android.domain.model.WeeklyActivity

interface ProgressRepository {
    suspend fun getProgress(classId: String): Result<Progress>
    suspend fun getWeeklyActivity(studentId: String? = null): Result<WeeklyActivity>
}

