package info.kronk.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// OAuth wire types — used by the Phase 2B auth flow.
//
// AppRegistration is the response of POST /api/v1/apps (register the
// Kronk client with the server). Token is the response of POST
// /oauth/token (exchange the auth code for a bearer token).

@Serializable
data class AppRegistrationDto(
    val id: String,
    val name: String,
    val website: String? = null,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("client_secret")
    val clientSecret: String,
    @SerialName("redirect_uri")
    val redirectUri: String,
    // The vapid public key is used later for WebPush; declaring it now
    // so the app-registration response deserialises fully.
    @SerialName("vapid_key")
    val vapidKey: String? = null,
)

@Serializable
data class TokenDto(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String,
    val scope: String,
    @SerialName("created_at")
    val createdAt: Long,
)
