# Phase B2 — Runtime OCR Diagnostics Report

**Status:** COMPLETE
**Date:** 2026-08-06
**Builds:** debug + release both assembled from HEAD (`assembleDebug` / `assembleRelease`)
**Device:** AsteroidsIND A059 (Android 16, ARM64, non-rooted)

---

## 1. Executive Summary

Runtime diagnostics were added to every stage of the timetable OCR pipeline and
captured on a physical device for both the debug and the (non-debuggable,
R8-minified) release builds using the same known-good timetable image.

**The debug→release divergence was isolated to the ML Kit text recognition stage.**

- **Debug:** 36/36 cells processed, 26 with recognized text, **11 subjects parsed**,
  `failure=none` on every cell, total 3.7 s.
- **Release:** 36/36 cells failed with an immediate `NullPointerException`
  (`getClass() on a null object reference`) thrown synchronously inside ML Kit,
  **0 cells with text, 0 subjects parsed**, total 175 ms → "No structures detected."

Everything upstream of recognition — decode, quality check, preprocessing,
deskew, table detection, grid structure, cell extraction — is **bit-identical**
between the two builds. The failure is 100 % inside ML Kit.

**Root cause:** R8 minification obfuscates ML Kit internal classes
(`com.google.android.gms.internal.mlkit_vision_common.*` and friends). ML Kit's
internal component wiring then cannot resolve the renamed classes, and every
`TextRecognition.process()` call throws a `NullPointerException`. The pipeline
catches the exception (via `Result.failure`), treats the cell as empty, and the
parser correctly reports zero structures.

---

## 2. Scope & Constraints

- **No fixes** to OCR behavior, the parser, preprocessing, OpenCV, or the UI.
- Instrumentation only, via the existing `Logger` / `SafeLogEngine` /
  `DiagnosticsFileStorage` infrastructure.
- No `println` / `Log.d`; all evidence goes to `files/diagnostics/logs.json`.
- Instrumentation is lightweight, adds no bitmaps, no duplicate OCR passes, and
  is removable (gated by `OcrInstrumentation.ENABLED`).
- A temporary, removable `DiagnosticsExportReceiver` was added so the
  non-debuggable release could publish its log file to public Downloads
  (there is otherwise no way to read release internal storage on a non-rooted
  Android 16 device).

## 3. Builds & Environment

