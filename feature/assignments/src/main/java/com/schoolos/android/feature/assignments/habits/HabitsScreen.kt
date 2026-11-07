package com.schoolos.android.feature.assignments.habits

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.CanonicalHabit
import com.schoolos.android.domain.model.CanonicalHabitsList
import com.schoolos.android.domain.model.HabitEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private data class HabitVisualConfig(
    val icon: ImageVector,
    val category: String,
    val timeBadge: String,
    val accentColor: Color,
)

private val habitVisuals = mapOf(
    "bangun_pagi" to HabitVisualConfig(
        icon = Icons.Default.Alarm,
        category = "Disiplin Waktu",
        timeBadge = "Sebelum Subuh",
        accentColor = Color(0xFFF59E0B)
    ),
    "beribadah" to HabitVisualConfig(
        icon = Icons.Default.SelfImprovement,
        category = "Spiritual & Moral",
        timeBadge = "Tepat Waktu",
        accentColor = Color(0xFF10B981)
    ),
    "berolahraga" to HabitVisualConfig(
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        category = "Kebugaran Fisik",
        timeBadge = "15-30 Menit",
        accentColor = Color(0xFFEC4899)
    ),
    "makan_sehat" to HabitVisualConfig(
        icon = Icons.Default.Restaurant,
        category = "Gizi Seimbang",
        timeBadge = "Sayur, Buah & Air",
        accentColor = Color(0xFF06B6D4)
    ),
    "gemar_belajar" to HabitVisualConfig(
        icon = Icons.Default.AutoStories,
        category = "Literasi & Nalar",
        timeBadge = "Membaca & Eksplorasi",
        accentColor = Color(0xFF3B82F6)
    ),
    "bermasyarakat" to HabitVisualConfig(
        icon = Icons.Default.Diversity3,
        category = "Gotong Royong",
        timeBadge = "Bantu Ortu & Sesama",
        accentColor = Color(0xFF8B5CF6)
    ),
    "tidur_tepat_waktu" to HabitVisualConfig(
        icon = Icons.Default.Bedtime,
        category = "Istirahat Cukup",
        timeBadge = "Sebelum 21.30 WIB",
        accentColor = Color(0xFF6366F1)
    )
)

