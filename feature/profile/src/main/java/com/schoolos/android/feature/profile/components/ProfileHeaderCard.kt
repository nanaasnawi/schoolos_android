package com.schoolos.android.feature.profile.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.User

@Composable
fun ProfileHeaderCard(
    user: User?,
    roleNeon: Color,
    isTeacher: Boolean,
    isParent: Boolean,
    displayContact: String,
    modifier: Modifier = Modifier,
) {
    // Animated gradient border ring
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ringAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicNavy)
            .border(1.dp, roleNeon.copy(alpha = 0.22f), RoundedCornerShape(16.dp)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Banner decoratif dengan abstract blobs ──────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                roleNeon.copy(alpha = 0.30f),
                                roleNeon.copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            ) {
                // Decorative blob 1
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 20.dp, y = (-20).dp)
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(roleNeon.copy(alpha = 0.12f)),
                )
                // Decorative blob 2
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-16).dp, y = 16.dp)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(NeonBlue.copy(alpha = 0.08f)),
                )
            }

            // ── Avatar with animated glow ring ──────────────────────────────
            Box(
                modifier = Modifier
                    .offset(y = (-44).dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        spotColor = roleNeon.copy(alpha = 0.45f),
                    )
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(CosmicNavy)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            listOf(roleNeon.copy(alpha = ringAlpha), NeonBlue.copy(alpha = ringAlpha * 0.8f)),
                        ),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val icon = when {
                    isTeacher -> Icons.Default.Person
                    isParent -> Icons.Default.Face
                    else -> Icons.Default.School
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(50.dp),
                    tint = roleNeon,
                )
            }

            // Offset the content up to compensate for the avatar negative offset
            Column(
                modifier = Modifier
                    .offset(y = (-32).dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // User full name
                Text(
                    text = user?.name?.ifBlank { "Pengguna SchoolOS" } ?: "Pengguna SchoolOS",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.4).sp,
                )

                Spacer(Modifier.height(6.dp))

                // Contact row
                if (isParent) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = NeonSuccess,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = displayContact,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonSuccess.copy(alpha = 0.12f))
                                .border(1.dp, NeonSuccess.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NeonSuccess,
                                    modifier = Modifier.size(10.dp),
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "Terverifikasi",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonSuccess,
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = displayContact,
                        fontSize = 12.sp,
                        color = TextTertiary,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Role Badge — inline implementation
                val roleLabel = when {
                    isTeacher -> "👨‍🏫 Guru"
                    isParent -> "👨‍👧 Wali Murid"
                    else -> "🎓 Siswa"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(roleNeon.copy(alpha = 0.12f))
                        .border(1.dp, roleNeon.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = roleLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = roleNeon,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 4-Metric Statistics Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CosmicBlack.copy(alpha = 0.4f))
                        .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when {
                            isTeacher -> {
                                ProfileMetricItem("JAM AJAR", "24 Jam", roleNeon)
                                ProfileMetricDivider()
                                ProfileMetricItem("ROMBEL", "4 Kelas", roleNeon)
                                ProfileMetricDivider()
                                ProfileMetricItem("MATERI", "18 Modul", roleNeon)
                                ProfileMetricDivider()
                                ProfileMetricItem("PRESENSI", "99%", NeonSuccess)
                            }
                            isParent -> {
                                ProfileMetricItem("HADIR ANAK", "96%", NeonSuccess)
                                ProfileMetricDivider()
                                ProfileMetricItem("RERATA", "86.0", roleNeon)
                                ProfileMetricDivider()
                                ProfileMetricItem("TUGAS", "Aktif", NeonBlue)
                                ProfileMetricDivider()
                                ProfileMetricItem("STATUS", "Resmi", NeonSuccess)
                            }
                            else -> {
                                ProfileMetricItem("KEHADIRAN", "98%", NeonSuccess)
                                ProfileMetricDivider()
                                ProfileMetricItem("RERATA", "88.5", roleNeon)
                                ProfileMetricDivider()
                                ProfileMetricItem("LEVEL", "Lvl 5", roleNeon)
                                ProfileMetricDivider()
                                ProfileMetricItem("BADGE", "14", NeonSuccess)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProfileMetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = (-0.3).sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextTertiary,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun ProfileMetricDivider() {
    Box(
        modifier = Modifier
            .height(22.dp)
            .width(1.dp)
            .background(GlassBorder),
    )
}

