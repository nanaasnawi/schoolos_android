package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DocumentItemDto(
    val id: String? = null,
    @SerialName("document_type") val documentType: String,
    val label: String,
    val description: String,
    @SerialName("is_required") val isRequired: Boolean,
    val status: String, // UNUPLOADED, PENDING, VERIFIED, REJECTED
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_size_bytes") val fileSizeBytes: Long? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("verified_at") val verifiedAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    val notes: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class StudentDossierDto(
    @SerialName("student_id") val studentId: String,
    val nisn: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("class_name") val className: String? = null,
    @SerialName("program_tier") val programTier: String,
    @SerialName("program_tier_label") val programTierLabel: String,
    @SerialName("is_complete") val isComplete: Boolean,
    @SerialName("completeness_percentage") val completenessPercentage: Double,
    @SerialName("required_count") val requiredCount: Int,
    @SerialName("verified_count") val verifiedCount: Int,
    @SerialName("pending_count") val pendingCount: Int,
    @SerialName("rejected_count") val rejectedCount: Int,
    val documents: List<DocumentItemDto>
)
