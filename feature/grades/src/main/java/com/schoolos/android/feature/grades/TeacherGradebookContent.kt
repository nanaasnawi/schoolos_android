package com.schoolos.android.feature.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.AcademicClass

fun LazyListScope.teacherGradebookContent(
    classes: List<AcademicClass>,
    onSubjectClick: (String, String) -> Unit
) {
    if (classes.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada data kelas yang terdaftar.",
                    color = TextTertiary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        items(classes) { cls ->
            TeacherClassGradeCard(
                kelas = cls.name,
                mapel = "Buku Nilai",
                onClick = { onSubjectClick(cls.name, "Buku Nilai") }
            )
        }
    }
}

@Composable
private fun TeacherClassGradeCard(
    kelas: String,
    mapel: String,
    onClick: () -> Unit
) {
    val accentColor = when (kelas.hashCode().mod(3)) {
        0 -> StudentNeon
        1 -> NeonBlue
        else -> TeacherNeon
    }

    // Re-writing the card structure safely:
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = accentColor.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(20.dp))
            .background(CosmicNavy)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.1f))
                            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(kelas.take(4), fontWeight = FontWeight.Black, fontSize = 16.sp, color = accentColor)
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$mapel — $kelas",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Laporan Nilai Kelas",
                                fontSize = 11.sp,
                                color = TextTertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // CTA Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CosmicDark)
                            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Buka", fontSize = 11.sp, fontWeight = FontWeight.Black, color = accentColor)
                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = accentColor, modifier = Modifier.size(11.dp))
                        }
                    }
                }
            }
        }
    }
}
