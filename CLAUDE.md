# CLAUDE.md

BitVibe is a native Android music player (Kotlin, Jetpack Compose, Media3, Hilt, Room) in `app/`.

## Commands

```bash
./gradlew assembleDebug                 # debug APK
./gradlew testReleaseUnitTest assembleRelease   # what CI runs
```

Releases are built by GitHub Actions (`.github/workflows/android-apk.yml`) on every push to `main`
that touches the app; there's no local release step. See README.md → Build & release.

## Git

Follow `.agents/rules/git-rules.md`: Conventional Commits (`feat:`, `fix:`, `refactor:`, `chore:`, `docs:`),
`main` is production, and larger work goes through `feature/<name>` or `fix/<name>` branches merged by PR.
PRs should explain the user-visible change, include screenshots for UI work, and state how it was tested.
