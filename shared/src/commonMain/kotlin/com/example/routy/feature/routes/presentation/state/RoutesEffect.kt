package com.example.routy.feature.routes.presentation.state

import com.example.routy.core.navigation.Destination

sealed interface RoutesEffect {
    data class Navigate(
        val destination: Destination,
    ) : RoutesEffect
}
