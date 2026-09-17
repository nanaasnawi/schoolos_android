package com.schoolos.android.feature.notifications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.schoolos.android.core.designsystem.CosmicSurface
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonError
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.NeonWarning
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary

enum class ChatFilter {
    ALL,
    WAITING,
    MATERIAL,
    ASSIGNMENT,
    ANSWERED
}

@Composable
fun TeacherChatScreen(
    chatManager: ChatManager,
    onOpenThread: (threadId: String, recipientName: String) -> Unit,
    isTeacherMode: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val threads by chatManager.threads.collectAsState()
    val isLoading by chatManager.isLoading.collectAsState()
    val isOnline by chatManager.isOnline.collectAsState()

    var selectedFilter by remember { mutableStateOf(ChatFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

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
            ChatFilter.MATERIAL   -> thread.inquiryType == InquiryType.MATERIAL
            ChatFilter.ASSIGNMENT -> thread.inquiryType == InquiryType.ASSIGNMENT
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

    // Refresh spin animation
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack)
            .statusBarsPadding()
    ) {
        // ── COMPACT TOP BAR ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Tanya Jawab",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                if (waitingCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NeonWarning)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isTeacherMode) "$waitingCount pertanyaan belum dijawab"
                                   else "$waitingCount pertanyaan menunggu jawaban guru",
                            fontSize = 11.sp,
                            color = NeonWarning,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = if (isTeacherMode) "Semua pertanyaan terjawab"
                               else "Konsultasi materi & tugas dengan guru",
                        fontSize = 11.sp,
                        color = TextTertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isOnline) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(NeonError.copy(alpha = 0.15f))
                            .border(1.dp, NeonError.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonError)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("Offline", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonError)
                        }
                    }
                }
                IconButton(
                    onClick = { chatManager.refresh() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CosmicNavy)
                        .border(1.dp, GlassBorder, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Muat Ulang",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (isLoading) rotation else 0f)
                    )
                }
            }
        }


        // ── INLINE STAT CHIPS ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Total diskusi chip
            InlineStatChip(
                value = "${threads.size}",
                label = "Diskusi",
                color = if (isTeacherMode) TeacherNeon else StudentNeon,
                isSelected = selectedFilter == ChatFilter.ALL,
                onClick = { selectedFilter = ChatFilter.ALL },
                modifier = Modifier.weight(1f)
            )
            // Perlu dijawab chip
            InlineStatChip(
                value = "$waitingCount",
                label = if (waitingCount > 0) {
                    if (isTeacherMode) "Belum Dijawab" else "Menunggu"
                } else {
                    if (isTeacherMode) "Semua Terjawab" else "Sudah Dibalas"
                },
                color = if (waitingCount > 0) NeonWarning else NeonSuccess,
                isSelected = selectedFilter == ChatFilter.WAITING,
                onClick = { selectedFilter = ChatFilter.WAITING },
                modifier = Modifier.weight(1f)
            )
            // Kontak chip
            InlineStatChip(
                value = "$uniqueContacts",
                label = if (isTeacherMode) "Siswa Aktif" else "Guru",
                color = NeonBlue,
                isSelected = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }

        // ── SEARCH BAR ───────────────────────────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            placeholder = {
                Text(
                    if (isTeacherMode) "Cari nama siswa, topik, atau kelas..."
                    else "Cari nama guru, materi, topik...",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Hapus",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CosmicNavy,
                unfocusedContainerColor = CosmicNavy,
                focusedBorderColor = (if (isTeacherMode) TeacherNeon else StudentNeon).copy(alpha = 0.6f),
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        // ── FILTER CHIPS ROW (Only shown when contextual categories exist) ───
        val hasCategoryFilters = waitingCount > 0 || materialCount > 0 || assignmentCount > 0 || answeredCount > 0
        if (hasCategoryFilters) {
            Spacer(Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (waitingCount > 0) {
                    item {
                        SmartFilterChip(
                            label = if (isTeacherMode) "Belum Dijawab" else "Menunggu Balasan",
                            count = waitingCount,
                            isSelected = selectedFilter == ChatFilter.WAITING,
                            activeColor = NeonWarning,
                            emoji = "⏳",
                            onClick = {
                                selectedFilter = if (selectedFilter == ChatFilter.WAITING) ChatFilter.ALL else ChatFilter.WAITING
                            }
                        )
                    }
                }
                if (materialCount > 0) {
                    item {
                        SmartFilterChip(
                            label = "Materi",
                            count = materialCount,
                            isSelected = selectedFilter == ChatFilter.MATERIAL,
                            activeColor = NeonBlue,
                            emoji = "📚",
                            onClick = {
                                selectedFilter = if (selectedFilter == ChatFilter.MATERIAL) ChatFilter.ALL else ChatFilter.MATERIAL
                            }
                        )
                    }
                }
                if (assignmentCount > 0) {
                    item {
                        SmartFilterChip(
                            label = "Tugas",
                            count = assignmentCount,
                            isSelected = selectedFilter == ChatFilter.ASSIGNMENT,
                            activeColor = StudentNeon,
                            emoji = "📝",
                            onClick = {
                                selectedFilter = if (selectedFilter == ChatFilter.ASSIGNMENT) ChatFilter.ALL else ChatFilter.ASSIGNMENT
                            }
                        )
                    }
                }
                if (answeredCount > 0) {
                    item {
                        SmartFilterChip(
                            label = if (isTeacherMode) "Terjawab" else "Sudah Dibalas",
                            count = answeredCount,
                            isSelected = selectedFilter == ChatFilter.ANSWERED,
                            activeColor = NeonSuccess,
                            emoji = "✓",
                            onClick = {
                                selectedFilter = if (selectedFilter == ChatFilter.ANSWERED) ChatFilter.ALL else ChatFilter.ANSWERED
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── LIST AREA ────────────────────────────────────────────────────
        AnimatedContent(
            targetState = when {
                isLoading && threads.isEmpty() -> "loading"
                filteredThreads.isEmpty()      -> "empty"
                else                           -> "list"
            },
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "chatContent",
            modifier = Modifier.weight(1f)
        ) { contentState ->
            when (contentState) {
                "loading" -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = TeacherNeon,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Memuat pertanyaan siswa...",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                "empty" -> {
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
                            // Big emoji icon
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(CosmicNavy)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "🔍" else "💬",
                                    fontSize = 36.sp
                                )
                            }
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "Tidak Ditemukan"
                                else if (selectedFilter != ChatFilter.ALL)
                                    "Tidak Ada Diskusi"
                                else
                                    "Belum Ada Pertanyaan",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "Coba kata kunci lain"
                                else if (selectedFilter != ChatFilter.ALL)
                                    "Tidak ada diskusi dengan filter ini"
                                else if (isTeacherMode)
                                    "Pertanyaan siswa akan muncul di sini"
                                else
                                    "Pertanyaan Anda seputar materi dan tugas kepada guru akan muncul di sini",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isNotBlank() || selectedFilter != ChatFilter.ALL) {
                                val resetColor = if (isTeacherMode) TeacherNeon else StudentNeon
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(resetColor.copy(alpha = 0.12f))
                                        .border(1.dp, resetColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            searchQuery = ""
                                            selectedFilter = ChatFilter.ALL
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Tampilkan Semua",
                                        color = resetColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Sort: waiting first, then by latest update
                        val sorted = filteredThreads.sortedWith(
                            compareByDescending<ChatThread> { it.status == InquiryStatus.WAITING_REPLY }
                                .thenByDescending { it.lastUpdated }
                        )
                        items(sorted, key = { it.id }) { thread ->
                            ThreadCard(
                                thread = thread,
                                isTeacherMode = isTeacherMode,
                                onClick = { onOpenThread(thread.id, if (isTeacherMode) thread.studentName else thread.teacherName) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}


// ── INLINE STAT CHIP ─────────────────────────────────────────────────────────

@Composable
private fun InlineStatChip(
    value: String,
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) color.copy(alpha = 0.14f) else CosmicNavy)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) color.copy(alpha = 0.6f) else GlassBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) color.copy(alpha = 0.85f) else TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


// ── SMART FILTER CHIP ─────────────────────────────────────────────────────────

@Composable
private fun SmartFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    activeColor: Color,
    emoji: String = "",
    onClick: () -> Unit,
) {
    val bgColor = if (isSelected) activeColor else CosmicNavy
    val textColor = if (isSelected) Color.White else TextSecondary
    val borderColor = if (isSelected) activeColor else GlassBorder

    Box(
        modifier = Modifier
            .shadow(
                elevation = if (isSelected) 4.dp else 0.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = activeColor.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (emoji.isNotBlank()) {
                Text(emoji, fontSize = 11.sp)
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                color = textColor
            )
            // Count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.25f)
                        else CosmicDark
                    )
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$count",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) Color.White else TextTertiary
                )
            }
        }
    }
}

// ── THREAD CARD — redesigned with priority stripe ────────────────────────────

@Composable
private fun ThreadCard(
    thread: ChatThread,
    isTeacherMode: Boolean,
    onClick: () -> Unit,
) {
    val isWaiting = thread.status == InquiryStatus.WAITING_REPLY
    val isAnswered = thread.status == InquiryStatus.ANSWERED
    val isMat = thread.inquiryType == InquiryType.MATERIAL

    val priorityColor = when {
        isWaiting  -> NeonWarning
        isAnswered -> NeonSuccess
        else       -> GlassBorder
    }
    val tagColor = if (isMat) NeonBlue else (if (isTeacherMode) StudentNeon else TeacherNeon)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = priorityColor.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(20.dp))
            .background(CosmicNavy)
            .border(1.dp, if (isWaiting) priorityColor.copy(alpha = 0.4f) else GlassBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        // ── Priority Stripe (left edge) ──
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(priorityColor)
        )

        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)) {
            // ── Row 1: Avatar + Contact Info + Status Badge ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gradient Avatar
                val avatarGradient = if (isTeacherMode) {
                    listOf(
                        StudentNeon.copy(alpha = 0.85f),
                        NeonBlue.copy(alpha = 0.85f)
                    )
                } else {
                    listOf(
                        TeacherNeon.copy(alpha = 0.85f),
                        NeonBlue.copy(alpha = 0.85f)
                    )
                }
                val contactInitial = if (isTeacherMode) thread.studentName.take(1).uppercase()
                                     else thread.teacherName.take(1).uppercase()

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(avatarGradient))
                        .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contactInitial,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isTeacherMode) thread.studentName else thread.teacherName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Class/Role badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(CosmicDark)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isTeacherMode) thread.studentClass else "Guru Pengampu",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary
                            )
                        }
                        Text("•", fontSize = 8.sp, color = TextTertiary)
                        Text(
                            thread.subjectName,
                            fontSize = 10.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(priorityColor.copy(alpha = 0.14f))
                        .border(1.dp, priorityColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            isWaiting  -> if (isTeacherMode) "⏳ Pending" else "⏳ Menunggu"
                            isAnswered -> if (isTeacherMode) "✔ Selesai" else "✔ Dibalas Guru"
                            else       -> "Aktif"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = priorityColor
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Row 2: Reference Tag ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(tagColor.copy(alpha = 0.08f))
                    .border(1.dp, tagColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isMat) Icons.AutoMirrored.Filled.MenuBook
                                      else Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = tagColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (isMat) "Materi" else "Tugas",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = tagColor
                    )
                    Text("•", fontSize = 8.sp, color = tagColor.copy(alpha = 0.5f))
                    Text(
                        text = thread.referenceTitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Row 3: Last Message Preview ──
            val lastMsg = thread.lastMessage
            if (lastMsg != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmicDark)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (lastMsg.isFromTeacher) "💬" else "📨",
                        fontSize = 11.sp
                    )
                    Text(
                        text = lastMsg.content,
                        fontSize = 11.sp,
                        color = if (isWaiting && !lastMsg.isFromTeacher) TextPrimary else TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp,
                        fontWeight = if (isWaiting && !lastMsg.isFromTeacher) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Row 4: Footer ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = formatTimeAgo(thread.lastUpdated),
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                    Text("•", fontSize = 8.sp, color = TextTertiary)
                    Text(
                        text = "${thread.messages.size} pesan",
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                }

                // Action Button
                val actionColor = if (isWaiting) priorityColor else (if (isTeacherMode) TeacherNeon else StudentNeon)
                Box(
                    modifier = Modifier
                        .shadow(if (isWaiting) 4.dp else 0.dp, RoundedCornerShape(20.dp), spotColor = priorityColor.copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isWaiting) priorityColor.copy(alpha = 0.16f)
                            else CosmicDark
                        )
                        .border(
                            1.dp,
                            if (isWaiting) priorityColor.copy(alpha = 0.5f) else GlassBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable(onClick = onClick)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isWaiting) (if (isTeacherMode) "Balas" else "Buka") else "Lihat",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = actionColor
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }
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
    if (mins < 60) return "$mins menit lalu"
    val hours = mins / 60
    if (hours < 24) return "$hours jam lalu"
    val days = hours / 24
    if (days < 30) return "$days hari lalu"
    return "${days / 30} bulan lalu"
}
