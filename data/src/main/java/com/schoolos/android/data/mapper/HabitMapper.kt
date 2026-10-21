package com.schoolos.android.data.mapper

import com.schoolos.android.data.remote.dto.HabitEntryDto
import com.schoolos.android.domain.model.HabitEntry
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun JsonObject?.toHabitsMap(): Map<String, Boolean> {
    if (this == null) return emptyMap()
    return this.mapNotNull { (key, element) ->
        val boolVal = element.jsonPrimitive.booleanOrNull ?: false
        key to boolVal
    }.toMap()
}

fun JsonObject?.toNotesMap(): Map<String, String> {
    if (this == null) return emptyMap()
    return this.mapNotNull { (key, element) ->
        val strVal = element.jsonPrimitive.contentOrNull
        if (strVal != null) key to strVal else null
    }.toMap()
}

fun Map<String, Boolean>.toHabitsJsonObject(): JsonObject {
    return buildJsonObject {
        forEach { (k, v) -> put(k, v) }
    }
}

fun Map<String, String>.toNotesJsonObject(): JsonObject {
    return buildJsonObject {
        forEach { (k, v) -> put(k, v) }
    }
}

fun HabitEntryDto.toDomain(): HabitEntry {
    return HabitEntry(
        id = id,
        studentId = studentId,
        studentName = studentName,
        className = className,
        entryDate = entryDate,
        academicYear = academicYear,
        semester = semester,
        habits = habits.toHabitsMap(),
        habitsNotes = habitsNotes.toNotesMap(),
        completedCount = completedCount,
        complianceRate = complianceRate,
        verificationStatus = verificationStatus,
        verifiedByRole = verifiedByRole,
        verifiedAt = verifiedAt,
        parentFeedback = parentFeedback,
        teacherNotes = teacherNotes,
        overrideReason = overrideReason,
    )
}
