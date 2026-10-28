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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    val isRead: Boolean = false,
    val readAt: Long? = null,
)

data class ChatThread(
    val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val studentName: String,
    val studentClass: String,
    val teacherId: String = "teacher-default",
    val teacherName: String = "Guru Mata Pelajaran",
    val subjectName: String = "Umum",
    val inquiryType: InquiryType = InquiryType.MATERIAL,
    val referenceTitle: String,
    val referenceId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val status: InquiryStatus = InquiryStatus.WAITING_REPLY,
    val lastUpdated: Long = System.currentTimeMillis(),
    val studentAvatarUrl: String? = null,
    val teacherAvatarUrl: String? = null,
    val isRead: Boolean = false,
    val readStatusLabel: String? = null,
    val lastMessageContent: String? = null,
    val lastSenderName: String? = null,
    val messageCount: Int = 0,
    val teacherLastReadAt: Long? = null,
    val studentLastReadAt: Long? = null,
) {
    val lastMessage: ChatMessage? get() = messages.lastOrNull() ?: if (!lastMessageContent.isNullOrBlank()) {
        ChatMessage(
            id = "preview-$id",
            threadId = id,
            senderId = "",
            senderName = lastSenderName ?: (if (status == InquiryStatus.ANSWERED) teacherName else studentName),
            senderRole = if (status == InquiryStatus.ANSWERED) "TEACHER" else "STUDENT",
            content = lastMessageContent,
            timestamp = lastUpdated,
            isFromTeacher = status == InquiryStatus.ANSWERED,
            isRead = isRead,
        )
    } else null

    val isAwaitingReply: Boolean get() = status == InquiryStatus.WAITING_REPLY
    val effectiveMessageCount: Int get() = if (messages.isNotEmpty()) messages.size else messageCount.coerceAtLeast(if (!lastMessageContent.isNullOrBlank()) 1 else 0)
}

