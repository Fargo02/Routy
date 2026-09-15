package com.example.routy.feature.route_details.presentation

import androidx.lifecycle.viewModelScope
import com.example.routy.core.mvi.MviViewModel
import com.example.routy.core.navigation.Destination
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import com.example.routy.feature.route_details.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RouteDetailsState(
    val details: RouteDetails? = null,
    val favorite: Boolean = false,
    val network: NetworkState = NetworkState(),
)

sealed interface RouteDetailsIntent {
    data object ToggleFavorite : RouteDetailsIntent

    data class SelectStop(
        val id: String,
    ) : RouteDetailsIntent

    data object ShowMap : RouteDetailsIntent

    data object Retry : RouteDetailsIntent
}

sealed interface RouteDetailsEffect {
    data class Navigate(
        val destination: Destination,
    ) : RouteDetailsEffect

    data class ShowMap(
        val routeId: String,
    ) : RouteDetailsEffect

    data class Error(
        val error: AppError,
    ) : RouteDetailsEffect
}

class RouteDetailsViewModel(
    private val routeId: String,
    private val transport: ObserveTransportUseCase,
    details: GetRouteDetailsUseCase,
    private val favorites: FavoritesUseCase,
) : MviViewModel<RouteDetailsIntent, RouteDetailsEffect>() {
    val state =
        combine(transport.state, favorites.state) { network, saved ->
            RouteDetailsState(network.network?.let { details(it, routeId) }, routeId in saved.routeIds, network)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), RouteDetailsState())

    override fun accept(intent: RouteDetailsIntent) {
        when (intent) {
            RouteDetailsIntent.ToggleFavorite ->
                viewModelScope.launch {
                    val result = favorites.route(routeId)
                    if (result is Outcome.Failure) effect(RouteDetailsEffect.Error(result.error))
                }
            is RouteDetailsIntent.SelectStop -> effect(RouteDetailsEffect.Navigate(Destination.StopDetails(intent.id)))
            RouteDetailsIntent.ShowMap -> effect(RouteDetailsEffect.ShowMap(routeId))
            RouteDetailsIntent.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }
}
