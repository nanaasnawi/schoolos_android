package com.schoolos.android.feature.notifications

import com.schoolos.android.core.R as CoreR
import androidx.compose.ui.res.painterResource
import com.schoolos.android.core.common.BuildConfig
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
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
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest
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
import com.schoolos.android.core.designsystem.ExecutiveTopBar
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.PullRefreshContainer
import com.schoolos.android.core.designsystem.ShimmerList
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF1F1F23) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    val innerCardBg = if (isDark) Color(0xFF161618) else Color(0xFFF8FAFC)
    val innerCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val studentAccent = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val teacherAccent = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val activeRoleAccent = if (isTeacherMode) teacherAccent else studentAccent

    val auth = chatManager.currentAuth
    val isPrincipal = auth?.isPrincipal == true
    var showPrincipalConsultDialog by remember { mutableStateOf(false) }

    val rawThreads by chatManager.threads.collectAsState()
    val threads = remember(rawThreads, isTeacherMode, isPrincipal, auth) {
        val userThreads = chatManager.filterThreadsForUser(rawThreads, auth)
        // Exclude AI thread from teacher consultation list so they are completely separated
        userThreads.filterNot { it.id.startsWith("ai-tutor") || it.id.startsWith("ai-principal") }
    }
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
        containerColor = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ExecutiveTopBar(
                title = "Tanya Jawab & Konsultasi",
                subtitle = if (isPrincipal) {
                    if (waitingCount > 0) "$waitingCount pesan konsultasi langsung"
                    else "Konsultasi langsung Dewan Guru & Siswa"
                } else if (isTeacherMode) {
                    if (waitingCount > 0) "$waitingCount pesan menunggu tanggapan"
                    else "Semua pesan konsultasi telah dijawab"
                } else {
                    "Pesan & Konsultasi Mata Pelajaran PKBM"
                },
                onBack = onBack,
                actions = {
                    if (!isOnline) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF4C0519) else Color(0xFFFEF2F2),
                            border = BorderStroke(0.5.dp, if (isDark) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFFFECACA))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                                Text("Offline", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
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
                            tint = textSecondary,
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
                // ── Search Bar ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = if (isPrincipal) "Cari nama guru, rombel, atau siswa..."
                                else if (isTeacherMode) "Cari nama pengirim, rombel, atau topik..."
                                else "Cari nama guru atau mata pelajaran...",
                                fontSize = 12.5.sp,
                                color = textTertiary,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = textTertiary,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Hapus",
                                        tint = textSecondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            focusedBorderColor = activeRoleAccent,
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    )
                }

                if (isPrincipal) {
                    val directoryVm: TeacherDirectoryViewModel = hiltViewModel()
                    val directory by directoryVm.state.collectAsState()
                    val myUid = auth?.userId.orEmpty().trim()
                    val myName = auth?.name.orEmpty().trim()
                    PrincipalSupervisionChatContent(
                        teachers = directory.teachers.filterNot { t ->
                            t.isPrincipal ||
                            (myUid.isNotBlank() && (t.id == myUid || t.userId == myUid)) ||
                            isSamePersonName(t.name, myName) ||
                            t.name.contains("Kepala Sekolah", ignoreCase = true) ||
                            t.roleTitle.contains("Kepala Sekolah", ignoreCase = true)
                        },
                        isDirectoryLoading = directory.isLoading,
                        threads = threads,
                        searchQuery = searchQuery,
                        chatManager = chatManager,
                        onOpenThread = onOpenThread,
                        isDark = isDark,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        innerCardBg = innerCardBg,
                        innerCardBorder = innerCardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        textTertiary = textTertiary,
                        accentColor = teacherAccent,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                } else if (!isTeacherMode) {
                    // ── SISWA MODE: WHATSAPP-STYLE DIRECT TEACHER CHAT LIST ──
                    val directoryVm: TeacherDirectoryViewModel = hiltViewModel()
                    val directory by directoryVm.state.collectAsState()
                    StudentWhatsAppChatList(
                        teachers = directory.teachers,
                        isDirectoryLoading = directory.isLoading,
                        threads = threads,
                        searchQuery = searchQuery,
                        chatManager = chatManager,
                        onOpenThread = onOpenThread,
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        textTertiary = textTertiary,
                        studentAccent = studentAccent,
                        innerCardBorder = innerCardBorder,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                } else {
                    // ── GURU MODE: INQUIRY OVERVIEW & FILTERED THREADS ──────
                    ChatOverviewCard(
                        totalCount = threads.size,
                        waitingCount = waitingCount,
                        contactCount = uniqueContacts,
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it },
                        isTeacherMode = isTeacherMode,
                        isDark = isDark,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        studentAccent = studentAccent,
                        teacherAccent = teacherAccent,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )

                    // C. Filter Tabs
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
                            isDark = isDark,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            textTertiary = textTertiary,
                            studentAccent = studentAccent,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            innerCardBg = innerCardBg,
                            innerCardBorder = innerCardBorder,
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // D. Thread List Content
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
                                ShimmerList(
                                    count = 5,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }

                            "empty" -> {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 24.dp),
                                    shape = RoundedCornerShape(22.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, cardBorder),
                                    shadowElevation = if (isDark) 0.dp else 1.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(innerCardBg)
                                                .border(1.dp, innerCardBorder, RoundedCornerShape(16.dp)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                painter = painterResource(id = CoreR.drawable.ic_modern_chat),
                                                contentDescription = null,
                                                tint = studentAccent,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                        Text(
                                            text = if (searchQuery.isNotBlank()) "Tidak Ditemukan"
                                            else if (isPrincipal) "Ruang Tanya Jawab Kepala Sekolah"
                                            else if (selectedFilter != ChatFilter.ALL) "Tidak Ada Diskusi"
                                            else "Belum Ada Tanya Jawab",
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                        )
                                        Text(
                                            text = if (searchQuery.isNotBlank())
                                                "Tidak ada diskusi yang cocok dengan \"$searchQuery\""
                                            else if (isPrincipal)
                                                "Saluran konsultasi langsung Kepala Sekolah dengan Dewan Guru dan Siswa. Pesan tanya jawab antar guru dan murid bersifat privat dan terlindungi."
                                            else if (selectedFilter != ChatFilter.ALL)
                                                "Tidak ada pertanyaan dalam kategori ini"
                                            else if (isTeacherMode)
                                                "Pesan konsultasi dari siswa maupun kepala sekolah akan muncul di sini"
                                            else
                                                "Ajukan pertanyaan kepada bapak/ibu guru langsung melalui modul materi atau tugas pembelajaran.",
                                            fontSize = 12.sp,
                                            color = textSecondary,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 17.sp
                                        )
                                        if (isPrincipal) {
                                            Spacer(Modifier.height(8.dp))
                                            Surface(
                                                onClick = { showPrincipalConsultDialog = true },
                                                shape = RoundedCornerShape(12.dp),
                                                color = teacherAccent,
                                                shadowElevation = 2.dp
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "Mulai Konsultasi Langsung",
                                                        color = Color.White,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        if (searchQuery.isNotBlank() || selectedFilter != ChatFilter.ALL) {
                                            Spacer(Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = innerCardBg,
                                                border = BorderStroke(1.dp, innerCardBorder),
                                                modifier = Modifier.clickable {
                                                    searchQuery = ""
                                                    selectedFilter = ChatFilter.ALL
                                                }
                                            ) {
                                                Text(
                                                    text = "Tampilkan Semua Diskusi",
                                                    color = studentAccent,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
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
                                            isDark = isDark,
                                            cardBg = cardBg,
                                            cardBorder = cardBorder,
                                            innerCardBg = innerCardBg,
                                            innerCardBorder = innerCardBorder,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary,
                                            textTertiary = textTertiary,
                                            studentAccent = studentAccent,
                                            teacherAccent = teacherAccent,
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

    if (showPrincipalConsultDialog) {
        var recipientType by remember { mutableStateOf("GURU") }
        var recipientName by remember { mutableStateOf("") }
        var topicText by remember { mutableStateOf("") }
        var initialMsgText by remember { mutableStateOf("") }

        val dialogCardBg = if (isDark) Color(0xFF161618) else Color.White
        val dialogCardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
        val dialogInnerBg = if (isDark) Color(0xFF1E1E22) else Color(0xFFF8FAFC)
        val dialogInnerBorder = if (isDark) Color(0xFF2F2F36) else Color(0xFFCBD5E1)
        val dialogAccent = teacherAccent

        Dialog(
            onDismissRequest = { showPrincipalConsultDialog = false }
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = dialogCardBg,
                border = BorderStroke(1.dp, dialogCardBorder),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(dialogAccent.copy(alpha = 0.15f))
                                    .border(1.dp, dialogAccent.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = dialogAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Konsultasi Langsung",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Akses langsung Kepala Sekolah",
                                    fontSize = 11.5.sp,
                                    color = textSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { showPrincipalConsultDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = textTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Recipient Type Segmented Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("GURU" to "Dewan Guru", "SISWA" to "Warga Belajar").forEach { (typeKey, typeLabel) ->
                            val isSel = recipientType == typeKey
                            Surface(
                                onClick = {
                                    recipientType = typeKey
                                    if (recipientName.isBlank()) {
                                        recipientName = if (typeKey == "GURU") "Guru Pamong" else "Warga Belajar"
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) dialogAccent.copy(alpha = 0.15f) else dialogInnerBg,
                                border = BorderStroke(1.dp, if (isSel) dialogAccent else dialogInnerBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = typeLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) dialogAccent else textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Recipient Name Field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (recipientType == "GURU") "Nama Guru / Tutor" else "Nama Siswa / Warga Belajar",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(dialogInnerBg)
                                .border(1.dp, dialogInnerBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = recipientName,
                                onValueChange = { recipientName = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(color = textPrimary, fontSize = 13.sp),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (recipientName.isEmpty()) {
                                        Text(
                                            if (recipientType == "GURU") "Ketik nama Bapak/Ibu Guru..." else "Ketik nama Siswa...",
                                            color = textTertiary,
                                            fontSize = 13.sp
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                    }

                    // Topic Field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Topik / Perihal",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(dialogInnerBg)
                                .border(1.dp, dialogInnerBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = topicText,
                                onValueChange = { topicText = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(color = textPrimary, fontSize = 13.sp),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (topicText.isEmpty()) {
                                        Text(
                                            "Contoh: Supervisi Pembelajaran / Bimbingan Siswa",
                                            color = textTertiary,
                                            fontSize = 13.sp
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                    }

                    // Message Field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Pesan Awal",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp, max = 130.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(dialogInnerBg)
                                .border(1.dp, dialogInnerBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = initialMsgText,
                                onValueChange = { initialMsgText = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(color = textPrimary, fontSize = 13.sp, lineHeight = 18.sp),
                                decorationBox = { inner ->
                                    if (initialMsgText.isEmpty()) {
                                        Text(
                                            "Ketik pesan atau instruksi konsultasi Anda di sini...",
                                            color = textTertiary,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showPrincipalConsultDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            color = dialogInnerBg,
                            border = BorderStroke(1.dp, dialogInnerBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text("Batal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                            }
                        }

                        Surface(
                            onClick = {
                                val targetName = recipientName.trim().ifBlank {
                                    if (recipientType == "GURU") "Guru Pamong" else "Warga Belajar"
                                }
                                val effectiveSubject = topicText.trim().ifBlank { "Konsultasi Kepala Sekolah" }
                                val effectiveMsg = initialMsgText.trim().ifBlank {
                                    "Halo $targetName, saya ingin berkonsultasi mengenai $effectiveSubject."
                                }
                                showPrincipalConsultDialog = false
                                val thread = chatManager.createInquiry(
                                    studentId = if (recipientType == "SISWA") "recipient-student" else auth?.userId ?: "principal",
                                    studentName = if (recipientType == "SISWA") targetName else auth?.name ?: "Kepala Sekolah",
                                    studentClass = if (recipientType == "SISWA") "Warga Belajar" else "Kepala Sekolah",
                                    teacherName = if (recipientType == "GURU") targetName else auth?.name ?: "Kepala Sekolah",
                                    subjectName = effectiveSubject,
                                    inquiryType = InquiryType.GENERAL,
                                    referenceTitle = "Konsultasi Langsung",
                                    referenceId = null,
                                    initialQuestion = effectiveMsg
                                )
                                onOpenThread(thread.id, targetName)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = dialogAccent,
                            shadowElevation = 2.dp,
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mulai Percakapan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 1. WHATSAPP-STYLE TEACHER CONTACTS & CHAT LIST (Zero WhatsApp dummy) ──

data class TeacherContactItem(
    val id: String,          // teachers table ID
    val userId: String,      // users table ID for real-time presence
    val name: String,
    val subject: String,
    val roleTitle: String = "Tutor Pengampu",
    val isPrincipal: Boolean = false,
)

@Composable
private fun StudentWhatsAppChatList(
    teachers: List<TeacherContactItem>,
    isDirectoryLoading: Boolean,
    threads: List<ChatThread>,
    searchQuery: String,
    chatManager: ChatManager,
    onOpenThread: (threadId: String, recipientName: String) -> Unit,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    studentAccent: Color,
    innerCardBorder: Color,
    modifier: Modifier = Modifier,
) {
    val presences by chatManager.teacherPresences.collectAsState()

    val filteredTeachers = remember(teachers, searchQuery) {
        if (searchQuery.isBlank()) teachers
        else teachers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.subject.contains(searchQuery, ignoreCase = true) ||
            it.roleTitle.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "GURU MATA PELAJARAN (${filteredTeachers.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = studentAccent,
            )
            Text(
                text = "Pilih untuk mulai chat",
                fontSize = 10.5.sp,
                color = textTertiary,
            )
        }

        // WhatsApp-style flat list (no card wrapper per user design specification)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (filteredTeachers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            isDirectoryLoading -> "Memuat daftar guru..."
                            searchQuery.isBlank() -> "Belum ada guru terdaftar"
                            else -> "Guru atau mata pelajaran '$searchQuery' tidak ditemukan"
                        },
                        fontSize = 12.5.sp,
                        color = textTertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(filteredTeachers, key = { _, it -> it.id }) { index, teacher ->
                        val thread = threads.firstOrNull {
                            (it.teacherId.isNotBlank() && it.teacherId.equals(teacher.id, ignoreCase = true)) ||
                            (it.teacherName.isNotBlank() && teacher.name.isNotBlank() && (
                                it.teacherName.contains(teacher.name.take(6), ignoreCase = true) ||
                                teacher.name.contains(it.teacherName.take(6), ignoreCase = true)
                            ))
                        }
                        val cleanMessages = remember(thread?.messages) {
                            thread?.messages?.filter { it.content.isNotBlank() || !it.attachmentUrl.isNullOrBlank() } ?: emptyList()
                        }
                        val lastMsg = cleanMessages.lastOrNull()?.let {
                            if (it.content.isNotBlank() && !it.content.equals("📷 [Foto Lampiran]", ignoreCase = true)) it.content
                            else if (!it.attachmentUrl.isNullOrBlank() || it.content.equals("📷 [Foto Lampiran]", ignoreCase = true)) "📷 [Foto Lampiran]"
                            else it.content
                        } ?: thread?.lastMessageContent?.takeIf { it.isNotBlank() }
                        val lastMsgTime = cleanMessages.lastOrNull()?.timestamp
                            ?: thread?.lastUpdated?.takeIf { it > 0L && !lastMsg.isNullOrBlank() }

                        // Real-time presence: ONLY online if Firebase RTDB reports active presence (< 3 min)
                        val isTeacherOnline = presences[teacher.userId] == true || presences[teacher.id] == true

                        WhatsAppChatRowItem(
                            teacher = teacher,
                            lastMessage = lastMsg,
                            lastMessageTime = lastMsgTime,
                            isOnline = isTeacherOnline,
                            hasUnread = thread?.status == InquiryStatus.ANSWERED,
                            isDark = isDark,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            textTertiary = textTertiary,
                            studentAccent = studentAccent,
                            onClick = {
                                val targetThread = thread ?: chatManager.getOrCreateTeacherThread(
                                    teacherId = teacher.id,
                                    teacherName = teacher.name,
                                    subjectName = teacher.subject,
                                )
                                onOpenThread(targetThread.id, targetThread.teacherName)
                            },
                        )

                        if (index < filteredTeachers.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                thickness = 0.5.dp,
                                color = if (isDark) innerCardBorder else Color(0xFFF1F5F9),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatsAppChatRowItem(
    teacher: TeacherContactItem,
    lastMessage: String?,
    lastMessageTime: Long?,
    isOnline: Boolean,
    hasUnread: Boolean,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    studentAccent: Color,
    onClick: () -> Unit,
) {
    val formattedTime = remember(lastMessageTime) {
        if (lastMessageTime != null && lastMessageTime > 0L) {
            val now = System.currentTimeMillis()
            val fDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val msgDay = fDay.format(Date(lastMessageTime))
            val today = fDay.format(Date(now))
            when {
                msgDay == today -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastMessageTime))
                now - lastMessageTime < 86400000L * 2 -> "Kemarin"
                else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(lastMessageTime))
            }
        } else ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── 1. AVATAR with Real-time Online Indicator (dot hijau kecil di profile) ──
        val effectiveRoleAccent = if (teacher.isPrincipal) {
            if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
        } else studentAccent

        Box(
            modifier = Modifier.size(44.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(effectiveRoleAccent.copy(alpha = if (isDark) 0.18f else 0.10f))
                    .border(1.dp, effectiveRoleAccent.copy(alpha = if (isDark) 0.35f else 0.22f), CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (teacher.isPrincipal) "K" else teacher.name.take(1).uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = effectiveRoleAccent,
                )
            }

            // Realtime Online Dot: ONLY shown if teacher is actively online in Firebase RTDB
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .border(2.dp, if (isDark) Color(0xFF09090B) else Color.White, CircleShape)
                        .align(Alignment.BottomEnd),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // ── 2. TEXT COLUMN: Clean WhatsApp Layout ──
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            // Top Row: Teacher Name + Time (NO "Online" text, NO "Aktif" text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(
                        text = teacher.name,
                        fontSize = 14.5.sp,
                        fontWeight = if (teacher.isPrincipal) FontWeight.Bold else FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (teacher.isPrincipal) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = effectiveRoleAccent.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, effectiveRoleAccent.copy(alpha = 0.4f)),
                        ) {
                            Text(
                                text = "Pimpinan",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = effectiveRoleAccent,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (formattedTime.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = if (hasUnread) Color(0xFF10B981) else textTertiary,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }

            Spacer(Modifier.height(3.dp))

            // Bottom Row: Subject / Last Message (NO wrapper, NO badge, plain typography) + Unread Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val subtitleText = remember(lastMessage, teacher.subject) {
                    if (!lastMessage.isNullOrBlank()) {
                        "${teacher.subject} • $lastMessage"
                    } else {
                        teacher.subject
                    }
                }

                Text(
                    text = subtitleText,
                    fontSize = 12.5.sp,
                    color = if (hasUnread) textPrimary else textSecondary,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (hasUnread) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                    )
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
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    studentAccent: Color,
    teacherAccent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1. Total Diskusi
            OverviewStatItem(
                value = "$totalCount",
                label = "Total Diskusi",
                valueColor = textPrimary,
                isSelected = selectedFilter == ChatFilter.ALL,
                onClick = { onFilterSelected(ChatFilter.ALL) },
                isDark = isDark,
                modifier = Modifier.weight(1f),
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(cardBorder),
            )

            // 2. Menunggu Respon
            val warningColor = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
            val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF16A34A)
            OverviewStatItem(
                value = "$waitingCount",
                label = if (isTeacherMode) "Perlu Dibalas" else "Menunggu Jawaban",
                valueColor = if (waitingCount > 0) warningColor else successColor,
                hasWarningDot = waitingCount > 0,
                isSelected = selectedFilter == ChatFilter.WAITING,
                onClick = {
                    onFilterSelected(
                        if (selectedFilter == ChatFilter.WAITING) ChatFilter.ALL else ChatFilter.WAITING
                    )
                },
                isDark = isDark,
                modifier = Modifier.weight(1f),
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(cardBorder),
            )

            // 3. Kontak
            OverviewStatItem(
                value = "$contactCount",
                label = if (isTeacherMode) "Kontak Aktif" else "Guru Pengampu",
                valueColor = if (isTeacherMode) studentAccent else teacherAccent,
                isSelected = false,
                onClick = {},
                isDark = isDark,
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
    isDark: Boolean,
    modifier: Modifier = Modifier,
    hasWarningDot: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
            if (hasWarningDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)),
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) valueColor else if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B),
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
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    studentAccent: Color,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
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
                accentColor = studentAccent,
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                cardBg = cardBg,
                cardBorder = cardBorder,
                innerCardBg = innerCardBg,
                innerCardBorder = innerCardBorder,
                onClick = { onFilterSelected(ChatFilter.ALL) },
            )
        }
        if (waitingCount > 0) {
            item {
                FilterPill(
                    label = "Menunggu Jawaban",
                    count = waitingCount,
                    isSelected = selectedFilter == ChatFilter.WAITING,
                    accentColor = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706),
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
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
                    accentColor = studentAccent,
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
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
                    accentColor = studentAccent,
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
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
                    label = "Dijawab",
                    count = answeredCount,
                    isSelected = selectedFilter == ChatFilter.ANSWERED,
                    accentColor = if (isDark) Color(0xFF10B981) else Color(0xFF16A34A),
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    innerCardBg = innerCardBg,
                    innerCardBorder = innerCardBorder,
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
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) accentColor.copy(alpha = if (isDark) 0.16f else 0.10f) else cardBg
    val borderCol = if (isSelected) accentColor.copy(alpha = if (isDark) 0.55f else 0.45f) else cardBorder
    val textColor = if (isSelected) accentColor else textSecondary
    val badgeBg = if (isSelected) accentColor.copy(alpha = if (isDark) 0.24f else 0.16f) else innerCardBg
    val badgeTextColor = if (isSelected) accentColor else textSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .border(0.5.dp, if (isSelected) Color.Transparent else innerCardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$count",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                )
            }
        }
    }
}

// ── REDESIGNED CONVERSATION THREAD CARD (22.dp ROUNDED, ADAPTIVE) ────────────

@Composable
private fun ThreadCard(
    thread: ChatThread,
    isTeacherMode: Boolean,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    studentAccent: Color,
    teacherAccent: Color,
    onClick: () -> Unit,
) {
    val isWaiting = thread.status == InquiryStatus.WAITING_REPLY
    val isAnswered = thread.status == InquiryStatus.ANSWERED
    val isMaterial = thread.inquiryType == InquiryType.MATERIAL

    val warningColor = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    val successColor = if (isDark) Color(0xFF10B981) else Color(0xFF16A34A)

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

    val rawAvatarUrl = if (isTeacherMode) thread.studentAvatarUrl else thread.teacherAvatarUrl
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

    val avatarGradient = remember(thread.id) {
        val colorIndex = (thread.subjectName.hashCode() and 0x7FFFFFFF) % 4
        when (colorIndex) {
            0 -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
            1 -> listOf(Color(0xFF10B981), Color(0xFF047857))
            2 -> listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
            else -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(
            1.dp,
            if (isWaiting) warningColor.copy(alpha = 0.5f) else cardBorder
        ),
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── TOP ROW: AVATAR + CONTACT INFO + TIME & STATUS ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar with presence / status badge
                Box(
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(avatarGradient)),
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }

                    if (isWaiting) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(warningColor)
                                .border(1.5.dp, cardBg, CircleShape),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Name + Class + Subject
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = contactName,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        val isPrincipalSender = thread.studentClass.contains("Kepala Sekolah", ignoreCase = true) ||
                            thread.studentName.contains("Kepala Sekolah", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isPrincipalSender) (if (isDark) Color(0xFF581C87) else Color(0xFFF3E8FF)) else innerCardBg,
                            border = BorderStroke(0.5.dp, if (isPrincipalSender) (if (isDark) Color(0xFFA855F7) else Color(0xFFD8B4FE)) else innerCardBorder)
                        ) {
                            Text(
                                text = if (isPrincipalSender) "Kepala Sekolah" else if (isTeacherMode) thread.studentClass.ifBlank { "Siswa" } else "Guru",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPrincipalSender) (if (isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE)) else textSecondary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }

                        Text("•", fontSize = 8.sp, color = textTertiary)

                        Text(
                            text = thread.subjectName.ifBlank { "Umum" },
                            fontSize = 11.5.sp,
                            color = textSecondary,
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
                        color = textTertiary,
                    )

                    if (isWaiting) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) warningColor.copy(alpha = 0.15f) else Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, if (isDark) warningColor.copy(alpha = 0.4f) else Color(0xFFFDE68A))
                        ) {
                            Text(
                                text = "Menunggu Jawaban",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) warningColor else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isAnswered) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) successColor.copy(alpha = 0.15f) else Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, if (isDark) successColor.copy(alpha = 0.4f) else Color(0xFF86EFAC))
                        ) {
                            Text(
                                text = "Dijawab",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) successColor else Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // ── CONTEXT CHIP (TOPIC) ──
            val refTagColor = if (isMaterial) studentAccent else Color(0xFF8B5CF6)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = innerCardBg,
                border = BorderStroke(1.dp, innerCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isMaterial) CoreR.drawable.ic_modern_book else CoreR.drawable.ic_modern_tasks
                        ),
                        contentDescription = null,
                        tint = refTagColor,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = if (isMaterial) "Materi" else "Tugas",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = refTagColor,
                    )
                    Text(
                        text = "•",
                        fontSize = 8.sp,
                        color = textTertiary,
                    )
                    Text(
                        text = thread.referenceTitle.ifBlank { "Topik Diskusi" },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
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
                            color = if (isWaiting) textPrimary else textTertiary,
                        )
                        val effectiveText = if (lastMsg.content.isNotBlank() && !lastMsg.content.equals("📷 [Foto Lampiran]", ignoreCase = true)) {
                            lastMsg.content
                        } else if (!lastMsg.attachmentUrl.isNullOrBlank() || lastMsg.content.equals("📷 [Foto Lampiran]", ignoreCase = true)) {
                            "📷 [Foto Lampiran]"
                        } else {
                            lastMsg.content
                        }
                        Text(
                            text = effectiveText,
                            fontSize = 12.sp,
                            fontWeight = if (isWaiting) FontWeight.Medium else FontWeight.Normal,
                            color = if (isWaiting) textPrimary else textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = "Belum ada pesan",
                        fontSize = 11.sp,
                        color = textTertiary,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "${thread.effectiveMessageCount} pesan",
                        fontSize = 10.5.sp,
                        color = textTertiary,
                    )
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                        contentDescription = "Buka",
                        tint = textTertiary,
                        modifier = Modifier.size(11.dp),
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

// ── 2. PRINCIPAL SUPERVISION & CHAT CONTENT ──

@Composable
private fun PrincipalSupervisionChatContent(
    teachers: List<TeacherContactItem>,
    isDirectoryLoading: Boolean,
    threads: List<ChatThread>,
    searchQuery: String,
    chatManager: ChatManager,
    onOpenThread: (threadId: String, recipientName: String) -> Unit,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    innerCardBg: Color,
    innerCardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val presences by chatManager.teacherPresences.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Dewan Guru, 1: Siswa

    val auth = chatManager.currentAuth
    val myName = auth?.name.orEmpty().trim()
    val myId = auth?.userId.orEmpty().trim()

    val filteredTeachers = remember(teachers, searchQuery) {
        if (searchQuery.isBlank()) teachers
        else teachers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.subject.contains(searchQuery, ignoreCase = true) ||
            it.roleTitle.contains(searchQuery, ignoreCase = true)
        }
    }

    val studentThreads = remember(threads, searchQuery, myName, myId) {
        val realStudentThreads = threads.filter { t ->
            val isMeAsStudent = (myId.isNotBlank() && t.studentId == myId) ||
                isSamePersonName(t.studentName, myName) ||
                t.studentClass.equals("Kepala Sekolah", ignoreCase = true) ||
                t.studentName.contains("Kepala Sekolah", ignoreCase = true)

            !isMeAsStudent && t.studentName.isNotBlank()
        }
        if (searchQuery.isBlank()) realStudentThreads
        else realStudentThreads.filter { t ->
            t.studentName.contains(searchQuery, ignoreCase = true) ||
            t.studentClass.contains(searchQuery, ignoreCase = true) ||
            t.referenceTitle.contains(searchQuery, ignoreCase = true) ||
            t.subjectName.contains(searchQuery, ignoreCase = true) ||
            (t.lastMessage?.content?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── PRINCIPAL TAB SELECTOR ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0xFF161618) else Color(0xFFF1F5F9))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 0: Dewan Guru
            Surface(
                onClick = { selectedTab = 0 },
                shape = RoundedCornerShape(10.dp),
                color = if (selectedTab == 0) (if (isDark) Color(0xFF27272A) else Color.White) else Color.Transparent,
                border = if (selectedTab == 0) BorderStroke(1.dp, if (isDark) Color(0xFF3F3F46) else Color(0xFFE2E8F0)) else null,
                shadowElevation = if (selectedTab == 0 && !isDark) 1.dp else 0.dp,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = if (selectedTab == 0) accentColor else textTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Dewan Guru (${teachers.size})",
                        fontSize = 12.5.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) textPrimary else textSecondary
                    )
                }
            }

            // Tab 1: Siswa
            Surface(
                onClick = { selectedTab = 1 },
                shape = RoundedCornerShape(10.dp),
                color = if (selectedTab == 1) (if (isDark) Color(0xFF27272A) else Color.White) else Color.Transparent,
                border = if (selectedTab == 1) BorderStroke(1.dp, if (isDark) Color(0xFF3F3F46) else Color(0xFFE2E8F0)) else null,
                shadowElevation = if (selectedTab == 1 && !isDark) 1.dp else 0.dp,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = if (selectedTab == 1) Color(0xFF38BDF8) else textTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (studentThreads.isNotEmpty()) "Siswa (${studentThreads.size})" else "Siswa",
                        fontSize = 12.5.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) textPrimary else textSecondary
                    )
                }
            }
        }

        if (selectedTab == 0) {
            // ── DEWAN GURU LIST: CLEAN WHATSAPP STYLE ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "DEWAN GURU (${filteredTeachers.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = accentColor,
                )
                Text(
                    text = "Pilih untuk mulai chat",
                    fontSize = 10.5.sp,
                    color = textTertiary,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (filteredTeachers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = when {
                                isDirectoryLoading -> "Memuat daftar dewan guru..."
                                searchQuery.isBlank() -> "Belum ada guru terdaftar"
                                else -> "Guru atau mata pelajaran '$searchQuery' tidak ditemukan"
                            },
                            fontSize = 12.5.sp,
                            color = textTertiary,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(filteredTeachers, key = { _, it -> it.id }) { index, teacher ->
                            val thread = threads.firstOrNull {
                                (it.teacherId.isNotBlank() && it.teacherId.equals(teacher.id, ignoreCase = true)) ||
                                (it.teacherName.isNotBlank() && teacher.name.isNotBlank() && (
                                    it.teacherName.contains(teacher.name.take(6), ignoreCase = true) ||
                                    teacher.name.contains(it.teacherName.take(6), ignoreCase = true)
                                ))
                            }
                            val cleanMessages = remember(thread?.messages) {
                                thread?.messages?.filter { it.content.isNotBlank() || !it.attachmentUrl.isNullOrBlank() } ?: emptyList()
                            }
                            val lastMsg = cleanMessages.lastOrNull()?.let {
                                if (it.content.isNotBlank() && !it.content.equals("📷 [Foto Lampiran]", ignoreCase = true)) it.content
                                else if (!it.attachmentUrl.isNullOrBlank() || it.content.equals("📷 [Foto Lampiran]", ignoreCase = true)) "📷 [Foto Lampiran]"
                                else it.content
                            } ?: thread?.lastMessageContent?.takeIf { it.isNotBlank() }
                            val lastMsgTime = cleanMessages.lastOrNull()?.timestamp
                                ?: thread?.lastUpdated?.takeIf { it > 0L && !lastMsg.isNullOrBlank() }

                            val isTeacherOnline = presences[teacher.userId] == true || presences[teacher.id] == true

                            PrincipalTeacherRowItem(
                                teacher = teacher,
                                lastMessage = lastMsg,
                                lastMessageTime = lastMsgTime,
                                isOnline = isTeacherOnline,
                                hasUnread = thread?.status == InquiryStatus.WAITING_REPLY,
                                isDark = isDark,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                textTertiary = textTertiary,
                                accentColor = accentColor,
                                onClick = {
                                    val targetThread = thread ?: chatManager.getOrCreateTeacherThread(
                                        teacherId = teacher.id,
                                        teacherName = teacher.name,
                                        subjectName = teacher.subject,
                                    )
                                    onOpenThread(targetThread.id, teacher.name)
                                },
                            )

                            if (index < filteredTeachers.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 56.dp),
                                    thickness = 0.5.dp,
                                    color = if (isDark) innerCardBorder else Color(0xFFF1F5F9),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ── SISWA: THREADS FROM REAL STUDENTS ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "PESAN WARGA BELAJAR (${studentThreads.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF38BDF8),
                )
                Text(
                    text = "Pertanyaan dari siswa",
                    fontSize = 10.5.sp,
                    color = textTertiary,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (studentThreads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = if (searchQuery.isNotBlank()) "Tidak Ditemukan" else "Belum Ada Konsultasi Siswa",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "Tidak ada pesan siswa yang cocok dengan \"$searchQuery\""
                                else
                                    "Pertanyaan dan konsultasi langsung dari warga belajar akan muncul di sini secara real-time.",
                                fontSize = 12.5.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(studentThreads, key = { it.id }) { thread ->
                            val cleanMessages = remember(thread.messages) {
                                thread.messages.filter { it.content.isNotBlank() || !it.attachmentUrl.isNullOrBlank() }
                            }
                            val lastMsg = cleanMessages.lastOrNull()?.let {
                                if (it.content.isNotBlank() && !it.content.equals("📷 [Foto Lampiran]", ignoreCase = true)) it.content
                                else if (!it.attachmentUrl.isNullOrBlank() || it.content.equals("📷 [Foto Lampiran]", ignoreCase = true)) "📷 [Foto Lampiran]"
                                else it.content
                            } ?: thread.lastMessageContent?.takeIf { it.isNotBlank() }
                            val lastMsgTime = cleanMessages.lastOrNull()?.timestamp
                                ?: thread.lastUpdated.takeIf { it > 0L && !lastMsg.isNullOrBlank() }

                            PrincipalStudentThreadRowItem(
                                thread = thread,
                                lastMessage = lastMsg,
                                lastMessageTime = lastMsgTime,
                                isDark = isDark,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                textTertiary = textTertiary,
                                onClick = {
                                    onOpenThread(thread.id, thread.studentName.ifBlank { "Warga Belajar" })
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrincipalTeacherRowItem(
    teacher: TeacherContactItem,
    lastMessage: String?,
    lastMessageTime: Long?,
    isOnline: Boolean,
    hasUnread: Boolean,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val formattedTime = remember(lastMessageTime) {
        if (lastMessageTime != null && lastMessageTime > 0L) {
            val now = System.currentTimeMillis()
            val fDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val msgDay = fDay.format(Date(lastMessageTime))
            val today = fDay.format(Date(now))
            when {
                msgDay == today -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastMessageTime))
                now - lastMessageTime < 86400000L * 2 -> "Kemarin"
                else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(lastMessageTime))
            }
        } else ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar with Online dot
        Box(modifier = Modifier.size(44.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.10f))
                    .border(1.dp, accentColor.copy(alpha = if (isDark) 0.35f else 0.22f), CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = teacher.name.take(1).uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
            }

            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .border(2.dp, if (isDark) Color(0xFF09090B) else Color.White, CircleShape)
                        .align(Alignment.BottomEnd),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Text Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = teacher.name,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                if (formattedTime.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = if (hasUnread) Color(0xFF10B981) else textTertiary,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = if (!lastMessage.isNullOrBlank()) "${teacher.subject} • $lastMessage" else teacher.subject,
                fontSize = 12.5.sp,
                color = if (hasUnread) textPrimary else textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PrincipalStudentThreadRowItem(
    thread: ChatThread,
    lastMessage: String?,
    lastMessageTime: Long?,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    onClick: () -> Unit,
) {
    val formattedTime = remember(lastMessageTime) {
        if (lastMessageTime != null && lastMessageTime > 0L) {
            val now = System.currentTimeMillis()
            val fDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val msgDay = fDay.format(Date(lastMessageTime))
            val today = fDay.format(Date(now))
            when {
                msgDay == today -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastMessageTime))
                now - lastMessageTime < 86400000L * 2 -> "Kemarin"
                else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(lastMessageTime))
            }
        } else ""
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = thread.studentName.take(1).uppercase().ifBlank { "S" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.studentName.ifBlank { "Warga Belajar" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (formattedTime.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = formattedTime,
                            fontSize = 11.sp,
                            color = textTertiary
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))

                val subtitle = remember(thread.studentClass, thread.referenceTitle, lastMessage) {
                    val contextPart = if (thread.studentClass.isNotBlank()) thread.studentClass else "Siswa"
                    if (!lastMessage.isNullOrBlank()) "$contextPart • $lastMessage"
                    else if (thread.referenceTitle.isNotBlank()) "$contextPart • ${thread.referenceTitle}"
                    else contextPart
                }

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun cleanPersonName(name: String?): String {
    if (name.isNullOrBlank()) return ""
    var cleaned = name.trim().lowercase().replace(Regex("[,.]"), " ")
    val titleTokens = setOf(
        "spd", "mpd", "spdsd", "sag", "skom", "se", "st", "msi", "mm",
        "ssi", "ssos", "lc", "ma", "phd", "psi", "drs", "dra", "dr",
        "prof", "h", "hj", "guru", "tutor", "kepala", "sekolah", "pkbm"
    )
    val tokens = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() && it !in titleTokens }
    return tokens.joinToString(" ")
}

private fun normalizePersonName(name: String?): String {
    return cleanPersonName(name)
}

private fun isSamePersonName(a: String?, b: String?): Boolean {
    if (a.isNullOrBlank() || b.isNullOrBlank()) return false
    val cleanA = cleanPersonName(a)
    val cleanB = cleanPersonName(b)
    if (cleanA.isBlank() || cleanB.isBlank()) return false
    if (cleanA == cleanB) return true
    if (cleanA.startsWith(cleanB) || cleanB.startsWith(cleanA)) return true
    val tokensA = cleanA.split(" ")
    val tokensB = cleanB.split(" ")
    if (tokensA.isNotEmpty() && tokensB.isNotEmpty() && tokensA[0] == tokensB[0]) {
        if (tokensA.size >= 2 && tokensB.size >= 2) {
            return tokensA[1] == tokensB[1]
        }
        return true
    }
    return false
}
