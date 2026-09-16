# KMP Project Architecture Guide --- Android + iOS

> **Purpose:** mandatory engineering guide for an AI/code agent
> implementing the Batumi transport application with Kotlin
> Multiplatform (KMP).
>
> This document defines **how the project must be structured and
> developed**. API details belong in `THETAMAPS_API_KMP.md`.
>
> The priorities are: maintainability, testability, feature isolation,
> SOLID, predictable state management, correct lifecycle handling,
> native-quality Android/iOS behavior, and minimal platform-specific
> code.

------------------------------------------------------------------------

# 1. Core engineering principles

The project must use:

-   Kotlin Multiplatform;
-   Clean Architecture;
-   feature-first / feature-based structure;
-   SOLID;
-   dependency inversion;
-   unidirectional data flow;
-   immutable UI state;
-   Coroutines + Flow;
-   structured concurrency;
-   Ktor Client;
-   kotlinx.serialization;
-   explicit error handling;
-   dependency injection;
-   platform abstractions only where platform behavior actually differs.

Avoid:

-   God classes;
-   global mutable state;
-   direct HTTP calls from UI;
-   direct database access from UI;
-   repositories containing UI logic;
-   platform checks scattered through common code;
-   duplicated Android/iOS business logic;
-   hard-coded API URLs;
-   swallowing exceptions;
-   `GlobalScope`;
-   leaking Activity/ViewController references;
-   putting all code into `commonMain`.

------------------------------------------------------------------------

# 2. Architecture

Use three conceptual layers:

``` text
Presentation
     ↓
Domain
     ↓
Data
```

Dependency direction must always point inward.

``` text
presentation ─────► domain ◄───── data
```

The domain layer must not depend on:

-   Ktor;
-   database implementation;
-   Android SDK;
-   UIKit;
-   SwiftUI;
-   Compose;
-   JSON DTOs.

## Presentation

Responsible for:

-   UI state;
-   user intents/actions;
-   screen coordination;
-   invoking use cases;
-   converting domain results into presentation state.

It must not know how HTTP or persistence works.

## Domain

Contains:

-   domain models;
-   repository contracts;
-   use cases;
-   business rules.

Example:

``` kotlin
interface TransportRepository {
    suspend fun getRoutes(): Result<List<Route>>
    suspend fun getStops(): Result<List<BusStop>>
    fun observeVehicles(routeId: RouteId): Flow<List<Vehicle>>
}
```

## Data

Contains:

-   API implementation;
-   DTOs;
-   local persistence;
-   cache;
-   repository implementations;
-   DTO → domain mapping.

Example flow:

``` text
ThetaMaps API
      ↓
RemoteDataSource
      ↓
DTO
      ↓
Mapper
      ↓
RepositoryImpl
      ↓
Domain Model
```

------------------------------------------------------------------------

# 3. Feature-first project structure

Do **not** organize the whole project globally as:

``` text
ui/
repositories/
viewmodels/
models/
```

Use feature ownership.

Recommended structure:

``` text
root/
├── composeApp/
├── iosApp/
├── shared/
│
├── core/
│   ├── common/
│   ├── network/
│   ├── database/
│   ├── designsystem/
│   ├── navigation/
│   ├── location/
│   └── testing/
│
├── feature/
│   ├── map/
│   ├── routes/
│   ├── route_details/
│   ├── stops/
│   ├── stop_details/
│   ├── trip_planner/
│   ├── favorites/
│   └── settings/
│
└── build-logic/
```

A feature should own its own layers:

``` text
feature/routes/
├── presentation/
│   ├── RoutesScreen.kt
│   ├── RoutesViewModel.kt
│   ├── RoutesState.kt
│   ├── RoutesAction.kt
│   └── RoutesEffect.kt
│
├── domain/
│   ├── GetRoutesUseCase.kt
│   └── model/
│
└── data/
    ├── mapper/
    └── repository/
```

Shared transport infrastructure can live in:

``` text
core/transport/
```

when several features consume the same repository.

Do not duplicate the same repository implementation in several features.

------------------------------------------------------------------------

# 4. Suggested Gradle modules

Prefer real module boundaries when the project becomes non-trivial.

``` text
:composeApp
:shared

:core:common
:core:network
:core:database
:core:model
:core:designsystem
:core:navigation
:core:location
:core:testing

:feature:map
:feature:routes
:feature:route-details
:feature:stops
:feature:stop-details
:feature:trip-planner
:feature:favorites
:feature:settings
```

