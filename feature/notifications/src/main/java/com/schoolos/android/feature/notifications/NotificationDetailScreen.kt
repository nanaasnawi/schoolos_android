package com.schoolos.android.feature.notifications

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    id: String,
    initialTitle: String = "",
    initialBody: String = "",
    initialType: String = "PENGUMUMAN",
    initialCreatedAt: String = "",
    initialReferenceType: String = "",
    initialReferenceId: String = "",
    onBack: () -> Unit = {},
    onOpenRelated: (referenceType: String, referenceId: String) -> Unit = { _, _ -> },
    viewModel: NotificationDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val effectiveTitle = state.title.ifBlank { initialTitle }
    val effectiveBody = state.body.ifBlank { initialBody }
    val effectiveType = state.category.ifBlank { initialType }
    val effectiveCreatedAt = state.createdAt.ifBlank { initialCreatedAt }
    val effectiveRefType = state.referenceType.ifBlank { initialReferenceType }
    val effectiveRefId = state.referenceId.ifBlank { initialReferenceId }
    val effectiveAuthor = state.author.ifBlank {
        if (effectiveType.contains("assignment", ignoreCase = true) || effectiveType.contains("quiz", ignoreCase = true)) {
            "Guru Pengampu"
        } else {
            "Kepala Sekolah"
        }
    }

    val accentColor = colorForType(effectiveType)
    val typeIcon = iconForType(effectiveType)
    val typeLabel = labelForType(effectiveType)

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Pengumuman", "$effectiveTitle\n\n$effectiveBody")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Teks pengumuman disalin ke papan klip", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Detail Notifikasi",
                subtitle = "Informasi & Pengumuman Sekolah",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { copyToClipboard() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Salin Teks",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── HEADER CARD ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Category & Relative Time Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(typeIcon, null, tint = accentColor, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    typeLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accentColor,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        if (effectiveCreatedAt.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CosmicSurface)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    formatRelative(effectiveCreatedAt),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Main Title
                    Text(
                        text = effectiveTitle.ifBlank { "Pengumuman Sekolah" },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 26.sp
                    )

                    HorizontalDivider(color = GlassBorder, thickness = 0.8.dp)

                    // Sender & Timestamp Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CosmicSurface)
                                .border(1.dp, GlassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Campaign,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column {
                            Text(
                                text = effectiveAuthor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (effectiveCreatedAt.isNotBlank()) {
                                Text(
                                    text = formatFullDate(effectiveCreatedAt),
                                    fontSize = 11.sp,
                                    color = TextTertiary,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // ── BODY / CONTENT CARD ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ISI PENGUMUMAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )

                        TextButton(
                            onClick = { copyToClipboard() },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, tint = NeonBlue, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Salin", fontSize = 12.sp, color = NeonBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = GlassBorder, thickness = 0.8.dp)

                    // Selectable Body Text
                    SelectionContainer {
                        Text(
                            text = effectiveBody.ifBlank { "Tidak ada rincian pesan tambahan pada notifikasi ini." },
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // ── RELATED ITEM / DEEP LINK BUTTON ──────────────────────────────
            if (effectiveRefId.isNotBlank() && !effectiveRefType.equals("announcement", ignoreCase = true)) {
                val actionLabel = when {
                    effectiveRefType.contains("assign", ignoreCase = true) || effectiveType.contains("tugas", ignoreCase = true) -> "Buka Lembar Tugas"
                    effectiveRefType.contains("quiz", ignoreCase = true) || effectiveType.contains("kuis", ignoreCase = true) -> "Buka Ujian / Kuis CBT"
                    effectiveRefType.contains("material", ignoreCase = true) || effectiveType.contains("materi", ignoreCase = true) -> "Buka Materi Belajar"
                    effectiveRefType.contains("grade", ignoreCase = true) || effectiveType.contains("nilai", ignoreCase = true) -> "Buka Buku Nilai"
                    effectiveRefType.contains("session", ignoreCase = true) || effectiveType.contains("jadwal", ignoreCase = true) -> "Buka Jadwal Sesi"
                    else -> "Buka Halaman Terkait"
                }

                Button(
                    onClick = { onOpenRelated(effectiveRefType, effectiveRefId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(StudentNeon, NeonBlue)))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                actionLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── BACK TO LIST BUTTON ──────────────────────────────────────────
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Kembali ke Pusat Notifikasi", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun labelForType(type: String): String = when {
    type.contains("assignment", ignoreCase = true) || type.contains("tugas", ignoreCase = true) -> "TUGAS"
    type.contains("quiz", ignoreCase = true) || type.contains("kuis", ignoreCase = true) -> "KUIS CBT"
    type.contains("grade", ignoreCase = true) || type.contains("assessment", ignoreCase = true) || type.contains("nilai", ignoreCase = true) -> "NILAI"
    type.contains("lesson", ignoreCase = true) || type.contains("session", ignoreCase = true) || type.contains("materi", ignoreCase = true) -> "MATERI BELAJAR"
    type.contains("progress", ignoreCase = true) -> "PROGRES"
    type.contains("achievement", ignoreCase = true) -> "PRESTASI"
    else -> "PENGUMUMAN RESMI"
}

private fun iconForType(type: String): ImageVector = when {
    type.contains("assignment", ignoreCase = true) || type.contains("tugas", ignoreCase = true) -> Icons.AutoMirrored.Filled.Assignment
    type.contains("quiz", ignoreCase = true) || type.contains("kuis", ignoreCase = true) -> Icons.Default.Quiz
    type.contains("grade", ignoreCase = true) || type.contains("assessment", ignoreCase = true) || type.contains("nilai", ignoreCase = true) -> Icons.Default.Grade
    type.contains("lesson", ignoreCase = true) || type.contains("session", ignoreCase = true) || type.contains("materi", ignoreCase = true) -> Icons.Default.Book
    type.contains("progress", ignoreCase = true) -> Icons.Default.TrendingUp
    type.contains("achievement", ignoreCase = true) -> Icons.Default.EmojiEvents
    else -> Icons.Default.Campaign
}

private fun colorForType(type: String): Color = when {
    type.contains("assignment", ignoreCase = true) || type.contains("tugas", ignoreCase = true) -> StudentNeon
    type.contains("quiz", ignoreCase = true) || type.contains("kuis", ignoreCase = true) -> NeonWarning
    type.contains("grade", ignoreCase = true) || type.contains("assessment", ignoreCase = true) || type.contains("nilai", ignoreCase = true) -> NeonSuccess
    type.contains("lesson", ignoreCase = true) || type.contains("session", ignoreCase = true) || type.contains("materi", ignoreCase = true) -> NeonBlue
    type.contains("progress", ignoreCase = true) -> NeonBlue
    type.contains("achievement", ignoreCase = true) -> StudentNeon
    else -> NeonBlue
}

private fun formatRelative(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val now = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours = ChronoUnit.HOURS.between(instant, now)
        val days = ChronoUnit.DAYS.between(instant, now)
        when {
            minutes < 1 -> "Baru saja"
            minutes < 60 -> "${minutes}m lalu"
            hours < 24 -> "${hours}j lalu"
            days < 7 -> "${days}h lalu"
            else -> {
                val zdt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
                zdt.format(DateTimeFormatter.ofPattern("dd MMM"))
            }
        }
    } catch (_: Exception) {
        iso.substringBefore("T")
    }
}

private fun formatFullDate(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("Asia/Jakarta"))
        zdt.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy • HH:mm 'WIB'", Locale.forLanguageTag("id-ID")))
    } catch (_: Exception) {
        iso
    }
}
