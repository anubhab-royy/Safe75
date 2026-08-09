# Project Completion Audit Report

**Project:** Safe75 (`com.attendance.tracker`)
**Version audited:** 1.0.0 (versionCode 1)
**Audit date:** 2026-08-02
**Audit type:** Code-level technical audit (read-only, evidence-based)
**Scope:** Full codebase (`:app`, `:benchmark`), build configuration, docs, release artifacts

---

## 1. Executive Summary

Safe75 is an offline-first, single-activity Android app built with
Kotlin + Jetpack Compose (Material 3), Hilt, Room, DataStore, WorkManager,
kotlinx.serialization, and ML Kit OCR. It delivers all 8 planned roadmap phases
(subject/schedule/attendance management, attendance intelligence, reminders,
data management/backup, home-screen widget, baseline profile, release hardening).

The codebase is well-organized (clean feature/domain/data/core layering),
compiles cleanly, passes the Android unit suite and `lintDebug`, and produces an
R8-minified release APK. Explicit bug reports use a durable Room queue and
HMAC-authenticated backend uploads. No critical code defects were found in the
available local validation environment.

The audit identified **1 HIGH, 6 MEDIUM, and 12 LOW** findings. The Android
Auto Backup exposure has been resolved; the remaining HIGH finding is the
documented plaintext user-selected backup export limitation. Live device and
MongoDB/R2 certification remain deployment-gate checks.

---

## 2. Project Statistics

| Metric | Value |
| :--- | :--- |
| Kotlin files (excl. build/.git) | 239 |
| Total lines | 19,348 |
| Non-blank lines | 17,472 |
| Java files | 0 |
| Gradle modules | 2 (`:app`, `:benchmark`) |
| Source sets | `main` (210 files / 16,266 lines), `test` (28 files / 3,046 lines) |
| Room entities / DAOs | 5 / 5 |
| Repository interfaces (all bound) | 8 |
| Use cases | ~45 |
| Unit test files / tests | 28 / **116 passing** |
| Instrumented tests | 0 (no `androidTest` sources) |
| CI pipelines | **none** |
| Release APK size | 40.9 MB (debug 60.8 MB) |
| Docs | 17 markdown files |

### 2.1 Main source-set breakdown by package area

| Area | Files | Lines |
| :--- | ---: | ---: |
| `feature/` | 50 | 9,005 |
| `domain/` | 70 | 1,768 |
| `core/` | 48 | 2,616 |
| `data/` | 33 | 2,398 |
| `di/` | 7 | 298 |
| `com.attendance.tracker` (root) | 2 | 181 |

### 2.2 Test breakdown

| Area | Files | Lines |
| :--- | ---: | ---: |
| `domain/` | 11 | 1,135 |
| `feature/` | 7 | 988 |
| `core/` | 6 | 513 |
| `data/` | 4 | 410 |

---

## 3. Architecture

### 3.1 Layering

The app follows a strict 4-layer structure with unidirectional dependencies:

```
feature/ (Compose UI + ViewModels)
   -> domain/ (use cases, models, validators, repository interfaces)
   -> data/ (repository impls, datasources, Room, DataStore, integrity)
   -> core/ (ui theme/components, model, navigation, util, backup, workers, widget)
```

- 8/8 repository interfaces have implementations and are DI-bound
  (`di/RepositoryModule.kt`, `di/BackupModule.kt`).
- 5 LocalDataSource interfaces/impls are DI-bound (`di/DataSourceModule.kt`).
- ViewModels expose `StateFlow` (via `MutableStateFlow.asStateFlow()` and
  `combine(...).stateIn(...)`); repositories emit Room `Flow` mapped to domain
  models. Idiomatic and consistent.
- 7 Hilt modules, all `@InstallIn(SingletonComponent::class)`; no scope misuse.
  Workers use `@HiltWorker` + `@AssistedInject` wired through `HiltWorkerFactory`
  (`MainApplication.kt:19-25`).

