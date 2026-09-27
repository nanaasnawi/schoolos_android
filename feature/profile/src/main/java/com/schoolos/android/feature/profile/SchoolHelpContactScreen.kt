package com.schoolos.android.feature.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
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
                "Hubungi staf Tata Usaha atau Wali Kelas untuk permintaan reset kata sandi sementara, lalu perbarui melalui menu Keamanan & Kata Sandi.",
            "Kapan tugas yang diunggah akan dinilai oleh guru?" to
                "Guru mata pelajaran biasanya memeriksa tugas dalam 1–3 hari kerja setelah tenggat waktu pengumpulan berakhir.",
            "Bagaimana cara mengajukan izin ketidakhadiran?" to
                "Orang tua/wali murid dapat menyampaikan surat izin sakit atau keperluan langsung kepada Wali Kelas atau melalui fitur pesan di aplikasi.",
            "Apa yang harus dilakukan jika kuis CBT mengalami kendala koneksi?" to
                "Sistem menyimpan jawaban otomatis setiap 3 detik. Jangan tutup aplikasi — cukup perbaiki koneksi atau hubungi proktor ruang ujian.",
        )
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Bantuan & Kontak Sekolah",
                subtitle = "Layanan akademik & dukungan teknis",
                onBack = onBack,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. HERO BANNER ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = Color(0x30F59E0B))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF78350F), Color(0xFFD97706), Color(0xFFF59E0B))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "PUSAT DUKUNGAN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.75f),
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    state.schoolName.ifBlank { "Layanan Sekolah" },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF6EE7B7)))
                                Spacer(Modifier.width(5.dp))
                                Text("SIAP MEMBANTU", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Text(
                        "Butuh bantuan terkait tugas, nilai, izin kehadiran, atau kendala akun? Tim Tata Usaha dan BK siap membantu Anda.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 17.sp
                    )
                }
            }

            // ── 2. CONTACT ACTION GRID ───────────────────────────────
            if (state.schoolContactLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicNavy.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NeonWarning, strokeWidth = 2.dp)
                }
            } else {
                val hasPhone   = !state.schoolPhone.isNullOrBlank()
                val hasEmail   = !state.schoolEmail.isNullOrBlank()
                val hasWebsite = !state.schoolWebsite.isNullOrBlank()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ContactActionCard(
                        emoji = "📞",
                        label = "Telepon TU",
                        value = if (hasPhone) state.schoolPhone!! else "Belum diatur",
                        accentColor = NeonBlue,
                        enabled = hasPhone,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val clean = state.schoolPhone!!.filter { it.isDigit() || it == '+' }
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")))
                        }
                    )
                    ContactActionCard(
                        emoji = "💬",
                        label = "WhatsApp",
                        value = if (hasPhone) "Chat Admin" else "Belum diatur",
                        accentColor = NeonSuccess,
                        enabled = hasPhone,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val clean = state.schoolPhone!!.filter { it.isDigit() }
                            val wa = if (clean.startsWith("0")) "62${clean.drop(1)}" else clean
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$wa")))
                            } catch (_: Exception) {
                                Toast.makeText(context, "WhatsApp tidak terpasang", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    ContactActionCard(
                        emoji = "✉️",
                        label = "Email",
                        value = if (hasEmail) state.schoolEmail!!.substringBefore("@") else "Belum diatur",
                        accentColor = StudentNeon,
                        enabled = hasEmail,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${state.schoolEmail}")
                                putExtra(Intent.EXTRA_SUBJECT, "Pertanyaan Akademik Siswa")
                            }
                            try { context.startActivity(intent) }
                            catch (_: Exception) { Toast.makeText(context, "Tidak ada aplikasi email", Toast.LENGTH_SHORT).show() }
                        }
                    )
                    ContactActionCard(
                        emoji = "🌐",
                        label = "Website",
                        value = if (hasWebsite) "Kunjungi" else "Belum diatur",
                        accentColor = NeonWarning,
                        enabled = hasWebsite,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val url = if (state.schoolWebsite!!.startsWith("http")) state.schoolWebsite!! else "https://${state.schoolWebsite}"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    )
                }
            }

            // ── 3. SCHOOL INFO CARD ──────────────────────────────────
            val hasAddress = !state.schoolAddress.isNullOrBlank()
            val hasNpsn    = !state.schoolNpsn.isNullOrBlank()
            val hasAccred  = !state.schoolAccreditation.isNullOrBlank()

            if (!state.schoolContactLoading && (hasAddress || hasNpsn || hasAccred)) {
                PremiumSectionCard(title = "INFORMASI SEKOLAH") {
                    if (hasNpsn) {
                        InfoRow("NPSN", state.schoolNpsn!!)
                        if (hasAccred || hasAddress) HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                    }
                    if (hasAccred) {
                        InfoRow("Akreditasi", state.schoolAccreditation!!)
                        if (hasAddress) HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                    }
                    if (hasAddress) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Alamat", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(86.dp))
                            Text(
                                state.schoolAddress!!,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // ── 4. FAQ ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        "PERTANYAAN UMUM (FAQ)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = TextTertiary,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    faqs.forEachIndexed { index, (question, answer) ->
                        val isExpanded = expandedFaqIndex == index
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isExpanded) CosmicDark.copy(alpha = 0.6f) else Color.Transparent)
                                .clickable { expandedFaqIndex = if (isExpanded) -1 else index }
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.Top, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 1.dp)
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isExpanded) NeonWarning.copy(alpha = 0.15f) else GlassBorder.copy(alpha = 0.08f))
                                            .border(1.dp, if (isExpanded) NeonWarning.copy(alpha = 0.3f) else GlassBorder, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Q", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isExpanded) NeonWarning else TextTertiary)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        question,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpanded) NeonWarning else TextPrimary,
                                        lineHeight = 18.sp
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.padding(start = 30.dp)) {
                                        Text(
                                            answer,
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                        if (index < faqs.size - 1) {
                            HorizontalDivider(
                                color = GlassBorder.copy(alpha = 0.5f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── 5. REPORT BUG ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NeonError.copy(alpha = 0.06f))
                    .border(1.dp, NeonError.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .clickable { showReportDialog = true }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonError.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.BugReport, null, tint = NeonError, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Laporkan Kendala Sistem", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeonError)
                        Text("Temukan bug atau masalah teknis? Beritahu kami", fontSize = 11.sp, color = TextTertiary)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = NeonError.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }

    // Report Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = CosmicNavy,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, null, tint = NeonError, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Laporkan Kendala", fontWeight = FontWeight.Black, color = TextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Jelaskan kendala teknis yang Anda alami:", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = reportText,
                        onValueChange = { reportText = it },
                        placeholder = { Text("Contoh: Halaman tugas tidak menampilkan berkas...") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportText.isNotBlank()) {
                            Toast.makeText(context, "Laporan telah diteruskan ke tim IT sekolah.", Toast.LENGTH_LONG).show()
                            reportText = ""; showReportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Kirim Laporan", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Batal", color = TextTertiary)
                }
            }
        )
    }
}

// ── LOCAL COMPONENTS ──────────────────────────────────────────────────────────

@Composable
private fun ContactActionCard(
    emoji: String,
    label: String,
    value: String,
    accentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = GlassOverlay)
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(1.dp, if (enabled) accentColor.copy(alpha = 0.25f) else GlassBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (enabled) accentColor.copy(alpha = 0.12f) else GlassBorder.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 18.sp)
            }
            Text(label, fontWeight = FontWeight.Black, fontSize = 11.sp, color = if (enabled) TextPrimary else TextTertiary, maxLines = 1)
            Text(value, fontSize = 9.sp, color = if (enabled) accentColor else TextTertiary, maxLines = 1, fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(86.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun PremiumSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay)
            .clip(RoundedCornerShape(20.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextTertiary, letterSpacing = 1.sp)
            content()
        }
    }
}
