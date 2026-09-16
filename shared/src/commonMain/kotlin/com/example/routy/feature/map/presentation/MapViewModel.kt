@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.routy.feature.map.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import com.example.routy.feature.map.presentation.state.*
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import com.example.routy.feature.route_details.navigation.RouteDetails
import com.example.routy.feature.routes.navigation.Routes
import com.example.routy.feature.stop_details.navigation.StopDetails
import com.example.routy.feature.stops.navigation.Stops
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(
    private val transport: ObserveTransportUseCase,
    vehicles: ObserveRouteVehiclesUseCase,
    details: GetRouteDetailsUseCase,
    favorites: FavoritesUseCase,
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val _effects = Channel<MapEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val selectedRouteIds =
        savedState.getStateFlow("routeIds", savedState.get<String>("routeId")?.let(::listOf).orEmpty())
    val uiState =
        combine(transport.state, selectedRouteIds, favorites.state) { network, routeIds, saved ->
            val routes = network.network?.let { data -> routeIds.mapNotNull { details(data, it) } }.orEmpty()
            val stops =
                if (routeIds.isEmpty()) {
                    network.network?.stops.orEmpty()
                } else {
                    routes
                        .flatMap { it.groups.values.flatten() }
                        .map { it.stop }
                        .distinctBy { it.id }
                }
            val geometries = routes.mapNotNull { it.geometry }
            MapState(network, routeIds, saved.routeIds, stops, geometries, stopsGeoJson(stops))
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), MapState())
    val vehicles =
        selectedRouteIds
            .flatMapLatest { routeIds ->
                if (routeIds.isEmpty()) {
                    flowOf(VehicleState(isLoading = false))
                } else {
                    combine(routeIds.map { vehicles(it) }) { states ->
                        VehicleState(
                            vehicles = states.flatMap { it.vehicles },
                            isLoading = states.any { it.isLoading },
                            isStale = states.any { it.isStale },
                            updatedAtMillis = states.mapNotNull { it.updatedAtMillis }.maxOrNull(),
                            error = states.firstNotNullOfOrNull { it.error },
                        )
                    }
                }
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
                VehicleState(isLoading = false),
            )

    fun actionHandler(action: MapAction) {
        when (action) {
            is MapAction.SelectRoute -> {
                val selected = selectedRouteIds.value
                savedState["routeIds"] = if (action.id in selected) selected - action.id else selected + action.id
            }
            is MapAction.SelectStop -> sendEffect(MapEffect.Navigate(StopDetails(action.id)))
            is MapAction.SelectVehicle -> sendEffect(MapEffect.ShowVehicle(action.id))
            MapAction.OpenSearch -> sendEffect(MapEffect.Navigate(Routes))
            MapAction.OpenStops -> sendEffect(MapEffect.Navigate(Stops))
            MapAction.OpenRouteDetails -> selectedRouteIds.value.lastOrNull()?.let { sendEffect(MapEffect.Navigate(RouteDetails(it))) }
            MapAction.MyLocation -> sendEffect(MapEffect.RequestLocation)
            MapAction.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }

    private fun sendEffect(effect: MapEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
