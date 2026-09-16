package com.example.routy.feature.routes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.route_details.navigation.RouteDetails
import com.example.routy.feature.routes.domain.SearchRoutesUseCase
import com.example.routy.feature.routes.presentation.state.*
import com.example.routy.feature.stop_details.navigation.StopDetails
import com.example.routy.feature.stops.domain.SearchStopsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RoutesViewModel(
    private val transport: ObserveTransportUseCase,
    private val search: SearchRoutesUseCase,
    private val searchStops: SearchStopsUseCase = SearchStopsUseCase(),
) : ViewModel() {
    private val _effects = Channel<RoutesEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val query = MutableStateFlow("")
    val uiState =
        combine(transport.state, query) { network, term ->
            RoutesState(
                term,
                search(network.network?.routes.orEmpty(), term),
                network,
                if (term.isBlank()) emptyList() else searchStops(network.network?.stops.orEmpty(), term),
            )
        }.flowOn(Dispatchers.Default).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(0),
            RoutesState(),
        )

    fun actionHandler(action: RoutesAction) {
        when (action) {
            is RoutesAction.Search -> query.value = action.query
            is RoutesAction.SelectStop -> sendEffect(RoutesEffect.Navigate(StopDetails(action.id)))
            is RoutesAction.Select -> sendEffect(RoutesEffect.Navigate(RouteDetails(action.id)))
            RoutesAction.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }

    private fun sendEffect(effect: RoutesEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
