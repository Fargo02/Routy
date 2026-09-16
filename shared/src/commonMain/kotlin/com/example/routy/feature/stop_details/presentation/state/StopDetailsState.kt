package com.example.routy.feature.stop_details.presentation.state

import com.example.routy.core.transport.domain.NetworkState
import com.example.routy.feature.stop_details.domain.StopDetails

data class StopDetailsState(
    val details: StopDetails? = null,
    val favorite: Boolean = false,
    val network: NetworkState = NetworkState(),
)
