@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.routy.feature.map.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.routy.core.mvi.MviViewModel
import com.example.routy.core.navigation.Destination
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MapState(
    val network: NetworkState = NetworkState(),
    val routeId: String? = null,
    val stops: List<BusStop> = emptyList(),
    val geometry: RouteGeometry? = null,
    val stopGeoJson: String = EMPTY_GEOJSON,
    val routeGeoJson: String = EMPTY_GEOJSON,
)

sealed interface MapIntent {
    data class SelectRoute(
        val id: String?,
    ) : MapIntent

    data class SelectStop(
        val id: String,
    ) : MapIntent

    data class SelectVehicle(
        val id: String,
    ) : MapIntent

    data object OpenSearch : MapIntent

    data object OpenStops : MapIntent

    data object OpenSettings : MapIntent

    data object OpenRouteDetails : MapIntent

    data object FitRoute : MapIntent

    data object MyLocation : MapIntent

    data object Retry : MapIntent
}

sealed interface MapEffect {
    data class Navigate(
        val destination: Destination,
    ) : MapEffect

    data class ShowVehicle(
        val id: String,
    ) : MapEffect

    data object FitRoute : MapEffect

    data object RequestLocation : MapEffect
}

class MapViewModel(
    private val transport: ObserveTransportUseCase,
    vehicles: ObserveRouteVehiclesUseCase,
    details: GetRouteDetailsUseCase,
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : MviViewModel<MapIntent, MapEffect>() {
    private val selectedRoute = savedState.getStateFlow<String?>("routeId", null)
    val state =
        combine(transport.state, selectedRoute) { network, routeId ->
            val route = network.network?.let { data -> routeId?.let { details(data, it) } }
            val stops =
                if (routeId ==
                    null
                ) {
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

    override fun accept(intent: MapIntent) {
        when (intent) {
            is MapIntent.SelectRoute -> savedState["routeId"] = intent.id
            is MapIntent.SelectStop -> effect(MapEffect.Navigate(Destination.StopDetails(intent.id)))
            is MapIntent.SelectVehicle -> effect(MapEffect.ShowVehicle(intent.id))
            MapIntent.OpenSearch -> effect(MapEffect.Navigate(Destination.Routes))
            MapIntent.OpenStops -> effect(MapEffect.Navigate(Destination.Stops))
            MapIntent.OpenSettings -> effect(MapEffect.Navigate(Destination.Settings))
            MapIntent.OpenRouteDetails -> selectedRoute.value?.let { effect(MapEffect.Navigate(Destination.RouteDetails(it))) }
            MapIntent.FitRoute -> effect(MapEffect.FitRoute)
            MapIntent.MyLocation -> effect(MapEffect.RequestLocation)
            MapIntent.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }
}
