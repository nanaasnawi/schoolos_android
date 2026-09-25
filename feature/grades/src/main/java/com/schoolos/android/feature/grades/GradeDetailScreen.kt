package com.schoolos.android.feature.grades

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface
import com.schoolos.android.core.designsystem.ErrorState
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.LoadingState
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.StatusChip
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.core.designsystem.subjectGradient
import com.schoolos.android.core.designsystem.subjectIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeDetailScreen(
    onBack: () -> Unit = {},
    viewModel: GradeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val detail = state.detail
    val role = state.userRole.lowercase()
    val isTeacher = role == "teacher" || role == "guru"
    val subjectName = detail?.summary?.subjectName ?: "Detail Akademik"

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = subjectName,
                subtitle = if (isTeacher) "Laporan Nilai • ${DapodikPeriod.getFullPeriodLabel()}" else "Detail Nilai • ${DapodikPeriod.getFullPeriodLabel()}",
                onBack = onBack,
                actions = {
                    if (isTeacher) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TeacherNeon.copy(alpha = 0.12f))
                                .border(0.5.dp, TeacherNeon.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("GURU", color = TeacherNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ── 1. COMPACT EXECUTIVE SUBJECT HERO CARD ────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(CosmicNavy, CosmicSurface)
                                    )
                                )
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(gradient))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
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
                                        color = TextTertiary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = d.summary.subjectName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = TextTertiary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = DapodikPeriod.getFullPeriodLabel(),
                                            fontSize = 11.sp,
                                            color = TextSecondary,
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