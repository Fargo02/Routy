@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.routy.feature.map.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routy.core.navigation.Destination
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.map.presentation.state.*
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(
    private val transport: ObserveTransportUseCase,
    vehicles: ObserveRouteVehiclesUseCase,
    details: GetRouteDetailsUseCase,
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val _effects = Channel<MapEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val selectedRoute = savedState.getStateFlow<String?>("routeId", null)
    val uiState =
        combine(transport.state, selectedRoute) { network, routeId ->
            val route = network.network?.let { data -> routeId?.let { details(data, it) } }
            val stops =
                if (routeId == null) {
                    network.network?.stops.orEmpty()
                } else {
                    route
                        ?.groups
                        ?.values
                        ?.flatten()
                        ?.map { it.stop }
                        ?.distinctBy { it.id }
                        .orEmpty()
                }
            MapState(network, routeId, stops, route?.geometry, stopsGeoJson(stops), routeGeoJson(route?.geometry))
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), MapState())
    val vehicles =
        selectedRoute
            .flatMapLatest { id -> if (id == null) flowOf(VehicleState(isLoading = false)) else vehicles(id) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
                VehicleState(isLoading = false),
            )

    fun actionHandler(action: MapAction) {
        when (action) {
            is MapAction.SelectRoute -> savedState["routeId"] = action.id
            is MapAction.SelectStop -> sendEffect(MapEffect.Navigate(Destination.StopDetails(action.id)))
            is MapAction.SelectVehicle -> sendEffect(MapEffect.ShowVehicle(action.id))
            MapAction.OpenSearch -> sendEffect(MapEffect.Navigate(Destination.Routes))
            MapAction.OpenStops -> sendEffect(MapEffect.Navigate(Destination.Stops))
            MapAction.OpenSettings -> sendEffect(MapEffect.Navigate(Destination.Settings))
            MapAction.OpenRouteDetails -> selectedRoute.value?.let { sendEffect(MapEffect.Navigate(Destination.RouteDetails(it))) }
            MapAction.FitRoute -> sendEffect(MapEffect.FitRoute)
            MapAction.MyLocation -> sendEffect(MapEffect.RequestLocation)
            MapAction.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }

    private fun sendEffect(effect: MapEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
