package info.kronk.core.network.api

import info.kronk.core.network.dto.AccountDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

// Minimum viable Accounts API for Phase 2.
//
// verify_credentials: who am I? Called after OAuth returns a token,
// stashed as the signed-in account.
// account(id): fetch any account by id. Used when rendering statuses
// in a timeline to enrich each status.account (which already ships in
// StatusDto — this endpoint is Phase 3+, added here to prove the
// pattern).
//
// Bearer token is passed via @Header for now; a Phase 2B
// AuthInterceptor will inject it automatically so callers don't need
// to pass it.

interface AccountsApi {
    @GET("api/v1/accounts/verify_credentials")
    suspend fun verifyCredentials(
        @Header("Authorization") bearer: String,
    ): AccountDto

    @GET("api/v1/accounts/{id}")
    suspend fun account(
        @Path("id") id: String,
        @Header("Authorization") bearer: String,
    ): AccountDto
}
