# SENGKODE - Architecture Validation & Technical Preparation Report

**Project:** SENGKODE - offline-first QR code generator (Android)
**Branding:** SENGKODE (display), sengkode (package segment)
**Package:** `com.hjinlabs.sengkode`
**Report type:** BUILD MODE OFF - architectural validation and technical
preparation before development. No files were created, modified,
deleted or renamed during this analysis.
**Encoding:** ASCII-safe.
**Date:** September 15, 2026
**Companion document:** docs/ROADMAP.md (product roadmap, name
candidates - SENGKODE was selected by the owner)

---

## 1. Executive Summary

The project is a greenfield Android application: the repository
currently contains exactly one file (docs/ROADMAP.md) and zero code.
There is therefore no existing implementation to audit for defects -
instead, this report validates the PLANNED architecture against the
roadmap, audits the development environment that will build it, and
produces the controlled, phase-gated implementation plan.

Key conclusions:

- The planned module graph (10 modules) is sound and correctly
  dependency-directed; one adjustment is recommended (a small
  `:core:common` for shared result/dispatcher types, or fold those
  into `:core:model`).
- The development environment is READY: JDK 17, Android SDK 34 with
  build-tools 34/35/36, and a proven toolchain pin-set (AGP 8.5.0,
  Kotlin 2.0.20, KSP 2.0.20-1.0.25, Hilt 2.52, Compose BOM
  2024.09.00, Room 2.6.1) already validated end to end by the
  previous multi-module project in this same environment.
- Build Mode Recommendation: the analysis phase is COMPLETE - the
  correct next step is Phase 0 (foundation) under BUILD MODE ON,
  gated on the owner's explicit approval, exactly as requested.
- Highest-risk technical areas are identified up front (QR
  scannability guarantees, styled rendering vs decoder tolerance,
  Room schema stability, R8 rules for the ZXing/KSP stack) and each
  is assigned a phase, a test strategy and a completion criterion.

---

## 2. Current Project Analysis

### 2.1 Actual repository state (inspected)

```
qrcode-generator/
  docs/
    ROADMAP.md        (the only file; product roadmap + naming)
```

- No Gradle wrapper, no settings.gradle, no modules, no source files.
- No Git repository initialized for this project yet.
- No CI, no signing config, no version catalog.

### 2.2 Planned vs actual (gap analysis)

| Planned (roadmap) | Actual state | Verdict |
|---|---|---|
| :app entry + navigation + DI | Absent | Phase 0 |
| :core:model domain models | Absent | Phase 0/1 |
| :core:qr engine (ZXing wrapper + validation) | Absent | Phase 1 |
| :core:designsystem | Absent | Phase 0 |
| :core:database (Room) | Absent | Phase 3 |
| :core:export (render/share) | Absent | Phase 1 |
| :feature:generator | Absent | Phase 1 |
| :feature:content editors | Absent | Phase 1 |
| :feature:history | Absent | Phase 3 |
| :feature:templates | Absent | Phase 3 |
| Offline/privacy guarantees | Design commitment | Must be enforced by phase criteria |

Nothing differs from the intended architecture because nothing
exists yet; there is also zero technical debt. The project folder
name (`qrcode-generator`) predates the naming decision - Phase 0
must rename it to `sengkode` and set `applicationId
com.hjinlabs.sengkode` (deliberately NOT done now, BUILD MODE OFF).

### 2.3 Environment inspection (this build machine)

| Component | Available | Notes |
|---|---|---|
| JDK | 17.0.2 | Required by AGP 8.x - OK |
| Android SDK | platform android-34; build-tools 34/35/36 | compileSdk 34 proven; roadmap should start at compileSdk 34 for reproducibility |
| Gradle | 8.7 cached (proven), 9.x cached | Wrapper 8.7 recommended (matches AGP 8.5.0) |
| Maven network access | Available | New dependencies (ZXing core) resolvable |

---

## 3. Architecture Audit

### 3.1 Current architecture diagram (text)

```
qrcode-generator/
  docs/ROADMAP.md          <- no code, no architecture yet
```

### 3.2 Recommended final architecture diagram (text)

```
                        :app  (single Activity, Hilt, navigation host)
                          |
        +-----------------+------------------+---------------------+
        |                 |                  |                     |
 :feature:generator  :feature:content  :feature:history     :feature:templates
   (create studio)    (type editors)   (history/favorites)   (style gallery)
        \______________|________ ________|______________________/
                        v
   PRESENTATION depends only on domain interfaces exposed below
                        |
              +---------+----------+
              |                    |
        :core:designsystem    :core:model        (pure Kotlin domain:
              |               /    \              QrContent, QrStyle,
              |              /      \             ExportSpec, HistoryItem)
              |         :core:qr   :core:database   :core:export
              |       (ZXing core, (Room: history,  (Compose render ->
              |        encoders,   favorites,        Bitmap/PNG,
              |        validation,  templates)       MediaStore, share)
              |        pure JVM)        |
              |                          |
              +------------+-------------+
                           v
                 Android SDK / Room / Hilt runtimes

Dependency direction (enforced rule): feature -> core only;
core modules never depend on features; :core:qr and :core:model
depend on NOTHING Android; UI state flows one way (UDF):
State (StateFlow) <- ViewModel <- Repositories/UseCases <- core engines.
```

