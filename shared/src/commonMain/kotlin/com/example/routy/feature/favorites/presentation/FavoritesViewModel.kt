package com.example.routy.feature.favorites.presentation

import androidx.lifecycle.viewModelScope
import com.example.routy.core.mvi.MviViewModel
import com.example.routy.core.navigation.Destination
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FavoritesState(
    val routes: List<Route> = emptyList(),
    val stops: List<BusStop> = emptyList(),
    val network: NetworkState = NetworkState(),
)

sealed interface FavoritesIntent {
    data class SelectRoute(
        val id: String,
    ) : FavoritesIntent

    data class SelectStop(
        val id: String,
    ) : FavoritesIntent

    data object Retry : FavoritesIntent
}

sealed interface FavoritesEffect {
    data class Navigate(
        val destination: Destination,
    ) : FavoritesEffect
}

class FavoritesViewModel(
    private val transport: ObserveTransportUseCase,
    favorites: FavoritesUseCase,
) : MviViewModel<FavoritesIntent, FavoritesEffect>() {
    val state =
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

    override fun accept(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.SelectRoute -> effect(FavoritesEffect.Navigate(Destination.RouteDetails(intent.id)))
            is FavoritesIntent.SelectStop -> effect(FavoritesEffect.Navigate(Destination.StopDetails(intent.id)))
            FavoritesIntent.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }
}
