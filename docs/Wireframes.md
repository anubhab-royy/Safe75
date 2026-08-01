# Wireframes Document

This document contains low-fidelity ASCII wireframes for the **Attendance Tracker** screens.

---

## 1. Splash Screen
- **Purpose:** Brand identification and launch sequence.
- **Navigation:** Automatic routing to Welcome or Home screen.

```text
+------------------------------------------+
|                                          |
|                                          |
|                                          |
|                 [ Logo ]                 |
|                                          |
|            ATTENDANCE TRACKER            |
|               "Safe at 75%"              |
|                                          |
|                                          |
|                  ( % )                   |
|                                          |
|                                          |
+------------------------------------------+
```

---

## 2. Welcome & Onboarding Screen
- **Purpose:** Introduce features and complete initial settings.
- **Navigation:** Pressing "GET STARTED" navigates to the Home screen.

```text
+------------------------------------------+
|  Welcome to Attendance Tracker           |
|                                          |
|  [ Timetable Illustration ]              |
|                                          |
|  * Scan your timetable to import.        |
|  * Track subjects, schedules & targets.  |
|  * Simulate attendance predictions.      |
|                                          |
|  Target percentage: [ 75% ] <            |
|                                          |
|   (o) ( ) ( )                            |
|                                          |
|  +------------------------------------+  |
|  |            GET STARTED             |  |
|  +------------------------------------+  |
+------------------------------------------+
```

---

## 3. Main Dashboard Tab Screen
- **Purpose:** Overview of statistics, progress rings, and skip predictions.
- **Navigation:** Access secondary features via tab clicks or list selections.

```text
+------------------------------------------+
|  Dashboard                    [Settings] |
|                                          |
|  +------------------------------------+  |
|  | OVERALL ATTENDANCE                 |  |
|  |                                    |  |
|  |        (  78.5%  )                 |  |
|  |                                    |  |
|  |  * Meets 75.0% target criteria     |  |
|  |  * Can skip 2 consecutive classes  |  |
|  +------------------------------------+  |
|                                          |
|  Subject Summaries                       |  |
|  +------------------------------------+  |
|  | Math-101                (82.0%)    |  |
|  | Attended: 12/15                    |  |
|  +------------------------------------+  |
|  | Chem-203                (68.0%) !  |  |
|  | Attended: 9/15                     |  |
|  +------------------------------------+  |
|                                          |
|  [Home]    [Subjects]  [Schedule] [Settings]
+------------------------------------------+
```

---

## 4. Subjects List Screen
- **Purpose:** View and delete subjects.
- **Navigation:** FAB opens the "Add Subject" screen; list cards navigate to details.

```text
+------------------------------------------+
|  Subjects                                |
|                                          |
|  +------------------------------------+  |
|  | Math-101                 [Edit] [x] |  |
|  | 4 Credit Hours                     |  |
|  +------------------------------------+  |
|  | Chem-203                 [Edit] [x] |  |
|  | 3 Credit Hours                     |  |
|  +------------------------------------+  |
|                                          |
|                                          |
|                                     (+)  |
|  [Home]    [Subjects]  [Schedule] [Settings]
+------------------------------------------+
```

---

## 5. Add Subject Screen
- **Purpose:** Add or edit a subject.
- **Navigation:** Saving returns users to the Subjects List.

```text
+------------------------------------------+
|  < Back              Add Subject         |
|                                          |
|  Subject Name                            |
|  +------------------------------------+  |
|  | Linear Algebra                     |  |
|  +------------------------------------+  |
|                                          |
|  Course Code                             |
|  +------------------------------------+  |
|  | MATH-201                           |  |
|  +------------------------------------+  |
|                                          |
|  Credit Hours                            |
|  ( ) 1   ( ) 2   (*) 3   ( ) 4           |
|                                          |
|  +------------------------------------+  |
|  |               SAVE                 |  |
|  +------------------------------------+  |
+------------------------------------------+
```

---

## 6. Schedule / Timetable Screen
- **Purpose:** Weekly timetable calendar view.
- **Navigation:** Clicking "SCAN TIMETABLE" triggers the OCR import pipeline.

```text
+------------------------------------------+
|  Weekly Timetable      [SCAN TIMETABLE]  |
|                                          |
|  [M]  [T]  [W]  [T]  [F]  [S]  [S]       |
|                                          |
|  Monday Classes                          |
|  +------------------------------------+  |
|  | 09:00 AM - 10:30 AM                |  |
|  | Math-101 (Room 302)                |  |
|  +------------------------------------+  |
|  | 11:00 AM - 12:30 PM                |  |
|  | Chem-203 (Lab 3)                   |  |
|  +------------------------------------+  |
|                                          |
|  [Home]    [Subjects]  [Schedule] [Settings]
+------------------------------------------+
```

