# SENGKODE

Privacy-first, offline QR code studio for Android.

No network access. No analytics. No data leaves the device.

---

## Building locally

**Requirements**

- JDK 17
- Android SDK (API 34 compile, API 24 minimum)
- No emulator required for unit tests

**Debug build**

```bash
./gradlew assembleDebug
```

**Release build** (requires a signing keystore — see `docs/RELEASE_SIGNING.md`)

```bash
./gradlew assembleRelease
```

---

## Running tests

All tests are JVM or Robolectric — no emulator or connected device needed.

**All modules**

```bash
./gradlew test
```

**Single module**

```bash
./gradlew :core:qr:test
./gradlew :feature:history:test
./gradlew :feature:templates:test
./gradlew :feature:generator:test
./gradlew :core:style:test
./gradlew :core:export:test
```

**Test reports** are written to `<module>/build/reports/tests/` after each run.

---

## CI pipeline

Every push to `main` and every merge request runs three automated stages:

| Stage | Command | Runs on |
|---|---|---|
| **build** | `./gradlew assembleDebug` | All branches + MRs |
| **test** | `./gradlew test` | All branches + MRs |
| **release-validation** | `./gradlew assembleRelease` | `main` and `release/*` only |

The pipeline uses Gradle caching keyed on `libs.versions.toml` and the
Gradle wrapper properties. A dependency change automatically invalidates
the cache.

Test results are published as JUnit XML artifacts and surfaced inline on
merge requests. Build reports are retained for 7 days; test reports for
14 days.

**No APK is uploaded or distributed automatically by the pipeline.**

---

## Module structure

```
app/                    Application shell + navigation
core/
  model/               Domain models (QrContent, QrStyle, …)
  qr/                  ZXing engine wrapper (pure JVM)
  style/               Render geometry + safety policy (pure JVM)
  export/              Bitmap renderer + file export (Android)
  database/            Room persistence (Android)
  designsystem/        Shared Compose theme
feature/
  generator/           QR studio screen + ViewModel
  history/             History list + detail screens
  templates/           Template gallery screen
```

---

## Documentation

Detailed phase reports and architecture decisions are in `docs/`.
