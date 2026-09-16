package com.example.routy.feature.favorites.presentation.state

sealed interface FavoritesAction {
    data class RemoveRoute(
        val id: String,
    ) : FavoritesAction

    data class RemoveStop(
        val id: String,
    ) : FavoritesAction

    data object Retry : FavoritesAction
}
