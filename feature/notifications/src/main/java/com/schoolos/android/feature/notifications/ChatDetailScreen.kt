package com.schoolos.android.feature.notifications

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Forum
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(threadId) { chatManager.loadThreadDetail(threadId) }
    androidx.compose.runtime.DisposableEffect(chatManager) {
        chatManager.startPolling()
        onDispose {
            chatManager.stopPolling()
        }
    }
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
                listState.animateScrollToItem(size - 1)
            }
        }
    }

    val contactName = if (isTeacherMode) {
        thread?.studentName?.ifBlank { null } ?: initialRecipientName.ifBlank { null } ?: "Siswa"
    } else {
        val tName = thread?.teacherName
        if (!tName.isNullOrBlank() && !tName.equals("Guru Pengampu", ignoreCase = true)) {
            tName
        } else if (initialRecipientName.isNotBlank() && !initialRecipientName.equals("Guru Pengampu", ignoreCase = true)) {
            initialRecipientName
        } else {
            "Guru Mata Pelajaran"
        }
    }
    val contactInitial = contactName.trim().take(1).uppercase().ifBlank { "?" }
    val contactSubtitle = if (isTeacherMode) "Siswa • ${thread?.studentClass?.ifBlank { "Rombel" } ?: "Rombel"}"
                          else "${thread?.subjectName?.ifBlank { "Mata Pelajaran" } ?: "Mata Pelajaran"} • Aktif"
    val roleNeon = if (isTeacherMode) StudentNeon else TeacherNeon

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack),
    ) {
        // ── 1. TOP BAR ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CosmicNavy)
                .statusBarsPadding()
                .border(width = 0.5.dp, color = GlassBorder, shape = RoundedCornerShape(0.dp)),
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(roleNeon.copy(alpha = 0.14f))
                        .border(1.dp, roleNeon.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = contactInitial,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = roleNeon,
                    )
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
                                .background(NeonSuccess),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = contactSubtitle,
                            fontSize = 10.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
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
        val messages = thread?.messages ?: emptyList()

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
                        text = "Pertanyaan seputar materi & tugas langsung terhubung antara guru dan siswa.",
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
                    .fillMaxWidth()
                    .imeNestedScroll(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                    val isMe = if (isTeacherMode) msg.isFromTeacher else !msg.isFromTeacher
                    val prevMsg = if (index > 0) messages[index - 1] else null
                    val isNewDay = prevMsg == null || !isSameDay(prevMsg.timestamp, msg.timestamp)
                    if (isNewDay) DatePill(dateText = formatDateGroup(msg.timestamp))
                    MessageBubble(
                        message = msg,
                        isMe = isMe,
                        isTeacherMode = isTeacherMode,
                        roleNeon = roleNeon,
                    )
                }
            }
        }

        // ── 3. INPUT BAR ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CosmicNavy)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(GlassBorder),
            )

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
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicSurface2)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Fitur lampiran berkas sedang disiapkan...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(26.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = "Lampiran",
                            tint = TextTertiary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 14.sp,
                        ),
                        cursorBrush = SolidColor(roleNeon),
                        maxLines = 4,
                        singleLine = false,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "Ketik pesan...",
                                    fontSize = 14.sp,
                                    color = TextTertiary,
                                )
                            }
                            innerTextField()
                        },
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Send button
                val hasText = inputText.isNotBlank()
                val sendScale by animateFloatAsState(
                    targetValue = if (hasText) 1f else 0.9f,
                    label = "sendScale",
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(sendScale)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (hasText) roleNeon.copy(alpha = 0.18f) else CosmicSurface2)
                        .border(
                            0.5.dp,
                            if (hasText) roleNeon.copy(alpha = 0.50f) else GlassBorder,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable(enabled = hasText) {
                            sendMessage(inputText, thread, isTeacherMode, chatManager) {
                                inputText = ""
                                focusManager.clearFocus()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Kirim",
                        tint = if (hasText) roleNeon else TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmicNavy)
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
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(roleNeon.copy(alpha = 0.12f))
                    .border(0.5.dp, roleNeon.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (thread.inquiryType == InquiryType.ASSIGNMENT)
                        Icons.AutoMirrored.Filled.Assignment
                    else Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = roleNeon,
                    modifier = Modifier.size(14.dp),
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${thread.subjectName} • ${thread.studentClass}",
                    fontSize = 10.sp,
                    color = TextTertiary,
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
                    .border(0.5.dp, roleNeon.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(CosmicNavy)
                .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = dateText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextTertiary,
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
) {
    val align = if (isMe) Alignment.End else Alignment.Start
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align,
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 300.dp)
                .clip(bubbleShape)
                .background(
                    if (isMe) roleNeon.copy(alpha = 0.14f) else CosmicNavy,
                )
                .border(
                    0.5.dp,
                    if (isMe) roleNeon.copy(alpha = 0.35f) else GlassBorder,
                    bubbleShape,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column {
                // Incoming sender label
                if (!isMe) {
                    val isAi = message.senderRole.contains("AI", ignoreCase = true) ||
                               message.senderName.contains("AI", ignoreCase = true)
                    val senderColor = when {
                        isAi -> NeonWarning
                        message.isFromTeacher -> TeacherNeon
                        else -> StudentNeon
                    }
                    val roleLabel = when {
                        isAi -> "Asisten AI"
                        message.isFromTeacher -> "Guru"
                        else -> "Siswa"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 3.dp),
                    ) {
                        Text(
                            text = message.senderName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = senderColor,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "• $roleLabel",
                            fontSize = 9.sp,
                            color = if (isAi) NeonWarning.copy(alpha = 0.85f) else TextTertiary,
                        )
                    }
                }

                Text(
                    text = message.content,
                    fontSize = 13.5.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp,
                )

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
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Terkirim",
                            tint = NeonSuccess.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
            }
        }
    }
}

// ── HELPERS ───────────────────────────────────────────────────────────────────

private fun sendMessage(
    text: String,
    thread: ChatThread?,
    isTeacherMode: Boolean,
    chatManager: ChatManager,
    onSuccess: () -> Unit,
) {
    if (text.isNotBlank() && thread != null) {
        val senderRole = if (isTeacherMode) "TEACHER" else "STUDENT"
        val senderName = if (isTeacherMode) thread.teacherName else thread.studentName
        val senderId = if (isTeacherMode) thread.teacherId else thread.studentId
        chatManager.sendMessage(
            threadId = thread.id,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole,
            content = text,
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
