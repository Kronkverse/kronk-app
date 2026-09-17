package info.kronk.app.push

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow

// Tracks whether the WebView is currently signed in based on the
// access-token extraction on every onPageFinished. Serves two
// purposes:
//
// 1. Fires the push subscription registration when a new token
//    first appears (sign-in), and unregisters + clears crypto keys
//    when the token goes missing (sign-out).
// 2. Exposes a `signedIn` StateFlow so ShellHost can decide when to
//    prompt for POST_NOTIFICATIONS (Android 13+ runtime perm —
//    without it, push subscriptions register successfully but
//    nothing shows in the tray).

object SessionTracker {

    private const val TAG = "SessionTracker"
    val signedIn: MutableStateFlow<Boolean> = MutableStateFlow(false)

    // Last token we saw. Kept in memory only — persistence isn't
    // required here since the WebView cookie jar holds the actual
    // session. This is a change-detector.
    @Volatile
    private var lastToken: String? = null

    fun onTokenSeen(context: Context, token: String?) {
        val previous = lastToken
        lastToken = token
        signedIn.value = token != null
        when {
            token != null && previous == null -> {
                Log.i(TAG, "Sign-in detected — registering push")
                PushRegistration.tryRegister(context, token)
            }
            token != null && previous != null && token != previous -> {
                // Account switch. Kronk supports multiple sessions
                // per browser via `session[:authed_accounts]`;
                // the WebView cookie for the new one has arrived.
                // Cheapest correct thing: re-register push against
                // the new token so nudges route to the newly-active
                // account.
                Log.i(TAG, "Account switch detected — re-registering push")
                PushRegistration.tryRegister(context, token)
            }
            token == null && previous != null -> {
                Log.i(TAG, "Sign-out detected — clearing push")
                PushRegistration.unregisterAndClear(context, previous)
            }
        }
    }
}
