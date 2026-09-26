package com.schoolos.android.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.schoolos.android.core.R as CoreR
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.auth.AuthState
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassOverlay
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.ParentNeon
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.navigation.Screen

data class BottomNavItem(
    val route: String,
    val label: String,
    @DrawableRes val iconRes: Int,
    val badgeCount: Int = 0,
)

@Composable
fun MainContainerScreen(
    authManager: AuthManager,
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit,
) {
    val authState by authManager.authState.collectAsState(initial = AuthState())
    val isParent = authState.isParent
    val isTeacher = authState.isTeacher
    val isDark = LocalIsDarkTheme.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Role-based accent colors (vibrant primary matching active state)
    val activeNeon = when {
        isTeacher -> TeacherNeon
        isParent  -> ParentNeon
        else      -> StudentNeon
    }

    // Role-based nav items with modern custom vector icons
    val navItems: List<BottomNavItem> = when {
        isTeacher -> listOf(
            BottomNavItem(Screen.Home.route,        "Beranda",     CoreR.drawable.ic_modern_home),
            BottomNavItem(Screen.Sessions.route,    "Kelas",       CoreR.drawable.ic_modern_classes),
            BottomNavItem(Screen.Assignments.route, "Tugas",       CoreR.drawable.ic_modern_tasks),
            BottomNavItem(Screen.Chat.route,        "Tanya Jawab", CoreR.drawable.ic_modern_chat),
            BottomNavItem(Screen.Profile.route,     "Akun",        CoreR.drawable.ic_modern_profile),
        )
        isParent -> listOf(
            BottomNavItem(Screen.Home.route,          "Beranda",  CoreR.drawable.ic_modern_home),
            BottomNavItem(Screen.Progress.route,      "Anak",     CoreR.drawable.ic_modern_child),
            BottomNavItem(Screen.Notifications.route, "Pesan",    CoreR.drawable.ic_modern_chat),
            BottomNavItem(Screen.Profile.route,       "Profil",   CoreR.drawable.ic_modern_profile),
        )
        else -> listOf(
            BottomNavItem(Screen.Home.route,          "Beranda",     CoreR.drawable.ic_modern_home),
            BottomNavItem(Screen.Sessions.route,      "Jadwal",      CoreR.drawable.ic_modern_calendar),
            BottomNavItem(Screen.Assignments.route,   "Tugas",       CoreR.drawable.ic_modern_tasks),
            BottomNavItem(Screen.Chat.route,          "Tanya Jawab", CoreR.drawable.ic_modern_chat),
            BottomNavItem(Screen.Profile.route,       "Akun",        CoreR.drawable.ic_modern_profile),
        )
    }

    // Determine if we should show the bottom bar
    val topLevelRoutes = navItems.map { it.route }.toSet()
    val hideBottomBar = currentRoute == null || currentRoute !in topLevelRoutes

    Scaffold(
        containerColor = CosmicBlack,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = !hideBottomBar,
                enter = slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(220)),
                exit  = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
            ) {
                // Flat Docked Modern Navigation Bar (Ordinary/Flat corners, subtle top divider)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = androidx.compose.ui.graphics.RectangleShape,
                            spotColor = if (isDark) GlassOverlay else Color(0x14000000),
                            ambientColor = if (isDark) GlassOverlay else Color(0x0A000000),
                        ),
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    color = if (isDark) CosmicNavy else Color.White,
                    tonalElevation = 2.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                    ) {
                        // Top divider line (clean, sharp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(if (isDark) GlassBorder else Color(0xFFE2E8F0))
                                .align(Alignment.TopCenter),
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                ModernNavItem(
                                    item = item,
                                    selected = selected,
                                    activeColor = activeNeon,
                                    isDark = isDark,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            content(innerPadding)
        }
    }
}

@Composable
private fun ModernNavItem(
    item: BottomNavItem,
    selected: Boolean,
    activeColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    // Spring scale pop on selection / tap
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "navIconScale",
    )

    // Smooth color animation for icon and text
    val inactiveColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val animatedColor by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 180),
        label = "navColor",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Icon container with soft active background capsule
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(activeColor.copy(alpha = if (isDark) 0.16f else 0.12f)),
                )
            }

            if (item.badgeCount > 0) {
                BadgedBox(
                    badge = {
                        Badge(containerColor = NeonError) {
                            Text(
                                text = "${item.badgeCount}",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = item.label,
                        tint = animatedColor,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                    )
                }
            } else {
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = item.label,
                    tint = animatedColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
            }
        }

        Spacer(Modifier.height(1.dp))

        // Label under icon (Clean compact typography, snugly paired with icon)
        Text(
            text = item.label,
            fontSize = 9.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = animatedColor,
            maxLines = 1,
            letterSpacing = 0.sp,
        )
    }
}
