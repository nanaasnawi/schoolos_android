package com.schoolos.android.feature.notifications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.chat.ChatManager
import com.schoolos.android.core.chat.ChatThread
import com.schoolos.android.core.chat.InquiryStatus
import com.schoolos.android.core.chat.InquiryType
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicDark
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface2
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.GlassBorder2
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.PullRefreshContainer
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

enum class ChatFilter {
    ALL,
    WAITING,
    ASSIGNMENT,
    MATERIAL,
    ANSWERED,
}

@Composable
fun TeacherChatScreen(
    chatManager: ChatManager,
    onOpenThread: (threadId: String, recipientName: String) -> Unit,
    isTeacherMode: Boolean = true,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val threads by chatManager.threads.collectAsState()
    val isLoading by chatManager.isLoading.collectAsState()
    val isOnline by chatManager.isOnline.collectAsState()

    var selectedFilter by remember { mutableStateOf(ChatFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    DisposableEffect(chatManager) {
        chatManager.startPolling()
        onDispose {
            chatManager.stopPolling()
        }
    }

    val waitingCount = threads.count { it.status == InquiryStatus.WAITING_REPLY }
    val answeredCount = threads.count { it.status == InquiryStatus.ANSWERED }
    val materialCount = threads.count { it.inquiryType == InquiryType.MATERIAL }
    val assignmentCount = threads.count { it.inquiryType == InquiryType.ASSIGNMENT }
    val uniqueContacts = if (isTeacherMode) {
        threads.map { it.studentId }.distinct().size
    } else {
        threads.map { it.teacherName }.distinct().size
    }

    val filteredThreads = threads.filter { thread ->
        val matchesFilter = when (selectedFilter) {
            ChatFilter.ALL        -> true
            ChatFilter.WAITING    -> thread.status == InquiryStatus.WAITING_REPLY
            ChatFilter.ASSIGNMENT -> thread.inquiryType == InquiryType.ASSIGNMENT
            ChatFilter.MATERIAL   -> thread.inquiryType == InquiryType.MATERIAL
            ChatFilter.ANSWERED   -> thread.status == InquiryStatus.ANSWERED
        }
        val matchesSearch = searchQuery.isBlank() ||
            thread.studentName.contains(searchQuery, ignoreCase = true) ||
            thread.teacherName.contains(searchQuery, ignoreCase = true) ||
            thread.referenceTitle.contains(searchQuery, ignoreCase = true) ||
            thread.studentClass.contains(searchQuery, ignoreCase = true) ||
            thread.subjectName.contains(searchQuery, ignoreCase = true) ||
            (thread.lastMessage?.content?.contains(searchQuery, ignoreCase = true) == true)
        matchesFilter && matchesSearch
    }

    val infiniteTransition = rememberInfiniteTransition(label = "refreshRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Scaffold(
        containerColor = CosmicBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ExecutiveTopBar(
                title = "Tanya Jawab",
                subtitle = if (isTeacherMode) {
                    if (waitingCount > 0) "$waitingCount pertanyaan menunggu tanggapan"
                    else "Semua pertanyaan telah terjawab"
                } else {
                    if (waitingCount > 0) "$waitingCount pertanyaan menunggu balasan guru"
                    else "Konsultasi materi & tugas bersama guru"
                },
                onBack = onBack,
                actions = {
                    if (!isOnline) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonError.copy(alpha = 0.12f))
                                .border(0.5.dp, NeonError.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(NeonError),
                                )
                                Text("Offline", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonError)
                            }
                        }
                    }
                    IconButton(
                        onClick = { chatManager.refresh() },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Muat Ulang",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (isLoading) rotation else 0f),
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        PullRefreshContainer(
            isRefreshing = isLoading,
            onRefresh = { chatManager.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // ── 1. UNIFIED STAT OVERVIEW ────────────────────────────────
                ChatOverviewCard(
                    totalCount = threads.size,
                    waitingCount = waitingCount,
                    contactCount = uniqueContacts,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    isTeacherMode = isTeacherMode,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )

                // ── 2. SEARCH BAR ───────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = if (isTeacherMode) "Cari siswa, kelas, topik tugas/materi..."
                                else "Cari nama guru, mata pelajaran, materi...",
                                fontSize = 12.sp,
                                color = TextTertiary,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Hapus",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CosmicNavy,
                            unfocusedContainerColor = CosmicNavy,
                            focusedBorderColor = (if (isTeacherMode) TeacherNeon else StudentNeon).copy(alpha = 0.6f),
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    )
                }

                // ── 3. FILTER TABS (Only if meaningful content exists) ─────
                val showFilterRow = threads.isNotEmpty()
                if (showFilterRow) {
                    Spacer(Modifier.height(4.dp))
                    ChatFilterRow(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it },
                        totalCount = threads.size,
                        waitingCount = waitingCount,
                        assignmentCount = assignmentCount,
                        materialCount = materialCount,
                        answeredCount = answeredCount,
                    )
                }

                Spacer(Modifier.height(6.dp))

                // ── 4. THREADS LIST CONTENT ─────────────────────────────────
                AnimatedContent(
                    targetState = when {
                        isLoading && threads.isEmpty() -> "loading"
                        filteredThreads.isEmpty()      -> "empty"
                        else                           -> "list"
                    },
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "chatContentTransition",
                    modifier = Modifier.weight(1f),
                ) { state ->
                    when (state) {
                        "loading" -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    CircularProgressIndicator(
                                        color = if (isTeacherMode) TeacherNeon else StudentNeon,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(32.dp),
                                    )
                                    Text(
                                        text = "Memuat percakapan...",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                    )
                                }
                            }
                        }

                        "empty" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(CosmicNavy)
                                            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (searchQuery.isNotBlank()) Icons.Default.Search else Icons.Default.Forum,
                                            contentDescription = null,
                                            tint = TextTertiary,
                                            modifier = Modifier.size(26.dp),
                                        )
                                    }
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Tidak Ditemukan"
                                        else if (selectedFilter != ChatFilter.ALL) "Tidak Ada Diskusi"
                                        else "Belum Ada Tanya Jawab",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                    )
                                    Text(
                                        text = if (searchQuery.isNotBlank())
                                            "Tidak ada diskusi yang cocok dengan \"$searchQuery\""
                                        else if (selectedFilter != ChatFilter.ALL)
                                            "Tidak ada pertanyaan dalam kategori ini"
                                        else if (isTeacherMode)
                                            "Pertanyaan siswa seputar materi dan tugas akan muncul di sini"
                                        else
                                            "Ajukan pertanyaan kepada guru melalui halaman materi atau tugas",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                    )
                                    if (searchQuery.isNotBlank() || selectedFilter != ChatFilter.ALL) {
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CosmicSurface2)
                                                .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    searchQuery = ""
                                                    selectedFilter = ChatFilter.ALL
                                                }
                                                .padding(horizontal = 14.dp, vertical = 7.dp),
                                        ) {
                                            Text(
                                                text = "Tampilkan Semua",
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            val sortedThreads = remember(filteredThreads) {
                                filteredThreads.sortedWith(
                                    compareByDescending<ChatThread> { it.status == InquiryStatus.WAITING_REPLY }
                                        .thenByDescending { it.lastUpdated }
                                )
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 96.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(sortedThreads, key = { it.id }) { thread ->
                                    ThreadCard(
                                        thread = thread,
                                        isTeacherMode = isTeacherMode,
                                        onClick = {
                                            val recipient = if (isTeacherMode) {
                                                thread.studentName.ifBlank { "Siswa" }
                                            } else {
                                                if (thread.teacherName.isNotBlank() && !thread.teacherName.equals("Guru Pengampu", ignoreCase = true)) {
                                                    thread.teacherName
                                                } else {
                                                    "Guru Mata Pelajaran"
                                                }
                                            }
                                            onOpenThread(thread.id, recipient)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── OVERVIEW STAT CARD ───────────────────────────────────────────────────────

@Composable
private fun ChatOverviewCard(
    totalCount: Int,
    waitingCount: Int,
    contactCount: Int,
    selectedFilter: ChatFilter,
    onFilterSelected: (ChatFilter) -> Unit,
    isTeacherMode: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1. Total Diskusi
            OverviewStatItem(
                value = "$totalCount",
                label = "Total Diskusi",
                valueColor = TextPrimary,
                isSelected = selectedFilter == ChatFilter.ALL,
                onClick = { onFilterSelected(ChatFilter.ALL) },
                modifier = Modifier.weight(1f),
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(26.dp)
                    .background(GlassBorder),
            )

            // 2. Menunggu Respon
            OverviewStatItem(
                value = "$waitingCount",
                label = if (isTeacherMode) "Perlu Dibalas" else "Menunggu",
                valueColor = if (waitingCount > 0) NeonWarning else NeonSuccess,
                hasWarningDot = waitingCount > 0,
                isSelected = selectedFilter == ChatFilter.WAITING,
                onClick = {
                    onFilterSelected(
                        if (selectedFilter == ChatFilter.WAITING) ChatFilter.ALL else ChatFilter.WAITING
                    )
                },
                modifier = Modifier.weight(1f),
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(26.dp)
                    .background(GlassBorder),
            )

            // 3. Kontak
            OverviewStatItem(
                value = "$contactCount",
                label = if (isTeacherMode) "Siswa Aktif" else "Guru",
                valueColor = if (isTeacherMode) StudentNeon else TeacherNeon,
                isSelected = false,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OverviewStatItem(
    value: String,
    label: String,
    valueColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasWarningDot: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
            if (hasWarningDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NeonWarning),
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) TextPrimary else TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── HORIZONTAL CATEGORY FILTER BAR ──────────────────────────────────────────

@Composable
private fun ChatFilterRow(
    selectedFilter: ChatFilter,
    onFilterSelected: (ChatFilter) -> Unit,
    totalCount: Int,
    waitingCount: Int,
    assignmentCount: Int,
    materialCount: Int,
    answeredCount: Int,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        item {
            FilterPill(
                label = "Semua",
                count = totalCount,
                isSelected = selectedFilter == ChatFilter.ALL,
                accentColor = TextPrimary,
                onClick = { onFilterSelected(ChatFilter.ALL) },
            )
        }
        if (waitingCount > 0) {
            item {
                FilterPill(
                    label = "Menunggu",
                    count = waitingCount,
                    isSelected = selectedFilter == ChatFilter.WAITING,
                    accentColor = NeonWarning,
                    onClick = {
                        onFilterSelected(
                            if (selectedFilter == ChatFilter.WAITING) ChatFilter.ALL else ChatFilter.WAITING
                        )
                    },
                )
            }
        }
        if (assignmentCount > 0) {
            item {
                FilterPill(
                    label = "Tugas",
                    count = assignmentCount,
                    isSelected = selectedFilter == ChatFilter.ASSIGNMENT,
                    accentColor = StudentNeon,
                    onClick = {
                        onFilterSelected(
                            if (selectedFilter == ChatFilter.ASSIGNMENT) ChatFilter.ALL else ChatFilter.ASSIGNMENT
                        )
                    },
                )
            }
        }
        if (materialCount > 0) {
            item {
                FilterPill(
                    label = "Materi",
                    count = materialCount,
                    isSelected = selectedFilter == ChatFilter.MATERIAL,
                    accentColor = NeonBlue,
                    onClick = {
                        onFilterSelected(
                            if (selectedFilter == ChatFilter.MATERIAL) ChatFilter.ALL else ChatFilter.MATERIAL
                        )
                    },
                )
            }
        }
        if (answeredCount > 0) {
            item {
                FilterPill(
                    label = "Terjawab",
                    count = answeredCount,
                    isSelected = selectedFilter == ChatFilter.ANSWERED,
                    accentColor = NeonSuccess,
                    onClick = {
                        onFilterSelected(
                            if (selectedFilter == ChatFilter.ANSWERED) ChatFilter.ALL else ChatFilter.ANSWERED
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) CosmicSurface2 else CosmicNavy
    val borderCol = if (isSelected) GlassBorder2 else GlassBorder

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(0.5.dp, borderCol, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) TextPrimary else TextSecondary,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isSelected) accentColor.copy(alpha = 0.16f) else CosmicDark)
                .padding(horizontal = 5.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$count",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else TextTertiary,
            )
        }
    }
}

// ── REDESIGNED CONVERSATION THREAD CARD ──────────────────────────────────────

@Composable
private fun ThreadCard(
    thread: ChatThread,
    isTeacherMode: Boolean,
    onClick: () -> Unit,
) {
    val isWaiting = thread.status == InquiryStatus.WAITING_REPLY
    val isAnswered = thread.status == InquiryStatus.ANSWERED
    val isMaterial = thread.inquiryType == InquiryType.MATERIAL

    val contactName = if (isTeacherMode) {
        thread.studentName.ifBlank { "Siswa" }
    } else {
        if (thread.teacherName.isNotBlank() && !thread.teacherName.equals("Guru Pengampu", ignoreCase = true)) {
            thread.teacherName
        } else {
            "Guru Mata Pelajaran"
        }
    }

    val contactInitial = contactName.trim().take(1).uppercase().ifBlank { "?" }

    val avatarGradient = remember(thread.id) {
        val colorIndex = (thread.subjectName.hashCode() and 0x7FFFFFFF) % 4
        when (colorIndex) {
            0 -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)) // Blue
            1 -> listOf(Color(0xFF10B981), Color(0xFF047857)) // Emerald
            2 -> listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)) // Violet
            else -> listOf(Color(0xFFF59E0B), Color(0xFFD97706)) // Amber
        }
    }

    val borderCol = if (isWaiting) NeonWarning.copy(alpha = 0.45f) else GlassBorder

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CosmicNavy)
            .border(0.5.dp, borderCol, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── TOP ROW: AVATAR + CONTACT INFO + TIME & STATUS ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar with presence / status badge
                Box(
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(avatarGradient)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = contactInitial,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    if (isWaiting) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(NeonWarning)
                                .border(1.5.dp, CosmicNavy, CircleShape),
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Name + Class + Subject
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = contactName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        // Class/Role pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CosmicSurface2)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = if (isTeacherMode) thread.studentClass.ifBlank { "Siswa" } else "Guru",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary,
                            )
                        }

                        Text("•", fontSize = 8.sp, color = TextTertiary)

                        Text(
                            text = thread.subjectName.ifBlank { "Umum" },
                            fontSize = 11.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Timestamp & Status Pill
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = formatTimeAgo(thread.lastUpdated),
                        fontSize = 11.sp,
                        color = TextTertiary,
                    )

                    if (isWaiting) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonWarning.copy(alpha = 0.12f))
                                .border(0.5.dp, NeonWarning.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "Menunggu",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonWarning,
                            )
                        }
                    } else if (isAnswered) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonSuccess.copy(alpha = 0.12f))
                                .border(0.5.dp, NeonSuccess.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "Terjawab",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonSuccess,
                            )
                        }
                    }
                }
            }

            // ── CONTEXT CHIP (Single sleek line, no bulky box) ──
            val refTagColor = if (isMaterial) NeonBlue else StudentNeon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CosmicSurface2)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = if (isMaterial) Icons.AutoMirrored.Filled.MenuBook
                    else Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = null,
                    tint = refTagColor,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = if (isMaterial) "Materi" else "Tugas",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = refTagColor,
                )
                Text(
                    text = "•",
                    fontSize = 8.sp,
                    color = TextTertiary,
                )
                Text(
                    text = thread.referenceTitle.ifBlank { "Topik Diskusi" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            // ── LATEST MESSAGE SNIPPET & FOOTER ──
            val lastMsg = thread.lastMessage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (lastMsg != null) {
                    val senderPrefix = if (isTeacherMode) {
                        if (lastMsg.isFromTeacher) "Anda: " else "${contactName.split(" ").firstOrNull() ?: "Siswa"}: "
                    } else {
                        if (!lastMsg.isFromTeacher) "Anda: " else "${contactName.split(" ").firstOrNull() ?: "Guru"}: "
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = senderPrefix,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isWaiting) TextPrimary else TextTertiary,
                        )
                        Text(
                            text = lastMsg.content,
                            fontSize = 12.sp,
                            fontWeight = if (isWaiting) FontWeight.Medium else FontWeight.Normal,
                            color = if (isWaiting) TextPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = "Belum ada pesan",
                        fontSize = 11.sp,
                        color = TextTertiary,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "${thread.messages.size} pesan",
                        fontSize = 10.sp,
                        color = TextTertiary,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Buka",
                        tint = TextTertiary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return "Baru saja"
    val mins = diff / (60 * 1000)
    if (mins < 1) return "Baru saja"
    if (mins < 60) return "$mins mnt lalu"
    val hours = mins / 60
    if (hours < 24) return "$hours jam lalu"
    val days = hours / 24
    if (days == 1L) return "Kemarin"
    if (days < 30) return "$days hr lalu"
    return "${days / 30} bln lalu"
}
