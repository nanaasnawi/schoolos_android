package com.schoolos.android.data.repository

import com.schoolos.android.core.database.dao.SessionDao
import com.schoolos.android.core.database.mapper.toDomain as entityToDomain
import com.schoolos.android.core.database.mapper.toEntity
import com.schoolos.android.data.mapper.toDomain
import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.domain.model.LearningSession
import com.schoolos.android.domain.model.SessionAttendance
import com.schoolos.android.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val api: SchoolOsApi,
    private val sessionDao: SessionDao,
    private val attendanceDao: com.schoolos.android.core.database.dao.SessionAttendanceDao,
) : SessionRepository {

    override suspend fun getSessions(classId: String?): Result<List<LearningSession>> = runCatching {
        val cleanClassId = classId?.takeIf { it.isNotBlank() }
        try {
            val response = api.getSessions(cleanClassId)
            val sessions = response.data?.map { it.toDomain() } ?: emptyList()
            try {
                if (cleanClassId == null) {
                    sessionDao.clearAll()
                }
                if (sessions.isNotEmpty()) {
                    sessionDao.insertAll(sessions.map { it.toEntity() })
                }
            } catch (_: Exception) {}
            return@runCatching sessions
        } catch (e: Exception) {
            android.util.Log.w("SessionRepo", "Remote fetch failed, falling back to cache: ${e.message}")
        }
        val cached = try {
            if (!cleanClassId.isNullOrBlank()) {
                val byClass = sessionDao.getSessionsByClass(cleanClassId).first()
                if (byClass.isNotEmpty()) byClass else sessionDao.getSessions().first()
            } else {
                sessionDao.getSessions().first()
            }
        } catch (_: Exception) {
            emptyList()
        }
        cached.map { it.entityToDomain() }
    }

    override suspend fun getSession(id: String): Result<LearningSession> = runCatching {
        try {
            val response = api.getSession(id)
            val session = response.data?.toDomain()
            if (session != null) {
                sessionDao.insert(session.toEntity())
                return@runCatching session
            }
        } catch (e: Exception) {
            android.util.Log.w("SessionRepo", "Remote getSession failed, checking cache: ${e.message}")
        }
        sessionDao.getSessionById(id)?.entityToDomain()
            ?: throw Exception("Sesi pembelajaran tidak ditemukan.")
    }

    override suspend fun getAttendance(sessionId: String): Result<List<SessionAttendance>> = runCatching {
        try {
            val response = api.getSessionAttendance(sessionId)
            val attendances = response.data?.map { it.toDomain() }
            if (attendances != null) {
                attendanceDao.clearSessionAttendance(sessionId)
                attendanceDao.insertAll(attendances.map { it.toEntity() })
                return@runCatching attendances
            }
        } catch (e: Exception) {
            android.util.Log.w("SessionRepo", "Remote getAttendance failed, falling back to cache: ${e.message}")
        }
        val cached = attendanceDao.getAttendanceBySession(sessionId).map { it.entityToDomain() }
        if (cached.isNotEmpty()) {
            cached
        } else {
            emptyList()
        }
    }

    override suspend fun recordAttendance(
        sessionId: String,
        studentId: String,
        status: String,
        notes: String?,
    ): Result<SessionAttendance> = runCatching {
        val idempotencyKey = java.util.UUID.randomUUID().toString()
        val request = com.schoolos.android.data.remote.dto.RecordAttendanceRequestDto(
            studentId = studentId,
            status = status,
            checkedInAt = java.time.Instant.now().toString(),
            notes = notes,
        )
        val response = api.recordAttendance(
            id = sessionId,
            request = request,
            idempotencyKey = idempotencyKey,
        )
        val dto = response.data ?: throw Exception(response.error?.message ?: "Gagal mencatat presensi kehadiran.")
        val domain = dto.toDomain()
        attendanceDao.insert(domain.toEntity())
        domain
    }

    override suspend fun recordAttendanceBulk(
        sessionId: String,
        items: List<com.schoolos.android.domain.repository.RecordAttendanceItem>,
    ): Result<List<SessionAttendance>> = runCatching {
        val idempotencyKey = java.util.UUID.randomUUID().toString()
        val request = items.map { item ->
            com.schoolos.android.data.remote.dto.RecordAttendanceRequestDto(
                studentId = item.studentId,
                status = item.status,
                checkedInAt = item.checkedInAt ?: java.time.Instant.now().toString(),
                notes = item.notes,
            )
        }
        val response = api.recordAttendanceBulk(
            id = sessionId,
            request = request,
            idempotencyKey = idempotencyKey,
        )
        val dtoList = response.data ?: throw Exception(response.error?.message ?: "Gagal mencatat presensi massal.")
        val domainList = dtoList.map { it.toDomain() }
        attendanceDao.insertAll(domainList.map { it.toEntity() })
        domainList
    }

    override suspend fun cancelSession(sessionId: String, reason: String?): Result<LearningSession> = runCatching {
        val response = api.cancelSession(sessionId, com.schoolos.android.data.remote.dto.CancelSessionRequestDto(reason))
        val dto = response.data ?: throw Exception(response.error?.message ?: "Gagal membatalkan sesi pembelajaran.")
        val domain = dto.toDomain()
        sessionDao.insert(domain.toEntity())
        domain
    }

    override suspend fun substituteTeacher(
        sessionId: String,
        substituteTeacherId: String,
        notes: String?,
    ): Result<LearningSession> = runCatching {
        val response = api.substituteTeacher(
            sessionId,
            com.schoolos.android.data.remote.dto.SubstituteTeacherRequestDto(substituteTeacherId, notes)
        )
        val dto = response.data ?: throw Exception(response.error?.message ?: "Gagal menugaskan guru pengganti.")
        val domain = dto.toDomain()
        sessionDao.insert(domain.toEntity())
        domain
    }
}

