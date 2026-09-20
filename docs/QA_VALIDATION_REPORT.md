# SENGKODE QA Validation Report - Phase 2 UX (Post-Implementation)

**Project:** SENGKODE - offline-first QR code generator (Android)
**Mode:** VALIDATION ONLY (BUILD MODE OFF - no project files
modified except this report; no fixes applied)
**Scope:** Phase 2 Create/Studio screen UX modernization
**Environment note:** validation ran on a Linux CI-style sandbox,
not Windows; all Gradle safety rules were followed in spirit
(--stop executed first, --no-daemon mandatory, no forced process
kills). No Android device/emulator is attached to this environment;
device validation is listed as NOT PERFORMED.
**Encoding:** ASCII-safe

---

## 1. Validation Summary

Overall verdict: **PASS with minor, well-scoped findings.** The
Phase 2 implementation is structurally sound, behavior-preserving,
and fully green on the automated gate. Two accessibility touch-
target findings and a few cleanliness items are documented below
for a future controlled BUILD MODE ON fix cycle. None block the
release path, but items V-1 and V-2 should be fixed before the
1.0 release.

## 2. Build Verification Results

- `./gradlew --stop` executed first: no daemons were running.
- `./gradlew test assembleDebug assembleRelease --no-daemon`:
  **BUILD SUCCESSFUL** (663 tasks; task outputs up-to-date from
  the same source state, which is the definition of a green
  incremental gate).
- Debug APK: 17,199,765 bytes. Release (R8) APK: 1,853,323 bytes.
- Warnings: none emitted in this (fully up-to-date) run.
- Manifest permissions: 0. TODO/FIXME/STOPSHIP markers: 0.

## 3. Test Verification Results

- 190 test executions, 0 failures, 0 errors (debug+release
  variants), including the full QR round-trip contract
  (generate -> render -> decode -> exact payload) and all Phase
  1-4 suites.

## 4. Device Validation Results

NOT PERFORMED - no device or emulator is attached to this
environment and adb is not applicable here. The checklists below
(create screen flows, responsive matrix, TalkBack) remain open
and are the top recommended next step on a real device
(Samsung mid-ranger recommended, given known OEM quirks).

## 5. UI Issues Found

- **V-1 (Medium, accessibility):** color swatches are custom
  44dp Box click targets. Material 3 components auto-enforce a
  48dp minimum interactive size, but a raw Box does NOT - the
  swatch touch target is 44dp, below the 48dp guideline.
  Fix when BUILD MODE ON: add
  `defaultMinSize(minimumWidth = 48.dp, minimumHeight = 48.dp)`
  or `minimumInteractiveComponentSize()` to the swatch modifier.
- **V-2 (Low-Medium, accessibility):** expandable row headers
  (Colors/Modules/Eyes/Logo/Frame) are clickable Rows with 12dp
  vertical padding around a labelLarge text - effective height
  roughly 44-46dp, borderline below the 48dp guideline. The main
  "Customize appearance" header (titleMedium + 4dp padding) is
  likely ~32-40dp. Fix: same minimum-interactive treatment.
- **V-3 (Low, cleanliness):** three now-unused string resources
  remain: `preview_cd`, `preview_unsafe_cd`, `logo_too_large`
  (superseded by `cd_qr_preview` and inline verification badge;
  `logo_too_large` was never wired). Harmless; dead code.
- **V-4 (Cosmetic, code style):** `FilledTonalButton` and one
  `OutlinedButton` are used via fully-qualified names in
  GeneratorScreen.kt while siblings use imports. Cosmetic
  inconsistency only.

Behavioral spot-checks (static): all good - section expansion uses
rememberSaveable (survives config change/process death), the eye
color dialog now has a correct reopen path, hex validation and the
ScanSafetyPolicy gates are untouched, collapsed-by-default
Customize section reduces initial composition cost.

## 6. Accessibility Issues Found

- V-1 and V-2 above (touch targets) are the real findings.
- Swatches DO expose contentDescription ("Color preset <name>")
  plus selected/unselected stateDescription - correct, not
  color-only.
- Expandable headers expose Button role + expanded/collapsed
  stateDescription - correct for TalkBack.
- Segmented controls (ECC, module shape, eye shape) use Material
  3 semantics with automatic minimum touch size - compliant.
- Switch rows, slider description, and the logo verification
  polite live region are preserved from Phase 4 - compliant.
- Remaining open question for on-device TalkBack testing: reading
  order of the hero preview + badge, and focus traversal into the
  collapsed Customize section (needs a real screen reader pass).

## 7. Performance Observations

- No regressions: rendering stays off the main thread with the
  same LaunchedEffect keys; bitmap lifecycle unchanged.
- Improvement: five style sections start collapsed, so the
  default composition is lighter than before.
- No new allocations, image loading, or caches introduced.
- Release APK grew from 1.79 MB (Phase 3) to 1.85 MB -
  proportional to the added Phase 4 + UX code, expected.

## 8. Release Risks

1. Touch-target findings V-1/V-2 are minor but user-visible for
   people with motor impairments; fix before 1.0.
2. Device validation has never been executed on real hardware;
   until a device pass is done, layout-at-font-scale-200 claims
   rest on Compose/M3 defaults, not observation.
3. Release APK is still unsigned (Phase 5 scope, unchanged).

## 9. Recommended Next Steps

1. Fix V-1 and V-2 (+ optionally V-3, V-4) in one controlled
   BUILD MODE ON micro-cycle; rerun the 190-test gate.
2. Run the device/emulator checklist from this report on real
   hardware: startup, create-screen flows, dark/light, font
   scale 200 percent, keyboard open, long content, TalkBack.
3. Then proceed to the Phase 5 release track: signing, version
   1.0.0, Play Store listing.

**END OF QA VALIDATION REPORT - BUILD MODE OFF RESPECTED**
