package com.schoolos.android.feature.assignments

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    // 0: Butir Soal & Petunjuk, 1: Pengumpulan Siswa
    var selectedTab by remember { mutableStateOf(if (assignment.questions.isNotEmpty()) 0 else 1) }
    // Filter: ALL, PENDING, GRADED, UNSUBMITTED
    var submissionFilter by remember { mutableStateOf("ALL") }

    // Track which submission is open in the grading dialog
    var gradingSubmission by remember { mutableStateOf<AssignmentSubmission?>(null) }
    // Track if teacher clicked an unsubmitted student to show dialog
    var unsubmittedStudentDialog by remember { mutableStateOf<AssignmentSubmission?>(null) }

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay)
                .clip(RoundedCornerShape(20.dp))
                .background(CosmicNavy)
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (assignment.createdAt.isNotBlank()) {
                        EducationalDateBadge(
                            dateIso = assignment.createdAt,
                            showTime = true,
                            accentColor = TeacherNeon,
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
                            .background(SuccessBg)
                            .border(1.dp, NeonSuccess.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Grade, null, tint = NeonSuccess, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Maks: ${assignment.maxScore} Poin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
                                Text("${assignment.questions.size} Butir Soal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonBlue)
                            }
                        }
                    }
                }
            }
        }

        // ── SEGMENTED NAVIGATION TABS (Butir Soal & Petunjuk vs Pengumpulan Siswa) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CosmicNavy)
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isTab0 = selectedTab == 0
            val isTab1 = selectedTab == 1

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isTab0) NeonBlue.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = if (isTab0) NeonBlue else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "📋 Butir Soal (${assignment.questions.size})",
                        fontSize = 12.sp,
                        fontWeight = if (isTab0) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTab0) NeonBlue else TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isTab1) NeonBlue.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        tint = if (isTab1) NeonBlue else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "📥 Koreksi Siswa ($submittedOrGradedCount/$totalStudents)",
                        fontSize = 12.sp,
                        fontWeight = if (isTab1) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTab1) NeonBlue else TextSecondary
                    )
                }
            }
        }

        // ════════════════════════════════════════════════════════════════════════
        // TAB 0: BUTIR SOAL & PETUNJUK PENGERJAAN
        // ════════════════════════════════════════════════════════════════════════
        if (selectedTab == 0) {
            // Petunjuk Pengerjaan & Deskripsi
            if (assignment.instructions?.isNotBlank() == true || assignment.description?.isNotBlank() == true) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicNavy)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = NeonBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Petunjuk & Deskripsi Tugas", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        if (assignment.instructions?.isNotBlank() == true) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonBlue.copy(alpha = 0.08f))
                                    .border(1.dp, NeonBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    assignment.instructions!!,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        if (assignment.description?.isNotBlank() == true && assignment.description != assignment.instructions) {
                            Text(
                                assignment.description!!,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Questions List
            if (assignment.questions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicNavy)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📝", fontSize = 32.sp)
                        Text(
                            "Tugas Berbasis Instruksi / Pengumpulan Dokumen",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Tugas ini tidak menggunakan butir soal pilihan ganda maupun esai. Siswa mengumpulkan lembar pengerjaan berupa file atau uraian teks langsung.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Text(
                    "Daftar Butir Soal (${assignment.questions.size} Soal):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                assignment.questions.forEachIndexed { qIdx, q ->
                    val isPg = q.questionType.uppercase().contains("CHOICE") || q.questionType.uppercase() == "PG"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CosmicNavy)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                            .background(NeonBlue.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            "Soal #${q.orderIndex ?: (qIdx + 1)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
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
                                            if (isPg) "🔘 Pilihan Ganda (PG)" else "📝 Soal Uraian / Esai",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPg) NeonBlue else NeonWarning
                                        )
                                    }
                                }

                                Text(
                                    "⭐ ${q.points ?: 10} Poin",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonSuccess
                                )
                            }

                            // Question Text
                            Text(
                                text = q.questionText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
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
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isCorrect) NeonSuccess.copy(alpha = 0.1f) else CosmicBlack)
                                                .border(
                                                    1.dp,
                                                    if (isCorrect) NeonSuccess.copy(alpha = 0.6f) else GlassBorder,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isCorrect) NeonSuccess else NeonBlue.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        choiceLetter,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isCorrect) CosmicBlack else TextPrimary
                                                    )
                                                }
                                                Spacer(Modifier.width(10.dp))
                                                Text(
                                                    choice.choiceText,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCorrect) NeonSuccess else TextSecondary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isCorrect) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        "✓ Kunci Benar",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = NeonSuccess
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
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NeonWarning.copy(alpha = 0.08f))
                                        .border(1.dp, NeonWarning.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, null, tint = NeonWarning, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Siswa akan menjawab soal ini secara deskriptif / uraian teks di HP mereka. Guru mengoreksi dan memberikan nilai secara objektif.",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
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
            // ── REKAP PENILAIAN ──
            if (totalStudents > 0) {
                val progress = if (totalStudents > 0) gradedCount.toFloat() / totalStudents else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CosmicNavy, NeonBlue.copy(alpha = 0.08f))
                            )
                        )
                        .border(1.dp, NeonBlue.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Rekap Progres Penilaian Rombel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
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
                                .background(NeonBlue.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(listOf(NeonBlue, NeonSuccess))
                                    )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatBadge("📥 Kumpul", submittedOrGradedCount.toString(), NeonBlue, Modifier.weight(1f))
                            StatBadge("⏳ Perlu Nilai", pendingCount.toString(), NeonWarning, Modifier.weight(1f))
                            StatBadge("✅ Dinilai", gradedCount.toString(), NeonSuccess, Modifier.weight(1f))
                            StatBadge("⚠️ Belum Kumpul", unsubmittedCount.toString(), TextTertiary, Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── SUCCESS & ERROR BANNERS ──
            if (gradeSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonSuccess.copy(alpha = 0.12f))
                        .border(1.dp, NeonSuccess.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = NeonSuccess, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("✅ Nilai & feedback berhasil disimpan ke siswa!", fontSize = 13.sp, color = NeonSuccess, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!gradeError.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonError.copy(alpha = 0.12f))
                        .border(1.dp, NeonError.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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
                    accentColor = NeonSuccess,
                    modifier = Modifier.weight(1f)
                )
                FilterTabChip(
                    label = "Belum ($unsubmittedCount)",
                    isSelected = submissionFilter == "UNSUBMITTED",
                    onClick = { submissionFilter = "UNSUBMITTED" },
                    accentColor = TextTertiary,
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CosmicNavy)
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (submissionFilter == "UNSUBMITTED") "🎉" else "📭", fontSize = 32.sp)
                        Text(
                            when (submissionFilter) {
                                "PENDING" -> "Tidak ada pengumpulan yang menunggu penilaian."
                                "GRADED" -> "Belum ada pengumpulan yang dinilai."
                                "UNSUBMITTED" -> "Semua siswa dalam kelas telah mengumpulkan tugas!"
                                else -> "Belum ada siswa yang mengumpulkan."
                            },
                            fontSize = 13.sp,
                            color = TextTertiary,
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
                                unsubmittedStudentDialog = submission
                            } else {
                                gradingSubmission = submission
                            }
                        }
                    )
                }
            }
        }
    }

    // ── GRADING DIALOG FOR SUBMITTED STUDENTS ──
    gradingSubmission?.let { sub ->
        GradingDialog(
            submission = sub,
            maxScore = assignment.maxScore,
            isGrading = isGrading,
            onDismiss = { gradingSubmission = null },
            onGrade = { score, feedback ->
                onGrade(sub.id, score, feedback)
                gradingSubmission = null
            }
        )
    }

    // ── DIALOG FOR UNSUBMITTED STUDENTS ──
    unsubmittedStudentDialog?.let { sub ->
        AlertDialog(
            onDismissRequest = { unsubmittedStudentDialog = null },
            title = {
                Text(
                    sub.studentName ?: "Informasi Siswa",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "NISN: ${sub.studentNisn ?: "-"}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        "Siswa ini belum mengumpulkan tugas. Siswa dapat mengerjakan tugas ini melalui aplikasi School OS Siswa.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { unsubmittedStudentDialog = null }) {
                    Text("Tutup", color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CosmicNavy,
            shape = RoundedCornerShape(16.dp)
        )
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else CosmicNavy)
            .border(
                1.dp,
                if (isSelected) accentColor.copy(alpha = 0.6f) else GlassBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            color = if (isSelected) accentColor else TextTertiary,
            maxLines = 1
        )
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextTertiary, maxLines = 1)
        }
    }
}

