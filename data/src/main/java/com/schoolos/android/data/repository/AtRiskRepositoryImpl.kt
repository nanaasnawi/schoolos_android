package com.schoolos.android.data.repository

import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.data.remote.SendReminderRequestDto
import com.schoolos.android.domain.model.AtRiskStudent
import com.schoolos.android.domain.repository.AtRiskRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AtRiskRepositoryImpl @Inject constructor(
    private val api: SchoolOsApi,
) : AtRiskRepository {

    override suspend fun getAtRiskStudents(): Result<List<AtRiskStudent>> = runCatching {
        // High-performance single database query endpoint with RLS & 4-category detection
        try {
            val response = api.getAtRiskStudents()
            if (response.success && response.data != null) {
                return@runCatching response.data.map { it.toDomain() }
            }
        } catch (e: Exception) {
            Timber.w(e, "Primary getAtRiskStudents API call failed, falling back to client-side detection")
        }

        // Resilient fallback in case API server version doesn't yet have the dedicated route
        val remindersMap = runCatching { api.getReminders().data }.getOrNull() ?: emptyMap()
        val materials = runCatching { api.getMaterials().data }.getOrNull() ?: emptyList()
        val assignments = runCatching { api.getAssignments().data }.getOrNull() ?: emptyList()
        val classes = runCatching { api.getClasses(pageSize = 100).data }.getOrNull() ?: emptyList()

        val classIdToName = classes.associate { it.id to it.name }
        val atRiskList = mutableListOf<AtRiskStudent>()
        val seenKeys = mutableSetOf<String>()

        // 1. Check Unread Materials (Materi Tertinggal)
        for (mat in materials.take(6)) {
            try {
                val completionsRes = api.getMaterialCompletions(mat.id)
                val completions = completionsRes.data ?: emptyList()
                for (c in completions) {
                    val key = "${c.studentId}_UNREAD_${mat.id}"
                    if (!c.isCompleted && (c.currentPage ?: 0) < 3 && !seenKeys.contains(key)) {
                        seenKeys.add(key)
                        val reminderRecord = remindersMap[c.studentId]
                        atRiskList.add(
                            AtRiskStudent(
                                studentId = c.studentId,
                                studentName = c.studentName.ifBlank { "Peserta Didik" },
                                nisn = c.nisn ?: "-",
                                className = c.className ?: mat.className ?: (mat.classId?.let { classIdToName[it] }) ?: "Rombel",
                                classId = mat.classId,
                                category = "UNREAD_MATERIAL",
                                categoryLabel = "Materi Tertinggal",
                                riskLevel = "MEDIUM",
                                title = "Belum Membaca Materi ${mat.title}",
                                description = "Siswa baru membaca sampai halaman ${c.currentPage ?: 1}. Diperlukan pengingat literasi.",
                                materialId = mat.id,
                                isReminded = reminderRecord?.sent == true,
                                remindedAt = reminderRecord?.sentAt,
                                updatedAt = c.lastReadAt,
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Error loading completions for material ${mat.id}")
            }
        }

        // 2. Check Overdue & Low Score Assignments
        for (a in assignments.take(6)) {
            try {
                val subs = api.getSubmissions(a.id).data ?: emptyList()
                for (s in subs) {
                    val isUnsubmitted = s.submittedAt.isNullOrBlank() || s.status.equals("unsubmitted", ignoreCase = true)
                    val isLowScore = s.score != null && s.score < 70

                    if (isUnsubmitted) {
                        val key = "${s.studentId}_OVERDUE_${a.id}"
                        if (!seenKeys.contains(key)) {
                            seenKeys.add(key)
                            val reminderRecord = remindersMap[s.studentId]
                            atRiskList.add(
                                AtRiskStudent(
                                    studentId = s.studentId,
                                    studentName = s.studentName ?: "Peserta Didik",
                                    nisn = s.studentNisn ?: "-",
                                    className = a.className ?: (a.classId?.let { classIdToName[it] }) ?: "Rombel",
                                    classId = a.classId,
                                    category = "OVERDUE_ASSIGNMENT",
                                    categoryLabel = "Tugas Tertunda",
                                    riskLevel = "HIGH",
                                    title = "Tugas ${a.title} Belum Dikumpulkan",
                                    description = "Siswa belum mengirimkan lembar jawaban tugas terstruktur.",
                                    assignmentId = a.id,
                                    isReminded = reminderRecord?.sent == true,
                                    remindedAt = reminderRecord?.sentAt,
                                    updatedAt = null,
                                )
                            )
                        }
                    } else if (isLowScore) {
                        val key = "${s.studentId}_LOW_SCORE_${a.id}"
                        if (!seenKeys.contains(key)) {
                            seenKeys.add(key)
                            val reminderRecord = remindersMap[s.studentId]
                            atRiskList.add(
                                AtRiskStudent(
                                    studentId = s.studentId,
                                    studentName = s.studentName ?: "Peserta Didik",
                                    nisn = s.studentNisn ?: "-",
                                    className = a.className ?: (a.classId?.let { classIdToName[it] }) ?: "Rombel",
                                    classId = a.classId,
                                    category = "LOW_SCORE",
                                    categoryLabel = "Nilai Rendah",
                                    riskLevel = if ((s.score ?: 100) < 60) "HIGH" else "MEDIUM",
                                    title = "Nilai ${a.title} di bawah KKTP (${s.score}/100)",
                                    description = "Siswa memperoleh skor ${s.score}. Perlu diberikan bimbingan atau tugas remedial.",
                                    assignmentId = a.id,
                                    isReminded = reminderRecord?.sent == true,
                                    remindedAt = reminderRecord?.sentAt,
                                    updatedAt = s.submittedAt,
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Error loading submissions for assignment ${a.id}")
            }
        }

        atRiskList
    }

    override suspend fun sendSingleReminder(student: AtRiskStudent): Result<Boolean> = runCatching {
        val req = SendReminderRequestDto(
            studentId = student.studentId,
            title = student.title,
            reason = student.description,
            category = student.category,
            materialId = student.materialId,
            assignmentId = student.assignmentId,
            quizId = student.quizId,
            teacherName = "Kepala Sekolah",
        )
        val res = api.sendReminder(req)
        res.data?.success == true || res.data?.sentCount ?: 0 > 0
    }

    override suspend fun sendBatchReminders(students: List<AtRiskStudent>, category: String): Result<Int> = runCatching {
        val studentIds = students.map { it.studentId }
        val req = SendReminderRequestDto(
            studentIds = studentIds,
            title = when (category) {
                "UNREAD_MATERIAL" -> "Membaca Modul Pembelajaran"
                "OVERDUE_ASSIGNMENT" -> "Tugas Belum Dikumpulkan"
                "LOW_SCORE" -> "Evaluasi & Remedial Belajar"
                "QUIZ_ALERT" -> "Penyelesaian Kuis & Ujian CBT"
                else -> "Penyelesaian Kendala Belajar"
            },
            category = category,
            teacherName = "Kepala Sekolah",
        )
        val res = api.sendReminder(req)
        res.data?.sentCount ?: studentIds.size
    }
}
