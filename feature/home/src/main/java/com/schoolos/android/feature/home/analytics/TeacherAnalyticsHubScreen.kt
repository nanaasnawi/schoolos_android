package com.schoolos.android.feature.home.analytics

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.feature.home.AtRiskMonitoringScreen
import com.schoolos.android.feature.home.AtRiskMonitoringViewModel

@Composable
fun TeacherAnalyticsHubScreen(
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null,
    onNavigateToStudentDetail: ((studentId: String, name: String, nisn: String, className: String) -> Unit)? = null,
    readingViewModel: TeacherReadingAnalyticsViewModel = hiltViewModel(),
    atRiskViewModel: AtRiskMonitoringViewModel = hiltViewModel(),
) {
    val isDark = LocalIsDarkTheme.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Strictly 2 tabs: 0 for Literasi, 1 for Early Warning
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 1)) }

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
                subtitle = "Monitoring Literasi & Early Warning Siswa",
                onBack = onBack,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── TOP SEGMENTED SWITCHER BAR (MINIMALIST, ZERO EMOJI, ZERO GRADIENT) ──
            SegmentedHubTabs(
                selectedTab = selectedTab,
                atRiskCount = atRiskState.stats.total,
                isDark = isDark,
                onSelectTab = { selectedTab = it }
            )

            Spacer(Modifier.height(4.dp))

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
            .border(0.8.dp, if (isDark) GlassBorder else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 0: Literasi & Bacaan (Zero Emoji)
            HubTabItem(
                title = "Literasi & Bacaan",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                isSelected = selectedTab == 0,
                isDark = isDark,
                onClick = { onSelectTab(0) },
                modifier = Modifier.weight(1f)
            )

            // Tab 1: Early Warning (Zero Emoji)
            HubTabItem(
                title = "Early Warning",
                icon = Icons.Default.Warning,
                badgeCount = atRiskCount,
                isSelected = selectedTab == 1,
                isDark = isDark,
                onClick = { onSelectTab(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HubTabItem(
    title: String,
    icon: ImageVector,
    badgeCount: Int? = null,
    isSelected: Boolean,
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
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                0.8.dp,
                if (isDark) GlassBorder else Color(0xFFE2E8F0)
            )
        } else null,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 9.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) {
                    if (isDark) TextPrimary else Color(0xFF0F172A)
                } else {
                    if (isDark) TextSecondary else Color(0xFF64748B)
                },
                modifier = Modifier.size(15.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    if (isDark) TextPrimary else Color(0xFF0F172A)
                } else {
                    if (isDark) TextSecondary else Color(0xFF64748B)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (badgeCount != null && badgeCount > 0) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonError)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badgeCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
