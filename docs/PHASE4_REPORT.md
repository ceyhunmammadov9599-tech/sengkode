# SENGKODE Phase 4 Report

**Project:** SENGKODE - offline-first QR code generator (Android)
**Version:** 0.5.0-phase4 (versionCode 5)
**Status:** COMPLETE - Phase 4 gate PASSED. Phase 5 release
signing NOT started (per instruction).
**Verification:** `./gradlew test assembleDebug assembleRelease
--no-daemon` -> BUILD SUCCESSFUL. 190 test executions, 0 failures.
0 TODO/FIXME/STOPSHIP, 0 manifest permissions.
**Encoding:** ASCII-safe.
**Date:** September 18, 2026

---

## 1. Implemented Features

1. **Batch QR generation (:core:export BatchPngExporter):** many
   (content, style) snapshots -> one ZIP of PNGs. Streams strictly
   one bitmap at a time (generate -> safety-gate -> render ->
   compress -> recycle), so a 100-code batch never holds 100
   bitmaps. Reuses the single existing pipeline (engine -> policy
   -> DrawListBitmapRenderer); unsafe styles are skipped and
   reported, never silently exported.
2. **Album/export workflow (:feature:history):** overflow menu on
   the history screen with "Export all as ZIP" and "Export
   favorites as ZIP". Writes the ZIP to the app cache and hands it
   to the system share sheet through the same narrow FileProvider
   grant pattern as single PNGs. Toast reports exported and
   skipped counts.
3. **Share into app (:feature:generator ShareIntake + :app):**
   SENGKODE now appears in the Android share sheet for text/plain.
   Shared URLs become URL content, other text becomes plain text;
   blank and oversized (>1000 chars) payloads are ignored. The
   content is staged through the EXISTING session restore store,
   so the studio consumes it through its tested exactly-once path
   and runs the normal validation + safety pipeline. No QR logic
   duplicated, no safety check bypassed.
4. **Logo placement UX (:feature:generator):** dedicated "Change
   image" / "Remove image" chips (replacing a duplicated pick
   chip), a semantics-labelled size slider (10-25 percent,
   policy-clamped), and a new on-device runtime scannability gate:
   when a logo is actually placed, the rendered 512px bitmap is
   decoded and must return the EXACT original payload. The result
   shows as a polite live-region badge ("Verified scannable" /
   "Logo blocks scanning - shrink or remove it"), and a failed
   verification BLOCKS save/share/export.
5. **Accessibility audit pass:** QR preview images carry
   meaningful content descriptions (studio + history detail),
   icon-only buttons are labelled, the logo slider exposes a
   content description, the verification badge is a polite live
   region, and all controls stay on Material3's 48dp minimum
   touch-target enforcement. Error states (unsafe style reasons,
   validation errors) are plain Text visible to TalkBack.
6. **Dark theme validation:** SengkodeTheme already ships
   first-class light AND dark schemes consumed via
   isSystemInDarkTheme; the audit found no hardcoded surface colors
   in the new screens (all MaterialTheme.colorScheme roles), and QR
   previews always draw their own style-defined background so they
   stay readable in both themes. No fixes were required; the
   finding is documented instead of inventing work.

## 2. UX Improvements

- History: batch export of the whole list or just favorites
  (previously only one-by-one regeneration existed).
- Studio: logo removal is now a first-class action; the dead
  duplicated pick-chip branch is gone.
- Share sheet entry: create a QR from any URL/text in two taps
  without typing.
- Scannability is now VERIFIED on-device for logo configurations,
  not just policy-predicted.

## 3. Accessibility Results

- Preview images: meaningful content descriptions everywhere
  (studio, history detail, template cards already had one).
- Icon-only actions (batch menu, clear all, favorite, delete)
  carry labels; the logo slider has a content description.
- The logo verification badge uses LiveRegionMode.Polite so
  TalkBack announces scan-safety changes without stealing focus.
- Touch targets: Material3 default 48dp interactive component
  enforcement active across all screens; no custom views added.
- No color-only information: the verification state has a text
  badge, unsafe states list textual reasons.
