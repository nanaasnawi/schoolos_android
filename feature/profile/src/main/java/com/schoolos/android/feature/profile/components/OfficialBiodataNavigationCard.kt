package com.schoolos.android.feature.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*

@Composable
fun OfficialBiodataNavigationCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = LocalIsDarkTheme.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDarkTheme) 0.dp else 1.5.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = Color(0x10000000)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(
                0.5.dp,
                if (isDarkTheme) StudentNeon.copy(alpha = 0.25f) else GlassBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(StudentNeon.copy(alpha = 0.12f))
                    .border(0.5.dp, StudentNeon.copy(alpha = 0.30f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = StudentNeon,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Info Column
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Biodata Pokok Resmi Siswa",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = "Dapodik Kemendikdasmen RI • NISN & Identitas",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Clean Trailing Navigation Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Buka Biodata",
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
