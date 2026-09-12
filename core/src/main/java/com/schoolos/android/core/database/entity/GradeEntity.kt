package com.schoolos.android.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val classId: String,
    val subjectId: String,
    val componentName: String,
    val sourceType: String,
    val rawScore: Double?,
    val maxRawScore: Double?,
    val weightedScore: Double?,
    val weightPercentage: Double?,
    val calculatedAt: String,
)
