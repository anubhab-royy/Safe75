# UI Design System Document

This document defines the **Material 3** principles, color tokens, typography scales, components structure, and spacing tokens for **Attendance Tracker**.

---

## 1. Material 3 & Design Token Sizing

We rely on static design tokens defined in the code to construct consistent interfaces:

### 1.1 Layout Dimensions (`Dimensions.kt`)
- **SpacingExtraSmall:** `4.dp` (spacing between related labels or subtexts)
- **SpacingSmall:** `8.dp` (standard card margin/inner layouts)
- **SpacingMedium:** `16.dp` (outer screen padding, layout partitions)
- **SpacingLarge:** `24.dp` (header spacing)
- **SpacingExtraLarge:** `32.dp` (form spacer columns)
- **ButtonHeight:** `50.dp` (for standard tactile button targets)

### 1.2 Elevations (`Elevation.kt`)
- **None:** `0.dp`
- **Card:** `2.dp`
- **Button:** `4.dp`
- **Dialog:** `8.dp`

### 1.3 Corner Radii (`Radius.kt`)
- **Small:** `4.dp` (small badges, tags)
- **Medium:** `8.dp` (buttons, text fields)
- **Large:** `16.dp` (standard subject and attendance cards)
- **ExtraLarge:** `24.dp` (bottom sheets, dialog content blocks)

---

## 2. Color Palette & States

We define explicit colors matching student attendance compliance states:

| State | Color Token | Hex Code (Dark) | Hex Code (Light) | Context of Use |
| :--- | :--- | :--- | :--- | :--- |
| **Safe** | Primary / Green | `#4caf50` | `#2e7d32` | Attendance percentage $\ge 75\%$. |
| **Warning** | Tertiary / Orange | `#ff9800` | `#ef6c00` | Attendance percentage between $70\%$ and $75\%$. |
| **Critical** | Error / Red | `#f44336` | `#d32f2f` | Attendance percentage $< 70\%$. |
| **Neutral** | Surface Variant | `#33333d` | `#f4f4f9` | Backgrounds, cancelled class cards. |

### 2.1 Dark & Light Theme Configurations
The theme uses dynamic colors on compatible devices running Android 12 (API 31) or later using the Material You library. On legacy devices:
- **Dark Theme:** Deep charcoal background (`#121212`) with active colored components using pastel hues for low eye-strain during night logging sessions.
- **Light Theme:** Off-white backgrounds (`#fafafa`) with deep contrast colors.

---

## 3. Typography Scale

Following standard Android Material 3 styles:

| Style Name | Font Family | Size (SP) | Weight | Line Height |
| :--- | :--- | :--- | :--- | :--- |
| **HeadlineLarge** | Inter / Roboto | 32 | Bold | 40sp |
| **HeadlineMedium**| Inter / Roboto | 28 | Medium | 36sp |
| **TitleLarge** | Inter / Roboto | 22 | Bold | 28sp |
| **TitleMedium** | Inter / Roboto | 16 | Medium | 24sp |
| **BodyLarge** | Inter / Roboto | 16 | Normal | 24sp |
| **BodyMedium** | Inter / Roboto | 14 | Normal | 20sp |

---

## 4. Reusable UI Components Hierarchy

### 4.1 Buttons
1. **PrimaryButton:** Filled button (using `Primary` color) used for positive flow confirmations (e.g. Save, Get Started).
2. **SecondaryButton:** Outlined button used for secondary choices (e.g. Cancel, Skip, Clear).

### 4.2 Cards
Attendance and subject lists render using `AttendanceCard` built with a large corner radius (`Radius.Large`), supporting dynamic background tints based on compliance states.

### 4.3 Dialogs
Confirmation dialogs are configured with `Elevation.Dialog` and `Radius.ExtraLarge`. They contain a clear title, descriptive body text, an outlined dismiss button, and a filled confirmation action button.
