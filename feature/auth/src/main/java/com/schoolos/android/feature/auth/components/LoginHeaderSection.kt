package com.schoolos.android.feature.auth.components

import com.schoolos.android.core.R as CoreR
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.DynamicSchoolLogo
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.SchoolOsBrandLogo
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary

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
        animationSpec = infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "headerAura",
    )
    val particlePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "headerPulse",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 1. Top Greeting (Welcome Back! - Left aligned matching reference mockup)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Welcome ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = accentColor,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = "Back!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp,
                )
            }
            Spacer(Modifier.height(4.dp))

            // Subtitle with slightly bold school name (PKBM AS-SALAFIYAH)
            val displaySchoolName = schoolName?.takeIf { it.isNotBlank() } ?: "PKBM AS-SALAFIYAH"
            val schoolTextColor = TextPrimary
            val annotatedGreeting = remember(displaySchoolName, schoolTextColor) {
                buildAnnotatedString {
                    append("Masuk untuk mengakses materi & tugas di ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = schoolTextColor)) {
                        append(displaySchoolName)
                    }
                }
            }
            Text(
                text = annotatedGreeting,
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(14.dp))

        // 2. 3D Student Illustration with Grand School Logo Backdrop (Behind head, space-saving)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(235.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Ambient Radial Glow in Background
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.Center)
                    .offset(x = (-20).dp, y = (-5).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                accentColor.copy(alpha = 0.28f * particlePulse),
                                secondaryColor.copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            // Grand School Logo Crest (Substantially Enlarged to 156dp, sitting behind student's shoulder & head)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = (-26).dp, y = 8.dp)
                    .size(156.dp)
                    .scale(0.97f + auraAnim * 0.03f)
                    .shadow(16.dp, CircleShape, spotColor = accentColor.copy(alpha = 0.40f))
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
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                DynamicSchoolLogo(
                    logoUrl = schoolLogoUrl,
                    modifier = Modifier
                        .size(142.dp)
                        .clip(CircleShape),
                    fallback = {
                        SchoolOsBrandLogo(size = 126)
                    },
                )
            }

            // 3D Student Character Image (Positioned slightly to the right of the logo)
            Image(
                painter = painterResource(id = CoreR.drawable.img_login_student_hero),
                contentDescription = "Siswa School OS",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .offset(x = 26.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
            )
        }
    }
}
