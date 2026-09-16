package com.example.routy.feature.map.presentation.state

import com.example.routy.core.transport.domain.BusStop
import com.example.routy.core.transport.domain.NetworkState
import com.example.routy.core.transport.domain.RouteGeometry
import com.example.routy.feature.map.presentation.EMPTY_GEOJSON

data class MapState(
    val network: NetworkState = NetworkState(),
    val selectedRouteIds: List<String> = emptyList(),
    val favoriteRouteIds: Set<String> = emptySet(),
    val stops: List<BusStop> = emptyList(),
    val geometries: List<RouteGeometry> = emptyList(),
    val stopGeoJson: String = EMPTY_GEOJSON,
) {
    val routeId: String?
        get() = selectedRouteIds.lastOrNull()
}
