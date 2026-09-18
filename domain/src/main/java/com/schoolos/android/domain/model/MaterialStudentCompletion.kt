package com.schoolos.android.domain.model

data class MaterialStudentCompletion(
    val studentId: String,
    val studentName: String,
    val nisn: String? = null,
    val gender: String? = null,
    val className: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
    val currentPage: Int? = null,
    val lastReadAt: String? = null,
)
