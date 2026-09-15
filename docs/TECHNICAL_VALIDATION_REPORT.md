# SENGKODE Technical Validation Report

**Project:** SENGKODE - offline-first QR code generator (Android)
**Branding:** SENGKODE (display) / sengkode (package segment)
**Package:** `com.hjinlabs.sengkode`
**Report type:** BUILD MODE OFF - deep technical validation before the
first line of production code. No implementation code was written;
the only file artifacts of this phase are this report and the
repository setup the owner explicitly requested.
**Reference documents:** docs/ROADMAP.md, docs/ARCHITECTURE_VALIDATION_REPORT.md
**Encoding:** ASCII-safe.
**Date:** September 15, 2026

---

## Executive Summary

SENGKODE is a greenfield, fully offline QR code generator. The
repository contains only planning documentation; every technical
decision is therefore still open, and this report converts the
roadmap's commitments into a verified, execution-ready plan.

Core verdicts:

1. The planned architecture (Kotlin 2.x, Compose + M3, Clean
   Architecture, MVVM + UDF, Hilt/KSP, single Activity) is
   APPROPRIATE for this product class - same proven pattern as the
   owner's previous multi-module projects in this same environment.
2. The QR engine choice is validated as **ZXing core 3.5.x**: pure
   JVM matrix generation, battle-tested, all error-correction
   levels, and - decisively - ZXing ships a QR *decoder* too, so the
   project's core contract ("every generated code scans") is
   provable in JVM unit tests via generate -> decode round-trips
   with no emulator required.
3. The build environment is ready (JDK 17, SDK 34, proven
   AGP/Gradle pin-set). No blocking risks exist. The project can
   safely enter Phase 0 (BUILD MODE ON) on the owner's approval.
4. Two planning gaps were found and closed in this report: the
   missing data-layer design (Room schema, v1 vs v2 split) and the
   missing privacy checklist (concrete, testable rules).
5. Estimated v1 complexity is MEDIUM overall - the difficulty is
   not QR generation itself but the scannability guarantee under
   heavy visual customization (Phase 2), which is where the risk
   budget belongs.

---

## Current Repository Status

Inspected state (facts, not assumptions):

```
qrcode-generator/
  docs/
    ARCHITECTURE_VALIDATION_REPORT.md   (previous Build Mode Off report)
    ROADMAP.md                          (product roadmap + naming decision)
```

- Zero source files, zero Gradle files, zero modules.
- No Git repository existed locally; per the owner's explicit
  instruction this phase created the GitHub repository
  `ceyhunmammadov9599-tech/sengkode` and pushed the current project
  files (documentation + repository hygiene files only).
- Folder is still named `qrcode-generator`; renaming to `sengkode`
  is the FIRST action of Phase 0 (deliberately not done in Build
  Mode Off).
- Documentation vs repository conflicts: NONE. Both documents
  describe planned state; nothing contradicts the empty repository.
- Environment: JDK 17.0.2, Android SDK platform 34 (build-tools
  34/35/36), Gradle 8.7 cached, Maven network access available.
  Proven pin-set from the previous project: AGP 8.5.0, Kotlin 2.0.20,
  KSP 2.0.20-1.0.25, Hilt 2.52, Compose BOM 2024.09.00, Room 2.6.1.

What exists: the decision layer (roadmap, architecture validation).
What is planned: everything else. What is missing: all code.

---

## Architecture Validation

**Is the architecture appropriate?** Yes. A QR generator is a
computation + rendering + local-persistence app with no network
dimension in v1. Clean Architecture's value here is not layers for
their own sake but two concrete properties: (a) the QR engine is a
pure Kotlin module with zero Android dependencies - the highest-value
tests in the project run on the JVM in seconds; (b) the UI layer is
thin and stateless-ish, so Compose previews and future UI tests stay
trivial.

**Is the module structure scalable?** The 10-module graph is sound.
Verdicts per module:

| Module | Verdict |
|---|---|
| :core:qr | Correct and essential - the purity rule is the project's main quality lever |
| :core:model | Correct - single owner of shared domain types (avoids a future god-module) |
| :core:designsystem | Correct - same proven setup as previous projects |
| :core:database, :core:export | Correct - both hide platform APIs behind interfaces |
| :feature:* | Correct; :feature:content may MERGE into :feature:generator in v1 if the editors turn out to be thin (decision deferred to Phase 1 reality, documented then) |

**Domain/data/presentation separation:** correct as planned. The
repository abstraction for history (interface in domain, Room impl
in data) keeps Room out of ViewModels.

**Future limitations (honest):**
1. Compose rendering -> Bitmap export ties :core:export to Compose;
   if a future headless/batch mode needs raw Android canvas, the
   render pipeline needs an abstraction seam (planned in Phase 4's
   design - flagged now).
