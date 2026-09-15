# QR Code Generator - Android - Project Roadmap

**Project:** QR code generator app (name pending - see Section 8)
**Platform:** Android (minSdk 24+, target latest stable)
**Package base:** `com.hjinlabs.<appname>` (final segment = chosen name)
**Status:** Planning / pre-development
**Companion decisions:** Fully offline generation, privacy-first, Jetpack Compose, Clean Architecture (MVVM + UDF), Hilt, Kotlin 2.x, single Activity.
**Encoding:** ASCII-safe.
**Date:** September 15, 2026

---

## 1. Product Vision

A fast, beautiful, privacy-first QR code generator. The user types or
picks content (URL, text, Wi-Fi, contact, etc.), instantly sees a live
QR preview, customizes the look, and saves/shares a high-resolution
image. Generation is 100% on-device: no server, no account, no
tracking, no permissions for the core flow.

**Non-goals (for v1):** scanning/decoding QR codes, cloud sync,
accounts, ads. (Scanning is a documented candidate for a later phase.)

---

## 2. Guiding Principles

1. **Offline-first, privacy-first:** the app must work in airplane
   mode. Zero network calls in the core product. No analytics in the
   core build.
2. **Instant preview:** every customization (color, style, logo,
   error correction) re-renders immediately on the main thread's
   snapshot state.
3. **Correct above all:** generated codes MUST scan. Contrast and
   error-correction safeguards refuse silent failure (e.g. a dark logo
   on a low-ECC code with no safe zone).
4. **Zero-hallucination, fail-safe engineering:** no TODOs, no stubbed
   features shipped, no fake success paths (same discipline as
   previous projects).
5. **Modular Clean Architecture:** MVVM + unidirectional data flow;
   the QR engine is a pure, platform-free Kotlin module, unit-testable
   on the JVM without Android.

---

## 3. Technology Stack

| Layer | Choice | Rationale |
|---|---|---|
| Language | Kotlin 2.x | Current, KSP-based toolchain |
| UI | Jetpack Compose + Material 3 | Live preview UI, dynamic color, dark/light |
| Architecture | MVVM + UDF (StateFlow) | Consistent with prior projects |
| DI | Hilt (KSP) | Proven setup |
| QR engine | ZXing core (`com.google.zxing:core`) | Mature, pure-JVM QR matrix generation; we render the matrix ourselves in Compose (no legacy views) |
| Persistence | Room (KSP) | History + templates |
| Image export | Compose Canvas -> Bitmap -> PNG (MediaStore on Android 10+, FileProvider share) | High-res export at user-chosen size |
| Build | Gradle version catalog, R8 rules from day one | Release-ready from the start |

**Rendering approach (important):** ZXing only produces the raw
`BitMatrix` (the module grid). All visual styling (colors, shapes,
logo overlay, frames) is our own Compose render pipeline over that
matrix - this is what makes deep customization possible.

---

## 4. Module Structure

```
:app                     entry point, navigation, Hilt wiring
:core:model              pure domain models (QrContent, QrStyle, ExportSpec)
:core:qr                 pure engine: content encoders + ZXing wrapper
                         + validation rules (JVM-only, 100% unit-testable)
:core:designsystem       theme, reusable components (same pattern as before)
:core:database           Room: history, favorites, templates
:core:export             bitmap/PNG rendering + MediaStore + share
:feature:generator       the main create screen (live preview studio)
:feature:content         content-type editors (URL/text/Wi-Fi/vCard/...)
:feature:history         history + favorites + regenerate
:feature:templates       style template gallery
```

---

## 5. Content Types (v1 scope)

1. Plain text
2. URL (with scheme normalization)
3. Wi-Fi join (WPA/WEP/nopass, escaped SSID/password per spec)
4. vCard contact (MECARD or vCard 3.0 format)
5. Email (mailto with subject/body)
6. SMS (SMSTO:) and phone (TEL:)
7. Geo location (GEO:lat,lng)
8. Calendar event (VEVENT - candidate for Phase 2 if scope tightens)

Every encoder lives in `:core:qr` as a pure function returning the
payload string + metadata (bytes used, recommended ECC). These are the
highest-value unit tests in the project: Wi-Fi escaping, vCard field
escaping and payload size vs ECC are where correctness bugs hide.

---

## 6. Development Phases

### Phase 0 - Foundation (target: buildable skeleton)
- Version catalog, module graph, Hilt + KSP, Compose theme shell,
  single-Activity navigation (type-safe routes, same pattern as
  previous project), R8 rules file, debug/release variant separation.
