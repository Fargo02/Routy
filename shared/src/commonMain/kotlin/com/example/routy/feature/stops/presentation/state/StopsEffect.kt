package com.example.routy.feature.stops.presentation.state

import com.example.routy.core.navigation.Destination

sealed interface StopsEffect {
    data class Navigate(
        val destination: Destination,
    ) : StopsEffect
}
