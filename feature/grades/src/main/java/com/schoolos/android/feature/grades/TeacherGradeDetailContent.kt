package com.schoolos.android.feature.grades

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.SubjectGradeDetail

@Composable
fun TeacherGradeDetailContent(
    detail: SubjectGradeDetail
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val teacherNeon = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    val components = detail.components
    val threshold = detail.summary.passingThreshold
    val graded = components.mapNotNull { it.rawScore ?: it.weightedScore }
    val avg = if (graded.isNotEmpty()) graded.average() else 0.0
    val passingCount = graded.count { it >= threshold }
    val totalCount = components.size

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Rincian Penilaian",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = textPrimary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        if (components.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = teacherNeon,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        "Belum ada data nilai masuk untuk mata pelajaran ini.",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Nilai Sumatif M1, M2, M3, ATS, SAS, serta deskripsi ketercapaian KKTP dapat diinput langsung pada Buku Nilai Siswa.",
                        fontSize = 11.5.sp,
                        color = textTertiary,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            components.forEach { entry ->
                val score = entry.rawScore ?: entry.weightedScore ?: 0.0
                val isPassed = score >= threshold
                val status = when {
                    score <= 0.0 -> "Belum Dinilai"
                    isPassed -> "Tuntas KKTP"
                    else -> "Perlu Penguatan"
                }
                TeacherStudentGradeRow(entry.componentName, score, status, isPassed)
            }
        }

        // ── Statistik Kelas Card ─────────────────────
        if (components.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Statistik Penilaian",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    SummaryRowCompact("Rata-rata Nilai", if (graded.isNotEmpty()) "%.1f".format(avg) else "-")
                    HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))
                    SummaryRowCompact("Tuntas KKTP", "$passingCount / $totalCount Selesai")
                    HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))
                    SummaryRowCompact("Ketercapaian KKTP", if (totalCount > 0) "${((passingCount.toDouble() / totalCount) * 100).toInt()}%" else "-")
                }
            }
        }
    }
}

@Composable
private fun TeacherStudentGradeRow(name: String, score: Double, status: String, isPassed: Boolean) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val warningColor = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    val blueAccent = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(blueAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_modern_profile),
                    contentDescription = null,
                    tint = blueAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = textPrimary,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (score > 0.0) "%.1f".format(score) else "-",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = textPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = status,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPassed) successColor else if (score > 0.0) warningColor else textTertiary
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                contentDescription = null,
                tint = textTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SummaryRowCompact(label: String, value: String) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.5.sp, color = textSecondary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = textPrimary)
    }
}