### 3.2 Navigation

- Navigation Compose 2.7 with **string routes** defined in a `Screen` sealed
  class (`core/navigation/Screen.kt`); `AppNavHost.kt` (306 lines) wires a
  two-level graph: splash -> welcome -> `main_graph`, with a **nested child
  `NavHost`** inside `HomeScreen` for the 4 tabs.
- **No deep links** anywhere (grep: 0 matches). Widget cold-start uses an
  intent extra (`WidgetIntent.EXTRA_DESTINATION`) + post-composition navigate.
- One route (`ocr_review?isTimetable=...&uri=...`) is a raw inline string
  outside the `Screen` sealed class (`AppNavHost.kt:222,228`) — inconsistent.
- String-route navigation means no compile-time route safety; the
  `kotlin.serialization` plugin is present but type-safe `@Serializable` routes
  are not used.

**Verdict: Good.** Layering, DI, and state management are clean and consistent.
Minor debt: string-route nav (no deep links, one raw route).

---

## 4. Database Design

### 4.1 Schema (Room, version 2, `exportSchema = false`)

5 entities: `SubjectEntity`, `ScheduleEntity`, `SemesterVersionEntity`,
`AttendanceEntity`, `ArchiveEntity`.

- **Foreign keys (CASCADE):** subject -> schedule; subject/version -> schedule;
  subject -> attendance; schedule -> attendance. `ScheduleEntity.kt:14-29`,
  `AttendanceEntity.kt:15-30`.
- **Indices:** all FK columns indexed; unique composite index on
  `(subjectId, scheduleId, date)` enforces one-log-per-class-per-day
  (`AttendanceEntity.kt:34`); unique index on subject `name`
  (`SubjectEntity.kt:14`). Search query on attendance joins subjects + LIKE
  (`AttendanceDao.kt:49-55`).
- **Type converters** for `WeekDay`, `LocalDate`, `LocalTime`
  (`Converters.kt`).
- **Archive** stores self-contained JSON snapshots (subjects/schedules/
  attendance) so archives survive semester resets (`ArchiveEntity.kt`).
- DAOs use `Flow` for observations and `suspend` for mutations; `ABORT` on
  conflict for normal inserts, `REPLACE` only for restore/upsert paths.

### 4.2 Migration strategy

- `DatabaseModule.kt:34` uses **`fallbackToDestructiveMigration(true)`** — on
  any future schema bump, the existing DB (subject/faculty names, attendance
  history) is silently destroyed.
- This is explicitly documented as a **development-only** strategy in
  `docs/DatabaseDesign.md` §4.1, with a production plan (§4.2: explicit
  `Migration` objects + `exportSchema = true`). The codebase has not yet
  switched. As a v1.0.0 with a fresh-install user base this is acceptable but
  must be fixed before the first schema version bump post-release.

**Verdict: Good, with a known debt.** Schema quality is high (FKs, unique
constraints, indices). Destructive migration is a documented, pre-agreed
trade-off for 1.0.0.

---

## 5. Code Quality & Consistency

### 5.1 Strengths

- No `TODO`/`FIXME`/`HACK`/`XXX` comments in `main` sources (0 matches).
- Kotlin code style (`official`) and `.editorconfig` present; consistent
  formatting.
- `UiState` sealed class (Loading/Success/Error/Empty) used in Subject and
  Schedule flows; error surfaces reviewed in Phase 8 accessibility/error-state
  audit.
- Material 3 design system centralized in `core/ui/theme` (Color, Shape,
  Type, Spacing, Elevation, Dimensions) + shared components in `core/ui/components`.

### 5.2 Findings