2. ZXing's BitMatrix is square-only; custom frame/label export is
   OUR layer's job (already the plan).
3. Multi-window/tablet and scanner feature would stress the single-
   Activity nav graph mildly - acceptable, no change needed now.

**Better alternatives?** No. Alternatives considered and rejected:
- MVI over MVVM+UDF: no net benefit at this state size.
- KMP: no v1 requirement (no iOS), would slow Phase 0 for nothing.
- Fragment-based UI: strictly worse with Compose.

---

## Recommended Final Architecture

(Unchanged from the validation report - re-confirmed, ASCII diagram:)

```
 :app (single Activity, Hilt, type-safe Compose navigation)
   |
   +-- :feature:generator   live preview studio (hosts content editors)
   +-- :feature:history     history + favorites + regenerate
   +-- :feature:templates   style template gallery
   |
   v  (features depend ONLY on core)
   +-- :core:model        QrContent, QrStyle, ExportSpec, HistoryItem (pure)
   +-- :core:qr           encoders + ZXing wrapper + validation (pure JVM)
   +-- :core:database      Room: history, favorites, templates, settings
   +-- :core:export        Compose render -> Bitmap/PNG, MediaStore, share
   +-- :core:designsystem  M3 theme + shared components
   |
   v
 Android SDK / Room / Hilt / ZXing runtimes

UDF rule: ViewModel exposes ONE immutable StateFlow; events go UP
as function calls; state comes DOWN as data; no bidirectional
bindings anywhere.
```

---

## QR Engine Technical Strategy

**Library decision: ZXing core 3.5.x** (com.google.zxing:core -
JVM artifact, no Android permissions, no network).

Evaluated alternatives:
- *qrcodegen (Nayuki)*: excellent, smaller; rejected as sole engine -
  no built-in decoder for round-trip tests, smaller ecosystem.
- *QRCode-kotlin*: nice Compose ergonomics; rejected - thinner
  battle record, matrix types not shared with any decoder.
- *ML Kit / Play Services*: rejected - violates offline/privacy
  first principles and adds a 500KB+ runtime.

**Why ZXing wins decisively:** zxing-core contains BOTH QRCodeWriter
and QRCodeReader. The scannability contract becomes a JVM unit
test: encode(content, ECC) -> BitMatrix -> decode(BitMatrix) ->
assertEquals(content). No emulator, no device, milliseconds per
case. This is the single most important architectural fact for
product quality.

**Encoding strategy:**
- One `QrEncoder` interface per content type in :core:qr, each a
  pure function `(Content) -> Result<Payload>`; payload string +
  hint map (ECC level, margin) feed the ZXing writer.
- Error correction: expose L/M/Q/H to the user, DEFAULT M; auto-
  upgrade to Q/H when a center logo is applied (Phase 2 rule).
- Payload validation BEFORE generation: per-type maximum bytes vs
  capacity table per ECC + version; refuse with a typed, user-
  presentable error (never a zxing stack trace).
- ASCII-safe discipline: all payloads and content builders are
  ASCII-safe by design (QR supports UTF-8 natively; escaping rules
  apply to Wi-Fi SSIDs/passwords and vCard fields per spec, covered
  by per-type unit tests).

**Content types (v1):** text, URL, Wi-Fi, vCard, email, SMS, phone,
geo. **Phase 2+:** calendar events. **Later evaluation (documented,
not committed):** payment QR formats (EMVCo is spec-heavy and
regionally fragmented - explicit owner decision required before
any payment work; roadmap keeps it out of v1 scope).

**Performance:** ZXing matrix generation is sub-millisecond for
v1-v10 payloads; recomputation per keystroke is safe in the
ViewModel (debounced 150ms) with the matrix cached in state.
Rendering a 1000px PNG takes ~10-30ms on mid-range - fine on a
background dispatcher.

**Export/share workflow (v1):** Compose render -> ARGB_8888 Bitmap
at user-chosen resolution (512/1024/2048) -> PNG -> MediaStore
(Android 10+) / FileProvider share. No storage permission on
Android 10+; legacy path documented for 24-28.

**Technical risks (engine):** Wi-Fi/vCard escaping bugs (mitigated
by spec-driven unit tests); ECC capacity edge cases (mitigated by
the capacity table + typed errors); styled rendering breaking
decodability (mitigated by round-trip tests over the STYLE layer in
Phase 2, not just the engine).

---

## UI/UX Architecture Review

Planned journey: Home (recent + quick content types) -> Content
editor -> Live preview studio (customize) -> Export/share.

Verdicts and improvements:

1. **The QR preview must be the visual anchor of the studio** -
   large, always visible, re-rendering on every change. Good as
   planned.
2. **Improvement (accepted into plan):** content type picker belongs
   on the studio screen as a segmented row, not a separate screen -
   faster path from open to code (reduces the two-hop journey).