### 3.3 Separation-of-concerns verdicts

| Concern | Planned treatment | Audit verdict |
|---|---|---|
| Domain/data/presentation boundaries | :core:model + :core:qr pure; repos behind interfaces | Sound; keep :core:qr Android-free so it is JVM-unit-testable |
| Dependency direction | features -> core only | Sound; enforce with module graph discipline |
| State management | MVVM + UDF, StateFlow, immutable UI state | Sound; same proven pattern as previous project |
| Navigation | Type-safe Compose routes, single Activity | Sound; reuse the proven sealed-route setup |
| DI | Hilt + KSP (kapt is forbidden - prior project proved KSP works) | Sound |
| Testability | :core:qr 100% JVM; ViewModel tests with fakes | Sound; add golden/round-trip decode tests (Phase 1-2) |

### 3.4 Architectural risks (before implementation)

1. **Scannability guarantee is the product's core contract.** Style
   freedom (colors, logos, custom modules) directly fights decoder
   tolerance. Mitigation: contrast guard + ECC auto-upgrade + a
   decoder round-trip test suite in CI from Phase 1 onward.
2. **:core:export boundary.** Compose rendering -> Bitmap must not
   leak into :core:qr; the engine stays matrix-pure.
3. **Room schema stability** (Phase 3): destructive migrations would
   wipe user history; export-based migration policy required.
4. **Module over-fragmentation.** 10 modules for v1 is acceptable but
   `:core:common`-style shared types must be placed once (in
   :core:model) to avoid a new god-module later.

---

## 4. Gradle / Build Audit

- No Gradle build exists yet; this audit covers the ENVIRONMENT and
  the process rules that Phase 0 must adopt.
- Environment: JDK 17 + AGP 8.5.0 + Gradle wrapper 8.7 is the
  proven, reproducible pin-set (validated end to end by the previous
  project in this same environment, including R8 release builds).
- Recommended pins: Kotlin 2.0.20, KSP 2.0.20-1.0.25, Hilt 2.52,
  Compose BOM 2024.09.00, Room 2.6.1, ZXing core 3.5.x (new
  dependency - resolvable over the network).
- Process rules adopted from the owner's directive: stop daemons with
  `./gradlew --stop` before process management; never force-kill
  Gradle/Java processes; automated tasks run with `--no-daemon`;
  avoid commands that can leave locked files. (This build machine is
  Linux, but the discipline is portable and enforced.)
- R8/ProGuard rules must exist from Phase 0 (not bolted on later):
  ZXing + Room + Hilt + Compose are all well-covered by consumer
  rules, but the release minification gate is part of every phase's
  completion criteria, as proven in the previous project.
- Debug/release variant separation from day one (`.debug` suffix,
  release minified + rules-verified).

---

## 5. Build Mode Recommendation

**Decision: BUILD MODE OFF was correct for this report, and its work
is now complete. The project should move to BUILD MODE ON for
Phase 0 upon the owner's explicit approval.**

Rationale: the analysis found no code to fix and no blocking risks -
the only deliverables that CAN exist before code are this report and
the roadmap, both now done. Every remaining decision (module graph,
pins, diagrams, test strategy) is documented below with completion
criteria, so implementation can proceed in controlled, verifiable
phase gates. Continuing BUILD MODE OFF beyond this point would
produce paperwork without new information.

---

## 6. Development Roadmap (implementation-ready)

### Phase 0 - Project Foundation Validation
- **Objective:** a buildable, release-ready skeleton with the final
  identity and the full module graph.
- **Required changes:** rename folder to `sengkode`; wrapper 8.7;
  version catalog; 10-module graph; `com.hjinlabs.sengkode`;
  Compose M3 theme shell; type-safe navigation; Hilt+KSP wiring;
  proguard-rules.pro; debug/release variants.
- **Dependencies:** environment (already ready); owner approval.
- **Files/modules affected:** all root Gradle files, :app,
  :core:designsystem, :core:model (empty surfaces).
- **Technical risks:** wrapper/AGP mismatch (mitigated by pin-set);
  KSP misconfiguration (proven setup reused).
- **Testing requirements:** `assembleDebug` + `assembleRelease` green;
  empty-screen smoke; no unit tests yet (nothing to test).
- **Completion criteria:** both builds green; 0 TODO/FIXME; report.

