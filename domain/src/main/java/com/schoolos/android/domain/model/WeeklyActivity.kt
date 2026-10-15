package com.schoolos.android.domain.model

data class WeeklyActivity(
    val studentId: String,
    val studentName: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val totalActivities: Int,
    val totalMaterials: Int,
    val totalAssignments: Int,
    val totalQuizzes: Int,
    val peakDay: String,
    val peakCount: Int,
    val activeDaysCount: Int,
    val days: List<DailyActivityStat>,
)

data class DailyActivityStat(
    val date: String,
    val dayName: String,
    val dayShort: String,
    val dayNumber: Int,
    val isToday: Boolean,
    val materialsCount: Int,
    val assignmentsCount: Int,
    val quizzesCount: Int,
    val totalCount: Int,
    val activities: List<DailyActivityItem>,
)

data class DailyActivityItem(
    val id: String,
    val title: String,
    val category: String, // "MATERIAL", "ASSIGNMENT", "QUIZ"
    val categoryLabel: String,
    val subType: String,
    val subjectName: String?,
    val time: String,
)
