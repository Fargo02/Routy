package com.example.routy.feature.route_details.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import com.example.routy.feature.route_details.domain.*
import com.example.routy.feature.route_details.presentation.state.*
import com.example.routy.feature.stop_details.navigation.StopDetails
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RouteDetailsViewModel(
    private val routeId: String,
    private val transport: ObserveTransportUseCase,
    details: GetRouteDetailsUseCase,
    private val favorites: FavoritesUseCase,
    observeVehicles: ObserveRouteVehiclesUseCase,
) : ViewModel() {
    private val _effects = Channel<RouteDetailsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState =
        combine(transport.state, favorites.state) { network, saved ->
            RouteDetailsState(network.network?.let { details(it, routeId) }, routeId in saved.routeIds, network)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), RouteDetailsState())

    val vehicles =
        observeVehicles(routeId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
            VehicleState(),
        )

    fun actionHandler(action: RouteDetailsAction) {
        when (action) {
            RouteDetailsAction.ToggleFavorite ->
                viewModelScope.launch {
                    val result = favorites.route(routeId)
                    if (result is Outcome.Failure) sendEffect(RouteDetailsEffect.Error(result.error))
                }
            is RouteDetailsAction.SelectStop -> sendEffect(RouteDetailsEffect.Navigate(StopDetails(action.id)))
            RouteDetailsAction.ShowMap -> sendEffect(RouteDetailsEffect.ShowMap(routeId))
            RouteDetailsAction.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }

    private fun sendEffect(effect: RouteDetailsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
