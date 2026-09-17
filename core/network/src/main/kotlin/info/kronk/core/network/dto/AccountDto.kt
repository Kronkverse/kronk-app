package info.kronk.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire type for `/api/v1/accounts/*` responses. Ignores fields we
// don't consume (Kronk's Account serializer is very wide).
//
// Kronk-specific fields to note:
//   - matesCount replaces followers_count in 2.0 (see kronk/docs/
//     rebuild/decisions.md 2026-08-15). We accept both during the
//     transition — matesCount preferred, followersCount as fallback.
//   - profileVisibility is a new per-account reach-ladder value
//     (public/mates/orbit/self_only), independent of post reach.

@Serializable
data class AccountDto(
    val id: String,
    val username: String,
    @SerialName("acct")
    val acct: String,
    @SerialName("display_name")
    val displayName: String = "",
    val note: String = "",
    val avatar: String = "",
    val header: String = "",
    val url: String = "",
    val locked: Boolean = false,

    // Kronk 2.0
    @SerialName("mates_count")
    val matesCount: Long? = null,
    @SerialName("profile_visibility")
    val profileVisibility: String? = null,

    // Legacy — read for during-transition compat, prefer matesCount.
    @SerialName("followers_count")
    val followersCount: Long? = null,
    @SerialName("statuses_count")
    val statusesCount: Long = 0,
)