3. **Improvement (accepted):** export affordance pinned as a
   bottom action bar (Save / Share), thumb-reachable, single column
   on all sizes.
4. **State management:** one StudioViewModel holding an immutable
   `StudioState(content, style, matrix, validation)`; the matrix is
   DERIVED state (cache keyed on content+style), never a second
   source of truth.
5. **Compose strategy:** stateless composables + state hoisting;
   previews for every component in :core:designsystem.
6. **Design system:** M3 dynamic color optional; brand color
   fallback; dark/light from day one.
7. **Accessibility:** touch targets >= 48dp, TalkBack labels on the
   preview ("QR code preview, currently valid/invalid"), contrast
   guard doubles as low-vision support; no color-only signaling.

---

## Data Layer Strategy

**Storage engine:** Room (KSP), schema v1 minimal:

```
history(id PK, content_json, content_type, style_json, title,
        created_at, is_favorite)
templates(id PK, name, style_json, created_at)
settings(key PK, value)   -- primitive app preferences only
```

- content_json/style_json: typed via Moshi (or kotlinx.serialization
  - decided in Phase 0 with the pin-set; either is proven here).
- **Repository approach:** `HistoryRepository` / `TemplateRepository`
  interfaces in :core:model, Room implementations in :core:database,
  bound by Hilt. ViewModels never see Room types.
- **History item = content + style snapshot**, so regenerate is
  byte-identical (testable property).
- **Migration strategy:** `exportSchema = true` from day one; schema
  JSON committed; migrations written per change; destructive
  migration FORBIDDEN after the first public release (history is
  user data). Privacy auto-purge is a scheduled DELETE query, not
  a schema concern.
- **Deferred:** user preferences beyond privacy mode go through
  DataStore in a later phase only if real settings appear (avoid
  speculative infrastructure).

---

## Security & Privacy Review

Threat model: the app touches no network, no accounts, no IDs. The
risks are data-at-rest (history contains the user's QR content -
   possibly Wi-Fi passwords or contact data) and export leakage.

**Privacy checklist (v1 contract, testable):**
1. ZERO network permissions in the manifest (assert in a build
   review step).
2. Zero analytics/ads SDKs in the dependency graph (build review).
3. Manifest contains only: none required on Android 10+ for
   MediaStore save.
