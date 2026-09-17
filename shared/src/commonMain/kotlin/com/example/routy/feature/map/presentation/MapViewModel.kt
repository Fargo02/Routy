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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(
    private val transport: ObserveTransportUseCase,
    private val observeVehicles: ObserveRouteVehiclesUseCase,
    details: GetRouteDetailsUseCase,
    private val favorites: FavoritesUseCase,
    private val searchRoutes: SearchRoutesUseCase = SearchRoutesUseCase(),
    private val searchStops: SearchStopsUseCase = SearchStopsUseCase(),
    private val savedState: SavedStateHandle = SavedStateHandle(),
    private val logger: AppLogger = SilentLogger,
) : ViewModel() {
    private companion object {
        const val MaxSelectedRoutes = 7
    }

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
    private val activeRouteIds =
        combine(selectedRouteIds, favorites.state, transport.state) { routeIds, saved, network ->
            val recoveryRouteIds =
                if (saved.trackedRouteId == null && saved.trackedStopId != null) {
                    network.network
                        ?.stops
                        ?.firstOrNull { it.id == saved.trackedStopId }
                        ?.services
                        ?.map { it.routeId }
                        .orEmpty()
                } else {
                    emptyList()
                }
            (routeIds + listOfNotNull(saved.trackedRouteId) + recoveryRouteIds).distinct()
        }
    private val vehicleStatesByRoute = mutableMapOf<String, VehicleState>()
    private val vehicleJobsByRoute = mutableMapOf<String, Job>()
    private val vehicleSubscriberCount = MutableStateFlow(0)
    private val _vehicles = MutableStateFlow(VehicleState(isLoading = false))
    private var activeVehicleRouteIds = emptyList<String>()

    /**
     * A single route has its own polling job. Adding another route therefore cannot cancel the
     * existing job and briefly replace its markers with the repository's initial empty state.
     */
    val vehicles: Flow<VehicleState> =
        _vehicles
            .onSubscription { vehicleSubscriberCount.update { it + 1 } }
            .onCompletion { vehicleSubscriberCount.update { count -> (count - 1).coerceAtLeast(0) } }

    val uiState =
        combine(transport.state, activeRouteIds, selectedRouteIds, favorites.state, searchRequest) { network, activeRouteIds, selectedRouteIds, saved, search ->
            val routes = network.network?.let { data -> activeRouteIds.mapNotNull { details(data, it) } }.orEmpty()
            val stops =
                if (activeRouteIds.isEmpty()) {
                    network.network?.stops.orEmpty()
                } else {
                    routes
                        .flatMap { it.groups.values.flatten() }
                        .map { it.stop }
                        .distinctBy { it.id }
                }
            val geometries =
                network.network
                    ?.let { data -> selectedRouteIds.mapNotNull { routeId -> details(data, routeId)?.geometry } }
                    .orEmpty()
            val allRoutes = network.network?.routes.orEmpty()
            val selectedRoutes = allRoutes.filter { it.id in selectedRouteIds }
            val favoriteRoutes = allRoutes.filter { it.id in saved.routeIds && it.id !in selectedRouteIds }
            val remainingRoutes = allRoutes.filterNot { it.id in selectedRouteIds || it.id in saved.routeIds }
            val orderedRoutes = selectedRoutes + favoriteRoutes + remainingRoutes
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
                selectedRouteIds = selectedRouteIds,
                favoriteRouteIds = saved.routeIds,
                favoriteStopIds = saved.stopIds,
                trackedVehicleId = saved.trackedVehicleId,
                trackedStopId = saved.trackedStopId,
                trackedRouteId = saved.trackedRouteId,
                stops = stops,
                geometries = geometries,
                trackingGeometry = saved.trackedRouteId?.let { routeId -> network.network?.geometries?.get(routeId) },
                stopGeoJson = stopsGeoJson(stops, saved.stopIds),
                search = searchState,
            )
        }.onEach { state ->
            logger.diagnostic(
                LogEvent.MapFavoriteStopsUpdated,
                "savedCount=${state.favoriteStopIds.size}, visibleCount=${state.stops.count { it.id in state.favoriteStopIds }}",
            )
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), MapState())
    init {
        viewModelScope.launch {
            combine(activeRouteIds, vehicleSubscriberCount) { routeIds, subscriberCount -> routeIds to subscriberCount }
                .collect { (routeIds, subscriberCount) ->
                    reconcileVehiclePolling(routeIds, subscriberCount)
                }
        }
    }

    private fun reconcileVehiclePolling(routeIds: List<String>, subscriberCount: Int) {
        if (subscriberCount == 0) {
            vehicleJobsByRoute.values.forEach(Job::cancel)
            vehicleJobsByRoute.clear()
            return
        }

        activeVehicleRouteIds = routeIds
        vehicleJobsByRoute.keys.filter { it !in routeIds }.toList().forEach { routeId ->
            vehicleJobsByRoute.remove(routeId)?.cancel()
            vehicleStatesByRoute.remove(routeId)
        }
        routeIds.filterNot(vehicleJobsByRoute::containsKey).forEach { routeId ->
            vehicleJobsByRoute[routeId] =
                viewModelScope.launch {
                    observeVehicles(routeId)
                        .onStart {
                            logger.diagnostic(
                                LogEvent.MapVehiclePollingStarted,
                                "activeRoutes=${vehicleJobsByRoute.size}",
                            )
                        }.onCompletion {
                            logger.diagnostic(
                                LogEvent.MapVehiclePollingStopped,
                                "activeRoutes=${vehicleJobsByRoute.size}",
                            )
                        }.collect { state ->
                            vehicleStatesByRoute[routeId] = state
                            publishVehicleState(activeVehicleRouteIds)
                        }
                }
        }
        publishVehicleState(routeIds)
    }

    private fun publishVehicleState(routeIds: List<String>) {
        val states = routeIds.mapNotNull(vehicleStatesByRoute::get)
        _vehicles.value =
            VehicleState(
                vehicles = states.flatMap { it.vehicles },
                isLoading = states.any { it.isLoading },
                isStale = states.any { it.isStale },
                updatedAtMillis = states.mapNotNull { it.updatedAtMillis }.maxOrNull(),
                error = states.firstNotNullOfOrNull { it.error },
            )
        logger.diagnostic(
            LogEvent.MapVehicleStateCombined,
            "vehicles=${_vehicles.value.vehicles.size}, loading=${_vehicles.value.isLoading}, stale=${_vehicles.value.isStale}",
        )
    }

    fun logVehicleLayersComposed(
        routeCount: Int,
        vehicleCount: Int,
        selectedRouteCount: Int,
    ) {
        logger.diagnostic(
            LogEvent.MapVehicleLayersComposed,
            "routes=$routeCount, vehicles=$vehicleCount, selected=$selectedRouteCount",
        )
    }

    fun logVehicleLayerAttached(vehicleCount: Int) {
        logger.diagnostic(LogEvent.MapVehicleLayerAttached, "vehicles=$vehicleCount")
    }

    fun logVehicleLayerDetached() {
        logger.diagnostic(LogEvent.MapVehicleLayerDetached, "")
    }

    fun actionHandler(action: MapAction) {
        when (action) {
            is MapAction.SelectRoute -> toggleRoute(action.id)
            MapAction.ClearSelectedRoutes -> savedState["routeIds"] = emptyList<String>()
            MapAction.ClearSelectedStop -> sendEffect(MapEffect.ClearStopSelection)
            is MapAction.SaveCamera -> saveCamera(action.camera)
            is MapAction.ToggleRouteFavorite -> viewModelScope.launch { favorites.route(action.id) }
            is MapAction.SetTracking -> viewModelScope.launch { favorites.tracking(action.vehicleId, action.stopId, action.routeId) }
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
            is MapAction.OpenRouteDetails -> sendEffect(MapEffect.Navigate(RouteDetails(action.id)))
            MapAction.MyLocation -> sendEffect(MapEffect.RequestLocation)
            MapAction.Retry -> viewModelScope.launch { transport.refresh() }
        }
    }

    private fun toggleRoute(id: String) {
        val selected = selectedRouteIds.value
        savedState["routeIds"] =
            when {
                id in selected -> selected - id
                selected.size >= MaxSelectedRoutes -> selected
                else -> selected + id
            }
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
