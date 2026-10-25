package com.schoolos.android.feature.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.common.DapodikPeriod
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.ClassStudent
import com.schoolos.android.domain.model.SubjectGradeSummary
import com.schoolos.android.domain.model.computeKurikulumMerdekaGrade
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// 1. KURIKULUM MERDEKA GRADEBOOK (TEACHER & PRINCIPAL SSOT)
// ─────────────────────────────────────────────────────────────────────────────

fun LazyListScope.kurikulumMerdekaGradebookContent(
    totalStudents: Int,
    averageScore: Double,
    passingPercentage: Int,
    passedCount: Int,
    remedialCount: Int,
    selectedSubjectName: String,
    availableSubjects: List<AcademicSubject>,
    selectedClassFilter: String,
    availableClasses: List<AcademicClass>,
    activeScheme: String,
    searchQuery: String,
    filteredRows: List<StudentGradebookRow>,
    onSelectSubject: (AcademicSubject) -> Unit,
    onSelectClassFilter: (String) -> Unit,
    onSelectScheme: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onOpenDetail: (StudentGradebookRow) -> Unit,
) {
    // ── 1. KURIKULUM MERDEKA HERO BANNER ──────────────────────────────────
    item {
        GradebookHeroBanner(
            selectedSubjectName = selectedSubjectName,
            activeScheme = activeScheme,
            onSelectScheme = onSelectScheme,
        )
    }

    // ── 2. 4 KPI SUMMARY CARDS (MATCHES WEB DASHBOARD 1:1) ────────────────
    item {
        GradebookKpiGrid(
            totalStudents = totalStudents,
            averageScore = averageScore,
            selectedSubjectName = selectedSubjectName,
            passingPercentage = passingPercentage,
            passedCount = passedCount,
            remedialCount = remedialCount,
        )
    }

    // ── 3. MAPEL SELECTOR CHIPS ──────────────────────────────────────────
    if (availableSubjects.isNotEmpty()) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "MATA PELAJARAN (${availableSubjects.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = TeacherNeon,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableSubjects.forEach { subj ->
                        val isSelected = subj.name.equals(selectedSubjectName, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) TeacherNeon.copy(alpha = 0.2f) else CosmicNavy)
                                .border(
                                    if (isSelected) 1.dp else 0.5.dp,
                                    if (isSelected) TeacherNeon else GlassBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectSubject(subj) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = subj.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TeacherNeon else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    // ── 4. ROMBEL SELECTOR CHIPS ─────────────────────────────────────────
    item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "PILIH ROMBEL / KELAS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TeacherNeon,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Semua Rombel
                val isAllSelected = selectedClassFilter.isBlank() || selectedClassFilter.equals("ALL", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAllSelected) TeacherNeon.copy(alpha = 0.2f) else CosmicNavy)
                        .border(
                            if (isAllSelected) 1.dp else 0.5.dp,
                            if (isAllSelected) TeacherNeon else GlassBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectClassFilter("ALL") }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Semua Rombel ($totalStudents)",
                        fontSize = 12.sp,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAllSelected) TeacherNeon else TextSecondary
                    )
                }

                // Individual classes
                availableClasses.forEach { cls ->
                    val isSelected = cls.name.equals(selectedClassFilter, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) TeacherNeon.copy(alpha = 0.2f) else CosmicNavy)
                            .border(
                                if (isSelected) 1.dp else 0.5.dp,
                                if (isSelected) TeacherNeon else GlassBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectClassFilter(cls.name) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cls.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TeacherNeon else TextSecondary
                        )
                    }
                }
            }
        }
    }

    // ── 5. SEARCH BAR ────────────────────────────────────────────────────
    item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicNavy)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Cari NISN atau nama siswa...",
                                color = TextTertiary,
                                fontSize = 13.sp
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextTertiary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onSearchQueryChanged("") }
                    )
                }
            }
        }
    }

    // ── 6. SECTION HEADER ────────────────────────────────────────────────
    item {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DAFTAR NILAI SISWA (${filteredRows.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TeacherNeon,
                letterSpacing = 1.sp
            )
            Text(
                text = selectedSubjectName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp)
            )
        }
    }

    // ── 7. STUDENT GRADEBOOK ROSTER ──────────────────────────────────────
    if (filteredRows.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MenuBook, null, tint = TextTertiary, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tidak ada peserta didik yang sesuai filter atau pencarian.",
                        fontSize = 13.sp,
                        color = TextTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        items(filteredRows, key = { it.studentId }) { row ->
            StudentGradebookItemCard(
                row = row,
                onClickDetail = { onOpenDetail(row) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. HERO BANNER WITH KURIKULUM MERDEKA FORMULA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GradebookHeroBanner(
    selectedSubjectName: String,
    activeScheme: String,
    onSelectScheme: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        TeacherContainer.copy(alpha = 0.4f),
                        CosmicNavy,
                        CosmicSurface2
                    )
                )
            )
            .border(1.dp, TeacherNeon.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TeacherNeon.copy(alpha = 0.12f))
                    .border(0.5.dp, TeacherNeon.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "STANDAR KURIKULUM MERDEKA • KEMENDIKDASMEN RI",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = TeacherNeon,
                    letterSpacing = 0.8.sp
                )
            }

            // Title & Subtitle
            Column {
                Text(
                    text = "Buku Nilai (Teacher Gradebook)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Asesmen Sumatif Lingkup Materi & Evaluasi KKTP di PKBM AS-SALAFIYAH • ${DapodikPeriod.getFullPeriodLabel()}",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }

            // Scheme Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CosmicBlack.copy(alpha = 0.5f))
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Skema A
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeScheme == "A") TeacherNeon.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            if (activeScheme == "A") 1.dp else 0.dp,
                            if (activeScheme == "A") TeacherNeon else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectScheme("A") }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Skema A (ATS & SAS)",
                        fontSize = 11.sp,
                        fontWeight = if (activeScheme == "A") FontWeight.Bold else FontWeight.Medium,
                        color = if (activeScheme == "A") TeacherNeon else TextTertiary
                    )
                }

                // Skema B
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeScheme == "B") TeacherNeon.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            if (activeScheme == "B") 1.dp else 0.dp,
                            if (activeScheme == "B") TeacherNeon else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectScheme("B") }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Skema B (Murni Modul)",
                        fontSize = 11.sp,
                        fontWeight = if (activeScheme == "B") FontWeight.Bold else FontWeight.Medium,
                        color = if (activeScheme == "B") TeacherNeon else TextTertiary
                    )
                }
            }

            // Formula banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TeacherNeon.copy(alpha = 0.08f))
                    .border(0.5.dp, TeacherNeon.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (activeScheme == "A")
                        "Bobot Kemendikdasmen: Rata-Rata Modul (60%) • ATS (20%) • SAS (20%) | Formatif Kualitatif 0%"
                    else
                        "Bobot Kemendikdasmen: 100% Rata-Rata Sumatif Modul (Pendidikan Kesetaraan PKBM)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TeacherNeon
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. 4 KPI SUMMARY CARDS GRID
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GradebookKpiGrid(
    totalStudents: Int,
    averageScore: Double,
    selectedSubjectName: String,
    passingPercentage: Int,
    passedCount: Int,
    remedialCount: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Total Siswa
            KpiStatCard(
                title = "Total Siswa Terdaftar",
                value = "$totalStudents",
                subtext = "Peserta Didik",
                icon = Icons.Default.Group,
                accentColor = NeonBlue,
                modifier = Modifier.weight(1f)
            )

            // Card 2: Rata-Rata Nilai Mapel
            KpiStatCard(
                title = "Rata-Rata Nilai Mapel",
                value = if (averageScore > 0) String.format(Locale.US, "%.1f", averageScore) else "0.0",
                subtext = "/ 100 • ${selectedSubjectName.take(15)}...",
                icon = Icons.Default.TrendingUp,
                accentColor = Color(0xFFA78BFA),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 3: Ketuntasan KKTP
            KpiStatCard(
                title = "Ketuntasan KKTP (≥ 75)",
                value = "$passingPercentage%",
                subtext = "$passedCount Siswa Tuntas",
                icon = Icons.Default.CheckCircle,
                accentColor = NeonSuccess,
                modifier = Modifier.weight(1f)
            )

            // Card 4: Perlu Pembinaan
            KpiStatCard(
                title = "Perlu Pembinaan (< 75)",
                value = "$remedialCount",
                subtext = "Perlu Remedial",
                icon = Icons.Default.Warning,
                accentColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )

            Text(
                text = subtext,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. STUDENT GRADEBOOK ITEM CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StudentGradebookItemCard(
    row: StudentGradebookRow,
    onClickDetail: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top Row: Student Name, NISN, Rombel Pill, and Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = row.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "NISN: ${row.nisn}",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonBlue.copy(alpha = 0.12f))
                                .border(0.5.dp, NeonBlue.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = row.className,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonBlue
                            )
                        }
                    }
                }

                // Status Chip
                GradeStatusPill(status = row.statusKktp)
            }

            // Scores Matrix Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CosmicBlack.copy(alpha = 0.45f))
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Row 1: Sumatif Modul (M1, M2, M3, Rerata)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScoreCell(label = "M1", value = row.sumatifM1.toInt().toString())
                        ScoreCell(label = "M2", value = row.sumatifM2.toInt().toString())
                        ScoreCell(label = "M3", value = row.sumatifM3.toInt().toString())
                        ScoreCell(
                            label = "RERATA",
                            value = if (row.avgSumatif > 0) String.format(Locale.US, "%.1f", row.avgSumatif) else "-",
                            isHighlight = true
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(GlassBorder)
                    )

                    // Row 2: Ujian & Evaluasi Akhir (ATS, SAS, N. Akhir, Predikat)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScoreCell(label = "ATS (20%)", value = row.ats.toInt().toString())
                        ScoreCell(label = "SAS (20%)", value = row.sas.toInt().toString())
                        ScoreCell(
                            label = "N. AKHIR",
                            value = if (row.finalScore > 0) row.finalScore.toInt().toString() else "-",
                            isHighlight = true,
                            highlightColor = if (row.finalScore >= 75) NeonSuccess else if (row.finalScore > 0) NeonError else TextTertiary
                        )
                        ScoreCell(
                            label = "PRED",
                            value = row.letterGrade,
                            isHighlight = true
                        )
                    }
                }
            }

            // Bottom CTA: Detail Capaian & Input Nilai
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TeacherNeon.copy(alpha = 0.12f))
                        .border(0.5.dp, TeacherNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onClickDetail)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = TeacherNeon,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Detail Capaian",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TeacherNeon
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreCell(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    highlightColor: Color = TeacherNeon,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = TextTertiary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isHighlight) FontWeight.Black else FontWeight.Bold,
            color = if (isHighlight) highlightColor else TextPrimary
        )
    }
}

