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
    // Points at shadow during the 2.0 rebuild — shadow is the rebuild
    // line and carries every 2.0 surface (Hub grid, Kommons tokens,
    // reach ladder, etc.). Production (`mastodon.kronk.info`) still
    // runs 1.x and doesn't have `/api/v1/korners`, `/@:user/mates`, or
    // the rebuild UI. Flip this back to production once the rebuild
    // has cut over there (Kronkverse/kronk#1859).
    const val value: String = "shadow.kronk.info"

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
