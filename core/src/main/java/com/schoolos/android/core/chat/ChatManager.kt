package com.schoolos.android.core.chat

import android.content.Context
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.auth.AuthState
import com.schoolos.android.core.network.ApiClient
import com.schoolos.android.core.network.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class InquiryType {
    MATERIAL,
    ASSIGNMENT,
    GENERAL
}

enum class InquiryStatus {
    WAITING_REPLY,
    ANSWERED
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val threadId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String, // "TEACHER" or "STUDENT"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromTeacher: Boolean = false,
)

data class ChatThread(
    val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val studentName: String,
    val studentClass: String,
    val teacherId: String = "teacher-default",
    val teacherName: String = "Guru Pengampu",
    val subjectName: String = "Umum",
    val inquiryType: InquiryType = InquiryType.MATERIAL,
    val referenceTitle: String,
    val referenceId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val status: InquiryStatus = InquiryStatus.WAITING_REPLY,
    val lastUpdated: Long = System.currentTimeMillis(),
) {
    val lastMessage: ChatMessage? get() = messages.lastOrNull()
    val isAwaitingReply: Boolean get() = status == InquiryStatus.WAITING_REPLY
}

@Singleton
class ChatManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: ApiClient,
    private val networkMonitor: NetworkMonitor,
    private val authManager: AuthManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val api: InquiriesApi by lazy { apiClient.create() }

    // Genuine real-time device network connectivity from Android OS
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(scope, SharingStarted.Eagerly, true)

    val isRealtimeConnected: StateFlow<Boolean> get() = isOnline

    private var cachedAuth: AuthState? = null

    // Load initial state synchronously from persistent disk cache so history is restored immediately on app launch
    private val _threads = MutableStateFlow<List<ChatThread>>(loadFromDisk())
    val threads: StateFlow<List<ChatThread>> = _threads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null
    private var activeSubscribers = 0

    init {
        // Observe auth state continuously
        scope.launch {
            authManager.authState.collectLatest { auth ->
                cachedAuth = auth
                // Reload disk cache if current threads are empty
                val currentThreads = loadFromDisk()
                if (currentThreads.isNotEmpty() && _threads.value.isEmpty()) {
                    _threads.value = currentThreads
                }
            }
        }

        // Initial fetch from database
        refresh()

        // Automatically sync whenever internet connection is restored
        scope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                if (online) {
                    try {
                        syncFromDatabase()
                    } catch (e: Exception) {
                        Timber.d(e, "Auto-sync on reconnect")
                    }
                }
            }
        }
    }

    /**
     * Start live polling when Chat UI is active and visible on screen.
     */
    fun startPolling() {
        activeSubscribers++
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                delay(8000)
                if (isOnline.value) {
                    try {
                        syncFromDatabase()
                    } catch (e: Exception) {
                        Timber.w(e, "Chat sync tick failed")
                    }
                }
            }
        }
    }

    /**
     * Stop live polling when user leaves Chat UI to save battery & cellular bandwidth.
     */
    fun stopPolling() {
        activeSubscribers = (activeSubscribers - 1).coerceAtLeast(0)
        if (activeSubscribers == 0) {
            pollingJob?.cancel()
            pollingJob = null
        }
    }

    fun refresh() {
        scope.launch {
            _isLoading.value = true
            try {
                syncFromDatabase()
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh inquiries")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncFromDatabase() {
        val auth = cachedAuth ?: authManager.authState.firstOrNull()
        val teacherName = if (auth?.isTeacher == true) auth.name else null
        val studentId = if (auth?.isStudent == true && !auth.userId.isNullOrBlank() && auth.userId.length == 36) auth.userId else null

        val response = api.listInquiries(
            teacherName = teacherName,
            studentId = studentId,
        )
        if (response.success && response.data != null) {
            val dbThreads = response.data
            val currentMap = _threads.value.associateBy { it.id }
            val dbIds = dbThreads.map { it.id }.toSet()

            val threadsNeedingDetails = mutableListOf<String>()

            val updatedThreads = dbThreads.map { dto ->
                val type = when (dto.inquiryType.uppercase()) {
                    "ASSIGNMENT" -> InquiryType.ASSIGNMENT
                    "GENERAL" -> InquiryType.GENERAL
                    else -> InquiryType.MATERIAL
                }
                val st = if (dto.status.uppercase() == "ANSWERED") InquiryStatus.ANSWERED else InquiryStatus.WAITING_REPLY
                val timestamp = parseIsoTimestamp(dto.lastMessageAt ?: dto.createdAt)

                // Preserve or update messages: NEVER wipe existing full message history!
                val existing = currentMap[dto.id]
                val messages = if (existing != null && existing.messages.isNotEmpty()) {
                    val hasLastMsg = !dto.lastMessageContent.isNullOrBlank() &&
                        existing.messages.any { it.content == dto.lastMessageContent }
                    if (!hasLastMsg && !dto.lastMessageContent.isNullOrBlank()) {
                        existing.messages + ChatMessage(
                            id = UUID.randomUUID().toString(),
                            threadId = dto.id,
                            senderId = if (st == InquiryStatus.ANSWERED) (dto.teacherId ?: "teacher") else dto.studentId,
                            senderName = if (st == InquiryStatus.ANSWERED) dto.teacherName else dto.studentName,
                            senderRole = if (st == InquiryStatus.ANSWERED) "TEACHER" else "STUDENT",
                            content = dto.lastMessageContent,
                            timestamp = timestamp,
                            isFromTeacher = (st == InquiryStatus.ANSWERED),
                        )
                    } else {
                        existing.messages
                    }
                } else if (!dto.lastMessageContent.isNullOrBlank()) {
                    listOf(
                        ChatMessage(
                            id = dto.id,
                            threadId = dto.id,
                            senderId = dto.studentId,
                            senderName = dto.studentName,
                            senderRole = "STUDENT",
                            content = dto.lastMessageContent,
                            timestamp = timestamp,
                            isFromTeacher = false,
                        )
                    )
                } else {
                    emptyList()
                }

                // If messages on device are fewer than server count or empty, automatically queue detail fetch
                if (messages.size < dto.messageCount) {
                    threadsNeedingDetails.add(dto.id)
                }

                val resolvedTeacherName = if (dto.teacherName.isNotBlank() && !dto.teacherName.equals("Guru Pengampu", ignoreCase = true)) {
                    dto.teacherName
                } else if (existing != null && existing.teacherName.isNotBlank() && !existing.teacherName.equals("Guru Pengampu", ignoreCase = true)) {
                    existing.teacherName
                } else {
                    dto.teacherName
                }

                ChatThread(
                    id = dto.id,
                    studentId = dto.studentId,
                    studentName = dto.studentName,
                    studentClass = dto.studentClass.ifBlank { "Siswa" },
                    teacherId = dto.teacherId ?: existing?.teacherId ?: "teacher-default",
                    teacherName = resolvedTeacherName,
                    subjectName = dto.subjectName,
                    inquiryType = type,
                    referenceTitle = dto.referenceTitle,
                    referenceId = dto.referenceId,
                    messages = messages,
                    status = st,
                    lastUpdated = timestamp,
                )
            }

            // Keep any local threads currently pending server persistence
            val pendingLocal = _threads.value.filter { it.id !in dbIds }
            val merged = pendingLocal + updatedThreads
            _threads.value = merged
            saveToDisk(merged)

            // Trigger detail loads in background for threads with missing messages
            for (tid in threadsNeedingDetails) {
                loadThreadDetail(tid)
            }
        }
    }

    fun loadThreadDetail(threadId: String) {
        scope.launch {
            try {
                val detailResp = api.getInquiryDetail(threadId)
                if (detailResp.success && detailResp.data != null) {
                    val detail = detailResp.data
                    val msgs = detail.messages.map { m ->
                        ChatMessage(
                            id = m.id,
                            threadId = m.threadId,
                            senderId = m.senderId,
                            senderName = m.senderName,
                            senderRole = m.senderRole,
                            content = m.content,
                            timestamp = parseIsoTimestamp(m.createdAt),
                            isFromTeacher = m.isFromTeacher,
                        )
                    }

                    _threads.value = _threads.value.let { currentList ->
                        val index = currentList.indexOfFirst { it.id == threadId }
                        if (index >= 0) {
                            val existing = currentList[index]
                            // Keep any unconfirmed local temporary messages
                            val serverMsgIds = msgs.map { it.id }.toSet()
                            val localPending = existing.messages.filter { it.id !in serverMsgIds && it.id.startsWith("temp-") }
                            val combinedMsgs = (msgs + localPending).sortedBy { it.timestamp }

                            val updatedTeacherName = if (detail.thread.teacherName.isNotBlank() && !detail.thread.teacherName.equals("Guru Pengampu", ignoreCase = true)) {
                                detail.thread.teacherName
                            } else {
                                existing.teacherName
                            }

                            val updated = existing.copy(
                                messages = combinedMsgs,
                                status = if (detail.thread.status == "ANSWERED") InquiryStatus.ANSWERED else InquiryStatus.WAITING_REPLY,
                                lastUpdated = parseIsoTimestamp(detail.thread.lastMessageAt ?: detail.thread.createdAt),
                                teacherName = updatedTeacherName,
                                teacherId = detail.thread.teacherId ?: existing.teacherId,
                            )
                            currentList.toMutableList().apply { set(index, updated) }
                        } else {
                            // Thread was opened directly (e.g. push notification or deep link)
                            val type = when (detail.thread.inquiryType.uppercase()) {
                                "ASSIGNMENT" -> InquiryType.ASSIGNMENT
                                "GENERAL" -> InquiryType.GENERAL
                                else -> InquiryType.MATERIAL
                            }
                            val st = if (detail.thread.status.uppercase() == "ANSWERED") InquiryStatus.ANSWERED else InquiryStatus.WAITING_REPLY
                            val timestamp = parseIsoTimestamp(detail.thread.lastMessageAt ?: detail.thread.createdAt)
                            listOf(
                                ChatThread(
                                    id = detail.thread.id,
                                    studentId = detail.thread.studentId,
                                    studentName = detail.thread.studentName,
                                    studentClass = detail.thread.studentClass.ifBlank { "Siswa" },
                                    teacherId = detail.thread.teacherId ?: "teacher-default",
                                    teacherName = detail.thread.teacherName,
                                    subjectName = detail.thread.subjectName,
                                    inquiryType = type,
                                    referenceTitle = detail.thread.referenceTitle,
                                    referenceId = detail.thread.referenceId,
                                    messages = msgs,
                                    status = st,
                                    lastUpdated = timestamp,
                                )
                            ) + currentList
                        }
                    }
                    saveToDisk(_threads.value)

                    // Mark as read in server persistence
                    launch {
                        try {
                            api.markAsRead(threadId)
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading thread detail $threadId")
            }
        }
    }

    fun getThread(threadId: String): ChatThread? {
        loadThreadDetail(threadId)
        return _threads.value.find { it.id == threadId }
    }

    fun sendMessage(
        threadId: String,
        senderId: String,
        senderName: String,
        senderRole: String,
        content: String,
    ): ChatMessage {
        val isTeacher = senderRole.equals("TEACHER", ignoreCase = true)
        val now = System.currentTimeMillis()
        val tempId = "temp-" + UUID.randomUUID().toString()

        val auth = cachedAuth
        val effectiveSenderName = when {
            !auth?.name.isNullOrBlank() -> auth!!.name!!
            senderName.isNotBlank() && !senderName.equals("Guru Pengampu", ignoreCase = true) -> senderName
            isTeacher -> "Guru Pengampu"
            else -> "Siswa"
        }
        val effectiveSenderId = when {
            !auth?.userId.isNullOrBlank() -> auth!!.userId!!
            senderId.isNotBlank() -> senderId
            else -> UUID.randomUUID().toString()
        }

        val localMsg = ChatMessage(
            id = tempId,
            threadId = threadId,
            senderId = effectiveSenderId,
            senderName = effectiveSenderName,
            senderRole = senderRole,
            content = content.trim(),
            timestamp = now,
            isFromTeacher = isTeacher,
        )

        // Optimistic local state update & immediate disk persistence!
        _threads.value = _threads.value.map { thread ->
            if (thread.id == threadId) {
                val updatedTeacherName = if (isTeacher && effectiveSenderName.isNotBlank() && !effectiveSenderName.equals("Guru Pengampu", ignoreCase = true)) {
                    effectiveSenderName
                } else {
                    thread.teacherName
                }
                thread.copy(
                    messages = thread.messages + localMsg,
                    status = if (isTeacher) InquiryStatus.ANSWERED else InquiryStatus.WAITING_REPLY,
                    lastUpdated = now,
                    teacherName = updatedTeacherName,
                )
            } else {
                thread
            }
        }
        saveToDisk(_threads.value)

        // Persist to real database via backend API with idempotent client_message_id
        scope.launch {
            try {
                val req = SendInquiryMessageRequestDto(
                    clientMessageId = tempId,
                    senderId = effectiveSenderId,
                    senderName = effectiveSenderName,
                    senderRole = senderRole,
                    content = content.trim()
                )
                val resp = api.sendMessage(threadId, req)
                if (resp.success && resp.data != null) {
                    val canonicalMsg = resp.data
                    _threads.value = _threads.value.map { th ->
                        if (th.id == threadId) {
                            val updatedMsgs = th.messages.map { msg ->
                                if (msg.id == tempId) msg.copy(id = canonicalMsg.id) else msg
                            }
                            th.copy(messages = updatedMsgs)
                        } else th
                    }
                    saveToDisk(_threads.value)
                    // Refresh detail to ensure canonical sync
                    loadThreadDetail(threadId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist chat message to database")
            }
        }

        return localMsg
    }

    fun createInquiry(
        studentId: String,
        studentName: String,
        studentClass: String,
        teacherName: String = "Guru Pengampu",
        subjectName: String = "Umum",
        inquiryType: InquiryType,
        referenceTitle: String,
        referenceId: String? = null,
        initialQuestion: String,
    ): ChatThread {
        val newThreadId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val auth = cachedAuth

        val effectiveStudentName = if (!auth?.name.isNullOrBlank()) auth!!.name!! else studentName
        val effectiveStudentId = if (!auth?.userId.isNullOrBlank()) auth!!.userId!! else studentId

        val initialMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            threadId = newThreadId,
            senderId = effectiveStudentId,
            senderName = effectiveStudentName,
            senderRole = "STUDENT",
            content = initialQuestion.trim(),
            timestamp = now,
            isFromTeacher = false,
        )

        val newThread = ChatThread(
            id = newThreadId,
            studentId = effectiveStudentId,
            studentName = effectiveStudentName,
            studentClass = studentClass,
            teacherName = teacherName,
            subjectName = subjectName,
            inquiryType = inquiryType,
            referenceTitle = referenceTitle,
            referenceId = referenceId,
            messages = listOf(initialMsg),
            status = InquiryStatus.WAITING_REPLY,
            lastUpdated = now,
        )

        _threads.value = listOf(newThread) + _threads.value
        saveToDisk(_threads.value)

        // Persist to database
        scope.launch {
            try {
                val req = CreateInquiryRequestDto(
                    id = newThreadId,
                    studentId = if (effectiveStudentId.contains("-") && effectiveStudentId.length == 36) effectiveStudentId else null,
                    studentName = effectiveStudentName,
                    studentClass = studentClass,
                    teacherName = teacherName,
                    subjectName = subjectName,
                    inquiryType = inquiryType.name,
                    referenceTitle = referenceTitle,
                    referenceId = referenceId,
                    initialMessage = initialQuestion.trim()
                )
                val resp = api.createInquiry(req)
                if (resp.success && resp.data != null) {
                    refresh()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist new inquiry to database")
            }
        }

        return newThread
    }

    private fun getCacheFile(): File {
        val uid = cachedAuth?.userId
        return if (!uid.isNullOrBlank()) {
            File(context.filesDir, "chat_threads_cache_${uid}.json")
        } else {
            File(context.filesDir, "chat_threads_cache.json")
        }
    }

    private fun saveToDisk(threadsList: List<ChatThread>) {
        try {
            val jsonArray = JSONArray()
            for (thread in threadsList) {
                val tObj = JSONObject().apply {
                    put("id", thread.id)
                    put("studentId", thread.studentId)
                    put("studentName", thread.studentName)
                    put("studentClass", thread.studentClass)
                    put("teacherId", thread.teacherId)
                    put("teacherName", thread.teacherName)
                    put("subjectName", thread.subjectName)
                    put("inquiryType", thread.inquiryType.name)
                    put("referenceTitle", thread.referenceTitle)
                    put("referenceId", thread.referenceId ?: JSONObject.NULL)
                    put("status", thread.status.name)
                    put("lastUpdated", thread.lastUpdated)

                    val mArray = JSONArray()
                    for (msg in thread.messages) {
                        val mObj = JSONObject().apply {
                            put("id", msg.id)
                            put("threadId", msg.threadId)
                            put("senderId", msg.senderId)
                            put("senderName", msg.senderName)
                            put("senderRole", msg.senderRole)
                            put("content", msg.content)
                            put("timestamp", msg.timestamp)
                            put("isFromTeacher", msg.isFromTeacher)
                        }
                        mArray.put(mObj)
                    }
                    put("messages", mArray)
                }
                jsonArray.put(tObj)
            }

            val file = getCacheFile()
            val tempFile = File(context.filesDir, file.name + ".tmp")
            tempFile.writeText(jsonArray.toString(), Charsets.UTF_8)
            if (!tempFile.renameTo(file)) {
                file.delete()
                if (!tempFile.renameTo(file)) {
                    file.writeText(jsonArray.toString(), Charsets.UTF_8)
                }
            }
            val generalFile = File(context.filesDir, "chat_threads_cache.json")
            if (file != generalFile) {
                generalFile.writeText(jsonArray.toString(), Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to persist chat cache to disk")
        }
    }

    private fun loadFromDisk(): List<ChatThread> {
        return try {
            var file = getCacheFile()
            if (!file.exists()) {
                val generalFile = File(context.filesDir, "chat_threads_cache.json")
                if (generalFile.exists()) {
                    file = generalFile
                } else {
                    return emptyList()
                }
            }
            val text = file.readText(Charsets.UTF_8)
            if (text.isBlank()) return emptyList()

            val jsonArray = JSONArray(text)
            val result = ArrayList<ChatThread>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                val tObj = jsonArray.getJSONObject(i)
                val msgs = ArrayList<ChatMessage>()
                if (tObj.has("messages")) {
                    val mArray = tObj.getJSONArray("messages")
                    for (j in 0 until mArray.length()) {
                        val mObj = mArray.getJSONObject(j)
                        msgs.add(
                            ChatMessage(
                                id = mObj.optString("id", UUID.randomUUID().toString()),
                                threadId = mObj.optString("threadId", tObj.optString("id")),
                                senderId = mObj.optString("senderId", ""),
                                senderName = mObj.optString("senderName", ""),
                                senderRole = mObj.optString("senderRole", "STUDENT"),
                                content = mObj.optString("content", ""),
                                timestamp = mObj.optLong("timestamp", System.currentTimeMillis()),
                                isFromTeacher = mObj.optBoolean("isFromTeacher", false),
                            )
                        )
                    }
                }
                val inqType = try {
                    InquiryType.valueOf(tObj.optString("inquiryType", "MATERIAL"))
                } catch (_: Exception) {
                    InquiryType.MATERIAL
                }
                val inqStatus = try {
                    InquiryStatus.valueOf(tObj.optString("status", "WAITING_REPLY"))
                } catch (_: Exception) {
                    InquiryStatus.WAITING_REPLY
                }
                result.add(
                    ChatThread(
                        id = tObj.optString("id"),
                        studentId = tObj.optString("studentId"),
                        studentName = tObj.optString("studentName"),
                        studentClass = tObj.optString("studentClass", "Siswa"),
                        teacherId = tObj.optString("teacherId", "teacher-default"),
                        teacherName = tObj.optString("teacherName", "Guru Pengampu"),
                        subjectName = tObj.optString("subjectName", "Umum"),
                        inquiryType = inqType,
                        referenceTitle = tObj.optString("referenceTitle", ""),
                        referenceId = if (tObj.isNull("referenceId")) null else tObj.optString("referenceId"),
                        messages = msgs,
                        status = inqStatus,
                        lastUpdated = tObj.optLong("lastUpdated", System.currentTimeMillis()),
                    )
                )
            }
            result
        } catch (e: Exception) {
            Timber.w(e, "Failed to load chat cache from disk")
            emptyList()
        }
    }

    private fun parseIsoTimestamp(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            Instant.parse(isoString).toEpochMilli()
        } catch (_: Exception) {
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(isoString)?.time
                    ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
