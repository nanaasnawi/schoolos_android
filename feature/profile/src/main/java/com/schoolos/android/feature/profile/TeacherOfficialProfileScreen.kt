package com.schoolos.android.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.OfficialTeacherBiodata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherOfficialProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val biodata = state.officialTeacherBiodata
    val isLoading = state.officialTeacherBiodataLoading
    val errorMessage = state.officialTeacherBiodataError

    LaunchedEffect(Unit) {
        if (biodata == null) {
            viewModel.loadTeacherOfficialBiodata()
        }
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Biodata Pokok Resmi GTK",
                subtitle = "Pendidik & Tenaga Kependidikan Terdaftar",
                onBack = onBack,
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CosmicNavy)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                            .clickable { viewModel.loadTeacherOfficialBiodata() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang Data",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoading && biodata == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = TeacherNeon,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Memuat data GTK dari server...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            errorMessage != null && biodata == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NeonError.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = NeonError,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Gagal Memuat Profil GTK",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.loadTeacherOfficialBiodata() },
                            colors = ButtonDefaults.buttonColors(containerColor = TeacherNeon)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Coba Lagi", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            biodata != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Hero Card
                    item {
                        TeacherOfficialHeroCard(
                            biodata = biodata,
                            userAvatar = state.user?.avatarUrl ?: state.userAvatarUrl ?: biodata.fotoProfile
                        )
                    }

                    // 2. Data Pokok Kepegawaian & Identitas Resmi
                    item {
                        GtkSectionCard(
                            title = "Identitas Kepegawaian Pokok",
                            icon = Icons.Default.Badge,
                            accent = TeacherNeon
                        ) {
                            GtkDataRow("Nama Lengkap", biodata.fullName)
                            GtkDataRow("NUPTK", biodata.nuptk?.takeIf { it.isNotBlank() } ?: "Belum Terdaftar / —")
                            GtkDataRow("NIP", biodata.nip?.takeIf { it.isNotBlank() } ?: "—")
                            GtkDataRow("Jenis PTK", biodata.jenisPtk?.takeIf { it.isNotBlank() } ?: "Guru / Tutor")
                            GtkDataRow("Status Kepegawaian", biodata.statusKepegawaian?.takeIf { it.isNotBlank() } ?: "Pendidik & Tenaga Kependidikan")
                            GtkDataRow("Jenis Kelamin", when (biodata.jk?.trim()?.uppercase()) {
                                "L", "LAKI-LAKI" -> "Laki-laki"
                                "P", "PEREMPUAN" -> "Perempuan"
                                else -> biodata.jk?.takeIf { it.isNotBlank() } ?: "—"
                            })
                            val ttl = listOfNotNull(
                                biodata.tempatLahir?.takeIf { it.isNotBlank() },
                                biodata.tanggalLahir?.takeIf { it.isNotBlank() }
                            ).joinToString(", ")
                            GtkDataRow("Tempat, Tanggal Lahir", ttl.ifBlank { "—" })
                            GtkDataRow("Agama", biodata.agama?.takeIf { it.isNotBlank() } ?: "—")
                        }
                    }

                    // 3. Penugasan & Pembelajaran
                    item {
                        GtkSectionCard(
                            title = "Penugasan & Pembelajaran",
                            icon = Icons.Default.School,
                            accent = NeonBlue
                        ) {
                            val mapel = biodata.subject?.takeIf { it.isNotBlank() } ?: "Semua Mata Pelajaran / Tematik"
                            GtkDataRow("Mata Pelajaran Diampu", mapel)
                            val rombel = biodata.classesAssigned?.takeIf { it.isNotBlank() }
                                ?: state.className.takeIf { it.isNotBlank() } ?: "Semua Rombongan Belajar"
                            GtkDataRow("Rombel Binaan / Pengampu", rombel)
                            GtkDataRow("Satuan Pendidikan", state.schoolName.ifBlank { "Lembaga Pendidikan Aktif" })
                            val npsn = state.schoolNpsn
                            if (!npsn.isNullOrBlank()) {
                                GtkDataRow("NPSN Sekolah", npsn)
                            }
                            GtkDataRow("Status Penugasan", "Pendidik Induk / Aktif Mengajar")
                        }
                    }

                    // 4. Domisili Terdaftar
                    item {
                        GtkSectionCard(
                            title = "Domisili Terdaftar",
                            icon = Icons.Default.Home,
                            accent = NeonInfo
                        ) {
                            GtkDataRow("Alamat Jalan", biodata.alamatJalan?.takeIf { it.isNotBlank() } ?: "—")
                            val rtrw = if (!biodata.rt.isNullOrBlank() || !biodata.rw.isNullOrBlank()) {
                                "RT ${biodata.rt ?: "00"} / RW ${biodata.rw ?: "00"}"
                            } else "—"
                            GtkDataRow("RT / RW", rtrw)
                            GtkDataRow("Dusun / Lingkungan", biodata.dusun?.takeIf { it.isNotBlank() } ?: "—")
                            GtkDataRow("Desa / Kelurahan", biodata.kelurahan?.takeIf { it.isNotBlank() } ?: "—")
                            GtkDataRow("Kecamatan", biodata.kecamatan?.takeIf { it.isNotBlank() } ?: "—")
                            GtkDataRow("Kode Pos", biodata.kodePos?.takeIf { it.isNotBlank() } ?: "—")
                        }
                    }

                    // 5. Kontak & Kebijakan Data
                    item {
                        GtkSectionCard(
                            title = "Kontak Terdaftar & Kebijakan Akun",
                            icon = Icons.Default.Lock,
                            accent = NeonWarning
                        ) {
                            GtkDataRow("Email Terverifikasi", biodata.email?.takeIf { it.isNotBlank() } ?: state.user?.email ?: "—")
                            val phone = biodata.noHp?.takeIf { it.isNotBlank() } ?: state.phone.ifBlank { state.identifier }
                            GtkDataRow("Nomor Telepon / WhatsApp", if (phone.isNotBlank()) phone else "—")
                            GtkDataRow("Status Akun", if (biodata.status.equals("active", ignoreCase = true)) "Aktif Terverifikasi" else biodata.status)

                            Spacer(Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CosmicSurface2)
                                    .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = null,
                                        tint = NeonWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Data kepegawaian pokok GTK (NUPTK, NIP, status kepegawaian, dan mata pelajaran) disinkronkan secara resmi dengan pangkalan data sekolah. Pembaruan data pokok dapat dikoordinasikan langsung dengan Admin Satuan Pendidikan.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherOfficialHeroCard(
    biodata: OfficialTeacherBiodata,
    userAvatar: String?,
) {
    val effectiveAvatar = userAvatar?.takeIf { it.isNotBlank() }
        ?: biodata.fotoProfile?.takeIf { it.isNotBlank() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserProfileAvatar(
                avatarUrl = effectiveAvatar,
                modifier = Modifier
                    .size(84.dp)
                    .border(2.dp, TeacherNeon, CircleShape),
                contentDescription = biodata.fullName,
                placeholderTint = TeacherNeon
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = biodata.fullName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                val roleSubtitle = biodata.jenisPtk?.takeIf { it.isNotBlank() } ?: "Pendidik & Tenaga Kependidikan"
                val nuptkText = biodata.nuptk?.takeIf { it.isNotBlank() }?.let { "NUPTK: $it" }
                    ?: biodata.nip?.takeIf { it.isNotBlank() }?.let { "NIP: $it" }
                    ?: "Pendidik Aktif"
                Text(
                    text = "$roleSubtitle • $nuptkText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // GTK Verified Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(TeacherNeon.copy(alpha = 0.12f))
                    .border(0.5.dp, TeacherNeon.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = TeacherNeon,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Data Pokok GTK Terverifikasi",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TeacherNeon
                    )
                }
            }
        }
    }
}

@Composable
private fun GtkSectionCard(
    title: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = TextPrimary
                )
            }

            HorizontalDivider(
                color = GlassBorder,
                thickness = 0.5.dp
            )

            content()
        }
    }
}

@Composable
private fun GtkDataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            color = TextSecondary,
            modifier = Modifier.weight(1.1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f)
        )
    }
}
