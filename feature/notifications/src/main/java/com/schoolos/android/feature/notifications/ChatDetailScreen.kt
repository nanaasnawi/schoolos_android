package com.schoolos.android.feature.notifications

import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.chat.ChatManager
import com.schoolos.android.core.chat.ChatMessage
import com.schoolos.android.core.chat.InquiryType
import com.schoolos.android.core.designsystem.CosmicBlack
import com.schoolos.android.core.designsystem.CosmicNavy
import com.schoolos.android.core.designsystem.CosmicSurface
import com.schoolos.android.core.designsystem.GlassBorder
import com.schoolos.android.core.designsystem.NeonBlue
import com.schoolos.android.core.designsystem.NeonSuccess
import com.schoolos.android.core.designsystem.StudentNeon
import com.schoolos.android.core.designsystem.TeacherNeon
import com.schoolos.android.core.designsystem.TextPrimary
import com.schoolos.android.core.designsystem.TextSecondary
import com.schoolos.android.core.designsystem.TextTertiary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modern Chat Canvas Colors
private val ChatBackground = Color(0xFF0C1017)
private val IncomingBubbleBg = Color(0xFF1B2332)
private val SenderTeacherGradient = Brush.linearGradient(
    listOf(Color(0xFF059669), Color(0xFF0D9488))
)
private val SenderStudentGradient = Brush.linearGradient(
    listOf(Color(0xFF4338CA), Color(0xFF2563EB))
)

@Composable
fun ChatDetailScreen(
    threadId: String,
    chatManager: ChatManager,
    onBack: () -> Unit,
    onOpenReference: (type: InquiryType, refId: String?) -> Unit = { _, _ -> },
    isTeacherMode: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val threads by chatManager.threads.collectAsState()
    val thread = threads.find { it.id == threadId }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Load full message history from database
    LaunchedEffect(threadId) {
        chatManager.loadThreadDetail(threadId)
    }

    // Scroll smoothly to bottom when messages change
    LaunchedEffect(thread?.messages?.size) {
        val size = thread?.messages?.size ?: 0
        if (size > 0) {
            listState.animateScrollToItem(size - 1)
        }
    }

    val quickTemplates = if (isTeacherMode) {
        listOf(
            "👍 Jawaban sudah tepat, lanjutkan!",
            "📖 Coba baca kembali rumus di modul ajar.",
            "📝 Silakan kumpulkan sebelum batas waktu.",
            "🏫 Besok kita bahas lebih lanjut di kelas ya."
        )
    } else {
        listOf(
            "Pak, saya masih bingung cara pengerjaannya.",
            "Apakah boleh memakai rumus lain pak?",
            "Terima kasih atas penjelasannya pak! 🙏"
        )
    }

    val contactName = if (isTeacherMode) (thread?.studentName ?: "Siswa")
                      else (thread?.teacherName ?: "Guru Pengampu")
    val contactInitial = contactName.take(1).uppercase()
    val contactSub = if (isTeacherMode) "Siswa • ${thread?.studentClass ?: "Rombel"}"
                     else "${thread?.subjectName ?: "Mata Pelajaran"} • Online"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // ── 1. MODERN TOP APP BAR (WHATSAPP / TELEGRAM STYLE) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CosmicNavy)
                .border(1.dp, GlassBorder)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Avatar with Online Dot
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isTeacherMode) StudentNeon.copy(alpha = 0.22f)
                                else TeacherNeon.copy(alpha = 0.22f)
                            )
                            .border(
                                1.5.dp,
                                if (isTeacherMode) StudentNeon.copy(alpha = 0.5f)
                                else TeacherNeon.copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contactInitial,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    // Green Active Indicator Dot
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(NeonSuccess)
                            .border(2.dp, CosmicNavy, CircleShape)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Contact Name & Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contactName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = contactSub,
                        fontSize = 11.sp,
                        color = if (isTeacherMode) StudentNeon else NeonSuccess,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ── 2. COMPACT REFERENCE BANNER (MATERI / TUGAS TERKAIT) ──
        if (thread != null && thread.referenceTitle.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131B2A))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
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
                        Icon(
                            if (thread.inquiryType == InquiryType.MATERIAL) Icons.AutoMirrored.Filled.MenuBook
                            else Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = if (thread.inquiryType == InquiryType.MATERIAL) NeonBlue else StudentNeon,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${if (thread.inquiryType == InquiryType.MATERIAL) "Materi: " else "Tugas: "}${thread.referenceTitle}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Surface(
                        onClick = { onOpenReference(thread.inquiryType, thread.referenceId) },
                        shape = RoundedCornerShape(8.dp),
                        color = CosmicSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Buka", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(Modifier.width(3.dp))
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(10.dp), tint = TextSecondary)
                        }
                    }
                }
            }
        }

        // ── 3. CHAT MESSAGES CANVAS ──
        val messages = thread?.messages ?: emptyList()
        val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")) }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                val isMe = if (isTeacherMode) msg.isFromTeacher else !msg.isFromTeacher

                // Show centered date chip if date changes or for first message
                val prevMsg = if (index > 0) messages[index - 1] else null
                val isNewDay = prevMsg == null || !isSameDay(prevMsg.timestamp, msg.timestamp)
                if (isNewDay) {
                    DateSeparator(dateText = formatDateGroup(msg.timestamp))
                }

                ModernMessageBubble(
                    message = msg,
                    isMe = isMe,
                    isTeacherMode = isTeacherMode
                )
            }
        }

        // ── 4. QUICK TEMPLATES CHIPS ──
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickTemplates) { tmpl ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicNavy)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable { inputText = tmpl }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tmpl,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        // ── 5. MODERN FLOATING INPUT BAR (WHATSAPP / TELEGRAM PILL + CIRCULAR FAB) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill Input Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CosmicNavy)
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment Icon
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Lampiran berkas / gambar", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = "Lampiran",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Dynamic text input
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(if (isTeacherMode) TeacherNeon else StudentNeon),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank() && thread != null) {
                                val senderRole = if (isTeacherMode) "TEACHER" else "STUDENT"
                                val senderName = if (isTeacherMode) thread.teacherName else thread.studentName
                                val senderId = if (isTeacherMode) thread.teacherId else thread.studentId

                                chatManager.sendMessage(
                                    threadId = thread.id,
                                    senderId = senderId,
                                    senderName = senderName,
                                    senderRole = senderRole,
                                    content = inputText
                                )
                                inputText = ""
                                focusManager.clearFocus()
                            }
                        }),
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text(
                                    text = if (isTeacherMode) "Ketik bimbingan untuk siswa..." else "Ketik pertanyaan ke guru...",
                                    color = TextTertiary,
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Circular Action / Send FAB
            val hasText = inputText.isNotBlank()
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasText) {
                            if (isTeacherMode) SenderTeacherGradient else SenderStudentGradient
                        } else {
                            Brush.linearGradient(listOf(CosmicNavy, CosmicNavy))
                        }
                    )
                    .border(1.dp, if (hasText) Color.Transparent else GlassBorder, CircleShape)
                    .clickable(enabled = hasText) {
                        if (thread != null) {
                            val senderRole = if (isTeacherMode) "TEACHER" else "STUDENT"
                            val senderName = if (isTeacherMode) thread.teacherName else thread.studentName
                            val senderId = if (isTeacherMode) thread.teacherId else thread.studentId

                            chatManager.sendMessage(
                                threadId = thread.id,
                                senderId = senderId,
                                senderName = senderName,
                                senderRole = senderRole,
                                content = inputText
                            )
                            inputText = ""
                            focusManager.clearFocus()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim",
                    tint = if (hasText) Color.White else TextTertiary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

/**
 * Centered Floating Date Separator Pill
 */
@Composable
private fun DateSeparator(dateText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = dateText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        }
    }
}