Do not create dozens of modules before they provide actual
isolation/value.

Start with logical feature boundaries and extract Gradle modules as
features stabilize.

------------------------------------------------------------------------

# 5. Source sets

Typical KMP source layout:

``` text
src/
├── commonMain/
├── commonTest/
├── androidMain/
├── androidUnitTest/
├── iosMain/
└── iosTest/
```

Put code into `commonMain` when behavior is genuinely shared.

Use platform source sets for:

-   location permissions;
-   platform map SDK adapters;
-   secure storage;
-   notifications;
-   deep links when platform APIs differ;
-   lifecycle integration;
-   native sharing;
-   platform logging where needed.

Do not create `expect/actual` just because KMP supports it.

Use interfaces + dependency injection when that produces a cleaner
design.

------------------------------------------------------------------------

# 6. SOLID requirements

## Single Responsibility

Each class has one reason to change.

Bad:

``` kotlin
class RoutesViewModel {
    fun callApi()
    fun saveDatabase()
    fun parseJson()
    fun requestLocationPermission()
    fun drawPolyline()
}
```

Correct:

``` text
RoutesViewModel
    ↓
GetRoutesUseCase
    ↓
TransportRepository
    ↓
Remote/Local Data Sources
```

## Open/Closed

Prefer contracts and composition so implementations can change without
rewriting consumers.

## Liskov Substitution

Implementations must honor interface behavior and error contracts.

## Interface Segregation

Avoid huge interfaces such as:

``` kotlin
interface AppRepository {
    fun routes()
    fun buses()
    fun favorites()
    fun settings()
    fun analytics()
    fun location()
}
```

Use focused contracts.

## Dependency Inversion

High-level code depends on abstractions:

``` kotlin
class GetRoutesUseCase(
    private val repository: TransportRepository
)
```

not:

``` kotlin
class GetRoutesUseCase {
    private val api = ThetaMapsApiImpl()
}
```

------------------------------------------------------------------------


# 7. MVI architecture — mandatory

The presentation layer MUST use **MVI (Model–View–Intent)** as the primary state-management pattern on both Android and iOS/shared presentation code.

Use a strict unidirectional flow:

```text
View
  ↓
Intent / Action
  ↓
ViewModel / Store
  ↓
Reducer / business operation
  ↓
State
  ↓
View
```

Every feature should define, where applicable:

```text
FeatureState
FeatureIntent (or FeatureAction)
FeatureEffect
FeatureViewModel / FeatureStore
```

Example:

```kotlin
data class RoutesState(
    val isLoading: Boolean = false,
    val routes: List<RouteUi> = emptyList(),
    val selectedRouteId: String? = null,
    val error: UiError? = null
)

sealed interface RoutesIntent {
    data object Load : RoutesIntent
    data object Refresh : RoutesIntent
    data class SelectRoute(val routeId: String) : RoutesIntent
}

sealed interface RoutesEffect {
    data class NavigateToRoute(val routeId: String) : RoutesEffect
    data class ShowMessage(val message: String) : RoutesEffect
}
```

The UI sends intents and renders state. It must not directly call repositories, APIs, databases, or contain business rules.

Persistent screen information belongs in `State`.

One-time events belong in `Effect`, for example:

- navigation;
- snackbar/toast;
- opening system settings;
- launching a platform action.

Do not store one-time events permanently inside `State`.

State must be immutable and exposed read-only:

```kotlin
private val _state = MutableStateFlow(RoutesState())
val state: StateFlow<RoutesState> = _state.asStateFlow()
```

Effects may use `SharedFlow` or another explicit one-shot event mechanism:

```kotlin
private val _effects = MutableSharedFlow<RoutesEffect>()
val effects: SharedFlow<RoutesEffect> = _effects.asSharedFlow()
```

State transitions should be explicit and predictable. Prefer reducer-style updates:

```kotlin
_state.update { state ->
    state.copy(
        isLoading = false,
        routes = routes,
        error = null
    )
}
```

For complex features, extract a reducer:

```kotlin
interface Reducer<S, A> {
    fun reduce(state: S, action: A): S
}
```

Do not introduce a reducer abstraction for trivial screens if it only adds boilerplate. The MVI contract and unidirectional flow are mandatory; implementation details may remain pragmatic.

