package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.AtRiskStudent

interface AtRiskRepository {
    suspend fun getAtRiskStudents(): Result<List<AtRiskStudent>>
    suspend fun sendSingleReminder(student: AtRiskStudent): Result<Boolean>
    suspend fun sendBatchReminders(students: List<AtRiskStudent>, category: String): Result<Int>
}
