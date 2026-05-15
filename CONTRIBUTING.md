# Contributing to the Kronk App

The Kronk app is a custom Android client for [mastodon.kronk.info](https://mastodon.kronk.info), based on the official [Mastodon for Android](https://github.com/mastodon/mastodon-android) app.

## Getting Started

1. **Fork** this repo on GitHub
2. **Clone** your fork locally
3. **Branch** off `development` (e.g. `git checkout -b feature/my-change`)
4. **Make your changes**
5. **Push** to your fork and **open a PR** against `development`

## Branch Structure

| Branch | Purpose |
|--------|---------|
| `main` | Stable releases (tagged `vX.Y.Z`) |
| `development` | Active development — auto-deploys dev APK |

PRs should target `development`. When ready for release, `development` is merged to `main` and tagged.

## Build Setup

This is a standard Android project. Open it in Android Studio and build:

```bash
./gradlew assembleRelease
```

### Key Config

| Item | Value |
|------|-------|
| Package ID | `info.kronk.app` |
| Min SDK | See `build.gradle` |
| Deep link scheme | `kronk-auth://` |

## CI/CD

- **Push to `development`** → GitHub Actions builds a dev APK, deploys to `kronk.info/dev/kronk.apk`
- **Push tag `vX.Y.Z`** → GitHub Actions builds a release APK, creates a GitHub Release, and the server picks it up automatically

You don't need to worry about deployment — just get your code into a PR.

## What We're Looking For

- Bug fixes
- UI/UX improvements
- New features that complement the Kronk Mastodon instance
- Performance improvements
- Accessibility improvements

## What to Avoid

- Breaking changes to the self-update mechanism
- Removing Kronk branding or deep link handling
- Large refactors without prior discussion in an issue

## Questions?

Open an issue if you're unsure about anything.
