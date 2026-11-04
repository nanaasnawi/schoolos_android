package com.schoolos.android.feature.grades

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.common.DapodikPeriod
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.core.auth.isPrincipalRole
import com.schoolos.android.core.auth.isTeacherRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeDetailScreen(
    onBack: () -> Unit = {},
    viewModel: GradeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val detail = state.detail
    val isPrincipal = isPrincipalRole(state.userRole)
    val isTeacher = isTeacherRole(state.userRole) || isPrincipal
    val subjectName = detail?.summary?.subjectName ?: "Detail Akademik"

    val isDark = LocalIsDarkTheme.current
    val screenBg = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    Scaffold(
        containerColor = screenBg,
        topBar = {
            ExecutiveTopBar(
                title = subjectName,
                subtitle = if (isPrincipal) "Leger Nilai & Capaian KKTP • ${DapodikPeriod.getFullPeriodLabel()}"
                    else if (isTeacher) "Laporan Nilai • ${DapodikPeriod.getFullPeriodLabel()}"
                    else "Detail Nilai • ${DapodikPeriod.getFullPeriodLabel()}",
                onBack = onBack,
                actions = {
                    if (isPrincipal || isTeacher) {
                        val badgeBg = if (isDark) TeacherNeon.copy(alpha = 0.15f) else Color(0xFFECFDF5)
                        val badgeBorder = if (isDark) TeacherNeon.copy(alpha = 0.4f) else Color(0xFFA7F3D0)
                        val badgeText = if (isDark) TeacherNeon else Color(0xFF047857)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeBg)
                                .border(0.5.dp, badgeBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                com.schoolos.android.core.auth.resolveRoleDisplay(state.userRole, isFormalUpper = true),
                                color = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        StatusChip(label = "Aktif")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> {
                    ErrorState(message = state.error!!, onRetry = viewModel::load)
                }
                detail != null -> {
                    val d = detail
                    val gradient = subjectGradient(d.summary.subjectName)
                    val icon = subjectIcon(d.summary.subjectName)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── 1. COMPACT EXECUTIVE SUBJECT HERO CARD ────────────
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(gradient)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isTeacher) "LAPORAN NILAI KELAS" else "DETAIL AKADEMIK MAPEL",
                                        color = textTertiary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = d.summary.subjectName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textPrimary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = CoreR.drawable.ic_modern_calendar),
                                            contentDescription = null,
                                            tint = textTertiary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = DapodikPeriod.getFullPeriodLabel(),
                                            fontSize = 11.5.sp,
                                            color = textSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // ── 2. MAIN GRADE DETAIL CONTENT ─────────────────────
                        if (isTeacher) {
                            TeacherGradeDetailContent(detail = d)
                        } else {
                            StudentGradeDetailContent(detail = d)
                        }

                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}
