# SENGKODE Final Release Readiness Validation Report

**Project:** SENGKODE - offline-first QR code generator (Android)
**Mode:** BUILD MODE OFF - validation and documentation only.
No source code, UI, Gradle configuration, QR logic, or rendering
logic was modified in this cycle. The only new file is this
report.
**Date:** September 20, 2026

## 1. Release Readiness Status

READY, pending the three owner inputs in section 6. The entire
automated gate is green end to end:

- `./gradlew --stop` before the session; every task with
  `--no-daemon`; no forced process termination.
- `./gradlew test assembleDebug bundleRelease --no-daemon` ->
  BUILD SUCCESSFUL (560 tasks).
- 190 test executions, 0 failures, 0 errors (full round-trip
  QR contract included).
- Debug APK: 17,200,684 bytes.
- Release AAB produced: 2,901,154 bytes (unsigned - expected,
  see section 3).
- Manifest permissions: 0. Lint errors: 0 (monochrome icon and
  launcher-shape warnings closed in Phase 5; remaining 30 are
  informational dependency-currency warnings).

## 2. Signing Status

- Workflow verified: the release buildType loads credentials
  ONLY from an uncommitted `keystore.properties` at repo root
  and signs automatically when it exists; without it, the build
  completes unsigned. No insecure defaults, no bypass.
- Secret scan of the FULL git history (all branches, all
  added-files ever committed): no .jks, no .keystore, no
  keystore.properties has ever been committed. CLEAN.
- `.gitignore` protection verified on the live repository:
  `keystore.properties`, `*.jks`, `*.keystore` all excluded.
- Documentation verified: docs/RELEASE_SIGNING.md covers
  keytool generation, properties file format, build and
  apksigner verification steps.
- No credentials were created or invented (per rules).

## 3. Versioning Status

Current (unchanged, per BUILD MODE OFF):

- versionName: 0.5.0-phase4
- versionCode: 5

Recommended release values (decision required, one-line
change when approved):

- versionName: 1.0.0
- versionCode: 6

## 4. Artifact Status

- `app-release.aab` (2.9 MB) builds successfully through the
  full Play-required AAB pipeline. UNSIGNED because no
  keystore exists in this environment - signing was not
  bypassed.
- After the keystore inputs are supplied, the same command
  produces a signed AAB, and `apksigner verify` /
  `jarsigner -verify` confirm it.
- Debug APK (17.2 MB) available for side-load smoke testing.

## 5. Device Validation Status

Not performed in this environment (no attached device, no
emulator, no system images; `adb devices -l` shows zero
devices, adb server killed per the safety rule). The hardware
checklist from the pre-release validation report (launch, QR
generation, save/share, history, templates, dark mode,
accessibility, font scaling) remains the OPEN device gate and
should be run once on real hardware with the debug APK or the
signed release build.

## 6. Remaining Blockers (exact missing inputs)

1. Production keystore: owner must run the keytool command in
   docs/RELEASE_SIGNING.md and place keystore.properties at the
   repository root (storeFile, storePassword, keyAlias,
   keyPassword).
2. Version bump approval: 1.0.0 / versionCode 6.
3. One hardware checklist pass (any real device).

## 7. Exact Next Actions Before Play Store Submission

1. Owner generates keystore + keystore.properties (Section 4 of
   docs/RELEASE_SIGNING.md; keep an offline backup).
2. Approve version bump -> 1.0.0 / 6 (one-line change in a
   short controlled cycle).
3. `./gradlew bundleRelease --no-daemon` -> signed AAB.
4. `apksigner verify --verbose app-release.aab` (or bundletool
   check) -> confirm signature.
5. Install the release build on a real device; run the device
   checklist (launch, QR types, save/share, history, templates,
   dark mode, TalkBack, font scale 200 percent).
6. Create the Play Console listing (privacy-first copy: zero
   permissions, fully offline), upload the signed AAB with a
   staged rollout, enable Play App Signing at listing setup.

**END OF FINAL RELEASE READINESS REPORT - BUILD MODE OFF
RESPECTED - NO FIXES APPLIED**
