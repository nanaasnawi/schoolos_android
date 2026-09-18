package com.schoolos.android.feature.learning.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.MaterialStudentCompletion

@Composable
fun TeacherCompletionsTrackerCard(
    completions: List<MaterialStudentCompletion>,
    isLoading: Boolean,
    className: String? = null,
    onOpenFullSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalStudents = completions.size
    val completedStudents = completions.count { it.isCompleted }
    val readingOnlyStudents = completions.count { !it.isCompleted && (it.currentPage ?: 0) > 0 }
    val notReadStudents = totalStudents - completedStudents - readingOnlyStudents

    val progressRatio = if (totalStudents > 0) completedStudents.toFloat() / totalStudents.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progressRatio, label = "progressRatio")

    var selectedFilter by remember { mutableStateOf("Semua") }
    var searchQuery by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }

    val filteredList = remember(completions, selectedFilter, searchQuery) {
        completions.filter { student ->
            val matchesFilter = when (selectedFilter) {
                "Selesai" -> student.isCompleted
                "Membaca" -> !student.isCompleted && (student.currentPage ?: 0) > 0
                "Belum" -> !student.isCompleted && (student.currentPage ?: 0) == 0
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                student.studentName.contains(searchQuery, ignoreCase = true) ||
                        (student.nisn ?: "").contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TeacherNeon.copy(alpha = 0.16f))
                            .border(1.dp, TeacherNeon.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = TeacherNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Monitoring Keterbacaan Siswa",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (!className.isNullOrBlank()) "Rombel " else "Daftar Siswa Kelas",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (completedStudents > 0) NeonSuccess.copy(alpha = 0.12f) else CosmicDark)
                        .border(1.dp, if (completedStudents > 0) NeonSuccess.copy(alpha = 0.35f) else GlassBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = " /  Selesai",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (completedStudents > 0) NeonSuccess else TextSecondary
                    )
                }
            }

            // Progress Bar & Percentage
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Persentase Penuntasan Modul",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                    Text(
                        text = "%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = TeacherNeon
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CosmicDark)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(TeacherNeon, NeonBlue)
                                )
                            )
                    )
                }
            }

            // Segmented Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Semua" to totalStudents,
                    "Selesai" to completedStudents,
                    "Membaca" to readingOnlyStudents,
                    "Belum" to notReadStudents
                ).forEach { (tab, count) ->
                    val isSelected = selectedFilter == tab
                    val tabColor = when (tab) {
                        "Selesai" -> NeonSuccess
                        "Membaca" -> NeonBlue
                        "Belum" -> TextSecondary
                        else -> TeacherNeon
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) tabColor.copy(alpha = 0.16f) else CosmicDark)
                            .border(1.dp, if (isSelected) tabColor.copy(alpha = 0.5f) else GlassBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedFilter = tab }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = " ()",
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) tabColor else TextTertiary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Student list preview or content
            when {
                isLoading && completions.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = TeacherNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Memuat data pembaca materi...", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
                completions.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicDark)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada riwayat aktivitas baca dari siswa untuk modul ini.",
                            fontSize = 11.sp,
                            color = TextTertiary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                filteredList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tidak ada siswa pada kategori ini.", fontSize = 11.sp, color = TextTertiary)
                    }
                }
                else -> {
                    val displayedList = if (isExpanded) filteredList else filteredList.take(4)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        displayedList.forEach { student ->
                            StudentCompletionRow(student = student)
                        }
                    }

                    // Toggle Expand or Open Sheet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (filteredList.size > 4) {
                            TextButton(
                                onClick = { isExpanded = !isExpanded },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isExpanded) "Sembunyikan Sebagian" else "Tampilkan Semua ( Siswa)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TeacherNeon
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = TeacherNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        TextButton(
                            onClick = onOpenFullSheet,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Roster Lengkap ?",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCompletionRow(
    student: MaterialStudentCompletion,
    modifier: Modifier = Modifier,
) {
    val isFemale = student.gender?.trim()?.uppercase() == "P"
    val genderColor = if (isFemale) StudentNeon else NeonBlue
    val avatarGradient = if (isFemale)
        Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFA855F7)))
    else
        Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicDark)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
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
                // Avatar
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(avatarGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.studentName.firstOrNull()?.toString()?.uppercase() ?: "?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Text(
                        text = student.studentName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "NISN: ",
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Status Badge
            when {
                student.isCompleted -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonSuccess.copy(alpha = 0.15f))
                            .border(1.dp, NeonSuccess.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = NeonSuccess, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Selesai", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonSuccess)
                        }
                    }
                }
                (student.currentPage ?: 0) > 0 -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonBlue.copy(alpha = 0.15f))
                            .border(1.dp, NeonBlue.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = NeonBlue, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Hal. ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonBlue)
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicNavy)
                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Belum Baca", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextTertiary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialCompletionsBottomSheet(
    completions: List<MaterialStudentCompletion>,
    onDismiss: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") }

    val total = completions.size
    val completed = completions.count { it.isCompleted }
    val reading = completions.count { !it.isCompleted && (it.currentPage ?: 0) > 0 }
    val notRead = total - completed - reading

    val filteredList = remember(completions, selectedFilter, searchQuery) {
        completions.filter { s ->
            val matchesFilter = when (selectedFilter) {
                "Selesai" -> s.isCompleted
                "Membaca" -> !s.isCompleted && (s.currentPage ?: 0) > 0
                "Belum" -> !s.isCompleted && (s.currentPage ?: 0) == 0
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                s.studentName.contains(searchQuery, ignoreCase = true) ||
                        (s.nisn ?: "").contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CosmicNavy,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GlassBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daftar Siswa & Keterbacaan Materi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = " dari  siswa telah menuntaskan materi ini",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextTertiary, modifier = Modifier.size(18.dp))
                }
            }

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama murid atau NISN...", fontSize = 12.sp, color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextTertiary, modifier = Modifier.size(16.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CosmicDark,
                    unfocusedContainerColor = CosmicDark,
                    focusedBorderColor = TeacherNeon,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Semua" to total,
                    "Selesai" to completed,
                    "Membaca" to reading,
                    "Belum" to notRead
                ).forEach { (tab, count) ->
                    val isSelected = selectedFilter == tab
                    val tabColor = when (tab) {
                        "Selesai" -> NeonSuccess
                        "Membaca" -> NeonBlue
                        "Belum" -> TextSecondary
                        else -> TeacherNeon
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) tabColor.copy(alpha = 0.16f) else CosmicDark)
                            .border(1.dp, if (isSelected) tabColor.copy(alpha = 0.5f) else GlassBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedFilter = tab }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = " ()",
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) tabColor else TextTertiary,
                            maxLines = 1
                        )
                    }
                }
            }

            // List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.studentId }) { student ->
                    StudentCompletionRow(student = student)
                }
                if (filteredList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tidak ditemukan siswa yang cocok.", fontSize = 12.sp, color = TextTertiary)
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
