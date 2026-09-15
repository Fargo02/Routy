# Batauto / ThetaMaps API --- KMP Integration Notes

> **Purpose:** handoff document for an engineering agent building a
> Kotlin Multiplatform (KMP) client for Batumi public transport.
>
> **Important:** these endpoints were observed from the official Batauto
> client/web application. They appear to be internal/undocumented APIs.
> Do **not** assume a stability or redistribution guarantee. Keep the
> API behind a repository/data-source abstraction and make the base URL
> configurable.

## 1. Base URL

``` text
https://thetamaps.site:54321
```

Observed server address:

``` text
195.238.122.165:54321
```

Use the hostname, **not the IP address**, in application code.

Recommended KMP configuration:

``` kotlin
const val BASE_URL = "https://thetamaps.site:54321"
```

## 2. Confirmed endpoints

### 2.1 `GET /api/getDbData`

Returns the main public-transport database used by the client.

``` http
GET https://thetamaps.site:54321/api/getDbData
```

Observed response header:

``` text
Content-Type: application/json
Cache-Control: public, max-age=600
```

The response contains at least:

-   `busStops` --- bus stops;
-   route associations for every stop;
-   stop order within a route;
-   scheduled times for a route at a stop;
-   route metadata/status information;
-   route names;
-   route geometry/coordinates.

Observed top-level structure:

``` json
{
  "data": {
    "busStops": {},
    "routeStatusInfo": {},
    "routesNames": {},
    "routeCoordinatesGrouped": {}
  }
}
```

#### `busStops`

The object is keyed by the stop ID.

Example:

``` json
{
  "data": {
    "busStops": {
      "615181c2384bd31f651a669e": {
        "BusStopIdGeoGps": "615181c2384bd31f651a669e",
        "BusStopNameGeoGps": "1963 ბათუმის ყინულის არენა",
        "BusStopNumber": 1963,
        "BusStopNameKA": "ბათუმის ყინულის არენა",
        "BusStopNameEN": "1963 ბათუმის ყინულის არენა",
        "BusStopLatitude": 41.633789,
        "BusStopLongitude": 41.63478,
        "RouteT_RouteIdGeoGps": null,
        "routes": {
          "5ed60e97340f60873ff9e1cb": {
            "Status": 1,
            "Order": 7,
            "times": [
              "07:13",
              "07:45",
              "08:17"
            ]
          }
        }
      }
    }
  }
}
```

Observed field meanings:

  -----------------------------------------------------------------------
  Field                               Meaning
  ----------------------------------- -----------------------------------
  `BusStopIdGeoGps`                   unique stop ID

  `BusStopNumber`                     public/numeric stop number

  `BusStopNameGeoGps`                 source/original stop name

  `BusStopNameKA`                     Georgian stop name

  `BusStopNameEN`                     English stop name

  `BusStopLatitude`                   latitude

  `BusStopLongitude`                  longitude

  `RouteT_RouteIdGeoGps`              nullable route-related field; exact
                                      semantics not yet confirmed

  `routes`                            routes serving this stop
  -----------------------------------------------------------------------

Each entry in `routes` is keyed by a route ID.

Observed route-at-stop fields:

  -----------------------------------------------------------------------
  Field                               Meaning
  ----------------------------------- -----------------------------------
  `Status`                            route/direction/status value; exact
                                      enum semantics require verification

  `Order`                             stop position/order within this
                                      route/direction

  `times`                             scheduled times at this stop
                                      (`HH:mm`)
  -----------------------------------------------------------------------

Do not hard-code the semantics of `Status` until both values/directions
have been tested.

#### `routeCoordinatesGrouped`

Contains route geometry. Keys are route IDs and values are ordered
coordinate arrays.

Example:

``` json
{
  "routeCoordinatesGrouped": {
    "5ed2bd4f657784b5a98a8c7e": [
      {
        "lat": 41.6208397963,
        "lon": 41.5918787908
      },
      {
        "lat": 41.621599521,
        "lon": 41.592486
      }
    ]
  }
}
```

The KMP client can convert this ordered list into a map polyline.

Do not confuse:

``` text
BusStopLatitude / BusStopLongitude
```

with route geometry fields:

``` text
lat / lon
```

The API uses inconsistent casing.

#### `routesNames`

Confirmed to exist in `getDbData`.

It maps route identifiers to human-readable route information/names. Its
complete object schema has not yet been captured, so deserialize
defensively.

#### `routeStatusInfo`

Confirmed to exist in `getDbData`.

Its exact schema and semantics have not yet been documented. Treat it as
optional/forward-compatible data until captured.

------------------------------------------------------------------------

### 2.2 `GET /api/getBusLocsOnRoute`

Returns current/live vehicle positions for a specific route.

``` http
GET https://thetamaps.site:54321/api/getBusLocsOnRoute?routeId={ROUTE_ID}
```

Confirmed example:

``` http
GET /api/getBusLocsOnRoute?routeId=60acde9ffcc7a224160c587c
```

Observed response header:

``` text
Content-Type: application/json
Cache-Control: public, max-age=4
```

This indicates live data is expected to update frequently.

Observed response:

``` json
{
  "data": [
    {
      "Lat": 41.6291033,
      "Lon": 41.6327,
      "Status": -1,
      "Name": "AA 315 GT"
    },
    {
      "Lat": 41.6295633,
      "Lon": 41.632445,
      "Status": -1,
      "Name": "UU 326 SU"
    }
  ]
}
```

Observed fields:

  -----------------------------------------------------------------------
  Field                               Meaning
  ----------------------------------- -----------------------------------
  `Lat`                               current vehicle latitude

  `Lon`                               current vehicle longitude

  `Status`                            vehicle/status field; observed
                                      value `-1`; exact semantics unknown

  `Name`                              vehicle identifier; observed values
                                      have Georgian registration-plate
                                      format
  -----------------------------------------------------------------------

Recommended polling interval:

``` text
4–5 seconds
```

Do not poll significantly faster than the server's observed `max-age=4`.

Suggested KMP model:

``` kotlin
@Serializable
data class VehiclePosition(
    @SerialName("Lat") val latitude: Double,
    @SerialName("Lon") val longitude: Double,
    @SerialName("Status") val status: Int,
    @SerialName("Name") val name: String
)

@Serializable
data class VehiclePositionsResponse(
    val data: List<VehiclePosition>
)
```

------------------------------------------------------------------------

### 2.3 `GET /api/getPointsBetweenStations`

**Observed endpoint, partially documented.**

The official web client makes a request named:

``` text
getPointsBetweenStations
```

Known API path:

``` text
/api/getPointsBetweenStations
```

It appears to be used when working with points/path data between
stations, but its complete query parameters, response schema, and exact
role have **not yet been captured**.

Engineering rule:

``` text
DO NOT invent parameters for this endpoint.
```

Before implementing it, capture the complete Request URL and Response
from Chrome DevTools.

Mark implementation as TODO:

``` kotlin
// TODO: Capture and document getPointsBetweenStations request/response.
```

------------------------------------------------------------------------

## 3. Relationship to BatBus

A separate service, BatBus, was observed exposing:

``` text
https://api.batbus.app/.../getDbData
https://api.batbus.app/.../getAllBuses
```

BatBus `getDbData` returned a structure containing the same
characteristic fields:

``` text
busStops
BusStopIdGeoGps
BusStopNameGeoGps
BusStopLatitude
BusStopLongitude
routes
Status
Order
routeCoordinatesGrouped
routesNames
routeStatusInfo
```

BatBus live vehicle data also uses:

``` json
{
  "Lat": 41.629355,
  "Lon": 41.6316283,
  "Status": -1,
  "Name": "CN 406 NC"
}
```

This strongly indicates that the two systems consume closely related
transport data.

For the KMP application, **do not depend on BatBus when the goal is
direct Batauto/ThetaMaps integration**.

Use:

``` text
thetamaps.site:54321
```

through a dedicated data-source abstraction.

------------------------------------------------------------------------

## 4. Suggested KMP architecture

Recommended shared-module structure:

