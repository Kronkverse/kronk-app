# Kronk Android app — 2.0 rewrite plan (v2)

**Date:** 2026-09-17
**Author:** claude (mainframe)
**Status:** Draft. Supersedes `_app_2_0_rebuild_plan_v1_superseded.md`, which was based on a stale fork read that missed 196 commits and a full Java Nudges implementation. Direction below reflects Tal's calls in the v1 conversation + course-corrections after the corrected upstream audit on 2026-09-17.

---

## Direction (locked 2026-09-17)

- **Fresh Kotlin/Compose rewrite** — new module layout, Hilt DI, Retrofit + kotlinx-serialization, Room + DataStore.
- **Day-one native surface: Home + Profile + Nudges only.** Ж menu native. Hub + other korners: Chrome Custom Tab into `kronk.info/hub/*` v1.
- **Distribution:** keep the kronk.info self-updater. Retire AppCenter flavours. No Play Store this cycle.
- **Instance URL:** hardcoded to `kronk.info` (server rename cutover tracked in Kronkverse/kronk#1859).
- **No side-by-side at ship.** End users see **one app icon**; the new build replaces the current one via the self-updater at cutover. During development (Phases 1–6), debug builds carry an `applicationIdSuffix` (`.next.debug`) so testers can side-load without wiping their real Kronk install. Release builds always target the shipping `info.kronk.app` applicationId — the cutover is dropping the debug suffix, not a flavour flip.

## What changed since v1

- **The "legacy is broken" framing is gone.** Upstream `Kronkverse/kronk-app/development` builds cleanly, has 59 fragments, and ships. Contributor mangobee has landed a complete Java Nudges implementation (threads, inbox, streaks, reactions, milestones, voice upload, per-partner counter) since June 2026. The rewrite is an **investment call for long-term velocity + parity with the still-evolving web spec**, not a rescue.
- **The Java Nudges implementation is a snapshot, not an oracle.** Per Tal: "the old nudges isn't up to date with new nudges, it's evolved a lot." Java Nudges is a **reference for what the current app users can do**, but not the target — the target is current web Nudges (see `~/kronk/docs/kronk_nudges.md`), which has moved on.
- **Coordination with mangobee is now a first-class concern.** Not a solo rewrite; needs to interlock with their active shipping cadence.

## Corrected current-app snapshot

- **Repo:** `Kronkverse/kronk-app`, PRs target `development`. Ships to `kronk.info/kronk.apk` (release, on `v*` tag) and `kronk.info/dev/kronk.apk` (dev, every push to `development`).
- **Stack:** Java, XML layouts, `grishka.appkit` foundation, Otto event bus, OkHttp 3.14, custom `MastodonAPIRequest<T>` + Gson 2.8, per-account SQLite + `SharedPreferences`. No DI, no meaningful tests. **All still true and all still working.**
- **Kronk-specific features shipped:** Orbit (`/timelines/friends_activity`), Live (Jitsi room), invite deep links, account-approval polling, and **complete Nudges** (16+ files across `model/`, `api/requests/`, `ui/displayitems/`, `fragments/` — voice recording removed to match web, expiry, milestones, reactions, streaks all present).
- **Deep links:** `kronk-auth://callback` (OAuth); `https://mastodon.kronk.info/@*` and `/invite/*` (needs updating to `kronk.info` at cutover).
- **Contributors:** Tal, `itsa-mangobee` (mangobee on mainframe). Coordinate via `/home/shared/inbox.md` + `/home/shared/workboard.md`.

## What the 2.0 web demands of a client

(No change from v1; details in the v1 audit at the top of `_app_2_0_rebuild_plan_v1_superseded.md`. Summary only here.)

- **Mates** (mutual-only follow) — `mates_count` replaces `followers_count`.
- **Reach ladder** — `public / mates / orbit / self_only`. Old `unlisted/private/direct/limited` retired.
- **Krews** — additive group targeting, orthogonal to reach.
- **Korners** — manifest-driven feature spaces at `/hub/<slug>`.
- **Nudges** — merged notifications + DMs, `Nudges::Conversation` + `Nudges::Message` + `Nudges::Nudge` inline events. WebSocket channel `timeline:nudges:account:<id>`.
- **Kommons** — proposals + votes + tasks + threaded comments.
- **Aesthetic** — token pipeline (`tokens.yaml`), Kronk-purple palette, radius/motion/elevation scales.
- **Frame / IA** — TopBand (Ж + HubSwitcher), SpaceNav, Stage (three archetypes), BottomBand (Me/Home/Hub/Nudges), Kosmos background.
- **Nudges evolved** — the web spec has moved beyond what Java Nudges implements; the app-web gap will grow unless the client tracks the current spec.

## Gap analysis (rebalanced)

| Area | Java app today | 2.0 rewrite target | Gap |
|---|---|---|---|
| Language / UI | Java + XML + AppKit | Kotlin + Compose | Full rewrite |
| DI | Static singletons | Hilt | Introduce |
| State | Otto + SQLite + prefs | Flow + StateFlow + Room + DataStore | Migrate |
| Networking | OkHttp 3 + hand-rolled + Gson | Retrofit + OkHttp 4 + kotlinx-serialization | Replace |
| **Nudges** | **Shipped in Java, stale vs web** | **Track current web spec** | **Rebuild against current spec** |
| Follow graph | Followers/Following | Mates (mutual-only) | New data model |
| Visibility | Upstream 4 values | Reach ladder | Add reach translator |
| Composer | Fixed status form | Per-korner via manifest | ComposeShell primitive |
| Kommons | Absent | Proposals + votes + comments | New feature module |
| Korner framework | Absent | Manifest-driven Hub tiles | Manifest fetcher + card adapter registry |
| Frame / IA | Fragment stack | 5-slot frame + Ж | New shell |
| Aesthetic | Kronk-purple in colors.xml | Full token map | Compose theme |
| Streaming | Push only | WebSocket for Nudges | WS client |

## Target architecture (unchanged from v1)

- Kotlin 2.x, JDK 21, AGP 8.5+, Gradle 8.9+, `libs.versions.toml`.
- Multi-module: `:app`, `:core:{common,model,network,data,designsystem,streaming}`, `:feature:{auth,home,profile,nudges,compose,hub,walkthrough,korner-card}`.
- Compose + Navigation Compose. Single Activity. Manifest-driven route registration.
- Hilt for DI. MVI-lite (StateFlow + Intent).
- Retrofit + OkHttp 4 + kotlinx-serialization. Room for lists, DataStore (proto) for prefs.
- Mirror `tokens.yaml` into Compose values (`KronkColors`, `KronkShapes`, `KronkMotion`, etc.).
- OAuth via Chrome Custom Tab. Instance URL hardcoded `kronk.info`.
- Chrome Custom Tab fallback for any korner without a native screen.

## Coexistence with the Java app

Direction cleared with Tal 2026-09-17: mangobee is fine with the rewrite; no coordination ceremony needed. Mechanics:

1. **The Java `:mastodon` module stays the shipping app** until `:app` reaches parity. Rewrite touches only additions (new modules, new packages) during Phases 0–6. Cutover in Phase 7.
2. **Debug side-load only during dev.** `:app`'s debug builds carry `.next.debug` suffix so testers can install alongside the real Kronk without collision. End users never see two icons.
3. **Nudges rebuild targets current web spec, not the Java implementation.** Java Nudges keeps shipping (mangobee's cadence uninterrupted); `:feature:nudges` mirrors what `~/kronk/docs/kronk_nudges.md` currently says.
4. **Cutover is atomic.** Phase 7 flips `:app` release builds to `info.kronk.app`, deletes `:mastodon` from the build, cuts `v3.0.0`, and the self-updater delivers the new APK to existing users on next check.

## Phased plan

Each phase ends on a **shippable dev APK** at `kronk.info/dev/kronk.apk`. Legacy `githubRelease` APK keeps building unchanged; the rewrite ships as `info.kronk.app.next` (`.debug` in debug builds) so both live side-by-side until cutover.

### Phase 0 — Announce + hygiene (docs + version catalog, no user-visible change)
1. **Post the direction announcement to inbox** + link to this plan doc.
2. Land the plan into `kronk-app/docs/rebuild/plan.md` via PR. Update the "current-app snapshot" section to keep pace with what mangobee ships.
3. Add `libs.versions.toml` extracting current deps (byte-identical dep graph verified).
4. `.gitignore` sweeps `**/build/` for the incoming modules.

### Phase 1 — Compose foundation (:core:designsystem)
1. Add `:core:common`, `:core:model`, `:core:designsystem` module scaffolds.
2. Populate `:core:designsystem` with the Kronk Compose theme mirroring `tokens.yaml` (Color / Typography / Shape / Motion / Theme).
3. Add the six primitives: `Stage`, `SpaceBadge`, `MembraneNav`, `KornerGlyph`, `StatusKornerCard`, `ComposeShell`.
4. New `:app` module with `MainActivity` + rose-emblem welcome pane. Base applicationId `info.kronk.app` (shipping ID); debug adds `.next.debug` suffix so testers side-load without collision. Release builds hit `info.kronk.app` directly but don't ship until cutover.

### Phase 2 — Auth + minimal Home
1. `:core:network` — Retrofit + AuthInterceptor + Account/Status/Timeline APIs.
2. `:feature:auth` — Chrome Custom Tab OAuth flow, DataStore-backed token store.
3. `:feature:home` — Mates + Kronk feed toggle, plain-status card via `StatusKornerCard`.
4. `Reach` sealed class in `:core:model` with write-time translator for legacy visibility values.
5. **Blocker:** `kronk.info` server cutover must have landed (Kronkverse/kronk#1859).

### Phase 3 — Profile + composer + card adapters
1. Profile screen with the reach picker + krew multi-select in the composer.
2. Card adapter registry — dispatches on `feed_projection.card`.
3. Ship `plain_status` + `proposal_card` adapters.
4. `:feature:profile` and `:feature:compose` modules.

### Phase 4 — Ж menu + korner manifests + Custom Tab fallback
1. `GET /api/v1/korners/manifests` fetcher + Room cache in `:core:data`.
2. `KornerGlyph(slug: String)` overload — resolves manifest icon.
3. Ж menu (draggable FAB) — composer routes from manifest, Custom Tab fallback for non-native korners.
4. **No native Hub screen** — Hub pillar opens Custom Tab into `https://kronk.info/hub`.

### Phase 5 — Nudges (against current web spec, not Java implementation)
1. `:feature:nudges` — conversation list + thread + reactions + inline nudge events.
2. `:core:streaming` — WebSocket client, `timeline:nudges:account:<id>` subscription.
3. Per-conversation settings (mute, leave, notification prefs) matching web.
4. **Coordinate with mangobee** — Java Nudges keeps shipping until `:app` Nudges reaches parity; then a coordinated switchover, not a fork.
5. Voice recording arrives if/when the web adds it back (web decision `2df080d8 "Nudges: remove voice recording from APK (match web)"` in current upstream shows voice was removed to match web).

### Phase 6 — Walkthrough
1. First-run bubbles matching the web tour: Welcome Home (rose) → Home → Profile → Nudges → Ж → close. Skip Hub bubble (Custom Tab).

### Phase 7 — Cutover
1. `:app` release keeps `info.kronk.app` (already the shipping ID); debug `.next.debug` suffix stays for continued dev builds.
2. Delete `:mastodon` from the build (module directory kept in git history but no longer included in `settings.gradle` and no longer shipped).
3. Retire AppCenter flavours.
4. **Silent-background updater work** — schedule Wi-Fi-only APK download via WorkManager, install prompt on next launch. Requires REQUEST_INSTALL_PACKAGES permission (already granted) and battery-optimisation exemption request.
5. Cut `v3.0.0` tag → `kronk.info/kronk.apk`.
6. Existing v2.x users pull the v3.0.0 APK via the self-updater, see "Kronk 2.0 is ready — restart to switch?" on next launch. Same app icon, new build. No second install.

## Resolved decisions (2026-09-17 round 2 with Tal)

| Question | Decision |
|---|---|
| Nudges parity timing | **Full-parity-then-switch.** All of `:feature:nudges` built against the current web spec, then Phase 7 cutover flips it wholesale. Java Nudges keeps shipping unchanged until then. |
| Version numbering at cutover | **`v3.0.0`.** Major bump signals fundamentally new build; matches semver intent for a rewrite. |
| In-app updater UX | **Silent background download on Wi-Fi, prompt on next launch.** Users get updates faster; no "install now?" mid-use. Requires background download work + battery-optimisation exemption; falls in Phase 7 alongside cutover, not earlier. |
| Federation posture | **Follow web — Kronk-only, no fediverse UI.** No cross-instance follow, no remote-user search, no ActivityPub-specific surfaces. Revisit if federation returns as a web priority. |

## Karpathy coding rules

Verbatim from https://github.com/multica-ai/andrej-karpathy-skills. Bias toward caution over speed; for trivial tasks, use judgment.

### 1. Think Before Coding
Don't assume. Don't hide confusion. Surface tradeoffs.
- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

**Direct lesson from v1:** the v1 audit assumed `origin/development` was ground truth without checking `upstream/development`. 196 commits + a full Nudges implementation invisible until Tal asked me to merge PRs. Fetch upstream first, always. Recorded in `~/.claude/projects/-home-tal/memory/project_kronk_app_active_upstream.md`.

### 2. Simplicity First
Minimum code that solves the problem. Nothing speculative.
- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.

### 3. Surgical Changes
Touch only what you must. Clean up only your own mess.
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- Remove imports/variables/functions your changes made unused; don't remove pre-existing dead code unless asked.

**Direct lesson from v1:** the rewrite plan proposed rewriting **everything** all at once. This ignores mangobee's active work in `:mastodon`. The corrected plan touches only additions (new modules, new packages) until cutover; the existing Java app stays untouched.

### 4. Goal-Driven Execution
Define success criteria. Loop until verified.
- Every phase ends on a shippable dev APK — that's the success criterion.
- Every PR names the verification command in the body.
- Compile-verify (not assume-verify).

## References

- Web rebuild integration branch: `Kronkverse/kronk/rebuild/2.0.0`
- Design decisions log: `~/kronk/docs/rebuild/decisions.md`
- Spec index: `~/kronk/docs/kronk_*.md`, `~/kronk/docs/spaces/*.md`, `~/kronk/docs/korners/*.md`
- Korner Standard: `~/kronk/docs/korners/korner_standard.md`
- Current app repo: `~/kronk-app/`, upstream `Kronkverse/kronk-app` (always fetch `upstream/development`, not `origin/development`)
- Coordination: `/home/shared/inbox.md`, `/home/shared/workboard.md`
- Superseded v1 plan: `~/kronk-notes/audits/_app_2_0_rebuild_plan_v1_superseded.md`
