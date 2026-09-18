package com.example.routy.feature.stop_details.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routy.core.preferences.domain.Preferences
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import com.example.routy.feature.stop_details.domain.*
import com.example.routy.feature.stop_details.presentation.state.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StopDetailsViewModel(
    private val stopId: String,
    private val transport: ObserveTransportUseCase,
    private val details: GetStopDetailsUseCase,
    private val favorites: FavoritesUseCase,
) : ViewModel() {
    private val _effects = Channel<StopDetailsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState =
        combine(transport.state, favorites.state) { network, saved -> stopDetailsState(network, saved) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(0),
                stopDetailsState(transport.state.value, favorites.state.value),
            )

    private fun stopDetailsState(
        network: NetworkState,
        saved: Preferences,
    ) = StopDetailsState(network.network?.let { details(it, stopId) }, stopId in saved.stopIds, network)

    fun actionHandler(action: StopDetailsAction) {
        when (action) {
            StopDetailsAction.ToggleFavorite ->
                viewModelScope.launch {
                    val result = favorites.stop(stopId)
                    if (result is Outcome.Failure) sendEffect(StopDetailsEffect.Error(result.error))
                }
            is StopDetailsAction.SelectRoute -> sendEffect(StopDetailsEffect.ShowRouteOnMap(action.id))
            StopDetailsAction.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }

    private fun sendEffect(effect: StopDetailsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
