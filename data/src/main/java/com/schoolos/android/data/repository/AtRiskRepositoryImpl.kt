package com.schoolos.android.data.repository

import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.data.remote.SendReminderRequestDto
import com.schoolos.android.domain.model.AtRiskStudent
import com.schoolos.android.domain.repository.AtRiskRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AtRiskRepositoryImpl @Inject constructor(
    private val api: SchoolOsApi,
) : AtRiskRepository {

    override suspend fun getAtRiskStudents(): Result<List<AtRiskStudent>> = runCatching {
        coroutineScope {
            val remindersDeferred = async { runCatching { api.getReminders().data }.getOrNull() ?: emptyMap() }
            val materialsDeferred = async { runCatching { api.getMaterials().data }.getOrNull() ?: emptyList() }
            val assignmentsDeferred = async { runCatching { api.getAssignments().data }.getOrNull() ?: emptyList() }
            val classesDeferred = async { runCatching { api.getClasses(pageSize = 100).data }.getOrNull() ?: emptyList() }

            val remindersMap = remindersDeferred.await()
            val materials = materialsDeferred.await()
            val assignments = assignmentsDeferred.await()
            val classes = classesDeferred.await()

            val classIdToName = classes.associate { it.id to it.name }
            val atRiskList = mutableListOf<AtRiskStudent>()
            val seenKeys = mutableSetOf<String>()

            // 1. Check Unread Materials (Materi Tertinggal)
            for (mat in materials.take(4)) {
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

            // 2. Check Low Scores & Overdue Assignments
            for (a in assignments.take(4)) {
                try {
                    val subs = api.getSubmissions(a.id).data ?: emptyList()
                    for (s in subs) {
                        if (s.score != null && s.score < 70) {
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
                                        riskLevel = if (s.score < 60) "HIGH" else "MEDIUM",
                                        title = "Nilai ${a.title} di bawah KKM (${s.score}/100)",
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
    }

    override suspend fun sendSingleReminder(student: AtRiskStudent): Result<Boolean> = runCatching {
        val req = SendReminderRequestDto(
            studentId = student.studentId,
            title = student.title,
            reason = student.description,
            category = student.category,
            materialId = student.materialId,
            assignmentId = student.assignmentId,
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
                else -> "Penyelesaian Kendala Belajar"
            },
            category = category,
            teacherName = "Kepala Sekolah",
        )
        val res = api.sendReminder(req)
        res.data?.sentCount ?: studentIds.size
    }
}
