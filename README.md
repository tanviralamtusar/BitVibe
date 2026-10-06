# BitVibe

An Android music player for musicians and learners: A-B segment looping, pitch-preserved speed
control, a 5-band equalizer, playlists, and browsing by artist, album and folder.

## Install on a phone

Open the [Releases page](https://github.com/tanviralamtusar/BitVibe/releases), take the latest
`bitvibe-<version>-<build>.apk`, and open it on the phone. Allow "install unknown apps" for your
browser when Android asks.

## App updates

Release builds update themselves from this repo's GitHub Releases (`data/update/UpdateManager.kt`):

1. When the app opens (at most every 30 minutes) it reads the latest release from the GitHub API.
   If the tag's build number is higher than the installed `versionCode`, it downloads the APK in the
   background (retrying up to 3 times).
2. **Android 12 and newer, after the first update:** the update installs silently, with no prompt,
   when you leave the app and nothing is playing. You come back to the new version.
3. **The first update, and Android 11 and older:** Android requires one confirmation tap, so the app
   shows **Update ready → Install**. The very first time it also asks you to allow "Install unknown
   apps" for BitVibe.

Silent installs work once BitVibe is its own "installer of record". An APK installed from a browser
or file manager is owned by that app, so the first update needs the tap and later ones don't.

**Settings → Updates** shows the version, has **Check for updates**, and an **Auto-update** switch.
With auto-update off, new versions are offered with **Update now / Later**. Debug builds never
self-update (different signing key). The check is unauthenticated, so **the repo must stay public**,
and the updater parses the release tag, so keep the `android-v<version>-b<build>` format. Every
release must be signed with the same keystore, otherwise Android refuses the update.

## Build & release

### GitHub Actions (default)

`.github/workflows/android-apk.yml` runs on every push to `main` that touches the app (and on demand
from the Actions tab). It:

1. runs the unit tests and `assembleRelease`, with the workflow run number as `versionCode`;
2. signs the APK with your keystore if the secrets exist, otherwise with a throwaway key;
3. uploads it as an artifact and publishes it as the **latest GitHub Release**, tagged
   `android-v<version>-b<build>` (e.g. `android-v1.0-b7`), with the commits since the previous
   release as notes.

Pull requests into `main` run the same build and upload the APK as an artifact, without releasing.

**Signing.** Android only installs an update over an app signed with the same key. Create your own
key once, before sharing the app, and add it in **Settings → Secrets and variables → Actions → Secrets**:

```bash
keytool -genkeypair -v -keystore bitvibe-release.jks -alias bitvibe -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 bitvibe-release.jks   # paste the output into ANDROID_KEYSTORE_BASE64
```

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | the keystore, base64-encoded |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | `bitvibe` (the alias above) |
| `ANDROID_KEY_PASSWORD` | key password |

Keep `bitvibe-release.jks` and its passwords backed up outside the repo. If you lose them, you can
never publish an update to the same app.

**Versions.** Bump `versionName` in `app/build.gradle.kts` for a new user-visible version; the build
number goes up on its own.

### Local

```bash
./gradlew assembleDebug    # app/build/outputs/apk/debug/app-debug.apk
```

## Contributing

See `.agents/rules/git-rules.md`: Conventional Commits, `main` is production, and larger work goes
through `feature/*` / `fix/*` branches merged by PR.
