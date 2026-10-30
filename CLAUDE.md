# Kronk App — Android

The Android app is a **Trusted Web Activity (TWA)**: an APK that opens Kronk
full-screen in the phone's browser engine (Chrome, or another that supports
TWAs). **Everything Kronk does lives on the web** (`Kronkverse/kronk`), so
new features, korners and fixes reach the app with each deploy and need no
work here. This repo only names the app, picks the host, carries the icons
and builds the APK. Decided 2026-10-29 (`docs/decisions.md` in the web repo).

**Don't build features here.** If the app needs to do something, the answer is
almost always a web feature (push, sharing in, camera, files are all web APIs
that work in the TWA). Native code is for what the web can't reach at all.

The Mastodon-based app this replaced is on the `legacy/mastodon-fork` branch.

## The two apps

| Flavor   | Package                 | Opens                       | Signed with                         |
| -------- | ----------------------- | --------------------------- | ----------------------------------- |
| `live`   | `info.kronk.app`        | `https://kronk.info`        | release key (`KEYSTORE_*` secrets)  |
| `shadow` | `info.kronk.app.shadow` | `https://shadow.kronk.info` | shadow key (`SHADOW_KEYSTORE_*`)    |

They install side by side. Both keys **must** be listed in the web repo's
`public/.well-known/assetlinks.json`; if a key is missing there, the app opens
with a browser address bar instead of full-screen. All host-specific values are
in `productFlavors` in `app/build.gradle`.

The app opens `/?source=app&app=<versionCode>`, which is how the site knows it
is inside the app and which build it is.

## Version

`appVersionName` / `appVersionCode` at the top of `app/build.gradle`. The app
has **its own version, named by year.month** (`2026.11` → `20261100`), not
Kronk's (which is 2.x, with 3.0 reserved for Kronk-native federation). The
code must always go up and must stay above 21401, the legacy app's last code,
or old installs won't take the update. Bump it only when the shell changes,
which should be rare.

## Branches and builds

PRs go into **`development`**. CI (all in `.github/workflows/build-apk.yml`,
called by the others) publishes to kronk.info:

| Trigger                                 | Builds  | Lands at                                    |
| --------------------------------------- | ------- | ------------------------------------------- |
| push to `feature/**` `fix/**` `docs/**` | shadow  | `kronk.info/dev/<branch-slug>/kronk.apk`    |
| push to `development`                   | shadow  | `kronk.info/dev/kronk.apk`                  |
|                                         | live    | `kronk.info/dev/live/kronk.apk`             |
| tag `v<appVersionName>` (e.g. `v2026.11`) | live  | `kronk.info/kronk.apk` + `version.json`, GitHub release |

**A tag is a production release.** `kronk.info/version.json` is read every
6 hours by the legacy app, which then offers its users the update, so a tag
moves every installed phone. Only Tal tags, and only a commit on `main`.

**Order at release:** the web release carrying the release key in
`assetlinks.json` must be live on kronk.info first. The release build checks
and stops if it isn't; otherwise every migrated phone would open with an
address bar.

Pushing a workflow change needs the `workflow` scope on your GitHub login
(`gh auth refresh -h github.com -s workflow`).

## Building locally

```bash
./gradlew assembleShadowRelease   # or assembleLiveRelease
```

Needs JDK 17 and an Android SDK with platform 36 and build-tools 35. On
mainframe `/opt/android-sdk` is read-only and older; use a personal SDK that
links the shared parts and adds the rest with `sdkmanager --sdk_root=…`,
then `export ANDROID_HOME=<that dir>`. Local builds are unsigned; to try one on
a phone, push a branch and use its kronk.info/dev link.

## Rules

- **minSdk 24** (Android 7.0), the TWA library's floor.
- **Keep the package id `info.kronk.app` and the release key.** Changing either
  strands every installed phone.
- Don't regenerate the project with Bubblewrap over these changes.