4. History is local-only; "privacy mode" (don't remember) is a
   one-tap toggle that generates WITHOUT persisting - unit-tested to
   leave zero rows.
5. No content ever reaches the system clipboard, logs, or
   share sheet except by explicit user action.
6. Share uses FileProvider with a narrow, per-export file grant -
   never blanket storage.
7. R8 minification ON in release (attack surface reduction), rules
   verified per phase - proven workflow from previous projects.
8. Clear in-app privacy statement: "Everything happens on your
   device."

---

## Performance Review

| Area | Analysis | Recommendation |
|---|---|---|
| Generation speed | Sub-ms matrix creation; negligible | Recompute per change, debounced 150ms |
| Large image render | 2048px PNG ~10-30ms | Background dispatcher; no jank path on main |
| Memory | Bitmaps are the only heavy objects | Render-on-demand, no bitmap caching in state (cache the MATRIX, not the bitmap); recycle on export completion |
| Recomposition | Studio screen updates on every keystroke | Matrix as derivedStateOf/state key; stable inputs to composables; no lambda allocations in tight loops |
| Battery | No background work AT ALL | Nothing to schedule; document "no services" as a feature |
| Storage | PNGs only where user saves | Dedup-by-name policy documented in Phase 1 |

---

## Roadmap Validation

- Phase 0 Foundation: correct as first. Missing task added: Git
  hygiene (repo exists now) + renaming folder + applicationId.
  Complexity LOW, risk LOW.
- Phase 1 Core Engine: correct order - engine before styling.
  Missing task added: round-trip decoder test suite as an explicit
  deliverable (was implied, now a completion criterion). Complexity
  MEDIUM, risk MEDIUM (escaping rules).
- Phase 2 Customization: correctly after engine. Risk concentration
  here is real - styled-render round-trip tests are the mitigation.
  Complexity HIGH (the visual pipeline), risk HIGH without tests /
  MEDIUM with them.
- Phase 3 History & Templates: correct - needs content+style models
  stable first. Schema/migration discipline added as completion
  criteria. Complexity MEDIUM, risk LOW-MEDIUM.
- Phase 4 Productivity (batch/CSV): correct as optional post-v1;
  needs the render seam from the Architecture section. Complexity
  MEDIUM, risk MEDIUM (memory).
- Phase 5 Release: correct last. Complexity MEDIUM, risk LOW.

**Order verdict:** no phase reordering required. The critical path
is 0 -> 1 -> 2; phases 3-5 are composable afterwards.

---

## Improved Development Roadmap

Same phases, tightened with the missing items found above:

**Phase 0 - Foundation:** rename folder; wrapper 8.7; version
catalog with pinned set; 10-module graph; applicationId
`com.hjinlabs.sengkode`; M3 theme; type-safe navigation; Hilt+KSP;
proguard-rules.pro + variant separation; serialization library
decision documented. Gate: assembleDebug + assembleRelease green,
0 TODOs, report.

**Phase 1 - Core Engine:** all 8 content encoders with typed
validation + capacity tables; ZXing wrapper; ECC L/M/Q/H;
StudioViewModel + live preview; content editors; export PNG +
MediaStore + share. Gate: round-trip decoder tests 100% green per
content type; engine 100% JVM-covered; device smoke test; report +
APK.

**Phase 2 - Customization Studio:** color pairs + WCAG-derived
scan-safe contrast guard; module shapes; eye colors; center logo
with auto-ECC-upgrade + quiet-zone protection; SCAN ME frames.
Gate: round-trip decode over STYLED renders for every preset;
unsafe combos refused with reasons; golden bitmap tests; report +
APK.

**Phase 3 - History & Templates:** Room v1 schema (above);
history + favorites + regenerate; template gallery; privacy mode.
Gate: DAO tests; regenerate byte-identical test; privacy mode
zero-trace test; migration discipline active; report + APK.

**Phase 4 - Productivity (post-v1, owner-gated):** CSV batch with
per-row validation report; ZIP export; render seam abstraction;
bounded-memory profile. Gate: 100-row fixture green.

**Phase 5 - Release:** localization (EN, TR candidate);
accessibility pass; final R8 verification; store assets; signing
setup. Gate: release green + a11y pass + final report.

---

## Implementation Order

(Execution-ready; each step names Objective/Reason/Modules/
Dependencies/Complexity/Risk/Outcome.)

1. **Project skeleton** - Objective: buildable identity + module
   graph. Reason: everything depends on it. Modules: all Gradle
   files + :app + :core:designsystem + :core:model. Deps: none.
   Complexity LOW, risk LOW. Outcome: green debug+release builds.
2. **Domain models** - Objective: QrContent/QrStyle/ExportSpec
   sealed hierarchies + validation types. Modules: :core:model.
   Deps: step 1. LOW/LOW. Outcome: compilable pure domain.
3. **Content encoders + capacity validation** - Objective: 8 pure
   encoders with typed errors. Modules: :core:qr. Deps: step 2 +
   ZXing dep. MEDIUM/MEDIUM. Outcome: encoder unit tests green.
4. **ZXing wrapper + round-trip test suite** - Objective: matrix
   generation + the generate-decode-assert harness. Modules:
   :core:qr (+ zxing decoder in test scope). Deps: step 3.
   MEDIUM/MEDIUM. Outcome: round-trip suite green.
5. **Studio screen + content editors + live preview** - Objective:
   the create experience. Modules: :feature:generator,
   :feature:content, :core:designsystem. Deps: step 4. MEDIUM/
   MEDIUM. Outcome: live preview updates in real time.
6. **Export pipeline** - Objective: PNG + MediaStore + share.
   Modules: :core:export. Deps: step 5. MEDIUM/MEDIUM. Outcome:
   save + share work on device.
7. **Phase 1 gate** - build, full test run, report, APK.
8. (Phase 2 begins: styling pipeline over the matrix - risk budget
   concentrated here; every subsequent step repeats the gate
   discipline.)

---

## Critical Risks Before Development

1. **Styled-render scannability (HIGH):** the only product-level
   risk that can break the core promise. Mitigation is structural:
   the round-trip harness from step 4 MUST be extended to styled
   renders in Phase 2 - a non-negotiable completion criterion.
2. **Escaping correctness (MEDIUM):** Wi-Fi/vCard payload bugs.
   Mitigation: spec-driven per-type tests with real-world fixtures.
3. **Schema churn (MEDIUM):** Room history changes after release.
   Mitigation: exportSchema + no destructive migrations post-
   release.
4. **:feature:content over-fragmentation (LOW):** merge decision
   deferred to Phase 1 with documentation either way.
5. **Environment drift (LOW):** pinned toolchain + wrapper 8.7
   proven in this environment.

---

## Final Recommendation

Proceed to **Phase 0 (BUILD MODE ON)** under the existing
verification rhythm: green `assembleDebug` + `assembleRelease`,
explicit test counts, 0 TODO/FIXME scan, per-phase reports and
downloadable APKs at every gate. The architecture and toolchain are
validated; the engine choice is decisive because it makes the
product's core promise testable; the risks above all have named
mitigations with phase-gated completion criteria.

**END OF TECHNICAL VALIDATION REPORT**
