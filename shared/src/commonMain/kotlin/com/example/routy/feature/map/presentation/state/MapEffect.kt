package com.example.routy.feature.map.presentation.state

import com.example.routy.core.navigation.Destination

sealed interface MapEffect {
    data class Navigate(
        val destination: Destination,
    ) : MapEffect

    data class ShowVehicle(
        val id: String,
    ) : MapEffect

    data object FitRoute : MapEffect

    data object RequestLocation : MapEffect
}
