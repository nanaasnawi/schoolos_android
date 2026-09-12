package com.schoolos.android.feature.auth.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.schoolos.android.core.designsystem.NeonBlue

@Composable
fun LoginBackgroundCanvas(
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "auras")
    val auraAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "auraFloat",
    )
    val auraAnim2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "auraFloat2",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.18f * (0.7f + auraAnim * 0.3f)),
                    Color.Transparent,
                ),
                center = Offset(-120f + auraAnim * 90f, -60f + auraAnim * 60f),
                radius = 650f,
            ),
            radius = 650f,
            center = Offset(-120f + auraAnim * 90f, -60f + auraAnim * 60f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = 0.14f * (0.6f + auraAnim2 * 0.4f)),
                    Color.Transparent,
                ),
                center = Offset(size.width + 80f - auraAnim2 * 70f, 220f + auraAnim2 * 50f),
                radius = 520f,
            ),
            radius = 520f,
            center = Offset(size.width + 80f - auraAnim2 * 70f, 220f + auraAnim2 * 50f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonBlue.copy(alpha = 0.10f * (0.8f + auraAnim * 0.2f)), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.88f - auraAnim * 50f),
                radius = 480f,
            ),
            radius = 480f,
            center = Offset(size.width * 0.85f, size.height * 0.88f - auraAnim * 50f),
        )
    }
}
