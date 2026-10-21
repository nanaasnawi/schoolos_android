package com.schoolos.android.feature.assignments.habits

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schoolos.android.core.R as CoreR
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.CanonicalHabit
import com.schoolos.android.domain.model.CanonicalHabitsList
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class HabitVisualConfig(
    val icon: ImageVector,
    val category: String,
    val timeBadge: String,
    val accentColor: Color,
    val lightBgColor: Color,
)

private val habitVisuals = mapOf(
    "bangun_pagi" to HabitVisualConfig(
        icon = Icons.Default.WbSunny,
        category = "Fajar & Mandiri",
        timeBadge = "04.30 - 05.30",
        accentColor = Color(0xFFF59E0B),
        lightBgColor = Color(0xFFFEF3C7)
    ),
    "beribadah" to HabitVisualConfig(
        icon = Icons.Default.SelfImprovement,
        category = "Iman & Takwa",
        timeBadge = "Khusyuk & Tepat Waktu",
        accentColor = Color(0xFF8B5CF6),
        lightBgColor = Color(0xFFEDE9FE)
    ),
    "berolahraga" to HabitVisualConfig(
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        category = "Kebugaran",
        timeBadge = "15+ Menit Gerak",
        accentColor = Color(0xFF10B981),
        lightBgColor = Color(0xFFD1FAE5)
    ),
    "makan_sehat" to HabitVisualConfig(
        icon = Icons.Default.Restaurant,
        category = "Gizi Seimbang",
        timeBadge = "Sayur, Buah & Air",
        accentColor = Color(0xFF06B6D4),
        lightBgColor = Color(0xFFCFFAFE)
    ),
    "gemar_belajar" to HabitVisualConfig(
        icon = Icons.Default.AutoStories,
        category = "Literasi & Nalar",
        timeBadge = "Membaca & Eksplorasi",
        accentColor = Color(0xFF3B82F6),
        lightBgColor = Color(0xFFDBEAFE)
    ),
    "bermasyarakat" to HabitVisualConfig(
        icon = Icons.Default.Diversity3,
        category = "Gotong Royong",
        timeBadge = "Bantu Ortu & Sesama",
        accentColor = Color(0xFFEC4899),
        lightBgColor = Color(0xFFFCE7F3)
    ),
    "tidur_tepat_waktu" to HabitVisualConfig(
        icon = Icons.Default.Bedtime,
        category = "Istirahat Cukup",
        timeBadge = "Sebelum 21.30 WIB",
        accentColor = Color(0xFF6366F1),
        lightBgColor = Color(0xFFE0E7FF)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToAssignments: () -> Unit,
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDark = LocalIsDarkTheme.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.successSnackbar) {
        state.successSnackbar?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        containerColor = CosmicBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ExecutiveTopBar(
                title = if (state.isParent) "Karakter Ananda" else "Jurnal Karakter G7KAIH",
                subtitle = "7 Kebiasaan Anak Indonesia Hebat",
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PullRefreshContainer(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.isLoading) {
                    LoadingState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Hero Overview & Gamified Radial Progress Card
                        item {
                            HabitsHeroCard(
                                state = state,
                                isDark = isDark
                            )
                        }

                        // 2. Slim Companion Link to Tasks ("Daftar Tugas & PR Siswa")
                        item {
                            QuickTasksStrip(
                                onNavigateToAssignments = onNavigateToAssignments,
                            )
                        }

                        // 3. Parent Feedback Banner if already verified
                        if (!state.parentFeedback.isNullOrBlank()) {
                            item {
                                ParentFeedbackCard(
                                    feedback = state.parentFeedback!!,
                                    isDark = isDark
                                )
                            }
                        }

                        // 4. Section Title & Pillar Counter
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "7 Pilar Pembiasaan Harian",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (state.isParent) "Pantau & verifikasi konsistensi ananda" else "Ketuk kartu kebiasaan yang telah kamu laksanakan",
                                        fontSize = 11.sp,
                                        color = TextTertiary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (state.completedCount >= 5) NeonSuccess.copy(alpha = 0.14f) else CosmicSurface2,
                                    border = BorderStroke(0.5.dp, if (state.completedCount >= 5) NeonSuccess.copy(alpha = 0.35f) else GlassBorder)
                                ) {
                                    Text(
                                        text = "${state.completedCount} dari 7 Selesai",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = if (state.completedCount >= 5) NeonSuccess else NeonBlue,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // 5. The 7 Canonical Habit Cards
                        items(CanonicalHabitsList.items, key = { it.key }) { habit ->
                            HabitCardRedesigned(
                                habit = habit,
                                isChecked = state.habitChecks[habit.key] ?: false,
                                note = state.habitNotes[habit.key] ?: "",
                                isExpanded = state.expandedNoteKey == habit.key,
                                isParent = state.isParent,
                                onToggle = { viewModel.toggleHabit(habit.key) },
                                onToggleExpand = { viewModel.toggleNoteExpanded(habit.key) },
                                onNoteChange = { note -> viewModel.setNote(habit.key, note) },
                            )
                        }
                    }
                }
            }

            // ── FLOATING ACTION CAPSULE DOCK (Clean, Non-Intrusive) ───────────
            if (!state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    CosmicBlack.copy(alpha = 0.85f),
                                    CosmicBlack
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isParent) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(12.dp, RoundedCornerShape(25.dp))
                                .clip(RoundedCornerShape(25.dp))
                                .clickable(enabled = !state.isVerifying, onClick = viewModel::openParentVerifyDialog),
                            shape = RoundedCornerShape(25.dp),
                            color = ParentNeon,
                            border = BorderStroke(1.dp, ParentLight.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (state.isVerifying) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Menyimpan Verifikasi...", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                } else {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (state.verificationStatus == "PARENT_VERIFIED") "Perbarui Verifikasi Orang Tua" else "Verifikasi Jurnal Ananda (${state.completedCount}/7)",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(12.dp, RoundedCornerShape(25.dp))
                                .clip(RoundedCornerShape(25.dp))
                                .clickable(enabled = !state.isSaving, onClick = viewModel::saveHabits),
                            shape = RoundedCornerShape(25.dp),
                            color = NeonBlueDark,
                            border = BorderStroke(1.dp, NeonBlueLight.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF2563EB),
                                                Color(0xFF3B82F6),
                                                if (state.completedCount >= 5) Color(0xFF10B981) else Color(0xFF2563EB)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (state.isSaving) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Menyimpan Jurnal...", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    } else {
                                        Icon(
                                            imageVector = if (state.completedCount >= 5) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Simpan Jurnal Karakter (${state.completedCount}/7 Selesai)",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Verifikasi Orang Tua
    if (state.showParentVerifyDialog) {
        AlertDialog(
            onDismissRequest = viewModel::closeParentVerifyDialog,
            shape = RoundedCornerShape(20.dp),
            containerColor = CosmicNavy,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = ParentNeon, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Verifikasi Jurnal Karakter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Sebagai orang tua/wali dari ${state.childName}, verifikasi bahwa ananda telah membiasakan ${state.completedCount} dari 7 karakter hebat hari ini.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )

                    OutlinedTextField(
                        value = state.parentFeedbackInput,
                        onValueChange = viewModel::setParentFeedbackInput,
                        label = { Text("Catatan / Apresiasi untuk Ananda (Opsional)", fontSize = 11.sp) },
                        placeholder = { Text("Contoh: Hebat nak, pertahankan bangun pagi & beribadah!", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = TextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::submitParentVerification,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ParentNeon)
                ) {
                    Text("Simpan Verifikasi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeParentVerifyDialog) {
                    Text("Batal", fontSize = 13.sp, color = TextSecondary)
                }
            }
        )
    }
}

// ── 1. HERO OVERVIEW WITH CIRCULAR PROGRESS RING ──────────────────────────────

@Composable
private fun HabitCircularProgress(
    completed: Int,
    total: Int = 7,
    modifier: Modifier = Modifier,
) {
    val progress = (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "habit_progress"
    )
    val strokeColor = if (completed >= 5) NeonSuccess else NeonBlue
    val trackColor = strokeColor.copy(alpha = 0.16f)

    Box(
        modifier = modifier.size(68.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val strokeWidth = 5.5.dp.toPx()
            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = strokeColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$completed/$total",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = strokeColor,
            )
        }
    }
}

@Composable
private fun HabitsHeroCard(
    state: HabitsUiState,
    isDark: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Top Row: Tag & Streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Curriculum Badge Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonBlue.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, NeonBlue.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "G7KAIH • KEMENDIKDASMEN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonBlue,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Streak Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentNeonAmber.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, AccentNeonAmber.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = AccentNeonAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (state.streakDays > 0) "${state.streakDays} Hari Streak" else "Hari ke-1",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentNeonAmber
                        )
                    }
                }
            }

            // Middle Row: Title, Motivation Quote & Circular Ring
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = if (state.isParent) "Karakter Harian ${state.childName}" else "7 Kebiasaan Anak Indonesia",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = getFormattedTodayDate(),
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                    Spacer(Modifier.height(6.dp))

                    val motivation = when (state.completedCount) {
                        0 -> "Mulai hari dengan kebiasaan baik!"
                        in 1..3 -> "Berproses hebat, pertahankan konsistensi!"
                        in 4..6 -> "Sedikit lagi tuntas seluruh 7 pilar!"
                        else -> "Luar biasa! Seluruh pilar terlaksana hari ini!"
                    }
                    Text(
                        text = motivation,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.completedCount >= 5) NeonSuccess else NeonBlue
                    )
                }

                HabitCircularProgress(
                    completed = state.completedCount,
                    total = 7
                )
            }

            HorizontalDivider(
                color = GlassBorder,
                thickness = 0.5.dp
            )

            // Bottom Metrics: Status & Semester
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statusText = when (state.verificationStatus) {
                    "PARENT_VERIFIED" -> "Diverifikasi Orang Tua"
                    "TEACHER_OVERRIDE" -> "Supervisi Wali Kelas"
                    "PENDING" -> "Menunggu Verifikasi"
                    else -> "Belum Dicatat Hari Ini"
                }
                val statusColor = when (state.verificationStatus) {
                    "PARENT_VERIFIED" -> NeonSuccess
                    "TEACHER_OVERRIDE" -> NeonBlue
                    "PENDING" -> NeonWarning
                    else -> TextTertiary
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }

                Text(
                    text = "Semester Ganjil 2026/2027",
                    fontSize = 10.sp,
                    color = TextTertiary
                )
            }
        }
    }
}

