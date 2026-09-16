@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.routy.feature.map.presentation

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

    data object RequestLocation : MapEffect
}

class MapViewModel(
    private val transport: ObserveTransportUseCase,
    vehicles: ObserveRouteVehiclesUseCase,
    details: GetRouteDetailsUseCase,
) : MviViewModel<MapIntent, MapEffect>() {
    private val selectedRoute = MutableStateFlow<String?>(null)
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
            is MapIntent.SelectRoute -> selectedRoute.value = intent.id
            is MapIntent.SelectStop -> effect(MapEffect.Navigate(Destination.StopDetails(intent.id)))
            is MapIntent.SelectVehicle -> effect(MapEffect.ShowVehicle(intent.id))
            MapIntent.MyLocation -> effect(MapEffect.RequestLocation)
            MapIntent.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }
}
