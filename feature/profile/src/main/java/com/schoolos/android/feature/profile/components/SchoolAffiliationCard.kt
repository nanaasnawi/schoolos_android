package com.schoolos.android.feature.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.SchoolOsBrandLogo
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

@Composable
fun SchoolAffiliationCard(
    schoolName: String?,
    schoolLogoUrl: String?,
    schoolNpsn: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = NeonBlue.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(20.dp))
            .background(CosmicNavy)
            .background(
                Brush.verticalGradient(
                    listOf(NeonBlue.copy(alpha = 0.06f), NeonBlue.copy(alpha = 0f)),
                    startY = 0f, endY = 200f,
                ),
            )
            .border(1.dp, NeonBlue.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // School logo
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(4.dp, CircleShape, spotColor = NeonBlue.copy(alpha = 0.25f))
                    .clip(CircleShape)
                    .background(CosmicBlack)
                    .border(1.5.dp, NeonBlue.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SchoolOsBrandLogo(
                    size = 44,
                    logoUrl = schoolLogoUrl,
                    modifier = Modifier.clip(CircleShape),
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = schoolName?.ifBlank { "School OS" } ?: "School OS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        letterSpacing = (-0.2).sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonSuccess.copy(alpha = 0.15f))
                            .border(1.dp, NeonSuccess.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "Akreditasi A",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonSuccess,
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                Text(
                    text = "NPSN: ${schoolNpsn?.ifBlank { "Terdaftar" } ?: "Terdaftar"}",
                    fontSize = 11.sp,
                    color = TextTertiary,
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text = "Lembaga Pendidikan Terverifikasi",
                    fontSize = 10.sp,
                    color = NeonBlue.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
