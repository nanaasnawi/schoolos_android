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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassBorder2
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
    className: String = "",
    modifier: Modifier = Modifier,
) {
    val roleLabel = when {
        isTeacher -> "GURU"
        isParent -> "WALI MURID"
        else -> "SISWA"
    }
    val avatarIcon = when {
        isTeacher -> Icons.Default.Person
        isParent -> Icons.Default.Face
        else -> Icons.Default.School
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Column {
            // ── Top label row ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "IDENTITAS PENGGUNA",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                )
                // Role pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(roleNeon.copy(alpha = 0.12f))
                        .border(0.5.dp, roleNeon.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = roleLabel,
                        color = roleNeon,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Avatar + Identity info row ─────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(roleNeon.copy(alpha = 0.10f))
                        .border(1.dp, roleNeon.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = avatarIcon,
                        contentDescription = "Avatar",
                        tint = roleNeon,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user?.name?.ifBlank { "Pengguna SchoolOS" } ?: "Pengguna SchoolOS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.3).sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = displayContact,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Divider ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(GlassBorder2),
            )

            Spacer(Modifier.height(14.dp))

            // ── 3-pill info strip (real data only) ────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val pillData: List<Pair<String, String>> = when {
                    isTeacher -> listOf(
                        "ROLE" to "Pengajar",
                        "STATUS" to "Aktif",
                        "AKUN" to (user?.email?.substringBefore("@")?.ifBlank { "-" } ?: "-"),
                    )
                    isParent -> listOf(
                        "ROLE" to "Wali",
                        "STATUS" to "Resmi",
                        "AKUN" to (user?.name?.ifBlank { "-" } ?: "-"),
                    )
                    else -> listOf(
                        "ROLE" to "Siswa",
                        "KELAS" to className.ifBlank { "-" },
                        "STATUS" to "Aktif",
                    )
                }

                pillData.forEach { (label, value) ->
                    ProfileInfoPill(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CosmicSurface2)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal,
            color = TextTertiary,
            letterSpacing = 0.4.sp,
            maxLines = 1,
        )
    }
}
