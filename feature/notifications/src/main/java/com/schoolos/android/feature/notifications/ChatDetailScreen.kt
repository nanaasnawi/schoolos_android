package com.schoolos.android.feature.notifications

import androidx.compose.material3.Surface

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.schoolos.android.core.common.BuildConfig
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import com.schoolos.android.core.designsystem.CustomBackButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.chat.ChatManager
import com.schoolos.android.core.chat.ChatMessage
import com.schoolos.android.core.chat.ChatThread
import com.schoolos.android.core.chat.InquiryStatus
import com.schoolos.android.core.chat.InquiryType
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassBorder2
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChatDetailScreen(
    threadId: String,
    chatManager: ChatManager,
    onBack: () -> Unit,
    onOpenReference: (type: InquiryType, refId: String?) -> Unit = { _, _ -> },
    isTeacherMode: Boolean = true,
    initialRecipientName: String = "",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val threads by chatManager.threads.collectAsState()
    val thread = threads.find { it.id == threadId }

    val isPrincipalAi = threadId.startsWith("ai-principal") || thread?.teacherName?.contains("Kepala Sekolah") == true || initialRecipientName.contains("Kepala Sekolah")
    val isTeacherAi = threadId.startsWith("ai-teacher") || (isTeacherMode && (threadId.startsWith("ai-tutor") || threadId.startsWith("ai-teacher") || initialRecipientName.contains("Copilot") || initialRecipientName.contains("AI Copilot")))
    val isAiThread = threadId.startsWith("ai-tutor") || threadId.startsWith("ai-principal") || threadId.startsWith("ai-teacher") || thread?.teacherName?.contains("Guru AI") == true || isPrincipalAi || isTeacherAi
    var isAiThinking by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var previewFullImageUrl by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedImageUri = uri
    }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(threadId) { chatManager.loadThreadDetail(threadId) }
    androidx.compose.runtime.DisposableEffect(chatManager, threadId) {
        chatManager.startPolling()
        chatManager.setPresence(threadId, isOnline = true)
        onDispose {
            chatManager.stopPolling()
            chatManager.setPresence(threadId, isOnline = false)
        }
    }

    val isParticipantOnline by chatManager.observeThreadPresence(
        threadId = threadId,
        isTeacherMode = isTeacherMode,
        targetUserId = if (isTeacherMode) thread?.studentId else thread?.teacherId,
    ).collectAsState(initial = false)

    LaunchedEffect(thread?.messages?.size) {
        val size = thread?.messages?.size ?: 0
        if (size > 0) listState.animateScrollToItem(size - 1)
    }

    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val isKeyboardOpen = remember {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    LaunchedEffect(isKeyboardOpen.value) {
        if (isKeyboardOpen.value) {
            val size = thread?.messages?.size ?: 0
            if (size > 0) {
                listState.scrollToItem(size - 1)
            }
        }
    }

    val contactName = if (isPrincipalAi) {
        "AI Analitik Kepala Sekolah"
    } else if (isTeacherAi) {
        "AI Copilot Guru"
    } else if (isTeacherMode) {
        thread?.studentName?.ifBlank { null } ?: initialRecipientName.ifBlank { null } ?: "Siswa"
    } else {
        val tName = thread?.teacherName
        if (!tName.isNullOrBlank() && !tName.equals("Guru Pengampu", ignoreCase = true) && !tName.equals("Bapak/Ibu Guru", ignoreCase = true)) {
            tName
        } else if (initialRecipientName.isNotBlank() && !initialRecipientName.equals("Guru Pengampu", ignoreCase = true) && !initialRecipientName.equals("Bapak/Ibu Guru", ignoreCase = true)) {
            initialRecipientName
        } else {
            "Guru Mata Pelajaran"
        }
    }
    val contactInitial = contactName.trim().take(1).uppercase().ifBlank { "?" }
    val statusText = if (isParticipantOnline) "Aktif" else "Tidak Aktif"
    val contactSubtitle = if (isPrincipalAi) {
        "Asisten Eksekutif Supervisi & Analitik Real-Time • NVIDIA NIM"
    } else if (isTeacherAi) {
        "Asisten Pedagogik & Administrasi Guru • Kurikulum Merdeka"
    } else if (isAiThread) {
        "Asisten Pintar 24 Jam • NVIDIA NIM"
    } else if (isTeacherMode) {
        "Siswa • ${thread?.studentClass?.ifBlank { "Rombel" } ?: "Rombel"} • $statusText"
    } else {
        "${thread?.subjectName?.ifBlank { "Mata Pelajaran" } ?: "Mata Pelajaran"} • $statusText"
    }
    val roleNeon = if (isPrincipalAi) Color(0xFF8B5CF6) else if (isTeacherAi) TeacherNeon else if (isTeacherMode) StudentNeon else TeacherNeon

    val rawAvatarUrl = if (isTeacherMode) thread?.studentAvatarUrl else thread?.teacherAvatarUrl
    val fullAvatarUrl = remember(rawAvatarUrl) {
        if (!rawAvatarUrl.isNullOrBlank()) {
            if (rawAvatarUrl.startsWith("http://", ignoreCase = true) || rawAvatarUrl.startsWith("https://", ignoreCase = true)) {
                rawAvatarUrl
            } else {
                val host = com.schoolos.android.core.common.BuildConfig.API_BASE_URL.substringBefore("/api/").trimEnd('/')
                "$host/${rawAvatarUrl.trimStart('/')}"
            }
        } else null
    }

    val isPermitted = isTeacherMode || thread == null || chatManager.isThreadOwnedByCurrentStudent(thread)

    val isDark = com.schoolos.android.core.designsystem.LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val studentAccent = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    if (!isPermitted) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC))
                .statusBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Percakapan Terisolasi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Percakapan tanya jawab ini bersifat privat dan terisolasi khusus untuk siswa yang bersangkutan.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudentNeon)
                    .clickable { onBack() }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text("Kembali", color = CosmicBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC)),
    ) {
        // ── 1. TOP BAR ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg)
                .statusBarsPadding()
                .border(width = 1.dp, color = cardBorder, shape = RoundedCornerShape(0.dp)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CustomBackButton(onClick = onBack)
                Spacer(Modifier.width(8.dp))

                // Avatar
                if (isAiThread) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPrincipalAi) {
                                    if (isDark) Color(0xFF2E1065) else Color(0xFFEDE9FE)
                                } else {
                                    if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE)
                                }
                            )
                            .border(
                                1.2.dp,
                                if (isPrincipalAi) {
                                    if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.65f) else Color(0xFF7C3AED).copy(alpha = 0.45f)
                                } else {
                                    if (isDark) Color(0xFF38BDF8).copy(alpha = 0.55f) else Color(0xFF0284C7).copy(alpha = 0.35f)
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = CoreR.drawable.mascot_akselerasi_edu),
                            contentDescription = if (isPrincipalAi) "AI Analitik Kepala Sekolah" else "Guru AI Akselerasi",
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(roleNeon.copy(alpha = 0.14f))
                            .border(1.dp, roleNeon.copy(alpha = 0.30f), CircleShape),
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
                            coil.compose.AsyncImage(
                                model = imageRequest,
                                contentDescription = contactName,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                            )
                        } else {
                            Text(
                                text = contactInitial,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = roleNeon,
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contactName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isParticipantOnline) NeonSuccess else TextTertiary.copy(alpha = 0.5f)),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = contactSubtitle,
                            fontSize = 10.sp,
                            color = if (isParticipantOnline) NeonSuccess.copy(alpha = 0.9f) else TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Status pill in header
                if (thread != null) {
                    val isAns = thread.status == InquiryStatus.ANSWERED
                    val isWait = thread.status == InquiryStatus.WAITING_REPLY
                    if (isAns) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonSuccess.copy(alpha = 0.12f))
                                .border(0.5.dp, NeonSuccess.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "Dijawab",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonSuccess,
                            )
                        }
                    } else if (isWait) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonWarning.copy(alpha = 0.12f))
                                .border(0.5.dp, NeonWarning.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "Menunggu Jawaban",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonWarning,
                            )
                        }
                    }
                }


            }

            // Topic Context Banner
            if (thread != null && thread.referenceTitle.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(GlassBorder2),
                )
                TopicContextBanner(
                    thread = thread,
                    onOpenReference = onOpenReference,
                    roleNeon = roleNeon,
                    isTeacherMode = isTeacherMode,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(GlassBorder),
            )
        }

        // ── 2. CHAT CANVAS ────────────────────────────────────────────────
        val rawMessages = thread?.messages ?: emptyList()
        val messages = remember(rawMessages) {
            rawMessages.filter { it.content.isNotBlank() || !it.attachmentUrl.isNullOrBlank() }
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CosmicNavy)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(
                        text = "Mulai Diskusi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Pertanyaan seputar materi & tugas.",
                        fontSize = 12.sp,
                        color = TextTertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                    val isMe = if (isTeacherMode) msg.isFromTeacher else !msg.isFromTeacher
                    val prevMsg = if (index > 0) messages[index - 1] else null
                    val isNewDay = prevMsg == null || !isSameDay(prevMsg.timestamp, msg.timestamp)
                    val isReadByOther = if (isTeacherMode) {
                        (thread?.studentLastReadAt ?: 0L) >= msg.timestamp
                    } else {
                        (thread?.teacherLastReadAt ?: 0L) >= msg.timestamp
                    }
                    if (isNewDay) DatePill(dateText = formatDateGroup(msg.timestamp))
                    MessageBubble(
                        message = msg,
                        isMe = isMe,
                        isTeacherMode = isTeacherMode,
                        roleNeon = roleNeon,
                        fallbackTeacherName = contactName,
                        isReadByRecipient = isReadByOther,
                        onImageClick = { previewFullImageUrl = it },
                    )
                }

                if (isAiThinking) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
                                    .background(if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF))
                                    .border(1.dp, if (isDark) Color(0xFF4338CA).copy(alpha = 0.5f) else Color(0xFFC7D2FE), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                                    )
                                    Text(
                                        text = if (isPrincipalAi) "AI Analitik sedang menganalisis data PKBM..." else if (isTeacherAi) "AI Copilot sedang menyusun saran pembelajaran..." else "Guru AI sedang meracik penjelasan...",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 3. INPUT BAR ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(cardBorder),
            )

            if (isAiThread) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isPrincipalAi) {
                        item {
                            PrincipalChatSuggestionChip(
                                label = "📊 Evaluasi Keaktifan Guru",
                                onClick = {
                                    inputText = "Bagaimana keaktifan para guru/tutor dalam mengunggah modul pembelajaran dan memberikan penugasan mandiri minggu ini?"
                                }
                            )
                        }
                        item {
                            PrincipalChatSuggestionChip(
                                label = "📝 Rekap Tugas & Kuis CBT",
                                onClick = {
                                    inputText = "Bagaimana rekapitulasi penugasan mandiri dan asesmen CBT yang sedang aktif di seluruh rombel PKBM?"
                                }
                            )
                        }
                        item {
                            PrincipalChatSuggestionChip(
                                label = "⚠️ Analisis Early Warning",
                                onClick = {
                                    inputText = "Apakah ada anomali atau warga belajar yang terdeteksi berisiko (at-risk) yang memerlukan disposisi kepala sekolah?"
                                }
                            )
                        }
                        item {
                            PrincipalChatSuggestionChip(
                                label = "📈 Ketercapaian KKTP Rombel",
                                onClick = {
                                    inputText = "Bagaimana evaluasi ketuntasan KKTP dan tingkat presensi warga belajar pada sesi tutorial hari ini?"
                                }
                            )
                        }
                        item {
                            PrincipalChatSuggestionChip(
                                label = "💡 Rekomendasi Supervisi Guru",
                                onClick = {
                                    inputText = "Berikan rekomendasi taktis manajerial kepala sekolah untuk meningkatkan keterlaksanaan sesi belajar dan efektivitas tutor."
                                }
                            )
                        }
                    } else if (isTeacherAi) {
                        item {
                            PrincipalChatSuggestionChip(
                                label = "📘 Buat Draf Modul Ajar",
                                onClick = {
                                    inputText = "Bantu susunkan draf modul ajar ringkas dan menarik sesuai Kurikulum Merdeka Paket Kesetaraan untuk materi saya."
                                }
                            )
                        }
                        item {
                            PrincipalChatSuggestionChip(
                                label = "✍️ Buatkan 5 Soal Kuis CBT",
                                onClick = {
                                    inputText = "Buatkan 5 butir soal pilihan ganda (PG) beserta kunci jawaban dan pembahasannya yang kontekstual dan berbobot."
                                }
                            )
                        }
                        item {
                            PrincipalChatSuggestionChip(
                                label = "💬 Ide Feedback Koreksi Tugas",
                                onClick = {
                                    inputText = "Berikan inspirasi feedback konstruktif dan memotivasi untuk mengoreksi tugas esai warga belajar yang masih kurang mendalam."
                                }
                            )
                        }
                        item {
                            PrincipalChatSuggestionChip(
                                label = "🎯 Strategi Remedial Siswa",
                                onClick = {
                                    inputText = "Bagaimana strategi remedial dan pendekatan diferensiasi yang efektif bagi siswa yang belum tuntas mencapai KKTP?"
                                }
                            )
                        }
                        item {
                            PrincipalChatSuggestionChip(
                                label = "📋 Rubrik Penilaian Soal Esai",
                                onClick = {
                                    inputText = "Bantu buatkan rubrik penilaian holistik berskala 100 untuk penilaian tugas esai reflektif siswa."
                                }
                            )
                        }
                    } else {
                        item {
                            PrincipalChatSuggestionChip(
                                label = "💡 Jelaskan Konsep",
                                onClick = {
                                    inputText = "Bisa tolong jelaskan konsep materi ini dengan bahasa yang mudah dipahami?"
                                }
                            )
                        }
                        item {
                            PrincipalChatSuggestionChip(
                                label = "📐 Rumus & Pembahasan",
                                onClick = {
                                    inputText = "Bagaimana rumus dan langkah-langkah penyelesaian untuk soal topik ini?"
                                }
                            )
                        }
                        item {
                            PrincipalChatSuggestionChip(
                                label = "📝 Ringkasan Poin Penting",
                                onClick = {
                                    inputText = "Tolong buatkan ringkasan poin-poin terpenting dari materi ini untuk persiapan ujian."
                                }
                            )
                        }
                        item {
                            PrincipalChatSuggestionChip(
                                label = "❓ Buat Soal Latihan",
                                onClick = {
                                    inputText = "Tolong berikan 3 contoh soal latihan beserta pembahasan jawabannya."
                                }
                            )
                        }
                    }
                }
            }

            // Attachment preview strip above input
            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(innerCardBg)
                        .border(1.dp, innerCardBorder, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Column {
                                Text(
                                    text = "Foto Terlampir",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary
                                )
                                Text(
                                    text = if (isUploadingImage) "Mengunggah berkas..." else "Siap dikirim bersama pesan",
                                    fontSize = 10.sp,
                                    color = if (isUploadingImage) roleNeon else textTertiary
                                )
                            }
                        }
                        if (isUploadingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = roleNeon
                            )
                        } else {
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Input pill
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(innerCardBg)
                        .border(1.dp, innerCardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!isAiThread) {
                        IconButton(
                            onClick = {
                                imagePickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Attachment,
                                contentDescription = "Lampiran Foto",
                                tint = if (selectedImageUri != null) roleNeon else textTertiary,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        textStyle = TextStyle(
                            color = textPrimary,
                            fontSize = 14.sp,
                        ),
                        cursorBrush = SolidColor(roleNeon),
                        maxLines = 4,
                        singleLine = false,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text(
                                    text = if (isPrincipalAi) "Tanya AI Analitik Kepala Sekolah..." else if (isTeacherAi) "Tanya AI Copilot Guru seputar modul, kuis, atau evaluasi..." else if (isAiThread) "Tanya apa saja ke Guru AI..." else "Ketik pesan...",
                                    fontSize = 14.sp,
                                    color = textTertiary,
                                )
                            }
                            innerTextField()
                        },
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Send button
                val hasText = inputText.isNotBlank()
                val canSend = (hasText || selectedImageUri != null) && !isUploadingImage
                val sendScale by animateFloatAsState(
                    targetValue = if (canSend) 1f else 0.9f,
                    label = "sendScale",
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(sendScale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canSend) roleNeon else innerCardBg)
                        .border(
                            1.dp,
                            if (canSend) roleNeon else innerCardBorder,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable(enabled = canSend) {
                            coroutineScope.launch {
                                var uploadedUrl: String? = null
                                if (selectedImageUri != null) {
                                    isUploadingImage = true
                                    try {
                                        val bytes = context.contentResolver.openInputStream(selectedImageUri!!)?.use { it.readBytes() }
                                        if (bytes != null) {
                                            val uploadRes = chatManager.uploadAttachment(bytes, "chat_photo_${System.currentTimeMillis()}.jpg", "image/jpeg")
                                            uploadedUrl = uploadRes.getOrNull()
                                        }
                                    } catch (e: Exception) {
                                        Timber.w(e, "Upload attachment failed")
                                    } finally {
                                        isUploadingImage = false
                                    }
                                }

                                val textToSend = inputText.trim()
                                val imgToSend = uploadedUrl
                                inputText = ""
                                selectedImageUri = null
                                focusManager.clearFocus()

                                sendMessage(
                                    text = textToSend,
                                    attachmentUrl = imgToSend,
                                    attachmentType = if (imgToSend != null) "image" else null,
                                    thread = thread,
                                    threadId = threadId,
                                    initialRecipientName = contactName,
                                    isTeacherMode = isTeacherMode,
                                    chatManager = chatManager,
                                    onSuccess = {}
                                )

                                if (isAiThread && textToSend.isNotBlank()) {
                                    isAiThinking = true
                                    chatManager.askAi(
                                        threadId = threadId,
                                        question = textToSend,
                                        subjectName = thread?.subjectName,
                                        gradeLevel = thread?.studentClass,
                                        history = messages
                                    )
                                    isAiThinking = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Kirim",
                        tint = if (canSend) Color.White else textTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }

    // Full screen image viewer dialog
    if (previewFullImageUrl != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { previewFullImageUrl = null }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { previewFullImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = previewFullImageUrl,
                    contentDescription = "Preview Lampiran",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                IconButton(
                    onClick = { previewFullImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }
        }
    }
}

// ── TOPIC CONTEXT BANNER ──────────────────────────────────────────────────────

@Composable
private fun TopicContextBanner(
    thread: ChatThread,
    onOpenReference: (type: InquiryType, refId: String?) -> Unit,
    roleNeon: Color,
    isTeacherMode: Boolean,
) {
    val isDark = com.schoolos.android.core.designsystem.LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(roleNeon.copy(alpha = 0.12f))
                    .border(1.dp, roleNeon.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        id = if (thread.inquiryType == InquiryType.ASSIGNMENT)
                            CoreR.drawable.ic_modern_tasks
                        else CoreR.drawable.ic_modern_book
                    ),
                    contentDescription = null,
                    tint = roleNeon,
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                val badgeLabel = when (thread.inquiryType) {
                    InquiryType.ASSIGNMENT -> "Tugas"
                    InquiryType.MATERIAL -> "Materi"
                    InquiryType.GENERAL -> "Umum"
                }
                Text(
                    text = "$badgeLabel: ${thread.referenceTitle}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${thread.subjectName} • ${thread.studentClass}",
                    fontSize = 10.sp,
                    color = textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val lastMsgTime = thread.messages.lastOrNull()?.timestamp ?: thread.lastUpdated
                val isTeacherRead = (thread.teacherLastReadAt ?: 0L) >= lastMsgTime
                val isAnswered = thread.status == InquiryStatus.ANSWERED
                val effectiveLabel = when {
                    isAnswered -> if (isTeacherMode) "Selesai Dijawab" else "Jawaban Tersedia"
                    isTeacherMode -> "Perlu Ditanggapi"
                    isTeacherRead -> "Dibaca Guru"
                    else -> "Menunggu Respon Guru"
                }
                val isSuccessColor = isAnswered || (isTeacherRead && !isTeacherMode)
                val badgeColor = if (isSuccessColor) Color(0xFF10B981) else Color(0xFFF59E0B)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = effectiveLabel,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (!thread.referenceId.isNullOrBlank()) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(roleNeon.copy(alpha = 0.12f))
                    .border(1.dp, roleNeon.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                    .clickable { onOpenReference(thread.inquiryType, thread.referenceId) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "Lihat",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = roleNeon,
                )
            }
        }
    }
}

// ── DATE PILL ─────────────────────────────────────────────────────────────────

@Composable
private fun DatePill(dateText: String) {
    val isDark = com.schoolos.android.core.designsystem.LocalIsDarkTheme.current
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF1F5F9)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(innerCardBg)
                .border(1.dp, innerCardBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = dateText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = textTertiary,
                letterSpacing = 0.4.sp,
            )
        }
    }
}

// ── MESSAGE BUBBLE ────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    isTeacherMode: Boolean,
    roleNeon: Color,
    fallbackTeacherName: String = "",
    isReadByRecipient: Boolean = false,
    onImageClick: (String) -> Unit = {},
) {
    val isDark = com.schoolos.android.core.designsystem.LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)

    val align = if (isMe) Alignment.End else Alignment.Start
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
    }

    val isAi = !isMe && (message.senderRole.contains("AI", ignoreCase = true) || message.senderName.contains("AI", ignoreCase = true))

    val bubbleBg = when {
        isMe -> if (isDark) roleNeon.copy(alpha = 0.20f) else roleNeon.copy(alpha = 0.12f)
        isAi -> if (isDark) Color(0xFF18181B) else Color(0xFFF1F5F9)
        else -> cardBg
    }
    val bubbleBorder = when {
        isMe -> roleNeon.copy(alpha = if (isDark) 0.50f else 0.35f)
        isAi -> if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
        else -> cardBorder
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align,
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 300.dp)
                .clip(bubbleShape)
                .background(bubbleBg)
                .border(1.dp, bubbleBorder, bubbleShape)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Column {
                // Incoming sender label
                if (!isMe) {
                    val senderColor = when {
                        isAi -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        message.isFromTeacher -> if (isDark) Color(0xFF10B981) else Color(0xFF059669)
                        else -> roleNeon
                    }
                    val roleLabel = when {
                        isAi -> "Asisten Pintar"
                        message.isFromTeacher -> "Guru"
                        else -> "Siswa"
                    }
                    val rawName = message.senderName.trim()
                    val effectiveSenderName = when {
                        isAi -> "Guru AI"
                        message.isFromTeacher -> {
                            if (rawName.isNotBlank() &&
                                !rawName.equals("Bapak/Ibu Guru", ignoreCase = true) &&
                                !rawName.equals("Guru Pengampu", ignoreCase = true) &&
                                !rawName.equals("Guru", ignoreCase = true)
                            ) {
                                rawName
                            } else if (fallbackTeacherName.isNotBlank() &&
                                !fallbackTeacherName.equals("Guru Pengampu", ignoreCase = true) &&
                                !fallbackTeacherName.equals("Bapak/Ibu Guru", ignoreCase = true)
                            ) {
                                fallbackTeacherName
                            } else {
                                "Guru"
                            }
                        }
                        else -> if (rawName.isNotBlank()) rawName else "Siswa"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        if (isAi) {
                            Image(
                                painter = painterResource(id = CoreR.drawable.mascot_akselerasi_edu),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                        Text(
                            text = effectiveSenderName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = senderColor,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "• $roleLabel",
                            fontSize = 9.5.sp,
                            color = textTertiary,
                        )
                    }
                }

                // Render image attachment if present
                if (!message.attachmentUrl.isNullOrBlank()) {
                    val rawUrl = message.attachmentUrl.orEmpty()
                    val fullUrl = remember(rawUrl) {
                        if (rawUrl.startsWith("http") || rawUrl.startsWith("content:") || rawUrl.startsWith("file:")) {
                            rawUrl
                        } else {
                            val host = com.schoolos.android.core.common.BuildConfig.API_BASE_URL.substringBefore("/api/").trimEnd('/')
                            val path = if (rawUrl.startsWith("/")) rawUrl else "/$rawUrl"
                            "$host$path"
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (message.content.isNotBlank()) 6.dp else 0.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(innerCardBg)
                            .border(1.dp, innerCardBorder, RoundedCornerShape(10.dp))
                            .clickable { onImageClick(fullUrl) }
                    ) {
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = "Lampiran Foto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 220.dp)
                        )
                    }
                }

                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        fontSize = 13.5.sp,
                        color = textPrimary,
                        lineHeight = 20.sp,
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = TextTertiary,
                    )
                    if (isMe) {
                        Spacer(Modifier.width(4.dp))
                        val isMessageRead = message.isRead || isReadByRecipient
                        if (isMessageRead) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.DoneAll, contentDescription = "Dibaca", tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Dibaca",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.Done, contentDescription = "Terkirim", tint = TextTertiary, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Terkirim",
                                    fontSize = 9.5.sp,
                                    color = TextTertiary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── HELPERS ───────────────────────────────────────────────────────────────────

private fun sendMessage(
    text: String,
    attachmentUrl: String? = null,
    attachmentType: String? = null,
    thread: ChatThread?,
    threadId: String,
    initialRecipientName: String,
    isTeacherMode: Boolean,
    chatManager: ChatManager,
    onSuccess: () -> Unit,
) {
    if (text.isNotBlank() || !attachmentUrl.isNullOrBlank()) {
        val auth = chatManager.currentAuth
        val targetThread = thread ?: chatManager.getOrCreateTeacherThread(
            teacherId = threadId,
            teacherName = initialRecipientName.ifBlank { "Guru Mata Pelajaran" },
            subjectName = "Umum",
        )
        val isTeacher = isTeacherMode || auth?.isTeacher == true
        val senderRole = if (isTeacher) "TEACHER" else "STUDENT"
        val senderName = when {
            isTeacher -> auth?.name?.takeIf { it.isNotBlank() } ?: targetThread.teacherName
            else -> auth?.name?.takeIf { it.isNotBlank() } ?: targetThread.studentName
        }
        val senderId = when {
            isTeacher -> auth?.userId?.takeIf { it.isNotBlank() } ?: targetThread.teacherId
            else -> auth?.userId?.takeIf { it.isNotBlank() } ?: targetThread.studentId
        }
        chatManager.sendMessage(
            threadId = targetThread.id,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole,
            content = text,
            attachmentUrl = attachmentUrl,
            attachmentType = attachmentType,
        )
        onSuccess()
    }
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val f = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return f.format(Date(t1)) == f.format(Date(t2))
}

private fun formatDateGroup(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val f = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val today = f.format(Date(now))
    val msgDay = f.format(Date(timestamp))
    return when {
        today == msgDay -> "HARI INI"
        now - timestamp < 86400000L * 2 -> "KEMARIN"
        else -> SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date(timestamp)).uppercase()
    }
}

@Composable
private fun PrincipalChatSuggestionChip(
    label: String,
    onClick: () -> Unit,
) {
    val isDark = com.schoolos.android.core.designsystem.LocalIsDarkTheme.current
    val innerCardBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFF1F5F9)
    val innerCardBorder = if (isDark) Color(0xFF2E2E32) else Color(0xFFCBD5E1)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(innerCardBg)
            .border(1.dp, innerCardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = textPrimary,
        )
    }
}
