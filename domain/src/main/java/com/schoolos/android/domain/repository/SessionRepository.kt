package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.SessionAttendance

interface SessionRepository {
    suspend fun getSessions(classId: String? = null): Result<List<LearningSession>>
    suspend fun getSession(id: String): Result<LearningSession>
    suspend fun getAttendance(sessionId: String): Result<List<SessionAttendance>>
    suspend fun recordAttendance(sessionId: String, studentId: String, status: String, notes: String? = null): Result<SessionAttendance>
    suspend fun recordAttendanceBulk(sessionId: String, items: List<RecordAttendanceItem>): Result<List<SessionAttendance>>
    suspend fun cancelSession(sessionId: String, reason: String? = null): Result<LearningSession>
    suspend fun substituteTeacher(sessionId: String, substituteTeacherId: String, notes: String? = null): Result<LearningSession>
}

data class RecordAttendanceItem(
    val studentId: String,
    val status: String,
    val checkedInAt: String? = null,
    val notes: String? = null,
)
