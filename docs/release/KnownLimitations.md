# Known Limitations

- A physical Android device and deployed MongoDB/R2 environment are required for complete end-to-end certification.
- The local environment cannot execute connected instrumentation tests when no device is attached.
- The backend currently does not accept a client idempotency key, leaving a narrow crash window after remote metadata acceptance and before local persistence.
- `SAFE75_API_BASE_URL` must be supplied for a production Android build; the repository default is an example host.
- Release builds without `keystore.properties` are signed with the debug key and must not be published.
- Lint may report dependency freshness, native-library alignment, locale, and other non-blocking warnings from existing dependencies or legacy modules.
