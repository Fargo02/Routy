package com.example.routy.feature.map.presentation.state

sealed interface MapAction {
    data class SelectRoute(
        val id: String?,
    ) : MapAction

    data class SelectStop(
        val id: String,
    ) : MapAction

    data class SelectVehicle(
        val id: String,
    ) : MapAction

    data object OpenSearch : MapAction

    data object OpenStops : MapAction

    data object OpenSettings : MapAction

    data object OpenRouteDetails : MapAction

    data object FitRoute : MapAction

    data object MyLocation : MapAction

    data object Retry : MapAction
}
