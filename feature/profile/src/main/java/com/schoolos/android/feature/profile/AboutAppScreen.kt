package com.schoolos.android.feature.profile

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import java.util.Calendar

@Composable
fun AboutAppScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var activeDialogDoc by remember { mutableStateOf<Pair<String, String>?>(null) }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Tentang Aplikasi",
                subtitle = "Sistem Operasi Digital & Lisensi",
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
            // ── 1. APP HERO BRANDING CARD ────────────────────────────
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
                                    .background(NeonBlue.copy(alpha = 0.12f))
                                    .border(1.dp, NeonBlue.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "SchoolOS Logo",
                                    tint = NeonBlue,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "SISTEM OPERASI PENDIDIKAN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                    letterSpacing = 0.8.sp,
                                )
                                Text(
                                    text = "SchoolOS Mobile",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonSuccess),
                                )
                                Text(
                                    text = "v${state.appVersion.ifBlank { "1.0.0" }}",
                                    color = NeonSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    Text(
                        text = "Platform terpadu untuk tata kelola sekolah, pembelajaran digital, ujian CBT, dan pemantauan akademik real-time dalam satu ekosistem institusi modern.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp,
                    )

                    // School License Badge
                    if (state.schoolName.isNotBlank()) {
                        HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CosmicDark)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    DynamicSchoolLogo(
                                        logoUrl = state.schoolLogoUrl,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        fallback = {
                                            Icon(
                                                imageVector = Icons.Default.School,
                                                contentDescription = null,
                                                tint = NeonBlue,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        },
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "TERLISENSI UNTUK INSTITUSI",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextTertiary,
                                        letterSpacing = 0.6.sp,
                                    )
                                    Text(
                                        text = state.schoolName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonBlue.copy(alpha = 0.12f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = "● RESMI",
                                    color = NeonBlue,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            // ── 2. TECH SPECIFICATIONS & ARCHITECTURE ─────────────────
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
                        text = "SPESIFIKASI & ARSITEKTUR TEKNOLOGI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary,
                        letterSpacing = 0.8.sp,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TechSpecCard(
                            icon = Icons.Default.Smartphone,
                            title = "Mobile Engine",
                            value = "Kotlin & Compose",
                            accentColor = NeonBlue,
                            modifier = Modifier.weight(1f),
                        )
                        TechSpecCard(
                            icon = Icons.Default.CloudQueue,
                            title = "Cloud Gateway",
                            value = "Rust & Axum",
                            accentColor = NeonSuccess,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TechSpecCard(
                            icon = Icons.Default.Lock,
                            title = "Protokol Transmisi",
                            value = "TLS 1.3 / SSE",
                            accentColor = StudentNeon,
                            modifier = Modifier.weight(1f),
                        )
                        TechSpecCard(
                            icon = Icons.Default.Storage,
                            title = "Penyimpanan Lokal",
                            value = "Room DB Encrypted",
                            accentColor = TeacherNeon,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── 3. CORE CAPABILITIES CARD ─────────────────────────────
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
                        text = "KAPABILITAS UTAMA SISTEM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary,
                        letterSpacing = 0.8.sp,
                    )

                    FeatureRow(
                        painter = painterResource(id = CoreR.drawable.ic_modern_bell),
                        accentColor = NeonBlue,
                        title = "Sinkronisasi Data Real-Time",
                        desc = "Agenda belajar, pengumuman darurat, dan nilai terupdate otomatis tanpa jeda manual.",
                    )

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    FeatureRow(
                        imageVector = Icons.Default.Security,
                        accentColor = NeonSuccess,
                        title = "Multi-Tenant Data Isolation",
                        desc = "Data tiap institusi sekolah terisolasi secara kriptografis demi keamanan informasi pendidikan.",
                    )

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    FeatureRow(
                        painter = painterResource(id = CoreR.drawable.ic_modern_tasks),
                        accentColor = StudentNeon,
                        title = "CBT & Asesmen Terintegrasi",
                        desc = "Mendukung pelaksanaan ujian sekolah, penyerahan tugas digital, serta rekapitulasi nilai otomatis.",
                    )

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    FeatureRow(
                        imageVector = Icons.Default.Palette,
                        accentColor = TeacherNeon,
                        title = "Antarmuka Material 3 Adaptif",
                        desc = "Tata letak konsisten dan optimal di berbagai rasio layar dengan dukungan tema terang dan gelap.",
                    )
                }
            }

            // ── 4. LEGAL & POLICIES CARD ─────────────────────────────
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
                        text = "DOKUMEN LEGAL & LISENSI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary,
                        letterSpacing = 0.8.sp,
                    )

                    PolicyRow(
                        title = "Kebijakan Privasi Data Pengguna",
                        onClick = {
                            activeDialogDoc = "Kebijakan Privasi Data" to
                                "SchoolOS memproses data profil, aktivitas akademik, dan hasil nilai secara aman sesuai dengan Undang-Undang Perlindungan Data Pribadi (UU PDP). Data Anda tidak diperjualbelikan kepada pihak ketiga manapun dan hanya dipergunakan untuk keperluan operasional sekolah tempat Anda terdaftar."
                        },
                    )

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    PolicyRow(
                        title = "Syarat & Ketentuan Layanan",
                        onClick = {
                            activeDialogDoc = "Syarat & Ketentuan" to
                                "Aplikasi SchoolOS disediakan untuk menunjang kegiatan belajar-mengajar. Pengguna dilarang menyalahgunakan akun, melakukan kecurangan selama ujian CBT, atau membocorkan materi pembelajaran berhak cipta ke luar lingkungan sekolah."
                        },
                    )

                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                    PolicyRow(
                        title = "Lisensi Perangkat Lunak & Open Source",
                        onClick = {
                            activeDialogDoc = "Lisensi Komponen Terbuka" to
                                "SchoolOS Android dibangun dengan komponen sumber terbuka berkualitas tinggi: Android Jetpack Compose, Kotlin Coroutines & Flow, Google Dagger-Hilt, Retrofit2 & OkHttp, Coil Image Loader, Room Persistence Library, serta Material 3 Components."
                        },
                    )
                }
            }

            // ── 5. COPYRIGHT & INSTITUTIONAL FOOTER ───────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "SchoolOS Educational Ecosystem",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                }
                Text(
                    text = "© ${Calendar.getInstance().get(Calendar.YEAR)} Seluruh hak cipta dilindungi undang-undang.",
                    fontSize = 11.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Document Preview Dialog
    activeDialogDoc?.let { (docTitle, docContent) ->
        AlertDialog(
            onDismissRequest = { activeDialogDoc = null },
            containerColor = CosmicNavy,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = docTitle,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp,
                )
            },
            text = {
                Text(
                    text = docContent,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = { activeDialogDoc = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
        )
    }
}

// ── SUBCOMPONENTS ─────────────────────────────────────────────────────────────

@Composable
private fun TechSpecCard(
    icon: ImageVector,
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicDark)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = title,
                fontSize = 10.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun FeatureRow(
    painter: Painter? = null,
    imageVector: ImageVector? = null,
    accentColor: Color,
    title: String,
    desc: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                )
            } else if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary,
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun PolicyRow(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = NeonBlue,
            modifier = Modifier.size(16.dp),
        )
    }
}
