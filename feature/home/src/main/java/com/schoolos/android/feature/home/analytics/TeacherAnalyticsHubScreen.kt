package com.schoolos.android.feature.home.analytics

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.schoolos.android.core.designsystem.*
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

    // Strictly 2 tabs: 0 for Literasi & Bacaan, 1 for Early Warning
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        color = CosmicSurface2,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
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
        targetValue = if (isSelected) CosmicNavy else Color.Transparent,
        label = "TabBg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = bgColor,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = if (isSelected) {
            BorderStroke(0.5.dp, GlassBorder)
        } else null,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) {
                    if (title.contains("Early", ignoreCase = true) && (badgeCount ?: 0) > 0) NeonError else NeonBlue
                } else TextSecondary,
                modifier = Modifier.size(15.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TextPrimary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (badgeCount != null && badgeCount > 0) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
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
