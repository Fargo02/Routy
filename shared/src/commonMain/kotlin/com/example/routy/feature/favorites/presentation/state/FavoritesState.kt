package com.example.routy.feature.favorites.presentation.state

import com.example.routy.core.transport.domain.BusStop
import com.example.routy.core.transport.domain.NetworkState
import com.example.routy.core.transport.domain.Route

data class FavoritesState(
    val routes: List<Route> = emptyList(),
    val stops: List<BusStop> = emptyList(),
    val network: NetworkState = NetworkState(),
)
