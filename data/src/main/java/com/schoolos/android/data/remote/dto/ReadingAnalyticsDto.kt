package com.schoolos.android.data.remote.dto

import com.schoolos.android.domain.model.MaterialSummaryItem
import com.schoolos.android.domain.model.StudentReadingRow
import com.schoolos.android.domain.model.TeacherReadingMetrics
import com.schoolos.android.domain.model.TeacherReadingSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReadingSummaryResponseDto(
    val materials: List<MaterialSummaryItemDto> = emptyList(),
    @SerialName("reading_rows") val readingRows: List<StudentReadingRowDto> = emptyList(),
    val metrics: ReadingSummaryMetricsDto? = null,
)

@Serializable
data class MaterialSummaryItemDto(
    @SerialName("material_id") val materialId: String,
    val title: String,
    @SerialName("material_type") val materialType: String = "document",
    @SerialName("class_name") val className: String = "Semua Rombel",
    @SerialName("class_id") val classId: String? = null,
    @SerialName("subject_name") val subjectName: String = "Umum",
    @SerialName("start_page") val startPage: Int = 1,
    @SerialName("end_page") val endPage: Int = 15,
    @SerialName("total_pages") val totalPages: Int = 15,
    @SerialName("total_assigned_students") val totalAssignedStudents: Long = 0,
    @SerialName("completed_students_count") val completedStudentsCount: Long = 0,
    @SerialName("average_progress_percentage") val averageProgressPercentage: Int = 0,
)

@Serializable
data class StudentReadingRowDto(
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String,
    val nisn: String = "-",
    val gender: String = "L",
    @SerialName("class_id") val classId: String? = null,
    @SerialName("class_name") val className: String = "Kelas Umum",
    @SerialName("material_id") val materialId: String,
    @SerialName("material_title") val materialTitle: String,
    @SerialName("subject_name") val subjectName: String = "Umum",
    @SerialName("current_page") val currentPage: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 15,
    @SerialName("completion_percentage") val completionPercentage: Int = 0,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("last_read_at") val lastReadAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("reading_status") val readingStatus: String = "Belum Mulai",
)

@Serializable
data class ReadingSummaryMetricsDto(
    @SerialName("total_materials") val totalMaterials: Long = 0,
    @SerialName("total_readers") val totalReaders: Long = 0,
    @SerialName("completed_readers") val completedReaders: Long = 0,
    @SerialName("overall_completion_rate") val overallCompletionRate: Int = 0,
    @SerialName("total_pages_read") val totalPagesRead: Long = 0,
)

fun ReadingSummaryResponseDto.toDomain() = TeacherReadingSummary(
    materials = materials.map { it.toDomain() },
    readingRows = readingRows.map { it.toDomain() },
    metrics = metrics?.toDomain() ?: TeacherReadingMetrics(
        totalMaterials = materials.size.toLong(),
        totalReaders = readingRows.size.toLong(),
        completedReaders = readingRows.count { it.isCompleted }.toLong(),
        overallCompletionRate = if (readingRows.isNotEmpty()) (readingRows.count { it.isCompleted } * 100 / readingRows.size) else 0,
        totalPagesRead = readingRows.sumOf { it.currentPage.toLong() }
    ),
)

fun MaterialSummaryItemDto.toDomain() = MaterialSummaryItem(
    materialId = materialId,
    title = title,
    materialType = materialType,
    className = className,
    classId = classId,
    subjectName = subjectName,
    startPage = startPage,
    endPage = endPage,
    totalPages = totalPages,
    totalAssignedStudents = totalAssignedStudents,
    completedStudentsCount = completedStudentsCount,
    averageProgressPercentage = averageProgressPercentage,
)

fun StudentReadingRowDto.toDomain() = StudentReadingRow(
    studentId = studentId,
    studentName = studentName,
    nisn = nisn,
    gender = gender,
    classId = classId,
    className = className,
    materialId = materialId,
    materialTitle = materialTitle,
    subjectName = subjectName,
    currentPage = currentPage,
    totalPages = totalPages,
    completionPercentage = completionPercentage,
    isCompleted = isCompleted,
    lastReadAt = lastReadAt,
    completedAt = completedAt,
    readingStatus = readingStatus,
)

fun ReadingSummaryMetricsDto.toDomain() = TeacherReadingMetrics(
    totalMaterials = totalMaterials,
    totalReaders = totalReaders,
    completedReaders = completedReaders,
    overallCompletionRate = overallCompletionRate,
    totalPagesRead = totalPagesRead,
)

