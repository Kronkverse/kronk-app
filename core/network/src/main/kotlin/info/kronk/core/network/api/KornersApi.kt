package info.kronk.core.network.api

import info.kronk.core.network.dto.KornerDto
import retrofit2.http.GET
import retrofit2.http.Header

// The korner registry endpoint. Returns every registered manifest;
// clients filter as appropriate (Hub grid keeps `enforced && !core`).
//
// The endpoint is public (no Doorkeeper scope required on index/show),
// but the app passes the bearer token when signed in so per-user
// tuned_in / unread_count fields come back populated.

interface KornersApi {
    @GET("api/v1/korners")
    suspend fun index(
        @Header("Authorization") bearer: String? = null,
    ): List<KornerDto>
}
