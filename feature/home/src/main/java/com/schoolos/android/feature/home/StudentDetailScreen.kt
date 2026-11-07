package com.schoolos.android.feature.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.Achievement
import com.schoolos.android.domain.model.ClassStudent
import com.schoolos.android.domain.model.OfficialStudentBiodata
import com.schoolos.android.domain.model.Progress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val student = uiState.student
    val biodata = uiState.officialBiodata
    val progress = uiState.progress
    val achievements = uiState.achievements
    val isLoading = uiState.isLoading
    val errorMessage = uiState.errorMessage
    val selectedTab = uiState.selectedTab

    val tabs = listOf("Biodata Pokok", "Perkembangan", "Metrik", "Pencapaian", "Kontak")

    val isFemale = student.gender?.trim()?.uppercase() == "P"
    val genderLabel = if (isFemale) "Perempuan" else "Laki-laki"
    val genderIcon = if (isFemale) Icons.Default.Female else Icons.Default.Male

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Detail Warga Belajar",
                subtitle = if (student.className.isNotBlank()) "${student.className} • Profil Warga Belajar" else "Profil Warga Belajar",
                onBack = onBack,
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CosmicNavy)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                            .clickable { viewModel.refresh() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── HERO PROFILE CARD (FLAT EXECUTIVE, ZERO GRADIENT) ───────────
            item {
                StudentHeroCard(
                    student = student,
                    genderLabel = genderLabel,
                    genderIcon = genderIcon,
                )
            }

            // ── SEAMLESS 4-METRIC EXECUTIVE STRIP (NO CLUNKY OUTER BOXES) ────
            item {
                val avgGrade = progress?.overallProgress?.let { "%.0f%%".format(it) } ?: "--"
                val quizDone = progress?.let { "${it.quizCompleted}/${it.quizTotal}" } ?: "--"
                val assignDone = progress?.let { "${it.assignmentCompleted}/${it.assignmentTotal}" } ?: "--"
                val attendance = progress?.let {
                    val pct = if (it.sessionTotal > 0) (it.sessionAttended * 100 / it.sessionTotal) else 0
                    "$pct%"
                } ?: "--"

                ExecutiveMetricsStrip(
                    avgGrade = avgGrade,
                    quizDone = quizDone,
                    assignDone = assignDone,
                    attendance = attendance,
                    isLoading = isLoading,
                    onSelectTab = viewModel::selectTab,
                )
            }

            // ── SEGMENTED TAB ROW ──────────────────────────────────────────
            item {
                StudentTabRow(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = viewModel::selectTab
                )
            }

            // ── ERROR BANNER ──────────────────────────────────────────────
            if (errorMessage != null) {
                item {
                    ErrorBanner(
                        message = errorMessage,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // ── TAB CONTENT ───────────────────────────────────────────────
            when (selectedTab) {
                0 -> {
                    item {
                        DapodikDetailTab(
                            student = student,
                            biodata = biodata,
                            isLoading = isLoading,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                1 -> {
                    item {
                        AcademicProgressTab(
                            progress = progress,
                            isLoading = isLoading,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                2 -> {
                    item {
                        LearningMetricsTab(
                            progress = progress,
                            isLoading = isLoading,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                3 -> {
                    if (achievements.isEmpty() && !isLoading) {
                        item {
                            EmptyAchievementsCard(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    } else {
                        items(achievements, key = { it.id }) { achievement ->
                            AchievementItem(
                                achievement = achievement,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                4 -> {
                    item {
                        ContactTab(
                            student = student,
                            context = context,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── HERO CARD (CLEAN FLAT EXECUTIVE, ZERO GRADIENT) ──────────────────────────
@Composable
private fun StudentHeroCard(
    student: ClassStudent,
    genderLabel: String,
    genderIcon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top: Avatar + Identity + Active Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Crisp flat avatar monogram
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CosmicSurface2)
                        .border(1.dp, GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.fullName.firstOrNull()?.toString()?.uppercase() ?: "?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = student.fullName.ifBlank { "Warga Belajar Akselerasi-Edu" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (student.nisn.isNotBlank()) {
                            Text(
                                text = "NISN: ${student.nisn}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }

                        // Status pill with crisp active dot
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonSuccess.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, NeonSuccess.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonSuccess)
                                )
                                Text(
                                    text = "Aktif",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonSuccess
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

            // Bottom: Meta info chips (Rombel & Gender)
            val formattedClass = when {
                student.className.isBlank() || student.className == "-" -> "Warga Belajar Aktif"
                student.className.startsWith("Paket", ignoreCase = true) -> student.className
                student.className.startsWith("Rombel", ignoreCase = true) -> student.className
                student.className.startsWith("Kelas", ignoreCase = true) -> student.className.replaceFirst("Kelas", "Rombel", ignoreCase = true)
                else -> "Rombel ${student.className}"
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ExecutiveChip(
                    label = formattedClass,
                    icon = Icons.Default.School,
                    tint = StudentNeon,
                    modifier = Modifier.weight(1f, fill = false)
                )
                ExecutiveChip(
                    label = genderLabel,
                    icon = genderIcon,
                    tint = TextSecondary,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

@Composable
private fun ExecutiveChip(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = CosmicSurface2,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── SEAMLESS 4-METRIC EXECUTIVE STRIP (NO CLUNKY OUTER BOXES) ────────────────
@Composable
private fun ExecutiveMetricsStrip(
    avgGrade: String,
    quizDone: String,
    assignDone: String,
    attendance: String,
    isLoading: Boolean,
    onSelectTab: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricColumnItem(
                label = "Rata-rata",
                value = avgGrade,
                accentColor = StudentNeon,
                isLoading = isLoading,
                onClick = onSelectTab?.let { { it(2) } },
                modifier = Modifier.weight(1f)
            )

            Box(modifier = Modifier.width(0.5.dp).height(22.dp).background(GlassBorder))

            MetricColumnItem(
                label = "Kuis",
                value = quizDone,
                accentColor = NeonBlue,
                isLoading = isLoading,
                onClick = onSelectTab?.let { { it(1) } },
                modifier = Modifier.weight(1f)
            )

            Box(modifier = Modifier.width(0.5.dp).height(22.dp).background(GlassBorder))

            MetricColumnItem(
                label = "Tugas",
                value = assignDone,
                accentColor = NeonSuccess,
                isLoading = isLoading,
                onClick = onSelectTab?.let { { it(1) } },
                modifier = Modifier.weight(1f)
            )

            Box(modifier = Modifier.width(0.5.dp).height(22.dp).background(GlassBorder))

            MetricColumnItem(
                label = "Hadir",
                value = attendance,
                accentColor = NeonWarning,
                isLoading = isLoading,
                onClick = onSelectTab?.let { { it(1) } },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricColumnItem(
    label: String,
    value: String,
    accentColor: Color,
    isLoading: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (isLoading) {
            ShimmerBox(
                modifier = Modifier
                    .width(42.dp)
                    .height(18.dp),
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(Modifier.height(2.dp))
            ShimmerBox(
                modifier = Modifier
                    .width(32.dp)
                    .height(11.dp),
                shape = RoundedCornerShape(3.dp)
            )
        } else {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─── SEGMENTED TAB ROW ────────────────────────────────────────────────────────
@Composable
private fun StudentTabRow(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val accentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val inactiveBg = if (isDark) Color(0xFF141417) else Color.White
    val inactiveBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val inactiveText = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            val tabIcon = when (index) {
                0 -> Icons.Default.Badge
                1 -> Icons.AutoMirrored.Filled.TrendingUp
                2 -> Icons.Default.Assessment
                3 -> Icons.Default.EmojiEvents
                4 -> Icons.Default.Phone
                else -> Icons.Default.Info
            }
            Surface(
                onClick = { onTabSelected(index) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) accentColor.copy(alpha = if (isDark) 0.16f else 0.10f) else inactiveBg,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) accentColor.copy(alpha = if (isDark) 0.55f else 0.45f) else inactiveBorder
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = tabIcon,
                        contentDescription = null,
                        tint = if (isSelected) accentColor else inactiveText,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) accentColor else inactiveText,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ─── TAB 0 — DAPODIK DETAIL TAB ───────────────────────────────────────────────
@Composable
private fun DapodikDetailTab(
    student: ClassStudent,
    biodata: OfficialStudentBiodata?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isLoading && biodata == null) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(3) {
                ShimmerCard()
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Identitas Pokok
        DapodikSectionCard(
            title = "Identitas Pribadi Pokok",
            icon = Icons.Default.Badge,
            accent = StudentNeon,
        ) {
            DapodikDataRow("Nama Lengkap", biodata?.fullName ?: student.fullName)
            DapodikDataRow("NISN", biodata?.nisn ?: student.nisn)
            DapodikDataRow("NIK", biodata?.nik ?: "-")
            DapodikDataRow("NIPD", biodata?.nipd ?: "-")
            DapodikDataRow("Rombongan Belajar", biodata?.className ?: student.className)
            DapodikDataRow("Jenis Kelamin", when (biodata?.gender?.uppercase() ?: student.gender?.uppercase()) {
                "L" -> "Laki-laki"
                "P" -> "Perempuan"
                else -> biodata?.gender ?: student.gender ?: "-"
            })
            val ttl = listOfNotNull(biodata?.placeOfBirth, biodata?.dateOfBirth).joinToString(", ")
            DapodikDataRow("Tempat, Tanggal Lahir", ttl.ifBlank { "-" })
            DapodikDataRow("Agama", biodata?.religion ?: "-")
        }

        // 2. Alamat Domisili
        DapodikSectionCard(
            title = "Alamat Domisili Terdaftar",
            icon = Icons.Default.Home,
            accent = NeonBlue,
        ) {
            DapodikDataRow("Alamat Jalan", biodata?.alamatJalan ?: "-")
            val rtrw = if (!biodata?.rt.isNullOrBlank() || !biodata?.rw.isNullOrBlank()) {
                "RT ${biodata?.rt ?: "00"} / RW ${biodata?.rw ?: "00"}"
            } else "-"
            DapodikDataRow("RT / RW", rtrw)
            DapodikDataRow("Dusun / Lingkungan", biodata?.dusun ?: "-")
            DapodikDataRow("Desa / Kelurahan", biodata?.kelurahan ?: "-")
            DapodikDataRow("Kecamatan", biodata?.kecamatan ?: "-")
            DapodikDataRow("Kode Pos", biodata?.kodePos ?: "-")
        }

        // 3. Data Orang Tua & Wali
        DapodikSectionCard(
            title = "Data Orang Tua & Wali",
            icon = Icons.Default.FamilyRestroom,
            accent = AccentNeonPurple,
        ) {
            Text(
                text = "DATA AYAH KANDUNG",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StudentNeon,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            DapodikDataRow("Nama Ayah", biodata?.namaAyah ?: "-")
            DapodikDataRow("Pekerjaan Ayah", biodata?.pekerjaanAyah ?: "-")
            DapodikDataRow("Penghasilan Ayah", biodata?.penghasilanAyah ?: "-")

            HorizontalDivider(
                color = GlassBorder,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            Text(
                text = "DATA IBU KANDUNG",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AccentNeonPink,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            DapodikDataRow("Nama Ibu", biodata?.namaIbu ?: "-")
            DapodikDataRow("Pekerjaan Ibu", biodata?.pekerjaanIbu ?: "-")
            DapodikDataRow("Penghasilan Ibu", biodata?.penghasilanIbu ?: "-")

            if (!biodata?.namaWali.isNullOrBlank() && biodata?.namaWali != "-") {
                HorizontalDivider(
                    color = GlassBorder,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                Text(
                    text = "DATA WALI MURID",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonWarning,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                DapodikDataRow("Nama Wali", biodata?.namaWali ?: "-")
                DapodikDataRow("Pekerjaan Wali", biodata?.pekerjaanWali ?: "-")
                DapodikDataRow("Penghasilan Wali", biodata?.penghasilanWali ?: "-")
            }
        }

        // 4. Kesejahteraan Pendidikan
        DapodikSectionCard(
            title = "Kesejahteraan & Bantuan Pendidikan",
            icon = Icons.Default.CardMembership,
            accent = NeonWarning,
        ) {
            DapodikDataRow("Penerima KPS", if (biodata?.penerimaKps == true) "Ya" else "Tidak")
            if (!biodata?.noKps.isNullOrBlank()) {
                DapodikDataRow("Nomor KPS", biodata?.noKps ?: "-")
            }
            DapodikDataRow("Penerima KIP", if (biodata?.penerimaKip == true) "Ya" else "Tidak")
            if (!biodata?.nomorKip.isNullOrBlank()) {
                DapodikDataRow("Nomor KIP", biodata?.nomorKip ?: "-")
                DapodikDataRow("Nama di KIP", biodata?.namaDiKip ?: "-")
            }
            if (!biodata?.nomorKks.isNullOrBlank()) {
                DapodikDataRow("Nomor KKS", biodata?.nomorKks ?: "-")
            }
            DapodikDataRow("Layak PIP", if (biodata?.layakPip == true) "Layak" else "Tidak")
            if (!biodata?.alasanLayakPip.isNullOrBlank()) {
                DapodikDataRow("Alasan Layak PIP", biodata?.alasanLayakPip ?: "-")
            }
        }

        // 5. Rekening Penyaluran Bantuan
        DapodikSectionCard(
            title = "Rekening Bank Penyaluran",
            icon = Icons.Default.AccountBalance,
            accent = NeonSuccess,
        ) {
            DapodikDataRow("Bank Penyalur", biodata?.bank ?: "-")
            DapodikDataRow("Nomor Rekening", biodata?.nomorRekeningBank ?: "-")
            DapodikDataRow("Rekening Atas Nama", biodata?.rekeningAtasNama ?: "-")
        }
    }
}

@Composable
private fun DapodikSectionCard(
    title: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
            content()
        }
    }
}

@Composable
private fun DapodikDataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f)
        )
    }
}

// ─── TAB 1 — ACADEMIC PROGRESS ───────────────────────────────────────────────
@Composable
private fun AcademicProgressTab(
    progress: Progress?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Status Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = CosmicNavy,
            border = BorderStroke(0.5.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = StudentNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Status Akademik Siswa",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Ringkasan evaluasi belajar siswa",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }

                HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                if (isLoading) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                } else {
                    val status = progress?.academicStatus ?: "Data perkembangan belum tersedia"
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = CosmicSurface2,
                        border = BorderStroke(0.5.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = StudentNeon,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = status,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    val notes = progress?.teacherNotes
                    if (!notes.isNullOrBlank()) {
                        Text(
                            text = "Catatan Evaluasi Guru",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = notes,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Coaching Notes Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = CosmicNavy,
            border = BorderStroke(0.5.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Catatan Pembinaan Siswa",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                val coachingNotes = progress?.teacherNotes?.takeIf { it.isNotBlank() }
                    ?: "Belum ada catatan pembinaan khusus dari guru pengampu."
                Text(
                    text = coachingNotes,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─── TAB 2 — LEARNING METRICS ───────────────────────────────────────────────
@Composable
private fun LearningMetricsTab(
    progress: Progress?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = StudentNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Metrik & Pencapaian Modul",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

            if (isLoading) {
                repeat(4) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            } else {
                val lessonsDone = progress?.lessonCompleted ?: 0
                val lessonsTotal = progress?.lessonTotal ?: 0
                val assignDone = progress?.assignmentCompleted ?: 0
                val assignTotal = progress?.assignmentTotal ?: 0
                val quizDone = progress?.quizCompleted ?: 0
                val quizTotal = progress?.quizTotal ?: 0
                val sessionDone = progress?.sessionAttended ?: 0
                val sessionTotal = progress?.sessionTotal ?: 0

                MetricProgressRow(
                    title = "Modul & Materi Pembelajaran",
                    current = lessonsDone,
                    total = lessonsTotal,
                    color = StudentNeon,
                    icon = Icons.AutoMirrored.Filled.MenuBook
                )
                MetricProgressRow(
                    title = "Tugas & Praktik Lapangan",
                    current = assignDone,
                    total = assignTotal,
                    color = NeonBlue,
                    icon = Icons.Default.Assignment
                )
                MetricProgressRow(
                    title = "Kuis & Asesmen Mandiri",
                    current = quizDone,
                    total = quizTotal,
                    color = NeonSuccess,
                    icon = Icons.Default.Quiz
                )
                MetricProgressRow(
                    title = "Kehadiran Sesi Pembelajaran",
                    current = sessionDone,
                    total = sessionTotal,
                    color = NeonWarning,
                    icon = Icons.Default.EventAvailable
                )
            }
        }
    }
}

@Composable
private fun MetricProgressRow(
    title: String,
    current: Int,
    total: Int,
    color: Color,
    icon: ImageVector,
) {
    val pct = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(600),
        label = "progress_$title"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
            Text(
                text = if (total > 0) "$current/$total (${(pct * 100).toInt()}%)" else "--",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.12f)
        )
    }
}

// ─── TAB 3 — ACHIEVEMENTS ───────────────────────────────────────────────────
@Composable
private fun EmptyAchievementsCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CosmicSurface2)
                    .border(0.5.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = StudentNeon,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Belum Ada Lencana Prestasi",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Lencana prestasi akan tercatat otomatis saat siswa menyelesaikan tantangan & kuis.",
                fontSize = 11.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AchievementItem(
    achievement: Achievement,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CosmicSurface2)
                    .border(0.5.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = StudentNeon,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = achievement.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = achievement.description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!achievement.earnedAt.isNullOrBlank()) {
                    Text(
                        text = "Diraih: ${achievement.earnedAt}",
                        fontSize = 10.sp,
                        color = StudentNeon,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─── TAB 4 — CONTACT ─────────────────────────────────────────────────────────
@Composable
private fun ContactTab(
    student: ClassStudent,
    context: Context,
    modifier: Modifier = Modifier,
) {
    val phoneRaw = student.noHp ?: ""
    val cleanPhone = phoneRaw.replace(Regex("[^0-9]"), "")
    val waPhone = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone
    val studentEmail = student.email ?: "${student.fullName.lowercase().replace(" ", ".")}@schoolos.id"

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Kontak & Koordinasi Orang Tua",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // WhatsApp Card
        ContactActionCard(
            title = "WhatsApp Wali Murid",
            subtitle = phoneRaw.ifBlank { "Nomor telepon tidak tersedia" },
            icon = Icons.AutoMirrored.Filled.Chat,
            accentColor = NeonSuccess,
            enabled = cleanPhone.isNotBlank(),
            onClick = {
                val message = "Halo Bapak/Ibu wali dari ${student.fullName} (${student.className}), saya dari pihak guru Akselerasi-Edu ingin berkoordinasi mengenai perkembangan siswa."
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?phone=$waPhone&text=${Uri.encode(message)}")
                }
                context.startActivity(intent)
            }
        )

        // Phone Call Card
        ContactActionCard(
            title = "Telepon Langsung",
            subtitle = phoneRaw.ifBlank { "Nomor telepon tidak tersedia" },
            icon = Icons.Default.Call,
            accentColor = NeonBlue,
            enabled = cleanPhone.isNotBlank(),
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanPhone")
                }
                context.startActivity(intent)
            }
        )

        // Email Card
        ContactActionCard(
            title = "Email Resmi Siswa",
            subtitle = studentEmail,
            icon = Icons.Default.Email,
            accentColor = StudentNeon,
            enabled = true,
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$studentEmail")
                }
                context.startActivity(intent)
            }
        )
    }
}

@Composable
private fun ContactActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (enabled) 1f else 0.4f
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CosmicSurface2)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = alpha),
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = accentColor.copy(alpha = alpha),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── ERROR BANNER ────────────────────────────────────────────────────────────
@Composable
private fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CosmicNavy,
        border = BorderStroke(0.5.dp, NeonError.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = NeonError,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                fontSize = 12.sp,
                color = NeonError,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
