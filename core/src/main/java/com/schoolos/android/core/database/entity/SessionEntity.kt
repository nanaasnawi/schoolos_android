package com.schoolos.android.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val lessonId: String?,
    val sessionType: String = "scheduled",
    val scheduleId: String? = null,
    val subjectId: String? = null,
    val classId: String,
    val teacherId: String,
    val substituteTeacherId: String? = null,
    val substituteTeacherName: String? = null,
    val sessionDate: String? = null,
    val sessionNumber: Int = 1,
    val startTime: String? = null,
    val endTime: String? = null,
    val scheduledAt: String?,
    val startedAt: String?,
    val endedAt: String?,
    val status: String,
    val notes: String?,
    val cancellationReason: String? = null,
    val subjectName: String?,
    val teacherName: String?,
    val className: String?,
    val room: String?,
)
