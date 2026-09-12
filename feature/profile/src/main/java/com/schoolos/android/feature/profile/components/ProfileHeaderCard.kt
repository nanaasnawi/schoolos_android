package com.schoolos.android.feature.profile.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.RoleBadge
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CosmicNavy)
            .border(1.dp, roleNeon.copy(alpha = 0.25f), RoundedCornerShape(22.dp)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Decorative ambient header banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                roleNeon.copy(alpha = 0.18f),
                                NeonBlue.copy(alpha = 0.04f),
                            ),
                        ),
                    ),
            )

            // Avatar container
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .shadow(6.dp, CircleShape, spotColor = roleNeon.copy(alpha = 0.35f))
                    .clip(CircleShape)
                    .background(CosmicNavy)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(listOf(roleNeon, NeonBlue)),
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
                    modifier = Modifier.size(46.dp),
                    tint = roleNeon,
                )
            }

            Spacer(Modifier.height(10.dp))

            // User full name
            Text(
                text = user?.name?.ifBlank { "Pengguna SchoolOS" } ?: "Pengguna SchoolOS",
                fontWeight = FontWeight.Black,
                fontSize = 19.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(6.dp))

            // Contact row with verified badge
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
                            .padding(horizontal = 6.dp, vertical = 2.dp),
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

            Spacer(Modifier.height(10.dp))

            // Role Badge
            RoleBadge(role = user?.role?.ifBlank { if (isParent) "Wali Murid" else "Siswa" } ?: "Pengguna")

            Spacer(Modifier.height(18.dp))
        }
    }
}
