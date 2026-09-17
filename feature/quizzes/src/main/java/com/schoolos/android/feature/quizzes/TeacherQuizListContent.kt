package com.schoolos.android.feature.quizzes

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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.Quiz

fun LazyListScope.teacherQuizListContent(
    activeQuizzes: List<Quiz>,
    doneQuizzes: List<Quiz>,
    onQuizClick: (String) -> Unit
) {
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
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                "$count",
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
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
    val gradientColors = subjectGradient(quiz.title)
    val icon = subjectIcon(quiz.title)
    val accentColor = if (isArchived) TextTertiary else TeacherNeon

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay, ambientColor = GlassOverlay)
            .clip(RoundedCornerShape(20.dp))
            .background(CosmicNavy)
            .border(
                width = 1.dp,
                color = if (isArchived) GlassBorder else TeacherNeon.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subject chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.10f))
                        .border(1.dp, accentColor.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = (quiz.subjectName ?: "Evaluasi").uppercase(),
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Status chip
                if (isArchived) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicDark)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("ARSIP", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TeacherNeon.copy(alpha = 0.12f))
                            .border(1.dp, TeacherNeon.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("AKTIF", color = TeacherNeon, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Main row
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accentColor.copy(alpha = 0.18f), accentColor.copy(alpha = 0.08f))
                            )
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quiz.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (isArchived) TextSecondary else TextPrimary,
                        lineHeight = 20.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val desc = quiz.description
                    if (!desc.isNullOrBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = desc,
                            color = TextTertiary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicDark)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("📝 ${quiz.questionsCount} Soal", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonSuccess.copy(alpha = 0.10f))
                            .border(1.dp, NeonSuccess.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("🎯 KKM ${quiz.passingScore}", color = NeonSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    quiz.timeLimitMinutes?.takeIf { it > 0 }?.let { mins ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CosmicDark)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(11.dp), tint = TextTertiary)
                            Spacer(Modifier.width(3.dp))
                            Text("$mins mnt", fontSize = 10.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Buka",
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
