package com.schoolos.android.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    val size: Float,
    val color: Color,
    val isCircle: Boolean,
)

private val CONFETTI_COLORS = listOf(
    Color(0xFFF59E0B), // Cyber Gold
    Color(0xFF10B981), // Emerald
    Color(0xFF3B82F6), // Neon Blue
    Color(0xFFEC4899), // Neon Pink
    Color(0xFF8B5CF6), // Electric Purple
    Color(0xFFF43F5E), // Coral Crimson
    Color(0xFF06B6D4), // Cyan
)

@Composable
fun ConfettiParticleEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 90,
    durationMs: Int = 2500,
    onFinished: (() -> Unit)? = null,
) {
    val progress = remember { Animatable(0f) }

    val particles = remember {
        List(particleCount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 18f + 8f
            ConfettiParticle(
                x = 0f,
                y = 0f,
                vx = kotlin.math.cos(angle) * speed * (Random.nextFloat() * 0.8f + 0.6f),
                vy = kotlin.math.sin(angle) * speed - (Random.nextFloat() * 12f + 6f),
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 24f,
                size = Random.nextFloat() * 12f + 10f,
                color = CONFETTI_COLORS.random(),
                isCircle = Random.nextBoolean(),
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMs, easing = LinearEasing),
        )
        onFinished?.invoke()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height * 0.42f
        val currentProgress = progress.value
        val alpha = if (currentProgress > 0.65f) {
            ((1f - currentProgress) / 0.35f).coerceIn(0f, 1f)
        } else 1f

        particles.forEach { p ->
            val t = currentProgress * 60f // simulated frames
            val px = centerX + p.vx * t
            val py = centerY + p.vy * t + 0.5f * 0.65f * t * t // gravity acceleration
            val rot = p.rotation + p.rotationSpeed * t

            rotate(rot, pivot = Offset(px, py)) {
                if (p.isCircle) {
                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = p.size / 2f,
                        center = Offset(px, py),
                    )
                } else {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(px - p.size / 2f, py - (p.size * 0.6f) / 2f),
                        size = Size(p.size, p.size * 0.6f),
                    )
                }
            }
        }
    }
}
