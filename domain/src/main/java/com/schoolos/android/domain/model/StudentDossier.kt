package com.schoolos.android.domain.model

data class StudentDocumentItem(
    val id: String? = null,
    val documentType: String,
    val label: String,
    val description: String,
    val isRequired: Boolean,
    val status: String, // UNUPLOADED, PENDING, VERIFIED, REJECTED
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSizeBytes: Long? = null,
    val mimeType: String? = null,
    val verifiedAt: String? = null,
    val rejectionReason: String? = null,
    val notes: String? = null,
    val updatedAt: String? = null
)

data class StudentDossier(
    val studentId: String,
    val nisn: String,
    val fullName: String,
    val className: String? = null,
    val programTier: String,
    val programTierLabel: String,
    val isComplete: Boolean,
    val completenessPercentage: Double,
    val requiredCount: Int,
    val verifiedCount: Int,
    val pendingCount: Int,
    val rejectedCount: Int,
    val documents: List<StudentDocumentItem>
)
