package com.schoolos.android.feature.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject

private val NvidiaGreen = Color(0xFF76B900)
private val NvidiaGradient = Brush.linearGradient(
    listOf(Color(0xFF76B900), Color(0xFF10B981))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoGenerateMaterialDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    availableSubjects: List<AcademicSubject>,
    availableClasses: List<AcademicClass>,
    currentSubjectName: String,
    currentClassName: String,
    onGenerate: (mode: String, subjectName: String, topic: String, gradeLevel: String) -> Unit,
    isGenerating: Boolean = false,
) {
    if (!isOpen) return

    val colorScheme = MaterialTheme.colorScheme
    var selectedMode by remember { mutableStateOf("INFOGRAPHIC") } // "INFOGRAPHIC" vs "ARTICLE"
    var topicInput by remember { mutableStateOf("") }

    var selectedSubject by remember(currentSubjectName, availableSubjects) {
        val matched = availableSubjects.find { it.name.equals(currentSubjectName, ignoreCase = true) }
        mutableStateOf(matched ?: availableSubjects.firstOrNull())
    }
    var customSubjectText by remember(currentSubjectName, availableSubjects) {
        val matched = availableSubjects.find { it.name.equals(currentSubjectName, ignoreCase = true) }
        val initial = currentSubjectName.ifBlank { matched?.name ?: "" }
        mutableStateOf(initial)
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
            color = colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant),
            shadowElevation = 16.dp
        ) {
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
                                .background(NvidiaGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Sintesis Materi AI",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = NvidiaGreen.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NvidiaGreen)
                                ) {
                                    Text(
                                        text = "NVIDIA NIM",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NvidiaGreen,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "NVIDIA NIM AI • Zero Local Mock",
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

                // Mode Format: Infografis vs Artikel
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Format Materi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !isGenerating) { selectedMode = "INFOGRAPHIC" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedMode == "INFOGRAPHIC") NvidiaGreen.copy(alpha = 0.15f) else colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (selectedMode == "INFOGRAPHIC") 1.5.dp else 1.dp,
                                if (selectedMode == "INFOGRAPHIC") NvidiaGreen else colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "🖼️ Infografis Majalah",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedMode == "INFOGRAPHIC") NvidiaGreen else colorScheme.onSurface
                                )
                                Text(
                                    text = "Visual dinamis, card & ilustrasi",
                                    fontSize = 10.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !isGenerating) { selectedMode = "ARTICLE" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedMode == "ARTICLE") NvidiaGreen.copy(alpha = 0.15f) else colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (selectedMode == "ARTICLE") 1.5.dp else 1.dp,
                                if (selectedMode == "ARTICLE") NvidiaGreen else colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "📰 Artikel Berita",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedMode == "ARTICLE") NvidiaGreen else colorScheme.onSurface
                                )
                                Text(
                                    text = "Sub-bab & fakta terstruktur",
                                    fontSize = 10.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Mapel Selection (Dukungan Bebas Semua Mata Pelajaran & Pilihan Cepat)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mata Pelajaran *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        if (availableSubjects.isNotEmpty()) {
                            Text(
                                text = "Ketik bebas / pilih daftar",
                                fontSize = 10.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customSubjectText,
                            onValueChange = { customSubjectText = it },
                            placeholder = {
                                Text(
                                    "Ketik mapel (misal: Bahasa Cirebon, PAI, IPA, Sejarah)...",
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isGenerating,
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                if (availableSubjects.isNotEmpty()) {
                                    IconButton(
                                        onClick = { isSubjectDropdownOpen = !isSubjectDropdownOpen },
                                        enabled = !isGenerating
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Pilih dari daftar mapel",
                                            tint = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NvidiaGreen,
                                unfocusedBorderColor = colorScheme.outlineVariant
                            )
                        )

                        DropdownMenu(
                            expanded = isSubjectDropdownOpen,
                            onDismissRequest = { isSubjectDropdownOpen = false }
                        ) {
                            availableSubjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.name, fontSize = 12.sp) },
                                    onClick = {
                                        customSubjectText = sub.name
                                        selectedSubject = sub
                                        isSubjectDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Rekomendasi Chip Mapel Cepat (jika ada daftar mapel)
                    if (availableSubjects.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableSubjects.take(8).forEach { sub ->
                                val isSelected = customSubjectText.equals(sub.name, ignoreCase = true)
                                Surface(
                                    modifier = Modifier.clickable(enabled = !isGenerating) {
                                        customSubjectText = sub.name
                                        selectedSubject = sub
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) NvidiaGreen.copy(alpha = 0.2f) else colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) NvidiaGreen else colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Text(
                                        text = sub.name,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NvidiaGreen else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Topik / Konsep Materi (Wajib)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Topik / Konsep Materi *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        placeholder = {
                            Text(
                                "Contoh: Siklus Air & Hidrologi, Planet & Tata Surya...",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGenerating,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NvidiaGreen,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        )
                    )
                }

                // Info Callout
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F3918).copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NvidiaGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚡", fontSize = 14.sp)
                        Text(
                            text = "NVIDIA NIM AI: Menyusun narasi, headline, callout dan visual ilustrasi materi siap ajar.",
                            fontSize = 11.sp,
                            color = NvidiaGreen,
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
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Batal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val subName = customSubjectText.trim().ifBlank {
                                selectedSubject?.name ?: currentSubjectName.ifBlank { "Umum" }
                            }
                            val clsName = selectedClass?.name ?: currentClassName.ifBlank { "Kelas 5 SD" }
                            onGenerate(selectedMode, subName, topicInput.trim(), clsName)
                        },
                        enabled = !isGenerating && topicInput.isNotBlank() && customSubjectText.isNotBlank(),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NvidiaGreen,
                            contentColor = Color.Black
                        )
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Sintesis NVIDIA NIM...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate (NVIDIA)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
