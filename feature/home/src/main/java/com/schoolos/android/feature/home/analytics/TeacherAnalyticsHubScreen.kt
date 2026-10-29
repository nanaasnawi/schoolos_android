package com.schoolos.android.feature.home.analytics

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicDark
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.feature.grades.GradebookListScreen
import com.schoolos.android.feature.grades.GradebookListViewModel
import com.schoolos.android.feature.home.AtRiskMonitoringScreen
import com.schoolos.android.feature.home.AtRiskMonitoringViewModel

@Composable
fun TeacherAnalyticsHubScreen(
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null,
    onNavigateToStudentDetail: ((studentId: String, name: String, nisn: String, className: String) -> Unit)? = null,
    onNavigateToGradeDetail: ((subjectId: String, subjectName: String) -> Unit)? = null,
    readingViewModel: TeacherReadingAnalyticsViewModel = hiltViewModel(),
    atRiskViewModel: AtRiskMonitoringViewModel = hiltViewModel(),
    gradebookViewModel: GradebookListViewModel = hiltViewModel(),
) {
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    val readingState by readingViewModel.state.collectAsState()
    val atRiskState by atRiskViewModel.state.collectAsState()

    LaunchedEffect(readingState.userMessage) {
        readingState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            readingViewModel.clearUserMessage()
        }
    }

    LaunchedEffect(atRiskState.userMessage) {
        atRiskState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            atRiskViewModel.clearUserMessage()
        }
    }

    Scaffold(
        containerColor = CosmicBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ExecutiveTopBar(
                title = "Teacher Analytics & Supervisi",
                subtitle = "Monitoring Literasi, Early Warning & Nilai",
                onBack = onBack,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── TOP SEGMENTED SWITCHER BAR ──────────────────────────────────
            SegmentedHubTabs(
                selectedTab = selectedTab,
                atRiskCount = atRiskState.stats.total,
                isDark = isDark,
                onSelectTab = { selectedTab = it }
            )

            // ── TAB CONTENT CROSSFADE ────────────────────────────────────────
            Crossfade(
                targetState = selectedTab,
                label = "HubTabTransition"
            ) { tab ->
                when (tab) {
                    0 -> {
                        TeacherReadingAnalyticsContent(
                            state = readingState,
                            isDark = isDark,
                            onRefresh = readingViewModel::refresh,
                            onExportCsv = { readingViewModel.exportCsv(context) },
                            onSelectClassFilter = readingViewModel::onSelectClassFilter,
                            onSelectMaterialFilter = readingViewModel::onSelectMaterialFilter,
                            onSelectStatusFilter = readingViewModel::onSelectStatusFilter,
                            onSearchQueryChange = readingViewModel::onSearchQueryChange,
                            onNavigateToStudentDetail = onNavigateToStudentDetail,
                        )
                    }
                    1 -> {
                        AtRiskMonitoringScreen(
                            showHeader = false,
                            onBack = onBack,
                            onNavigateToStudentDetail = onNavigateToStudentDetail,
                            viewModel = atRiskViewModel,
                        )
                    }
                    2 -> {
                        GradebookListScreen(
                            showHeader = false,
                            onBack = onBack,
                            onSubjectClick = onNavigateToGradeDetail ?: { _, _ -> },
                            viewModel = gradebookViewModel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedHubTabs(
    selectedTab: Int,
    atRiskCount: Int,
    isDark: Boolean,
    onSelectTab: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) CosmicDark else Color(0xFFF1F5F9))
            .border(0.6.dp, if (isDark) GlassBorder else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 0: Literasi
            HubTabItem(
                title = "📊 Literasi & Baca",
                isSelected = selectedTab == 0,
                activeColor = Color(0xFFA855F7),
                isDark = isDark,
                onClick = { onSelectTab(0) },
                modifier = Modifier.weight(1f)
            )

            // Tab 1: Early Warning
            HubTabItem(
                title = "⚠️ Early Warning",
                badgeCount = atRiskCount,
                isSelected = selectedTab == 1,
                activeColor = NeonError,
                isDark = isDark,
                onClick = { onSelectTab(1) },
                modifier = Modifier.weight(1.1f)
            )

            // Tab 2: Buku Nilai
            HubTabItem(
                title = "📖 Buku Nilai",
                isSelected = selectedTab == 2,
                activeColor = NeonBlue,
                isDark = isDark,
                onClick = { onSelectTab(2) },
                modifier = Modifier.weight(0.9f)
            )
        }
    }
}

@Composable
private fun HubTabItem(
    title: String,
    badgeCount: Int? = null,
    isSelected: Boolean,
    activeColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) CosmicNavy else Color.White
        } else Color.Transparent,
        label = "TabBg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = bgColor,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) {
                    if (isDark) TextPrimary else Color(0xFF0F172A)
                } else {
                    if (isDark) TextSecondary else Color(0xFF64748B)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (badgeCount != null && badgeCount > 0) {
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonError)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badgeCount.toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

