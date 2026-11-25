package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StudyTrackerDto(
    @SerialName("student_id") val studentId: String = "",
    @SerialName("student_name") val studentName: String = "",
    @SerialName("history") val history: TrackerCategoryDto = TrackerCategoryDto(),
    @SerialName("missed") val missed: TrackerCategoryDto = TrackerCategoryDto(),
    @SerialName("in_progress") val inProgress: TrackerCategoryDto = TrackerCategoryDto(),
)

@Serializable
data class TrackerCategoryDto(
    @SerialName("key") val key: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("items") val items: List<TrackerItemDto> = emptyList(),
)

@Serializable
data class TrackerItemDto(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("item_type") val itemType: String = "",
    @SerialName("sub_type") val subType: String? = null,
    @SerialName("subject_name") val subjectName: String = "Mata Pelajaran",
    @SerialName("progress_percent") val progressPercent: Int? = null,
    @SerialName("score") val score: Double? = null,
    @SerialName("max_score") val maxScore: Double? = null,
    @SerialName("timestamp_label") val timestampLabel: String = "",
    @SerialName("date_iso") val dateIso: String? = null,
    @SerialName("status_badge") val statusBadge: String = "",
    @SerialName("badge_variant") val badgeVariant: String = "neutral",
    @SerialName("reason_note") val reasonNote: String? = null,
    @SerialName("action_url") val actionUrl: String = "",
)

@Serializable
data class UpdateReadingProgressRequestDto(
    @SerialName("material_id") val materialId: String,
    @SerialName("current_page") val currentPage: Int,
    @SerialName("is_completed") val isCompleted: Boolean,
)

@Serializable
data class ReadingProgressResponseDto(
    @SerialName("student_id") val studentId: String = "",
    @SerialName("material_id") val materialId: String = "",
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("last_read_at") val lastReadAt: String = "",
)
