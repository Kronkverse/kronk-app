package info.kronk.feature.auth.data

import android.net.Uri
import info.kronk.core.common.KronkHost
import info.kronk.core.network.api.AccountsApi
import info.kronk.core.network.api.AppsApi
import info.kronk.core.network.api.OAuthApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull

// Orchestrates the OAuth flow. Callers are the SignInScreen (kicks
// off the flow) and MainActivity (handles the deep-link callback).
//
// State model (SignInState):
//   SignedOut     — no token; show sign-in UI
//   Authorising   — Custom Tab is up; app waits for callback
//   Exchanging    — code received, POSTing to /oauth/token
//   SignedIn      — token stored, account fetched
//   Failed(cause) — something went wrong; show error + retry
//
// Flow (happy path):
//   1. UI taps sign-in → beginSignIn() → returns authorize URL
//   2. UI launches Custom Tab with that URL
//   3. User approves on kronk.info → Kronk redirects to
//      kronk-auth://callback?code=...
//   4. MainActivity's intent-filter catches the deep link and calls
//      handleAuthCallback(uri)
//   5. Repository exchanges code for token, saves it, fetches
//      /accounts/verify_credentials, saves the account id + username
//   6. UI reactively flips to SignedIn state

class AuthRepository(
    private val storage: AuthStorage,
    private val apps: AppsApi,
    private val oauth: OAuthApi,
    private val accounts: AccountsApi,
) {
    // What the UI reads. Emits every state change.
    val state: Flow<SignInState> = combine(
        storage.token,
        storage.signedInAccountUsername,
    ) { token, username ->
        if (token != null && username != null) {
            SignInState.SignedIn(username)
        } else {
            SignInState.SignedOut
        }
    }

    // Constructs the authorize URL. Registers the app first if we
    // don't yet have client credentials (first launch or after wipe).
    // The Custom Tab launcher in the UI opens the returned URL.
    suspend fun beginSignIn(): String {
        val creds = ensureCredentials()
        return Uri.parse("${KronkHost.origin}/oauth/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", creds.id)
            .appendQueryParameter("redirect_uri", KronkHost.authCallbackUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", OAUTH_SCOPE)
            .build()
            .toString()
    }

    // Called by MainActivity when kronk-auth://callback fires. Extracts
    // the code, exchanges it for a token, fetches the signed-in
    // account, saves everything. Returns true on success.
    suspend fun handleAuthCallback(uri: Uri): Boolean {
        val code = uri.getQueryParameter("code") ?: return false
        val creds = storage.credentials.firstOrNull() ?: return false
        val token = oauth.token(
            clientId = creds.id,
            clientSecret = creds.secret,
            redirectUri = KronkHost.authCallbackUri,
            grantType = "authorization_code",
            code = code,
            scope = OAUTH_SCOPE,
        )
        storage.setToken(token.accessToken)
        val me = accounts.verifyCredentials("Bearer ${token.accessToken}")
        storage.setSignedInAccount(id = me.id, username = me.username)
        return true
    }

    suspend fun signOut() {
        storage.clearSession()
    }

    private suspend fun ensureCredentials(): AppCredentials {
        val existing = storage.credentials.firstOrNull()
        if (existing != null) return existing
        val reg = apps.register(
            clientName = APP_NAME,
            redirectUris = KronkHost.authCallbackUri,
            scopes = OAUTH_SCOPE,
            website = KronkHost.origin,
        )
        storage.setCredentials(id = reg.clientId, secret = reg.clientSecret)
        return AppCredentials(reg.clientId, reg.clientSecret)
    }

    private companion object {
        // Standard Mastodon-compatible scopes. Per-korner scopes land
        // when the manifest fetcher does (Phase 4).
        const val OAUTH_SCOPE = "read write follow push"
        const val APP_NAME = "Kronk"
    }
}

sealed interface SignInState {
    data object SignedOut : SignInState
    data class SignedIn(val username: String) : SignInState
}
