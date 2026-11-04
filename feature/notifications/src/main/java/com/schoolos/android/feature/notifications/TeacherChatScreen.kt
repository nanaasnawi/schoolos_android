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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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

    val rawThreads by chatManager.threads.collectAsState()
    val threads = remember(rawThreads, isTeacherMode, chatManager.currentAuth) {
        val userThreads = if (!isTeacherMode) {
            chatManager.filterThreadsForUser(rawThreads, chatManager.currentAuth)
        } else {
            rawThreads
        }
        // Exclude AI thread from teacher consultation list so they are completely separated
        userThreads.filterNot { it.id.startsWith("ai-tutor") }
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
                title = "Tanya Jawab",
                subtitle = if (isTeacherMode) {
                    if (waitingCount > 0) "$waitingCount pertanyaan menunggu tanggapan"
                    else "Semua pertanyaan siswa telah dijawab"
                } else {
                    if (waitingCount > 0) "$waitingCount pertanyaan menunggu balasan guru"
                    else "Konsultasi materi & tugas bersama guru pengampu"
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
                // ── 1. METRIC OVERVIEW ──────────────────────────────────────────
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )

                // ── 2. KONTAK GURU PENGAMPU (Direct Contact Hub) ───────────────
                if (!isTeacherMode) {
                    TeacherContactsSection(
                        threads = threads,
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
                        studentAccent = studentAccent,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                    // B. Search Bar
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
                                            else if (selectedFilter != ChatFilter.ALL) "Tidak Ada Diskusi"
                                            else "Belum Ada Tanya Jawab",
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                        )
                                        Text(
                                            text = if (searchQuery.isNotBlank())
                                                "Tidak ada diskusi yang cocok dengan \"$searchQuery\""
                                            else if (selectedFilter != ChatFilter.ALL)
                                                "Tidak ada pertanyaan dalam kategori ini"
                                            else if (isTeacherMode)
                                                "Pertanyaan siswa seputar materi dan tugas akan muncul di sini"
                                            else
                                                "Ajukan pertanyaan kepada bapak/ibu guru langsung melalui modul materi atau tugas pembelajaran.",
                                            fontSize = 12.sp,
                                            color = textSecondary,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 17.sp
                                        )
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

// ── 1. KONTAK GURU MAPEL SECTION (Direct Interaction for Students) ────────

data class TeacherContactItem(
    val id: String,
    val name: String,
    val subject: String,
    val phone: String,
    val email: String,
    val roleTitle: String = "Tutor Pengampu",
    val status: String = "Online",
)

private val OfficialTeachersList = listOf(
    TeacherContactItem(
        id = "teacher-asep-rifai",
        name = "ASEP RIFAI, S.Pd",
        subject = "Ilmu Pengetahuan Alam dan Sosial (IPAS)",
        phone = "081223344551",
        email = "aseprifai@pkbm.sch.id",
        roleTitle = "Tutor IPAS",
    ),
    TeacherContactItem(
        id = "teacher-asy-syifa",
        name = "ASY SYIFA RAHMAH IHSANI, S.Pd",
        subject = "Bahasa Inggris",
        phone = "085211223344",
        email = "asysyifa@pkbm.sch.id",
        roleTitle = "Tutor Bhs. Inggris",
    ),
    TeacherContactItem(
        id = "teacher-amin-lisana",
        name = "AMIN LISANA, S.Pd",
        subject = "Bahasa Indonesia",
        phone = "087799881122",
        email = "aminlisana@pkbm.sch.id",
        roleTitle = "Tutor Bhs. Indonesia",
    ),
    TeacherContactItem(
        id = "teacher-siti-muniroh",
        name = "Siti Muniroh, S.Pd",
        subject = "Pendidikan Pancasila & Karakter",
        phone = "081388776655",
        email = "sitimuniroh@pkbm.sch.id",
        roleTitle = "Tutor Karakter & PKn",
    ),
    TeacherContactItem(
        id = "teacher-eulis-komala",
        name = "Dra. Hj. Eulis Komala",
        subject = "Bimbingan & Konseling",
        phone = "081299887766",
        email = "euliskomala@pkbm.sch.id",
        roleTitle = "Kepala PKBM / Pembina",
    ),
)

@Composable
private fun TeacherContactsSection(
    threads: List<ChatThread>,
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
    studentAccent: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(studentAccent.copy(alpha = 0.12f))
                            .border(1.dp, studentAccent.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_classes),
                            contentDescription = null,
                            tint = studentAccent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Kontak Guru Mapel",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                        )
                        Text(
                            text = "Hubungi langsung guru via WhatsApp atau Chat",
                            fontSize = 10.5.sp,
                            color = textSecondary,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = innerCardBg,
                    border = BorderStroke(1.dp, innerCardBorder),
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = if (isExpanded) "Sembunyikan" else "Lihat Semua",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = studentAccent,
                        )
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_chevron_right),
                            contentDescription = null,
                            tint = studentAccent,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }

            // Teachers List
            val displayTeachers = if (isExpanded) OfficialTeachersList else OfficialTeachersList.take(2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displayTeachers.forEach { teacher ->
                    TeacherContactCardItem(
                        teacher = teacher,
                        threads = threads,
                        chatManager = chatManager,
                        onOpenThread = onOpenThread,
                        isDark = isDark,
                        innerCardBg = innerCardBg,
                        innerCardBorder = innerCardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        textTertiary = textTertiary,
                        studentAccent = studentAccent,
                        onWhatsAppClick = {
                            openWhatsAppDirect(context, teacher.phone, teacher.name, teacher.subject, chatManager)
                        },
                        onCallClick = {
                            openDialerDirect(context, teacher.phone)
                        },
                        onEmailClick = {
                            openEmailDirect(context, teacher.email, teacher.name, teacher.subject, chatManager)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TeacherContactCardItem(
    teacher: TeacherContactItem,
    threads: List<ChatThread>,
    chatManager: ChatManager,
    onOpenThread: (threadId: String, recipientName: String) -> Unit,
    isDark: Boolean,
    innerCardBg: Color,
    innerCardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    studentAccent: Color,
    onWhatsAppClick: () -> Unit,
    onCallClick: () -> Unit,
    onEmailClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = innerCardBg,
        border = BorderStroke(1.dp, innerCardBorder),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = teacher.name.take(1).uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = teacher.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                        }
                        Text(
                            text = teacher.subject,
                            fontSize = 11.sp,
                            color = studentAccent,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Phone text label
                Text(
                    text = teacher.phone,
                    fontSize = 10.5.sp,
                    color = textTertiary,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Action Buttons Row (WhatsApp, Telepon, Chat di Aplikasi)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. WhatsApp Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF10B981).copy(alpha = 0.45f) else Color(0xFF86EFAC)),
                    modifier = Modifier
                        .weight(1.1f)
                        .clickable { onWhatsAppClick() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_whatsapp),
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF34D399) else Color(0xFF15803D),
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "WhatsApp",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF15803D),
                        )
                    }
                }

                // 2. Call Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = innerCardBg,
                    border = BorderStroke(1.dp, innerCardBorder),
                    modifier = Modifier
                        .weight(0.9f)
                        .clickable { onCallClick() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_phone),
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Telepon",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary,
                        )
                    }
                }

                // 3. In-App Chat Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = studentAccent,
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable {
                            val existingThread = threads.firstOrNull {
                                it.teacherName.contains(teacher.name.take(6), ignoreCase = true) ||
                                it.subjectName.contains(teacher.subject.take(5), ignoreCase = true)
                            }
                            if (existingThread != null) {
                                onOpenThread(existingThread.id, teacher.name)
                            } else {
                                val dummyThreadId = "teacher-${teacher.name.filter { it.isLetter() }.lowercase()}"
                                onOpenThread(dummyThreadId, teacher.name)
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = CoreR.drawable.ic_modern_chat),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Tanya di Chat",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

private fun openWhatsAppDirect(
    context: android.content.Context,
    phone: String,
    teacherName: String,
    subject: String,
    chatManager: ChatManager
) {
    try {
        val clean = phone.replace("+", "").replace("-", "").replace(" ", "").trim()
        val standardPhone = if (clean.startsWith("0")) "62" + clean.substring(1) else clean
        val auth = chatManager.currentAuth
        val studentName = auth?.name?.ifBlank { "Siswa" } ?: "Siswa"
        val studentClass = auth?.className?.ifBlank { "PKBM As-Salafiyah" } ?: "PKBM As-Salafiyah"
        val message = "Halo Bapak/Ibu $teacherName, saya $studentName ($studentClass) ingin berkonsultasi mengenai mata pelajaran $subject."
        val url = "https://api.whatsapp.com/send?phone=$standardPhone&text=${java.net.URLEncoder.encode(message, "UTF-8")}"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Tidak dapat membuka WhatsApp", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun openDialerDirect(context: android.content.Context, phone: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone"))
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Tidak dapat membuka panggilan", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun openEmailDirect(
    context: android.content.Context,
    email: String,
    teacherName: String,
    subject: String,
    chatManager: ChatManager
) {
    try {
        val auth = chatManager.currentAuth
        val studentName = auth?.name?.ifBlank { "Siswa" } ?: "Siswa"
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:$email")
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Konsultasi $subject - $studentName")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Tidak dapat membuka email", android.widget.Toast.LENGTH_SHORT).show()
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
                label = if (isTeacherMode) "Siswa Aktif" else "Guru Pengampu",
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
    val bg = if (isSelected) (if (isDark) Color(0xFF27272A) else Color(0xFF0F172A)) else cardBg
    val borderCol = if (isSelected) (if (isDark) Color(0xFF3F3F46) else Color(0xFF0F172A)) else cardBorder
    val textColor = if (isSelected) Color.White else textSecondary

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
                    .background(if (isSelected) (if (isDark) Color(0xFF3F3F46) else Color(0xFF334155)) else innerCardBg)
                    .border(0.5.dp, if (isSelected) Color.Transparent else innerCardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$count",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else textSecondary,
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
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = innerCardBg,
                            border = BorderStroke(0.5.dp, innerCardBorder)
                        ) {
                            Text(
                                text = if (isTeacherMode) thread.studentClass.ifBlank { "Siswa" } else "Guru",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
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
                        Text(
                            text = lastMsg.content,
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