``` text
shared/
  data/
    remote/
      ThetaMapsApi.kt
      ThetaMapsDataSource.kt
      dto/
        DbDataResponse.kt
        BusStopDto.kt
        RouteDto.kt
        RoutePointDto.kt
        VehiclePositionDto.kt
    repository/
      TransportRepositoryImpl.kt

  domain/
    model/
      BusStop.kt
      Route.kt
      RouteGeometry.kt
      Vehicle.kt
      StopSchedule.kt
    repository/
      TransportRepository.kt

  presentation/
    map/
    routes/
    stops/
```

Recommended networking stack:

``` text
Ktor Client
kotlinx.serialization
Coroutines / Flow
```

Example API interface:

``` kotlin
interface ThetaMapsApi {
    suspend fun getDbData(): DbDataResponse

    suspend fun getBusLocationsOnRoute(
        routeId: String
    ): VehiclePositionsResponse
}
```

Implementation concept:

``` kotlin
class ThetaMapsApiImpl(
    private val client: HttpClient,
    private val baseUrl: String
) : ThetaMapsApi {

    override suspend fun getDbData(): DbDataResponse =
        client.get("$baseUrl/api/getDbData").body()

    override suspend fun getBusLocationsOnRoute(
        routeId: String
    ): VehiclePositionsResponse =
        client.get("$baseUrl/api/getBusLocsOnRoute") {
            parameter("routeId", routeId)
        }.body()
}
```

Do not concatenate an untrusted `routeId` into a raw URL. Use Ktor's
`parameter()`.

------------------------------------------------------------------------

## 5. Caching strategy

### Database data

`getDbData` was observed with:

``` text
Cache-Control: public, max-age=600
```

Recommended:

-   keep an in-memory copy;
-   persist the last successful response locally;
-   refresh approximately every 10 minutes when the app is active;
-   allow stale cached data when offline;
-   avoid blocking map startup while refreshing if cached data exists.

### Vehicle positions

`getBusLocsOnRoute` was observed with:

``` text
Cache-Control: public, max-age=4
```

Recommended:

-   poll only while the relevant route/map screen is visible;
-   interval approximately 4--5 seconds;
-   stop polling when the app/screen is inactive;
-   cancel the coroutine with the screen lifecycle;
-   retain the previous successful position briefly when a request
    fails.

------------------------------------------------------------------------

## 6. Parsing requirements

The API is not designed like a strongly typed modern public API. The
client must be defensive.

Important inconsistencies:

``` text
BusStopLatitude
BusStopLongitude

lat
lon

Lat
Lon
```

These are three different casing conventions in the same ecosystem.

Use explicit `@SerialName` mappings.

Unknown fields should not break deserialization:

``` kotlin
Json {
    ignoreUnknownKeys = true
    isLenient = true
}
```

Do not model IDs as numeric values. IDs such as:

``` text
615181c2384bd31f651a669e
5ed60e97340f60873ff9e1cb
60acde9ffcc7a224160c587c
```

must be represented as `String`.

------------------------------------------------------------------------

## 7. Core domain relationships

The application should construct the transport graph approximately as:

``` text
Route ID
   │
   ├── route metadata/name
   │
   ├── routeCoordinatesGrouped[routeId]
   │      └── ordered polyline points
   │
   ├── stops containing routes[routeId]
   │      ├── Order
   │      ├── Status
   │      └── times[]
   │
   └── getBusLocsOnRoute(routeId)
          └── current vehicles
```

To obtain ordered stops for a route:

1.  iterate through `busStops`;
2.  keep stops where `stop.routes[routeId] != null`;
3.  account for direction/status where necessary;
4.  sort using `Order`.

Do not assume `Order` alone uniquely describes both travel directions
until `Status` semantics are confirmed.

------------------------------------------------------------------------

## 8. Features possible with currently confirmed data

The confirmed API is sufficient to implement:

-   map of bus stops;
-   stop names in Georgian/English where provided;
-   route list after parsing `routesNames`;
-   route polyline;
-   stops belonging to a route;
-   ordered route stops;
-   scheduled times at stops;
-   current vehicles for a selected route;
-   periodic live movement updates;
-   vehicle markers on the map;
-   basic offline display of cached routes/stops.