| ID | Severity | Finding | Location |
| :-- | :--- | :--- | :--- |
| Q1 | LOW | No string resources app-wide — every UI label, notification title, and channel name is a hardcoded English literal. `res/values/strings.xml` has only 4 entries. No i18n/localization possible. | all `feature/**`, `TrackerNotificationManager.kt:19-66` |
| Q2 | LOW | Magic numbers duplicated: attendance targets `75`/`85` in ~6 sites; safety-status hex colors (`0xFF2E7D32` etc.) duplicated ~6 times; `840.dp` breakpoint. | `AttendanceViewModel.kt:209`, `DashboardViewModel.kt:135`, `WidgetDataProvider.kt:42`, screens |
| Q3 | LOW | Duplicated logic: `DayOfWeek->WeekDay` mapping 3x; safety color mapping 2x; "Unknown Subject"/grey fallback in 4 VMs. | `AttendanceViewModel.kt:119-127`, `DashboardViewModel.kt:208-216`, `WidgetDataProvider.kt:73-81` |
| Q4 | LOW | God-composable files: `ScheduleScreen.kt` (532, inline dialog), `OCRReviewScreen.kt` (491), `DashboardScreen.kt` (457). | feature screens |
| Q5 | LOW | Dead code: `core/util/Constants.kt` (unused), `SemesterEntity`/`Semester`/`SemesterValidator` (unregistered/dead), `SettingsRepositoryImpl.get/updateAttendanceTarget` stub, placeholder files (Backup/Semester/Planner models, `Screen.Semester`/`Screen.Planner` routes unreachable), duplicate SQL in `AttendanceDao` (`getAttendanceByDate` vs `observeTodayAttendance`). | various |
| Q6 | LOW | Muted error handling: `ScheduleViewModel` swallows exceptions to `emptyList()` / `false` (`:150-151,218-220`); restore/deserializer `runCatching{}.getOrNull()` silently skips corrupt rows. | `ScheduleViewModel.kt`, `BackupRepositoryImpl.kt:193-218`, `BackupDeserializer.kt:47-75` |
| Q7 | LOW | Race-prone `stateIn(...).value` read in `getAttendanceById` may return initial `emptyList()`. | `AttendanceViewModel.kt:333-335` |

**Verdict: Good overall; LOW-severity debt.** No structural or maintainability
blockers, but Q1 (i18n) is the largest quality gap.

---

## 6. UI/UX & Accessibility

### 6.1 UX

- Full Material 3: dynamic color on API 31+, custom light/dark palettes,
  Cards, Tabs, FilterChips, TimePicker, NavigationRail/NavigationBar,
  CenterAlignedTopAppBar, Snackbars, animated reset wizard.
- **Adaptive layout:** `BoxWithConstraints` switches bottom `NavigationBar` to
  `NavigationRail` at `>= 840.dp` (`HomeScreen.kt:72-116`). No
  `WindowSizeClass`; no adaptive list/detail panes elsewhere.
- Legacy `android.app.DatePickerDialog` used in 3 screens instead of the M3
  `DatePicker` (minor inconsistency).

### 6.2 Accessibility (verified in Phase 8)

- All 29 `contentDescription = null` Icon sites reviewed: every one is
  decorative (icon beside text, search leading icon, dropdown affordance).
  Interactive icons are labeled ("Clear search", "Sort Options", etc.).
- `supportsRtl="true"`; content is scrollable; dialogs provide cancel paths.

### 6.3 Confirmed bugs

| ID | Severity | Finding | Location |
| :-- | :--- | :--- | :--- |
| U1 | MEDIUM | **Theme setting has no effect.** The settings screen persists `themeMode` (SYSTEM/LIGHT/DARK) via DataStore, but `MainActivity.kt:47-49` always calls `AttendanceTrackerTheme { ... }` with default args, so the selected theme is never applied. The preference is written and restored (even across backup import) but ignored at the root. | `MainActivity.kt:47-49`, `SettingsScreen.kt:195-205`, `SettingsViewModel.kt:72-74` |
| U2 | LOW | Splash and Welcome screens are placeholder text ("Splash Screen", "Welcome Screen") — no branding or onboarding content. | `feature/splash/SplashScreen.kt:35`, `feature/welcome/WelcomeScreen.kt:39` |

