package info.kronk.core.network.api

import info.kronk.core.network.dto.AppRegistrationDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

// One-shot client registration. Called on first launch (or after a
// wipe) to obtain client_id/client_secret from the server. The
// Kronk 2.0 app registers as "Kronk" with the `read write follow push`
// scopes plus any per-korner scopes advertised in manifests (per-
// korner scopes land in Phase 4 when the manifest fetcher does).

interface AppsApi {
    @FormUrlEncoded
    @POST("api/v1/apps")
    suspend fun register(
        @Field("client_name") clientName: String,
        @Field("redirect_uris") redirectUris: String,
        @Field("scopes") scopes: String,
        @Field("website") website: String? = null,
    ): AppRegistrationDto
}
