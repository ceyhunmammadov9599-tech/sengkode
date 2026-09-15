# SENGKODE Phase 2 - Customization Studio - Report

**Project:** SENGKODE - offline-first QR code generator (Android)
**Version:** 0.3.0-phase2 (versionCode 3)
**Status:** COMPLETE - Phase 2 gate PASSED.
**Verification:** `./gradlew test assembleDebug assembleRelease
--no-daemon` -> BUILD SUCCESSFUL. 101 test executions, 0 failures.
0 TODO/FIXME/STOPSHIP, 0 manifest permissions, no architecture
violations.
**Encoding:** ASCII-safe.
**Date:** September 15, 2026

---

## 1. Implemented Features

1. **Customization domain model (:core:model):** QrStyle extended
   backward-compatibly (all Phase 1 call sites unchanged): module
   shape (SQUARE / ROUNDED / DOT), eye shape (SQUARE / ROUNDED),
   eye color (null = follow modules), LogoSpec (size fraction
   10-25%), FrameSpec (SCAN ME border + label), quiet-zone config.
   Logo PIXELS are deliberately NOT in the domain model (bitmaps do
   not belong there) - they are a render-time input.
2. **Color customization:** 8 curated scan-safe presets + custom
   hex input for foreground, background and eye color, all gated by
   the safety policy before anything renders.
3. **Scan-safety policy (:core:style):** centralized, pure,
   deterministic ScanSafetyPolicy: WCAG-derived module contrast
   (4.5:1, 5.5:1 for dots), inversion rejection, quiet-zone spec
   minimum, eye-contrast check, version-aware logo-vs-finder
   bounds. Unsafe = explicit reasons shown verbatim in the UI -
   never silently rendered.
4. **Logo with ECC strategy (:core:style):** LogoEccPolicy maps
   coverage to ECC (<=18% -> Q, larger -> H), user choice is a
   floor, engine capacity errors surface as typed errors - never a
   silent downgrade.
5. **Module + eye shapes (:core:style renderer):** deterministic
   draw-list geometry; finder patterns keep the exact 7-5-3
   structural layering under every shape/color combination; the
   encoded matrix is never modified for visuals.
6. **SCAN ME frames:** one high-quality preset (rounded border +
   label) rendered strictly outside the symbol and quiet zone.
7. **Styled preview == export (:feature:generator + :core:export):**
   preview and PNG export run through ONE backend
   (DrawListBitmapRenderer) fed by the SAME pure draw-list - visual
   equality is structural, not coincidental.
8. **Studio UI:** preview anchor + content + colors + modules +
   eyes + logo + frame + ECC (with auto-upgrade notice) + save/
   share; logo image picked via system OpenDocument picker (no
   permission); 48dp-minimum M3 touch targets, contentDescription
   on presets and preview (TalkBack).

## 2. Files Created / Modified

- :core:model - QrStyle extended + StyleSpecs.kt (ModuleShape,
  EyeShape, LogoSpec, FrameSpec, FrameStyle).
- :core:style (NEW pure-JVM module) - ColorMath, ScanSafetyPolicy,
  LogoEccPolicy, StylePresets, DrawOp (backend-independent
  primitives), QrStyleRenderer; 4 test suites.
- :core:export - DrawListBitmapRenderer (Android backend).
- :feature:generator - StudioViewModel (style + safety + effective
  ECC state), GeneratorScreen (bitmap preview, logo picker,
  sections), StudioSections (all style controls), strings;
  StudioViewModelTest extended (6 new tests).
- :app - version 0.3.0-phase2.

## 3. Architecture Decisions

1. **Draw-list rendering seam (the Phase 2 core decision):** the
   styled renderer emits backend-independent DrawOps in a PURE JVM
   module; the JVM test backend paints them for the decode gate,
   the Android backend paints the same list for preview + PNG.
   Rationale: styled scannability must be verified WITHOUT an
   emulator (JVM), while preview and export must be pixel-equal -
   one geometry satisfies both. No existing module was redesigned;
   :core:export gained a second renderer class and a dependency on
   the new :core:style.
2. **Bitmap-based preview:** the Compose preview displays the
   rendered bitmap (background thread) instead of re-drawing the
   matrix in Canvas. Guarantees preview == export, removes ~31k
   per-frame draw calls, recomposition only on new bitmaps.
3. **Logo pixels outside the domain model:** LogoSpec (fraction)
   in :core:model; LogoImage (IntArray) at the render boundary.
4. **Centralized safety:** zero color logic in composables - the
   policy is the single authority; UI shows its reasons verbatim.
5. **Policies in :core:style, not :core:qr:** the engine stays
   style-blind (encoding only); the studio composes engine +
   policy at the ViewModel level.

## 4. Scan-Safety Strategy

- Heuristic gate (BEFORE wasting a generation): WCAG-derived
  contrast, inversion, quiet-zone, eye-contrast, version-aware
  logo bounds -> deterministic Safe/Unsafe with user-presentable
  reasons.
- Authority (AFTER generation): the styled round-trip suite - the
  policy refuses a case, the decode gate verifies everything it
  passes. WCAG is explicitly NOT treated as a scannability proof.
