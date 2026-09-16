@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.routy.feature.map.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.routy.core.logging.AppLogger
import com.example.routy.core.logging.LogEvent
import com.example.routy.core.logging.SilentLogger
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import com.example.routy.feature.map.presentation.state.*
import com.example.routy.feature.route_details.domain.GetRouteDetailsUseCase
import com.example.routy.feature.route_details.navigation.RouteDetails
import com.example.routy.feature.routes.domain.SearchRoutesUseCase
import com.example.routy.feature.stop_details.navigation.StopDetails
import com.example.routy.feature.stops.domain.SearchStopsUseCase
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
    private val searchRoutes: SearchRoutesUseCase = SearchRoutesUseCase(),
    private val searchStops: SearchStopsUseCase = SearchStopsUseCase(),
    private val savedState: SavedStateHandle = SavedStateHandle(),
    private val logger: AppLogger = SilentLogger,
) : ViewModel() {
    private val _effects = Channel<MapEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val selectedRouteIds =
        savedState.getStateFlow("routeIds", savedState.get<String>("routeId")?.let(::listOf).orEmpty())
    private val _camera =
        MutableStateFlow(
            MapCamera(
                longitude = savedState.get<Double>("cameraLongitude") ?: MapCamera().longitude,
                latitude = savedState.get<Double>("cameraLatitude") ?: MapCamera().latitude,
                zoom = savedState.get<Double>("cameraZoom") ?: MapCamera().zoom,
            ),
        )
    val camera = _camera.asStateFlow()
    private val searchOpen = MutableStateFlow(savedState.get<Boolean>("searchOpen") ?: false)
    private val searchQuery = MutableStateFlow(savedState.get<String>("searchQuery") ?: "")
    private val searchRequest = combine(searchOpen, searchQuery) { isOpen, query -> isOpen to query }
    val uiState =
        combine(transport.state, selectedRouteIds, favorites.state, searchRequest) { network, routeIds, saved, search ->
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
            val allRoutes = network.network?.routes.orEmpty()
            val orderedRoutes = allRoutes.filter { it.id in saved.routeIds } + allRoutes.filterNot { it.id in saved.routeIds }
            val query = search.second
            val searchState =
                MapSearchState(
                    isOpen = search.first,
                    query = query,
                    quickRoutes = orderedRoutes.take(6),
                    routes = if (query.isBlank()) orderedRoutes else searchRoutes(orderedRoutes, query),
                    stops = if (query.isBlank()) emptyList() else searchStops(network.network?.stops.orEmpty(), query),
                    routeStopCounts =
                        network.network
                            ?.stops
                            .orEmpty()
                            .flatMap { stop -> stop.services.map { it.routeId } }
                            .groupingBy { it }
                            .eachCount(),
                )
            MapState(
                network = network,
                selectedRouteIds = routeIds,
                favoriteRouteIds = saved.routeIds,
                favoriteStopIds = saved.stopIds,
                stops = stops,
                geometries = geometries,
                stopGeoJson = stopsGeoJson(stops, saved.stopIds),
                search = searchState,
            )
        }.onEach { state ->
            logger.diagnostic(
                LogEvent.MapFavoriteStopsUpdated,
                "savedCount=${state.favoriteStopIds.size}, visibleCount=${state.stops.count { it.id in state.favoriteStopIds }}",
            )
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
            is MapAction.SelectRoute -> toggleRoute(action.id)
            MapAction.ClearSelectedRoutes -> savedState["routeIds"] = emptyList<String>()
            MapAction.ClearSelectedStop -> sendEffect(MapEffect.ClearStopSelection)
            is MapAction.SaveCamera -> saveCamera(action.camera)
            MapAction.OpenSearch -> setSearchOpen(true)
            MapAction.CloseSearch -> closeSearch()
            is MapAction.SearchQueryChanged -> setSearchQuery(action.query)
            MapAction.ClearSearch -> setSearchQuery("")
            is MapAction.SelectSearchRoute -> {
                closeSearch()
                toggleRoute(action.id)
            }
            is MapAction.SelectSearchStop -> {
                closeSearch()
                sendEffect(MapEffect.Navigate(StopDetails(action.id)))
            }
            is MapAction.SelectStop -> sendEffect(MapEffect.Navigate(StopDetails(action.id)))
            is MapAction.ShowStopOnMap -> sendEffect(MapEffect.ShowStopOnMap(action.id))
            is MapAction.SelectVehicle -> sendEffect(MapEffect.ShowVehicle(action.id))
            MapAction.OpenStops -> sendEffect(MapEffect.Navigate(Stops))
            MapAction.OpenRouteDetails -> selectedRouteIds.value.lastOrNull()?.let { sendEffect(MapEffect.Navigate(RouteDetails(it))) }
            MapAction.MyLocation -> sendEffect(MapEffect.RequestLocation)
            MapAction.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }

    private fun toggleRoute(id: String) {
        val selected = selectedRouteIds.value
        savedState["routeIds"] = if (id in selected) selected - id else selected + id
    }

    private fun saveCamera(camera: MapCamera) {
        _camera.value = camera
        savedState["cameraLongitude"] = camera.longitude
        savedState["cameraLatitude"] = camera.latitude
        savedState["cameraZoom"] = camera.zoom
    }

    private fun closeSearch() {
        setSearchOpen(false)
        setSearchQuery("")
    }

    private fun setSearchOpen(isOpen: Boolean) {
        searchOpen.value = isOpen
        savedState["searchOpen"] = isOpen
    }

    private fun setSearchQuery(query: String) {
        searchQuery.value = query
        savedState["searchQuery"] = query
    }

    private fun sendEffect(effect: MapEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
