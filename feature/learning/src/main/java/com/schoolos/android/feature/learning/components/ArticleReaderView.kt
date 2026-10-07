package com.schoolos.android.feature.learning.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MenuBook
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*

/**
 * Article & Text Reader View with Typography, Copy-Paste, and Professional Reading Mode
 */
@Composable
fun ArticleReaderView(
    title: String,
    content: String,
    textSizeMultiplier: Float = 1.0f,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copiedAll by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicNavy),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(GlassBorder, GlassBorder))),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NeonSuccess.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = NeonSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "📖 Naskah Artikel & Materi Teks",
                            color = NeonSuccess,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val wordCount = content.split("\\s+".toRegex()).count { it.isNotBlank() }
                        val estMinutes = maxOf(1, wordCount / 120)
                        Text(
                            text = "$wordCount kata • ± $estMinutes mnt baca",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Salin Semua Teks Button
                Surface(
                    color = if (copiedAll) NeonSuccess.copy(alpha = 0.2f) else CosmicSurface2,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (copiedAll) NeonSuccess else GlassBorder
                    ),
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(content))
                        copiedAll = true
                        Toast.makeText(context, "✓ Seluruh naskah artikel disalin ke papan klip!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (copiedAll) Icons.Default.Done else Icons.Default.ContentCopy,
                            contentDescription = "Salin Teks",
                            tint = if (copiedAll) NeonSuccess else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (copiedAll) "Tersalin" else "Salin Naskah",
                            color = if (copiedAll) NeonSuccess else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(color = GlassBorder)

            // Content Paragraphs
            val paragraphs = if (content.isNotBlank()) {
                content.split("\n\n").filter { it.isNotBlank() }
            } else {
                listOf("Belum ada teks naskah artikel yang disematkan.")
            }

            paragraphs.forEachIndexed { index, p ->
                var paragraphCopied by remember(index) { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmicDark.copy(alpha = 0.5f))
                        .border(0.5.dp, GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bagian ${index + 1}",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CosmicSurface2)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(p.trim()))
                                    paragraphCopied = true
                                    Toast.makeText(context, "✓ Paragraf disalin ke papan klip", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (paragraphCopied) Icons.Default.Done else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = if (paragraphCopied) NeonSuccess else TextTertiary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = if (paragraphCopied) "Disalin" else "Salin",
                                    color = if (paragraphCopied) NeonSuccess else TextTertiary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = p.trim(),
                        color = TextPrimary,
                        fontSize = (14 * textSizeMultiplier).sp,
                        lineHeight = (22 * textSizeMultiplier).sp,
                        textAlign = TextAlign.Justify,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}