The agent MUST NOT mix unrelated presentation patterns feature-by-feature. Do not implement some screens as ad-hoc MVVM, others as MVP, and others as MVI.

For this project:

```text
Clean Architecture
+
Feature-first modules
+
MVI presentation
+
SOLID
+
Coroutines / Flow
```

is the default architectural stack.


# 8. State management

Use unidirectional data flow.

``` text
User Action
    ↓
ViewModel
    ↓
Use Case
    ↓
Repository
    ↓
New State
    ↓
UI
```

Example:

``` kotlin
data class RoutesState(
    val isLoading: Boolean = false,
    val routes: List<RouteUi> = emptyList(),
    val error: UiError? = null
)

sealed interface RoutesAction {
    data object Refresh : RoutesAction
    data class RouteClicked(val id: String) : RoutesAction
}
```

For one-time events, use an explicit effect/event mechanism.

Do not encode navigation commands as persistent state.

State exposed to UI should be read-only:

``` kotlin
private val _state = MutableStateFlow(RoutesState())
val state: StateFlow<RoutesState> = _state.asStateFlow()
```

------------------------------------------------------------------------

# 8. Coroutines and Flow

Use structured concurrency.

Never use:

``` kotlin
GlobalScope.launch { }
```

Network polling must stop when no longer needed.

For live buses:

``` text
screen active
   ↓
start Flow/polling
   ↓
4–5 sec refresh
   ↓
screen inactive
   ↓
cancel collection/polling
```

Handle cancellation correctly:

``` kotlin
catch (e: CancellationException) {
    throw e
}
```

Do not convert cancellation into a generic network error.

Use:

-   `StateFlow` for observable state;
-   `Flow` for streams;
-   `SharedFlow` when appropriate for transient streams;
-   `suspend` functions for one-shot operations.

------------------------------------------------------------------------

# 9. Networking

Use Ktor Client in shared code.

Recommended engines:

``` text
Android → CIO or OkHttp engine
iOS     → Darwin engine
```

Centralize client configuration.

``` kotlin
HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        )
    }
}
```

Configure:

-   timeouts;
-   serialization;
-   request logging for debug builds;
-   retry policy where safe;
-   user-agent if required;
-   HTTP error mapping.

Never log sensitive tokens or personal information.

The base URL must come from configuration:

``` text
BuildConfig / generated config / environment-specific configuration
```

not from individual repositories.

------------------------------------------------------------------------

# 10. API isolation

The undocumented ThetaMaps API must be isolated.

Recommended:

``` text
TransportRepository
        ↑
TransportRepositoryImpl
        ↑
ThetaMapsRemoteDataSource
        ↑
ThetaMapsApi
```

If the API changes, the rest of the app should require minimal
modification.

Never expose API DTOs outside the data layer.

Bad:

``` kotlin
fun getStops(): List<BusStopDto>
```

Correct:

``` kotlin
fun getStops(): List<BusStop>
```

Refer to:

``` text
THETAMAPS_API_KMP.md
```

for endpoint details.

------------------------------------------------------------------------

# 11. Error model

Create a domain-level error model.

Example:

``` kotlin
sealed interface AppError {
    data object NoInternet : AppError
    data object Timeout : AppError
    data object ServerUnavailable : AppError
    data object InvalidData : AppError
    data class Unknown(val cause: Throwable? = null) : AppError
}
```

Do not expose:

``` text
IOException
Darwin NSError
HTTP client exceptions
serialization exceptions
```

directly to presentation.

UI should decide how to render domain errors.

------------------------------------------------------------------------

# 12. Offline-first behavior

Transport data should remain useful during connectivity loss.

For `getDbData`:

``` text
local cache → show immediately
      +
background refresh
      ↓
new data → update cache → update UI
```

Recommended strategy:

``` text
Single Source of Truth
```

Persistent data:

-   routes;
-   stops;
-   route geometry;
-   schedules;
-   favorites.

Usually ephemeral:

-   live bus coordinates.

If live refresh fails, preserve the last known positions briefly but
clearly treat them as stale.

------------------------------------------------------------------------

# 13. Persistence

Use a KMP-compatible persistence solution.

Good options include:

-   Room KMP when appropriate for the project's supported
    targets/version;
-   SQLDelight when its explicit SQL/model approach better fits the
    project.

Hide implementation behind interfaces.

Example:

``` kotlin
interface TransportLocalDataSource {
    suspend fun getCachedNetwork(): TransportNetwork?
    suspend fun saveNetwork(network: TransportNetwork)
}
```

