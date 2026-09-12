package com.schoolos.android.feature.auth.components

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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.ParentNeon
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary

@Composable
fun LoginHelpDialog(
    accentColor: Color,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        title = {
            Text(
                text = "Panduan Masuk Akun",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Silakan gunakan format kredensial resmi sesuai data Dapodik Sekolah:",
                    fontSize = 13.sp,
                    color = TextSecondary,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CosmicSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.School,
                                null,
                                tint = StudentNeon,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Siswa: 10 Digit NISN atau Username Siswa",
                                fontSize = 12.sp,
                                color = StudentNeon,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                null,
                                tint = TeacherNeon,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Guru: NIP, NUPTK, atau Username PTK",
                                fontSize = 12.sp,
                                color = TeacherNeon,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.People,
                                null,
                                tint = ParentNeon,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Wali Murid: Akun Ibu (ibu_<nisn>) / Username",
                                fontSize = 12.sp,
                                color = ParentNeon,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.08f))
                        .padding(10.dp),
                ) {
                    Text(
                        text = "💡 Login Instan: Anda juga dapat memindai Badge QR Code resmi tanpa mengetik kata sandi. Lupa kata sandi? Hubungi Operator / Admin IT Sekolah.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            ) {
                Text("Saya Mengerti", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        containerColor = CosmicNavy,
        shape = RoundedCornerShape(22.dp),
    )
}
