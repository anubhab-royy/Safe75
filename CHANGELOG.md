# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2026-08-02

First production release.

### Added
- **Home-screen widget** (Jetpack Glance): attendance overview with today's
  progress, next class, and quick actions; auto-refreshed daily, on data
  changes, and via system broadcasts.
- **Widget deep links**: quick actions launch the app directly into the
  dashboard, attendance history, schedule, or settings.
- **Release configuration**: R8 minification + resource shrinking, ProGuard
  keep rules, versionCode 1 / versionName 1.0.0, optional release signing with
  a safe debug-keystore fallback.
- **Debug-only tooling**: StrictMode (thread + VM) and a no-op log engine in
  release builds so no debug logging ships to production.
- **Startup optimizations**: notification-channel creation moved off the main
  thread, `androidx.profileinstaller` shipped, and a bundled `baseline-prof.txt`.
- **Adaptive layout**: NavigationRail replaces the bottom bar on wide screens.
- **Benchmark module**: `:benchmark` macrobenchmark scaffold for generating an
  ART baseline profile on a device.
- **Play Store release docs**: `docs/release/` checklist, listing copy, and
  privacy policy.
- **Accessibility audit**: all decorative icons verified (correct `null`
  content descriptions); interactive icons already labeled.

### Notes
- 116 unit tests pass across 24 suites (`:app:testDebugUnitTest`).
- Baseline-profile generation (`:benchmark:connectedCheck`) requires a
  device/emulator and is deferred to CI or a post-release follow-up.

## [Unreleased]
### Added
- Phase 0: Detailed system architecture design and project requirements.
- Core models: `AttendanceStatus`, `AttendanceTarget`, and `WeekDay`.
- Domain layer repositories and business validators.
- Reusable Material 3 UI component layouts (`PrimaryButton`, `AttendanceCard`, etc.).
- Modular Hilt dependency injection graphs.
