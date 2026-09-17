# SENGKODE Phase 3 Report - History & Templates

**Project:** SENGKODE - offline-first QR code generator (Android)
**Version:** 0.4.0-phase3 (versionCode 4)
**Status:** COMPLETE - Phase 3 gate PASSED.
**Verification:** `./gradlew test assembleDebug assembleRelease
--no-daemon` -> BUILD SUCCESSFUL. 162 test executions, 0 failures.
0 TODO/FIXME/STOPSHIP, 0 manifest permissions.
**Encoding:** ASCII-safe.
**Date:** September 17, 2026

---

## 1. Implemented Features

1. **:core:database (Room + KSP):** AppDatabase schema v1 with
   HistoryEntity and TemplateEntity, HistoryDao and TemplateDao,
   exportSchema = true (committed schemas/.../1.json).
2. **Snapshot persistence (no bitmaps, ever):** rows store ONLY
   content JSON + style JSON + metadata. Every image is regenerated
   through the single existing pipeline (QrMatrix -> style
   renderer -> export renderer), so Preview == History Render ==
   Export is structural, not coincidental.
3. **History feature (:feature:history):** list (favorites pinned
   first, newest first), detail screen with a pipeline-regenerated
   preview, regenerate (loads the snapshot back into the studio),
   delete single item, favorite toggle, and a confirm-guarded
   Clear-All privacy action.
4. **Template gallery (:feature:templates):** five decode-verified
   built-ins (Minimal, Business, Social, Event, Modern) seeded
   idempotently on first run, mini styled preview per card rendered
   by the SAME engine + renderer, Apply -> seeds the studio,
   custom styles saved from the studio, custom templates
   deletable (built-ins are not).
5. **Studio integration (:feature:generator):** Save-to-history and
   Save-style-as-template actions (named dialog), plus session
   restore consumption - a pending (content, style) pair from
   history regenerate or template apply is consumed exactly once
   when the studio re-enters composition.
6. **Navigation:** new type-safe route HistoryDetailRoute(itemId),
   full round-trip pinned in RouteContractTest.
7. **Shared editor defaults:** QrContentDefaults (defaultFor,
   titleFor) in :core:model - one source for studio, regeneration
   and template application.

## 2. Database Schema

Schema v1 (schemas/com.hjinlabs.sengkode.core.database.AppDatabase/
1.json, exportSchema = true):
- **history:** id (PK auto), title, contentJson (QrContent),
  styleJson (QrStyle), createdAtEpochMs, isFavorite. Index-free by
  design; ordering handled in SQL (ORDER BY isFavorite DESC,
  createdAtEpochMs DESC).
- **templates:** id (PK auto), name, description, styleJson
  (QrStyle), contentTypeHint (QrContentType.name), isBuiltIn.
- JSON codec (kotlinx.serialization, ignoreUnknownKeys = true) at
  the repository boundary - Room stays dumb strings, forward
  compatibility is free.
- Images are NOT stored, per requirement. History rows are a few
  hundred bytes.

## 3. Migration Strategy

- exportSchema = true from the first commit; schema JSON committed.
- No destructive migration policy anywhere; schema v1 is the
  baseline every future migration must satisfy.
- MigrationContractTest (Robolectric + room-testing
  MigrationTestHelper): creates a v1 database from the committed
  JSON and validates it with runMigrationsAndValidate - the exact
  scaffold the future 1->2 migration test plugs into.
- Codec forward-compatibility tested (unknown JSON keys tolerated).

## 4. Architecture Decisions

1. **Repository interfaces in the domain layer (:core:model):**
   HistoryRepository, TemplateRepository, StudioRestoreStore -
   ViewModels depend only on these; Room stays isolated in
   :core:database behind Hilt @Binds. No redesign of existing
   modules - :core:database was staged since Phase 0 and received
   the planned entities.
2. **Session-scope restore store instead of fat route args:**
   history regenerate and template apply hand (content, style) to
   the studio through a one-slot, consume-once store rather than
   URL-encoding JSON into navigation. Consumption happens at screen
   composition, exactly once per navigation.
3. **Templates provide QrStyle configuration ONLY:** applying a
   template = seeding studio style + default content for the
   hinted type. Zero duplicated QR generation logic in the
   templates feature; the studio pipeline does all generation,
   safety and rendering.
4. **Built-in templates live in :core:style:** near the safety
   policy and presets they derive from; seeding copies them into
   Room so users see/own one gallery. Every built-in is
   policy-verified + decode-verified + serialization-round-trip
   verified (BuiltInTemplatesTest).
