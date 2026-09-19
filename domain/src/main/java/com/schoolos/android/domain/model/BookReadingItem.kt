package com.schoolos.android.domain.model

data class BookReadingItem(
    val id: String,
    val title: String,
    val author: String? = null,
    val publisher: String? = null,
    val subjectName: String? = null,
    val gradeLevelName: String? = null,
    val coverUrl: String? = null,
    val fileUrl: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 100,
    val startPage: Int? = null,
    val endPage: Int? = null,
    val isCompleted: Boolean = false,
    val lastReadAt: String? = null,
    val materialId: String? = null,
) {
    val progressPercentage: Float
        get() {
            if (startPage != null && endPage != null && endPage > startPage) {
                val totalAssigned = endPage - startPage + 1
                val currentAssigned = (currentPage - startPage + 1).coerceAtLeast(0)
                return (currentAssigned.toFloat() / totalAssigned.toFloat()).coerceIn(0f, 1f)
            }
            return if (totalPages > 0) (currentPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f) else 0f
        }
}