@Composable
private fun GradeStatusPill(status: String) {
    val (bg, border, text, label) = when (status) {
        "Tuntas KKTP" -> Quad(
            NeonSuccess.copy(alpha = 0.12f),
            NeonSuccess.copy(alpha = 0.4f),
            NeonSuccess,
            "Tuntas KKTP"
        )
        "Perlu Remedial" -> Quad(
            NeonError.copy(alpha = 0.12f),
            NeonError.copy(alpha = 0.4f),
            NeonError,
            "Perlu Remedial"
        )
        else -> Quad(
            Color(0xFF334155).copy(alpha = 0.4f),
            Color(0xFF64748B).copy(alpha = 0.4f),
            Color(0xFFCBD5E1),
            "Belum Diinput"
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = text
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ─────────────────────────────────────────────────────────────────────────────
// 5. INTERACTIVE DETAIL CAPAIAN & INPUT NILAI MODAL SHEET
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeDetailModalSheet(
    row: StudentGradebookRow,
    selectedSubjectName: String,
    activeScheme: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (m1: Double, m2: Double, m3: Double, ats: Double, sas: Double, highest: String, lowest: String, notes: String) -> Unit,
) {
    var m1Text by remember { mutableStateOf(if (row.sumatifM1 > 0) row.sumatifM1.toInt().toString() else "") }
    var m2Text by remember { mutableStateOf(if (row.sumatifM2 > 0) row.sumatifM2.toInt().toString() else "") }
    var m3Text by remember { mutableStateOf(if (row.sumatifM3 > 0) row.sumatifM3.toInt().toString() else "") }
    var atsText by remember { mutableStateOf(if (row.ats > 0) row.ats.toInt().toString() else "") }
    var sasText by remember { mutableStateOf(if (row.sas > 0) row.sas.toInt().toString() else "") }
    var highestDesc by remember { mutableStateOf(row.highestCompetencyDesc) }
    var lowestDesc by remember { mutableStateOf(row.lowestCompetencyDesc) }
    var notesText by remember { mutableStateOf(row.formatifNotes) }

    val m1Val = m1Text.toDoubleOrNull() ?: 0.0
    val m2Val = m2Text.toDoubleOrNull() ?: 0.0
    val m3Val = m3Text.toDoubleOrNull() ?: 0.0
    val atsVal = atsText.toDoubleOrNull() ?: 0.0
    val sasVal = sasText.toDoubleOrNull() ?: 0.0

    val previewEval = remember(m1Val, m2Val, m3Val, atsVal, sasVal, activeScheme) {
        computeKurikulumMerdekaGrade(m1Val, m2Val, m3Val, atsVal, sasVal, activeScheme)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CosmicNavy,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(GlassBorder2)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Input Nilai & Capaian",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${row.name} • ${row.className} • $selectedSubjectName",
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = TextTertiary)
                }
            }

            // Live Calculated Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TeacherNeon.copy(alpha = 0.08f))
                    .border(0.5.dp, TeacherNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScoreCell(label = "RERATA MODUL", value = if (previewEval.avgSumatif > 0) previewEval.avgSumatif.toString() else "-")
                    ScoreCell(label = "NILAI AKHIR", value = if (previewEval.finalScore > 0) previewEval.finalScore.toInt().toString() else "-", isHighlight = true)
                    ScoreCell(label = "PREDIKAT", value = previewEval.letterGrade, isHighlight = true)
                    GradeStatusPill(status = previewEval.statusKktp)
                }
            }

            // Numerical Inputs Grid
            Text(
                text = "SUMATIF MODUL & ASESMEN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TeacherNeon,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = m1Text,
                    onValueChange = { m1Text = it.take(3) },
                    label = { Text("M1", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TeacherNeon,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = m2Text,
                    onValueChange = { m2Text = it.take(3) },
                    label = { Text("M2", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TeacherNeon,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = m3Text,
                    onValueChange = { m3Text = it.take(3) },
                    label = { Text("M3", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TeacherNeon,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = atsText,
                    onValueChange = { atsText = it.take(3) },
                    label = { Text("ATS (20%)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TeacherNeon,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = sasText,
                    onValueChange = { sasText = it.take(3) },
                    label = { Text("SAS (20%)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TeacherNeon,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    singleLine = true
                )
            }

            // Qualitative Competency Descriptions
            Text(
                text = "DESKRIPSI CAPAIAN KOMPETENSI (KURIKULUM MERDEKA)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TeacherNeon,
                letterSpacing = 1.sp
            )

            OutlinedTextField(
                value = highestDesc,
                onValueChange = { highestDesc = it },
                label = { Text("Capaian Kompetensi Tertinggi (Optimal)", fontSize = 11.sp) },
                placeholder = { Text("Contoh: Menunjukkan pemahaman istimewa dalam...", fontSize = 11.sp, color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TeacherNeon,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                )
            )

            OutlinedTextField(
                value = lowestDesc,
                onValueChange = { lowestDesc = it },
                label = { Text("Capaian Kompetensi Terendah (Perlu Bimbingan)", fontSize = 11.sp) },
                placeholder = { Text("Contoh: Perlu peningkatan bimbingan pada...", fontSize = 11.sp, color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TeacherNeon,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                )
            )

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Catatan Evaluasi Formatif / Kualitatif", fontSize = 11.sp) },
                placeholder = { Text("Refleksi perkembangan karakter & modul belajar...", fontSize = 11.sp, color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TeacherNeon,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                )
            )

            Spacer(Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Text("Batal")
                }

                Button(
                    onClick = {
                        onSave(m1Val, m2Val, m3Val, atsVal, sasVal, highestDesc, lowestDesc, notesText)
                    },
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(containerColor = TeacherNeon, contentColor = Color.Black),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                            Text("Simpan Nilai & Capaian", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. MODULE OVERVIEW CARDS LIST (REKAP 18 MODUL)
// ─────────────────────────────────────────────────────────────────────────────

fun LazyListScope.teacherGradebookContent(
    subjects: List<SubjectGradeSummary>,
    className: String,
    teacherSubject: String? = null,
    totalSubjectCount: Int = subjects.size,
    selectedFilter: String = "Semua Mapel",
    isPrincipal: Boolean = false,
    onSelectFilter: (String) -> Unit = {},
    onSubjectClick: (String, String) -> Unit,
) {
    item {
        TeacherClassHeroHeader(
            subjects = subjects,
            className = className,
            teacherSubject = teacherSubject,
            isPrincipal = isPrincipal,
        )
    }

    item {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isPrincipal) "LEGER NILAI SELURUH MODUL KESETARAAN"
                else if (!teacherSubject.isNullOrBlank()) "LEGER NILAI MAPEL DIAMPU${if (className.isNotBlank()) " • $className" else ""}"
                else if (className.isNotBlank()) "LEGER NILAI ROMBEL $className"
                else "REKAP NILAI MODUL DIAMPU",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TeacherNeon,
                letterSpacing = 1.sp
            )
            Text(
                if (!teacherSubject.isNullOrBlank()) "${subjects.size} Mapel Diampu" else "${subjects.size} Mapel",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextTertiary
            )
        }
    }

    val dynamicCategories = (listOf("Semua Mapel") + subjects.map { it.category }.filter { it.isNotBlank() && it != "Wajib" }).distinct()
    if (dynamicCategories.size > 1) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                dynamicCategories.forEach { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) TeacherNeon.copy(alpha = 0.15f) else CosmicNavy)
                            .border(
                                0.5.dp,
                                if (isSelected) TeacherNeon.copy(alpha = 0.4f) else GlassBorder,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable { onSelectFilter(filter) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            filter,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) TeacherNeon else TextTertiary,
                        )
                    }
                }
            }
        }
    }

    if (subjects.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (!teacherSubject.isNullOrBlank()) "Belum ada nilai modul tercatat untuk mata pelajaran $teacherSubject."
                        else if (className.isNotBlank()) "Belum ada modul pelajaran tercatat untuk Rombel $className."
                        else "Belum ada modul mata pelajaran tercatat.",
                        color = TextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        items(subjects, key = { it.subjectId }) { subject ->
            TeacherSubjectGradeCard(
                summary = subject,
                className = className,
                onClick = { onSubjectClick(subject.subjectId, subject.subjectName) }
            )
        }
    }
}

@Composable
private fun TeacherClassHeroHeader(
    subjects: List<SubjectGradeSummary>,
    className: String,
    teacherSubject: String? = null,
    isPrincipal: Boolean = false,
) {
    val gradedSubjects = subjects.filter { it.finalScore > 0.0 && it.gradedComponentCount > 0 }
    val hasGrades = gradedSubjects.isNotEmpty()
    val avgScore = if (hasGrades) gradedSubjects.map { it.finalScore }.average() else 0.0

    val headerBadgeText = when {
        isPrincipal -> "KEPALA PKBM • EKSEKUTIF LEGER KESETARAAN"
        !teacherSubject.isNullOrBlank() -> "GURU MAPEL ${teacherSubject.uppercase()} • ${DapodikPeriod.getActiveSemester().uppercase()}"
        className.isNotBlank() -> "TUTOR PAMONG • ${DapodikPeriod.getActiveSemester().uppercase()}"
        else -> "TUTOR MODUL / MAPEL • ${DapodikPeriod.getActiveSemester().uppercase()}"
    }

    val headerTitleText = when {
        isPrincipal -> "Leger Capaian Belajar PKBM"
        !teacherSubject.isNullOrBlank() -> teacherSubject
        className.isNotBlank() -> if (className.startsWith("Paket", ignoreCase = true)) className else "Rombel $className"
        else -> "Buku Penilaian Modul & Capaian Belajar"
    }

    val headerSubtitleText = when {
        isPrincipal -> "Monitoring Capaian KKTP & Mutu Belajar Rombel • ${DapodikPeriod.getFullPeriodLabel()}"
        !teacherSubject.isNullOrBlank() && className.isNotBlank() -> "Penilaian Sumatif Modul & Capaian KKTP • Rombel $className • ${DapodikPeriod.getFullPeriodLabel()}"
        className.isNotBlank() -> "Leger Nilai Rombel • ${DapodikPeriod.getFullPeriodLabel()}"
        else -> "Penilaian Capaian & Evaluasi Modul • ${DapodikPeriod.getFullPeriodLabel()}"
    }

    val passRateText = if (hasGrades) {
        val passedCount = gradedSubjects.count { it.isPassed }
        "${(passedCount * 100) / gradedSubjects.size}% Tuntas"
    } else {
        "—"
    }

    val isDark = LocalIsDarkTheme.current
    val heroBg = if (isDark) {
        Brush.linearGradient(
            listOf(
                TeacherContainer.copy(alpha = 0.35f),
                CosmicNavy,
                CosmicSurface2
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                TeacherNeon.copy(alpha = 0.08f),
                CosmicNavy,
                CosmicSurface2.copy(alpha = 0.5f)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(heroBg)
            .border(
                width = 1.dp,
                color = if (isDark) TeacherNeon.copy(alpha = 0.35f) else TeacherNeon.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(TeacherNeon.copy(alpha = 0.12f))
                .border(1.dp, TeacherNeon.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TeacherNeon)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = headerBadgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = TeacherNeon,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = headerTitleText,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = headerSubtitleText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = TextSecondary
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (hasGrades) TeacherNeon.copy(alpha = 0.12f) else CosmicNavy)
                .border(0.5.dp, if (hasGrades) TeacherNeon.copy(alpha = 0.35f) else GlassBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (hasGrades) TeacherNeon else Color(0xFF94A3B8))
            )
            Text(
                text = if (hasGrades) "Aktif Dinilai" else "Belum Dinilai",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (hasGrades) TeacherNeon else TextTertiary
            )
            Text(
                text = if (hasGrades) "• Rerata %.1f".format(avgScore) else "• Menunggu input nilai",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CosmicNavy.copy(alpha = 0.6f))
                .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                .padding(vertical = 10.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Modul / Pelajaran",
                    fontSize = 11.sp,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${subjects.size} Modul",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(GlassBorder)
            )

            Column {
                Text(
                    text = "Target KKTP",
                    fontSize = 11.sp,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "75",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(GlassBorder)
            )

            Column {
                Text(
                    text = "Ketercapaian",
                    fontSize = 11.sp,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = passRateText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasGrades) TeacherNeon else TextTertiary
                )
            }
        }
    }
}

@Composable
fun TeacherSubjectGradeCard(
    summary: SubjectGradeSummary,
    className: String,
    onClick: () -> Unit,
) {
    val isGraded = summary.finalScore > 0.0
    val score = summary.finalScore
    val isPassing = summary.isPassed
    val isDark = LocalIsDarkTheme.current
    val accentColor = if (isGraded) (if (isPassing) TeacherNeon else Color(0xFFFF5252)) else TeacherNeon

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(
                width = if (isGraded) 1.dp else 0.5.dp,
                color = if (isGraded) accentColor.copy(alpha = 0.3f) else GlassBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = subjectIcon(summary.subjectName)
                val gradient = subjectGradient(summary.subjectName)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.subjectName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CosmicSurface2)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "KKTP 75",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary
                            )
                        }

                        if (summary.category.isNotBlank() && summary.category != "Wajib") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TeacherNeon.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = summary.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TeacherNeon
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isGraded) "%.1f".format(score) else "—",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isGraded) accentColor else TextTertiary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = when {
                            !isGraded -> "Belum Dinilai"
                            isPassing -> "Tuntas KKTP"
                            else -> "Perlu Remedial"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            !isGraded -> TextTertiary
                            isPassing -> TeacherNeon
                            else -> Color(0xFFFF5252)
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(GlassBorder)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                ) {
                    val progress = if (summary.componentCount > 0) {
                        (summary.gradedComponentCount.toFloat() / summary.componentCount.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (isGraded) accentColor else TextTertiary.copy(alpha = 0.4f),
                        trackColor = CosmicSurface2,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (summary.componentCount > 0) {
                            "${summary.gradedComponentCount}/${summary.componentCount} Nilai"
                        } else {
                            "0 Nilai Masuk"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextTertiary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Buka Leger", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}
