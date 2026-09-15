package com.example.routy.feature.routes.presentation

import androidx.lifecycle.viewModelScope
import com.example.routy.core.mvi.MviViewModel
import com.example.routy.core.navigation.Destination
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.routes.domain.SearchRoutesUseCase
import com.example.routy.feature.stops.domain.SearchStopsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RoutesState(
    val query: String = "",
    val items: List<Route> = emptyList(),
    val network: NetworkState = NetworkState(),
    val stops: List<BusStop> = emptyList(),
)

sealed interface RoutesIntent {
    data class Search(
        val query: String,
    ) : RoutesIntent

    data class Select(
        val id: String,
    ) : RoutesIntent

    data class SelectStop(
        val id: String,
    ) : RoutesIntent

    data object Retry : RoutesIntent
}

sealed interface RoutesEffect {
    data class Navigate(
        val destination: Destination,
    ) : RoutesEffect
}

class RoutesViewModel(
    private val transport: ObserveTransportUseCase,
    private val search: SearchRoutesUseCase,
    private val searchStops: SearchStopsUseCase = SearchStopsUseCase(),
) : MviViewModel<RoutesIntent, RoutesEffect>() {
    private val query = MutableStateFlow("")
    val state =
        combine(transport.state, query) { network, term ->
            RoutesState(
                term,
                search(network.network?.routes.orEmpty(), term),
                network,
                if (term.isBlank()) emptyList() else searchStops(network.network?.stops.orEmpty(), term),
            )
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), RoutesState())

    override fun accept(intent: RoutesIntent) {
        when (intent) {
            is RoutesIntent.Search -> query.value = intent.query
            is RoutesIntent.SelectStop -> effect(RoutesEffect.Navigate(Destination.StopDetails(intent.id)))
            is RoutesIntent.Select -> effect(RoutesEffect.Navigate(Destination.RouteDetails(intent.id)))
            RoutesIntent.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }
}
