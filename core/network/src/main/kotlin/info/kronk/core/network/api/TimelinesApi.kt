package info.kronk.core.network.api

import info.kronk.core.network.dto.StatusDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// Home timeline for Phase 2C. Kronk-specific:
//   - `feed_scope` narrows the feed to mates / orbit / kronk (public).
//     Server-side enforced; the picker just POSTs the setting and
//     re-fetches. See kronk/docs/kronk_feed_and_reach.md § feed
//     scope.
//   - `limit` / `since_id` / `max_id` are the standard Mastodon
//     pagination triple.

interface TimelinesApi {
    @GET("api/v1/timelines/home")
    suspend fun home(
        @Header("Authorization") bearer: String,
        @Query("feed_scope") feedScope: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("max_id") maxId: String? = null,
    ): List<StatusDto>
}
