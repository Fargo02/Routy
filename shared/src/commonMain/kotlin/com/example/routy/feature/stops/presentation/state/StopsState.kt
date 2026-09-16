package com.example.routy.feature.stops.presentation.state

import com.example.routy.core.transport.domain.BusStop
import com.example.routy.core.transport.domain.NetworkState

data class StopsState(
    val query: String = "",
    val items: List<BusStop> = emptyList(),
    val network: NetworkState = NetworkState(),
)
