package com.schoolos.android.feature.learning

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onAskTeacher: ((materialTitle: String, materialId: String, subjectName: String, teacherName: String) -> Unit)? = null,
    viewModel: LearningViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    LaunchedEffect(materialId) {
        viewModel.loadMaterialDetail(materialId)
    }

    val materialState by viewModel.selectedMaterial.collectAsState()
    if (materialState == null) {
        Scaffold(
            containerColor = CosmicBlack,
            topBar = {
                ExecutiveTopBar(
                    title = "Memuat Modul...",
                    onBack = onBack,
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                LoadingState()
            }
        }
        return
    }

    val material = materialState!!
    val isCompleted = material.isCompleted

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

    // ── ANTI-CHEATING READING DURATION ENGINE ────────────────────────────────
    val estimatedReadingMinutes = remember(material.id) { calculateEstimatedReadingMinutes(material) }
    val estimatedReadingSeconds = (estimatedReadingMinutes * 60L).coerceAtLeast(60L)
    val minRequiredReadingSeconds = (estimatedReadingSeconds * 0.60).toLong().coerceAtLeast(30L)
    val minRequiredMinutes = minRequiredReadingSeconds / 60
    val minRequiredRemSec = minRequiredReadingSeconds % 60

    var elapsedReadingSeconds by rememberSaveable(material.id) { mutableStateOf(0L) }
    val isDurationSufficient = elapsedReadingSeconds >= minRequiredReadingSeconds
    var showCheatingWarningDialog by remember { mutableStateOf(false) }

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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Floating Icon Button: Hapus Materi
                    SmallFloatingActionButton(
                        onClick = { showDeleteDialog = true },
                        containerColor = CosmicSurface2,
                        contentColor = NeonError,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, NeonError.copy(alpha = 0.45f), CircleShape),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Materi",
                            modifier = Modifier.size(20.dp),
                            tint = NeonError
                        )
                    }

                    // Floating Icon Button: Edit Materi
                    FloatingActionButton(
                        onClick = { showEditDialog = true },
                        containerColor = TeacherNeon,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 10.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Materi",
                            modifier = Modifier.size(22.dp),
                            tint = Color.White
                        )
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
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                if (isCompleted) "Status Pembelajaran" else "Waktu Membaca (Min. 60%)",
                                color = TextTertiary,
                                fontSize = 10.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCompleted) NeonSuccess
                                            else if (isDurationSufficient) NeonSuccess
                                            else NeonWarning
                                        )
                                )
                                Spacer(Modifier.width(6.dp))
                                if (isCompleted) {
                                    Text(
                                        "✓ Selesai (Terkunci)",
                                        color = NeonSuccess,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                } else {
                                    val currentMins = elapsedReadingSeconds / 60
                                    val currentSecs = elapsedReadingSeconds % 60
                                    Text(
                                        String.format("%02d:%02d / %02d:%02d", currentMins, currentSecs, minRequiredMinutes, minRequiredRemSec),
                                        color = if (isDurationSufficient) NeonSuccess else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            if (!isCompleted) {
                                Spacer(Modifier.height(3.dp))
                                val progressFraction = (elapsedReadingSeconds.toFloat() / minRequiredReadingSeconds.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (isDurationSufficient) NeonSuccess else NeonBlue,
                                    trackColor = CosmicDark,
                                )
                            }
                        }

                        if (isCompleted) {
                            // ── PERMANENTLY LOCKED: NO BATAL / CANCEL BUTTON ALLOWED ──
                            Surface(
                                color = NeonSuccess.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonSuccess.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .height(44.dp)
                                    .clickable {
                                        Toast.makeText(
                                            context,
                                            "Materi ini telah Anda selesaikan dan status terkunci secara permanen.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Terkunci",
                                        tint = NeonSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Selesai (Terkunci)",
                                        color = NeonSuccess,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            // ── UNCOMPLETED: Anti-cheating duration gate on click ──
                            Button(
                                onClick = {
                                    if (!isDurationSufficient) {
                                        // REJECT IMMEDIATELY! Show anti-cheating warning dialog
                                        showCheatingWarningDialog = true
                                    } else {
                                        // ELIGIBLE! Mark complete permanently
                                        viewModel.markMaterialAsCompleted(material.id) { success, err ->
                                            if (success) {
                                                Toast.makeText(
                                                    context,
                                                    "✓ Hebat! Materi berhasil diselesaikan dan status sekarang terkunci.",
                                                    Toast.LENGTH_LONG
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
                                    tint = if (isDurationSufficient) Color.White else NeonWarning
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
            if (material.materialType == MaterialType.IMAGE || material.materialType == MaterialType.ARTICLE) {
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
            } else if (material.materialType == MaterialType.IMAGE && (richBlocks == null || richBlocks.isEmpty())) {
                Box(modifier = Modifier.fillMaxWidth().background(CosmicNavy)) {
                    InAppImageViewer(
                        imageUrl = mediaUrl ?: material.thumbnailUrl ?: "",
                        title = material.title,
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    )
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
                                            MaterialType.ARTICLE -> "Artikel"
                                            else -> "Gambar"
                                        },
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (isCompleted) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NeonSuccess.copy(alpha = 0.14f))
                                        .border(1.dp, NeonSuccess.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = NeonSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("✓ Selesai (Terkunci)", color = NeonSuccess, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
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

                // ── STUDENT: PERMANENTLY COMPLETED & LOCKED CARD ──────────────────────
                if (!isTeacher && isCompleted) {
                    Surface(
                        color = NeonSuccess.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonSuccess.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(NeonSuccess.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = NeonSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Modul Selesai Dipelajari (Terkunci)",
                                    color = NeonSuccess,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Status tervalidasi dan terkunci permanen untuk menjaga integritas belajar siswa.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // ── STUDENT: ANTI-CHEATING READING DURATION GATE ────────────────────────
                if (!isTeacher && !isCompleted) {
                    Surface(
                        color = CosmicNavy,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDurationSufficient) NeonSuccess.copy(alpha = 0.4f) else NeonBlue.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCheatingWarningDialog = true }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isDurationSufficient) NeonSuccess.copy(alpha = 0.15f) else NeonBlue.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDurationSufficient) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = if (isDurationSufficient) NeonSuccess else NeonBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isDurationSufficient) "Syarat Durasi Baca Terpenuhi ✓" else "Pencegahan Kecurangan Belajar",
                                            color = if (isDurationSufficient) NeonSuccess else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isDurationSufficient) "Anda siap menandai selesai modul ini" else "Wajib baca min. 60% (${minRequiredMinutes}m) dari estimasi (${estimatedReadingMinutes}m)",
                                            color = TextTertiary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Surface(
                                    color = if (isDurationSufficient) NeonSuccess.copy(alpha = 0.15f) else CosmicSurface2,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (isDurationSufficient) NeonSuccess.copy(alpha = 0.4f) else GlassBorder
                                    )
                                ) {
                                    Text(
                                        text = if (isDurationSufficient) "60% Terpenuhi" else "Min. 60%",
                                        color = if (isDurationSufficient) NeonSuccess else NeonWarning,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            val progressFraction = (elapsedReadingSeconds.toFloat() / minRequiredReadingSeconds.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (isDurationSufficient) NeonSuccess else NeonBlue,
                                trackColor = CosmicDark
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val curMin = elapsedReadingSeconds / 60
                                val curSec = elapsedReadingSeconds % 60
                                Text(
                                    text = "Waktu membaca: ${curMin}m ${curSec}s",
                                    color = if (isDurationSufficient) NeonSuccess else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!isDurationSufficient) {
                                    val remTotal = (minRequiredReadingSeconds - elapsedReadingSeconds).coerceAtLeast(0L)
                                    val remM = remTotal / 60
                                    val remS = remTotal % 60
                                    Text(
                                        text = "Sisa: ${remM}m ${remS}s lagi",
                                        color = NeonWarning,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "Siap Selesai 🔓",
                                        color = NeonSuccess,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // ── RESOLVED TEACHER NAME ───────────────────────────────────
                val descPartsForQ = (material.description ?: "").split(" • ")
                val resolvedTeacherName = material.teacherName?.takeIf { it.isNotBlank() && !it.equals("Guru Pengampu", ignoreCase = true) }
                    ?: if (descPartsForQ.size >= 3 && descPartsForQ[2].isNotBlank() && !descPartsForQ[2].equals("Guru Pengampu", ignoreCase = true)) descPartsForQ[2] else "Guru Mata Pelajaran"

                // ── RICH BLOCK CONTENT RENDERER (INFOGRAFIS / MULTI-BLOCK) ────────
                if (material.materialType == MaterialType.IMAGE) {
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
                            .clickable { onAskTeacher(material.title, material.id, material.subject, resolvedTeacherName) }
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
                                    text = "Tanya Guru 💬",
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
                onSave = { updatedTitle, updatedDesc, updatedUrl, updatedStart, updatedEnd ->
                    viewModel.updateMaterial(
                        id = material.id,
                        title = updatedTitle,
                        description = updatedDesc,
                        mediaUrl = updatedUrl,
                        startPage = updatedStart,
                        endPage = updatedEnd,
                    ) { success, err ->
                        if (success) {
                            showEditDialog = false
                            Toast.makeText(context, "✓ Materi berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Gagal: ${err ?: "Terjadi kesalahan"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        // ── DELETE CONFIRMATION DIALOG ──
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = CosmicNavy,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonError.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = NeonError, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Hapus Materi?", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        "Materi \"${material.title}\" akan dihapus secara permanen dari portal siswa. Tindakan ini tidak dapat dibatalkan.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteMaterial(material.id) { success, err ->
                                if (success) {
                                    showDeleteDialog = false
                                    Toast.makeText(context, "✓ Materi berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                } else {
                                    Toast.makeText(context, "Gagal: ${err ?: "Terjadi kesalahan"}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonError, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ya, Hapus", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Batal", color = TextTertiary, fontSize = 12.sp)
                    }
                }
            )
        }

        // ── ANTI-CHEATING WARNING ALERT DIALOG ──
        if (showCheatingWarningDialog) {
            val currentMins = elapsedReadingSeconds / 60
            val currentSecs = elapsedReadingSeconds % 60
            val remainingSeconds = (minRequiredReadingSeconds - elapsedReadingSeconds).coerceAtLeast(0L)
            val remMins = remainingSeconds / 60
            val remSecs = remainingSeconds % 60
            val progressFraction = (elapsedReadingSeconds.toFloat() / minRequiredReadingSeconds.toFloat()).coerceIn(0f, 1f)

            AlertDialog(
                onDismissRequest = { showCheatingWarningDialog = false },
                containerColor = CosmicNavy,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonWarning.copy(alpha = 0.18f))
                                .border(1.dp, NeonWarning.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = NeonWarning,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                "Peringatan Anti-Kecurangan",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Durasi Belajar Belum Mencukupi",
                                color = NeonWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
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
                            text = "Penandaan selesai DITOLAK! Sistem mendeteksi Anda baru membuka materi beberapa saat. Untuk mencegah kecurangan dan menjaga integritas belajar, Anda wajib membaca minimal 60% dari estimasi durasi belajar.",
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
                                    Text("Batas Wajib Minimal (60%):", color = TextTertiary, fontSize = 11.sp)
                                    Text("${minRequiredMinutes}m ${if (minRequiredRemSec > 0) "${minRequiredRemSec}s" else ""}", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Waktu Membaca Anda:", color = TextTertiary, fontSize = 11.sp)
                                    Text("${currentMins}m ${currentSecs}s", color = NeonWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sisa Waktu yang Diperlukan:", color = TextTertiary, fontSize = 11.sp)
                                    Text("${remMins}m ${remSecs}s lagi", color = NeonError, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
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
                            text = "💡 Pelajari materi dengan sungguh-sungguh. Setelah syarat 60% terpenuhi, Anda dapat menyelesaikan modul dan status akan terkunci secara permanen (tidak dapat dibatalkan).",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showCheatingWarningDialog = false },
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
    onSave: (title: String, description: String, mediaUrl: String?, startPage: Int?, endPage: Int?) -> Unit,
) {
    var title by remember { mutableStateOf(material.title) }
    var description by remember {
        val d = material.description ?: ""
        mutableStateOf(if (d.trim().startsWith("[") || d.trim().startsWith("{")) "" else d)
    }
    var mediaUrl by remember { mutableStateOf(material.mediaUrl ?: "") }
    var startPageStr by remember { mutableStateOf(material.startPage?.toString() ?: "") }
    var endPageStr by remember { mutableStateOf(material.endPage?.toString() ?: "") }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CosmicNavy,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TeacherNeon.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = TeacherNeon, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Edit Materi Pembelajaran", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                        .height(100.dp),
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

                // Editable Book / PDF Page Range
                Surface(
                    color = CosmicSurface2,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Rentang Halaman Buku / Modul:", color = TeacherNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            if (mediaUrl.isNotBlank()) {
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        isSaving = true
                        onSave(
                            title.trim(),
                            description.trim(),
                            mediaUrl.trim().ifBlank { null },
                            startPageStr.toIntOrNull(),
                            endPageStr.toIntOrNull()
                        )
                    }
                },
                enabled = title.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = TeacherNeon, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text("Simpan Perubahan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Batal", color = TextTertiary, fontSize = 12.sp)
            }
        }
    )
}

/**
 * Menghitung estimasi durasi membaca (dalam menit) berdasarkan tipe materi dan kuantitas konten.
 */
fun calculateEstimatedReadingMinutes(material: LearningMaterial): Int {
    return when (material.materialType) {
        MaterialType.DOCUMENT -> {
            val start = material.startPage
            val end = material.endPage
            if (start != null && end != null) {
                val pages = (end - start + 1).coerceAtLeast(1)
                // Realistis 2 menit per halaman materi modul/akademik, minimal 2 menit
                (pages * 2).coerceIn(2, 45)
            } else {
                3
            }
        }
        MaterialType.ARTICLE -> {
            val content = material.contentBody ?: material.description ?: ""
            val wordCount = content.split(Regex("\\s+")).count { it.isNotBlank() }
            if (wordCount > 30) {
                // Kecepatan membaca komprehensif ~130 kata per menit
                ((wordCount / 130.0).roundToInt()).coerceIn(2, 30)
            } else {
                2
            }
        }
        MaterialType.VIDEO -> 5
        MaterialType.IMAGE -> 2
    }
}



