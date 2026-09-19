# SENGKODE UI/UX Modernization Audit and Migration Report

**Project:** SENGKODE - offline-first QR code generator (Android)
**Phase type:** AUDIT AND PLANNING ONLY (Build Mode: OFF - no
project files modified except this report)
**Inspected:** actual source at v0.5.0-phase4 (190 tests green)
**Encoding:** ASCII-safe

---

## A. Current Architecture Summary (all VERIFIED)

- **UI toolkit: 100 percent Jetpack Compose + Material 3.**
  `find` over the whole repo returns ZERO XML layouts, ZERO
  Fragments, ZERO RecyclerView adapters, ZERO findViewById, and
  no android.widget usage except Toast. The app was built
  Compose-first in Phases 1-4; there is no legacy View system.
- **Single-activity Compose Navigation:** `AppRoot.kt` hosts one
  NavHost with three bottom NavigationBar destinations
  (Generator/Studio, History, Templates).
- **Stack:** Kotlin 2.0.20, AGP 8.5.0, Compose BOM 2024.09.00,
  compileSdk 34, minSdk 24, Hilt, Room, kotlinx-serialization.
- **MVVM + UDF:** `StudioViewModel` exposes
  `StateFlow<StudioState>` (content, ecc, style, result, safety);
  generation is debounced (150 ms) on a background dispatcher.
  History/Templates have equivalent ViewModels over Room-backed
  repositories.
- **Rendering pipeline (single source of truth):** pure JVM
  `QrStyleRenderer` produces a draw-list; the Android
  `DrawListBitmapRenderer` rasterizes it. Preview and PNG export
  use the SAME pipeline, so the preview is exactly the exported
  file. Runtime logo verification decodes the rendered bitmap and
  requires the exact payload.
- **Design system:** `core/designsystem/Theme.kt` - dual
  light/dark Material 3 schemes, teal primary (0xFF00695C /
  0xFF80D5C5), red tertiary accent, dynamic color opt-in on
  Android 12+.
- **Testing:** 190 unit test executions, 0 failures; round-trip
  generate -> decode -> assert is mandatory for every QR feature.

## B. The Migration Premise: VERIFIED Answer

The prompt asks to plan an incremental XML -> Compose migration.
**That migration is unnecessary because it already happened:**
SENGKODE has no XML UI layer at all. The prompt's final question -
"can the Create QR screen be migrated to Compose independently?" -
is answered: it is ALREADY Compose; the correct scope is an
**in-place Compose UX modernization**, not a migration. Nothing in
sections 5/11 of the prompt (coexistence, XML removal, adapter
replacement) applies. This is stated plainly rather than inventing
migration work.

## C. Current UI/UX Problems (VERIFIED unless noted)

### C.1 Create (Studio) screen - the valid core of the critique

The screen is one long `verticalScroll` Column exposing EVERYTHING
simultaneously, in this order (GeneratorScreen.kt:134-215):

1. TypePicker (FilterChips: Text/URL/Wi-Fi/Contact/...)
2. QrPreview
3. ContentEditor (per-type text fields)
4. EccPicker (chips) + effective-ECC note
5. Five stacked SectionCards: Colors, Shape, Eyes, Logo, Frame
6. Action row 1: [Save to history] [Save as template]
7. Action row 2: [Save] [Share]

Why it reads as a developer panel (all VERIFIED):

- **No progressive disclosure:** 5 appearance cards are always
  fully expanded even though appearance is secondary to content.
- **Flat hierarchy:** SectionCard adds structure, but visually all
  sections have equal weight; nothing signals "this is the main
  path" vs "this is optional customization".
- **Hex text fields:** colors are entered via OutlinedTextField
  ARGB hex strings - the single most developer-oriented control in
  the app. No swatches, no palette, no visual feedback beyond the
  preview.
- **Four equal buttons in two rows** at the bottom: primary
  (Save to gallery) is a plain filled Button sitting next to an
  OutlinedButton, followed by another OutlinedButton/TextButton
  row. Weak affordance, no bottom-anchored action area, easy to
  mis-tap, and the actions scroll away with the content.
- **Switch + Slider logo/frame sections** inline: dense, technical.
- **No hero moment:** preview is correctly near the top, but it
  has no elevation/branding treatment; the empty state (blank
  content) still shows all controls.
- Touch targets, contrast and descriptions are however in good
  shape (Phase 4 a11y pass); the problems are structural, not
  compliance-level.

### C.2 History / Templates

- History: TopAppBar + list + batch export menu - already
  reasonable. OPTIONAL polish: empty state is plain text, no
  illustration; list rows could show the content type as a chip.
- Templates: cards with preview + apply - reasonable. OPTIONAL:
  "Apply" affordance could be a filled tonal button per card for
  clearer hierarchy.

## D. Recommended New UX Structure for Create (RECOMMENDED)

Top-to-bottom, with progressive disclosure:

1. **Preview hero** (pinned visual anchor, elevated card,
   0.72 width -> up to ~0.85, keep aspect 1:1, subtle border;
   logo-verification badge as today).
2. **Type selector:** keep FilterChips (correct M3 pattern) but
   wrap in a labelled section; chips already carry icons? If not,
   OPTIONAL icons per type.
3. **Content editor:** unchanged (per-type fields are right).
4. **ECC:** replace chips with a compact SegmentedButton row
   (L/M/Q/H) - 4 options are exactly the segmented use case.
5. **Appearance:** ONE collapsed-by-default "Customize appearance"
   section containing the five current cards as expandable rows
   (Accordion). Expansion state = rememberSaveable (UI-only, not
   ViewModel state).
