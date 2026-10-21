package com.schoolos.android.feature.assignments.habits

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.CanonicalHabit
import com.schoolos.android.domain.model.CanonicalHabitsList
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                title = if (state.isParent) "Karakter Ananda" else "Jurnal Karakter (G7KAIH)",
                subtitle = "Gerakan 7 Kebiasaan Anak Indonesia Hebat",
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!state.isLoading) {
                Surface(
                    color = if (isDark) CosmicNavy else Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                enabled = !state.isVerifying,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ParentNeon,
                                    contentColor = Color.White
                                )
                            ) {
                                if (state.isVerifying) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Menyimpan Verifikasi...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (state.verificationStatus == "PARENT_VERIFIED") "Perbarui Verifikasi & Feedback"
                                        else "Verifikasi Jurnal Ananda Hari Ini",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = viewModel::saveHabits,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                enabled = !state.isSaving,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Menyimpan Jurnal...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Simpan Jurnal Karakter (${state.completedCount}/7 Selesai)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Header Overview & Progress Card
                        item {
                            HabitsOverviewCard(
                                state = state,
                                isDark = isDark
                            )
                        }

                        // 2. Quick Link Card to Tasks ("Daftar Tugas Siswa")
                        item {
                            QuickTasksCard(
                                onNavigateToAssignments = onNavigateToAssignments,
                                isDark = isDark
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

                        // 4. Section Title
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
                                        fontSize = 16.sp,
                                        color = if (isDark) Color.White else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = if (state.isParent) "Pantau & berikan tanda bintang atas capaian ananda" else "Centang kebiasaan yang sudah kamu lakukan hari ini",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = "${state.completedCount}/7",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = if (state.completedCount >= 5) AccentNeonGreen else NeonBlue,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // 5. The 7 Canonical Habit Items
                        items(CanonicalHabitsList.items, key = { it.key }) { habit ->
                            HabitItemCard(
                                habit = habit,
                                isChecked = state.habitChecks[habit.key] ?: false,
                                note = state.habitNotes[habit.key] ?: "",
                                isExpanded = state.expandedNoteKey == habit.key,
                                isParent = state.isParent,
                                onToggle = { viewModel.toggleHabit(habit.key) },
                                onToggleExpand = { viewModel.toggleNoteExpanded(habit.key) },
                                onNoteChange = { note -> viewModel.setNote(habit.key, note) },
                                isDark = isDark
                            )
                        }

                        // Spacer to prevent content cut by bottom button
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
            shape = RoundedCornerShape(20.dp),
            containerColor = if (isDark) CosmicNavy else Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = ParentNeon, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Verifikasi Jurnal Karakter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Sebagai orang tua/wali dari ${state.childName}, verifikasi bahwa ananda telah membiasakan ${state.completedCount} dari 7 karakter hebat hari ini.",
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    )

                    OutlinedTextField(
                        value = state.parentFeedbackInput,
                        onValueChange = viewModel::setParentFeedbackInput,
                        label = { Text("Catatan / Apresiasi untuk Ananda (Opsional)") },
                        placeholder = { Text("Contoh: Hebat nak, pertahankan bangun pagi & sholat berjamaah!") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::submitParentVerification,
                    colors = ButtonDefaults.buttonColors(containerColor = ParentNeon)
                ) {
                    Text("Simpan Verifikasi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeParentVerifyDialog) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun HabitsOverviewCard(
    state: HabitsUiState,
    isDark: Boolean,
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E3A8A), // Pure Royal Blue
            Color(0xFF0F172A),
            Color(0xFF064E3B)  // Subtle Emerald
        )
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, if (isDark) GlassOverlay else Color(0xFFE2E8F0))
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Top Tag & Streak
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x33FFFFFF)
                    ) {
                        Text(
                            text = "G7KAIH • KURIKULUM MERDEKA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF93C5FD),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (state.streakDays > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x33F59E0B)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔥", fontSize = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${state.streakDays} Hari Beruntun",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                        }
                    }
                }

                // Title & Subtitle
                Column {
                    Text(
                        text = if (state.isParent) "Jurnal Karakter ${state.childName}" else "7 Kebiasaan Anak Indonesia Hebat",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = getFormattedTodayDate(),
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }

                // Progress Bar & Percentage
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tingkat Kepatuhan Pembiasaan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = "${state.completedCount} dari 7 Selesai (${state.complianceRate.toInt()}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.completedCount >= 5) Color(0xFF6EE7B7) else Color(0xFF93C5FD)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (state.completedCount.toFloat() / 7f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (state.completedCount >= 5) Color(0xFF10B981) else Color(0xFF3B82F6),
                        trackColor = Color(0x33FFFFFF),
                    )
                }

                // Status Verification Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val statusText = when (state.verificationStatus) {
                        "PARENT_VERIFIED" -> "Diverifikasi Orang Tua"
                        "TEACHER_OVERRIDE" -> "Supervisi Wali Kelas"
                        "PENDING" -> "Menunggu Verifikasi Ortu"
                        else -> "Belum Dicatat Hari Ini"
                    }
                    val statusColor = when (state.verificationStatus) {
                        "PARENT_VERIFIED" -> Color(0xFF10B981)
                        "TEACHER_OVERRIDE" -> Color(0xFF3B82F6)
                        "PENDING" -> Color(0xFFF59E0B)
                        else -> Color(0xFF94A3B8)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Semester Ganjil 2026/2027",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickTasksCard(
    onNavigateToAssignments: () -> Unit,
    isDark: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToAssignments),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NeonBlue.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Daftar Tugas & PR Siswa",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Pantau batas waktu tugas & kumpulkan tepat waktu",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ParentFeedbackCard(
    feedback: String,
    isDark: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF43F5E).copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFF43F5E),
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Catatan & Apresiasi Orang Tua / Wali",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFFF43F5E)
                )
                Text(
                    text = "\"$feedback\"",
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun HabitItemCard(
    habit: CanonicalHabit,
    isChecked: Boolean,
    note: String,
    isExpanded: Boolean,
    isParent: Boolean,
    onToggle: () -> Unit,
    onToggleExpand: () -> Unit,
    onNoteChange: (String) -> Unit,
    isDark: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (!isParent) Modifier.clickable(onClick = onToggle) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (isChecked) {
            if (isDark) Color(0xFF064E3B).copy(alpha = 0.25f) else Color(0xFFECFDF5)
        } else {
            if (isDark) Color(0xFF0F172A) else Color.White
        },
        border = BorderStroke(
            1.dp,
            if (isChecked) {
                if (isDark) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF6EE7B7)
            } else {
                if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Number Circle Badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isChecked) Color(0xFF10B981) else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            text = habit.number.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Title and Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = habit.subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isChecked) Color(0xFF10B981) else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }

                // Checkbox / Toggle for Student, or Status for Parent
                if (!isParent) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF10B981),
                            checkmarkColor = Color.White
                        )
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isChecked) Color(0x3310B981) else Color(0x2294A3B8)
                    ) {
                        Text(
                            text = if (isChecked) "Terlaksana ✓" else "Belum",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isChecked) Color(0xFF10B981) else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Description
            Spacer(Modifier.height(8.dp))
            Text(
                text = habit.description,
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                lineHeight = 16.sp
            )

            // Existing note or toggle note button
            if (note.isNotBlank() && !isExpanded) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleExpand)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Catatan: $note",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                            maxLines = 2
                        )
                    }
                }
            } else if (!isParent && !isExpanded) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "+ Tambah Catatan Pembiasaan",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonBlue,
                    modifier = Modifier
                        .clickable(onClick = onToggleExpand)
                        .padding(vertical = 2.dp)
                )
            }

            // Expanded Note Input Field
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        label = { Text("Catatan Pelaksanaan ${habit.title}") },
                        placeholder = { Text("Contoh: ${habit.exampleNote}") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onToggleExpand) {
                            Text("Selesai", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun getFormattedTodayDate(): String {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
    return today.format(formatter)
}
