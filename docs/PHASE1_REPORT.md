# SENGKODE Phase 1 - Core QR Generation Engine - Report

**Project:** SENGKODE - offline-first QR code generator (Android)
**Version:** 0.2.0-phase1 (versionCode 2)
**Status:** COMPLETE - Phase 1 gate PASSED.
**Verification:** `./gradlew test assembleDebug assembleRelease
--no-daemon` -> BUILD SUCCESSFUL. 0 TODO/FIXME, 0 manifest
permissions, no architecture violations.
**Encoding:** ASCII-safe.
**Date:** September 15, 2026

---

## 1. Features Implemented

1. **Domain models (:core:model):** immutable, pure-Kotlin
   `QrContent` sealed hierarchy (8 content types), `QrStyle`
   (Phase 1: colors + quiet zone; Phase 2 extends), `ExportSpec`
   (512/1024/2048), typed `QrError` taxonomy
   (Validation / Capacity / Encoding) with user-presentable
   messages, `ValidationResult`, and the pure `QrMatrix` +
   `QrGenerationResult` types the UI and export consume.
2. **Encoder system (:core:qr):** independent `QrEncoder` per
   content type (Text, URL, Wi-Fi, vCard 3.0, Email mailto:, SMS,
   Phone TEL:, Geo) behind a `QrEncoderRegistry`. Spec-driven
   escaping (Wi-Fi `\ ; , : "`, vCard backslash/semicolon/comma/
   newline), URL normalization, phone normalization, validation
   BEFORE generation, typed errors - no raw ZXing exception ever
   leaves the engine.
