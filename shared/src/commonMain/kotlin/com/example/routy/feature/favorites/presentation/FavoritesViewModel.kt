package com.example.routy.feature.favorites.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routy.core.preferences.domain.Preferences
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import com.example.routy.feature.favorites.presentation.state.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val transport: ObserveTransportUseCase,
    private val favorites: FavoritesUseCase,
) : ViewModel() {
    val uiState =
        combine(transport.state, favorites.state) { network, saved -> favoritesState(network, saved) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(0),
                favoritesState(transport.state.value, favorites.state.value),
            )

    private fun favoritesState(
        network: NetworkState,
        saved: Preferences,
    ) = FavoritesState(
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

    fun actionHandler(action: FavoritesAction) {
        when (action) {
            is FavoritesAction.RemoveRoute -> viewModelScope.launch { favorites.route(action.id) }
            is FavoritesAction.RemoveStop -> viewModelScope.launch { favorites.stop(action.id) }
            FavoritesAction.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }
}
