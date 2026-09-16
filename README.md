# Routy

Batumi public transport for Android and iOS, built with Kotlin Multiplatform and
Compose Multiplatform. Uses the real ThetaMaps database and live vehicle API.

## Features

- MapLibre map, configurable light/dark OpenFreeMap styles and attribution.
- Routes, stops, route geometry and stop timetables; route groups retain the API's
  opaque status values without inventing inbound/outbound meanings.
- Live buses for the visible selected route; foreground polling every five seconds.
- Local route/stop search, saved routes/stops and persistent appearance/language.
- Atomic persistent transport snapshots for offline startup.
- English and Georgian UI and safe transport-name fallbacks.
- Optional foreground location, requested only from My location.
- Typed destinations, saved navigation/map state, Android back and iOS edge swipe.

Trip planning and arrival predictions are intentionally unavailable: the necessary
API contracts have not been confirmed. Timetables are scheduled times in Batumi,
not real-time arrival predictions.

## Development

Use JDK 21, the Android SDK specified in `gradle/libs.versions.toml`, and Xcode
with the iOS SDK on macOS. Versions remain pinned in the catalog. `local.properties`
is machine-local and must not be committed.

```sh
./gradlew :androidApp:assembleDebug
```

Open `iosApp/iosApp.xcodeproj` and run the `iosApp` scheme. Physical devices need
a signing team configured locally. Simulator build without signing:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

## Verification

The final test pass is deferred at the user's request; see
[implementation status](docs/IMPLEMENTATION_STATUS.md) for checks already run.

```sh
./gradlew ktlintCheck :shared:allTests :androidApp:lintDebug :androidApp:assembleDebug
./gradlew :shared:linkDebugFrameworkIosArm64
python3 scripts/check_architecture.py
```

`ktlintFormat` formats handwritten Kotlin only. Android lint plus the dependency
check enforce platform/static rules. GitHub Actions contains Android and iOS jobs;
it has not run remotely because GitHub authentication is not configured locally.

## Architecture and configuration

Read [architecture](docs/KMP_PROJECT_ARCHITECTURE.md) and
[API contract](docs/THETAMAPS_API_KMP.md) before changing implementation.

```text
Android / iOS composition roots → AppGraph
feature presentation → domain use cases → repository contracts
                                            ↑
                            Ktor + atomic local files
```

`shared/src/commonMain/kotlin/com/example/routy/feature` owns MVI contracts,
ViewModels, UI and feature queries. Shared transport infrastructure lives under
`core/transport`; storage and logging implementations live in platform source sets.
No transport DTO or map SDK type belongs in domain.

`TransportConfig` owns API URL, refresh cadence and stale-vehicle retention.
`MapStyleConfig` owns map style URLs. Neither requires a secret in this configuration.
Map tiles are provided by OpenFreeMap; provider attribution remains visible.
Full offline map-region downloads are not implemented. Transport lists, schedules,
favorites and cached geometry remain available independently of tile availability.

Release validation, service usage authorization, physical-device accessibility,
Georgian copy review and store-signing/distribution remain release work, not an
assertion implied by a successful debug build.
