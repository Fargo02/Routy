package com.example.routy.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {
    @Serializable data object Map : Destination

    @Serializable data object Routes : Destination

    @Serializable data object Stops : Destination

    @Serializable data object Favorites : Destination

    @Serializable data object Settings : Destination

    @Serializable data class RouteDetails(
        val routeId: String,
    ) : Destination

    @Serializable data class StopDetails(
        val stopId: String,
    ) : Destination
}
