package com.schoolos.android.feature.home

import com.schoolos.android.core.R as CoreR
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    var showSendAllConfirmDialog by remember { mutableStateOf(false) }
    var singleItemToRemind by remember { mutableStateOf<AtRiskStudent?>(null) }

    if (showSendAllConfirmDialog) {
        SchoolOsConfirmationDialog(
            title = "Kirim Pengingat Terpadu?",
            message = "Notifikasi pengingat akademik akan dikirimkan langsung ke smartphone warga belajar ${state.studentName} untuk ${state.totalIssues} kendala belajar.",
            confirmText = "Kirim Pengingat",
            dismissText = "Batal",
            icon = Icons.Outlined.NotificationsActive,
            accentColor = Color(0xFFEF4444),
            onConfirm = {
                showSendAllConfirmDialog = false
                viewModel.sendAllReminders()
            },
            onDismiss = { showSendAllConfirmDialog = false }
        )
    }

    singleItemToRemind?.let { item ->
        SchoolOsConfirmationDialog(
            title = "Kirim Pengingat Kendala?",
            message = "Kirimkan pengingat untuk: \"${item.title}\" kepada warga belajar ${state.studentName}?",
            confirmText = "Kirim",
            dismissText = "Batal",
            icon = Icons.Outlined.Notifications,
            accentColor = Color(0xFFEF4444),
            onConfirm = {
                val target = item
                singleItemToRemind = null
                viewModel.sendSingleReminder(target)
            },
            onDismiss = { singleItemToRemind = null }
        )
    }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        containerColor = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ExecutiveTopBar(
                title = "Detail Kendala Belajar",
                subtitle = "Early Warning System & Intervensi Warga Belajar",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.loadDetails(isRefresh = true) }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && !state.isRefreshing) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            AtRiskDetailHeroSkeleton(isDark = isDark)
                        }
                    }
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            AtRiskStatStripSkeleton(isDark = isDark)
                        }
                    }
                    items(3) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            AtRiskIssueItemSkeleton(isDark = isDark)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Student Identity Hero Card (With Primary Send All Reminders Action)
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            StudentIdentityHeroCard(
                                studentName = state.studentName,
                                nisn = state.nisn,
                                className = state.className,
                                totalIssues = state.totalIssues,
                                isSendingAll = state.isSendingAll,
                                onSendAllReminders = { showSendAllConfirmDialog = true },
                                isDark = isDark,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                innerCardBg = innerCardBg,
                                innerCardBorder = innerCardBorder,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                textTertiary = textTertiary,
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
                    }

                    // 2. 4-Pillar Stat Grid
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            FourPillarStatStrip(
                                total = state.totalIssues,
                                unreadMaterials = state.unreadMaterialsCount,
                                overdueAssignments = state.overdueAssignmentsCount,
                                lowScores = state.lowScoresCount,
                                quizAlerts = state.quizAlertsCount,
                                isDark = isDark,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                textTertiary = textTertiary,
                            )
                        }
                    }

                    // 3. Category Filter Tabs (Edge-to-edge LazyRow)
                    item {
                        AtRiskCategoryFilterRow(
                            selected = state.selectedCategoryFilter,
                            total = state.totalIssues,
                            unreadMaterials = state.unreadMaterialsCount,
                            overdueAssignments = state.overdueAssignmentsCount,
                            scoresAndQuizzes = state.lowScoresCount + state.quizAlertsCount,
                            isDark = isDark,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            innerCardBg = innerCardBg,
                            innerCardBorder = innerCardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onSelect = viewModel::onSelectCategoryFilter
                        )
                    }

                    // 4. Issue Breakdown Cards
                    if (state.filteredItems.isEmpty()) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981).copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = "Tidak Ada Kendala Pada Kategori Ini",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Warga belajar terpantau aktif dan tidak memiliki masalah belajar pada filter yang dipilih.",
                                            fontSize = 12.sp,
                                            color = textSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = state.filteredItems,
                            key = { _, it -> "${it.studentId}_${it.category}_${it.materialId ?: it.assignmentId ?: it.quizId ?: it.title}" }
                        ) { _, item ->
                            val isSending = state.sendingItemId == "${item.studentId}_${item.category}_${item.materialId ?: item.assignmentId ?: item.quizId}"
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                AtRiskDetailItemCard(
                                    item = item,
                                    isSending = isSending,
                                    isDark = isDark,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    innerCardBg = innerCardBg,
                                    innerCardBorder = innerCardBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    textTertiary = textTertiary,
                                    onSendReminder = { singleItemToRemind = item },
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
                    }

                    // 5. Pedagogical Intervention Guidelines
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PedagogicalInterventionCard(
                                isDark = isDark,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                innerCardBg = innerCardBg,
                                innerCardBorder = innerCardBorder,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                textTertiary = textTertiary,
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUB-COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StudentIdentityHeroCard(
    studentName: String,
    nisn: String,
    className: String,
    totalIssues: Int,
    isSendingAll: Boolean,
    onSendAllReminders: () -> Unit,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    onOpenFullProfile: () -> Unit,
) {
    val initial = studentName.firstOrNull()?.uppercase() ?: "W"
    val hasIssues = totalIssues > 0

    val alertAccent = if (hasIssues) Color(0xFFEF4444) else Color(0xFF10B981)
    val cardBackground = if (isDark) {
        if (hasIssues) Color(0xFF151214) else Color(0xFF111113)
    } else {
        if (hasIssues) Color(0xFFFFFDFD) else Color.White
    }
    val cardOutline = if (hasIssues) {
        alertAccent.copy(alpha = if (isDark) 0.35f else 0.28f)
    } else {
        cardBorder
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBackground,
        border = BorderStroke(1.dp, cardOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar Box
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (hasIssues) {
                                if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.45f) else Color(0xFFFEE2E2)
                            } else {
                                if (isDark) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFDCFCE7)
                            }
                        )
                        .border(
                            1.dp,
                            if (hasIssues) alertAccent.copy(alpha = 0.4f) else Color(0xFF10B981).copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasIssues) (if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)) else (if (isDark) Color(0xFF6EE7B7) else Color(0xFF16A34A))
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = studentName.ifBlank { "Warga Belajar" },
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF),
                            border = BorderStroke(0.5.dp, if (isDark) Color(0xFF3730A3) else Color(0xFFC7D2FE))
                        ) {
                            Text(
                                text = className.ifBlank { "Rombel" },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "NISN: ${nisn.ifBlank { "-" }}",
                            fontSize = 11.5.sp,
                            color = textSecondary
                        )
                    }
                }

                // Alert Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = alertAccent.copy(alpha = if (isDark) 0.16f else 0.1f),
                    border = BorderStroke(0.5.dp, alertAccent.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(alertAccent)
                        )
                        Text(
                            text = if (hasIssues) "$totalIssues Kendala" else "Aman",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = alertAccent
                        )
                    }
                }
            }

            // PRIMARY ACTION BUTTON (Moved here from bottom sticky bar per user request)
            if (hasIssues) {
                Button(
                    onClick = onSendAllReminders,
                    enabled = !isSendingAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFDC2626) else Color(0xFFE11D48),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isSendingAll) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Mengirimkan Notifikasi...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Kirim Pengingat Terpadu ($totalIssues Kendala)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Direct link to student's profile & portfolio
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = innerCardBg,
                border = BorderStroke(0.5.dp, innerCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenFullProfile)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Badge,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Lihat Profil & Biodata Lengkap Warga Belajar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
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
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UnifiedStatColumnItem(
                label = "Kendala",
                value = "$total",
                accentColor = if (total > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(28.dp)
                    .background(cardBorder)
            )

            UnifiedStatColumnItem(
                label = "Modul",
                value = "$unreadMaterials",
                accentColor = Color(0xFFF59E0B),
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(28.dp)
                    .background(cardBorder)
            )

            UnifiedStatColumnItem(
                label = "Tugas",
                value = "$overdueAssignments",
                accentColor = Color(0xFF38BDF8),
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(28.dp)
                    .background(cardBorder)
            )

            UnifiedStatColumnItem(
                label = "Nilai / Kuis",
                value = "${lowScores + quizAlerts}",
                accentColor = Color(0xFFA855F7),
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UnifiedStatColumnItem(
    label: String,
    value: String,
    accentColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
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
                color = textSecondary,
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
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CategoryFilterChip(
                label = "Semua ($total)",
                isSelected = selected == "ALL",
                isDark = isDark,
                onClick = { onSelect("ALL") }
            )
        }
        item {
            CategoryFilterChip(
                label = "Modul Belajar ($unreadMaterials)",
                isSelected = selected == "UNREAD_MATERIAL",
                isDark = isDark,
                onClick = { onSelect("UNREAD_MATERIAL") }
            )
        }
        item {
            CategoryFilterChip(
                label = "Tugas Tertunda ($overdueAssignments)",
                isSelected = selected == "OVERDUE_ASSIGNMENT",
                isDark = isDark,
                onClick = { onSelect("OVERDUE_ASSIGNMENT") }
            )
        }
        item {
            CategoryFilterChip(
                label = "Nilai & Kuis ($scoresAndQuizzes)",
                isSelected = selected == "LOW_SCORE" || selected == "QUIZ_ALERT",
                isDark = isDark,
                onClick = { onSelect("LOW_SCORE") }
            )
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val activeColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val inactiveBg = if (isDark) Color(0xFF161618) else Color(0xFFF1F5F9)
    val inactiveBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val inactiveText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.15f) else inactiveBg,
        border = BorderStroke(1.dp, if (isSelected) activeColor else inactiveBorder),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun AtRiskDetailItemCard(
    item: AtRiskStudent,
    isSending: Boolean,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    onSendReminder: () -> Unit,
    onOpenItem: () -> Unit,
) {
    val (badgeText, badgeColor, categoryIcon) = when (item.category) {
        "UNREAD_MATERIAL" -> Triple("Modul Literasi Tertinggal", Color(0xFFF59E0B), Icons.Outlined.MenuBook)
        "OVERDUE_ASSIGNMENT" -> Triple("Tugas Belum Dikumpulkan", Color(0xFFEF4444), Icons.AutoMirrored.Outlined.Assignment)
        "LOW_SCORE" -> Triple("Nilai di Bawah KKTP (< 70)", Color(0xFFE11D48), Icons.Outlined.TrendingDown)
        "QUIZ_ALERT" -> Triple("Kuis & Ujian CBT Terlewat", Color(0xFFA855F7), Icons.Outlined.Timer)
        else -> Triple("Perlu Perhatian", Color(0xFFEF4444), Icons.Outlined.WarningAmber)
    }

    val actionButtonText = when {
        item.materialId != null -> "Buka Modul"
        item.assignmentId != null -> "Buka Tugas"
        item.quizId != null -> "Buka Kuis"
        else -> null
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Category Badge + Risk Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                    )
                }

                val isHighRisk = item.riskLevel.equals("HIGH", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isHighRisk) Color(0xFFEF4444).copy(alpha = 0.14f) else Color(0xFFF59E0B).copy(alpha = 0.14f))
                        .border(
                            0.5.dp,
                            if (isHighRisk) Color(0xFFEF4444).copy(alpha = 0.45f) else Color(0xFFF59E0B).copy(alpha = 0.45f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isHighRisk) "Prioritas Tinggi" else "Perhatian Sedang",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighRisk) Color(0xFFEF4444) else Color(0xFFF59E0B),
                    )
                }
            }

            // Title
            Text(
                text = item.title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                lineHeight = 19.sp
            )

            // Diagnostic Explanation Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = innerCardBg,
                border = BorderStroke(0.5.dp, innerCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 1.dp)
                    )
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        color = textPrimary,
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (actionButtonText != null) {
                    OutlinedButton(
                        onClick = onOpenItem,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.75.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(actionButtonText, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                // Send Single Reminder Button
                Button(
                    onClick = onSendReminder,
                    enabled = !isSending,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF),
                        contentColor = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                    ),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Mengirim...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Kirim Pengingat",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PedagogicalInterventionCard(
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Panduan Tindakan Pedagogis Guru / Kepala Sekolah",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }

            Spacer(Modifier.height(2.dp))

            InterventionStepItem(
                step = "1",
                title = "Notifikasi & Pengingat Instan",
                text = "Kirimkan pengingat terpadu agar warga belajar menerima peringatan langsung pada perangkat smartphone.",
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
            )
            InterventionStepItem(
                step = "2",
                title = "Penugasan Remedial & Pengayaan",
                text = "Berikan penugasan remedial fleksibel bagi materi atau topik kuis yang belum melampaui kriteria KKTP 70.",
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
            )
            InterventionStepItem(
                step = "3",
                title = "Konseling Personal & Bimbingan Tutor",
                text = "Lakukan koordinasi bimbingan belajar dengan tutor pengampu atau wali apabila kendala berlanjut lebih dari 3 hari.",
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
            )
        }
    }
}