Not yet sufficiently documented for production implementation:

-   trip planning between arbitrary points;
-   exact ETA calculation;
-   meaning of all `Status` values;
-   complete `routesNames` schema;
-   complete `routeStatusInfo` schema;
-   `getPointsBetweenStations` parameters and response;
-   service-day/calendar exceptions;
-   accessibility metadata;
-   official API usage/redistribution policy.

------------------------------------------------------------------------

## 9. Agent implementation rules

1.  **Do not invent undocumented endpoints or fields.**
2.  Treat `https://thetamaps.site:54321` as configurable.
3.  Keep transport API code isolated behind `TransportRepository`.
4.  Use `String` for all opaque IDs.
5.  Use explicit JSON field mappings.
6.  Use `ignoreUnknownKeys = true`.
7.  Respect observed server cache intervals.
8.  Do not poll live locations when they are not visible/needed.
9.  Preserve Georgian UTF-8 text exactly.
10. Prefer English name when available for English UI, Georgian name for
    Georgian UI.
11. Provide a fallback when one localization is missing.
12. Persist `getDbData` locally for offline startup.
13. Do not use BatBus endpoints as a fallback without an explicit
    product decision.
14. Do not assume this undocumented API will remain stable.
15. Log parsing/API failures without exposing sensitive device/user
    data.

------------------------------------------------------------------------

## 10. Endpoints summary

  -------------------------------------------------------------------------------------------------
  Method            Endpoint                                Status            Purpose
  ----------------- --------------------------------------- ----------------- ---------------------
  GET               `/api/getDbData`                        **Confirmed**     Stops, route
                                                                              relationships,
                                                                              schedules, route
                                                                              metadata and route
                                                                              geometry

  GET               `/api/getBusLocsOnRoute?routeId={id}`   **Confirmed**     Live vehicle
                                                                              positions for one
                                                                              route

  GET               `/api/getPointsBetweenStations`         **Observed /      Points/path-related
                                                            incomplete**      operation between
                                                                              stations;
                                                                              parameters/schema
                                                                              still need capture
  -------------------------------------------------------------------------------------------------

## 11. Next reverse-engineering tasks

Before declaring the integration complete, capture from the official
client:

``` text
getPointsBetweenStations
    → complete Request URL
    → query parameters
    → example Response

getDbData.data.routesNames
    → one complete route object

getDbData.data.routeStatusInfo
    → one complete object
```

Also inspect all `Fetch/XHR` calls while exercising:

-   Routes;
-   Stops;
-   Live;
-   destination/trip planning;
-   selecting a route;
-   selecting a stop;
-   changing direction;
-   changing language.

Add newly **observed** endpoints to this document only after their
request URL and response have been captured.

------------------------------------------------------------------------

## 12. API provenance

The official Batauto Android package examined during investigation was:

``` text
com.companyname.batumitransportmobile
```

The application contains/uses:

``` text
https://thetamaps.site:54321/
```

The official web client was then observed making successful JSON
requests directly to that host.

Therefore, for engineering purposes this document treats ThetaMaps as
the **observed backend used by the official Batauto client**.

This does **not** establish that ThetaMaps is the ultimate GPS
hardware/data-provider layer, nor does it establish a public API
license.

Before publishing an application that depends on it, verify
authorization, terms of use, rate limits, and redistribution
requirements with the service/operator.

## 13. Direct response capture — 2026-09-15

A successful TLS-verified GET to `/api/getDbData` returned 578 bus stops,
28 routesNames entries and 28 geometry entries. Counts describe this capture only.

Observed complete route metadata example:

```json
{"RouteIdGeoGps":"5ed6077c340f60873ff9e1be","RouteNameGeoGps":"2ა","RouteNameKA":"2ა","RouteNameEN":"2A","RouteIsCircle":false,"RouteSortOrder":25}
```

`routeStatusInfo` is keyed by route ID, then string status keys such as `1`
and `2`, with `highestId` and `lowestId` stop IDs. Their operational meaning
remains unconfirmed. Do not infer inbound/outbound or active/inactive meanings.
Route metadata remains optional and unknown fields are ignored.
