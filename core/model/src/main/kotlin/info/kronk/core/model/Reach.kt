package info.kronk.core.model

// Kronk's reach ladder — the sole audience vocabulary in 2.0. Replaces
// Mastodon's `unlisted / private / direct / limited` visibility values,
// which are retired on the reach ladder (see kronk/docs/rebuild/
// decisions.md 2026-08-28).
//
// Four tiers, ordered widest → narrowest:
//   Public   (0) — Kronkverse: any signed-in local member
//   Mates    (6) — mutual-only
//   Orbit    (7) — mates + mates-of-mates (backend-computed)
//   SelfOnly (8) — owner-only, no feed radiation
//
// Wire values are strings ("public" / "mates" / "orbit" / "self_only"),
// matching the API contract in the web repo's
// app/models/concerns/status/visibility.rb.

enum class Reach(val wire: String) {
    Public("public"),
    Mates("mates"),
    Orbit("orbit"),
    SelfOnly("self_only"),
    ;

    companion object {
        // Parse an inbound wire value into a Reach. Returns null for
        // anything we don't recognise (including retired Mastodon
        // values like "unlisted" / "private" / "direct" / "limited")
        // so callers can decide whether to fail loud or fall back.
        //
        // A legacy-value translator will live here when we have a
        // consumer for it (feed cards reading historical rows). For
        // now, YAGNI — the composer only produces current wire values
        // and no feed layer exists yet.
        fun fromWireOrNull(wire: String): Reach? =
            entries.firstOrNull { it.wire == wire }
    }
}
