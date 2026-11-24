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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.Quiz
import com.schoolos.android.domain.model.QuizProctorScheduleDashboard
import com.schoolos.android.domain.model.QuizProctorSessionOverview
import java.time.LocalDate
import java.time.ZoneId

fun LazyListScope.teacherQuizListContent(
    liveQuizzes: List<Quiz>,
    upcomingQuizzes: List<Quiz>,
    draftQuizzes: List<Quiz>,
    endedQuizzes: List<Quiz>,
    sessionsById: Map<String, QuizProctorSessionOverview>,
    columns: Int,
    nowMs: Long,
    onQuizClick: (String) -> Unit,
) {
    if (liveQuizzes.isNotEmpty()) {
        item(key = "hdr-live") {
            TeacherQuizSectionHeader(
                title = "Sedang Berlangsung (Live)",
                count = liveQuizzes.size,
                tone = QuizSectionTone.EMERALD,
            )
        }
        adaptiveItems(
            items = liveQuizzes,
            columns = columns,
            keyPrefix = "live",
            itemKey = { it.id },
        ) { quiz ->
            TeacherQuizCard(
                quiz = quiz,
                sessionOverview = sessionsById[quiz.id],
                nowMs = nowMs,
                onClick = { onQuizClick(quiz.id) },
            )
        }
    }

    if (upcomingQuizzes.isNotEmpty()) {
        item(key = "hdr-upcoming") {
            TeacherQuizSectionHeader(
                title = "Terjadwal Akan Datang",
                count = upcomingQuizzes.size,
                tone = QuizSectionTone.AMBER,
            )
        }
        adaptiveItems(
            items = upcomingQuizzes,
            columns = columns,
            keyPrefix = "upcoming",
            itemKey = { it.id },
        ) { quiz ->
            TeacherQuizCard(
                quiz = quiz,
                sessionOverview = sessionsById[quiz.id],
                nowMs = nowMs,
                onClick = { onQuizClick(quiz.id) },
            )
        }
    }

    if (draftQuizzes.isNotEmpty()) {
        item(key = "hdr-draft") {
            TeacherQuizSectionHeader(
                title = "Konsep / Draf Soal",
                count = draftQuizzes.size,
                tone = QuizSectionTone.BLUE,
            )
        }
        adaptiveItems(
            items = draftQuizzes,
            columns = columns,
            keyPrefix = "draft",
            itemKey = { it.id },
        ) { quiz ->
            TeacherQuizCard(
                quiz = quiz,
                sessionOverview = sessionsById[quiz.id],
                nowMs = nowMs,
                onClick = { onQuizClick(quiz.id) },
            )
        }
    }

    if (endedQuizzes.isNotEmpty()) {
        item(key = "hdr-ended") {
            TeacherQuizSectionHeader(
                title = "Selesai & Riwayat",
                count = endedQuizzes.size,
                tone = QuizSectionTone.NEUTRAL,
            )
        }
        adaptiveItems(
            items = endedQuizzes,
            columns = columns,
            keyPrefix = "ended",
            itemKey = { it.id },
        ) { quiz ->
            TeacherQuizCard(
                quiz = quiz,
                sessionOverview = sessionsById[quiz.id],
                nowMs = nowMs,
                onClick = { onQuizClick(quiz.id) },
            )
        }
    }
}

fun LazyListScope.proctorScheduleTabContent(
    dashboard: QuizProctorScheduleDashboard?,
    filteredSessions: List<QuizProctorSessionOverview>,
    columns: Int,
    nowMs: Long,
    onQuizClick: (String) -> Unit,
) {
    if (dashboard != null && dashboard.conflicts.isNotEmpty()) {
        item(key = "conflict-banner") {
            ScheduleConflictWarningCard(
                conflicts = dashboard.conflicts,
                onInspectQuiz = onQuizClick,
            )
        }
    }

    val wibToday = LocalDate.now(ZoneId.of("Asia/Jakarta"))
    val wibTomorrow = wibToday.plusDays(1)
    val grouped = filteredSessions.groupBy { s ->
        formatExamWindowWib(s.startAt, s.endAt, s.releaseAt).wibDate
    }

    val orderedDates = grouped.keys.sortedWith(
        compareBy<LocalDate?>(
            { if (it == null) 1 else 0 },
            { it ?: LocalDate.MAX },
        )
    )

    orderedDates.forEach { dateKey ->
        val daySessions = grouped[dateKey].orEmpty()
        val groupTitle = when (dateKey) {
            null -> "Jadwal Fleksibel (Belum Ditentukan Jam)"
            wibToday -> "Hari Ini • ${formatExamWindowWib(daySessions.firstOrNull()?.startAt, daySessions.firstOrNull()?.endAt, daySessions.firstOrNull()?.releaseAt).dateLabel}"
            wibTomorrow -> "Besok • ${formatExamWindowWib(daySessions.firstOrNull()?.startAt, daySessions.firstOrNull()?.endAt, daySessions.firstOrNull()?.releaseAt).dateLabel}"
            else -> formatExamWindowWib(daySessions.firstOrNull()?.startAt, daySessions.firstOrNull()?.endAt, daySessions.firstOrNull()?.releaseAt).dateLabel
        }
        val tone = if (dateKey == wibToday) QuizSectionTone.EMERALD else QuizSectionTone.BLUE

        item(key = "sch-hdr-${dateKey?.toString() ?: "flex"}") {
            TeacherQuizSectionHeader(
                title = groupTitle,
                count = daySessions.size,
                tone = tone,
            )
        }
        adaptiveItems(
            items = daySessions,
            columns = columns,
            keyPrefix = "sch-${dateKey?.toString() ?: "flex"}",
            itemKey = { it.id },
        ) { sess ->
            ProctorScheduleSessionCard(
                session = sess,
                nowMs = nowMs,
                onClick = { onQuizClick(sess.id) },
            )
        }
    }
}

