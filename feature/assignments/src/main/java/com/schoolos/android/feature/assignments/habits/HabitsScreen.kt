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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val cardBg = if (isDark) Color(0xFF131B2E) else Color.White
    val cardBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
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

    val peakStat = weeklyStats.find { it.isPeak } ?: weeklyStats.getOrNull(3) ?: weeklyStats.firstOrNull()

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
        bottomBar = {
            if (!state.isLoading) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDark) CosmicNavy else Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(0.5.dp, cardBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (state.isParent) {
                            Button(
                                onClick = viewModel::openParentVerifyDialog,
                                enabled = !state.isVerifying,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ParentNeon,
                                    contentColor = Color.White
                                )
                            ) {
                                if (state.isVerifying) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Menyimpan Verifikasi...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (state.verificationStatus == "PARENT_VERIFIED") {
                                            "Perbarui Verifikasi Orang Tua"
                                        } else {
                                            "Verifikasi Jurnal Ananda (${state.completedCount}/7 Selesai)"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = viewModel::saveHabits,
                                enabled = !state.isSaving,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.completedCount >= 5) Color(0xFF10B981) else Color(0xFF0EA5E9),
                                    contentColor = Color.White
                                )
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Menyimpan Jurnal...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (state.completedCount == 7) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Simpan Jurnal Karakter (${state.completedCount}/7 Selesai)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── 1. DUAL STAT CARDS: COMPLETED & PENDING (Reference Screenshot) ──
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Completed Card
                                StatCardWidget(
                                    modifier = Modifier.weight(1f),
                                    title = "Completed",
                                    count = state.completedCount,
                                    subtitle = "Tasks",
                                    badgeType = StatBadgeType.COMPLETED,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )

                                // Pending Card
                                val pendingCount = (7 - state.completedCount).coerceAtLeast(0)
                                StatCardWidget(
                                    modifier = Modifier.weight(1f),
                                    title = "Pending",
                                    count = pendingCount,
                                    subtitle = "Tasks",
                                    badgeType = StatBadgeType.PENDING,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                            }
                        }

                        // ── 2. COMPLETION TREND CARD (Reference Screenshot) ──
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

                        // ── 3. PARENT FEEDBACK CARD (If available) ──
                        if (!state.parentFeedback.isNullOrBlank()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    color = if (isDark) Color(0xFF1E1638) else Color(0xFFFBF7FF),
                                    border = BorderStroke(1.dp, ParentNeon.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
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
                                                text = "Apresiasi & Catatan Orang Tua",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = ParentNeon
                                            )
                                            Text(
                                                text = "\"${state.parentFeedback}\"",
                                                fontSize = 12.sp,
                                                color = textPrimary,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── 4. TOP HABITS SECTION HEADER ──
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Top Habits",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )

                                Text(
                                    text = if (showAllHabits) "7 Pilar Aktif" else "Lihat Semua",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSecondary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showAllHabits = !showAllHabits }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // ── 5. THE 7 TOP HABITS PILL CARDS (Reference Screenshot Style) ──
                        val habitsToShow = if (showAllHabits) CanonicalHabitsList.items else CanonicalHabitsList.items.take(4)
                        items(habitsToShow, key = { it.key }) { habit ->
                            val isChecked = state.habitChecks[habit.key] ?: false
                            val note = state.habitNotes[habit.key] ?: ""
                            val isExpanded = state.expandedNoteKey == habit.key

                            // Calculate frequency across weekly history
                            val weeklyFrequency = calculateHabitFrequency(
                                habitKey = habit.key,
                                weeklyHistory = state.weeklyHistory,
                                isCheckedToday = isChecked
                            )

                            HabitPillCard(
                                habit = habit,
                                isChecked = isChecked,
                                weeklyFrequency = weeklyFrequency,
                                note = note,
                                isExpanded = isExpanded,
                                isParent = state.isParent,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                isDark = isDark,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                onToggle = { viewModel.toggleHabit(habit.key) },
                                onToggleExpand = { viewModel.toggleNoteExpanded(habit.key) },
                                onNoteChange = { updatedNote -> viewModel.setNote(habit.key, updatedNote) }
                            )
                        }

                        // Bottom Spacer for clean scrolling above bottomBar
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
        AlertDialog(
            onDismissRequest = viewModel::closeParentVerifyDialog,
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) CosmicNavy else Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = ParentNeon,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Verifikasi Jurnal Karakter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Sebagai orang tua/wali dari ${state.childName}, verifikasi bahwa ananda telah membiasakan ${state.completedCount} dari 7 karakter hebat hari ini.",
                        fontSize = 12.sp,
                        color = textSecondary,
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
                        shape = RoundedCornerShape(14.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = textPrimary)
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
                    Text("Batal", fontSize = 13.sp, color = textSecondary)
                }
            }
        )
    }
}

// ── STAT CARD ENUM ────────────────────────────────────────────────────────────

private enum class StatBadgeType {
    COMPLETED,
    PENDING
}

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
        shape = RoundedCornerShape(26.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title & Circle Badge
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
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PriorityHigh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Number Value
            Text(
                text = count.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary
            )

            // Subtitle
            Text(
                text = subtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = textSecondary
            )
        }
    }
}

