package info.kronk.app.push

import android.content.Context
import android.util.Log
import org.unifiedpush.android.connector.UnifiedPush

// Push transport registrar.
//
// Auto-detect at boot:
//   1. Look for an installed UnifiedPush distributor
//      (ntfy, NextPush, Molly-FCM, or any other implementing the
//      spec). If one is present, register with it and let the
//      distributor handle push delivery. No Google dependency.
//   2. If no distributor is installed, fall through to FCM via the
//      raw `com.google.iid.TOKEN_REQUEST` broadcast to Google
//      Services Framework — same technique the legacy :mastodon
//      module uses, no Firebase SDK, ~zero APK bloat. (Wired in
//      the next commit.)
//
// The registrar is idempotent — safe to call on every app launch;
// UnifiedPush.saveDistributor + registerApp are no-ops if the
// distributor selection hasn't changed since last boot.
//
// Endpoints + push tokens land in KronkMessagingReceiver's
// onNewEndpoint (UnifiedPush) or the FCM broadcast receiver's
// onReceive (fallback). Both route into the shared PushTokenStore
// which the Kronk /api/v1/push/subscriptions registration reads
// (wired in commit #4 of the push series).

object PushRegistrar {

    private const val TAG = "PushRegistrar"
    // UnifiedPush supports multiple "instances" — one per account,
    // per feature, etc. For now we register a single instance keyed
    // to the whole app; when multi-account lands (Path B Week 3)
    // each account gets its own instance keyed by account id.
    const val INSTANCE_DEFAULT: String = "kronk"

    fun ensureRegistered(context: Context) {
        val distributors = runCatching { UnifiedPush.getDistributors(context) }
            .getOrDefault(emptyList())
        if (distributors.isEmpty()) {
            Log.i(TAG, "No UnifiedPush distributor installed; FCM fallback pending (push commit #2).")
            // TODO(push #2): fire FCM-via-GSF `com.google.iid.TOKEN_REQUEST` broadcast.
            return
        }
        val already = runCatching { UnifiedPush.getSavedDistributor(context) }
            .getOrNull()
        if (already.isNullOrEmpty()) {
            // Pick the first available distributor. If the user has
            // more than one installed we'll ship a picker screen in a
            // follow-up (rare case — most users have zero or one).
            val chosen = distributors.first()
            UnifiedPush.saveDistributor(context, chosen)
            Log.i(TAG, "Selected UnifiedPush distributor: $chosen")
        }
        UnifiedPush.registerApp(
            context = context,
            instance = INSTANCE_DEFAULT,
            features = arrayListOf(UnifiedPush.FEATURE_BYTES_MESSAGE),
            messageForDistributor = "Kronk nudges + activity",
        )
    }
}
