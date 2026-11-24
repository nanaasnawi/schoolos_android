package com.schoolos.android.feature.quizzes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
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
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.Quiz

fun LazyListScope.studentQuizListContent(
    activeQuizzes: List<Quiz>,
    upcomingQuizzes: List<Quiz> = emptyList(),
    doneQuizzes: List<Quiz>,
    columns: Int = 1,
    nowMs: Long = System.currentTimeMillis(),
    onQuizClick: (String) -> Unit,
) {
    if (activeQuizzes.isNotEmpty()) {
        item(key = "stu-hdr-live") {
            QuizSectionHeader(
                title = "Sedang Dibuka & Siap Dikerjakan",
                count = activeQuizzes.size,
                accentColor = StudentNeon,
            )
        }
        adaptiveItems(
            items = activeQuizzes,
            columns = columns,
            keyPrefix = "stu-live",
            itemKey = { it.id },
        ) { quiz ->
            StudentQuizCard(quiz = quiz, nowMs = nowMs, onClick = { onQuizClick(quiz.id) }, isDone = false)
        }
    }

    if (upcomingQuizzes.isNotEmpty()) {
        item(key = "stu-hdr-upcoming") {
            QuizSectionHeader(
                title = "Jadwal Ujian Mendatang",
                count = upcomingQuizzes.size,
                accentColor = NeonWarning,
            )
        }
        adaptiveItems(
            items = upcomingQuizzes,
            columns = columns,
            keyPrefix = "stu-upcoming",
            itemKey = { it.id },
        ) { quiz ->
            StudentQuizCard(quiz = quiz, nowMs = nowMs, onClick = { onQuizClick(quiz.id) }, isDone = false)
        }
    }

    if (doneQuizzes.isNotEmpty()) {
        item(key = "stu-hdr-done") {
            QuizSectionHeader(
                title = "Selesai & Riwayat Ujian",
                count = doneQuizzes.size,
                accentColor = NeonSuccess,
            )
        }
        adaptiveItems(
            items = doneQuizzes,
            columns = columns,
            keyPrefix = "stu-done",
            itemKey = { it.id },
        ) { quiz ->
            StudentQuizCard(quiz = quiz, nowMs = nowMs, onClick = { onQuizClick(quiz.id) }, isDone = true)
        }
    }
}

@Composable
private fun QuizSectionHeader(title: String, count: Int, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.6.sp,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(accentColor.copy(alpha = 0.14f))
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text(
                "$count",
                color = accentColor,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudentQuizCard(
    quiz: Quiz,
    nowMs: Long,
    onClick: () -> Unit,
    isDone: Boolean = false,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val studentAccent = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val successColor = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A)
    val warningColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    val phase = quiz.effectivePhase
    val isCompletedByStudent = quiz.studentHasCompleted ||
        quiz.studentAttemptStatus?.lowercase() in listOf("completed", "submitted", "graded")
    val isInProgress = quiz.studentAttemptStatus?.lowercase() == "in_progress"
    val accentColor = when {
        isCompletedByStudent -> successColor
        phase == "LIVE" -> studentAccent
        phase == "UPCOMING" -> warningColor
        else -> textTertiary
    }

    val scheduleInfo = formatExamWindowWib(quiz.startAt, quiz.endAt, quiz.releaseAt)
    val countdownText = when {
        isCompletedByStudent || phase == "ENDED" -> null
        phase == "UPCOMING" -> computeSecondsUntilStart(quiz.startAt, quiz.releaseAt, quiz.secondsUntilStart, nowMs)
            ?.let { "Dibuka ${formatCountdownHuman(it)}" }
        phase == "LIVE" -> computeSecondsRemaining(quiz.endAt, quiz.secondsRemaining, nowMs)
            ?.let { "Sisa ${formatCountdownHuman(it)}" }
        else -> null
    }
    val needsToken = quiz.requiresToken || quiz.examMode.equals("PROCTORED_CBT", ignoreCase = true)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(
            width = if (phase == "LIVE" && !isCompletedByStudent) 1.25.dp else 1.dp,
            color = if (phase == "LIVE" && !isCompletedByStudent) studentAccent.copy(alpha = 0.5f) else cardBorder,
        ),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Top Row: Subject + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.weight(1f, fill = false),
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
                            text = (quiz.subjectName ?: "Evaluasi CBT").uppercase(),
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.35.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                when {
                    isCompletedByStudent -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF10B981).copy(alpha = 0.45f) else Color(0xFF86EFAC)),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = successColor, modifier = Modifier.size(12.dp))
                                Text("Selesai", color = successColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, softWrap = false)
                            }
                        }
                    }
                    isInProgress -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = studentAccent.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, studentAccent.copy(alpha = 0.45f)),
                        ) {
                            Text(
                                text = "SEDANG DIKERJAKAN",
                                color = studentAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                softWrap = false,
                            )
                        }
                    }
                    else -> ExamPhasePill(phase = phase)
                }
            }

            // Title row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.11f))
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isCompletedByStudent) Icons.Rounded.CheckCircle else Icons.Rounded.FactCheck,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(21.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = quiz.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = textPrimary,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        formatAssessmentCategoryLabel(quiz.assessmentCategory)?.let { cat ->
                            Text(
                                text = cat,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary,
                            )
                        }
                        if (needsToken && !isCompletedByStudent && phase != "ENDED") {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = warningColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, warningColor.copy(alpha = 0.35f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(Icons.Rounded.Lock, null, tint = warningColor, modifier = Modifier.size(10.dp))
                                    Text("Wajib Token", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = warningColor)
                                }
                            }
                        }
                    }
                }
            }

            // Exam schedule strip
            if (scheduleInfo.hasWindow) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = innerCardBg,
                    border = BorderStroke(1.dp, innerCardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = if (phase == "LIVE") studentAccent else warningColor,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = scheduleInfo.combinedLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!countdownText.isNullOrBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = countdownText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (phase == "LIVE") studentAccent else warningColor,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }

            // Bottom meta row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    quiz.timeLimitMinutes?.let { mins ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = innerCardBg,
                            border = BorderStroke(1.dp, innerCardBorder),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Rounded.Timer, null, modifier = Modifier.size(11.dp), tint = textTertiary)
                                Text(
                                    text = "$mins mnt",
                                    fontSize = 10.5.sp,
                                    color = textSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    if (isCompletedByStudent && quiz.studentLastScore != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = successColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, successColor.copy(alpha = 0.35f)),
                        ) {
                            Text(
                                text = "Nilai: ${quiz.studentLastScore}/${quiz.maxScore}",
                                color = successColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    } else if (isCompletedByStudent) {
                        Text(
                            text = "Menunggu Nilai",
                            color = warningColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (phase == "LIVE" && !isCompletedByStudent) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = studentAccent, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isInProgress) "Lanjutkan" else "Masuk Ujian",
                            color = studentAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Text(
                            text = if (isCompletedByStudent) "Lihat Hasil" else "Detail",
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Icon(Icons.Rounded.ChevronRight, null, tint = textTertiary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
