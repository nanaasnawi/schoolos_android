package com.schoolos.android.feature.assignments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.schoolos.android.core.designsystem.AiGenerationLoadingOverlay
import com.schoolos.android.core.designsystem.AiGenerationType
import com.schoolos.android.domain.model.AcademicSubject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoGenerateAssignmentDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    availableSubjects: List<AcademicSubject>,
    currentSubjectName: String,
    onGenerate: (
        format: String,
        subjectId: String,
        subjectName: String,
        topic: String,
        gradeLevel: String,
        difficulty: String,
        numMcq: Int,
        numEssay: Int,
        includeImages: Boolean,
    ) -> Unit,
    isGenerating: Boolean = false,
) {
    if (!isOpen) return

    val colorScheme = MaterialTheme.colorScheme

    val gradeOptions = remember {
        listOf(
            "Kelas 1 SD", "Kelas 2 SD", "Kelas 3 SD", "Kelas 4 SD", "Kelas 5 SD", "Kelas 6 SD",
            "Kelas 7 SMP", "Kelas 8 SMP", "Kelas 9 SMP",
            "Kelas 10 SMA", "Kelas 11 SMA", "Kelas 12 SMA",
            "Paket A (Setara SD)", "Paket B (Setara SMP)", "Paket C 12 (Setara SMA)"
        )
    }

    var selectedFormat by remember { mutableStateOf("STRUCTURED_QUESTIONS") } // "STRUCTURED_QUESTIONS" | "HOMEWORK_PR"
    var customTopic by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf("Kelas 5 SD") }
    var selectedDifficulty by remember { mutableStateOf("Sedang") } // "Mudah" | "Sedang" | "HOTS"
    var numMcq by remember { mutableIntStateOf(4) }
    var numEssay by remember { mutableIntStateOf(2) }
    var includeImages by remember { mutableStateOf(true) }

    var selectedSubject by remember(currentSubjectName, availableSubjects) {
        val matched = availableSubjects.find { it.name.equals(currentSubjectName, ignoreCase = true) }
        mutableStateOf(matched ?: availableSubjects.firstOrNull())
    }

    var isSubjectDropdownOpen by remember { mutableStateOf(false) }
    var isGradeDropdownOpen by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            color = if (isGenerating) Color(0xFF0A0F1D) else colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isGenerating) Color(0x3376B900) else colorScheme.outlineVariant
            ),
            shadowElevation = 16.dp
        ) {
            if (isGenerating) {
                AiGenerationLoadingOverlay(
                    isVisible = true,
                    type = AiGenerationType.ASSIGNMENT,
                    subjectName = selectedSubject?.name ?: currentSubjectName,
                    topic = customTopic,
                    gradeLevel = selectedGrade,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // ── Header ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF16A34A), Color(0xFF0284C7))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Generate Tugas Siswa Otomatis",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFDCFCE7),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
                                    ) {
                                        Text(
                                            text = "AI Kurikulum Merdeka",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Sintesis butir penugasan terstruktur & tugas mandiri berbasis Kurikulum Merdeka",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // ── Scrollable Form Body ──
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Mata Pelajaran (Mapel)
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = "Mata Pelajaran (Mapel) *Terkunci, isolasi penuh kurikulum",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isSubjectDropdownOpen = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 11.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedSubject?.name ?: "Pilih Mata Pelajaran",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selectedSubject != null) colorScheme.onSurface else colorScheme.onSurfaceVariant
                                        )
                                        Text(text = "▼", fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
                                    }
                                }

                                DropdownMenu(
                                    expanded = isSubjectDropdownOpen,
                                    onDismissRequest = { isSubjectDropdownOpen = false },
                                    modifier = Modifier
                                        .background(colorScheme.surface)
                                        .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                ) {
                                    availableSubjects.forEach { sub ->
                                        DropdownMenuItem(
                                            text = { Text(sub.name, color = colorScheme.onSurface, fontSize = 13.sp) },
                                            onClick = {
                                                selectedSubject = sub
                                                isSubjectDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Topik / Materi Penugasan Spesifik (Opsional)
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = "Topik / Materi Penugasan Spesifik (Opsional)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = customTopic,
                                onValueChange = { customTopic = it },
                                placeholder = {
                                    Text(
                                        "Contoh: Laporan Praktikum Pengukuran Besaran (atau biarkan kosong)",
                                        fontSize = 12.sp,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colorScheme.primary,
                                    unfocusedBorderColor = colorScheme.outlineVariant
                                )
                            )
                        }

                        // 3. Jenjang / Tingkat Kelas & Tingkat Kesulitan Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Jenjang / Tingkat Kelas
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "Jenjang / Tingkat Kelas",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isGradeDropdownOpen = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 11.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = selectedGrade,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(text = "▼", fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = isGradeDropdownOpen,
                                        onDismissRequest = { isGradeDropdownOpen = false },
                                        modifier = Modifier
                                            .background(colorScheme.surface)
                                            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    ) {
                                        gradeOptions.forEach { gr ->
                                            DropdownMenuItem(
                                                text = { Text(gr, color = colorScheme.onSurface, fontSize = 12.sp) },
                                                onClick = {
                                                    selectedGrade = gr
                                                    isGradeDropdownOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Tingkat Kesulitan
                            Column(
                                modifier = Modifier.weight(1.2f),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "Tingkat Kesulitan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Mudah", "Sedang", "HOTS").forEach { diff ->
                                        val isSel = selectedDifficulty == diff
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedDifficulty = diff },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) Color(0xFFDCFCE7) else colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSel) Color(0xFF16A34A) else colorScheme.outlineVariant
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = diff,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) Color(0xFF15803D) else colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Format Tugas yang Diinginkan
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Format Tugas yang Diinginkan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FormatOptionDetailedCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Opsi A: Soal Terstruktur (PG & Essay)",
                                    subtitle = "Kombinasi soal pilihan ganda otomatis dan uraian analitis dengan rubrik penilaian lengkap.",
                                    icon = Icons.Default.MenuBook,
                                    isSelected = selectedFormat == "STRUCTURED_QUESTIONS",
                                    onClick = { selectedFormat = "STRUCTURED_QUESTIONS" }
                                )

                                FormatOptionDetailedCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Opsi B: Tugas Mandiri / PR (Lembar Kerja)",
                                    subtitle = "Instruksi tugas mandiri, petunjuk unggah lembar kerja, & rubrik komprehensif.",
                                    icon = Icons.Default.Description,
                                    isSelected = selectedFormat == "HOMEWORK_PR",
                                    onClick = { selectedFormat = "HOMEWORK_PR" }
                                )
                            }
                        }

                        // 5. Dynamic Count Selectors (PG & Essay)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (selectedFormat == "STRUCTURED_QUESTIONS") {
                                    // Pilihan Ganda (PG)
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Jumlah Soal Pilihan Ganda (PG):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf(2, 4, 5, 10).forEach { cnt ->
                                                val isSel = numMcq == cnt
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { numMcq = cnt },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) Color(0xFF16A34A) else colorScheme.surface,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isSel) Color(0xFF15803D) else colorScheme.outlineVariant
                                                    )
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 7.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "$cnt PG",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSel) Color.White else colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }

                                            // Direct counter input box
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A)),
                                                modifier = Modifier.width(42.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 7.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$numMcq",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF16A34A)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Uraian / Essay
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Jumlah Soal Uraian / Essay:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf(1, 2, 3, 5).forEach { cnt ->
                                                val isSel = numEssay == cnt
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { numEssay = cnt },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) Color(0xFF7C3AED) else colorScheme.surface,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isSel) Color(0xFF6D28D9) else colorScheme.outlineVariant
                                                    )
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 7.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "$cnt Essay",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSel) Color.White else colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }

                                            // Direct counter box
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED)),
                                                modifier = Modifier.width(42.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 7.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$numEssay",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF7C3AED)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Tugas Mandiri (Butir Uraian / Tugas)
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Jumlah Butir Tugas Mandiri / Penugasan:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(1, 2, 3, 5).forEach { cnt ->
                                                val isSel = numEssay == cnt
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { numEssay = cnt },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) Color(0xFF0284C7) else colorScheme.surface,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isSel) Color(0xFF0369A1) else colorScheme.outlineVariant
                                                    )
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 7.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "$cnt Butir",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSel) Color.White else colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 6. Checkbox: Sertakan Soal Bergambar / Diagram
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (includeImages) Color(0xFFECFDF5) else colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (includeImages) Color(0xFFA7F3D0) else colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { includeImages = !includeImages }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = includeImages,
                                    onCheckedChange = { includeImages = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF059669)
                                    )
                                )
                                Column {
                                    Text(
                                        text = "Sertakan Soal Bergambar / Diagram",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (includeImages) Color(0xFF065F46) else colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Visual sains, diagram alur, grafik statistik, dan geometri terkurasi",
                                        fontSize = 10.sp,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // ── Action Buttons ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
                        ) {
                            Text("Batal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val sub = selectedSubject ?: availableSubjects.firstOrNull()
                                if (sub != null) {
                                    onGenerate(
                                        selectedFormat,
                                        sub.id,
                                        sub.name,
                                        customTopic.trim(),
                                        selectedGrade,
                                        selectedDifficulty,
                                        numMcq,
                                        numEssay,
                                        includeImages
                                    )
                                }
                            },
                            enabled = selectedSubject != null || availableSubjects.isNotEmpty(),
                            modifier = Modifier
                                .weight(2f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Generate Tugas Otomatis Sekarang",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatOptionDetailedCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFF0FDF4) else colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) Color(0xFF16A34A) else colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) Color(0xFF15803D) else colorScheme.onSurfaceVariant
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF15803D) else colorScheme.onSurface
                )
            }
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = colorScheme.onSurfaceVariant,
                lineHeight = 13.sp
            )
        }
    }
}
