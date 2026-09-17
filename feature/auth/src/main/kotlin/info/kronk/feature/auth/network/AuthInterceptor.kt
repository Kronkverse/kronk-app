package info.kronk.feature.auth.network

import info.kronk.feature.auth.data.AuthStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

// Injects the bearer token into every outgoing request. Called
// synchronously by OkHttp; token read is a blocking DataStore lookup.
//
// Requests that shouldn't have auth (POST /api/v1/apps, POST /oauth/
// token) skip the header if the URL matches — during first launch
// there IS no token to inject, and Mastodon rejects auth-header
// presence on some public endpoints.

class AuthInterceptor(
    private val storage: AuthStorage,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val isAuthExempt = path == "/api/v1/apps" || path == "/oauth/token"
        val token = if (isAuthExempt) null else runBlocking { storage.currentToken() }
        val out = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        return chain.proceed(out)
    }
}
