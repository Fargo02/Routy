package com.example.routy.feature.stops.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routy.core.navigation.Destination
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.stops.domain.SearchStopsUseCase
import com.example.routy.feature.stops.presentation.state.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StopsViewModel(
    private val transport: ObserveTransportUseCase,
    private val search: SearchStopsUseCase,
) : ViewModel() {
    private val _effects = Channel<StopsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val query = MutableStateFlow("")
    val uiState =
        combine(transport.state, query) { network, term ->
            StopsState(term, search(network.network?.stops.orEmpty(), term), network)
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), StopsState())

    fun actionHandler(action: StopsAction) {
        when (action) {
            is StopsAction.Search -> query.value = action.query
            is StopsAction.Select -> sendEffect(StopsEffect.Navigate(Destination.StopDetails(action.id)))
            StopsAction.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }

    private fun sendEffect(effect: StopsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
