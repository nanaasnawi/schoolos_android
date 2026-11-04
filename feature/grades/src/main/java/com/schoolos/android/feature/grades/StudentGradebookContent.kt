package com.schoolos.android.feature.grades

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.SubjectGradeSummary

fun LazyListScope.studentGradebookContent(
    subjects: List<SubjectGradeSummary>,
    onSubjectClick: (String, String) -> Unit
) {
    items(subjects, key = { it.subjectId }) { subject ->
        StudentSubjectGradeCard(
            summary = subject,
            onClick = { onSubjectClick(subject.subjectId, subject.subjectName) },
        )
    }
}

@Composable
private fun StudentSubjectGradeCard(summary: SubjectGradeSummary, onClick: () -> Unit) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val icon = subjectIcon(summary.subjectName)
    val kktp = summary.passingThreshold
    val hasScore = summary.finalScore > 0.0
    val isPassed = summary.finalScore >= kktp
    val scoreColor = when {
        summary.finalScore >= kktp + 10.0 -> NeonSuccess
        summary.finalScore >= kktp -> StudentNeon
        summary.finalScore >= kktp - 10.0 -> NeonWarning
        summary.finalScore > 0.0 -> NeonError
        else -> textTertiary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subject squircle icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(innerCardBg)
                        .border(0.5.dp, innerCardBorder, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (hasScore) scoreColor else NeonBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.subjectName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = innerCardBg,
                            border = BorderStroke(0.5.dp, innerCardBorder)
                        ) {
                            Text(
                                text = "${summary.gradedComponentCount}/${summary.componentCount} Modul Dinilai",
                                fontSize = 10.5.sp,
                                color = textTertiary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (summary.category.isNotBlank() && summary.category != "Wajib") {
                            Text(
                                text = "• ${summary.category}",
                                fontSize = 11.sp,
                                color = textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (hasScore) "%.1f".format(java.util.Locale.US, summary.finalScore) else "—",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = scoreColor,
                    )
                    if (summary.letterGrade.isNotBlank() && summary.letterGrade != "-") {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = scoreColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, scoreColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Predikat ${summary.letterGrade}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Target KKTP ${kktp.toInt()}",
                            fontSize = 9.5.sp,
                            color = textTertiary
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Icon(
                    painter = painterResource(CoreR.drawable.ic_modern_chevron_right),
                    contentDescription = null,
                    tint = textTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Progress bar to KKTP
            if (hasScore) {
                val progressFloat = (summary.finalScore.toFloat() / 100f).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = scoreColor,
                        trackColor = innerCardBg
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isPassed) "Memenuhi KKTP (≥ ${kktp.toInt()})" else "Di bawah KKTP (< ${kktp.toInt()})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isPassed) NeonSuccess else NeonWarning
                        )
                        Text(
                            text = "${(progressFloat * 100).toInt()}% capaian",
                            fontSize = 10.sp,
                            color = textTertiary
                        )
                    }
                }
            }
        }
    }
}
