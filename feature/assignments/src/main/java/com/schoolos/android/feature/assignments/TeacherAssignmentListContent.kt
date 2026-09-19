package com.schoolos.android.feature.assignments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
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
import com.schoolos.android.domain.model.Assignment

fun LazyListScope.teacherAssignmentListContent(
    activeItems: List<Assignment>,
    dueSoonItems: List<Assignment>,
    selectedTab: String,
    onAssignmentClick: (String) -> Unit
) {
    if (selectedTab == "Semua" || selectedTab == "Perlu Dinilai") {
        renderTeacherSection("Perlu Dinilai", dueSoonItems, onAssignmentClick)
    }
    if (selectedTab == "Semua" || selectedTab == "Aktif") {
        renderTeacherSection("Tugas Berjalan", activeItems, onAssignmentClick)
    }
}

private fun LazyListScope.renderTeacherSection(
    title: String,
    items: List<Assignment>,
    onAssignmentClick: (String) -> Unit,
) {
    if (items.isEmpty()) return

    item {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 2.dp, top = 12.dp, bottom = 4.dp)
        ) {
            Text(
                title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextTertiary,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("${items.size}", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    items(items, key = { it.id }) { assignment ->
        TeacherAssignmentCard(assignment = assignment, onClick = { onAssignmentClick(assignment.id) })
    }
}

@Composable
private fun TeacherAssignmentCard(
    assignment: Assignment,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        assignment.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    val subtitle = listOfNotNull(
                        assignment.subjectName,
                        assignment.className?.let { "Kelas $it" }
                    ).joinToString(" • ").ifBlank {
                        assignment.assignmentType.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                    }
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        color = TextTertiary,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.height(12.dp))

            // METADATA STRIP
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(Icons.Default.DateRange, null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    val dueIso = assignment.dueAt
                    val dueLabel = if (!dueIso.isNullOrBlank()) {
                        dueIso.take(16).replace('T', ' ')
                    } else {
                        "Tanpa Batas Waktu"
                    }
                    Text(
                        dueLabel,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (assignment.isActive) NeonSuccess.copy(alpha = 0.12f) else com.schoolos.android.core.designsystem.CosmicSurface3)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Maks. ${assignment.maxScore} Poin",
                        fontSize = 10.sp,
                        color = if (assignment.isActive) NeonSuccess else TextTertiary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
