package com.schoolos.android.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * High-performance Luminous Shimmer Brush.
 * Provides a dynamic specular sweep with balanced contrast across dark and light themes.
 *
 * The sweep is computed in screen space: pass the placeholder's position in root as
 * [origin] so that every placeholder on screen shares one continuous highlight band.
 */
@Composable
fun ShimmerBrush(
    targetValue: Float? = null,
    showShimmer: Boolean = true,
    origin: Offset = Offset.Zero,
): Brush {
    if (!showShimmer) return Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))

    val bandWidth = 520f
    val config = LocalConfiguration.current
    val (screenWidthPx, screenHeightPx) = with(LocalDensity.current) {
        config.screenWidthDp.dp.toPx() to config.screenHeightDp.dp.toPx()
    }
    // The band is angled, so placeholders lower on screen are reached later; extend the sweep
    // by the vertical component so the band fully clears the bottom-right corner too.
    val endValue = targetValue ?: (screenWidthPx + screenHeightPx * 0.35f + bandWidth)

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -bandWidth,
        targetValue = endValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    val isDark = LocalIsDarkTheme.current
    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF1C1F2A),
            Color(0xFF1C1F2A),
            Color(0xFF2A3042),
            Color(0xFF3D4760), // Luminous crest
            Color(0xFF2A3042),
            Color(0xFF1C1F2A),
            Color(0xFF1C1F2A),
        )
    } else {
        listOf(
            Color(0xFFE2E8F0),
            Color(0xFFE2E8F0),
            Color(0xFFEDF1F6),
            Color(0xFFFFFFFF), // Crisp specular gleam
            Color(0xFFEDF1F6),
            Color(0xFFE2E8F0),
            Color(0xFFE2E8F0),
        )
    }

    // Angled ~20-degree light sweep, expressed in screen space then shifted into local space.
    val start = Offset(translateAnim, translateAnim * 0.35f) - origin
    val end = Offset(translateAnim + bandWidth, (translateAnim + bandWidth) * 0.35f) - origin
    return Brush.linearGradient(colors = shimmerColors, start = start, end = end)
}

/**
 * Standard placeholder block animated with ShimmerBrush.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .clip(shape)
            .background(ShimmerBrush(origin = origin))
    )
}

/**
 * Authentic, rich card skeleton mimicking modern mobile list cards.
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(
                width = 1.dp,
                color = if (isDark) GlassBorder else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            // Top Row: Category tag + Score/Status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(modifier = Modifier.width(72.dp).height(18.dp), shape = RoundedCornerShape(6.dp))
                ShimmerBox(modifier = Modifier.width(48.dp).height(18.dp), shape = RoundedCornerShape(6.dp))
            }

            Spacer(Modifier.height(12.dp))

            // Middle Row: Rounded Icon + Title & Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(10.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.72f).height(15.dp), shape = RoundedCornerShape(4.dp))
                    Spacer(Modifier.height(8.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.42f).height(12.dp), shape = RoundedCornerShape(4.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            // Bottom Row: Date pill & Status chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicSurface2)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(modifier = Modifier.size(12.dp), shape = CircleShape)
                    Spacer(Modifier.width(6.dp))
                    ShimmerBox(modifier = Modifier.width(84.dp).height(10.dp), shape = RoundedCornerShape(3.dp))
                }
                ShimmerBox(modifier = Modifier.width(56.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
            }
        }
    }
}

/**
 * Immersive Hero Header skeleton for list screens.
 */
@Composable
fun ShimmerHeroHeader(
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(
                width = 1.dp,
                color = if (isDark) GlassBorder else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(modifier = Modifier.width(110.dp).height(12.dp), shape = RoundedCornerShape(4.dp))
                ShimmerBox(modifier = Modifier.width(76.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
            }
            Spacer(Modifier.height(12.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f).height(18.dp), shape = RoundedCornerShape(4.dp))
            Spacer(Modifier.height(6.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.78f).height(12.dp), shape = RoundedCornerShape(4.dp))
            Spacer(Modifier.height(14.dp))
            // Progress bar skeleton
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(6.dp), shape = RoundedCornerShape(3.dp))
        }
    }
}

/**
 * Dedicated Bento Shimmer Skeleton for HomeScreen initial loading state.
 */
@Composable
fun HomeShimmerSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Top Bar Shimmer
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(10.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    ShimmerBox(modifier = Modifier.width(120.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
                    Spacer(Modifier.height(4.dp))
                    ShimmerBox(modifier = Modifier.width(160.dp).height(11.dp), shape = RoundedCornerShape(4.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(modifier = Modifier.size(42.dp), shape = CircleShape)
                ShimmerBox(modifier = Modifier.size(42.dp), shape = CircleShape)
            }
        }

        // 2. Hero Spotlight Card Shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicNavy)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShimmerBox(modifier = Modifier.width(100.dp).height(12.dp), shape = RoundedCornerShape(4.dp))
                    ShimmerBox(modifier = Modifier.width(80.dp).height(14.dp), shape = RoundedCornerShape(6.dp))
                }
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.65f).height(18.dp), shape = RoundedCornerShape(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShimmerBox(modifier = Modifier.width(140.dp).height(12.dp), shape = RoundedCornerShape(4.dp))
                    ShimmerBox(modifier = Modifier.width(70.dp).height(24.dp), shape = RoundedCornerShape(6.dp))
                }
            }
        }

        // 3. Bento Grid Shimmer (2x2 Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShimmerBox(modifier = Modifier.weight(1f).height(108.dp), shape = RoundedCornerShape(14.dp))
            ShimmerBox(modifier = Modifier.weight(1f).height(108.dp), shape = RoundedCornerShape(14.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShimmerBox(modifier = Modifier.weight(1f).height(108.dp), shape = RoundedCornerShape(14.dp))
            ShimmerBox(modifier = Modifier.weight(1f).height(108.dp), shape = RoundedCornerShape(14.dp))
        }

        // 4. Schedule Section Header & Card Shimmers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBox(modifier = Modifier.width(130.dp).height(16.dp), shape = RoundedCornerShape(4.dp))
            ShimmerBox(modifier = Modifier.width(70.dp).height(14.dp), shape = RoundedCornerShape(4.dp))
        }

        ShimmerCard()
        ShimmerCard()
    }
}

/**
 * Filter Chip Row skeleton.
 */
@Composable
fun ShimmerFilterChips(
    count: Int = 4,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val widths = listOf(68.dp, 60.dp, 80.dp, 72.dp)
        repeat(count) { index ->
            val width = widths[index % widths.size]
            ShimmerBox(
                modifier = Modifier.width(width).height(32.dp),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun ShimmerList(
    count: Int = 4,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(count) {
            ShimmerCard()
        }
    }
}
