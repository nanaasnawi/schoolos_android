package com.schoolos.android.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.AtRiskStudent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtRiskStudentDetailScreen(
    onBack: () -> Unit,
    onNavigateToFullProfile: (studentId: String, studentName: String, nisn: String, className: String) -> Unit,
    onOpenMaterial: ((materialId: String) -> Unit)? = null,
    onOpenAssignment: ((assignmentId: String) -> Unit)? = null,
    onOpenQuiz: ((quizId: String) -> Unit)? = null,
    viewModel: AtRiskStudentDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        containerColor = if (LocalIsDarkTheme.current) Color(0xFF09090B) else Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = if (LocalIsDarkTheme.current) Color(0xFF09090B) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Detail Kendala Belajar",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Early Warning & Rekomendasi Intervensi",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                    IconButton(onClick = { viewModel.loadDetails(isRefresh = true) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = TextTertiary
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (state.items.isNotEmpty()) {
                Surface(
                    color = if (LocalIsDarkTheme.current) Color(0xFF111113) else Color.White,
                    border = BorderStroke(1.dp, if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFE2E8F0)),
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.sendAllReminders() },
                            enabled = !state.isSendingAll,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonError,
                                contentColor = Color.White
                            )
                        ) {
                            if (state.isSendingAll) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Mengirim Notifikasi...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Kirim Pengingat Terpadu (${state.totalIssues} Kendala)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && !state.isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Student Identity Hero Card
                    item {
                        StudentIdentityHeroCard(
                            studentName = state.studentName,
                            nisn = state.nisn,
                            className = state.className,
                            totalIssues = state.totalIssues,
                            onOpenFullProfile = {
                                onNavigateToFullProfile(
                                    state.studentId,
                                    state.studentName,
                                    state.nisn,
                                    state.className
                                )
                            }
                        )
                    }

                    // 2. 4-Pillar Stat Grid
                    item {
                        FourPillarStatStrip(
                            total = state.totalIssues,
                            unreadMaterials = state.unreadMaterialsCount,
                            overdueAssignments = state.overdueAssignmentsCount,
                            lowScores = state.lowScoresCount,
                            quizAlerts = state.quizAlertsCount
                        )
                    }

                    // 3. Category Filter Tabs
                    item {
                        AtRiskCategoryFilterRow(
                            selected = state.selectedCategoryFilter,
                            total = state.totalIssues,
                            unreadMaterials = state.unreadMaterialsCount,
                            overdueAssignments = state.overdueAssignmentsCount,
                            scoresAndQuizzes = state.lowScoresCount + state.quizAlertsCount,
                            onSelect = viewModel::onSelectCategoryFilter
                        )
                    }

                    // 4. Issue Breakdown Cards
                    if (state.filteredItems.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = if (LocalIsDarkTheme.current) Color(0xFF111113) else Color.White,
                                border = BorderStroke(1.dp, if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = NeonSuccess,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "Tidak Ada Kendala Pada Kategori Ini",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Siswa terpantau tidak memiliki masalah belajar pada filter yang dipilih.",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = state.filteredItems,
                            key = { _, it -> "${it.studentId}_${it.category}_${it.materialId ?: it.assignmentId ?: it.quizId ?: it.title}" }
                        ) { _, item ->
                            val isSending = state.sendingItemId == "${item.studentId}_${item.category}_${item.materialId ?: item.assignmentId ?: item.quizId}"
                            AtRiskDetailItemCard(
                                item = item,
                                isSending = isSending,
                                onSendReminder = { viewModel.sendSingleReminder(item) },
                                onOpenItem = {
                                    val matId = item.materialId
                                    val assignId = item.assignmentId
                                    val qzId = item.quizId
                                    when {
                                        matId != null -> onOpenMaterial?.invoke(matId)
                                        assignId != null -> onOpenAssignment?.invoke(assignId)
                                        qzId != null -> onOpenQuiz?.invoke(qzId)
                                    }
                                }
                            )
                        }
                    }

                    // 5. Pedagogical Intervention Guidelines
                    item {
                        PedagogicalInterventionCard()
                    }
                }
            }
        }
    }
}

