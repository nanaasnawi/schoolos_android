package com.schoolos.android.feature.profile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*

@Composable
fun SchoolAffiliationCard(
    schoolName: String?,
    schoolLogoUrl: String?,
    schoolNpsn: String? = null,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // School logo
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = innerCardBg,
                border = BorderStroke(1.dp, innerCardBorder),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DynamicSchoolLogo(
                        logoUrl = schoolLogoUrl,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        fallback = {
                            SchoolOsBrandLogo(size = 44)
                        }
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = schoolName?.ifBlank { "Akselerasi-Edu" } ?: "Akselerasi-Edu",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = textPrimary,
                        letterSpacing = (-0.2).sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = successColor.copy(alpha = if (isDark) 0.15f else 0.10f),
                        border = BorderStroke(1.dp, successColor.copy(alpha = if (isDark) 0.40f else 0.25f))
                    ) {
                        Text(
                            text = "Terverifikasi",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = successColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (!schoolNpsn.isNullOrBlank()) {
                    Text(
                        text = "NPSN: $schoolNpsn",
                        fontSize = 11.5.sp,
                        color = textTertiary,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Text(
                    text = "Satuan Pendidikan Terdaftar Resmi",
                    fontSize = 11.5.sp,
                    color = textSecondary,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}
