package com.example.routy.feature.stop_details.presentation.state

import com.example.routy.core.transport.domain.AppError

sealed interface StopDetailsEffect {
    data class ShowRouteOnMap(
        val id: String,
    ) : StopDetailsEffect

    data class Error(
        val error: AppError,
    ) : StopDetailsEffect
}
