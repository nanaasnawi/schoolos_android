package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.HabitEntry

interface HabitRepository {
    suspend fun getHabitsCalendar(
        studentId: String? = null,
        entryDate: String? = null,
        classId: String? = null,
    ): Result<List<HabitEntry>>

    suspend fun recordHabitEntry(
        studentId: String,
        entryDate: String,
        academicYear: String? = "2026/2027",
        semester: String? = "GANJIL",
        habits: Map<String, Boolean>,
        habitsNotes: Map<String, String>? = null,
    ): Result<HabitEntry>

    suspend fun verifyHabitParent(
        entryId: String,
        parentFeedback: String?,
    ): Result<Boolean>
}
