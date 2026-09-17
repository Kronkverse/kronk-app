package info.kronk.feature.auth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore-backed storage for OAuth credentials + bearer tokens.
//
// One file, "kronk_auth.preferences_pb", private to the app process.
// Preferences DataStore (not Proto) chosen for simplicity — the auth
// state is 5 string keys, doesn't justify a schema. Migrate to Proto
// DataStore if the shape grows.
//
// Keys:
//   client_id, client_secret — from POST /api/v1/apps, saved on first
//                              registration and reused indefinitely
//                              (Mastodon doesn't rotate these).
//   access_token             — the bearer, injected by AuthInterceptor.
//   account_id               — the signed-in account's id (used to
//                              key per-account caches later).
//   account_username         — cached for showing "@name" without a
//                              network round-trip.

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kronk_auth",
)

class AuthStorage(private val context: Context) {
    private val store get() = context.authDataStore

    val credentials: Flow<AppCredentials?> = store.data.map { prefs ->
        val id = prefs[CLIENT_ID] ?: return@map null
        val secret = prefs[CLIENT_SECRET] ?: return@map null
        AppCredentials(id, secret)
    }

    val token: Flow<String?> = store.data.map { it[ACCESS_TOKEN] }

    val signedInAccountId: Flow<String?> = store.data.map { it[ACCOUNT_ID] }
    val signedInAccountUsername: Flow<String?> = store.data.map { it[ACCOUNT_USERNAME] }

    suspend fun currentToken(): String? = token.first()

    suspend fun setCredentials(id: String, secret: String) {
        store.edit {
            it[CLIENT_ID] = id
            it[CLIENT_SECRET] = secret
        }
    }

    suspend fun setToken(bearer: String) {
        store.edit { it[ACCESS_TOKEN] = bearer }
    }

    suspend fun setSignedInAccount(id: String, username: String) {
        store.edit {
            it[ACCOUNT_ID] = id
            it[ACCOUNT_USERNAME] = username
        }
    }

    suspend fun clearSession() {
        store.edit {
            it.remove(ACCESS_TOKEN)
            it.remove(ACCOUNT_ID)
            it.remove(ACCOUNT_USERNAME)
            // Deliberately keep the app credentials — the client_id/
            // secret are per-instance, not per-user, and re-registering
            // on every sign-in is wasteful.
        }
    }

    private companion object {
        val CLIENT_ID = stringPreferencesKey("client_id")
        val CLIENT_SECRET = stringPreferencesKey("client_secret")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val ACCOUNT_ID = stringPreferencesKey("account_id")
        val ACCOUNT_USERNAME = stringPreferencesKey("account_username")
    }
}

data class AppCredentials(val id: String, val secret: String)