Do not make domain objects depend on database annotations.

------------------------------------------------------------------------

# 14. Dependency injection

Use constructor injection.

A KMP-compatible DI framework such as Koin may be used, or a lightweight
manual DI approach.

Preferred:

``` kotlin
class RoutesViewModel(
    private val getRoutes: GetRoutesUseCase
)
```

Avoid service locators inside business classes.

Platform implementations should be wired at the composition root.

------------------------------------------------------------------------

# 15. Android UI requirements

If UI is shared with Compose Multiplatform, Android should still follow
Android lifecycle conventions.

Account for:

-   configuration changes;
-   process recreation;
-   foreground/background transitions;
-   runtime permissions;
-   back navigation;
-   predictive back where supported;
-   edge-to-edge layouts;
-   system bars/insets;
-   dynamic font scaling;
-   dark theme;
-   accessibility semantics;
-   TalkBack;
-   reduced-motion considerations where applicable.

Never assume a fixed screen size.

Support phones with:

-   display cutouts;
-   gesture navigation;
-   large fonts;
-   different densities.

Use lifecycle-aware Flow collection.

------------------------------------------------------------------------

# 16. iOS requirements

The iOS build must behave like an iOS application, not an Android screen
running on iPhone.

Account for:

-   safe areas;
-   swipe-back/navigation expectations;
-   Dynamic Type;
-   VoiceOver;
-   light/dark appearance;
-   app foreground/background lifecycle;
-   iOS permission descriptions;
-   native map/location behavior;
-   memory pressure;
-   iOS networking lifecycle.

Any required permission must have the corresponding `Info.plist` usage
description.

Examples include location usage descriptions when location is requested.

Do not request permissions at application launch without user context.

------------------------------------------------------------------------

# 17. Shared UI vs native UI

Prefer sharing business logic aggressively.

UI sharing is a product decision.

Possible approach:

``` text
Compose Multiplatform
Android + iOS UI shared
```

This is suitable if the team wants maximum code sharing.

Alternative:

``` text
shared domain/data/state
Android → Compose
iOS → SwiftUI
```

Use this when highly native iOS UI integration is a priority.

Regardless of UI strategy, domain and data logic should remain shared.

Do not duplicate networking and business logic in Swift.

------------------------------------------------------------------------

# 18. Maps

Map integration must be abstracted because platform SDK requirements may
differ.

Domain must know only:

``` kotlin
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)
```

Domain must not import:

``` text
Google Maps classes
MapKit classes
Compose map classes
```

Presentation adapters convert `GeoPoint` into platform map coordinates.

Features required:

-   route polyline;
-   stop markers;
-   live bus markers;
-   selected marker state;
-   camera bounds;
-   user location when permission is granted.

Do not redraw the entire map unnecessarily on every 4-second vehicle
update.

Update vehicle marker state efficiently.

------------------------------------------------------------------------

# 19. Location permissions

Location must be optional for browsing routes.

The user should still be able to:

-   browse routes;
-   inspect stops;
-   view buses;
-   search manually;

without granting location.

Request location only when the user invokes a location-dependent feature
such as:

``` text
My location
Nearby stops
Route from my position
```

Android and iOS permission implementations belong behind a shared
abstraction.

------------------------------------------------------------------------

# 20. Navigation

Use typed destinations rather than arbitrary route strings where
possible.

Conceptual destinations:

``` kotlin
sealed interface Destination {
    data object Map : Destination
    data object Routes : Destination
    data class RouteDetails(val routeId: String) : Destination
    data class StopDetails(val stopId: String) : Destination
}
```

IDs passed between screens must remain strings.

Support deep-link-friendly screen parameters.

Do not pass large objects through navigation.

Pass IDs and load state from repositories.

------------------------------------------------------------------------

# 21. Localization

At minimum plan for:

``` text
English
Georgian
```

If Russian is part of product requirements, add it through the same
localization system rather than hard-coding strings.

Never put visible strings directly in business logic.

Transport API names may be incomplete. Use fallback logic such as:

``` text
requested locale name
→ English
→ Georgian
→ GeoGps/original name
→ safe placeholder
```

Preserve Unicode exactly.

------------------------------------------------------------------------

# 22. Accessibility

Accessibility is mandatory.

Support:

