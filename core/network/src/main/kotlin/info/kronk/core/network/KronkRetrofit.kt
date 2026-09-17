package info.kronk.core.network

import info.kronk.core.common.KronkHost
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

// Retrofit factory for the 2.0 network stack. One instance per app;
// consumers get typed API interfaces via [create].
//
// Intentionally simple: base URL = KronkHost, JSON decoder ignores
// unknown fields (Mastodon returns lots of fields we don't consume),
// OkHttp logging on debug only.
//
// Authentication (AuthInterceptor) lands with Phase 2B — for now
// this client is unauthenticated. Endpoints that don't need auth
// (POST /api/v1/apps, POST /oauth/token) work as-is.

class KronkRetrofit(
    private val debug: Boolean,
    private val extraInterceptors: List<okhttp3.Interceptor> = emptyList(),
) {
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        // Coerce absent fields to defaults where declared, instead of
        // throwing MissingFieldException. Mastodon's API is old + wide
        // enough that partial responses are common.
        coerceInputValues = true
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .apply {
                extraInterceptors.forEach(::addInterceptor)
                if (debug) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                }
            }
            .build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("${KronkHost.origin}/")
            .client(okHttp)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType()),
            )
            .build()
    }

    fun <T> create(service: Class<T>): T = retrofit.create(service)
}

inline fun <reified T> KronkRetrofit.create(): T = create(T::class.java)
