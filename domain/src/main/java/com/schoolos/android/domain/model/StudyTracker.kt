package com.schoolos.android.domain.model

data class StudyTracker(
    val studentId: String,
    val studentName: String,
    val history: TrackerCategory,
    val missed: TrackerCategory,
    val inProgress: TrackerCategory,
)

data class TrackerCategory(
    val key: String,
    val title: String,
    val description: String,
    val totalCount: Int,
    val items: List<StudyTrackerItem>,
)

data class StudyTrackerItem(
    val id: String,
    val title: String,
    val itemType: String, // "MATERIAL", "ASSIGNMENT", "QUIZ", "SESSION"
    val subType: String?,
    val subjectName: String,
    val progressPercent: Int?,
    val score: Double?,
    val maxScore: Double?,
    val timestampLabel: String,
    val dateIso: String?,
    val statusBadge: String,
    val badgeVariant: String, // "success", "warning", "danger", "info", "neutral"
    val reasonNote: String?,
    val actionUrl: String,
)
