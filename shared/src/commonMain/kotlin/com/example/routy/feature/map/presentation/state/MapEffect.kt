package com.example.routy.feature.map.presentation.state

import com.example.routy.core.navigation.Destination

sealed interface MapEffect {
    data class Navigate(
        val destination: Destination,
    ) : MapEffect

    data class FocusRoute(
        val id: String,
    ) : MapEffect

    data object RequestLocation : MapEffect
}