@Composable
private fun InterventionStepItem(
    step: String,
    title: String,
    text: String,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = text,
                fontSize = 11.5.sp,
                color = textSecondary,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun AtRiskDetailHeroSkeleton(isDark: Boolean) {
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(modifier = Modifier.size(52.dp), shape = RoundedCornerShape(16.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(modifier = Modifier.width(150.dp).height(16.dp), shape = RoundedCornerShape(4.dp))
                    ShimmerBox(modifier = Modifier.width(100.dp).height(12.dp), shape = RoundedCornerShape(4.dp))
                }
                ShimmerBox(modifier = Modifier.width(70.dp).height(24.dp), shape = RoundedCornerShape(8.dp))
            }
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(12.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(10.dp))
        }
    }
}

@Composable
private fun AtRiskStatStripSkeleton(isDark: Boolean) {
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            repeat(4) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShimmerBox(modifier = Modifier.width(36.dp).height(18.dp), shape = RoundedCornerShape(4.dp))
                    ShimmerBox(modifier = Modifier.width(48.dp).height(12.dp), shape = RoundedCornerShape(4.dp))
                }
            }
        }
    }
}

@Composable
private fun AtRiskIssueItemSkeleton(isDark: Boolean) {
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(modifier = Modifier.width(130.dp).height(16.dp), shape = RoundedCornerShape(6.dp))
                ShimmerBox(modifier = Modifier.width(80.dp).height(16.dp), shape = RoundedCornerShape(6.dp))
            }
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f).height(16.dp), shape = RoundedCornerShape(4.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ShimmerBox(modifier = Modifier.width(90.dp).height(34.dp), shape = RoundedCornerShape(10.dp))
                ShimmerBox(modifier = Modifier.width(110.dp).height(34.dp), shape = RoundedCornerShape(10.dp))
            }
        }
    }
}
