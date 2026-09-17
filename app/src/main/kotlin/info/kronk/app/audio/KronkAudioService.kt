package info.kronk.app.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import info.kronk.app.MainActivity

// Foreground service that keeps the app process alive while the
// WebView is playing media (Booth DJ sets, Moments voice notes,
// Huddle live audio, Cinema playback). Without this, Android's
// process-scheduler kills the app on backgrounding and the audio
// stops mid-note.
//
// The service is a **presence sentinel**, not an audio pipeline —
// the WebView continues to own the actual audio decoding + output.
// Chromium's `navigator.mediaSession` bridging surfaces album-art
// lock-screen controls automatically when Kronk's SPA sets media
// session metadata (Booth already does this per kronk_frame + the
// audio player component).
//
// Started via `KronkAudioService.start(context)` when the JS bridge
// (KronkJsBridge.onMediaPlay) reports a play; stopped when the
// bridge reports a pause on the last playing element.

class KronkAudioService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, fgType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        ensureChannel(this)
        val openApp = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(info.kronk.core.designsystem.R.drawable.ic_korner_headphones)
            .setContentTitle("Playing on Kronk")
            .setContentText("Tap to open")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 42_101
        private const val CHANNEL_ID = "kronk_audio"
        private const val CHANNEL_NAME = "Playback"

        fun start(context: Context) {
            val i = Intent(context, KronkAudioService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KronkAudioService::class.java))
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val mgr = context.getSystemService<NotificationManager>() ?: return
            if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Kronk is playing audio in the background."
                    setShowBadge(false)
                },
            )
        }
    }
}