- **Done when:** `assembleDebug` + `assembleRelease` green; empty
  screens render; CI-style verification command documented.

### Phase 1 - Core Generation Engine (the product's heart)
- Pure `:core:qr` engine: all v1 content encoders + ZXing BitMatrix
  wrapper; ECC levels L/M/Q/H; quiet-zone sizing; payload-length
  validation (reject what cannot be encoded at the chosen ECC).
- Live preview screen: content editor + instant QR preview at ECC/size
  changes.
- Export: PNG at chosen resolution, save to gallery, share sheet.
- **Done when:** every content type generates a scannable code
  (verified against a reference decoder in JVM tests), export and
  share work on a device, engine is 100% covered by unit tests.

### Phase 2 - Customization Studio
- Foreground/background color pairs with WCAG contrast guard (refuse
  or warn below a scan-safe contrast ratio).
- Module shapes: square, rounded, dot; custom eye (corner finder
  pattern) colors.
- Center logo embed with automatic ECC upgrade + quiet-zone
  protection (the app guarantees the code still scans).
- Style presets and "SCAN ME" frame/label rendering.
- **Done when:** every styled export passes the scannability
  validation suite (golden bitmap tests + decoder round-trip).

### Phase 3 - History & Templates
- Room-backed generation history (content + style + timestamp),
  favorites, one-tap regenerate; optional privacy auto-purge
  ("don't remember anything" mode).
- Template gallery: saved style presets, apply to any content.
- **Done when:** history survives restart, regenerate reproduces the
  exact code, privacy mode leaves zero traces (unit-tested).

### Phase 4 - Batch & Productivity (post-v1 candidate)
- CSV import -> batch generation -> ZIP export; per-row validation
  report; progress + failure isolation.
- **Done when:** a 100-row CSV produces 100 valid PNGs (or a precise
  per-row failure report) without OOM on a mid-range device.

### Phase 5 - Polish & Release Readiness
- Localization (English first, Turkish candidate), dynamic color,
  accessibility ( TalkBack labels, minimum touch targets), final
  R8/proguard verification, Play Store listing assets.
- **Done when:** release build green, store-ready APK produced,
  report written, all phases marked DONE (same reporting style as
  previous projects).

---

## 7. Testing Strategy

- **JVM unit tests:** content encoders (escaping, spec compliance),
  payload/ECC validation, contrast guards, Room logic (fake DAOs).
- **Golden/round-trip tests:** render styled matrices -> decode with
  a reference QR decoder -> assert payload equality (the strongest
  guarantee a generator can have).
- **ViewModel tests:** Turbine, fake repositories (proven setup).
- **UI tests (later):** Compose tests for the editor + preview loop.

---

## 8. Name Candidates (global, brandable, Singapore-flavored)

The name must be globally pronounceable and effectively non-existing
(as a known tech brand). Package name = `com.hjinlabs.<name>`.

1. **Kilat** (kee-laht) - Malay for "lightning"; Singapore's national
   language is Malay (also on the SG crest). Fast + snappy, exactly
   like QR generation. Short, global, almost zero brand collision.
   Package: `com.hjinlabs.kilat`
2. **Merqode** (mer-kohd) - Merlion + code. Fully invented,
   unmistakably Singapore-flavored, zero collision, tech-friendly.
   Package: `com.hjinlabs.merqode`
3. **Pindai** (pin-dye) - Malay for "to scan". Literal product
   meaning, soft and friendly sound, easy to say worldwide.
   Package: `com.hjinlabs.pindai`
4. **Lionkode** (lyen-kohd) - Lion City + Malay "kode". Tells the QR
   story by itself; slightly longer but very memorable.
   Package: `com.hjinlabs.lionkode`
5. **Sengkode** (seng-kohd) - "Seng" (a distinctly Singaporean
   Hokkien prefix, cf. Sengkang) + "kode". Invented, rhythmic,
   local flavor with global simplicity.
   Package: `com.hjinlabs.sengkode`

**Recommendation:** "Kilat" - shortest, easiest to pronounce in every
market, meaningful (speed), and the safest trademark/collision profile.
Runner-up: "Merqode" for a more distinctive, brandable identity.

---

## 9. Immediate Next Steps

1. User picks the final name (5 candidates above).
2. Rename the project folder + set `applicationId` accordingly.
3. Execute Phase 0 (foundation skeleton).
4. Phase-by-phase delivery with green builds, test counts, reports
   and APK artifacts at every gate - same working rhythm as the
   previous project.

**END OF ROADMAP**
