package com.schoolos.android.core.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import com.schoolos.android.core.R
import timber.log.Timber

/**
 * Dedicated sound player for School OS notifications.
 *
 * Solves the OEM/Android foreground suppression bug where the system silences
 * notification tones while the application is active in foreground, as well as
 * providing guaranteed playback for the user's custom notification.mp3 sound.
 */
object NotificationSoundPlayer {

    private var lastPlayedTimestamp: Long = 0L
    private const val DEBOUNCE_INTERVAL_MS = 1500L

    fun playSound(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastPlayedTimestamp < DEBOUNCE_INTERVAL_MS) {
            return
        }

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val ringerMode = audioManager.ringerMode
                if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                    // Respect user's silent/vibrate device profile
                    return
                }
            }

            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.notification)?.apply {
                setAudioAttributes(attributes)
                setOnCompletionListener { mp ->
                    try {
                        mp.stop()
                        mp.release()
                    } catch (e: Exception) {
                        Timber.w(e, "Error releasing notification media player")
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    Timber.w("Notification sound playback error: what=$what, extra=$extra")
                    try {
                        mp.release()
                    } catch (_: Exception) {}
                    true
                }
                start()
            }

            if (mediaPlayer != null) {
                lastPlayedTimestamp = now
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to play custom notification sound")
        }
    }
}
