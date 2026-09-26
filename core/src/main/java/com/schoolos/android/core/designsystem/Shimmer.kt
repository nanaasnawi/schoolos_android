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

@Composable
fun ShimmerBrush(
    targetValue: Float = 1200f,
    showShimmer: Boolean = true,
): Brush {
    if (!showShimmer) return Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )
    val isDark = LocalIsDarkTheme.current
    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF18181B).copy(alpha = 0.55f),
            Color(0xFF27272A).copy(alpha = 0.95f),
            Color(0xFF18181B).copy(alpha = 0.55f),
        )
    } else {
        listOf(
            Color(0xFFE2E8F0).copy(alpha = 0.55f),
            Color(0xFFF8FAFC).copy(alpha = 0.95f),
            Color(0xFFE2E8F0).copy(alpha = 0.55f),
        )
    }
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - targetValue, 0.0f),
        end = Offset(translateAnim, 0.0f),
    )
}

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

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBox(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.65f).height(15.dp))
                Spacer(Modifier.height(8.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.35f).height(12.dp))
            }
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
