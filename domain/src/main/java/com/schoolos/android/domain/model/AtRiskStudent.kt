package com.schoolos.android.domain.model

data class AtRiskStudent(
    val studentId: String,
    val studentName: String,
    val nisn: String,
    val className: String,
    val classId: String? = null,
    val category: String, // "UNREAD_MATERIAL" | "OVERDUE_ASSIGNMENT" | "LOW_SCORE"
    val categoryLabel: String, // "Materi Tertinggal" | "Tugas Tertunda" | "Nilai Rendah"
    val riskLevel: String = "MEDIUM", // "HIGH" | "MEDIUM"
    val title: String, // e.g. "Belum Membaca Materi Bernapas Untuk Kehidupan"
    val description: String, // e.g. "Siswa baru membaca sampai halaman 1. Diperlukan pengingat literasi."
    val materialId: String? = null,
    val assignmentId: String? = null,
    val isReminded: Boolean = false,
    val remindedAt: String? = null,
    val updatedAt: String? = null,
)

data class AtRiskStats(
    val total: Int = 0,
    val unreadMaterials: Int = 0,
    val overdueAssignments: Int = 0,
    val lowScores: Int = 0,
)
