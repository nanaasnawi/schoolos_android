package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.Progress
import com.schoolos.android.domain.model.StudyTracker
import com.schoolos.android.domain.model.WeeklyActivity

interface ProgressRepository {
    suspend fun getProgress(classId: String): Result<Progress>
    suspend fun getWeeklyActivity(studentId: String? = null): Result<WeeklyActivity>
    suspend fun getStudyTracker(studentId: String? = null): Result<StudyTracker>
    suspend fun updateReadingProgress(
        materialId: String,
        currentPage: Int,
        isCompleted: Boolean,
    ): Result<Boolean>
}

