# Kronk App — Android Client

Custom-branded Mastodon client for the **kronk.info** instance. Based on the official Mastodon for Android app.

- **Package ID:** `info.kronk.app`
- **License:** GPL-3

## How development works here

**There is no emulator on this server.** You cannot run the app locally. The testing loop is:

1. Make changes on a feature branch
2. Build locally to catch compile errors: `./gradlew assembleBlueDebug 2>&1 | tail -30` (~5 min)
3. Push to your fork, open a draft PR to `development`
4. Once merged, GitHub Actions builds the signed dev APK (~10–15 min)
5. Install from **https://kronk.info/dev/kronk.apk** on a physical Android device
6. Test on device, iterate

This is slower than the web loop. Compile-check locally before pushing; keep PRs small so CI time is not wasted on obvious errors.

### Monitoring CI builds

After a merge to `development`:

```bash
gh run list --repo Kronkverse/kronk-app --limit 5
gh run view <run-id> --repo Kronkverse/kronk-app --log-failed
```

### Check the current deployed dev version

```bash
curl -s https://kronk.info/dev/version.json | python3 -m json.tool
```

### Features that also need web changes

If your feature calls an API endpoint that does not exist yet on the server side, build and merge the web change first. The dev APK talks to **mastodon.kronk.info** (production) by default. If you need to test against the staging backend (**dev.mastodon.kronk.info**), that requires a manual config change in the build.

---

## Branch Strategy

| Branch | Purpose | CI/CD |
|--------|---------|-------|
| `development` | Active development, PRs go here | Builds to kronk.info/dev/kronk.apk |
| `main` | Stable releases | Tag `v*` triggers release build |

Branch off `upstream/development` for all feature work. Never commit directly to `development` or `main`.

## Building (compile check only — no emulator)

Android SDK is pre-installed at `/opt/android-sdk/` with `ANDROID_HOME` already set.

```bash
# Compile check — catches errors before pushing
./gradlew assembleBlueDebug 2>&1 | tail -30

# Full debug build
./gradlew assembleDebug
```

Requirements: Java 17 (OpenJDK), Android SDK (compileSdk 35, minSdk 23), Gradle 8.5.

## Key Customizations

- **Deep links:** `kronk-auth://callback` for OAuth, `https://mastodon.kronk.info/@*` and `/invite/*` for app links
- **Self-updating:** App checks `kronk.info/version.json` for new versions
- **Account approval notifications:** `AccountApprovalCheckReceiver` polls for approval status
- **Kronk branding:** Custom app name, icons, colors
- **Hub spaces:** `HomeFragment` manages navigation between Feed, Huddle, ₭alendar, ₭ommons, Nudges
- **Compose UI:** `ui/compose/` — Jetpack Compose components for the Kosmos navigation layer

## CI/CD (GitHub Actions)

- **`build-dev.yml`** — Triggers on push to `development`. Builds signed APK, deploys to `kronk.info/dev/kronk.apk`.
- **`build-release.yml`** — Triggers on push of `v*` tag. Creates GitHub Release, deploys to `kronk.info/kronk.apk`.

Server-side cron polls GitHub releases every 5 minutes and updates the production APK.

## Release Process

1. Develop on `development` branch (auto-deploys dev build after merge)
2. Test via `kronk.info/dev/kronk.apk` on a physical device
3. Open PR: `development` to `main`
4. After merge, tag: `git tag vX.Y.Z && git push --tags`
5. GitHub Actions builds release, server picks it up automatically

## Contributing

1. Fork `Kronkverse/kronk-app` on GitHub (if not done: `gh repo fork Kronkverse/kronk-app --clone=false`)
2. Add your fork as a remote: `git remote add fork https://github.com/<you>/kronk-app.git`
3. Branch off upstream: `git fetch upstream && git checkout -b feature/my-change upstream/development`
4. Make changes, commit, push to your fork: `git push fork feature/my-change`
5. Open a draft PR to `development` on `Kronkverse/kronk-app`
6. Once tested on device, mark the PR ready for review

## Important Rules

- **Don't remove Kronk branding.** Custom icons, colors, app name, and deep link schemes must stay.
- **Don't break the self-update mechanism.** The `updater/` package and version check are critical.
- **Keep minSdk at 23** unless explicitly agreed otherwise.
- **Don't push directly to `development` or `main`.** Always go through a PR.

## Useful Links

- Production APK: https://kronk.info/kronk.apk
- Dev APK: https://kronk.info/dev/kronk.apk
- Issues: https://github.com/Kronkverse/kronk-app/issues
