package info.kronk.app.push

import android.content.Context
import android.util.Base64
import android.util.Log
import info.kronk.core.common.KronkHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

// Once we have a push endpoint (UnifiedPush or FCM) AND an OAuth
// access token (extracted from the SPA's initial-state — see
// KronkWebViewClient), register the subscription with Kronk so its
// Rails webpush worker starts delivering nudges to our endpoint.
//
// POST /api/v1/push/subscriptions
// {
//   "subscription": {
//     "endpoint": "<endpoint URL>",
//     "keys": {
//       "p256dh": "<base64url uncompressed P-256 key>",
//       "auth":   "<base64url 16-byte auth secret>"
//     }
//   },
//   "data": {
//     "alerts": { "mention": true, "favourite": true, ... },
//     "policy": "all"
//   }
// }
//
// Response carries a subscription id we cache for later deletes /
// updates. `PushTokenStore` persists that alongside the endpoint.

object PushRegistration {

    private const val TAG = "PushRegistration"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient.Builder().build()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    // Called from KronkWebViewClient after we detect a signed-in
    // page and pull the access token out of initial-state, and
    // from PushTokenStore when the endpoint appears (whichever
    // arrives last kicks off the registration).
    fun tryRegister(context: Context, accessToken: String) {
        val endpoint = PushTokenStore.endpoint(context) ?: run {
            Log.i(TAG, "tryRegister: no endpoint yet, deferring")
            return
        }
        scope.launch { registerBlocking(context, accessToken, endpoint) }
    }

    private fun registerBlocking(context: Context, accessToken: String, endpoint: String) {
        runCatching {
            val keys = PushCrypto.ensureKeys(context)
            val body = JSONObject().apply {
                put(
                    "subscription",
                    JSONObject().apply {
                        put("endpoint", endpoint)
                        put(
                            "keys",
                            JSONObject().apply {
                                put("p256dh", base64UrlNoPad(keys.publicKeyRaw))
                                put("auth", base64UrlNoPad(keys.authSecret))
                            },
                        )
                    },
                )
                put(
                    "data",
                    JSONObject().apply {
                        // Ask for all alerts by default. Kronk's
                        // per-korner mute + per-type mute already
                        // live server-side; a Settings screen in the
                        // app is the follow-up that lets the user
                        // trim this locally.
                        put(
                            "alerts",
                            JSONObject().apply {
                                put("mention", true)
                                put("status", true)
                                put("reblog", true)
                                put("follow", true)
                                put("follow_request", true)
                                put("favourite", true)
                                put("poll", true)
                                put("update", true)
                            },
                        )
                        put("policy", "all")
                    },
                )
            }
            val req = Request.Builder()
                .url(KronkHost.origin + "/api/v1/push/subscriptions")
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonMedia))
                .build()
            val response = http.newCall(req).execute()
            try {
                if (!response.isSuccessful) {
                    Log.w(TAG, "registerBlocking: HTTP ${response.code} ${response.message}")
                } else {
                    val json = response.body?.string().orEmpty()
                    Log.i(TAG, "registerBlocking: registered — ${json.take(200)}")
                }
            } finally {
                response.close()
            }
        }.onFailure { t: Throwable -> Log.e(TAG, "registerBlocking failed", t) }
    }

    // Base64url without padding — the RFC 8291 encoding used in
    // Web Push subscription payloads.
    private fun base64UrlNoPad(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
