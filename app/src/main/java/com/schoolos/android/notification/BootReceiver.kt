package com.schoolos.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Setelah reboot/update aplikasi: subscribe ulang topik FCM + jadwalkan
 * polling fallback agar notif tetap masuk walau HP lama idle.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Timber.d("BootReceiver: %s", action)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val um = context.getSystemService(android.os.UserManager::class.java)
            if (um != null && !um.isUserUnlocked) {
                Timber.w("Device locked during boot; postponing notification initialization")
                return
            }
        }

        try {
            val entry = dagger.hilt.android.EntryPointAccessors.fromApplication(
                context.applicationContext,
                com.schoolos.android.SchoolOsApp.AuthEntryPoint::class.java
            )
            val authManager = entry.authManager()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val auth = authManager.authState.first()
                if (auth.isLoggedIn && !auth.tenantId.isNullOrBlank()) {
                    SchoolOsFirebaseMessagingService.syncUserTopics(
                        tenantId = auth.tenantId,
                        userId = auth.userId,
                        classId = auth.classId,
                        role = auth.role,
                        className = auth.className,
                        context = context.applicationContext,
                    )
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "BootReceiver syncUserTopics failed")
        }
    }
}
