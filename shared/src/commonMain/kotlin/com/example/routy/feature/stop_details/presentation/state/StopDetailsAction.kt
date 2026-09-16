package com.example.routy.feature.stop_details.presentation.state

sealed interface StopDetailsAction {
    data object ToggleFavorite : StopDetailsAction

    data class SelectRoute(
        val id: String,
    ) : StopDetailsAction

    data object Retry : StopDetailsAction
}
