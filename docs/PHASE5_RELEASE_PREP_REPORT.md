# SENGKODE Phase 5 Release Preparation Report

**Project:** SENGKODE - offline-first QR code generator (Android)
**Mode:** BUILD MODE ON - release preparation only
**Scope:** L-1 monochrome launcher icon fix, secure release
signing preparation, artifact generation and validation.
**Date:** September 20, 2026

## 1. Files Modified / Added

1. `app/src/main/res/drawable/ic_launcher_monochrome.xml`
   (NEW) - Android 13+ themed icon layer.
2. `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (M) -
   added the monochrome layer reference.
3. `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
   (M) - same.
4. `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/
   ic_launcher.png` + `ic_launcher_round.png` (NEW, 10 files) -
   legacy raster fallbacks for API 24-25 (minSdk 24 had NO raster
   launcher icon before; adaptive icons require API 26+).
5. `app/build.gradle.kts` (M) - release signingConfig that reads
   an uncommitted keystore.properties when present.
6. `.gitignore` (NEW) - keystore.properties, *.jks, *.keystore
   plus standard Gradle/AndroidStudio ignores. No .gitignore
   existed before; this closes a real release-secret risk.
7. `docs/RELEASE_SIGNING.md` (NEW) - step-by-step keystore guide.
8. This report.

## 2. Reason for Each Modification

- L-1 (from the final pre-release validation report): the
  launcher icon had no monochrome layer, so Android 13+ themed
  icons fell back to a default tinted shape. Fixed without
  changing existing appearance.
- Legacy gap discovered during Phase 5 source inspection: minSdk
  is 24 but only mipmap-anydpi-v26 icons existed, leaving Android
  7.x without a launcher icon. Raster fallbacks restore correct
  branding on API 24/25. This is icon-resource-only work allowed
  by the release-prep scope ("legacy Android versions still
  work").
- Signing: the release build was unsigned with no signing path.
  A standard, secure pattern is now wired: it activates only when
  a local, gitignored keystore.properties exists.

## 3. Monochrome Icon Implementation Details

- `ic_launcher_monochrome.xml` reuses the exact QR finder-pattern
  motif of the existing foreground vector, recolored to a single
  solid (system tints themed icons by alpha, not color).
- Both adaptive-icon XMLs (square + round) now declare
  `<monochrome android:drawable="@drawable/ic_launcher_monochrome" />`
  after the foreground layer - the supported Android 13+ pattern.
- Existing foreground/background/round appearance unchanged; no
  branding change; no runtime behavior change; no dependencies.
- Legacy rasters were rendered from the same motif (white QR
  pattern on the #00695C teal), with a 6 percent transparent
  margin so they satisfy the IconLauncherShape design guideline.

## 4. Signing Configuration Status

- `keystore.properties` pattern implemented in the release
  buildType; NO credentials exist or were invented, NOTHING
  secret is committed, and the build remains unchanged (still
  unsigned) until the owner supplies a real keystore.
- Required from the owner (documented in
  docs/RELEASE_SIGNING.md):
  1. one-time `keytool -genkeypair` (RSA 4096, validity 10000),
  2. `keystore.properties` at repo root with storeFile,
     storePassword, keyAlias, keyPassword,
  3. offline backup of keystore + passwords.
- After that, `assembleRelease` / `bundleRelease` sign
  automatically; `apksigner verify` confirms.

## 5. Gradle Commands Executed

All per the safety rules: `./gradlew --stop` before each session;
every task ran with `--no-daemon`; no forced process termination.

- `./gradlew test assembleDebug assembleRelease --no-daemon`
- `./gradlew :app:assembleRelease --no-daemon`
- `./gradlew test assembleDebug :app:lintRelease --no-daemon`
- `./gradlew :app:assembleRelease :app:lintRelease --no-daemon`

## 6. Build Results

- Debug APK: 17,200,684 bytes. BUILD SUCCESSFUL.
- Release (R8) APK: 1,884,811 bytes. BUILD SUCCESSFUL.
- Lint release: 0 errors; MonochromeLauncherIcon warnings 2 -> 0
  (L-1 CLOSED); IconLauncherShape 0; remaining 30 warnings are
  pre-existing dependency-currency informational only
  (deliberately out of scope: no dependency upgrades allowed
  this phase).

## 7. Test Results

- 190 test executions, 0 failures, 0 errors - including the full
  QR round-trip contract and all Phase 1-4 suites. No QR
  regression, no rendering regression, no export regression.

## 8. Release Artifact Status

- `app-release-unsigned.apk` regenerated and validated. Signing
  remains blocked ONLY on the owner's keystore (see section 4).
- Play submission will additionally need `bundleRelease` (AAB)
  and a version bump decision (current: 0.5.0-phase4 /
  versionCode 5; recommending 1.0.0 / 6 at the actual release).

## 9. Remaining Blockers

1. Owner must generate and place the signing keystore (no code
   blocker).
2. Version bump for the actual 1.0.0 release (one-line change,
   awaiting decision).
3. The hardware device checklist from the final validation report
   is still open (no device in this environment).

## 10. Confirmations

- No UI changes: YES (only launcher icon resources)
- No architecture changes: YES
- No dependency upgrades / Gradle changes: YES
- QR behavior unchanged: YES (190 tests green)
- Rendering unchanged: YES
- Database unchanged: YES
- Navigation unchanged: YES
- No secrets in version control: YES (gitignored pattern)
- Manifest permissions: still 0

**END OF PHASE 5 RELEASE PREPARATION REPORT**
