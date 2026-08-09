# Bug Reporting

Safe75 supports explicit, user-initiated bug reports. Reporting is offline-first:
the report is saved locally before any network operation and is uploaded later by
WorkManager when a connected network is available.

## What Is Submitted

- User-entered issue description.
- Diagnostics JSON containing technical device metadata, recent Safe75 logs, and available crash reports.
- App/device metadata required by the backend.
- One optional JPEG, PNG, or WEBP screenshot up to 2 MB.

Attendance records, subjects, schedules, notes, backups, and unrelated files are
not read by the bug-report queue and are not sent to the backend.

## Submission Flow

1. The user enters a description and optionally selects one screenshot.
2. Diagnostics are generated at submit time.
3. The complete payload is persisted in Room; the screenshot is copied to private app storage.
4. Unique WorkManager work is scheduled with a connected-network constraint.
5. The worker submits metadata, stores the backend report ID, then uploads the screenshot if present.
6. Successful uploads remove the queue row and temporary screenshot file.

## Reliability

The queue survives process death, app restart, reboot, and app updates through Room
and WorkManager persistence. Network failures, timeouts, I/O failures, and 5xx
responses use exponential backoff up to five retries. Validation, authentication,
authorization, not-found, conflict, and other client failures are not retried.

## Configuration

Release builds must set `SAFE75_API_BASE_URL` to the deployed HTTPS backend URL.
The default example URL is intentionally non-production.

## Known Idempotency Boundary

The current backend generates the remote `reportId` and does not accept a client
idempotency key. Safe75 prevents duplicate local queue entries and persists the
remote ID before screenshot upload. A process crash after the backend accepts
metadata but before the local ID is persisted remains a theoretical duplicate
window that requires a future backend API change to eliminate completely.
