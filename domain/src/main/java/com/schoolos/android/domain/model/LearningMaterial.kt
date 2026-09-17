package com.schoolos.android.domain.model

data class LearningMaterial(
    val id: String,
    val title: String,
    val description: String?,
    val materialType: MaterialType,
    val contentBody: String? = null,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val subject: String,
    val size: String? = null,
    val isCompleted: Boolean = false,
    val completedCount: Long = 0L,
    val teacherName: String? = null,
    val teacherId: String? = null,
    val className: String? = null,
    val classId: String? = null,
    val startPage: Int? = null,
    val endPage: Int? = null,
)

enum class MaterialType {
    VIDEO, DOCUMENT, ARTICLE, IMAGE
}
