package com.schoolos.android.feature.quizzes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class QuizWindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

fun resolveQuizWindowWidthClass(maxWidth: Dp): QuizWindowWidthClass = when {
    maxWidth < 600.dp -> QuizWindowWidthClass.COMPACT
    maxWidth < 840.dp -> QuizWindowWidthClass.MEDIUM
    else -> QuizWindowWidthClass.EXPANDED
}

fun QuizWindowWidthClass.contentHorizontalPadding(): Dp = when (this) {
    QuizWindowWidthClass.COMPACT -> 14.dp
    QuizWindowWidthClass.MEDIUM -> 22.dp
    QuizWindowWidthClass.EXPANDED -> 28.dp
}

fun <T> LazyListScope.adaptiveItems(
    items: List<T>,
    columns: Int,
    keyPrefix: String,
    itemKey: (T) -> String,
    horizontalSpacing: Dp = 12.dp,
    itemContent: @Composable (T) -> Unit,
) {
    if (columns <= 1) {
        items(items, key = { "$keyPrefix-${itemKey(it)}" }) { item ->
            itemContent(item)
        }
    } else {
        val rows = items.chunked(columns)
        items(
            rows,
            key = { row -> "$keyPrefix-${row.joinToString("_") { itemKey(it) }}" },
        ) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            ) {
                row.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        itemContent(item)
                    }
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun rememberTickerMillis(intervalMs: Long = 1000L): Long {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(intervalMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(intervalMs)
        }
    }
    return nowMs
}

private val WIB_ZONE: ZoneId = ZoneId.of("Asia/Jakarta")
private val ID_LOCALE: Locale = Locale("id", "ID")
private val DATE_FMT_ID = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", ID_LOCALE)
private val HM_FMT_ID = DateTimeFormatter.ofPattern("HH:mm", ID_LOCALE)
private val FULL_WIB_FMT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'WIB'", ID_LOCALE)

data class ExamScheduleFormatted(
    val hasWindow: Boolean,
    val dateLabel: String,
    val timeRangeLabel: String,
    val combinedLabel: String,
    val wibDate: LocalDate?,
    val startInstant: Instant?,
    val endInstant: Instant?,
)

