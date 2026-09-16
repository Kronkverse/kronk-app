# Kronk Android app — 2.0 rebuild plan

**Date:** 2026-09-16
**Author:** claude (mainframe), directional decisions from Tal
**Status:** Working plan. Four core direction questions answered (see below); a handful of low-stakes items remain, called out inline. Amended by later phase work — this doc gets updated as decisions land, not superseded.

---

## Direction (locked with Tal 2026-09-16)

- **Fresh Kotlin/Compose rewrite** — new module layout, Hilt DI, Retrofit + kotlinx-serialization, Room + DataStore. Legacy app stays shippable behind a build flavour during the transition.
- **Day-one native surface: Home + Profile + Nudges only.** Ж menu is native. Hub is NOT a native screen day-one — Hub tiles open in Chrome Custom Tab into `/hub/*` on the server. Kommons, Kalendar, Booth, Albutts, Klot, Moments, Kuestions, etc. — all Custom Tab fallbacks in v1. Native ports come later per user demand.
- **Distribution: keep the kronk.info self-updater as-is.** GitHub Actions builds → `kronk.info/kronk.apk` (release) + `kronk.info/dev/kronk.apk` (dev). Retire the two AppCenter flavours. No Play Store this cycle.
- **Instance URL: hardcoded to `kronk.info`.** The Kronk server is being renamed from `mastodon.kronk.info` → `kronk.info` as part of the 2.0 rebrand (per Tal 2026-09-16). Shadow stays on `shadow.kronk.info`. No instance-picker screen. OAuth callback stays `kronk-auth://callback`. The rewrite is the natural moment to bake in the new host; verify the cutover has landed on the server before shipping a release build.

---

## TL;DR

- Java + AppKit + Otto + OkHttp 3.14 codebase → Kotlin + Compose + Hilt + Retrofit + Room. Fresh module tree, not a refactor.
- Same repo (`Kronkverse/kronk-app`), branch `feature/2.0.0-rebuild` off `development`, new build flavour `kronk2` so the shipped app keeps building through the transition. Flavour flip at cutover.
- Ship the minimum viable native surface (Home + Profile + Nudges + Ж) and let Custom Tabs cover korner discovery + korner-specific spaces until user demand dictates a native port. Keeps the rewrite scope tractable.
- 7 shippable phases, each ending on a dev APK at `kronk.info/dev/kronk.apk`.

---

## Current app snapshot

- **Language / UI:** 100% Java, 127 XML layouts, no Jetpack Compose, no Jetpack Navigation, no DI. `grishka.appkit` provides the `FragmentStackActivity` foundation — the library is single-maintainer and hasn't been updated since 2022, so any Android API change we can't shim ourselves is a blocker.
- **State:** hybrid — `AccountSessionManager` singleton + per-account SQLite (`accounts/{accountID}.db`) + `SharedPreferences` for global prefs + in-memory. No Room. Event dispatch through **Otto** (deprecated since 2017).
- **Networking:** OkHttp 3.14.9 (5 years old, security-relevant) + hand-rolled `MastodonAPIRequest<T>` + Gson 2.8.9 + custom `IsoInstant`/`IsoLocalDate` type adapters. No Retrofit, no Ktor.
- **Feature surface (~40 fragments):** upstream Mastodon parity + Kronk touches. Kronk-specific: **Orbit** (via `/timelines/friends_activity`), **Live** (Jitsi room, replaced Search), invite deep links, account-approval polling, discovery banners. No mates, no krews, no nudges, no korners, no kommons.
- **Assumptions that break under 2.0:** `StatusPrivacy` enum hardcoded to `PUBLIC/UNLISTED/PRIVATE/DIRECT` (all four terms retired on the reach ladder); `followers_count` field expected; instance hardcoded to `mastodon.kronk.info` in `AndroidManifest.xml`; token scope hardcoded to `read write follow push`; no `source_korner` handling on statuses.
- **CI / distribution:** GitHub Actions builds `githubRelease` on every push to `development`, signs, deploys to `kronk.info/dev/kronk.apk` + a `dev/version.json` for the in-app updater. Release channel builds on `v*` tag → `kronk.info/kronk.apk`. Two AppCenter flavours (`appcenterPrivateBeta`, `appcenterPublicBeta`) exist alongside.
- **Tests:** effectively none (a screenshot generator for Play Store is the only "test").
- **Docs:** `README.md` (user-facing) + `CLAUDE.md` (2026-03-25, contributor + release workflow). No `docs/` folder with architecture or API contracts.

## What Kronk 2.0 demands of a client

(Full audit against the rebuild in the parent conversation. Summary only here.)