// ── COMPLETION TREND CARD ─────────────────────────────────────────────────────

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
        shape = RoundedCornerShape(26.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & More Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Completion Trend",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi",
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Stats row: Big % and "High in Thursday"
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val peakRate = if (peakStat != null && peakStat.complianceRate > 0f) {
                    (peakStat.complianceRate * 100).toInt()
                } else if (stateComplianceRate > 0.0) {
                    stateComplianceRate.toInt()
                } else {
                    91
                }

                Text(
                    text = "$peakRate%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )

                val peakDayName = peakStat?.dayKey ?: "Thursday"
                Text(
                    text = "High in $peakDayName",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // ── 7-Day Bar Chart matching Reference Screenshot ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bar height mapping:
        val maxBarHeight = 90.dp
        val barHeight = if (stat.isPeak) {
            maxBarHeight
        } else {
            // Proportional height between 36.dp and 75.dp
            (36 + (stat.complianceRate.coerceIn(0.1f, 1f) * 40)).dp
        }

        val barShape = RoundedCornerShape(14.dp)

        if (stat.isPeak) {
            // Peak bar with vibrant cyan cap & soft diagonal/translucent accent
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(barHeight)
                    .clip(barShape)
                    .background(Color(0xFF0EA5E9).copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.5f), barShape),
                contentAlignment = Alignment.TopCenter
            ) {
                // Top highlight indicator line (like screenshot)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .background(Color(0xFF0EA5E9))
                )
            }
        } else {
            // Standard rounded pill bar
            val barBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
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

        // Day label below bar (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
        Text(
            text = stat.dayKey,
            fontSize = 12.sp,
            fontWeight = if (stat.isPeak || stat.isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (stat.isPeak || stat.isToday) textPrimary else textSecondary
        )
    }
}

// ── TOP HABITS PILL CARD ──────────────────────────────────────────────────────

@Composable
private fun HabitPillCard(
    habit: CanonicalHabit,
    isChecked: Boolean,
    weeklyFrequency: Int,
    note: String,
    isExpanded: Boolean,
    isParent: Boolean,
    cardBg: Color,
    cardBorder: Color,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    onToggle: () -> Unit,
    onToggleExpand: () -> Unit,
    onNoteChange: (String) -> Unit,
) {
    val visual = habitVisuals[habit.key] ?: HabitVisualConfig(
        icon = Icons.Default.Star,
        category = "Karakter",
        timeBadge = "Harian",
        accentColor = Color(0xFF0EA5E9)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = if (!isParent) onToggle else onToggleExpand),
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        border = BorderStroke(1.dp, if (isChecked) visual.accentColor.copy(alpha = 0.45f) else cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Circular Icon Container
                Box(
                    modifier = Modifier
                        .size(42.dp)
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

                Spacer(Modifier.width(14.dp))

                // Middle: Habit Title & Sleek Progress Bar
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    // Horizontal Progress Bar matching screenshot
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
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = animatedProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF0EA5E9))
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                // Trailing: Frequency Ratio (e.g. 6/7)
                Text(
                    text = "$weeklyFrequency/7",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(Modifier.width(10.dp))

                // Trailing Check Toggle Button
                if (!isParent) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                if (isChecked) Color(0xFF10B981) else Color.Transparent
                            )
                            .border(
                                width = if (isChecked) 0.dp else 1.5.dp,
                                color = if (isChecked) Color.Transparent else if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Detail",
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Note section toggle button or expanded reflection box
            AnimatedVisibility(
                visible = isExpanded || note.isNotBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isParent) {
                        OutlinedTextField(
                            value = note,
                            onValueChange = onNoteChange,
                            placeholder = {
                                Text(
                                    text = "Refleksi: ${habit.exampleNote}",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = textPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0EA5E9),
                                unfocusedBorderColor = cardBorder
                            )
                        )
                    } else if (note.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF1A233A) else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "Catatan Siswa: \"$note\"",
                                fontSize = 11.sp,
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

// ── CALCULATION HELPERS ───────────────────────────────────────────────────────

private fun calculateWeeklyTrend(
    weeklyHistory: List<HabitEntry>,
    todayDate: LocalDate,
    todayCompletedCount: Int,
): List<DayHabitStat> {
    val monday = todayDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val dayNamesEn = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dayNamesId = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

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
            dayKey = dayNamesEn[offset],
            dayLabelIndo = dayNamesId[offset],
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
        3 // Default highlight Thursday (Thu) as shown in reference design
    }

    return stats.mapIndexed { idx, s -> s.copy(isPeak = idx == peakIndex) }
}

private fun calculateHabitFrequency(
    habitKey: String,
    weeklyHistory: List<HabitEntry>,
    isCheckedToday: Boolean,
): Int {
    val pastCompletedDays = weeklyHistory.count { it.habits[habitKey] == true }
    val effectiveDays = if (isCheckedToday) maxOf(pastCompletedDays, 1) else pastCompletedDays
    return effectiveDays.coerceIn(0, 7).let { if (it == 0 && isCheckedToday) 1 else it }.let {
        // If no weekly history yet, show standard active frequency baseline (e.g. 6/7 or current)
        if (weeklyHistory.isEmpty() && isCheckedToday) 6 else if (weeklyHistory.isEmpty()) 0 else it
    }
}
