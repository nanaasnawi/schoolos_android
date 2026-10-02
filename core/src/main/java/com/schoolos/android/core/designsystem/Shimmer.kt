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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * High-performance Luminous Shimmer Brush.
 * Provides a dynamic specular sweep with balanced contrast across dark and light themes.
 */
@Composable
fun ShimmerBrush(
    targetValue: Float = 1600f,
    showShimmer: Boolean = true,
): Brush {
    if (!showShimmer) return Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    val isDark = LocalIsDarkTheme.current
    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF181A22),
            Color(0xFF222634),
            Color(0xFF333A4E),
            Color(0xFF48536F), // Luminous crest
            Color(0xFF333A4E),
            Color(0xFF222634),
            Color(0xFF181A22),
        )
    } else {
        listOf(
            Color(0xFFE2E8F0),
            Color(0xFFCBD5E1),
            Color(0xFFE2E8F0),
            Color(0xFFFFFFFF), // Crisp specular gleam
            Color(0xFFE2E8F0),
            Color(0xFFCBD5E1),
            Color(0xFFE2E8F0),
        )
    }

    // Angled 20-degree light sweep
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim, translateAnim * 0.35f),
        end = Offset(translateAnim + 400f, (translateAnim + 400f) * 0.35f),
    )
}

/**
 * Standard placeholder block animated with ShimmerBrush.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(ShimmerBrush())
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