-   screen-reader labels;
-   minimum touch target sizes;
-   scalable text;
-   sufficient contrast;
-   non-color-only status indicators;
-   meaningful route/stop descriptions;
-   logical focus order.

A map marker should have a semantic description such as:

``` text
Bus 10, approaching Chavchavadze Street
```

rather than simply:

``` text
Marker
```

------------------------------------------------------------------------

# 23. Performance

Important because the app contains:

-   maps;
-   many route points;
-   stop markers;
-   continuously changing vehicle locations.

Requirements:

-   avoid parsing `getDbData` repeatedly;
-   cache normalized route data;
-   avoid rebuilding all polylines on every recomposition;
-   use stable/immutable UI models;
-   update only changed vehicle markers;
-   perform heavy parsing away from the main thread;
-   avoid excessive allocations in polling loops.

Measure before applying complex optimizations.

------------------------------------------------------------------------

# 24. Security

The application currently consumes an HTTPS API.

Rules:

-   never disable TLS verification;
-   never accept all certificates;
-   never ship debug trust managers;
-   do not hard-code secrets;
-   do not treat an API URL as a secret;
-   validate data before using it;
-   avoid logging personal location in production.

If user location is stored, define an explicit product/privacy
requirement first.

Do not persist location by default.

------------------------------------------------------------------------

# 25. Testing strategy

## Domain tests

Must cover:

-   route filtering;
-   stop ordering;
-   schedule conversion;
-   direction/status logic once understood;
-   fallback names;
-   error mapping.

These tests should run in `commonTest`.

## Repository tests

Use fake remote/local data sources.

Test:

``` text
cache available + network success
cache available + network failure
no cache + network success
no cache + network failure
malformed API response
```

## API parsing tests

Store small sanitized JSON fixtures.

Verify API casing explicitly:

``` text
Lat
Lon
lat
lon
BusStopLatitude
BusStopLongitude
```

## ViewModel tests

Test state transitions:

``` text
Idle → Loading → Content
Idle → Loading → Error
Content → Refreshing → Content
```

## Platform tests

Add Android/iOS tests for behavior that cannot be validated in common
code.

------------------------------------------------------------------------

# 26. Code quality

Use:

-   ktlint or equivalent formatting;
-   Detekt or equivalent static analysis;
-   compiler warnings treated seriously;
-   version catalogs;
-   centralized dependency versions;
-   CI checks.

Every pull request should at least run:

``` text
format/lint
static analysis
common tests
Android compilation
iOS framework/build verification
```

------------------------------------------------------------------------

# 27. Naming

Prefer explicit names.

Good:

``` text
GetRouteDetailsUseCase
ThetaMapsRemoteDataSource
TransportRepositoryImpl
ObserveRouteVehiclesUseCase
```

Avoid meaningless names:

``` text
Manager
Helper
Utils
Common
DataHandler
ApiManager
```

A `Utils.kt` file should be treated as a warning sign.

------------------------------------------------------------------------

# 28. Models

Keep model types separate when responsibilities differ:

``` text
DTO
↓ mapper
Domain Model
↓ mapper
UI Model
```

Do not mechanically create three models for trivial values if they add
no value, but never leak transport-specific JSON structures into UI.

Consider value classes for important IDs:

``` kotlin
@JvmInline
value class RouteId(val value: String)

@JvmInline
value class StopId(val value: String)
```

Ensure chosen value-class usage remains compatible with
serialization/platform boundaries where used.

------------------------------------------------------------------------

# 29. Date and time

Do not treat schedule strings as arbitrary text throughout the app.

API currently returns values such as:

``` text
"07:13"
"20:54"
```

Parse them at the boundary into an appropriate domain representation.

Use `kotlinx-datetime` where date/time functionality is needed.

Do not assume the device timezone is always the transport network
timezone.

The transport network is in Batumi, Georgia.

Service-day logic must remain explicit once discovered.

------------------------------------------------------------------------

# 30. Refresh behavior

Static network data:

``` text
getDbData
≈ server cache 600 seconds
```

Live route vehicles:

``` text
getBusLocsOnRoute
≈ server cache 4 seconds
```

Implement refresh policy centrally.

Do not allow every composable/screen component to start its own polling
loop.

There should be one owner of live vehicle observation for the relevant
route/session.

------------------------------------------------------------------------

# 31. Lifecycle rules

## Android

When the app or relevant screen becomes inactive:

-   stop unnecessary polling;
-   release expensive map resources where appropriate;
-   retain recoverable UI state.

