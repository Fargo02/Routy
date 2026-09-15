package com.example.routy.feature.stops.presentation

import androidx.lifecycle.viewModelScope
import com.example.routy.core.mvi.MviViewModel
import com.example.routy.core.navigation.Destination
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.stops.domain.SearchStopsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StopsState(
    val query: String = "",
    val items: List<BusStop> = emptyList(),
    val network: NetworkState = NetworkState(),
)

sealed interface StopsIntent {
    data class Search(
        val query: String,
    ) : StopsIntent

    data class Select(
        val id: String,
    ) : StopsIntent

    data object Retry : StopsIntent
}

sealed interface StopsEffect {
    data class Navigate(
        val destination: Destination,
    ) : StopsEffect
}

class StopsViewModel(
    private val transport: ObserveTransportUseCase,
    private val search: SearchStopsUseCase,
) : MviViewModel<StopsIntent, StopsEffect>() {
    private val query = MutableStateFlow("")
    val state =
        combine(transport.state, query) { network, term ->
            StopsState(term, search(network.network?.stops.orEmpty(), term), network)
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), StopsState())

    override fun accept(intent: StopsIntent) {
        when (intent) {
            is StopsIntent.Search -> query.value = intent.query
            is StopsIntent.Select -> effect(StopsEffect.Navigate(Destination.StopDetails(intent.id)))
            StopsIntent.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }
}
