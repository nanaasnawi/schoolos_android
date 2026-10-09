package com.schoolos.android.feature.learning.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.feature.learning.ContentBlock
import com.schoolos.android.feature.learning.ContentBlockType

data class ParsedInfographicCard(
    val id: String,
    val stepNumber: Int,
    val headline: String,
    val body: String,
    val imageUrl: String?,
    val takeaway: String?,
)

data class ParsedMagazineData(
    val heroCoverUrl: String?,
    val heroCaption: String,
    val cards: List<ParsedInfographicCard>,
    val didYouKnowText: String?,
    val glossaryTerms: List<String>,
)

/**
 * Parses raw AI content blocks into clean, structured digital magazine sections.
 */
fun parseInfographicBlocks(
    title: String,
    blocks: List<ContentBlock>
): ParsedMagazineData {
    if (blocks.isEmpty()) {
        return ParsedMagazineData(
            heroCoverUrl = null,
            heroCaption = "$title • Edisi Visual Interaktif",
            cards = emptyList(),
            didYouKnowText = "Visual ilustrasi pada modul pembelajaran membantu daya ingat siswa hingga 65% lebih kuat dibanding membaca teks polos!",
            glossaryTerms = listOf("Literasi Visual", "Kurikulum Merdeka", "Eksplorasi Konsep")
        )
    }

    var heroCoverUrl: String? = null
    val heroCaption = "$title • Edisi Visual Interaktif"
    var didYouKnow: String? = null
    val terms = linkedSetOf<String>()
    val cardList = mutableListOf<ParsedInfographicCard>()

    // First image for hero cover
    val firstImgIdx = blocks.indexOfFirst { it.type == ContentBlockType.IMAGE && it.content.isNotBlank() }
    val workingBlocks = if (firstImgIdx == 0) {
        heroCoverUrl = blocks[firstImgIdx].content.trim()
        blocks.drop(1)
    } else if (firstImgIdx != -1) {
        heroCoverUrl = blocks[firstImgIdx].content.trim()
        blocks
    } else {
        blocks
    }

    var currentPendingImage: String? = null
    var cardCounter = 1

    var i = 0
    while (i < workingBlocks.size) {
        val b = workingBlocks[i]

        if (b.type == ContentBlockType.IMAGE) {
            currentPendingImage = b.content.trim()
        } else if (b.type == ContentBlockType.TEXT) {
            val text = b.content.trim()

            // Check if text is "Tahukah Kamu?" / Did you know callout
            if (text.contains("tahukah kamu", ignoreCase = true) || text.contains("fakta menarik", ignoreCase = true)) {
                didYouKnow = text
                    .replace(Regex("""^###\s*📌?\s*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""^Tahukah Kamu\??:?\s*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""^Fakta Menarik\??:?\s*""", RegexOption.IGNORE_CASE), "")
                    .trim()
                i++
                continue
            }

            // Extract headline & body cleanly
            var headline = "Poin Esensial #$cardCounter"
            var body = text
            var step = cardCounter

            val headingRegex = Regex("""^###\s*📌?\s*(\d+)?\.?\s*([^\n]+)""")
            val match = headingRegex.find(text)
            if (match != null) {
                val numStr = match.groupValues.getOrNull(1)
                if (!numStr.isNullOrBlank()) {
                    numStr.toIntOrNull()?.let { step = it }
                }
                headline = match.groupValues.getOrNull(2)?.trim() ?: headline
                body = text.substring(match.range.last + 1).trim()
            } else {
                val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.isNotEmpty() && lines[0].length < 85 && (lines[0].startsWith("#") || lines.size > 1)) {
                    headline = lines[0].replace(Regex("""^[#*•\-\s]+"""), "").trim()
                    body = lines.drop(1).joinToString("\n").trim()
                }
            }

            // Clean headline of symbols
            headline = headline
                .replace(Regex("""^[📌💡🔍📖⭐\s]+"""), "")
                .replace(Regex("""^#+\s*"""), "")
                .trim()

            // Extract keywords for glossary
            headline.split(Regex("""\s+"""))
                .map { it.replace(Regex("""[^a-zA-Z0-9]"""), "").trim() }
                .filter { it.length > 5 && !it.startsWith("http", ignoreCase = true) }
                .take(2)
                .forEach { terms.add(it) }

            // Pair image
            val imgToUse = currentPendingImage
                ?: if (i + 1 < workingBlocks.size && workingBlocks[i + 1].type == ContentBlockType.IMAGE) {
                    val next = workingBlocks[i + 1].content.trim()
                    i++ // consume paired image
                    next
                } else null
            currentPendingImage = null

            val takeaway = if (body.length > 90) {
                "Intisari: Memahami konsep penting seputar $headline."
            } else null

            cardList.add(
                ParsedInfographicCard(
                    id = b.id,
                    stepNumber = step,
                    headline = headline,
                    body = body,
                    imageUrl = imgToUse,
                    takeaway = takeaway
                )
            )
            cardCounter++
        }
        i++
    }

    if (didYouKnow.isNullOrBlank()) {
        didYouKnow = "Visual ilustrasi interaktif membantu daya serap materi dan retensi pemahaman siswa hingga 65% lebih tinggi dibanding teks standar!"
    }
    if (terms.isEmpty()) {
        terms.add("Literasi Visual")
        terms.add("Kurikulum Merdeka")
        terms.add("Eksplorasi Konsep")
    }

    return ParsedMagazineData(
        heroCoverUrl = heroCoverUrl,
        heroCaption = heroCaption,
        cards = cardList,
        didYouKnowText = didYouKnow,
        glossaryTerms = terms.take(6).toList()
    )
}