**Verdict: Good UX with solid accessibility; one user-visible bug (U1 theme**
**toggle) that should be fixed for 1.0.0.**

---

## 7. Security & Privacy

### 7.1 Confirmed positive controls

- **Network access is scoped** — the app uses network access for device
  enrollment and explicit bug-report uploads only. Attendance records are not
  included in the submission model.
- **Logger is a release no-op** and never logs PII: `Logger.kt` uses
  `DefaultLogEngine` only in DEBUG builds, `NoOpLogEngine` in release. The
  network interceptor is limited to BASIC request logging and never logs
  descriptions, diagnostics, screenshots, or HMAC headers.
- **All broadcast receivers are `exported="false"`** (`AttendanceActionReceiver`,
  `AttendanceWidgetReceiver`, `WidgetSyncReceiver`); all `PendingIntent`s use
  `FLAG_IMMUTABLE`. No custom permissions needed.
- **Backup I/O is SAF-only** — files are written through
  `contentResolver.openOutputStream(uri)` on a user-chosen URI. The app never
  touches external/Downloads storage directly.
- WorkManager + StrictMode (debug-only) configured correctly.

### 7.2 Findings

| ID | Severity | Finding | Location |
| :-- | :--- | :--- | :--- |
| S1 | **RESOLVED** | Automatic Android app-data backup is disabled and explicit cloud/device-transfer exclusion rules are registered. | `AndroidManifest.xml`; `res/xml/backup_rules.xml`; `res/xml/data_extraction_rules.xml` |
| S2 | **HIGH** | **Backups are plaintext JSON containing PII, with no integrity/authenticity protection.** `BackupSerializer.kt:17-21` pretty-prints JSON (subject names, `facultyName`, `teacherOverride`/`room`, attendance `remarks`). No encryption (no SQLCipher/crypto deps) and no checksum/HMAC/signature — a file can be forged or modified and `BackupValidator` only checks structure. If the user picks Downloads/Drive, PII lands there unprotected. | `core/backup/BackupSerializer.kt`, `BackupData.kt`, `BackupValidator.kt` |
| S3 | MEDIUM | **`AttendanceActionReceiver` runs a fire-and-forget coroutine without `goAsync()`** and discards the validation result — the process may be killed mid-write, and duplicate/failed marks are silently swallowed. | `AttendanceActionReceiver.kt:41-50`, `:49` |
| S4 | LOW/MED | Notification channels/titles and all UI strings are hardcoded English (user-visible in OS Settings). Already recorded as Q1. | `TrackerNotificationManager.kt:19-66` |

**Certification update:** Strong baseline with scoped network access, no release
**PII logging, disabled automatic backup, SAF-only user backup I/O, and locked-down**
**receivers. S2 remains a documented limitation of user-selected plaintext backup**
**exports; S3 remains a future robustness item.**

---

## 8. Performance & Optimization

### 8.1 Build & runtime

- R8 minify + shrink enabled for release (`isMinifyEnabled`, `isShrinkResources`),
  `proguard-rules.pro` (59 lines) covers serialization, Room, Hilt, WorkManager,
  ML Kit, Glance, receivers. `assembleRelease` succeeds; **release APK 40.9 MB**
  (debug 60.8 MB).
- Gradle build cache + configuration cache enabled; JDK 17 toolchain;
  non-transitive R class.
- Splash screen via `core-splashscreen`; startup classes kept.
- **Baseline profile** ships with the app (`app/src/main/baseline-prof.txt`,
  2 plain class rules: `MainApplication`, `MainActivity`). It is minimal —
  generated by hand, not yet measured via Macrobenchmark.
- **Benchmark module `:benchmark`** is scaffolded and compiles
  (`BaselineProfileGenerator` using `BaselineProfileRule`), producing
  `benchmark-benchmark.apk`. The profile has **not yet been generated on a
  device** (`:benchmark:connectedCheck` deferred to post-release/CI).

