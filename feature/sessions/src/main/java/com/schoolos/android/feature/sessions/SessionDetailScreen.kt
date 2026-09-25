package com.schoolos.android.feature.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface
import com.schoolos.android.core.designsystem.ErrorState
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.LoadingState
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.StatusChip
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.core.designsystem.subjectGradient
import com.schoolos.android.core.designsystem.subjectIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    onBack: () -> Unit = {},
    onOpenAssignments: (String) -> Unit = {},
    onOpenQuizzes: (String) -> Unit = {},
    onOpenMaterials: (String) -> Unit = {},
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val session = state.session
    val rawSubject = session?.subjectName ?: session?.notes ?: "Pelajaran"
    val subject = rawSubject.substringBefore(" • ").substringBefore(" (Ruang").trim()
    val roomText = session?.let { listOfNotNull(it.room ?: "Ruang Kelas", it.className).joinToString(" • ") } ?: "Ruang Belajar"

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = subject,
                subtitle = "Jadwal Pembelajaran • $roomText",
                onBack = onBack,
                actions = {
                    session?.let {
                        StatusChip(label = it.status)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> {
                    ErrorState(message = state.error!!, onRetry = viewModel::load)
                }
                session != null -> {
                    val s = session
                    val gradient = subjectGradient(subject)
                    val icon = subjectIcon(subject)
                    val role = state.userRole.lowercase()
                    val isTeacher = role == "teacher" || role == "guru"
                    val accentColor = if (isTeacher) TeacherNeon else StudentNeon

                    // Wrap callbacks to pass the subject name for filtering
                    val openAssignments: (String) -> Unit = { onOpenAssignments(subject) }
                    val openQuizzes: (String) -> Unit = { onOpenQuizzes(subject) }
                    val openMaterials: (String) -> Unit = { onOpenMaterials(subject) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ── 1. COMPACT EXECUTIVE SESSION HERO CARD ────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(CosmicNavy, CosmicSurface)
                                    )
                                )
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(gradient))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = subject,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = roomText,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(NeonSuccess)
                                        )
                                        Text(
                                            text = "Sesi Terjadwal Resmi",
                                            fontSize = 11.sp,
                                            color = TextTertiary
                                        )
                                    }
                                }
                            }
                        }

                        // ── 2. ROLE-BASED SESSION CONTENT ─────────────────────
                        if (isTeacher) {
                            TeacherSessionDetailContent(
                                session = s,
                                attendance = state.attendance,
                                students = state.studentItems,
                                onUpdateAttendance = viewModel::updateAttendance,
                                onOpenAssignments = openAssignments,
                                onOpenQuizzes = openQuizzes,
                                onOpenMaterials = openMaterials,
                                accentColor = accentColor,
                            )
                        } else {
                            StudentSessionDetailContent(
                                session = s,
                                attendance = state.attendance,
                                onOpenAssignments = openAssignments,
                                onOpenQuizzes = openQuizzes,
                                onOpenMaterials = openMaterials,
                                accentColor = accentColor,
                            )
                        }

                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}