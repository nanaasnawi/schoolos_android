package com.schoolos.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.designsystem.LocalIsDarkTheme
import com.schoolos.android.core.designsystem.LocalThemeToggle
import com.schoolos.android.core.designsystem.SchoolOsTheme
import androidx.lifecycle.lifecycleScope
import com.schoolos.android.core.network.MaintenanceManager
import com.schoolos.android.core.notification.SystemNotificationHelper
import com.schoolos.android.feature.notifications.NotificationSyncManager
import com.schoolos.android.navigation.NavGraph
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var notificationSyncManager: NotificationSyncManager
    @Inject lateinit var maintenanceManager: MaintenanceManager
    @Inject lateinit var chatManager: com.schoolos.android.core.chat.ChatManager

    private var pendingNavigationRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractNavigationTarget(intent)
        enableEdgeToEdge()

        // Create Android Notification Channel
        SystemNotificationHelper.createNotificationChannel(this)

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Start background notification & maintenance sync listener
        notificationSyncManager.start()

        // Dismiss any old persistent notification & delete service channel
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            nm?.cancel(8801)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm?.deleteNotificationChannel("school_os_service_channel")
            }
        } catch (_: Exception) {}

        // Subscribe SEMUA topik belajar (bukan cuma pengumuman) agar materi/tugas/
        // kuis/nilai/sesi ikut membangunkan HP saat idle via FCM data-message.
        try {
            com.schoolos.android.notification.SchoolOsFirebaseMessagingService.subscribeAllTopics()
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        timber.log.Timber.d("FCM Token: %s", token)
                        getSharedPreferences("schoolos_fcm", Context.MODE_PRIVATE)
                            .edit()
                            .putString("fcm_token", token)
                            .apply()
                        com.schoolos.android.notification.SchoolOsFirebaseMessagingService.subscribeAllTopics()
                    }
                }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Firebase initialization error")
        }

        // Fallback polling (AlarmManager allow-while-idle) untuk Doze / HP idle lama.
        try {
            com.schoolos.android.notification.NotificationPollReceiver.schedule(this)
        } catch (_: Exception) {}


        // Initial check for system maintenance mode
        lifecycleScope.launch {
            maintenanceManager.checkServerStatus()
        }

        // Load saved theme preference (default = LIGHT)
        val prefs = getSharedPreferences("school_os_prefs", Context.MODE_PRIVATE)

        setContent {
            var isDarkTheme by remember {
                mutableStateOf(prefs.getBoolean("dark_theme", false))
            }

            val toggleTheme: () -> Unit = {
                isDarkTheme = !isDarkTheme
                prefs.edit().putBoolean("dark_theme", isDarkTheme).apply()
            }

            CompositionLocalProvider(
                LocalThemeToggle provides toggleTheme,
                LocalIsDarkTheme  provides isDarkTheme,
            ) {
                SchoolOsTheme(darkTheme = isDarkTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        NavGraph(
                            authManager = authManager,
                            maintenanceManager = maintenanceManager,
                            chatManager = chatManager,
                            pendingDeepLink = pendingNavigationRoute,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNavigationTarget(intent)
    }

    private fun extractNavigationTarget(intent: android.content.Intent?) {
        val target = intent?.getStringExtra("navigate_to")
            ?: intent?.extras?.getString("navigate_to")
        if (!target.isNullOrBlank()) {
            pendingNavigationRoute = target
        }
    }
}
