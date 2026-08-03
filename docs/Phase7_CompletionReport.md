# Phase 7 — Data Management & Semester Lifecycle: Completion Report

## Status

**COMPLETE.** Compilation, unit tests, and APK build all green.

- `:app:compileDebugKotlin` — BUILD SUCCESSFUL
- `:app:testDebugUnitTest` — 116 tests, 0 failures (24 suites)
- `:app:assembleDebug` — BUILD SUCCESSFUL
- `:app:lintDebug` — Phase 7 warnings fixed (0 remaining); one pre-existing error unrelated to Phase 7 (see below)

---

## 1. What Phase 7 Delivers

| Capability | Entry points |
|---|---|
| **Export backup** (JSON via SAF) | Backup screen "Export backup" |
| **Restore backup** (selective) | Backup screen → Restore from Backup; Settings → Restore from Backup |
| **Preview before restore** | Restore screen validates + previews, then applies |
| **Semester archive** | Backup screen / Settings → Semester Archive; creates, browses, deletes, restores snapshots |
| **Semester reset** | 3-step wizard with live preview counts and typed "RESET" confirmation |
| **Data integrity scan** | Backup screen / Settings → Data Integrity; read-only report of orphan refs / duplicates |
| **Last-backup timestamp** | Backup screen shows most recent export time |

Out of scope (per requirements): no cloud backup, no home-screen widget, no Play Store release prep.

---

## 2. Backup Architecture

Single serialization/validation pipeline owned by `core/backup/BackupManager.kt`:

```
BackupData (DTOs)
   │ BackupSerializer.serialize / BackupDeserializer.deserialize
   ▼
BackupManager.parseBackup(json)
   ├─ blank check           → BackupError.InvalidJson
   ├─ deserialize           → BackupError.InvalidJson
   ├─ schemaVersion check   → BackupError.UnsupportedSchema
   ├─ backupVersion check   → BackupError.OldBackupVersion
   └─ BackupValidator       → ValidationResult.Valid / Invalid → InvalidJson
```

- **`BackupError.kt`** — typed taxonomy: `InvalidJson`, `OldBackupVersion`, `UnsupportedSchema`, `FilePermission`, `StorageUnavailable`, `CorruptedArchive`, `Unknown`.
- **`BackupManager.classifyError(Throwable)`** — maps arbitrary exceptions to `BackupError` (Serialization → InvalidJson, Security/AccessControl/FileNotFound → FilePermission, IO → StorageUnavailable/FilePermission by message, else Unknown).
- **`BackupFileProvider.kt`** — pure, Android-free naming: `safe75_backup_yyyyMMdd_HHmmss.json`, case-insensitive `.json` check.
- **`BackupRepositoryImpl`** — routes every parse+validate through `BackupManager` (`readAndParse` helper); SAF file I/O via `ContentResolver`. No duplicated deserialization/validation logic anywhere.
- **Versions** — `CURRENT_SCHEMA_VERSION = 1`, `CURRENT_BACKUP_VERSION = 1` (`BackupMetadata.kt`).

### JSON schema

```jsonc
{
  "metadata": { "backupVersion": 1, "appVersion": "1.0", "schemaVersion": 1, "createdAt": 1700000000000 },
  "subjects":         [ { "id", "name", "facultyName", "color", "requiredAttendancePercentage",
                          "personalAttendanceGoal", "createdAt", "updatedAt" } ],
  "schedules":        [ { "id", "subjectId", "dayOfWeek", "startTime": "HH:mm:ss", "endTime": "HH:mm:ss",
                          "room", "teacherOverride", "versionId", "createdAt", "updatedAt" } ],
  "semesterVersions": [ { "id", "name", "isActive", "createdAt" } ],
  "attendanceRecords":[ { "id", "subjectId", "scheduleId", "date": "yyyy-MM-dd", "status",
                          "remarks", "createdAt", "updatedAt" } ],
  "settings":         { "themeMode", "notificationsEnabled", "morningReminderEnabled",
                        "attendanceReminderEnabled", "missedReminderEnabled" }
}
```

