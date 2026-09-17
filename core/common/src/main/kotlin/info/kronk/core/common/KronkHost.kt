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
    // Populated by the :app module's KronkHostInit on first read.
    // Debug builds get shadow.kronk.info; release builds get
    // kronk.info (post-cutover Kronkverse/kronk#1859). :core:common
    // is Android-independent so it can't read BuildConfig itself —
    // :app writes the concrete value in via `initFromApp`.
    @Volatile
    private var _value: String = "shadow.kronk.info"

    val value: String get() = _value

    // Full origin including scheme, for building URLs to hand off to
    // Chrome Custom Tab (OAuth authorize URL, korner Custom Tab
    // fallback URLs).
    val origin: String get() = "https://$_value"

    // Called by KronkApplication.onCreate — must run before any
    // consumer reads KronkHost. Idempotent.
    fun initFromApp(host: String) {
        _value = host
    }

    // The auth callback scheme the app claims via intent-filter.
    // Fixed regardless of host — the OAuth redirect_uri stays
    // `kronk-auth://callback` even after the host rename.
    const val authCallbackScheme: String = "kronk-auth"
    const val authCallbackHost: String = "callback"
    const val authCallbackUri: String = "$authCallbackScheme://$authCallbackHost"
}
