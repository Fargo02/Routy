package com.example.routy.feature.favorites.presentation.state

sealed interface FavoritesAction {
    data class SelectRoute(
        val id: String,
    ) : FavoritesAction

    data class SelectStop(
        val id: String,
    ) : FavoritesAction

    data object Retry : FavoritesAction
}
