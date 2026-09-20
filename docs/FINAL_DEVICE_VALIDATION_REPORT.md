# SENGKODE Final Pre-Release Device Validation Report

**Project:** SENGKODE - offline-first QR code generator (Android)
**Mode:** BUILD MODE OFF - validation and observation only.
**No source code, UI, tests, or configuration were modified in
this cycle.** The only new file is this report.
**Date:** September 20, 2026

## 1. Device / Environment Tested

- Environment: isolated Linux CI-style sandbox (headless).
- Android SDK present (platforms, build-tools, platform-tools).
- `adb devices -l` executed per the adb safety rule, chained
  with `adb kill-server`: **0 devices attached**.
- No emulator binary and no system images are installed in this
  environment.
- **Consequence: on-device / on-emulator validation (the device
  checklist, responsive matrix, TalkBack pass, and performance
  observation) could NOT be executed here.** This is recorded as
  an OPEN item, not skipped by choice. The items below are what
  could be validated in this environment, plus static-analysis
  proxies for the rest.

## 2. Build Verification Status

- `./gradlew --stop` first: no daemons running.
- `./gradlew test assembleDebug assembleRelease --no-daemon`:
  **BUILD SUCCESSFUL** (663 tasks, all up-to-date from the same
  source state - a green incremental gate).
- Debug APK: 17,199,765 bytes. Release (R8) APK: 1,853,323 bytes.
- Manifest permissions: 0. TODO/FIXME/STOPSHIP markers: 0.

## 3. Test Verification Status

- **190 test executions, 0 failures, 0 errors**, including the
  full round-trip contract (generate -> render -> decode ->
  assert exact payload) for all content types, ECC levels, style
  presets, custom colors, module/eye styles, logo, frame, export,
  history, and templates.

## 4. UI Validation Results

Executed (automated, in-suite): Robolectric UI tests for the
studio screen, history, and templates - all green. State
behavior verified: expansion state survives configuration
changes, eye-color dialog reopen path, hex validation, safety
gating of preview and export.

NOT executed (requires hardware): visual rendering of the hero
preview, dark/light theme appearance, font-scale-200 layout,
keyboard-open behavior, long-text input observation, chip
overflow on narrow screens, bottom-navigation inset behavior.

## 5. Accessibility Validation Results

- Static verification (code inspection, unchanged this cycle):
  swatches and expandable headers now meet the 48dp minimum
  interactive size; selected/unselected stateDescriptions,
  Button role + expanded/collapsed announcements, switch and
  slider semantics are all in place.
- Android Lint release analysis: **0 accessibility errors**.
- NOT executed: on-device TalkBack navigation, live screen-
  reader announcements, and touch-target behavior under real
  input. These remain part of the open hardware pass.

## 6. Performance Observations

- Static (no profiling, no code changes): QR rendering remains
  off the main thread with the existing debounce and bitmap
  lifecycle; sections start collapsed, keeping default
  composition light.
- NOT measured: preview update latency, scroll performance, and
  memory behavior on hardware. No symptoms can be observed
  without a device; none are implied.

## 7. Bugs Found

None blocking. One genuine pre-release finding from Lint:

- **L-1 (Low):** `MonochromeLauncherIcon` - the launcher icon
  lacks a monochrome layer, so Android 13+ themed icons fall
  back to a default tinted shape. Cosmetic; document for a
  future controlled fix cycle (icon asset only, no logic).

Informational (no action required for release): 30 dependency
and Gradle-plugin currency warnings (newer versions exist for
Compose BOM, Room, lifecycle, activity, navigation, core-ktx,
AGP). All pinned versions are stable and fully tested; upgrades
belong to a deliberate future dependency-refresh cycle, not to
this validation.

## 8. Release Risks

1. No hardware pass has ever been executed (layout, TalkBack,
   performance, OEM quirks such as Samsung aggressive power
   saving are unobserved on-device).
2. Release APK is unsigned - Phase 5 signing required.
3. L-1 monochrome icon (cosmetic, Android 13+ only).
4. Dependency currency is behind latest stable (deliberate,
   tested pins; upgrade at leisure post-1.0).

## 9. Recommendation for Phase 5 Release Readiness

**CONDITIONAL GO.** The automated gate is fully green (190
tests, debug + release builds, 0 lint errors, 0 permissions) and
the QA findings from previous cycles are closed. Recommended
order:

1. Execute the device checklist from this report once, on real
   hardware (Samsung mid-range + Pixel emulator covers the risk
   matrix), including TalkBack and font-scale 200 percent.
2. Proceed to Phase 5: signing config, version 1.0.0, Play
   listing. Fix L-1 (monochrome icon) inside Phase 5 if asset
   time allows.

**END OF FINAL DEVICE VALIDATION REPORT - BUILD MODE OFF
RESPECTED - NO FIXES APPLIED**
