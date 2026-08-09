# Safe75 — Privacy Policy

Effective date: August 2026

**Safe75** ("the App") is an offline-first attendance tracker for
students. This policy explains what data the App collects and how it is handled.

## 1. Data collected

Safe75 has no account system and performs no analytics. Attendance data remains
local, but the App has network access for device enrollment and for explicit,
user-initiated bug-report uploads.

Subjects, schedules, attendance records, reminders, and settings are stored
locally in SQLite and Preferences DataStore. A submitted bug report contains
only the issue description, technical diagnostics, required app/device metadata,
and an optional screenshot.

## 2. How data is used

Your data is used solely to provide the App's features on your own device:
- Displaying attendance percentages and history.
- Scheduling local notification reminders.
- Exporting a JSON backup file that you choose to create and save (e.g., to a
  cloud drive of your choice via the system file picker — the App itself does
  not upload it).
- Importing a backup file you provide.
- Uploading a submitted bug report to Safe75's backend. Metadata is stored in
  MongoDB Atlas and optional screenshots are stored in Cloudflare R2.

## 3. Permissions

Permissions and access used by the App include:

- **Internet / network state** — used for device enrollment and queued bug-report
  uploads only.

- **Notifications** — used only to show locally-scheduled class reminders on
  your device.
- **Photos / storage** — used only when you explicitly choose an image for OCR
  or a bug report. OCR images stay on-device; a selected bug-report screenshot
  is uploaded only as part of that report.

## 4. Third parties

The App uses Google's on-device ML Kit text recognition for OCR. ML Kit
processes images locally; no image data is sent to Google for this feature.
Bug-report infrastructure uses MongoDB Atlas and Cloudflare R2 through the
Safe75 backend. The App does not upload attendance databases, timetables,
notes, backups, or unrelated files. Automatic Android app-data backup is
disabled.

## 5. Data deletion

You can delete your data at any time:

- Delete individual records in the App.
- Use **Semester Reset** to clear attendance and related data.
- Uninstall the App, which removes all locally stored data.

## 6. Children

The App is intended for general audiences and does not knowingly collect
personal information from children.

## 7. Changes to this policy

If this policy changes, an updated version will be published here and marked
with a new effective date.

## 8. Contact

Questions about this policy can be directed to the App's support channel listed
on the Google Play listing.
