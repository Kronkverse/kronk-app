# Kronk App — Android Client

Custom-branded Mastodon client for the **kronk.info** instance. Based on the official Mastodon for Android app.

- **Package ID:** `info.kronk.app`
- **License:** GPL-3

## Branch Strategy

| Branch | Purpose | CI/CD |
|--------|---------|-------|
| `development` | Active development, PRs go here | Builds to kronk.info/dev/kronk.apk |
| `main` | Stable releases | Tag `v*` triggers release build |

## Building

Requirements: Java 17 (OpenJDK), Android SDK (compileSdk 35, minSdk 23), Gradle 8.5.

```bash
# Debug build
./gradlew assembleDebug

# Release build (needs signing key)
./gradlew assembleRelease
```

Android SDK is pre-installed on the dev server at `/opt/android-sdk/` with `ANDROID_HOME` already set.

## Key Customizations

- **Deep links:** `kronk-auth://callback` for OAuth, `https://mastodon.kronk.info/@*` and `/invite/*` for app links
- **Self-updating:** App checks `kronk.info/version.json` for new versions
- **Account approval notifications:** `AccountApprovalCheckReceiver` polls for approval status
- **Kronk branding:** Custom app name, icons, colors
- **Events support:** `events/` package for Kronk event features

## CI/CD (GitHub Actions)

- **`build-dev-branch.yml`** — Triggers on every push to `feature/**`, `fix/**`, `docs/**`. Builds signed APK, deploys to `kronk.info/dev/<branch-slug>/kronk.apk`. Use this to test your branch before merging.
- **`build-dev.yml`** — Triggers on push to `development`. Builds signed APK, deploys to `kronk.info/dev/kronk.apk`.
- **`build-release.yml`** — Triggers on push of `v*` tag. Creates GitHub Release, deploys to `kronk.info/kronk.apk`.

Server-side cron polls GitHub releases every 5 minutes and updates the production APK.

## Release Process

1. Branch off `development` (e.g. `feature/my-thing`)
2. Push commits — APK auto-builds at `kronk.info/dev/feature-my-thing/kronk.apk`
3. Test via the branch APK — **do this before opening a PR**
4. When satisfied, open PR to `development` and merge
5. Merge to `main` + tag `vX.Y.Z` to cut a release

## Contributing

1. Fork `Kronkverse/kronk-app` on GitHub
2. Branch off `development` using `feature/`, `fix/`, or `docs/` prefix
3. Make changes, commit, push to your fork
4. **Test via the auto-built branch APK** at `kronk.info/dev/<branch-slug>/` — no merging needed
5. Open a PR to `development` once you're happy with it

## Important Rules

- **Don't remove Kronk branding.** Custom icons, colors, app name, and deep link schemes must stay.
- **Don't break the self-update mechanism.** The `updater/` package and version check are critical.
- **Keep minSdk at 23** unless explicitly agreed otherwise.

## Useful Links

- Production APK: https://kronk.info/kronk.apk
- Dev APK: https://kronk.info/dev/kronk.apk
- Issues: https://github.com/Kronkverse/kronk-app/issues