## iOS

When moving to background:

-   do not assume continuous polling will run;
-   suspend unnecessary network work;
-   refresh when returning to foreground.

The UI must tolerate a time gap and refresh stale vehicle data.

------------------------------------------------------------------------

# 32. Logging and observability

Create a shared logging abstraction.

Levels:

``` text
Debug
Info
Warning
Error
```

Production logs must not contain:

-   precise user location;
-   personal identifiers;
-   authentication secrets;
-   full sensitive payloads.

Log enough information to diagnose:

-   endpoint category;
-   HTTP status;
-   parsing failures;
-   cache fallback;
-   polling failures.

------------------------------------------------------------------------

# 33. Feature boundaries for this application

Recommended initial features:

## Map

Responsibilities:

-   map viewport;
-   route geometry;
-   stop markers;
-   vehicle markers;
-   user location presentation.

## Routes

Responsibilities:

-   list/search routes;
-   select route.

## Route Details

Responsibilities:

-   route information;
-   ordered stops;
-   schedule;
-   live buses;
-   direction selection.

## Stops

Responsibilities:

-   stop list/search;
-   nearby stops when location is granted.

## Stop Details

Responsibilities:

-   routes serving stop;
-   timetable;
-   future ETA functionality if reliable data becomes available.

## Trip Planner

Keep isolated because `getPointsBetweenStations` is not fully documented
yet.

## Favorites

Local-first feature for:

-   favorite routes;
-   favorite stops.

## Settings

Contains:

-   language;
-   appearance/product settings;
-   optional map preferences.

------------------------------------------------------------------------

# 34. Suggested dependency graph

``` text
feature:map ───────────────┐
feature:routes ────────────┤
feature:route-details ─────┤
feature:stops ─────────────┤
feature:stop-details ──────┤
                           ▼
                    core:transport
                     /          \
                    ▼            ▼
              core:network   core:database
                    \            /
                     ▼          ▼
                       core:model
```

Features must not depend directly on other feature implementations
unless there is a deliberate navigation/API contract.

------------------------------------------------------------------------

# 35. Initial implementation order

The agent should implement the project in this order:

1.  Create KMP project and target Android + iOS.
2.  Configure Gradle/version catalog.
3.  Add core model module.
4.  Add networking module with Ktor.
5.  Implement ThetaMaps API from `THETAMAPS_API_KMP.md`.
6.  Add DTO parsing tests.
7.  Implement domain models and repository contract.
8.  Implement remote data source.
9.  Implement local cache.
10. Implement repository.
11. Add use cases.
12. Implement Routes feature.
13. Implement Route Details.
14. Implement map abstraction/integration.
15. Add live vehicle observation.
16. Implement Stops.
17. Add favorites.
18. Add location support.
19. Implement trip planner only after its API is fully documented.
20. Add Android/iOS lifecycle and permission integration.
21. Add accessibility/localization.
22. Add CI and release checks.

Do not start with a giant map screen containing networking, parsing, and
business logic.

------------------------------------------------------------------------

# 36. Definition of Done for each feature

A feature is complete only when:

-   architecture boundaries are respected;
-   loading/content/empty/error states exist;
-   common business logic has tests;
-   Android behavior is verified;
-   iOS behavior is verified;
-   dark mode works;
-   large text does not break critical UI;
-   accessibility labels exist;
-   offline/error behavior is defined;
-   cancellation/lifecycle behavior is correct;
-   no API DTO leaks into presentation;
-   no hard-coded visible strings;
-   no hard-coded endpoint inside feature code.

------------------------------------------------------------------------

# 37. Agent rules

The coding agent MUST:

1.  Read this file before modifying architecture.
2.  Read `THETAMAPS_API_KMP.md` before implementing transport
    networking.
3.  Preserve feature-first boundaries.
4.  Follow Clean Architecture dependency direction.
5.  Follow SOLID.
6.  Prefer constructor injection.
7.  Keep business logic in shared Kotlin.
8.  Keep platform-specific code minimal and explicit.
9.  Never invent API behavior.
10. Never invent meanings for undocumented `Status` fields.
11. Add tests when introducing business rules.
12. Handle coroutine cancellation correctly.
13. Respect Android and iOS lifecycles.
14. Never disable TLS validation.
15. Never use `GlobalScope`.
16. Never expose mutable state publicly.
17. Avoid unnecessary abstractions with no concrete use.
18. Avoid premature optimization.
19. Keep code compilable after each meaningful implementation step.
20. Prefer small, reviewable changes over giant rewrites.
21. Use MapLibre as the mandatory map SDK for Android and iOS.
22. Never expose MapLibre-specific types to the domain layer.
23. Keep the map style/tile provider configurable and preserve required attribution.

