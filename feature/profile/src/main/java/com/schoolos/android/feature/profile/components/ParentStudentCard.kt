package com.schoolos.android.feature.profile.components

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
fun ParentStudentCard(
    studentName: String,
    studentClass: String,
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

    val parentNeon = if (isDark) Color(0xFFF43F5E) else Color(0xFFE11D48)
    val studentNeon = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(parentNeon.copy(alpha = if (isDark) 0.15f else 0.10f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_child),
                            contentDescription = null,
                            tint = parentNeon,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Siswa Dalam Bimbingan",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary,
                        )
                        Text(
                            text = "Pemantauan aktif tugas & kehadiran",
                            fontSize = 10.5.sp,
                            color = textTertiary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Active badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = successColor.copy(alpha = if (isDark) 0.15f else 0.10f),
                    border = BorderStroke(1.dp, successColor.copy(alpha = if (isDark) 0.40f else 0.25f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(successColor),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Aktif",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = successColor,
                        )
                    }
                }
            }

            // Divider
            HorizontalDivider(color = innerCardBorder, thickness = 0.5.dp, modifier = Modifier.fillMaxWidth())

            // Student Info Row
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = innerCardBg,
                border = BorderStroke(1.dp, innerCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(studentNeon.copy(alpha = if (isDark) 0.15f else 0.10f))
                            .border(1.dp, studentNeon.copy(alpha = if (isDark) 0.40f else 0.25f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_profile),
                            contentDescription = null,
                            tint = studentNeon,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = studentName.ifBlank { "Data Siswa Binaan" },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = textPrimary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Peserta didik aktif",
                            fontSize = 11.5.sp,
                            color = textTertiary,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // Class Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = studentNeon.copy(alpha = if (isDark) 0.15f else 0.10f),
                        border = BorderStroke(1.dp, studentNeon.copy(alpha = if (isDark) 0.40f else 0.25f))
                    ) {
                        Text(
                            text = studentClass.ifBlank { "Kelas" },
                            fontSize = 11.5.sp,
                            color = studentNeon,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