---

## 7. Attendance Logs Screen
- **Purpose:** Logging attendance records.
- **Navigation:** Back exits to previous views.

```text
+------------------------------------------+
|  < Back          Math-101                |
|                                          |
|  Class Date: 2026-08-01                  |
|                                          |
|  Select Status:                          |
|  +-----------+-----------+------------+  |
|  |  PRESENT  |  ABSENT   | CANCELLED  |  |
|  +-----------+-----------+------------+  |
|                                          |
|  Notes / Comments                        |
|  +------------------------------------+  |
|  | Write comment notes here...        |
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  |
|  |             SUBMIT                 |  |
|  +------------------------------------+  |
+------------------------------------------+
```

---

## 8. Attendance Simulator Screen
- **Purpose:** "What-if" predictive calculations.
- **Navigation:** Simulator operates on in-memory configurations.

```text
+------------------------------------------+
|  < Back     Attendance Simulator         |
|                                          |
|  Overall Attendance: 75.0%               |
|                                          |
|  Subject: [ Math-101  v ]                |
|                                          |
|  Assume I attend:                        |
|  [ - ]   5   [ + ]  classes              |
|                                          |
|  Assume I skip:                          |
|  [ - ]   2   [ + ]  classes              |
|                                          |
|  Predicted Math Percentage:              |
|  82.5% (Meeting target: Safe!)           |
|                                          |
+------------------------------------------+
```

---

## 9. Leave Planner Screen
- **Purpose:** Verify leave timing risks.

```text
+------------------------------------------+
|  < Back         Leave Planner            |
|                                          |
|  Start Date: [ 2026-08-10 ]              |
|  End Date:   [ 2026-08-15 ]              |
|                                          |
|  Leaves identified: 4 scheduled classes  |
|                                          |
|  Impact Analysis:                        |
|  - Math-101: 78% -> 73% (UNSAFE)         |
|  - Chem-203: 80% -> 76% (SAFE)           |
|                                          |
|  +------------------------------------+  |
|  |           CHECK IMPACT             |  |
|  +------------------------------------+  |
+------------------------------------------+
```

---

## 10. Semester Reset & Archive Screen
- **Purpose:** Prepare databases for new academic terms.

```text
+------------------------------------------+
|  < Back       Semester Management        |
|                                          |
|  Current Semester: Spring 2026           |
|                                          |
|  * This will archive current subjects    |
|    and logging records.                  |
|  * The active database tables will be    |
|    reset to empty configurations.        |
|                                          |
|  [ ] Archive data before resetting       |
|                                          |
|  +------------------------------------+  |
|  |           RESET DATABASE           |  |
|  +------------------------------------+  |
+------------------------------------------+
```

---

## 11. Backup Screen
- **Purpose:** Local data backups.

```text
+------------------------------------------+
|  < Back       Backup & Restore           |
|                                          |
|  Last Backup Run: 2026-08-01 10:30 AM    |
|                                          |
|  +------------------------------------+  |
|  |           CREATE BACKUP            |  |
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  |
|  |          RESTORE DATA              |  |
|  +------------------------------------+  |
+------------------------------------------+
```

---

## 12. Settings Screen
- **Purpose:** Application preferences.

```text
+------------------------------------------+
|  Settings                                |
|                                          |
|  App Settings                            |
|  * Target Percentage: [ 75% ]            |
|  * Theme Mode:        [ System v ]       |
|  * Class Reminders:   [ [x] Enabled ]    |
|                                          |
|  Maintenance Settings                    |
|  * Backup & Restore                      |
|  * Semester Reset / Archive              |
|                                          |
|  [Home]    [Subjects]  [Schedule] [Settings]
+------------------------------------------+
```

---

## 13. OCR Review Screen
- **Purpose:** Verify OCR-parsed timetable data.

```text
+------------------------------------------+
|  Timetable Parser Review                 |
|                                          |
|  Confirm the parsed classes:             |
|                                          |
|  [x] Math-101                            |
|      Monday: 09:00 AM - 10:30 AM         |
|      Room: 302     [Edit]                |
|                                          |
|  [x] Chem-203                            |
|      Monday: 11:00 AM - 12:30 PM         |
|      Room: Lab 3   [Edit]                |
|                                          |
|  +------------------------------------+  |
|  |         IMPORT TIMETABLE           |  |
|  +------------------------------------+  |
+------------------------------------------+
```

---

## 14. Homescreen Widgets
- **Purpose:** Quick glance overview widget on the Android Homescreen.

```text
+-----------------------+
|  Attendance  78%      |
|  Math-101 : 82% [P]   |
|  Chem-203 : 68% [!]   |
+-----------------------+
```
