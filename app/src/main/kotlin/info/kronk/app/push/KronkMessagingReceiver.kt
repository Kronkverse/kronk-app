package info.kronk.app.push

import android.content.Context
import android.util.Log
import org.unifiedpush.android.connector.MessagingReceiver

// UnifiedPush MessagingReceiver — the entry point for every push
// event the chosen distributor sends us.
//
// This commit is scaffold only:
//   - onNewEndpoint logs the endpoint URL; commit #4 registers it
//     with Kronk via POST /api/v1/push/subscriptions.
//   - onMessage logs the payload; commit #3 wires the Web Push
//     AES-GCM decrypt + commit #5 posts a system notification.
//   - onRegistrationFailed / onUnregistered log for now; future
//     UX can retry or fall through to FCM.
//
// The receiver is registered in AndroidManifest.xml so the OS can
// broadcast to it even when the app process is dead — that's the
// whole point of push.

class KronkMessagingReceiver : MessagingReceiver() {

    override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
        Log.i(TAG, "onNewEndpoint instance=$instance endpoint=$endpoint")
        PushTokenStore.saveEndpoint(context, endpoint, PushTokenStore.Transport.UNIFIED_PUSH)
        // TODO(push #4): POST /api/v1/push/subscriptions with OAuth
        // token so Kronk starts sending pushes to this endpoint.
    }

    override fun onRegistrationFailed(context: Context, instance: String) {
        Log.w(TAG, "onRegistrationFailed instance=$instance")
        // TODO(push #2): retry via FCM-via-GSF broadcast.
    }

    override fun onUnregistered(context: Context, instance: String) {
        Log.i(TAG, "onUnregistered instance=$instance")
        // TODO(push #4): DELETE /api/v1/push/subscription on Kronk.
    }

    override fun onMessage(context: Context, message: ByteArray, instance: String) {
        Log.i(TAG, "onMessage instance=$instance payload=${message.size}B")
        // TODO(push #3): AES-GCM decrypt via ECDH keys stored at
        // subscription time.
        // TODO(push #5): post as system notification with deep-link.
    }

    private companion object {
        const val TAG = "KronkPush"
    }
}