/**
 * Modern Editorial Infographic & Magazine Poster Reader for Android
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InfographicMagazineReaderView(
    title: String,
    subtitle: String? = null,
    subjectName: String = "Mata Pelajaran",
    className: String = "Semua Rombel",
    teacherName: String = "Guru Pengampu",
    blocks: List<ContentBlock>,
    textSizeMultiplier: Float = 1.0f,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copiedSuccess by remember { mutableStateOf(false) }

    val magazineData = remember(title, blocks) {
        parseInfographicBlocks(title, blocks)
    }

    val copyAllText = {
        val sb = StringBuilder()
        sb.append("📘 ").append(title.uppercase()).append("\n")
        sb.append("Mata Pelajaran: ").append(subjectName).append(" | Rombel: ").append(className).append("\n")
        sb.append("Penyusun: ").append(teacherName).append("\n\n")

        magazineData.cards.forEach { card ->
            sb.append("📌 ").append(card.stepNumber).append(". ").append(card.headline).append("\n")
            sb.append(card.body).append("\n\n")
        }

        if (!magazineData.didYouKnowText.isNullOrBlank()) {
            sb.append("💡 Tahukah Kamu?\n").append(magazineData.didYouKnowText).append("\n\n")
        }

        clipboardManager.setText(AnnotatedString(sb.toString()))
        copiedSuccess = true
        Toast.makeText(context, "✓ Seluruh naskah materi berhasil disalin ke papan klip", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. EDITORIAL MASTHEAD (HEADER MAJALAH) ──────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicNavy),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Top Edition Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(NeonBlue.copy(alpha = 0.2f), AccentNeonPurple.copy(alpha = 0.2f))))
                            .border(0.5.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.AutoAwesome, null, tint = NeonBlue, modifier = Modifier.size(12.dp))
                            Text(
                                text = "INFOGRAFIS KURIKULUM MERDEKA",
                                color = NeonBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Schedule, null, tint = TextTertiary, modifier = Modifier.size(13.dp))
                        Text(
                            text = "~4 Mnt Baca",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Magazine Title
                Text(
                    text = title.ifBlank { "Materi Infografis Digital" },
                    color = TextPrimary,
                    fontSize = (20 * textSizeMultiplier).sp,
                    lineHeight = (26 * textSizeMultiplier).sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // Subtitle description
                Text(
                    text = subtitle ?: "Modul eksplorasi visual interaktif yang menyajikan ringkasan konsep esensial secara tematis, menarik, dan mudah dipahami.",
                    color = TextSecondary,
                    fontSize = (12.5 * textSizeMultiplier).sp,
                    lineHeight = (18 * textSizeMultiplier).sp
                )

                // Metadata Pill Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicSurface2)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Explore, null, tint = NeonBlue, modifier = Modifier.size(12.dp))
                            Text(subjectName, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicSurface2)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(className, color = TextSecondary, fontSize = 11.sp)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TeacherNeon.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Person, null, tint = TeacherNeon, modifier = Modifier.size(12.dp))
                            Text(teacherName, color = TeacherNeon, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                HorizontalDivider(color = GlassBorder)

                // Quick Action Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎨 Mode Poster & Majalah Digital",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )

                    Surface(
                        color = if (copiedSuccess) NeonSuccess.copy(alpha = 0.15f) else CosmicSurface2,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            if (copiedSuccess) NeonSuccess.copy(alpha = 0.6f) else GlassBorder
                        ),
                        modifier = Modifier.clickable { copyAllText() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (copiedSuccess) Icons.Default.Done else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = if (copiedSuccess) NeonSuccess else NeonBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (copiedSuccess) "Tersalin!" else "Salin Naskah",
                                color = if (copiedSuccess) NeonSuccess else NeonBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ── 2. HERO FEATURE VISUAL COVER ─────────────────────────────────────
        if (!magazineData.heroCoverUrl.isNullOrBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicNavy),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        InAppImageViewer(
                            imageUrl = magazineData.heroCoverUrl,
                            title = magazineData.heroCaption,
                            fallbackIndex = 0,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top-left hero badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.72f))
                                .border(0.5.dp, Color(0xFFFACC15).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFACC15), modifier = Modifier.size(12.dp))
                                Text(
                                    text = "SAMPUL VISUAL UTAMA",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Caption below cover
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmicDark)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = magazineData.heroCaption,
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Ketuk untuk Pinch-to-Zoom",
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // ── 3. SECTION CARDS (THE INFOGRAPHIC JOURNEY) ───────────────────────
        magazineData.cards.forEachIndexed { idx, card ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicNavy),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: Step pill + Clean bold headline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NeonBlue.copy(alpha = 0.15f))
                                .border(1.dp, NeonBlue.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format("%02d", card.stepNumber),
                                color = NeonBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.headline,
                                color = TextPrimary,
                                fontSize = (15.5 * textSizeMultiplier).sp,
                                lineHeight = (21 * textSizeMultiplier).sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Poin Inti Bab ${card.stepNumber}",
                                color = TextTertiary,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    // Visual Illustration (Paired with this step)
                    if (!card.imageUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
                        ) {
                            InAppImageViewer(
                                imageUrl = card.imageUrl,
                                title = "${card.headline} (Visual #${card.stepNumber})",
                                fallbackIndex = idx + 1,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Readable Paragraph Body
                    if (card.body.isNotBlank()) {
                        Text(
                            text = card.body,
                            color = TextSecondary,
                            fontSize = (13.5 * textSizeMultiplier).sp,
                            lineHeight = (21 * textSizeMultiplier).sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Key Takeaway Pill
                    if (!card.takeaway.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CosmicSurface2)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = NeonBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = card.takeaway,
                                    color = TextPrimary,
                                    fontSize = (11.5 * textSizeMultiplier).sp,
                                    lineHeight = (16 * textSizeMultiplier).sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 4. TAHUKAH KAMU? CALLOUT CARD ────────────────────────────────────
        if (!magazineData.didYouKnowText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(NeonWarning.copy(alpha = 0.08f))
                    .border(1.dp, NeonWarning.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NeonWarning.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = NeonWarning,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "💡 TAHUKAH KAMU?",
                            color = NeonWarning,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = magazineData.didYouKnowText,
                        color = TextPrimary,
                        fontSize = (13 * textSizeMultiplier).sp,
                        lineHeight = (20 * textSizeMultiplier).sp
                    )
                }
            }
        }

        // ── 5. KOSAKATA & KONSEP KUNCI (GLOSSARY) ───────────────────────────
        if (magazineData.glossaryTerms.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicNavy),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Tag, null, tint = AccentNeonPurple, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Konsep & Kosakata Kunci",
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        magazineData.glossaryTerms.forEach { term ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentNeonPurple.copy(alpha = 0.12f))
                                    .border(0.5.dp, AccentNeonPurple.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "#$term",
                                    color = AccentNeonPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
