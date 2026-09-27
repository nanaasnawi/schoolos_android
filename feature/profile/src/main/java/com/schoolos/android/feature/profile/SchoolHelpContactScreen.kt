package com.schoolos.android.feature.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.R as CoreR
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicDark
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.DynamicSchoolLogo
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

@Composable
fun SchoolHelpContactScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var expandedFaqIndex by remember { mutableIntStateOf(-1) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportText by remember { mutableStateOf("") }

    val faqs = remember {
        listOf(
            "Bagaimana jika saya lupa kata sandi akun?" to
                "Hubungi staf Tata Usaha atau Wali Kelas untuk permintaan reset kata sandi sementara, lalu segera perbarui kata sandi melalui menu Keamanan & Kata Sandi.",
            "Kapan tugas yang diunggah akan dinilai oleh guru?" to
                "Guru mata pelajaran biasanya memeriksa dan memasukkan nilai dalam 1–3 hari kerja setelah batas waktu pengumpulan tugas berakhir.",
            "Bagaimana cara mengajukan izin ketidakhadiran?" to
                "Orang tua atau wali murid dapat mengunggah surat izin sakit atau keperluan keluarga melalui fitur absensi atau menghubungi wali kelas.",
            "Apa yang harus dilakukan jika kuis CBT mengalami kendala koneksi?" to
                "Sistem menyimpan jawaban Anda secara berkala ke database lokal. Jangan tutup aplikasi — periksa koneksi internet Anda atau hubungi proktor ujian.",
        )
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Bantuan & Kontak",
                subtitle = "Layanan Institusi & Pusat Kendala",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 1. HERO INSTITUTIONAL IDENTITY CARD ────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CosmicDark)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                DynamicSchoolLogo(
                                    logoUrl = state.schoolLogoUrl,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    fallback = {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = "Logo Sekolah",
                                            tint = NeonBlue,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PUSAT BANTUAN INSTITUSI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                    letterSpacing = 0.8.sp,
                                )
                                Text(
                                    text = state.schoolName.ifBlank { "Layanan Sekolah Terdaftar" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonSuccess.copy(alpha = 0.14f))
                                .border(1.dp, NeonSuccess.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "● LAYANAN AKTIF",
                                color = NeonSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }

                    Text(
                        text = "Pusat komunikasi resmi untuk kebutuhan akademik, administrasi tata usaha, dan bimbingan siswa maupun wali murid.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp,
                    )

                    val hasNpsn = !state.schoolNpsn.isNullOrBlank()
                    val hasAccreditation = !state.schoolAccreditation.isNullOrBlank()

                    if (hasNpsn || hasAccreditation) {
                        HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (hasNpsn) {
                                ContactMetaPill(
                                    label = "NPSN Sekolah",
                                    value = state.schoolNpsn!!,
                                    accentColor = NeonBlue,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (hasAccreditation) {
                                ContactMetaPill(
                                    label = "Akreditasi",
                                    value = "Peringkat ${state.schoolAccreditation!!}",
                                    accentColor = NeonSuccess,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            // ── 2. DYNAMIC CONTACT ACTION GRID ───────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "SALURAN KOMUNIKASI RESMI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary,
                        letterSpacing = 0.8.sp,
                    )

                    if (state.schoolContactLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = NeonBlue,
                                strokeWidth = 2.dp,
                            )
                        }
                    } else {
                        val hasPhone = !state.schoolPhone.isNullOrBlank()
                        val hasEmail = !state.schoolEmail.isNullOrBlank()
                        val hasWebsite = !state.schoolWebsite.isNullOrBlank()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Telepon TU
                            ContactActionCard(
                                icon = Icons.Default.Phone,
                                label = "Telepon TU",
                                value = if (hasPhone) state.schoolPhone!! else "Belum diatur",
                                accentColor = NeonBlue,
                                isAvailable = hasPhone,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (hasPhone) {
                                        val clean = state.schoolPhone!!.filter { it.isDigit() || it == '+' }
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")))
                                    } else {
                                        Toast.makeText(context, "Nomor telepon belum diatur oleh pihak sekolah.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )

                            // WhatsApp Resmi
                            ContactActionCard(
                                icon = Icons.AutoMirrored.Filled.Chat,
                                label = "WhatsApp",
                                value = if (hasPhone) "Chat Resmi" else "Belum diatur",
                                accentColor = NeonSuccess,
                                isAvailable = hasPhone,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (hasPhone) {
                                        val clean = state.schoolPhone!!.filter { it.isDigit() }
                                        val waNumber = if (clean.startsWith("0")) "62${clean.drop(1)}" else clean
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waNumber")))
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Aplikasi WhatsApp tidak terpasang di perangkat.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Nomor WhatsApp belum diatur oleh pihak sekolah.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Email Resmi
                            ContactActionCard(
                                icon = Icons.Default.Email,
                                label = "Email Resmi",
                                value = if (hasEmail) state.schoolEmail!!.substringBefore("@") else "Belum diatur",
                                accentColor = StudentNeon,
                                isAvailable = hasEmail,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (hasEmail) {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:${state.schoolEmail}")
                                            putExtra(Intent.EXTRA_SUBJECT, "Pertanyaan Layanan Akademik - SchoolOS")
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Tidak ada aplikasi email yang tersedia.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Alamat email resmi belum diatur oleh sekolah.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )

                            // Website Resmi
                            ContactActionCard(
                                icon = Icons.Default.Language,
                                label = "Portal Web",
                                value = if (hasWebsite) "Kunjungi Web" else "Belum diatur",
                                accentColor = NeonWarning,
                                isAvailable = hasWebsite,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (hasWebsite) {
                                        val url = if (state.schoolWebsite!!.startsWith("http")) {
                                            state.schoolWebsite!!
                                        } else {
                                            "https://${state.schoolWebsite}"
                                        }
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Gagal membuka peramban web.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Alamat website resmi belum diatur oleh sekolah.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // ── 3. SCHOOL LOCATION & ADDRESS (IF CONFIGURED) ──────────
            if (!state.schoolAddress.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicNavy)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(18.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "ALAMAT INSTITUSI SEKOLAH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary,
                                letterSpacing = 0.8.sp,
                            )
                        }
                        Text(
                            text = state.schoolAddress!!,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            lineHeight = 19.sp,
                        )
                    }
                }
            }

            // ── 4. FAQ ACCORDION CARD ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "PERTANYAAN UMUM (FAQ)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary,
                        letterSpacing = 0.8.sp,
                    )

                    faqs.forEachIndexed { index, (question, answer) ->
                        val isExpanded = expandedFaqIndex == index
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isExpanded) CosmicDark else Color.Transparent)
                                .clickable { expandedFaqIndex = if (isExpanded) -1 else index }
                                .padding(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isExpanded) NeonBlue.copy(alpha = 0.15f) else CosmicDark)
                                            .border(
                                                1.dp,
                                                if (isExpanded) NeonBlue.copy(alpha = 0.35f) else GlassBorder,
                                                RoundedCornerShape(6.dp),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Q",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isExpanded) NeonBlue else TextTertiary,
                                        )
                                    }
                                    Text(
                                        text = question,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isExpanded) NeonBlue else TextPrimary,
                                        lineHeight = 18.sp,
                                    )
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                Column {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = answer,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(start = 32.dp),
                                    )
                                }
                            }
                        }

                        if (index < faqs.size - 1) {
                            HorizontalDivider(
                                color = GlassBorder.copy(alpha = 0.6f),
                                thickness = 0.5.dp,
                            )
                        }
                    }
                }
            }

            // ── 5. LAPORKAN KENDALA TEKNIS CARD ──────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(1.dp, NeonError.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable { showReportDialog = true }
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonError.copy(alpha = 0.12f))
                                .border(1.dp, NeonError.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = NeonError,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Laporkan Kendala Sistem",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Kirim laporan bug atau masalah sinkronisasi data",
                                fontSize = 11.sp,
                                color = TextSecondary,
                            )
                        }
                    }

                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Report Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = CosmicNavy,
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonError.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.BugReport, null, tint = NeonError, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "Laporkan Kendala",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp,
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tuliskan deskripsi kendala teknis yang Anda temui agar tim IT sekolah dapat menindaklanjuti:",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp,
                    )
                    OutlinedTextField(
                        value = reportText,
                        onValueChange = { reportText = it },
                        placeholder = { Text("Contoh: Halaman tugas tidak memuat lampiran dokumen...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = CosmicDark,
                            unfocusedContainerColor = CosmicDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedPlaceholderColor = TextTertiary,
                            unfocusedPlaceholderColor = TextTertiary,
                        ),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportText.isNotBlank()) {
                            Toast.makeText(context, "Laporan kendala telah diteruskan ke tim IT sekolah.", Toast.LENGTH_LONG).show()
                            reportText = ""
                            showReportDialog = false
                        } else {
                            Toast.makeText(context, "Harap isi deskripsi kendala sebelum mengirim.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Kirim Laporan", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Batal", color = TextTertiary)
                }
            },
        )
    }
}

// ── SUBCOMPONENTS ─────────────────────────────────────────────────────────────

@Composable
private fun ContactActionCard(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    isAvailable: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicDark)
            .border(
                1.dp,
                if (isAvailable) accentColor.copy(alpha = 0.25f) else GlassBorder,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isAvailable) accentColor.copy(alpha = 0.12f) else GlassBorder.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        if (isAvailable) accentColor.copy(alpha = 0.25f) else GlassBorder,
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isAvailable) accentColor else TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isAvailable) TextPrimary else TextTertiary,
                    maxLines = 1,
                )
                Text(
                    text = value,
                    fontSize = 11.sp,
                    color = if (isAvailable) accentColor else TextTertiary,
                    fontWeight = if (isAvailable) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ContactMetaPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicDark)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }
    }
}