// ─── Sub-Composables ────────────────────────────────────────────────────────────

@Composable
private fun StudentIdentityHeroCard(
    studentName: String,
    nisn: String,
    className: String,
    totalIssues: Int,
    onOpenFullProfile: () -> Unit,
) {
    val initial = studentName.firstOrNull()?.uppercase() ?: "S"

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (LocalIsDarkTheme.current) Color(0xFF111113) else Color.White,
        border = BorderStroke(1.dp, if (totalIssues > 0) NeonError.copy(alpha = 0.35f) else (if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFE2E8F0))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (LocalIsDarkTheme.current) Color(0xFF1B1B1E) else Color(0xFFF1F5F9))
                        .border(1.dp, if (LocalIsDarkTheme.current) Color(0xFF2E2E32) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = studentName.ifBlank { "Peserta Didik" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NISN: ${nisn.ifBlank { "-" }}",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (LocalIsDarkTheme.current) Color(0xFF1B1B1E) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (LocalIsDarkTheme.current) Color(0xFF2E2E32) else Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = className.ifBlank { "Rombel" },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Alert Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (totalIssues > 0) NeonError.copy(alpha = 0.15f) else NeonSuccess.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, if (totalIssues > 0) NeonError.copy(alpha = 0.4f) else NeonSuccess.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (totalIssues > 0) "$totalIssues Kendala" else "Aman",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalIssues > 0) NeonError else NeonSuccess,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(thickness = 1.dp, color = if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFE2E8F0))
            Spacer(Modifier.height(12.dp))

            // Link to Full Dapodik Biodata
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenFullProfile),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Lihat Profil & Biodata Lengkap Warga Belajar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun FourPillarStatStrip(
    total: Int,
    unreadMaterials: Int,
    overdueAssignments: Int,
    lowScores: Int,
    quizAlerts: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PillarStatBox(
            modifier = Modifier.weight(1f),
            label = "Total Kendala",
            count = total,
            icon = Icons.Default.WarningAmber,
            color = if (total > 0) NeonError else NeonSuccess
        )
        PillarStatBox(
            modifier = Modifier.weight(1f),
            label = "Materi",
            count = unreadMaterials,
            icon = Icons.Default.MenuBook,
            color = AccentNeonAmber
        )
        PillarStatBox(
            modifier = Modifier.weight(1f),
            label = "Tugas",
            count = overdueAssignments,
            icon = Icons.AutoMirrored.Filled.Assignment,
            color = NeonWarning
        )
        PillarStatBox(
            modifier = Modifier.weight(1f),
            label = "Nilai / Kuis",
            count = lowScores + quizAlerts,
            icon = Icons.Default.TrendingDown,
            color = Color(0xFFA855F7)
        )
    }
}

