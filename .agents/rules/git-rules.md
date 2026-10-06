---
trigger: always_on
---

## Git Rules

### Branch Strategy
```
main          → the only long-lived branch: production (every push touching the app
                publishes a new Android release)
feature/*     → short-lived, branched from main, merged back by PR
fix/*         → short-lived bug fixes, same flow
```

There is no `develop`/staging branch. Remember that a push to `main` ships: the
`Android APK` workflow builds, signs and publishes the APK as the latest GitHub Release.

### Commit Messages
```
feat: add A-B loop fine-tune buttons
fix: keep album art after track change
refactor: extract playlist repository
chore: bump media3 to latest version
docs: describe release signing in README
```

### Workflow
```bash
git checkout main
git pull origin main
git checkout -b feature/sleep-timer
# ... make changes ...
git add .
git commit -m "feat: add sleep timer"
git push origin feature/sleep-timer
# Create PR → CI builds the APK (downloadable from the run) → test it on a phone → merge to main
```
