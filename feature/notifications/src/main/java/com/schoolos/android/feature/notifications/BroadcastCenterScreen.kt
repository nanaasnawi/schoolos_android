package com.schoolos.android.feature.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastCenterScreen(
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {},
    viewModel: BroadcastViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetStudents by remember { mutableStateOf(true) }
    var targetParents by remember { mutableStateOf(true) }
    var targetTeachers by remember { mutableStateOf(true) }

    val hasRecipient = targetStudents || targetParents || targetTeachers

    LaunchedEffect(state.success) {
        if (state.success) {
            onSuccess()
            viewModel.resetState()
        }
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Pusat Siaran Sekolah",
                subtitle = "Kirim pengumuman resmi ke seluruh civitas sekolah",
                onBack = onBack,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonBlue.copy(alpha = 0.15f))
                            .border(1.dp, NeonBlue.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Siaran Pengumuman Resmi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Pesan akan masuk ke notifikasi Android dan papan pengumuman.", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            // Title Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Judul Pengumuman", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Contoh: Jadwal Belajar Online Semester Ganjil", color = TextTertiary, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = CosmicSurface,
                        unfocusedContainerColor = CosmicSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Message Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Isi Pesan / Instruksi Lengkap", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Tuliskan seluruh rincian informasi, waktu, tautan materi, atau instruksi kegiatan di sini...", color = TextTertiary, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = CosmicSurface,
                        unfocusedContainerColor = CosmicSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Quick Target Presets
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Target Penerima Notifikasi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                // Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TargetPresetChip(
                        title = "Semua",
                        selected = targetStudents && targetParents && targetTeachers,
                        onClick = {
                            targetStudents = true
                            targetParents = true
                            targetTeachers = true
                        }
                    )
                    TargetPresetChip(
                        title = "Siswa & Guru",
                        selected = targetStudents && !targetParents && targetTeachers,
                        onClick = {
                            targetStudents = true
                            targetParents = false
                            targetTeachers = true
                        }
                    )
                    TargetPresetChip(
                        title = "Siswa Saja",
                        selected = targetStudents && !targetParents && !targetTeachers,
                        onClick = {
                            targetStudents = true
                            targetParents = false
                            targetTeachers = false
                        }
                    )
                    TargetPresetChip(
                        title = "Guru Saja",
                        selected = !targetStudents && !targetParents && targetTeachers,
                        onClick = {
                            targetStudents = false
                            targetParents = false
                            targetTeachers = true
                        }
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Toggle Selection Cards
                RecipientTargetCard(
                    title = "Siswa",
                    description = "Seluruh peserta didik / siswa yang terdaftar di sistem",
                    icon = Icons.Default.School,
                    accentColor = StudentNeon,
                    checked = targetStudents,
                    onCheckedChange = { targetStudents = it }
                )

                RecipientTargetCard(
                    title = "Dewan Guru",
                    description = "Bapak/Ibu dewan guru pengampu dan wali kelas",
                    icon = Icons.Default.Group,
                    accentColor = NeonBlue,
                    checked = targetTeachers,
                    onCheckedChange = { targetTeachers = it }
                )

                RecipientTargetCard(
                    title = "Wali Siswa / Orang Tua",
                    description = "Akun orang tua atau wali pendamping siswa",
                    icon = Icons.Default.People,
                    accentColor = NeonWarning,
                    checked = targetParents,
                    onCheckedChange = { targetParents = it }
                )

                // Target Summary Info Box
                if (hasRecipient) {
                    val summaryList = mutableListOf<String>()
                    if (targetStudents) summaryList.add("Siswa")
                    if (targetTeachers) summaryList.add("Dewan Guru")
                    if (targetParents) summaryList.add("Wali Siswa")
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonSuccess.copy(alpha = 0.08f))
                            .border(1.dp, NeonSuccess.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = NeonSuccess, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Penerima: ${summaryList.joinToString(" • ")}",
                                fontSize = 12.sp,
                                color = NeonSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonError.copy(alpha = 0.08f))
                            .border(1.dp, NeonError.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, null, tint = NeonError, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pilih minimal satu target penerima siaran", fontSize = 12.sp, color = NeonError, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.error != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    state.error ?: "",
                    color = NeonError,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = { viewModel.sendBroadcast(title, message, targetStudents, targetParents, targetTeachers) },
                enabled = title.isNotBlank() && message.isNotBlank() && hasRecipient && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonBlue,
                    disabledContainerColor = CosmicSurface2
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("KIRIM SIARAN PENGUMUMAN", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TargetPresetChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) NeonBlue.copy(alpha = 0.2f) else CosmicSurface)
            .border(
                0.8.dp,
                if (selected) NeonBlue else GlassBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) NeonBlue else TextSecondary
        )
    }
}

@Composable
private fun RecipientTargetCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) CosmicSurface else CosmicNavy)
            .border(
                0.8.dp,
                if (checked) accentColor.copy(alpha = 0.5f) else GlassBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = if (checked) 0.2f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) accentColor else TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (checked) TextPrimary else TextSecondary
                )
                Text(
                    description,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    lineHeight = 15.sp
                )
            }

            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = accentColor,
                    uncheckedColor = GlassBorder,
                    checkmarkColor = Color.White
                )
            )
        }
    }
}
