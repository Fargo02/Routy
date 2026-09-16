package com.example.routy.feature.route_details.presentation.state

import com.example.routy.core.navigation.Destination
import com.example.routy.core.transport.domain.AppError

sealed interface RouteDetailsEffect {
    data class Navigate(
        val destination: Destination,
    ) : RouteDetailsEffect

    data class ShowMap(
        val routeId: String,
    ) : RouteDetailsEffect

    data class Error(
        val error: AppError,
    ) : RouteDetailsEffect
}