private data class DayHabitStat(
    val dayKey: String,
    val dayLabelIndo: String,
    val dayFullIndo: String,
    val date: LocalDate,
    val completedCount: Int,
    val complianceRate: Float,
    val isToday: Boolean,
    val isPeak: Boolean,
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
    var showAllHabits by remember { mutableStateOf(true) }

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

    // Modern card background and border colors respecting theme
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val screenBg = if (isDark) CosmicBlack else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Calculate 7-day stats for the Completion Trend chart
    val todayDate = remember { LocalDate.now() }
    val weeklyStats = remember(state.weeklyHistory, state.completedCount, todayDate) {
        calculateWeeklyTrend(
            weeklyHistory = state.weeklyHistory,
            todayDate = todayDate,
            todayCompletedCount = state.completedCount
        )
    }

    val peakStat = weeklyStats.find { it.isPeak && it.completedCount > 0 }

    Scaffold(
        containerColor = screenBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ExecutiveTopBar(
                title = if (state.isParent) "Karakter Ananda" else "Jurnal Karakter G7KAIH",
                subtitle = "7 Kebiasaan Anak Indonesia Hebat",
                onBack = onBack,
            )
        },
        bottomBar = {}, // Tidak ada tombol simpan di luar; simpan langsung di dalam pengisian task jurnal!
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
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ── 1. DUAL STAT CARDS: SELESAI & BELUM DIISI (Bahasa Indonesia Baku) ──
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Selesai Card
                                StatCardWidget(
                                    modifier = Modifier.weight(1f),
                                    title = "Selesai",
                                    count = state.completedCount,
                                    subtitle = "Aktivitas",
                                    badgeType = StatBadgeType.COMPLETED,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )

                                // Belum Diisi Card
                                val pendingCount = (7 - state.completedCount).coerceAtLeast(0)
                                StatCardWidget(
                                    modifier = Modifier.weight(1f),
                                    title = "Belum Diisi",
                                    count = pendingCount,
                                    subtitle = "Aktivitas",
                                    badgeType = StatBadgeType.PENDING,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                            }
                        }

                        // ── 2. TREN PEMBIASAAN KARAKTER (7 Hari) ──
                        item {
                            CompletionTrendCard(
                                peakStat = peakStat,
                                weeklyStats = weeklyStats,
                                stateComplianceRate = state.complianceRate,
                                streakDays = state.streakDays,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                isDark = isDark,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                        }

                        // ── 3. PARENT VERIFICATION BANNER (Hanya jika peran Orang Tua) ──
                        if (state.isParent) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    color = if (isDark) Color(0xFF1E1638) else Color(0xFFFBF7FF),
                                    border = BorderStroke(1.dp, ParentNeon.copy(alpha = 0.35f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(ParentNeon.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = null,
                                                    tint = ParentNeon,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Verifikasi Pendampingan Orang Tua",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = ParentNeon
                                                )
                                                Text(
                                                    text = if (state.verificationStatus == "PARENT_VERIFIED") {
                                                        "Jurnal hari ini telah diverifikasi oleh Anda"
                                                    } else {
                                                        "Ananda telah mengisi ${state.completedCount}/7 pembiasaan hari ini"
                                                    },
                                                    fontSize = 11.5.sp,
                                                    color = textSecondary
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = viewModel::openParentVerifyDialog,
                                            enabled = !state.isVerifying,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ParentNeon,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RateReview,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (state.verificationStatus == "PARENT_VERIFIED") "Perbarui Verifikasi" else "Verifikasi Jurnal Ananda",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── 4. CATATAN DARI ORANG TUA (Jika ada) ──
                        if (!state.parentFeedback.isNullOrBlank()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isDark) Color(0xFF1E1638) else Color(0xFFFBF7FF),
                                    border = BorderStroke(1.dp, ParentNeon.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(ParentNeon.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RateReview,
                                                contentDescription = null,
                                                tint = ParentNeon,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Apresiasi Orang Tua",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = ParentNeon
                                            )
                                            Text(
                                                text = "\"${state.parentFeedback}\"",
                                                fontSize = 12.sp,
                                                color = textPrimary,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── 5. HEADER 7 PEMBIASAAN KARAKTER HEBAT ──
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "7 Pembiasaan Karakter Hebat",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )

                                Text(
                                    text = if (showAllHabits) "7 Pilar Aktif" else "Lihat Semua",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSecondary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showAllHabits = !showAllHabits }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // ── 6. DAFTAR TASK JURNAL (Dengan Pengisian & Tombol Simpan di Dalam Task) ──
                        val habitsToShow = if (showAllHabits) CanonicalHabitsList.items else CanonicalHabitsList.items.take(4)
                        items(habitsToShow, key = { it.key }) { habit ->
                            val isChecked = state.habitChecks[habit.key] ?: false
                            val note = state.habitNotes[habit.key] ?: ""
                            val isExpanded = state.expandedNoteKey == habit.key

                            val weeklyFrequency = calculateHabitFrequency(
                                habitKey = habit.key,
                                weeklyHistory = state.weeklyHistory,
                                isCheckedToday = isChecked
                            )

                            HabitTaskJournalCard(
                                habit = habit,
                                isChecked = isChecked,
                                weeklyFrequency = weeklyFrequency,
                                note = note,
                                isExpanded = isExpanded,
                                isParent = state.isParent,
                                isSaving = state.isSaving,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                isDark = isDark,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                onToggleCheck = { viewModel.toggleHabitAndSave(habit.key) },
                                onToggleExpand = { viewModel.toggleNoteExpanded(habit.key) },
                                onSaveTaskNote = { updatedNote ->
                                    viewModel.saveHabitTask(habit.key, updatedNote)
                                }
                            )
                        }

                        item {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Dialog Verifikasi Orang Tua
    if (state.showParentVerifyDialog) {
        ParentVerificationDialog(
            state = state,
            onDismiss = viewModel::closeParentVerifyDialog,
            onFeedbackChange = viewModel::setParentFeedbackInput,
            onSubmit = viewModel::submitParentVerification
        )
    }
}

// ── WIDGET KARTU STATISTIK (SELESAI & BELUM DIISI) ────────────────────────────

private enum class StatBadgeType { COMPLETED, PENDING }

@Composable
private fun StatCardWidget(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    subtitle: String,
    badgeType: StatBadgeType,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )

                if (badgeType == StatBadgeType.COMPLETED) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PriorityHigh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Text(
                text = count.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary
            )

            Text(
                text = subtitle,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                color = textSecondary
            )
        }
    }
}

// ── KARTU TREN PEMBIASAAN MINGGUAN ───────────────────────────────────────────

@Composable
private fun CompletionTrendCard(
    peakStat: DayHabitStat?,
    weeklyStats: List<DayHabitStat>,
    stateComplianceRate: Double,
    streakDays: Int,
    cardBg: Color,
    cardBorder: Color,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tren Pembiasaan Karakter",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF27272A) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "🔥 Streak: $streakDays Hari",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Skor % dan Hari Tertinggi (Dihitung Real Tanpa Data Dummy)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val peakRate = if (peakStat != null && peakStat.complianceRate > 0f) {
                    (peakStat.complianceRate * 100).toInt()
                } else if (stateComplianceRate > 0.0) {
                    stateComplianceRate.toInt()
                } else {
                    0
                }

                Text(
                    text = "$peakRate%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )

                val trendSubtitle = when {
                    peakStat != null && peakStat.completedCount > 0 -> "Tertinggi hari ${peakStat.dayFullIndo}"
                    peakRate > 0 -> "Kepatuhan hari ini"
                    else -> "Belum ada aktivitas selesai"
                }

                Text(
                    text = trendSubtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Grafik 7 Hari Bar Chart (Senin s/d Minggu)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyStats.forEach { stat ->
                    DayBarColumn(
                        stat = stat,
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DayBarColumn(
    stat: DayHabitStat,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
) {
    val barHeight = remember(stat.complianceRate) {
        val calculated = (stat.complianceRate * 64.dp.value).dp
        calculated.coerceIn(24.dp, 64.dp)
    }

    val barShape = RoundedCornerShape(14.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (stat.isPeak) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(barHeight)
                    .clip(barShape)
                    .background(if (isDark) Color(0xFF0F243A) else Color(0xFFE0F2FE))
                    .border(1.5.dp, Color(0xFF0EA5E9), barShape),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .background(Color(0xFF0EA5E9))
                )
            }
        } else {
            val barBg = if (isDark) Color(0xFF27272A) else Color(0xFFF8FAFC)
            val barBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(barHeight)
                    .clip(barShape)
                    .background(barBg)
                    .border(1.dp, barBorderColor, barShape)
            )
        }

        // Label Hari Bahasa Indonesia (Sen, Sel, Rab, Kam, Jum, Sab, Min)
        Text(
            text = stat.dayLabelIndo,
            fontSize = 11.5.sp,
            fontWeight = if (stat.isPeak || stat.isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (stat.isPeak || stat.isToday) textPrimary else textSecondary
        )
    }
}

// ── KARTU TASK JURNAL DENGAN PENGISIAN & TOMBOL SIMPAN DI DALAM ──────────────

@Composable
private fun HabitTaskJournalCard(
    habit: CanonicalHabit,
    isChecked: Boolean,
    weeklyFrequency: Int,
    note: String,
    isExpanded: Boolean,
    isParent: Boolean,
    isSaving: Boolean,
    cardBg: Color,
    cardBorder: Color,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    onToggleCheck: () -> Unit,
    onToggleExpand: () -> Unit,
    onSaveTaskNote: (String) -> Unit,
) {
    val visual = habitVisuals[habit.key] ?: HabitVisualConfig(
        icon = Icons.Default.Star,
        category = "Karakter",
        timeBadge = "Harian",
        accentColor = Color(0xFF0EA5E9)
    )

    var currentNoteInput by remember(note) { mutableStateOf(note) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onToggleExpand),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, if (isChecked) visual.accentColor.copy(alpha = 0.5f) else cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Baris Utama: Ikon, Judul, Progress Bar, Frekuensi, dan Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ikon Habit
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(visual.accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Judul & Bar Frekuensi
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (visual.timeBadge.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = visual.timeBadge,
                            fontSize = 10.5.sp,
                            color = visual.accentColor,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(5.dp))

                    val animatedProgress by animateFloatAsState(
                        targetValue = if (isChecked) 1f else (weeklyFrequency.toFloat() / 7f).coerceIn(0.12f, 1f),
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        label = "habit_bar_progress"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = animatedProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(visual.accentColor)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Rasio Frekuensi Mingguan
                Text(
                    text = "$weeklyFrequency/7",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(Modifier.width(10.dp))

                // Tombol Checklist Cepat (Otomatis Simpan Status)
                if (!isParent) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isChecked) Color(0xFF10B981) else Color.Transparent)
                            .border(
                                width = if (isChecked) 0.dp else 1.5.dp,
                                color = if (isChecked) Color.Transparent else if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                                shape = CircleShape
                            )
                            .clickable(onClick = onToggleCheck),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Tandai Selesai",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isChecked) Color(0xFF10B981) else textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // ── AREA PENGISIAN TASK JURNAL (Refleksi & Tombol Simpan di Dalam Task) ──
            AnimatedVisibility(
                visible = isExpanded || note.isNotBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Deskripsi Panduan Karakter
                    Text(
                        text = habit.description,
                        fontSize = 11.5.sp,
                        color = textSecondary,
                        lineHeight = 16.sp
                    )

                    if (!isParent) {
                        // Input Catatan Jurnal
                        OutlinedTextField(
                            value = currentNoteInput,
                            onValueChange = { currentNoteInput = it },
                            placeholder = {
                                Text(
                                    text = "Catatan jurnal: ${habit.exampleNote}",
                                    fontSize = 11.5.sp,
                                    color = textSecondary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = textPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = visual.accentColor,
                                unfocusedBorderColor = cardBorder
                            )
                        )

                        // Baris Aksi Di Dalam Task: Tombol Simpan Task Jurnal & Tutup
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isChecked) "✓ Status: Sudah Dilaksanakan" else "○ Status: Belum Selesai",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isChecked) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onSaveTaskNote(currentNoteInput)
                                    },
                                    enabled = !isSaving,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = visual.accentColor,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Simpan Jurnal",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else if (note.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF161618) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else cardBorder)
                        ) {
                            Text(
                                text = "Catatan Ananda: \"$note\"",
                                fontSize = 11.5.sp,
                                color = textPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── DIALOG VERIFIKASI ORANG TUA ───────────────────────────────────────────────

@Composable
private fun ParentVerificationDialog(
    state: HabitsUiState,
    onDismiss: () -> Unit,
    onFeedbackChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Verifikasi Karakter Ananda",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Berikan apresiasi dan konfirmasi pembiasaan 7 karakter ananda hari ini (${state.completedCount}/7 Selesai).",
                    fontSize = 12.sp,
                    color = if (isDark) TextSecondary else Color(0xFF64748B)
                )

                OutlinedTextField(
                    value = state.parentFeedbackInput,
                    onValueChange = onFeedbackChange,
                    placeholder = {
                        Text(
                            text = "Contoh: Alhamdulillah ananda konsisten bangun pagi dan beribadah dengan tertib.",
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = ParentNeon)
            ) {
                Text("Simpan Verifikasi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextTertiary)
            }
        }
    )
}

// ── PERHITUNGAN BANTUAN TREN MINGGUAN ──────────────────────────────────────────

private fun calculateWeeklyTrend(
    weeklyHistory: List<HabitEntry>,
    todayDate: LocalDate,
    todayCompletedCount: Int,
): List<DayHabitStat> {
    val monday = todayDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val dayNamesId = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    val dayFullId = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

    val stats = (0..6).map { offset ->
        val date = monday.plusDays(offset.toLong())
        val dateStr = date.toString()
        val entry = weeklyHistory.find { it.entryDate == dateStr }
        val isToday = date == todayDate
        val count = if (isToday) todayCompletedCount else (entry?.completedCount ?: 0)
        val rate = if (isToday) {
            (todayCompletedCount.toFloat() / 7f)
        } else {
            (entry?.complianceRate?.toFloat() ?: (count.toFloat() / 7f * 100f)) / 100f
        }

        DayHabitStat(
            dayKey = dayNamesId[offset],
            dayLabelIndo = dayNamesId[offset],
            dayFullIndo = dayFullId[offset],
            date = date,
            completedCount = count,
            complianceRate = rate.coerceIn(0f, 1f),
            isToday = isToday,
            isPeak = false,
        )
    }

    val maxCount = stats.maxOfOrNull { it.completedCount } ?: 0
    val peakIndex = if (maxCount > 0) {
        stats.indexOfFirst { it.completedCount == maxCount }
    } else {
        -1
    }

    return stats.mapIndexed { idx, s -> s.copy(isPeak = (peakIndex != -1 && idx == peakIndex)) }
}

private fun calculateHabitFrequency(
    habitKey: String,
    weeklyHistory: List<HabitEntry>,
    isCheckedToday: Boolean,
): Int {
    val pastCompletedDays = weeklyHistory.count { it.habits[habitKey] == true }
    val effectiveDays = if (isCheckedToday) maxOf(pastCompletedDays, 1) else pastCompletedDays
    return effectiveDays.coerceIn(0, 7)
}
