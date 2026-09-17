package info.kronk.feature.home.data

import info.kronk.core.network.api.TimelinesApi
import info.kronk.core.network.dto.StatusDto
import info.kronk.feature.auth.data.AuthStorage

// Fetches the Home timeline. Minimal shape for Phase 2C:
//   - one page at a time (no pagination)
//   - accepts a feed scope (mates / kronk); null → server default
//   - reads the bearer token from AuthStorage (AuthInterceptor
//     doesn't reach TimelinesApi.home yet because the Retrofit method
//     signature still takes a bearer @Header explicitly — that
//     migration happens when we drop the @Header params for all
//     API interfaces)
//
// Pagination + refresh + Room cache land alongside real feed use.

class HomeRepository(
    private val timelines: TimelinesApi,
    private val storage: AuthStorage,
) {
    suspend fun homeTimeline(scope: FeedScope, limit: Int = 40): List<StatusDto> {
        val token = storage.currentToken()
            ?: error("HomeRepository called without a signed-in session")
        return timelines.home(
            bearer = "Bearer $token",
            feedScope = scope.wire,
            limit = limit,
        )
    }
}

// Kronk's feed-scope selector, per docs/kronk_feed_and_reach.md.
// Mates  = mutual-only radiations
// Kronk  = the whole Kronkverse (public)
// Orbit  = mates + mates-of-mates (backend-computed; not exposed in
//          Phase 2C UI — reserved for later when the Home picker
//          gains a third option)
enum class FeedScope(val wire: String, val label: String) {
    Mates("mates", "Mates"),
    Kronk("kronk", "Kronk"),
}