### 8.2 Runtime concerns

| ID | Severity | Finding | Location |
| :-- | :--- | :--- | :--- |
| P1 | LOW | **No lifecycle-aware flow collection** — all 45 collection sites use `collectAsState()`; `collectAsStateWithLifecycle()` (dep present) is unused, so flows keep collecting while backgrounded. | all screens |
| P2 | LOW | `scanImage`'s ML Kit `await()` bridge has no cancellation handler, and EXIF rotation is only handled on the `scanBitmap` path. | `feature/ocr/scanner/OcrScanner.kt:26-59` |
| P3 | LOW | OCR save does per-row (unbatched) DB inserts without `withTransaction`. | `OcrReviewViewModel.kt:132-210` |
| P4 | LOW | 40.9 MB release APK exceeds the 15 MB roadmap goal (ML Kit OCR + `material-icons-extended`). Documented in ReleaseChecklist as a post-1.0 optimization (not a blocker). | — |

**Verdict: Good.** Release build is minified and passes lint. Startup
optimization work (async notification channels, StrictMode off in release) is
in place. Remaining items are post-1.0 tuning.

---

## 9. Testing

### 9.1 Coverage

- **116 unit tests across 28 files, all passing** (`:app:testDebugUnitTest`),
  covering: use cases (attendance, planner, schedule, subject), validators
  (Subject/Schedule/Attendance), backup (serializer, validator, manager, file
  provider), data integrity verifier, repository impls, OCR parser/review VM,
  and 4 feature ViewModels (Dashboard, Attendance, Schedule, Subject,
  OCR review).
- Test stack: JUnit4, Mockito, kotlinx-coroutines-test — appropriate and
  lightweight; `DispatcherProvider` enables IO-dispatcher injection.
- **No instrumented tests** (`androidTest` empty). `uiautomator` dependency was
  added (Phase 8) for future widget/accessibility smoke tests, but none exist.
- **No CI pipeline** — there are no `.github/workflows`; only issue/PR
  templates. No automated gate runs lint/tests/build on commit.

### 9.2 Gaps

| ID | Severity | Finding |
| :-- | :--- | :--- |
| T1 | MEDIUM | No instrumented tests (UI/Room integration/widget); the widget, navigation graph, and Room schema are untested at runtime. |
| T2 | LOW | No CI — lint + unit tests + release build are only run manually. |
| T3 | LOW | No Room schema/migration tests (`exportSchema = false` prevents schema-verification testing). |

**Verdict: Good unit coverage for the domain/data logic; instrumentation and CI**
**are the biggest testing gaps.**

---

## 10. Documentation

- **17 markdown docs**, including design (SystemArchitecture, DatabaseDesign,
  FeatureSpecification, Navigation, Wireframes, UserFlow, DesignSystem),
  process (Roadmap, TestingStrategy, ProjectRequirements, Branding,
  CONTRIBUTING), phase reports (Phase 7, Phase 8), and release (ReleaseChecklist,
  PlayStoreListing, PrivacyPolicy).
- `README.md`, `CHANGELOG.md` (`[1.0.0] - 2026-08-02`), `SECURITY.md`,
  `CODE_OF_CONDUCT.md`, `LICENSE` present.
- Docs accurately reflect the codebase **except**:
  - `docs/DatabaseDesign.md` ER diagram is stale (describes a legacy `semesters`
    schema with `semester_id` on subjects that no longer exists).
  - Phase 8 report documents `compileDebugKotlin`/QA as green — confirmed.
- Privacy Policy and Play Store listing are written and ready.

**Verdict: Excellent.** Docs are a project strength; only the DB ER diagram is
stale.

---

## 11. Release Readiness

