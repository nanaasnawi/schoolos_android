package com.schoolos.android.feature.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val icon = subjectIcon(summary.subjectName)
    val scoreColor = when {
        summary.finalScore >= 85.0 -> NeonSuccess
        summary.finalScore >= 75.0 -> StudentNeon
        summary.finalScore >= 65.0 -> NeonWarning
        summary.finalScore > 0.0 -> NeonError
        else -> TextTertiary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicSurface2)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        summary.subjectName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (summary.finalScore > 0) "%.1f".format(summary.finalScore) else "-",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = scoreColor,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (summary.letterGrade.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(scoreColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(summary.letterGrade, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        "${summary.gradedComponentCount}/${summary.componentCount} Dinilai",
                        fontSize = 11.sp,
                        color = TextTertiary,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
