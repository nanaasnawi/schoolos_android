package com.schoolos.android.feature.quizzes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.MoreTime
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.QuestionPromptWithDiagram
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.Quiz
import com.schoolos.android.domain.model.QuizProctorEventLog
import com.schoolos.android.domain.model.QuizProctorMonitor
import com.schoolos.android.domain.model.QuizProctorParticipantRow
import com.schoolos.android.domain.model.QuizQuestion
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ExecutiveTokenVaultCard(
    quiz: Quiz,
    isProcessing: Boolean,
    onRotateClick: () -> Unit,
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val vaultBg = if (isDark) Color(0xFF1B1406) else Color(0xFFFFFBEB)
    val vaultBorder = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.45f) else Color(0xFFFCD34D)
    val amberFg = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569)

    val token = quiz.examToken?.trim()?.takeIf { it.isNotEmpty() }
    val scheduleInfo = formatExamWindowWib(quiz.startAt, quiz.endAt, quiz.releaseAt)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, if (token != null) vaultBorder else cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(amberFg.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Key, contentDescription = null, tint = amberFg, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text(
                            text = "TOKEN UJIAN CBT PROCTORED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = amberFg,
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            text = if (token != null) "Bagikan kode ini ke pengawas atau peserta" else "Aktifkan token agar siswa wajib verifikasi",
                            fontSize = 11.sp,
                            color = textSecondary,
                        )
                    }
                }
            }

            // Large Monospace Token Display Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = token != null) {
                        if (token != null) copyExamTokenToClipboard(context, token, quiz.title)
                    },
                shape = RoundedCornerShape(14.dp),
                color = if (token != null) vaultBg else (if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, if (token != null) vaultBorder else cardBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (token != null) {
                        Text(
                            text = token.toCharArray().joinToString(" "),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            color = textPrimary,
                        )
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = amberFg.copy(alpha = 0.15f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Rounded.ContentCopy, null, tint = amberFg, modifier = Modifier.size(14.dp))
                                Text("Salin", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = amberFg)
                            }
                        }
                    } else {
                        Text(
                            text = "MODE TANPA TOKEN",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                        )
                    }
                }
            }

            // Quick Action Row: Rotate/Set Token & Share to Proctors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onRotateClick,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, amberFg.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = amberFg),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).height(40.dp),
                ) {
                    Icon(Icons.Rounded.AutoMode, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (token != null) "Acak / Ubah" else "Buat Token",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }

                Button(
                    onClick = {
                        shareExamTokenBrief(
                            context = context,
                            title = quiz.title,
                            subjectName = quiz.subjectName,
                            className = quiz.effectiveDisplayClass,
                            teacherName = quiz.teacherName,
                            scheduleLabel = scheduleInfo.combinedLabel,
                            durationMinutes = quiz.timeLimitMinutes ?: 60,
                            token = token,
                        )
                    },
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TeacherNeon,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).height(40.dp),
                ) {
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Bagikan Info", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun RotateTokenDialog(
    currentToken: String?,
    isProcessing: Boolean,
    onConfirmAuto: () -> Unit,
    onConfirmCustom: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val dialogBg = if (isDark) Color(0xFF111113) else Color.White
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569)
    val inputBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val amberFg = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    var customToken by remember { mutableStateOf("") }
    val isValidCustom = customToken.isBlank() || (customToken.length in 4..12 && customToken.all { it.isLetterOrDigit() })

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Key, null, tint = amberFg, modifier = Modifier.size(20.dp))
                Text("Kelola Token Ujian CBT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!currentToken.isNullOrBlank()) {
                    Text(
                        text = "Token saat ini: $currentToken. Memperbarui token tidak mengeluarkan siswa yang sudah masuk.",
                        fontSize = 12.sp,
                        color = textSecondary,
                    )
                }
                OutlinedTextField(
                    value = customToken,
                    onValueChange = { raw ->
                        customToken = raw.uppercase().filter { it.isLetterOrDigit() }.take(12)
                    },
                    label = { Text("Token Kustom (Opsional, 4-12 huruf/angka)", fontSize = 11.5.sp) },
                    placeholder = { Text("Kosongkan untuk acak otomatis", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                    ),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp,
                        color = textPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedBorderColor = amberFg,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!isValidCustom) {
                    Text("Token kustom minimal 4 karakter huruf/angka.", fontSize = 11.sp, color = NeonError)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customToken.isNotBlank()) onConfirmCustom(customToken) else onConfirmAuto()
                },
                enabled = !isProcessing && isValidCustom,
                colors = ButtonDefaults.buttonColors(containerColor = amberFg, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (customToken.isNotBlank()) "Simpan Token" else "Acak Token Baru", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text("Batal", color = textSecondary)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuizScheduleBottomSheet(
    quiz: Quiz,
    isProcessing: Boolean,
    onSave: (startAtIso: String, endAtIso: String, durationMinutes: Int, examMode: String, examToken: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val sheetBg = if (isDark) Color(0xFF111113) else Color.White
    val innerBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569)

    val zone = remember { ZoneId.of("Asia/Jakarta") }
    val initStartZdt = remember(quiz.startAt, quiz.releaseAt) {
        (parseQuizInstant(quiz.startAt ?: quiz.releaseAt) ?: java.time.Instant.now().plusSeconds(3600))
            .atZone(zone)
    }
    val initEndZdt = remember(quiz.endAt) {
        (parseQuizInstant(quiz.endAt) ?: initStartZdt.toInstant().plusSeconds(((quiz.timeLimitMinutes ?: 60) * 60L)))
            .atZone(zone)
    }

    var selectedDate by remember { mutableStateOf(initStartZdt.toLocalDate()) }
    var startTime by remember { mutableStateOf(LocalTime.of(initStartZdt.hour, initStartZdt.minute)) }
    var endTime by remember { mutableStateOf(LocalTime.of(initEndZdt.hour, initEndZdt.minute)) }
    var durationMinutes by remember { mutableIntStateOf(quiz.timeLimitMinutes ?: 60) }
    var isProctored by remember { mutableStateOf(quiz.requiresToken || quiz.examMode.equals("PROCTORED_CBT", ignoreCase = true)) }
    var tokenText by remember { mutableStateOf(quiz.examToken ?: "") }

    val idLocale = remember { Locale("id", "ID") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, d MMM yyyy", idLocale) }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm", idLocale) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Atur Jadwal & Mode Ujian CBT",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = textPrimary,
            )

            // Date Selector
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> selectedDate = LocalDate.of(y, m + 1, d) },
                            selectedDate.year,
                            selectedDate.monthValue - 1,
                            selectedDate.dayOfMonth,
                        ).show()
                    },
                shape = RoundedCornerShape(12.dp),
                color = innerBg,
                border = BorderStroke(1.dp, innerBorder),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.CalendarToday, null, tint = TeacherNeon, modifier = Modifier.size(16.dp))
                        Text("Tanggal Pelaksanaan", fontSize = 12.5.sp, color = textSecondary)
                    }
                    Text(selectedDate.format(dateFmt), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }
            }

            // Start & End Time Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, h, min ->
                                    startTime = LocalTime.of(h, min)
                                    endTime = startTime.plusMinutes(durationMinutes.toLong())
                                },
                                startTime.hour,
                                startTime.minute,
                                true,
                            ).show()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = innerBg,
                    border = BorderStroke(1.dp, innerBorder),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Jam Mulai (WIB)", fontSize = 10.5.sp, color = textSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Rounded.AccessTime, null, tint = TeacherNeon, modifier = Modifier.size(15.dp))
                            Text(startTime.format(timeFmt), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, h, min -> endTime = LocalTime.of(h, min) },
                                endTime.hour,
                                endTime.minute,
                                true,
                            ).show()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = innerBg,
                    border = BorderStroke(1.dp, innerBorder),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Jam Selesai (WIB)", fontSize = 10.5.sp, color = textSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Rounded.AccessTime, null, tint = NeonWarning, modifier = Modifier.size(15.dp))
                            Text(endTime.format(timeFmt), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    }
                }
            }

            // Duration presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Durasi Pengerjaan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(30, 45, 60, 90, 120).forEach { mins ->
                        val selected = durationMinutes == mins
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    durationMinutes = mins
                                    endTime = startTime.plusMinutes(mins.toLong())
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) TeacherNeon.copy(alpha = 0.16f) else innerBg,
                            border = BorderStroke(1.dp, if (selected) TeacherNeon else innerBorder),
                        ) {
                            Text(
                                text = "${mins}m",
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) TeacherNeon else textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }

            // Proctored toggle + optional token
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = innerBg,
                border = BorderStroke(1.dp, innerBorder),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mode Pengawasan Ketat (Proctored CBT)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("Wajib token pengawas & deteksi pelanggaran layar.", fontSize = 11.sp, color = textSecondary)
                        }
                        Switch(
                            checked = isProctored,
                            onCheckedChange = { isProctored = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TeacherNeon),
                        )
                    }
                    if (isProctored) {
                        OutlinedTextField(
                            value = tokenText,
                            onValueChange = { raw -> tokenText = raw.uppercase().filter { it.isLetterOrDigit() }.take(12) },
                            label = { Text("Token Ujian (4-12 huruf/angka)", fontSize = 11.5.sp) },
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val startIso = buildWibUtcIso(selectedDate, startTime)
                    val endDate = if (!endTime.isAfter(startTime)) selectedDate.plusDays(1) else selectedDate
                    val endIso = buildWibUtcIso(endDate, endTime)
                    onSave(
                        startIso,
                        endIso,
                        durationMinutes,
                        if (isProctored) "PROCTORED_CBT" else "HOMEWORK_QUIZ",
                        if (isProctored) tokenText.trim().takeIf { it.isNotEmpty() } else "",
                    )
                },
                enabled = !isProcessing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TeacherNeon, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Simpan Jadwal Ujian CBT", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProctorLiveMonitorPanel(
    monitor: QuizProctorMonitor?,
    fallbackAttempts: List<com.schoolos.android.domain.model.QuizAttempt>,
    maxScore: Int,
    isRefreshing: Boolean,
    isAutoSync: Boolean,
    onToggleAutoSync: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onExtendTime15Min: () -> Unit,
    onGradeAttempt: (attemptId: String, studentId: String, studentName: String, nisn: String?, status: String, score: Int?, totalPoints: Int) -> Unit,
    onViewAttemptDetail: (attemptId: String, score: Int, totalPoints: Int) -> Unit,
    onForceSubmitAttempt: (attemptId: String, studentName: String) -> Unit,
    onReopenAttempt: (attemptId: String, studentName: String) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var searchName by remember { mutableStateOf("") }

    val participants: List<QuizProctorParticipantRow> = remember(monitor, fallbackAttempts, maxScore) {
        monitor?.participants ?: fallbackAttempts.map { att ->
            val st = when (att.status.lowercase()) {
                "in_progress", "started", "created" -> "IN_PROGRESS"
                "completed", "submitted", "auto_graded" -> "NEED_GRADING"
                "graded", "reviewed" -> "COMPLETED"
                else -> "NOT_STARTED"
            }
            QuizProctorParticipantRow(
                studentId = att.studentId,
                studentName = att.studentName ?: "Siswa",
                studentNisn = att.studentNisn ?: "-",
                attemptId = att.id.takeIf { !it.startsWith("unsub-") },
                rawAttemptStatus = att.status,
                proctorStatus = st,
                startedAt = att.startedAt.takeIf { it.isNotBlank() },
                completedAt = att.completedAt,
                score = att.score,
                totalPoints = if (att.totalPoints > 0) att.totalPoints else maxScore,
                isOnline = st == "IN_PROGRESS",
            )
        }
    }

    val totalEnrolled = monitor?.summary?.totalEnrolled ?: participants.size
    val inProgress = monitor?.summary?.inProgressCount ?: participants.count { it.proctorStatus == "IN_PROGRESS" }
    val needGrading = monitor?.summary?.needGradingCount ?: participants.count { it.proctorStatus == "NEED_GRADING" }
    val completed = monitor?.summary?.completedCount ?: participants.count { it.proctorStatus == "COMPLETED" }
    val notStarted = monitor?.summary?.notStartedCount ?: participants.count { it.proctorStatus == "NOT_STARTED" }
    val flaggedCount = monitor?.summary?.flaggedCount ?: participants.count { it.leaveViolationsCount > 0 }

    val qLower = searchName.trim().lowercase()
    val filtered = remember(participants, selectedStatusFilter, qLower) {
        participants.filter { p ->
            val matchFilter = when (selectedStatusFilter) {
                "FLAGGED" -> p.leaveViolationsCount > 0
                "IN_PROGRESS" -> p.proctorStatus == "IN_PROGRESS"
                "DONE" -> p.proctorStatus == "COMPLETED" || p.proctorStatus == "NEED_GRADING"
                "NOT_STARTED" -> p.proctorStatus == "NOT_STARTED"
                else -> true
            }
            val matchSearch = qLower.isEmpty() ||
                p.studentName.lowercase().contains(qLower) ||
                p.studentNisn.lowercase().contains(qLower)
            matchFilter && matchSearch
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header Controls Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.MonitorHeart, null, tint = TeacherNeon, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Mode Pantau Live Proktor", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("Status pengerjaan & pelanggaran realtime", fontSize = 11.sp, color = textSecondary)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleAutoSync(!isAutoSync) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAutoSync) TeacherNeon.copy(alpha = 0.14f) else innerCardBg,
                            border = BorderStroke(1.dp, if (isAutoSync) TeacherNeon.copy(alpha = 0.4f) else innerCardBorder),
                        ) {
                            Text(
                                text = if (isAutoSync) "Live 10d" else "Manual",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAutoSync) TeacherNeon else textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        IconButton(onClick = onRefresh, enabled = !isRefreshing, modifier = Modifier.size(30.dp)) {
                            if (isRefreshing) {
                                CircularProgressIndicator(color = TeacherNeon, strokeWidth = 2.dp, modifier = Modifier.size(15.dp))
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Segarkan Monitor", tint = TeacherNeon, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // KPI Telemetry Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MonitorKpiPill("Belum", notStarted, textSecondary, innerCardBg, Modifier.weight(1f))
                    MonitorKpiPill("Ujian", inProgress, NeonBlue, NeonBlue.copy(alpha = 0.1f), Modifier.weight(1f))
                    MonitorKpiPill("Koreksi", needGrading, NeonWarning, NeonWarning.copy(alpha = 0.1f), Modifier.weight(1f))
                    MonitorKpiPill("Selesai", completed, NeonSuccess, NeonSuccess.copy(alpha = 0.1f), Modifier.weight(1f))
                }

                val completionPct = if (totalEnrolled > 0) (completed + needGrading).toFloat() / totalEnrolled.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { completionPct.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = TeacherNeon,
                    trackColor = innerCardBg,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (flaggedCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.WarningAmber, null, tint = NeonError, modifier = Modifier.size(14.dp))
                            Text(
                                text = "$flaggedCount siswa terdeteksi keluar layar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonError,
                            )
                        }
                    } else {
                        Text(
                            text = "${completed + needGrading}/$totalEnrolled selesai",
                            fontSize = 11.sp,
                            color = textSecondary,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    TextButton(
                        onClick = onExtendTime15Min,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Icon(Icons.Rounded.MoreTime, null, tint = NeonBlue, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("+15 Menit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonBlue)
                    }
                }
            }
        }

        // Participant Search & Filter
        OutlinedTextField(
            value = searchName,
            onValueChange = { searchName = it },
            singleLine = true,
            placeholder = { Text("Cari nama atau NISN peserta...", fontSize = 12.sp, color = textTertiary) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = textTertiary, modifier = Modifier.size(17.dp)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
                focusedBorderColor = TeacherNeon,
                unfocusedBorderColor = cardBorder,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                "ALL" to "Semua ($totalEnrolled)",
                "IN_PROGRESS" to "Mengerjakan ($inProgress)",
                "FLAGGED" to "Pelanggaran ($flaggedCount)",
                "DONE" to "Selesai (${completed + needGrading})",
                "NOT_STARTED" to "Belum ($notStarted)",
            ).forEach { (key, label) ->
                val isSel = selectedStatusFilter == key
                val col = when (key) {
                    "FLAGGED" -> NeonError
                    "IN_PROGRESS" -> NeonBlue
                    "DONE" -> NeonSuccess
                    else -> TeacherNeon
                }
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable { selectedStatusFilter = key },
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSel) col.copy(alpha = 0.14f) else cardBg,
                    border = BorderStroke(1.dp, if (isSel) col else cardBorder),
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) col else textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        softWrap = false,
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = innerCardBg,
                border = BorderStroke(1.dp, innerCardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Belum ada peserta pada kategori ini.",
                    fontSize = 12.sp,
                    color = textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp),
                )
            }
        } else {
            filtered.forEach { p ->
                ProctorParticipantCard(
                    participant = p,
                    maxScore = maxScore,
                    onGrade = {
                        p.attemptId?.let { attId ->
                            onGradeAttempt(attId, p.studentId, p.studentName, p.studentNisn, p.rawAttemptStatus ?: "submitted", p.score, p.totalPoints)
                        }
                    },
                    onViewAnswers = {
                        p.attemptId?.let { attId -> onViewAttemptDetail(attId, p.score ?: 0, p.totalPoints) }
                    },
                    onForceSubmit = {
                        p.attemptId?.let { attId -> onForceSubmitAttempt(attId, p.studentName) }
                    },
                    onReopen = {
                        p.attemptId?.let { attId -> onReopenAttempt(attId, p.studentName) }
                    },
                )
            }
        }
    }
}

