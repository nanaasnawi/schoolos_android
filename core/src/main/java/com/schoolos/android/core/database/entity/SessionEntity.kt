package com.schoolos.android.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val classId: String,
    val teacherId: String,
    val scheduledAt: String?,
    val startedAt: String?,
    val endedAt: String?,
    val status: String,
    val notes: String?,
    val subjectName: String?,
    val teacherName: String?,
    val className: String?,
    val room: String?,
)
