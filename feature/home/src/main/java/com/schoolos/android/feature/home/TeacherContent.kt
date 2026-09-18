package com.schoolos.android.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Quiz
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject

fun LazyListScope.teacherContent(
    onNavigateToSessions: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToQuizzes: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAssignmentCreator: () -> Unit,
    onNavigateToQuizBuilder: () -> Unit,
    onNavigateToBroadcastCenter: () -> Unit,
    onNavigateToLearning: () -> Unit = {},
    onNavigateToRombelStudents: (String) -> Unit = {},
    activeSubject: String = "-",
    activeClass: String = "-",
    isHomeroom: Boolean = false,
    teacherClasses: List<AcademicClass> = emptyList(),
    teacherSubjects: List<AcademicSubject> = emptyList(),
) {
    val displayClass = when {
        activeClass.isNotBlank() && activeClass != "-" -> activeClass
        isHomeroom && teacherClasses.isNotEmpty() -> teacherClasses.first().name
        teacherClasses.isNotEmpty() -> teacherClasses.first().name
        else -> "KELAS VII A"
    }

    // ── 1. HOMEROOM / TEACHING CLASS HERO ────────────────────────────────────────
    item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = TeacherNeon.copy(alpha = 0.25f))
                .clip(RoundedCornerShape(22.dp))
                .background(CosmicNavy)
                .background(
                    Brush.linearGradient(
                        listOf(TeacherNeon.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .border(1.dp, TeacherNeon.copy(alpha = 0.30f), RoundedCornerShape(22.dp))
                .clickable {
                    if (displayClass.isNotBlank()) onNavigateToRombelStudents(displayClass)
                }
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TeacherNeon.copy(alpha = 0.18f))
                            .border(1.dp, TeacherNeon.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = TeacherNeon,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TeacherNeon.copy(alpha = 0.18f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = if (isHomeroom) "WALI KELAS RESMI" else "KELAS AMPUAN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TeacherNeon,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = displayClass,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Ketuk untuk melihat daftar & presensi siswa",
                            fontSize = 11.sp,
                            color = TextTertiary,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TeacherNeon.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TeacherNeon,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    // ── 2. BENTO ACTION GRID GURU ────────────────────────────────────────────────
    item {
        LightSectionHeader(
            title = "Pusat Aksi Pengajar",
            sub = "Kelola materi, tugas, dan evaluasi",
        )
    }

    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BentoActionCard(
                    title = "Buat Tugas",
                    subtitle = "Tugaskan PR/proyek",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    accentColor = TeacherNeon,
                    badgeText = "Baru",
                    onClick = onNavigateToAssignmentCreator,
                    modifier = Modifier.weight(1f),
                )

                BentoActionCard(
                    title = "Buat Kuis",
                    subtitle = "Ujian & bank soal",
                    icon = Icons.Default.Quiz,
                    accentColor = NeonWarning,
                    onClick = onNavigateToQuizBuilder,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BentoActionCard(
                    title = "Pengumuman",
                    subtitle = "Broadcast ke siswa/wali",
                    icon = Icons.Default.Campaign,
                    accentColor = NeonBlue,
                    onClick = onNavigateToBroadcastCenter,
                    modifier = Modifier.weight(1f),
                )

                BentoActionCard(
                    title = "Bahan Ajar",
                    subtitle = "Kelola modul belajar",
                    icon = Icons.Default.Book,
                    accentColor = TeacherNeon,
                    onClick = onNavigateToLearning,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // ── 3. QUICK NAVIGATION TILES ────────────────────────────────────────────────
    item {
        LightSectionHeader(
            title = "Akses Cepat",
            sub = "Menu operasional harian",
        )
    }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val quickItems = listOf(
                QuickAction("Jadwal", Icons.Default.School, TeacherNeon, onNavigateToSessions),
                QuickAction("Tugas", Icons.AutoMirrored.Filled.Assignment, TeacherNeon, onNavigateToAssignments),
                QuickAction("Kuis", Icons.Default.Quiz, NeonWarning, onNavigateToQuizzes),
                QuickAction("Nilai", Icons.Default.Assessment, NeonBlue, onNavigateToGrades),
            )
            quickItems.forEach { action ->
                LightQuickActionBtn(action = action)
            }
        }
    }
}
