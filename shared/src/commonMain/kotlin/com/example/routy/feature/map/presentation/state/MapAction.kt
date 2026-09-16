package com.example.routy.feature.map.presentation.state

sealed interface MapAction {
    data class SelectRoute(
        val id: String,
    ) : MapAction

    data class SelectStop(
        val id: String,
    ) : MapAction

    data class SelectVehicle(
        val id: String,
    ) : MapAction

    data object OpenStops : MapAction

    data object OpenRouteDetails : MapAction

    data object MyLocation : MapAction

    data object Retry : MapAction
}
