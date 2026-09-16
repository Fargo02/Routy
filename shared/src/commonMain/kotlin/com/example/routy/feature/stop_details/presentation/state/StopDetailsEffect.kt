package com.example.routy.feature.stop_details.presentation.state

import com.example.routy.core.navigation.Destination
import com.example.routy.core.transport.domain.AppError

sealed interface StopDetailsEffect {
    data class Navigate(
        val destination: Destination,
    ) : StopDetailsEffect

    data class Error(
        val error: AppError,
    ) : StopDetailsEffect
}