- Keyboard/navigation: standard Compose text fields and chips,
  no custom focus logic needed.

## 4. Performance Results

- Studio generation debounce unchanged (150 ms); the logo
  verification render (512 px) runs once per
  (matrix, style, logo) change on Dispatchers.Default, off the
  main thread, and the bitmap is recycled immediately.
- Batch exporter processes one bitmap at a time on
  Dispatchers.IO; ZIP entries are compressed streamed, never
  buffered whole. No new caches were introduced (measured need
  first, per the prompt).
- No changes on the Phase 1-3 hot paths; regression suites all
  green.
- Robolectric note: bitmap-touching tests run with
  GraphicsMode.NATIVE so pixel assertions are real, not shadowed.

## 5. Files Modified

Created: :core:export BatchPngExporter, BitmapScannabilityVerifier,
QrFileExporter.shareZip; :feature:generator ShareIntake;
tests: BatchPngExporterTest (4), BitmapScannabilityVerifierTest
(3), ShareIntakeTest (7).
Modified: :app MainActivity (share intake + restore-store
injection), AndroidManifest (text/plain SEND filter), version
0.5.0-phase4 / versionCode 5; :feature:generator GeneratorScreen
(logo verification state + badge + export gate, QrPreview
descriptions), StudioSections (LogoSection UX rewrite), strings;
:feature:history HistoryScreens (batch export menu + actions,
detail preview description), strings; :core:export build
(:core:qr api + zxing core + Robolectric test deps).

## 6. Test Coverage

190 executions, 0 failures (134 unique test methods; variant-doubled
suites counted twice). New Phase 4 suites: BatchPngExporterTest -
ZIP contains one scannable PNG per safe snapshot, each decodes
back to its EXACT payload; unsafe styles skipped + reported;
empty batch yields a valid empty ZIP; portable ASCII slugs.
BitmapScannabilityVerifierTest - a rendered code verifies against
its own payload, fails on a different payload and on a blank
bitmap. ShareIntakeTest - URL detection, trimming, blank and
oversized rejection, mixed text stays text, staging into the
restore store with a default style, no-op on nothing shared.
Phase 1-3 regression fully green (engine, renderer, policy,
database, view models, routes untouched).

## 7. Build Results

- `test` (all modules, both variants): ALL GREEN (190/190).
- `assembleDebug`: BUILD SUCCESSFUL (17.1 MB).
- `assembleRelease` (R8 ON): BUILD SUCCESSFUL (1.83 MB, up from
  1.79 MB - the share/batch additions).
- 0 TODO/FIXME/STOPSHIP, 0 permissions (SEND intake needs none).

## 8. Remaining Risks

1. Logo verification decodes at 512 px; scanners in poor lighting
   may still fail on extreme logos - but the gate now catches
   everything the decoder can see, which is the honest maximum.
2. Batch export shares via ACTION_SEND; some targets (e.g. certain
   cloud apps) handle application/zip poorly. The ZIP itself is
   always written to cache correctly.
3. UI snapshot testing was deliberately NOT added as screenshot
   infrastructure (per "do not add unnecessary testing
   infrastructure"); the valuable invariants are covered by
   semantics/pipeline tests instead. If visual regression coverage
   is wanted later, Robolectric screenshot tests are the natural
   extension.
4. Release APK still unsigned (Phase 5 scope).
5. Share intake accepts a single EXTRA_TEXT only; multi-text
   multi-line shares arrive as one text payload (by design).

## 9. APK Information

- Debug: 17.1 MB, com.hjinlabs.sengkode.debug, versionName
  0.5.0-phase4, versionCode 5 - delivered for device testing.
- Release: 1.83 MB R8-minified, unsigned.

## 10. Recommendation For Final Release Phase

**Phase 5 - Release Readiness:** create an upload keystore and
sign the release APK (AAB recommended), set versionName to 1.0.0
with versionCode managed, final TalkBack + dark theme device
sweep on a real phone, README + Play Store listing copy (privacy
story is a headline feature: zero permissions, zero network,
on-device everything), tag v1.0.0 and cut a GitHub release.

**END OF PHASE 4 REPORT - ALL STEPS DONE**
