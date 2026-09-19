# SENGKODE Phase 2 Report - Create/Studio Screen UX Modernization

**Project:** SENGKODE - offline-first QR code generator (Android)
**Mode:** In-place Compose UI/UX modernization (BUILD MODE ON)
**Version:** 0.5.0-phase4 code base, UI-only changes
**Verification:** `./gradlew test assembleDebug assembleRelease
--no-daemon` -> BUILD SUCCESSFUL. 190 test executions, 0 failures
(the full pre-existing suite - engine round-trips, style policies,
rendering, database, view models - is the regression gate).
**Encoding:** ASCII-safe. 0 TODO/FIXME/STOPSHIP, 0 permissions.
**Date:** September 19, 2026

---

## 1. Summary of Implemented Changes

The Create/Studio screen was restructured from a flat, always-open
"settings panel" into a consumer-grade flow with clear hierarchy:

Preview hero -> QR type -> Content -> Error correction ->
Customize appearance (collapsed) -> Primary actions.

- **Preview hero first:** the QR preview is now the first element
  (visual anchor: "what am I creating?"), on a bordered tonal
  surface with a responsive 360 dp max width and a 300 dp image
  cap, 1:1 ratio, quiet zone unchanged (renderer-owned).
  Empty state ("Enter content to generate a QR code"), unsafe-style
  reasons, and validation errors keep their existing behavior.
- **Customize appearance - progressive disclosure:** the five
  former always-visible cards (Colors, Modules, Eyes, Logo, Frame)
  now live inside ONE collapsed-by-default section with compact
  expandable rows (AnimatedVisibility, rememberSaveable state).
- **Colors:** 8 visual swatches (Classic/Teal/Indigo/Crimson/
  Emerald/Navy/Coffee/Charcoal - the shipped, policy-validated
  presets) with check-mark + border selection indicator and
  selected/unselected semantics. Custom hex editing moved behind
  a "Custom colors" dialog that retains both fields and hex
  validation. ScanSafetyPolicy is untouched and still gates the
  preview/export.
- **Modules and Eyes:** compact Material 3 segmented single-choice
  controls. Eye color keeps "Match modules" plus a custom hex
  dialog (with a fixed reopen path).
- **Logo and Frame:** settings-row pattern preserved; secondary
  controls hidden while disabled. Logo verification badge and
  export blocking are unchanged.
- **Error correction:** L/M/Q/H replaced with a Material 3
  segmented control; the effective-ECC ("auto-upgraded for logo")
  note is preserved.
- **Action hierarchy:** primary "Save to gallery" (filled) and
  "Share" (filled tonal) side by side; secondary "Save to
  history" and "Save style as template" as an outlined row. All
  behaviors, gating (canRender), toasts and dialogs unchanged.

## 2. Files Modified (and why)

1. `feature/generator/.../GeneratorScreen.kt` - hierarchy reorder,
   preview hero surface, segmented ECC, action hierarchy.
2. `feature/generator/.../StudioSections.kt` - one collapsed
   Customize section, expandable rows, color swatches + hex
   dialog, segmented shape/eye controls, eye-color dialog fix.
3. `feature/generator/src/main/res/values/strings.xml` - new
   labels/state strings (customize, expanded/collapsed,
   selected/unselected, custom colors dialog).
No other file was modified (verified: git status clean elsewhere).

## 3. Explicit Confirmations

- XML was NOT introduced (project stays 100 percent Compose/M3).
- ViewModel behavior preserved (StudioViewModel untouched).
- QR rendering pipeline preserved (same renderer, same keys,
  preview still rendered off the main thread; no rendering in
  composition).
- QR payload behavior preserved (engine, encoders untouched).
- Database behavior preserved (no database file touched).
- Navigation behavior preserved (AppRoot, MainActivity untouched).
- Existing tests not deleted or weakened - all 190 still green.

## 4. Accessibility

- Swatches expose "Color preset <name>" + selected/unselected
  stateDescription (not color-only information).
- Expandable headers expose Button role + expanded/collapsed
  stateDescription for TalkBack.
- Segmented controls carry Material 3 selected semantics; 48 dp
  minimum interactive size applies to all chips/segments.
- Logo slider content description and verification live region
  (Phase 4) preserved.

## 5. Performance Considerations

- Generation debounce, background rendering, bitmap recycling and
  LaunchedEffect keys unchanged; sections start collapsed so
  initial composition is cheaper.
- rememberSaveable only for genuine UI-local state (section
  expansion); no state moved into the ViewModel.

## 6. Known Limitations / Intentionally NOT Done

- No sticky bottom action bar: kept at the end of the scroll to
  avoid NavigationBar/inset/IME conflicts (prompt explicitly
  allowed this safer choice).
- No new dependencies, no third-party color picker, no new frame
  formats, no auto-save behavior changes.
- Real-device manual validation (TalkBack pass, font-scale sweep)
  recommended before the 1.0 release; the debug APK is attached
  for that pass.

## 7. Recommendation

Proceed with device validation, then Phase B (History/Templates
polish - optional) and the release/signing phase.

**END OF PHASE 2 UX REPORT**
