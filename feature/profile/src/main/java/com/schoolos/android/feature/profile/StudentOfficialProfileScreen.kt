package com.schoolos.android.feature.profile

import androidx.compose.foundation.BorderStroke
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
import com.schoolos.android.domain.model.OfficialStudentBiodata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentOfficialProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val biodata = state.officialBiodata
    val isLoading = state.officialBiodataLoading
    val errorMessage = state.officialBiodataError

    LaunchedEffect(Unit) {
        if (biodata == null) {
            viewModel.loadOfficialBiodata()
        }
    }

    val isDark = LocalIsDarkTheme.current
    val screenBg = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    Scaffold(
        containerColor = screenBg,
        topBar = {
            ExecutiveTopBar(
                title = "Biodata Pokok Warga Belajar",
                subtitle = "Sinkronisasi Pangkalan Data Pokok Pendidikan",
                onBack = onBack,
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            .clickable { viewModel.loadOfficialBiodata() },
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
                            color = StudentNeon,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Memuat biodata pokok dari server...",
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
                            text = "Gagal Memuat Biodata Resmi",
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
                            onClick = { viewModel.loadOfficialBiodata() },
                            colors = ButtonDefaults.buttonColors(containerColor = StudentNeon)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Coba Lagi", color = Color.White)
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
                        OfficialHeroCard(
                            biodata = biodata,
                            userAvatar = state.user?.avatarUrl ?: state.userAvatarUrl ?: biodata.fotoProfile
                        )
                    }

                    // 2. Data Pokok Siswa
                    item {
                        OfficialSectionCard(
                            title = "Identitas Pribadi Pokok",
                            icon = Icons.Default.Badge,
                            accent = StudentNeon
                        ) {
                            OfficialDataRow("Nama Lengkap", biodata.fullName)
                            OfficialDataRow("NISN", biodata.nisn)
                            OfficialDataRow("NIK", biodata.nik ?: "-")
                            OfficialDataRow("NIPD", biodata.nipd ?: "-")
                            OfficialDataRow("Rombongan Belajar", biodata.className ?: "-")
                            OfficialDataRow("Jenis Kelamin", when (biodata.gender?.uppercase()) {
                                "L" -> "Laki-laki"
                                "P" -> "Perempuan"
                                else -> biodata.gender ?: "-"
                            })
                            val ttl = listOfNotNull(biodata.placeOfBirth, biodata.dateOfBirth).joinToString(", ")
                            OfficialDataRow("Tempat, Tanggal Lahir", ttl.ifBlank { "-" })
                            OfficialDataRow("Agama", biodata.religion ?: "-")
                        }
                    }

                    // 3. Alamat Domisili
                    item {
                        OfficialSectionCard(
                            title = "Domisili Terdaftar",
                            icon = Icons.Default.Home,
                            accent = NeonInfo
                        ) {
                            OfficialDataRow("Alamat Jalan", biodata.alamatJalan ?: "-")
                            val rtrw = if (!biodata.rt.isNullOrBlank() || !biodata.rw.isNullOrBlank()) {
                                "RT ${biodata.rt ?: "00"} / RW ${biodata.rw ?: "00"}"
                            } else "-"
                            OfficialDataRow("RT / RW", rtrw)
                            OfficialDataRow("Dusun / Lingkungan", biodata.dusun ?: "-")
                            OfficialDataRow("Desa / Kelurahan", biodata.kelurahan ?: "-")
                            OfficialDataRow("Kecamatan", biodata.kecamatan ?: "-")
                            OfficialDataRow("Kode Pos", biodata.kodePos ?: "-")
                        }
                    }

                    // 4. Data Orang Tua & Wali
                    item {
                        OfficialSectionCard(
                            title = "Data Orang Tua & Wali",
                            icon = Icons.Default.FamilyRestroom,
                            accent = Color(0xFFA855F7)
                        ) {
                            Text(
                                text = "DATA AYAH KANDUNG",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = StudentNeon,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OfficialDataRow("Nama Ayah", biodata.namaAyah ?: "-")
                            OfficialDataRow("Pekerjaan Ayah", biodata.pekerjaanAyah ?: "-")
                            OfficialDataRow("Penghasilan Ayah", biodata.penghasilanAyah ?: "-")

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = GlassBorder,
                                thickness = 0.5.dp
                            )

                            Text(
                                text = "DATA IBU KANDUNG",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFEC4899),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OfficialDataRow("Nama Ibu", biodata.namaIbu ?: "-")
                            OfficialDataRow("Pekerjaan Ibu", biodata.pekerjaanIbu ?: "-")
                            OfficialDataRow("Penghasilan Ibu", biodata.penghasilanIbu ?: "-")

                            if (!biodata.namaWali.isNullOrBlank()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = GlassBorder,
                                    thickness = 0.5.dp
                                )
                                Text(
                                    text = "DATA WALI MURID",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NeonWarning,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OfficialDataRow("Nama Wali", biodata.namaWali ?: "-")
                                OfficialDataRow("Pekerjaan Wali", biodata.pekerjaanWali ?: "-")
                                OfficialDataRow("Penghasilan Wali", biodata.penghasilanWali ?: "-")
                            }
                        }
                    }

                    // 5. Bantuan PIP & Kesiapan Rekening
                    item {
                        OfficialSectionCard(
                            title = "Program Indonesia Pintar (PIP) & Rekening",
                            icon = Icons.Default.AccountBalance,
                            accent = NeonSuccess
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Status Penerima KIP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                if (biodata.penerimaKip) {
                                    StatusBadge(text = "Penerima KIP Aktif", isPositive = true)
                                } else {
                                    StatusBadge(text = "Bukan Penerima KIP", isPositive = false)
                                }
                            }
                            OfficialDataRow("Nomor KIP", biodata.nomorKip ?: "-")
                            OfficialDataRow("Nama Tertera di KIP", biodata.namaDiKip ?: "-")
                            OfficialDataRow("Nomor KKS", biodata.nomorKks ?: "-")

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = GlassBorder,
                                thickness = 0.5.dp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Usulan Layak PIP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                if (biodata.layakPip) {
                                    StatusBadge(text = "Ya (Layak PIP)", isPositive = true)
                                } else {
                                    StatusBadge(text = "Tidak Ada Usulan", isPositive = false)
                                }
                            }
                            OfficialDataRow("Alasan Layak PIP", biodata.alasanLayakPip ?: "-")

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = GlassBorder,
                                thickness = 0.5.dp
                            )

                            Text(
                                text = "REKENING PENYALUR RESMI",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeonSuccess,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OfficialDataRow("Bank Penyalur", biodata.bank ?: "-")
                            OfficialDataRow("Nomor Rekening", biodata.nomorRekeningBank ?: "-")
                            OfficialDataRow("Rekening Atas Nama", biodata.rekeningAtasNama ?: "-")
                        }
                    }

                    // 6. Kontak & Aturan Penguncian
                    item {
                        OfficialSectionCard(
                            title = "Kontak Terdaftar & Kebijakan Data",
                            icon = Icons.Default.Lock,
                            accent = NeonWarning
                        ) {
                            OfficialDataRow("Email Akun", biodata.email ?: state.user?.email ?: "-")
                            OfficialDataRow("Nomor Telepon / WA", biodata.noHp ?: state.phone.ifBlank { "-" })

                            Spacer(Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(innerCardBg)
                                    .border(0.5.dp, innerCardBorder, RoundedCornerShape(14.dp))
                                    .padding(14.dp)
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
                                        text = "Sesuai regulasi sistem, hanya Email dan Nomor Telepon yang dapat diperbarui secara mandiri oleh warga belajar pada halaman Profil. Perubahan identitas pokok, domisili, orang tua, dan bantuan pendidikan wajib melalui Operator PKBM / Lembaga.",
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
private fun OfficialHeroCard(
    biodata: OfficialStudentBiodata,
    userAvatar: String?,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val studentNeon = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    val effectiveAvatar = userAvatar?.takeIf { it.isNotBlank() }
        ?: biodata.fotoProfile?.takeIf { it.isNotBlank() }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            UserProfileAvatar(
                avatarUrl = effectiveAvatar,
                modifier = Modifier
                    .size(84.dp)
                    .border(2.dp, studentNeon, CircleShape),
                contentDescription = biodata.fullName,
                placeholderTint = studentNeon
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = biodata.fullName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "NISN: ${biodata.nisn} • ${biodata.className ?: "Kelas Belum Terdata"}",
                    fontSize = 13.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Dapodik Verified Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = successColor.copy(alpha = if (isDark) 0.15f else 0.10f),
                border = BorderStroke(0.5.dp, successColor.copy(alpha = if (isDark) 0.40f else 0.25f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = successColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Data Pokok Terverifikasi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = successColor
                    )
                }
            }
        }
    }
}

@Composable
private fun OfficialSectionCard(
    title: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = if (isDark) 0.15f else 0.10f)),
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
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }

            content()
        }
    }
}

@Composable
private fun OfficialDataRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.3f)
        )
    }
}

@Composable
private fun StatusBadge(
    text: String,
    isPositive: Boolean,
) {
    val bg = if (isPositive) NeonSuccess.copy(alpha = 0.12f) else CosmicSurface2
    val border = if (isPositive) NeonSuccess.copy(alpha = 0.3f) else GlassBorder
    val textColor = if (isPositive) NeonSuccess else TextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
            color = textColor
        )
    }
}