private enum class QuizSectionTone {
    EMERALD,
    AMBER,
    BLUE,
    NEUTRAL,
}

@Composable
private fun TeacherQuizSectionHeader(title: String, count: Int, tone: QuizSectionTone) {
    val accentColor = when (tone) {
        QuizSectionTone.EMERALD -> TeacherNeon
        QuizSectionTone.AMBER -> NeonWarning
        QuizSectionTone.BLUE -> NeonBlue
        QuizSectionTone.NEUTRAL -> TextTertiary
    }
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
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
private fun TeacherQuizCard(
    quiz: Quiz,
    sessionOverview: QuizProctorSessionOverview?,
    nowMs: Long,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val teacherAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val warningColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    val phase = quiz.effectivePhase
    val accentColor = when (phase) {
        "LIVE" -> teacherAccent
        "UPCOMING" -> warningColor
        "ENDED" -> textTertiary
        else -> if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    }

    val scheduleInfo = formatExamWindowWib(quiz.startAt, quiz.endAt, quiz.releaseAt)
    val countdownLabel = when (phase) {
        "UPCOMING" -> computeSecondsUntilStart(quiz.startAt, quiz.releaseAt, quiz.secondsUntilStart, nowMs)
            ?.let { "Mulai ${formatCountdownHuman(it)}" }
        "LIVE" -> computeSecondsRemaining(quiz.endAt, quiz.secondsRemaining, nowMs)
            ?.let { "Sisa ${formatCountdownHuman(it)}" }
        else -> null
    }
    val isProctored = quiz.requiresToken || quiz.examMode.equals("PROCTORED_CBT", ignoreCase = true)
    val tokenValue = quiz.examToken?.trim()?.takeIf { it.isNotEmpty() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(
            width = if (phase == "LIVE") 1.25.dp else 1.dp,
            color = if (phase == "LIVE") teacherAccent.copy(alpha = 0.5f) else cardBorder,
        ),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Top Row: Subject chip (weighted) + Fixed single-line Phase badge
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

                ExamPhasePill(phase = phase)
            }

            // Main row: Crisp Clipboard Icon + Title
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
                        imageVector = Icons.Rounded.FactCheck,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(21.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = quiz.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = textPrimary,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!quiz.teacherName.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = textTertiary,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = quiz.teacherName!!,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Chips Row: Prominent Rombel, Category, Proctored Mode, Conflict
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                quiz.effectiveDisplayClass?.let { cls ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color(0xFFBAE6FD)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = cls,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }

                formatAssessmentCategoryLabel(quiz.assessmentCategory)?.let { cat ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = innerCardBg,
                        border = BorderStroke(1.dp, innerCardBorder),
                    ) {
                        Text(
                            text = cat,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                ExamProctoredModeChip(isProctored = isProctored)

                if (sessionOverview?.hasConflict == true) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonError.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, NeonError.copy(alpha = 0.4f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(Icons.Rounded.WarningAmber, null, tint = NeonError, modifier = Modifier.size(11.dp))
                            Text("Bentrok", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonError)
                        }
                    }
                }
            }

            // Schedule + Countdown Strip
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
                            tint = if (phase == "LIVE") teacherAccent else warningColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Column {
                            Text(
                                text = scheduleInfo.combinedLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (!countdownLabel.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = countdownLabel,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (phase == "LIVE") teacherAccent else warningColor,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }

            // Token Vault + Live Proctor Telemetry Row
            if (tokenValue != null || sessionOverview != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (tokenValue != null) {
                        ExamTokenQuickBadge(
                            token = tokenValue,
                            onCopy = { copyExamTokenToClipboard(context, tokenValue, quiz.title) },
                        )
                    } else if (isProctored) {
                        Text(
                            text = "Token belum diset (ketuk Detail)",
                            fontSize = 10.5.sp,
                            color = warningColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    sessionOverview?.let { sess ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (sess.flaggedStudentsCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = NeonError.copy(alpha = 0.14f),
                                ) {
                                    Text(
                                        text = "${sess.flaggedStudentsCount} Flag",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonError,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MonitorHeart,
                                    contentDescription = null,
                                    tint = if (sess.inProgressCount > 0) teacherAccent else textTertiary,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "${sess.startedCount}/${sess.totalEnrolled.coerceAtLeast(sess.startedCount)} Peserta",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (sess.inProgressCount > 0) teacherAccent else textSecondary,
                                )
                            }
                        }
                    }
                }
            }

            // Footer metrics & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${quiz.questionsCount} Soal",
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("•", color = textTertiary, fontSize = 11.sp)
                    quiz.timeLimitMinutes?.let { mins ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(Icons.Rounded.Timer, null, tint = textTertiary, modifier = Modifier.size(12.dp))
                            Text(
                                text = "$mins mnt",
                                fontSize = 11.sp,
                                color = textSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    if (quiz.maxScore > 0) {
                        Text("•", color = textTertiary, fontSize = 11.sp)
                        Text(
                            text = "${quiz.maxScore} Poin",
                            color = textTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = if (phase == "LIVE") "Pantau Room" else "Kelola & Token",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleConflictWarningCard(
    conflicts: List<com.schoolos.android.domain.model.QuizProctorScheduleConflict>,
    onInspectQuiz: (String) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val borderCol = NeonError.copy(alpha = 0.45f)
    val bgCol = if (isDark) Color(0xFF450A0A).copy(alpha = 0.45f) else Color(0xFFFEF2F2)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = bgCol,
        border = BorderStroke(1.dp, borderCol),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.WarningAmber, null, tint = NeonError, modifier = Modifier.size(18.dp))
                Text(
                    text = "Terdeteksi ${conflicts.size} Bentrok Jadwal CBT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonError,
                )
            }
            conflicts.take(3).forEach { c ->
                val window = formatExamWindowWib(c.overlapStart, c.overlapEnd, null)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF18181B) else Color.White,
                    border = BorderStroke(1.dp, borderCol.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInspectQuiz(c.primaryQuizId) },
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${c.scopeLabel} • ${window.combinedLabel}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonError,
                        )
                        Text(
                            text = "${c.primaryTitle} ↔ ${c.conflictingTitle}",
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProctorScheduleSessionCard(
    session: QuizProctorSessionOverview,
    nowMs: Long,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569)
    val teacherAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val warningColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    val phase = session.examPhase.uppercase()
    val scheduleInfo = formatExamWindowWib(session.startAt, session.endAt, session.releaseAt)
    val countdown = when (phase) {
        "UPCOMING" -> computeSecondsUntilStart(session.startAt, session.releaseAt, session.secondsUntilStart, nowMs)
            ?.let { "Mulai ${formatCountdownHuman(it)}" }
        "LIVE" -> computeSecondsRemaining(session.endAt, session.secondsRemaining, nowMs)
            ?.let { "Sisa ${formatCountdownHuman(it)}" }
        else -> null
    }
    val enrolled = session.totalEnrolled.coerceAtLeast(session.startedCount).coerceAtLeast(1L)
    val progress = (session.startedCount.toFloat() / enrolled.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(
            1.dp,
            if (session.hasConflict) NeonError.copy(alpha = 0.5f) else cardBorder,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = if (phase == "LIVE") teacherAccent else warningColor,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = scheduleInfo.timeRangeLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (phase == "LIVE") teacherAccent else warningColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ExamPhasePill(phase = phase)
            }

            Text(
                text = session.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                session.className?.takeIf { it.isNotBlank() }?.let { cls ->
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE),
                    ) {
                        Text(
                            text = cls,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                session.teacherName?.takeIf { it.isNotBlank() }?.let { tName ->
                    Surface(shape = RoundedCornerShape(7.dp), color = innerCardBg) {
                        Text(
                            text = tName,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                if (!countdown.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = (if (phase == "LIVE") teacherAccent else warningColor).copy(alpha = 0.14f),
                    ) {
                        Text(
                            text = countdown,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (phase == "LIVE") teacherAccent else warningColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            // Progress bar & Token
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Kehadiran: ${session.startedCount}/${session.totalEnrolled} (Aktif: ${session.inProgressCount}, Selesai: ${session.completedCount + session.needGradingCount})",
                        fontSize = 10.5.sp,
                        color = textSecondary,
                    )
                    if (session.flaggedStudentsCount > 0) {
                        Text(
                            text = "${session.flaggedStudentsCount} Pelanggaran",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonError,
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (phase == "LIVE") teacherAccent else NeonBlue,
                    trackColor = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val tok = session.examToken?.trim()?.takeIf { it.isNotEmpty() }
                if (tok != null) {
                    ExamTokenQuickBadge(
                        token = tok,
                        onCopy = { copyExamTokenToClipboard(context, tok, session.title) },
                    )
                } else {
                    Text(
                        text = if (session.requiresToken) "Token belum dibuat" else "Tanpa Token",
                        fontSize = 10.5.sp,
                        color = TextTertiary,
                    )
                }

                Text(
                    text = "Buka Ruang Proktor →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = teacherAccent,
                )
            }
        }
    }
}
