package info.kronk.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire type for `/api/v1/korners` — the manifest registry entry.
//
// The server returns every registered manifest (including the core
// spaces feed/profile/hub/nudges/settings), so consumers can resolve
// any slug to its declared identity. Hub's native grid filters to
// `enforced && !core` client-side; core spaces already have their
// place in the bottom nav.
//
// Only the fields the app uses for the Hub grid + korner routing are
// declared. Unknown fields are ignored by the shared kronkJson config.

@Serializable
data class KornerDto(
    val slug: String,
    val name: String,
    val icon: KornerIconDto? = null,
    val tagline: String? = null,
    @SerialName("hub_teaser")
    val hubTeaser: KornerHubTeaserDto? = null,
    val enforced: Boolean = false,
    val core: Boolean = false,
    // Server hint at the SPA route for the korner's landing page. For
    // pluggable korners this is usually null and the client derives
    // `/hub/<slug>`. Core spaces carry their own top-level mount.
    val mount: String? = null,
    @SerialName("tuned_in")
    val tunedIn: Boolean = true,
    @SerialName("tune_in_count")
    val tuneInCount: Int = 0,
    @SerialName("unread_count")
    val unreadCount: Int = 0,
)

@Serializable
data class KornerIconDto(
    val material: String? = null,
)

@Serializable
data class KornerHubTeaserDto(
    val static: String? = null,
)
