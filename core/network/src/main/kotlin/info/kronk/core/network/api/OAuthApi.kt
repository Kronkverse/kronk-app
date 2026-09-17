package info.kronk.core.network.api

import info.kronk.core.network.dto.TokenDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

// OAuth token exchange. After the Custom Tab authorize flow deposits
// an auth code on the kronk-auth://callback deep link, the app POSTs
// it here to swap for a bearer token.
//
// Mastodon supports two grant_types: `authorization_code` (with the
// code from the redirect) and `client_credentials` (app-only, no user
// context). Phase 2 uses only the authorization_code flow.

interface OAuthApi {
    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun token(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("grant_type") grantType: String,
        @Field("code") code: String,
        @Field("scope") scope: String,
    ): TokenDto
}
