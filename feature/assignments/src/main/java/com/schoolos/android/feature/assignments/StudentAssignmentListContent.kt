package com.schoolos.android.feature.assignments

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
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
import androidx.compose.material3.*
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
import com.schoolos.android.domain.model.Assignment

fun LazyListScope.studentAssignmentListContent(
    activeItems: List<Assignment>,
    dueSoonItems: List<Assignment>,
    completedItems: List<Assignment>,
    selectedTab: String,
    onAssignmentClick: (String) -> Unit,
) {
    val showAll = selectedTab == "Semua"
    val showDueSoon = showAll || selectedTab == "Segera"
    val showActive = showAll || selectedTab == "Aktif"
    val showCompleted = showAll || selectedTab == "Selesai"

    val hasAnyItems = (showDueSoon && dueSoonItems.isNotEmpty()) ||
            (showActive && activeItems.isNotEmpty()) ||
            (showCompleted && completedItems.isNotEmpty())

    if (!hasAnyItems) {
        item {
            EmptyAssignmentState(selectedTab = selectedTab)
        }
    } else {
        // Show due soon (urgent) first
        if (showDueSoon && dueSoonItems.isNotEmpty()) {
            renderAssignmentSection(
                title = "Segera Berakhir",
                items = dueSoonItems,
                accentColor = NeonWarning,
                onAssignmentClick = onAssignmentClick
            )
        }

        // Show active assignments
        if (showActive && activeItems.isNotEmpty()) {
            renderAssignmentSection(
                title = "Sedang Berlangsung",
                items = activeItems,
                accentColor = StudentNeon,
                onAssignmentClick = onAssignmentClick
            )
        }

        // Show completed assignments
        if (showCompleted && completedItems.isNotEmpty()) {
            renderAssignmentSection(
                title = "Tugas Selesai",
                items = completedItems,
                accentColor = NeonSuccess,
                onAssignmentClick = onAssignmentClick
            )
        }
    }
}

private fun LazyListScope.renderAssignmentSection(
    title: String,
    items: List<Assignment>,
    accentColor: Color,
    onAssignmentClick: (String) -> Unit,
) {
    if (items.isEmpty()) return

    item {
        SectionHeader(
            title = title,
            count = items.size,
            accentColor = accentColor,
        )
    }

    items(items, key = { it.id }) { assignment ->
        StudentAssignmentCard(
            assignment = assignment,
            accentColor = accentColor,
            onClick = { onAssignmentClick(assignment.id) }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    accentColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 12.dp, bottom = 4.dp)
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
            Text(
                "$count",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StudentAssignmentCard(
    assignment: Assignment,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val dueInfo = dueDateInfo(assignment.dueAt)
    val isOverdue = dueInfo?.label?.contains("Terlambat") == true
    val isCompleted = assignment.status == "submitted" || assignment.status == "graded"
    val title = assignment.title.ifBlank { "Tugas Tanpa Judul" }

    val subjectLabel = assignment.subjectName ?: assignment.className ?: "Umum"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(
                width = 0.5.dp,
                color = if (isOverdue) NeonWarning.copy(alpha = 0.4f) else GlassBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            // TOP ROW: SUBJECT TAG & SCORE PILL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subject / Class pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = subjectLabel.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Max Score pill
                if (assignment.maxScore > 0) {
                    Text(
                        text = "${assignment.maxScore} Poin",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // MAIN CONTENT: ICON + TITLE + CLASS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = if (isCompleted) CoreR.drawable.ic_modern_check_circle else CoreR.drawable.ic_modern_tasks),
                        contentDescription = null,
                        tint = if (isCompleted) NeonSuccess else StudentNeon,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!assignment.className.isNullOrBlank() && assignment.subjectName != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = assignment.className!!,
                            fontSize = 11.sp,
                            color = TextTertiary,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // BOTTOM ROW: DUE DATE BADGE & STATUS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_clock),
                        contentDescription = null,
                        tint = when {
                            isOverdue -> NeonWarning
                            dueInfo != null -> dueInfo.color
                            else -> TextTertiary
                        },
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = dueInfo?.label ?: "Tanpa batas waktu",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = when {
                            isOverdue -> NeonWarning
                            dueInfo != null -> dueInfo.color
                            else -> TextSecondary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                val statusText = when {
                    isCompleted -> "Selesai"
                    isOverdue -> "Terlambat"
                    else -> "Belum Dikumpul"
                }
                val statusColor = when {
                    isCompleted -> NeonSuccess
                    isOverdue -> NeonWarning
                    else -> TextTertiary
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isCompleted) NeonSuccess.copy(alpha = 0.12f) else com.schoolos.android.core.designsystem.CosmicSurface3)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAssignmentState(selectedTab: String) {
    val (title, subtitle, accentColor) = when (selectedTab) {
        "Segera" -> Triple(
            "Tidak Ada Tugas Mendesak",
            "Bagus sekali! Semua tugas aman dan tidak ada tenggat waktu yang mepet.",
            NeonSuccess
        )
        "Aktif" -> Triple(
            "Semua Tugas Aktif Selesai",
            "Tidak ada tugas yang sedang berjalan saat ini. Kerja bagus!",
            StudentNeon
        )
        "Selesai" -> Triple(
            "Belum Ada Tugas Selesai",
            "Tugas yang telah kamu kumpulkan akan tersimpan dan tampil di sini.",
            TextTertiary
        )
        else -> Triple(
            "Semua Tugas Beres! 🎉",
            "Belum ada penugasan baru dari bapak/ibu guru. Waktunya istirahat atau pelajari materi berikutnya.",
            NeonSuccess
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 32.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(0.5.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (selectedTab == "Selesai") CoreR.drawable.ic_modern_tasks else CoreR.drawable.ic_modern_check_circle
                    ),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextTertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(com.schoolos.android.core.designsystem.CosmicSurface2)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Tarik ke bawah untuk memuat ulang",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }
    }
}