/**
 * Modern, Natural-Width Chat Bubble
 */
@Composable
private fun ModernMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    isTeacherMode: Boolean,
) {
    val align = if (isMe) Alignment.End else Alignment.Start
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomEnd = 18.dp,
            bottomStart = 4.dp
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 72.dp, max = 295.dp)
                .shadow(elevation = 2.dp, shape = bubbleShape)
                .clip(bubbleShape)
                .background(
                    if (isMe) {
                        if (isTeacherMode) SenderTeacherGradient else SenderStudentGradient
                    } else {
                        Brush.linearGradient(listOf(IncomingBubbleBg, IncomingBubbleBg))
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isMe) Color.White.copy(alpha = 0.15f) else GlassBorder,
                    shape = bubbleShape
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                // Incoming message sender header
                if (!isMe) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 3.dp)
                    ) {
                        Text(
                            text = message.senderName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (message.isFromTeacher) TeacherNeon else StudentNeon
                        )
                        if (message.isFromTeacher) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = TeacherNeon,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                // Message Text
                Text(
                    text = message.content,
                    fontSize = 13.5.sp,
                    color = if (isMe) Color.White else TextPrimary,
                    lineHeight = 19.sp
                )

                Spacer(Modifier.height(3.dp))

                // Time & Status Row (aligned to end)
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.72f) else TextTertiary
                    )
                    if (isMe) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Terkirim",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
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
        today == msgDay -> "Hari ini"
        now - timestamp < 86400000L * 2 -> "Kemarin"
        else -> SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date(timestamp))
    }
}
