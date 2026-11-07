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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
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
    val attachmentUrl: String? = null,
    val attachmentType: String? = null,
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

    val teacherPresences: StateFlow<Map<String, Boolean>> = firebaseRtdbManager
        .observeAllPresences()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private var cachedAuth: AuthState? = null
    val currentAuth: AuthState? get() = cachedAuth

    fun cleanPersonName(name: String?): String {
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
        val normCandidate = cleanPersonName(candidateName)
        val normMyName = cleanPersonName(myName)
        return normMyName.isNotBlank() && normCandidate.isNotBlank() && (
            normCandidate == normMyName ||
            normCandidate.startsWith(normMyName) ||
            normMyName.startsWith(normCandidate)
        )
    }

    fun isSamePersonName(candidateName: String?, myName: String?): Boolean {
        if (candidateName.isNullOrBlank() || myName.isNullOrBlank()) return false
        val cleanCandidate = cleanPersonName(candidateName)
        val cleanMy = cleanPersonName(myName)
        if (cleanCandidate.isBlank() || cleanMy.isBlank()) return false
        if (cleanCandidate == cleanMy) return true
        if (cleanCandidate.startsWith(cleanMy) || cleanMy.startsWith(cleanCandidate)) return true
        val tokensA = cleanCandidate.split(" ")
        val tokensB = cleanMy.split(" ")
        if (tokensA.isNotEmpty() && tokensB.isNotEmpty() && tokensA[0] == tokensB[0]) {
            if (tokensA.size >= 2 && tokensB.size >= 2) {
                return tokensA[1] == tokensB[1]
            }
            return true
        }
        return false
    }

    private fun isPrincipalThread(t: ChatThread, auth: AuthState): Boolean {
        val myId = auth.userId.orEmpty().trim()
        val myName = auth.name.orEmpty().trim()

        val idMatches = myId.isNotBlank() && (
            t.teacherId.equals(myId, ignoreCase = true) ||
            t.teacherId.replace("-", "").equals(myId.replace("-", ""), ignoreCase = true) ||
            t.studentId.equals(myId, ignoreCase = true) ||
            t.studentId.replace("-", "").equals(myId.replace("-", ""), ignoreCase = true)
        )
        if (idMatches) return true

        if (isSamePersonName(t.teacherName, myName) || isSamePersonName(t.studentName, myName)) return true

        val cleanTeacher = cleanPersonName(t.teacherName).lowercase()
        val cleanStudent = cleanPersonName(t.studentName).lowercase()
        val cleanMy = cleanPersonName(myName).lowercase()

        val nameMatchesSubstr = cleanMy.isNotBlank() && (
            (cleanTeacher.isNotBlank() && (cleanTeacher.contains(cleanMy) || cleanMy.contains(cleanTeacher))) ||
            (cleanStudent.isNotBlank() && (cleanStudent.contains(cleanMy) || cleanMy.contains(cleanStudent)))
        )
        if (nameMatchesSubstr) return true

        // Keyword matches for Principal consultations
        val matchesPrincipalRole =
            t.teacherName.contains("Kepala", ignoreCase = true) ||
            t.studentName.contains("Kepala", ignoreCase = true) ||
            t.studentClass.contains("Kepala", ignoreCase = true) ||
            t.referenceTitle.contains("Koordinasi", ignoreCase = true) ||
            t.referenceTitle.contains("Konsultasi Kebijakan", ignoreCase = true) ||
            t.referenceTitle.contains("Pengaduan", ignoreCase = true) ||
            t.subjectName.contains("Kepala", ignoreCase = true) ||
            t.subjectName.contains("Konsultasi Kebijakan", ignoreCase = true) ||
            t.subjectName.contains("Pengaduan", ignoreCase = true) ||
            t.id.startsWith("ai-principal")

        return matchesPrincipalRole
    }

    fun filterThreadsForUser(threadList: List<ChatThread>, auth: AuthState? = cachedAuth): List<ChatThread> {
        if (auth == null || !auth.isLoggedIn) return emptyList()
        if (auth.isPrincipal) {
            // Respect teacher-student privacy: Principal does NOT spy on private teacher-student inquiries.
            // Principal only sees inquiries directed to Principal or where Principal is participant/sender.
            return threadList.filter { t ->
                isPrincipalThread(t, auth)
            }
        }
        if (auth.isTeacher) {
            val myId = auth.userId.orEmpty().trim()
            val myName = auth.name.orEmpty().trim()
            val cleanMyName = cleanPersonName(myName).lowercase()
            return threadList.filter { t ->
                val idMatches = myId.isNotBlank() && (
                    t.teacherId.equals(myId, ignoreCase = true) ||
                    t.teacherId.replace("-", "").equals(myId.replace("-", ""), ignoreCase = true)
                )
                val cleanTeacher = cleanPersonName(t.teacherName).lowercase()
                val nameMatches = isSamePersonName(t.teacherName, myName) ||
                    (cleanMyName.isNotBlank() && cleanTeacher.isNotBlank() && (
                        cleanTeacher == cleanMyName ||
                        cleanTeacher.contains(cleanMyName) ||
                        cleanMyName.contains(cleanTeacher)
                    ))
                idMatches || nameMatches
            }
        }
        return threadList.filter { t ->
            isSameStudentIdentity(t.studentId, t.studentName, auth.userId, auth.name)
        }
    }

    fun isThreadOwnedByCurrentStudent(thread: ChatThread): Boolean {
        val auth = cachedAuth ?: return false
        if (!auth.isLoggedIn) return false
        if (auth.isPrincipal) {
            return isPrincipalThread(thread, auth)
        }
        if (auth.isTeacher) {
            val myId = auth.userId.orEmpty().trim()
            val myName = auth.name.orEmpty().trim()
            val cleanMyName = cleanPersonName(myName).lowercase()
            val idMatches = myId.isNotBlank() && (
                thread.teacherId.equals(myId, ignoreCase = true) ||
                thread.teacherId.replace("-", "").equals(myId.replace("-", ""), ignoreCase = true)
            )
            val cleanTeacher = cleanPersonName(thread.teacherName).lowercase()
            val nameMatches = isSamePersonName(thread.teacherName, myName) ||
                (cleanMyName.isNotBlank() && cleanTeacher.isNotBlank() && (
                    cleanTeacher == cleanMyName ||
                    cleanTeacher.contains(cleanMyName) ||
                    cleanMyName.contains(cleanTeacher)
                ))
            return idMatches || nameMatches
        }
        return isSameStudentIdentity(thread.studentId, thread.studentName, auth.userId, auth.name)
    }

    // Clean initial state to avoid cross-user bleed before auth resolution
    private val _threads = MutableStateFlow<List<ChatThread>>(emptyList())
    val threads: StateFlow<List<ChatThread>> = _threads.asStateFlow()

    fun getThreadById(threadId: String): ChatThread? {
        val direct = _threads.value.find { it.id == threadId }
        if (direct != null) return direct
        return _threads.value.find { t -> t.messages.any { m -> m.threadId == threadId } }
    }

    fun consolidateAndDeduplicateThreads(threadList: List<ChatThread>): List<ChatThread> {
        if (threadList.isEmpty()) return emptyList()

        return threadList
            .groupBy { thread ->
                if (thread.id.startsWith("ai-")) {
                    "ai_${thread.id}"
                } else {
                    val cleanStudent = cleanPersonName(thread.studentName).trim().lowercase()
                    val cleanTeacher = cleanPersonName(thread.teacherName).trim().lowercase()
                    val isPrincipal = thread.studentClass.contains("Kepala", ignoreCase = true) ||
                        thread.studentName.contains("Kepala", ignoreCase = true)
                    if (isPrincipal) {
                        "principal_${cleanStudent}_${cleanTeacher}"
                    } else {
                        val sId = thread.studentId.trim().ifBlank { cleanStudent }
                        "student_${sId}_${cleanStudent}_${cleanTeacher}"
                    }
                }
            }
            .map { (_, groupThreads) ->
                if (groupThreads.size == 1) {
                    groupThreads.first()
                } else {
                    val sorted = groupThreads.sortedByDescending { it.lastUpdated }
                    val primary = sorted.first()
                    val allMsgs = groupThreads.flatMap { it.messages }
                        .distinctBy { it.id }
                        .sortedBy { it.timestamp }
                    val lastMsg = allMsgs.lastOrNull()
                    val effectiveLastContent = lastMsg?.content
                        ?: sorted.firstOrNull { !it.lastMessageContent.isNullOrBlank() }?.lastMessageContent
                    val effectiveLastSender = lastMsg?.senderName
                        ?: sorted.firstOrNull { !it.lastSenderName.isNullOrBlank() }?.lastSenderName
                    val effectiveStatus = if (lastMsg?.isFromTeacher == true) {
                        InquiryStatus.ANSWERED
                    } else if (sorted.any { it.status == InquiryStatus.WAITING_REPLY }) {
                        InquiryStatus.WAITING_REPLY
                    } else {
                        InquiryStatus.ANSWERED
                    }
                    val effectiveTeacherLastReadAt = sorted.mapNotNull { it.teacherLastReadAt }.maxOrNull() ?: 0L
                    val effectiveStudentLastReadAt = sorted.mapNotNull { it.studentLastReadAt }.maxOrNull() ?: 0L

                    primary.copy(
                        messages = allMsgs,
                        messageCount = if (allMsgs.isNotEmpty()) allMsgs.size else sorted.sumOf { it.messageCount }.coerceAtLeast(1),
                        lastMessageContent = effectiveLastContent,
                        lastSenderName = effectiveLastSender,
                        status = effectiveStatus,
                        lastUpdated = lastMsg?.timestamp ?: primary.lastUpdated,
                        teacherLastReadAt = effectiveTeacherLastReadAt,
                        studentLastReadAt = effectiveStudentLastReadAt,
                    )
                }
            }
            .sortedByDescending { it.lastUpdated }
    }

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

        // Real-time listener: Stream all inquiries from Firebase Realtime Database
        // Automatically discovers new threads created by students, teachers, or principals in real time
        scope.launch {
            firebaseRtdbManager.observeAllInquiries().collectLatest { rtdbThreads ->
                val auth = cachedAuth ?: return@collectLatest
                if (!auth.isLoggedIn) return@collectLatest

                // Strictly filter for current authenticated user under ADR-0011 isolation rules
                val myRtdbThreads = filterThreadsForUser(rtdbThreads, auth)
                if (myRtdbThreads.isEmpty() && _threads.value.isEmpty()) return@collectLatest

                val currentMap = _threads.value.associateBy { it.id }
                val merged = myRtdbThreads.map { rtdbTh ->
                    val existing = currentMap[rtdbTh.id]
                    if (existing != null) {
                        rtdbTh.copy(
                            messages = if (existing.messages.isNotEmpty()) existing.messages else rtdbTh.messages,
                            messageCount = if (existing.messages.isNotEmpty()) existing.messages.size else rtdbTh.messageCount,
                            studentAvatarUrl = existing.studentAvatarUrl ?: rtdbTh.studentAvatarUrl,
                            teacherAvatarUrl = existing.teacherAvatarUrl ?: rtdbTh.teacherAvatarUrl,
                        )
                    } else {
                        rtdbTh
                    }
                }

                // Preserve local-only threads like AI Tutor / AI Copilot
                val rtdbIds = myRtdbThreads.map { it.id }.toSet()
                val localOnly = _threads.value.filter { it.id !in rtdbIds && (it.id.startsWith("ai-") || it.messages.isNotEmpty()) }

                val updatedList = consolidateAndDeduplicateThreads(merged + localOnly)
                _threads.value = updatedList
                saveToDisk(updatedList, auth.userId)
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
            // Strictly filter server response for current user to guarantee isolation
            val dbThreads = if (auth.isStudent) {
                rawDbThreads.filter { dto ->
                    isSameStudentIdentity(dto.studentId, dto.studentName, auth.userId, auth.name)
                }
            } else if (auth.isTeacher) {
                val myId = auth.userId.orEmpty().trim()
                val myName = auth.name.orEmpty().trim()
                val cleanMyName = cleanPersonName(myName).lowercase()
                rawDbThreads.filter { dto ->
                    val idMatches = myId.isNotBlank() && (
                        dto.teacherId?.equals(myId, ignoreCase = true) == true ||
                        dto.teacherId?.replace("-", "")?.equals(myId.replace("-", ""), ignoreCase = true) == true
                    )
                    val cleanTeacher = cleanPersonName(dto.teacherName).lowercase()
                    val nameMatches = isSamePersonName(dto.teacherName, myName) ||
                        (cleanMyName.isNotBlank() && cleanTeacher.isNotBlank() && (
                            cleanTeacher == cleanMyName ||
                            cleanTeacher.contains(cleanMyName) ||
                            cleanMyName.contains(cleanTeacher)
                        ))
                    idMatches || nameMatches
                }
            } else if (auth.isPrincipal) {
                rawDbThreads.filter { dto ->
                    val myId = auth.userId.orEmpty()
                    val myName = auth.name.orEmpty()
                    (myId.isNotBlank() && (dto.teacherId == myId || dto.studentId == myId)) ||
                    isSamePersonName(dto.teacherName, myName) ||
                    isSamePersonName(dto.studentName, myName) ||
                    dto.teacherName.contains("Kepala Sekolah", ignoreCase = true) ||
                    dto.studentName.contains("Kepala Sekolah", ignoreCase = true)
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
            val pendingLocal = filterThreadsForUser(_threads.value.filter { it.id !in dbIds }, auth)
            val merged = pendingLocal + consolidatedThreads
            val strictlyFiltered = consolidateAndDeduplicateThreads(filterThreadsForUser(merged, auth))
            _threads.value = strictlyFiltered
            saveToDisk(strictlyFiltered, auth.userId)
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
                    val cleanMsgs = rtdbMsgs.filter { it.content.isNotBlank() || !it.attachmentUrl.isNullOrBlank() }
                    val currentList = _threads.value
                    val threadExists = currentList.any { it.id == threadId }
                    if (threadExists) {
                        _threads.value = currentList.map { th ->
                            if (th.id == threadId) {
                                val lastMsg = cleanMsgs.lastOrNull()
                                val updatedStatus = if (lastMsg?.isFromTeacher == true) {
                                    InquiryStatus.ANSWERED
                                } else if (cleanMsgs.isNotEmpty()) {
                                    InquiryStatus.WAITING_REPLY
                                } else {
                                    th.status
                                }
                                th.copy(
                                    messages = cleanMsgs,
                                    status = updatedStatus,
                                    lastUpdated = lastMsg?.timestamp ?: th.lastUpdated,
                                    lastMessageContent = lastMsg?.content ?: th.lastMessageContent,
                                    lastSenderName = lastMsg?.senderName ?: th.lastSenderName,
                                    messageCount = cleanMsgs.size,
                                )
                            } else th
                        }
                    } else if (cleanMsgs.isNotEmpty()) {
                        val lastMsg = cleanMsgs.lastOrNull()
                        val newPlaceholder = ChatThread(
                            id = threadId,
                            studentId = lastMsg?.senderId.orEmpty(),
                            studentName = lastMsg?.senderName.orEmpty(),
                            studentClass = "Konsultasi",
                            teacherId = "",
                            teacherName = "",
                            subjectName = "Umum",
                            inquiryType = InquiryType.GENERAL,
                            referenceTitle = "Konsultasi Langsung",
                            referenceId = null,
                            messages = cleanMsgs,
                            status = if (lastMsg?.isFromTeacher == true) InquiryStatus.ANSWERED else InquiryStatus.WAITING_REPLY,
                            lastUpdated = lastMsg?.timestamp ?: System.currentTimeMillis(),
                            lastMessageContent = lastMsg?.content,
                            lastSenderName = lastMsg?.senderName,
                            messageCount = cleanMsgs.size,
                            readStatusLabel = "Pesan Real-time",
                        )
                        _threads.value = listOf(newPlaceholder) + currentList
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
                            val apiMessages = detail.messages
                                .filter { it.content.isNotBlank() }
                                .map { m ->
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
        attachmentUrl: String? = null,
        attachmentType: String? = null,
    ): ChatMessage {
        val auth = cachedAuth
        val currentThread = _threads.value.find { it.id == threadId }
        val now = System.currentTimeMillis()
        val clientUuid = UUID.randomUUID().toString()

        val effectiveSenderName = when {
            auth?.isStudent == true -> auth.name ?: "Siswa"
            !auth?.name.isNullOrBlank() -> auth.name!!
            senderName.isNotBlank() && !senderName.equals("Guru Pengampu", ignoreCase = true) -> senderName
            senderRole.equals("TEACHER", ignoreCase = true) || auth?.isTeacher == true -> "Guru Pengajar"
            else -> "Siswa"
        }
        val effectiveSenderId = when {
            !auth?.userId.isNullOrBlank() -> auth.userId!!
            senderId.isNotBlank() -> senderId
            else -> UUID.randomUUID().toString()
        }

        // Check whether the sender is the recorded student / initiator of this thread
        val isSenderTheInitiator = currentThread != null && (
            (currentThread.studentId.isNotBlank() && currentThread.studentId == effectiveSenderId) ||
            isSamePersonName(currentThread.studentName, effectiveSenderName)
        )

        val isFromTeacher = when {
            isSenderTheInitiator -> false
            auth?.isStudent == true -> false
            auth?.isPrincipal == true -> false
            senderRole.equals("TEACHER", ignoreCase = true) || auth?.isTeacher == true -> true
            else -> false
        }
        val effectiveRole = when {
            auth?.isPrincipal == true -> "PRINCIPAL"
            isFromTeacher -> "TEACHER"
            else -> "STUDENT"
        }

        val localMsg = ChatMessage(
            id = clientUuid,
            threadId = threadId,
            senderId = effectiveSenderId,
            senderName = effectiveSenderName,
            senderRole = effectiveRole,
            content = content.trim(),
            timestamp = now,
            isFromTeacher = isFromTeacher,
            isRead = false,
            attachmentUrl = attachmentUrl,
            attachmentType = attachmentType,
        )

        // NEVER overwrite valid teacherName with student/initiator or principal name!
        val updatedTeacherName = when {
            currentThread != null && currentThread.teacherName.isNotBlank() &&
                !currentThread.teacherName.equals("Guru Pengampu", ignoreCase = true) &&
                !currentThread.teacherName.equals("Bapak/Ibu Guru", ignoreCase = true) &&
                !isSamePersonName(currentThread.teacherName, currentThread.studentName) -> {
                currentThread.teacherName
            }
            isFromTeacher && effectiveSenderName.isNotBlank() && !effectiveSenderName.equals("Guru Pengampu", ignoreCase = true) -> {
                effectiveSenderName
            }
            else -> currentThread?.teacherName ?: ""
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

        val isFirstMessageInThread = currentThread == null || currentThread.messages.isEmpty()
        val isThreadPrincipal = currentThread?.teacherName?.contains("Kepala", ignoreCase = true) == true ||
            currentThread?.subjectName?.contains("Konsultasi Kebijakan", ignoreCase = true) == true
        val targetTeacherId = if (isThreadPrincipal) {
            currentThread?.teacherId?.takeIf { it.isNotBlank() && it.contains("-") && it.length == 36 }
                ?: "01a092fc-af59-76b2-85f2-cca8b8d8d515"
        } else {
            currentThread?.teacherId?.takeIf { it.isNotBlank() && it.contains("-") && it.length == 36 }
        }
        val targetTeacherName = if (isThreadPrincipal) {
            "Kepala Sekolah PKBM"
        } else {
            updatedTeacherName.ifBlank { currentThread?.teacherName ?: "Guru Mata Pelajaran" }
        }
        val targetSubject = if (isThreadPrincipal) {
            "Konsultasi Kebijakan, Akademik & Pengaduan Warga Belajar"
        } else {
            currentThread?.subjectName ?: "Umum"
        }
        val targetClass = currentThread?.studentClass ?: "Siswa"
        val targetTitle = if (isThreadPrincipal) {
            "Konsultasi Kebijakan, Akademik & Pengaduan Warga Belajar"
        } else {
            currentThread?.referenceTitle ?: "Konsultasi $targetSubject"
        }
        val targetRefId = currentThread?.referenceId
        val targetType = currentThread?.inquiryType ?: InquiryType.GENERAL

        // Optimistically update in-memory thread
        val found = _threads.value.any { it.id == threadId }
        val effectiveSnippet = if (content.isNotBlank()) content.trim() else if (!attachmentUrl.isNullOrBlank()) "📷 [Foto Lampiran]" else ""
        _threads.value = if (found) {
            _threads.value.map { thread ->
                if (thread.id == threadId) {
                    val alreadyHas = thread.messages.any { it.id == clientUuid }
                    val updatedMsgs = if (alreadyHas) thread.messages else thread.messages + localMsg
                    thread.copy(
                        messages = updatedMsgs,
                        status = if (isFromTeacher) InquiryStatus.ANSWERED else InquiryStatus.WAITING_REPLY,
                        lastUpdated = now,
                        teacherName = updatedTeacherName.ifBlank { thread.teacherName },
                        lastMessageContent = effectiveSnippet,
                        lastSenderName = effectiveSenderName,
                        messageCount = updatedMsgs.size,
                        readStatusLabel = if (isFromTeacher) "Dijawab Tutor Pamong" else "Menunggu Ditinjau Tutor Pamong",
                        teacherLastReadAt = if (isFromTeacher) now else 0L,
                    )
                } else {
                    thread
                }
            }
        } else {
            val fallbackThread = ChatThread(
                id = threadId,
                studentId = effectiveSenderId,
                studentName = effectiveSenderName,
                studentClass = if (auth?.isPrincipal == true) "Kepala Sekolah" else if (auth?.isTeacher == true) "Guru" else "Siswa",
                teacherId = targetTeacherId ?: "",
                teacherName = targetTeacherName,
                subjectName = targetSubject,
                inquiryType = targetType,
                referenceTitle = targetTitle,
                referenceId = targetRefId,
                messages = listOf(localMsg),
                status = if (isFromTeacher) InquiryStatus.ANSWERED else InquiryStatus.WAITING_REPLY,
                lastUpdated = now,
                lastMessageContent = effectiveSnippet,
                lastSenderName = effectiveSenderName,
                messageCount = 1,
                readStatusLabel = if (isFromTeacher) "Dijawab Tutor Pamong" else "Menunggu Ditinjau Tutor Pamong",
                teacherLastReadAt = if (isFromTeacher) now else 0L,
            )
            listOf(fallbackThread) + _threads.value
        }
        saveToDisk(_threads.value, auth?.userId)

        // Asynchronously persist to backend API so PostgreSQL stays 100% synchronized
        scope.launch {
            try {
                if (isFirstMessageInThread) {
                    val createReq = CreateInquiryRequestDto(
                        id = threadId,
                        studentId = if (effectiveSenderId.contains("-") && effectiveSenderId.length == 36) effectiveSenderId else null,
                        studentName = effectiveSenderName,
                        studentClass = targetClass,
                        teacherId = targetTeacherId,
                        teacherName = targetTeacherName,
                        subjectName = targetSubject,
                        inquiryType = targetType.name,
                        referenceTitle = targetTitle,
                        referenceId = targetRefId,
                        initialMessage = content.trim(),
                        clientMessageId = clientUuid,
                    )
                    val resp = api.createInquiry(createReq)
                    if (resp.success && resp.data != null) {
                        Timber.d("Inquiry created in PostgreSQL: ${resp.data.id}")
                    }
                } else {
                    val resp = api.sendMessage(
                        threadId = threadId,
                        request = SendInquiryMessageRequestDto(
                            clientMessageId = clientUuid,
                            senderId = effectiveSenderId,
                            senderName = effectiveSenderName,
                            senderRole = effectiveRole,
                            content = content.trim(),
                            attachmentUrl = attachmentUrl,
                            attachmentType = attachmentType,
                        )
                    )
                    if (!resp.success && (resp.error?.code == "ResourceNotFound" || resp.error?.message?.contains("not found", ignoreCase = true) == true)) {
                        // Fallback self-heal: create thread in database if missing
                        val createReq = CreateInquiryRequestDto(
                            id = threadId,
                            studentId = if (effectiveSenderId.contains("-") && effectiveSenderId.length == 36) effectiveSenderId else null,
                            studentName = effectiveSenderName,
                            studentClass = targetClass,
                            teacherId = targetTeacherId,
                            teacherName = targetTeacherName,
                            subjectName = targetSubject,
                            inquiryType = targetType.name,
                            referenceTitle = targetTitle,
                            referenceId = targetRefId,
                            initialMessage = content.trim(),
                            clientMessageId = clientUuid,
                        )
                        api.createInquiry(createReq)
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Async persist of message to server database failed")
            }
        }

        return localMsg
    }

    suspend fun uploadAttachment(bytes: ByteArray, filename: String, mimeType: String): Result<String> {
        return try {
            val reqBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", filename, reqBody)
            val resp = api.uploadAttachment(part)
            if (resp.success && resp.data != null) {
                Result.success(resp.data.url)
            } else {
                Result.failure(Exception(resp.error?.message ?: "Gagal mengunggah foto lampiran"))
            }
        } catch (e: Exception) {
            Timber.e(e, "uploadAttachment failed")
            Result.failure(e)
        }
    }

    @Volatile
    private var principalContextSummary: String? = null

    fun setPrincipalContext(summary: String) {
        principalContextSummary = summary
    }

    fun getPrincipalContext(): String? = principalContextSummary

    suspend fun askAi(
        threadId: String,
        question: String,
        subjectName: String? = null,
        gradeLevel: String? = null,
        history: List<ChatMessage> = emptyList(),
    ): Result<String> {
        return try {
            val isPrincipalMode = threadId.startsWith("ai-principal") ||
                (cachedAuth?.isPrincipal == true) ||
                com.schoolos.android.core.auth.isPrincipalRole(cachedAuth?.role ?: "")
            val isTeacherMode = (threadId.startsWith("ai-teacher") || cachedAuth?.isTeacher == true) && !isPrincipalMode

            val finalSubject = when {
                isPrincipalMode -> "Supervisi & Analitik PKBM"
                isTeacherMode -> "Pedagogik & Administrasi Guru"
                else -> (subjectName ?: "Umum")
            }
            val finalGrade = when {
                isPrincipalMode -> "KEPALA_SEKOLAH"
                isTeacherMode -> "GURU_PENGAJAR"
                else -> (gradeLevel ?: "Siswa")
            }

            val finalQuestion = when {
                isPrincipalMode && !principalContextSummary.isNullOrBlank() ->
                    "[KONTEKS DATA REAL-TIME SEKOLAH (PORTAL KEPALA SEKOLAH)]:\n$principalContextSummary\n\n[PERTANYAAN / INSTRUKSI KEPALA SEKOLAH]:\n$question"
                isTeacherMode ->
                    "[PERAN AI: ASISTEN PEDAGOGIK DAN ADMINISTRASI GURU / TUTOR PKBM]\nBantu guru/tutor dalam merancang pembelajaran Kurikulum Merdeka (Paket A/B/C), modul ajar, bank soal kuis/tugas HOTS & asesmen CBT, rubrik skor penilaian, catatan feedback koreksi tugas siswa, atau strategi pembelajaran diferensiasi dan remedial.\n\n[PERTANYAAN / INSTRUKSI GURU]:\n$question"
                else -> question
            }

            val historyDtos = history.takeLast(6).map {
                TanyaAiHistoryDto(
                    role = if (it.isFromTeacher) "assistant" else "user",
                    content = it.content,
                )
            }
            val resp = api.tanyaAi(
                TanyaAiRequestDto(
                    question = finalQuestion,
                    subjectName = finalSubject,
                    gradeLevel = finalGrade,
                    history = historyDtos,
                )
            )
            if (resp.success && resp.data != null) {
                val replyText = resp.data.reply
                val now = System.currentTimeMillis()
                val senderTitle = when {
                    isPrincipalMode -> "AI Analitik Kepala Sekolah"
                    isTeacherMode -> "AI Copilot Guru"
                    else -> "Guru AI"
                }
                val senderIdVal = when {
                    isPrincipalMode -> "ai-principal"
                    isTeacherMode -> "ai-teacher"
                    else -> "ai-tutor"
                }
                val aiMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    threadId = threadId,
                    senderId = senderIdVal,
                    senderName = senderTitle,
                    senderRole = "TEACHER",
                    content = replyText,
                    timestamp = now,
                    isFromTeacher = true,
                    isRead = true,
                )
                firebaseRtdbManager.pushMessage(
                    threadId = threadId,
                    message = aiMsg,
                    teacherId = senderIdVal,
                    teacherName = senderTitle,
                )
                _threads.value = _threads.value.map { thread ->
                    if (thread.id == threadId) {
                        val updated = thread.messages + aiMsg
                        thread.copy(
                            messages = updated,
                            status = InquiryStatus.ANSWERED,
                            lastUpdated = now,
                            lastMessageContent = replyText,
                            lastSenderName = senderTitle,
                            messageCount = updated.size,
                            readStatusLabel = if (isPrincipalMode) "Dijawab AI Analitik" else "Dijawab Guru AI",
                        )
                    } else {
                        thread
                    }
                }
                Result.success(replyText)
            } else {
                Result.failure(Exception(resp.error?.message ?: "Gagal mendapatkan respon AI"))
            }
        } catch (e: Exception) {
            Timber.e(e, "askAi failed")
            Result.failure(e)
        }
    }

    fun getOrCreateAiThread(): ChatThread {
        val auth = cachedAuth
        val isPrincipal = auth?.isPrincipal == true || com.schoolos.android.core.auth.isPrincipalRole(auth?.role ?: "")
        val isTeacher = (auth?.isTeacher == true) && !isPrincipal
        val studentId = auth?.userId ?: when {
            isPrincipal -> "principal-default"
            isTeacher -> "teacher-default"
            else -> "student-default"
        }
        val studentName = auth?.name ?: when {
            isPrincipal -> "Kepala Sekolah"
            isTeacher -> "Guru Pengampu"
            else -> "Siswa"
        }
        val studentClass = auth?.className ?: when {
            isPrincipal -> "Manajerial"
            isTeacher -> "Dewan Guru"
            else -> "Kelas"
        }
        val aiThreadId = when {
            isPrincipal -> "ai-principal-$studentId"
            isTeacher -> "ai-teacher-$studentId"
            else -> "ai-tutor-$studentId"
        }

        val existing = _threads.value.find { it.id == aiThreadId }
        if (existing != null) return existing

        val senderTitle = when {
            isPrincipal -> "AI Analitik Kepala Sekolah"
            isTeacher -> "AI Copilot Guru"
            else -> "Guru AI"
        }
        val initialContent = when {
            isPrincipal -> "Halo Bapak/Ibu Kepala Sekolah! 🏛️\n\nSaya **AI Analitik Kepala Sekolah** didukung komputasi cerdas NVIDIA AI.\n\nSaya siap menganalisis data operasional dan mutu akademik sekolah secara real-time:\n• **Keaktifan Guru**: Pengunggahan materi pembelajaran, penugasan mandiri siswa, & kuis asesmen CBT.\n• **Keterlaksanaan Pembelajaran**: Kehadiran pada sesi kelas & pemantauan jalannya rombel.\n• **Kualitas & Mutu Akademik**: Analisis ketercapaian KKTP rombel serta deteksi dini siswa butuh intervensi (Early Warning System).\n\nSilakan ajukan telaah analitik atau gunakan tombol pintasan di bawah ini!"
            isTeacher -> "Halo Bapak/Ibu Guru ${studentName.substringBefore(" ")}! 🎓\n\nSaya **AI Copilot Guru** (Asisten Pedagogik & Administrasi Guru didukung NVIDIA NIM).\n\nSaya siap membantu mempermudah tugas mengajar dan administrasi Kurikulum Merdeka PKBM:\n• 📝 **Penyusunan Modul Ajar**: Bantuan merancang Modul Ajar & RPP tematik (Paket A, B, C).\n• 🎯 **Bank Soal & Asesmen CBT**: Pembuatan butir soal PG & essay HOTS lengkap kunci jawaban & rubrik skor.\n• 💬 **Saran Koreksi Tugas**: Rekomendasi feedback evaluatif yang membangun bagi warga belajar.\n• 🧩 **Strategi Pembelajaran & Remedial**: Solusi diferensiasi belajar dan penanganan siswa yang memerlukan perhatian khusus.\n\nSilakan ketik pertanyaan Anda atau gunakan tombol pintasan cepat di bawah!"
            else -> "Halo ${studentName.substringBefore(" ")}! 👋 Saya Guru AI (Asisten Belajar Pintar didukung NVIDIA AI).\n\nAda materi, rumus, konsep, atau tugas yang ingin kamu tanyakan hari ini? Ketik pertanyaanmu atau kirimkan foto soal/materi ya!"
        }

        val initialAiMessage = ChatMessage(
            id = when {
                isPrincipal -> "ai-welcome-principal-$studentId"
                isTeacher -> "ai-welcome-teacher-$studentId"
                else -> "ai-welcome-$studentId"
            },
            threadId = aiThreadId,
            senderId = when {
                isPrincipal -> "ai-principal"
                isTeacher -> "ai-teacher"
                else -> "ai-tutor"
            },
            senderName = senderTitle,
            senderRole = "TEACHER",
            content = initialContent,
            timestamp = System.currentTimeMillis(),
            isFromTeacher = true,
            isRead = true,
        )

        val newAiThread = ChatThread(
            id = aiThreadId,
            studentId = studentId,
            studentName = studentName,
            studentClass = studentClass,
            teacherId = when {
                isPrincipal -> "ai-principal"
                isTeacher -> "ai-teacher"
                else -> "ai-tutor"
            },
            teacherName = when {
                isPrincipal -> "AI Analitik Kepala Sekolah (NVIDIA NIM)"
                isTeacher -> "AI Copilot Guru (NVIDIA NIM)"
                else -> "Guru AI (NVIDIA NIM)"
            },
            subjectName = when {
                isPrincipal -> "Supervisi & Analitik PKBM"
                isTeacher -> "Pedagogik & Modul Ajar"
                else -> "Tanya AI"
            },
            inquiryType = InquiryType.GENERAL,
            referenceTitle = when {
                isPrincipal -> "Asisten Eksekutif Supervisi & Analitik Real-Time"
                isTeacher -> "Asisten Pedagogik & Administrasi Guru"
                else -> "Asisten Belajar Pintar 24 Jam"
            },
            referenceId = null,
            messages = listOf(initialAiMessage),
            status = InquiryStatus.ANSWERED,
            lastUpdated = System.currentTimeMillis(),
            lastMessageContent = initialAiMessage.content,
            lastSenderName = senderTitle,
            messageCount = 1,
            readStatusLabel = when {
                isPrincipal -> "Siap Menganalisis Real-Time"
                isTeacher -> "Siap Membantu Mengajar & Modul"
                else -> "Siap Menjawab 24 Jam"
            },
            teacherLastReadAt = System.currentTimeMillis(),
        )

        _threads.value = listOf(newAiThread) + _threads.value
        saveToDisk(_threads.value, auth?.userId)
        firebaseRtdbManager.syncThreadMetadata(newAiThread)
        firebaseRtdbManager.pushMessage(aiThreadId, initialAiMessage)
        return newAiThread
    }

    /**
     * Get or create a direct teacher consultation thread.
     * Guaranteed valid UUID and real-time Firebase RTDB synchronization.
     */
    fun getOrCreateTeacherThread(
        teacherId: String,
        teacherName: String,
        subjectName: String,
    ): ChatThread {
        val auth = cachedAuth
        val isTargetPrincipal = teacherName.contains("Kepala", ignoreCase = true) ||
            subjectName.contains("Konsultasi Kebijakan", ignoreCase = true) ||
            subjectName.contains("Pengaduan", ignoreCase = true)

        val effectiveTeacherName = if (isTargetPrincipal) "Kepala Sekolah PKBM" else teacherName
        val effectiveSubjectName = if (isTargetPrincipal) "Konsultasi Kebijakan, Akademik & Pengaduan Warga Belajar" else subjectName
        val effectiveTeacherId = if (isTargetPrincipal && (teacherId.isBlank() || teacherId == "principal-user")) {
            "01a092fc-af59-76b2-85f2-cca8b8d8d515"
        } else {
            teacherId
        }

        val effectiveStudentName = if (!auth?.name.isNullOrBlank()) auth.name!! else "Siswa"
        val effectiveStudentId = if (!auth?.userId.isNullOrBlank()) auth.userId!! else UUID.randomUUID().toString()
        val effectiveStudentClass = if (!auth?.className.isNullOrBlank()) {
            auth.className!!
        } else if (auth?.isPrincipal == true) {
            "Kepala Sekolah"
        } else if (auth?.isTeacher == true) {
            "Guru"
        } else {
            "Siswa"
        }
        val refTitle = if (auth?.isPrincipal == true) {
            "Koordinasi $effectiveSubjectName"
        } else if (isTargetPrincipal) {
            "Konsultasi Kebijakan, Akademik & Pengaduan Warga Belajar"
        } else {
            "Konsultasi $effectiveSubjectName"
        }

        // Check if an active thread already exists for this teacher
        val existing = _threads.value.firstOrNull { t ->
            val matchesTeacher = if (isTargetPrincipal) {
                t.teacherName.contains("Kepala", ignoreCase = true) ||
                t.subjectName.contains("Konsultasi Kebijakan", ignoreCase = true) ||
                (effectiveTeacherId.isNotBlank() && t.teacherId.equals(effectiveTeacherId, ignoreCase = true))
            } else {
                !t.teacherName.contains("Kepala", ignoreCase = true) &&
                !t.subjectName.contains("Konsultasi Kebijakan", ignoreCase = true) &&
                ((t.teacherId.isNotBlank() && t.teacherId.equals(effectiveTeacherId, ignoreCase = true)) ||
                 (effectiveTeacherName.isNotBlank() && isSamePersonName(t.teacherName, effectiveTeacherName)))
            }
            val matchesStudent = auth == null || isSameStudentIdentity(t.studentId, t.studentName, auth.userId, auth.name)
            matchesTeacher && matchesStudent
        }

        if (existing != null) {
            return existing
        }

        val newThreadId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val newThread = ChatThread(
            id = newThreadId,
            studentId = effectiveStudentId,
            studentName = effectiveStudentName,
            studentClass = effectiveStudentClass,
            teacherId = effectiveTeacherId,
            teacherName = effectiveTeacherName,
            subjectName = effectiveSubjectName,
            inquiryType = InquiryType.GENERAL,
            referenceTitle = refTitle,
            referenceId = null,
            messages = emptyList(),
            status = InquiryStatus.WAITING_REPLY,
            lastUpdated = now,
            lastMessageContent = null,
            lastSenderName = null,
            messageCount = 0,
            readStatusLabel = "Mulai Diskusi",
            teacherLastReadAt = 0L,
        )

        _threads.value = listOf(newThread) + _threads.value
        saveToDisk(_threads.value, auth?.userId)

        // Sync metadata to Firebase RTDB so the node exists immediately
        firebaseRtdbManager.syncThreadMetadata(newThread)

        return newThread
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