@Composable
private fun TeacherSubmissionRow(
    submission: AssignmentSubmission,
    maxScore: Int,
    onClick: () -> Unit,
) {
    val isUnsubmitted = submission.status == "unsubmitted"
    val isGraded = submission.status == "graded"
    val accentColor = when {
        isGraded -> NeonSuccess
        isUnsubmitted -> TextTertiary
        else -> NeonWarning
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp), spotColor = accentColor.copy(alpha = 0.1f))
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(1.dp, if (isUnsubmitted) GlassBorder else accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f))
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
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                if (submission.studentNisn != null) {
                    Spacer(Modifier.height(1.dp))
                    Text("NISN: ${submission.studentNisn}", fontSize = 10.sp, color = TextTertiary)
                }
                Spacer(Modifier.height(2.dp))
                if (isUnsubmitted) {
                    Text(
                        "Belum Mengumpulkan",
                        fontSize = 10.sp,
                        color = TextTertiary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    Text(
                        "Kumpul: ${submission.submittedAt.take(10)}",
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                }
                val subContent = submission.content
                if (!subContent.isNullOrBlank() && !isUnsubmitted) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        subContent.take(50) + if (subContent.length > 50) "..." else "",
                        fontSize = 10.sp,
                        color = TextSecondary,
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
                                .background(SuccessBg)
                                .border(1.dp, NeonSuccess.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${submission.score ?: 0}/${maxScore}",
                                color = NeonSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    isUnsubmitted -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicBlack)
                                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("BELUM KUMPUL", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                            Text("KOREKSI", color = NeonWarning, fontSize = 9.sp, fontWeight = FontWeight.Black)
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
private fun GradingDialog(
    submission: AssignmentSubmission,
    maxScore: Int,
    isGrading: Boolean,
    onDismiss: () -> Unit,
    onGrade: (score: Int, feedback: String?) -> Unit,
) {
    var scoreInput by remember { mutableStateOf((submission.score ?: maxScore).toFloat()) }
    var feedbackInput by remember { mutableStateOf(submission.feedback ?: "") }

    Dialog(
        onDismissRequest = { if (!isGrading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CosmicNavy)
                .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                "Lembar Koreksi Siswa",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            val displayName = submission.studentName ?: "ID: ${submission.studentId.take(8)}"
                            Text("$displayName (${submission.studentNisn ?: "-"})", fontSize = 11.sp, color = TextTertiary)
                        }
                    }
                    IconButton(onClick = { if (!isGrading) onDismiss() }) {
                        Icon(Icons.Default.Close, null, tint = TextTertiary)
                    }
                }

                HorizontalDivider(color = GlassBorder)

                // Structured per-question answers (PG + Essay)
                if (submission.answers.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "JAWABAN BUTIR SOAL (${submission.answers.size} SOAL)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonBlue,
                            letterSpacing = 1.sp
                        )
                        submission.answers.forEachIndexed { idx, answer ->
                            val isPg = answer.questionType.uppercase().contains("CHOICE") || answer.questionType.uppercase() == "PG"
                            val answerBg = when {
                                isPg && answer.isCorrect == true -> NeonSuccess
                                isPg && answer.isCorrect == false -> NeonError
                                else -> NeonWarning
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(answerBg.copy(alpha = 0.06f))
                                    .border(1.dp, answerBg.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${idx + 1}. ${answer.questionText}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "${answer.pointsEarned}/${answer.maxPoints} Poin",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = answerBg
                                        )
                                    }
                                    if (isPg) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val icon = if (answer.isCorrect == true) Icons.Default.CheckCircle else Icons.Default.Cancel
                                            Icon(icon, null, tint = answerBg, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Jawaban Siswa: ${answer.chosenChoiceText ?: "-"}",
                                                fontSize = 11.sp,
                                                color = answerBg,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CosmicBlack)
                                                .padding(8.dp)
                                        ) {
                                            Column {
                                                Text("Jawaban Uraian Siswa:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    answer.textAnswer ?: "-",
                                                    fontSize = 11.sp,
                                                    color = TextPrimary,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = GlassBorder)
                }

                // Free-text answer preview
                val dialogContent = submission.content
                if (!dialogContent.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "CATATAN / TEKS PENGUMPULAN SISWA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonBlue,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicBlack)
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(dialogContent, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
                        }
                    }
                }

                if (!submission.fileUrl.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Attachment, null, tint = NeonBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Siswa melampirkan file dokumen", fontSize = 11.sp, color = NeonBlue)
                    }
                }

                // No recorded answers at all — e.g. submissions made while the server failed to
                // persist PG/essay answers. Tell the teacher explicitly instead of an empty sheet.
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
                                    fontSize = 11.sp,
                                    color = TextSecondary,
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = TextTertiary,
                            letterSpacing = 1.sp
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
                                fontWeight = FontWeight.Black,
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
                            inactiveTrackColor = GlassBorder,
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0", fontSize = 10.sp, color = TextTertiary)
                        Text("$maxScore", fontSize = 10.sp, color = TextTertiary)
                    }
                }

                // Feedback
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "CATATAN / KOMENTAR GURU (opsional)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = TextTertiary,
                        letterSpacing = 1.sp
                    )
                    OutlinedTextField(
                        value = feedbackInput,
                        onValueChange = { feedbackInput = it },
                        placeholder = { Text("Tuliskan evaluasi atau catatan koreksi untuk siswa...", fontSize = 11.sp, color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CosmicBlack,
                            unfocusedContainerColor = CosmicBlack,
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = ButtonDefaults.outlinedButtonBorder().copy(
                            brush = Brush.linearGradient(listOf(GlassBorder, GlassBorder))
                        )
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
                            Text("Simpan Nilai", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
