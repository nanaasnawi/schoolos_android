package com.schoolos.android.feature.quizzes

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.R as CoreR
import com.schoolos.android.core.common.formatPublishTimestamp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.Quiz

fun LazyListScope.teacherQuizListContent(
    activeQuizzes: List<Quiz>,
    doneQuizzes: List<Quiz>,
    scheduledQuizzes: List<Quiz> = emptyList(),
    onQuizClick: (String) -> Unit
) {
    if (scheduledQuizzes.isNotEmpty()) {
        item {
            TeacherQuizSectionHeader(
                title = "Draf Terjadwal",
                count = scheduledQuizzes.size,
                accentColor = NeonWarning
            )
        }
        items(scheduledQuizzes, key = { it.id }) { quiz ->
            TeacherQuizCard(quiz = quiz, onClick = { onQuizClick(quiz.id) }, isArchived = false)
        }
    }

    if (activeQuizzes.isNotEmpty()) {
        item {
            TeacherQuizSectionHeader(
                title = "Kuis Aktif",
                count = activeQuizzes.size,
                accentColor = TeacherNeon
            )
        }
        items(activeQuizzes, key = { it.id }) { quiz ->
            TeacherQuizCard(quiz = quiz, onClick = { onQuizClick(quiz.id) }, isArchived = false)
        }
    }

    if (doneQuizzes.isNotEmpty()) {
        item {
            TeacherQuizSectionHeader(
                title = "Riwayat & Arsip",
                count = doneQuizzes.size,
                accentColor = TextTertiary
            )
        }
        items(doneQuizzes, key = { it.id }) { quiz ->
            TeacherQuizCard(quiz = quiz, onClick = { onQuizClick(quiz.id) }, isArchived = true)
        }
    }
}

@Composable
private fun TeacherQuizSectionHeader(title: String, count: Int, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                "$count",
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TeacherQuizCard(
    quiz: Quiz,
    onClick: () -> Unit,
    isArchived: Boolean = false
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val teacherAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val warningColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    val accentColor = if (isArchived) textTertiary else teacherAccent
    val isScheduled = quiz.isScheduled || quiz.status.lowercase() == "draft"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subject chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = (quiz.subjectName ?: "Evaluasi Kuis").uppercase(),
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status chip
                if (isArchived) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = innerCardBg,
                        border = BorderStroke(1.dp, innerCardBorder),
                    ) {
                        Text(
                            "ARSIP",
                            color = textTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (isScheduled) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFFF59E0B).copy(alpha = 0.4f) else Color(0xFFFDE68A)),
                    ) {
                        Text(
                            "TERJADWAL (DRAFT)",
                            color = warningColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFF86EFAC)),
                    ) {
                        Text(
                            "AKTIF",
                            color = teacherAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Main row
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(innerCardBg)
                        .border(1.dp, innerCardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_quiz),
                        contentDescription = null,
                        tint = if (isArchived) textTertiary else teacherAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quiz.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = if (isArchived) textSecondary else textPrimary,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val desc = quiz.description
                    if (!desc.isNullOrBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = desc,
                            color = textSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val targetDate = if (isScheduled) quiz.releaseAt ?: quiz.createdAt else quiz.createdAt
                    if (targetDate.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_modern_clock),
                                contentDescription = null,
                                tint = if (isScheduled) warningColor else textTertiary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = if (isScheduled) "Tayang: ${formatPublishTimestamp(targetDate)}" else "Dibuat: ${formatPublishTimestamp(targetDate)}",
                                fontSize = 10.sp,
                                color = if (isScheduled) warningColor else textTertiary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Bottom meta row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Questions count
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = innerCardBg,
                        border = BorderStroke(1.dp, innerCardBorder),
                    ) {
                        Text(
                            text = "${quiz.questionsCount} Soal",
                            color = textSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Time limit
                    quiz.timeLimitMinutes?.let { mins ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = innerCardBg,
                            border = BorderStroke(1.dp, innerCardBorder),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = CoreR.drawable.ic_modern_clock),
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = textTertiary
                                )
                                Text(
                                    text = "$mins mnt",
                                    fontSize = 10.5.sp,
                                    color = textSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (quiz.maxScore > 0) {
                        Text(
                            text = "${quiz.maxScore} poin",
                            color = textTertiary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Forward chevron
                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                    contentDescription = "Buka Detail Kuis",
                    tint = textTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
