package com.schoolos.android.feature.learning

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import com.schoolos.android.core.common.formatPublishTimestamp
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.LearningMaterial
import com.schoolos.android.domain.model.MaterialType
import com.schoolos.android.feature.learning.components.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Composable
fun LearningMaterialDetailScreen(
    materialId: String,
    onBack: () -> Unit = {},
    onCreateMaterial: () -> Unit = {},
    onAskTeacher: ((materialTitle: String, materialId: String, subjectName: String, teacherName: String, question: String) -> Unit)? = null,
    viewModel: LearningViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    LaunchedEffect(materialId) {
        viewModel.loadMaterialDetail(materialId)
    }

    val materialState by viewModel.selectedMaterial.collectAsState()
    val isLoadingDetail by viewModel.isLoadingMaterialDetail.collectAsState()
    val detailError by viewModel.materialDetailError.collectAsState()

    if (materialState == null) {
        val isDark = LocalIsDarkTheme.current
        val screenBg = if (isDark) CosmicBlack else Color(0xFFF8FAFC)
        Scaffold(
            containerColor = screenBg,
            topBar = {
                ExecutiveTopBar(
                    title = if (detailError != null) "Materi Pembelajaran" else "Memuat Modul...",
                    onBack = onBack,
                )
            }
        ) { padding ->
            if (detailError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) CosmicNavy else Color.White)
                            .border(1.dp, if (isDark) GlassBorder else Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(NeonWarning.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = NeonWarning,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Akses Materi Dibatasi",
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = detailError ?: "Modul ini tidak tersedia untuk rombel belajar Anda atau telah diarsipkan oleh guru pengampu.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp,
                        )
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Kembali", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                MaterialDetailShimmerSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
        return
    }

    val material = materialState!!
    val isCompleted = material.isCompleted
    val isDark = LocalIsDarkTheme.current
    val descParts = (material.description ?: "").split(" • ")
    val resolvedTeacherName = material.teacherName?.takeIf { it.isNotBlank() && !it.equals("Guru Pengampu", ignoreCase = true) }
        ?: if (descParts.size >= 3 && descParts[2].isNotBlank() && !descParts[2].equals("Guru Pengampu", ignoreCase = true)) descParts[2] else "Guru Mata Pelajaran"

    // Role-aware UX: teachers/principals supervise & monitor materials, students consume them
    val learningState by viewModel.state.collectAsState()
    val isPrincipal = com.schoolos.android.core.auth.isPrincipalRole(learningState.userRole)
    val isTeacher = com.schoolos.android.core.auth.isTeacherRole(learningState.userRole) || isPrincipal
    val materialCompletions by viewModel.materialCompletions.collectAsState()
    val isLoadingCompletions by viewModel.isLoadingCompletions.collectAsState()

    var textSizeMultiplier by remember { mutableStateOf(1.0f) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCompletionsSheet by remember { mutableStateOf(false) }
    var isSpeedDialOpen by remember { mutableStateOf(false) }
    var showAskTeacherDialog by remember { mutableStateOf(false) }
    var tutorQuestionDraft by remember { mutableStateOf("") }

    // ── ANTI-CHEATING READING DURATION ENGINE ────────────────────────────────
    val estimatedReadingMinutes = remember(material.id) { calculateEstimatedReadingMinutes(material) }
    val estimatedReadingSeconds = (estimatedReadingMinutes * 60L).coerceAtLeast(60L)
    val minRequiredReadingSeconds = (estimatedReadingSeconds * 0.60).toLong().coerceAtLeast(30L)
    val minRequiredMinutes = minRequiredReadingSeconds / 60
    val minRequiredRemSec = minRequiredReadingSeconds % 60

    var elapsedReadingSeconds by rememberSaveable(material.id) { mutableStateOf(0L) }
    val isDurationSufficient = elapsedReadingSeconds >= minRequiredReadingSeconds
    var showDurationWarningDialog by remember { mutableStateOf(false) }

    // Pause timer when app is not in foreground to ensure authentic student reading engagement
    var isAppInForeground by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppInForeground = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(material.id, isCompleted) {
        if (!isCompleted) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                if (isAppInForeground) {
                    elapsedReadingSeconds++
                }
            }
        }
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = if (isPrincipal) "Monitoring Modul Ajar" else "Isi Modul Pembelajaran",
                subtitle = if (isPrincipal) "Supervisi Keterbacaan • ${material.subject}" else material.subject,
                onBack = onBack,
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                            .clickable { if (textSizeMultiplier > 0.85f) textSizeMultiplier -= 0.15f }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("A-", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicSurface2)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                            .clickable { if (textSizeMultiplier < 1.4f) textSizeMultiplier += 0.15f }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("A+", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        },
        floatingActionButton = {
            if (isTeacher) {
                val isDark = LocalIsDarkTheme.current
                val speedDialCardBg = if (isDark) Color(0xFF141416) else Color.White
                val speedDialBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
                val primaryColor = if (isDark) Color(0xFF059669) else Color(0xFF10B981)
                val primaryBorder = if (isDark) Color(0xFF34D399).copy(alpha = 0.35f) else Color(0xFF059669).copy(alpha = 0.20f)
                val deleteBg = if (isDark) Color(0xFF3B1215) else Color(0xFFFEE2E2)
                val deleteBorder = if (isDark) Color(0xFFEF4444).copy(alpha = 0.45f) else Color(0xFFF87171).copy(alpha = 0.40f)

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    AnimatedVisibility(
                        visible = isSpeedDialOpen,
                        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 2 },
                        exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { it / 2 },
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Action: Hapus Materi
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        isSpeedDialOpen = false
                                        showDeleteDialog = true
                                    }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = speedDialCardBg,
                                    border = BorderStroke(1.dp, speedDialBorder),
                                    shadowElevation = 0.dp,
                                ) {
                                    Text(
                                        text = "Hapus Materi",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonError,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = deleteBg,
                                    border = BorderStroke(1.dp, deleteBorder),
                                    shadowElevation = 0.dp,
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus Materi",
                                            tint = NeonError,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // 2. Action: Edit Materi
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        isSpeedDialOpen = false
                                        showEditDialog = true
                                    }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = speedDialCardBg,
                                    border = BorderStroke(1.dp, speedDialBorder),
                                    shadowElevation = 0.dp,
                                ) {
                                    Text(
                                        text = "Edit Materi",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = primaryColor,
                                    border = BorderStroke(1.dp, primaryBorder),
                                    shadowElevation = 0.dp,
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Materi",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Main Executive Floating Button
                    val rotation by animateFloatAsState(
                        targetValue = if (isSpeedDialOpen) 135f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "fabRotation"
                    )

                    Surface(
                        onClick = { isSpeedDialOpen = !isSpeedDialOpen },
                        shape = CircleShape,
                        color = if (isSpeedDialOpen) (if (isDark) Color(0xFF27272A) else Color(0xFFF1F5F9)) else primaryColor,
                        border = BorderStroke(1.dp, if (isSpeedDialOpen) speedDialBorder else primaryBorder),
                        shadowElevation = 0.dp,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSpeedDialOpen) Icons.Default.Close else Icons.Default.Tune,
                                contentDescription = if (isSpeedDialOpen) "Tutup Menu" else "Aksi Materi",
                                tint = if (isSpeedDialOpen) (if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)) else Color.White,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer { rotationZ = rotation }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = CosmicNavy,
                tonalElevation = 10.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTeacher) {
                        // ── TEACHER: Class utilization overview (clean, spacious, zero collision) ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showCompletionsSheet = true }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val completedCount = materialCompletions.count { it.isCompleted }
                            val totalCount = materialCompletions.size
                            val hasData = totalCount > 0

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(TeacherNeon)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Pemanfaatan Kelas",
                                        color = TextTertiary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    if (hasData) "$completedCount / $totalCount siswa selesai"
                                    else if (material.completedCount > 0) "${material.completedCount} siswa menyelesaikan"
                                    else "Belum ada siswa yang menyelesaikan",
                                    color = TeacherNeon,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }

                            // Interactive pill button to open student completion breakdown
                            Surface(
                                color = TeacherNeon.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, TeacherNeon.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Detail Siswa",
                                        color = TeacherNeon,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = TeacherNeon,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // ── STUDENT: learning progress + mark-as-done ──
                        if (isCompleted) {
                            Column {
                                Text("Status Pembelajaran", color = TextTertiary, fontSize = 11.sp)
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NeonSuccess)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Selesai",
                                        color = NeonSuccess,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 14.dp)
                            ) {
                                Text(
                                    "Waktu Membaca (Min. 60%)",
                                    color = TextTertiary,
                                    fontSize = 10.sp
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isDurationSufficient) NeonSuccess else NeonBlue
                                            )
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    val currentMins = elapsedReadingSeconds / 60
                                    val currentSecs = elapsedReadingSeconds % 60
                                    Text(
                                        String.format("%02d:%02d / %02d:%02d", currentMins, currentSecs, minRequiredMinutes, minRequiredRemSec),
                                        color = if (isDurationSufficient) NeonSuccess else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                val progressFraction = (elapsedReadingSeconds.toFloat() / minRequiredReadingSeconds.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (isDurationSufficient) NeonSuccess else NeonBlue,
                                    trackColor = CosmicDark,
                                )
                            }

                            Button(
                                onClick = {
                                    if (!isDurationSufficient) {
                                        showDurationWarningDialog = true
                                    } else {
                                        viewModel.markMaterialAsCompleted(material.id) { success, err ->
                                            if (success) {
                                                Toast.makeText(
                                                    context,
                                                    "Materi berhasil diselesaikan!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Gagal: ${err ?: "Terjadi kesalahan"}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDurationSufficient) NeonBlue else CosmicSurface2,
                                    contentColor = if (isDurationSufficient) Color.White else TextSecondary
                                ),
                                border = if (!isDurationSufficient) androidx.compose.foundation.BorderStroke(1.dp, GlassBorder) else null,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDurationSufficient) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isDurationSufficient) Color.White else NeonBlue
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Tandai Selesai",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val mediaUrl = material.mediaUrl

            // ── PARSED RICH CONTENT (INFOGRAFIS / ARTIKEL) ───────────────────
            var richBlocks: List<ContentBlock>? = null
            if (material.materialType == MaterialType.IMAGE || material.materialType == MaterialType.ARTICLE || material.materialType == MaterialType.INFOGRAPHIC) {
                val candidateBody = material.contentBody ?: material.description
                if (!candidateBody.isNullOrBlank()) {
                    try {
                        val json = Json { ignoreUnknownKeys = true; isLenient = true }
                        richBlocks = json.decodeFromString<List<ContentBlock>>(candidateBody)
                    } catch (_: Exception) {
                        try {
                            val arr = org.json.JSONArray(candidateBody)
                            val list = mutableListOf<ContentBlock>()
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val id = obj.optString("id", "block-$i")
                                val rawType = obj.optString("type", obj.optString("block_type", "TEXT")).uppercase()
                                val type = if (rawType == "IMAGE") ContentBlockType.IMAGE else ContentBlockType.TEXT
                                val content = obj.optString("content", "")
                                list.add(ContentBlock(id = id, type = type, content = content))
                            }
                            richBlocks = list
                        } catch (_: Exception) {}
                    }
                }
            }

            // ── VIDEO OR IMAGE VIEWER ─────────────────────────────────────────
            if (material.materialType == MaterialType.VIDEO) {
                Box(modifier = Modifier.fillMaxWidth().background(Color.Black)) {
                    if (!mediaUrl.isNullOrBlank()) {
                        InAppVideoPlayer(
                            videoUrl = mediaUrl,
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                .background(CosmicNavy),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(44.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Tautan video pembelajaran belum disematkan oleh pengampu.", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else if ((material.materialType == MaterialType.IMAGE || material.materialType == MaterialType.INFOGRAPHIC) && (richBlocks == null || richBlocks.isEmpty())) {
                val candidateImg = mediaUrl ?: material.thumbnailUrl
                if (!candidateImg.isNullOrBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().background(CosmicNavy)) {
                        InAppImageViewer(
                            imageUrl = candidateImg,
                            title = material.title,
                            modifier = Modifier.fillMaxWidth().height(260.dp)
                        )
                    }
                }
            }

            // ── MAIN CONTENT CONTAINER ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                // ── HERO MATERIAL OVERVIEW CARD ─────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicNavy)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        // Top Meta Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NeonBlue.copy(alpha = 0.12f))
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(material.subject, color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CosmicDark)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        when (material.materialType) {
                                            MaterialType.DOCUMENT -> "PDF"
                                            MaterialType.VIDEO -> "Video"
                                            MaterialType.INFOGRAPHIC -> "Infografis"
                                            MaterialType.ARTICLE -> "Artikel"
                                            else -> "Gambar"
                                        },
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Material Title
                        Text(
                            text = material.title,
                            fontSize = (18 * textSizeMultiplier).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            lineHeight = (25 * textSizeMultiplier).sp
                        )

                        Spacer(Modifier.height(14.dp))

                        // Divider
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))

                        Spacer(Modifier.height(12.dp))

                        // Teacher & Class Pill Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val descParts = (material.description ?: "").split(" • ")
                            val teacherName = material.teacherName?.takeIf { it.isNotBlank() && !it.equals("Guru Pengampu", ignoreCase = true) }
                                ?: if (descParts.size >= 3 && descParts[2].isNotBlank() && !descParts[2].equals("Guru Pengampu", ignoreCase = true)) descParts[2] else "Guru Mata Pelajaran"
                            val className = material.className?.ifBlank { null }
                                ?: if (descParts.size >= 2 && descParts[1].isNotBlank()) descParts[1] else "Semua Rombel"

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(TeacherNeon.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.School, null, tint = TeacherNeon, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(teacherName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Default.Verified, null, tint = NeonBlue, modifier = Modifier.size(14.dp))
                                    }
                                    Text("Pengampu • $className", color = TextTertiary, fontSize = 11.sp)
                                    if (!material.createdAt.isNullOrBlank()) {
                                        Spacer(Modifier.height(3.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = TextTertiary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "Diterbitkan: ${formatPublishTimestamp(material.createdAt)}",
                                                color = TextTertiary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NeonSuccess.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Modul Aktif", color = NeonSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                            }
                        }
                    }
                }

                // ── RICH BLOCK CONTENT RENDERER (INFOGRAFIS / MULTI-BLOCK) ────────
                if (material.materialType == MaterialType.IMAGE || material.materialType == MaterialType.INFOGRAPHIC) {
                    val effectiveBlocks = if (richBlocks != null && richBlocks.isNotEmpty()) {
                        richBlocks
                    } else if (!material.mediaUrl.isNullOrBlank()) {
                        listOf(ContentBlock(type = ContentBlockType.IMAGE, content = material.mediaUrl ?: ""))
                    } else {
                        emptyList()
                    }

                    InfographicMagazineReaderView(
                        title = material.title,
                        subtitle = if (material.description?.trim()?.startsWith("[") == true) null else material.description,
                        subjectName = material.subject,
                        className = material.className ?: "Semua Rombel",
                        teacherName = resolvedTeacherName,
                        blocks = effectiveBlocks,
                        textSizeMultiplier = textSizeMultiplier
                    )
                }

                // ── MONITORING KETERBACAAN SISWA (Role: Guru) ────────────────
                if (isTeacher) {
                    TeacherCompletionsTrackerCard(
                        completions = materialCompletions,
                        isLoading = isLoadingCompletions,
                        className = material.className,
                        onOpenFullSheet = { showCompletionsSheet = true }
                    )
                }

                // ── TANYA GURU / KONSULTASI MATERI (In-App Q&A) ──────────────
                if (!isTeacher && onAskTeacher != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(TeacherNeon.copy(alpha = 0.08f))
                            .border(1.dp, TeacherNeon.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .clickable {
                                tutorQuestionDraft = "Halo $resolvedTeacherName, saya ingin bertanya mengenai materi '${material.title}' ini karena ada bagian yang belum saya pahami:"
                                showAskTeacherDialog = true
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(TeacherNeon.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = TeacherNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Ada Bagian yang Belum Dipahami?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Tanya $resolvedTeacherName langsung tanpa keluar app",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TeacherNeon)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Tanya Tutor Pamong",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // ── RINGKASAN & TUJUAN PEMBELAJARAN (Structured) ───────────
                val matDesc = material.description
                val isDescJson = matDesc?.trim()?.let { it.startsWith("[") || it.startsWith("{") } == true
                if (!matDesc.isNullOrBlank() && !isDescJson && material.materialType != MaterialType.IMAGE) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CosmicNavy)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Capaian & Tujuan Pembelajaran",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            val bulletPoints = matDesc.split("\n").filter { it.isNotBlank() }
                            if (bulletPoints.size > 1) {
                                bulletPoints.forEach { point ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 6.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(NeonBlue)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            point.trim().removePrefix("- ").removePrefix("• "),
                                            color = TextSecondary,
                                            fontSize = (12 * textSizeMultiplier).sp,
                                            lineHeight = (18 * textSizeMultiplier).sp
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = matDesc,
                                    color = TextSecondary,
                                    fontSize = (12 * textSizeMultiplier).sp,
                                    lineHeight = (18 * textSizeMultiplier).sp
                                )
                            }
                        }
                    }
                }

                // ── IN-APP DOCUMENT VIEWER ───────────────────────────────────
                if (material.materialType == MaterialType.DOCUMENT) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CosmicNavy)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Description, null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Dokumen Materi Ajar", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CosmicDark)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        if (material.startPage != null && material.endPage != null)
                                            "Hal. ${material.startPage} — ${material.endPage}"
                                        else "Buku Resmi",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            InAppPdfViewer(
                                pdfUrl = mediaUrl ?: "",
                                title = material.title,
                                subject = material.subject,
                                description = material.description ?: "",
                                startPage = material.startPage,
                                endPage = material.endPage,
                                isTeacher = isTeacher,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ── ARTICLE TEXT READER ──────────────────────────────────────
                if (material.materialType == MaterialType.ARTICLE) {
                    val articleContent = if (richBlocks != null && richBlocks.isNotEmpty()) {
                        richBlocks.filter { it.type.name == "TEXT" }.joinToString("\n\n") { it.content }
                    } else {
                        val raw = material.contentBody ?: material.description ?: ""
                        if (raw.trim().startsWith("[") || raw.trim().startsWith("{")) "" else raw
                    }
                    ArticleReaderView(
                        title = material.title,
                        content = articleContent,
                        textSizeMultiplier = textSizeMultiplier
                    )
                }

                Spacer(Modifier.height(84.dp))
            }
        }

        // ── EDIT MATERIAL DIALOG ──
        if (showEditDialog) {
            EditMaterialDialog(
                material = material,
                onDismiss = { showEditDialog = false },
                onSave = { updatedTitle, updatedDesc, updatedUrl, updatedStart, updatedEnd, updatedVisibility, updatedReleaseAt ->
                    viewModel.updateMaterial(
                        id = material.id,
                        title = updatedTitle,
                        description = updatedDesc,
                        mediaUrl = updatedUrl,
                        startPage = updatedStart,
                        endPage = updatedEnd,
                        visibility = updatedVisibility,
                        releaseAt = updatedReleaseAt,
                    ) { success, err ->
                        if (success) {
                            showEditDialog = false
                            Toast.makeText(context, "Materi berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Gagal: ${err ?: "Terjadi kesalahan"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        // ── DELETE CONFIRMATION DIALOG ──
        if (showDeleteDialog) {
            SchoolOsConfirmationDialog(
                title = "Hapus Materi Pembelajaran?",
                message = "Materi \"${material.title}\" akan dihapus secara permanen dari portal siswa. Tindakan ini tidak dapat dibatalkan.",
                confirmText = "Ya, Hapus",
                dismissText = "Batal",
                icon = Icons.Default.Delete,
                accentColor = NeonError,
                onConfirm = {
                    viewModel.deleteMaterial(material.id) { success, err ->
                        if (success) {
                            showDeleteDialog = false
                            Toast.makeText(context, "Materi berhasil dihapus!", Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            Toast.makeText(context, "Gagal: ${err ?: "Terjadi kesalahan"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = { showDeleteDialog = false }
            )
        }

        // ── CUSTOM TANYA TUTOR PAMONG CONFIRMATION & EDIT FORM DIALOG ──
        if (showAskTeacherDialog) {
            val askDialogCardBg = if (isDark) Color(0xFF161618) else Color.White
            val askDialogCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
            val askDialogTextPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
            val askDialogTextSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
            val askDialogTextTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
            val askDialogInnerBg = if (isDark) Color(0xFF1E1E22) else Color(0xFFF8FAFC)
            val askDialogInnerBorder = if (isDark) Color(0xFF2F2F36) else Color(0xFFCBD5E1)
            val askDialogAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)

            Dialog(
                onDismissRequest = { showAskTeacherDialog = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = askDialogCardBg,
                    border = BorderStroke(1.dp, askDialogCardBorder),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(askDialogAccent.copy(alpha = 0.15f))
                                        .border(1.dp, askDialogAccent.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = askDialogAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Tanya Tutor Pamong",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = askDialogTextPrimary
                                    )
                                    Text(
                                        text = "Konsultasi materi dengan $resolvedTeacherName",
                                        fontSize = 11.5.sp,
                                        color = askDialogTextSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showAskTeacherDialog = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = askDialogTextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Context Badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = askDialogInnerBg,
                            border = BorderStroke(1.dp, askDialogInnerBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = askDialogAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${material.subject} • ${material.title}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = askDialogTextPrimary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Question Form Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Draft / Edit Pertanyaan Anda",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = askDialogTextSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp, max = 170.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(askDialogInnerBg)
                                    .border(1.dp, askDialogInnerBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = tutorQuestionDraft,
                                    onValueChange = { tutorQuestionDraft = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = askDialogTextPrimary,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (tutorQuestionDraft.isEmpty()) {
                                            Text(
                                                text = "Tulis pertanyaan atau bagian modul yang belum dipahami di sini...",
                                                color = askDialogTextTertiary,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        // Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { showAskTeacherDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                color = askDialogInnerBg,
                                border = BorderStroke(1.dp, askDialogInnerBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Batal",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = askDialogTextSecondary
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    val finalQuestion = tutorQuestionDraft.trim().ifBlank {
                                        "Halo $resolvedTeacherName, saya ingin bertanya mengenai materi '${material.title}' ini karena ada bagian yang belum saya pahami."
                                    }
                                    showAskTeacherDialog = false
                                    onAskTeacher?.invoke(material.title, material.id, material.subject, resolvedTeacherName, finalQuestion)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = askDialogAccent,
                                shadowElevation = 0.dp,
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Kirim Pertanyaan",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── READING DURATION INFORMATION DIALOG ──
        if (showDurationWarningDialog) {
            val currentMins = elapsedReadingSeconds / 60
            val currentSecs = elapsedReadingSeconds % 60
            val remainingSeconds = (minRequiredReadingSeconds - elapsedReadingSeconds).coerceAtLeast(0L)
            val remMins = remainingSeconds / 60
            val remSecs = remainingSeconds % 60
            val progressFraction = (elapsedReadingSeconds.toFloat() / minRequiredReadingSeconds.toFloat()).coerceIn(0f, 1f)

            AlertDialog(
                onDismissRequest = { showDurationWarningDialog = false },
                containerColor = CosmicNavy,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonBlue.copy(alpha = 0.15f))
                                .border(1.dp, NeonBlue.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = NeonBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                "Pelajari Materinya Dulu",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Waktu baca minimal belum tercapai",
                                color = NeonBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Silakan luangkan waktu sejenak untuk membaca isi materi ini sebelum menandai selesai (minimal 60% dari estimasi durasi belajar).",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Surface(
                            color = CosmicDark,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Estimasi Waktu Belajar:", color = TextTertiary, fontSize = 11.sp)
                                    Text("$estimatedReadingMinutes Menit", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Waktu Baca Minimal (60%):", color = TextTertiary, fontSize = 11.sp)
                                    Text("${minRequiredMinutes}m ${if (minRequiredRemSec > 0) "${minRequiredRemSec}s" else ""}", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Waktu Membaca Anda:", color = TextTertiary, fontSize = 11.sp)
                                    Text("${currentMins}m ${currentSecs}s", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sisa Waktu yang Diperlukan:", color = TextTertiary, fontSize = 11.sp)
                                    Text("${remMins}m ${remSecs}s lagi", color = NeonWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = NeonBlue,
                                    trackColor = CosmicSurface2
                                )
                            }
                        }

                        Text(
                            text = "Setelah waktu membaca minimal terpenuhi, tombol Tandai Selesai akan aktif.",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showDurationWarningDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Lanjutkan Membaca", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // ── ROSTER KETERBACAAN SISWA BOTTOM SHEET (Guru) ──
        if (showCompletionsSheet) {
            MaterialCompletionsBottomSheet(
                completions = materialCompletions,
                onDismiss = { showCompletionsSheet = false }
            )
        }

        // ── XP CELEBRATION & CONFETTI (Gamifikasi) ──
        val xpReward by viewModel.xpRewardEvent.collectAsState()
        XpCelebrationDialog(
            reward = xpReward,
            onDismiss = { viewModel.clearXpReward() },
        )
    }
}

@Composable
private fun EditMaterialDialog(
    material: LearningMaterial,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, mediaUrl: String?, startPage: Int?, endPage: Int?, visibility: String, releaseAt: String) -> Unit,
) {
    var title by remember { mutableStateOf(material.title) }
    var description by remember {
        val d = material.description ?: ""
        mutableStateOf(if (d.trim().startsWith("[") || d.trim().startsWith("{")) "" else d)
    }
    var mediaUrl by remember { mutableStateOf(material.mediaUrl ?: "") }
    var startPageStr by remember { mutableStateOf(material.startPage?.toString() ?: "") }
    var endPageStr by remember { mutableStateOf(material.endPage?.toString() ?: "") }
    var isScheduled by remember { mutableStateOf(material.isScheduled) }
    var scheduledDate by remember {
        val initDate = material.releaseAt?.let {
            try { java.time.Instant.parse(it).atZone(java.time.ZoneId.of("Asia/Jakarta")).toLocalDate() } catch (_: Exception) { null }
        } ?: java.time.LocalDate.now().plusDays(1)
        mutableStateOf(initDate)
    }
    var scheduledTime by remember {
        val initTime = material.releaseAt?.let {
            try { java.time.Instant.parse(it).atZone(java.time.ZoneId.of("Asia/Jakarta")).toLocalTime() } catch (_: Exception) { null }
        } ?: java.time.LocalTime.of(7, 0)
        mutableStateOf(initTime)
    }
    var showPdfReaderPreview by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    if (showPdfReaderPreview && mediaUrl.isNotBlank()) {
        BookReaderDialog(
            title = title.ifBlank { "Pratinjau Buku" },
            pdfUrl = mediaUrl,
            subject = material.subject,
            startPage = startPageStr.toIntOrNull(),
            endPage = endPageStr.toIntOrNull(),
            onDismiss = { showPdfReaderPreview = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = CosmicNavy,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TeacherNeon.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = TeacherNeon, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("Edit Materi Pembelajaran", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Pembaruan modul, berkas, & jadwal tayang", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextTertiary, modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                // Form Fields (Scrollable)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Judul Materi *", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TeacherNeon,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi / Petunjuk", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TeacherNeon,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = mediaUrl,
                        onValueChange = { mediaUrl = it },
                        label = { Text("URL Media / File / Video (opsional)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TeacherNeon,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Jadwal Tayang & Status Rilis Selector
                    PublishingScheduleSelector(
                        isScheduled = isScheduled,
                        onScheduleModeChange = { isScheduled = it },
                        scheduledDate = scheduledDate,
                        onDateChange = { scheduledDate = it },
                        scheduledTime = scheduledTime,
                        onTimeChange = { scheduledTime = it }
                    )

                    // Editable Book / PDF Page Range (Hanya tampil jika dokumen PDF / Buku SIBI)
                    val isBookOrPdf = (material.materialType == MaterialType.DOCUMENT ||
                        mediaUrl.contains(".pdf", ignoreCase = true) ||
                        ((material.startPage ?: 0) > 0)) &&
                        material.materialType != MaterialType.INFOGRAPHIC &&
                        material.materialType != MaterialType.ARTICLE &&
                        material.materialType != MaterialType.VIDEO

                    if (isBookOrPdf) {
                        Surface(
                            color = CosmicSurface2,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Rentang Halaman Buku / Modul:", color = TeacherNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    if (mediaUrl.isNotBlank() && mediaUrl.contains(".pdf", ignoreCase = true)) {
                                        OutlinedButton(
                                            onClick = { showPdfReaderPreview = true },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TeacherNeon),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, TeacherNeon.copy(alpha = 0.5f))
                                        ) {
                                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Buka Isi Buku", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = startPageStr,
                                        onValueChange = { startPageStr = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Hal. Mulai", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = TeacherNeon,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                    OutlinedTextField(
                                        value = endPageStr,
                                        onValueChange = { endPageStr = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Hal. Selesai", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = TeacherNeon,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Batal", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                isSaving = true
                                val finalVisibility = if (isScheduled) "draft" else "published"
                                val finalReleaseAt = if (isScheduled) {
                                    com.schoolos.android.core.designsystem.buildScheduledUtcIso(scheduledDate, scheduledTime)
                                } else {
                                    java.time.Instant.now().toString()
                                }
                                onSave(
                                    title.trim(),
                                    description.trim(),
                                    mediaUrl.trim().ifBlank { null },
                                    startPageStr.toIntOrNull(),
                                    endPageStr.toIntOrNull(),
                                    finalVisibility,
                                    finalReleaseAt
                                )
                            }
                        },
                        enabled = title.isNotBlank() && !isSaving,
                        modifier = Modifier.weight(1.4f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TeacherNeon, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Simpan Perubahan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Menghitung estimasi durasi membaca (dalam menit) secara realistis berdasarkan tipe materi dan kuantitas konten.
 */
fun calculateEstimatedReadingMinutes(material: LearningMaterial): Int {
    return when (material.materialType) {
        MaterialType.DOCUMENT -> {
            val start = material.startPage
            val end = material.endPage
            if (start != null && end != null) {
                val pages = (end - start + 1).coerceAtLeast(1)
                when {
                    pages <= 3 -> 2
                    pages <= 7 -> 3
                    pages <= 12 -> 4
                    else -> 5
                }
            } else {
                3
            }
        }
        MaterialType.ARTICLE -> {
            val content = material.contentBody ?: material.description ?: ""
            val wordCount = content.split(Regex("\\s+")).count { it.isNotBlank() }
            when {
                wordCount <= 100 -> 2
                wordCount <= 300 -> 3
                else -> 4
            }
        }
        MaterialType.VIDEO -> 4
        MaterialType.IMAGE, MaterialType.INFOGRAPHIC -> 2
    }
}

/**
 * Dedicated high-precision skeleton loader for Learning Material Detail Screen.
 * Perfectly mirrors the actual reading layout (Header, Cover Media, Content Blocks).
 */
@Composable
private fun MaterialDetailShimmerSkeleton(
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) CosmicNavy else Color.White
    val cardBorder = if (isDark) GlassBorder else Color(0xFFE2E8F0)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Header Card (Tags, Title, Teacher & Date Meta)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Top Tag Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShimmerBox(modifier = Modifier.width(96.dp).height(24.dp), shape = RoundedCornerShape(6.dp))
                    ShimmerBox(modifier = Modifier.width(72.dp).height(24.dp), shape = RoundedCornerShape(6.dp))
                }

                Spacer(Modifier.height(2.dp))

                // Module Title (2 Lines)
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.92f).height(20.dp), shape = RoundedCornerShape(5.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.68f).height(18.dp), shape = RoundedCornerShape(5.dp))

                Spacer(Modifier.height(4.dp))

                // Teacher Avatar + Name + Reading Time Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ShimmerBox(modifier = Modifier.size(36.dp), shape = CircleShape)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ShimmerBox(modifier = Modifier.width(130.dp).height(12.dp), shape = RoundedCornerShape(4.dp))
                        ShimmerBox(modifier = Modifier.width(90.dp).height(10.dp), shape = RoundedCornerShape(4.dp))
                    }
                }
            }
        }

        // 2. Featured Image / Hero Media Skeleton Block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ShimmerBox(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
            )
        }

        // 3. Module Reading Content Paragraphs (Simulating Book / Article text)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Section Subtitle
                ShimmerBox(modifier = Modifier.width(150.dp).height(15.dp), shape = RoundedCornerShape(4.dp))
                Spacer(Modifier.height(2.dp))

                // Paragraph 1
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(11.dp), shape = RoundedCornerShape(4.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.96f).height(11.dp), shape = RoundedCornerShape(4.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.88f).height(11.dp), shape = RoundedCornerShape(4.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.72f).height(11.dp), shape = RoundedCornerShape(4.dp))

                Spacer(Modifier.height(6.dp))

                // Paragraph 2
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(11.dp), shape = RoundedCornerShape(4.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.92f).height(11.dp), shape = RoundedCornerShape(4.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f).height(11.dp), shape = RoundedCornerShape(4.dp))
            }
        }
    }
}