- Proof it works: during development the policy rejected emerald
  modules on F1F8E9 at 4.4:1 - a borderline pair a human eye
  accepts. The gate caught it before any decoder had to.
- Unsafe styles: preview and export are both blocked at
  StudioState.canRender; the UI lists every reason.

## 5. Logo / ECC Strategy

- Coverage-based: <=18% of symbol side -> Q; 19-25% -> H
  (LogoEccPolicy). User's ECC is a floor, never lowered.
- Version-aware bounds: the logo must keep >= 2 modules gap from
  each finder; on a version-1 symbol (21 modules) the safe logo
  max is 14.3% - the policy computes this from the ACTUAL matrix
  width after generation and reports it when exceeded.
- Capacity awareness: if the upgraded ECC overflows the payload,
  the engine's typed Capacity error reaches the user with exact
  numbers - no silent downgrade, no unsafe generation.

## 6. Rendering Strategy

QrMatrix (unchanged, source of truth) -> QrStyleRenderer (pure
geometry -> DrawOps) -> one backend per surface (JVM test painter /
DrawListBitmapRenderer). Modules, timing and alignment patterns are
rendered from the same bits the decoder validates; finders are
always drawn as the exact 7-5-3 structure (shape/color vary,
geometry never). Frames expand the canvas; logo blits are
deterministic nearest-neighbor (identical in test backend and
Android backend).

## 7. Test Coverage

Executed: 101, failures 0, errors 0. Suites:
- :core:qr (Phase 1 regression, untouched): 42 tests.
- :core:style (new): 33 tests - ScanSafetyPolicyTest 11,
  LogoEccPolicyTest 5, QrStyleRendererTest 7 (geometry contracts:
  7-5-3 eye structure, no data modules inside finders, frame band
  separation, logo centering/bounds, dot-cell containment,
  determinism), StyledRoundTripTest 10.
- :feature:generator: StudioViewModelTest 22 (11 x debug+release):
  Phase 1 behavior + style regeneration, logo ECC upgrade (spy
  verifies the engine is actually asked for H), ECC floor, unsafe
  style blocks rendering, quiet-zone violation, safe dot render.
- :app: RouteContractTest 4 (2 x variants).
- Phase 1 regression: zero changes to encoder/engine code; all 42
  Phase 1 tests still green.

## 8. Styled Round-Trip Results

31 individual generate -> style -> paint -> decode -> compare
cases, ALL PASSED:
- Content types: Text, URL, Wi-Fi, vCard, Geo (5), incl. unicode
  (CJK + emoji + combining text).
- Styles: 8 color presets, 3 module shapes x 2 eye shapes (all 6
  combos), custom eye colors, tinted backgrounds (3), SCAN ME frame
  (2 cases, one combined with logo + rounded shapes).
- Logos: 3 sizes (12/18/25%) + dot-module logo (4 cases), each at
  policy-mandated ECC.
- ECC levels: all of L/M/Q/H under styled rendering.
- Zero decoder weakening: the gate refuses any style the policy
  marks unsafe BEFORE decoding, then decodes everything it passes.

## 9. Build Results

- `test` (all modules, both app variants): ALL GREEN (101/101).
- `assembleDebug`: BUILD SUCCESSFUL (app-debug.apk 16.9 MB).
- `assembleRelease` (R8 ON): BUILD SUCCESSFUL (1.65 MB).
- Scans: 0 TODO/FIXME/STOPSHIP, 0 manifest permissions.

## 10. Performance Results

- Preview: rendered off the main thread (Dispatchers.Default),
  one 512px bitmap per (matrix, style, logo) change - the old
  ~31k-per-frame Canvas draw calls are gone; recomposition happens
  only when a new bitmap exists.
- Generation: still debounced 150 ms; style changes reuse the
  existing matrix (no re-encode unless ECC changes).
- Export: 1024px styled PNG in single-digit ms on Default/IO
  dispatchers; main thread never renders.
- No bitmap caching introduced (none needed - no measured problem).

## 11. Remaining Risks

1. Real-scanner variance remains the residual risk for heavily
   styled codes (dots + logo + tinted bg combined); the shipped
   presets are all decode-verified, and ECC floors + policy gates
   bound the worst case. Device testing with the debug APK is the
   next validation step.
2. Photo-picker logos are untested on devices (JVM-tested with
   synthetic logos); the picker flow uses only public APIs.
3. Frame label uses the system default typeface - consistent
   within a device, varies across OEMs (cosmetic only, sits
   outside the symbol).
4. Release APK still unsigned until Phase 5.

## 12. APK Information

- Debug: 16.9 MB, com.hjinlabs.sengkode.debug, versionName
  0.3.0-phase2, versionCode 3 - delivered for device testing.
- Release: 1.65 MB R8-minified, unsigned.

## 13. Recommended Next Phase

**Phase 3 - History & Templates:** Room persistence (schema is
staged since Phase 0) for generation history (payload, style,
timestamps, favorite flag; images derived on demand, never
stored as blobs), template gallery (style presets + content
type), list/detail/history screens on the staged bottom
navigation. Gate: Room migration tests + full round-trip suite
still green; the history re-render must use the same pipeline
(preview == history render == export).

**END OF PHASE 2 REPORT - ALL STEPS DONE**
