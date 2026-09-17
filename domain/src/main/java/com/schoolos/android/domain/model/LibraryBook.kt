package com.schoolos.android.domain.model

data class LibraryBook(
    val id: String,
    val title: String,
    val author: String? = null,
    val publisher: String? = null,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val gradeLevelId: String? = null,
    val gradeLevelName: String? = null,
    val totalPages: Int = 100,
    val coverUrl: String? = null,
    val fileUrl: String? = null,
)
