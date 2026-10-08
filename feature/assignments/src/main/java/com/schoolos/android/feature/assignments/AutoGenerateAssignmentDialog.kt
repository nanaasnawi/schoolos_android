package com.schoolos.android.feature.assignments

import androidx.compose.animation.AnimatedVisibility
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
import com.schoolos.android.domain.model.AcademicSubject

private val CosmicSurface2 = Color(0xFF1E293B)
private val CosmicNavy = Color(0xFF0F172A)
private val TeacherNeon = Color(0xFF3B82F6)
private val GlassBorder = Color(0xFF334155)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoGenerateAssignmentDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    availableSubjects: List<AcademicSubject>,
    currentSubjectName: String,
    onGenerate: (format: String, subjectId: String, subjectName: String) -> Unit,
    isGenerating: Boolean = false,
) {
    if (!isOpen) return

    val colorScheme = MaterialTheme.colorScheme
    var selectedFormat by remember { mutableStateOf("STRUCTURED_QUESTIONS") }
    var selectedSubject by remember(currentSubjectName, availableSubjects) {
        val matched = availableSubjects.find { it.name.equals(currentSubjectName, ignoreCase = true) }
        mutableStateOf(matched ?: availableSubjects.firstOrNull())
    }
    var isSubjectDropdownOpen by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isGenerating) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(20.dp),
            color = colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                        listOf(colorScheme.primary, colorScheme.tertiary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Sintesis Tugas AI",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Otomatisasi kurikulum mapel",
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isGenerating) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Subject Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Mata Pelajaran (Mapel)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isGenerating) {
                                    isSubjectDropdownOpen = true
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                    text = {
                                        Text(sub.name, color = colorScheme.onSurface, fontSize = 13.sp)
                                    },
                                    onClick = {
                                        selectedSubject = sub
                                        isSubjectDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Format Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Format Penugasan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FormatOptionCard(
                            modifier = Modifier.weight(1f),
                            title = "Soal Terstruktur",
                            subtitle = "Pilihan Ganda & Esai",
                            isSelected = selectedFormat == "STRUCTURED_QUESTIONS",
                            onClick = { if (!isGenerating) selectedFormat = "STRUCTURED_QUESTIONS" }
                        )

                        FormatOptionCard(
                            modifier = Modifier.weight(1f),
                            title = "Tugas Mandiri",
                            subtitle = "PR & Lembar Kerja",
                            isSelected = selectedFormat == "HOMEWORK_PR",
                            onClick = { if (!isGenerating) selectedFormat = "HOMEWORK_PR" }
                        )
                    }
                }

                // Info Callout
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.secondary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔒", fontSize = 14.sp)
                        Text(
                            text = "100% Terisolasi: Soal dirancang ketat hanya dari materi mata pelajaran terpilih tanpa resiko kebocoran antar rombel.",
                            fontSize = 11.sp,
                            color = colorScheme.onSecondaryContainer,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isGenerating,
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
                                onGenerate(selectedFormat, sub.id, sub.name)
                            }
                        },
                        enabled = !isGenerating && (selectedSubject != null || availableSubjects.isNotEmpty()),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Menyusun Soal...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate Tugas", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatOptionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) colorScheme.primary else colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
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
    }
}