fun parseQuizInstant(raw: String?): Instant? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    trimmed.toLongOrNull()?.let { return Instant.ofEpochMilli(it) }
    return try {
        OffsetDateTime.parse(trimmed).toInstant()
    } catch (_: Exception) {
        try {
            Instant.parse(trimmed)
        } catch (_: Exception) {
            try {
                ZonedDateTime.parse(trimmed).toInstant()
            } catch (_: Exception) {
                try {
                    val clean = trimmed.replace(' ', 'T').substringBefore(".")
                    LocalDateTime.parse(clean).atZone(WIB_ZONE).toInstant()
                } catch (_: Exception) {
                    try {
                        val datePart = trimmed.substringBefore(" ").substringBefore("T")
                        LocalDate.parse(datePart).atStartOfDay(WIB_ZONE).toInstant()
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }
}

fun formatExamWindowWib(
    startAt: String?,
    endAt: String?,
    releaseAt: String? = null,
): ExamScheduleFormatted {
    val startInst = parseQuizInstant(startAt ?: releaseAt)
    val endInst = parseQuizInstant(endAt)
    val refInst = startInst ?: endInst
    if (refInst == null) {
        return ExamScheduleFormatted(
            hasWindow = false,
            dateLabel = "Jadwal Fleksibel",
            timeRangeLabel = "Terbuka sesuai aktivasi",
            combinedLabel = "Jadwal Fleksibel",
            wibDate = null,
            startInstant = null,
            endInstant = null,
        )
    }

    val refWib = refInst.atZone(WIB_ZONE)
    val dateLabel = refWib.format(DATE_FMT_ID)
    val timeRangeLabel = when {
        startInst != null && endInst != null -> {
            val sWib = startInst.atZone(WIB_ZONE)
            val eWib = endInst.atZone(WIB_ZONE)
            if (sWib.toLocalDate() == eWib.toLocalDate()) {
                "${sWib.format(HM_FMT_ID)} – ${eWib.format(HM_FMT_ID)} WIB"
            } else {
                "${sWib.format(FULL_WIB_FMT)} – ${eWib.format(FULL_WIB_FMT)}"
            }
        }
        startInst != null -> "Mulai ${startInst.atZone(WIB_ZONE).format(HM_FMT_ID)} WIB"
        else -> "Tenggat ${endInst!!.atZone(WIB_ZONE).format(HM_FMT_ID)} WIB"
    }

    return ExamScheduleFormatted(
        hasWindow = true,
        dateLabel = dateLabel,
        timeRangeLabel = timeRangeLabel,
        combinedLabel = "$dateLabel • $timeRangeLabel",
        wibDate = refWib.toLocalDate(),
        startInstant = startInst,
        endInstant = endInst,
    )
}

fun computeSecondsUntilStart(
    startAt: String?,
    releaseAt: String?,
    serverFallback: Long? = null,
    nowMs: Long = System.currentTimeMillis(),
): Long? {
    val startInst = parseQuizInstant(startAt ?: releaseAt)
    return if (startInst != null) {
        ((startInst.toEpochMilli() - nowMs) / 1000L).coerceAtLeast(0L)
    } else {
        serverFallback?.coerceAtLeast(0L)
    }
}

fun computeSecondsRemaining(
    endAt: String?,
    serverFallback: Long? = null,
    nowMs: Long = System.currentTimeMillis(),
): Long? {
    val endInst = parseQuizInstant(endAt)
    return if (endInst != null) {
        ((endInst.toEpochMilli() - nowMs) / 1000L).coerceAtLeast(0L)
    } else {
        serverFallback?.coerceAtLeast(0L)
    }
}

fun formatCountdownHuman(seconds: Long?): String {
    if (seconds == null || seconds <= 0L) return "Segera"
    val days = seconds / 86400L
    val hours = (seconds % 86400L) / 3600L
    val mins = (seconds % 3600L) / 60L
    val secs = seconds % 60L
    return when {
        days >= 1L -> if (hours > 0L) "$days hari $hours jam" else "$days hari"
        hours >= 1L -> "${hours}j ${mins}m"
        mins >= 1L -> "${mins}m ${secs}d"
        else -> "${secs} detik"
    }
}

fun formatDigitalTimer(seconds: Long): String {
    val clamped = seconds.coerceAtLeast(0L)
    val hrs = clamped / 3600L
    val mins = (clamped % 3600L) / 60L
    val secs = clamped % 60L
    return if (hrs > 0L) {
        String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

fun buildWibUtcIso(date: LocalDate, time: LocalTime): String =
    date.atTime(time).atZone(WIB_ZONE).toInstant().toString()

fun copyExamTokenToClipboard(context: Context, token: String, quizTitle: String? = null) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Token Ujian CBT", token))
    val msg = if (!quizTitle.isNullOrBlank()) {
        "Token $token disalin"
    } else {
        "Token $token berhasil disalin"
    }
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

fun shareExamTokenBrief(
    context: Context,
    title: String,
    subjectName: String?,
    className: String?,
    teacherName: String?,
    scheduleLabel: String,
    durationMinutes: Int,
    token: String?,
) {
    val lines = buildList {
        add("*INFO PELAKSANAAN UJIAN CBT*")
        add("Judul: $title")
        if (!subjectName.isNullOrBlank()) add("Mapel: $subjectName")
        if (!className.isNullOrBlank()) add("Rombel/Kelas: $className")
        if (!teacherName.isNullOrBlank()) add("Pengampu: $teacherName")
        add("Jadwal: $scheduleLabel ($durationMinutes Menit)")
        if (!token.isNullOrBlank()) {
            add("")
            add("TOKEN MASUK UJIAN: *$token*")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, lines.joinToString("\n"))
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan Jadwal & Token Ujian"))
}

fun formatAssessmentCategoryLabel(category: String?): String? = when (category?.trim()?.uppercase()) {
    "SUMMATIVE", "PAS", "SAS", "PAT", "SAT", "UAS", "US" -> "PAS / SAS"
    "MIDTERM", "PTS", "STS", "UTS" -> "PTS / STS"
    "DAILY_QUIZ", "UH", "FORMATIF", "FORMATIVE" -> "Ulangan Harian"
    null, "" -> null
    else -> category.trim().replace("_", " ")
}

@Composable
fun ExamPhasePill(
    phase: String,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val upper = phase.trim().uppercase()
    val (label, textColor, bgColor, borderColor, showLiveDot) = when (upper) {
        "LIVE" -> PhaseChipPalette(
            label = "SEDANG LIVE",
            fg = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
            bg = if (isDark) Color(0xFF064E3B).copy(alpha = 0.55f) else Color(0xFFDCFCE7),
            border = if (isDark) Color(0xFF10B981).copy(alpha = 0.55f) else Color(0xFF86EFAC),
            liveDot = true,
        )
        "UPCOMING" -> PhaseChipPalette(
            label = "TERJADWAL",
            fg = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
            bg = if (isDark) Color(0xFF451A03).copy(alpha = 0.65f) else Color(0xFFFEF3C7),
            border = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.5f) else Color(0xFFFDE68A),
            liveDot = false,
        )
        "ENDED" -> PhaseChipPalette(
            label = "BERAKHIR",
            fg = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
            bg = if (isDark) Color(0xFF450A0A).copy(alpha = 0.6f) else Color(0xFFFEE2E2),
            border = if (isDark) Color(0xFFEF4444).copy(alpha = 0.45f) else Color(0xFFFECACA),
            liveDot = false,
        )
        else -> PhaseChipPalette(
            label = "DRAF",
            fg = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9),
            bg = if (isDark) Color(0xFF2E1065).copy(alpha = 0.45f) else Color(0xFFF3E8FF),
            border = if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.4f) else Color(0xFFDDD6FE),
            liveDot = false,
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (showLiveDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(textColor)
                )
            }
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class PhaseChipPalette(
    val label: String,
    val fg: Color,
    val bg: Color,
    val border: Color,
    val liveDot: Boolean,
)

@Composable
fun ExamTokenQuickBadge(
    token: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val amberText = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
    val amberBg = if (isDark) Color(0xFF451A03).copy(alpha = 0.5f) else Color(0xFFFFFBEB)
    val amberBorder = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.55f) else Color(0xFFFCD34D)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onCopy),
        shape = RoundedCornerShape(9.dp),
        color = amberBg,
        border = BorderStroke(1.dp, amberBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Key,
                contentDescription = null,
                tint = amberText,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = token,
                color = amberText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp,
                maxLines = 1,
                softWrap = false,
            )
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Salin Token",
                tint = amberText.copy(alpha = 0.85f),
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
fun ExamProctoredModeChip(
    isProctored: Boolean,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val fg = if (isProctored) {
        if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
    } else {
        TextSecondary
    }
    val bg = if (isProctored) {
        if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF)
    } else {
        if (isDark) Color(0xFF18181B) else Color(0xFFF1F5F9)
    }
    val border = if (isProctored) {
        if (isDark) Color(0xFF3B82F6).copy(alpha = 0.45f) else Color(0xFFBFDBFE)
    } else {
        if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (isProctored) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(11.dp),
                )
            }
            Text(
                text = if (isProctored) "Proctored CBT" else "Kuis Reguler",
                color = fg,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
fun AdaptiveQuizWidthContainer(
    maxWidth: Dp = 1160.dp,
    modifier: Modifier = Modifier,
    content: @Composable (QuizWindowWidthClass) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val widthClass = resolveQuizWindowWidthClass(maxWidth = this.maxWidth)
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
        ) {
            content(widthClass)
        }
    }
}

fun proctorStatusDisplay(proctorStatus: String): String = when (proctorStatus.uppercase()) {
    "IN_PROGRESS" -> "Sedang Ujian"
    "NEED_GRADING" -> "Perlu Koreksi"
    "COMPLETED" -> "Selesai"
    else -> "Belum Masuk"
}
