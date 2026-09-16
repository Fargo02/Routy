package com.example.routy.feature.routes.presentation.state

sealed interface RoutesAction {
    data class Search(
        val query: String,
    ) : RoutesAction

    data class Select(
        val id: String,
    ) : RoutesAction

    data class SelectStop(
        val id: String,
    ) : RoutesAction

    data object Retry : RoutesAction
}