// ── 2. SLIM COMPANION TASKS STRIP ─────────────────────────────────────────────

@Composable
private fun QuickTasksStrip(
    onNavigateToAssignments: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onNavigateToAssignments)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudentNeon.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_tasks),
                        contentDescription = null,
                        tint = StudentNeon,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Daftar Tugas & PR Siswa",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Cek tenggat & kumpulkan tugas tepat waktu",
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Lihat",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StudentNeon
                )
                Spacer(Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = StudentNeon,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

// ── 3. PARENT FEEDBACK CARD ───────────────────────────────────────────────────

@Composable
private fun ParentFeedbackCard(
    feedback: String,
    isDark: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ParentNeon.copy(alpha = 0.08f))
            .border(0.8.dp, ParentNeon.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = ParentNeon,
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Catatan & Apresiasi Orang Tua / Wali",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ParentNeon
                )
                Text(
                    text = "\"$feedback\"",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

// ── 4. REDESIGNED HABIT ITEM CARD ─────────────────────────────────────────────

@Composable
private fun HabitCardRedesigned(
    habit: CanonicalHabit,
    isChecked: Boolean,
    note: String,
    isExpanded: Boolean,
    isParent: Boolean,
    onToggle: () -> Unit,
    onToggleExpand: () -> Unit,
    onNoteChange: (String) -> Unit,
) {
    val visual = habitVisuals[habit.key] ?: HabitVisualConfig(
        icon = Icons.Default.Star,
        category = "Karakter",
        timeBadge = "Harian",
        accentColor = NeonBlue,
        lightBgColor = Color(0xFFDBEAFE)
    )

    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isChecked) {
        if (isDark) visual.accentColor.copy(alpha = 0.08f) else visual.lightBgColor.copy(alpha = 0.45f)
    } else {
        CosmicNavy
    }
    val cardBorderColor = if (isChecked) {
        visual.accentColor.copy(alpha = 0.5f)
    } else {
        GlassBorder
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(if (isChecked) 1.dp else 0.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = if (!isParent) onToggle else onToggleExpand)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Main Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Icon Squircle + Title & Badges
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon Squircle Badge
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isChecked) visual.accentColor.copy(alpha = 0.2f)
                                else CosmicSurface2
                            )
                            .border(
                                0.5.dp,
                                if (isChecked) visual.accentColor.copy(alpha = 0.4f) else GlassBorder,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = null,
                            tint = visual.accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = visual.category.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = visual.accentColor,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = " • ",
                                fontSize = 9.sp,
                                color = TextTertiary
                            )
                            Text(
                                text = visual.timeBadge,
                                fontSize = 9.sp,
                                color = TextTertiary
                            )
                        }
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = habit.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = habit.subtitle,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Right: Check Action (Interactive Circle) or Parent Verification Badge
                if (!isParent) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isChecked) visual.accentColor else Color.Transparent
                            )
                            .border(
                                width = if (isChecked) 0.dp else 1.5.dp,
                                color = if (isChecked) Color.Transparent else if (isDark) Color(0xFF3F3F46) else Color(0xFFCBD5E1),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Tuntas",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isChecked) NeonSuccess.copy(alpha = 0.15f) else Color(0x1594A3B8),
                        border = BorderStroke(
                            0.5.dp,
                            if (isChecked) NeonSuccess.copy(alpha = 0.4f) else GlassBorder
                        )
                    ) {
                        Text(
                            text = if (isChecked) "Terlaksana" else "Belum",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isChecked) NeonSuccess else TextTertiary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Description text
            Text(
                text = habit.description,
                fontSize = 11.sp,
                color = TextTertiary,
                lineHeight = 15.sp,
                modifier = Modifier.padding(start = 54.dp)
            )

            // Note pill or + Catatan button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 54.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.isNotBlank() && !isExpanded) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CosmicSurface2,
                        border = BorderStroke(0.5.dp, GlassBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleExpand)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.StickyNote2,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = note,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (!isParent) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit catatan",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                } else if (!isParent && !isExpanded) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        border = BorderStroke(0.5.dp, GlassBorder),
                        modifier = Modifier.clickable(onClick = onToggleExpand)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = visual.accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Catatan refleksi (opsional)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = visual.accentColor
                            )
                        }
                    }
                }
            }

            // Expanded Note Input Field
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 54.dp, top = 4.dp)
                ) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        placeholder = { Text("Contoh: ${habit.exampleNote}", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = TextPrimary)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onToggleExpand,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Tutup", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = visual.accentColor)
                        }
                    }
                }
            }
        }
    }
}

private fun getFormattedTodayDate(): String {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID"))
    return today.format(formatter)
}
