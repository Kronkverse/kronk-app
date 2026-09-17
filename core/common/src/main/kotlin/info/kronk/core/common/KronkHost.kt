package info.kronk.core.common

// Kronk instance host. Every network call, OAuth URL, and deep-link
// intent-filter check reads from here.
//
// The production server is being renamed from `mastodon.kronk.info`
// to `kronk.info` as part of the 2.0 rebrand — cutover tracked in
// Kronkverse/kronk#1859. Until the API host actually serves at
// `kronk.info`, we point at the current live host and flip this
// constant at cutover. One-line change; ripples through every
// consumer because they all read `KronkHost.value` / `KronkHost.origin`.
//
// Not user-configurable — Kronk isn't a fediverse client; there's
// exactly one instance, and the app is branded to it (see
// docs/rebuild/plan.md § direction: "Instance URL hardcoded to
// kronk.info").

object KronkHost {
    // The current live API host. Flip to "kronk.info" at cutover
    // (Kronkverse/kronk#1859). Debug builds might override via a
    // separate constant later; for now, dev + release share this.
    const val value: String = "mastodon.kronk.info"

    // Full origin including scheme, for building URLs to hand off to
    // Chrome Custom Tab (OAuth authorize URL, korner Custom Tab
    // fallback URLs).
    const val origin: String = "https://$value"

    // The auth callback scheme the app claims via intent-filter.
    // Fixed regardless of host — the OAuth redirect_uri stays
    // `kronk-auth://callback` even after the host rename.
    const val authCallbackScheme: String = "kronk-auth"
    const val authCallbackHost: String = "callback"
    const val authCallbackUri: String = "$authCallbackScheme://$authCallbackHost"
}