5. **Robolectric for the data layer:** real in-memory Room DAO
   tests plus the migration scaffold run on JVM, keeping the CI
   gate honest without an emulator. Feature ViewModel tests use
   fakes of the domain interfaces.
6. **List rendering deferred to the detail screen:** history rows
   show title/type/date only; the detail screen and studio
   regenerate bitmaps on demand. No bitmap caches, no orphaned
   image rows, no stale previews.

## 5. Files Created / Modified

Created: :core:database (Entities, Daos, AppDatabase,
SnapshotCodec + repository impls, DI DatabaseModule, 3 test
suites, schemas/1.json); :core:model/repository/Repositories.kt;
QrContentDefaults.kt; @Serializable annotations across domain
types; :core:style BuiltInTemplates.kt + test + shared
RoundTripHarness; :feature:history (HistoryViewModel,
HistoryScreens, strings, 6 tests); :feature:templates
(TemplatesViewModel, TemplatesScreen, strings, 4 tests).
Modified: :feature:generator (StudioViewModel save/restore/
events, GeneratorScreen actions + template dialog, InMemoryStudio-
RestoreStore + binding, strings, 5 new tests); :app (routes,
AppRoot wiring, version 0.4.0-phase3, RouteContractTest).

## 6. Test Coverage

162 executions, 0 failures, 0 errors. By suite (debug+release
where applicable): core:qr 42 (regression, untouched), core:style
36 including 3 new built-in template contract tests, core:
database 28 (14 x 2: 9 DB contract tests incl. save -> reload ->
regenerate-identical, 4 codec tests, 1 migration scaffold),
feature:generator 32 (16 x 2, +5 Phase 3 tests), feature:history
12 (6 x 2), feature:templates 8 (4 x 2), app routes 4. Phase 1/2
regression fully green - no engine or renderer code changed.

## 7. Build Results

- `test` (all modules, both variants): ALL GREEN (162/162).
- `assembleDebug`: BUILD SUCCESSFUL (17.1 MB).
- `assembleRelease` (R8 ON): BUILD SUCCESSFUL (1.79 MB, up from
  1.65 MB - the Room/serialization cost of persistence).
- 0 TODO/FIXME/STOPSHIP, 0 permissions.

## 8. Performance Impact

- No new work on the generation hot path; studio generation stays
  debounced at 150 ms.
- History writes happen only on explicit user action (one small
  INSERT). Lists observe a Room Flow (while-subscribed, 5s
  grace) - zero background polling.
- Detail/template previews render at 512/256 px off the main
  thread (Dispatchers.Default), same as the studio; list rows
  render no bitmaps at all.
- Template seeding runs once per app lifetime (idempotent count
  check).

## 9. Privacy Review

- Storage is local-only: Room file on device, no network
  permission, no analytics, no external storage. Sensitive payloads
  (Wi-Fi passwords, contacts, URLs) never leave the device.
- Clear-All wipes the entire history with an explicit
  confirmation dialog; single-item delete always available.
- Snapshots store the minimum needed (content + style + metadata);
  no bitmaps, no usage telemetry, no timestamps beyond creation.
- Built-in templates ship in code; nothing fetched.

## 10. Remaining Risks

1. Robolectric tests pin SDK 34; compileSdk is 34 - consistent
   today, but a future compileSdk bump needs the @Config bump too.
2. History has no automatic deduplication (saving the same content
   twice creates two rows) - deliberate (append-only audit trail);
   revisit if lists get noisy.
3. Release APK remains unsigned (Phase 5 release-readiness scope).
4. Template previews render on every card composition at 256 px -
   cheap today; if the gallery grows into dozens of templates, add
   a small bitmap cache keyed by (id, style).

## 11. APK Information

- Debug: 17.1 MB, com.hjinlabs.sengkode.debug, versionName
  0.4.0-phase3, versionCode 4 - delivered for device testing.
- Release: 1.79 MB R8-minified, unsigned.

## 12. Recommendation For Next Phase

**Phase 4 - UX Completion:** batch generation (multiple PNGs/
album of saved codes), share-to-app (receive text/URL via
ACTION_SEND and pre-fill the studio), drag-and-drop logo
placement, full TalkBack sweep of the new screens, snapshot
testing of history/template UIs, and dark theme verification of
every new surface. Alternative candidate: Batch/Album export
(styled ZIP) if device feedback asks for bulk workflows first.

**END OF PHASE 3 REPORT - ALL STEPS DONE**
