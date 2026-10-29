package com.schoolos.android.domain.model

data class TeacherReadingSummary(
    val materials: List<MaterialSummaryItem> = emptyList(),
    val readingRows: List<StudentReadingRow> = emptyList(),
    val metrics: TeacherReadingMetrics = TeacherReadingMetrics(),
)

data class MaterialSummaryItem(
    val materialId: String,
    val title: String,
    val materialType: String = "document",
    val className: String = "Semua Rombel",
    val classId: String? = null,
    val subjectName: String = "Umum",
    val startPage: Int = 1,
    val endPage: Int = 15,
    val totalPages: Int = 15,
    val totalAssignedStudents: Long = 0,
    val completedStudentsCount: Long = 0,
    val averageProgressPercentage: Int = 0,
)

data class StudentReadingRow(
    val studentId: String,
    val studentName: String,
    val nisn: String = "-",
    val gender: String = "L",
    val classId: String? = null,
    val className: String = "Kelas Umum",
    val materialId: String,
    val materialTitle: String,
    val subjectName: String = "Umum",
    val currentPage: Int = 0,
    val totalPages: Int = 15,
    val completionPercentage: Int = 0,
    val isCompleted: Boolean = false,
    val lastReadAt: String? = null,
    val completedAt: String? = null,
    val readingStatus: String = "Belum Mulai",
)

data class TeacherReadingMetrics(
    val totalMaterials: Long = 0,
    val totalReaders: Long = 0,
    val completedReaders: Long = 0,
    val overallCompletionRate: Int = 0,
    val totalPagesRead: Long = 0,
)

