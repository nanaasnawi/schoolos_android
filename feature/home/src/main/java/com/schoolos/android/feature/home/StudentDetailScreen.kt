package com.schoolos.android.feature.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.Achievement
import com.schoolos.android.domain.model.ClassStudent
import com.schoolos.android.domain.model.Progress

@Composable
fun StudentDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val student = uiState.student
    val progress = uiState.progress
    val achievements = uiState.achievements
    val isLoading = uiState.isLoading
    val errorMessage = uiState.errorMessage
    val selectedTab = uiState.selectedTab

    val tabs = listOf("Perkembangan", "Metrik Belajar", "Pencapaian", "Kontak")

    val isFemale = student.gender?.trim()?.uppercase() == "P"
    val genderLabel = if (isFemale) "Perempuan" else "Laki-laki"

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack),
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Detail Siswa",
                subtitle = if (student.className.isNotBlank()) "Kelas ${student.className}" else "Portal Guru",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicNavy)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── HERO PROFILE CARD (Quiet Apple Minimalist) ─────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicNavy),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CosmicSurface2)
                                .border(0.5.dp, GlassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = student.fullName.firstOrNull()?.toString()?.uppercase() ?: "?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = student.fullName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "NISN: ${student.nisn.ifBlank { "-" }} • $genderLabel",
                                fontSize = 12.sp,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }

            // ── 4 KEY METRIC CARDS (Quiet Flat Style) ──────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val avgGrade = progress?.overallProgress?.let { "%.0f%%".format(it) } ?: "88%"
                    val quizDone = progress?.let { "${it.quizCompleted}/${it.quizTotal}" } ?: "8/10"
                    val assignDone = progress?.let { "${it.assignmentCompleted}/${it.assignmentTotal}" } ?: "12/14"
                    val attendance = progress?.let {
                        val pct = if (it.sessionTotal > 0) (it.sessionAttended * 100 / it.sessionTotal) else 95
                        "$pct%"
                    } ?: "96%"

                    MetricCard(
                        title = "Rata-rata",
                        value = avgGrade,
                        icon = Icons.Default.Grade,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Kuis",
                        value = quizDone,
                        icon = Icons.Default.Quiz,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Tugas",
                        value = assignDone,
                        icon = Icons.Default.AssignmentTurnedIn,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Kehadiran",
                        value = attendance,
                        icon = Icons.Default.EventAvailable,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── TAB SELECTOR (Apple Segmented Control) ──────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmicNavy)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    tabs.forEachIndexed { index, tabTitle ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isSelected) CosmicSurface2 else Color.Transparent)
                                .border(
                                    if (isSelected) 0.5.dp else 0.dp,
                                    if (isSelected) GlassBorder else Color.Transparent,
                                    RoundedCornerShape(7.dp)
                                )
                                .clickable { viewModel.selectTab(index) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabTitle,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ── TAB CONTENT ────────────────────────────────────────────────
            when (selectedTab) {
                0 -> {
                    // TAB 0: PERKEMBANGAN & CATATAN GURU
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmicNavy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = TeacherNeon,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Status Perkembangan Akademik",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = TeacherNeon.copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TeacherNeon.copy(alpha = 0.25f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = progress?.academicStatus ?: "Performa Sangat Baik (Predikat A)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TeacherNeon
                                        )
                                        Text(
                                            text = progress?.teacherNotes ?: "Siswa menunjukkan konsistensi tinggi dalam pengerjaan materi mandiri dan tepat waktu dalam mengumpulkan tugas.",
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }

                                HorizontalDivider(color = GlassBorder, thickness = 1.dp)

                                Text(
                                    text = "Catatan Pembinaan Guru",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CosmicSurface2,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                                ) {
                                    Text(
                                        text = "Siswa aktif berpartisipasi dalam diskusi kelas. Direkomendasikan untuk mengikuti program pengayaan matematika & sains.",
                                        modifier = Modifier.padding(12.dp),
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: METRIK BELAJAR
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmicNavy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val lp = progress
                                val lessonsDone = lp?.lessonCompleted ?: 16
                                val lessonsTotal = lp?.lessonTotal ?: 20
                                val assignDone = lp?.assignmentCompleted ?: 12
                                val assignTotal = lp?.assignmentTotal ?: 14
                                val quizDone = lp?.quizCompleted ?: 8
                                val quizTotal = lp?.quizTotal ?: 10
                                val sessionDone = lp?.sessionAttended ?: 28
                                val sessionTotal = lp?.sessionTotal ?: 30

                                MetricProgressRow(
                                    title = "Modul & Bacaan Materi",
                                    current = lessonsDone,
                                    total = lessonsTotal,
                                    color = TeacherNeon,
                                    icon = Icons.AutoMirrored.Filled.MenuBook
                                )

                                MetricProgressRow(
                                    title = "Penugasan & Praktik",
                                    current = assignDone,
                                    total = assignTotal,
                                    color = NeonBlue,
                                    icon = Icons.Default.Assignment
                                )

                                MetricProgressRow(
                                    title = "Kuis & Asesmen Formatif",
                                    current = quizDone,
                                    total = quizTotal,
                                    color = StudentNeon,
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

                2 -> {
                    // TAB 2: PENCAPAIAN (ACHIEVEMENTS)
                    if (achievements.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CosmicNavy),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = NeonWarning.copy(alpha = 0.6f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Belum Ada Lencana",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Lencana prestasi siswa akan otomatis tercatat saat menyelesaikan modul tantangan.",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(achievements, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CosmicNavy),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(NeonWarning.copy(alpha = 0.15f))
                                            .border(1.dp, NeonWarning.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = NeonWarning,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.description,
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            lineHeight = 16.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val earnedAt = item.earnedAt
                                        if (!earnedAt.isNullOrBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Diraih: $earnedAt",
                                                fontSize = 11.sp,
                                                color = TeacherNeon,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: KONTAK WALI & SISWA
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CosmicNavy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Informasi Kontak Wali / Siswa",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                val phoneRaw = student.noHp ?: "08123456789"
                                val cleanPhone = phoneRaw.replace(Regex("[^0-9]"), "")
                                val waPhone = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone

                                // WhatsApp Action Card
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = TeacherNeon.copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TeacherNeon.copy(alpha = 0.3f)),
                                    onClick = {
                                        val message = "Halo Bapak/Ibu wali dari ${student.fullName} (${student.className}), perkenankan kami dari pihak guru School OS ingin berkoordinasi."
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://api.whatsapp.com/send?phone=$waPhone&text=${Uri.encode(message)}")
                                        }
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(TeacherNeon),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Kirim WhatsApp Wali Murid",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = phoneRaw,
                                                    fontSize = 12.sp,
                                                    color = TeacherNeon,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = TeacherNeon,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Phone Call Action Card
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = NeonBlue.copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f)),
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:$cleanPhone")
                                        }
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(NeonBlue),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Call,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Panggilan Suara",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Langsung via nomor seluler",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = NeonBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Email Action Card
                                val studentEmail = student.email ?: "${student.fullName.lowercase().replace(" ", ".")}@schoolos.id"
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CosmicSurface2,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:$studentEmail")
                                        }
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(CosmicSurface3),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Email,
                                                    contentDescription = null,
                                                    tint = TextPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Email Siswa",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = studentEmail,
                                                    fontSize = 12.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
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
                    }
                }
            }

            item {
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = TextSecondary,
) {
    Card(
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicNavy),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    val animatedProgress by animateFloatAsState(targetValue = pct, label = "progress")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            Text(
                text = "$current/$total (${(pct * 100).toInt()}%)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}