**Vocabulary + graph:**
- **Mates** — mutual-only follow graph. `mates_count` replaces `followers_count`. No one-way follows.
- **Orbit** — mates + mates-of-mates, computed at query time; likely backend-only for now (users see the reach effect, don't pick "orbit" as a scope in v1).
- **Krews** — additive group targeting, orthogonal to reach. Composer picker is separate from visibility.
- **Froth** — the platform-wide favourite/reaction primitive; code stays `favourite`, UI reads "froth".

**Reach ladder** (replaces upstream visibility):
- `public` (Kronkverse), `mates` (mutual-only), `orbit` (2-hop), `self_only` (private draft-like).
- Old values (`unlisted/private/direct/limited`) are retired; client must translate them on write during the transition. Replies **inherit parent visibility** — no per-reply picker.

**Korner framework:**
- Manifest-driven feature spaces. Manifests live in `config/korners/*.yaml`; the app fetches them via `GET /api/v1/korners/manifests` and dynamically renders the Hub grid, per-korner tiles, feed cards, and Ж composer entries.
- Every korner card wraps a `StatusKornerCard` primitive (icon + label + title + summary + action). Manifest declares which fields feed the card and where clicks go.
- Icons resolve to Material Symbols (via `useKornerIcon(slug)`).
- Boot-time validation on the server (`bin/tootctl korners doctor`) enforces L1–L10 conformance; the app must trust manifests.

**Nudges** (merged notifications + DMs):
- `Nudges::Conversation` (mate or krew), `Nudges::Message`, `Nudges::Nudge` (inline korner-sourced event routed via manifest bus).
- WebSocket channel `timeline:nudges:account:<id>` for live push. `/api/v1/streaming` unchanged transport-wise.
- Per-conversation unread combining message count + unseen nudge events.
- Voice recording is a live feature (parity gate — app must ship it before users see the "voice" affordance on web).

**Kommons** (governance):
- Proposals + votes (backing/abstaining/blocking) + tasks + threaded comments. Backing = froth under the hood.
- `/api/v1/kommons/*` API surface.
- Proposals auto-project into feed with `source_korner: "kommons"` and a `proposal_card` adapter.

**Frame / IA:**
- Five-slot frame: TopBand (Ж + HubSwitcher), SpaceNav (badge + title + view picker), Stage (content), BottomBand on mobile (Me/Home/Hub/Nudges pillars), Kosmos starfield background.
- **Ж menu** is the single floating action button for every composer entry; no per-korner FABs.
- Three stage archetypes: `stage-fill` (full-bleed), `stage-column` (capped 38.75rem reading column), `stage-grid` (tiles). **Vertical scroll only on phone; no horizontal side-scrolling ever.**
- **Membrane nav** — the shared tab idiom (moving light pool under a wire) for every tabbed surface.

**Aesthetic system:**
- Token pipeline (`tokens.yaml` → generated SCSS on web). Semantic tokens (`--accent`, `--surface-*`, `--text-*`), Kronk-purple brand, radius scale, motion durations, elevation shadows. No raw hex on web; app should adopt an equivalent tokens map.
- Fonts: `--font-display` (serif for headers/wordmark), `--font-body` (sans), `--font-mono` (Roboto Mono).

**Auth:**
- OAuth unchanged in transport, but signup is now immediate-activation and email is voluntary. No confirm-email gate.
- Per-user tokens deferred (awaiting Anthemos consent layer).

**Landmines** (client-visible things that break naive Mastodon clients):
- `followers_count` field rename.
- Visibility enum values.
- Reply inheritance (no per-reply picker).
- New `source_korner` field on Status.
- Server-side feed filter respects tune-outs (client must POST tune-out setting).
- Notifications store is being replaced — Nudges is the real system.
- Profile privacy (`profile_visibility`) is a **separate** enum from post reach.

## Gap analysis

| Area | Today | 2.0 needs | Gap |
|---|---|---|---|
| Language / UI | Java + XML + AppKit | Modern Android idiom | Kotlin + Compose + Navigation Compose |
| DI | None (singletons) | Testable, module-boundaried | Hilt |
| State | Otto + SQLite + in-memory | Reactive, unidirectional | Kotlin Flow + StateFlow + DataStore + Room |
| Networking | OkHttp 3 + hand-rolled | Typed API surface, streaming | Retrofit (or Ktor) + OkHttp 4 + kotlinx-serialization + WebSocket client |
| Serialisation | Gson 2.8 | Kotlinx | kotlinx-serialization |
| Testing | ~0 | Unit + UI + integration | JUnit5 + Turbine + Compose UI test |
| Visibility model | 4 upstream values | Reach ladder | New `Reach` enum + write-time translation of legacy |
| Follow graph | Follower/Following | Mates | Mates data model + request/accept flow |
| Composer | Fixed per-status form | Per-korner via manifest | ComposeShell primitive fed by `compose.route` |
| Nudges | Bell + DMs (separate) | Unified conversation stream | Full new module (conversation list, thread, voice, live WS) |
| Kommons | Absent | Proposals, votes, tasks, comments | New module |
| Korner framework | Absent | Manifest-driven Hub | Manifest fetcher + Hub grid + card adapter registry + icon resolver |
| Frame / IA | Fragment stack | 5-slot frame with Ж | New shell, single top-level `KronkAppScaffold` composable |
| Aesthetic | Kronk-purple in colors.xml | Full token map | Material 3 `ColorScheme` + Compose theme + typography + shape tokens |
| Auth | OAuth + hardcoded scopes | OAuth + per-korner scopes | Scope negotiation from manifest permissions |
| Streaming | Not verified | `timeline:nudges:account:<id>` channel | WebSocket client + Nudges live push |
| Distribution | GH Actions + kronk.info APK | Same, plus Play Store when ready | Keep pipeline; add Play Store variant later |

## Target architecture

**Language + toolchain.** Kotlin 2.x. JDK 21 source/target (up from 17). AGP 8.5+. Gradle 8.9+. `libs.versions.toml` for version catalog.

**Module layout.** Multi-module — keeps build times sane and enforces separation.

```
:app                        # composition root, DI wiring, MainActivity
:core:designsystem          # theme, tokens, typography, shapes, Compose primitives (Stage, SpaceBadge, MembraneNav, KornerGlyph, StatusKornerCard, ComposeShell)
:core:model                 # domain types (Account, Status, Reach, Krew, Nudge, Proposal, KornerManifest, etc.) — no Android deps
:core:network               # Retrofit + kotlinx-serialization DTOs + interceptors + auth
:core:data                  # repositories, Room DB, DataStore, pagination
:core:streaming             # WebSocket client for /api/v1/streaming + Nudges channel
:core:common                # utilities, Result types, dispatchers
:feature:auth               # OAuth flow, instance picker
:feature:home               # Home feed (Mates/Kronk selector), Orbit
:feature:profile            # Profile + composer + settings
:feature:hub                # Hub grid (manifest-driven)
:feature:nudges             # Conversation list + thread + voice + settings
:feature:kommons            # Proposals + vote + comments
:feature:compose            # Post composer + reach picker + krew picker + attachments
:feature:korner-card        # Card adapter registry (renders any korner card by adapter name from manifest)
:feature:walkthrough        # First-run bubbles
```

**Rendering.** Jetpack Compose. Material 3. Navigation Compose (`androidx.navigation:navigation-compose`). Single Activity. Manifest-driven route registration — the Hub screen reads `GET /api/v1/korners/manifests` and mounts routes for each korner it can render natively; the rest are marked as web-fallback (open in Custom Tab).

**DI.** Hilt for the app graph. `@HiltAndroidApp` on the Application, one `@InstallIn` module per `:core:*` module.

**State.** MVI-lite: each screen has a `ViewModel` exposing a `StateFlow<UiState>` and receiving `Intent` events. No Otto, no LiveData. Turbine in tests.

**Networking.** Retrofit + OkHttp 4 + kotlinx-serialization. One `MastodonApi` interface per resource (matches server-side controller shape). Bearer token via `AuthInterceptor`. `HttpLoggingInterceptor` in debug only.

**Persistence.** Room for anything list-cached (timelines, nudges, manifests). DataStore (proto) for global prefs. Per-account isolation via a Room `accountId` foreign key on every row (avoids the per-account-DB-file pattern which is brittle when account IDs change).

**Streaming.** OkHttp WebSocket wrapped in a Kotlin Flow. Reconnect with exponential backoff. Multiplexed channels (one WS per session, many subscriptions).

**Design tokens.** Mirror `tokens.yaml` into Compose values (`KronkColors`, `KronkRadii`, `KronkTypography`, `KronkDuration`, `KronkEasing`). One source-of-truth file per token category. Consider a small codegen step later so the app tokens stay in lockstep with the web `tokens.yaml` — deferred until we know it hurts.

**Icons.** Material Symbols (via `material-icons-extended` Compose artifact). Kronk-bespoke glyphs (rose, cinema, karporn, kronikles, zhong, etc.) ship as vendored SVGs converted to Compose `ImageVector`s. `useKornerIcon(slug)` becomes `rememberKornerIcon(slug)`.

**Auth.** OAuth (Custom Tab flow). Instance URL hardcoded to `kronk.info` (new host as of 2.0). Deep-link scheme stays `kronk-auth://callback`. Scopes constructed from union of `read write follow push` + any per-korner scopes advertised in manifests.

**Korner fallback.** Any korner without a native screen renders as a Chrome Custom Tab into the corresponding `https://kronk.info/hub/<slug>` route. The web SPA is already responsive and works well in a Custom Tab, so this is a first-class fallback rather than a placeholder. The card adapter registry (see below) still renders korner cards natively in the Home feed even for korners whose detail screens are Custom Tab.

## Phased plan

Each phase is a **shippable dev-APK checkpoint** — the app builds, signs, and lands at `kronk.info/dev/kronk.apk` at the end of every phase. No long-lived unstable branches.

### Phase 0 — Foundation (repo hygiene, no user-visible change)
- [ ] Merge this plan doc into `kronk-app/docs/rebuild/plan.md` via PR to `development`.
- [ ] Add a `docs/rebuild/` folder mirroring the web repo's structure.
- [ ] Land a `libs.versions.toml` version catalog and pin all deps.
- [ ] Add `spotless` + `detekt` + `ktlint` config; run in the pre-push hook.
- [ ] Bump OkHttp to 4.x on the legacy app — low-risk, unblocks WS client work.
- [ ] Decide on module names (this doc is a proposal; adjust before Phase 1).

### Phase 1 — Compose foundation module
- [ ] Create `:app` + `:core:designsystem` + `:core:model` + `:core:common`.
- [ ] Compose theme, typography, colours, shapes, elevation — mirror `tokens.yaml`.
- [ ] Primitives: `Stage`, `SpaceBadge`, `MembraneNav`, `KornerGlyph`, `StatusKornerCard`, `ComposeShell`.
- [ ] A new `MainActivity` gated by a new build flavour (`kronk2`) — flag off by default so the shipped app keeps working.
- [ ] Sample screen: Kronk-purple welcome pane with the rose emblem (visual smoke test that tokens + assets flow end-to-end).

### Phase 2 — Auth + minimal Home
- [ ] `:feature:auth` — OAuth Custom Tab, token storage in DataStore.
- [ ] `:core:network` — Retrofit + AuthInterceptor + `AccountApi`, `StatusApi`, `TimelineApi`.
- [ ] `:feature:home` — Home timeline (Mates + Kronk toggle), rendered with `StatusKornerCard` + a plain post card.
- [ ] Reach ladder mapped in domain: `Reach` sealed class, write-time translator for legacy visibility.
- [ ] End-of-phase: dev APK on `kronk2` flavour signs into shadow, shows the Home timeline.

### Phase 3 — Profile, compose, korner card adapters
- [ ] Profile screen + composer (reach picker + krew multi-select).
- [ ] Card adapter registry — reads `feed_projection.card` from manifest, dispatches to a Compose adapter component (per korner).
- [ ] Ship adapters for: `plain_status`, `proposal_card`, and 1-2 more depending on what the web ships.

### Phase 4 — Ж menu + korner manifest fetch + Custom Tab fallback
- [ ] `GET /api/v1/korners/manifests` fetcher + Room cache.
- [ ] Ж menu (draggable FAB, single entry point) — dropdown built from `compose.route` in each manifest. Composer routes with a native equivalent open native (Post, Nudge). All others open a Chrome Custom Tab into `https://kronk.info/hub/<slug>/composer`.
- [ ] Card adapter registry — reads `feed_projection.card` from each manifest, dispatches to a Compose adapter. Ship native adapters for `plain_status` + whichever cards render natively in Home feed. Unknown card types fall back to a generic `StatusKornerCard` that renders icon + title + summary + a "View on kronk.info" Custom-Tab button.
- [ ] **No native Hub screen** in v1 — the Hub pillar (bottom-nav "Hub" slot) opens a Custom Tab into `https://kronk.info/hub`. This is intentional (day-one scope decision); replace with a native grid only if user friction demands it.

### Phase 5 — Nudges (the biggest phase)
- [ ] `:feature:nudges` — conversation list, thread, message send, reactions.
- [ ] `:core:streaming` — WS client, `timeline:nudges:account:<id>` subscription, live-push handling.
- [ ] Voice recording (MediaRecorder → upload → send as audio nudge). Parity gate cleared.
- [ ] Nudge event routing — inline events (froth, RSVP, etc.) rendered as system messages in the thread.
- [ ] Per-conversation settings surface (mute / leave / notification prefs) matching the web.

### Phase 6 — Walkthrough
- [ ] First-run bubbles matching the web tour: Welcome Home (with rose emblem) → Home → Profile → Nudges → Ж → close. Skip the Hub bubble since Hub isn't a native pillar in v1 (or replace it with a "Discover more on kronk.info" bubble that opens a Custom Tab).

### Phase 7 — Cutover
- [ ] Feature-flag flip: `kronk2` flavour becomes the default `githubRelease` build. Legacy Java app renamed `kronk1_*` and put on maintenance-only.
- [ ] Retire the two AppCenter flavours (`appcenterPrivateBeta`, `appcenterPublicBeta`).
- [ ] Promote dev APK, cut a `v3.0.0` tag (major bump = major rewrite), let it deploy to `kronk.info/kronk.apk`.
- [ ] In-app updater shepherds existing users onto the new build.

## Resolved decisions (2026-09-16 with Tal)

| Question | Decision |
|---|---|
| Rewrite vs graft | Fresh Kotlin/Compose rewrite |
| Day-one native korners | Absolute minimum: Home + Profile + Nudges. Ж native. Hub + everything else = Chrome Custom Tab into `kronk.info` |
| Distribution | Keep kronk.info self-updater. Retire AppCenter flavours. No Play Store |
| Instance URL | Hardcoded to `kronk.info` (new host as of 2.0 rebrand). No picker |

## Still-open questions (low-stakes, resolvable in-line during Phase 0)

1. **Repo strategy.** Same repo (`Kronkverse/kronk-app`) with a new `kronk2` flavour, or a fresh sibling repo (`Kronkverse/kronk-app-2`)? Recommending same-repo — preserves release history, CI, distribution URLs, credentials. Confirm if you disagree.
2. **Design-token codegen.** Hand-mirror `tokens.yaml` into Compose values now and add a codegen step later if they drift? Recommending yes — one small file per token category, review at Phase 1.
3. **Federation posture.** Web 2.0 deprioritises federation (Nudges is local-first). App follows suit unless you say otherwise — meaning no ActivityPub-specific UI, no cross-instance follow (mates only within Kronk).
4. **In-app updater semantics.** Currently the updater reads `dev/version.json` and prompts the user to install. Keep the same UX for the rewrite, or move to a silent background download-on-Wi-Fi model? Recommending "keep same UX" for parity, revisit as a follow-up if users complain about prompts.
5. **New major version number.** I'm planning to cut `v3.0.0` at cutover (current is `v2.12.0`). Reasonable? Or would you prefer to keep counting from `v2.13.x` since the underlying service is still Kronk?

## Immediate next steps

Phase 0 is in flight as of this doc landing:

1. This PR — promotion of the plan into the repo (`docs/rebuild/plan.md`).
2. Follow-up PRs on `development`, small and reviewable:
   - `libs.versions.toml` version catalog.
   - Spotless + ktlint (Detekt as first Kotlin lands).
   - OkHttp 3.14 → 4.x bump.
3. Phase 1 begins when Phase 0 is green: `:core:designsystem` + Compose foundation + rose-emblem welcome pane behind the `kronk2` flavour.
4. **Blocker for Phase 2:** the `kronk.info` server cutover must have landed before auth can target it. Track via `Kronkverse/kronk#1859`.

## References

- Web rebuild integration branch: `Kronkverse/kronk/rebuild/2.0.0`
- Design decisions log: `~/kronk/docs/rebuild/decisions.md`
- Spec index: `~/kronk/docs/kronk_*.md`, `~/kronk/docs/spaces/*.md`, `~/kronk/docs/korners/*.md`
- Korner Standard: `~/kronk/docs/korners/korner_standard.md`
- Current app repo: `~/kronk-app/`, `Kronkverse/kronk-app`
- Current app internal guide: `~/kronk-app/CLAUDE.md`

---

## Coding behaviour (Karpathy rules)

Verbatim from https://github.com/multica-ai/andrej-karpathy-skills. **Tradeoff:** these bias toward caution over speed; for trivial tasks, use judgment. The rewrite is greenfield and moves fast — these rules keep the "greenfield" from turning into "kitchen sink."

### 1. Think Before Coding
Don't assume. Don't hide confusion. Surface tradeoffs.
- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First
Minimum code that solves the problem. Nothing speculative.
- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### 3. Surgical Changes
Touch only what you must. Clean up only your own mess.
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it — don't delete it.
- Remove imports/variables/functions that YOUR changes made unused; don't remove pre-existing dead code unless asked.

The test: every changed line should trace directly to the request.

### 4. Goal-Driven Execution
Define success criteria. Loop until verified.
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

If the codebase grows and these rules deserve their own home, split into `docs/rebuild/coding_rules.md` at that point.
