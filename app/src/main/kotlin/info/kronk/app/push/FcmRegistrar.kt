package info.kronk.app.push

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

// FCM-via-GSF fallback: broadcasts a TOKEN_REQUEST to Google Services
// Framework and receives an FCM token back via the paired
// `FcmRegistrationReceiver`. No Firebase SDK, no
// `com.google.gms:google-services` plugin, no `google-services.json`
// — same technique the shipping :mastodon module uses (see
// `PushSubscriptionManager.java:100`), just Kotlin.
//
// The FCM sender ID is Kronk's — copied verbatim from the shipping
// app so nudges the Rails backend already emits (encrypted via
// Web Push with the Kronk VAPID keys + this sender) reach us.
//
// Called from PushRegistrar when UnifiedPush finds no distributor.
// If Google Services Framework isn't installed either (rare — no
// Play Services on device), the broadcast is dropped by the OS and
// we're stuck with no push. UX for that case comes later.

object FcmRegistrar {

    // Kronk's Google Cloud Messaging / FCM project sender ID —
    // matches PushSubscriptionManager.java:55 in the shipping app so
    // pushes emitted by Rails reach both apps interchangeably.
    private const val FCM_SENDER_ID = "449535203550"
    private const val GSF_PACKAGE = "com.google.android.gms"
    private const val KID_VALUE = "|ID|1|"
    private const val EXTRA_APPLICATION_PENDING_INTENT = "app"
    private const val EXTRA_SENDER = "sender"
    private const val EXTRA_SUBTYPE = "subtype"
    private const val EXTRA_SCOPE = "scope"

    fun requestToken(context: Context) {
        Log.i(TAG, "Requesting FCM token via GSF broadcast")
        val intent = Intent("com.google.iid.TOKEN_REQUEST").apply {
            setPackage(GSF_PACKAGE)
            putExtra(
                EXTRA_APPLICATION_PENDING_INTENT,
                PendingIntent.getBroadcast(
                    context, 0, Intent(),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            putExtra(EXTRA_SENDER, FCM_SENDER_ID)
            putExtra(EXTRA_SUBTYPE, FCM_SENDER_ID)
            putExtra(EXTRA_SCOPE, "*")
            putExtra("kid", KID_VALUE)
        }
        context.sendBroadcast(intent)
    }

    private const val TAG = "FcmRegistrar"

    // Receives the response broadcast from GSF containing the token.
    // Manifest-registered so the OS can fire it even when the process
    // is dead. Kronk's `/api/v1/push/subscriptions` registration
    // (push #4) reads the saved endpoint out of PushTokenStore.
    class RegistrationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != "com.google.android.c2dm.intent.REGISTRATION") return
            val raw = intent.getStringExtra("registration_id")
            if (raw.isNullOrEmpty()) {
                Log.w(TAG, "FCM registration reply missing registration_id: $intent")
                return
            }
            val token = if (raw.startsWith(KID_VALUE)) raw.substring(KID_VALUE.length + 1) else raw
            // Web Push–compatible FCM endpoint format: the same
            // shape Chrome + Firefox produce for their Web Push
            // subscriptions, so Kronk's Rails backend can POST to
            // it via the standard webpush gem code path.
            val endpoint = "https://fcm.googleapis.com/fcm/send/$token"
            PushTokenStore.saveEndpoint(context, endpoint, PushTokenStore.Transport.FCM_VIA_GSF)
            Log.i(TAG, "FCM registered; endpoint=$endpoint")
            // TODO(push #4): trigger Kronk /api/v1/push/subscriptions
            //   registration with the newly-saved endpoint.
        }
    }

    // Receives actual push payloads over FCM. Manifest-registered.
    // Payload arrives base85-encoded in the `p` extra per the
    // shipping :mastodon path; decode, then RFC 8291 decrypt.
    class MessageReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != "com.google.android.c2dm.intent.RECEIVE") return
            val extras = intent.extras ?: return
            // Mastodon's Rails backend delivers the RFC 8188 aes128gcm
            // payload as the `p` extra (base64url, RFC 8291 §5).
            // Copied convention from :mastodon PushNotificationReceiver.
            val bodyStr = extras.getString("p") ?: run {
                Log.w(TAG, "FCM push: no `p` payload extra")
                return
            }
            val body = android.util.Base64.decode(
                bodyStr,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP,
            )
            runCatching {
                val keys = PushCrypto.ensureKeys(context)
                val decrypted = PushCrypto.decrypt(keys, body)
                PushNotifications.display(context, decrypted)
            }.onFailure { Log.e(TAG, "FCM push decrypt/display failed", it) }
        }
    }
}