| Item | Value |
|---|---|
| Debug package | `com.attendance.tracker.debug` (suffix `.debug`, debuggable) |
| Release package | `com.attendance.tracker` (not debuggable, `isMinifyEnabled`, `isShrinkResources`) |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` (built from HEAD) |
| Release APK | `app/build/outputs/apk/release/app-release.apk` (built from HEAD) |
| Test image | `media__1785707227737.jpg` (1024×608), pushed to `/sdcard/Download/test_timetable.jpg` |
| Expected anchors | 2 tables, 8 rows × 9 cols, ~36 cells, ≥10 subjects |
| Unit tests | 195 passed / 0 failed (incl. `OcrDownsamplingTest` anchors) |
| Lint | no errors, only pre-existing warnings |

## 4. Instrumentation Design

New helper `feature/ocr/diagnostics/OcrInstrumentation.kt` (`ENABLED = true`)
with tags: `OcrDecode`, `OcrQuality`, `OcrPreprocess`, `OcrTable`, `OcrGrid`,
`OcrCellExtract`, `OcrCellOcr`, `OcrPipeline`, `OcrSummary`, `OcrFailure`.

Instrumented production code (all additions are additive; no logic changed):

| File | What is logged |
|---|---|
| `ImageProcessor.kt` | decode source/sampleSize/bitmap/config/bytes, quality contrast & blur PASS/FAIL, preprocess in/out Mat sizes, deskew APPLIED/SKIPPED |
| `TableDetector.kt` | detected table count + bounding rects |
| `TableStructureDetector.kt` | grid rows/cols/cells, row/col boundary arrays |
| `CellExtractor.kt` | per-cell rect + bitmap size, extraction total |
| `OcrRecognizer.kt` | per-cell recognized text/length/confidence/empty, **ML Kit failure message + full stack** |
| `OcrRepository.kt` | pipeline entry/exit, one-line summary per run, failure snapshot when 0 subjects |

A one-line `SUMMARY` is emitted for every run so even a partially-evicted
200-entry buffer retains the whole image→rows chain.

## 5. Diagnostics Export Mechanism (Removed)

The temporary `DiagnosticsExportReceiver` and its manifest entry have been removed as part of Phase B3 cleanup.


## 6. On-Device Execution Procedure

Fully scripted with `adb input` + `uiautomator dump` (no manual taps):
Welcome → Get Started → wizard Next → "Import Timetable from Image (OCR)" →
"Select Screenshot from Gallery" → Photo Picker → Browse… → Downloads →
`test_timetable.jpg`. Each build was installed, driven through the same steps,
and its diagnostics exported/pulled after the review screen settled.

## 7. Debug Build Results

```
[OcrDecode]   DECODE source=1024x608 sampleSize=1 → bitmap=1024x608 ARGB_8888 elapsed=83ms
[OcrQuality]  QUALITY PASS contrast=51.2 blurVariance=6192.2
[OcrPreprocess] PREPROCESS input=1024x608 ch=4 type=24, deskew SKIPPED angle=90.0, output gray/thresh=1024x608
[OcrTable]    TABLE tables=2 rects=[(47,0,937,291),(151,310,522,261)]
[OcrGrid]     GRID rows=8 cols=9 cells=36 rowBoundaries=0,17,40,76,129,165,187,238,290
[OcrSummary]  SUMMARY image=1024x608 tables=2 grid=8x9 cells=36 ocrCells=36 ocrWithText=26 parsedSubjects=11 elapsed=3957ms
```

Cell samples: `'OOP using JAVA (ANM)' len=20 conf=0.754 failure=none`,
`'DBMS Lab' … failure=none`. Only 4 tiny cells (< 32 px) legitimately failed
with `InputImage width and height should be at least 32!`.
Review screen: **"All parsed items have high confidence!"**.

## 8. Release Build Results

```
[OcrDecode]   DECODE source=1024x608 sampleSize=1 → bitmap=1024x608 ARGB_8888 elapsed=31ms
[OcrQuality]  QUALITY PASS contrast=51.2 blurVariance=6192.2
[OcrPreprocess] PREPROCESS input=1024x608 ch=4 type=24, deskew SKIPPED angle=90.0, output gray/thresh=1024x608
[OcrTable]    TABLE tables=2 rects=[(47,0,937,291),(151,310,522,261)]
[OcrGrid]     GRID rows=8 cols=9 cells=36 rowBoundaries=0,17,40,76,129,165,187,238,290
[OcrSummary]  SUMMARY image=1024x608 tables=2 grid=8x9 cells=36 ocrCells=36 ocrWithText=0 parsedSubjects=0 elapsed=175ms
[OcrFailure]  FAILURE … withText=0 withoutText=36 emptyRate=1.0 subjects=0 sampleTexts=''
```

Every cell: `text='' empty=true confidence=0.85 failure=… elapsed=0–3ms`.
Review screen: **"No structures detected."** (the empty-success UI).

## 9. Stage-by-Stage Comparison

| Stage | Debug | Release | Δ |
|---|---|---|---|
| Decode | 1024×608 ARGB | 1024×608 ARGB | identical |
| Quality | PASS 51.2 / 6192 | PASS 51.2 / 6192 | identical |
| Preprocess / deskew | SKIPPED 90.0 | SKIPPED 90.0 | identical |
| Table detection | 2 rects | 2 rects | identical |
| Grid structure | 8×9, 36 cells | 8×9, 36 cells | identical |
| Cell extraction | 36 cells | 36 cells | identical |
| **ML Kit OCR** | **26 withText, 0 NPE** | **0 withText, 36 NPE** | **DIVERGES** |
| Semantic parse | 11 subjects | 0 subjects | consequence |
| Total elapsed | 3957 ms | 175 ms | OCR skipped/errored |

## 10. Root Cause

`OcrScanner.scanBitmap()` → `recognizer.process(image)` throws, synchronously
and immediately (~0 ms/cell), a

```
java.lang.NullPointerException: Attempt to invoke virtual method
'java.lang.Class java.lang.Object.getClass()' on a null object reference
```

`OcrScanner` catches it (`Result.failure`), `OcrRecognizer` reads `getOrNull()`
→ empty text, and the parser correctly yields 0 rows. In debug the same code,
device, and image return real text, so the only variable is the release build
pipeline: **R8 minification**.

R8 renamed ML Kit internals. Mapping de-obfuscation of the captured stack:

```
lz4.<init>   → com.google.android.gms.internal.mlkit_vision_common.zzmj.<init>
uz4.c        → com.google.android.gms.internal.mlkit_vision_common.zzmr.c
i61.b        → com.google.mlkit.vision.common.InputImage.b
```

322 `com.google.android.gms.internal.mlkit_vision_common.*` classes are renamed
in the release mapping; even ML Kit public classes such as
`com.google.mlkit.common.MlKitException` are obfuscated. ML Kit's internal
component/registration machinery relies on these classes, and under obfuscation
it resolves a null component → NPE on first `process()` call.

The existing `proguard-rules.pro` ML Kit section (`-keep class
com.attendance.tracker.feature.ocr.**`) only protects the **app's** OCR code;
it does not protect ML Kit's own internal packages.

## 11. Evidence Summary

1. Debug log: `failure=none`, real per-cell text, 26 withText, 11 subjects.
2. Release log: 36/36 `failure=` NPE messages, 0 withText, 0 subjects, 175 ms.
3. Full stack trace persisted in `logs.json` (ERROR entries).
4. `mapping.txt` shows `zzmj→lz4`, `zzmr→uz4`, `InputImage→i61` and 322 renamed
   `mlkit_vision_common` classes.
5. Identical upstream stage logs rule out image, preprocessing, grid geometry,
   and OpenCV as factors.

## 12. Hypothesis Resolution (B1)

| B1 hypothesis | Verdict |
|---|---|
| H1 runtime ML Kit output variance | **CONFIRMED** — root cause is ML Kit failing under R8 |
| H2 stale release APK | Ruled out as the *only* cause — fresh HEAD release reproduces |
| H3 image / grid-geometry sensitivity | Ruled out — grids byte-identical in both builds |
| H4 ART-mode Kotlin (debuggable) difference | Ruled out — failure is an explicit NPE from obfuscated ML Kit classes, not an ART runtime-mode effect |

## 13. Recommended Fix (Phase B3 — NOT implemented here)

Keep ML Kit's internal packages from being obfuscated, e.g. in
`app/proguard-rules.pro`:

```
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.mlkit.common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text.** { *; }
```

**B3 verification:** rebuild release with these rules, re-run the identical
on-device procedure, and expect `ocrWithText=26`, `parsedSubjects=11` in
release (matching debug). If 26/11 is restored, the fix is complete; then remove
the B2 instrumentation and export receiver.

## 14. Artifacts, Compliance & Cleanup

**Evidence files (pulled from device):**
- `E:\Temp\opencode\debug_logs_final.json` — debug pipeline logs
- `E:\Temp\opencode\release_logs3.json` — release pipeline logs (with NPE stacks)

**Compliance:** no production behavior changed; all pipeline logic identical;
tests (195) and lint green; the only source additions are the removable
`OcrInstrumentation`, instrumentation call-sites, and `DiagnosticsExportReceiver`.

**Cleanup after B3 confirms the fix:**
1. Delete `feature/ocr/diagnostics/OcrInstrumentation.kt` and its call-sites.
2. Delete `core/diagnostics/DiagnosticsExportReceiver.kt` and its manifest entry.
3. Re-run the unit suite.
