package com.example.routy.feature.map.presentation.state

sealed interface MapAction {
    data class SelectRoute(
        val id: String,
    ) : MapAction

    data object ClearSelectedRoutes : MapAction

    data object ClearSelectedStop : MapAction

    data object OpenSearch : MapAction

    data object CloseSearch : MapAction

    data class SearchQueryChanged(
        val query: String,
    ) : MapAction

    data object ClearSearch : MapAction

    data class SelectSearchRoute(
        val id: String,
    ) : MapAction

    data class SelectSearchStop(
        val id: String,
    ) : MapAction

    data class SelectStop(
        val id: String,
    ) : MapAction

    data class ShowStopOnMap(
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
