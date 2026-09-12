package com.schoolos.android.feature.auth.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.DynamicSchoolLogo
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.SchoolOsBrandLogo
import com.schoolos.android.core.designsystem.TextPrimary

@Composable
fun LoginHeaderSection(
    schoolName: String?,
    schoolLogoUrl: String?,
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "headerTransition")
    val auraAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "headerAura",
    )
    val particlePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "headerPulse",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // School Crest / App Logo with Ambient Glow
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.scale(0.96f + auraAnim * 0.04f),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.40f * particlePulse),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .shadow(20.dp, CircleShape, spotColor = accentColor.copy(alpha = 0.45f))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                CosmicNavy,
                                CosmicSurface2,
                            ),
                        ),
                    )
                    .border(
                        2.5.dp,
                        Brush.sweepGradient(
                            listOf(
                                accentColor,
                                secondaryColor,
                                NeonBlue,
                                accentColor,
                            ),
                        ),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                DynamicSchoolLogo(
                    logoUrl = schoolLogoUrl,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape),
                    fallback = {
                        SchoolOsBrandLogo(size = 80)
                    },
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = schoolName ?: "School OS",
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            color = TextPrimary,
            letterSpacing = (-0.8).sp,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "SISTEM INFORMASI AKADEMIK",
            fontSize = 13.sp,
            color = accentColor,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
