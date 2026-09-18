package com.schoolos.android.domain.repository

import com.schoolos.android.domain.model.LearningMaterial

interface LearningMaterialRepository {
    suspend fun getMaterials(): Result<List<LearningMaterial>>
    suspend fun getMaterialById(id: String): Result<LearningMaterial>
    suspend fun createMaterial(
        title: String,
        description: String?,
        materialType: com.schoolos.android.domain.model.MaterialType,
        contentBody: String? = null,
        mediaUrl: String? = null,
        subject: String,
        classId: String? = null
    ): Result<LearningMaterial>
    suspend fun uploadMaterialFile(
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<String>
    suspend fun toggleMaterialCompletion(id: String): Result<Boolean>
    suspend fun getMaterialCompletions(id: String): Result<List<com.schoolos.android.domain.model.MaterialStudentCompletion>>
    suspend fun getLibraryBooks(search: String? = null): Result<List<com.schoolos.android.domain.model.LibraryBook>>
    suspend fun assignReadingMaterial(
        bookId: String,
        title: String,
        instructions: String?,
        classId: String,
        subjectId: String?,
        startPage: Int,
        endPage: Int
    ): Result<String>
    suspend fun updateMaterial(
        id: String,
        title: String?,
        description: String?,
        mediaUrl: String? = null,
        storageKey: String? = null
    ): Result<LearningMaterial>
    suspend fun deleteMaterial(id: String): Result<Unit>
}
