# SENGKODE Phase 0 - Foundation - Report

**Project:** SENGKODE - offline-first QR code generator (Android)
**Package:** `com.hjinlabs.sengkode` | Branding: SENGKODE
**Status:** COMPLETE - Phase 0 gate PASSED.
**Discipline:** No TODOs, no FIXMEs, no speculative dependencies, no
placeholder success paths. Zero manifest permissions (privacy-first
contract verified - see Section 6).
**Encoding:** ASCII-safe.
**Date:** September 15, 2026
**Companion documents:** docs/ROADMAP.md, ARCHITECTURE_VALIDATION_REPORT.md,
TECHNICAL_VALIDATION_REPORT.md

---

## 1. What Was Implemented

1. **Project identity:** folder renamed `qrcode-generator` ->
   `sengkode`; `applicationId com.hjinlabs.sengkode` (debug variant
   `.debug`); version 0.1.0-phase0 (versionCode 1); app label
   SENGKODE.
2. **Gradle foundation:** wrapper 8.7 (proven pin-set); version
   catalog with the validated versions (AGP 8.5.0, Kotlin 2.0.20,
   KSP 2.0.20-1.0.25, Hilt 2.52, Compose BOM 2024.09.00, Room 2.6.1,
   ZXing 3.5.3 pinned for Phase 1); plugin management; FAIL_ON_PROJECT_REPOS
   repository lock; Kotlin 17 toolchain everywhere.
3. **Module structure (9 modules + app):**
   - `:core:model` and `:core:qr` - pure Kotlin JVM modules
     (kotlin-jvm plugin): the QR engine stays Android-free and
     JVM-testable by construction.
   - `:core:database` - Room runtime+KSP staged ("Room preparation").
   - `:core:export`, `:core:designsystem`, `:feature:generator`,
     `:feature:history`, `:feature:templates` - Android libraries.
4. **Application foundation:** @HiltAndroidApp Application class;
   @AndroidEntryPoint single-Activity MainActivity; type-safe
   navigation (kotlinx.serialization routes) with M3 bottom
   navigation (Create / History / Templates); Material 3 theme with
   full dark + light schemes (teal primary, red tertiary accent) and
   optional dynamic color; adaptive launcher icon (QR finder-pattern
   motif).
5. **Testing infrastructure:** JUnit + coroutines-test + MockK +
   Turbine pinned in the catalog; first real unit tests live in
   :app (see Section 5).

## 2. Files Created / Modified

- Root: settings.gradle.kts, build.gradle.kts, gradle.properties,
  gradle/libs.versions.toml, gradlew wrapper set.
- app/: build.gradle.kts, proguard-rules.pro, AndroidManifest.xml
  (zero permissions), SengkodeApplication.kt, MainActivity.kt,
  AppRoot.kt, SengkodeRoutes.kt, res (strings/colors/themes,
  adaptive icon), RouteContractTest.kt.
- core/: model + qr (kotlin-jvm), database (Room+KSP staged),
  export, designsystem (Theme.kt: dark/light schemes).
- feature/: generator, history, templates (scaffold screens -
  real working UI, bodies documented for Phase 1/3 replacement).
- docs/: this report.

## 3. Architecture Decisions

1. **Pure-JVM engine modules:** :core:model and :core:qr use the
   kotlin-jvm plugin, not android-library - the architecture's main
   quality lever (engine unit-testable on the JVM, no Android
   surface) is enforced by the build system itself.
2. **Type-safe routes as serializable objects:** Navigation
   Compose resolves route types; adding parameters later is a
   compile-time change, never a string-contract change.
3. **Zero-permission manifest is a Phase 0 deliverable:** the
   privacy contract (Technical Validation Report, checklist items
   1-3) is enforced from the first commit, not retrofitted.
4. **R8 ON in release from day one:** a release variant that cannot
   survive minification is a failed build; every phase re-verifies
   assembleRelease.
5. **No speculative dependencies wired:** Room/ZXing are pinned in
   the catalog; only Room (staged per directive) is wired, ZXing
   enters in Phase 1 with the encoder surface.

## 4. Commands Executed

- `./gradlew assembleDebug --no-daemon` - BUILD SUCCESSFUL (2m 45s,
  first run; 213 tasks).
- `./gradlew test assembleRelease --no-daemon` - BUILD SUCCESSFUL
  after test fixes (final verification run green).
- `./gradlew --stop` before process management; `--no-daemon` for
  every automated invocation (process-management discipline).
- TODO/FIXME scan: 0 matches. Manifest permission scan: 0.

## 5. Build / Test Results

- `assembleDebug`: BUILD SUCCESSFUL - app-debug.apk (16.5 MB).
- `assembleRelease` (R8 minification ON): BUILD SUCCESSFUL -
  app-release-unsigned.apk (1.3 MB).
- `test`: ALL GREEN - 2 unit tests in :app (executed in both debug
  and release variants, 0 failures):
  1. `routes round-trip through serialization` - every type-safe
     route survives encode/decode.
  2. `distinct routes resolve to distinct navigation identities` -
     pins the real Navigation Compose contract.

**Phase 0 finding (documented, test-harness discovery):**
kotlinx.serialization encodes every @Serializable data object as "{}"
(no class discriminator). Navigation Compose therefore identifies
destinations by qualified class name - the second test pins exactly
that contract. The initial assumption (distinct encoded payloads)
was wrong; the test asserts verified behavior, not a guess.

## 6. Quality Verification

- Build green: debug + release (R8).
- Tests: all green (0 failures).
- No TODO/FIXME/STOPSHIP anywhere.
- No unnecessary dependencies wired.
- Architecture violations: none - features depend only on core;
  engine modules have zero Android dependencies.
- Privacy contract: 0 uses-permission entries in the manifest.

## 7. Potential Risks (for Phase 1)

1. Compose preview tooling on minSdk 24 devices is fine, but the
   live preview pipeline (Phase 1) must debounce matrix generation
   to keep low-end devices smooth (already planned).
2. The `:feature:content` merge-into-`:feature:generator` decision
   (Technical Validation Report) resolves in Phase 1 - module graph
   may grow by one module.
3. Release APK is unsigned (no keystore in this environment);
   signing remains a Phase 5 step.

## 8. Next Recommended Step

**Phase 1 - Core QR Generation Engine:** the 8 content encoders
(text, URL, Wi-Fi, vCard, email, SMS, phone, geo) with typed
validation + capacity tables in :core:qr; the ZXing wrapper; the
generate-decode round-trip test suite; the live preview studio;
PNG export + MediaStore + share in :core:export. Gate: round-trip
tests 100% green per content type, engine 100% JVM-covered.

**END OF PHASE 0 REPORT - ALL STEPS DONE**