3. **ZXing integration (:core:qr):** `ZxingQrEngine` wraps
   QRCodeWriter with ECC L/M/Q/H (default M), forced UTF-8,
   MARGIN 0 (quiet zone is the renderer's concern), and reports
   the selected symbol version. ZXing is `implementation`-scoped -
   it never leaks past the engine boundary.
4. **Capacity system (:core:qr):** ISO 18004 byte-mode capacity
   table (L 2953 / M 2331 / Q 1863 / H 1273) checked BEFORE the
   writer; oversized content gets a typed `Capacity` error carrying
   the exact numbers - invalid codes are never generated silently.
5. **Round-trip test system (:core:qr):** every content type is
   generated, rendered to a binary image and decoded with ZXing's
   QRCodeReader (reference scanner) on the JVM - the product's
   core promise ("every code scans") is now a regression suite.
6. **Live preview foundation (:feature:generator):**
   `StudioViewModel` with immutable `StudioState` StateFlow (UDF),
   150 ms debounced generation, engine runs on a background
   dispatcher, full editor UI for all 8 content types, ECC picker,
   Compose Canvas preview rendering the pure matrix, and
   typed-error display.
7. **Export foundation (:core:export):** `QrBitmapRenderer`
   (matrix -> ARGB_8888 Bitmap at any size, quiet zone included,
   seam-guarding insets), `QrFileExporter` (PNG -> MediaStore on
   Android 10+ with no permission, app-owned external dir on
   Android 8.x/9 - the zero-permission privacy contract holds on
   every supported API level), share via FileProvider with a
   narrow per-export read grant.

## 2. Files Created / Modified

- :core:model - 6 new files (QrContent, QrStyle, ExportSpec,
  QrError, ValidationResult, QrGenerationResult).
- :core:qr - QrEncoder, Encoders (8 encoders), QrEncoderRegistry,
  Capacity, QrEngine/ZxingQrEngine + 4 test suites
  (Encoders, Capacity, QrEngine, RoundTrip).
- :core:export - QrBitmapRenderer, QrFileExporter (Compose plugin
  removed: the Phase 1 pipeline is pure android.graphics by design).
- :feature:generator - StudioViewModel, GeneratorScreen (type
  picker, 8 editors, ECC picker, preview, save/share),
  GeneratorModule (DI bindings at the consumer boundary),
  strings.xml, StudioViewModelTest.
- :app - FileProvider declared (variant-aware authority),
  file_paths.xml, version 0.2.0-phase1.

## 3. Architecture Decisions

1. **Engine purity is build-enforced:** :core:qr stays a kotlin-jvm
   module; DI bindings live in the consumer feature module, so the
   engine has zero Android and zero DI awareness.
2. **Quiet zone decoupled from the matrix:** the engine emits a
   margin-free matrix; renderer/preview add the quiet zone. One
   matrix, any render size, preview == export pixel-for-pixel.
3. **Normalize-then-encode:** machine-readable payloads (SMSTO:,
   TEL:) strip visual separators; contact cards (vCard TEL) keep
   formatting. Both choices are spec-driven and unit-tested.
4. **:core:export is Compose-free:** the export pipeline is plain
   android.graphics, reusable from any future surface (Phase 4
   batch/headless mode).
5. **Geo editor state:** partial coordinate input stays local;
   `LaunchedEffect` pushes only fully-parsed coordinates into the
   ViewModel - no side effects during composition.

## 4. Test Coverage

Executed: 56 (0 failures, 0 errors). Breakdown:

- :core:qr (pure JVM) - 42 tests:
  EncodersTest 20 (payload formats, escaping, validation rules)
  CapacityTest 5 (spec table, level-aware checks, UTF-8 sizing)
  QrEngineTest 7 (success shape, typed errors, ECC fallback,
  unicode)
  RoundTripTest 10 (all 8 content types, unicode, long realistic
  payload, every ECC level for Text)
- :feature:generator - StudioViewModelTest 10 (5 tests x debug +
  release variants): debounce, typed error surfacing, ECC
  regeneration, exactly-one-generation-under-rapid-edits.
- :app - RouteContractTest 4 (2 x variants, from Phase 0).

**Bugs the suite caught and the fixes applied (before any manual
testing):** mailto: payloads used form-encoding "+" instead of
URI %20 for spaces; SMS/TEL payloads kept visual separators from
user input. Both are exactly the class of scannability bug the
Technical Validation Report predicted - caught on the JVM in
milliseconds.

## 5. Build Results

- `test` (all modules, both app variants): ALL GREEN.
- `assembleDebug`: BUILD SUCCESSFUL - app-debug.apk 16.9 MB.
- `assembleRelease` (R8 ON): BUILD SUCCESSFUL - 1.6 MB.
- Scan: 0 TODO/FIXME/STOPSHIP; 0 manifest permissions.

## 6. APK Information

- Debug (test build): 16.9 MB, applicationId
  com.hjinlabs.sengkode.debug, installable alongside release.
- Release: 1.6 MB R8-minified, unsigned (signing is a Phase 5
  step per roadmap).
- Debug APK packaged and delivered to the owner for device
  testing.

## 7. Performance Observations

- Matrix generation: sub-millisecond (JVM-measured) even for long
  payloads; the 150 ms debounce means the preview is effectively
  instant for humans on any device.
- Preview rendering: one Canvas pass per NEW matrix only -
  recomposition is keyed on the result, not on editor keystrokes.
- PNG export (1024 px, 33x33 symbols): single-digit milliseconds;
  runs on background dispatchers, main thread never blocks.
- Known optimization headroom (deliberately deferred): the Canvas
  preview draws per-module rects (up to ~31k for version 40) -
  an ImageBitmap cache is the Phase 2 answer if profiling ever
  demands it. No premature optimization shipped.

## 8. Remaining Risks

1. Real-scanner variance: ZXing Reader is a reference, not every
   phone scanner. Phase 2's styled-render round-trip tests plus
   the owner's device APK testing narrow this; keep ECC >= M
   default (already done).
2. SMSTO:/mailto: behavior varies across scanner apps (platform
   convention, not a spec) - payload formats follow the most
   widely-supported conventions and are documented in tests.
3. Release APK unsigned until Phase 5 (keystore setup).
4. StudioViewModel tests assert engine-backed behavior (real
   ZxingQrEngine, not a fake) - intentional: it tests the actual
   contract; no fake-success risk.

## 9. Recommended Next Phase

**Phase 2 - Customization Studio:** foreground/background color
pairs with a scan-safe contrast guard (WCAG-derived),
module/eye shapes, center logo with automatic ECC upgrade +
quiet-zone protection, SCAN ME frames. Gate: styled renders pass
the SAME round-trip decoder suite for every preset; unsafe
combinations are refused with reasons, never silently generated.

**END OF PHASE 1 REPORT - ALL STEPS DONE**
