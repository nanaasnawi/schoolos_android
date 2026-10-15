package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeeklyActivityDto(
    @SerialName("student_id") val studentId: String = "",
    @SerialName("student_name") val studentName: String = "",
    @SerialName("week_start_date") val weekStartDate: String = "",
    @SerialName("week_end_date") val weekEndDate: String = "",
    @SerialName("total_activities") val totalActivities: Int = 0,
    @SerialName("total_materials") val totalMaterials: Int = 0,
    @SerialName("total_assignments") val totalAssignments: Int = 0,
    @SerialName("total_quizzes") val totalQuizzes: Int = 0,
    @SerialName("peak_day") val peakDay: String = "Senin",
    @SerialName("peak_count") val peakCount: Int = 0,
    @SerialName("active_days_count") val activeDaysCount: Int = 0,
    @SerialName("days") val days: List<DailyActivityStatDto> = emptyList(),
)

@Serializable
data class DailyActivityStatDto(
    @SerialName("date") val date: String = "",
    @SerialName("day_name") val dayName: String = "",
    @SerialName("day_short") val dayShort: String = "",
    @SerialName("day_number") val dayNumber: Int = 1,
    @SerialName("is_today") val isToday: Boolean = false,
    @SerialName("materials_count") val materialsCount: Int = 0,
    @SerialName("assignments_count") val assignmentsCount: Int = 0,
    @SerialName("quizzes_count") val quizzesCount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("activities") val activities: List<DailyActivityItemDto> = emptyList(),
)

@Serializable
data class DailyActivityItemDto(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("category") val category: String = "",
    @SerialName("category_label") val categoryLabel: String = "",
    @SerialName("sub_type") val subType: String = "",
    @SerialName("subject_name") val subjectName: String? = null,
    @SerialName("time") val time: String = "",
)
