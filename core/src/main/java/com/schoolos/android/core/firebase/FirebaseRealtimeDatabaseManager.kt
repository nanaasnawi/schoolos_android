package com.schoolos.android.core.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.schoolos.android.core.chat.ChatMessage
import com.schoolos.android.core.chat.ChatThread
import com.schoolos.android.core.chat.InquiryStatus
import com.schoolos.android.core.chat.InquiryType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRealtimeDatabaseManager @Inject constructor() {

    companion object {
        const val DATABASE_URL = "https://akselerasi-edu-default-rtdb.asia-southeast1.firebasedatabase.app/"
    }

    private val database: FirebaseDatabase by lazy {
        try {
            val instance = FirebaseDatabase.getInstance(DATABASE_URL)
            try {
                instance.setPersistenceEnabled(true)
            } catch (e: Exception) {
                // Ignore if persistence was already enabled
                Timber.d("Firebase RTDB persistence already enabled or skipped: ${e.message}")
            }
            instance
        } catch (e: Exception) {
            Timber.e(e, "Error initializing FirebaseRealtimeDatabase instance")
            FirebaseDatabase.getInstance()
        }
    }

    private val rootRef: DatabaseReference by lazy { database.reference }

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        // Monitor connection status to Firebase RTDB server
        rootRef.child(".info/connected").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                _isConnected.value = connected
                Timber.d("Firebase Realtime Database connected: %b", connected)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("Firebase RTDB connection status listener cancelled: %s", error.message)
            }
        })
    }

    /**
     * Push a message to a thread in Realtime Database.
     */
    fun pushMessage(
        threadId: String,
        message: ChatMessage,
        studentId: String? = null,
        studentName: String? = null,
        teacherId: String? = null,
        teacherName: String? = null,
    ) {
        try {
            val threadRef = rootRef.child("inquiries").child(threadId)
            val msgRef = threadRef.child("messages").child(message.id)

            val msgMap = mutableMapOf<String, Any>(
                "id" to message.id,
                "threadId" to threadId,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "senderRole" to message.senderRole,
                "content" to message.content,
                "timestamp" to message.timestamp,
                "isFromTeacher" to message.isFromTeacher,
                "isRead" to message.isRead,
            )

            msgRef.setValue(msgMap)

            // Update thread last message summary
            val summaryMap = mutableMapOf<String, Any>(
                "lastMessageContent" to message.content,
                "lastMessageAt" to message.timestamp,
                "status" to if (message.isFromTeacher) "ANSWERED" else "WAITING_REPLY",
                "lastSenderName" to message.senderName,
                "lastSenderId" to message.senderId,
                "lastSenderRole" to message.senderRole,
                "readStatusLabel" to if (message.isFromTeacher) "✓ Dijawab Guru" else "Menunggu Dibaca Guru",
                "teacherLastReadAt" to if (message.isFromTeacher) message.timestamp else 0L,
            )
            if (!studentId.isNullOrBlank()) summaryMap["studentId"] = studentId
            if (!studentName.isNullOrBlank()) summaryMap["studentName"] = studentName
            if (!teacherId.isNullOrBlank()) summaryMap["teacherId"] = teacherId
            if (!teacherName.isNullOrBlank()) summaryMap["teacherName"] = teacherName

            threadRef.child("summary").updateChildren(summaryMap)

            // If teacher replied, ensure metadata also holds the correct teacher name & ID
            if (message.isFromTeacher && message.senderName.isNotBlank() &&
                !message.senderName.equals("Bapak/Ibu Guru", ignoreCase = true) &&
                !message.senderName.equals("Guru Pengampu", ignoreCase = true)
            ) {
                threadRef.child("metadata").updateChildren(
                    mapOf(
                        "teacherName" to message.senderName,
                        "teacherId" to message.senderId,
                        "lastUpdated" to message.timestamp,
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to push message to Firebase RTDB for thread $threadId")
        }
    }

    /**
     * Mark thread as read by teacher in Firebase RTDB in real time.
     */
    fun markThreadReadByTeacher(threadId: String) {
        try {
            val now = System.currentTimeMillis()
            val summaryRef = rootRef.child("inquiries").child(threadId).child("summary")
            summaryRef.updateChildren(
                mapOf(
                    "teacherLastReadAt" to now,
                    "readStatusLabel" to "✓ Dibaca Guru"
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark thread read by teacher in RTDB: $threadId")
        }
    }

    /**
     * Set user online presence in Firebase RTDB (global and thread-scoped).
     */
    fun setUserPresence(userId: String, userName: String, role: String, isOnline: Boolean) {
        if (userId.isBlank()) return
        try {
            val presenceRef = rootRef.child("inquiries").child("presence").child(userId)
            if (isOnline) {
                val onlineData = mapOf(
                    "userId" to userId,
                    "name" to userName,
                    "role" to role,
                    "status" to "online",
                    "lastSeen" to com.google.firebase.database.ServerValue.TIMESTAMP,
                )
                val offlineData = mapOf(
                    "userId" to userId,
                    "name" to userName,
                    "role" to role,
                    "status" to "offline",
                    "lastSeen" to com.google.firebase.database.ServerValue.TIMESTAMP,
                )
                presenceRef.onDisconnect().setValue(offlineData)
                presenceRef.setValue(onlineData)
            } else {
                presenceRef.updateChildren(
                    mapOf(
                        "status" to "offline",
                        "lastSeen" to com.google.firebase.database.ServerValue.TIMESTAMP,
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set user presence for $userId")
        }
    }

    /**
     * Set presence for a specific inquiry thread.
     */
    fun setThreadPresence(threadId: String, role: String, userId: String, userName: String, isOnline: Boolean) {
        if (threadId.isBlank()) return
        try {
            val roleKey = role.lowercase()
            val threadPresenceRef = rootRef.child("inquiries").child(threadId).child("presence").child(roleKey)
            if (isOnline) {
                val onlineData = mapOf(
                    "userId" to userId,
                    "name" to userName,
                    "status" to "online",
                    "lastSeen" to com.google.firebase.database.ServerValue.TIMESTAMP,
                )
                val offlineData = mapOf(
                    "userId" to userId,
                    "name" to userName,
                    "status" to "offline",
                    "lastSeen" to com.google.firebase.database.ServerValue.TIMESTAMP,
                )
                threadPresenceRef.onDisconnect().setValue(offlineData)
                threadPresenceRef.setValue(onlineData)
            } else {
                threadPresenceRef.updateChildren(
                    mapOf(
                        "status" to "offline",
                        "lastSeen" to com.google.firebase.database.ServerValue.TIMESTAMP,
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set thread presence for thread $threadId")
        }
    }

    /**
     * Observe online presence for a specific thread participant in real time.
     */
    fun observeThreadPresence(threadId: String, targetRole: String, targetUserId: String? = null): Flow<Boolean> = callbackFlow {
        val roleKey = targetRole.lowercase()
        val threadPresenceRef = rootRef.child("inquiries").child(threadId).child("presence").child(roleKey)
        val userPresenceRef = if (!targetUserId.isNullOrBlank()) {
            rootRef.child("inquiries").child("presence").child(targetUserId)
        } else null

        var isThreadPresenceOnline = false
        var isUserPresenceOnline = false

        fun evaluate() {
            trySend(isThreadPresenceOnline || isUserPresenceOnline)
        }

        val threadListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.child("status").getValue(String::class.java)
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                val isFresh = (System.currentTimeMillis() - lastSeen) < 180_000L // 3 minutes timeout
                isThreadPresenceOnline = status.equals("online", ignoreCase = true) && isFresh
                evaluate()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        threadPresenceRef.addValueEventListener(threadListener)

        val userListener = if (userPresenceRef != null) {
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.child("status").getValue(String::class.java)
                    val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                    val isFresh = (System.currentTimeMillis() - lastSeen) < 180_000L
                    isUserPresenceOnline = status.equals("online", ignoreCase = true) && isFresh
                    evaluate()
                }
                override fun onCancelled(error: DatabaseError) {}
            }.also { userPresenceRef.addValueEventListener(it) }
        } else null

        awaitClose {
            threadPresenceRef.removeEventListener(threadListener)
            if (userPresenceRef != null && userListener != null) {
                userPresenceRef.removeEventListener(userListener)
            }
        }
    }

    /**
     * Sync thread metadata to Firebase RTDB.
     */
    fun syncThreadMetadata(thread: ChatThread) {
        try {
            val threadRef = rootRef.child("inquiries").child(thread.id).child("metadata")
            val metaMap = mapOf(
                "id" to thread.id,
                "studentId" to thread.studentId,
                "studentName" to thread.studentName,
                "studentClass" to thread.studentClass,
                "teacherId" to thread.teacherId,
                "teacherName" to thread.teacherName,
                "subjectName" to thread.subjectName,
                "inquiryType" to thread.inquiryType.name,
                "referenceTitle" to thread.referenceTitle,
                "referenceId" to (thread.referenceId ?: ""),
                "status" to thread.status.name,
                "lastUpdated" to thread.lastUpdated,
            )
            threadRef.setValue(metaMap)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync thread metadata to Firebase RTDB: ${thread.id}")
        }
    }

    /**
     * Observe real-time messages for a specific inquiry thread.
     */
    fun observeThreadMessages(threadId: String): Flow<List<ChatMessage>> = callbackFlow {
        val messagesRef = rootRef.child("inquiries").child(threadId).child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messagesList = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    try {
                        val id = child.child("id").getValue(String::class.java) ?: child.key ?: UUID.randomUUID().toString()
                        val senderId = child.child("senderId").getValue(String::class.java) ?: ""
                        val senderName = child.child("senderName").getValue(String::class.java) ?: ""
                        val senderRole = child.child("senderRole").getValue(String::class.java) ?: "STUDENT"
                        val content = child.child("content").getValue(String::class.java) ?: ""
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val isFromTeacher = child.child("isFromTeacher").getValue(Boolean::class.java)
                            ?: senderRole.equals("TEACHER", ignoreCase = true)
                        val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                        val readAt = child.child("readAt").getValue(Long::class.java)

                        messagesList.add(
                            ChatMessage(
                                id = id,
                                threadId = threadId,
                                senderId = senderId,
                                senderName = senderName,
                                senderRole = senderRole,
                                content = content,
                                timestamp = timestamp,
                                isFromTeacher = isFromTeacher,
                                isRead = isRead,
                                readAt = readAt,
                            )
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse RTDB message node")
                    }
                }
                // Deduplicate messages by ID or identical sender, content and close timestamp (< 8s)
                val deduplicated = mutableListOf<ChatMessage>()
                for (msg in messagesList.sortedBy { it.timestamp }) {
                    val isDuplicate = deduplicated.any { existing ->
                        existing.id == msg.id || (
                            existing.senderRole == msg.senderRole &&
                            existing.content.trim() == msg.content.trim() &&
                            Math.abs(existing.timestamp - msg.timestamp) < 8000L
                        )
                    }
                    if (!isDuplicate) {
                        deduplicated.add(msg)
                    }
                }
                trySend(deduplicated)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("Firebase RTDB observeThreadMessages cancelled: ${error.message}")
            }
        }

        messagesRef.addValueEventListener(listener)
        awaitClose {
            messagesRef.removeEventListener(listener)
        }
    }

    /**
     * Observe real-time thread summaries from Firebase RTDB.
     */
    fun observeAllInquiries(): Flow<Map<String, Map<String, Any>>> = callbackFlow {
        val inquiriesRef = rootRef.child("inquiries")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val resultMap = mutableMapOf<String, Map<String, Any>>()
                for (threadSnap in snapshot.children) {
                    val threadId = threadSnap.key ?: continue
                    val metaSnap = threadSnap.child("metadata")
                    val summarySnap = threadSnap.child("summary")

                    val itemMap = mutableMapOf<String, Any>()
                    itemMap["id"] = threadId

                    metaSnap.children.forEach { c ->
                        c.value?.let { v -> itemMap[c.key ?: ""] = v }
                    }
                    summarySnap.children.forEach { c ->
                        c.value?.let { v -> itemMap[c.key ?: ""] = v }
                    }

                    resultMap[threadId] = itemMap
                }
                trySend(resultMap)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("Firebase RTDB observeAllInquiries cancelled: ${error.message}")
            }
        }

        inquiriesRef.addValueEventListener(listener)
        awaitClose {
            inquiriesRef.removeEventListener(listener)
        }
    }

    /**
     * Observe real-time system maintenance status node.
     */
    fun observeSystemMaintenanceStatus(): Flow<Boolean> = callbackFlow {
        val maintenanceRef = rootRef.child("system_status").child("is_maintenance")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isMaintenance = snapshot.getValue(Boolean::class.java) ?: false
                trySend(isMaintenance)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("Firebase RTDB observeSystemMaintenanceStatus cancelled: ${error.message}")
            }
        }

        maintenanceRef.addValueEventListener(listener)
        awaitClose {
            maintenanceRef.removeEventListener(listener)
        }
    }
}
