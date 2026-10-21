package com.schoolos.android.data.repository

import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.data.mapper.toDomain
import com.schoolos.android.data.mapper.toHabitsJsonObject
import com.schoolos.android.data.mapper.toNotesJsonObject
import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.data.remote.dto.RecordHabitEntryRequestDto
import com.schoolos.android.data.remote.dto.VerifyHabitParentRequestDto
import com.schoolos.android.domain.model.HabitEntry
import com.schoolos.android.domain.repository.HabitRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val api: SchoolOsApi,
    private val authManager: AuthManager,
) : HabitRepository {

    override suspend fun getHabitsCalendar(
        studentId: String?,
        entryDate: String?,
        classId: String?,
    ): Result<List<HabitEntry>> = runCatching {
        val effectiveStudentId = if (!studentId.isNullOrBlank()) {
            studentId
        } else {
            val child = authManager.getChildId()
            if (!child.isNullOrBlank()) child else authManager.getStudentId()
        }

        val response = api.getHabitsCalendar(
            studentId = effectiveStudentId,
            entryDate = entryDate,
            classId = classId,
        )

        val list = response.data ?: emptyList()
        list.map { it.toDomain() }
    }.onFailure {
        Timber.w(it, "Failed to fetch habits calendar from server")
    }

    override suspend fun recordHabitEntry(
        studentId: String,
        entryDate: String,
        academicYear: String?,
        semester: String?,
        habits: Map<String, Boolean>,
        habitsNotes: Map<String, String>?,
    ): Result<HabitEntry> = runCatching {
        val request = RecordHabitEntryRequestDto(
            studentId = studentId,
            entryDate = entryDate,
            academicYear = academicYear ?: "2026/2027",
            semester = semester ?: "GANJIL",
            habits = habits.toHabitsJsonObject(),
            habitsNotes = habitsNotes?.toNotesJsonObject(),
        )

        val response = api.recordHabitEntry(request)
        val data = response.data ?: throw IllegalStateException("Respon server kosong saat mencatat jurnal")

        HabitEntry(
            id = data.id,
            studentId = data.studentId.ifBlank { studentId },
            entryDate = data.entryDate.ifBlank { entryDate },
            academicYear = academicYear ?: "2026/2027",
            semester = semester ?: "GANJIL",
            habits = habits,
            habitsNotes = habitsNotes ?: emptyMap(),
            completedCount = data.completedCount,
            complianceRate = data.complianceRate,
            verificationStatus = data.verificationStatus.ifBlank { "PENDING" },
        )
    }.onFailure {
        Timber.e(it, "Failed to record habit entry")
    }

    override suspend fun verifyHabitParent(
        entryId: String,
        parentFeedback: String?,
    ): Result<Boolean> = runCatching {
        val request = VerifyHabitParentRequestDto(
            entryId = entryId,
            parentFeedback = parentFeedback,
        )

        val response = api.verifyHabitParent(request)
        response.success
    }.onFailure {
        Timber.e(it, "Failed to verify habit entry as parent")
    }
}
