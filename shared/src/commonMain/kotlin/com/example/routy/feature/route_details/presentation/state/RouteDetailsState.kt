package com.example.routy.feature.route_details.presentation.state

import com.example.routy.core.transport.domain.NetworkState
import com.example.routy.feature.route_details.domain.RouteDetails

data class RouteDetailsState(
    val details: RouteDetails? = null,
    val favorite: Boolean = false,
    val network: NetworkState = NetworkState(),
)
