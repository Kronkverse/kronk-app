package info.kronk.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire type for `/api/v1/statuses/*` and `/api/v1/timelines/*`
// responses. Only fields Phase 2 consumes are declared; rest ignored.
//
// Kronk-specific:
//   - visibility uses the reach ladder ("public"/"mates"/"orbit"/
//     "self_only"). Legacy Mastodon values may still appear in
//     historical rows until the Phase 2a server migration lands.
//   - source_korner is present when the status is a korner card
//     (proposal_card, event_card, etc.). Null for plain statuses.

@Serializable
data class StatusDto(
    val id: String,
    val uri: String,
    val url: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    val content: String,
    val visibility: String,
    val account: AccountDto,
    @SerialName("in_reply_to_id")
    val inReplyToId: String? = null,
    @SerialName("reblogs_count")
    val reblogsCount: Long = 0,
    @SerialName("favourites_count")
    val favouritesCount: Long = 0,
    @SerialName("replies_count")
    val repliesCount: Long = 0,
    val favourited: Boolean = false,
    val reblogged: Boolean = false,
    val sensitive: Boolean = false,
    @SerialName("spoiler_text")
    val spoilerText: String = "",

    // Kronk 2.0 korner card marker. Null on plain statuses.
    @SerialName("source_korner")
    val sourceKorner: String? = null,
)
