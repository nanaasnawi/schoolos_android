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
) : SessionRepository {

    override suspend fun getSessions(classId: String?): Result<List<LearningSession>> = runCatching {
        try {
            val response = api.getSessions(classId)
            val sessions = response.data?.map { it.toDomain() }
            if (sessions != null) {
                if (classId.isNullOrBlank()) {
                    sessionDao.clearAll()
                }
                sessionDao.insertAll(sessions.map { it.toEntity() })
                return@runCatching sessions
            }
        } catch (e: Exception) {
            android.util.Log.w("SessionRepo", "Remote fetch failed, falling back to cache: ${e.message}")
        }
        val cached = try {
            if (!classId.isNullOrBlank()) {
                sessionDao.getSessionsByClass(classId).first()
            } else {
                sessionDao.getSessions().first()
            }
        } catch (_: Exception) {
            emptyList()
        }
        if (cached.isNotEmpty()) {
            cached.map { it.entityToDomain() }
        } else {
            emptyList()
        }
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
        val response = api.getSessionAttendance(sessionId)
        response.data?.map { it.toDomain() }
            ?: throw Exception(response.error?.message ?: "Gagal memuat presensi sesi.")
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
        dto.toDomain()
    }
}
