package com.schoolos.android.feature.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicDark
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.CosmicSurface3
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassBorder2
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.ClassStudent

private fun formatClassName(raw: String?): String {
    if (raw.isNullOrBlank() || raw == "-") return "Kelas"
    val clean = raw.trim()
    return if (clean.startsWith("Kelas", ignoreCase = true) ||
        clean.startsWith("Paket", ignoreCase = true) ||
        clean.startsWith("Ruang", ignoreCase = true)
    ) {
        clean
    } else {
        "Kelas $clean"
    }
}

@Composable
fun RombelStudentsScreen(
    className: String? = null,
    onBack: () -> Unit,
    onNavigateToStudentDetail: (ClassStudent) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RombelStudentsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(className) {
        if (!className.isNullOrBlank() && className != uiState.className) {
            viewModel.loadStudents(className)
        }
    }

    val students = uiState.allStudents
    val isLoading = uiState.isLoading
    val errorMessage = uiState.errorMessage
    val searchQuery = uiState.searchQuery
    val filteredStudents = uiState.filteredStudents
    val selectedFilter = uiState.selectedClassFilter
    val maleCount = uiState.maleCount
    val femaleCount = uiState.femaleCount

    val subtitleText = if (selectedFilter == "ALL" || selectedFilter.isBlank()) {
        if (filteredStudents.isNotEmpty()) "Semua Kelas Diampu • ${filteredStudents.size} Siswa ($maleCount L • $femaleCount P)"
        else "Semua Kelas Diampu"
    } else {
        if (filteredStudents.isNotEmpty()) "${formatClassName(selectedFilter)} • ${filteredStudents.size} Siswa ($maleCount L • $femaleCount P)"
        else formatClassName(selectedFilter)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack)
    ) {
        // Executive TopBar
        ExecutiveTopBar(
            title = "Rekap Siswa & Presensi",
            subtitle = subtitleText,
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { viewModel.loadStudents(selectedFilter) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmicNavy)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Muat Ulang",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        )

        // ── 1. FILTER TABS KELAS DIAMPU ───────────────────────────────────────
        if (uiState.availableClasses.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip "Semua Kelas"
                item {
                    val isAllSelected = selectedFilter == "ALL" || selectedFilter.isBlank()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isAllSelected) TeacherNeon.copy(alpha = 0.18f) else CosmicNavy)
                            .border(
                                width = if (isAllSelected) 1.dp else 0.5.dp,
                                color = if (isAllSelected) TeacherNeon else GlassBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.selectClassFilter("ALL") }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Semua Kelas (${students.size})",
                            fontSize = 11.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllSelected) TeacherNeon else TextSecondary
                        )
                    }
                }

                // Chips per kelas
                items(uiState.availableClasses) { cls ->
                    val isSelected = selectedFilter.equals(cls.name, ignoreCase = true)
                    val countForClass = students.count { it.className.equals(cls.name, ignoreCase = true) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonBlue.copy(alpha = 0.18f) else CosmicNavy)
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) NeonBlue else GlassBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.selectClassFilter(cls.name) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "${formatClassName(cls.name)} ($countForClass)",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) NeonBlue else TextSecondary
                        )
                    }
                }
            }
        }

        // ── 2. QUICK PRESENSI ACTION & SUMMARY BAR ───────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = CosmicNavy,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Counters
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AttendanceCountPill(label = "H", count = uiState.hadirCount, color = NeonSuccess)
                    AttendanceCountPill(label = "S", count = uiState.sakitCount, color = NeonBlue)
                    AttendanceCountPill(label = "I", count = uiState.izinCount, color = NeonWarning)
                    AttendanceCountPill(label = "A", count = uiState.alpaCount, color = NeonError)
                }

                // Right: Batch Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.markAllPresent() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonSuccess),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, NeonSuccess.copy(alpha = 0.6f)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hadir Semua", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.saveAttendance { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !uiState.isSavingAttendance && filteredStudents.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TeacherNeon, contentColor = Color.White),
                        modifier = Modifier.height(32.dp)
                    ) {
                        if (uiState.isSavingAttendance) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Simpan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── 3. MINIMALIST SEARCH BAR ─────────────────────────────────────────
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Cari nama, NISN, atau kelas...",
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CosmicNavy,
                    unfocusedContainerColor = CosmicNavy,
                    focusedBorderColor = NeonBlue.copy(alpha = 0.5f),
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
        }

        // ── 4. CONTENT ───────────────────────────────────────────────────────
        when {
            isLoading && students.isEmpty() -> {
                com.schoolos.android.core.designsystem.ShimmerList(
                    count = 6,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            errorMessage != null && students.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(NeonError.copy(alpha = 0.1f))
                                .border(0.5.dp, NeonError.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = NeonError,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Gagal Memuat Data",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = errorMessage,
                            fontSize = 12.sp,
                            color = TextTertiary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Surface(
                            onClick = { viewModel.loadStudents(selectedFilter) },
                            shape = RoundedCornerShape(8.dp),
                            color = CosmicNavy,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder)
                        ) {
                            Text(
                                text = "Coba Lagi",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
            filteredStudents.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CosmicNavy)
                                .border(0.5.dp, GlassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotBlank()) "Siswa Tidak Ditemukan" else "Belum Ada Siswa",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (searchQuery.isNotBlank())
                                "Tidak ada siswa yang cocok dengan \"$searchQuery\"."
                            else
                                "Belum ada siswa terdaftar di pilihan rombel ini.",
                            fontSize = 12.sp,
                            color = TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredStudents, key = { _, s -> s.id }) { index, student ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { visible = true }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(140 + index * 15)) +
                                    slideInVertically(tween(140 + index * 15)) { it / 4 }
                        ) {
                            val currentStatus = uiState.attendanceMap[student.id] ?: "HADIR"
                            StudentAttendanceCard(
                                student = student,
                                currentStatus = currentStatus,
                                onStatusChange = { newStatus ->
                                    viewModel.setStudentAttendance(student.id, newStatus)
                                },
                                onClick = { onNavigateToStudentDetail(student) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceCountPill(label: String, count: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.5.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$label: $count",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Modern interactive student row with class badge and quick attendance toggle (Hadir, Sakit, Izin, Alpa).
 */
@Composable
private fun StudentAttendanceCard(
    student: ClassStudent,
    currentStatus: String,
    onStatusChange: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFemale = student.gender?.trim()?.uppercase() == "P"
    val genderLabel = if (isFemale) "P" else "L"
    val initial = student.fullName.firstOrNull()?.toString()?.uppercase() ?: "?"

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CosmicNavy,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top Section: Info & Class Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CosmicSurface2)
                        .border(0.5.dp, GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Name & details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = student.fullName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Class Badge Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonBlue.copy(alpha = 0.14f))
                                .border(0.5.dp, NeonBlue.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formatClassName(student.className),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonBlue
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "NISN: ${student.nisn ?: "-"} • $genderLabel",
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                }

                Spacer(Modifier.width(6.dp))

                // Detail indicator
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Detail",
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Bottom Section: Quick Attendance Segmented Selector (H | S | I | A)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicDark)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AttendanceStatusOption(
                    label = "Hadir",
                    shortLabel = "H",
                    isSelected = currentStatus.equals("HADIR", ignoreCase = true),
                    activeColor = NeonSuccess,
                    onClick = { onStatusChange("HADIR") },
                    modifier = Modifier.weight(1f)
                )
                AttendanceStatusOption(
                    label = "Sakit",
                    shortLabel = "S",
                    isSelected = currentStatus.equals("SAKIT", ignoreCase = true),
                    activeColor = NeonBlue,
                    onClick = { onStatusChange("SAKIT") },
                    modifier = Modifier.weight(1f)
                )
                AttendanceStatusOption(
                    label = "Izin",
                    shortLabel = "I",
                    isSelected = currentStatus.equals("IZIN", ignoreCase = true),
                    activeColor = NeonWarning,
                    onClick = { onStatusChange("IZIN") },
                    modifier = Modifier.weight(1f)
                )
                AttendanceStatusOption(
                    label = "Alpa",
                    shortLabel = "A",
                    isSelected = currentStatus.equals("ALPA", ignoreCase = true),
                    activeColor = NeonError,
                    onClick = { onStatusChange("ALPA") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AttendanceStatusOption(
    label: String,
    shortLabel: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) activeColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = shortLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color.White else TextTertiary
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else TextTertiary
            )
        }
    }
}
