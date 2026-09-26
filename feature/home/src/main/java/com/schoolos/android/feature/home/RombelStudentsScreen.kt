package com.schoolos.android.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import com.schoolos.android.domain.model.ClassStudent

@Composable
fun RombelStudentsScreen(
    className: String? = null,
    onBack: () -> Unit,
    onNavigateToStudentDetail: (ClassStudent) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RombelStudentsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    val displayClassName = if (!className.isNullOrBlank()) className else uiState.className

    LaunchedEffect(className) {
        if (!className.isNullOrBlank() && className != uiState.className) {
            viewModel.loadStudents(className)
        }
    }

    val students = uiState.students
    val isLoading = uiState.isLoading
    val errorMessage = uiState.errorMessage
    val searchQuery = uiState.searchQuery
    val filteredStudents = uiState.filteredStudents
    val maleCount = uiState.maleCount
    val femaleCount = uiState.femaleCount

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack)
    ) {
        // Quiet Executive TopBar with compact ratio subtitle
        ExecutiveTopBar(
            title = "Daftar Siswa",
            subtitle = if (students.isNotEmpty()) "Kelas $displayClassName • ${students.size} Siswa ($maleCount L • $femaleCount P)" else "Kelas $displayClassName",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { viewModel.loadStudents(displayClassName) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmicNavy)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Muat Ulang",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        )

        // ── Minimalist Search Bar ───────────────────────────────────────
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Cari nama atau NISN...",
                        fontSize = 13.sp,
                        color = TextTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CosmicNavy,
                    unfocusedContainerColor = CosmicNavy,
                    focusedBorderColor = NeonBlue.copy(alpha = 0.5f),
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
        }

        // ── Content ─────────────────────────────────────────────────────
        when {
            isLoading && students.isEmpty() -> {
                com.schoolos.android.core.designsystem.ShimmerList(
                    count = 6,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            errorMessage != null && students.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(NeonError.copy(alpha = 0.1f))
                                .border(0.5.dp, NeonError.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = NeonError,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Gagal Memuat Data",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = errorMessage,
                            fontSize = 12.sp,
                            color = TextTertiary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Surface(
                            onClick = { viewModel.loadStudents(displayClassName) },
                            shape = RoundedCornerShape(8.dp),
                            color = CosmicNavy,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder)
                        ) {
                            Text(
                                text = "Coba Lagi",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
            filteredStudents.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CosmicNavy)
                                .border(0.5.dp, GlassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotBlank()) "Siswa Tidak Ditemukan" else "Belum Ada Siswa",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (searchQuery.isNotBlank())
                                "Tidak ada siswa yang cocok dengan \"$searchQuery\"."
                            else
                                "Belum ada siswa terdaftar di rombel $displayClassName.",
                            fontSize = 12.sp,
                            color = TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredStudents, key = { _, s -> s.id }) { index, student ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { visible = true }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(140 + index * 20)) +
                                    slideInVertically(tween(140 + index * 20)) { it / 4 }
                        ) {
                            StudentCard(
                                student = student,
                                onClick = { onNavigateToStudentDetail(student) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ultra-minimalist student row item (Apple Contacts / Linear style).
 * Highly readable, noise-free, and sleek.
 */
@Composable
private fun StudentCard(
    student: ClassStudent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFemale = student.gender?.trim()?.uppercase() == "P"
    val genderLabel = if (isFemale) "Perempuan" else "Laki-laki"
    val initial = student.fullName.firstOrNull()?.toString()?.uppercase() ?: "?"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quiet Monochromatic Avatar
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(CosmicSurface2)
                .border(0.5.dp, GlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Spacer(Modifier.width(12.dp))

        // Student Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.fullName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "NISN: ${student.nisn ?: "-"} • $genderLabel",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        // Subtle Right Arrow
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Detail",
            tint = TextTertiary,
            modifier = Modifier.size(15.dp)
        )
    }
}