6. **Action area:** a sticky-feeling bottom action row (inside
   the scroll, kept last):
   - Primary filled Button: **Save to gallery** (weight 1)
   - Filled-tonal Button: **Share** (weight 1)
   - "Save to history" and "Save as template" move into a compact
     icon/overflow pair (auto-snapshot on export is OPTIONAL -
     behavior change, needs explicit approval).
7. Empty state: when content is blank, preview area shows a
   friendly prompt instead of an error stack (RECOMMENDED).

## E. Material 3 Design System (RECOMMENDED, palette stays)

- Keep the teal/red brand (do not add competing hues).
- Spacing scale: 4/8/12/16/24 dp only (today's 16 + spacedBy(16)
  is consistent; formalize it).
- Corner radius: cards 16 dp, chips/inputs default M3.
- Elevation: preview hero card 1-3 dp; everything else flat
  (tonal surfaces), which fits M3.
- Buttons: filled = primary export; filled-tonal = share;
  outlined/text = secondary record actions.
- Dark theme: already first-class; verify only the new hero card
  in dark (OPTIONAL check).

## F. XML -> Compose Migration Strategy (N/A -> RECOMMENDED plan)

Since no XML exists, the equivalent safe sequence is:

Phase A - Create screen restructure (GeneratorScreen.kt +
StudioSections.kt only; VM and core modules untouched) -> test.
Phase B - History/Templates polish (OPTIONAL; low risk).
Phase C - Device validation + a11y re-audit.

The existing 190-test suite and the generate/decode round-trip
contract are the regression gate for every phase.

## G. Compose Component Architecture (RECOMMENDED)

Reusable in core/designsystem or feature-shared:
- `QrPreview` (exists in GeneratorScreen - hoist reusable parts
  as-is; do NOT duplicate render logic)
- `CollapsibleSection(title, expanded)` - replaces SectionCard
- `ColorSwatchRow(presetColors, selected, onPick)` + advanced hex
  behind a small dialog (keeps the existing hex capability)
- `SegmentedSelector(options)` - ECC (and possibly shapes)
Screen-specific (stay in StudioSections.kt): type picker, content
editors, logo section, frame section.

## H. State Management Strategy (RECOMMENDED - minimal change)

- ViewModel state is already correct UDF; NO change needed.
- UI-only state (section expansion, dialog visibility) stays in
  remember/rememberSaveable, NOT the ViewModel.
- Config changes: preview re-renders from the matrix already held
  in VM state; keep LaunchedEffect keys as-is
  (matrix/style/logoImage) to avoid redundant renders.
- Do not move rendering into composition - keep the existing
  off-main-thread render + Image bitmap flow.

## I. Performance Considerations (VERIFIED + RECOMMENDED)

- Generation is debounced in the VM; preview updates reactively
  without regenerating on recomposition (render is keyed by
  matrix/style/logoImage, not by recomposition).
- Bitmaps are recycled (VERIFIED in current code).
- Collapse sections reduce initial composition cost of 5 cards.
- No new image loading, no new caches needed.

## J. Accessibility (VERIFIED status + RECOMMENDED additions)

Current: content descriptions on previews and icon buttons,
polite live region for logo verification, 48 dp M3 targets, no
color-only state. Add: selected/unselected stateDescription for
segmented ECC and shape selectors; ensure expandable section
headers expose expanded state (Role.Collapse/Expand semantics);
maintain text-scaling resilience (no fixed heights).

## K. Responsive Design (RECOMMENDED)

- Keep verticalScroll + NavigationBar (verified working pattern).
- Preview hero max width ~360 dp so tablets/large phones do not
  stretch it; content column max width OPTIONAL on sw600+.
- No hardcoded dp beyond the spacing scale; large font scale safe
  because all controls wrap.

## L. Risks and Compatibility

- Lowest risk: restructure is Compose-only in 2 feature files.
- Moderate: swatch presets must map to style colors and pass the
  existing ScanSafetyPolicy (only safe presets offered).
- Risk of scope creep: behavior changes (auto-history, preset
  palettes) need explicit approval before implementation.

## M. Files to Modify in a Future Implementation Phase

- feature/generator/.../GeneratorScreen.kt
- feature/generator/.../StudioSections.kt
- feature/generator/src/main/res/values/strings.xml
- core/designsystem Theme.kt (OPTIONAL tokens) / new components
- feature/history & templates screens (OPTIONAL Phase B)

## N. Files That Must NOT Be Modified

- core/qr, core/style (engine, policies, render pipeline)
- core/export (file/batch exporters, verifier)
- core/database, repositories, StudioViewModel, ShareIntake
- app/ AppRoot navigation, MainActivity, manifest
- All existing tests (must stay green, may only be extended)

## O-Q. Implementation Phases, Testing, Roadmap (RECOMMENDED)

1. **Phase A (Create screen):** collapsible Appearance section,
   segmented ECC, color swatches + hex dialog, action-row
   hierarchy, empty-state preview. Test: full 190+ suite green +
   manual device pass (small phone, large phone, dark, 200
   percent font). No logic changes.
2. **Phase B (OPTIONAL):** History/Templates polish. Test: suite
   green + screenshot sanity.
3. **Phase C:** a11y re-audit with TalkBack, release build.

**FINAL STATEMENT:** The Create QR screen requires NO XML-to-
Compose migration - it is already fully Jetpack Compose + Material
3, as is the entire app. The safest path is an in-place,
two-file, Compose-only UX restructure that reuses the existing
ViewModel, rendering pipeline, and test contract unchanged.

**END OF AUDIT REPORT - NO PROJECT FILES MODIFIED**
