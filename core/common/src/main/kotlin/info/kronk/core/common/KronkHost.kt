package info.kronk.core.common

// Kronk instance host. Every network call, OAuth URL, and deep-link
// intent-filter check reads from here.
//
// The 2.0 direction (docs/rebuild/plan.md § direction) locks the app
// on the new host `kronk.info`. The server-side rename from
// `mastodon.kronk.info` → `kronk.info` is tracked in
// Kronkverse/kronk#1859; the plan explicitly names it as a Phase-2
// blocker (auth won't complete against a host that isn't routing
// the API yet).
//
// Deliberately no fallback / no legacy hostname here — carrying
// `mastodon.kronk.info` in the app perpetuates a URL that's supposed
// to retire, and every future consumer would have to know which one
// to prefer. One source of truth: `kronk.info`. If a dev build hits
// the app before the server cutover has landed, sign-in will 404 on
// `/api/v1/apps` — that's the intended failure mode, not something
// to work around here.
//
// Not user-configurable: Kronk isn't a fediverse client, there's
// exactly one instance, the app is branded to it.

object KronkHost {
    const val value: String = "kronk.info"

    // Full origin including scheme, for building URLs to hand off to
    // Chrome Custom Tab (OAuth authorize URL, korner Custom Tab
    // fallback URLs).
    const val origin: String = "https://$value"

    // The OAuth redirect scheme the app claims via intent-filter.
    // Kept independent of the host — the scheme survives host renames.
    const val authCallbackScheme: String = "kronk-auth"
    const val authCallbackHost: String = "callback"
    const val authCallbackUri: String = "$authCallbackScheme://$authCallbackHost"
}