@Singleton
class ChatManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: ApiClient,
    private val networkMonitor: NetworkMonitor,
    private val authManager: AuthManager,
    private val firebaseRtdbManager: com.schoolos.android.core.firebase.FirebaseRealtimeDatabaseManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val api: InquiriesApi by lazy { apiClient.create() }

    // Genuine real-time device network connectivity & Firebase Realtime Database status
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(scope, SharingStarted.Eagerly, true)

    val isRealtimeConnected: StateFlow<Boolean> = firebaseRtdbManager.isConnected

    private var cachedAuth: AuthState? = null
    val currentAuth: AuthState? get() = cachedAuth

    private fun normalizePersonName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        return name.trim().lowercase().replace(Regex("\\s+"), " ")
    }

    private fun isSameStudentIdentity(
        candidateId: String?,
        candidateName: String?,
        myUid: String?,
        myName: String?,
    ): Boolean {
        val cleanCandidateId = candidateId?.trim().orEmpty()
        val cleanMyUid = myUid?.trim().orEmpty()
        val matchesId = cleanMyUid.isNotBlank() && cleanCandidateId.isNotBlank() && (
            cleanCandidateId.equals(cleanMyUid, ignoreCase = true) ||
                cleanCandidateId.replace("-", "").equals(cleanMyUid.replace("-", ""), ignoreCase = true)
            )
        if (matchesId) return true
        // Only fall back to exact normalized full-name match if studentId is missing/non-UUID
        val normCandidate = normalizePersonName(candidateName)
        val normMyName = normalizePersonName(myName)
        return normMyName.isNotBlank() && normCandidate.isNotBlank() && normCandidate == normMyName
    }

    fun filterThreadsForUser(threadList: List<ChatThread>, auth: AuthState? = cachedAuth): List<ChatThread> {
        if (auth == null || !auth.isLoggedIn) return emptyList()
        if (auth.isTeacher) {
            return threadList
        }
        return threadList.filter { t ->
            isSameStudentIdentity(t.studentId, t.studentName, auth.userId, auth.name)
        }
    }

    fun isThreadOwnedByCurrentStudent(thread: ChatThread): Boolean {
        val auth = cachedAuth ?: return false
        if (!auth.isLoggedIn) return false
        if (auth.isTeacher) return true
        return isSameStudentIdentity(thread.studentId, thread.studentName, auth.userId, auth.name)
    }

    // Clean initial state to avoid cross-user bleed before auth resolution
    private val _threads = MutableStateFlow<List<ChatThread>>(emptyList())
    val threads: StateFlow<List<ChatThread>> = _threads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null
    private var threadDetailJob: kotlinx.coroutines.Job? = null
    private var activeDetailThreadId: String? = null
    private var activeSubscribers = 0

    init {
        // Purge any legacy unnamespaced global cache file to prevent cross-account leak
        try {
            val legacyCache = File(context.filesDir, "chat_threads_cache.json")
            if (legacyCache.exists()) {
                legacyCache.delete()
            }
        } catch (_: Exception) {}

        // Observe auth state continuously
        scope.launch {
            authManager.authState.collectLatest { auth ->
                val oldUid = cachedAuth?.userId
                val newUid = auth?.userId
                val wasLoggedIn = cachedAuth?.isLoggedIn == true
                val isLoggedIn = auth?.isLoggedIn == true
                cachedAuth = auth

                if (newUid != oldUid || wasLoggedIn != isLoggedIn) {
                    // Identity changed: immediately wipe threads from memory to prevent leak!
                    _threads.value = emptyList()
                    threadDetailJob?.cancel()
                    threadDetailJob = null
                    activeDetailThreadId = null
                    if (isLoggedIn && !newUid.isNullOrBlank()) {
                        val userCached = loadFromDisk(newUid)
                        _threads.value = filterThreadsForUser(userCached, auth)
                        refresh()
                    }
                } else if (_threads.value.isEmpty() && isLoggedIn && !newUid.isNullOrBlank()) {
                    val userCached = loadFromDisk(newUid)
                    _threads.value = filterThreadsForUser(userCached, auth)
                }
            }
        }

        // Real-time listener scoped strictly to the user's known thread IDs (never root /inquiries)
        scope.launch {
            @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
            _threads
                .map { list -> list.map { it.id }.filter { it.isNotBlank() }.toSet() }
                .distinctUntilChanged()
                .flatMapLatest { ids ->
                    firebaseRtdbManager.observeThreadSummaries(ids)
                }
                .collectLatest { rtdbMap: Map<String, Map<String, Any>> ->
                    if (rtdbMap.isNotEmpty()) {
                        val auth = cachedAuth ?: return@collectLatest
                        if (!auth.isLoggedIn) return@collectLatest
                        val currentList = _threads.value
                        var hasChanges = false
                        val updatedList = currentList.map { thread ->
                            val rtdbData = rtdbMap[thread.id]
                            if (rtdbData != null) {
                                val rtdbStatusStr = rtdbData["status"] as? String
                                val rtdbStatus = when (rtdbStatusStr?.uppercase()) {
                                    "ANSWERED" -> InquiryStatus.ANSWERED
                                    "WAITING_REPLY", "OPEN" -> InquiryStatus.WAITING_REPLY
                                    else -> thread.status
                                }
                                val rtdbLastMsg = rtdbData["lastMessageContent"] as? String
                                val rtdbLastMsgAt = (rtdbData["lastMessageAt"] as? Number)?.toLong() ?: thread.lastUpdated
                                val rtdbLastSender = rtdbData["lastSenderName"] as? String
                                val rtdbTeacherReadAt = (rtdbData["teacherLastReadAt"] as? Number)?.toLong()
                                val rtdbReadStatusLabel = rtdbData["readStatusLabel"] as? String

                                val isStatusChanged = thread.status != rtdbStatus
                                val isMsgChanged = rtdbLastMsg != null && thread.lastMessage?.content != rtdbLastMsg
                                val isReadChanged = rtdbTeacherReadAt != null && rtdbTeacherReadAt != thread.teacherLastReadAt
                                val isLabelChanged = rtdbReadStatusLabel != null && rtdbReadStatusLabel != thread.readStatusLabel

                                if (isStatusChanged || isMsgChanged || isReadChanged || isLabelChanged) {
                                    hasChanges = true
                                    thread.copy(
                                        status = rtdbStatus,
                                        lastUpdated = rtdbLastMsgAt.coerceAtLeast(thread.lastUpdated),
                                        lastMessageContent = rtdbLastMsg ?: thread.lastMessageContent,
                                        lastSenderName = rtdbLastSender ?: thread.lastSenderName,
                                        teacherLastReadAt = rtdbTeacherReadAt ?: thread.teacherLastReadAt,
                                        readStatusLabel = rtdbReadStatusLabel ?: thread.readStatusLabel,
                                        messageCount = if (thread.messages.isNotEmpty()) thread.messages.size else (thread.messageCount.coerceAtLeast(if (rtdbLastMsg != null) 1 else 0)),
                                    )
                                } else {
                                    thread
                                }
                            } else {
                                thread
                            }
                        }
                        if (hasChanges) {
                            _threads.value = updatedList
                            saveToDisk(updatedList, auth.userId)
                        }
                    }
                }
        }

        // Initial fetch from database
        refresh()

        // Automatically sync whenever internet connection is restored
        scope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                if (online && cachedAuth?.isLoggedIn == true) {
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
     * Full sandbox purge on logout: cancel polling & active RTDB listeners, clear memory & disk caches.
     */
    @Synchronized
    fun clearOnLogout() {
        activeSubscribers = 0
        pollingJob?.cancel()
        pollingJob = null
        threadDetailJob?.cancel()
        threadDetailJob = null
        activeDetailThreadId = null
        _threads.value = emptyList()
        try {
            context.filesDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("chat_threads_cache")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete chat cache files on logout")
        }
    }

    /**
     * Start live polling when Chat UI is active and visible on screen.
     */
    @Synchronized
    fun startPolling() {
        activeSubscribers++
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                delay(8000)
                if (isOnline.value && cachedAuth?.isLoggedIn == true) {
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
    @Synchronized
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
        if (auth == null || !auth.isLoggedIn) {
            _threads.value = emptyList()
            return
        }

        val teacherName = if (auth.isTeacher) auth.name else null
        val studentId = if (auth.isStudent && !auth.userId.isNullOrBlank() && auth.userId.length == 36) auth.userId else null

        val response = api.listInquiries(
            teacherName = teacherName,
            studentId = studentId,
        )
        if (response.success && response.data != null) {
            val rawDbThreads = response.data
            // Strictly filter server response if student to guarantee isolation
            val dbThreads = if (auth.isStudent) {
                rawDbThreads.filter { dto ->
                    isSameStudentIdentity(dto.studentId, dto.studentName, auth.userId, auth.name)
                }
            } else {
                rawDbThreads
            }

            val currentMap = _threads.value.associateBy { it.id }
            val dbIds = dbThreads.map { it.id }.toSet()

            val updatedThreads = dbThreads.map { dto ->
                val type = when (dto.inquiryType.uppercase()) {
                    "ASSIGNMENT" -> InquiryType.ASSIGNMENT
                    "GENERAL" -> InquiryType.GENERAL
                    else -> InquiryType.MATERIAL
                }
                val st = if (dto.status.uppercase() == "ANSWERED") InquiryStatus.ANSWERED else InquiryStatus.WAITING_REPLY
                val timestamp = parseIsoTimestamp(dto.lastMessageAt ?: dto.createdAt)

                // Messages are strictly managed in real time via Firebase Realtime Database
                val existing = currentMap[dto.id]
                val messages = existing?.messages ?: emptyList()

                val resolvedTeacherName = if (dto.teacherName.isNotBlank() && !dto.teacherName.equals("Guru Pengampu", ignoreCase = true)) {
                    dto.teacherName
                } else if (existing != null && existing.teacherName.isNotBlank() && !existing.teacherName.equals("Guru Pengampu", ignoreCase = true)) {
                    existing.teacherName
                } else {
                    "Guru Mata Pelajaran"
                }

                val effectiveStatus = if (messages.lastOrNull()?.isFromTeacher == true) {
                    InquiryStatus.ANSWERED
                } else if (dto.status.uppercase() == "ANSWERED") {
                    InquiryStatus.ANSWERED
                } else {
                    InquiryStatus.WAITING_REPLY
                }

                val lastMsgText = messages.lastOrNull()?.content ?: dto.lastMessageContent ?: existing?.lastMessageContent
                val count = if (messages.isNotEmpty()) messages.size else dto.messageCount.toInt().coerceAtLeast(existing?.messageCount ?: 0)
                val tReadAt = dto.teacherLastReadAt?.let { parseIsoTimestamp(it) } ?: existing?.teacherLastReadAt
                val sReadAt = dto.studentLastReadAt?.let { parseIsoTimestamp(it) } ?: existing?.studentLastReadAt

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
                    status = effectiveStatus,
                    lastUpdated = messages.lastOrNull()?.timestamp ?: timestamp,
                    studentAvatarUrl = dto.studentAvatarUrl ?: existing?.studentAvatarUrl,
                    teacherAvatarUrl = dto.teacherAvatarUrl ?: existing?.teacherAvatarUrl,
                    isRead = dto.isRead,
                    readStatusLabel = dto.readStatusLabel ?: existing?.readStatusLabel,
                    lastMessageContent = lastMsgText,
                    lastSenderName = messages.lastOrNull()?.senderName ?: existing?.lastSenderName,
                    messageCount = count,
                    teacherLastReadAt = tReadAt,
                    studentLastReadAt = sReadAt,
                )
            }

            // Consolidate duplicate threads for the same student + subject / reference
            val consolidatedThreads = updatedThreads
                .groupBy { "${it.studentName.trim().lowercase()}_${it.subjectName.trim().lowercase()}_${it.referenceTitle.trim().lowercase()}" }
                .map { (_, groupThreads) ->
                    if (groupThreads.size == 1) {
                        val single = groupThreads.first()
                        val lastMsg = single.messages.lastOrNull()
                        val latestStatus = if (lastMsg?.isFromTeacher == true) InquiryStatus.ANSWERED else single.status
                        single.copy(status = latestStatus)
                    } else {
                        val sortedByTime = groupThreads.sortedByDescending { it.lastUpdated }
                        val primary = sortedByTime.first()
                        val allMessages = groupThreads.flatMap { it.messages }
                            .distinctBy { it.id }
                            .sortedBy { it.timestamp }

                        val lastMsg = allMessages.lastOrNull()
                        val latestStatus = if (lastMsg?.isFromTeacher == true) InquiryStatus.ANSWERED else InquiryStatus.WAITING_REPLY

                        primary.copy(
                            messages = allMessages,
                            status = latestStatus,
                            lastUpdated = lastMsg?.timestamp ?: primary.lastUpdated
                        )
                    }
                }

            // Keep any local threads currently pending server persistence, strictly isolated
            val pendingLocal = _threads.value.filter { it.id !in dbIds }.let { list ->
                if (auth.isStudent) filterThreadsForUser(list, auth) else list
            }
            val merged = (pendingLocal + consolidatedThreads).sortedByDescending { it.lastUpdated }
            _threads.value = merged
            saveToDisk(merged, auth.userId)
        }
    }

    fun loadThreadDetail(threadId: String) {
        if (threadId.isBlank()) return
        // Cancel any previous thread message listener before starting a new one
        if (activeDetailThreadId != threadId || threadDetailJob?.isActive != true) {
            threadDetailJob?.cancel()
            activeDetailThreadId = threadId
            threadDetailJob = scope.launch {
                firebaseRtdbManager.observeThreadMessages(threadId).collect { rtdbMsgs ->
                    _threads.value = _threads.value.map { th ->
                        if (th.id == threadId) {
                            val lastMsg = rtdbMsgs.lastOrNull()
                            val updatedStatus = if (lastMsg?.isFromTeacher == true) {
                                InquiryStatus.ANSWERED
                            } else if (rtdbMsgs.isNotEmpty()) {
                                InquiryStatus.WAITING_REPLY
                            } else {
                                th.status
                            }
                            th.copy(
                                messages = rtdbMsgs,
                                status = updatedStatus,
                                lastUpdated = lastMsg?.timestamp ?: th.lastUpdated,
                                lastMessageContent = lastMsg?.content ?: th.lastMessageContent,
                                lastSenderName = lastMsg?.senderName ?: th.lastSenderName,
                                messageCount = rtdbMsgs.size,
                            )
                        } else th
                    }
                }
            }
        }

        // If teacher is viewing, immediately mark thread as read in Firebase RTDB
        if (cachedAuth?.isTeacher == true) {
            firebaseRtdbManager.markThreadReadByTeacher(threadId)
        }

        scope.launch {
            try {
                val detailResp = api.getInquiryDetail(threadId)
                if (detailResp.success && detailResp.data != null) {
                    val detail = detailResp.data
                    val auth = cachedAuth

                    // Student security: ignore thread if it does not belong to the current student
                    if (auth != null && auth.isStudent) {
                        if (!isSameStudentIdentity(detail.thread.studentId, detail.thread.studentName, auth.userId, auth.name)) {
                            Timber.w("Refusing to display unowned thread ${detail.thread.id} to student ${auth.name}")
                            return@launch
                        }
                    }

                    _threads.value = _threads.value.let { currentList ->
                        val index = currentList.indexOfFirst { it.id == threadId }
                        if (index >= 0) {
                            val existing = currentList[index]
                            val updatedTeacherName = if (detail.thread.teacherName.isNotBlank() && !detail.thread.teacherName.equals("Guru Pengampu", ignoreCase = true)) {
                                detail.thread.teacherName
                            } else {
                                existing.teacherName
                            }
                            val apiMessages = detail.messages.map { m ->
                                ChatMessage(
                                    id = m.id,
                                    threadId = m.threadId,
                                    senderId = m.senderId,
                                    senderName = m.senderName,
                                    senderRole = m.senderRole,
                                    content = m.content,
                                    timestamp = parseIsoTimestamp(m.createdAt),
                                    isFromTeacher = m.isFromTeacher,
                                    isRead = m.isRead,
                                    readAt = m.readAt?.let { parseIsoTimestamp(it) },
                                )
                            }
                            val mergedMessages = if (existing.messages.isNotEmpty()) {
                                existing.messages.map { em ->
                                    val match = apiMessages.find { it.id == em.id || (it.senderId == em.senderId && it.content == em.content) }
                                    if (match != null) em.copy(isRead = match.isRead, readAt = match.readAt) else em
                                }
                            } else {
                                apiMessages
                            }
                            val updated = existing.copy(
                                teacherName = updatedTeacherName,
                                teacherId = detail.thread.teacherId ?: existing.teacherId,
                                studentAvatarUrl = detail.thread.studentAvatarUrl ?: existing.studentAvatarUrl,
                                teacherAvatarUrl = detail.thread.teacherAvatarUrl ?: existing.teacherAvatarUrl,
                                isRead = detail.thread.isRead,
                                readStatusLabel = detail.thread.readStatusLabel ?: existing.readStatusLabel,
                                messages = mergedMessages,
                            )
                            currentList.toMutableList().apply { set(index, updated) }
                        } else {
                            // Thread was opened directly (e.g. push notification or deep link)
                            val type = when (detail.thread.inquiryType.uppercase()) {
                                "ASSIGNMENT" -> InquiryType.ASSIGNMENT
                                "GENERAL" -> InquiryType.GENERAL
                                else -> InquiryType.MATERIAL
                            }
                            val initialMsgs = detail.messages.map { m ->
                                ChatMessage(
                                    id = m.id,
                                    threadId = m.threadId,
                                    senderId = m.senderId,
                                    senderName = m.senderName,
                                    senderRole = m.senderRole,
                                    content = m.content,
                                    timestamp = parseIsoTimestamp(m.createdAt),
                                    isFromTeacher = m.isFromTeacher,
                                    isRead = m.isRead,
                                    readAt = m.readAt?.let { parseIsoTimestamp(it) },
                                )
                            }
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
                                    messages = initialMsgs,
                                    status = if (detail.thread.status.uppercase() == "ANSWERED") InquiryStatus.ANSWERED else InquiryStatus.WAITING_REPLY,
                                    lastUpdated = parseIsoTimestamp(detail.thread.lastMessageAt ?: detail.thread.createdAt),
                                    studentAvatarUrl = detail.thread.studentAvatarUrl,
                                    teacherAvatarUrl = detail.thread.teacherAvatarUrl,
                                    isRead = detail.thread.isRead,
                                    readStatusLabel = detail.thread.readStatusLabel,
                                )
                            ) + currentList
                        }
                    }

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
        if (activeDetailThreadId != threadId || threadDetailJob?.isActive != true) {
            loadThreadDetail(threadId)
        }
        return _threads.value.find { it.id == threadId }
    }

    fun sendMessage(
        threadId: String,
        senderId: String,
        senderName: String,
        senderRole: String,
        content: String,
    ): ChatMessage {
        val auth = cachedAuth
        val isTeacher = if (auth?.isStudent == true) false else (senderRole.equals("TEACHER", ignoreCase = true) || auth?.isTeacher == true)
        val now = System.currentTimeMillis()
        val clientUuid = UUID.randomUUID().toString()

        val effectiveSenderName = when {
            auth?.isStudent == true -> auth.name ?: "Siswa"
            !auth?.name.isNullOrBlank() -> auth.name!!
            senderName.isNotBlank() && !senderName.equals("Guru Pengampu", ignoreCase = true) -> senderName
            isTeacher -> "Guru Pengajar"
            else -> "Siswa"
        }
        val effectiveSenderId = when {
            !auth?.userId.isNullOrBlank() -> auth.userId!!
            senderId.isNotBlank() -> senderId
            else -> UUID.randomUUID().toString()
        }
        val effectiveRole = if (isTeacher) "TEACHER" else "STUDENT"

        val localMsg = ChatMessage(
            id = clientUuid,
            threadId = threadId,
            senderId = effectiveSenderId,
            senderName = effectiveSenderName,
            senderRole = effectiveRole,
            content = content.trim(),
            timestamp = now,
            isFromTeacher = isTeacher,
            isRead = false,
        )

        val currentThread = _threads.value.find { it.id == threadId }
        val updatedTeacherName = if (isTeacher && effectiveSenderName.isNotBlank() && !effectiveSenderName.equals("Guru Pengampu", ignoreCase = true)) {
            effectiveSenderName
        } else {
            currentThread?.teacherName ?: ""
        }

        // Single Source of Truth: Push directly to Firebase Realtime Database
        firebaseRtdbManager.pushMessage(
            threadId = threadId,
            message = localMsg,
            studentId = currentThread?.studentId,
            studentName = currentThread?.studentName,
            teacherId = currentThread?.teacherId,
            teacherName = updatedTeacherName,
        )

        // Optimistically update in-memory thread
        _threads.value = _threads.value.map { thread ->
            if (thread.id == threadId) {
                val alreadyHas = thread.messages.any { it.id == clientUuid }
                val updatedMsgs = if (alreadyHas) thread.messages else thread.messages + localMsg
                thread.copy(
                    messages = updatedMsgs,
                    status = if (isTeacher) InquiryStatus.ANSWERED else InquiryStatus.WAITING_REPLY,
                    lastUpdated = now,
                    teacherName = updatedTeacherName,
                    lastMessageContent = content.trim(),
                    lastSenderName = effectiveSenderName,
                    messageCount = updatedMsgs.size,
                    readStatusLabel = if (isTeacher) "Dijawab Tutor Pamong" else "Menunggu Ditinjau Tutor Pamong",
                    teacherLastReadAt = if (isTeacher) now else 0L,
                )
            } else {
                thread
            }
        }

        // Asynchronously persist to backend API so PostgreSQL stays 100% synchronized
        scope.launch {
            try {
                api.sendMessage(
                    threadId = threadId,
                    request = SendInquiryMessageRequestDto(
                        clientMessageId = clientUuid,
                        senderId = effectiveSenderId,
                        senderName = effectiveSenderName,
                        senderRole = effectiveRole,
                        content = content.trim(),
                    )
                )
            } catch (e: Exception) {
                Timber.w(e, "Async persist of message to server database failed")
            }
        }

        return localMsg
    }

    fun createInquiry(
        studentId: String,
        studentName: String,
        studentClass: String,
        teacherName: String = "Guru Mata Pelajaran",
        subjectName: String = "Umum",
        inquiryType: InquiryType,
        referenceTitle: String,
        referenceId: String? = null,
        initialQuestion: String,
    ): ChatThread {
        val auth = cachedAuth
        val effectiveStudentName = if (!auth?.name.isNullOrBlank()) auth.name!! else studentName
        val effectiveStudentId = if (!auth?.userId.isNullOrBlank()) auth.userId!! else studentId
        val effectiveTeacherName = if (teacherName.isNotBlank() && !teacherName.equals("Guru Pengampu", ignoreCase = true)) {
            teacherName
        } else {
            "Guru Mata Pelajaran"
        }

        // Reuse existing active thread if student already has a thread for this subject/reference
        val existingThread = _threads.value.find { thread ->
            val matchesStudent = (effectiveStudentId.isNotBlank() && thread.studentId == effectiveStudentId) ||
                (effectiveStudentName.isNotBlank() && thread.studentName.equals(effectiveStudentName, ignoreCase = true))
            val matchesSubject = thread.subjectName.equals(subjectName, ignoreCase = true) ||
                thread.referenceTitle.equals(referenceTitle, ignoreCase = true)
            matchesStudent && matchesSubject
        }

        if (existingThread != null) {
            sendMessage(
                threadId = existingThread.id,
                senderId = effectiveStudentId,
                senderName = effectiveStudentName,
                senderRole = "STUDENT",
                content = initialQuestion.trim(),
            )
            return _threads.value.firstOrNull { it.id == existingThread.id } ?: existingThread
        }

        val newThreadId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val initialMsgId = UUID.randomUUID().toString()

        val initialMsg = ChatMessage(
            id = initialMsgId,
            threadId = newThreadId,
            senderId = effectiveStudentId,
            senderName = effectiveStudentName,
            senderRole = "STUDENT",
            content = initialQuestion.trim(),
            timestamp = now,
            isFromTeacher = false,
            isRead = false,
        )

        val newThread = ChatThread(
            id = newThreadId,
            studentId = effectiveStudentId,
            studentName = effectiveStudentName,
            studentClass = studentClass,
            teacherName = effectiveTeacherName,
            subjectName = subjectName,
            inquiryType = inquiryType,
            referenceTitle = referenceTitle,
            referenceId = referenceId,
            messages = listOf(initialMsg),
            status = InquiryStatus.WAITING_REPLY,
            lastUpdated = now,
            lastMessageContent = initialQuestion.trim(),
            lastSenderName = effectiveStudentName,
            messageCount = 1,
            readStatusLabel = "Menunggu Dibaca Guru",
            teacherLastReadAt = 0L,
        )

        _threads.value = listOf(newThread) + _threads.value
        saveToDisk(_threads.value, auth?.userId)

        // Push thread & initial message to Firebase Realtime Database instantly!
        firebaseRtdbManager.syncThreadMetadata(newThread)
        firebaseRtdbManager.pushMessage(newThreadId, initialMsg)

        // Persist to database with client_message_id matching initialMsgId to prevent duplication
        scope.launch {
            try {
                val req = CreateInquiryRequestDto(
                    id = newThreadId,
                    studentId = if (effectiveStudentId.contains("-") && effectiveStudentId.length == 36) effectiveStudentId else null,
                    studentName = effectiveStudentName,
                    studentClass = studentClass,
                    teacherName = effectiveTeacherName,
                    subjectName = subjectName,
                    inquiryType = inquiryType.name,
                    referenceTitle = referenceTitle,
                    referenceId = referenceId,
                    initialMessage = initialQuestion.trim(),
                    clientMessageId = initialMsgId,
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

    private fun getCacheFile(userId: String? = cachedAuth?.userId): File? {
        if (userId.isNullOrBlank()) return null
        val sanitized = userId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(context.filesDir, "chat_threads_cache_${sanitized}.json")
    }

    private fun saveToDisk(threadsList: List<ChatThread>, targetUid: String? = cachedAuth?.userId) {
        val file = getCacheFile(targetUid) ?: return
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
                    put("lastMessageContent", thread.lastMessageContent ?: JSONObject.NULL)
                    put("lastSenderName", thread.lastSenderName ?: JSONObject.NULL)
                    put("messageCount", thread.effectiveMessageCount)
                    put("readStatusLabel", thread.readStatusLabel ?: JSONObject.NULL)
                    put("teacherLastReadAt", thread.teacherLastReadAt ?: JSONObject.NULL)
                    put("studentLastReadAt", thread.studentLastReadAt ?: JSONObject.NULL)

                    // Chat messages are managed exclusively via Firebase RTDB (no disk cache)
                    put("messages", JSONArray())
                }
                jsonArray.put(tObj)
            }

            val tempFile = File(context.filesDir, file.name + ".tmp")
            tempFile.writeText(jsonArray.toString(), Charsets.UTF_8)
            if (!tempFile.renameTo(file)) {
                file.delete()
                if (!tempFile.renameTo(file)) {
                    file.writeText(jsonArray.toString(), Charsets.UTF_8)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to persist chat cache to disk")
        }
    }

    private fun loadFromDisk(targetUid: String? = cachedAuth?.userId): List<ChatThread> {
        val file = getCacheFile(targetUid) ?: return emptyList()
        return try {
            if (!file.exists()) return emptyList()
            val text = file.readText(Charsets.UTF_8)
            if (text.isBlank()) return emptyList()

            val jsonArray = JSONArray(text)
            val result = ArrayList<ChatThread>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                val tObj = jsonArray.getJSONObject(i)
                val msgs = emptyList<ChatMessage>() // Real-time messages are streamed directly from Firebase RTDB
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
                val rawTeacherName = tObj.optString("teacherName", "")
                val effectiveTeacherName = if (rawTeacherName.isNotBlank() && !rawTeacherName.equals("Guru Pengampu", ignoreCase = true)) {
                    rawTeacherName
                } else {
                    "Guru Mata Pelajaran"
                }
                result.add(
                    ChatThread(
                        id = tObj.optString("id"),
                        studentId = tObj.optString("studentId"),
                        studentName = tObj.optString("studentName"),
                        studentClass = tObj.optString("studentClass", "Siswa"),
                        teacherId = tObj.optString("teacherId", "teacher-default"),
                        teacherName = effectiveTeacherName,
                        subjectName = tObj.optString("subjectName", "Umum"),
                        inquiryType = inqType,
                        referenceTitle = tObj.optString("referenceTitle", ""),
                        referenceId = if (tObj.isNull("referenceId")) null else tObj.optString("referenceId"),
                        messages = msgs,
                        status = inqStatus,
                        lastUpdated = tObj.optLong("lastUpdated", System.currentTimeMillis()),
                        lastMessageContent = if (tObj.isNull("lastMessageContent")) null else tObj.optString("lastMessageContent"),
                        lastSenderName = if (tObj.isNull("lastSenderName")) null else tObj.optString("lastSenderName"),
                        messageCount = tObj.optInt("messageCount", 0),
                        readStatusLabel = if (tObj.isNull("readStatusLabel")) null else tObj.optString("readStatusLabel"),
                        teacherLastReadAt = if (tObj.isNull("teacherLastReadAt")) null else tObj.optLong("teacherLastReadAt"),
                        studentLastReadAt = if (tObj.isNull("studentLastReadAt")) null else tObj.optLong("studentLastReadAt"),
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

    /**
     * Update active presence for user in thread and globally in Firebase RTDB.
     */
    fun setPresence(threadId: String, isOnline: Boolean) {
        val auth = cachedAuth ?: return
        val userId = auth.userId ?: return
        val userName = auth.name ?: "User"
        val role = if (auth.isTeacher) "TEACHER" else "STUDENT"
        firebaseRtdbManager.setUserPresence(userId, userName, role, isOnline)
        if (threadId.isNotBlank()) {
            firebaseRtdbManager.setThreadPresence(threadId, role, userId, userName, isOnline)
        }
    }

    /**
     * Observe real-time online presence for participant in a thread.
     */
    fun observeThreadPresence(threadId: String, isTeacherMode: Boolean, targetUserId: String? = null): kotlinx.coroutines.flow.Flow<Boolean> {
        val targetRole = if (isTeacherMode) "STUDENT" else "TEACHER"
        return firebaseRtdbManager.observeThreadPresence(threadId, targetRole, targetUserId)
    }
}

