package com.schoolos.android.feature.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Tentang Aplikasi",
                subtitle = "Informasi sistem & lisensi perangkat lunak",
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
            // ── 1. APP HERO ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(28.dp), spotColor = Color(0x352563EB))
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF4F46E5), Color(0xFF6D28D9))
                        )
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // App icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎓", fontSize = 42.sp)
                    }
                    // App name
                    Text(
                        "SchoolOS",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    // Version badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF6EE7B7)))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Versi ${state.appVersion.ifBlank { "1.0.0" }} • Stable Release",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Sistem Operasi Digital Terpadu untuk Tata Kelola & Pembelajaran Sekolah Modern",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                }
            }

            // ── 2. SCHOOL INSTITUTION BADGE ──────────────────────────
            if (state.schoolName.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CosmicNavy)
                        .border(1.dp, NeonBlue.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonBlue.copy(alpha = 0.10f))
                                .border(1.dp, NeonBlue.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏫", fontSize = 22.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "INSTITUSI TERDAFTAR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = TextTertiary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                state.schoolName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (!state.schoolNpsn.isNullOrBlank()) {
                                Text("NPSN: ${state.schoolNpsn}", fontSize = 11.sp, color = TextTertiary)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonSuccess.copy(alpha = 0.12f))
                                .border(1.dp, NeonSuccess.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("✓ AKTIF", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonSuccess)
                        }
                    }
                }
            }

            // ── 3. FEATURE HIGHLIGHTS ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "KEUNGGULAN SISTEM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = TextTertiary,
                        letterSpacing = 1.sp
                    )
                    AboutFeatureRow("⚡", "Sinkronisasi Real-time", "Pembaruan otomatis agenda, nilai, dan absensi tanpa perlu refresh manual.", NeonBlue)
                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                    AboutFeatureRow("🔐", "Keamanan Data Siswa", "Enkripsi sesi pengguna SHA-256 dan kepatuhan regulasi privasi data pendidikan.", NeonSuccess)
                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                    AboutFeatureRow("🎨", "UI Native Material 3", "Antarmuka modern Jetpack Compose yang mulus, responsif, dan adaptif.", StudentNeon)
                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                    AboutFeatureRow("☁️", "Backend Terdistribusi", "Infrastruktur cloud Rust yang cepat dan andal untuk ratusan sekolah.", TeacherNeon)
                }
            }

            // ── 4. TECH DIAGNOSTICS ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TechInfoCard("🤖", "Platform", "Android\nNative", modifier = Modifier.weight(1f))
                TechInfoCard("⚙️", "UI Toolkit", "Jetpack\nCompose", modifier = Modifier.weight(1f))
                TechInfoCard("🦀", "Backend", "Rust /\nAxum", modifier = Modifier.weight(1f))
                TechInfoCard("🗃️", "Database", "PostgreSQL\nTenanted", modifier = Modifier.weight(1f))
            }

            // ── 5. LEGAL & POLICY ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = GlassOverlay)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("LEGAL & KEBIJAKAN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextTertiary, letterSpacing = 1.sp)

                    PolicyLink("Kebijakan Privasi Data Pengguna", "https://schoolos.id/privacy", context)
                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                    PolicyLink("Syarat & Ketentuan Layanan", "https://schoolos.id/terms", context)
                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                    PolicyLink("Lisensi Open Source", "https://schoolos.id/licenses", context)
                }
            }

            // ── 6. COPYRIGHT ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🚀", fontSize = 22.sp)
                Text(
                    "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} SchoolOS Ecosystem",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Text(
                    "Dibangun dengan ❤️ untuk pendidikan Indonesia",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── LOCAL COMPONENTS ──────────────────────────────────────────────────────────

@Composable
private fun AboutFeatureRow(emoji: String, title: String, desc: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.10f))
                .border(1.dp, accent.copy(alpha = 0.20f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(desc, fontSize = 11.sp, color = TextTertiary, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun TechInfoCard(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 18.sp)
            Text(label, fontSize = 9.sp, color = TextTertiary, letterSpacing = 0.5.sp)
            Text(value, fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextPrimary, textAlign = TextAlign.Center, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun PolicyLink(title: String, url: String, context: android.content.Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeonBlue)
        Icon(Icons.Default.OpenInNew, null, tint = NeonBlue, modifier = Modifier.size(16.dp))
    }
}