### Phase 1 - Core QR Generation Engine
- **Objective:** every v1 content type generates a scannable code.
- **Required changes:** :core:qr encoders (text, URL, Wi-Fi, vCard,
  email, SMS, phone, geo); ZXing BitMatrix wrapper; ECC L/M/Q/H +
  payload-length validation; live preview in :feature:generator +
  :feature:content editors; :core:export PNG + gallery + share.
- **Dependencies:** Phase 0.
- **Files/modules:** :core:qr, :core:model, :core:export,
  :feature:generator, :feature:content.
- **Technical risks:** payload/ECC edge cases; Wi-Fi/vCard escaping
  bugs; MediaStore behavior across API levels.
- **Testing:** encoder unit tests per content type (spec-driven);
  decoder round-trip tests (generated matrix -> reference decoder ->
  payload equality); export smoke on device.
- **Completion criteria:** all content types green in round-trip
  tests; export + share work; engine 100% JVM-covered; report + APK.

### Phase 2 - Customization Studio
- **Objective:** styled codes that are guaranteed to still scan.
- **Required changes:** color pairs with contrast guard; module
  shapes (square/rounded/dot); custom eye colors; center logo with
  automatic ECC upgrade + quiet-zone protection; SCAN ME frames.
- **Dependencies:** Phase 1 render pipeline.
- **Files/modules:** :core:qr (style rules), :core:export,
  :feature:generator.
- **Technical risks:** decoder tolerance vs styling freedom; logo
  occlusion breaking quiet zones.
- **Testing:** golden bitmap tests + round-trip decode for every
  style preset; contrast-guard unit tests.
- **Completion criteria:** every style preset passes round-trip;
  unsafe combos are refused with a clear reason; report + APK.

### Phase 3 - History & Templates
- **Objective:** persistence and reusability without privacy leaks.
- **Required changes:** Room setup; history, favorites, one-tap
  regenerate; style template gallery; privacy auto-purge mode.
- **Dependencies:** Phase 1-2 models.
- **Files/modules:** :core:database, :feature:history,
  :feature:templates, :app (navigation).
- **Technical risks:** schema migrations; privacy mode leaving
  traces.
- **Testing:** Room DAO tests (fake/in-memory); regenerate-exactness
  tests; privacy-mode zero-trace test.
- **Completion criteria:** history survives restart; regenerate is
  byte-identical; privacy mode proven empty; report + APK.

### Phase 4 - Advanced Productivity Features
- **Objective:** batch generation from CSV with per-row validation.
- **Required changes:** CSV parser + validation report; batch render
  pipeline; ZIP export; progress + failure isolation.
- **Dependencies:** Phase 1-3.
- **Files/modules:** new :feature:batch (or extend :feature:generator),
  :core:export.
- **Technical risks:** OOM on large batches; storage permission
  prompts on old Androids.
- **Testing:** 100-row fixture -> 100 valid PNGs or precise per-row
  failures; memory profile on a mid-range profile.
- **Completion criteria:** batch fixture green in bounded memory;
  report + APK.

### Phase 5 - Release Preparation
- **Objective:** store-ready product.
- **Required changes:** localization (EN first, TR candidate);
  accessibility pass (TalkBack, touch targets); final R8
  verification; store listing assets; versioning + signing.
- **Dependencies:** all phases.
- **Files/modules:** :app, :core:designsystem, all features (strings).
- **Technical risks:** late-found a11y regressions; R8 stripping.
- **Testing:** release build green with minification; manual a11y
  pass; final regression suite.
- **Completion criteria:** signed-able release build green, all
  phases marked DONE, final report + APK.

### Critical Issues List (pre-implementation)
1. Folder still named `qrcode-generator`; identity must be applied in
   Phase 0 before any code exists (trivial, but first action).
2. No Git repository yet - Phase 0 initializes it before any feature
   work (commits pushed only when the owner explicitly instructs).
3. No CI - a documented verification command per phase substitutes
   until the owner requests automation.

### Priority Matrix
- **Critical:** scannability guarantee architecture (round-trip test
  suite, Phase 1); offline guarantee (no network code paths, Phase 0
  review gate).
- **High:** :core:qr purity (JVM-testable engine, Phase 1);
  decoder-tolerance guards for styling (Phase 2); Room schema
  stability (Phase 3).
- **Medium:** module graph discipline (Phase 0); export resolution
  options (Phase 1); CSV error reporting (Phase 4); localization
  readiness (Phase 0 string policy, Phase 5).
- **Low:** template gallery polish (Phase 3); store assets
  (Phase 5); batch UX refinements (Phase 4).

---

## 7. Next Safe Action

Await the owner's explicit BUILD MODE ON approval for Phase 0.
Upon approval, the first (already safe) actions are, in order:
1. Rename the project folder to `sengkode`.
2. Initialize the Gradle skeleton with the pin-set above and the
   `com.hjinlabs.sengkode` identity.
3. Deliver Phase 0 with green debug + release builds and a report -
   the same verification rhythm as the previous project.

**END OF ARCHITECTURE VALIDATION REPORT**