------------------------------------------------------------------------

# 38. Architectural decision rule

When deciding whether code belongs in shared or platform-specific code,
ask:

``` text
Is this business/product behavior identical on Android and iOS?
```

If yes:

``` text
commonMain
```

If it fundamentally interacts with a platform API:

``` text
platform implementation
```

Then expose the smallest useful shared contract.

The goal is **not maximum percentage of shared code**.

The goal is:

``` text
maximum reuse of stable business logic
+
native-correct platform behavior
+
maintainable boundaries
```

------------------------------------------------------------------------

# 39. Final project quality target

The application should be designed so that:

-   ThetaMaps can be replaced without rewriting UI;
-   the local database can be replaced without rewriting domain logic;
-   Android/iOS platform services can evolve independently;
-   features can be developed and tested separately;
-   live bus polling cannot leak after navigation;
-   offline route/stop data remains usable;
-   UI remains responsive while parsing large transport datasets;
-   future API endpoints can be added without breaking existing
    features.

The architecture is a tool for maintaining these properties. Do not add
layers or abstractions merely to make the project look architecturally
complex.


# 18. Maps — MapLibre is mandatory

The project MUST use **MapLibre** as the primary map technology on both Android and iOS.

Do not replace MapLibre with Google Maps, Yandex Maps, MapKit, or another map SDK unless there is an explicit architectural/product decision to do so.

Target implementation:

```text
KMP shared presentation/domain
        ↓
shared map models/state
        ↓
MapLibre integration
   ├── Android → MapLibre
   └── iOS     → MapLibre
```

MapLibre-specific SDK objects MUST NOT leak into the domain layer.

Domain/shared business models should use platform-independent types:

```kotlin
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

data class VehicleMarker(
    val id: String,
    val position: GeoPoint,
    val routeId: String
)
```

Domain must not import:

```text
MapLibre SDK classes
Google Maps classes
MapKit classes
Compose map SDK classes
```

Presentation/map adapters are responsible for converting shared models into MapLibre objects.

## Route rendering

ThetaMaps `routeCoordinatesGrouped` contains ordered route points:

```json
[
  { "lat": 41.6208397963, "lon": 41.5918787908 },
  { "lat": 41.621599521, "lon": 41.592486 }
]
```

Convert these points into a **GeoJSON LineString** (or the most appropriate MapLibre source representation) and render them as route polylines.

Do not rebuild static route geometry every time vehicle positions refresh.

## Stops

Bus stops should be rendered from:

```text
BusStopLatitude
BusStopLongitude
```

Requirements:

- stable stop IDs;
- selectable markers;
- clustering only if needed after performance testing;
- selected-stop visual state;
- efficient visibility/filtering by viewport/zoom when necessary.

## Live buses

Vehicle coordinates come from:

```text
GET /api/getBusLocsOnRoute?routeId={id}
```

and contain:

```text
Lat
Lon
Name
Status
```

Update only the affected MapLibre vehicle source/markers during live polling.

Do NOT:

```text
recreate the whole map
recreate all route geometry
reset the camera
reload all stops
```

on every 4–5 second live update.

## Camera

The map layer must support:

- initial Batumi viewport;
- fit route bounds;
- focus selected stop;
- focus selected vehicle;
- user-controlled camera;
- user-location focus when permission is granted.

Automatic updates must not constantly override a camera that the user is manually moving.

## Map style and tiles

Map style/tile configuration MUST be configurable and must not be hard-coded throughout feature code.

Keep style configuration behind the map/data configuration layer so the tile/style provider can be changed without rewriting map features.

Any third-party tile/style provider must be reviewed for:

- production usage terms;
- attribution requirements;
- rate limits;
- API keys if applicable;
- offline/cache restrictions.

MapLibre is the rendering SDK; it does not by itself remove the need to choose and comply with a map tile/style provider.

## Attribution

Preserve all attribution required by MapLibre and the selected map-data/tile provider. Do not hide required map attribution.

## Performance

The map is a high-frequency UI component. The implementation must:

