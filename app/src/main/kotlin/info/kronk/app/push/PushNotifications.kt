package info.kronk.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import info.kronk.app.MainActivity
import info.kronk.core.common.KronkHost
import org.json.JSONObject

// Turns a decrypted Web Push payload into a system-tray
// notification. The payload shape is Mastodon's standard notification
// JSON (title, body, notification_id, notification_type, icon, url).
// See MastodonAndroid PushNotificationReceiver for reference —
// `body` isn't always present, and the icon URL is the actor's
// avatar which we don't preload here (loading in background could
// delay the notification appearing; Android renders icon-less
// notifications fine).
//
// Tap intent: open MainActivity with a kronk.info deep-link matching
// the notification's `url` (e.g. `/@user/12345`) — MainActivity's
// existing intent dispatcher lands the user on the right pillar and
// loads the URL in the tab's WebView.

object PushNotifications {

    private const val TAG = "PushNotifications"
    private const val CHANNEL_ID = "kronk_nudges"
    private const val CHANNEL_NAME = "Nudges"
    private const val CHANNEL_DESC = "Activity from your Mates, Krews, and Korners."

    fun display(context: Context, decrypted: ByteArray) {
        ensureChannel(context)
        val payload = runCatching { JSONObject(decrypted.toString(Charsets.UTF_8)) }.getOrNull()
        if (payload == null) {
            Log.w(TAG, "display: payload wasn't valid JSON")
            return
        }
        val title = payload.optString("title", "Kronk")
        val body = payload.optString("body", "")
        val notifId = payload.optLong("notification_id", System.currentTimeMillis())
        val urlPath = payload.optString("preferred_locale", null)
            ?: payload.optString("url", "/nudges")
        val deepLink = if (urlPath.startsWith("http")) urlPath else KronkHost.origin + urlPath

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(deepLink)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(info.kronk.core.designsystem.R.drawable.ic_korner_raven)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notifId.toInt(), notification)
        }.onFailure {
            // Android 13+ requires POST_NOTIFICATIONS runtime perm;
            // if we don't have it yet the display silently drops.
            // Follow-up: prompt for the perm from the shell's
            // permission launcher.
            Log.w(TAG, "notify: refused (POST_NOTIFICATIONS not granted?)", it)
        }
    }

    private fun ensureChannel(context: Context) {
        val mgr = context.getSystemService<NotificationManager>() ?: return
        val existing = mgr.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = CHANNEL_DESC
            enableLights(true)
            enableVibration(true)
        }
        mgr.createNotificationChannel(channel)
    }
}
