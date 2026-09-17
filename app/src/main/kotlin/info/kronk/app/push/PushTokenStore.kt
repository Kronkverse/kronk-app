package info.kronk.app.push

import android.content.Context

// One place to persist the push transport + token/endpoint we
// picked at boot. Read by:
//   - the Kronk /api/v1/push/subscriptions registration (push #4)
//     to build the Web Push subscription payload,
//   - the incoming-message path (FCM RECEIVE + UnifiedPush MESSAGE)
//     to decide which crypto keys to use for decrypt.
//
// The store also holds the app-scope ECDH keypair + auth secret
// the Rails/webpush backend uses to encrypt payloads to us; those
// are generated on first successful endpoint receipt and reused
// across every re-registration until the user signs out.

object PushTokenStore {

    private const val PREFS = "push"

    // Which transport was picked at boot. Set by PushRegistrar +
    // the FCM registration receiver.
    enum class Transport(val wire: String) {
        UNIFIED_PUSH("unifiedpush"),
        FCM_VIA_GSF("fcm"),
        NONE("none"),
    }

    // The endpoint the Kronk server will POST push payloads to. For
    // FCM this is `https://fcm.googleapis.com/fcm/send/<token>`; for
    // UnifiedPush this is whatever URL the distributor gave us.
    fun endpoint(context: Context): String? = prefs(context).getString(KEY_ENDPOINT, null)

    fun transport(context: Context): Transport {
        val wire = prefs(context).getString(KEY_TRANSPORT, null) ?: return Transport.NONE
        return Transport.values().firstOrNull { it.wire == wire } ?: Transport.NONE
    }

    fun saveEndpoint(context: Context, endpoint: String, transport: Transport) {
        prefs(context).edit()
            .putString(KEY_ENDPOINT, endpoint)
            .putString(KEY_TRANSPORT, transport.wire)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val KEY_ENDPOINT = "endpoint"
    private const val KEY_TRANSPORT = "transport"
}
