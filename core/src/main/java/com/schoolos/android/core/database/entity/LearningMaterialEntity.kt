package com.schoolos.android.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_materials")
data class LearningMaterialEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val materialType: String,
    val contentBody: String?,
    val mediaUrl: String?,
    val thumbnailUrl: String?,
    val subject: String,
    val size: String?,
    val isCompleted: Boolean,
    val completedCount: Long,
    val teacherName: String? = null,
    val teacherId: String? = null,
    val className: String? = null,
    val classId: String? = null,
    val startPage: Int? = null,
    val endPage: Int? = null,
    val cachedAt: Long = System.currentTimeMillis(),
)
