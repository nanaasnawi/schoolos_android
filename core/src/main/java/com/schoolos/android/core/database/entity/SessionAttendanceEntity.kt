package com.schoolos.android.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_attendances")
data class SessionAttendanceEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val studentId: String,
    val status: String,
    val checkedInAt: String?,
    val notes: String?,
)
