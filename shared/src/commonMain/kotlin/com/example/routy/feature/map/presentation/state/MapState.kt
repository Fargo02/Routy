package com.example.routy.feature.map.presentation.state

import com.example.routy.core.transport.domain.BusStop
import com.example.routy.core.transport.domain.NetworkState
import com.example.routy.core.transport.domain.Route
import com.example.routy.core.transport.domain.RouteGeometry
import com.example.routy.feature.map.presentation.EMPTY_GEOJSON

data class MapCamera(
    val longitude: Double = 41.6367,
    val latitude: Double = 41.6461,
    val zoom: Double = 13.0,
)

data class MapState(
    val network: NetworkState = NetworkState(),
    val selectedRouteIds: List<String> = emptyList(),
    val favoriteRouteIds: Set<String> = emptySet(),
    val favoriteStopIds: Set<String> = emptySet(),
    val stops: List<BusStop> = emptyList(),
    val geometries: List<RouteGeometry> = emptyList(),
    val stopGeoJson: String = EMPTY_GEOJSON,
    val search: MapSearchState = MapSearchState(),
) {
    val routeId: String?
        get() = selectedRouteIds.lastOrNull()
}

data class MapSearchState(
    val isOpen: Boolean = false,
    val query: String = "",
    val quickRoutes: List<Route> = emptyList(),
    val routes: List<Route> = emptyList(),
    val stops: List<BusStop> = emptyList(),
    val routeStopCounts: Map<String, Int> = emptyMap(),
)
