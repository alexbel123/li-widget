# Changelog

## 1.0 · build 21 — 2026-11-18

- Overview and Large fill the available launcher frame. Widening or narrowing the same widget switches between range, actions, a compact overview and a dashboard. Large also has a distinct picker preview.
- Both Favorites providers switch between a row and a grid. The smallest row can page through all four original actions.
- Compact metrics show an explicit source error. An unchanged first timestamp can show how long no new readings have arrived. Minute ticks use the cached reading and only republish affected widgets.
- The button editor shows translated source actions only on formats that have controls. All existing short action labels are preserved.

[Widget behavior](docs/WIDGETS.md) · [Verification](docs/VERIFICATION.md).

## 1.0 · build 20 — 2026-11-17

- Light Glass uses deeper neutral tones from the Android palette. The button panel and small labels are tuned for the darker widget surface.
- Verified all seven widget formats on one device palette, then verified the overview on a second palette and in system dark mode.

[Theme settings](docs/THEMING.md) · [Verification](docs/VERIFICATION.md).

## 1.0 · build 19 — 2026-11-17

Android `versionName` remains **1.0**; `versionCode` is **19**. Release tag: `v1.0-build19`.

- Fixed a startup crash when an unbound Minimal, Range or Charging preview was selected, including after removing the last home-screen widget. Their FrameLayout previews are now returned as View instead of being cast to LinearLayout.
- Made the thin button dividers clearer in Overview, Large and both Favorites formats. Favorites 2×2 also separates its rows. Minimum touch targets and original Li Auto actions are retained.
- Removed the explanatory paragraph from the Animation setting in both languages.
- Replaced paint-tinted avatar backgrounds with neutral light or graphite backgrounds selected for contrast with the body and roof. Imported photos keep their original appearance.
- Passed 82 native widget renders and interaction checks, 56 unbound-preview cases and checks for every catalog paint. Startup without a home-screen widget was confirmed on a physical Galaxy S24 Ultra.

[Verification](docs/VERIFICATION.md) · [Widget behavior](docs/WIDGETS.md).

## 1.0 · build 18 — 2026-11-17

Android `versionName` remains **1.0**; `versionCode` is **18**. Release tag: `v1.0-build18`.

- Fixed disappearing electricity/fuel symbols in the compact overview: dedicated ImageViews survive RemoteViews parceling. Original header click handlers remain intact beneath the non-interactive visual layer.
- Range and Charging settings have independent **48×48 dp** touch targets, including at minimum height, while preserving the small icon and reading layout.
- Minimal, Range and Charging support tap-to-refresh through the original source PendingIntent. Disabled or unavailable source actions are respected. No new network client or vehicle command is introduced.
- Removed the button-label size setting from the three formats without a control panel. System font scaling remains supported.
- Updated the native screenshot gallery and interaction instructions. Charging-time handling is unchanged.
- Passed **82 native rendering checks**, attached-window touch tests, seven-provider binding, original callback retention, RU/EN, light/dark, minimum sizes with larger system text, and settings visibility for every format.

[Verification](docs/VERIFICATION.md) · [Widget behavior](docs/WIDGETS.md).

## 1.0 · build 17 — 2026-11-17

Android `versionName` remains **1.0**; `versionCode` is **17**. Release tag: `v1.0-build17`. This update includes the work since public build 9.

- Seven registered Android widgets: overview 4×2, large 4×3, minimal 2×1, range 2×2, favorites 4×1 and 2×2, charging 4×2. Each has its own size constraints; small formats no longer reserve the overview's footprint.
- Widget-type selection and pinning in the app, plus per-widget settings across every provider. Removing one type keeps updates running for the other installed widgets.
- Both favorites layouts retain the original four action handlers, order, disabled states and confirmations. Configure the shared action panel in Li Auto.
- Redesigned mobile settings with a compact header, native preview and clearer groups; sample previews are explicitly labelled.
- Matte/glass appearance, Android theme colors, system theme updates, and pure black support.
- Larger vehicle header and optional avatar: generated L-series illustrations, model/paint selection, or a local photo through Android's system picker.
- Brief charging pulse on a changed percentage or remaining time, followed by a static bolt. Existing charging-time handling is unchanged.
- Tailgate artwork and inactive icon colors aligned with the other controls.
- Bounded render/resource caches and private local build-key permissions on POSIX systems; release signing secrets remain outside the repository.
- 48 native rendering checks, all seven registered/bound providers, original test action handlers, RU/EN, minimum sizes with larger text, and the production format-picker/pin flow verified on Android API 36. Real-car commands were not part of these checks.

[Widget gallery and sizes](docs/WIDGETS.md) · [Verification and limits](docs/VERIFICATION.md).

## 1.0 — 2026-11-16

First public GitHub release. Android `versionName` **1.0**, `versionCode` **9**.

- Russian / English app, widget and notification.
- Light, dark, pure black and system themes; Android or neutral colors.
- Per-widget appearance and resizing down to 250 × 110 dp.
- Four original actions configured in Li Auto's editor.
- L-series terminology, including «Движение» / Move.
- Consistent inactive tailgate icon color for both known original icon variants; original active-state colors preserved.
- Honest update-age display and stale-data indication.
- Independent launcher icon and native widget-picker preview; no extracted Li Auto graphics distributed.
- Connection placeholders fit compact widths with larger system text.
- MIT source, bilingual documentation, privacy notice, release checks and downloadable APK/ZIP.

This keeps the signing certificate of the earlier locally distributed builds. It can update those builds without removing settings. The visible release version is 1.0; earlier development labels such as 2.2 are not public releases of this repository.

Cabin temperature is not included: no measured value was exposed by the inspected original widget. No claim of real-vehicle validation is made.
