package com.schoolos.android.feature.profile.components

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.core.auth.isPkbmInstitution
import com.schoolos.android.core.auth.resolveRoleDisplay
import com.schoolos.android.domain.model.User

@Composable
fun ProfileHeaderCard(
    user: User?,
    roleNeon: Color,
    isTeacher: Boolean,
    isParent: Boolean,
    displayContact: String,
    className: String = "",
    schoolName: String = "",
    serverBaseUrl: String = "",
    isUploading: Boolean = false,
    isPrincipal: Boolean = false,
    onAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    val roleLabel = resolveRoleDisplay(user?.role, schoolName, isFormalUpper = true)
    val avatarDrawableRes = when {
        isTeacher -> CoreR.drawable.ic_modern_profile
        isParent -> CoreR.drawable.ic_modern_child
        else -> CoreR.drawable.ic_modern_graduation
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // ── Top label row ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "IDENTITAS PENGGUNA",
                    color = textTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                )
                // Role pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = roleNeon.copy(alpha = if (isDark) 0.15f else 0.10f),
                    border = BorderStroke(0.5.dp, roleNeon.copy(alpha = if (isDark) 0.40f else 0.25f))
                ) {
                    Text(
                        text = roleLabel,
                        color = roleNeon,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Avatar + Identity info row ─────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val fullAvatarUrl = if (!user?.avatarUrl.isNullOrBlank()) {
                    val raw = user.avatarUrl!!
                    if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true) || raw.startsWith("data:", ignoreCase = true)) {
                        raw
                    } else {
                        val host = serverBaseUrl.ifBlank { com.schoolos.android.core.common.BuildConfig.API_BASE_URL }.substringBefore("/api/").trimEnd('/')
                        val path = if (raw.startsWith("/")) raw else "/$raw"
                        "$host$path"
                    }
                } else null

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clickable(enabled = !isUploading) { onAvatarClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    // Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.TopStart)
                            .clip(CircleShape)
                            .background(roleNeon.copy(alpha = if (isDark) 0.15f else 0.10f))
                            .border(2.dp, roleNeon.copy(alpha = if (isDark) 0.55f else 0.40f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!fullAvatarUrl.isNullOrBlank()) {
                            val context = LocalContext.current
                            val imageRequest = remember(fullAvatarUrl) {
                                ImageRequest.Builder(context)
                                    .data(fullAvatarUrl)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(true)
                                    .build()
                            }
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = "Foto Profil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = avatarDrawableRes),
                                contentDescription = "Avatar",
                                tint = roleNeon,
                                modifier = Modifier.size(36.dp),
                            )
                        }

                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                )
                            }
                        }
                    }

                    // Floating camera badge
                    if (!isUploading) {
                        val cameraBadgeBg = if (isDark) Color(0xFF18181B) else Color.White
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(cameraBadgeBg)
                                .border(1.5.dp, roleNeon, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Ubah Foto",
                                tint = roleNeon,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = user?.name?.ifBlank { "Pengguna Akselerasi-Edu" } ?: "Pengguna Akselerasi-Edu",
                        fontSize = 16.5.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.2).sp,
                    )
                    if (displayContact.isNotBlank()) {
                        Text(
                            text = displayContact,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = textSecondary,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "Ketuk foto untuk lihat / ganti",
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        color = roleNeon.copy(alpha = 0.90f),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Divider ───────────────────────────────────────────────────
            HorizontalDivider(
                color = innerCardBorder,
                thickness = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // ── 3-pill info strip ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val isPkbm = isPkbmInstitution(schoolName)
                val pillData: List<Pair<String, String>> = when {
                    isPrincipal -> listOf(
                        "ROLE" to if (isPkbm) "Kepala PKBM" else "Kepala Sekolah",
                        "LEMBAGA" to if (isPkbm) "PKBM" else "Sekolah",
                        "STATUS" to "Aktif",
                    )
                    isTeacher -> listOf(
                        "ROLE" to if (isPkbm) "Tutor" else "Pengajar",
                        "STATUS" to "Aktif",
                        "AKUN" to (user?.email?.substringBefore("@")?.ifBlank { "-" } ?: "-"),
                    )
                    isParent -> listOf(
                        "ROLE" to if (isPkbm) "Wali WB" else "Wali",
                        "STATUS" to "Resmi",
                        "AKUN" to (user?.name?.ifBlank { "-" } ?: "-"),
                    )
                    else -> listOf(
                        "ROLE" to if (isPkbm) "Warga Belajar" else "Siswa",
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
    val isDark = LocalIsDarkTheme.current
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = innerCardBg,
        border = BorderStroke(0.5.dp, innerCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                color = textTertiary,
                letterSpacing = 0.4.sp,
                maxLines = 1,
            )
        }
    }
}
