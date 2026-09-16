package com.example.routy.feature.route_details.presentation.state

sealed interface RouteDetailsAction {
    data object ToggleFavorite : RouteDetailsAction

    data class SelectStop(
        val id: String,
    ) : RouteDetailsAction

    data object ShowMap : RouteDetailsAction

    data object Retry : RouteDetailsAction
}
