# Release Checklist — v1.0.0

Checklist for producing, signing, testing, and publishing the **Attendance Tracker** v1.0.0 release build.

---

## 1. Pre-build checks

- [ ] Working tree clean / desired changes committed on `main`.
- [ ] `./gradlew :app:testDebugUnitTest` green (116 tests).
- [ ] `./gradlew :app:lintDebug` has no new errors (one known pre-existing
      `RemoveWorkManagerInitializer` warning may appear; it is a Phase 6
      artifact and non-blocking).
- [ ] `./gradlew :app:assembleDebug` green.
- [ ] Manual smoke test on a physical device and an emulator:
  - Onboarding → Dashboard → add subject → add schedule → mark attendance.
  - OCR import, simulator, leave planner, notification settings.
  - Backup / restore / archive / semester reset / integrity scan.
  - Home-screen widget: add (2x2), verify today's data + overall %, press
    Dashboard / Attendance / Refresh, confirm deep links land on the right tab
    and history opens without splash.

## 2. Release signing

Create `keystore.properties` in the project root (never commit it):

```properties
storeFile=/absolute/path/to/release.keystore
storePassword=********
keyAlias=release
keyPassword=********
```

- [ ] Generate the keystore with `keytool` if it does not exist yet.
- [ ] Back up the keystore and store the passwords in a secure vault.
- [ ] When `keystore.properties` is absent, `assembleRelease` falls back to the
      debug keystore (installable but **not** suitable for Play upload) —
      ensure the real keystore is present before publishing.

## 3. Build & verify the release APK

```bash
./gradlew :app:assembleRelease
```

- [ ] Confirm `app/build/outputs/apk/release/app-release.apk` exists.
- [ ] Confirm the APK is signed with the release key:
  ```bash
  apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
  ```
- [ ] Install on a device and smoke-test the same flows as step 1
      (release build is minified/R8-obfuscated).
- [ ] Optional: run `./gradlew :app:bundletool` / produce an AAB with
      `./gradlew :app:bundleRelease` for Play upload.

## 4. Baseline profile (recommended, v1.1 follow-up)

The `:benchmark` module scaffolds profile generation against a release-like
app variant:

```bash
./gradlew :benchmark:connectedCheck
```

- [ ] Copy the generated profile into `app/src/main/baseline-prof.txt`.
- [ ] Rebuild `:app:assembleRelease` so the profile is bundled.
- [ ] A curated `baseline-prof.txt` is already committed for the v1.0.0 launch.

## 5. Store listing & legal

- [ ] `docs/release/PlayStoreListing.md` — short/long description, feature
      bullets, category, tags.
- [ ] `docs/release/PrivacyPolicy.md` — publish at a public URL and link it in
      the Play Console data-safety form.
- [ ] Data-safety form: **no data collected / no internet permission**, data is
      stored locally on-device only.
- [ ] Screenshots (phone + tablet) and a feature graphic (1024×500).
- [ ] App icon set from the design system.

## 6. Publishing

- [ ] Upload the AAB to Play Console → Internal testing → promote to Closed →
      Open testing → Production.
- [ ] Track versionCode bumps: next release = `versionCode 2`, `versionName 1.0.1`+.
- [ ] Tag the release commit in git (`git tag v1.0.0`).

---

## Rollback

Keep the previous tagged release (`git tag`) and the previous AAB for quick
rollback in Play Console. Never re-sign a previously uploaded AAB with a
different key.
