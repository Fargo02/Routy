package com.example.routy.feature.stops.presentation.state

sealed interface StopsAction {
    data class Search(
        val query: String,
    ) : StopsAction

    data class Select(
        val id: String,
    ) : StopsAction

    data object Retry : StopsAction
}