- keep route geometry cached;
- avoid full-source recreation when a data update can be incremental;
- avoid unnecessary recomposition;
- use stable marker IDs;
- update live vehicles efficiently;
- stop live polling when the relevant screen is inactive;
- profile performance with realistic numbers of stops/routes/vehicles.

## Architecture rule

MapLibre is an implementation detail of the map/presentation/platform layer.

The rest of the application should be able to work with:

```text
GeoPoint
RouteGeometry
BusStop
Vehicle
MapUiState
```

without knowing which map SDK renders them.

# 40. Implemented foundation decisions — 2026-09-16

- Existing `:shared` and `:androidApp` modules are retained. Shared feature packages
  are the initial boundaries; they can become Gradle modules when isolation warrants
  the extra build complexity. `scripts/check_architecture.py` checks imports now.
- Compose Multiplatform is used for Android/iOS. MapLibre Compose 0.16.0 supplies
  Android OpenGL and iOS native rendering. Domain contains no SDK-specific types.
- Manual constructor injection lives in `AppGraph`. Platform roots provide Ktor
  engines, `PersistentFiles`, and structured log sinks. The root owns and closes
  both the original and configured HTTP clients.
- Atomic files are the initial persistence implementation, behind interfaces.
  Android uses `AtomicFile`; iOS uses atomic Foundation writes in Application
  Support. The observed database is about 1.2 MB. A versioned snapshot stores its
  timestamp and complete payload; normalization/parsing runs on Default once per
  load/refresh, with normalized models retained in memory. This preserves unknown
  fields and allows SQLite/Room migration without changing domain contracts.
- Cached content is emitted before the network completes. Invalid responses do not
  replace the cache. Cache-write failure retains current fresh in-memory content and
  exposes a storage error. Corrupt cache reads are logged and recovered via network.
- One foreground application collector owns database refresh. Map and route-details
  collect vehicles only while visible; `WhileSubscribed(0)` cancels polling as soon
  as the view stops collecting. Live positions are never persisted and expire after
  30 seconds of failures. Returning to the foreground starts a fresh observation.
- Map stop/route sources are separate from the vehicle source. Vehicle updates do
  not rebuild route geometry or change the camera. Camera fitting responds to route
  selection or an explicit action. Map camera state is saved across destinations.
- Feature StateFlow values are read-only; one-shot effects use a buffered Channel
  exposed as Flow and a single lifecycle-aware UI collector. Search is local and
  executes on Default. Navigation carries typed destinations and string IDs only.
- Shared read-only feature query APIs (`SearchStopsUseCase`, `GetRouteDetailsUseCase`)
  may be composed by other features. No feature accesses another feature's data or
  presentation implementation. These queries are extraction candidates if modules
  are split; no duplicate repository is introduced.
- Typed English/Georgian string catalogs live in `core/localization`. Both catalogs
  are completeness-tested. No UI component contains user-facing literal copy.
  Transport names use requested language → English → Georgian → original → ID.
  iOS permission descriptions use localized InfoPlist.strings.
- Timetables are parsed into `ScheduleTime`. No service-day or ETA inference is
  made because the API does not document those rules. Display explicitly identifies
  Batumi time. Raw Status values are presented as neutral stop-group identifiers.
- Optional foreground location uses MapLibre's platform provider and permission
  adapter. No location is requested at launch or persisted. Denial, rationale and
  system-settings paths are presented from a user action. iOS has a native edge-pan
  back handler; Android uses the lifecycle-bound system back handler.
- Logs accept only enumerated event/error categories, never arbitrary payloads,
  coordinates, plate numbers or opaque IDs.

## Map provider review

OpenFreeMap's [mobile integration guide](https://openfreemap.org/quick_start/)
explicitly supports these styles with MapLibre Native. The provider's
[terms](https://openfreemap.org/tos/) were reviewed on 2026-09-16 (revision shown:
2026-09-09). The public service is as-is and may change/discontinue; attribution
is preserved through MapLibre's default overlay. No bulk downloading/offline-region
feature is enabled. Re-review provider terms and capacity before distribution;
self-hosting or replacing styles is a configuration change.

## Release verification boundary

Successful compilation/simulator smoke checks do not certify physical-device
TalkBack/VoiceOver, every Dynamic Type size, memory-pressure behavior, store
compliance, production API service guarantees or translation quality. See
`IMPLEMENTATION_STATUS.md` for actual checks and remaining release verification.
