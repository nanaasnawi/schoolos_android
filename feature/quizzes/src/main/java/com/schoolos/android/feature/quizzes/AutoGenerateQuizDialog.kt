package com.schoolos.android.feature.quizzes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
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
import com.schoolos.android.core.designsystem.AiGenerationLoadingOverlay
import com.schoolos.android.core.designsystem.AiGenerationType
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoGenerateQuizDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    availableSubjects: List<AcademicSubject>,
    availableClasses: List<AcademicClass>,
    currentSubjectName: String,
    currentClassName: String,
    onGenerate: (
        type: String,
        subjectId: String,
        subjectName: String,
        classId: String?,
        className: String,
        topic: String,
        numQuestions: Int,
        difficulty: String
    ) -> Unit,
    isGenerating: Boolean = false,
) {
    if (!isOpen) return

    val colorScheme = MaterialTheme.colorScheme
    var selectedType by remember { mutableStateOf("QUIZ_MCQ_ONLY") }
    var customTopic by remember { mutableStateOf("") }
    var numQuestions by remember { mutableIntStateOf(5) }
    var selectedDifficulty by remember { mutableStateOf("Sedang") }

    var selectedSubject by remember(currentSubjectName, availableSubjects) {
        val matched = availableSubjects.find { it.name.equals(currentSubjectName, ignoreCase = true) }
        mutableStateOf(matched ?: availableSubjects.firstOrNull())
    }
    var selectedClass by remember(currentClassName, availableClasses) {
        val matched = availableClasses.find { it.name.equals(currentClassName, ignoreCase = true) }
        mutableStateOf(matched ?: availableClasses.firstOrNull())
    }

    var isSubjectDropdownOpen by remember { mutableStateOf(false) }
    var isClassDropdownOpen by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isGenerating) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
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
                    type = AiGenerationType.QUIZ,
                    subjectName = selectedSubject?.name ?: currentSubjectName,
                    topic = customTopic,
                    gradeLevel = selectedClass?.name ?: currentClassName,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                // Header
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(colorScheme.tertiary, colorScheme.primary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Sintesis Kuis (AI)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Penyusunan paket kuis CBT Kurikulum Merdeka",
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isGenerating) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = colorScheme.onSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Subject Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Mata Pelajaran (Mapel)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isGenerating) { isSubjectDropdownOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedSubject?.name ?: "Pilih Mapel",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedSubject != null) colorScheme.onSurface else colorScheme.onSurfaceVariant
                                )
                                Text(text = "▼", fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
                            }
                        }

                        DropdownMenu(
                            expanded = isSubjectDropdownOpen,
                            onDismissRequest = { isSubjectDropdownOpen = false },
                            modifier = Modifier.background(colorScheme.surface).border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp))
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

                // Custom Topic
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Topik / Materi Spesifik (Opsional)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = customTopic,
                        onValueChange = { customTopic = it },
                        placeholder = {
                            Text(
                                "Contoh: Hukum Newton (atau biarkan kosong)",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant,
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface
                        )
                    )
                }

                // Target Class Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Rombel / Kelas Target",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isGenerating) { isClassDropdownOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedClass?.name ?: "Pilih Rombel",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedClass != null) colorScheme.onSurface else colorScheme.onSurfaceVariant
                                )
                                Text(text = "▼", fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
                            }
                        }

                        DropdownMenu(
                            expanded = isClassDropdownOpen,
                            onDismissRequest = { isClassDropdownOpen = false },
                            modifier = Modifier.background(colorScheme.surface).border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        ) {
                            availableClasses.forEach { cls ->
                                DropdownMenuItem(
                                    text = { Text(cls.name, color = colorScheme.onSurface, fontSize = 13.sp) },
                                    onClick = {
                                        selectedClass = cls
                                        isClassDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Row: Jumlah Soal & Tingkat Kesulitan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Jumlah Soal
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Jumlah Soal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(5, 10, 15, 20).forEach { cnt ->
                                val isSel = numQuestions == cnt
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { numQuestions = cnt },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) Color(0xFF0284C7) else colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) Color(0xFF0369A1) else colorScheme.outlineVariant
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$cnt",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tingkat Kesulitan
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Tingkat Kesulitan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                        modifier = Modifier.padding(vertical = 8.dp),
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

                // Evaluation Scope & Format
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Format Soal Evaluasi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    QuizFormatOptionCard(
                        title = "Pilihan Ganda Saja (PG A-D)",
                        subtitle = "$numQuestions Soal PG dinamis dengan kunci jawaban acak",
                        isSelected = selectedType == "QUIZ_MCQ_ONLY",
                        onClick = { selectedType = "QUIZ_MCQ_ONLY" }
                    )

                    QuizFormatOptionCard(
                        title = "Kombinasi PG & Esai Analitis",
                        subtitle = "Kombinasi ${maxOf(2, numQuestions - 2)} PG + 2 Soal Esai HOTS dengan rubrik",
                        isSelected = selectedType == "QUIZ_MCQ_ESSAY",
                        onClick = { selectedType = "QUIZ_MCQ_ESSAY" }
                    )

                    QuizFormatOptionCard(
                        title = "Ujian Tengah / Akhir Semester",
                        subtitle = "$numQuestions Soal komprehensif (KKM 75, durasi 90 mnt)",
                        isSelected = selectedType == "EXAM_MONTHLY",
                        onClick = { selectedType = "EXAM_MONTHLY" }
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isGenerating,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
                    ) {
                        Text("Batal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val sub = selectedSubject ?: availableSubjects.firstOrNull()
                            val cls = selectedClass ?: availableClasses.firstOrNull()
                            if (sub != null) {
                                onGenerate(
                                    selectedType,
                                    sub.id,
                                    sub.name,
                                    cls?.id,
                                    cls?.name ?: "",
                                    customTopic.trim(),
                                    numQuestions,
                                    selectedDifficulty
                                )
                            }
                        },
                        enabled = !isGenerating && (selectedSubject != null || availableSubjects.isNotEmpty()),
                        modifier = Modifier.weight(1.5f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Menyusun...", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate AI", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun QuizFormatOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) colorScheme.primary else colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = if (isSelected) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Text(text = "✓", fontSize = 14.sp, color = colorScheme.primary, fontWeight = FontWeight.Black)
            }
        }
    }
}
