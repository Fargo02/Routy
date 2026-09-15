package com.example.routy.feature.stop_details.presentation

import androidx.lifecycle.viewModelScope
import com.example.routy.core.mvi.MviViewModel
import com.example.routy.core.navigation.Destination
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import com.example.routy.feature.stop_details.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StopDetailsState(
    val details: StopDetails? = null,
    val favorite: Boolean = false,
    val network: NetworkState = NetworkState(),
)

sealed interface StopDetailsIntent {
    data object ToggleFavorite : StopDetailsIntent

    data class SelectRoute(
        val id: String,
    ) : StopDetailsIntent

    data object Retry : StopDetailsIntent
}

sealed interface StopDetailsEffect {
    data class Navigate(
        val destination: Destination,
    ) : StopDetailsEffect

    data class Error(
        val error: AppError,
    ) : StopDetailsEffect
}

class StopDetailsViewModel(
    private val stopId: String,
    private val transport: ObserveTransportUseCase,
    details: GetStopDetailsUseCase,
    private val favorites: FavoritesUseCase,
) : MviViewModel<StopDetailsIntent, StopDetailsEffect>() {
    val state =
        combine(transport.state, favorites.state) { network, saved ->
            StopDetailsState(network.network?.let { details(it, stopId) }, stopId in saved.stopIds, network)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), StopDetailsState())

    override fun accept(intent: StopDetailsIntent) {
        when (intent) {
            StopDetailsIntent.ToggleFavorite ->
                viewModelScope.launch {
                    val result = favorites.stop(stopId)
                    if (result is Outcome.Failure) effect(StopDetailsEffect.Error(result.error))
                }
            is StopDetailsIntent.SelectRoute -> effect(StopDetailsEffect.Navigate(Destination.RouteDetails(intent.id)))
            StopDetailsIntent.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }
}
