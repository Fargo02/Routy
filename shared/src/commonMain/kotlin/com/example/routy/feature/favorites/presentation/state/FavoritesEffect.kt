package com.example.routy.feature.favorites.presentation.state

import com.example.routy.core.navigation.Destination

sealed interface FavoritesEffect {
    data class Navigate(
        val destination: Destination,
    ) : FavoritesEffect
}
