package com.schoolos.android.feature.notifications

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Otentik WhatsApp Color Palette (Dark Theme) ──
private val WaTopBarBg = Color(0xFF1F2C34)
private val WaChatBg = Color(0xFF0B141A)
private val WaInputPillBg = Color(0xFF202C33)
private val WaSentBubbleBg = Color(0xFF005C4B)
private val WaReceivedBubbleBg = Color(0xFF202C33)
private val WaPrimaryGreen = Color(0xFF00A884)
private val WaBlueTick = Color(0xFF53BDEB)
private val WaTextPrimary = Color(0xFFE9EDEF)
private val WaTextSecondary = Color(0xFF8696A0)
private val WaDatePillBg = Color(0xFF182229)

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

    val contactName = if (isTeacherMode) (thread?.studentName ?: "Siswa")
                      else (thread?.teacherName ?: "Guru Pengampu")
    val contactInitial = contactName.take(1).uppercase()
    val contactSubtitle = if (isTeacherMode) "Siswa • ${thread?.studentClass ?: "Rombel"}"
                          else "online"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WaChatBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // ── 1. WHATSAPP TOP BAR ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WaTopBarBg)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = WaTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Circular Contact Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6B7280)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contactInitial,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.width(10.dp))

            // Contact Name & Status Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contactName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WaTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contactSubtitle,
                    fontSize = 12.sp,
                    color = if (contactSubtitle == "online") WaPrimaryGreen else WaTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── 2. WHATSAPP CHAT CANVAS ──
        val messages = thread?.messages ?: emptyList()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                val isMe = if (isTeacherMode) msg.isFromTeacher else !msg.isFromTeacher

                val prevMsg = if (index > 0) messages[index - 1] else null
                val isNewDay = prevMsg == null || !isSameDay(prevMsg.timestamp, msg.timestamp)
                if (isNewDay) {
                    WhatsAppDatePill(dateText = formatDateGroup(msg.timestamp))
                }

                WhatsAppMessageBubble(
                    message = msg,
                    isMe = isMe
                )
            }
        }

        // ── 3. WHATSAPP INPUT BAR (PILL + CIRCULAR GREEN SEND BUTTON) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(WaInputPillBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment Icon
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Lampirkan berkas...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = "Lampiran",
                            tint = WaTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Text Input Field
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        textStyle = TextStyle(
                            color = WaTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(WaPrimaryGreen),
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            sendMessage(inputText, thread, isTeacherMode, chatManager) {
                                inputText = ""
                                focusManager.clearFocus()
                            }
                        }),
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "Ketik pesan",
                                    color = WaTextSecondary,
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            // WhatsApp Emerald Green Circular Send FAB
            val hasText = inputText.isNotBlank()
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (hasText) WaPrimaryGreen else Color(0xFF1E2B33))
                    .clickable(enabled = hasText) {
                        sendMessage(inputText, thread, isTeacherMode, chatManager) {
                            inputText = ""
                            focusManager.clearFocus()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim",
                    tint = if (hasText) Color.White else WaTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun sendMessage(
    text: String,
    thread: com.schoolos.android.core.chat.ChatThread?,
    isTeacherMode: Boolean,
    chatManager: ChatManager,
    onSuccess: () -> Unit
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
            content = text
        )
        onSuccess()
    }
}

/**
 * Centered WhatsApp Floating Date Pill
 */
@Composable
private fun WhatsAppDatePill(dateText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(WaDatePillBg)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = dateText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = WaTextSecondary
            )
        }
    }
}

/**
 * WhatsApp Chat Bubble with Tail and Blue Ticks
 */
@Composable
private fun WhatsAppMessageBubble(
    message: ChatMessage,
    isMe: Boolean
) {
    val align = if (isMe) Alignment.End else Alignment.Start
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = 12.dp,
            bottomEnd = 2.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomEnd = 12.dp,
            bottomStart = 2.dp
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 68.dp, max = 295.dp)
                .clip(bubbleShape)
                .background(if (isMe) WaSentBubbleBg else WaReceivedBubbleBg)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column {
                // Incoming message sender header
                if (!isMe) {
                    Text(
                        text = message.senderName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WaPrimaryGreen,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Message Text
                Text(
                    text = message.content,
                    fontSize = 14.5.sp,
                    color = WaTextPrimary,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(2.dp))

                // Time & Status Row (aligned to bottom-right)
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeStr,
                        fontSize = 10.5.sp,
                        color = WaTextSecondary
                    )
                    if (isMe) {
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Terkirim",
                            tint = WaBlueTick,
                            modifier = Modifier.size(14.dp)
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
        today == msgDay -> "HARI INI"
        now - timestamp < 86400000L * 2 -> "KEMARIN"
        else -> SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date(timestamp)).uppercase()
    }
}
