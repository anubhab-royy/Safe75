# Testing Strategy Document

This document defines the testing methodology, tools, execution frameworks, and quality gates for **Safe75**.

---

## 1. Testing Pyramid Overview

We employ a balanced testing structure to verify correctness at every level of our clean architecture:

| Test Type | Target Layer | Execution Framework | Focus Area |
| :--- | :--- | :--- | :--- |
| **Unit Tests** | Domain Use Cases & Validators | JUnit 5, Mockk | Business logic validity, timing boundaries. |
| **Database Tests** | Data / Room Schema | AndroidX Test, Robolectric | SQLite constraint validation, transaction cascades. |
| **Integration Tests** | ViewModels / Repository | Mockk, Turbine | Flow emissions, UI state transitions. |
| **UI Tests** | Compose Screens | Compose UI Test, Espresso | Composed views rendering, user click flows. |

---

## 2. Testing Details

### 2.1 Domain Unit Testing
- **Target:** Use Cases (e.g. `AddSubjectUseCase`) and Validators (`ScheduleValidator`).
- **Dependencies:** Mocked using **Mockk**.
- **Rules:**
  - Tests run on the local JVM for quick feedback (under 1 second).
  - Use `DispatcherProvider` to inject test dispatchers (`UnconfinedTestDispatcher`).

### 2.2 Database Testing
- **Target:** Room DAOs and schema migrations.
- **Execution:** Runs in-memory via Robolectric or instrumentation.
- **Verification Criteria:**
  - Cascading deletes of a subject must wipe all schedules and attendance records.
  - Unique constraint violations must throw a `SQLiteConstraintException`.

### 2.3 Settings & Preference Testing
- **Target:** `SettingsPreferences` DataStore persistence.
- **Approach:** Use a temporary file-based DataStore instance in tests to isolate preferences from the device settings.
- **Verification Criteria:**
  - Flow values update correctly after write transactions.

### 2.4 OCR Timetable Parser Testing
- **Target:** `OcrParser` mapping algorithms.
- **Approach:** Mock ML Kit text-block extraction arrays and feed them into the parser.
- **Verification Criteria:**
  - Correct identification of days and timings in various grid formats.

### 2.5 Backup JSON Validation Testing
- **Target:** JSON serialization and deserialization.
- **Approach:** Parse mock valid and corrupted JSON files.
- **Verification Criteria:**
  - Corrupted schemas are rejected, and schema version updates trigger notifications.

---

## 3. Release Quality Checklist

Before building release bundles (APKs/AABs) for distribution:
- [ ] Code compiles cleanly with zero errors.
- [ ] All unit tests pass.
- [ ] DB migration tests pass.
- [ ] Proguard obfuscation rules are verified.
- [ ] APK size is verified as within targets.
- [ ] No hardcoded passwords, keys, or debug flags exist in build properties.
