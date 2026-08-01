# Feature Specification Document

This document provides specifications for the features of **Attendance Tracker**.

---

## 1. Feature Index

| Feature | Scope / Status | Core Purpose |
| :--- | :--- | :--- |
| **Subjects CRUD** | Phase 2 (Target) | Create and configure study courses and credit hour targets. |
| **Weekly Schedule** | Phase 2 (Target) | Define timing slots and classroom locations on standard weekdays. |
| **Attendance Logs** | Phase 2 (Target) | Mark present, absent, or cancelled logs on current/past dates. |
| **Simulators** | Phase 3 (Planned) | Run "What-If" skip calculations without modifying actual logs. |
| **OCR Scanner** | Phase 3 (Planned) | Snapshot timetable sheets and extract courses/timings locally. |
| **Leave Planner** | Phase 3 (Planned) | Block leave dates and preview percentage outcomes in advance. |
| **Archive & Reset** | Phase 3 (Planned) | Store completed semester records and clear dashboards for new terms. |
| **Backup / Sync** | Phase 3 (Planned) | Export/Import settings and db tables as offline JSON data. |

---

## 2. Detailed Specifications

### 2.1 Subjects Management
- **Purpose:** Create, edit, and delete study subjects.
- **Workflow:**
  1. Click the floating action button (FAB) on the Subjects screen.
  2. Input subject name, optional course code, and credit hours.
  3. Save. The item immediately renders in the subjects list.
- **UI Components:** TextFields for names and codes, Dropdowns for credit hours, Cards with indicators.
- **Business Rules:**
  - Subject names must be unique.
  - Credit hours must be positive (1 to 6).
- **Edge Cases:**
  - *Deleting a Subject:* Must trigger a confirmation dialog warning that all attendance logs and schedules will be deleted.
- **Future Improvements:** Custom subject card themes and colors.

### 2.2 Weekly Schedule Timetable
- **Purpose:** Configure weekly class times to send reminders.
- **Workflow:**
  1. Navigate to the Schedule screen.
  2. Tap "+" to select a subject and add weekday timing blocks.
  3. Save. Timetable lists are updated.
- **UI Components:** TimePicker dialog, Dropdowns for weekdays.
- **Business Rules:**
  - Start time must precede end time.
- **Edge Cases:**
  - *Timetable overlaps:* Warn the user about class timing conflicts but do not block scheduling.
- **Future Improvements:** Support bi-weekly timetable cycles.

### 2.3 Attendance Logger
- **Purpose:** Update daily attendance states.
- **Workflow:**
  1. Tap an active class entry on the Dashboard or Schedule view.
  2. Select status: **Present**, **Absent**, or **Cancelled**.
  3. (Optional) Input a text note.
- **UI Components:** Material 3 segment toggle buttons, bottom sheet note editor.
- **Business Rules:**
  - *Cancelled Classes:* Excluded from total calculations.
  - Logs are mapped to actual calendar dates.
- **Edge Cases:**
  - *Double Logging:* Logging attendance twice on the same day updates the existing record rather than creating a duplicate.
- **Future Improvements:** Support quick undo notifications.

### 2.4 Dashboard Overview
- **Purpose:** Present stats at a glance.
- **Workflow:** Runs database aggregation queries and updates Material 3 indicators.
- **UI Components:** Circular progress rings, list warnings.
- **Business Rules:**
  - Displays predictive stats indicating how many consecutive classes a user can skip or must attend to meet targets.
- **Edge Cases:**
  - *No subjects added:* Renders the clean `EmptyState` view prompting setup.

### 2.5 Attendance Simulator
- **Purpose:** Predict future percentages.
- **Workflow:**
  1. Open Simulator from the Dashboard.
  2. Use sliders to simulate prospective skipping/attending numbers.
  3. Preview final percentages immediately.
- **Business Rules:** Does not write to Room tables; operates strictly on in-memory clones.

### 2.6 OCR Timetable Import
- **Purpose:** Automate schedule setup.
- **Workflow:**
  1. Select an image or take a photo of a timetable sheet.
  2. ML Kit extracts text blocks.
  3. The parser maps courses to weekdays and times.
  4. The review screen allows verification before saving.
- **Edge Cases:**
  - *Unrecognized text:* Prompt the user to map fields manually on the review screen.

### 2.7 Leave Planner
- **Purpose:** Plan academic leaves.
- **Workflow:**
  1. Enter prospective leave dates.
  2. The system checks scheduled classes for those dates.
  3. Warns the user if the planned leave will drop attendance below the threshold.
- **Business Rules:** Planned leaves do not impact the actual database percentage until the dates pass.

### 2.8 Backup & Restore
- **Purpose:** Export/import database files locally.
- **Workflow:** Save Room DB and Datastore configs into a readable JSON file.
- **Edge Cases:**
  - *Invalid JSON imports:* Validate file structure and check signature versions before wiping current tables.
