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
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.DynamicSchoolLogo
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
            .clip(RoundedCornerShape(18.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(CosmicBlack)
                    .border(1.dp, NeonBlue.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                DynamicSchoolLogo(
                    logoUrl = schoolLogoUrl,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape),
                    fallback = {
                        SchoolOsBrandLogo(size = 38)
                    },
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = schoolName?.ifBlank { "School OS" } ?: "School OS",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Resmi",
                        tint = NeonSuccess,
                        modifier = Modifier.size(14.dp),
                    )
                }

                Text(
                    text = "NPSN: ${schoolNpsn?.ifBlank { "Terdaftar" } ?: "Terdaftar"}",
                    fontSize = 11.sp,
                    color = TextTertiary,
                )
            }
        }
    }
}
