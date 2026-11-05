package com.schoolos.android.feature.assignments

import com.schoolos.android.core.R as CoreR
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import com.schoolos.android.core.common.formatPublishTimestamp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.Assignment
import com.schoolos.android.domain.model.AssignmentSubmission

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherAssignmentDetailContent(
    assignment: Assignment,
    allSubmissions: List<AssignmentSubmission>,
    isGrading: Boolean = false,
    gradeSuccess: Boolean = false,
    gradeError: String? = null,
    onGrade: (submissionId: String, score: Int, feedback: String?) -> Unit = { _, _, _ -> },
    onDismissGradeSuccess: () -> Unit = {},
    onDismissGradeError: () -> Unit = {},
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val teacherAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    // 0: Butir Soal & Petunjuk, 1: Pengumpulan Siswa
    var selectedTab by remember { mutableStateOf(if (assignment.questions.isNotEmpty()) 0 else 1) }
    // Filter: ALL, PENDING, GRADED, UNSUBMITTED
    var submissionFilter by remember { mutableStateOf("ALL") }

    val context = LocalContext.current
    // Track which submission is actively open in the inline grading workspace
    var gradingSubmission by remember { mutableStateOf<AssignmentSubmission?>(null) }

    // Grade success snackbar
    LaunchedEffect(gradeSuccess) {
        if (gradeSuccess) {
            kotlinx.coroutines.delay(2000)
            onDismissGradeSuccess()
        }
    }

    val totalStudents = allSubmissions.size
    val gradedCount = allSubmissions.count { it.status == "graded" }
    val pendingCount = allSubmissions.count { it.status == "submitted" }
    val submittedOrGradedCount = gradedCount + pendingCount
    val unsubmittedCount = allSubmissions.count { it.status == "unsubmitted" }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (gradingSubmission == null) {
            // ── METADATA & OVERVIEW CARD ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 0.dp else 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (assignment.createdAt.isNotBlank()) {
                            EducationalDateBadge(
                                dateIso = assignment.createdAt,
                                showTime = true,
                                accentColor = teacherAccent,
                                labelPrefix = "Diterbitkan: "
                            )
                        }
                        assignment.dueAt?.let { dueIso ->
                            EducationalDateBadge(
                                dateIso = dueIso,
                                showTime = true,
                                accentColor = NeonBlue,
                                labelPrefix = "Tenggat: "
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5))
                                .border(1.dp, if (isDark) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFA7F3D0), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Grade, null, tint = teacherAccent, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Maks: ${assignment.maxScore} Poin", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            }
                        }
                        if (assignment.questions.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonBlue.copy(alpha = 0.1f))
                                    .border(1.dp, NeonBlue.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MenuBook, null, tint = NeonBlue, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("${assignment.questions.size} Butir Soal", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = NeonBlue)
                                }
                            }
                        }
                    }
                }
            }

            // ── SEGMENTED NAVIGATION TABS (Butir Soal & Petunjuk vs Pengumpulan Siswa) ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 0.dp else 1.dp,
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isTab0 = selectedTab == 0
                    val isTab1 = selectedTab == 1

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isTab0) NeonBlue.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                if (isTab0) 1.dp else 0.dp,
                                if (isTab0) NeonBlue.copy(alpha = 0.35f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = if (isTab0) NeonBlue else textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Butir Soal (${assignment.questions.size})",
                                fontSize = 12.sp,
                                fontWeight = if (isTab0) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTab0) NeonBlue else textSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isTab1) NeonBlue.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                if (isTab1) 1.dp else 0.dp,
                                if (isTab1) NeonBlue.copy(alpha = 0.35f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AssignmentTurnedIn,
                                contentDescription = null,
                                tint = if (isTab1) NeonBlue else textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Koreksi ($submittedOrGradedCount/$totalStudents)",
                                fontSize = 12.sp,
                                fontWeight = if (isTab1) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTab1) NeonBlue else textSecondary
                            )
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════════════════
        // TAB 0: BUTIR SOAL & PETUNJUK PENGERJAAN
        // ════════════════════════════════════════════════════════════════════════
        if (selectedTab == 0) {
            // Petunjuk Pengerjaan & Deskripsi
            if (assignment.instructions?.isNotBlank() == true || assignment.description?.isNotBlank() == true) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    shadowElevation = if (isDark) 0.dp else 1.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Petunjuk & Deskripsi Tugas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                        if (assignment.instructions?.isNotBlank() == true) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonBlue.copy(alpha = 0.08f))
                                    .border(1.dp, NeonBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    assignment.instructions!!,
                                    fontSize = 12.5.sp,
                                    color = textPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        if (assignment.description?.isNotBlank() == true && assignment.description != assignment.instructions) {
                            Text(
                                assignment.description!!,
                                fontSize = 12.sp,
                                color = textSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Questions List
            if (assignment.questions.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    shadowElevation = if (isDark) 0.dp else 1.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            "Tugas Berbasis Instruksi / Pengumpulan Dokumen",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            "Tugas ini tidak menggunakan butir soal pilihan ganda maupun esai. Siswa mengumpulkan lembar pengerjaan berupa file atau uraian teks langsung.",
                            fontSize = 11.5.sp,
                            color = textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Text(
                    "Daftar Butir Soal (${assignment.questions.size} Soal):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                assignment.questions.forEachIndexed { qIdx, q ->
                    val isPg = q.questionType.uppercase().contains("CHOICE") || q.questionType.uppercase() == "PG"

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = if (isDark) 0.dp else 1.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Question Card Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NeonBlue.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            "Soal #${q.orderIndex ?: (qIdx + 1)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonBlue
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isPg) NeonBlue.copy(alpha = 0.1f) else NeonWarning.copy(alpha = 0.1f))
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            if (isPg) "Pilihan Ganda (PG)" else "Soal Uraian / Esai",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPg) NeonBlue else NeonWarning
                                        )
                                    }
                                }

                                Text(
                                    "${q.points ?: 10} Poin",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = teacherAccent
                                )
                            }

                            // Question Text
                            Text(
                                text = q.questionText,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                lineHeight = 19.sp
                            )

                            // Choices for PG or Essay explanation
                            if (isPg) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    q.choices.forEachIndexed { cIdx, choice ->
                                        val choiceLetter = ('A' + (choice.orderIndex?.minus(1) ?: cIdx)).toString()
                                        val isCorrect = choice.isCorrect == true

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isCorrect) (if (isDark) Color(0xFF064E3B).copy(alpha = 0.3f) else Color(0xFFECFDF5)) else innerCardBg)
                                                .border(
                                                    1.dp,
                                                    if (isCorrect) (if (isDark) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFA7F3D0)) else innerCardBorder,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isCorrect) teacherAccent else NeonBlue.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        choiceLetter,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isCorrect) Color.White else textPrimary
                                                    )
                                                }
                                                Spacer(Modifier.width(10.dp))
                                                Text(
                                                    choice.choiceText,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCorrect) teacherAccent else textSecondary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isCorrect) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        "Kunci Benar",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = teacherAccent
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NeonWarning.copy(alpha = 0.08f))
                                        .border(1.dp, NeonWarning.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                    ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, null, tint = NeonWarning, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Siswa akan menjawab soal ini secara deskriptif / uraian teks di HP mereka. Guru mengoreksi dan memberikan nilai secara objektif.",
                                            fontSize = 11.5.sp,
                                            color = textSecondary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════════════════
        // TAB 1: PENGUMPULAN & KOREKSI SISWA
        // ════════════════════════════════════════════════════════════════════════
        if (selectedTab == 1) {
            if (gradingSubmission != null) {
                TeacherInlineGradingCard(
                    submission = gradingSubmission!!,
                    maxScore = assignment.maxScore,
                    isGrading = isGrading,
                    onDismiss = { gradingSubmission = null },
                    onGrade = { score, feedback ->
                        onGrade(gradingSubmission!!.id, score, feedback)
                        gradingSubmission = null
                    }
                )
            } else {
                // ── REKAP PENILAIAN ──
            if (totalStudents > 0) {
                val progress = if (totalStudents > 0) gradedCount.toFloat() / totalStudents else 0f

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    shadowElevation = if (isDark) 0.dp else 1.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Rekap Progres Penilaian Rombel",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                "$gradedCount / $totalStudents Dinilai",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonBlue
                            )
                        }

                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(innerCardBg)
                                .border(1.dp, innerCardBorder, CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(teacherAccent)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatBadge("Kumpul", submittedOrGradedCount.toString(), NeonBlue, Modifier.weight(1f))
                            StatBadge("Perlu Nilai", pendingCount.toString(), NeonWarning, Modifier.weight(1f))
                            StatBadge("Dinilai", gradedCount.toString(), teacherAccent, Modifier.weight(1f))
                            StatBadge("Belum Kumpul", unsubmittedCount.toString(), textTertiary, Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── SUCCESS & ERROR BANNERS ──
            if (gradeSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NeonSuccess.copy(alpha = 0.12f))
                        .border(1.dp, NeonSuccess.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = NeonSuccess, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Nilai & feedback berhasil disimpan ke siswa!", fontSize = 13.sp, color = NeonSuccess, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!gradeError.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NeonError.copy(alpha = 0.12f))
                        .border(1.dp, NeonError.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clickable { onDismissGradeError() }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = NeonError, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(gradeError, fontSize = 12.sp, color = NeonError)
                    }
                }
            }

            // ── FILTER CHIPS ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterTabChip(
                    label = "Semua ($totalStudents)",
                    isSelected = submissionFilter == "ALL",
                    onClick = { submissionFilter = "ALL" },
                    modifier = Modifier.weight(1f)
                )
                FilterTabChip(
                    label = "Perlu Nilai ($pendingCount)",
                    isSelected = submissionFilter == "PENDING",
                    onClick = { submissionFilter = "PENDING" },
                    accentColor = NeonWarning,
                    modifier = Modifier.weight(1.1f)
                )
                FilterTabChip(
                    label = "Dinilai ($gradedCount)",
                    isSelected = submissionFilter == "GRADED",
                    onClick = { submissionFilter = "GRADED" },
                    accentColor = teacherAccent,
                    modifier = Modifier.weight(1f)
                )
                FilterTabChip(
                    label = "Belum ($unsubmittedCount)",
                    isSelected = submissionFilter == "UNSUBMITTED",
                    onClick = { submissionFilter = "UNSUBMITTED" },
                    accentColor = textTertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Filtered Submissions
            val displayedSubmissions = allSubmissions.filter { sub ->
                when (submissionFilter) {
                    "PENDING" -> sub.status == "submitted"
                    "GRADED" -> sub.status == "graded"
                    "UNSUBMITTED" -> sub.status == "unsubmitted"
                    else -> true
                }
            }

            if (displayedSubmissions.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    shadowElevation = if (isDark) 0.dp else 1.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (submissionFilter == "UNSUBMITTED") Icons.Default.CheckCircle else Icons.Default.Inbox,
                            contentDescription = null,
                            tint = if (submissionFilter == "UNSUBMITTED") teacherAccent else textTertiary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            when (submissionFilter) {
                                "PENDING" -> "Tidak ada pengumpulan yang menunggu penilaian."
                                "GRADED" -> "Belum ada pengumpulan yang dinilai."
                                "UNSUBMITTED" -> "Semua warga belajar telah mengumpulkan tugas!"
                                else -> "Belum ada warga belajar yang mengumpulkan."
                            },
                            fontSize = 13.sp,
                            color = textTertiary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                displayedSubmissions.forEach { submission ->
                    TeacherSubmissionRow(
                        submission = submission,
                        maxScore = assignment.maxScore,
                        onClick = {
                            if (submission.status == "unsubmitted") {
                                Toast.makeText(context, "${submission.studentName ?: "Siswa"} belum mengumpulkan tugas ini.", Toast.LENGTH_SHORT).show()
                            } else {
                                gradingSubmission = submission
                            }
                        }
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun FilterTabChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = NeonBlue,
) {
    val isDark = LocalIsDarkTheme.current
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else innerCardBg)
            .border(
                1.dp,
                if (isSelected) accentColor.copy(alpha = 0.5f) else innerCardBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) accentColor else textTertiary,
            maxLines = 1
        )
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val isDark = LocalIsDarkTheme.current
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 8.5.sp, fontWeight = FontWeight.Medium, color = textTertiary, maxLines = 1)
        }
    }
}

