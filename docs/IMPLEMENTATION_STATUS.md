# Implementation status — 2026-09-16

## Implemented

- Real ThetaMaps DTO parsing, validated transport models, Ktor Android/iOS engines,
  domain error mapping, cancellation and structured privacy-safe diagnostics.
- Atomic transport cache, cache-first startup, background refresh, local search,
  schedules, localized name fallbacks and stable route/stop IDs.
- Feature-specific MVI for map, routes, route details, stops, stop details,
  favorites and settings. Local persistent favorites, language and appearance.
- MapLibre on both platforms: selectable stops, selected route line/stops,
  live vehicles, selection, optional foreground user location and camera controls.
- Separate static/vehicle sources, lifecycle-aware flow collection and polling,
  saved navigation/map state and Android/iOS back handling.
- Light/dark design system, English/Georgian catalogs, loading/empty/error/offline
  presentation, scalable text and accessible alternatives to map markers via lists.
- Common tests, an iOS atomic-storage test, formatting, Android lint, architecture
  dependency check and GitHub Actions configuration.

## Observed verification

Before the request to defer further testing:

- Android debug APK built and ran on the Pixel 9 Pro emulator.
- iOS simulator host built with Xcode 26.5 and ran on iPhone 17 Pro / iOS 26.5.
- Both platforms displayed actual Batumi map tiles and ThetaMaps stops.
- Android route selection displayed its geometry and eight reported live buses.
- Android offline restart loaded the saved snapshot and displayed the stale/offline
  notice with Retry while preserving stops. Emulator connectivity was restored.
- ktlintCheck, Android lint and common tests on Android/iOS completed successfully.
- iOS device framework linked successfully; physical-device signing was not tested.
- The last already-running verification command completed successfully after the
  request to defer new tests. Further regression/device checks are deferred.

Subsequent navigation polish is compilation-checked; do not interpret earlier
runtime checks as full validation of every final change.

## Deferred validation / external dependencies

- Final regression pass, denied/permanently-denied location on physical devices,
  process recreation, large-font layouts and TalkBack/VoiceOver walkthroughs.
- iOS edge-swipe interaction under all nested sheet/navigation cases and actual
  memory-pressure behavior.
- Georgian copy review by a fluent product reviewer.
- Release signing, store identity/icons/privacy declarations and distribution.
- Public API authorization/redistribution terms and production service guarantees.
- GitHub push: HTTPS remote is configured, but Git cannot obtain credentials
  (`could not read Username for 'https://github.com': Device not configured`).
  Local commits are preserved on `dev`; no force-push/history rewriting was used.
- CI is committed but has not executed on GitHub.

## Deliberately unavailable

- Trip planner, ETA predictions and named travel directions: API contracts remain
  unverified. `feature/trip_planner` records this boundary; no invented request exists.
- Downloadable offline map regions. Offline transport data is independent of tiles.