@Composable
private fun PillarStatBox(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (LocalIsDarkTheme.current) Color(0xFF111113) else Color.White,
        border = BorderStroke(1.dp, if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$count",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AtRiskCategoryFilterRow(
    selected: String,
    total: Int,
    unreadMaterials: Int,
    overdueAssignments: Int,
    scoresAndQuizzes: Int,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryFilterChip(
            label = "Semua ($total)",
            isSelected = selected == "ALL",
            onClick = { onSelect("ALL") }
        )
        CategoryFilterChip(
            label = "Materi ($unreadMaterials)",
            isSelected = selected == "UNREAD_MATERIAL",
            onClick = { onSelect("UNREAD_MATERIAL") }
        )
        CategoryFilterChip(
            label = "Tugas ($overdueAssignments)",
            isSelected = selected == "OVERDUE_ASSIGNMENT",
            onClick = { onSelect("OVERDUE_ASSIGNMENT") }
        )
        CategoryFilterChip(
            label = "Nilai & Kuis ($scoresAndQuizzes)",
            isSelected = selected == "LOW_SCORE" || selected == "QUIZ_ALERT",
            onClick = { onSelect("LOW_SCORE") }
        )
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) (if (LocalIsDarkTheme.current) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFFD1FAE5)) else (if (LocalIsDarkTheme.current) Color(0xFF1B1B1E) else Color(0xFFF1F5F9)),
        border = BorderStroke(1.dp, if (isSelected) (if (LocalIsDarkTheme.current) Color(0xFF10B981) else Color(0xFF059669)) else (if (LocalIsDarkTheme.current) Color(0xFF2E2E32) else Color(0xFFE2E8F0))),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) (if (LocalIsDarkTheme.current) Color(0xFF10B981) else Color(0xFF059669)) else (if (LocalIsDarkTheme.current) Color(0xFF94A3B8) else Color(0xFF64748B)),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun AtRiskDetailItemCard(
    item: AtRiskStudent,
    isSending: Boolean,
    onSendReminder: () -> Unit,
    onOpenItem: () -> Unit,
) {
    val (badgeText, badgeColor) = when (item.category) {
        "UNREAD_MATERIAL" -> "Materi Literasi Tertinggal" to AccentNeonAmber
        "OVERDUE_ASSIGNMENT" -> "Tugas Belum Dikumpulkan" to NeonWarning
        "LOW_SCORE" -> "Nilai di Bawah KKTP (< 70)" to NeonError
        "QUIZ_ALERT" -> "Kuis & Ujian CBT Terlewat" to Color(0xFFA855F7)
        else -> "Perlu Perhatian" to NeonError
    }

    val actionButtonText = when {
        item.materialId != null -> "Buka Materi"
        item.assignmentId != null -> "Buka Tugas"
        item.quizId != null -> "Buka Kuis"
        else -> null
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (LocalIsDarkTheme.current) Color(0xFF111113) else Color.White,
        border = BorderStroke(1.dp, if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Category Badge + Risk Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (item.riskLevel == "HIGH") NeonError.copy(alpha = 0.15f) else NeonWarning.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, if (item.riskLevel == "HIGH") NeonError.copy(alpha = 0.4f) else NeonWarning.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (item.riskLevel == "HIGH") "Prioritas Tinggi" else "Perhatian Sedang",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.riskLevel == "HIGH") NeonError else NeonWarning,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Title
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(8.dp))

            // Explanation box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (LocalIsDarkTheme.current) Color(0xFF1B1B1E) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (LocalIsDarkTheme.current) Color(0xFF2E2E32) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(15.dp).padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Open Item Button (if ID exists)
                if (actionButtonText != null) {
                    OutlinedButton(
                        onClick = onOpenItem,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, NeonBlue.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(actionButtonText, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                // Send Single Reminder Button
                Surface(
                    onClick = onSendReminder,
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.isReminded) NeonSuccess.copy(alpha = 0.12f) else NeonError.copy(alpha = 0.12f),
                    border = BorderStroke(
                        0.5.dp,
                        if (item.isReminded) NeonSuccess.copy(alpha = 0.4f) else NeonError.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = NeonError
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Mengirim...",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonError
                            )
                        } else if (item.isReminded) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = NeonSuccess,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Terkirim",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonSuccess
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = NeonError,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Ingatkan",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonError
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PedagogicalInterventionCard() {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (LocalIsDarkTheme.current) Color(0xFF111113) else Color.White,
        border = BorderStroke(1.dp, if (LocalIsDarkTheme.current) Color(0xFF232326) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = AccentNeonAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Panduan Tindakan Pedagogis Guru",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(10.dp))
            InterventionStepItem(
                step = "1",
                text = "Kirim notifikasi pengingat terpadu agar siswa menerima peringatan instan pada smartphone."
            )
            Spacer(Modifier.height(6.dp))
            InterventionStepItem(
                step = "2",
                text = "Berikan penugasan remedial atau materi pengayaan bagi topik evaluasi yang belum mencapai KKTP 70."
            )
            Spacer(Modifier.height(6.dp))
            InterventionStepItem(
                step = "3",
                text = "Lakukan kontak langsung dengan orang tua/wali murid apabila kendala berlanjut lebih dari 3 hari."
            )
        }
    }
}

@Composable
private fun InterventionStepItem(
    step: String,
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (LocalIsDarkTheme.current) Color(0xFF1B1B1E) else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NeonBlue
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 11.5.sp,
            color = TextSecondary,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