`LocalTime` → `"HH:mm:ss"`, `WeekDay`/`AttendanceStatus` → enum names, dates → ISO strings.

### Validation strategy (`BackupValidator`)

1. Schema version must be ≤ current (early return).
2. Required metadata: `appVersion` non-blank, `createdAt > 0`.
3. Duplicate IDs in each list (subjects, schedules, attendance, semesterVersions).
4. Referential integrity: schedules → subjects, schedules → semesterVersions, attendance → subjects, attendance → schedules.
5. Status values ∈ {PRESENT, ABSENT, CANCELLED}; `dayOfWeek` ∈ {Monday..Sunday}.

---

## 3. Data Integrity (`feature/integrity`)

`DataIntegrityVerifier` (read-only, `Dispatchers.IO`) scans:

1. Schedules referencing missing subjects (CRITICAL / MISSING_SUBJECTS).
2. Schedules referencing missing semester versions (CRITICAL / MISSING_SEMESTER_VERSIONS).
3. Orphan attendance by missing subject (CRITICAL / ORPHAN_ATTENDANCE).
4. Orphan attendance by missing schedule (CRITICAL / ORPHAN_ATTENDANCE).
5. Missing-schedule references (WARNING / MISSING_SCHEDULES, only when not already covered by #4).
6. Duplicate subject names (case-insensitive) (WARNING / DUPLICATE_IDS).
7. Duplicate attendance keys subjectId+scheduleId+date (WARNING / DUPLICATE_IDS).

`IntegrityRepositoryImpl` → `CheckDataIntegrityUseCase` → `IntegrityViewModel` → `IntegrityScreen` (summary + per-issue list with repair suggestions).

---

## 4. Archive & Reset (`feature/archive`, `feature/backup`)

- **Archive**: `ArchiveEntity` (embedded `subjectsJson`/`schedulesJson`/`attendanceJson`) + `ArchiveDao`; `ArchiveRepositoryImpl.createArchive` snapshots via `BackupManager` serialization. Browser (`ArchiveScreen`), details + restore-with-warning (`ArchiveDetailsScreen`), all in `AppDatabase` v2.
- **Restore**: `RestoreScreen` (OpenDocument → validate → preview → selective restore). `RestoreOptionsDialog` (shared general params + `BackupData` convenience overload) with flags `showPreviewSummary` / `showSemesterVersions` / `showSettings` so the archive restore reuses the same dialog.
- **Reset**: `ArchiveRepositoryImpl.getResetPreview()` returns live record counts (`GetResetPreviewUseCase`). `SemesterResetScreen` is a 3-step wizard: options → preview counts → typed "RESET" confirmation (`ResetConfirmationDialog`, destructive confirm disabled until the user types RESET). `resetSemester()` clears attendance (+ optional schedules/subjects/versions) and archives first.
- **Settings restore on import**: only when `options.restoreSettings` — reapplies theme, notifications, and reminder flags via `SettingsPreferences`.

---

## 5. Navigation & Entry Points

- New routes in `Screen.kt`: `Backup`, `Restore`, `SemesterReset`, `Archive`, `ArchiveDetails(archiveId)`, `Integrity`.
- `AppNavHost.kt` registers all Phase 7 composables; Settings-tab rows push full-screen routes through the root `NavController` via expanded `HomeScreen` callbacks.
- `SettingsScreen.kt` gained a **Data Management** section: Backup & Restore, Restore from Backup, Semester Archive, Semester Reset, Data Integrity.
- `BackupScreen` is the hub (export SAF launcher, last-backup timestamp, entry cards); `BackupViewModel` is shared by Backup/Restore screens.

---

## 6. Files

### Created (main)
```
core/backup/            BackupError.kt, BackupFileProvider.kt, BackupManager.kt
data/integrity/         DataIntegrityVerifier.kt
data/local/database/    entity/ArchiveEntity.kt, dao/ArchiveDao.kt
data/repository/        ArchiveRepositoryImpl.kt, BackupRepositoryImpl.kt, IntegrityRepositoryImpl.kt
di/                     BackupModule.kt
domain/model/           ArchiveData.kt, BackupResult.kt, IntegrityModels.kt,
                        ResetOptions.kt, ResetPreview.kt, RestoreOptions.kt
domain/repository/      ArchiveRepository.kt, BackupRepository.kt, IntegrityRepository.kt
domain/usecase/backup/  ArchiveUseCases.kt, BackupQueryUseCases.kt, CheckDataIntegrityUseCase.kt,
                        ExportBackupUseCase.kt, GetLastBackupTimestampUseCase.kt, ImportBackupUseCase.kt
feature/archive/        ArchiveScreen.kt, ArchiveDetailsScreen.kt, ArchiveViewModel.kt
feature/backup/         BackupDialogs.kt, BackupScreen.kt, BackupViewModel.kt,
                        ResetConfirmationDialog.kt, RestoreScreen.kt, SemesterResetScreen.kt, SemesterViewModel.kt
feature/integrity/      IntegrityScreen.kt, IntegrityViewModel.kt
```

### Created (test)
```
core/backup/   BackupSerializerTest.kt, BackupValidatorTest.kt, BackupManagerTest.kt, BackupFileProviderTest.kt
data/integrity/DataIntegrityVerifierTest.kt
```

### Modified
```
core/navigation/AppNavHost.kt, Screen.kt
data/local/database/AppDatabase.kt (v2: archives table)
data/local/database/dao/AttendanceDao.kt (getAllAttendance, deleteAllAttendance, upsertAttendance)
data/local/database/dao/ScheduleDao.kt (getAllSchedules, deleteAllSchedules, upsertSchedule)
data/local/database/dao/SemesterDao.kt (upsertVersion, deleteAllVersions)
data/local/database/dao/SubjectDao.kt (upsertSubject, deleteAllSubjects)
di/DatabaseModule.kt (ArchiveDao binding)
feature/home/HomeScreen.kt, feature/settings/SettingsScreen.kt
```

### Pre-existing (already in repo at session start, finalized this session)
`core/backup/{BackupData,BackupMetadata,BackupSerializer,BackupDeserializer,BackupValidator}.kt`,
`domain/model/{BackupResult,RestoreOptions,ArchiveData,ResetOptions}.kt`,
`domain/repository/{BackupRepository,ArchiveRepository}.kt`,
`domain/usecase/backup/{ExportBackupUseCase,ImportBackupUseCase,BackupQueryUseCases,ArchiveUseCases}.kt`,
old `BackupScreen`/`BackupViewModel`/`SemesterViewModel`/`SemesterResetScreen`/`ArchiveViewModel`.

---

## 7. Tests

| Suite | Focus |
|---|---|
| `BackupSerializerTest` | round-trip field preservation, valid JSON doc, blank/corrupt rejection, embedded subject-list round trip |
| `BackupValidatorTest` | schema version, metadata, duplicate IDs, referential integrity, status/day-of-week values |
| `BackupManagerTest` | parse pipeline (valid/blank/corrupt/unsupported schema/older backup/invalid refs), error classification, file naming |
| `BackupFileProviderTest` | naming conventions, case-insensitive extension check |
| `DataIntegrityVerifierTest` | healthy DB, orphan schedules/subjects, missing semester, orphan attendance, duplicates, consistent DB |

Result: **116 tests / 24 suites, 0 failures** (`:app:testDebugUnitTest`). Existing suites continued to pass — no completed-feature behavior regressed.

---

## 8. Verification & Notes

- Commands: `gradlew.bat :app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug`.
- Settings-restore path and `setLastBackupTimestamp` wiring verified against `SettingsPreferences` signatures.
- **Pre-existing lint error (not Phase 7):** `AndroidManifest.xml:35` `RemoveWorkManagerInitializer` (Phase 6 notification concern). The manifest already carries the standard `tools:node="remove"` meta-data fix; lint still flags it. Left untouched per "do not modify completed features unless strictly required."
- No commit was made for Phase 7; changes are staged in the working tree awaiting review/commit.