@Composable
private fun TeacherSubmissionRow(
    submission: AssignmentSubmission,
    maxScore: Int,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val teacherAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    val isUnsubmitted = submission.status == "unsubmitted"
    val isGraded = submission.status == "graded"
    val accentColor = when {
        isGraded -> teacherAccent
        isUnsubmitted -> textTertiary
        else -> NeonWarning
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, if (isUnsubmitted) cardBorder else accentColor.copy(alpha = 0.35f)),
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when {
                        isGraded -> Icons.Default.CheckCircle
                        isUnsubmitted -> Icons.Default.HourglassEmpty
                        else -> Icons.Default.AssignmentTurnedIn
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    submission.studentName ?: "Siswa: ${submission.studentId.take(8)}...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = textPrimary
                )
                if (submission.studentNisn != null) {
                    Spacer(Modifier.height(1.dp))
                    Text("NISN: ${submission.studentNisn}", fontSize = 10.5.sp, color = textTertiary)
                }
                Spacer(Modifier.height(2.dp))
                if (isUnsubmitted) {
                    Text(
                        "Belum Mengumpulkan",
                        fontSize = 10.5.sp,
                        color = textTertiary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    Text(
                        "Kumpul: ${submission.submittedAt.take(10)}",
                        fontSize = 10.5.sp,
                        color = textTertiary
                    )
                }
                val subContent = submission.content
                if (!subContent.isNullOrBlank() && !isUnsubmitted) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        subContent.take(50) + if (subContent.length > 50) "..." else "",
                        fontSize = 11.sp,
                        color = textSecondary,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when {
                    isGraded -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(teacherAccent.copy(alpha = 0.12f))
                                .border(1.dp, teacherAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${submission.score ?: 0}/${maxScore}",
                                color = teacherAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    isUnsubmitted -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(innerCardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("BELUM KUMPUL", color = textTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonWarning.copy(alpha = 0.15f))
                                .border(1.dp, NeonWarning.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("KOREKSI", color = NeonWarning, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (!isUnsubmitted) {
                    Icon(
                        if (isGraded) Icons.Default.Edit else Icons.Default.Grade,
                        null,
                        tint = accentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TeacherInlineGradingCard(
    submission: AssignmentSubmission,
    maxScore: Int,
    isGrading: Boolean,
    onDismiss: () -> Unit,
    onGrade: (score: Int, feedback: String?) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val teacherAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val greenBg = if (isDark) Color(0xFF10B981).copy(alpha = 0.14f) else Color(0xFFECFDF5)
    val redFg = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
    val redBg = if (isDark) Color(0xFFEF4444).copy(alpha = 0.14f) else Color(0xFFFEF2F2)
    val amberFg = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    val amberBg = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.14f) else Color(0xFFFFFBEB)
    val skyFg = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val skyBg = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.14f) else Color(0xFFEFF6FF)

    val totalQuestions = submission.answers.size
    val defaultPerQuestion = if (totalQuestions > 0) (maxScore / totalQuestions).coerceAtLeast(1) else maxScore
    val pgAnswersList = submission.answers.filter {
        val qt = it.questionType.uppercase()
        qt.contains("CHOICE") || qt == "PG"
    }
    val correctPgCount = pgAnswersList.count { it.isCorrect == true }
    val totalPgCount = pgAnswersList.size

    val autoCalculatedScore = remember(submission.answers, maxScore) {
        if (submission.answers.isEmpty()) {
            submission.score ?: maxScore
        } else {
            val totalMaxPoints = submission.answers.sumOf { if (it.maxPoints > 0) it.maxPoints else defaultPerQuestion }.coerceAtLeast(1)
            val earnedPoints = submission.answers.sumOf { ans ->
                val isPg = ans.questionType.uppercase().contains("CHOICE") || ans.questionType.uppercase() == "PG"
                val qMax = if (ans.maxPoints > 0) ans.maxPoints else defaultPerQuestion
                if (isPg) {
                    if (ans.isCorrect == true) (if (ans.pointsEarned > 0) ans.pointsEarned else qMax) else 0
                } else {
                    ans.pointsEarned
                }
            }
            ((earnedPoints.toFloat() / totalMaxPoints.toFloat()) * maxScore).toInt().coerceIn(0, maxScore)
        }
    }

    var scoreInput by remember(submission.id) {
        mutableStateOf((submission.score ?: autoCalculatedScore).toFloat())
    }
    var feedbackInput by remember(submission.id) { mutableStateOf(submission.feedback ?: "") }
    var showGradingSheet by remember(submission.id) { mutableStateOf(false) }

    val studentNameDisplay = submission.studentName?.takeIf { it.isNotBlank() } ?: "Siswa (${submission.studentId.take(8)})"
    val studentInitials = remember(studentNameDisplay) {
        studentNameDisplay
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "S" }
    }
    val formattedSubmittedDate = remember(submission.submittedAt) {
        if (submission.submittedAt.isNotBlank()) formatPublishTimestamp(submission.submittedAt) else "-"
    }
    val isGraded = submission.status == "graded" || submission.score != null

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── CARD 1: STUDENT HEADER & SUMMARY METRICS ─────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = if (isDark) 0.dp else 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Bar: Back Button + Status Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.clickable(enabled = !isGrading, onClick = onDismiss),
                        shape = RoundedCornerShape(10.dp),
                        color = skyBg,
                        border = BorderStroke(0.5.dp, skyFg.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke Daftar",
                                tint = skyFg,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Kembali ke Daftar",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = skyFg
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isGraded) greenBg else amberBg,
                        border = BorderStroke(
                            0.5.dp,
                            (if (isGraded) teacherAccent else amberFg).copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isGraded) teacherAccent else amberFg)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isGraded) "Dinilai • ${submission.score ?: autoCalculatedScore}/$maxScore" else "Perlu Koreksi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGraded) teacherAccent else amberFg
                            )
                        }
                    }
                }

                // Student Identity Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(skyBg)
                            .border(1.dp, skyFg.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = studentInitials,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = skyFg
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = studentNameDisplay,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        val nisnText = if (!submission.studentNisn.isNullOrBlank()) "NISN: ${submission.studentNisn}" else "ID: ${submission.studentId.take(8)}"
                        Text(
                            text = "$nisnText • Dikumpulkan $formattedSubmittedDate",
                            fontSize = 11.5.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 3-Column Summary Metric Boxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(greenBg)
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (totalPgCount > 0) "$correctPgCount/$totalPgCount" else "${submission.answers.size}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = teacherAccent
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (totalPgCount > 0) "BENAR PG" else "TOTAL SOAL",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = teacherAccent.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(skyBg)
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$autoCalculatedScore/$maxScore",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = skyFg
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "SKOR PG",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = skyFg.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isGraded) greenBg else amberBg)
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (submission.score != null) "${submission.score}/$maxScore" else "Belum",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isGraded) teacherAccent else amberFg
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "NILAI AKHIR",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = (if (isGraded) teacherAccent else amberFg).copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                }

                // Existing Teacher Feedback Preview (if already graded with feedback)
                if (!submission.feedback.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(innerCardBg)
                            .border(1.dp, innerCardBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "CATATAN EVALUASI GURU",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = teacherAccent,
                                letterSpacing = 0.4.sp
                            )
                            Text(
                                text = submission.feedback!!,
                                fontSize = 12.sp,
                                color = textPrimary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Primary CTA to open Grading Bottom Sheet
                Button(
                    onClick = { showGradingSheet = true },
                    enabled = !isGrading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skyFg,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isGraded) Icons.Default.Edit else Icons.Default.Grade,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isGraded) "Ubah Nilai & Umpan Balik" else "Beri Nilai & Umpan Balik",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ── SECTION 2: INDIVIDUAL QUESTION ANSWER CARDS ──────────────────
        if (submission.answers.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "JAWABAN BUTIR SOAL",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textTertiary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${submission.answers.size} Soal Terjawab",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = skyFg
                )
            }

            submission.answers.forEachIndexed { idx, answer ->
                val isPg = answer.questionType.uppercase().contains("CHOICE") || answer.questionType.uppercase() == "PG"
                val effectiveMax = if (answer.maxPoints > 0) answer.maxPoints else defaultPerQuestion
                val effectiveEarned = if (isPg) {
                    if (answer.isCorrect == true) (if (answer.pointsEarned > 0) answer.pointsEarned else effectiveMax) else 0
                } else {
                    answer.pointsEarned
                }

                val statusFg = when {
                    isPg && answer.isCorrect == true -> teacherAccent
                    isPg && answer.isCorrect == false -> redFg
                    else -> amberFg
                }
                val statusBg = when {
                    isPg && answer.isCorrect == true -> greenBg
                    isPg && answer.isCorrect == false -> redBg
                    else -> amberBg
                }
                val cleanedQuestionText = remember(answer.questionText) {
                    answer.questionText
                        .trim()
                        .replace(Regex("\\r\\n"), "\n")
                        .replace(Regex("\\n{2,}"), "\n\n")
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    shadowElevation = if (isDark) 0.dp else 1.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Question Card Header Row: Badge on left, Points Pill on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = skyBg
                                ) {
                                    Text(
                                        text = "Soal #${idx + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = skyFg,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = innerCardBg,
                                    border = BorderStroke(0.5.dp, innerCardBorder)
                                ) {
                                    Text(
                                        text = if (isPg) "Pilihan Ganda" else "Uraian / Esai",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusBg,
                                border = BorderStroke(0.5.dp, statusFg.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    val badgeText = when {
                                        isPg && answer.isCorrect == true -> "✓ +$effectiveEarned/$effectiveMax Poin"
                                        isPg && answer.isCorrect == false -> "✕ 0/$effectiveMax Poin"
                                        else -> "Maks $effectiveMax Poin"
                                    }
                                    Text(
                                        text = badgeText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusFg
                                    )
                                }
                            }
                        }

                        // Clean Full-Width Question Text
                        Text(
                            text = cleanedQuestionText,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary,
                            lineHeight = 20.sp
                        )

                        // Student Answer Box
                        if (isPg) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(statusBg)
                                    .border(1.dp, statusFg.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (answer.isCorrect == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = statusFg,
                                        modifier = Modifier
                                            .size(17.dp)
                                            .padding(top = 1.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = if (answer.isCorrect == true) "Jawaban Siswa (Benar)" else "Jawaban Siswa (Kurang Tepat)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusFg.copy(alpha = 0.85f)
                                        )
                                        Text(
                                            text = answer.chosenChoiceText?.takeIf { it.isNotBlank() } ?: "-",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(innerCardBg)
                                    .border(1.dp, innerCardBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Jawaban Uraian Siswa:",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textTertiary
                                    )
                                    Text(
                                        text = answer.textAnswer?.takeIf { it.isNotBlank() } ?: "-",
                                        fontSize = 13.sp,
                                        color = textPrimary,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── SECTION 3: STUDENT NOTE & ATTACHMENT CARD ────────────────────
        val dialogContent = submission.content
        if (!dialogContent.isNullOrBlank() || !submission.fileUrl.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDark) 0.dp else 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!dialogContent.isNullOrBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "CATATAN / JAWABAN TEKS SISWA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = skyFg,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(innerCardBg)
                                    .border(1.dp, innerCardBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = dialogContent,
                                    fontSize = 12.5.sp,
                                    color = textPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    if (!submission.fileUrl.isNullOrBlank()) {
                        val context = LocalContext.current
                        val rawFileUrl = submission.fileUrl!!
                        val resolvedFileUrl = remember(rawFileUrl) {
                            if (rawFileUrl.startsWith("http://", ignoreCase = true) || rawFileUrl.startsWith("https://", ignoreCase = true)) {
                                rawFileUrl
                            } else {
                                val host = com.schoolos.android.core.common.BuildConfig.API_BASE_URL.substringBefore("/api/").trimEnd('/')
                                "$host/${rawFileUrl.trimStart('/')}"
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(skyBg)
                                .border(1.dp, skyFg.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable {
                                    runCatching {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(resolvedFileUrl)
                                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Attachment, null, tint = skyFg, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Lampiran Dokumen Siswa",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Text(
                                            rawFileUrl.substringAfterLast('/'),
                                            fontSize = 10.5.sp,
                                            color = skyFg,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(skyFg)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text("Buka Berkas", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Empty submission fallback
        if (submission.answers.isEmpty() && dialogContent.isNullOrBlank() && submission.fileUrl.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Warning, null, tint = amberFg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Jawaban siswa tidak tercatat",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = amberFg
                        )
                        Text(
                            "Lembar ini dikumpulkan tanpa jawaban PG/esai yang tersimpan di server. Minta siswa mengerjakan & mengumpulkan ulang tugas ini.",
                            fontSize = 12.sp,
                            color = textSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // ── SECTION 4: BOTTOM ACTION CARD (OPENS BOTTOM SHEET) ───────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = if (isDark) 0.dp else 1.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isGrading,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary),
                    border = BorderStroke(1.dp, innerCardBorder)
                ) {
                    Text("Kembali", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { showGradingSheet = true },
                    enabled = !isGrading,
                    modifier = Modifier
                        .weight(1.6f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = teacherAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Grade, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isGraded) "Edit Nilai (${submission.score}/$maxScore)" else "Input Nilai Akhir",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // ── GRADING MODAL BOTTOM SHEET ───────────────────────────────────────
    if (showGradingSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (!isGrading) showGradingSheet = false },
            sheetState = sheetState,
            containerColor = cardBg,
            contentColor = textPrimary,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Penilaian & Evaluasi Tugas",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = "$studentNameDisplay • Skor PG Otomatis: $autoCalculatedScore/$maxScore",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = skyBg,
                        border = BorderStroke(0.5.dp, skyFg.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "${scoreInput.toInt()} / $maxScore",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = skyFg,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp)

                // Quick Score Presets + Slider
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ATUR NILAI AKHIR SISWA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textTertiary,
                        letterSpacing = 0.5.sp
                    )

                    val presets = remember(autoCalculatedScore, maxScore) {
                        listOf(
                            "Skor PG ($autoCalculatedScore)" to autoCalculatedScore,
                            "70" to (maxScore * 0.70f).toInt(),
                            "80" to (maxScore * 0.80f).toInt(),
                            "90" to (maxScore * 0.90f).toInt(),
                            "100" to maxScore
                        ).distinctBy { it.second }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { (label, valScore) ->
                            val isSelected = scoreInput.toInt() == valScore
                            Surface(
                                modifier = Modifier.clickable { scoreInput = valScore.toFloat() },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) skyFg else innerCardBg,
                                border = BorderStroke(1.dp, if (isSelected) skyFg else innerCardBorder)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Slider(
                        value = scoreInput.coerceIn(0f, maxScore.toFloat()),
                        onValueChange = { scoreInput = it },
                        valueRange = 0f..maxScore.toFloat(),
                        steps = if (maxScore > 1) maxScore - 1 else 0,
                        colors = SliderDefaults.colors(
                            thumbColor = skyFg,
                            activeTrackColor = skyFg,
                            inactiveTrackColor = innerCardBorder,
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0 Poin", fontSize = 11.sp, color = textTertiary)
                        Text("Maks $maxScore Poin", fontSize = 11.sp, color = textTertiary)
                    }
                }

                // Quick Feedback Chips + Input Field
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CATATAN / UMPAN BALIK GURU (OPSIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textTertiary,
                        letterSpacing = 0.5.sp
                    )

                    val quickFeedbacks = listOf(
                        "Sudah sangat baik, pertahankan!",
                        "Perhatikan kembali ketelitian jawaban.",
                        "Pelajari kembali materi modul terkait."
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickFeedbacks.forEach { template ->
                            Surface(
                                modifier = Modifier.clickable { feedbackInput = template },
                                shape = RoundedCornerShape(8.dp),
                                color = innerCardBg,
                                border = BorderStroke(0.5.dp, innerCardBorder)
                            ) {
                                Text(
                                    text = template,
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = feedbackInput,
                        onValueChange = { feedbackInput = it },
                        placeholder = {
                            Text(
                                "Tuliskan evaluasi atau catatan koreksi untuk siswa...",
                                fontSize = 12.5.sp,
                                color = textTertiary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = skyFg,
                            unfocusedBorderColor = innerCardBorder,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedContainerColor = innerCardBg,
                            unfocusedContainerColor = innerCardBg,
                        )
                    )
                }

                // Submit / Cancel Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showGradingSheet = false },
                        enabled = !isGrading,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary),
                        border = BorderStroke(1.dp, innerCardBorder)
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showGradingSheet = false
                            onGrade(
                                scoreInput.toInt(),
                                feedbackInput.trim().ifBlank { null }
                            )
                        },
                        enabled = !isGrading,
                        modifier = Modifier
                            .weight(1.6f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = teacherAccent,
                            contentColor = Color.White
                        )
                    ) {
                        if (isGrading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Simpan Nilai", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    }
                }
            }
        }
    }
}
