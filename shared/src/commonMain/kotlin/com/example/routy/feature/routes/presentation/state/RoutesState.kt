package com.example.routy.feature.routes.presentation.state

import com.example.routy.core.transport.domain.BusStop
import com.example.routy.core.transport.domain.NetworkState
import com.example.routy.core.transport.domain.Route

data class RoutesState(
    val query: String = "",
    val items: List<Route> = emptyList(),
    val network: NetworkState = NetworkState(),
    val stops: List<BusStop> = emptyList(),
)