@Composable
private fun MonitorKpiPill(
    label: String,
    value: Int,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "$value", fontSize = 15.sp, fontWeight = FontWeight.Black, color = color)
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color.copy(alpha = 0.9f), maxLines = 1)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProctorParticipantCard(
    participant: QuizProctorParticipantRow,
    maxScore: Int,
    onGrade: () -> Unit,
    onViewAnswers: () -> Unit,
    onForceSubmit: () -> Unit,
    onReopen: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (participant.leaveViolationsCount > 0) NeonError.copy(alpha = 0.45f)
    else if (participant.proctorStatus == "IN_PROGRESS") NeonBlue.copy(alpha = 0.45f)
    else if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569)

    val statusColor = when (participant.proctorStatus) {
        "IN_PROGRESS" -> NeonBlue
        "NEED_GRADING" -> NeonWarning
        "COMPLETED" -> NeonSuccess
        else -> TextTertiary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = participant.studentName.firstOrNull()?.uppercase() ?: "S",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                        )
                    }
                    if (participant.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(NeonSuccess)
                                .border(1.5.dp, cardBg, CircleShape)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(participant.studentName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = buildString {
                            append("NISN: ${participant.studentNisn}")
                            if (!participant.lastDevicePlatform.isNullOrBlank()) append(" • ${participant.lastDevicePlatform}")
                        },
                        fontSize = 10.5.sp,
                        color = textSecondary,
                    )
                }

                Surface(shape = RoundedCornerShape(7.dp), color = statusColor.copy(alpha = 0.14f)) {
                    Text(
                        text = proctorStatusDisplay(participant.proctorStatus),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            if (participant.proctorStatus != "NOT_STARTED") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (participant.totalQuestions > 0) {
                        Text(
                            text = "Terjawab: ${participant.answeredCount}/${participant.totalQuestions}",
                            fontSize = 11.sp,
                            color = textSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (participant.score != null) {
                        Text(
                            text = "Skor: ${participant.score}/${participant.totalPoints.takeIf { it > 0 } ?: maxScore}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonSuccess,
                        )
                    }
                    if (participant.leaveViolationsCount > 0) {
                        Text(
                            text = "${participant.leaveViolationsCount}x Keluar Aplikasi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonError,
                        )
                    }
                }
            }

            if (participant.attemptId != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SmallProctorButton("Jawaban", Icons.Rounded.Visibility, NeonBlue, onViewAnswers)
                    SmallProctorButton(
                        if (participant.proctorStatus == "NEED_GRADING") "Beri Nilai" else "Koreksi",
                        Icons.Rounded.FactCheck,
                        TeacherNeon,
                        onGrade,
                    )
                    if (participant.proctorStatus == "IN_PROGRESS") {
                        SmallProctorButton("Paksa Kumpul", Icons.Rounded.Gavel, NeonError, onForceSubmit)
                    } else if (participant.proctorStatus in listOf("COMPLETED", "NEED_GRADING")) {
                        SmallProctorButton("Buka Ulang", Icons.Rounded.Replay, NeonWarning, onReopen)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallProctorButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ProctorEventsFeedSection(events: List<QuizProctorEventLog>) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    if (events.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
        ) {
            Text(
                text = "Belum ada aktivitas pengawasan atau pelanggaran tercatat.",
                fontSize = 12.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(20.dp),
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        events.forEach { ev ->
            val isViolation = ev.eventType.uppercase().contains("LEAVE") ||
                ev.eventType.uppercase().contains("BACKGROUND") ||
                ev.eventType.uppercase().contains("SWITCH") ||
                ev.eventType.uppercase().contains("BLUR")
            val evColor = if (isViolation) NeonError else TeacherNeon
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = cardBg,
                border = BorderStroke(1.dp, if (isViolation) NeonError.copy(alpha = 0.35f) else cardBorder),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(evColor)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${ev.studentName} • ${ev.eventType}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        if (!ev.detail.isNullOrBlank()) {
                            Text(ev.detail!!, fontSize = 11.sp, color = textSecondary)
                        }
                    }
                    Text(
                        text = ev.devicePlatform ?: "-",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = evColor,
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionPreviewListSection(
    questions: List<QuizQuestion>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    when {
        isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TeacherNeon)
        }
        error != null -> Surface(shape = RoundedCornerShape(16.dp), color = cardBg, border = BorderStroke(1.dp, cardBorder)) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error, color = NeonError, fontSize = 12.5.sp)
                TextButton(onClick = onRetry) { Text("Coba Lagi", fontWeight = FontWeight.Bold) }
            }
        }
        questions.isEmpty() -> Surface(shape = RoundedCornerShape(16.dp), color = cardBg, border = BorderStroke(1.dp, cardBorder), modifier = Modifier.fillMaxWidth()) {
            Text("Belum ada butir soal tersimpan pada paket ujian ini.", fontSize = 12.sp, color = textSecondary, modifier = Modifier.padding(20.dp), textAlign = TextAlign.Center)
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            questions.forEachIndexed { idx, q ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Soal #${idx + 1} (${q.questionType})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonBlue)
                            Text("${q.points} Poin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TeacherNeon)
                        }
                        QuestionPromptWithDiagram(
                            rawText = q.questionText,
                            explicitImageUrl = q.imageUrl,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary,
                            lineHeight = 19.sp,
                        )
                        if (q.choices.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                val letters = listOf("A", "B", "C", "D", "E")
                                q.choices.forEachIndexed { cIdx, c ->
                                    val isRight = c.isCorrect == true
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (isRight) NeonSuccess.copy(alpha = 0.13f) else innerBg,
                                        border = BorderStroke(1.dp, if (isRight) NeonSuccess.copy(alpha = 0.5f) else cardBorder),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = letters.getOrElse(cIdx) { "${cIdx + 1}" },
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isRight) NeonSuccess else textSecondary,
                                            )
                                            Text(
                                                text = c.choiceText,
                                                fontSize = 12.sp,
                                                color = if (isRight) textPrimary else textSecondary,
                                                fontWeight = if (isRight) FontWeight.SemiBold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f),
                                            )
                                            if (isRight) {
                                                Icon(Icons.Rounded.CheckCircle, null, tint = NeonSuccess, modifier = Modifier.size(15.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