| Check | Status |
| :--- | :--- |
| `assembleRelease` (R8, shrink) | PASS — signed APK 40.9 MB |
| `testDebugUnitTest` (116) | PASS |
| `lintDebug` | PASS (BUILD SUCCESSFUL; 1 pre-existing `RemoveWorkManagerInitializer` warning documented, non-blocking) |
| `assembleDebug` | PASS |
| `:benchmark:assemble` | PASS |
| Configuration cache | PASS (stored) |
| versionCode/versionName | 1 / 1.0.0 |
| Release signing | Debug keystore fallback when `keystore.properties` absent (documented) |
| Baseline profile | Ships, minimal (2 rules) |
| Play Store materials | PlayStoreListing.md, PrivacyPolicy.md ready |

**Not blocking for 1.0.0:** S1/S2 (privacy hardening), benchmark profile
generation on device, CI, instrumented tests — all tracked as post-release work.

**Required before store submission (per ReleaseChecklist):** create
`keystore.properties` with a real release key; re-verify on a physical device;
resolve the S1 auto-backup exposure.

---

## 12. Technical Debt Register

| ID | Sev | Item | Effort |
| :-- | :-- | :--- | :--- |
| S1 | HIGH | Wire backup rules / disable cloud backup of PII DB | S |
| S2 | HIGH | Decide on backup encryption or document plaintext trade-off | M |
| U1 | MED | Apply persisted theme setting at the root | S |
| D1 | MED | Replace destructive migration with explicit `Migration`s + `exportSchema=true` before first schema bump | S |
| M1 | MED | Restore path: wrap in `withTransaction`, avoid `REPLACE` cascade-delete | M |
| M2 | MED | OCR attendance import: link real schedules instead of `scheduleId=0` + backdated dates | M |
| M3 | MED | `AttendanceActionReceiver`: `goAsync()` + surface validation result | S |
| Q1 | LOW | Extract strings to resources + localize | M |
| Q2 | LOW | Centralize constants (targets, colors, breakpoints) | S |
| Q5 | LOW | Delete dead code (Constants.kt, Semester leftovers, placeholders, unused routes) | S |
| P1 | LOW | Switch to `collectAsStateWithLifecycle` | S |
| T1 | LOW | Add instrumented tests + CI | M |
| P4 | LOW | Reduce APK size (split icons, review ML Kit strategy) | M |

---

## 13. Scorecard

| Area | Score | Notes |
| :--- | :---: | :--- |
| Architecture & Layering | **9/10** | Clean 4-layer, all repos bound, idiomatic Flow/StateFlow |
| Database Design | **8/10** | Strong schema; destructive migration is known debt |
| Code Quality | **8/10** | Consistent, no TODOs; i18n + duplication debt |
| UI/UX & Accessibility | **8/10** | Polished M3; theme toggle bug; placeholder splash |
| Security & Privacy | **7/10** | Excellent offline/locking baseline; 2 HIGH privacy items |
| Performance | **8/10** | R8 + shrink + startup work; profile/APK tuning deferred |
| Testing | **7/10** | 116 passing unit tests; no instrumentation, no CI |
| Documentation | **9/10** | Excellent; one stale ER diagram |
| Release Readiness | **9/10** | All builds/lint/tests green; signing + privacy follow-ups |
| **Overall** | **8.1/10** | **Production-ready with documented follow-ups** |

---

## 14. Verdict

**APPROVED for v1.0.0 release**, subject to deployment-gate validation:

1. **S1:** Resolved by disabling automatic backup and registering explicit
   `dataExtractionRules`/`fullBackupContent` exclusions.
2. **S2 (HIGH):** The plaintext user-selected backup trade-off is documented
   in the privacy policy and remains a known limitation.
3. Complete physical-device and live MongoDB/Cloudflare R2 validation with the
   production endpoint and signing key.

The codebase is well-engineered, offline-first, tested, documented, and builds
a clean R8 release artifact. No critical defects were found locally. All
remaining findings are tracked in the debt register (§12) or
`docs/release/KnownLimitations.md`.
