package com.example.routy.feature.favorites.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import com.example.routy.feature.favorites.presentation.state.*
import com.example.routy.feature.route_details.navigation.RouteDetails
import com.example.routy.feature.stop_details.navigation.StopDetails
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val transport: ObserveTransportUseCase,
    favorites: FavoritesUseCase,
) : ViewModel() {
    private val _effects = Channel<FavoritesEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState =
        combine(transport.state, favorites.state) { network, saved ->
            FavoritesState(
                network.network
                    ?.routes
                    .orEmpty()
                    .filter { it.id in saved.routeIds },
                network.network?.stops.orEmpty().filter {
                    it.id in
                        saved.stopIds
                },
                network,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), FavoritesState())

    fun actionHandler(action: FavoritesAction) {
        when (action) {
            is FavoritesAction.SelectRoute -> sendEffect(FavoritesEffect.Navigate(RouteDetails(action.id)))
            is FavoritesAction.SelectStop -> sendEffect(FavoritesEffect.Navigate(StopDetails(action.id)))
            FavoritesAction.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }

    private fun sendEffect(effect: FavoritesEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
