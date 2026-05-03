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

- **`build-dev.yml`** — Triggers on push to `development`. Builds signed APK, deploys to `kronk.info/dev/kronk.apk`.
- **`build-release.yml`** — Triggers on push of `v*` tag. Creates GitHub Release, deploys to `kronk.info/kronk.apk`.

Server-side cron polls GitHub releases every 5 minutes and updates the production APK.

## Release Process

1. Develop on `development` branch (auto-deploys dev build)
2. Test via `kronk.info/dev/kronk.apk`
3. Merge `development` into `main`
4. Tag: `git tag vX.Y.Z && git push --tags`
5. GitHub Actions builds release, server picks it up automatically

## Contributing

1. Fork `Kronkverse/kronk-app` on GitHub
2. Branch off `development`
3. Make changes, commit, push to your fork
4. Open a PR to `development` on `Kronkverse/kronk-app`
5. After merge, dev build deploys automatically for testing

## Important Rules

- **Don't remove Kronk branding.** Custom icons, colors, app name, and deep link schemes must stay.
- **Don't break the self-update mechanism.** The `updater/` package and version check are critical.
- **Keep minSdk at 23** unless explicitly agreed otherwise.
- **Never tag or release without explicit confirmation.** Before running `git tag vX.Y.Z` or pushing any tag, state the exact version number and ask the user to confirm. Tagging immediately triggers CI and deploys to production within minutes — there is no safe window to abort. This applies even when the user says "ship it" or "release it"; always confirm the specific version first.

## Useful Links

- Production APK: https://kronk.info/kronk.apk
- Dev APK: https://kronk.info/dev/kronk.apk
- Issues: https://github.com/Kronkverse/kronk-app/issues
