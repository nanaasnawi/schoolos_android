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
    fun pushMessage(threadId: String, message: ChatMessage) {
        try {
            val threadRef = rootRef.child("inquiries").child(threadId)
            val msgRef = threadRef.child("messages").child(message.id)

            val msgMap = mapOf(
                "id" to message.id,
                "threadId" to threadId,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "senderRole" to message.senderRole,
                "content" to message.content,
                "timestamp" to message.timestamp,
                "isFromTeacher" to message.isFromTeacher,
            )

            msgRef.setValue(msgMap)

            // Update thread last message summary
            val summaryMap = mapOf(
                "lastMessageContent" to message.content,
                "lastMessageAt" to message.timestamp,
                "status" to if (message.isFromTeacher) "ANSWERED" else "WAITING_REPLY",
                "lastSenderName" to message.senderName,
            )
            threadRef.child("summary").updateChildren(summaryMap)
        } catch (e: Exception) {
            Timber.e(e, "Failed to push message to Firebase RTDB for thread $threadId")
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
                            )
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse RTDB message node")
                    }
                }
                messagesList.sortBy { it.timestamp }
                trySend(messagesList)
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
