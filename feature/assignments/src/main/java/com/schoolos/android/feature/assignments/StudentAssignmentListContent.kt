package com.schoolos.android.feature.assignments

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
import com.schoolos.android.core.common.formatPublishTimestamp
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
                type = AssignmentSectionType.DUE_SOON,
                onAssignmentClick = onAssignmentClick
            )
        }

        // Show active assignments
        if (showActive && activeItems.isNotEmpty()) {
            renderAssignmentSection(
                title = "Sedang Berlangsung",
                items = activeItems,
                type = AssignmentSectionType.ACTIVE,
                onAssignmentClick = onAssignmentClick
            )
        }

        // Show completed assignments
        if (showCompleted && completedItems.isNotEmpty()) {
            renderAssignmentSection(
                title = "Tugas Selesai",
                items = completedItems,
                type = AssignmentSectionType.COMPLETED,
                onAssignmentClick = onAssignmentClick
            )
        }
    }
}

private enum class AssignmentSectionType {
    DUE_SOON,
    ACTIVE,
    COMPLETED
}

private fun LazyListScope.renderAssignmentSection(
    title: String,
    items: List<Assignment>,
    type: AssignmentSectionType,
    onAssignmentClick: (String) -> Unit,
) {
    if (items.isEmpty()) return

    item {
        SectionHeader(
            title = title,
            count = items.size,
        )
    }

    items(items, key = { it.id }) { assignment ->
        val accentColor = when (type) {
            AssignmentSectionType.DUE_SOON -> NeonWarning
            AssignmentSectionType.ACTIVE -> StudentNeon
            AssignmentSectionType.COMPLETED -> NeonSuccess
        }
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
) {
    val isDark = LocalIsDarkTheme.current
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(innerCardBg)
                .border(1.dp, innerCardBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$count Tugas",
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
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
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val studentAccentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    val dueInfo = dueDateInfo(assignment.dueAt)
    val isOverdue = dueInfo?.label?.contains("Terlambat") == true
    val isCompleted = assignment.status == "submitted" || assignment.status == "graded"
    val title = assignment.title.ifBlank { "Tugas Tanpa Judul" }
    val subjectLabel = assignment.subjectName ?: assignment.className ?: "Umum"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(
            width = if (isOverdue) 1.5.dp else 1.dp,
            color = if (isOverdue) NeonWarning.copy(alpha = 0.6f) else cardBorder
        ),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // TOP ROW: SUBJECT TAG & SCORE PILL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subject / Class pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFBAE6FD)),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = subjectLabel.uppercase(),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = studentAccentColor,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Max Score pill
                if (assignment.maxScore > 0) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = innerCardBg,
                        border = BorderStroke(1.dp, innerCardBorder)
                    ) {
                        Text(
                            text = "${assignment.maxScore} Poin",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // MAIN CONTENT: ICON + TITLE + CLASS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCompleted) (if (isDark) NeonSuccess.copy(alpha = 0.15f) else Color(0xFFDCFCE7)) else innerCardBg)
                        .border(
                            1.dp,
                            if (isCompleted) (if (isDark) NeonSuccess.copy(alpha = 0.4f) else Color(0xFF86EFAC)) else innerCardBorder,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = if (isCompleted) CoreR.drawable.ic_modern_check_circle else CoreR.drawable.ic_modern_tasks),
                        contentDescription = null,
                        tint = if (isCompleted) NeonSuccess else studentAccentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp,
                    )
                    if (!assignment.className.isNullOrBlank() && assignment.subjectName != null) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = assignment.className!!,
                            fontSize = 12.sp,
                            color = textSecondary,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (assignment.createdAt.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_modern_clock),
                                contentDescription = null,
                                tint = textTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Diterbitkan: ${formatPublishTimestamp(assignment.createdAt)}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // BOTTOM ROW: DUE DATE BADGE & STATUS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(innerCardBg)
                    .border(1.dp, innerCardBorder, RoundedCornerShape(10.dp))
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
                            else -> textTertiary
                        },
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = dueInfo?.label ?: "Tanpa batas waktu",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isOverdue -> NeonWarning
                            dueInfo != null -> dueInfo.color
                            else -> textSecondary
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
                    else -> textTertiary
                }

                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyAssignmentState(selectedTab: String) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

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
            textTertiary
        )
        else -> Triple(
            "Semua Tugas Selesai",
            "Belum ada penugasan baru dari tutor pamong. Waktunya istirahat atau pelajari modul berikutnya.",
            NeonSuccess
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
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
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = textTertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
