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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeacherInlineGradingCard(
    submission: AssignmentSubmission,
    maxScore: Int,
    isGrading: Boolean,
    onDismiss: () -> Unit,
    onGrade: (score: Int, feedback: String?) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val dialogBg = if (isDark) Color(0xFF111113) else Color.White
    val dialogBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val teacherAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    var scoreInput by remember(submission.id) { mutableStateOf((submission.score ?: maxScore).toFloat()) }
    var feedbackInput by remember(submission.id) { mutableStateOf(submission.feedback ?: "") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = dialogBg,
        border = BorderStroke(1.dp, dialogBorder),
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Return Navigation Pill & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !isGrading, onClick = onDismiss)
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Daftar",
                        tint = NeonBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Kembali ke Daftar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonBlue
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonBlue.copy(alpha = 0.12f))
                        .border(0.5.dp, NeonBlue.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "LEMBAR KOREKSI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonBlue
                    )
                }
            }

            // Student Identity Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NeonBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Grade, null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        submission.studentName ?: "ID: ${submission.studentId.take(8)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    val nisnText = if (!submission.studentNisn.isNullOrBlank()) "NISN: ${submission.studentNisn}" else "NISN: -"
                    val dateText = if (submission.submittedAt.isNotBlank()) " • Kumpul: ${submission.submittedAt.take(16)}" else ""
                    Text("$nisnText$dateText", fontSize = 11.5.sp, color = textTertiary)
                }
            }

                HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp)

                // Structured per-question answers (PG + Essay)
                if (submission.answers.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "JAWABAN BUTIR SOAL (${submission.answers.size} SOAL)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonBlue,
                            letterSpacing = 0.5.sp
                        )
                        submission.answers.forEachIndexed { idx, answer ->
                            val isPg = answer.questionType.uppercase().contains("CHOICE") || answer.questionType.uppercase() == "PG"
                            val answerBg = when {
                                isPg && answer.isCorrect == true -> teacherAccent
                                isPg && answer.isCorrect == false -> NeonError
                                else -> NeonWarning
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(answerBg.copy(alpha = 0.08f))
                                    .border(1.dp, answerBg.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${idx + 1}. ${answer.questionText}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "${answer.pointsEarned}/${answer.maxPoints} Poin",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = answerBg
                                        )
                                    }
                                    if (isPg) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val icon = if (answer.isCorrect == true) Icons.Default.CheckCircle else Icons.Default.Cancel
                                            Icon(icon, null, tint = answerBg, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Jawaban Siswa: ${answer.chosenChoiceText ?: "-"}",
                                                fontSize = 11.5.sp,
                                                color = answerBg,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(innerCardBg)
                                                .border(1.dp, innerCardBorder, RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            Column {
                                                Text("Jawaban Uraian Siswa:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = textTertiary)
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    answer.textAnswer ?: "-",
                                                    fontSize = 11.5.sp,
                                                    color = textPrimary,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp)
                }

                // Free-text answer preview
                val dialogContent = submission.content
                if (!dialogContent.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "CATATAN / TEKS PENGUMPULAN SISWA",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonBlue,
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
                            Text(dialogContent, fontSize = 12.sp, color = textSecondary, lineHeight = 18.sp)
                        }
                    }
                }

                if (!submission.fileUrl.isNullOrBlank()) {
                    val context = androidx.compose.ui.platform.LocalContext.current
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
                            .background(NeonBlue.copy(alpha = 0.12f))
                            .border(1.dp, NeonBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
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
                                Icon(Icons.Default.Attachment, null, tint = NeonBlue, modifier = Modifier.size(16.dp))
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
                                        color = NeonBlue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonBlue)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("Buka Berkas", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                if (submission.answers.isEmpty() && dialogContent.isNullOrBlank() && submission.fileUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonWarning.copy(alpha = 0.08f))
                            .border(1.dp, NeonWarning.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Warning, null, tint = NeonWarning, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Jawaban siswa tidak tercatat",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonWarning
                                )
                                Text(
                                    "Lembar ini dikumpulkan tanpa jawaban PG/esai yang tersimpan di server. Minta siswa mengerjakan & mengumpulkan ulang tugas ini.",
                                    fontSize = 11.5.sp,
                                    color = textSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Score Slider & Quick Select
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "NILAI AKHIR TUGAS",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textTertiary,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonBlue.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${scoreInput.toInt()} / $maxScore",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonBlue
                            )
                        }
                    }
                    Slider(
                        value = scoreInput.coerceIn(0f, maxScore.toFloat()),
                        onValueChange = { scoreInput = it },
                        valueRange = 0f..maxScore.toFloat(),
                        steps = if (maxScore > 1) maxScore - 1 else 0,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonBlue,
                            activeTrackColor = NeonBlue,
                            inactiveTrackColor = innerCardBorder,
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0", fontSize = 10.5.sp, color = textTertiary)
                        Text("$maxScore", fontSize = 10.5.sp, color = textTertiary)
                    }
                }

                // Feedback
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "CATATAN / KOMENTAR GURU (opsional)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textTertiary,
                        letterSpacing = 0.5.sp
                    )
                    OutlinedTextField(
                        value = feedbackInput,
                        onValueChange = { feedbackInput = it },
                        placeholder = { Text("Tuliskan evaluasi atau catatan koreksi untuk siswa...", fontSize = 11.5.sp, color = textTertiary) },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = innerCardBorder,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedContainerColor = innerCardBg,
                            unfocusedContainerColor = innerCardBg,
                        )
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isGrading,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary),
                        border = BorderStroke(1.dp, innerCardBorder)
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onGrade(
                                scoreInput.toInt(),
                                feedbackInput.trim().ifBlank { null }
                            )
                        },
                        enabled = !isGrading,
                        modifier = Modifier.weight(1.5f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                    ) {
                        if (isGrading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Send,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Simpan Nilai", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
}
